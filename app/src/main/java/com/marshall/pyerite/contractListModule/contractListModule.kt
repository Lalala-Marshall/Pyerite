package com.marshall.pyerite.contractListModule

import com.marshall.pyerite.contractListModule.data.ContractListLoader
import com.marshall.pyerite.contractListModule.viewModel.ContractListDetailViewModel
import com.marshall.pyerite.contractListModule.viewModel.ContractListRepository
import com.marshall.pyerite.contractListModule.viewModel.ContractListViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val contractListModule = module {
    singleOf(::ContractListLoader)
    singleOf(::ContractListRepository)
    viewModelOf(::ContractListViewModel)
    viewModelOf(::ContractListDetailViewModel)
}
