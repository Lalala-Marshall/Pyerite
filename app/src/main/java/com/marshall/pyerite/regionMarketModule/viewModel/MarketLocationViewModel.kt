package com.marshall.pyerite.regionMarketModule.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.eveAuthModule.model.EveSessionIdentity
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.regionMarketModule.data.MarketRepository
import com.marshall.pyerite.regionMarketModule.data.MarketStructureStore
import com.marshall.pyerite.regionMarketModule.data.SelectedMarketStore
import com.marshall.pyerite.regionMarketModule.model.MarketPlaceName
import com.marshall.pyerite.regionMarketModule.model.MarketSelection
import com.marshall.pyerite.regionMarketModule.model.MarketStructureHit
import com.marshall.pyerite.regionMarketModule.model.MarketSystemOption
import com.marshall.pyerite.regionMarketModule.model.SavedMarketStructure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class MarketLocationUiState(
    val editing: Boolean = false,
    val pinned: List<MarketSelection> = emptyList(),
    val structures: List<SavedMarketStructure> = emptyList(),
    val majorSystems: List<MarketSystemOption> = emptyList(),
    val regionSections: List<MarketRegionSection> = emptyList(),
    val selected: MarketSelection = MarketSelection.default(),
    val query: String = "",
    val characters: List<EveSessionIdentity> = emptyList(),
    val structureHits: List<MarketStructureHit> = emptyList(),
    val searchingStructures: Boolean = false,
    val structureSearchDenied: Boolean = false,
    val structureMarketDenied: Boolean = false,
)

internal data class MarketRegionSection(
    val letter: String,
    val regions: List<MarketPlaceName>,
)

internal data class MarketSelectionLabel(
    val title: String,
    val subtitle: String?,
    val security: Double?,
    val iconFileName: String?,
)

