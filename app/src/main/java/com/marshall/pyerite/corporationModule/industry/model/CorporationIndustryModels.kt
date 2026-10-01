package com.marshall.pyerite.corporationModule.industry.model

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

internal enum class CorporationIndustryFamily(
    @param:ColorRes val colorRes: Int,
) {
    MANUFACTURING(R.color.corporation_industry_manufacturing),
    RESEARCH(R.color.corporation_industry_research),
    REACTION(R.color.corporation_industry_reaction),
}

internal enum class CorporationIndustryActivity(
    val activityId: Int,
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int,
    val family: CorporationIndustryFamily,
) {
    MANUFACTURING(
        activityId = 1,
        titleRes = R.string.corporation_industry_activity_manufacturing,
        iconRes = R.drawable.ic_industry_manufacturing,
        family = CorporationIndustryFamily.MANUFACTURING,
    ),
    TIME_EFFICIENCY(
        activityId = 3,
        titleRes = R.string.corporation_industry_activity_time,
        iconRes = R.drawable.ic_industry_time_efficiency,
        family = CorporationIndustryFamily.RESEARCH,
    ),
    MATERIAL_EFFICIENCY(
        activityId = 4,
        titleRes = R.string.corporation_industry_activity_material,
        iconRes = R.drawable.ic_industry_material_efficiency,
        family = CorporationIndustryFamily.RESEARCH,
    ),
    COPYING(
        activityId = 5,
        titleRes = R.string.corporation_industry_activity_copying,
        iconRes = R.drawable.ic_industry_copying,
        family = CorporationIndustryFamily.RESEARCH,
    ),
    INVENTION(
        activityId = 8,
        titleRes = R.string.corporation_industry_activity_invention,
        iconRes = R.drawable.ic_industry_invention,
        family = CorporationIndustryFamily.RESEARCH,
    ),
    REACTION(
        activityId = 9,
        titleRes = R.string.corporation_industry_activity_reaction,
        iconRes = R.drawable.ic_industry_reaction,
        family = CorporationIndustryFamily.REACTION,
    ),
    ;

    companion object {
        fun fromId(activityId: Int): CorporationIndustryActivity? =
            entries.firstOrNull { it.activityId == activityId }
    }
}

internal enum class CorporationIndustryJobStatus(
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
        fun fromWire(value: String): CorporationIndustryJobStatus? =
            entries.firstOrNull { it.wireValue == value }
    }
}

internal enum class CorporationIndustryBucket {
    READY,
    DUE_SOON,
    ACTIVE,
    HISTORY,
}

internal class CorporationIndustryAccessException : Exception()

internal data class CorporationIndustryJob(
    val jobId: Long,
    val activity: CorporationIndustryActivity,
    val status: CorporationIndustryJobStatus,
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
        if (status == CorporationIndustryJobStatus.READY) return true
        if (status != CorporationIndustryJobStatus.ACTIVE) return false
        val end = endDateMillis ?: return false
        return end <= nowMs
    }

    fun bucket(nowMs: Long): CorporationIndustryBucket = when {
        status.isHistory -> CorporationIndustryBucket.HISTORY
        isDeliverable(nowMs) -> CorporationIndustryBucket.READY
        status == CorporationIndustryJobStatus.ACTIVE -> {
            val remaining = endDateMillis?.minus(nowMs)
            if (remaining != null &&
                remaining > 0L &&
                remaining <= CorporationIndustryConfig.DUE_SOON_WINDOW_MILLIS
            ) {
                CorporationIndustryBucket.DUE_SOON
            } else {
                CorporationIndustryBucket.ACTIVE
            }
        }
        status == CorporationIndustryJobStatus.PAUSED -> CorporationIndustryBucket.ACTIVE
        else -> CorporationIndustryBucket.HISTORY
    }

    /** Remaining time for jobs that are still running. Paused jobs freeze at `pause_date`. */
    fun remainingMillis(nowMs: Long): Long? {
        if (isDeliverable(nowMs) || status.isHistory) return null
        val end = endDateMillis ?: return null
        val cursor = if (status == CorporationIndustryJobStatus.PAUSED && pauseDateMillis != null) {
            pauseDateMillis
        } else {
            nowMs
        }
        return (end - cursor).coerceAtLeast(0L)
    }

    fun progressFraction(nowMs: Long): Float {
        if (isDeliverable(nowMs) || status == CorporationIndustryJobStatus.DELIVERED) {
            return CorporationIndustryConfig.PROGRESS_COMPLETE
        }
        val start = startDateMillis ?: return CorporationIndustryConfig.PROGRESS_EMPTY
        val end = endDateMillis ?: return CorporationIndustryConfig.PROGRESS_EMPTY
        if (end <= start) return CorporationIndustryConfig.PROGRESS_EMPTY
        val cursor = when {
            status == CorporationIndustryJobStatus.PAUSED && pauseDateMillis != null ->
                pauseDateMillis
            status.isHistory && completedDateMillis != null -> completedDateMillis
            else -> nowMs
        }
        val elapsed = (cursor - start).toFloat()
        val span = (end - start).toFloat()
        return (elapsed / span).coerceIn(
            CorporationIndustryConfig.PROGRESS_EMPTY,
            CorporationIndustryConfig.PROGRESS_COMPLETE,
        )
    }

    fun historyTimeMillis(): Long? = completedDateMillis ?: endDateMillis
}

