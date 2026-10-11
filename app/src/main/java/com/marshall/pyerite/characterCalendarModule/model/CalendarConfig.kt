package com.marshall.pyerite.characterCalendarModule.model

/** ESI calendar list pagination (50 events per page; `from_event` only moves forward). */
internal object CalendarEsiConfig {
    const val PAGE_SIZE = 50
    const val MAX_PAGES = 5
    const val DETAIL_PROBE_CONCURRENCY = 4
}

internal object CalendarTimeConfig {
    const val MILLIS_PER_SECOND = 1_000L
    const val SECONDS_PER_MINUTE = 60
    const val MINUTES_PER_HOUR = 60
    const val DAYS_PER_WEEK = 7
    /** `Calendar.MONTH` is 0-based; domain months are 1–12. */
    const val JAVA_CALENDAR_MONTH_OFFSET = 1
    const val MILLIS_PER_MINUTE = MILLIS_PER_SECOND * SECONDS_PER_MINUTE
}

internal object CalendarReminderOffsets {
    const val TWO_HOURS_MINUTES = 2 * CalendarTimeConfig.MINUTES_PER_HOUR
    const val ONE_HOUR_MINUTES = CalendarTimeConfig.MINUTES_PER_HOUR
    const val THIRTY_MINUTES = 30
    const val FIFTEEN_MINUTES = 15
    const val AT_START_MINUTES = 0
}

internal object CalendarEventImportance {
    const val NORMAL = 0
    const val IMPORTANT = 1

    fun isImportant(importance: Int): Boolean = when (importance) {
        NORMAL -> false
        IMPORTANT -> true
        else -> importance > NORMAL
    }
}

internal object CalendarDisplayConfig {
    const val MONTH_TITLE_PATTERN = "yyyy-MM"
    const val DAY_TITLE_PATTERN = "yyyy-MM-dd"
}

internal object CalendarEventCountConfig {
    const val BADGE_MAX = 99
}

internal object CalendarSheetConfig {
    const val HEIGHT_FRACTION = 0.85f
}

/**
 * ESI calendar summaries can include deleted / empty-title ghosts whose detail
 * route returns 404. Those are not useful to show.
 */
internal object CalendarEventStatus {
    fun isExpired(
        startEpochMs: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Boolean = startEpochMs <= nowEpochMs

    fun isUpcoming(
        startEpochMs: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Boolean = startEpochMs > nowEpochMs

    fun isDisplayableTitle(title: String): Boolean = title.isNotBlank()
}
