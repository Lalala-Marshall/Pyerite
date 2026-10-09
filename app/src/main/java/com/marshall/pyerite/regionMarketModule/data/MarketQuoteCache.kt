package com.marshall.pyerite.regionMarketModule.data

import com.marshall.pyerite.regionMarketModule.model.MarketQuote
import com.marshall.pyerite.regionMarketModule.model.MarketSelection
import java.util.concurrent.ConcurrentHashMap

/** In-memory book so the orders page can reuse orders already loaded for the detail page. */
internal class MarketQuoteCache {
    private val quotes = ConcurrentHashMap<String, MarketQuote>()

    fun key(typeId: Int, selection: MarketSelection): String = "$typeId|${selection.persistKey}"

    fun get(typeId: Int, selection: MarketSelection): MarketQuote? = quotes[key(typeId, selection)]

    fun put(typeId: Int, selection: MarketSelection, quote: MarketQuote) {
        quotes[key(typeId, selection)] = quote
    }
}
