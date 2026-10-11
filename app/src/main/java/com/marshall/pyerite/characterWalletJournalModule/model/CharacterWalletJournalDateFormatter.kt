package com.marshall.pyerite.characterWalletJournalModule.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object CharacterWalletJournalDateFormatter {

    fun dayKey(epochMs: Long): String =
        formatLocal(epochMs, CharacterWalletJournalConfig.DAY_KEY_PATTERN)

    fun displayDate(epochMs: Long): String =
        formatLocal(epochMs, CharacterWalletJournalConfig.DAY_KEY_PATTERN)

    fun displayDate(dayKey: String): String {
        val parsed = parseDayKey(dayKey) ?: return dayKey
        return displayDate(parsed)
    }

    fun displayDateTimeMinute(epochMs: Long): String =
        formatLocal(epochMs, CharacterWalletJournalConfig.DISPLAY_DATE_TIME_PATTERN)

    fun isWithinRollingDays(epochMs: Long, days: Int, nowMs: Long): Boolean {
        val windowMs = days * CharacterWalletJournalConfig.MILLIS_PER_DAY
        val age = nowMs - epochMs
        return age in 0 until windowMs
    }

    private fun parseDayKey(dayKey: String): Long? = runCatching {
        SimpleDateFormat(CharacterWalletJournalConfig.DAY_KEY_PATTERN, Locale.US).apply {
            timeZone = TimeZone.getDefault()
            isLenient = false
        }.parse(dayKey)?.time
    }.getOrNull()

    private fun formatLocal(epochMs: Long, pattern: String): String =
        SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date(epochMs))
}
