package com.marshall.pyerite.characterIndustryModule.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.characterIndustryModule.data.CharacterIndustrySettingsStore
import com.marshall.pyerite.characterIndustryModule.data.CharacterIndustryTarget
import com.marshall.pyerite.characterIndustryModule.model.CharacterIndustrySettings
import com.marshall.pyerite.characterIndustryModule.model.CharacterIndustrySlotCapacity
import com.marshall.pyerite.charactersListModule.model.LoggedInCharacter
import com.marshall.pyerite.charactersListModule.viewModel.CharacterRepository
import com.marshall.pyerite.esiModule.data.portraitUrl
import com.marshall.pyerite.industryCommon.model.IndustryActivity
import com.marshall.pyerite.industryCommon.model.IndustryFilter
import com.marshall.pyerite.industryCommon.model.IndustryJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CharacterIndustryViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CharacterIndustryRepository,
    private val settingsStore: CharacterIndustrySettingsStore,
    private val characterRepository: CharacterRepository,
) : ViewModel() {

    val characterId: Long = checkNotNull(savedStateHandle[NAV_ARG_CHARACTER_ID]) {
        "Missing $NAV_ARG_CHARACTER_ID"
    }

    private val _uiState = MutableStateFlow(
        CharacterIndustryUiState(settings = settingsStore.current()),
    )
    val uiState: StateFlow<CharacterIndustryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            characterRepository.loggedInCharacters.collect { characters ->
                _uiState.update { it.copy(loggedInCharacters = characters) }
            }
        }
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            settingsStore.settings
                .map { settings -> settings.includedCharacterIds(characterId) }
                .distinctUntilChanged()
                .collect { ids -> load(ids, forceRefresh = false) }
        }
    }

    fun refresh() {
        val state = _uiState.value
        val ids = state.settings.includedCharacterIds(characterId)
        load(ids, forceRefresh = true)
    }

    fun updateSettings(settings: CharacterIndustrySettings) {
        settingsStore.save(settings)
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

    fun toggleSystem(solarSystemId: Long) {
        _uiState.update { it.copy(filter = it.filter.toggleSystem(solarSystemId)) }
    }

    fun toggleAllSystems(optionIds: Set<Long>) {
        _uiState.update { it.copy(filter = it.filter.toggleAllSystems(optionIds)) }
    }

    private fun load(ids: List<Long>, forceRefresh: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, loadFailed = false, permissionDenied = false)
            }
            if (ids.isEmpty()) {
                _uiState.update {
                    it.copy(
                        jobs = emptyList(),
                        slotsByCharacterId = emptyMap(),
                        isLoading = false,
                        loadFailed = false,
                        permissionDenied = false,
                    )
                }
                return@launch
            }
            val loggedIn = characterRepository.loggedInCharacters.value
            val targets = ids.map { id ->
                val character = loggedIn.find { it.characterId == id }
                CharacterIndustryTarget(
                    characterId = id,
                    name = character?.name.orEmpty(),
                    portraitUrl = character?.portraitUrl ?: portraitUrl(id),
                )
            }
            try {
                val batch = repository.load(targets, forceRefresh = forceRefresh)
                _uiState.update { current ->
                    if (batch.permissionDenied || batch.loadFailed) {
                        current.copy(
                            isLoading = false,
                            loadFailed = batch.loadFailed,
                            permissionDenied = batch.permissionDenied,
                        )
                    } else {
                        current.copy(
                            jobs = batch.jobs,
                            slotsByCharacterId = batch.slotsByCharacterId,
                            isLoading = false,
                            loadFailed = false,
                            permissionDenied = false,
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        loadFailed = true,
                        permissionDenied = false,
                    )
                }
            }
        }
    }

    companion object {
        const val NAV_ARG_CHARACTER_ID = "characterId"
    }
}

internal data class CharacterIndustryUiState(
    val jobs: List<IndustryJob> = emptyList(),
    val slotsByCharacterId: Map<Long, CharacterIndustrySlotCapacity?> = emptyMap(),
    val filter: IndustryFilter = IndustryFilter(),
    val settings: CharacterIndustrySettings = CharacterIndustrySettings(),
    val loggedInCharacters: List<LoggedInCharacter> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val permissionDenied: Boolean = false,
)
