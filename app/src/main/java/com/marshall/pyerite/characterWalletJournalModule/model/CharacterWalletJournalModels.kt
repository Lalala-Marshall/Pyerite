package com.marshall.pyerite.characterWalletJournalModule.model

import androidx.annotation.StringRes

internal class CharacterWalletJournalAccessException : Exception()

internal enum class CharacterWalletJournalSummaryWindow(val days: Int) {
    DAYS_30(CharacterWalletJournalConfig.WINDOW_DAYS_30),
    DAYS_7(CharacterWalletJournalConfig.WINDOW_DAYS_7),
    DAYS_1(CharacterWalletJournalConfig.WINDOW_DAYS_1),
    ;

    fun next(): CharacterWalletJournalSummaryWindow = when (this) {
        DAYS_30 -> DAYS_7
        DAYS_7 -> DAYS_1
        DAYS_1 -> DAYS_30
    }
}

internal enum class CharacterWalletJournalDescriptionKind {
    INDUSTRY_PROJECT,
    FALLBACK,
}

internal data class CharacterWalletJournalSnapshot(
    val characterId: Long,
    val journal: List<CharacterWalletJournalEntry>,
)

internal data class CharacterWalletJournalDescription(
    val kind: CharacterWalletJournalDescriptionKind,
    val firstPartyName: String?,
    val secondPartyName: String?,
    val contextId: Long?,
    val fallbackText: String,
)

internal data class CharacterWalletJournalEntry(
    val id: Long,
    val dateEpochMs: Long,
    val dayKey: String,
    val amount: Double,
    val balance: Double?,
    val refType: String,
    @param:StringRes val titleRes: Int,
    val description: CharacterWalletJournalDescription,
)

internal data class CharacterWalletJournalDayRow(
    val dayKey: String,
    val entryCount: Int,
    val netIsk: Double,
)

internal data class CharacterWalletJournalPeriodSummary(
    val income: Double,
    val expense: Double,
    val net: Double,
)
