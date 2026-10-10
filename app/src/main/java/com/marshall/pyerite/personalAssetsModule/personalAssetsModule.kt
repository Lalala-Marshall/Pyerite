package com.marshall.pyerite.personalAssetsModule

import com.marshall.pyerite.personalAssetsModule.data.PersonalAssetsDiskCache
import com.marshall.pyerite.personalAssetsModule.data.PersonalAssetsLoader
import com.marshall.pyerite.personalAssetsModule.data.PersonalAssetsSettingsStore
import com.marshall.pyerite.personalAssetsModule.viewModel.PersonalAssetContainerViewModel
import com.marshall.pyerite.personalAssetsModule.viewModel.PersonalAssetLocationViewModel
import com.marshall.pyerite.personalAssetsModule.viewModel.PersonalAssetsCoordinator
import com.marshall.pyerite.personalAssetsModule.viewModel.PersonalAssetsRepository
import com.marshall.pyerite.personalAssetsModule.viewModel.PersonalAssetsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val personalAssetsModule = module {
    singleOf(::PersonalAssetsLoader)
    single { PersonalAssetsDiskCache(androidContext()) }
    single { PersonalAssetsSettingsStore(androidContext()) }
    singleOf(::PersonalAssetsRepository)
    singleOf(::PersonalAssetsCoordinator)
    viewModelOf(::PersonalAssetsViewModel)
    viewModelOf(::PersonalAssetLocationViewModel)
    viewModelOf(::PersonalAssetContainerViewModel)
}
