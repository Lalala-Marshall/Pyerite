package com.marshall.pyerite.personalAssetsModule.viewModel

import com.marshall.pyerite.personalAssetsModule.data.PersonalAssetsDiskCache
import com.marshall.pyerite.personalAssetsModule.data.PersonalAssetsLoader
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

internal class PersonalAssetsRepository(
    private val loader: PersonalAssetsLoader,
    private val diskCache: PersonalAssetsDiskCache,
) {
    private val snapshots = ConcurrentHashMap<Long, MutableStateFlow<PersonalAssetsSnapshot?>>()
    private val locks = ConcurrentHashMap<Long, Mutex>()

    fun cached(characterId: Long): PersonalAssetsSnapshot? = snapshots[characterId]?.value

    /** True when a fresh memory or disk snapshot can be shown without calling ESI. */
    suspend fun isCached(characterId: Long): Boolean {
        if (cached(characterId) != null) return true
        return withContext(Dispatchers.IO) { diskCache.readIfFresh(characterId) != null }
    }

    suspend fun load(characterId: Long, forceRefresh: Boolean): PersonalAssetsSnapshot {
        val lock = locks.getOrPut(characterId) { Mutex() }
        return lock.withLock {
            if (!forceRefresh) {
                snapshots[characterId]?.value?.let { return@withLock it }
                val cached = withContext(Dispatchers.IO) { diskCache.readIfFresh(characterId) }
                if (cached != null) {
                    publish(characterId, cached)
                    return@withLock cached
                }
            }
            val loaded = loader.load(characterId, forceRefresh = forceRefresh)
            withContext(Dispatchers.IO) {
                runCatching { diskCache.write(characterId, loaded) }
            }
            publish(characterId, loaded)
            loaded
        }
    }

    private fun publish(characterId: Long, snapshot: PersonalAssetsSnapshot) {
        snapshots.getOrPut(characterId) { MutableStateFlow(null) }.value = snapshot
    }
}
