package com.marshall.pyerite.characterCalendarModule.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.marshall.pyerite.R
import com.marshall.pyerite.characterCalendarModule.data.CalendarReminderScheduler
import com.marshall.pyerite.characterCalendarModule.data.CalendarReminderStore
import com.marshall.pyerite.characterCalendarModule.model.CalendarReminderLead
import org.koin.core.context.GlobalContext

internal class CalendarAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CalendarNotificationConfig.ACTION_REMINDER) return
        val characterId = intent.getLongExtra(CalendarNotificationConfig.EXTRA_CHARACTER_ID, 0L)
        val eventId = intent.getLongExtra(CalendarNotificationConfig.EXTRA_EVENT_ID, 0L)
        val leadName = intent.getStringExtra(CalendarNotificationConfig.EXTRA_LEAD).orEmpty()
        val title = intent.getStringExtra(CalendarNotificationConfig.EXTRA_TITLE).orEmpty()
        val lead = runCatching { CalendarReminderLead.valueOf(leadName) }.getOrNull()
            ?: return

        val koin = GlobalContext.getOrNull() ?: return
        val store = koin.get<CalendarReminderStore>()
        val scheduler = koin.get<CalendarReminderScheduler>()
        if (!scheduler.canPost()) return
        store.remove(characterId, eventId, lead)
        scheduler.ensureChannel()

        val tapIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = tapIntent?.let { launch ->
            PendingIntent.getActivity(
                context,
                CalendarReminderScheduler.requestCode(characterId, eventId, leadName),
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val notification = NotificationCompat.Builder(context, CalendarNotificationConfig.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_character_calendar)
            .setContentTitle(title)
            .setContentText(leadBody(context, lead))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        postReminderNotification(
            context = context,
            notificationId = CalendarReminderScheduler.requestCode(characterId, eventId, leadName),
            notification = notification,
        )
    }

    @SuppressLint("MissingPermission")
    private fun postReminderNotification(
        context: Context,
        notificationId: Int,
        notification: Notification,
    ) {
        if (!canPostNotifications(context)) return
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            return
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun leadBody(context: Context, lead: CalendarReminderLead): String {
        val res = when (lead) {
            CalendarReminderLead.TWO_HOURS -> R.string.character_calendar_notification_body_two_hours
            CalendarReminderLead.ONE_HOUR -> R.string.character_calendar_notification_body_one_hour
            CalendarReminderLead.THIRTY_MINUTES ->
                R.string.character_calendar_notification_body_thirty_minutes
            CalendarReminderLead.FIFTEEN_MINUTES ->
                R.string.character_calendar_notification_body_fifteen_minutes
            CalendarReminderLead.AT_START -> R.string.character_calendar_notification_body_at_start
        }
        return context.getString(res)
    }
}
