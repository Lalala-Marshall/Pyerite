package com.marshall.pyerite.agentsModule.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.agentsModule.data.AgentsRepository
import com.marshall.pyerite.agentsModule.data.applyAgentsFilter
import com.marshall.pyerite.agentsModule.data.corporationChoices
import com.marshall.pyerite.agentsModule.data.divisionChoices
import com.marshall.pyerite.agentsModule.data.factionChoices
import com.marshall.pyerite.agentsModule.data.orderedAgentTypes
import com.marshall.pyerite.agentsModule.data.regionOptions
import com.marshall.pyerite.agentsModule.data.systemOptions
import com.marshall.pyerite.agentsModule.data.toAgentItem
import com.marshall.pyerite.agentsModule.model.AgentsFilter
import com.marshall.pyerite.agentsModule.model.AgentsSecurityBand
import com.marshall.pyerite.agentsModule.model.AgentsUiState
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.sdeModule.room.npc.AgentQueryRow
import com.marshall.pyerite.sdeModule.update.SdeUpdateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AgentsViewModel(
    private val repository: AgentsRepository,
    private val localeController: LocaleController,
    private val sdeUpdateRepository: SdeUpdateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentsUiState())
    val uiState: StateFlow<AgentsUiState> = _uiState.asStateFlow()

    private var rows: List<AgentQueryRow> = emptyList()
    private var loading: Boolean = true
    private var draft: AgentsFilter = AgentsFilter()
    private var submitted: AgentsFilter? = null
    private var loadJob: Job? = null

    init {
        reload()
        viewModelScope.launch {
            localeController.databaseLanguage.collect {
                publish()
            }
        }
        viewModelScope.launch {
            sdeUpdateRepository.contentRefreshed.collect { reload() }
        }
    }

    fun setLevel(level: Int?) = updateDraft { copy(level = level) }

    fun setSecurity(security: AgentsSecurityBand?) = updateDraft { copy(security = security) }

    fun setFaction(factionId: Int?) = updateDraft { copy(factionId = factionId, corporationId = null) }

    fun setCorporation(corporationId: Long?) = updateDraft { copy(corporationId = corporationId) }

    fun setDivision(divisionId: Int?) = updateDraft { copy(divisionId = divisionId) }

    fun setAgentType(agentType: Int?) = updateDraft { copy(agentType = agentType) }

    fun setLocatorOnly(enabled: Boolean) = updateDraft { copy(locatorOnly = enabled) }

    fun setSpaceOnly(enabled: Boolean) = updateDraft { copy(spaceOnly = enabled) }

    fun setRegion(regionId: Int?) = updateDraft { copy(regionId = regionId, solarSystemId = null) }

    fun setSolarSystem(solarSystemId: Int?) = updateDraft { copy(solarSystemId = solarSystemId) }

    fun showAgents() {
        submitted = draft
        publish()
    }

    private fun updateDraft(transform: AgentsFilter.() -> AgentsFilter) {
        draft = draft.transform()
        publish()
    }

    private fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loading = true
            publish()
            rows = runCatching { repository.loadAgents() }.getOrDefault(emptyList())
            loading = false
            publish()
        }
    }

    private fun publish() {
        val language = localeController.contentLanguage
        val items = rows.map { it.toAgentItem(language) }
        val regions = regionOptions(items, language)
        val systems = systemOptions(items, language)
        val submittedFilter = submitted
        _uiState.value = AgentsUiState(
            loading = loading,
            filter = draft,
            factions = factionChoices(items, language),
            corporations = draft.factionId?.let { corporationChoices(items, it, language) }.orEmpty(),
            divisions = divisionChoices(items),
            agentTypes = orderedAgentTypes(items),
            regions = regions,
            systems = systems,
            selectedRegionName = regions.firstOrNull { it.id == draft.regionId }?.name,
            selectedSystemName = systems.firstOrNull { it.id == draft.solarSystemId }?.name,
            results = submittedFilter?.let { applyAgentsFilter(items, it) }.orEmpty(),
            hasSubmitted = submittedFilter != null,
        )
    }
}
