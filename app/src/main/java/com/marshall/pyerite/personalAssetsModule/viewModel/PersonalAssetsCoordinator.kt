package com.marshall.pyerite.personalAssetsModule.viewModel

import com.marshall.pyerite.charactersListModule.viewModel.CharacterRepository
import com.marshall.pyerite.personalAssetsModule.data.PersonalAssetsSettingsStore
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsAccessException
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsSettings
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsSnapshot
import com.marshall.pyerite.personalAssetsModule.model.combine
import com.marshall.pyerite.personalAssetsModule.model.withOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class PersonalAssetsLoadState(
    val snapshot: PersonalAssetsSnapshot? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val permissionDenied: Boolean = false,
    /** True while a selected character has no memory or disk snapshot yet. */
    val showLoadingIcon: Boolean = false,
)

/**
 * One shared tree for the personal-assets back stack.
 * Per-character snapshots stay cached; changing merge only recombines them.
 */
internal class PersonalAssetsCoordinator(
    private val repository: PersonalAssetsRepository,
    private val settingsStore: PersonalAssetsSettingsStore,
    private val characters: CharacterRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PersonalAssetsLoadState(isLoading = false))
    val state: StateFlow<PersonalAssetsLoadState> = _state.asStateFlow()
    val settings: StateFlow<PersonalAssetsSettings> = settingsStore.settings

    private var anchorCharacterId: Long? = null
    private var generation = 0

    fun bind(anchorCharacterId: Long) {
        val sameAnchor = this.anchorCharacterId == anchorCharacterId
        val inFlightOrReady = _state.value.isLoading || _state.value.snapshot != null
        if (sameAnchor && inFlightOrReady && !_state.value.permissionDenied) return
        this.anchorCharacterId = anchorCharacterId
        startLoad(forceRefresh = false)
    }

    fun refresh() {
        if (anchorCharacterId == null || _state.value.isLoading) return
        startLoad(forceRefresh = true)
    }

    fun applySettings(settings: PersonalAssetsSettings) {
        val changed = settings != settingsStore.current()
        settingsStore.save(settings)
        if (!changed || anchorCharacterId == null) return
        startLoad(forceRefresh = false)
    }

    private fun startLoad(forceRefresh: Boolean) {
        val anchor = anchorCharacterId ?: return
        val ticket = ++generation
        _state.update {
            it.copy(
                isLoading = true,
                loadFailed = false,
                permissionDenied = false,
                showLoadingIcon = false,
            )
        }
        scope.launch {
            val settings = settingsStore.current()
            val ids = settings.characterIds(anchor)
            val awaitingUncached = ids.any { characterId -> !repository.isCached(characterId) }
            if (ticket != generation) return@launch
            if (awaitingUncached) {
                _state.update { it.copy(showLoadingIcon = true) }
            }
            val loaded = ids.map { characterId ->
                characterId to runCatching {
                    repository.load(characterId, forceRefresh = forceRefresh)
                }
            }
            if (ticket != generation) return@launch
            val anchorError = loaded.first { it.first == anchor }.second.exceptionOrNull()
            if (anchorError is PersonalAssetsAccessException) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        loadFailed = false,
                        permissionDenied = true,
                        showLoadingIcon = false,
                    )
                }
                return@launch
            }
            if (anchorError != null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        loadFailed = true,
                        permissionDenied = false,
                        showLoadingIcon = false,
                    )
                }
                return@launch
            }
            val snapshots = loaded.mapNotNull { (characterId, result) ->
                val snapshot = result.getOrNull() ?: return@mapNotNull null
                if (!settings.aggregateCharacters) {
                    snapshot
                } else {
                    val name = characters.loggedInCharacters.value
                        .firstOrNull { it.characterId == characterId }
                        ?.name
                    snapshot.withOwner(characterId, name)
                }
            }
            _state.value = PersonalAssetsLoadState(
                snapshot = snapshots.combine(
                    mergeSameLocation = settings.aggregateCharacters && settings.mergeSameLocation,
                ),
                isLoading = false,
                showLoadingIcon = false,
            )
        }
    }
}