internal data class CorporationIndustrySnapshot(
    val jobs: List<CorporationIndustryJob>,
)

internal data class CorporationIndustryFilter(
    val hideClosed: Boolean = true,
    val excludedActivities: Set<CorporationIndustryActivity> = emptySet(),
    val excludedInstallerIds: Set<Long> = emptySet(),
    val excludedSolarSystemIds: Set<Long> = emptySet(),
) {
    val allActivitiesSelected: Boolean
        get() = excludedActivities.isEmpty()

    fun matches(job: CorporationIndustryJob): Boolean {
        if (hideClosed && job.status.isHistory) return false
        if (job.activity in excludedActivities) return false
        if (job.installerId in excludedInstallerIds) return false
        val systemId = job.solarSystemId
        if (systemId != null && systemId in excludedSolarSystemIds) return false
        return true
    }

    fun toggleActivity(activity: CorporationIndustryActivity): CorporationIndustryFilter {
        val next = if (activity in excludedActivities) {
            excludedActivities - activity
        } else {
            excludedActivities + activity
        }
        return copy(excludedActivities = next)
    }

    fun toggleAllActivities(): CorporationIndustryFilter =
        if (allActivitiesSelected) {
            copy(excludedActivities = CorporationIndustryActivity.entries.toSet())
        } else {
            copy(excludedActivities = emptySet())
        }

    fun allInstallersSelected(optionIds: Set<Long>): Boolean =
        optionIds.none { it in excludedInstallerIds }

    fun toggleInstaller(installerId: Long): CorporationIndustryFilter {
        val next = if (installerId in excludedInstallerIds) {
            excludedInstallerIds - installerId
        } else {
            excludedInstallerIds + installerId
        }
        return copy(excludedInstallerIds = next)
    }

    fun toggleAllInstallers(optionIds: Set<Long>): CorporationIndustryFilter =
        if (allInstallersSelected(optionIds)) {
            copy(excludedInstallerIds = excludedInstallerIds + optionIds)
        } else {
            copy(excludedInstallerIds = excludedInstallerIds - optionIds)
        }

    fun allSystemsSelected(optionIds: Set<Long>): Boolean =
        optionIds.none { it in excludedSolarSystemIds }

    fun toggleSystem(solarSystemId: Long): CorporationIndustryFilter {
        val next = if (solarSystemId in excludedSolarSystemIds) {
            excludedSolarSystemIds - solarSystemId
        } else {
            excludedSolarSystemIds + solarSystemId
        }
        return copy(excludedSolarSystemIds = next)
    }

    fun toggleAllSystems(optionIds: Set<Long>): CorporationIndustryFilter =
        if (allSystemsSelected(optionIds)) {
            copy(excludedSolarSystemIds = excludedSolarSystemIds + optionIds)
        } else {
            copy(excludedSolarSystemIds = excludedSolarSystemIds - optionIds)
        }
}

internal data class CorporationIndustrySummary(
    val manufacturing: Int,
    val research: Int,
    val reaction: Int,
) {
    val isEmpty: Boolean
        get() = manufacturing == 0 && research == 0 && reaction == 0
}

