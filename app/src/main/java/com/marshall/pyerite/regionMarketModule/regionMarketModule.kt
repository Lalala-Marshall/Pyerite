package com.marshall.pyerite.regionMarketModule

import com.marshall.pyerite.regionMarketModule.data.MarketQuoteCache
import com.marshall.pyerite.regionMarketModule.data.MarketRepository
import com.marshall.pyerite.regionMarketModule.data.MarketStructureStore
import com.marshall.pyerite.regionMarketModule.data.SelectedMarketStore
import com.marshall.pyerite.regionMarketModule.viewModel.MarketBrowserViewModel
import com.marshall.pyerite.regionMarketModule.viewModel.MarketDetailViewModel
import com.marshall.pyerite.regionMarketModule.viewModel.MarketLocationViewModel
import com.marshall.pyerite.regionMarketModule.viewModel.MarketOrdersViewModel
import com.marshall.pyerite.regionMarketModule.viewModel.MarketTypeListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val regionMarketModule = module {
    single { SelectedMarketStore(androidContext()) }
    single { MarketStructureStore(androidContext()) }
    single { MarketQuoteCache() }
    single {
        MarketRepository(
            roomProvider = get(),
            localeController = get(),
            marketApi = get(),
            characterApi = get(),
            universeApi = get(),
            tokenManager = get(),
            structureStore = get(),
            quoteCache = get(),
        )
    }
    viewModel { (parentGroupId: Int) ->
        MarketBrowserViewModel(parentGroupId = parentGroupId, repository = get())
    }
    viewModel { (marketGroupId: Int) ->
        MarketTypeListViewModel(marketGroupId = marketGroupId, repository = get())
    }
    viewModel { (typeId: Int) ->
        MarketDetailViewModel(
            typeId = typeId,
            repository = get(),
            selectionStore = get(),
            localeController = get(),
        )
    }
    viewModel { (typeId: Int) ->
        MarketOrdersViewModel(
            typeId = typeId,
            repository = get(),
            selectionStore = get(),
            localeController = get(),
        )
    }
    viewModel {
        MarketLocationViewModel(
            repository = get(),
            selectionStore = get(),
            structureStore = get(),
            tokenManager = get(),
            localeController = get(),
        )
    }
}
