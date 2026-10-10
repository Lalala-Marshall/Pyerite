package com.marshall.pyerite.personalAssetsModule.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetLocationKey
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetLocationKind
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsConfig
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsSettings
import com.marshall.pyerite.personalAssetsModule.navHost.PersonalAssetsNavArgs
import com.marshall.pyerite.personalAssetsModule.navHost.PersonalAssetsRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class PersonalAssetsViewModel(
    savedStateHandle: SavedStateHandle,
    private val coordinator: PersonalAssetsCoordinator,
) : ViewModel() {
    val characterId: Long = checkNotNull(savedStateHandle[PersonalAssetsNavArgs.CHARACTER_ID])
    val uiState = coordinator.state
    val settings = coordinator.settings

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    init {
        coordinator.bind(characterId)
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun refresh() = coordinator.refresh()

    fun applySettings(settings: PersonalAssetsSettings) = coordinator.applySettings(settings)
}

internal class PersonalAssetLocationViewModel(
    savedStateHandle: SavedStateHandle,
    private val coordinator: PersonalAssetsCoordinator,
) : ViewModel() {
    val characterId: Long = checkNotNull(savedStateHandle[PersonalAssetsNavArgs.CHARACTER_ID])
    val locationKey: PersonalAssetLocationKey = PersonalAssetLocationKey(
        ownerCharacterId = PersonalAssetsRoute.parseOwnerKey(
            checkNotNull(savedStateHandle[PersonalAssetsNavArgs.OWNER_KEY]),
        ),
        kind = PersonalAssetLocationKind.fromRoute(
            checkNotNull(savedStateHandle[PersonalAssetsNavArgs.LOCATION_KIND]),
        ),
        locationId = checkNotNull(savedStateHandle[PersonalAssetsNavArgs.LOCATION_ID]),
    )
    val uiState = coordinator.state
    val settings = coordinator.settings

    init {
        coordinator.bind(characterId)
    }

    fun refresh() = coordinator.refresh()
}

internal class PersonalAssetContainerViewModel(
    savedStateHandle: SavedStateHandle,
    private val coordinator: PersonalAssetsCoordinator,
) : ViewModel() {
    val characterId: Long = checkNotNull(savedStateHandle[PersonalAssetsNavArgs.CHARACTER_ID])
    val itemId: Long = PersonalAssetsRoute.Container.parseItemId(
        checkNotNull(savedStateHandle[PersonalAssetsNavArgs.ITEM_ID]),
    )
    val uiState = coordinator.state

    init {
        coordinator.bind(characterId)
    }

    fun refresh() = coordinator.refresh()
}

internal fun PersonalAssetLocationKey.ownerRouteKey(): String =
    ownerCharacterId?.toString() ?: PersonalAssetsConfig.MERGED_OWNER_KEY
