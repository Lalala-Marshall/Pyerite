package com.marshall.pyerite.contractsCommon

import com.marshall.pyerite.contractsCommon.data.ContractContextResolver
import com.marshall.pyerite.contractsCommon.model.ContractsListSettingsStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val contractsCommonModule = module {
    singleOf(::ContractsListSettingsStore)
    singleOf(::ContractContextResolver)
}
