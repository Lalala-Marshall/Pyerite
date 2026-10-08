package com.marshall.pyerite.corporationModule.assets.viewModel

import com.marshall.pyerite.corporationModule.assets.data.CorporationAssetsDiskCache
import com.marshall.pyerite.corporationModule.assets.data.CorporationAssetsLoader
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetsSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

internal class CorporationAssetsRepository(
    private val loader: CorporationAssetsLoader,
    private val diskCache: CorporationAssetsDiskCache,
) {
    private val snapshots = ConcurrentHashMap<Long, MutableStateFlow<CorporationAssetsSnapshot?>>()
    private val locks = ConcurrentHashMap<Long, Mutex>()

    fun cached(characterId: Long): CorporationAssetsSnapshot? = snapshots[characterId]?.value

    fun snapshots(characterId: Long): StateFlow<CorporationAssetsSnapshot?> =
        snapshots.getOrPut(characterId) { MutableStateFlow(null) }

    suspend fun load(characterId: Long, forceRefresh: Boolean): CorporationAssetsSnapshot {
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
            val loaded = loader.load(characterId)
            withContext(Dispatchers.IO) {
                runCatching { diskCache.write(characterId, loaded) }
            }
            publish(characterId, loaded)
            loaded
        }
    }

    private fun publish(characterId: Long, snapshot: CorporationAssetsSnapshot) {
        snapshots.getOrPut(characterId) { MutableStateFlow(null) }.value = snapshot
    }
}
