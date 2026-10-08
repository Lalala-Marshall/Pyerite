package com.marshall.pyerite.corporationModule.assets.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetLocationKey
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetLocationKind
import com.marshall.pyerite.corporationModule.assets.navHost.CorporationAssetsNavArgs
import com.marshall.pyerite.corporationModule.assets.navHost.CorporationAssetsRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class CorporationAssetsViewModel(
    savedStateHandle: SavedStateHandle,
    repository: CorporationAssetsRepository,
) : ViewModel() {
    val characterId: Long = checkNotNull(savedStateHandle[CorporationAssetsNavArgs.CHARACTER_ID])
    private val session = CorporationAssetsSession(characterId, repository, viewModelScope)
    val uiState = session.state

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun refresh() = session.refresh()
}

internal class CorporationAssetLocationViewModel(
    savedStateHandle: SavedStateHandle,
    repository: CorporationAssetsRepository,
) : ViewModel() {
    val characterId: Long = checkNotNull(savedStateHandle[CorporationAssetsNavArgs.CHARACTER_ID])
    val locationKey: CorporationAssetLocationKey = CorporationAssetLocationKey(
        kind = CorporationAssetLocationKind.fromRoute(
            checkNotNull(savedStateHandle[CorporationAssetsNavArgs.LOCATION_KIND]),
        ),
        locationId = checkNotNull(savedStateHandle[CorporationAssetsNavArgs.LOCATION_ID]),
    )
    private val session = CorporationAssetsSession(characterId, repository, viewModelScope)
    val uiState = session.state

    fun refresh() = session.refresh()
}

internal class CorporationAssetFolderViewModel(
    savedStateHandle: SavedStateHandle,
    repository: CorporationAssetsRepository,
) : ViewModel() {
    val characterId: Long = checkNotNull(savedStateHandle[CorporationAssetsNavArgs.CHARACTER_ID])
    val locationKey: CorporationAssetLocationKey = CorporationAssetLocationKey(
        kind = CorporationAssetLocationKind.fromRoute(
            checkNotNull(savedStateHandle[CorporationAssetsNavArgs.LOCATION_KIND]),
        ),
        locationId = checkNotNull(savedStateHandle[CorporationAssetsNavArgs.LOCATION_ID]),
    )
    val routeFlag: String = checkNotNull(savedStateHandle[CorporationAssetsNavArgs.FLAG])
    private val session = CorporationAssetsSession(characterId, repository, viewModelScope)
    val uiState = session.state

    fun refresh() = session.refresh()
}

internal class CorporationAssetContainerViewModel(
    savedStateHandle: SavedStateHandle,
    repository: CorporationAssetsRepository,
) : ViewModel() {
    val characterId: Long = checkNotNull(savedStateHandle[CorporationAssetsNavArgs.CHARACTER_ID])
    val itemId: Long = CorporationAssetsRoute.Container.parseItemId(
        checkNotNull(savedStateHandle[CorporationAssetsNavArgs.ITEM_ID]),
    )
    private val session = CorporationAssetsSession(characterId, repository, viewModelScope)
    val uiState = session.state

    fun refresh() = session.refresh()
}
