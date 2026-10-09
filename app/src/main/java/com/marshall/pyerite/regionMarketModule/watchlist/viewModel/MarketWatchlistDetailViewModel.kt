package com.marshall.pyerite.regionMarketModule.watchlist.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.regionMarketModule.model.MarketQuote
import com.marshall.pyerite.regionMarketModule.model.MarketSelection
import com.marshall.pyerite.regionMarketModule.watchlist.data.MarketWatchlistRepository
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchItem
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchOrderSide
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlist
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistConfig
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistExportResult
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistImportMode
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistImportResult
import com.marshall.pyerite.regionMarketModule.watchlist.model.sanitizeWatchlistQuantityInput
import com.marshall.pyerite.sdeModule.room.type.TypeEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal data class MarketWatchlistItemUi(
    val typeId: Int,
    val name: String,
    val iconFileName: String?,
    val quantity: Long,
    val unitPrice: Double?,
    val lineTotal: Double?,
    val unitVolume: Double,
)

internal data class MarketWatchlistDetailUi(
    val missing: Boolean = false,
    val title: String = "",
    val locationName: String = "",
    val marketKey: String = "",
    val orderSide: MarketWatchOrderSide = MarketWatchOrderSide.SELL,
    val editingQuantities: Boolean = false,
    val quantityDrafts: Map<Int, String> = emptyMap(),
    val items: List<MarketWatchlistItemUi> = emptyList(),
    val marketPrice: Double? = null,
    val totalVolume: Double? = null,
    val loadingPrices: Boolean = false,
    val priceFailed: Boolean = false,
)

