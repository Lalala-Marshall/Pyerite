package com.marshall.pyerite.entityProfileModule.model

import com.marshall.pyerite.ui.golbalComponents.UniverseEntityKind
import com.marshall.pyerite.ui.golbalComponents.UniverseEntityRef

internal object EntityProfileConfig {
    const val HEIGHT_FRACTION = 0.94f

    /** NPC corporation ids are below this; player corps start at 98_000_000. */
    const val NPC_CORPORATION_ID_MAX = 2_000_000L

    const val STANDING_NEUTRAL = 0.0
    const val STANDING_ZERO_EPSILON = 0.0001
    const val STANDING_FRACTION_DIGITS = 2
    const val STANDING_FRACTION_SCALE = 100.0
    const val STANDING_HUNDREDTHS_PER_UNIT = 100L
    /** Layout sample so "0" and "10" keep standing icons aligned. */
    const val STANDING_COLUMN_SAMPLE = "10"

    const val DISPLAY_DATE_PATTERN = "yyyy-MM-dd"

    const val EVEWHO_BASE_URL = "https://evewho.com/"
    const val ZKILL_BASE_URL = "https://zkillboard.com/"
    const val EVEWHO_CHARACTER_PATH = "character/"
    const val EVEWHO_CORPORATION_PATH = "corporation/"
    const val EVEWHO_ALLIANCE_PATH = "alliance/"
    const val ZKILL_CHARACTER_PATH = "character/"
    const val ZKILL_CORPORATION_PATH = "corporation/"
    const val ZKILL_ALLIANCE_PATH = "alliance/"
    const val ZKILL_PATH_SUFFIX = "/"

    fun isNpcCorporation(corporationId: Long): Boolean = corporationId < NPC_CORPORATION_ID_MAX

    fun eveWhoUrl(kind: UniverseEntityKind, id: Long): String =
        EVEWHO_BASE_URL + pathFor(kind, eveWho = true) + id

    fun zKillUrl(kind: UniverseEntityKind, id: Long): String =
        ZKILL_BASE_URL + pathFor(kind, eveWho = false) + id + ZKILL_PATH_SUFFIX

    fun formatStanding(value: Double): String {
        if (kotlin.math.abs(value) < STANDING_ZERO_EPSILON) return "0"
        val hundredths = if (value >= 0.0) {
            kotlin.math.floor(value * STANDING_FRACTION_SCALE).toLong()
        } else {
            kotlin.math.ceil(value * STANDING_FRACTION_SCALE).toLong()
        }
        val sign = if (hundredths < 0L) "-" else ""
        val absolute = kotlin.math.abs(hundredths)
        val whole = absolute / STANDING_HUNDREDTHS_PER_UNIT
        val fraction = absolute % STANDING_HUNDREDTHS_PER_UNIT
        if (fraction == 0L) return "$sign$whole"
        val fractionText = fraction.toString()
            .padStart(STANDING_FRACTION_DIGITS, '0')
            .trimEnd('0')
        return "$sign$whole.$fractionText"
    }

    private fun pathFor(kind: UniverseEntityKind, eveWho: Boolean): String = when (kind) {
        UniverseEntityKind.CHARACTER ->
            if (eveWho) EVEWHO_CHARACTER_PATH else ZKILL_CHARACTER_PATH
        UniverseEntityKind.CORPORATION ->
            if (eveWho) EVEWHO_CORPORATION_PATH else ZKILL_CORPORATION_PATH
        UniverseEntityKind.ALLIANCE ->
            if (eveWho) EVEWHO_ALLIANCE_PATH else ZKILL_ALLIANCE_PATH
    }
}

internal data class EntityProfile(
    val ref: UniverseEntityRef,
    val name: String,
    val iconUrl: String?,
    val corporationId: Long? = null,
    val corporationName: String? = null,
    val corporationIconUrl: String? = null,
    val isNpcCorporation: Boolean = false,
    val allianceId: Long? = null,
    val allianceName: String? = null,
    val allianceIconUrl: String? = null,
    val isCeo: Boolean = false,
    val descriptionHtml: String? = null,
    val factionId: Long? = null,
    val factionName: String? = null,
    val factionIconUrl: String? = null,
    val standingSections: List<EntityStandingSection> = emptyList(),
    val employment: List<EntityEmploymentEntry> = emptyList(),
    val detailsReady: Boolean = false,
)

internal data class EntityStandingSection(
    val kind: EntityStandingSectionKind,
    val target: EntityStandingParty,
    val rows: List<EntityStandingRow>,
)

internal enum class EntityStandingSectionKind {
    PERSONAL,
    CORPORATION,
    ALLIANCE,
}

internal data class EntityStandingParty(
    val ref: UniverseEntityRef,
    val name: String,
    val iconUrl: String?,
)

internal data class EntityStandingRow(
    val from: EntityStandingParty,
    val standing: Double,
    val toward: EntityStandingParty,
)

internal data class EntityEmploymentEntry(
    val corporationId: Long,
    val corporationName: String,
    val corporationIconUrl: String?,
    val isNpc: Boolean,
    val allianceId: Long?,
    val allianceName: String?,
    val allianceIconUrl: String?,
    val startEpochMs: Long,
    val endEpochMs: Long?,
)
