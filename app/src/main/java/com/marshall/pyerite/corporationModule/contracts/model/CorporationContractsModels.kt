package com.marshall.pyerite.corporationModule.contracts.model

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
    val volume: Double,
    val issuedAtMs: Long,
    val expiresAtMs: Long?,
    val completedAtMs: Long?,
    val issuerId: Long,
    val assigneeId: Long,
    val startLocationId: Long,
)

/** Finished, reversed, deleted, and other closed contracts show the status-change time. */
internal val CorporationContractStatus.showsStatusChangedAt: Boolean
    get() = this != CorporationContractStatus.OUTSTANDING &&
        this != CorporationContractStatus.IN_PROGRESS

internal data class CorporationContractsSnapshot(
    val contracts: List<CorporationContract>,
)

internal data class CorporationContractSection(
    val id: String,
    val kind: CorporationContractSectionKind,
    val dayEpochMs: Long?,
    val contracts: List<CorporationContract>,
)

internal data class CorporationContractsFilter(
    val types: Set<CorporationContractType> = CorporationContractType.entries.toSet(),
    val statuses: Set<CorporationContractStatus> = setOf(CorporationContractStatus.OUTSTANDING),
    val minPriceText: String = "",
    val maxPriceText: String = "",
    val displayLimit: CorporationContractDisplayLimit = CorporationContractDisplayLimit.THREE_HUNDRED,
    val groupBy: CorporationContractGroupBy = CorporationContractGroupBy.ISSUED,
) {
    val allTypesSelected: Boolean = types.containsAll(CorporationContractType.entries)

    val allStatusesSelected: Boolean = statuses.containsAll(CorporationContractStatus.entries)

    fun matches(contract: CorporationContract): Boolean {
        if (contract.type !in types) return false
        if (contract.status !in statuses) return false
        return matchesPrice(contract.signedIsk)
    }

    fun toggleType(type: CorporationContractType): CorporationContractsFilter {
        val next = types.toMutableSet()
        if (!next.add(type)) next.remove(type)
        return copy(types = next)
    }

    fun toggleAllTypes(): CorporationContractsFilter = copy(
        types = if (allTypesSelected) emptySet() else CorporationContractType.entries.toSet(),
    )

    fun toggleStatus(status: CorporationContractStatus): CorporationContractsFilter {
        val next = statuses.toMutableSet()
        if (!next.add(status)) next.remove(status)
        return copy(statuses = next)
    }

    fun toggleAllStatuses(): CorporationContractsFilter = copy(
        statuses = if (allStatusesSelected) {
            emptySet()
        } else {
            CorporationContractStatus.entries.toSet()
        },
    )

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

internal fun List<CorporationContract>.toSections(
    filter: CorporationContractsFilter,
): List<CorporationContractSection> {
    val matched = filter { filter.matches(it) }
    return when (filter.groupBy) {
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
