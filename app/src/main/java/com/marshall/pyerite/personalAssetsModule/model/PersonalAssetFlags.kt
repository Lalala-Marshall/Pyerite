package com.marshall.pyerite.personalAssetsModule.model

import androidx.annotation.StringRes
import com.marshall.pyerite.R
import kotlinx.serialization.Serializable

/** Fitting groups shown on a structure container, in display order. */
@Serializable
internal enum class PersonalAssetSlotGroup(
    @param:StringRes val labelRes: Int,
    val order: Int,
) {
    RIG(R.string.corporation_assets_slot_rig, 0),
    SERVICE(R.string.corporation_assets_slot_service, 1),
    HIGH(R.string.corporation_assets_slot_high, 2),
    MEDIUM(R.string.corporation_assets_slot_med, 3),
    LOW(R.string.corporation_assets_slot_low, 4),
    QUANTUM_CORE(R.string.corporation_assets_slot_quantum_core, 5),
    STRUCTURE_FUEL(R.string.corporation_assets_slot_structure_fuel, 6),
    FIGHTER(R.string.corporation_assets_slot_fighter, 7),
    SUBSYSTEM(R.string.corporation_assets_slot_subsystem, 8),
    BOOSTER(R.string.corporation_assets_slot_booster, 9),
}

/**
 * ESI `location_flag` values and the order / labels used to group them.
 * Wire strings stay in this file; UI reads [StringRes] only.
 */
internal object PersonalAssetFlags {
    const val OFFICE_FOLDER = "OfficeFolder"
    const val ASSET_SAFETY = "AssetSafety"
    const val HANGAR = "Hangar"
    const val CARGO = "Cargo"
    const val CORP_DELIVERIES = "CorpDeliveries"
    const val DELIVERIES = "Deliveries"
    const val CAPSULEER_DELIVERIES = "CapsuleerDeliveries"
    const val CORPORATION_GOAL_DELIVERIES = "CorporationGoalDeliveries"
    const val DRONE_BAY = "DroneBay"
    const val FLEET_HANGAR = "FleetHangar"
    const val SHIP_HANGAR = "ShipHangar"
    const val FRIGATE_ESCAPE_BAY = "FrigateEscapeBay"
    const val SPECIALIZED_FUEL_BAY = "SpecializedFuelBay"
    const val SPECIALIZED_ORE_HOLD = "SpecializedOreHold"
    const val SPECIALIZED_GAS_HOLD = "SpecializedGasHold"
    const val SPECIALIZED_MINERAL_HOLD = "SpecializedMineralHold"
    const val SPECIALIZED_ICE_HOLD = "SpecializedIceHold"
    const val SPECIALIZED_SALVAGE_HOLD = "SpecializedSalvageHold"
    const val SPECIALIZED_SHIP_HOLD = "SpecializedShipHold"
    const val SPECIALIZED_SMALL_SHIP_HOLD = "SpecializedSmallShipHold"
    const val SPECIALIZED_MEDIUM_SHIP_HOLD = "SpecializedMediumShipHold"
    const val SPECIALIZED_LARGE_SHIP_HOLD = "SpecializedLargeShipHold"
    const val SPECIALIZED_INDUSTRIAL_SHIP_HOLD = "SpecializedIndustrialShipHold"
    const val SPECIALIZED_AMMO_HOLD = "SpecializedAmmoHold"
    const val SPECIALIZED_COMMAND_CENTER_HOLD = "SpecializedCommandCenterHold"
    const val SPECIALIZED_PLANETARY_COMMODITIES_HOLD = "SpecializedPlanetaryCommoditiesHold"
    const val SPECIALIZED_MATERIAL_BAY = "SpecializedMaterialBay"
    const val MOON_MATERIAL_BAY = "MoonMaterialBay"
    const val INFRASTRUCTURE_HANGAR = "InfrastructureHangar"
    const val MOBILE_DEPOT_HOLD = "MobileDepotHold"
    const val QUAFE_BAY = "QuafeBay"
    const val IMPOUNDED = "Impounded"
    const val LOCKED = "Locked"
    const val UNLOCKED = "Unlocked"
    const val JUNKYARD_REPROCESSED = "JunkyardReprocessed"
    const val JUNKYARD_TRASHED = "JunkyardTrashed"
    const val CRATE_LOOT = "CrateLoot"
    const val PLANET_SURFACE = "PlanetSurface"
    const val SECONDARY_STORAGE = "SecondaryStorage"
    const val QUANTUM_CORE_ROOM = "QuantumCoreRoom"
    const val STRUCTURE_FUEL = "StructureFuel"
    const val FIGHTER_BAY = "FighterBay"
    const val SUBSYSTEM_BAY = "SubSystemBay"
    const val BOOSTER = "Booster"
    const val BOOSTER_BAY = "BoosterBay"

