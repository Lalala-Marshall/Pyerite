package com.marshall.pyerite.corporationModule.assets.model

/** Pagination, SDE category ids, and display rules for corporation assets. */
internal object CorporationAssetsConfig {
    const val FIRST_PAGE = 1
    /** ESI corporation-assets page size. */
    const val ASSETS_PAGE_SIZE = 1_000
    const val ASSETS_MAX_PAGES = 200
    const val QUERY_CHUNK = 500
    const val ASSET_NAMES_CHUNK = 1_000
    const val STAR_LOOKUP_CONCURRENCY = 4

    const val SLOT_INDEX_FIRST = 0
    const val SLOT_INDEX_LAST = 7
    const val FIGHTER_TUBE_LAST = 4
    const val HANGAR_DIVISION_FIRST = 1
    const val HANGAR_DIVISION_LAST = 7

    /** invCategories: Ship. */
    const val CATEGORY_SHIP = 6
    /** invCategories: Starbase. */
    const val CATEGORY_STARBASE = 23
    /** invCategories: Structure. */
    const val CATEGORY_STRUCTURE = 65

    /**
     * G5 yellow sun. Used when ESI does not return a star type for a solar system.
     * Same type id the character sheet uses for an unresolved sun.
     */
    const val FALLBACK_SUN_TYPE_ID = 6

    /** ESI asset-safety location id. Not a station or solar system. */
    const val ASSET_SAFETY_LOCATION_ID = 2_004L

    /**
     * Synthetic container ids for station categories that are not a real ESI item
     * (e.g. CorpDeliveries). Always negative so they never collide with item ids.
     */
    const val CATEGORY_CONTAINER_ID_BASE = -1_000_000_000_000L

    /** NPC station ids from ESI asset location docs. */
    const val NPC_STATION_ID_MIN = 60_000_000L
    const val NPC_STATION_ID_MAX = 64_000_000L

    /** Solar-system ids from ESI asset location docs. */
    const val SOLAR_SYSTEM_ID_MIN = 30_000_000L
    const val SOLAR_SYSTEM_ID_MAX = 32_000_000L

    /** Stable synthetic id for a location category container. */
    fun categoryContainerId(locationId: Long, routeFlag: String): Long {
        var hash = 17L
        hash = 31L * hash + locationId
        routeFlag.forEach { ch -> hash = 31L * hash + ch.code }
        val positive = hash and 0x7FFF_FFFF_FFFF_FFFFL
        return CATEGORY_CONTAINER_ID_BASE - (positive % 900_000_000_000L)
    }

    const val SYSTEM_SECURITY_FORMAT = "%.1f"
    const val SECURITY_STATUS_NAME_GAP = " "
    const val BUILDING_NAME_OPEN = " ["
    const val BUILDING_NAME_CLOSE = "]"
    const val SECURITY_NEGATIVE_MAX = 0.0
    const val SECURITY_LOW_MAX = 0.5

    const val CAPACITY_EMPTY_FRACTION = 0f
    const val CAPACITY_FULL_FRACTION = 1f
    const val UNLIMITED_CAPACITY_MAX = 0.0

    /** Contents bar turns yellow at this share of capacity, red once it is full. */
    const val CAPACITY_WARN_RATIO = 0.8
    const val CAPACITY_FULL_RATIO = 1.0
    const val CAPACITY_PERCENT_SCALE = 100.0

    /** Assembled asset tree kept on disk; pull-to-refresh and the top-bar action bypass it. */
    const val DISK_CACHE_TTL_MS = 24L * 60L * 60L * 1_000L
    const val DISK_CACHE_DIR = "corporation_assets"
    const val DISK_CACHE_FILE_PREFIX = "snapshot_"
    const val DISK_CACHE_FILE_SUFFIX = ".json"
    const val DISK_CACHE_TMP_SUFFIX = ".tmp"

    const val SEARCH_PATH_SEPARATOR = " / "
}
