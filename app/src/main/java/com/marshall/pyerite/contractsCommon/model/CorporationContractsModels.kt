package com.marshall.pyerite.contractsCommon.model

import androidx.annotation.StringRes
import com.marshall.pyerite.R
import com.marshall.pyerite.esiModule.model.EsiContractStatusValue
import com.marshall.pyerite.esiModule.model.EsiContractTypeValue
import kotlin.math.abs

internal class CorporationContractsAccessException : Exception()

internal enum class CorporationContractType(
    @param:StringRes val titleRes: Int,
    private val wireValue: String,
) {
    COURIER(R.string.corporation_contracts_type_courier, EsiContractTypeValue.COURIER),
    ITEM_EXCHANGE(
        R.string.corporation_contracts_type_item_exchange,
        EsiContractTypeValue.ITEM_EXCHANGE,
    ),
    AUCTION(R.string.corporation_contracts_type_auction, EsiContractTypeValue.AUCTION),
    ;

    companion object {
        fun fromWire(value: String): CorporationContractType? =
            entries.firstOrNull { it.wireValue == value }
    }
}

internal enum class CorporationContractStatusTone {
    OUTSTANDING,
    IN_PROGRESS,
    FINISHED,
    CLOSED,
    DELETED,
}

internal enum class CorporationContractStatus(
    @param:StringRes val titleRes: Int,
    val tone: CorporationContractStatusTone,
    private val wireValues: Set<String>,
) {
    OUTSTANDING(
        R.string.corporation_contracts_status_outstanding,
        CorporationContractStatusTone.OUTSTANDING,
        setOf(EsiContractStatusValue.OUTSTANDING),
    ),
    IN_PROGRESS(
        R.string.corporation_contracts_status_in_progress,
        CorporationContractStatusTone.IN_PROGRESS,
        setOf(EsiContractStatusValue.IN_PROGRESS),
    ),
    FINISHED(
        R.string.corporation_contracts_status_finished,
        CorporationContractStatusTone.FINISHED,
        setOf(
            EsiContractStatusValue.FINISHED,
            EsiContractStatusValue.FINISHED_ISSUER,
            EsiContractStatusValue.FINISHED_CONTRACTOR,
        ),
    ),
    CANCELLED(
        R.string.corporation_contracts_status_cancelled,
        CorporationContractStatusTone.CLOSED,
        setOf(EsiContractStatusValue.CANCELLED),
    ),
    REJECTED(
        R.string.corporation_contracts_status_rejected,
        CorporationContractStatusTone.CLOSED,
        setOf(EsiContractStatusValue.REJECTED),
    ),
    FAILED(
        R.string.corporation_contracts_status_failed,
        CorporationContractStatusTone.CLOSED,
        setOf(EsiContractStatusValue.FAILED),
    ),
    DELETED(
        R.string.corporation_contracts_status_deleted,
        CorporationContractStatusTone.DELETED,
        setOf(EsiContractStatusValue.DELETED),
    ),
    REVERSED(
        R.string.corporation_contracts_status_reversed,
        CorporationContractStatusTone.CLOSED,
        setOf(EsiContractStatusValue.REVERSED),
    ),
    ;

    companion object {
        fun fromWire(value: String): CorporationContractStatus? =
            entries.firstOrNull { value in it.wireValues }
    }
}

internal enum class CorporationContractGroupBy {
    ISSUED,
    COMPLETED,
}

/** Contract-list filter: current character, their corporation, or their alliance. */
internal enum class ContractListScope {
    CHARACTER,
    CORPORATION,
    ALLIANCE,
}

/**
 * Scopes for one row of `GET /corporations/{corporation_id}/contracts/`.
 * Corporation: the assignee is the character's corporation.
 * Alliance: the assignee is the character's alliance.
 * Deleted rows, and rows assigned to anyone else, are omitted.
 */
internal fun corporationContractListScopes(
    corporationId: Long,
    allianceId: Long?,
    assigneeId: Long,
    status: String,
): Set<ContractListScope> {
    if (status == EsiContractStatusValue.DELETED) return emptySet()
    return buildSet {
        if (assigneeId == corporationId) add(ContractListScope.CORPORATION)
        if (allianceId != null && allianceId > 0L && assigneeId == allianceId) {
            add(ContractListScope.ALLIANCE)
        }
    }
}

internal enum class CorporationContractDisplayLimit(
    val maxCount: Int?,
) {
    FIFTY(CorporationContractsConfig.LIMIT_50),
    HUNDRED(CorporationContractsConfig.LIMIT_100),
    THREE_HUNDRED(CorporationContractsConfig.LIMIT_300),
    FIVE_HUNDRED(CorporationContractsConfig.LIMIT_500),
    UNLIMITED(null),
}

