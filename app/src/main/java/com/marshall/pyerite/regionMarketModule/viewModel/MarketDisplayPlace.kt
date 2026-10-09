package com.marshall.pyerite.regionMarketModule.viewModel

import com.marshall.pyerite.regionMarketModule.data.SelectedMarketStore
import com.marshall.pyerite.regionMarketModule.model.MarketSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Market place shown on a detail or orders page.
 * With no initial key, the page follows [SelectedMarketStore].
 * With a key, it stays on that place until [followGlobal].
 */
internal class MarketDisplayPlace(
    initialPlaceKey: String?,
    private val selectionStore: SelectedMarketStore,
) {
    var followingGlobal: Boolean = initialPlaceKey.isNullOrBlank()
        private set

    var selection: MarketSelection = if (followingGlobal) {
        selectionStore.selection.value
    } else {
        MarketSelection.parse(initialPlaceKey)
    }
        private set

    private var followJob: Job? = null

    fun bind(scope: CoroutineScope, onPlace: (MarketSelection) -> Unit) {
        if (followingGlobal) {
            collect(scope, onPlace)
        } else {
            onPlace(selection)
        }
    }

    fun followGlobal(scope: CoroutineScope, onPlace: (MarketSelection) -> Unit) {
        if (followJob != null) return
        followingGlobal = true
        collect(scope, onPlace)
    }

    fun refreshSelection(): MarketSelection = if (followingGlobal) {
        selectionStore.selection.value
    } else {
        selection
    }

    private fun collect(scope: CoroutineScope, onPlace: (MarketSelection) -> Unit) {
        if (followJob != null) return
        followJob = scope.launch {
            selectionStore.selection.collect { next ->
                selection = next
                followingGlobal = true
                onPlace(next)
            }
        }
    }
}
