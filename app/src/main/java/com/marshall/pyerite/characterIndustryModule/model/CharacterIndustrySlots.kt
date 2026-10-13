package com.marshall.pyerite.characterIndustryModule.model

import com.marshall.pyerite.industryCommon.model.IndustryFamily
import com.marshall.pyerite.industryCommon.model.IndustryJob

/** Max manufacturing, science, and reaction lines from skills. Null when skills could not be read. */
internal data class CharacterIndustrySlotCapacity(
    val manufacturing: Int,
    val research: Int,
    val reaction: Int,
)

internal data class CharacterIndustrySlotLine(
    val used: Int,
    val max: Int?,
)

internal data class CharacterIndustrySlotSummary(
    val manufacturing: CharacterIndustrySlotLine,
    val research: CharacterIndustrySlotLine,
    val reaction: CharacterIndustrySlotLine,
) {
    operator fun plus(other: CharacterIndustrySlotSummary): CharacterIndustrySlotSummary =
        CharacterIndustrySlotSummary(
            manufacturing = manufacturing + other.manufacturing,
            research = research + other.research,
            reaction = reaction + other.reaction,
        )
}

internal fun List<CharacterIndustrySlotSummary>.summed(): CharacterIndustrySlotSummary {
    if (isEmpty()) return UNKNOWN_SLOT_SUMMARY
    return fold(ZERO_SLOT_SUMMARY) { total, line -> total + line }
}

/** Online jobs occupy a line until they are delivered, cancelled, or reverted. */
internal fun slotSummaryFor(
    characterId: Long,
    jobs: List<IndustryJob>,
    capacity: CharacterIndustrySlotCapacity?,
): CharacterIndustrySlotSummary {
    val online = jobs.filter { job ->
        job.installerId == characterId && job.status.isOnline
    }
    fun used(family: IndustryFamily): Int = online.count { it.activity.family == family }
    return CharacterIndustrySlotSummary(
        manufacturing = CharacterIndustrySlotLine(
            used = used(IndustryFamily.MANUFACTURING),
            max = capacity?.manufacturing,
        ),
        research = CharacterIndustrySlotLine(
            used = used(IndustryFamily.RESEARCH),
            max = capacity?.research,
        ),
        reaction = CharacterIndustrySlotLine(
            used = used(IndustryFamily.REACTION),
            max = capacity?.reaction,
        ),
    )
}

private operator fun CharacterIndustrySlotLine.plus(
    other: CharacterIndustrySlotLine,
): CharacterIndustrySlotLine {
    val leftMax = max
    val rightMax = other.max
    return CharacterIndustrySlotLine(
        used = used + other.used,
        max = if (leftMax == null || rightMax == null) null else leftMax + rightMax,
    )
}

private val ZERO_SLOT_SUMMARY = CharacterIndustrySlotSummary(
    manufacturing = CharacterIndustrySlotLine(used = 0, max = 0),
    research = CharacterIndustrySlotLine(used = 0, max = 0),
    reaction = CharacterIndustrySlotLine(used = 0, max = 0),
)

private val UNKNOWN_SLOT_SUMMARY = CharacterIndustrySlotSummary(
    manufacturing = CharacterIndustrySlotLine(used = 0, max = null),
    research = CharacterIndustrySlotLine(used = 0, max = null),
    reaction = CharacterIndustrySlotLine(used = 0, max = null),
)
