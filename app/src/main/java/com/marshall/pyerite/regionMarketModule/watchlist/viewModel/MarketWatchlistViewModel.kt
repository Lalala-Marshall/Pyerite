package com.marshall.pyerite.regionMarketModule.watchlist.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.localization.ContentLanguage
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.regionMarketModule.data.SelectedMarketStore
import com.marshall.pyerite.regionMarketModule.model.MarketSelection
import com.marshall.pyerite.regionMarketModule.watchlist.data.MarketWatchlistRepository
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class MarketWatchlistRowUi(
    val id: String,
    val title: String,
    val locationName: String,
    val iconFileName: String?,
)

internal class MarketWatchlistViewModel(
    private val repository: MarketWatchlistRepository,
    private val selectionStore: SelectedMarketStore,
    private val localeController: LocaleController,
) : ViewModel() {
    private val _rows = MutableStateFlow<List<MarketWatchlistRowUi>>(emptyList())
    val rows: StateFlow<List<MarketWatchlistRowUi>> = _rows.asStateFlow()

    private val reloadMutex = Mutex()
    private var loadedLanguage: ContentLanguage? = null

    init {
        viewModelScope.launch {
            repository.lists.collect { lists ->
                reload(lists)
            }
        }
    }

    fun onContentLanguage(language: ContentLanguage) {
        if (language == loadedLanguage) return
        viewModelScope.launch { reload(repository.lists.value) }
    }

    fun create(title: String): String = repository.create(
        title = title,
        marketKey = selectionStore.selection.value.persistKey,
    )

    fun rename(id: String, title: String) {
        repository.rename(id, title)
    }

    fun delete(id: String) {
        repository.delete(id)
    }

    private suspend fun reload(lists: List<MarketWatchlist>) {
        reloadMutex.withLock {
            loadedLanguage = localeController.contentLanguage
            val firstIds = lists.mapNotNull { it.items.firstOrNull()?.typeId }
            val icons = repository.typesByIds(firstIds).associate { it.id to it.iconFilename }
            _rows.value = lists.map { list ->
                MarketWatchlistRowUi(
                    id = list.id,
                    title = list.title,
                    locationName = repository.locationName(MarketSelection.parse(list.marketKey)),
                    iconFileName = list.items.firstOrNull()?.typeId?.let { icons[it] },
                )
            }
        }
    }
}
