package com.marshall.pyerite.corporationModule.wallet.model

/** Pagination, date patterns, and location display for corporation wallets. */
internal object CorporationWalletConfig {
    const val FIRST_PAGE = 1
    const val JOURNAL_PAGE_SIZE = 2_500
    const val JOURNAL_MAX_PAGES = 10
    const val TRANSACTIONS_PAGE_SIZE = 2_500
    /** Newest page only. */
    const val TRANSACTIONS_MAX_PAGES = 1
    const val TYPE_QUERY_CHUNK = 500
    const val MASTER_WALLET_DIVISION = 1
    /** Player-owned Upwell structures use location IDs at or above this value. */
    const val PLAYER_STRUCTURE_ID_MIN = 1_000_000_000_000L

    const val WINDOW_DAYS_30 = 30
    const val WINDOW_DAYS_7 = 7
    const val WINDOW_DAYS_1 = 1

    const val MILLIS_PER_SECOND = 1_000L
    const val SECONDS_PER_MINUTE = 60
    const val MINUTES_PER_HOUR = 60
    const val HOURS_PER_DAY = 24
    const val MILLIS_PER_DAY =
        MILLIS_PER_SECOND * SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY

    const val DAY_KEY_PATTERN = "yyyy-MM-dd"
    const val DISPLAY_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm"
    const val DISPLAY_TIME_SECOND_PATTERN = "HH:mm:ss"

    const val SYSTEM_SECURITY_FORMAT = "%.1f"
    const val LOCATION_SEGMENT_GAP = " "
    const val LOCATION_PLACE_SEPARATOR = " - "
    const val SECURITY_NEGATIVE_MAX = 0.0
    const val SECURITY_LOW_MAX = 0.5

    const val ZERO_ISK = 0.0

    const val FORCE_REFRESH_DEBOUNCE_MS = 1_000L
    const val FILTER_SHEET_HEIGHT_FRACTION = 0.85f
    const val SETTINGS_SHEET_HEIGHT_FRACTION = 0.70f
}

