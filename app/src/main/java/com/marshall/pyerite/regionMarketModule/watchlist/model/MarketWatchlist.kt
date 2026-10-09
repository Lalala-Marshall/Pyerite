package com.marshall.pyerite.regionMarketModule.watchlist.model

import kotlinx.serialization.Serializable

@Serializable
internal enum class MarketWatchOrderSide {
    SELL,
    BUY,
}

@Serializable
internal data class MarketWatchItem(
    val typeId: Int,
    val quantity: Long,
)

@Serializable
internal data class MarketWatchlist(
    val id: String,
    val title: String,
    val marketKey: String,
    val orderSide: MarketWatchOrderSide = MarketWatchOrderSide.SELL,
    val items: List<MarketWatchItem> = emptyList(),
)

internal sealed interface MarketWatchlistExportResult {
    data class Success(val text: String) : MarketWatchlistExportResult
    data object Empty : MarketWatchlistExportResult
}

internal enum class MarketWatchlistImportMode {
    Overwrite,
    Append,
}

internal sealed interface MarketWatchlistImportResult {
    data class Ready(val items: List<MarketWatchItem>) : MarketWatchlistImportResult
    data object ClipboardEmpty : MarketWatchlistImportResult
    data object ParseFailed : MarketWatchlistImportResult
}

internal class MarketWatchlistImportFormatException : Exception()

/** Digits only, capped length. Null means the keystroke is rejected. */
internal fun sanitizeWatchlistQuantityInput(raw: String): String? {
    if (raw.isEmpty()) return ""
    if (raw.length > MarketWatchlistConfig.QUANTITY_MAX_DIGITS) return null
    if (raw.any { !it.isDigit() }) return null
    return raw
}
