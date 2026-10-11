package com.marshall.pyerite.characterTransactionsModule.model

import com.marshall.pyerite.localization.LocalizableName

internal class CharacterTransactionsAccessException : Exception()

internal enum class CharacterTransactionSideFilter {
    ALL,
    SELL,
    BUY,
    ;

    fun matches(isBuy: Boolean): Boolean = when (this) {
        ALL -> true
        SELL -> !isBuy
        BUY -> isBuy
    }
}

internal data class CharacterTransactionsSnapshot(
    val characterId: Long,
    val transactions: List<CharacterMarketTransaction>,
)

internal data class CharacterTransactionLocation(
    val securityStatus: Double?,
    val systemZhName: String?,
    val systemEnName: String?,
    val systemName: String?,
    val placeName: String?,
)

internal data class CharacterMarketTransaction(
    val transactionId: Long,
    val dateEpochMs: Long,
    val dayKey: String,
    val typeId: Int,
    val locationId: Long,
    override val zhName: String?,
    override val enName: String?,
    override val name: String?,
    val iconFilename: String?,
    val quantity: Long,
    val unitPrice: Double,
    val totalIsk: Double = quantity.toDouble() * unitPrice,
    val isBuy: Boolean,
    val location: CharacterTransactionLocation?,
) : LocalizableName {
    val signedNet: Double = if (isBuy) -totalIsk else totalIsk
}

internal data class CharacterTransactionDayRow(
    val dayKey: String,
    val buyCount: Int,
    val sellCount: Int,
    val netIsk: Double,
)
