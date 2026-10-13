package com.marshall.pyerite.characterIndustryModule.viewModel

import com.marshall.pyerite.characterIndustryModule.data.CharacterIndustryCharacterResult
import com.marshall.pyerite.characterIndustryModule.data.CharacterIndustryLoader
import com.marshall.pyerite.characterIndustryModule.data.CharacterIndustryTarget
import com.marshall.pyerite.characterIndustryModule.model.CharacterIndustrySlotCapacity
import com.marshall.pyerite.industryCommon.model.IndustryJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal data class CharacterIndustryBatch(
    val jobs: List<IndustryJob>,
    val slotsByCharacterId: Map<Long, CharacterIndustrySlotCapacity?>,
    val permissionDenied: Boolean,
    val loadFailed: Boolean,
)

internal class CharacterIndustryRepository(
    private val loader: CharacterIndustryLoader,
) {
    private val cache = ConcurrentHashMap<Long, CharacterIndustryCharacterResult>()
    private val locks = ConcurrentHashMap<Long, Mutex>()

    suspend fun load(
        targets: List<CharacterIndustryTarget>,
        forceRefresh: Boolean,
    ): CharacterIndustryBatch = coroutineScope {
        val results = targets.map { target ->
            async { loadOne(target, forceRefresh) }
        }.awaitAll()
        val successes = results.filter { result -> !result.accessDenied && !result.failed }
        val anySuccess = successes.isNotEmpty()
        CharacterIndustryBatch(
            jobs = successes.flatMap { it.jobs }.distinctBy { it.jobId },
            slotsByCharacterId = successes.associate { result ->
                result.characterId to result.slots
            },
            permissionDenied = !anySuccess && results.isNotEmpty() && results.all { it.accessDenied },
            loadFailed = !anySuccess && results.any { it.failed },
        )
    }

    private suspend fun loadOne(
        target: CharacterIndustryTarget,
        forceRefresh: Boolean,
    ): CharacterIndustryCharacterResult {
        val lock = locks.getOrPut(target.characterId) { Mutex() }
        return lock.withLock {
            if (!forceRefresh) {
                cache[target.characterId]?.let { return@withLock it }
            }
            val loaded = loader.loadCharacter(target)
            if (!loaded.failed && !loaded.accessDenied) {
                cache[target.characterId] = loaded
            }
            loaded
        }
    }
}