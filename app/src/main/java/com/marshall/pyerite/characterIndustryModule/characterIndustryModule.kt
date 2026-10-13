package com.marshall.pyerite.characterIndustryModule

import com.marshall.pyerite.characterIndustryModule.data.CharacterIndustryLoader
import com.marshall.pyerite.characterIndustryModule.data.CharacterIndustrySettingsStore
import com.marshall.pyerite.characterIndustryModule.viewModel.CharacterIndustryRepository
import com.marshall.pyerite.characterIndustryModule.viewModel.CharacterIndustryViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val characterIndustryModule = module {
    single { CharacterIndustrySettingsStore(androidContext()) }
    singleOf(::CharacterIndustryLoader)
    singleOf(::CharacterIndustryRepository)
    viewModelOf(::CharacterIndustryViewModel)
}