internal enum class CorporationContractSectionKind {
    ISSUED_DAY,
    COMPLETED_DAY,
    INCOMPLETE,
}

internal data class CorporationContract(
    val contractId: Long,
    val type: CorporationContractType,
    val status: CorporationContractStatus,
    val title: String,
    val signedIsk: Double,
    val price: Double = CorporationContractsConfig.ZERO_ISK,
    val reward: Double = CorporationContractsConfig.ZERO_ISK,
    val acceptorId: Long = 0L,
    val volume: Double,
    val issuedAtMs: Long,
    val expiresAtMs: Long?,
    val completedAtMs: Long?,
    val issuerId: Long,
    val assigneeId: Long,
    val startLocationId: Long,
    val scopes: Set<ContractListScope> = emptySet(),
    /** Corp-endpoint rows use corporation contract items; character-endpoint rows do not. */
    val itemsViaCorporation: Boolean = false,
)

/** Finished, reversed, deleted, and other closed contracts show the status-change time. */
internal val CorporationContractStatus.showsStatusChangedAt: Boolean
    get() = this != CorporationContractStatus.OUTSTANDING &&
        this != CorporationContractStatus.IN_PROGRESS

/** Outstanding and in-progress contracts show the days left until expiry. */
internal val CorporationContractStatus.showsIssueCountdown: Boolean
    get() = this == CorporationContractStatus.OUTSTANDING ||
        this == CorporationContractStatus.IN_PROGRESS

internal data class CorporationContractsSnapshot(
    val contracts: List<CorporationContract>,
)

internal data class CorporationContractSection(
    val id: String,
    val kind: CorporationContractSectionKind,
    val dayEpochMs: Long?,
    val contracts: List<CorporationContract>,
)

internal data class ContractsListSettings(
    val types: Set<CorporationContractType> = CorporationContractType.entries.toSet(),
    val statuses: Set<CorporationContractStatus> = CorporationContractStatus.entries.toSet(),
    val displayLimit: CorporationContractDisplayLimit = CorporationContractDisplayLimit.THREE_HUNDRED,
    val groupBy: CorporationContractGroupBy = CorporationContractGroupBy.ISSUED,
) {
    fun toggleType(type: CorporationContractType): ContractsListSettings {
        val next = types.toMutableSet()
        if (!next.add(type)) next.remove(type)
        return copy(types = next)
    }

    fun toggleAllTypes(): ContractsListSettings = copy(
        types = if (types.containsAll(CorporationContractType.entries)) {
            emptySet()
        } else {
            CorporationContractType.entries.toSet()
        },
    )

    fun toggleStatus(status: CorporationContractStatus): ContractsListSettings {
        val next = statuses.toMutableSet()
        if (!next.add(status)) next.remove(status)
        return copy(statuses = next)
    }

    fun toggleAllStatuses(): ContractsListSettings = copy(
        statuses = if (statuses.containsAll(CorporationContractStatus.entries)) {
            emptySet()
        } else {
            CorporationContractStatus.entries.toSet()
        },
    )
}

internal data class CorporationContractsFilter(
    val types: Set<CorporationContractType> = CorporationContractType.entries.toSet(),
    val statuses: Set<CorporationContractStatus> = CorporationContractStatus.entries.toSet(),
    val minPriceText: String = "",
    val maxPriceText: String = "",
    val displayLimit: CorporationContractDisplayLimit = CorporationContractDisplayLimit.THREE_HUNDRED,
    val groupBy: CorporationContractGroupBy = CorporationContractGroupBy.ISSUED,
) {
    fun withSettings(settings: ContractsListSettings): CorporationContractsFilter = copy(
        types = settings.types,
        statuses = settings.statuses,
        displayLimit = settings.displayLimit,
        groupBy = settings.groupBy,
    )

    val allTypesSelected: Boolean = types.containsAll(CorporationContractType.entries)

    val allStatusesSelected: Boolean = statuses.containsAll(CorporationContractStatus.entries)

    fun matches(contract: CorporationContract): Boolean {
        if (contract.type !in types) return false
        if (contract.status !in statuses) return false
        return matchesPrice(contract.signedIsk)
    }

    private fun matchesPrice(signedIsk: Double): Boolean {
        val amount = abs(signedIsk)
        val min = parseContractPriceBound(minPriceText)
        val max = parseContractPriceBound(maxPriceText)
        if (min != null && max != null && min > max) return false
        if (min != null && amount < min) return false
        if (max != null && amount > max) return false
        return true
    }
}

