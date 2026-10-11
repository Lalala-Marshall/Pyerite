package com.marshall.pyerite.characterWalletJournalModule.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalAccessException
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalEntry
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CharacterWalletJournalDayViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CharacterWalletJournalRepository,
) : ViewModel() {

    val characterId: Long = checkNotNull(savedStateHandle[NAV_ARG_CHARACTER_ID]) {
        "Missing $NAV_ARG_CHARACTER_ID"
    }
    val dayKey: String = checkNotNull(savedStateHandle[NAV_ARG_DAY_KEY]) {
        "Missing $NAV_ARG_DAY_KEY"
    }

    private var sourceJournal: List<CharacterWalletJournalEntry> =
        repository.cachedJournal(characterId)?.journal.orEmpty()

    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<CharacterWalletJournalDayUiState> = _uiState.asStateFlow()

    init {
        load(forceRefresh = false)
        viewModelScope.launch {
            repository.journalFilter.collect { publishEntries() }
        }
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        load(forceRefresh = true)
    }

    private fun initialUiState(): CharacterWalletJournalDayUiState {
        val cached = repository.cachedJournal(characterId)
        return if (cached != null) {
            CharacterWalletJournalDayUiState(
                dayKey = dayKey,
                entries = visibleEntries(cached.journal, repository.journalFilter.value),
                isLoading = false,
            )
        } else {
            CharacterWalletJournalDayUiState(dayKey = dayKey)
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
                    onSuccess = { snapshot ->
                        sourceJournal = snapshot.journal
                        current.copy(
                            entries = visibleEntries(snapshot.journal, repository.journalFilter.value),
                            isLoading = false,
                            loadFailed = false,
                            permissionDenied = false,
                        )
                    },
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

    private fun publishEntries() {
        val filter = repository.journalFilter.value
        _uiState.update { it.copy(entries = visibleEntries(sourceJournal, filter)) }
    }

    private fun visibleEntries(
        journal: List<CharacterWalletJournalEntry>,
        filter: CharacterWalletJournalFilter,
    ): List<CharacterWalletJournalEntry> {
        return journal.filter { entry ->
            entry.dayKey == dayKey && filter.matches(entry)
        }
    }

    companion object {
        const val NAV_ARG_CHARACTER_ID = "characterId"
        const val NAV_ARG_DAY_KEY = "dayKey"
    }
}

internal data class CharacterWalletJournalDayUiState(
    val dayKey: String,
    val entries: List<CharacterWalletJournalEntry> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val permissionDenied: Boolean = false,
)
