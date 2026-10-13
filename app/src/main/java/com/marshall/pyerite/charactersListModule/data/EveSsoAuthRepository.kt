package com.marshall.pyerite.charactersListModule.data

import android.net.Uri
import androidx.annotation.StringRes
import com.marshall.pyerite.R
import com.marshall.pyerite.charactersListModule.viewModel.CharacterRepository
import com.marshall.pyerite.infra.network.PyeriteJson
import com.marshall.pyerite.esiModule.data.portraitUrl
import com.marshall.pyerite.eveAuthModule.sso.EveJwtDecoder
import com.marshall.pyerite.eveAuthModule.model.EveOAuthErrorDto
import com.marshall.pyerite.eveAuthModule.sso.EvePendingLoginStore
import com.marshall.pyerite.eveAuthModule.sso.EveSsoCallbackBus
import com.marshall.pyerite.eveAuthModule.sso.EveSsoConfig
import com.marshall.pyerite.eveAuthModule.sso.EveSsoHttpException
import com.marshall.pyerite.eveAuthModule.sso.EveSsoPkce
import com.marshall.pyerite.eveAuthModule.sso.EveSsoRemoteDataSource
import com.marshall.pyerite.eveAuthModule.model.EveStoredSession
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.infra.notification.SkillQueueExpiryScheduler
import com.marshall.pyerite.eveAuthModule.token.EveTokenRefreshResult
import com.marshall.pyerite.eveAuthModule.model.EveTokenSet
import com.marshall.pyerite.eveAuthModule.model.normalizeTokenType
import com.marshall.pyerite.eveAuthModule.sso.queryParamOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

sealed interface EveSsoUiStatus {
    data object Idle : EveSsoUiStatus
    data object AwaitingBrowser : EveSsoUiStatus
    data object ExchangingToken : EveSsoUiStatus
    data class Failed(
        @param:StringRes val messageRes: Int,
        val formatArgs: List<Any> = emptyList(),
    ) : EveSsoUiStatus
    data class Succeeded(val characterName: String) : EveSsoUiStatus
}

