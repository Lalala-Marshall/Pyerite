package com.marshall.pyerite.characterTransactionsModule.model

/** Local history and location display for character wallet transactions. */
internal object CharacterTransactionsConfig {
    /** Skip deletion until the stored history reaches this many rows. */
    const val CLEANUP_MIN_ROW_COUNT = 100_000

    /** Rows older than this many years are removed once [CLEANUP_MIN_ROW_COUNT] is reached. */
    const val CLEANUP_RETENTION_YEARS = 1

    /**
     * Skip another network read when a finished history sync is newer than this.
     * Same one-hour window as the personal wallet transaction cache.
     */
    const val DISK_CACHE_TTL_MS = 60L * 60L * 1_000L
    const val DISK_CACHE_DIR = "character_transactions"
    const val DISK_CACHE_FILE_PREFIX = "character_"
    const val DISK_CACHE_FILE_SUFFIX = ".json"
    const val DISK_CACHE_TMP_SUFFIX = ".tmp"

    const val TYPE_QUERY_CHUNK = 500

    /** Player-owned Upwell structures use location IDs at or above this value. */
    const val PLAYER_STRUCTURE_ID_MIN = 1_000_000_000_000L

    const val DAY_KEY_PATTERN = "yyyy-MM-dd"
    const val DISPLAY_TIME_SECOND_PATTERN = "HH:mm:ss"

    const val SYSTEM_SECURITY_FORMAT = "%.1f"
    const val LOCATION_SEGMENT_GAP = " "
    const val LOCATION_PLACE_SEPARATOR = " - "
    const val SECURITY_NEGATIVE_MAX = 0.0
    const val SECURITY_LOW_MAX = 0.5

    const val ZERO_ISK = 0.0
    const val SETTINGS_SHEET_HEIGHT_FRACTION = 0.70f
}
