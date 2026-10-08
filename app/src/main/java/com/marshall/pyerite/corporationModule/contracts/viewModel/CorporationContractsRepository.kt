package com.marshall.pyerite.corporationModule.contracts.viewModel

import com.marshall.pyerite.corporationModule.contracts.data.CorporationContractsLoader
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractDetail
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractsSnapshot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal class CorporationContractsRepository(
    private val loader: CorporationContractsLoader,
) {
    private val contractsByCharacterId = ConcurrentHashMap<Long, CorporationContractsSnapshot>()
    private val locks = ConcurrentHashMap<Long, Mutex>()

    fun cachedContracts(characterId: Long): CorporationContractsSnapshot? =
        contractsByCharacterId[characterId]

    suspend fun loadContracts(
        characterId: Long,
        forceRefresh: Boolean = false,
    ): CorporationContractsSnapshot {
        val lock = locks.getOrPut(characterId) { Mutex() }
        return lock.withLock {
            if (!forceRefresh) {
                contractsByCharacterId[characterId]?.let { return@withLock it }
            }
            val loaded = loader.loadContracts(characterId)
            contractsByCharacterId[characterId] = loaded
            loaded
        }
    }

    suspend fun loadContractDetail(
        characterId: Long,
        contractId: Long,
        forceRefresh: Boolean,
    ): CorporationContractDetail? {
        val snapshot = loadContracts(characterId, forceRefresh)
        val contract = snapshot.contracts.find { it.contractId == contractId } ?: return null
        return loader.loadContractDetail(characterId, contract)
    }
}
