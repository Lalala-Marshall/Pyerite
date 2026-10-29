package com.marshall.pyerite.agentsModule.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/** Shared agent-search ids, security bands, and display rules. */
internal object AgentsConfig {
    const val UNSET_ID = -1

    const val BASIC_AGENT_TYPE = 2
    const val LOCATOR_FLAG = 1

    const val SECURITY_FORMAT = "%.1f"
    const val SECURITY_NULL_EXCLUSIVE_MAX = 0.0
    const val SECURITY_HIGH_MIN = 0.5

    const val SECTION_OTHER = "#"

    /** Security, Mining, Distribution, R&D, then the remaining divisions by id. */
    val MAIN_DIVISION_IDS: List<Int> = listOf(
        DIVISION_SECURITY,
        DIVISION_MINING,
        DIVISION_DISTRIBUTION,
        DIVISION_RESEARCH,
    )

    /** Basic, Research, generic storyline, faction warfare. */
    val MAIN_AGENT_TYPE_IDS: List<Int> = listOf(
        TYPE_BASIC,
        TYPE_RESEARCH,
        TYPE_GENERIC_STORYLINE,
        TYPE_FACTION_WARFARE,
    )

    /** CONCORD, epic arc, career. */
    val SECONDARY_AGENT_TYPE_IDS: List<Int> = listOf(
        TYPE_CONCORD,
        TYPE_EPIC_ARC,
        TYPE_CAREER,
    )

    const val DIVISION_RESEARCH = 18
    const val DIVISION_DISTRIBUTION = 22
    const val DIVISION_MINING = 23
    const val DIVISION_SECURITY = 24

    const val TYPE_NON_AGENT = 1
    const val TYPE_BASIC = 2
    const val TYPE_TUTORIAL = 3
    const val TYPE_RESEARCH = 4
    const val TYPE_CONCORD = 5
    const val TYPE_GENERIC_STORYLINE = 6
    const val TYPE_STORYLINE = 7
    const val TYPE_EVENT = 8
    const val TYPE_FACTION_WARFARE = 9
    const val TYPE_EPIC_ARC = 10
    const val TYPE_AURA = 11
    const val TYPE_CAREER = 12
    const val TYPE_HERALDRY = 13

    const val LEVEL_MIN = 1
    const val LEVEL_MAX = 5
}

internal enum class AgentsSecurityBand {
    HIGH,
    LOW,
    NULL,
}

internal enum class AgentsResultLevel {
    FACTION,
    CORPORATION,
    DIVISION,
    LEVEL,
    AGENT,
    ;

    fun next(): AgentsResultLevel = when (this) {
        FACTION -> CORPORATION
        CORPORATION -> DIVISION
        DIVISION -> LEVEL
        LEVEL -> AGENT
        AGENT -> AGENT
    }
}

/**
 * One-decimal display security. Half-up so 0.45 becomes 0.5 (high-sec),
 * matching the band Tritanium searches on.
 */
internal fun displaySecurity(security: Double): Double =
    BigDecimal.valueOf(security)
        .setScale(1, RoundingMode.HALF_UP)
        .toDouble()

internal fun formatDisplaySecurity(security: Double): String =
    String.format(Locale.US, AgentsConfig.SECURITY_FORMAT, displaySecurity(security))

internal fun AgentsSecurityBand.matches(security: Double?): Boolean {
    if (security == null) return false
    val display = displaySecurity(security)
    return when (this) {
        AgentsSecurityBand.HIGH -> display >= AgentsConfig.SECURITY_HIGH_MIN
        AgentsSecurityBand.LOW ->
            display >= AgentsConfig.SECURITY_NULL_EXCLUSIVE_MAX &&
                display < AgentsConfig.SECURITY_HIGH_MIN
        AgentsSecurityBand.NULL -> display < AgentsConfig.SECURITY_NULL_EXCLUSIVE_MAX
    }
}
