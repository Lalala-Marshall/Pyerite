package com.marshall.pyerite.eveAuthModule.token

import com.marshall.pyerite.eveAuthModule.model.EveSessionIdentity
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.eveAuthModule.model.EveStoredSession
import com.marshall.pyerite.eveAuthModule.model.EveTokenSet
import com.marshall.pyerite.eveAuthModule.model.authorizationHeader
import com.marshall.pyerite.eveAuthModule.model.normalizeTokenType
import com.marshall.pyerite.eveAuthModule.model.toStoredSession
import com.marshall.pyerite.eveAuthModule.sso.EveJwtDecoder
import com.marshall.pyerite.eveAuthModule.sso.EveSsoConfig
import com.marshall.pyerite.eveAuthModule.sso.EveSsoHttpException
import com.marshall.pyerite.eveAuthModule.sso.EveSsoRemoteDataSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.util.concurrent.ConcurrentHashMap

internal sealed interface EveTokenRefreshResult {
    data object NotFound : EveTokenRefreshResult
    data class Unchanged(val tokenSet: EveTokenSet) : EveTokenRefreshResult
    data class Refreshed(val tokenSet: EveTokenSet) : EveTokenRefreshResult
    data class Failed(
        val characterId: Long,
        val error: Throwable,
        val permanent: Boolean,
    ) : EveTokenRefreshResult
}

/**
 * Sole issuer of access tokens.
 * Persistence ([EveTokenStore]) and raw [EveTokenSet] stay internal to this package.
 */
