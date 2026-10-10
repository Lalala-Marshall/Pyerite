package com.marshall.pyerite.personalPropertyModule.viewModel

import com.marshall.pyerite.personalPropertyModule.data.PersonalPropertyLoader
import com.marshall.pyerite.personalPropertyModule.model.PersonalPropertyCategory
import com.marshall.pyerite.personalPropertyModule.model.PersonalPropertyDecoratedRanking
import com.marshall.pyerite.personalPropertyModule.model.PersonalPropertySnapshot
import com.marshall.pyerite.personalPropertyModule.model.PersonalPropertySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

internal class PersonalPropertyRepository(
    private val loader: PersonalPropertyLoader,
) {
    private val snapshotByCharacterId = ConcurrentHashMap<Long, PersonalPropertySnapshot>()
    private val loadLocks = ConcurrentHashMap<Long, Mutex>()

    fun seedSummary(characterId: Long): PersonalPropertySummary {
        return snapshotByCharacterId[characterId]?.summary
            ?: PersonalPropertySummary.empty(characterId)
    }

    fun cachedSummary(characterId: Long): PersonalPropertySummary? =
        snapshotByCharacterId[characterId]?.summary

    /**
     * @param forceRefresh when true, always hits ESI and replaces the cache entry.
     * Otherwise returns the in-memory summary for [characterId] if present.
     */
    suspend fun loadSummary(
        characterId: Long,
        forceRefresh: Boolean = false,
    ): PersonalPropertySummary =
        loadSnapshot(characterId, forceRefresh = forceRefresh).summary

    suspend fun loadRanking(
        characterId: Long,
        category: PersonalPropertyCategory,
        forceRefresh: Boolean = false,
    ): PersonalPropertyDecoratedRanking = withContext(Dispatchers.IO) {
        val ranking = loadSnapshot(characterId, forceRefresh = forceRefresh)
            .rankingFor(category)
        loader.decorateRanking(ranking)
    }

    private suspend fun loadSnapshot(
        characterId: Long,
        forceRefresh: Boolean,
    ): PersonalPropertySnapshot {
        val lock = loadLocks.getOrPut(characterId) { Mutex() }
        return lock.withLock {
            if (!forceRefresh) {
                snapshotByCharacterId[characterId]?.let { return@withLock it }
            }
            val loaded = loader.load(characterId, forceRefresh = forceRefresh)
            snapshotByCharacterId[characterId] = loaded
            loaded
        }
    }
}
