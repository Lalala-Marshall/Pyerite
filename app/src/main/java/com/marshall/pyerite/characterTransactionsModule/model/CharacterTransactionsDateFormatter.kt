package com.marshall.pyerite.characterTransactionsModule.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object CharacterTransactionsDateFormatter {

    fun dayKey(epochMs: Long): String = formatLocal(epochMs, CharacterTransactionsConfig.DAY_KEY_PATTERN)

    /** Day keys are already [CharacterTransactionsConfig.DAY_KEY_PATTERN]. */
    fun displayDate(dayKey: String): String = dayKey

    fun displayTimeSecond(epochMs: Long): String =
        formatLocal(epochMs, CharacterTransactionsConfig.DISPLAY_TIME_SECOND_PATTERN)

    private fun formatLocal(
        epochMs: Long,
        pattern: String,
        locale: Locale = Locale.US,
    ): String = SimpleDateFormat(pattern, locale).apply {
        timeZone = TimeZone.getDefault()
    }.format(Date(epochMs))
}
