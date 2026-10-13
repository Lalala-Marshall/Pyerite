package com.marshall.pyerite.infra.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.context.GlobalContext

internal class SkillQueueExpiryAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SkillQueueExpiryConfig.ACTION_EXPIRY) return
        val characterId = intent.getLongExtra(SkillQueueExpiryConfig.EXTRA_CHARACTER_ID, 0L)
        if (characterId == 0L) return
        val koin = GlobalContext.getOrNull() ?: return
        koin.get<SkillQueueExpiryScheduler>().onAlarm(characterId)
    }
}
