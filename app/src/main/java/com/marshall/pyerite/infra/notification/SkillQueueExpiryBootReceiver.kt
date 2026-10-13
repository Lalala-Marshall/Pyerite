package com.marshall.pyerite.infra.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.context.GlobalContext

internal class SkillQueueExpiryBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULE_ACTIONS) return
        val koin = GlobalContext.getOrNull() ?: return
        koin.get<SkillQueueExpiryScheduler>().syncAll()
    }

    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
        )
    }
}
