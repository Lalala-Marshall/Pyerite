package com.marshall.pyerite.infra.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.marshall.pyerite.R
import com.marshall.pyerite.infra.network.PyeriteJson
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.util.DurationDisplayFormatter
import com.marshall.pyerite.util.formatDurationDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer

/**
 * Schedules one notification per logged-in character when the last skill in the
 * pulled queue finishes within 24 hours. Does not call ESI.
 */
internal class SkillQueueExpiryScheduler(
    context: Context,
    private val notificationPreferences: NotificationPreferences,
    private val localeController: LocaleController,
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(
        SkillQueueExpiryConfig.PREFS_NAME,
        Context.MODE_PRIVATE,
    )
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    private var snapshots: Map<Long, SkillQueueExpirySnapshot> = readSnapshots()

    init {
        scope.launch {
            combine(
                notificationPreferences.master,
                notificationPreferences.skillQueueEnabled,
            ) { _, _ -> }
                .collect { syncAll() }
        }
    }

    fun report(characterId: Long, characterName: String, queueEndEpochMs: Long?) {
        synchronized(lock) {
            val previous = snapshots[characterId]
            val name = characterName.ifBlank { previous?.characterName.orEmpty() }
            val notified = previous
                ?.takeIf { it.queueEndEpochMs == queueEndEpochMs }
                ?.notifiedForQueueEndEpochMs
            val updated = SkillQueueExpirySnapshot(
                characterId = characterId,
                characterName = name,
                queueEndEpochMs = queueEndEpochMs,
                notifiedForQueueEndEpochMs = notified,
            )
            snapshots = snapshots + (characterId to updated)
            persistLocked()
            syncLocked(updated)
        }
    }

    fun clear(characterId: Long) {
        synchronized(lock) {
            cancelAlarm(characterId)
            cancelPosted(characterId)
            if (characterId !in snapshots) return
            snapshots = snapshots - characterId
            persistLocked()
        }
    }

    fun syncAll() {
        synchronized(lock) {
            snapshots.values.toList().forEach(::syncLocked)
        }
    }

    fun onAlarm(characterId: Long) {
        synchronized(lock) {
            val snapshot = snapshots[characterId] ?: return
            syncLocked(snapshot)
        }
    }

    private fun syncLocked(snapshot: SkillQueueExpirySnapshot) {
        cancelAlarm(snapshot.characterId)
        if (!allowsSkillQueueNotifications()) return
        val end = snapshot.queueEndEpochMs ?: return
        val now = System.currentTimeMillis()
        if (end <= now) return
        val notifyAt = end - SkillQueueExpiryConfig.WINDOW_MS
        if (now >= notifyAt) {
            if (snapshot.notifiedForQueueEndEpochMs == end) return
            if (post(snapshot, end, now)) {
                val updated = snapshot.copy(notifiedForQueueEndEpochMs = end)
                snapshots = snapshots + (snapshot.characterId to updated)
                persistLocked()
            }
        } else {
            scheduleAlarm(snapshot.characterId, notifyAt)
        }
    }

    private fun allowsSkillQueueNotifications(): Boolean {
        if (!notificationPreferences.skillQueueEnabled.value) return false
        return notificationPreferences.isMasterEnabled(NotificationAccess.isGranted(appContext))
    }

    @SuppressLint("MissingPermission")
    private fun post(snapshot: SkillQueueExpirySnapshot, queueEndEpochMs: Long, nowMs: Long): Boolean {
        ensureChannel()
        val remainingSeconds = (
            (queueEndEpochMs - nowMs) / SkillQueueExpiryConfig.MILLIS_PER_SECOND
            ).coerceAtLeast(0L)
        val remaining = formatDurationDisplay(
            totalSeconds = remainingSeconds,
            precision = DurationDisplayFormatter.Precision.MINUTE,
            language = localeController.resolvedAppContentLanguage(),
            maxUnit = DurationDisplayFormatter.MaxUnit.DAY,
        )
        val title = snapshot.characterName.ifBlank {
            appContext.getString(R.string.settings_skill_queue_notification_title_unnamed)
        }
        val body = appContext.getString(R.string.settings_skill_queue_notification_body, remaining)
        val notificationId = notificationId(snapshot.characterId)
        val launch = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
        val contentIntent = launch?.let { intent ->
            PendingIntent.getActivity(
                appContext,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val notification = NotificationCompat.Builder(appContext, SkillQueueExpiryConfig.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_character_skills)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        if (!NotificationAccess.isGranted(appContext)) return false
        return try {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun scheduleAlarm(characterId: Long, triggerAtEpochMs: Long) {
        val pending = pendingIntent(
            characterId = characterId,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMs, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMs, pending)
        }
    }

    private fun cancelAlarm(characterId: Long) {
        val existing = pendingIntent(
            characterId = characterId,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(existing)
        existing.cancel()
    }

    private fun cancelPosted(characterId: Long) {
        NotificationManagerCompat.from(appContext).cancel(notificationId(characterId))
    }

    private fun pendingIntent(characterId: Long, flags: Int): PendingIntent? {
        val intent = Intent(appContext, SkillQueueExpiryAlarmReceiver::class.java).apply {
            action = SkillQueueExpiryConfig.ACTION_EXPIRY
            putExtra(SkillQueueExpiryConfig.EXTRA_CHARACTER_ID, characterId)
        }
        return PendingIntent.getBroadcast(
            appContext,
            notificationId(characterId),
            intent,
            flags,
        )
    }

    private fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return alarmManager.canScheduleExactAlarms()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(SkillQueueExpiryConfig.CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                SkillQueueExpiryConfig.CHANNEL_ID,
                appContext.getString(R.string.settings_skill_queue_notification_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    }

    private fun persistLocked() {
        val encoded = PyeriteJson.encodeToString(
            ListSerializer(SkillQueueExpirySnapshot.serializer()),
            snapshots.values.toList(),
        )
        prefs.edit { putString(SkillQueueExpiryConfig.KEY_SNAPSHOTS, encoded) }
    }

    private fun readSnapshots(): Map<Long, SkillQueueExpirySnapshot> {
        val raw = prefs.getString(SkillQueueExpiryConfig.KEY_SNAPSHOTS, null) ?: return emptyMap()
        val decoded = runCatching {
            PyeriteJson.decodeFromString(
                ListSerializer(SkillQueueExpirySnapshot.serializer()),
                raw,
            )
        }.getOrDefault(emptyList())
        return decoded.associateBy { it.characterId }
    }

    companion object {
        fun notificationId(characterId: Long): Int {
            var hash = SkillQueueExpiryConfig.NOTIFICATION_ID_SEED
            hash = hash * SkillQueueExpiryConfig.NOTIFICATION_ID_MULTIPLIER + characterId.hashCode()
            return hash
        }
    }
}
