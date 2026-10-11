package com.marshall.pyerite.characterTransactionsModule.model

import java.util.Calendar

internal fun mergeSimilarCharacterTransactions(
    transactions: List<CharacterMarketTransaction>,
): List<CharacterMarketTransaction> {
    if (transactions.size <= 1) return transactions
    val grouped = LinkedHashMap<CharacterTransactionMergeKey, MutableList<CharacterMarketTransaction>>()
    transactions.forEach { transaction ->
        grouped.getOrPut(transaction.mergeKey()) { mutableListOf() }.add(transaction)
    }
    return grouped.values.map { group ->
        val newest = group.maxBy { it.dateEpochMs }
        if (group.size == 1) {
            newest
        } else {
            val totalQuantity = group.sumOf { it.quantity }
            val totalAmount = group.sumOf { it.quantity.toDouble() * it.unitPrice }
            val averagePrice = if (totalQuantity == 0L) {
                CharacterTransactionsConfig.ZERO_ISK
            } else {
                totalAmount / totalQuantity.toDouble()
            }
            newest.copy(
                quantity = totalQuantity,
                unitPrice = averagePrice,
                totalIsk = totalAmount,
            )
        }
    }
}

internal fun visibleCharacterTransactions(
    transactions: List<CharacterMarketTransaction>,
    sideFilter: CharacterTransactionSideFilter,
    mergeSimilar: Boolean,
    dayKey: String? = null,
): List<CharacterMarketTransaction> {
    val filtered = transactions.filter { transaction ->
        (dayKey == null || transaction.dayKey == dayKey) && sideFilter.matches(transaction.isBuy)
    }
    return if (mergeSimilar) mergeSimilarCharacterTransactions(filtered) else filtered
}

internal fun characterTransactionDays(
    entries: List<CharacterMarketTransaction>,
): List<CharacterTransactionDayRow> {
    return entries.groupBy { it.dayKey }
        .map { (dayKey, dayEntries) ->
            CharacterTransactionDayRow(
                dayKey = dayKey,
                buyCount = dayEntries.count { it.isBuy },
                sellCount = dayEntries.count { !it.isBuy },
                netIsk = dayEntries.sumOf { it.signedNet },
            )
        }
        .sortedByDescending { it.dayKey }
}

private data class CharacterTransactionMergeKey(
    val minuteEpochMs: Long,
    val typeId: Int,
    val locationId: Long,
    val isBuy: Boolean,
)

/** Same local minute, item, buy/sell side, and location. */
private fun CharacterMarketTransaction.mergeKey() = CharacterTransactionMergeKey(
    minuteEpochMs = localMinuteEpochMs(dateEpochMs),
    typeId = typeId,
    locationId = locationId,
    isBuy = isBuy,
)

private fun localMinuteEpochMs(epochMs: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = epochMs
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
