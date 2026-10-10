package com.marshall.pyerite.contractListModule.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.contractsCommon.model.ContractListScope
import com.marshall.pyerite.contractsCommon.model.CorporationContract
import com.marshall.pyerite.contractsCommon.model.CorporationContractDisplayLimit
import com.marshall.pyerite.contractsCommon.model.CorporationContractGroupBy
import com.marshall.pyerite.contractsCommon.model.CorporationContractStatus
import com.marshall.pyerite.contractsCommon.model.CorporationContractType
import com.marshall.pyerite.contractsCommon.model.CorporationContractsAccessException
import com.marshall.pyerite.contractsCommon.model.CorporationContractsFilter
import com.marshall.pyerite.contractsCommon.model.ContractsListSettingsStore
import com.marshall.pyerite.contractsCommon.model.sanitizeContractPriceInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ContractListViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ContractListRepository,
    private val settingsStore: ContractsListSettingsStore,
) : ViewModel() {

    val characterId: Long = checkNotNull(savedStateHandle[NAV_ARG_CHARACTER_ID]) {
        "Missing $NAV_ARG_CHARACTER_ID"
    }

    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<ContractListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                _uiState.update { it.copy(filter = it.filter.withSettings(settings)) }
            }
        }
        if (repository.cachedContracts(characterId) == null) {
            load(forceRefresh = false)
        }
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        load(forceRefresh = true)
    }

    fun setScope(scope: ContractListScope) {
        _uiState.update { it.copy(scope = scope) }
    }

    fun setGroupBy(groupBy: CorporationContractGroupBy) {
        settingsStore.update { it.copy(groupBy = groupBy) }
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
        settingsStore.update { it.toggleType(type) }
    }

    fun toggleAllTypes() {
        settingsStore.update { it.toggleAllTypes() }
    }

    fun toggleStatus(status: CorporationContractStatus) {
        settingsStore.update { it.toggleStatus(status) }
    }

    fun toggleAllStatuses() {
        settingsStore.update { it.toggleAllStatuses() }
    }

    fun setDisplayLimit(limit: CorporationContractDisplayLimit) {
        settingsStore.update { it.copy(displayLimit = limit) }
    }

    private fun initialUiState(): ContractListUiState {
        val cached = repository.cachedContracts(characterId)
        val filter = CorporationContractsFilter().withSettings(settingsStore.settings.value)
        return if (cached != null) {
            ContractListUiState(
                contracts = cached.contracts,
                filter = filter,
                isLoading = false,
            )
        } else {
            ContractListUiState(filter = filter)
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

internal data class ContractListUiState(
    val contracts: List<CorporationContract> = emptyList(),
    val filter: CorporationContractsFilter = CorporationContractsFilter(),
    val scope: ContractListScope = ContractListScope.CHARACTER,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val permissionDenied: Boolean = false,
)
