package com.marshall.pyerite.sdeModule.room.npc

import androidx.room.Dao
import androidx.room.Query

@Dao
interface AgentDao {
    @Query(
        """
        SELECT
            a.agent_id AS agentId,
            COALESCE(a.agent_type, 0) AS agentType,
            COALESCE(a.agent_name, '') AS agentName,
            COALESCE(a.level, 0) AS level,
            a.corporationID AS corporationId,
            COALESCE(a.divisionID, 0) AS divisionId,
            COALESCE(a.isLocator, 0) AS isLocator,
            COALESCE(a.locationID, 0) AS locationId,
            a.solarSystemID AS solarSystemId,
            st.stationName AS stationName,
            st.solarSystemID AS stationSystemId,
            COALESCE(u.system_security, ss.security_status) AS systemSecurity,
            ss.solarSystemName AS systemName,
            ss.solarSystemName_zh AS systemZhName,
            ss.solarSystemName_en AS systemEnName,
            c.name AS corporationName,
            c.zh_name AS corporationZhName,
            c.en_name AS corporationEnName,
            c.icon_filename AS corporationIcon,
            f.id AS factionId,
            f.name AS factionName,
            f.zh_name AS factionZhName,
            f.en_name AS factionEnName,
            f.iconName AS factionIcon,
            COALESCE(d.name, '') AS divisionName,
            u.region_id AS regionId,
            r.regionName AS regionName,
            r.regionName_zh AS regionZhName,
            r.regionName_en AS regionEnName
        FROM agents a
        INNER JOIN npcCorporations c ON c.corporation_id = a.corporationID
        INNER JOIN factions f ON f.id = c.faction_id
        LEFT JOIN stations st ON st.stationID = a.locationID
        LEFT JOIN solarsystems ss ON ss.solarSystemID = COALESCE(a.solarSystemID, st.solarSystemID)
        LEFT JOIN universe u ON u.solarsystem_id = ss.solarSystemID
        LEFT JOIN regions r ON r.regionID = u.region_id
        LEFT JOIN divisions d ON d.division_id = a.divisionID
        """,
    )
    suspend fun getAllAgents(): List<AgentQueryRow>
}
