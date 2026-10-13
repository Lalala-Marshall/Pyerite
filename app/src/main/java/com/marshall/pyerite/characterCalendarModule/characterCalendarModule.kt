package com.marshall.pyerite.characterCalendarModule

import com.marshall.pyerite.characterCalendarModule.data.CalendarReminderScheduler
import com.marshall.pyerite.characterCalendarModule.data.CalendarReminderStore
import com.marshall.pyerite.characterCalendarModule.notification.CalendarNotificationCoordinator
import com.marshall.pyerite.characterCalendarModule.data.CharacterCalendarCache
import com.marshall.pyerite.characterCalendarModule.data.CharacterCalendarLoader
import com.marshall.pyerite.characterCalendarModule.viewModel.CharacterCalendarRepository
import com.marshall.pyerite.characterCalendarModule.viewModel.CharacterCalendarViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val characterCalendarModule = module {
    single { CharacterCalendarCache(androidContext()) }
    single { CalendarReminderStore(androidContext()) }
    single { CalendarReminderScheduler(androidContext(), get()) }
    single(createdAtStart = true) {
        CalendarNotificationCoordinator(
            preferences = get(),
            repository = get(),
        ).also(CalendarNotificationCoordinator::start)
    }
    singleOf(::CharacterCalendarLoader)
    singleOf(::CharacterCalendarRepository)
    viewModelOf(::CharacterCalendarViewModel)
}
