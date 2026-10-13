package com.marshall.pyerite.settingsModule

import com.marshall.pyerite.settingsModule.data.AppCacheCleaner
import com.marshall.pyerite.settingsModule.viewModel.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule = module {
    single { AppCacheCleaner(androidContext()) }
    viewModelOf(::SettingsViewModel)
}
