package com.marshall.pyerite.esiModule

import com.marshall.pyerite.esiModule.api.EsiAllianceApi
import com.marshall.pyerite.esiModule.api.EsiCharacterApi
import com.marshall.pyerite.esiModule.api.EsiCorporationApi
import com.marshall.pyerite.esiModule.api.EsiMarketApi
import com.marshall.pyerite.esiModule.api.EsiUniverseApi
import com.marshall.pyerite.esiModule.data.CharacterAssetListCache
import com.marshall.pyerite.esiModule.data.EsiPublicDataSource
import com.marshall.pyerite.esiModule.http.EsiConfig
import com.marshall.pyerite.esiModule.http.EsiHttp
import com.marshall.pyerite.esiModule.http.createEsiOkHttpClient
import com.marshall.pyerite.infra.network.createApi
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val esiModule = module {
    single { EsiHttp(createEsiOkHttpClient(get())) }
    single { get<EsiHttp>().client.createApi<EsiCharacterApi>(EsiConfig.BASE_URL) }
    single { get<EsiHttp>().client.createApi<EsiUniverseApi>(EsiConfig.BASE_URL) }
    single { get<EsiHttp>().client.createApi<EsiCorporationApi>(EsiConfig.BASE_URL) }
    single { get<EsiHttp>().client.createApi<EsiAllianceApi>(EsiConfig.BASE_URL) }
    single { get<EsiHttp>().client.createApi<EsiMarketApi>(EsiConfig.BASE_URL) }
    singleOf(::EsiPublicDataSource)
    singleOf(::CharacterAssetListCache)
}
