package com.marshall.pyerite.characterWalletJournalModule.model

import androidx.annotation.StringRes
import com.marshall.pyerite.R
import com.marshall.pyerite.walletJournal.model.WalletJournalRef

internal enum class CharacterWalletJournalDirection(
    @param:StringRes val titleRes: Int,
) {
    INCOME(R.string.corporation_wallet_filter_income),
    EXPENSE(R.string.corporation_wallet_filter_expense),
}

internal enum class CharacterWalletJournalFilterType(
    @param:StringRes val titleRes: Int,
    val refTypes: Set<String>,
) {
    PLAYER_TRADING(
        R.string.corporation_journal_ref_player_trading,
        setOf("player_trading"),
    ),
    DAILY_GOAL_PAYOUTS(
        R.string.corporation_journal_ref_daily_goal_payouts,
        setOf("daily_goal_payouts", "daily_goal_payout", "daily_goal_reward"),
    ),
    PLAYER_DONATION(
        R.string.corporation_journal_ref_player_donation,
        setOf("player_donation"),
    ),
    MANUFACTURING(
        R.string.corporation_journal_ref_manufacturing,
        setOf("manufacturing", "industry_job"),
    ),
    STRUCTURE_GATE_JUMP(
        R.string.character_wallet_journal_ref_structure_gate_jump,
        setOf("structure_gate_jump"),
    ),
    CONTRACT_BROKERS_FEE(
        R.string.corporation_journal_ref_contract_brokers_fee,
        setOf("contract_brokers_fee"),
    ),
    CORPORATE_REWARD_PAYOUT(
        R.string.character_wallet_journal_ref_corporate_reward_payout,
        setOf("corporate_reward_payout"),
    ),
    REACTION(
        R.string.corporation_journal_ref_reaction,
        setOf("reaction"),
    ),
    MARKET_TRANSACTION(
        R.string.corporation_journal_ref_market_transaction,
        setOf("market_transaction"),
    ),
    TRANSACTION_TAX(
        R.string.corporation_journal_ref_transaction_tax,
        setOf("transaction_tax"),
    ),
    SKILL_PURCHASE(
        R.string.corporation_journal_ref_skill_purchase,
        setOf("skill_purchase"),
    ),
    INSURANCE(
        R.string.corporation_journal_ref_insurance,
        setOf("insurance"),
    ),
    CORPORATION_ACCOUNT_WITHDRAWAL(
        R.string.corporation_journal_ref_corporation_account_withdrawal,
        setOf("corporation_account_withdrawal"),
    ),
    BOUNTY(
        R.string.character_wallet_journal_ref_bounty,
        setOf("bounty"),
    ),
    AGENT_MISSION_REWARD(
        R.string.corporation_journal_ref_agent_mission_reward,
        setOf("agent_mission_reward"),
    ),
    INDUSTRY_JOB_TAX(
        R.string.corporation_journal_ref_industry_job_tax,
        setOf("industry_job_tax"),
    ),
    MARKET_ESCROW(
        R.string.corporation_journal_ref_market_escrow,
        setOf("market_escrow"),
    ),
    AGENT_MISSION_TIME_BONUS_REWARD(
        R.string.character_wallet_journal_ref_agent_mission_time_bonus_reward,
        setOf("agent_mission_time_bonus_reward"),
    ),
    REPAIR_BILL(
        R.string.character_wallet_journal_ref_repair_bill,
        setOf("repair_bill"),
    ),
    JUMP_CLONE_INSTALLATION_FEE(
        R.string.character_wallet_journal_ref_jump_clone_installation_fee,
        setOf("jump_clone_installation_fee"),
    ),
    ;

    companion object {
        private val BY_REF_TYPE: Map<String, CharacterWalletJournalFilterType> =
            entries.flatMap { type -> type.refTypes.map { ref -> ref to type } }.toMap()

        fun forRefType(refType: String): CharacterWalletJournalFilterType? =
            BY_REF_TYPE[refType.trim()]

        @StringRes
        fun titleResFor(refType: String): Int =
            forRefType(refType)?.titleRes ?: WalletJournalRef.titleResFor(refType)
    }
}

internal data class CharacterWalletJournalFilter(
    val directions: Set<CharacterWalletJournalDirection>,
    val types: Set<CharacterWalletJournalFilterType>,
) {
    val allDirectionsSelected: Boolean =
        directions.containsAll(CharacterWalletJournalDirection.entries)

    val allTypesSelected: Boolean =
        types.containsAll(CharacterWalletJournalFilterType.entries)

    val isDefault: Boolean = allDirectionsSelected && allTypesSelected

    fun matches(entry: CharacterWalletJournalEntry): Boolean {
        val directionMatches = when {
            entry.amount > CharacterWalletJournalConfig.ZERO_ISK ->
                CharacterWalletJournalDirection.INCOME in directions
            entry.amount < CharacterWalletJournalConfig.ZERO_ISK ->
                CharacterWalletJournalDirection.EXPENSE in directions
            else -> directions.isNotEmpty()
        }
        if (!directionMatches) return false
        val type = CharacterWalletJournalFilterType.forRefType(entry.refType)
        return if (type == null) allTypesSelected else type in types
    }

    fun toggleAllDirections(): CharacterWalletJournalFilter = copy(
        directions = if (allDirectionsSelected) {
            emptySet()
        } else {
            CharacterWalletJournalDirection.entries.toSet()
        },
    )

    fun toggleDirection(direction: CharacterWalletJournalDirection): CharacterWalletJournalFilter {
        val next = directions.toMutableSet()
        if (!next.add(direction)) next.remove(direction)
        return copy(directions = next)
    }

    fun toggleAllTypes(): CharacterWalletJournalFilter = copy(
        types = if (allTypesSelected) {
            emptySet()
        } else {
            CharacterWalletJournalFilterType.entries.toSet()
        },
    )

    fun toggleType(type: CharacterWalletJournalFilterType): CharacterWalletJournalFilter {
        val next = types.toMutableSet()
        if (!next.add(type)) next.remove(type)
        return copy(types = next)
    }

    companion object {
        val ALL = CharacterWalletJournalFilter(
            directions = CharacterWalletJournalDirection.entries.toSet(),
            types = CharacterWalletJournalFilterType.entries.toSet(),
        )
    }
}
