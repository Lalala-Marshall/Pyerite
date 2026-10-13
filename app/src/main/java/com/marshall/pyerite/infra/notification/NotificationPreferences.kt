package com.marshall.pyerite.infra.notification

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _master = MutableStateFlow(
        NotificationMasterChoice.fromStoredName(prefs.getString(KEY_MASTER, null)),
    )
    private val _calendarEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_CALENDAR, DEFAULT_ITEM_ENABLED),
    )
    private val _skillQueueEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_SKILL_QUEUE, DEFAULT_ITEM_ENABLED),
    )

    val master: StateFlow<NotificationMasterChoice> = _master.asStateFlow()
    val calendarEnabled: StateFlow<Boolean> = _calendarEnabled.asStateFlow()
    val skillQueueEnabled: StateFlow<Boolean> = _skillQueueEnabled.asStateFlow()

    fun isMasterEnabled(hasPermission: Boolean): Boolean = when (_master.value) {
        NotificationMasterChoice.DISABLED -> false
        NotificationMasterChoice.FOLLOW_PERMISSION,
        NotificationMasterChoice.ENABLED,
        -> hasPermission
    }

    fun setMaster(choice: NotificationMasterChoice) {
        if (_master.value == choice) return
        prefs.edit { putString(KEY_MASTER, choice.name) }
        _master.value = choice
    }

    fun setCalendarEnabled(enabled: Boolean) {
        if (_calendarEnabled.value == enabled) return
        prefs.edit { putBoolean(KEY_CALENDAR, enabled) }
        _calendarEnabled.value = enabled
    }

    fun setSkillQueueEnabled(enabled: Boolean) {
        if (_skillQueueEnabled.value == enabled) return
        prefs.edit { putBoolean(KEY_SKILL_QUEUE, enabled) }
        _skillQueueEnabled.value = enabled
    }

    fun hasRequestedPostNotifications(): Boolean =
        prefs.getBoolean(KEY_PERMISSION_REQUESTED, false)

    fun markPostNotificationsRequested() {
        prefs.edit { putBoolean(KEY_PERMISSION_REQUESTED, true) }
    }

    companion object {
        private const val PREFS_NAME = "pyerite_notification_settings"
        private const val KEY_MASTER = "master"
        private const val KEY_CALENDAR = "calendar_reminders"
        private const val KEY_SKILL_QUEUE = "skill_queue_expiry"
        private const val KEY_PERMISSION_REQUESTED = "post_notifications_requested"
        private const val DEFAULT_ITEM_ENABLED = true
    }
}