internal data class CorporationIndustryInstallerOption(
    val installerId: Long,
    val name: String,
    val portraitUrl: String,
)

internal data class CorporationIndustrySystemOption(
    val solarSystemId: Long,
    val systemZhName: String?,
    val systemEnName: String?,
    val systemName: String?,
    val securityStatus: Double?,
) {
    fun displayName(language: ContentLanguage): String =
        localizedName(systemZhName, systemEnName, systemName, language)
}

internal data class CorporationIndustrySections(
    val summary: CorporationIndustrySummary,
    val ready: List<CorporationIndustryJob>,
    val dueSoon: List<CorporationIndustryJob>,
    val active: List<CorporationIndustryJob>,
    val history: List<CorporationIndustryJob>,
    val installers: List<CorporationIndustryInstallerOption>,
    val systems: List<CorporationIndustrySystemOption>,
) {
    val isEmpty: Boolean
        get() = summary.isEmpty &&
            ready.isEmpty() &&
            dueSoon.isEmpty() &&
            active.isEmpty() &&
            history.isEmpty()
}

internal fun List<CorporationIndustryJob>.toSections(
    filter: CorporationIndustryFilter,
    nowMs: Long,
    language: ContentLanguage,
): CorporationIndustrySections {
    val visible = filter { filter.matches(it) }
    val online = visible.filter { it.status.isOnline }
    val summary = CorporationIndustrySummary(
        manufacturing = online.count { it.activity.family == CorporationIndustryFamily.MANUFACTURING },
        research = online.count { it.activity.family == CorporationIndustryFamily.RESEARCH },
        reaction = online.count { it.activity.family == CorporationIndustryFamily.REACTION },
    )
    val readyOrder = compareByDescending<CorporationIndustryJob> { it.jobId }
    val upcomingOrder = compareBy<CorporationIndustryJob> {
        it.endDateMillis ?: Long.MAX_VALUE
    }.thenBy { it.jobId }
    val historyOrder = compareByDescending<CorporationIndustryJob> {
        it.historyTimeMillis() ?: Long.MIN_VALUE
    }.thenByDescending { it.jobId }
    val optionJobs = if (filter.hideClosed) {
        filter { it.status.isOnline }
    } else {
        this
    }
    return CorporationIndustrySections(
        summary = summary,
        ready = visible.filter { it.bucket(nowMs) == CorporationIndustryBucket.READY }.sortedWith(readyOrder),
        dueSoon = visible.filter { it.bucket(nowMs) == CorporationIndustryBucket.DUE_SOON }.sortedWith(upcomingOrder),
        active = visible.filter { it.bucket(nowMs) == CorporationIndustryBucket.ACTIVE }.sortedWith(upcomingOrder),
        history = visible.filter { it.bucket(nowMs) == CorporationIndustryBucket.HISTORY }.sortedWith(historyOrder),
        installers = optionJobs.installerOptions(),
        systems = optionJobs.systemOptions(language),
    )
}

private fun List<CorporationIndustryJob>.installerOptions(): List<CorporationIndustryInstallerOption> =
    distinctBy { it.installerId }
        .map { job ->
            CorporationIndustryInstallerOption(
                installerId = job.installerId,
                name = job.installerName,
                portraitUrl = job.installerPortraitUrl,
            )
        }
        .sortedWith(compareBy({ it.name.isBlank() }, { it.name.lowercase() }))

private fun List<CorporationIndustryJob>.systemOptions(
    language: ContentLanguage,
): List<CorporationIndustrySystemOption> =
    mapNotNull { job ->
        val systemId = job.solarSystemId ?: return@mapNotNull null
        CorporationIndustrySystemOption(
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

internal object CorporationIndustryDateFormatter {
    fun format(epochMs: Long, language: ContentLanguage): String {
        val pattern = when (language) {
            ContentLanguage.CHINESE -> CorporationIndustryConfig.DISPLAY_DATE_TIME_PATTERN_ZH
            ContentLanguage.ENGLISH -> CorporationIndustryConfig.DISPLAY_DATE_TIME_PATTERN_EN
        }
        val locale = when (language) {
            ContentLanguage.CHINESE -> Locale.SIMPLIFIED_CHINESE
            ContentLanguage.ENGLISH -> Locale.US
        }
        return SimpleDateFormat(pattern, locale).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date(epochMs))
    }
}
