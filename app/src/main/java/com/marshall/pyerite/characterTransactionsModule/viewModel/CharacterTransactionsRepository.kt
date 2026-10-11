package com.marshall.pyerite.characterTransactionsModule.viewModel

import com.marshall.pyerite.characterTransactionsModule.data.CharacterTransactionsDiskCache
import com.marshall.pyerite.characterTransactionsModule.data.CharacterTransactionsLoader
import com.marshall.pyerite.characterTransactionsModule.model.CharacterMarketTransaction
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionSideFilter
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

internal class CharacterTransactionsRepository(
    private val loader: CharacterTransactionsLoader,
    private val diskCache: CharacterTransactionsDiskCache,
) {
    private val snapshotByCharacterId = ConcurrentHashMap<Long, CharacterTransactionsSnapshot>()
    private val locks = ConcurrentHashMap<Long, Mutex>()

    private val _sideFilter = MutableStateFlow(CharacterTransactionSideFilter.ALL)
    val sideFilter: StateFlow<CharacterTransactionSideFilter> = _sideFilter.asStateFlow()

    private val _mergeSimilarTransactions = MutableStateFlow(false)
    val mergeSimilarTransactions: StateFlow<Boolean> = _mergeSimilarTransactions.asStateFlow()

    fun cached(characterId: Long): CharacterTransactionsSnapshot? = snapshotByCharacterId[characterId]

    fun setSideFilter(filter: CharacterTransactionSideFilter) {
        _sideFilter.value = filter
    }

    fun setMergeSimilarTransactions(enabled: Boolean) {
        _mergeSimilarTransactions.value = enabled
    }

    suspend fun load(
        characterId: Long,
        forceRefresh: Boolean = false,
    ): CharacterTransactionsSnapshot = withContext(Dispatchers.IO) {
        val lock = locks.getOrPut(characterId) { Mutex() }
        lock.withLock {
            val nowEpochMs = System.currentTimeMillis()
            val stored = diskCache.read(characterId)
            if (!forceRefresh && stored != null && stored.isFresh(nowEpochMs)) {
                val snapshot = snapshotByCharacterId[characterId]
                    ?: snapshotOf(characterId, stored.transactions)
                snapshotByCharacterId[characterId] = snapshot
                return@withLock snapshot
            }
            if (stored != null) {
                snapshotByCharacterId[characterId] = snapshotOf(characterId, stored.transactions)
            }
            val loaded = loader.load(characterId)
            val merged = unionTransactions(
                stored = stored?.transactions.orEmpty(),
                fetched = loaded.transactions,
            )
            diskCache.write(
                characterId = characterId,
                savedAtEpochMs = nowEpochMs,
                transactions = merged,
            )
            val snapshot = snapshotOf(characterId, merged)
            snapshotByCharacterId[characterId] = snapshot
            snapshot
        }
    }

    private fun snapshotOf(
        characterId: Long,
        transactions: List<CharacterMarketTransaction>,
    ): CharacterTransactionsSnapshot = CharacterTransactionsSnapshot(
        characterId = characterId,
        transactions = transactions,
    )
}

/** Keep every transaction id already saved, and add any older or newer row from this sync. */
private fun unionTransactions(
    stored: List<CharacterMarketTransaction>,
    fetched: List<CharacterMarketTransaction>,
): List<CharacterMarketTransaction> {
    val byId = LinkedHashMap<Long, CharacterMarketTransaction>()
    stored.forEach { transaction -> byId[transaction.transactionId] = transaction }
    fetched.forEach { transaction -> byId[transaction.transactionId] = transaction }
    return byId.values.sortedWith(
        compareByDescending<CharacterMarketTransaction> { it.dateEpochMs }
            .thenByDescending { it.transactionId },
    )
}
