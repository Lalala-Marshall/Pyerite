package com.marshall.pyerite.esiModule.data

import com.marshall.pyerite.esiModule.api.EsiCharacterApi
import com.marshall.pyerite.esiModule.model.CharacterAssetListConfig
import com.marshall.pyerite.esiModule.model.EsiCharacterAssetDto
import com.marshall.pyerite.esiModule.model.EsiHttpStatus
import com.marshall.pyerite.esiModule.model.EsiPagedQuery
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.util.concurrent.ConcurrentHashMap

internal sealed interface CharacterAssetListResult {
    data class Ready(val assets: List<EsiCharacterAssetDto>) : CharacterAssetListResult
    data object Forbidden : CharacterAssetListResult
    data object Failed : CharacterAssetListResult
}

/**
 * In-memory pages of `GET /characters/{id}/assets`.
 * Personal property and personal assets share one fetch per character until a force refresh.
 */
internal class CharacterAssetListCache(
    private val tokenManager: EveTokenManager,
    private val characterApi: EsiCharacterApi,
) {
    private val assetsByCharacterId = ConcurrentHashMap<Long, List<EsiCharacterAssetDto>>()
    private val locks = ConcurrentHashMap<Long, Mutex>()

    suspend fun load(characterId: Long, forceRefresh: Boolean): CharacterAssetListResult {
        val lock = locks.getOrPut(characterId) { Mutex() }
        return lock.withLock {
            if (!forceRefresh) {
                assetsByCharacterId[characterId]?.let { return@withLock CharacterAssetListResult.Ready(it) }
            }
            if (EveSsoScope.ASSETS_READ !in tokenManager.grantedScopes(characterId)) {
                assetsByCharacterId.remove(characterId)
                return@withLock CharacterAssetListResult.Forbidden
            }
            val fetched = runCatching { fetchAll(characterId) }
            fetched.fold(
                onSuccess = { assets ->
                    assetsByCharacterId[characterId] = assets
                    CharacterAssetListResult.Ready(assets)
                },
                onFailure = { error ->
                    assetsByCharacterId.remove(characterId)
                    if (error is CharacterAssetListForbiddenException) {
                        CharacterAssetListResult.Forbidden
                    } else {
                        CharacterAssetListResult.Failed
                    }
                },
            )
        }
    }

    private suspend fun fetchAll(characterId: Long): List<EsiCharacterAssetDto> {
        val byItemId = LinkedHashMap<Long, EsiCharacterAssetDto>()
        var page = CharacterAssetListConfig.FIRST_PAGE
        var totalPages = CharacterAssetListConfig.FIRST_PAGE
        while (page <= totalPages && page <= CharacterAssetListConfig.MAX_PAGES) {
            val response = tokenManager.executeWithAuthRetry(characterId) { auth ->
                val result = characterApi.fetchAssets(characterId, auth, page)
                if (result.code() == EsiHttpStatus.UNAUTHORIZED) throw HttpException(result)
                result
            }
            if (!response.isSuccessful) {
                if (page > CharacterAssetListConfig.FIRST_PAGE &&
                    response.code() == EsiHttpStatus.NOT_FOUND
                ) {
                    break
                }
                if (response.code() == EsiHttpStatus.FORBIDDEN) {
                    throw CharacterAssetListForbiddenException()
                }
                throw HttpException(response)
            }
            val chunk = response.body().orEmpty()
            chunk.forEach { asset -> byItemId[asset.itemId] = asset }
            val headerPages = response.headers()[EsiPagedQuery.PAGES_HEADER]?.toIntOrNull()
            totalPages = when {
                headerPages != null -> headerPages
                chunk.size < CharacterAssetListConfig.PAGE_SIZE -> page
                else -> page + 1
            }
            page++
        }
        return byItemId.values.toList()
    }
}

private class CharacterAssetListForbiddenException : Exception()
