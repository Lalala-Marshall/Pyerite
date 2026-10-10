package com.marshall.pyerite.contractListModule.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.contractsCommon.model.CorporationContractDetailUiState
import com.marshall.pyerite.contractsCommon.model.CorporationContractsAccessException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ContractListDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ContractListRepository,
) : ViewModel() {

    internal val characterId: Long = checkNotNull(
        savedStateHandle[ContractListViewModel.NAV_ARG_CHARACTER_ID],
    ) {
        "Missing ${ContractListViewModel.NAV_ARG_CHARACTER_ID}"
    }

    private val contractId: Long = checkNotNull(savedStateHandle[NAV_ARG_CONTRACT_ID]) {
        "Missing $NAV_ARG_CONTRACT_ID"
    }

    private val _uiState = MutableStateFlow(CorporationContractDetailUiState())
    val uiState: StateFlow<CorporationContractDetailUiState> = _uiState.asStateFlow()

    init {
        load(forceRefresh = false)
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        load(forceRefresh = true)
    }

    private fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, loadFailed = false, permissionDenied = false, missing = false)
            }
            val result = runCatching {
                repository.loadContractDetail(
                    characterId = characterId,
                    contractId = contractId,
                    forceRefresh = forceRefresh,
                )
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { detail ->
                        current.copy(
                            detail = detail,
                            isLoading = false,
                            loadFailed = false,
                            permissionDenied = false,
                            missing = detail == null,
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isLoading = false,
                            loadFailed = error !is CorporationContractsAccessException,
                            permissionDenied = error is CorporationContractsAccessException,
                            missing = false,
                        )
                    },
                )
            }
        }
    }

    companion object {
        const val NAV_ARG_CONTRACT_ID = "contractId"
    }
}