internal class MarketLocationViewModel(
    private val repository: MarketRepository,
    private val selectionStore: SelectedMarketStore,
    private val structureStore: MarketStructureStore,
    private val tokenManager: EveTokenManager,
    private val localeController: LocaleController,
) : ViewModel() {
    private val regions = MutableStateFlow<List<MarketPlaceName>>(emptyList())
    private val systems = MutableStateFlow<List<MarketSystemOption>>(emptyList())
    private val structureSystemNames = MutableStateFlow<Map<Int, String>>(emptyMap())

    private val _ui = MutableStateFlow(MarketLocationUiState())
    val ui: StateFlow<MarketLocationUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            regions.value = repository.tradeRegions()
            systems.value = repository.majorSystems()
            rebuild()
        }
        viewModelScope.launch {
            selectionStore.selection.collect {
                _ui.value = _ui.value.copy(selected = it)
            }
        }
        viewModelScope.launch {
            selectionStore.pinned.collect {
                _ui.value = _ui.value.copy(pinned = it)
                rebuild()
            }
        }
        viewModelScope.launch {
            structureStore.structures.collect { saved ->
                val names = saved.map { it.systemId }.distinct().associateWith { systemId ->
                    val option = repository.systemOption(systemId)
                    option?.let { placeName(it.systemName, it.systemZhName, it.systemEnName) }.orEmpty()
                }
                structureSystemNames.value = names
                _ui.value = _ui.value.copy(structures = saved)
            }
        }
    }

    fun setEditing(editing: Boolean) {
        _ui.value = _ui.value.copy(editing = editing)
    }

    fun setQuery(query: String) {
        _ui.value = _ui.value.copy(query = query)
        rebuild()
    }

    fun select(selection: MarketSelection) {
        selectionStore.select(selection)
    }

    fun pin(selection: MarketSelection) {
        val current = selectionStore.pinned.value
        if (current.any { it.persistKey == selection.persistKey }) return
        selectionStore.setPinned(current + selection)
    }

    fun unpin(selection: MarketSelection) {
        selectionStore.setPinned(
            selectionStore.pinned.value.filterNot { it.persistKey == selection.persistKey },
        )
    }

    fun movePinned(from: Int, to: Int) {
        val current = selectionStore.pinned.value.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        val item = current.removeAt(from)
        current.add(to, item)
        selectionStore.setPinned(current)
    }

    fun placeName(name: String, zhName: String?, enName: String?): String =
        repository.displayPlaceName(name, zhName, enName)

    fun systemName(systemId: Int): String = structureSystemNames.value[systemId].orEmpty()

    fun label(selection: MarketSelection): MarketSelectionLabel = when (selection) {
        is MarketSelection.Region -> {
            val region = regions.value.firstOrNull { it.regionId == selection.regionId }
            MarketSelectionLabel(
                title = region?.let { placeName(it.name, it.zhName, it.enName) }
                    ?: selection.regionId.toString(),
                subtitle = null,
                security = null,
                iconFileName = null,
            )
        }
        is MarketSelection.System -> {
            val system = systems.value.firstOrNull { it.systemId == selection.systemId }
            MarketSelectionLabel(
                title = system?.let { placeName(it.systemName, it.systemZhName, it.systemEnName) }
                    ?: selection.systemId.toString(),
                subtitle = system?.let { placeName(it.regionName, it.regionZhName, it.regionEnName) },
                security = system?.security,
                iconFileName = null,
            )
        }
        is MarketSelection.Structure -> {
            val structure = structureStore.structures.value.firstOrNull { it.structureId == selection.structureId }
            MarketSelectionLabel(
                title = structure?.name ?: selection.structureId.toString(),
                subtitle = structure?.systemId?.let { structureSystemNames.value[it] },
                security = structure?.security,
                iconFileName = structure?.iconFileName,
            )
        }
    }

    fun refreshCharacters() {
        _ui.value = _ui.value.copy(characters = tokenManager.sessionIdentities())
    }

    fun searchStructures(characterId: Long, query: String) {
        val searchDenied = !repository.hasStructureSearchScope(characterId)
        val marketDenied = !repository.hasStructureMarketScope(characterId)
        if (searchDenied || marketDenied) {
            _ui.value = _ui.value.copy(
                structureSearchDenied = searchDenied,
                structureMarketDenied = marketDenied,
                structureHits = emptyList(),
            )
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                searchingStructures = true,
                structureSearchDenied = false,
                structureMarketDenied = false,
            )
            val hits = runCatching { repository.searchStructures(characterId, query) }.getOrDefault(emptyList())
            _ui.value = _ui.value.copy(searchingStructures = false, structureHits = hits)
        }
    }

    fun saveStructure(hit: MarketStructureHit, characterId: Long) {
        if (!repository.hasStructureMarketScope(characterId)) {
            _ui.value = _ui.value.copy(structureSearchDenied = true)
            return
        }
        repository.saveStructure(hit, characterId)
        _ui.value = _ui.value.copy(structureHits = emptyList())
    }

    fun clearStructureSearch() {
        _ui.value = _ui.value.copy(
            structureHits = emptyList(),
            structureSearchDenied = false,
            structureMarketDenied = false,
        )
    }

    private fun rebuild() {
        val language = localeController.contentLanguage
        val pinnedRegionIds = selectionStore.pinned.value.mapNotNull { place ->
            (place as? MarketSelection.Region)?.regionId
        }.toSet()
        val query = _ui.value.query.trim()
        val filtered = regions.value.filter { region ->
            region.regionId !in pinnedRegionIds && region.matches(query)
        }
        val sections = filtered
            .groupBy { region ->
                marketSectionLetter(repository.displayPlaceName(region.name, region.zhName, region.enName), language)
            }
            .map { (letter, items) ->
                MarketRegionSection(
                    letter = letter,
                    regions = items.sortedBy { region ->
                        repository.displayPlaceName(region.name, region.zhName, region.enName)
                    },
                )
            }
            .sortedBy { it.letter }
        _ui.value = _ui.value.copy(regionSections = sections, majorSystems = systems.value)
    }

    private fun MarketPlaceName.matches(query: String): Boolean {
        if (query.isEmpty()) return true
        val label = repository.displayPlaceName(name, zhName, enName)
        return label.contains(query, ignoreCase = true)
    }
}
