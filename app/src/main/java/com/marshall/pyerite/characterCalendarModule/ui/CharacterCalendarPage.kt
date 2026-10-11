package com.marshall.pyerite.characterCalendarModule.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.characterCalendarModule.model.CalendarAddReminderResult
import com.marshall.pyerite.characterCalendarModule.model.CalendarDate
import com.marshall.pyerite.characterCalendarModule.model.CalendarEventStatus
import com.marshall.pyerite.characterCalendarModule.model.CalendarReminderLead
import com.marshall.pyerite.characterCalendarModule.model.CharacterCalendarEvent
import com.marshall.pyerite.characterCalendarModule.viewModel.CharacterCalendarViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemHint
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.pyeritePullRefreshTopBarAction
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.rememberScrollTitleCollapsed
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CharacterCalendarPage(
    navController: NavController,
    viewModel: CharacterCalendarViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val pageTitle = stringResource(R.string.character_calendar)
    val onBack = navController.rememberNavigateUpAction()
    val showCollapsedTitle = rememberScrollTitleCollapsed(scrollState)
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val bottomPadding = dimensionResource(R.dimen.type_detail_bottom_padding)
    val context = LocalContext.current
    val reminderAdded = stringResource(R.string.character_calendar_reminder_added)
    val reminderDuplicate = stringResource(R.string.character_calendar_reminder_duplicate)
    val reminderPast = stringResource(R.string.character_calendar_reminder_past)
    val remindersDescription = stringResource(R.string.character_calendar_reminders)
    val reminderDenied = stringResource(R.string.character_calendar_reminder_notification_denied)
    var showingReminders by rememberSaveable { mutableStateOf(false) }
    var pendingReminderLead by rememberSaveable { mutableStateOf<String?>(null) }
    val endActions = listOfNotNull(
        pyeritePullRefreshTopBarAction(
            isRefreshing = uiState.isLoading,
            refreshFailed = uiState.loadFailed,
            onRefresh = viewModel::refresh,
        ),
        PyeriteTopBarActionItem(
            onClick = { showingReminders = true },
            icon = Icons.Outlined.Notifications,
            contentDescription = remindersDescription,
        ),
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val leadName = pendingReminderLead
        pendingReminderLead = null
        if (!granted) {
            Toast.makeText(
                context,
                reminderDenied,
                Toast.LENGTH_SHORT,
            ).show()
            return@rememberLauncherForActivityResult
        }
        val lead = leadName?.let { name ->
            runCatching { CalendarReminderLead.valueOf(name) }.getOrNull()
        } ?: return@rememberLauncherForActivityResult
        applyCalendarReminder(
            context = context,
            viewModel = viewModel,
            lead = lead,
            added = reminderAdded,
            duplicate = reminderDuplicate,
            past = reminderPast,
        )
    }

    fun tryAddReminder(lead: CalendarReminderLead) {
        if (!hasNotificationPermission(context)) {
            pendingReminderLead = lead.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            return
        }
        applyCalendarReminder(
            context = context,
            viewModel = viewModel,
            lead = lead,
            added = reminderAdded,
            duplicate = reminderDuplicate,
            past = reminderPast,
        )
    }

    PyeritePageScaffold(
        title = pageTitle,
        showCollapsedTitle = showCollapsedTitle,
        onBack = onBack,
        endActions = endActions,
    ) { topBarPadding ->
        PyeritePullToRefreshBox(
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(topBarPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = bottomPadding),
            ) {
                PageTitle(text = pageTitle)
                Spacer(modifier = Modifier.height(sectionGap))
                BaseContainer(title = null, useSystemBarsPadding = false) {
                    CalendarMonthSection(
                        year = uiState.visibleYear,
                        month = uiState.visibleMonth,
                        selectedDate = uiState.selectedDate,
                        eventCountOn = uiState::eventCountOn,
                        onSelectDate = viewModel::selectDate,
                        onShiftMonth = viewModel::shiftVisibleMonth,
                    )
                }
                Spacer(modifier = Modifier.height(sectionGap))
                CalendarDayEventsSection(
                    selectedDate = uiState.selectedDate,
                    events = uiState.selectedDayEvents,
                    detailsPending = !uiState.detailsReady,
                    loadFailed = uiState.loadFailed,
                    onEventClick = viewModel::openEvent,
                )
            }
        }
    }

    val openedEventId = uiState.openedEventId
    if (openedEventId != null) {
        val summary = uiState.events.firstOrNull { it.eventId == openedEventId }
        CalendarEventDetailSheet(
            summary = summary,
            detail = uiState.openedDetail,
            loading = uiState.detailLoading,
            failed = uiState.detailFailed,
            onDismiss = viewModel::dismissEvent,
            onAddReminder = { tryAddReminder(it) },
        )
    }

    if (showingReminders) {
        CalendarRemindersSheet(
            reminders = uiState.reminders,
            onRemove = viewModel::removeReminder,
            onDismiss = { showingReminders = false },
        )
    }
}

