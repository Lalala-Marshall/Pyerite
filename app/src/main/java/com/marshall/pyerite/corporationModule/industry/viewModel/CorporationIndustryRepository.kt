package com.marshall.pyerite.corporationModule.industry.viewModel

import com.marshall.pyerite.corporationModule.industry.data.CorporationIndustryLoader
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustrySnapshot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal class CorporationIndustryRepository(
    private val loader: CorporationIndustryLoader,
) {
    private val jobsByCharacterId = ConcurrentHashMap<Long, CorporationIndustrySnapshot>()
    private val locks = ConcurrentHashMap<Long, Mutex>()

    fun cachedJobs(characterId: Long): CorporationIndustrySnapshot? =
        jobsByCharacterId[characterId]

    suspend fun loadJobs(
        characterId: Long,
        forceRefresh: Boolean = false,
    ): CorporationIndustrySnapshot {
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
