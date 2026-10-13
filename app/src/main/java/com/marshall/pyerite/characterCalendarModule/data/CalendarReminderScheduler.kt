package com.marshall.pyerite.characterCalendarModule.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.marshall.pyerite.R
import com.marshall.pyerite.characterCalendarModule.model.CalendarReminder
import com.marshall.pyerite.characterCalendarModule.notification.CalendarAlarmReceiver
import com.marshall.pyerite.characterCalendarModule.notification.CalendarNotificationConfig
import com.marshall.pyerite.infra.notification.NotificationAccess
import com.marshall.pyerite.infra.notification.NotificationPreferences

internal class CalendarReminderScheduler(
    context: Context,
    private val notificationPreferences: NotificationPreferences,
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return alarmManager.canScheduleExactAlarms()
    }

    fun exactAlarmSettingsIntent(): Intent {
        val data = "${CalendarNotificationConfig.PACKAGE_URI_PREFIX}${appContext.packageName}".toUri()
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(CalendarNotificationConfig.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        }
        return intent.apply {
            this.data = data
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun schedule(reminder: CalendarReminder) {
        if (!allowsCalendarNotifications()) {
            cancel(reminder)
            return
        }
        ensureChannel()
        val pending = requireNotNull(
            pendingIntent(
                reminder = reminder,
                flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.fireAtEpochMs,
                pending,
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.fireAtEpochMs,
                pending,
            )
        }
    }

    fun cancel(reminder: CalendarReminder) {
        val existing = pendingIntent(
            reminder = reminder,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(existing)
        existing.cancel()
    }

    fun reschedule(reminders: List<CalendarReminder>) {
        if (!allowsCalendarNotifications()) {
            reminders.forEach(::cancel)
            return
        }
        ensureChannel()
        reminders.forEach(::schedule)
    }

    private fun allowsCalendarNotifications(): Boolean {
        if (!notificationPreferences.calendarEnabled.value) return false
        return notificationPreferences.isMasterEnabled(NotificationAccess.isGranted(appContext))
    }

    fun canPost(): Boolean = allowsCalendarNotifications()

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CalendarNotificationConfig.CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CalendarNotificationConfig.CHANNEL_ID,
                appContext.getString(R.string.character_calendar_notification_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    }

    private fun pendingIntent(reminder: CalendarReminder, flags: Int): PendingIntent? {
        val intent = Intent(appContext, CalendarAlarmReceiver::class.java).apply {
            action = CalendarNotificationConfig.ACTION_REMINDER
            putExtra(CalendarNotificationConfig.EXTRA_CHARACTER_ID, reminder.characterId)
            putExtra(CalendarNotificationConfig.EXTRA_EVENT_ID, reminder.eventId)
            putExtra(CalendarNotificationConfig.EXTRA_LEAD, reminder.lead.name)
            putExtra(CalendarNotificationConfig.EXTRA_TITLE, reminder.eventTitle)
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode(reminder),
            intent,
            flags,
        )
    }

    companion object {
        fun requestCode(reminder: CalendarReminder): Int = requestCode(
            characterId = reminder.characterId,
            eventId = reminder.eventId,
            leadName = reminder.lead.name,
        )

        fun requestCode(characterId: Long, eventId: Long, leadName: String): Int {
            var hash = REQUEST_CODE_SEED
            hash = hash * REQUEST_CODE_MULTIPLIER + characterId.hashCode()
            hash = hash * REQUEST_CODE_MULTIPLIER + eventId.hashCode()
            hash = hash * REQUEST_CODE_MULTIPLIER + leadName.hashCode()
            return hash
        }

        private const val REQUEST_CODE_SEED = 17
        private const val REQUEST_CODE_MULTIPLIER = 31
    }
}
