package com.marshall.pyerite.contractsCommon.model

/** Pagination, date patterns, and list limits for corporation-issued contracts. */
internal object CorporationContractsConfig {
    const val FIRST_PAGE = 1
    /** ESI corporation-contracts page size used when `X-Pages` is absent. */
    const val CONTRACTS_PAGE_SIZE = 1_000
    const val CONTRACTS_MAX_PAGES = 20

    const val MILLIS_PER_SECOND = 1_000L
    const val SECONDS_PER_MINUTE = 60
    const val MINUTES_PER_HOUR = 60
    const val HOURS_PER_DAY = 24
    const val MILLIS_PER_DAY =
        MILLIS_PER_SECOND * SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY

    const val DAY_KEY_PATTERN = "yyyy-MM-dd"
    const val DISPLAY_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm"

    /** Player structures use ids at or above this; smaller ids are NPC stations. */
    const val PLAYER_STRUCTURE_ID_MIN = 1_000_000_000_000L
    const val TYPE_QUERY_CHUNK = 500

    const val SYSTEM_SECURITY_FORMAT = "%.1f"
    const val SECURITY_STATUS_NAME_GAP = " "
    const val SECURITY_NEGATIVE_MAX = 0.0
    const val SECURITY_LOW_MAX = 0.5

    const val ZERO_ISK = 0.0
    const val PRICE_INPUT_MAX_LENGTH = 18

    const val LIMIT_50 = 50
    const val LIMIT_100 = 100
    const val LIMIT_300 = 300
    const val LIMIT_500 = 500

    const val FILTER_SHEET_HEIGHT_FRACTION = 0.85f

    const val INCOMPLETE_SECTION_ID = "incomplete"

    /** Escaped so a contract title containing `%` is not treated as a format specifier. */
    const val FORMAT_PERCENT = "%"
    const val FORMAT_PERCENT_ESCAPED = "%%"
}
