package com.marshall.pyerite.corporationModule.wallet.model

import androidx.annotation.StringRes
import com.marshall.pyerite.localization.LocalizableName
import com.marshall.pyerite.walletJournal.model.WalletJournalCategory

internal class CorporationWalletAccessException : Exception()

internal enum class CorporationWalletDivisionTab {
    JOURNAL,
    TRANSACTIONS,
}

internal enum class CorporationWalletSummaryWindow(val days: Int) {
    DAYS_30(CorporationWalletConfig.WINDOW_DAYS_30),
    DAYS_7(CorporationWalletConfig.WINDOW_DAYS_7),
    DAYS_1(CorporationWalletConfig.WINDOW_DAYS_1),
    ;

    fun next(): CorporationWalletSummaryWindow = when (this) {
        DAYS_30 -> DAYS_7
        DAYS_7 -> DAYS_1
        DAYS_1 -> DAYS_30
    }
}

internal enum class CorporationWalletJournalDescriptionKind {
    INDUSTRY_PROJECT,
    FALLBACK,
}

internal data class CorporationWalletDivision(
    val division: Int,
    val name: String?,
    val balance: Double,
)

internal data class CorporationWalletsSnapshot(
    val characterId: Long,
    val corporationId: Long,
    val divisions: List<CorporationWalletDivision>,
)

internal data class CorporationWalletLedger(
    val characterId: Long,
    val corporationId: Long,
    val division: Int,
    val journal: List<CorporationWalletJournalEntry>,
    val transactions: List<CorporationWalletMarketTransaction>,
)

internal data class CorporationWalletJournalDescription(
    val kind: CorporationWalletJournalDescriptionKind,
    val firstPartyName: String?,
    val secondPartyName: String?,
    val contextId: Long?,
    val fallbackText: String,
)

internal data class CorporationWalletJournalEntry(
    val id: Long,
    val dateEpochMs: Long,
    val dayKey: String,
    val amount: Double,
    val balance: Double?,
    val refType: String,
    val category: WalletJournalCategory,
    @param:StringRes val titleRes: Int,
    val description: CorporationWalletJournalDescription,
)

internal data class CorporationWalletLocation(
    val securityStatus: Double?,
    val systemZhName: String?,
    val systemEnName: String?,
    val systemName: String?,
    val placeName: String?,
)

internal data class CorporationWalletMarketTransaction(
    val transactionId: Long,
    val dateEpochMs: Long,
    val dayKey: String,
    val typeId: Int,
    val locationId: Long,
    override val zhName: String?,
    override val enName: String?,
    override val name: String?,
    val iconFilename: String?,
    val quantity: Int,
    val unitPrice: Double,
    val isBuy: Boolean,
    val location: CorporationWalletLocation?,
) : LocalizableName {
    val totalIsk: Double = quantity * unitPrice
    val signedNet: Double = if (isBuy) -totalIsk else totalIsk
}

internal data class CorporationWalletDayRow(
    val dayKey: String,
    val dateEpochMs: Long,
    val entryCount: Int,
    val buyCount: Int,
    val sellCount: Int,
    val netIsk: Double,
)

internal data class CorporationWalletPeriodSummary(
    val income: Double,
    val expense: Double,
    val net: Double,
)
