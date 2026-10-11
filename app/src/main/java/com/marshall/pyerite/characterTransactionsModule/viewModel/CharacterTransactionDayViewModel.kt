package com.marshall.pyerite.characterTransactionsModule.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.characterTransactionsModule.model.CharacterMarketTransaction
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionSideFilter
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsAccessException
import com.marshall.pyerite.characterTransactionsModule.model.visibleCharacterTransactions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CharacterTransactionDayViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CharacterTransactionsRepository,
) : ViewModel() {

    val characterId: Long = checkNotNull(savedStateHandle[NAV_ARG_CHARACTER_ID]) {
        "Missing $NAV_ARG_CHARACTER_ID"
    }
    val dayKey: String = checkNotNull(savedStateHandle[NAV_ARG_DAY_KEY]) {
        "Missing $NAV_ARG_DAY_KEY"
    }

    private var sourceTransactions: List<CharacterMarketTransaction> =
        repository.cached(characterId)?.transactions.orEmpty()

    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<CharacterTransactionDayUiState> = _uiState.asStateFlow()

    init {
        load(forceRefresh = false)
        viewModelScope.launch {
            combine(
                repository.sideFilter,
                repository.mergeSimilarTransactions,
            ) { filter, mergeSimilar -> filter to mergeSimilar }
                .collect { (filter, mergeSimilar) ->
                    publishEntries(filter, mergeSimilar)
                }
        }
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        load(forceRefresh = true)
    }

    private fun initialUiState(): CharacterTransactionDayUiState {
        val cached = repository.cached(characterId)
        return if (cached != null) {
            CharacterTransactionDayUiState(
                dayKey = dayKey,
                entries = visibleEntries(cached.transactions),
                isLoading = false,
            )
        } else {
            CharacterTransactionDayUiState(dayKey = dayKey)
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
                    onSuccess = { snapshot ->
                        sourceTransactions = snapshot.transactions
                        current.copy(
                            entries = visibleEntries(
                                transactions = snapshot.transactions,
                                sideFilter = repository.sideFilter.value,
                                mergeSimilar = repository.mergeSimilarTransactions.value,
                            ),
                            isLoading = false,
                            loadFailed = false,
                            permissionDenied = false,
                        )
                    },
                    onFailure = { error ->
                        val fallback = repository.cached(characterId)?.transactions
                        if (fallback != null) sourceTransactions = fallback
                        current.copy(
                            entries = if (fallback != null) {
                                visibleEntries(fallback)
                            } else {
                                current.entries
                            },
                            isLoading = false,
                            loadFailed = error !is CharacterTransactionsAccessException,
                            permissionDenied = error is CharacterTransactionsAccessException,
                        )
                    },
                )
            }
        }
    }

    private fun publishEntries(
        sideFilter: CharacterTransactionSideFilter,
        mergeSimilar: Boolean,
    ) {
        _uiState.update {
            it.copy(
                entries = visibleEntries(
                    transactions = sourceTransactions,
                    sideFilter = sideFilter,
                    mergeSimilar = mergeSimilar,
                ),
            )
        }
    }

    private fun visibleEntries(
        transactions: List<CharacterMarketTransaction>,
        sideFilter: CharacterTransactionSideFilter = repository.sideFilter.value,
        mergeSimilar: Boolean = repository.mergeSimilarTransactions.value,
    ): List<CharacterMarketTransaction> = visibleCharacterTransactions(
        transactions = transactions,
        sideFilter = sideFilter,
        mergeSimilar = mergeSimilar,
        dayKey = dayKey,
    )

    companion object {
        const val NAV_ARG_CHARACTER_ID = "characterId"
        const val NAV_ARG_DAY_KEY = "dayKey"
    }
}

internal data class CharacterTransactionDayUiState(
    val dayKey: String,
    val entries: List<CharacterMarketTransaction> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val permissionDenied: Boolean = false,
)
