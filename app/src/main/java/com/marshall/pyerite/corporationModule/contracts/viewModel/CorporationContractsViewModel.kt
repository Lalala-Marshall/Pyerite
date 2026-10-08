package com.marshall.pyerite.corporationModule.contracts.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContract
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractDisplayLimit
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractGroupBy
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractStatus
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractType
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractsAccessException
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractsFilter
import com.marshall.pyerite.corporationModule.contracts.model.sanitizeContractPriceInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CorporationContractsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CorporationContractsRepository,
) : ViewModel() {

    val characterId: Long = checkNotNull(savedStateHandle[NAV_ARG_CHARACTER_ID]) {
        "Missing $NAV_ARG_CHARACTER_ID"
    }

    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<CorporationContractsUiState> = _uiState.asStateFlow()

    init {
        if (repository.cachedContracts(characterId) == null) {
            load(forceRefresh = false)
        }
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        load(forceRefresh = true)
    }

    fun setGroupBy(groupBy: CorporationContractGroupBy) {
        _uiState.update { it.copy(filter = it.filter.copy(groupBy = groupBy)) }
    }

    fun setMinPriceText(raw: String) {
        _uiState.update {
            it.copy(filter = it.filter.copy(minPriceText = sanitizeContractPriceInput(raw)))
        }
    }

    fun setMaxPriceText(raw: String) {
        _uiState.update {
            it.copy(filter = it.filter.copy(maxPriceText = sanitizeContractPriceInput(raw)))
        }
    }

    fun toggleType(type: CorporationContractType) {
        _uiState.update { it.copy(filter = it.filter.toggleType(type)) }
    }

    fun toggleAllTypes() {
        _uiState.update { it.copy(filter = it.filter.toggleAllTypes()) }
    }

    fun toggleStatus(status: CorporationContractStatus) {
        _uiState.update { it.copy(filter = it.filter.toggleStatus(status)) }
    }

    fun toggleAllStatuses() {
        _uiState.update { it.copy(filter = it.filter.toggleAllStatuses()) }
    }

    fun setDisplayLimit(limit: CorporationContractDisplayLimit) {
        _uiState.update { it.copy(filter = it.filter.copy(displayLimit = limit)) }
    }

    private fun initialUiState(): CorporationContractsUiState {
        val cached = repository.cachedContracts(characterId)
        return if (cached != null) {
            CorporationContractsUiState(contracts = cached.contracts, isLoading = false)
        } else {
            CorporationContractsUiState()
        }
    }

    private fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, loadFailed = false, permissionDenied = false)
            }
            val result = runCatching {
                repository.loadContracts(characterId, forceRefresh = forceRefresh)
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { snapshot ->
                        current.copy(
                            contracts = snapshot.contracts,
                            isLoading = false,
                            loadFailed = false,
                            permissionDenied = false,
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isLoading = false,
                            loadFailed = error !is CorporationContractsAccessException,
                            permissionDenied = error is CorporationContractsAccessException,
                        )
                    },
                )
            }
        }
    }

    companion object {
        const val NAV_ARG_CHARACTER_ID = "characterId"
    }
}

internal data class CorporationContractsUiState(
    val contracts: List<CorporationContract> = emptyList(),
    val filter: CorporationContractsFilter = CorporationContractsFilter(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val permissionDenied: Boolean = false,
)
