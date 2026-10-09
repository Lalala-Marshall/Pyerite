package com.marshall.pyerite.regionMarketModule.watchlist.data

import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.regionMarketModule.data.MarketRepository
import com.marshall.pyerite.regionMarketModule.model.MarketQuote
import com.marshall.pyerite.regionMarketModule.model.MarketSelection
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchItem
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchOrderSide
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlist
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistConfig
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistExportResult
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistImportExport
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistImportFormatException
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistImportMode
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistImportResult
import com.marshall.pyerite.sdeModule.room.type.TypeEntity
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

internal class MarketWatchlistRepository(
    private val store: MarketWatchlistStore,
    private val marketRepository: MarketRepository,
    private val localeController: LocaleController,
) {
    val lists: StateFlow<List<MarketWatchlist>> = store.lists

    fun list(id: String): MarketWatchlist? = store.lists.value.find { it.id == id }

    fun create(title: String, marketKey: String): String {
        val id = UUID.randomUUID().toString()
        val created = MarketWatchlist(
            id = id,
            title = title.trim(),
            marketKey = marketKey,
            orderSide = MarketWatchOrderSide.SELL,
        )
        synchronized(lock) {
            store.save(store.lists.value + created)
        }
        return id
    }

    fun rename(id: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        update(id) { it.copy(title = trimmed) }
    }

    fun delete(id: String) {
        synchronized(lock) {
            store.save(store.lists.value.filter { it.id != id })
        }
    }

    fun update(id: String, block: (MarketWatchlist) -> MarketWatchlist) {
        synchronized(lock) {
            val current = store.lists.value
            if (current.none { it.id == id }) return
            store.save(current.map { if (it.id == id) block(it) else it })
        }
    }

    fun addItems(id: String, typeIds: List<Int>) {
        if (typeIds.isEmpty()) return
        update(id) { list ->
            val existing = list.items.map { it.typeId }.toSet()
            val appended = typeIds
                .distinct()
                .filter { it !in existing }
                .map { typeId ->
                    MarketWatchItem(
                        typeId = typeId,
                        quantity = MarketWatchlistConfig.DEFAULT_QUANTITY,
                    )
                }
            if (appended.isEmpty()) list else list.copy(items = list.items + appended)
        }
    }

    suspend fun locationName(selection: MarketSelection): String =
        marketRepository.resolve(
            selection = selection,
            typeId = MarketWatchlistConfig.LOCATION_RESOLVE_TYPE_ID,
        ).displayName

    suspend fun typesByIds(typeIds: List<Int>): List<TypeEntity> =
        marketRepository.typesByIds(typeIds)

    suspend fun loadQuote(
        typeId: Int,
        selection: MarketSelection,
        forceRefresh: Boolean,
    ): MarketQuote = marketRepository.loadQuote(
        typeId = typeId,
        selection = selection,
        includeHistory = false,
        forceRefresh = forceRefresh,
    )

    suspend fun exportText(listId: String): MarketWatchlistExportResult {
        val list = list(listId) ?: return MarketWatchlistExportResult.Empty
        if (list.items.isEmpty()) return MarketWatchlistExportResult.Empty
        val types = marketRepository.typesByIds(list.items.map { it.typeId }).associateBy { it.id }
        val lines = list.items.mapNotNull { item ->
            val name = types[item.typeId]?.displayName(localeController)?.trim().orEmpty()
            if (name.isEmpty()) null else name to item.quantity
        }
        val text = MarketWatchlistImportExport.format(lines)
        if (text.isEmpty()) return MarketWatchlistExportResult.Empty
        return MarketWatchlistExportResult.Success(text)
    }

    suspend fun prepareImport(raw: String?): MarketWatchlistImportResult {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return MarketWatchlistImportResult.ClipboardEmpty
        val parsed = try {
            MarketWatchlistImportExport.parse(text)
        } catch (_: MarketWatchlistImportFormatException) {
            return MarketWatchlistImportResult.ParseFailed
        }
        val quantities = linkedMapOf<Int, Long>()
        for (line in parsed) {
            val typeId = marketRepository.findTypeIdByExactName(line.name)
                ?: return MarketWatchlistImportResult.ParseFailed
            quantities[typeId] = line.quantity
        }
        if (quantities.isEmpty()) return MarketWatchlistImportResult.ParseFailed
        val items = quantities.map { (typeId, quantity) ->
            MarketWatchItem(typeId = typeId, quantity = quantity)
        }
        return MarketWatchlistImportResult.Ready(items)
    }

    fun applyImport(
        listId: String,
        items: List<MarketWatchItem>,
        mode: MarketWatchlistImportMode,
    ) {
        if (items.isEmpty()) return
        update(listId) { list ->
            val next = when (mode) {
                MarketWatchlistImportMode.Overwrite -> items
                MarketWatchlistImportMode.Append -> appendImportedItems(list.items, items)
            }
            list.copy(items = next)
        }
    }

    private fun appendImportedItems(
        existing: List<MarketWatchItem>,
        incoming: List<MarketWatchItem>,
    ): List<MarketWatchItem> {
        val added = incoming.associate { it.typeId to it.quantity }
        val updated = existing.map { item ->
            val extra = added[item.typeId] ?: return@map item
            item.copy(quantity = combinedQuantity(item.quantity, extra))
        }
        val existingIds = existing.map { it.typeId }.toSet()
        val appended = incoming
            .filter { it.typeId !in existingIds }
            .map { item ->
                item.copy(quantity = item.quantity.coerceAtMost(MarketWatchlistConfig.MAX_QUANTITY))
            }
        return updated + appended
    }

    private fun combinedQuantity(current: Long, added: Long): Long {
        val room = MarketWatchlistConfig.MAX_QUANTITY - current
        if (added >= room) return MarketWatchlistConfig.MAX_QUANTITY
        return current + added
    }

    private val lock = Any()
}
