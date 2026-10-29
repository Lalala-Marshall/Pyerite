package com.marshall.pyerite.agentsModule.model

internal data class AgentsFilter(
    val level: Int? = null,
    val security: AgentsSecurityBand? = null,
    val factionId: Int? = null,
    val corporationId: Long? = null,
    val divisionId: Int? = null,
    val agentType: Int? = null,
    val locatorOnly: Boolean = false,
    val spaceOnly: Boolean = false,
    val regionId: Int? = null,
    val solarSystemId: Int? = null,
)

internal data class AgentItem(
    val id: Int,
    val agentType: Int,
    val name: String,
    val level: Int,
    val corporationId: Long,
    val corporationName: String,
    val corporationIcon: String?,
    val divisionId: Int,
    val divisionName: String,
    val isLocator: Boolean,
    val isSpaceAgent: Boolean,
    val locationName: String,
    val security: Double?,
    val factionId: Int,
    val factionName: String,
    val factionIcon: String?,
    val regionId: Int?,
    val regionName: String,
    val regionZhName: String?,
    val regionEnName: String?,
    val solarSystemId: Int?,
    val systemName: String,
    val systemZhName: String?,
    val systemEnName: String?,
)

internal data class AgentChoice(
    val id: Long,
    val name: String,
)

internal data class AgentRegionOption(
    val id: Int,
    val name: String,
    val zhName: String?,
    val enName: String?,
)

internal data class AgentSystemOption(
    val id: Int,
    val regionId: Int?,
    val name: String,
    val zhName: String?,
    val enName: String?,
    val security: Double?,
)

internal data class AgentGroupRow(
    val id: Long,
    val name: String,
    val iconFileName: String?,
    val count: Int,
    val levelNumber: Int? = null,
)

internal data class AgentsResolvedPage(
    val title: String?,
    val level: AgentsResultLevel,
    val factionId: Int?,
    val corporationId: Long?,
    val divisionId: Int?,
    val agentLevel: Int?,
    val groups: List<AgentGroupRow>,
    val agents: List<AgentItem>,
)

internal data class AgentsUiState(
    val loading: Boolean = true,
    val filter: AgentsFilter = AgentsFilter(),
    val factions: List<AgentChoice> = emptyList(),
    val corporations: List<AgentChoice> = emptyList(),
    val divisions: List<AgentChoice> = emptyList(),
    val agentTypes: List<Int> = emptyList(),
    val regions: List<AgentRegionOption> = emptyList(),
    val systems: List<AgentSystemOption> = emptyList(),
    val selectedRegionName: String? = null,
    val selectedSystemName: String? = null,
    val results: List<AgentItem> = emptyList(),
    val hasSubmitted: Boolean = false,
)