    /** Route segment for flags that have no dedicated label. */
    const val OTHER_ROUTE = "other"

    const val LOCATION_STATION = "station"
    const val LOCATION_SOLAR_SYSTEM = "solar_system"

    private const val SAG_PREFIX = "CorpSAG"
    private const val RIG_SLOT_PREFIX = "RigSlot"
    private const val SERVICE_SLOT_PREFIX = "ServiceSlot"
    private const val HI_SLOT_PREFIX = "HiSlot"
    private const val MED_SLOT_PREFIX = "MedSlot"
    private const val LO_SLOT_PREFIX = "LoSlot"
    private const val FIGHTER_TUBE_PREFIX = "FighterTube"
    private const val SUBSYSTEM_SLOT_PREFIX = "SubSystemSlot"

    private const val CONTENT_ORDER_HANGAR = 10
    private const val CONTENT_ORDER_OFFICE = 15
    private const val CONTENT_ORDER_SAG_BASE = 20
    private const val CONTENT_ORDER_LIST_BASE = 100
    private const val CONTENT_ORDER_SLOT_BASE = 500
    private const val CONTENT_ORDER_OTHER = 10_000

    /** Known non-hangar, non-slot flags, earliest first. */
    private val contentFlagOrder = listOf(
        CORP_DELIVERIES,
        DELIVERIES,
        CAPSULEER_DELIVERIES,
        CORPORATION_GOAL_DELIVERIES,
        CARGO,
        DRONE_BAY,
        FLEET_HANGAR,
        SHIP_HANGAR,
        FRIGATE_ESCAPE_BAY,
        SPECIALIZED_FUEL_BAY,
        SPECIALIZED_ORE_HOLD,
        SPECIALIZED_GAS_HOLD,
        SPECIALIZED_MINERAL_HOLD,
        SPECIALIZED_ICE_HOLD,
        SPECIALIZED_SALVAGE_HOLD,
        SPECIALIZED_SHIP_HOLD,
        SPECIALIZED_SMALL_SHIP_HOLD,
        SPECIALIZED_MEDIUM_SHIP_HOLD,
        SPECIALIZED_LARGE_SHIP_HOLD,
        SPECIALIZED_INDUSTRIAL_SHIP_HOLD,
        SPECIALIZED_AMMO_HOLD,
        SPECIALIZED_COMMAND_CENTER_HOLD,
        SPECIALIZED_PLANETARY_COMMODITIES_HOLD,
        SPECIALIZED_MATERIAL_BAY,
        MOON_MATERIAL_BAY,
        INFRASTRUCTURE_HANGAR,
        MOBILE_DEPOT_HOLD,
        QUAFE_BAY,
        IMPOUNDED,
        LOCKED,
        UNLOCKED,
        ASSET_SAFETY,
        JUNKYARD_REPROCESSED,
        JUNKYARD_TRASHED,
        CRATE_LOOT,
        PLANET_SURFACE,
        SECONDARY_STORAGE,
    )