@Composable
private fun CalendarDayEventsSection(
    selectedDate: CalendarDate,
    events: List<CharacterCalendarEvent>,
    detailsPending: Boolean,
    loadFailed: Boolean,
    onEventClick: (Long) -> Unit,
) {
    val placeholder = stringResource(R.string.character_sheet_value_placeholder)
    BaseContainer(
        title = formatCalendarDayTitle(selectedDate),
        titleTrailingContent = {
            Text(
                text = stringResource(R.string.character_calendar_event_count, events.size),
                color = colorResource(R.color.text_caption),
                fontSize = dimensionResource(R.dimen.list_section_subheader_text_size).value.sp,
            )
        },
        useSystemBarsPadding = false,
    ) {
        when {
            detailsPending -> {
                BaseLazyColumnItem(
                    model = BaseLazyColumnItemModel(
                        showLeadingIcon = false,
                        itemName = placeholder,
                        showChevron = false,
                        onClick = null,
                    ),
                    showDivider = false,
                )
            }
            loadFailed && events.isEmpty() -> {
                BaseLazyColumnItem(
                    model = BaseLazyColumnItemModel(
                        showLeadingIcon = false,
                        itemName = stringResource(R.string.character_sheet_load_failed),
                        showChevron = false,
                        onClick = null,
                    ),
                    showDivider = false,
                )
            }
            events.isEmpty() -> {
                BaseLazyColumnItem(
                    model = BaseLazyColumnItemModel(
                        showLeadingIcon = false,
                        itemName = stringResource(R.string.character_calendar_empty_day),
                        showChevron = false,
                        onClick = null,
                    ),
                    showDivider = false,
                )
            }
            else -> {
                val nowEpochMs = System.currentTimeMillis()
                val pastColor = colorResource(R.color.text_caption)
                Column {
                    events.forEachIndexed { index, event ->
                        val time = formatCalendarDateTime(event.startEpochMs)
                        val response = calendarResponseLabel(event.response)
                        val expired = CalendarEventStatus.isExpired(event.startEpochMs, nowEpochMs)
                        BaseLazyColumnItem(
                            model = BaseLazyColumnItemModel(
                                showLeadingIcon = false,
                                itemName = event.title.ifBlank { placeholder },
                                itemNameBold = true,
                                itemNameColor = if (expired) pastColor else null,
                                itemHints = listOf(
                                    BaseLazyColumnItemHint(
                                        text = stringResource(
                                            R.string.character_calendar_event_hint,
                                            time,
                                            response,
                                        ),
                                        color = if (expired) pastColor else null,
                                    ),
                                ),
                                onClick = { onEventClick(event.eventId) },
                            ),
                            showDivider = index != events.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

private fun applyCalendarReminder(
    context: Context,
    viewModel: CharacterCalendarViewModel,
    lead: CalendarReminderLead,
    added: String,
    duplicate: String,
    past: String,
) {
    when (viewModel.addReminder(lead)) {
        CalendarAddReminderResult.ADDED -> {
            Toast.makeText(context, added, Toast.LENGTH_SHORT).show()
        }
        CalendarAddReminderResult.DUPLICATE -> {
            Toast.makeText(context, duplicate, Toast.LENGTH_SHORT).show()
        }
        CalendarAddReminderResult.FIRE_TIME_PASSED -> {
            Toast.makeText(context, past, Toast.LENGTH_SHORT).show()
        }
        CalendarAddReminderResult.EXACT_ALARM_DENIED -> {
            Toast.makeText(context, added, Toast.LENGTH_SHORT).show()
            context.startActivity(viewModel.exactAlarmSettingsIntent())
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}