internal class MarketWatchlistDetailViewModel(
    private val listId: String,
    private val repository: MarketWatchlistRepository,
    private val localeController: LocaleController,
) : ViewModel() {
    private val _ui = MutableStateFlow(MarketWatchlistDetailUi())
    val ui: StateFlow<MarketWatchlistDetailUi> = _ui.asStateFlow()

    private var quoteJob: Job? = null
    private var quoteSignature: QuoteSignature? = null
    private var editing = false
    private var drafts: Map<Int, String> = emptyMap()
    private var publishToken = 0

    init {
        viewModelScope.launch {
            repository.lists.collect { lists ->
                publish(lists.find { it.id == listId })
            }
        }
    }

    fun onContentLanguageChanged() {
        publish(repository.list(listId), reloadQuotes = false)
    }

    fun refresh() {
        val list = repository.list(listId) ?: return
        reloadQuotes(list, forceRefresh = true)
    }

    fun setMarket(selection: MarketSelection) {
        repository.update(listId) { it.copy(marketKey = selection.persistKey) }
    }

    fun setOrderSide(side: MarketWatchOrderSide) {
        repository.update(listId) { it.copy(orderSide = side) }
    }

    fun beginEditing() {
        val list = repository.list(listId) ?: return
        editing = true
        drafts = list.items.associate { it.typeId to it.quantity.toString() }
        publish(list, reloadQuotes = false)
    }

    fun updateQuantityDraft(typeId: Int, raw: String) {
        val sanitized = sanitizeWatchlistQuantityInput(raw) ?: return
        drafts = drafts + (typeId to sanitized)
        _ui.value = _ui.value.copy(quantityDrafts = drafts)
    }

    fun finishEditing() {
        val list = repository.list(listId) ?: return
        val next = list.items.mapNotNull { item ->
            val quantity = resolvedQuantity(item) ?: return@mapNotNull null
            item.copy(quantity = quantity)
        }
        editing = false
        drafts = emptyMap()
        // An unchanged list does not emit, so the collector would leave the page in edit mode.
        publishToken++
        applyItems(next)
        repository.update(listId) { it.copy(items = next) }
    }

    fun removeItem(typeId: Int) {
        drafts = drafts - typeId
        publishToken++
        val next = (repository.list(listId)?.items ?: return).filter { it.typeId != typeId }
        applyItems(next)
        repository.update(listId) { list ->
            list.copy(items = list.items.filter { it.typeId != typeId })
        }
    }

    /** Blank input keeps the previous quantity. Zero removes the item. */
    private fun resolvedQuantity(item: MarketWatchItem): Long? {
        val raw = drafts[item.typeId]
        if (raw.isNullOrEmpty()) return item.quantity
        val parsed = raw.toLongOrNull() ?: return item.quantity
        if (parsed <= 0L) return null
        return parsed
    }

    private fun applyItems(next: List<MarketWatchItem>) {
        val quantities = next.associate { it.typeId to it.quantity }
        val items = _ui.value.items.mapNotNull { uiItem ->
            val quantity = quantities[uiItem.typeId] ?: return@mapNotNull null
            uiItem.copy(
                quantity = quantity,
                lineTotal = uiItem.unitPrice?.times(quantity.toDouble()),
            )
        }
        _ui.value = _ui.value.copy(
            editingQuantities = editing,
            quantityDrafts = if (editing) drafts else emptyMap(),
            items = items,
            marketPrice = pricedTotal(items),
            totalVolume = if (items.isEmpty()) null else items.sumOf { it.unitVolume * it.quantity },
        )
    }

    fun addTypes(typeIds: List<Int>) {
        repository.addItems(listId, typeIds)
    }

    suspend fun exportText(): MarketWatchlistExportResult = repository.exportText(listId)

    suspend fun prepareImport(raw: String?): MarketWatchlistImportResult =
        repository.prepareImport(raw)

    fun applyImport(items: List<MarketWatchItem>, mode: MarketWatchlistImportMode) {
        editing = false
        drafts = emptyMap()
        publishToken++
        _ui.value = _ui.value.copy(editingQuantities = false, quantityDrafts = emptyMap())
        repository.applyImport(listId, items, mode)
    }

    private fun publish(list: MarketWatchlist?, reloadQuotes: Boolean = true) {
        if (list == null) {
            quoteJob?.cancel()
            _ui.value = MarketWatchlistDetailUi(missing = true)
            return
        }
        val token = ++publishToken
        viewModelScope.launch {
            val types = repository.typesByIds(list.items.map { it.typeId }).associateBy { it.id }
            if (token != publishToken) return@launch
            val previous = _ui.value.items.associateBy { it.typeId }
            val sameQuoteContext = list.marketKey == _ui.value.marketKey &&
                list.orderSide == _ui.value.orderSide
            val signature = QuoteSignature(
                marketKey = list.marketKey,
                orderSide = list.orderSide,
                typeIds = list.items.map { it.typeId },
            )
            val items = list.items.map { item ->
                itemUi(
                    item = item,
                    type = types[item.typeId],
                    previous = if (sameQuoteContext) previous[item.typeId] else null,
                )
            }
            val locationName = repository.locationName(MarketSelection.parse(list.marketKey))
            if (token != publishToken) return@launch
            val draftsNow = if (editing) {
                drafts + list.items
                    .filter { it.typeId !in drafts }
                    .associate { it.typeId to it.quantity.toString() }
            } else {
                emptyMap()
            }
            if (token != publishToken) return@launch
            drafts = draftsNow
            _ui.value = MarketWatchlistDetailUi(
                title = list.title,
                locationName = locationName,
                marketKey = list.marketKey,
                orderSide = list.orderSide,
                editingQuantities = editing,
                quantityDrafts = draftsNow,
                items = items,
                marketPrice = pricedTotal(items),
                totalVolume = if (items.isEmpty()) null else items.sumOf { it.unitVolume * it.quantity },
                loadingPrices = _ui.value.loadingPrices && sameQuoteContext,
                priceFailed = _ui.value.priceFailed && sameQuoteContext,
            )
            if (reloadQuotes && signature != quoteSignature) {
                reloadQuotes(list, forceRefresh = false)
            }
        }
    }

    private fun reloadQuotes(list: MarketWatchlist, forceRefresh: Boolean) {
        val signature = QuoteSignature(
            marketKey = list.marketKey,
            orderSide = list.orderSide,
            typeIds = list.items.map { it.typeId },
        )
        quoteSignature = signature
        quoteJob?.cancel()
        if (list.items.isEmpty()) {
            _ui.value = _ui.value.copy(loadingPrices = false, priceFailed = false, marketPrice = null)
            return
        }
        quoteJob = viewModelScope.launch {
            _ui.value = _ui.value.copy(loadingPrices = true, priceFailed = false)
            val selection = MarketSelection.parse(list.marketKey)
            val semaphore = Semaphore(MarketWatchlistConfig.QUOTE_PARALLELISM)
            val results = coroutineScope {
                list.items.map { item ->
                    async {
                        semaphore.withPermit {
                            item.typeId to runCatching {
                                repository.loadQuote(
                                    typeId = item.typeId,
                                    selection = selection,
                                    forceRefresh = forceRefresh,
                                )
                            }
                        }
                    }
                }.awaitAll()
            }
            if (quoteSignature != signature) return@launch
            val current = repository.list(listId) ?: return@launch
            if (
                current.marketKey != list.marketKey ||
                current.orderSide != list.orderSide
            ) {
                return@launch
            }
            val quotes = results.associate { (typeId, result) -> typeId to result }
            val anySuccess = quotes.values.any { it.isSuccess }
            val anyFailure = quotes.values.any { it.isFailure }
            val items = _ui.value.items.map { item ->
                val quote = quotes[item.typeId]?.getOrNull()
                val unit = unitPrice(quote, current.orderSide)
                val quantity = current.items.find { it.typeId == item.typeId }?.quantity ?: item.quantity
                item.copy(
                    quantity = quantity,
                    unitPrice = unit,
                    lineTotal = unit?.times(quantity.toDouble()),
                )
            }
            _ui.value = _ui.value.copy(
                items = items,
                marketPrice = pricedTotal(items),
                loadingPrices = false,
                priceFailed = anyFailure && !anySuccess,
            )
        }
    }

    private fun itemUi(
        item: MarketWatchItem,
        type: TypeEntity?,
        previous: MarketWatchlistItemUi?,
    ): MarketWatchlistItemUi {
        val unitVolume = type?.repackagedVolume ?: type?.volume ?: 0.0
        val unitPrice = previous?.unitPrice
        return MarketWatchlistItemUi(
            typeId = item.typeId,
            name = type?.displayName(localeController).orEmpty(),
            iconFileName = type?.iconFilename,
            quantity = item.quantity,
            unitPrice = unitPrice,
            lineTotal = unitPrice?.times(item.quantity.toDouble()),
            unitVolume = unitVolume,
        )
    }

    private fun unitPrice(quote: MarketQuote?, side: MarketWatchOrderSide): Double? = when (side) {
        MarketWatchOrderSide.SELL -> quote?.lowestSell
        MarketWatchOrderSide.BUY -> quote?.highestBuy
    }

    private fun pricedTotal(items: List<MarketWatchlistItemUi>): Double? {
        val priced = items.mapNotNull { it.lineTotal }
        if (priced.isEmpty()) return null
        return priced.sum()
    }

    private data class QuoteSignature(
        val marketKey: String,
        val orderSide: MarketWatchOrderSide,
        val typeIds: List<Int>,
    )
}
