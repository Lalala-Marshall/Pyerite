package com.marshall.pyerite.characterWalletJournalModule

import com.marshall.pyerite.characterWalletJournalModule.data.CharacterWalletJournalDiskCache
import com.marshall.pyerite.characterWalletJournalModule.data.CharacterWalletJournalLoader
import com.marshall.pyerite.characterWalletJournalModule.viewModel.CharacterWalletJournalDayViewModel
import com.marshall.pyerite.characterWalletJournalModule.viewModel.CharacterWalletJournalRepository
import com.marshall.pyerite.characterWalletJournalModule.viewModel.CharacterWalletJournalViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val characterWalletJournalModule = module {
    singleOf(::CharacterWalletJournalLoader)
    single { CharacterWalletJournalDiskCache(androidContext()) }
    singleOf(::CharacterWalletJournalRepository)
    viewModelOf(::CharacterWalletJournalViewModel)
    viewModelOf(::CharacterWalletJournalDayViewModel)
}
