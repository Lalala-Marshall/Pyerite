package com.marshall.pyerite.industryCommon.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.marshall.pyerite.R
import com.marshall.pyerite.localization.ContentLanguage
import com.marshall.pyerite.localization.localizedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal enum class IndustryFamily(
    @param:ColorRes val colorRes: Int,
) {
    MANUFACTURING(R.color.corporation_industry_manufacturing),
    RESEARCH(R.color.corporation_industry_research),
    REACTION(R.color.corporation_industry_reaction),
}

internal enum class IndustryActivity(
    val activityId: Int,
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int,
    val family: IndustryFamily,
) {
    MANUFACTURING(
        activityId = 1,
        titleRes = R.string.corporation_industry_activity_manufacturing,
        iconRes = R.drawable.ic_industry_manufacturing,
        family = IndustryFamily.MANUFACTURING,
    ),
    TIME_EFFICIENCY(
        activityId = 3,
        titleRes = R.string.corporation_industry_activity_time,
        iconRes = R.drawable.ic_industry_time_efficiency,
        family = IndustryFamily.RESEARCH,
    ),
    MATERIAL_EFFICIENCY(
        activityId = 4,
        titleRes = R.string.corporation_industry_activity_material,
        iconRes = R.drawable.ic_industry_material_efficiency,
        family = IndustryFamily.RESEARCH,
    ),
    COPYING(
        activityId = 5,
        titleRes = R.string.corporation_industry_activity_copying,
        iconRes = R.drawable.ic_industry_copying,
        family = IndustryFamily.RESEARCH,
    ),
    INVENTION(
        activityId = 8,
        titleRes = R.string.corporation_industry_activity_invention,
        iconRes = R.drawable.ic_industry_invention,
        family = IndustryFamily.RESEARCH,
    ),
    REACTION(
        activityId = 9,
        titleRes = R.string.corporation_industry_activity_reaction,
        iconRes = R.drawable.ic_industry_reaction,
        family = IndustryFamily.REACTION,
    ),
    ;

    companion object {
        fun fromId(activityId: Int): IndustryActivity? =
            entries.firstOrNull { it.activityId == activityId }
    }
}

internal enum class IndustryJobStatus(
    val wireValue: String,
    @param:StringRes val historyTitleRes: Int? = null,
) {
    ACTIVE("active"),
    PAUSED("paused"),
    READY("ready"),
    DELIVERED("delivered", R.string.corporation_industry_status_delivered),
    CANCELLED("cancelled", R.string.corporation_industry_status_cancelled),
    REVERTED("reverted", R.string.corporation_industry_status_reverted),
    ;

    val isOnline: Boolean
        get() = this == ACTIVE || this == PAUSED || this == READY

    val isHistory: Boolean
        get() = this == DELIVERED || this == CANCELLED || this == REVERTED

    companion object {
        fun fromWire(value: String): IndustryJobStatus? =
            entries.firstOrNull { it.wireValue == value }
    }
}

internal enum class IndustryBucket {
    READY,
    DUE_SOON,
    ACTIVE,
    HISTORY,
}

internal class IndustryAccessException : Exception()