    private val rigSlots = indexedFlags(RIG_SLOT_PREFIX, PersonalAssetsConfig.SLOT_INDEX_LAST)
    private val serviceSlots = indexedFlags(SERVICE_SLOT_PREFIX, PersonalAssetsConfig.SLOT_INDEX_LAST)
    private val highSlots = indexedFlags(HI_SLOT_PREFIX, PersonalAssetsConfig.SLOT_INDEX_LAST)
    private val medSlots = indexedFlags(MED_SLOT_PREFIX, PersonalAssetsConfig.SLOT_INDEX_LAST)
    private val lowSlots = indexedFlags(LO_SLOT_PREFIX, PersonalAssetsConfig.SLOT_INDEX_LAST)
    private val fighterTubes = indexedFlags(FIGHTER_TUBE_PREFIX, PersonalAssetsConfig.FIGHTER_TUBE_LAST)
    private val subsystemSlots = indexedFlags(SUBSYSTEM_SLOT_PREFIX, PersonalAssetsConfig.SLOT_INDEX_LAST)

    fun sagDivision(flag: String): Int? {
        if (!flag.startsWith(SAG_PREFIX)) return null
        val division = flag.removePrefix(SAG_PREFIX).toIntOrNull() ?: return null
        if (division !in PersonalAssetsConfig.HANGAR_DIVISION_FIRST..PersonalAssetsConfig.HANGAR_DIVISION_LAST) {
            return null
        }
        return division
    }

    fun slotGroup(flag: String): PersonalAssetSlotGroup? = when {
        flag in rigSlots -> PersonalAssetSlotGroup.RIG
        flag in serviceSlots -> PersonalAssetSlotGroup.SERVICE
        flag in highSlots -> PersonalAssetSlotGroup.HIGH
        flag in medSlots -> PersonalAssetSlotGroup.MEDIUM
        flag in lowSlots -> PersonalAssetSlotGroup.LOW
        flag == QUANTUM_CORE_ROOM -> PersonalAssetSlotGroup.QUANTUM_CORE
        flag == STRUCTURE_FUEL -> PersonalAssetSlotGroup.STRUCTURE_FUEL
        flag == FIGHTER_BAY || flag in fighterTubes -> PersonalAssetSlotGroup.FIGHTER
        flag == SUBSYSTEM_BAY || flag in subsystemSlots -> PersonalAssetSlotGroup.SUBSYSTEM
        flag == BOOSTER || flag == BOOSTER_BAY -> PersonalAssetSlotGroup.BOOSTER
        else -> null
    }

    fun folderRoute(flag: String): String {
        if (sagDivision(flag) != null || slotGroup(flag) != null) return flag
        if (contentSortOrder(flag) != CONTENT_ORDER_OTHER) return flag
        return OTHER_ROUTE
    }

    @StringRes
    fun folderLabelRes(routeFlag: String): Int = when (routeFlag) {
        OTHER_ROUTE -> R.string.corporation_assets_flag_other
        else -> contentLabelRes(routeFlag)
    }

    fun contentSortOrder(flag: String): Int {
        if (flag == HANGAR) return CONTENT_ORDER_HANGAR
        if (flag == OFFICE_FOLDER) return CONTENT_ORDER_OFFICE
        sagDivision(flag)?.let { return CONTENT_ORDER_SAG_BASE + it }
        val listed = contentFlagOrder.indexOf(flag)
        if (listed >= 0) return CONTENT_ORDER_LIST_BASE + listed
        slotGroup(flag)?.let { return CONTENT_ORDER_SLOT_BASE + it.order }
        return CONTENT_ORDER_OTHER
    }

