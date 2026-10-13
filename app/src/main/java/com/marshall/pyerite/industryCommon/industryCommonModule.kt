package com.marshall.pyerite.industryCommon

import com.marshall.pyerite.industryCommon.data.IndustryJobEnricher
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val industryCommonModule = module {
    singleOf(::IndustryJobEnricher)
}
