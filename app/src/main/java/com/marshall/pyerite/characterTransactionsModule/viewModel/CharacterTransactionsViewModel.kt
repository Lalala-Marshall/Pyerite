package com.marshall.pyerite.characterTransactionsModule.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.characterTransactionsModule.model.CharacterMarketTransaction
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionDayRow
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionSideFilter
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsAccessException
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsSnapshot
import com.marshall.pyerite.characterTransactionsModule.model.characterTransactionDays
import com.marshall.pyerite.characterTransactionsModule.model.visibleCharacterTransactions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CharacterTransactionsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CharacterTransactionsRepository,
) : ViewModel() {

    val characterId: Long = checkNotNull(savedStateHandle[NAV_ARG_CHARACTER_ID]) {
        "Missing $NAV_ARG_CHARACTER_ID"
    }

    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<CharacterTransactionsUiState> = _uiState.asStateFlow()

    init {
        load(forceRefresh = false)
        viewModelScope.launch {
            combine(
                repository.sideFilter,
                repository.mergeSimilarTransactions,
            ) { filter, mergeSimilar -> filter to mergeSimilar }
                .collect { (filter, mergeSimilar) ->
                    _uiState.update { current ->
                        present(
                            current.copy(
                                sideFilter = filter,
                                mergeSimilarTransactions = mergeSimilar,
                            ),
                        )
                    }
                }
        }
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        load(forceRefresh = true)
    }

    fun onSideFilterChange(filter: CharacterTransactionSideFilter) {
        repository.setSideFilter(filter)
    }

    fun onMergeSimilarTransactionsChange(enabled: Boolean) {
        repository.setMergeSimilarTransactions(enabled)
    }

    private fun initialUiState(): CharacterTransactionsUiState {
        val cached = repository.cached(characterId)
        return if (cached != null) {
            stateFromSnapshot(cached)
        } else {
            CharacterTransactionsUiState(
                sideFilter = repository.sideFilter.value,
                mergeSimilarTransactions = repository.mergeSimilarTransactions.value,
            )
        }
    }

    private fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, loadFailed = false, permissionDenied = false)
            }
            val result = runCatching {
                repository.load(characterId, forceRefresh = forceRefresh)
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { snapshot -> stateFromSnapshot(snapshot) },
                    onFailure = { error ->
                        val fallback = repository.cached(characterId)
                        val base = if (fallback != null) stateFromSnapshot(fallback) else current
                        base.copy(
                            isLoading = false,
                            loadFailed = error !is CharacterTransactionsAccessException,
                            permissionDenied = error is CharacterTransactionsAccessException,
                        )
                    },
                )
            }
        }
    }

    private fun stateFromSnapshot(
        snapshot: CharacterTransactionsSnapshot,
    ): CharacterTransactionsUiState {
        return present(
            CharacterTransactionsUiState(
                transactions = snapshot.transactions,
                sideFilter = repository.sideFilter.value,
                mergeSimilarTransactions = repository.mergeSimilarTransactions.value,
                isLoading = false,
                loadFailed = false,
                permissionDenied = false,
            ),
        )
    }

    private fun present(state: CharacterTransactionsUiState): CharacterTransactionsUiState {
        val visible = visibleCharacterTransactions(
            transactions = state.transactions,
            sideFilter = state.sideFilter,
            mergeSimilar = state.mergeSimilarTransactions,
        )
        return state.copy(days = characterTransactionDays(visible))
    }

    companion object {
        const val NAV_ARG_CHARACTER_ID = "characterId"
    }
}

internal data class CharacterTransactionsUiState(
    val transactions: List<CharacterMarketTransaction> = emptyList(),
    val days: List<CharacterTransactionDayRow> = emptyList(),
    val sideFilter: CharacterTransactionSideFilter = CharacterTransactionSideFilter.ALL,
    val mergeSimilarTransactions: Boolean = false,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val permissionDenied: Boolean = false,
)