class EveTokenManager internal constructor(
    private val remote: EveSsoRemoteDataSource,
    private val tokenStore: EveTokenStore,
) {
    private val characterLocks = ConcurrentHashMap<Long, Mutex>()

    /** Returns a usable access token, refreshing when within the pre-expiry buffer. */
    suspend fun getValidAccessToken(characterId: Long): String =
        accessTokenFor(refreshIfNeeded(characterId), characterId)

    /** Forces a refresh regardless of expiry (e.g. after HTTP 401). */
    suspend fun forceRefresh(characterId: Long): String =
        accessTokenFor(forceRefreshInternal(characterId), characterId)

    /** Session presence only — never returns token material. */
    fun hasStoredSession(characterId: Long): Boolean =
        tokenStore.get(characterId) != null

    /** Granted scopes for UI / feature gates — never returns token material. */
    fun grantedScopes(characterId: Long): Set<EveSsoScope> =
        tokenStore.get(characterId)?.grantedScopes.orEmpty()

    /** Logged-in characters (id + name only) for pickers that must not read the token store. */
    fun sessionIdentities(): List<EveSessionIdentity> =
        tokenStore.all().map { EveSessionIdentity(it.characterId, it.characterName) }

    /** Persist after SSO code exchange. */
    internal fun save(tokenSet: EveTokenSet): EveStoredSession {
        tokenStore.save(tokenSet)
        return tokenSet.toStoredSession()
    }

    internal fun peekRefreshToken(characterId: Long): String? =
        tokenStore.get(characterId)?.refreshToken

    internal fun remove(characterId: Long) {
        tokenStore.remove(characterId)
    }

    /** Non-secret identities for session restore. */
    internal fun storedSessions(): List<EveStoredSession> =
        tokenStore.all().map { it.toStoredSession() }

    /** Batch refresh for app start / foreground; auth layer applies invalidation. */
    internal suspend fun refreshAllIfNeeded(): List<EveTokenRefreshResult> =
        tokenStore.all().map { refreshIfNeeded(it.characterId) }

    /** Authenticated ESI entry point: valid token first, one forced refresh on 401. */
    internal suspend fun <T> executeWithAuthRetry(
        characterId: Long,
        block: suspend (authorization: String) -> T,
    ): T {
        try {
            return block(authorizationHeader(characterId, getValidAccessToken(characterId)))
        } catch (httpError: HttpException) {
            if (httpError.code() != 401) throw httpError
        }
        return block(authorizationHeader(characterId, forceRefresh(characterId)))
    }

    private suspend fun forceRefreshInternal(characterId: Long): EveTokenRefreshResult =
        lockFor(characterId).withLock {
            val existing = tokenStore.get(characterId) ?: return@withLock EveTokenRefreshResult.NotFound
            refreshAndPersist(existing)
        }

    private suspend fun refreshIfNeeded(characterId: Long): EveTokenRefreshResult =
        lockFor(characterId).withLock {
            val existing = tokenStore.get(characterId) ?: return@withLock EveTokenRefreshResult.NotFound
            if (!needsRefresh(existing)) {
                return@withLock EveTokenRefreshResult.Unchanged(existing)
            }
            refreshAndPersist(existing)
        }

    private fun needsRefresh(
        tokenSet: EveTokenSet,
        now: Long = System.currentTimeMillis(),
    ): Boolean = now >= tokenSet.expiresAtEpochMs - EveSsoConfig.ACCESS_TOKEN_REFRESH_BUFFER_MS

    private fun accessTokenFor(result: EveTokenRefreshResult, characterId: Long): String =
        when (result) {
            is EveTokenRefreshResult.Unchanged -> result.tokenSet.accessToken
            is EveTokenRefreshResult.Refreshed -> result.tokenSet.accessToken
            is EveTokenRefreshResult.NotFound -> error("No token for character $characterId")
            is EveTokenRefreshResult.Failed -> throw result.error
        }

    private fun authorizationHeader(characterId: Long, accessToken: String): String {
        val stored = tokenStore.get(characterId)
            ?: return "${EveSsoConfig.TOKEN_TYPE_BEARER} $accessToken"
        return stored.copy(accessToken = accessToken).authorizationHeader()
    }

    private suspend fun refreshAndPersist(existing: EveTokenSet): EveTokenRefreshResult {
        return try {
            val response = remote.refreshAccessToken(existing.refreshToken)
            val fromExpiresIn = System.currentTimeMillis() +
                response.expiresInSeconds * EveSsoConfig.MILLIS_PER_SECOND
            val jwtExp = runCatching {
                EveJwtDecoder.decodeAndVerify(response.accessToken, remote.fetchJwks()).expiresAtEpochMs
            }.getOrNull()
            val updated = existing.copy(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken.ifBlank { existing.refreshToken },
                tokenType = normalizeTokenType(response.tokenType),
                expiresAtEpochMs = listOfNotNull(fromExpiresIn, jwtExp).minOrNull() ?: fromExpiresIn,
                scopes = response.scopes.ifEmpty { existing.scopes },
            )
            tokenStore.save(updated)
            EveTokenRefreshResult.Refreshed(updated)
        } catch (error: Exception) {
            val permanent = isPermanentAuthFailure(error)
            if (permanent) {
                tokenStore.remove(existing.characterId)
            } else if (System.currentTimeMillis() < existing.expiresAtEpochMs) {
                return EveTokenRefreshResult.Unchanged(existing)
            }
            EveTokenRefreshResult.Failed(
                characterId = existing.characterId,
                error = error,
                permanent = permanent,
            )
        }
    }

    private fun isPermanentAuthFailure(error: Throwable): Boolean {
        when (error) {
            is EveSsoHttpException -> return error.isPermanentRefreshFailure
            is HttpException -> {
                if (error.code() in EveSsoConfig.permanentRefreshHttpStatuses) return true
                val body = runCatching { error.response()?.errorBody()?.string().orEmpty() }.getOrDefault("")
                return body.contains(EveSsoConfig.OAuth.ERROR_INVALID_GRANT, ignoreCase = true)
            }
        }
        return error.message.orEmpty()
            .contains(EveSsoConfig.OAuth.ERROR_INVALID_GRANT, ignoreCase = true)
    }

    private fun lockFor(characterId: Long): Mutex =
        characterLocks.getOrPut(characterId) { Mutex() }
}