    @StringRes
    fun contentLabelRes(flag: String): Int {
        sagDivision(flag)?.let { return R.string.corporation_assets_hangar_division }
        slotGroup(flag)?.let { return it.labelRes }
        return when (flag) {
            HANGAR -> R.string.corporation_assets_flag_hangar
            OFFICE_FOLDER -> R.string.corporation_assets_offices
            CORP_DELIVERIES -> R.string.corporation_assets_flag_corp_deliveries
            DELIVERIES -> R.string.corporation_assets_flag_deliveries
            CAPSULEER_DELIVERIES -> R.string.corporation_assets_flag_capsuleer_deliveries
            CORPORATION_GOAL_DELIVERIES -> R.string.corporation_assets_flag_corp_goal_deliveries
            CARGO -> R.string.corporation_assets_flag_cargo
            DRONE_BAY -> R.string.corporation_assets_flag_drone_bay
            FLEET_HANGAR -> R.string.corporation_assets_flag_fleet_hangar
            SHIP_HANGAR -> R.string.corporation_assets_flag_ship_hangar
            FRIGATE_ESCAPE_BAY -> R.string.corporation_assets_flag_frigate_escape
            SPECIALIZED_FUEL_BAY -> R.string.corporation_assets_flag_fuel_bay
            SPECIALIZED_ORE_HOLD -> R.string.corporation_assets_flag_ore_hold
            SPECIALIZED_GAS_HOLD -> R.string.corporation_assets_flag_gas_hold
            SPECIALIZED_MINERAL_HOLD -> R.string.corporation_assets_flag_mineral_hold
            SPECIALIZED_ICE_HOLD -> R.string.corporation_assets_flag_ice_hold
            SPECIALIZED_SALVAGE_HOLD -> R.string.corporation_assets_flag_salvage_hold
            SPECIALIZED_SHIP_HOLD -> R.string.corporation_assets_flag_ship_hold
            SPECIALIZED_SMALL_SHIP_HOLD -> R.string.corporation_assets_flag_small_ship_hold
            SPECIALIZED_MEDIUM_SHIP_HOLD -> R.string.corporation_assets_flag_medium_ship_hold
            SPECIALIZED_LARGE_SHIP_HOLD -> R.string.corporation_assets_flag_large_ship_hold
            SPECIALIZED_INDUSTRIAL_SHIP_HOLD -> R.string.corporation_assets_flag_industrial_ship_hold
            SPECIALIZED_AMMO_HOLD -> R.string.corporation_assets_flag_ammo_hold
            SPECIALIZED_COMMAND_CENTER_HOLD -> R.string.corporation_assets_flag_command_center
            SPECIALIZED_PLANETARY_COMMODITIES_HOLD -> R.string.corporation_assets_flag_planetary
            SPECIALIZED_MATERIAL_BAY -> R.string.corporation_assets_flag_material_bay
            MOON_MATERIAL_BAY -> R.string.corporation_assets_flag_moon_material
            INFRASTRUCTURE_HANGAR -> R.string.corporation_assets_flag_infrastructure
            MOBILE_DEPOT_HOLD -> R.string.corporation_assets_flag_mobile_depot
            QUAFE_BAY -> R.string.corporation_assets_flag_quafe
            IMPOUNDED -> R.string.corporation_assets_flag_impounded
            LOCKED -> R.string.corporation_assets_flag_locked
            UNLOCKED -> R.string.corporation_assets_flag_unlocked
            ASSET_SAFETY -> R.string.corporation_assets_asset_safety
            JUNKYARD_REPROCESSED -> R.string.corporation_assets_flag_junkyard_reprocessed
            JUNKYARD_TRASHED -> R.string.corporation_assets_flag_junkyard_trashed
            CRATE_LOOT -> R.string.corporation_assets_flag_crate_loot
            PLANET_SURFACE -> R.string.corporation_assets_flag_planet_surface
            SECONDARY_STORAGE -> R.string.corporation_assets_flag_secondary_storage
            else -> R.string.corporation_assets_flag_other
        }
    }

    private fun indexedFlags(prefix: String, lastIndex: Int): Set<String> =
        (PersonalAssetsConfig.SLOT_INDEX_FIRST..lastIndex).map { index -> "$prefix$index" }.toSet()
}
