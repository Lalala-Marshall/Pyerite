package com.marshall.pyerite.agentsModule.data

import com.marshall.pyerite.agentsModule.model.AgentChoice
import com.marshall.pyerite.agentsModule.model.AgentGroupRow
import com.marshall.pyerite.agentsModule.model.AgentItem
import com.marshall.pyerite.agentsModule.model.AgentRegionOption
import com.marshall.pyerite.agentsModule.model.AgentSystemOption
import com.marshall.pyerite.agentsModule.model.AgentsConfig
import com.marshall.pyerite.agentsModule.model.AgentsFilter
import com.marshall.pyerite.agentsModule.model.AgentsResolvedPage
import com.marshall.pyerite.agentsModule.model.AgentsResultLevel
import com.marshall.pyerite.agentsModule.model.matches
import com.marshall.pyerite.localization.ContentLanguage
import com.marshall.pyerite.localization.localizedName
import com.marshall.pyerite.sdeModule.room.npc.AgentQueryRow
import java.text.Collator
import java.util.Locale

internal fun AgentQueryRow.toAgentItem(language: ContentLanguage): AgentItem {
    val systemName = localizedName(systemZhName, systemEnName, systemName, language)
    val regionName = localizedName(regionZhName, regionEnName, this.regionName, language)
    val factionName = localizedName(factionZhName, factionEnName, this.factionName, language)
    val corporationName = localizedName(corporationZhName, corporationEnName, this.corporationName, language)
    val spaceAgent = solarSystemId != null
    val station = stationName?.takeIf { it.isNotBlank() }
    val locationName = if (spaceAgent) systemName else station ?: systemName
    return AgentItem(
        id = agentId,
        agentType = agentType,
        name = agentName,
        level = level,
        corporationId = corporationId,
        corporationName = corporationName,
        corporationIcon = corporationIcon,
        divisionId = divisionId,
        divisionName = divisionName,
        isLocator = isLocator == AgentsConfig.LOCATOR_FLAG,
        isSpaceAgent = spaceAgent,
        locationName = locationName,
        security = systemSecurity,
        factionId = factionId,
        factionName = factionName,
        factionIcon = factionIcon,
        regionId = regionId,
        regionName = regionName,
        regionZhName = regionZhName,
        regionEnName = regionEnName,
        solarSystemId = solarSystemId ?: stationSystemId,
        systemName = systemName,
        systemZhName = systemZhName,
        systemEnName = systemEnName,
    )
}

internal fun applyAgentsFilter(agents: List<AgentItem>, filter: AgentsFilter): List<AgentItem> =
    agents.filter { agent -> agent.matches(filter) }
        .sortedWith(compareBy<AgentItem> { it.locationName }.thenBy { it.name })

private fun AgentItem.matches(filter: AgentsFilter): Boolean {
    if (filter.level != null && level != filter.level) return false
    if (filter.security != null && !filter.security.matches(security)) return false
    if (filter.factionId != null && factionId != filter.factionId) return false
    if (filter.corporationId != null && corporationId != filter.corporationId) return false
    if (filter.divisionId != null && divisionId != filter.divisionId) return false
    if (filter.agentType != null && agentType != filter.agentType) return false
    if (filter.locatorOnly && !isLocator) return false
    if (filter.spaceOnly && !isSpaceAgent) return false
    if (filter.regionId != null && regionId != filter.regionId) return false
    if (filter.solarSystemId != null && solarSystemId != filter.solarSystemId) return false
    return true
}

internal fun factionChoices(agents: List<AgentItem>, language: ContentLanguage): List<AgentChoice> =
    agents.distinctBy { it.factionId }
        .map { AgentChoice(it.factionId.toLong(), it.factionName) }
        .sortedWith(nameOrder(language))

internal fun corporationChoices(
    agents: List<AgentItem>,
    factionId: Int,
    language: ContentLanguage,
): List<AgentChoice> =
    agents.filter { it.factionId == factionId }
        .distinctBy { it.corporationId }
        .map { AgentChoice(it.corporationId, it.corporationName) }
        .sortedWith(nameOrder(language))

internal fun divisionChoices(agents: List<AgentItem>): List<AgentChoice> {
    val present = agents.distinctBy { it.divisionId }
        .associate { it.divisionId to it.divisionName }
    val main = AgentsConfig.MAIN_DIVISION_IDS.mapNotNull { id ->
        present[id]?.let { AgentChoice(id.toLong(), it) }
    }
    val rest = present
        .filterKeys { it !in AgentsConfig.MAIN_DIVISION_IDS }
        .toSortedMap()
        .map { (id, name) -> AgentChoice(id.toLong(), name) }
    return main + rest
}

internal fun orderedAgentTypes(agents: List<AgentItem>): List<Int> {
    val present = agents.map { it.agentType }.toSet()
    val main = AgentsConfig.MAIN_AGENT_TYPE_IDS.filter { it in present }
    val secondary = AgentsConfig.SECONDARY_AGENT_TYPE_IDS.filter { it in present }
    val rest = present
        .filter { it !in AgentsConfig.MAIN_AGENT_TYPE_IDS && it !in AgentsConfig.SECONDARY_AGENT_TYPE_IDS }
        .sorted()
    return main + secondary + rest
}

internal fun regionOptions(agents: List<AgentItem>, language: ContentLanguage): List<AgentRegionOption> =
    agents.mapNotNull { agent ->
        val id = agent.regionId ?: return@mapNotNull null
        AgentRegionOption(
            id = id,
            name = agent.regionName,
            zhName = agent.regionZhName,
            enName = agent.regionEnName,
        )
    }
        .distinctBy { it.id }
        .sortedWith(compareBy(collator(language)) { it.name })

