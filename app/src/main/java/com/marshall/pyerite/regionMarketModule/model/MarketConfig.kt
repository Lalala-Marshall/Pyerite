package com.marshall.pyerite.regionMarketModule.model

/**
 * Market browser ids, ESI paging, and display thresholds that are shared
 * across the region-market feature.
 */
internal object MarketConfig {
    /** The Forge. Default selected market when nothing has been chosen. */
    const val DEFAULT_REGION_ID = 10000002

    /** PLEX orders are served from the global market, not a k-space region. */
    const val PLEX_TYPE_ID = 44992
    const val PLEX_REGION_ID = 19000001

    /** Regions at or above this id are wormhole / abyss / special and are not listed. */
    const val TRADE_REGION_ID_EXCLUSIVE_MAX = 11_000_000

    const val PLAYER_STRUCTURE_ID_MIN = 1_000_000_000_000L

    const val NO_PARENT_GROUP = -1

    const val FIRST_PAGE = 1
    const val ORDER_PAGE_SIZE = 1_000
    const val MAX_ORDER_PAGES = 100

    const val SQL_IN_CHUNK = 400
    const val SEARCH_RESULT_LIMIT = 50
    const val STRUCTURE_SEARCH_LIMIT = 20
    const val SEARCH_DEBOUNCE_MS = 300L

    /** Pad above the highest and below the lowest history price, matching Tritanium. */
    const val AXIS_PRICE_PADDING = 0.15

    /** Leading price ticks, matching Tritanium's desired count. */
    const val AXIS_TICK_COUNT = 5

    /** Used when every history point has the same price, so the line is not stuck on the axis. */
    const val AXIS_FLAT_PRICE_SPAN_FRACTION = 0.15

    /** Volume bars occupy this fraction of the price axis, matching Tritanium. */
    const val VOLUME_AXIS_HEIGHT_FRACTION = 0.7

    /**
     * Upwell hulls that cannot fit a market service. Other types are kept only when
     * ESI structure orders succeed.
     */
    val NON_MARKET_STRUCTURE_TYPE_IDS: Set<Int> = setOf(
        35825,
        35826,
        35832,
        35835,
        35836,
    )

    const val HISTORY_DAYS_MONTH = 30
    const val HISTORY_DAYS_QUARTER = 90
    const val HISTORY_DAYS_YEAR = 365

    /** Below 0.1M, show the full ISK amount instead of a compact abbreviation. */
    const val FULL_PRICE_EXCLUSIVE_MAX = 100_000.0

    const val SECURITY_NEGATIVE_MAX = 0.0
    const val SECURITY_LOW_MAX = 0.5
    const val SECURITY_FORMAT = "%.1f"
    const val SECTION_OTHER = "#"

    const val DATE_PATTERN = "yyyy-MM-dd"
    const val MONTH_PATTERN = "MMM"

    /** Jita, Amarr, Rens, Hek, Zarzakh — fixed trade-hub order. */
    val MAJOR_SYSTEM_IDS: List<Int> = listOf(
        30000142,
        30002187,
        30002510,
        30002053,
        30100000,
    )
}