/** Whose cash flow the +/- on a contract amount follows. */
internal enum class ContractAmountViewpoint {
    /** Contract list: the logged-in character pays or is paid. */
    CHARACTER,
    /** Corporation-issued list: the corporation is the issuer. */
    CORPORATION_ISSUER,
}

internal enum class ContractAmountTone {
    INCOME,
    EXPENSE,
    NEUTRAL,
    OPEN,
}

internal data class ContractAmountPart(
    val amount: Double,
    val tone: ContractAmountTone,
)

internal fun contractAmountParts(
    contract: CorporationContract,
    viewpoint: ContractAmountViewpoint,
    characterId: Long,
): List<ContractAmountPart> = when (viewpoint) {
    ContractAmountViewpoint.CHARACTER -> characterContractAmountParts(
        type = contract.type,
        price = contract.price,
        reward = contract.reward,
        characterIsIssuer = characterId > 0L && contract.issuerId == characterId,
        characterIsAcceptor = characterId > 0L && contract.acceptorId == characterId,
    )
    ContractAmountViewpoint.CORPORATION_ISSUER -> corporationIssuedAmountParts(
        type = contract.type,
        price = contract.price,
        reward = contract.reward,
    )
}

private fun characterContractAmountParts(
    type: CorporationContractType,
    price: Double,
    reward: Double,
    characterIsIssuer: Boolean,
    characterIsAcceptor: Boolean,
): List<ContractAmountPart> {
    val hasPrice = price > CorporationContractsConfig.ZERO_ISK
    val hasReward = reward > CorporationContractsConfig.ZERO_ISK
    val priceFromIssuer = if (characterIsIssuer) {
        ContractAmountTone.INCOME
    } else {
        ContractAmountTone.EXPENSE
    }
    val rewardFromIssuer = if (characterIsIssuer) {
        ContractAmountTone.EXPENSE
    } else {
        ContractAmountTone.INCOME
    }
    return when (type) {
        CorporationContractType.ITEM_EXCHANGE -> when {
            hasPrice && hasReward -> listOf(
                ContractAmountPart(price, priceFromIssuer),
                ContractAmountPart(reward, rewardFromIssuer),
            )
            hasPrice -> listOf(ContractAmountPart(price, priceFromIssuer))
            hasReward -> listOf(ContractAmountPart(reward, rewardFromIssuer))
            else -> listOf(ContractAmountPart(price, priceFromIssuer))
        }
        CorporationContractType.COURIER -> when {
            hasPrice && hasReward -> listOf(
                ContractAmountPart(price, ContractAmountTone.NEUTRAL),
                ContractAmountPart(reward, rewardFromIssuer),
            )
            hasReward -> listOf(ContractAmountPart(reward, rewardFromIssuer))
            hasPrice -> listOf(ContractAmountPart(price, ContractAmountTone.NEUTRAL))
            else -> listOf(ContractAmountPart(reward, rewardFromIssuer))
        }
        CorporationContractType.AUCTION -> {
            val priceTone = when {
                characterIsIssuer -> ContractAmountTone.INCOME
                characterIsAcceptor -> ContractAmountTone.EXPENSE
                else -> ContractAmountTone.OPEN
            }
            when {
                hasPrice && hasReward -> listOf(
                    ContractAmountPart(price, priceTone),
                    ContractAmountPart(reward, ContractAmountTone.INCOME),
                )
                hasPrice -> listOf(ContractAmountPart(price, priceTone))
                hasReward -> listOf(ContractAmountPart(reward, ContractAmountTone.INCOME))
                else -> listOf(ContractAmountPart(price, priceTone))
            }
        }
    }
}

private fun corporationIssuedAmountParts(
    type: CorporationContractType,
    price: Double,
    reward: Double,
): List<ContractAmountPart> {
    val hasPrice = price > CorporationContractsConfig.ZERO_ISK
    val hasReward = reward > CorporationContractsConfig.ZERO_ISK
    val priceTone = when (type) {
        CorporationContractType.ITEM_EXCHANGE,
        CorporationContractType.AUCTION,
        -> ContractAmountTone.INCOME
        CorporationContractType.COURIER -> ContractAmountTone.NEUTRAL
    }
    val rewardTone = when (type) {
        CorporationContractType.COURIER -> ContractAmountTone.EXPENSE
        CorporationContractType.ITEM_EXCHANGE,
        CorporationContractType.AUCTION,
        -> ContractAmountTone.INCOME
    }
    return when {
        hasPrice && hasReward -> listOf(
            ContractAmountPart(price, priceTone),
            ContractAmountPart(reward, rewardTone),
        )
        !hasPrice && hasReward -> listOf(ContractAmountPart(reward, rewardTone))
        hasPrice -> listOf(ContractAmountPart(price, priceTone))
        else -> when (type) {
            CorporationContractType.ITEM_EXCHANGE,
            CorporationContractType.AUCTION,
            -> listOf(ContractAmountPart(price, ContractAmountTone.INCOME))
            CorporationContractType.COURIER ->
                listOf(ContractAmountPart(reward, ContractAmountTone.EXPENSE))
        }
    }
}