internal fun systemOptions(agents: List<AgentItem>, language: ContentLanguage): List<AgentSystemOption> =
    agents.mapNotNull { agent ->
        val id = agent.solarSystemId ?: return@mapNotNull null
        AgentSystemOption(
            id = id,
            regionId = agent.regionId,
            name = agent.systemName,
            zhName = agent.systemZhName,
            enName = agent.systemEnName,
            security = agent.security,
        )
    }
        .distinctBy { it.id }
        .sortedWith(compareBy(collator(language)) { it.name })

internal fun resolveAgentsPage(
    agents: List<AgentItem>,
    factionId: Int?,
    corporationId: Long?,
    divisionId: Int?,
    agentLevel: Int?,
    levelName: (Int) -> String,
    language: ContentLanguage,
): AgentsResolvedPage {
    var pool = agents
    var level = AgentsResultLevel.FACTION
    var title: String? = null
    var resolvedFaction = factionId
    var resolvedCorporation = corporationId
    var resolvedDivision = divisionId
    var resolvedLevel = agentLevel

    if (factionId != null) {
        pool = pool.filter { it.factionId == factionId }
        title = pool.firstOrNull()?.factionName
        level = AgentsResultLevel.CORPORATION
    }
    if (corporationId != null) {
        pool = pool.filter { it.corporationId == corporationId }
        title = pool.firstOrNull()?.corporationName ?: title
        level = AgentsResultLevel.DIVISION
    }
    if (divisionId != null) {
        pool = pool.filter { it.divisionId == divisionId }
        title = pool.firstOrNull()?.divisionName ?: title
        level = AgentsResultLevel.LEVEL
    }
    if (agentLevel != null) {
        pool = pool.filter { it.level == agentLevel }
        title = levelName(agentLevel)
        level = AgentsResultLevel.AGENT
    }

    while (level != AgentsResultLevel.AGENT) {
        val groups = groupsFor(level, pool, levelName, language)
        if (groups.size != 1) break
        val only = groups.first()
        title = only.name
        when (level) {
            AgentsResultLevel.FACTION -> {
                resolvedFaction = only.id.toInt()
                pool = pool.filter { it.factionId == resolvedFaction }
            }
            AgentsResultLevel.CORPORATION -> {
                resolvedCorporation = only.id
                pool = pool.filter { it.corporationId == resolvedCorporation }
            }
            AgentsResultLevel.DIVISION -> {
                resolvedDivision = only.id.toInt()
                pool = pool.filter { it.divisionId == resolvedDivision }
            }
            AgentsResultLevel.LEVEL -> {
                resolvedLevel = only.id.toInt()
                pool = pool.filter { it.level == resolvedLevel }
            }
        }
        level = level.next()
    }

    val groups = if (level == AgentsResultLevel.AGENT) {
        emptyList()
    } else {
        groupsFor(level, pool, levelName, language)
    }
    val shown = if (level == AgentsResultLevel.AGENT) {
        pool.sortedWith(compareBy<AgentItem> { it.locationName }.thenBy { it.name })
    } else {
        emptyList()
    }
    return AgentsResolvedPage(
        title = title,
        level = level,
        factionId = resolvedFaction,
        corporationId = resolvedCorporation,
        divisionId = resolvedDivision,
        agentLevel = resolvedLevel,
        groups = groups,
        agents = shown,
    )
}

private fun groupsFor(
    level: AgentsResultLevel,
    agents: List<AgentItem>,
    levelName: (Int) -> String,
    language: ContentLanguage,
): List<AgentGroupRow> = when (level) {
    AgentsResultLevel.FACTION -> agents.groupBy { it.factionId }.map { (id, rows) ->
        val first = rows.first()
        AgentGroupRow(id.toLong(), first.factionName, first.factionIcon, rows.size)
    }.sortedWith(compareBy(collator(language)) { it.name })
    AgentsResultLevel.CORPORATION -> agents.groupBy { it.corporationId }.map { (id, rows) ->
        val first = rows.first()
        AgentGroupRow(id, first.corporationName, first.corporationIcon, rows.size)
    }.sortedWith(compareBy(collator(language)) { it.name })
    AgentsResultLevel.DIVISION -> agents.groupBy { it.divisionId }.map { (id, rows) ->
        AgentGroupRow(id.toLong(), rows.first().divisionName, null, rows.size)
    }.sortedByDescending { it.id }
    AgentsResultLevel.LEVEL -> agents.groupBy { it.level }.map { (id, rows) ->
        AgentGroupRow(
            id = id.toLong(),
            name = levelName(id),
            iconFileName = null,
            count = rows.size,
            levelNumber = id,
        )
    }.sortedByDescending { it.id }
    AgentsResultLevel.AGENT -> emptyList()
}

private fun nameOrder(language: ContentLanguage): Comparator<AgentChoice> =
    compareBy(collator(language)) { it.name }

private fun collator(language: ContentLanguage): Collator =
    Collator.getInstance(
        if (language == ContentLanguage.CHINESE) Locale.CHINA else Locale.US,
    ).apply { strength = Collator.PRIMARY }

internal fun AgentRegionOption.matchesQuery(query: String): Boolean = matchesPlace(query, name, zhName, enName)

internal fun AgentSystemOption.matchesQuery(query: String): Boolean = matchesPlace(query, name, zhName, enName)

private fun matchesPlace(query: String, name: String, zhName: String?, enName: String?): Boolean {
    if (query.isBlank()) return true
    return listOf(name, zhName, enName).any { it?.contains(query, ignoreCase = true) == true }
}
