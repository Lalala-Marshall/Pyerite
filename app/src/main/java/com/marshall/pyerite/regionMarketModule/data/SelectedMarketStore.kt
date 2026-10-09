package com.marshall.pyerite.regionMarketModule.data

import android.content.Context
import androidx.core.content.edit
import com.marshall.pyerite.regionMarketModule.model.MarketSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Global selected market and the user's pinned places. */
internal class SelectedMarketStore(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _selection = MutableStateFlow(readSelection())
    val selection: StateFlow<MarketSelection> = _selection.asStateFlow()

    private val _pinned = MutableStateFlow(readPinned())
    val pinned: StateFlow<List<MarketSelection>> = _pinned.asStateFlow()

    fun select(selection: MarketSelection) {
        prefs.edit { putString(KEY_SELECTION, selection.persistKey) }
        _selection.value = selection
    }

    fun setPinned(places: List<MarketSelection>) {
        prefs.edit { putString(KEY_PINNED, places.joinToString(PINNED_SEPARATOR) { it.persistKey }) }
        _pinned.value = places
    }

    private fun readSelection(): MarketSelection =
        MarketSelection.parse(prefs.getString(KEY_SELECTION, null))

    private fun readPinned(): List<MarketSelection> {
        val raw = prefs.getString(KEY_PINNED, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(PINNED_SEPARATOR).map { MarketSelection.parse(it) }
    }

    private companion object {
        const val PREFS_NAME = "pyerite_region_market"
        const val KEY_SELECTION = "selected_market"
        const val KEY_PINNED = "pinned_markets"
        const val PINNED_SEPARATOR = "\n"
    }
}
