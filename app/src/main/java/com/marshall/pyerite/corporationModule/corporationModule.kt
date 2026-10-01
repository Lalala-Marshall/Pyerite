package com.marshall.pyerite.corporationModule

import com.marshall.pyerite.corporationModule.industry.data.CorporationIndustryLoader
import com.marshall.pyerite.corporationModule.industry.viewModel.CorporationIndustryRepository
import com.marshall.pyerite.corporationModule.industry.viewModel.CorporationIndustryViewModel
import com.marshall.pyerite.corporationModule.members.data.CorporationMemberWatchStore
import com.marshall.pyerite.corporationModule.members.data.CorporationMembersLoader
import com.marshall.pyerite.corporationModule.members.viewModel.CorporationMembersRepository
import com.marshall.pyerite.corporationModule.members.viewModel.CorporationMembersViewModel
import com.marshall.pyerite.corporationModule.structures.data.CorporationStructureFuelMonitorStore
import com.marshall.pyerite.corporationModule.structures.data.CorporationStructuresLoader
import com.marshall.pyerite.corporationModule.structures.viewModel.CorporationStructuresRepository
import com.marshall.pyerite.corporationModule.structures.viewModel.CorporationStructuresViewModel
import com.marshall.pyerite.corporationModule.wallet.data.CorporationWalletLoader
import com.marshall.pyerite.corporationModule.wallet.viewModel.CorporationWalletDivisionViewModel
import com.marshall.pyerite.corporationModule.wallet.viewModel.CorporationWalletJournalDayViewModel
import com.marshall.pyerite.corporationModule.wallet.viewModel.CorporationWalletRepository
import com.marshall.pyerite.corporationModule.wallet.viewModel.CorporationWalletTransactionDayViewModel
import com.marshall.pyerite.corporationModule.wallet.viewModel.CorporationWalletsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val corporationModule = module {
    single { CorporationMemberWatchStore(androidContext()) }
    single { CorporationStructureFuelMonitorStore(androidContext()) }
    singleOf(::CorporationMembersLoader)
    singleOf(::CorporationMembersRepository)
    viewModelOf(::CorporationMembersViewModel)
    singleOf(::CorporationStructuresLoader)
    singleOf(::CorporationStructuresRepository)
    viewModelOf(::CorporationStructuresViewModel)
    singleOf(::CorporationIndustryLoader)
    singleOf(::CorporationIndustryRepository)
    viewModelOf(::CorporationIndustryViewModel)
    singleOf(::CorporationWalletLoader)
    singleOf(::CorporationWalletRepository)
    viewModelOf(::CorporationWalletsViewModel)
    viewModelOf(::CorporationWalletDivisionViewModel)
    viewModelOf(::CorporationWalletJournalDayViewModel)
    viewModelOf(::CorporationWalletTransactionDayViewModel)
}
