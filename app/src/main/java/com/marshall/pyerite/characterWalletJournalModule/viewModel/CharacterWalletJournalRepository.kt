package com.marshall.pyerite.characterWalletJournalModule.viewModel

import com.marshall.pyerite.characterWalletJournalModule.data.CharacterWalletJournalDiskCache
import com.marshall.pyerite.characterWalletJournalModule.data.CharacterWalletJournalLoader
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalEntry
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalFilter
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

internal class CharacterWalletJournalRepository(
    private val loader: CharacterWalletJournalLoader,
    private val diskCache: CharacterWalletJournalDiskCache,
) {
    private val journalByCharacterId = ConcurrentHashMap<Long, CharacterWalletJournalSnapshot>()
    private val locks = ConcurrentHashMap<Long, Mutex>()

    private val _journalFilter = MutableStateFlow(CharacterWalletJournalFilter.ALL)
    val journalFilter: StateFlow<CharacterWalletJournalFilter> = _journalFilter.asStateFlow()

    fun cachedJournal(characterId: Long): CharacterWalletJournalSnapshot? =
        journalByCharacterId[characterId]

    fun setJournalFilter(filter: CharacterWalletJournalFilter) {
        _journalFilter.value = filter
    }

    suspend fun loadJournal(
        characterId: Long,
        forceRefresh: Boolean = false,
    ): CharacterWalletJournalSnapshot = withContext(Dispatchers.IO) {
        val lock = locks.getOrPut(characterId) { Mutex() }
        lock.withLock {
            val nowEpochMs = System.currentTimeMillis()
            val stored = diskCache.read(characterId)
            if (!forceRefresh && stored != null && stored.isFresh(nowEpochMs)) {
                val snapshot = journalByCharacterId[characterId]
                    ?: snapshotOf(characterId, stored.entries)
                journalByCharacterId[characterId] = snapshot
                return@withLock snapshot
            }
            val loaded = loader.load(characterId)
            val merged = appendNewerJournal(
                stored = stored?.entries.orEmpty(),
                fetched = loaded.journal,
            )
            diskCache.write(
                characterId = characterId,
                savedAtEpochMs = nowEpochMs,
                entries = merged,
            )
            val snapshot = snapshotOf(characterId, merged)
            journalByCharacterId[characterId] = snapshot
            snapshot
        }
    }

    private fun snapshotOf(
        characterId: Long,
        journal: List<CharacterWalletJournalEntry>,
    ): CharacterWalletJournalSnapshot = CharacterWalletJournalSnapshot(
        characterId = characterId,
        journal = journal,
    )
}

/** Keep saved rows, and add fetched rows whose id is greater than the saved maximum. */
private fun appendNewerJournal(
    stored: List<CharacterWalletJournalEntry>,
    fetched: List<CharacterWalletJournalEntry>,
): List<CharacterWalletJournalEntry> {
    val maxId = stored.maxOfOrNull { it.id } ?: 0L
    val newer = fetched.filter { it.id > maxId }
    return (stored + newer).sortedWith(
        compareByDescending<CharacterWalletJournalEntry> { it.dateEpochMs }
            .thenByDescending { it.id },
    )
}
