package com.marshall.pyerite.infra

import com.marshall.pyerite.infra.notification.NotificationPreferences
import com.marshall.pyerite.infra.notification.SkillQueueExpiryScheduler
import com.marshall.pyerite.infra.prefs.HomeLayoutPreferences
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.ui.theme.ThemeModeStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** App-wide non-domain singletons (locale, theme, notification prefs). */
val appModule = module {
    single { LocaleController(androidContext()) }
    single { ThemeModeStore(androidContext()) }
    single { HomeLayoutPreferences(androidContext()) }
    single { NotificationPreferences(androidContext()) }
    single(createdAtStart = true) {
        SkillQueueExpiryScheduler(
            context = androidContext(),
            notificationPreferences = get(),
            localeController = get(),
        )
    }
}