class EveSsoAuthRepository internal constructor(
    private val remote: EveSsoRemoteDataSource,
    private val tokenManager: EveTokenManager,
    private val profileLoader: CharacterProfileLoader,
    private val characterRepository: CharacterRepository,
    private val callbackBus: EveSsoCallbackBus,
    private val pendingLoginStore: EvePendingLoginStore,
    private val profileCache: CharacterProfileCache,
    private val skillQueueExpiryScheduler: SkillQueueExpiryScheduler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    /** Cold start already refreshed tokens in [restoreSessions]; skip one ProcessLifecycle onStart. */
    private val skipNextForegroundRefresh = AtomicBoolean(false)
    private val profileRefreshInFlight = AtomicBoolean(false)

    private val _status = MutableStateFlow<EveSsoUiStatus>(EveSsoUiStatus.Idle)
    val status: StateFlow<EveSsoUiStatus> = _status.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshFailed = MutableStateFlow(false)
    val refreshFailed: StateFlow<Boolean> = _refreshFailed.asStateFlow()

    init {
        scope.launch {
            restoreSessions()
            if (pendingLoginStore.hasPending()) {
                _status.value = EveSsoUiStatus.AwaitingBrowser
            }
            callbackBus.callbacks.collect { uri ->
                handleCallback(uri)
            }
        }
    }

    fun clearStatus() {
        _status.value = EveSsoUiStatus.Idle
    }

    fun cancelLogin() {
        pendingLoginStore.clear()
        _status.value = EveSsoUiStatus.Idle
    }

    /** Returns the authorization URL for the UI to open, or null if preparation failed. */
    suspend fun prepareLogin(): String? = mutex.withLock {
        if (EveSsoConfig.clientId.isBlank()) {
            _status.value = EveSsoUiStatus.Failed(R.string.character_sso_error_missing_client_id)
            return@withLock null
        }
        val verifier = EveSsoPkce.generateVerifier()
        val challenge = EveSsoPkce.challengeS256(verifier)
        val state = EveSsoPkce.generateState()
        pendingLoginStore.save(state = state, codeVerifier = verifier)

        val authorizationEndpoint = remote.resolveAuthorizationEndpoint()
        val url = buildAuthorizationUrl(
            authorizationEndpoint = authorizationEndpoint,
            state = state,
            codeChallenge = challenge,
        )
        _status.value = EveSsoUiStatus.AwaitingBrowser
        url
    }

    suspend fun refreshStoredSessionsOnForeground() {
        if (skipNextForegroundRefresh.compareAndSet(true, false)) return
        withContext(Dispatchers.IO) {
            applyRefreshResults(tokenManager.refreshAllIfNeeded())
        }
    }

    fun removeCharacterSession(characterId: Long) {
        scope.launch(Dispatchers.IO) {
            skillQueueExpiryScheduler.clear(characterId)
            val refreshToken = tokenManager.peekRefreshToken(characterId)
            tokenManager.remove(characterId)
            profileCache.remove(characterId)
            characterRepository.removeLoggedInCharacter(characterId)
            if (!refreshToken.isNullOrBlank()) {
                runCatching { remote.revokeRefreshToken(refreshToken) }
            }
        }
    }

    private suspend fun handleCallback(uri: Uri) {
        mutex.withLock {
            val error = uri.queryParamOrNull(EveSsoConfig.OAuth.QUERY_ERROR)
            if (error != null) {
                pendingLoginStore.clear()
                val description = uri.queryParamOrNull(EveSsoConfig.OAuth.QUERY_ERROR_DESCRIPTION)
                _status.value = providerOrDeniedFailure(description)
                return
            }

            val code = uri.queryParamOrNull(EveSsoConfig.OAuth.QUERY_CODE)
            val state = uri.queryParamOrNull(EveSsoConfig.OAuth.QUERY_STATE)
            if (code.isNullOrBlank() || state.isNullOrBlank()) {
                pendingLoginStore.clear()
                _status.value = EveSsoUiStatus.Failed(R.string.character_sso_error_invalid_callback)
                return
            }

            val verifier = pendingLoginStore.consume(state)
            if (verifier.isNullOrBlank()) {
                _status.value = EveSsoUiStatus.Failed(R.string.character_sso_error_state_mismatch)
                return
            }

            _status.value = EveSsoUiStatus.ExchangingToken
            try {
                val tokenResponse = remote.exchangeAuthorizationCode(code, verifier)
                val claims = EveJwtDecoder.decodeAndVerify(tokenResponse.accessToken, remote.fetchJwks())
                EveJwtDecoder.assertBasicClaims(claims, EveSsoConfig.clientId)

                val fromExpiresIn = System.currentTimeMillis() +
                    tokenResponse.expiresInSeconds * EveSsoConfig.MILLIS_PER_SECOND
                val session = tokenManager.save(
                    EveTokenSet(
                        characterId = claims.characterId,
                        characterName = claims.characterName,
                        accessToken = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken,
                        tokenType = normalizeTokenType(tokenResponse.tokenType),
                        expiresAtEpochMs = minOf(fromExpiresIn, claims.expiresAtEpochMs),
                        scopes = tokenResponse.scopes.ifEmpty { claims.scopes },
                    ),
                )

                val loggedIn = profileLoader.load(session)
                profileCache.save(loggedIn)
                characterRepository.upsertLoggedInCharacter(loggedIn)
                characterRepository.selectCurrentCharacter(loggedIn)
                _status.value = EveSsoUiStatus.Succeeded(loggedIn.name)
            } catch (error: Exception) {
                _status.value = failureFromException(error)
            }
        }
    }

    /**
     * Manual / pull-to-refresh: runs on the auth repository scope so leaving the
     * character list does not cancel work. No-ops if a refresh is already in flight.
     */
    fun requestLoggedInProfilesRefresh() {
        scope.launch {
            withProfileRefresh {
                val tokensOk = applyRefreshResults(tokenManager.refreshAllIfNeeded())
                val profilesOk = enrichProfilesDeferred(tokenManager.storedSessions())
                tokensOk && profilesOk
            }
        }
    }

    /**
     * Startup restore priority: must (local session) → cache → deferred network enrich.
     * UI is hydrated before any ESI profile calls so a slow network never looks like logout.
     */
    private suspend fun restoreSessions() {
        withContext(Dispatchers.IO) {
            val sessions = tokenManager.storedSessions()
            hydrateLocalCharacters(sessions)
            characterRepository.restoreCurrentCharacterSelection()

            skipNextForegroundRefresh.set(true)
            val remaining = tokenManager.storedSessions()
            if (remaining.isEmpty()) return@withContext
            withProfileRefresh {
                val tokensOk = applyRefreshResults(tokenManager.refreshAllIfNeeded())
                val profilesOk = enrichProfilesDeferred(tokenManager.storedSessions())
                tokensOk && profilesOk
            }
        }
    }

    private suspend fun withProfileRefresh(block: suspend () -> Boolean) {
        if (!profileRefreshInFlight.compareAndSet(false, true)) return
        _isRefreshing.value = true
        _refreshFailed.value = false
        var success = false
        try {
            withContext(Dispatchers.IO) {
                success = block()
            }
        } catch (_: Exception) {
            success = false
        } finally {
            _isRefreshing.value = false
            _refreshFailed.value = !success
            profileRefreshInFlight.set(false)
        }
    }

    private fun hydrateLocalCharacters(sessions: List<EveStoredSession>) {
        sessions.forEach { session ->
            val scopes = tokenManager.grantedScopes(session.characterId)
            val cached = profileCache.get(session.characterId)
            val display = when {
                cached != null -> cached.copy(
                    name = session.characterName.ifBlank { cached.name },
                    portraitUrl = portraitUrl(session.characterId),
                    grantedScopes = scopes.ifEmpty { cached.grantedScopes },
                )
                else -> localLoggedInCharacter(session, scopes)
            }
            characterRepository.upsertLoggedInCharacter(display)
        }
        characterRepository.applyPersistedCharacterOrder()
    }

    /** @return false if any profile enrich failed. */
    private suspend fun enrichProfilesDeferred(sessions: List<EveStoredSession>): Boolean {
        if (sessions.isEmpty()) return true
        val results = coroutineScope {
            sessions.map { session ->
                async {
                    runCatching {
                        val loaded = profileLoader.load(session)
                        profileCache.save(loaded)
                        characterRepository.upsertLoggedInCharacter(loaded)
                    }.isSuccess
                }
            }.awaitAll()
        }
        return results.all { it }
    }

    /**
     * Applies permanent session removals.
     * @return false if any non-permanent token refresh failed.
     */
    private fun applyRefreshResults(results: List<EveTokenRefreshResult>): Boolean {
        var hadTransientFailure = false
        results.forEach { result ->
            if (result is EveTokenRefreshResult.Failed) {
                if (result.permanent) {
                    profileCache.remove(result.characterId)
                    characterRepository.removeLoggedInCharacter(result.characterId)
                } else {
                    hadTransientFailure = true
                }
            }
        }
        return !hadTransientFailure
    }

    private fun buildAuthorizationUrl(
        authorizationEndpoint: String,
        state: String,
        codeChallenge: String,
    ): String {
        val scope = EveSsoConfig.requestedScopeApiValues.joinToString(" ")
        val query = listOf(
            EveSsoConfig.OAuth.PARAM_RESPONSE_TYPE to EveSsoConfig.OAuth.RESPONSE_TYPE_CODE,
            EveSsoConfig.OAuth.PARAM_REDIRECT_URI to EveSsoConfig.redirectUri,
            EveSsoConfig.OAuth.PARAM_CLIENT_ID to EveSsoConfig.clientId,
            EveSsoConfig.OAuth.PARAM_SCOPE to scope,
            EveSsoConfig.OAuth.PARAM_STATE to state,
            EveSsoConfig.OAuth.PARAM_CODE_CHALLENGE to codeChallenge,
            EveSsoConfig.OAuth.PARAM_CODE_CHALLENGE_METHOD to
                EveSsoConfig.OAuth.CODE_CHALLENGE_METHOD_S256,
        ).joinToString("&") { (key, value) ->
            "${enc(key)}=${enc(value)}"
        }
        return "$authorizationEndpoint?$query"
    }

    private fun enc(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}

private fun providerOrDeniedFailure(description: String?): EveSsoUiStatus.Failed {
    val detail = description?.trim()?.takeIf { it.isNotEmpty() }
    return if (detail != null) {
        EveSsoUiStatus.Failed(
            messageRes = R.string.character_sso_error_provider,
            formatArgs = listOf(detail),
        )
    } else {
        EveSsoUiStatus.Failed(R.string.character_sso_error_provider_denied)
    }
}

private fun failureFromException(error: Exception): EveSsoUiStatus.Failed {
    val detail = oauthErrorDetail(error)?.trim()?.takeIf { it.isNotEmpty() }
    return if (detail != null) {
        EveSsoUiStatus.Failed(
            messageRes = R.string.character_sso_error_provider,
            formatArgs = listOf(detail),
        )
    } else {
        EveSsoUiStatus.Failed(R.string.character_sso_error_generic)
    }
}

private fun oauthErrorDetail(error: Throwable): String? {
    when (error) {
        is EveSsoHttpException -> {
            parseOAuthErrorDescription(error.errorBody)?.let { return it }
            error.message?.takeIf { it.isNotBlank() }?.let { return it }
        }
        else -> {
            error.message?.takeIf { it.isNotBlank() }?.let { return it }
        }
    }
    return null
}

private fun parseOAuthErrorDescription(rawBody: String): String? {
    if (rawBody.isBlank()) return null
    return runCatching {
        val dto = PyeriteJson.decodeFromString<EveOAuthErrorDto>(rawBody)
        dto.errorDescription?.takeIf { it.isNotBlank() }
            ?: dto.error?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