internal fun corporationContractSignedIsk(
    type: CorporationContractType,
    price: Double,
    reward: Double,
    buyout: Double,
): Double = when (type) {
    CorporationContractType.ITEM_EXCHANGE -> price - reward
    CorporationContractType.COURIER -> -reward
    CorporationContractType.AUCTION ->
        if (price > CorporationContractsConfig.ZERO_ISK) price else buyout
}

internal fun sanitizeContractPriceInput(raw: String): String = buildString {
    var dotSeen = false
    for (ch in raw) {
        if (length >= CorporationContractsConfig.PRICE_INPUT_MAX_LENGTH) break
        when {
            ch.isDigit() -> append(ch)
            ch == '.' && !dotSeen -> {
                dotSeen = true
                append(ch)
            }
        }
    }
}

internal fun parseContractPriceBound(text: String): Double? {
    val trimmed = text.trim().trimEnd('.')
    if (trimmed.isEmpty()) return null
    val value = trimmed.toDoubleOrNull() ?: return null
    if (!value.isFinite() || value < CorporationContractsConfig.ZERO_ISK) return null
    return value
}

internal data class CorporationContractsListResult(
    val filteredCount: Int,
    val sections: List<CorporationContractSection>,
) {
    val shownCount: Int
        get() = sections.sumOf { it.contracts.size }
}

internal fun List<CorporationContract>.toListResult(
    filter: CorporationContractsFilter,
): CorporationContractsListResult {
    val matched = filter { filter.matches(it) }
    val sections = when (filter.groupBy) {
        CorporationContractGroupBy.ISSUED -> matched
            .sortedByDescending { it.issuedAtMs }
            .limited(filter.displayLimit)
            .groupByDay(CorporationContractSectionKind.ISSUED_DAY) { it.issuedAtMs }
        CorporationContractGroupBy.COMPLETED -> {
            val incomplete = matched
                .filter { it.completedAtMs == null }
                .sortedByDescending { it.issuedAtMs }
            val completed = matched
                .filter { it.completedAtMs != null }
                .sortedByDescending { it.completedAtMs }
            val ordered = (incomplete + completed).limited(filter.displayLimit)
            buildList {
                val open = ordered.filter { it.completedAtMs == null }
                if (open.isNotEmpty()) {
                    add(
                        CorporationContractSection(
                            id = CorporationContractsConfig.INCOMPLETE_SECTION_ID,
                            kind = CorporationContractSectionKind.INCOMPLETE,
                            dayEpochMs = null,
                            contracts = open,
                        ),
                    )
                }
                addAll(
                    ordered
                        .filter { it.completedAtMs != null }
                        .groupByDay(CorporationContractSectionKind.COMPLETED_DAY) {
                            it.completedAtMs ?: 0L
                        },
                )
            }
        }
    }
    return CorporationContractsListResult(
        filteredCount = matched.size,
        sections = sections,
    )
}

private fun List<CorporationContract>.limited(
    limit: CorporationContractDisplayLimit,
): List<CorporationContract> {
    val maxCount = limit.maxCount ?: return this
    return take(maxCount)
}

private fun List<CorporationContract>.groupByDay(
    kind: CorporationContractSectionKind,
    epochMs: (CorporationContract) -> Long,
): List<CorporationContractSection> {
    val prefix = when (kind) {
        CorporationContractSectionKind.ISSUED_DAY -> "issued"
        CorporationContractSectionKind.COMPLETED_DAY -> "completed"
        CorporationContractSectionKind.INCOMPLETE -> CorporationContractsConfig.INCOMPLETE_SECTION_ID
    }
    return groupBy { CorporationContractsDateFormatter.dayKey(epochMs(it)) }
        .map { (dayKey, items) ->
            CorporationContractSection(
                id = "$prefix-$dayKey",
                kind = kind,
                dayEpochMs = epochMs(items.first()),
                contracts = items,
            )
        }
}
