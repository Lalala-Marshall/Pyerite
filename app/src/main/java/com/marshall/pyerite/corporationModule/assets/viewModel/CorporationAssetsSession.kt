package com.marshall.pyerite.corporationModule.assets.viewModel

import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetsAccessException
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetsSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class CorporationAssetsLoadState(
    val snapshot: CorporationAssetsSnapshot? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val permissionDenied: Boolean = false,
)

internal class CorporationAssetsSession(
    private val characterId: Long,
    private val repository: CorporationAssetsRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(
        CorporationAssetsLoadState(
            snapshot = repository.cached(characterId),
            isLoading = repository.cached(characterId) == null,
        ),
    )
    val state: StateFlow<CorporationAssetsLoadState> = _state.asStateFlow()

    init {
        scope.launch {
            repository.snapshots(characterId).collect { snapshot ->
                if (snapshot != null) {
                    _state.update { it.copy(snapshot = snapshot) }
                }
            }
        }
        if (repository.cached(characterId) == null) {
            load(forceRefresh = false)
        }
    }

    fun refresh() {
        if (_state.value.isLoading) return
        load(forceRefresh = true)
    }

    private fun load(forceRefresh: Boolean) {
        scope.launch {
            _state.update {
                it.copy(isLoading = true, loadFailed = false, permissionDenied = false)
            }
            val result = runCatching { repository.load(characterId, forceRefresh = forceRefresh) }
            _state.update { current ->
                result.fold(
                    onSuccess = { snapshot ->
                        current.copy(
                            snapshot = snapshot,
                            isLoading = false,
                            loadFailed = false,
                            permissionDenied = false,
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isLoading = false,
                            loadFailed = error !is CorporationAssetsAccessException,
                            permissionDenied = error is CorporationAssetsAccessException,
                        )
                    },
                )
            }
        }
    }
}
