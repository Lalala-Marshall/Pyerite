package com.marshall.pyerite.characterTransactionsModule

import com.marshall.pyerite.characterTransactionsModule.data.CharacterTransactionsDiskCache
import com.marshall.pyerite.characterTransactionsModule.data.CharacterTransactionsLoader
import com.marshall.pyerite.characterTransactionsModule.viewModel.CharacterTransactionDayViewModel
import com.marshall.pyerite.characterTransactionsModule.viewModel.CharacterTransactionsRepository
import com.marshall.pyerite.characterTransactionsModule.viewModel.CharacterTransactionsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val characterTransactionsModule = module {
    singleOf(::CharacterTransactionsLoader)
    single { CharacterTransactionsDiskCache(androidContext()) }
    singleOf(::CharacterTransactionsRepository)
    viewModelOf(::CharacterTransactionsViewModel)
    viewModelOf(::CharacterTransactionDayViewModel)
}
