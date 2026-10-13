package com.marshall.pyerite.corporationModule.industry.viewModel

import com.marshall.pyerite.corporationModule.industry.data.CorporationIndustryLoader
import com.marshall.pyerite.industryCommon.model.IndustrySnapshot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal class CorporationIndustryRepository(
    private val loader: CorporationIndustryLoader,
) {
    private val jobsByCharacterId = ConcurrentHashMap<Long, IndustrySnapshot>()
    private val locks = ConcurrentHashMap<Long, Mutex>()

    fun cachedJobs(characterId: Long): IndustrySnapshot? =
        jobsByCharacterId[characterId]

    suspend fun loadJobs(
        characterId: Long,
        forceRefresh: Boolean = false,
    ): IndustrySnapshot {
        val lock = locks.getOrPut(characterId) { Mutex() }
        return lock.withLock {
            if (!forceRefresh) {
                jobsByCharacterId[characterId]?.let { return@withLock it }
            }
            val loaded = loader.loadJobs(characterId)
            jobsByCharacterId[characterId] = loaded
            loaded
        }
    }
}
