package com.marshall.pyerite.characterWalletJournalModule.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalAccessException
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalConfig
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalDateFormatter
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalDayRow
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalEntry
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalFilter
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalPeriodSummary
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalSnapshot
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalSummaryWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CharacterWalletJournalViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CharacterWalletJournalRepository,
) : ViewModel() {

    val characterId: Long = checkNotNull(savedStateHandle[NAV_ARG_CHARACTER_ID]) {
        "Missing $NAV_ARG_CHARACTER_ID"
    }

    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<CharacterWalletJournalUiState> = _uiState.asStateFlow()

    private var lastForceRefreshAtMs = 0L

    init {
        load(forceRefresh = false)
        viewModelScope.launch {
            repository.journalFilter.collect { filter ->
                _uiState.update { current -> present(current.copy(journalFilter = filter)) }
            }
        }
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        load(forceRefresh = true)
    }

    fun forceRefreshJournal() {
        val nowMs = System.currentTimeMillis()
        if (_uiState.value.isLoading) return
        if (nowMs - lastForceRefreshAtMs < CharacterWalletJournalConfig.FORCE_REFRESH_DEBOUNCE_MS) {
            return
        }
        lastForceRefreshAtMs = nowMs
        load(forceRefresh = true)
    }

    fun cycleSummaryWindow() {
        _uiState.update { current ->
            present(current.copy(summaryWindow = current.summaryWindow.next()))
        }
    }

    fun onJournalFilterChange(filter: CharacterWalletJournalFilter) {
        repository.setJournalFilter(filter)
    }

    private fun initialUiState(): CharacterWalletJournalUiState {
        val cached = repository.cachedJournal(characterId)
        return if (cached != null) {
            present(
                CharacterWalletJournalUiState(
                    journal = cached.journal,
                    journalFilter = repository.journalFilter.value,
                    isLoading = false,
                ),
            )
        } else {
            CharacterWalletJournalUiState(journalFilter = repository.journalFilter.value)
        }
    }

    private fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, loadFailed = false, permissionDenied = false)
            }
            val result = runCatching {
                repository.loadJournal(characterId, forceRefresh = forceRefresh)
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { snapshot -> stateFromSnapshot(snapshot, current) },
                    onFailure = { error ->
                        current.copy(
                            isLoading = false,
                            loadFailed = error !is CharacterWalletJournalAccessException,
                            permissionDenied = error is CharacterWalletJournalAccessException,
                        )
                    },
                )
            }
        }
    }

    private fun stateFromSnapshot(
        snapshot: CharacterWalletJournalSnapshot,
        current: CharacterWalletJournalUiState,
    ): CharacterWalletJournalUiState {
        return present(
            current.copy(
                journal = snapshot.journal,
                journalFilter = repository.journalFilter.value,
                isLoading = false,
                loadFailed = false,
                permissionDenied = false,
            ),
        )
    }

    private fun present(state: CharacterWalletJournalUiState): CharacterWalletJournalUiState {
        val visibleJournal = state.journal.filter { state.journalFilter.matches(it) }
        return state.copy(
            journalDays = journalDays(visibleJournal),
            periodSummary = summarizeJournal(visibleJournal, state.summaryWindow),
        )
    }

    companion object {
        const val NAV_ARG_CHARACTER_ID = "characterId"
    }
}

internal data class CharacterWalletJournalUiState(
    val summaryWindow: CharacterWalletJournalSummaryWindow =
        CharacterWalletJournalSummaryWindow.DAYS_30,
    val journal: List<CharacterWalletJournalEntry> = emptyList(),
    val journalDays: List<CharacterWalletJournalDayRow> = emptyList(),
    val periodSummary: CharacterWalletJournalPeriodSummary = CharacterWalletJournalPeriodSummary(
        income = CharacterWalletJournalConfig.ZERO_ISK,
        expense = CharacterWalletJournalConfig.ZERO_ISK,
        net = CharacterWalletJournalConfig.ZERO_ISK,
    ),
    val journalFilter: CharacterWalletJournalFilter = CharacterWalletJournalFilter.ALL,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val permissionDenied: Boolean = false,
)

internal fun summarizeJournal(
    entries: List<CharacterWalletJournalEntry>,
    window: CharacterWalletJournalSummaryWindow,
    nowMs: Long = System.currentTimeMillis(),
): CharacterWalletJournalPeriodSummary {
    var income = CharacterWalletJournalConfig.ZERO_ISK
    var expense = CharacterWalletJournalConfig.ZERO_ISK
    entries.forEach { entry ->
        if (!CharacterWalletJournalDateFormatter.isWithinRollingDays(
                entry.dateEpochMs,
                window.days,
                nowMs,
            )
        ) {
            return@forEach
        }
        when {
            entry.amount > CharacterWalletJournalConfig.ZERO_ISK -> income += entry.amount
            entry.amount < CharacterWalletJournalConfig.ZERO_ISK -> expense += -entry.amount
        }
    }
    return CharacterWalletJournalPeriodSummary(
        income = income,
        expense = expense,
        net = income - expense,
    )
}

internal fun journalDays(
    entries: List<CharacterWalletJournalEntry>,
): List<CharacterWalletJournalDayRow> {
    return entries.groupBy { it.dayKey }
        .map { (dayKey, dayEntries) ->
            val income = dayEntries.fold(CharacterWalletJournalConfig.ZERO_ISK) { acc, entry ->
                if (entry.amount > CharacterWalletJournalConfig.ZERO_ISK) acc + entry.amount else acc
            }
            val expense = dayEntries.fold(CharacterWalletJournalConfig.ZERO_ISK) { acc, entry ->
                if (entry.amount < CharacterWalletJournalConfig.ZERO_ISK) acc - entry.amount else acc
            }
            CharacterWalletJournalDayRow(
                dayKey = dayKey,
                entryCount = dayEntries.size,
                netIsk = income - expense,
            )
        }
        .sortedByDescending { it.dayKey }
}
