package com.marshall.pyerite.corporationModule.industry.model

/** Pagination, due-soon window, and list display for corporation industry jobs. */
internal object CorporationIndustryConfig {
    const val FIRST_PAGE = 1
    /** ESI corporation industry-jobs page size used when `X-Pages` is absent. */
    const val JOBS_PAGE_SIZE = 1_000
    const val JOBS_MAX_PAGES = 20
    const val QUERY_CHUNK = 500
    const val STRUCTURE_LOOKUP_PARALLELISM = 4

    /** Always request the 90-day history so the hide-closed switch does not refetch. */
    const val INCLUDE_COMPLETED = true

    /** NPC stations sit below this id; player structures are at or above it. */
    const val PLAYER_STRUCTURE_ID_MIN = 1_000_000_000_000L

    const val MILLIS_PER_SECOND = 1_000L
    const val SECONDS_PER_MINUTE = 60
    const val MINUTES_PER_HOUR = 60

    /** Tritanium `soonCompleteThreshold`: active jobs finishing inside this window. */
    const val DUE_SOON_WINDOW_HOURS = 8
    const val DUE_SOON_WINDOW_MILLIS =
        MILLIS_PER_SECOND * SECONDS_PER_MINUTE * MINUTES_PER_HOUR * DUE_SOON_WINDOW_HOURS

    const val PROGRESS_EMPTY = 0f
    const val PROGRESS_COMPLETE = 1f

    const val DISPLAY_DATE_TIME_PATTERN_ZH = "yyyy/MM/dd HH:mm"
    const val DISPLAY_DATE_TIME_PATTERN_EN = "MMM d, yyyy HH:mm"

    const val SYSTEM_SECURITY_FORMAT = "%.1f"
    const val SECURITY_STATUS_NAME_GAP = " "
    const val SECURITY_NEGATIVE_MAX = 0.0
    const val SECURITY_LOW_MAX = 0.5

    const val FILTER_SHEET_HEIGHT_FRACTION = 0.85f

    const val PROGRESS_SHIMMER_DURATION_MS = 2_400
    const val PROGRESS_SHIMMER_WIDTH_FRACTION = 0.4f
    const val PROGRESS_SHIMMER_PEAK_ALPHA = 0.55f
}
