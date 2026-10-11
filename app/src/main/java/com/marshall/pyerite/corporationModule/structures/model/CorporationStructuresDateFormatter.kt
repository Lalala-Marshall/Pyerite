package com.marshall.pyerite.corporationModule.structures.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object CorporationStructuresDateFormatter {

    fun displayDateTime(epochMs: Long): String =
        SimpleDateFormat(CorporationStructuresConfig.DISPLAY_DATE_TIME_PATTERN, Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date(epochMs))

    fun remainingSeconds(expiresAtMillis: Long, nowMs: Long): Long {
        val remainingMs = expiresAtMillis - nowMs
        if (remainingMs <= 0L) return 0L
        return remainingMs / CorporationStructuresConfig.MILLIS_PER_SECOND
    }

    fun isExpired(expiresAtMillis: Long, nowMs: Long): Boolean = expiresAtMillis <= nowMs
}
