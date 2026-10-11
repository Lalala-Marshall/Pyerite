package com.marshall.pyerite.characterWalletJournalModule.model

internal object CharacterWalletJournalConfig {
    const val FIRST_PAGE = 1
    const val JOURNAL_PAGE_SIZE = 2_500
    const val JOURNAL_MAX_PAGES = 10

    /** Skip another ESI read when a finished sync is newer than this. */
    const val DISK_CACHE_TTL_MS = 60L * 60L * 1_000L
    const val DISK_CACHE_DIR = "character_wallet_journal"
    const val DISK_CACHE_FILE_PREFIX = "character_"
    const val DISK_CACHE_FILE_SUFFIX = ".json"
    const val DISK_CACHE_TMP_SUFFIX = ".tmp"

    /** Skip deletion until every character file together reaches this many rows. */
    const val CLEANUP_MIN_ROW_COUNT = 100_000

    /** Rows older than this many years are removed once [CLEANUP_MIN_ROW_COUNT] is reached. */
    const val CLEANUP_RETENTION_YEARS = 1

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

    const val ZERO_ISK = 0.0

    const val FORCE_REFRESH_DEBOUNCE_MS = 1_000L
    const val FILTER_SHEET_HEIGHT_FRACTION = 0.85f
}
