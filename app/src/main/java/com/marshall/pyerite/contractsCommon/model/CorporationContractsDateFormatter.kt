package com.marshall.pyerite.contractsCommon.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object CorporationContractsDateFormatter {

    fun dayKey(epochMs: Long): String = formatLocal(epochMs, CorporationContractsConfig.DAY_KEY_PATTERN)

    fun displayDate(epochMs: Long): String =
        formatLocal(epochMs, CorporationContractsConfig.DAY_KEY_PATTERN)

    fun displayDateTime(epochMs: Long): String =
        formatLocal(epochMs, CorporationContractsConfig.DISPLAY_DATE_TIME_PATTERN)

    /** Whole local calendar days from today until [expiryMs]. Negative when already past. */
    fun daysUntil(expiryMs: Long, nowMs: Long): Int =
        (localDayIndex(expiryMs) - localDayIndex(nowMs)).toInt()

    private fun localDayIndex(epochMs: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = epochMs }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val midnight = calendar.timeInMillis
        val offset = calendar.timeZone.getOffset(midnight).toLong()
        return (midnight + offset) / CorporationContractsConfig.MILLIS_PER_DAY
    }

    private fun formatLocal(
        epochMs: Long,
        pattern: String,
        locale: Locale = Locale.US,
    ): String = SimpleDateFormat(pattern, locale).apply {
        timeZone = TimeZone.getDefault()
    }.format(Date(epochMs))
}
