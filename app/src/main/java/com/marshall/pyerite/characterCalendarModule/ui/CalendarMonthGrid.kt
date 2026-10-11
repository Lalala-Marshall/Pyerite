package com.marshall.pyerite.characterCalendarModule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R
import com.marshall.pyerite.characterCalendarModule.model.CalendarDate
import com.marshall.pyerite.characterCalendarModule.model.CalendarDates
import com.marshall.pyerite.characterCalendarModule.model.CalendarTimeConfig
import com.marshall.pyerite.localization.LocaleController
import org.koin.compose.koinInject

@Composable
internal fun CalendarMonthSection(
    year: Int,
    month: Int,
    selectedDate: CalendarDate,
    eventCountOn: (CalendarDate) -> Int,
    onSelectDate: (CalendarDate) -> Unit,
    onShiftMonth: (Int) -> Unit,
) {
    val localeController: LocaleController = koinInject()
    val language = localeController.contentLanguage
    val locale = CalendarDates.contentLocale(language)
    val firstDayOfWeek = CalendarDates.firstDayOfWeek(locale)
    val cells = CalendarDates.monthCells(year, month, firstDayOfWeek)
    val weekdays = calendarWeekdayLabels(language)
    val today = CalendarDates.today()
    val cellShape = RoundedCornerShape(dimensionResource(R.dimen.character_calendar_day_corner))
    val todayBorder = colorResource(R.color.calendar_today_border)
    val todayBackground = colorResource(R.color.calendar_today_background)
    val todayText = colorResource(R.color.calendar_today_text)
    val selectedBorder = colorResource(R.color.calendar_selected_border)
    val selectedText = colorResource(R.color.calendar_selected_text)
    val primaryText = colorResource(R.color.text_primary)
    val captionText = colorResource(R.color.text_caption)
    val todayBorderWidth = dimensionResource(R.dimen.character_calendar_today_border)
    val selectedBorderWidth = dimensionResource(R.dimen.character_calendar_selected_border)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.character_calendar_grid_gap)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.character_calendar_month_nav_horizontal_padding)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CalendarMonthNavButton(
                onClick = { onShiftMonth(-1) },
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.character_calendar_previous_month),
            )
            Text(
                text = formatCalendarMonthTitle(year, month),
                color = primaryText,
                fontSize = dimensionResource(R.dimen.list_section_header_text_size).value.sp,
                fontWeight = FontWeight.SemiBold,
            )
            CalendarMonthNavButton(
                onClick = { onShiftMonth(1) },
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.character_calendar_next_month),
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.character_calendar_weekday_top_gap)))
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = captionText,
                    fontSize = dimensionResource(R.dimen.character_calendar_weekday_text_size).value.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.character_calendar_grid_gap)))
        cells.chunked(CalendarTimeConfig.DAYS_PER_WEEK).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    val date = cell.date
                    val isToday = date == today
                    val isSelected = date == selectedDate
                    val count = eventCountOn(date)
                    val isPastDate = date < today
                    val textColor = when {
                        isSelected -> selectedText
                        isToday -> todayText
                        !cell.inCurrentMonth -> captionText
                        else -> primaryText
                    }
                    val background = if (isToday) todayBackground else Color.Transparent
                    val borderColor = when {
                        isSelected -> selectedBorder
                        isToday -> todayBorder
                        else -> Color.Transparent
                    }
                    val borderWidth = when {
                        isSelected -> selectedBorderWidth
                        isToday -> todayBorderWidth
                        else -> dimensionResource(R.dimen.character_calendar_idle_border)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(CalendarDayAspect.RATIO)
                            .padding(dimensionResource(R.dimen.character_calendar_day_cell_inset))
                            .clip(cellShape)
                            .background(background, cellShape)
                            .border(width = borderWidth, color = borderColor, shape = cellShape)
                            .clickable { onSelectDate(date) }
                            .semantics { role = Role.Button }
                            .padding(dimensionResource(R.dimen.character_calendar_day_cell_padding)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = date.day.toString(),
                            color = textColor,
                            fontSize = dimensionResource(R.dimen.character_calendar_day_text_size).value.sp,
                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                        CalendarEventCountBadge(count = count, muted = isPastDate)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthNavButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
) {
    Box(
        modifier = Modifier
            .size(dimensionResource(R.dimen.top_bar_back_button_size))
            .clickable(onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colorResource(R.color.text_primary),
            modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
        )
    }
}

@Composable
private fun CalendarEventCountBadge(count: Int, muted: Boolean) {
    val badgeSize = dimensionResource(R.dimen.character_calendar_count_badge_size)
    if (count <= 0) {
        Spacer(modifier = Modifier.height(badgeSize))
        return
    }
    val badgeBackground = if (muted) {
        colorResource(R.color.calendar_event_count_badge_background_past)
    } else {
        colorResource(R.color.calendar_event_count_badge_background)
    }
    Box(
        modifier = Modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(badgeBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = calendarEventCountBadgeLabel(count),
            color = colorResource(R.color.calendar_event_count_badge_text),
            fontSize = dimensionResource(R.dimen.character_calendar_count_text_size).value.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

private object CalendarDayAspect {
    const val RATIO = 1f
}
