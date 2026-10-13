package com.marshall.pyerite.infra.notification

import com.marshall.pyerite.util.DurationDisplayFormatter

internal object SkillQueueExpiryConfig {
    const val CHANNEL_ID = "skill_queue_expiry"
    const val ACTION_EXPIRY = "com.marshall.pyerite.SKILL_QUEUE_EXPIRY"
    const val EXTRA_CHARACTER_ID = "character_id"
    const val PREFS_NAME = "pyerite_skill_queue_expiry"
    const val KEY_SNAPSHOTS = "snapshots"
    const val MILLIS_PER_SECOND = 1_000L
    const val NOTIFICATION_ID_SEED = 23
    const val NOTIFICATION_ID_MULTIPLIER = 31

    val WINDOW_MS: Long =
        DurationDisplayFormatter.HOURS_PER_DAY.toLong() *
            DurationDisplayFormatter.SECONDS_PER_HOUR *
            MILLIS_PER_SECOND
}
