package com.marshall.pyerite.regionMarketModule.model

internal enum class MarketHistoryRange(val days: Int) {
    MONTH(MarketConfig.HISTORY_DAYS_MONTH),
    QUARTER(MarketConfig.HISTORY_DAYS_QUARTER),
    YEAR(MarketConfig.HISTORY_DAYS_YEAR),
}

internal data class MarketOrder(
    val orderId: Long,
    val typeId: Int,
    val isBuyOrder: Boolean,
    val price: Double,
    val volumeRemain: Long,
    val locationId: Long,
    val systemId: Int?,
)

internal data class MarketHistoryPoint(
    val date: String,
    val average: Double,
    val volume: Long,
)

internal data class MarketOrderLocation(
    val locationId: Long,
    val security: Double?,
    val placeName: String,
)

internal data class ResolvedMarket(
    val selection: MarketSelection,
    val displayName: String,
    val orderRegionId: Int,
    val historyRegionId: Int,
    val systemFilterId: Int?,
    val structureId: Long?,
    val structureCharacterId: Long?,
    val hideLocations: Boolean,
)

internal data class MarketQuote(
    val orders: List<MarketOrder>,
    val history: List<MarketHistoryPoint>,
    val lowestSell: Double?,
    val highestBuy: Double?,
    val structureAccessDenied: Boolean,
    val historyLoaded: Boolean,
)

internal data class MarketPlaceName(
    val regionId: Int,
    val name: String,
    val zhName: String?,
    val enName: String?,
)

internal data class MarketSystemOption(
    val systemId: Int,
    val regionId: Int,
    val systemName: String,
    val systemZhName: String?,
    val systemEnName: String?,
    val regionName: String,
    val regionZhName: String?,
    val regionEnName: String?,
    val security: Double?,
)

internal data class MarketStructureHit(
    val structureId: Long,
    val name: String,
    val systemId: Int,
    val regionId: Int,
    val systemName: String,
    val security: Double?,
    val typeId: Int?,
    val iconFileName: String?,
)
