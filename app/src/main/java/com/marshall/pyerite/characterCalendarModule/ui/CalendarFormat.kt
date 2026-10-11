package com.marshall.pyerite.characterCalendarModule.ui

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.text.style.URLSpan
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.text.HtmlCompat
import com.marshall.pyerite.R
import com.marshall.pyerite.characterCalendarModule.model.CalendarDate
import com.marshall.pyerite.characterCalendarModule.model.CalendarDates
import com.marshall.pyerite.characterCalendarModule.model.CalendarDisplayConfig
import com.marshall.pyerite.characterCalendarModule.model.CalendarEventCountConfig
import com.marshall.pyerite.characterCalendarModule.model.CalendarEventResponse
import com.marshall.pyerite.characterCalendarModule.model.CalendarOwnerType
import com.marshall.pyerite.characterCalendarModule.model.CalendarReminderLead
import com.marshall.pyerite.characterCalendarModule.model.CalendarTimeConfig
import com.marshall.pyerite.esiModule.data.allianceLogoUrl
import com.marshall.pyerite.esiModule.data.corporationLogoUrl
import com.marshall.pyerite.esiModule.data.portraitUrl
import com.marshall.pyerite.esiModule.model.EsiDateTimeConfig
import com.marshall.pyerite.localization.ContentLanguage
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal fun formatCalendarMonthTitle(
    year: Int,
    month: Int,
): String {
    val cal = Calendar.getInstance()
    cal.clear()
    cal.set(Calendar.YEAR, year)
    cal.set(
        Calendar.MONTH,
        month - CalendarTimeConfig.JAVA_CALENDAR_MONTH_OFFSET,
    )
    cal.set(Calendar.DAY_OF_MONTH, 1)
    return SimpleDateFormat(CalendarDisplayConfig.MONTH_TITLE_PATTERN, Locale.US).format(cal.time)
}

internal fun formatCalendarDayTitle(date: CalendarDate): String {
    val cal = Calendar.getInstance()
    cal.clear()
    cal.set(Calendar.YEAR, date.year)
    cal.set(
        Calendar.MONTH,
        date.month - CalendarTimeConfig.JAVA_CALENDAR_MONTH_OFFSET,
    )
    cal.set(Calendar.DAY_OF_MONTH, date.day)
    return SimpleDateFormat(CalendarDisplayConfig.DAY_TITLE_PATTERN, Locale.US).format(cal.time)
}

internal fun calendarWeekdayLabels(language: ContentLanguage): List<String> {
    val locale = CalendarDates.contentLocale(language)
    val names = DateFormatSymbols(locale).shortWeekdays
    val first = CalendarDates.firstDayOfWeek(locale)
    return List(CalendarTimeConfig.DAYS_PER_WEEK) { offset ->
        val dow =
            ((first - 1 + offset) % CalendarTimeConfig.DAYS_PER_WEEK) + 1
        names[dow]
    }
}

/** ESI timestamps are UTC; show the device local date and time. */
internal fun formatCalendarDateTime(epochMs: Long): String {
    return SimpleDateFormat(EsiDateTimeConfig.DISPLAY_DATE_TIME_PATTERN, Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }.format(Date(epochMs))
}

@Composable
internal fun calendarEventCountBadgeLabel(count: Int): String {
    return if (count > CalendarEventCountConfig.BADGE_MAX) {
        stringResource(R.string.character_calendar_event_count_capped)
    } else {
        count.toString()
    }
}

@Composable
internal fun calendarResponseLabel(response: CalendarEventResponse): String {
    val res = when (response) {
        CalendarEventResponse.ACCEPTED -> R.string.character_calendar_response_accepted
        CalendarEventResponse.DECLINED -> R.string.character_calendar_response_declined
        CalendarEventResponse.NOT_RESPONDED -> R.string.character_calendar_response_not_responded
        CalendarEventResponse.TENTATIVE -> R.string.character_calendar_response_tentative
    }
    return stringResource(res)
}

internal fun calendarOwnerIconUrl(type: CalendarOwnerType, ownerId: Long): String? {
    if (ownerId <= 0L) return null
    return when (type) {
        CalendarOwnerType.CHARACTER -> portraitUrl(ownerId)
        CalendarOwnerType.CORPORATION -> corporationLogoUrl(ownerId)
        CalendarOwnerType.ALLIANCE -> allianceLogoUrl(ownerId)
        CalendarOwnerType.FACTION,
        CalendarOwnerType.EVE_SERVER,
        CalendarOwnerType.UNKNOWN,
        -> null
    }
}

@Composable
internal fun calendarOwnerTypeLabel(type: CalendarOwnerType): String {
    val res = when (type) {
        CalendarOwnerType.CHARACTER -> R.string.character_calendar_owner_character
        CalendarOwnerType.CORPORATION -> R.string.character_calendar_owner_corporation
        CalendarOwnerType.ALLIANCE -> R.string.character_calendar_owner_alliance
        CalendarOwnerType.FACTION -> R.string.character_calendar_owner_faction
        CalendarOwnerType.EVE_SERVER -> R.string.character_calendar_owner_eve_server
        CalendarOwnerType.UNKNOWN -> R.string.character_sheet_value_placeholder
    }
    return stringResource(res)
}

@Composable
internal fun calendarReminderLeadLabel(lead: CalendarReminderLead): String {
    val res = when (lead) {
        CalendarReminderLead.TWO_HOURS -> R.string.character_calendar_reminder_lead_two_hours
        CalendarReminderLead.ONE_HOUR -> R.string.character_calendar_reminder_lead_one_hour
        CalendarReminderLead.THIRTY_MINUTES -> R.string.character_calendar_reminder_lead_thirty_minutes
        CalendarReminderLead.FIFTEEN_MINUTES -> R.string.character_calendar_reminder_lead_fifteen_minutes
        CalendarReminderLead.AT_START -> R.string.character_calendar_reminder_lead_at_start
    }
    return stringResource(res)
}

internal fun calendarEventBodyWithoutLinkStyling(html: String): CharSequence {
    val spannable = SpannableString(
        HtmlCompat.fromHtml(
            htmlWithPreservedLineBreaks(html),
            HtmlCompat.FROM_HTML_MODE_COMPACT,
        ),
    )
    spannable.getSpans(0, spannable.length, ForegroundColorSpan::class.java)
        .forEach(spannable::removeSpan)
    spannable.getSpans(0, spannable.length, UnderlineSpan::class.java)
        .forEach(spannable::removeSpan)
    spannable.getSpans(0, spannable.length, URLSpan::class.java)
        .forEach(spannable::removeSpan)
    return spannable
}

private fun htmlWithPreservedLineBreaks(html: String): String {
    if (html.isEmpty()) return html
    val normalized = html
        .replace(CalendarBodyHtml.CRLF, CalendarBodyHtml.NEWLINE)
        .replace(CalendarBodyHtml.CARRIAGE_RETURN, CalendarBodyHtml.NEWLINE)
    return normalized.replace(CalendarBodyHtml.NEWLINE, CalendarBodyHtml.BREAK_TAG)
}

private object CalendarBodyHtml {
    const val BREAK_TAG = "<br>"
    const val NEWLINE = "\n"
    const val CARRIAGE_RETURN = "\r"
    const val CRLF = "\r\n"
}