internal data class IndustryJob(
    val jobId: Long,
    val activity: IndustryActivity,
    val status: IndustryJobStatus,
    val runs: Int,
    val detailTypeId: Int,
    val blueprintZhName: String?,
    val blueprintEnName: String?,
    val blueprintName: String?,
    val blueprintIconFilename: String?,
    val installerId: Long,
    val installerName: String,
    val installerPortraitUrl: String,
    val facilityName: String,
    val solarSystemId: Long?,
    val systemZhName: String?,
    val systemEnName: String?,
    val systemName: String?,
    val securityStatus: Double?,
    val startDateMillis: Long?,
    val endDateMillis: Long?,
    val pauseDateMillis: Long?,
    val completedDateMillis: Long?,
) {
    fun blueprintDisplayName(language: ContentLanguage): String =
        localizedName(blueprintZhName, blueprintEnName, blueprintName, language)

    fun systemDisplayName(language: ContentLanguage): String =
        localizedName(systemZhName, systemEnName, systemName, language)

    /** Finished and waiting for delivery: ESI `ready`, or still `active` after `end_date`. */
    fun isDeliverable(nowMs: Long): Boolean {
        if (status == IndustryJobStatus.READY) return true
        if (status != IndustryJobStatus.ACTIVE) return false
        val end = endDateMillis ?: return false
        return end <= nowMs
    }

    fun bucket(nowMs: Long): IndustryBucket = when {
        status.isHistory -> IndustryBucket.HISTORY
        isDeliverable(nowMs) -> IndustryBucket.READY
        status == IndustryJobStatus.ACTIVE -> {
            val remaining = endDateMillis?.minus(nowMs)
            if (remaining != null &&
                remaining > 0L &&
                remaining <= IndustryConfig.DUE_SOON_WINDOW_MILLIS
            ) {
                IndustryBucket.DUE_SOON
            } else {
                IndustryBucket.ACTIVE
            }
        }
        status == IndustryJobStatus.PAUSED -> IndustryBucket.ACTIVE
        else -> IndustryBucket.HISTORY
    }

    /** Remaining time for jobs that are still running. Paused jobs freeze at `pause_date`. */
    fun remainingMillis(nowMs: Long): Long? {
        if (isDeliverable(nowMs) || status.isHistory) return null
        val end = endDateMillis ?: return null
        val cursor = if (status == IndustryJobStatus.PAUSED && pauseDateMillis != null) {
            pauseDateMillis
        } else {
            nowMs
        }
        return (end - cursor).coerceAtLeast(0L)
    }

    fun progressFraction(nowMs: Long): Float {
        if (isDeliverable(nowMs) || status == IndustryJobStatus.DELIVERED) {
            return IndustryConfig.PROGRESS_COMPLETE
        }
        val start = startDateMillis ?: return IndustryConfig.PROGRESS_EMPTY
        val end = endDateMillis ?: return IndustryConfig.PROGRESS_EMPTY
        if (end <= start) return IndustryConfig.PROGRESS_EMPTY
        val cursor = when {
            status == IndustryJobStatus.PAUSED && pauseDateMillis != null ->
                pauseDateMillis
            status.isHistory && completedDateMillis != null -> completedDateMillis
            else -> nowMs
        }
        val elapsed = (cursor - start).toFloat()
        val span = (end - start).toFloat()
        return (elapsed / span).coerceIn(
            IndustryConfig.PROGRESS_EMPTY,
            IndustryConfig.PROGRESS_COMPLETE,
        )
    }

    fun historyTimeMillis(): Long? = completedDateMillis ?: endDateMillis
}

internal data class IndustrySnapshot(
    val jobs: List<IndustryJob>,
)

internal data class IndustryFilter(
    val hideClosed: Boolean = true,
    val excludedActivities: Set<IndustryActivity> = emptySet(),
    val excludedInstallerIds: Set<Long> = emptySet(),
    val excludedSolarSystemIds: Set<Long> = emptySet(),
) {
    val allActivitiesSelected: Boolean
        get() = excludedActivities.isEmpty()

    fun matches(job: IndustryJob): Boolean {
        if (hideClosed && job.status.isHistory) return false
        if (job.activity in excludedActivities) return false
        if (job.installerId in excludedInstallerIds) return false
        val systemId = job.solarSystemId
        if (systemId != null && systemId in excludedSolarSystemIds) return false
        return true
    }

    fun toggleActivity(activity: IndustryActivity): IndustryFilter {
        val next = if (activity in excludedActivities) {
            excludedActivities - activity
        } else {
            excludedActivities + activity
        }
        return copy(excludedActivities = next)
    }

    fun toggleAllActivities(): IndustryFilter =
        if (allActivitiesSelected) {
            copy(excludedActivities = IndustryActivity.entries.toSet())
        } else {
            copy(excludedActivities = emptySet())
        }

    fun allInstallersSelected(optionIds: Set<Long>): Boolean =
        optionIds.none { it in excludedInstallerIds }

    fun toggleInstaller(installerId: Long): IndustryFilter {
        val next = if (installerId in excludedInstallerIds) {
            excludedInstallerIds - installerId
        } else {
            excludedInstallerIds + installerId
        }
        return copy(excludedInstallerIds = next)
    }

    fun toggleAllInstallers(optionIds: Set<Long>): IndustryFilter =
        if (allInstallersSelected(optionIds)) {
            copy(excludedInstallerIds = excludedInstallerIds + optionIds)
        } else {
            copy(excludedInstallerIds = excludedInstallerIds - optionIds)
        }

    fun allSystemsSelected(optionIds: Set<Long>): Boolean =
        optionIds.none { it in excludedSolarSystemIds }

    fun toggleSystem(solarSystemId: Long): IndustryFilter {
        val next = if (solarSystemId in excludedSolarSystemIds) {
            excludedSolarSystemIds - solarSystemId
        } else {
            excludedSolarSystemIds + solarSystemId
        }
        return copy(excludedSolarSystemIds = next)
    }

    fun toggleAllSystems(optionIds: Set<Long>): IndustryFilter =
        if (allSystemsSelected(optionIds)) {
            copy(excludedSolarSystemIds = excludedSolarSystemIds + optionIds)
        } else {
            copy(excludedSolarSystemIds = excludedSolarSystemIds - optionIds)
        }
}

