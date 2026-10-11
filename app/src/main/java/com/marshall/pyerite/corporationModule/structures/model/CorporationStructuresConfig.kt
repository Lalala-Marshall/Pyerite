package com.marshall.pyerite.corporationModule.structures.model

/** Pagination, fuel-monitor windows, and list display for corporation structures. */
internal object CorporationStructuresConfig {
    const val FIRST_PAGE = 1
    /** ESI corporation-structures page size. */
    const val STRUCTURES_PAGE_SIZE = 1_000
    const val STRUCTURES_MAX_PAGES = 20
    const val QUERY_CHUNK = 500

    const val MONITOR_NONE_DAYS = 0
    const val MONITOR_ONE_WEEK_DAYS = 7
    const val MONITOR_TWO_WEEKS_DAYS = 14
    const val MONITOR_THREE_WEEKS_DAYS = 21
    const val MONITOR_ONE_MONTH_DAYS = 30
    const val MONITOR_TWO_MONTHS_DAYS = 60

    const val MILLIS_PER_SECOND = 1_000L
    const val SECONDS_PER_MINUTE = 60
    const val MINUTES_PER_HOUR = 60
    const val HOURS_PER_DAY = 24
    const val MILLIS_PER_DAY =
        MILLIS_PER_SECOND * SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY

    const val DISPLAY_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm"

    const val SYSTEM_SECURITY_FORMAT = "%.1f"
    const val SECURITY_STATUS_NAME_GAP = " "
    const val SECURITY_NEGATIVE_MAX = 0.0
    const val SECURITY_LOW_MAX = 0.5

    const val SETTINGS_SHEET_HEIGHT_FRACTION = 0.70f
}
