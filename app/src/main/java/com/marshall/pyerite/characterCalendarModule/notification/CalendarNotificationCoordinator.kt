package com.marshall.pyerite.characterCalendarModule.notification

import com.marshall.pyerite.characterCalendarModule.viewModel.CharacterCalendarRepository
import com.marshall.pyerite.infra.notification.NotificationPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class CalendarNotificationCoordinator(
    private val preferences: NotificationPreferences,
    private val repository: CharacterCalendarRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            combine(
                preferences.master,
                preferences.calendarEnabled,
            ) { _, _ -> }
                .collect {
                    repository.reschedulePendingReminders()
                }
        }
    }
}