internal data class IndustrySummary(
    val manufacturing: Int,
    val research: Int,
    val reaction: Int,
) {
    val isEmpty: Boolean
        get() = manufacturing == 0 && research == 0 && reaction == 0
}

internal data class IndustryInstallerOption(
    val installerId: Long,
    val name: String,
    val portraitUrl: String,
)

internal data class IndustrySystemOption(
    val solarSystemId: Long,
    val systemZhName: String?,
    val systemEnName: String?,
    val systemName: String?,
    val securityStatus: Double?,
) {
    fun displayName(language: ContentLanguage): String =
        localizedName(systemZhName, systemEnName, systemName, language)
}

internal data class IndustrySections(
    val summary: IndustrySummary,
    val ready: List<IndustryJob>,
    val dueSoon: List<IndustryJob>,
    val active: List<IndustryJob>,
    val history: List<IndustryJob>,
    val installers: List<IndustryInstallerOption>,
    val systems: List<IndustrySystemOption>,
) {
    val isEmpty: Boolean
        get() = summary.isEmpty &&
            ready.isEmpty() &&
            dueSoon.isEmpty() &&
            active.isEmpty() &&
            history.isEmpty()
}

internal fun List<IndustryJob>.toSections(
    filter: IndustryFilter,
    nowMs: Long,
    language: ContentLanguage,
): IndustrySections {
    val visible = filter { filter.matches(it) }
    val online = visible.filter { it.status.isOnline }
    val summary = IndustrySummary(
        manufacturing = online.count { it.activity.family == IndustryFamily.MANUFACTURING },
        research = online.count { it.activity.family == IndustryFamily.RESEARCH },
        reaction = online.count { it.activity.family == IndustryFamily.REACTION },
    )
    val readyOrder = compareByDescending<IndustryJob> { it.jobId }
    val upcomingOrder = compareBy<IndustryJob> {
        it.endDateMillis ?: Long.MAX_VALUE
    }.thenBy { it.jobId }
    val historyOrder = compareByDescending<IndustryJob> {
        it.historyTimeMillis() ?: Long.MIN_VALUE
    }.thenByDescending { it.jobId }
    val optionJobs = if (filter.hideClosed) {
        filter { it.status.isOnline }
    } else {
        this
    }
    return IndustrySections(
        summary = summary,
        ready = visible.filter { it.bucket(nowMs) == IndustryBucket.READY }.sortedWith(readyOrder),
        dueSoon = visible.filter { it.bucket(nowMs) == IndustryBucket.DUE_SOON }.sortedWith(upcomingOrder),
        active = visible.filter { it.bucket(nowMs) == IndustryBucket.ACTIVE }.sortedWith(upcomingOrder),
        history = visible.filter { it.bucket(nowMs) == IndustryBucket.HISTORY }.sortedWith(historyOrder),
        installers = optionJobs.installerOptions(),
        systems = optionJobs.systemOptions(language),
    )
}

private fun List<IndustryJob>.installerOptions(): List<IndustryInstallerOption> =
    distinctBy { it.installerId }
        .map { job ->
            IndustryInstallerOption(
                installerId = job.installerId,
                name = job.installerName,
                portraitUrl = job.installerPortraitUrl,
            )
        }
        .sortedWith(compareBy({ it.name.isBlank() }, { it.name.lowercase() }))

private fun List<IndustryJob>.systemOptions(
    language: ContentLanguage,
): List<IndustrySystemOption> =
    mapNotNull { job ->
        val systemId = job.solarSystemId ?: return@mapNotNull null
        IndustrySystemOption(
            solarSystemId = systemId,
            systemZhName = job.systemZhName,
            systemEnName = job.systemEnName,
            systemName = job.systemName,
            securityStatus = job.securityStatus,
        )
    }
        .distinctBy { it.solarSystemId }
        .sortedWith(
            compareBy(
                { it.displayName(language).isBlank() },
                { it.displayName(language) },
            ),
        )

internal object IndustryDateFormatter {
    fun format(epochMs: Long): String =
        SimpleDateFormat(IndustryConfig.DISPLAY_DATE_TIME_PATTERN, Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date(epochMs))
}
