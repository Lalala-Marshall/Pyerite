package com.marshall.pyerite.agentsModule

import com.marshall.pyerite.agentsModule.data.AgentsRepository
import com.marshall.pyerite.agentsModule.viewModel.AgentsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val agentsModule = module {
    single { AgentsRepository(get()) }
    viewModel { AgentsViewModel(get(), get(), get()) }
}
