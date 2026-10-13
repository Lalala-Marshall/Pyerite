package com.marshall.pyerite.corporationModule.industry.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.industryCommon.model.IndustryAccessException
import com.marshall.pyerite.industryCommon.model.IndustryActivity
import com.marshall.pyerite.industryCommon.model.IndustryFilter
import com.marshall.pyerite.industryCommon.model.IndustryJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CorporationIndustryViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CorporationIndustryRepository,
) : ViewModel() {

    val characterId: Long = checkNotNull(savedStateHandle[NAV_ARG_CHARACTER_ID]) {
        "Missing $NAV_ARG_CHARACTER_ID"
    }

    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<CorporationIndustryUiState> = _uiState.asStateFlow()

    init {
        if (repository.cachedJobs(characterId) == null) {
            load(forceRefresh = false)
        }
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        load(forceRefresh = true)
    }

    fun setHideClosed(hideClosed: Boolean) {
        _uiState.update { it.copy(filter = it.filter.copy(hideClosed = hideClosed)) }
    }

    fun toggleActivity(activity: IndustryActivity) {
        _uiState.update { it.copy(filter = it.filter.toggleActivity(activity)) }
    }

    fun toggleAllActivities() {
        _uiState.update { it.copy(filter = it.filter.toggleAllActivities()) }
    }

    fun toggleInstaller(installerId: Long) {
        _uiState.update { it.copy(filter = it.filter.toggleInstaller(installerId)) }
    }

    fun toggleAllInstallers(optionIds: Set<Long>) {
        _uiState.update { it.copy(filter = it.filter.toggleAllInstallers(optionIds)) }
    }

    fun toggleSystem(solarSystemId: Long) {
        _uiState.update { it.copy(filter = it.filter.toggleSystem(solarSystemId)) }
    }

    fun toggleAllSystems(optionIds: Set<Long>) {
        _uiState.update { it.copy(filter = it.filter.toggleAllSystems(optionIds)) }
    }

    private fun initialUiState(): CorporationIndustryUiState {
        val cached = repository.cachedJobs(characterId)
        return if (cached != null) {
            CorporationIndustryUiState(jobs = cached.jobs, isLoading = false)
        } else {
            CorporationIndustryUiState()
        }
    }

    private fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, loadFailed = false, permissionDenied = false)
            }
            val result = runCatching {
                repository.loadJobs(characterId, forceRefresh = forceRefresh)
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { snapshot ->
                        current.copy(
                            jobs = snapshot.jobs,
                            isLoading = false,
                            loadFailed = false,
                            permissionDenied = false,
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isLoading = false,
                            loadFailed = error !is IndustryAccessException,
                            permissionDenied = error is IndustryAccessException,
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

internal data class CorporationIndustryUiState(
    val jobs: List<IndustryJob> = emptyList(),
    val filter: IndustryFilter = IndustryFilter(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val permissionDenied: Boolean = false,
)
