package com.marshall.pyerite.sdeModule.room.npc

/**
 * One agent joined to its station, system, faction, corporation, and division.
 * Bilingual columns are present in both language databases. `agentName`,
 * `stationName`, and `divisionName` are the active language of the open file.
 */
data class AgentQueryRow(
    val agentId: Int,
    val agentType: Int,
    val agentName: String,
    val level: Int,
    val corporationId: Long,
    val divisionId: Int,
    val isLocator: Int,
    val locationId: Long,
    val solarSystemId: Int?,
    val stationName: String?,
    val stationSystemId: Int?,
    val systemSecurity: Double?,
    val systemName: String?,
    val systemZhName: String?,
    val systemEnName: String?,
    val corporationName: String?,
    val corporationZhName: String?,
    val corporationEnName: String?,
    val corporationIcon: String?,
    val factionId: Int,
    val factionName: String?,
    val factionZhName: String?,
    val factionEnName: String?,
    val factionIcon: String?,
    val divisionName: String,
    val regionId: Int?,
    val regionName: String?,
    val regionZhName: String?,
    val regionEnName: String?,
)
