package com.marshall.pyerite.regionMarketModule.watchlist.data

import android.content.Context
import androidx.core.content.edit
import com.marshall.pyerite.infra.network.PyeriteJson
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlist
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-wide market watchlists. Shared by every signed-in character. */
internal class MarketWatchlistStore(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(
        MarketWatchlistConfig.PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    private val _lists = MutableStateFlow(read())
    val lists: StateFlow<List<MarketWatchlist>> = _lists.asStateFlow()

    fun save(lists: List<MarketWatchlist>) {
        val encoded = PyeriteJson.encodeToString(lists)
        prefs.edit(commit = true) {
            putString(MarketWatchlistConfig.KEY_LISTS, encoded)
        }
        _lists.value = lists
    }

    private fun read(): List<MarketWatchlist> {
        val raw = prefs.getString(MarketWatchlistConfig.KEY_LISTS, null) ?: return emptyList()
        return runCatching {
            PyeriteJson.decodeFromString<List<MarketWatchlist>>(raw)
        }.getOrElse { emptyList() }
    }
}
