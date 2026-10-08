package com.marshall.pyerite.sdeModule.room.map

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Matches the prepackaged SDE `solarsystems` table exactly (Room createFromFile validates).
 */
@Entity(tableName = "solarsystems")
data class SolarSystemEntity(
    @PrimaryKey
    @ColumnInfo(name = "solarSystemID")
    val id: Int,
    @ColumnInfo(name = "solarSystemName") val name: String? = null,
    @ColumnInfo(name = "solarSystemName_de") val deName: String? = null,
    @ColumnInfo(name = "solarSystemName_en") val enName: String? = null,
    @ColumnInfo(name = "solarSystemName_es") val esName: String? = null,
    @ColumnInfo(name = "solarSystemName_fr") val frName: String? = null,
    @ColumnInfo(name = "solarSystemName_ja") val jaName: String? = null,
    @ColumnInfo(name = "solarSystemName_ko") val koName: String? = null,
    @ColumnInfo(name = "solarSystemName_ru") val ruName: String? = null,
    @ColumnInfo(name = "solarSystemName_zh") val zhName: String? = null,
    @ColumnInfo(name = "security_status") val securityStatus: Double? = null,
)

/**
 * Matches the prepackaged SDE `stations` table exactly.
 */
@Entity(
    tableName = "stations",
    indices = [
        Index(value = ["solarSystemID"], name = "idx_stations_solarSystemID"),
    ],
)
data class StationEntity(
    @PrimaryKey
    @ColumnInfo(name = "stationID")
    val id: Long,
    @ColumnInfo(name = "stationTypeID") val typeId: Int? = null,
    @ColumnInfo(name = "stationName") val name: String? = null,
    @ColumnInfo(name = "regionID") val regionId: Int? = null,
    @ColumnInfo(name = "solarSystemID") val solarSystemId: Int? = null,
    @ColumnInfo(name = "security") val security: Double? = null,
)

/**
 * Matches the prepackaged SDE `regions` table exactly.
 */
@Entity(tableName = "regions")
data class RegionEntity(
    @PrimaryKey
    @ColumnInfo(name = "regionID")
    val id: Int,
    @ColumnInfo(name = "regionName") val name: String? = null,
    @ColumnInfo(name = "regionName_de") val deName: String? = null,
    @ColumnInfo(name = "regionName_en") val enName: String? = null,
    @ColumnInfo(name = "regionName_es") val esName: String? = null,
    @ColumnInfo(name = "regionName_fr") val frName: String? = null,
    @ColumnInfo(name = "regionName_ja") val jaName: String? = null,
    @ColumnInfo(name = "regionName_ko") val koName: String? = null,
    @ColumnInfo(name = "regionName_ru") val ruName: String? = null,
    @ColumnInfo(name = "regionName_zh") val zhName: String? = null,
)

/**
 * Matches the prepackaged SDE `universe` table exactly.
 */
@Entity(
    tableName = "universe",
    primaryKeys = ["region_id", "constellation_id", "solarsystem_id"],
)
data class UniverseLinkEntity(
    @ColumnInfo(name = "region_id") val regionId: Int,
    @ColumnInfo(name = "constellation_id") val constellationId: Int,
    @ColumnInfo(name = "solarsystem_id") val solarSystemId: Int,
    @ColumnInfo(name = "system_security") val systemSecurity: Double? = null,
    @ColumnInfo(name = "system_type") val systemType: Int? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    @ColumnInfo(name = "hasStation") val hasStation: Int,
    @ColumnInfo(name = "hasJumpGate") val hasJumpGate: Int,
    @ColumnInfo(name = "isJSpace") val isJSpace: Int,
    val jove: Int,
    val temperate: Int,
    val barren: Int,
    val oceanic: Int,
    val ice: Int,
    val gas: Int,
    val lava: Int,
    val storm: Int,
    val plasma: Int,
)

/**
 * Joined solar-system + region row for character location display.
 * Not a table entity — Room @Query projection only.
 */
/**
 * Solar-system id plus display fields for batch location lookup.
 * Not a table entity — Room @Query projection only.
 */
data class SolarSystemLookupRow(
    @ColumnInfo(name = "solarSystemID") val solarSystemId: Long,
    @ColumnInfo(name = "system_name") val systemName: String?,
    @ColumnInfo(name = "system_zh_name") val systemZhName: String?,
    @ColumnInfo(name = "system_en_name") val systemEnName: String?,
    @ColumnInfo(name = "security_status") val securityStatus: Double?,
)

/** Batch solar-system lookup that also includes the parent region name. */
data class SolarSystemRegionLookupRow(
    @ColumnInfo(name = "solarSystemID") val solarSystemId: Long,
    @ColumnInfo(name = "system_name") val systemName: String?,
    @ColumnInfo(name = "system_zh_name") val systemZhName: String?,
    @ColumnInfo(name = "system_en_name") val systemEnName: String?,
    @ColumnInfo(name = "security_status") val securityStatus: Double?,
    @ColumnInfo(name = "region_name") val regionName: String?,
    @ColumnInfo(name = "region_zh_name") val regionZhName: String?,
    @ColumnInfo(name = "region_en_name") val regionEnName: String?,
)

data class SolarSystemLocationRow(
    @ColumnInfo(name = "system_name") val systemName: String?,
    @ColumnInfo(name = "system_zh_name") val systemZhName: String?,
    @ColumnInfo(name = "system_en_name") val systemEnName: String?,
    @ColumnInfo(name = "security_status") val securityStatus: Double?,
    @ColumnInfo(name = "region_name") val regionName: String?,
    @ColumnInfo(name = "region_zh_name") val regionZhName: String?,
    @ColumnInfo(name = "region_en_name") val regionEnName: String?,
)

/**
 * NPC station name + type for docked location icons.
 * Not a table entity — Room @Query projection over [StationEntity].
 */
data class StationLocationRow(
    @ColumnInfo(name = "stationID") val stationId: Long,
    @ColumnInfo(name = "stationTypeID") val typeId: Int?,
    @ColumnInfo(name = "stationName") val name: String?,
    @ColumnInfo(name = "solarSystemID") val solarSystemId: Int? = null,
)

/** Universe coordinates for nearest-system lookup (e.g. asset safety). */
data class SolarSystemPositionRow(
    val solarSystemId: Long,
    val x: Double?,
    val y: Double?,
    val z: Double?,
)
