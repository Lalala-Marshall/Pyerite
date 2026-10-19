package com.marshall.pyerite.charactersListModule.model

import com.marshall.pyerite.eveAuthModule.model.EveSsoScope

data class CharacterSummary(
    val characterId: Long,
    val name: String,
    val portraitUrl: String?,
    val corporationName: String?,
    val corporationIconUrl: String?,
    val allianceName: String?,
    val allianceIconUrl: String?,
)

enum class CharacterLocationPresence {
    IN_STRUCTURE,
    IN_SPACE,
}

/** Resolved location line: system security, system/region names, docked vs space. */
data class CharacterLocationInfo(
    val systemSecurityStatus: Double,
    val systemName: String,
    val regionName: String,
    val presence: CharacterLocationPresence,
)

enum class SkillQueueTrainingState {
    /** Empty queue from ESI. */
    IDLE,

    /** Queue present but start/finish dates omitted (training paused). */
    PAUSED,

    /** At least one entry has projected finish time. */
    TRAINING,
}

/**
 * One ESI skill-queue row, with names resolved and dates parsed.
 */
data class SkillQueueEntry(
    val skillName: String,
    val level: Int,
    val startAtEpochMs: Long?,
    val finishAtEpochMs: Long?,
    val trainingStartSp: Long?,
    val levelStartSp: Long?,
    val levelEndSp: Long?,
) {
    fun remainingSecondsAt(nowMs: Long): Long {
        val finishMs = finishAtEpochMs ?: return 0L
        val deltaMs = finishMs - nowMs
        if (deltaMs <= 0L) return 0L
        return deltaMs / SkillQueueDisplayConfig.MILLIS_PER_SECOND
    }

    /**
     * Level progress (0…1). With dates, interpolates SP over the training window;
     * without dates (paused), uses [trainingStartSp] as the frozen current SP.
     */
    fun progressAt(nowMs: Long): Float {
        val levelStart = levelStartSp
        val levelEnd = levelEndSp
        val trainingStart = trainingStartSp
        val finishMs = finishAtEpochMs

        if (finishMs != null) {
            val timeFraction = timeFractionAt(nowMs)
            if (trainingStart != null && levelEnd != null && levelEnd > trainingStart) {
                val currentSp = trainingStart +
                    (levelEnd - trainingStart).toDouble() * timeFraction.toDouble()
                return spProgressWithinLevel(currentSp, levelStart, levelEnd)
                    ?: timeFraction
            }
            return timeFraction
        }

        if (trainingStart != null) {
            return spProgressWithinLevel(
                currentSp = trainingStart.toDouble(),
                levelStart = levelStart,
                levelEnd = levelEnd,
            ) ?: SkillQueueDisplayConfig.PROGRESS_MIN
        }
        return SkillQueueDisplayConfig.PROGRESS_MIN
    }

    private fun timeFractionAt(nowMs: Long): Float {
        val finishMs = finishAtEpochMs ?: return SkillQueueDisplayConfig.PROGRESS_MIN
        val startMs = startAtEpochMs
            ?: return if (nowMs >= finishMs) {
                SkillQueueDisplayConfig.PROGRESS_MAX
            } else {
                SkillQueueDisplayConfig.PROGRESS_MIN
            }
        if (finishMs <= startMs) return SkillQueueDisplayConfig.PROGRESS_MIN
        if (nowMs <= startMs) return SkillQueueDisplayConfig.PROGRESS_MIN
        if (nowMs >= finishMs) return SkillQueueDisplayConfig.PROGRESS_MAX
        return ((nowMs - startMs).toDouble() / (finishMs - startMs).toDouble())
            .toFloat()
            .coerceIn(
                SkillQueueDisplayConfig.PROGRESS_MIN,
                SkillQueueDisplayConfig.PROGRESS_MAX,
            )
    }

    private fun spProgressWithinLevel(
        currentSp: Double,
        levelStart: Long?,
        levelEnd: Long?,
    ): Float? {
        if (levelStart == null || levelEnd == null || levelEnd <= levelStart) return null
        return ((currentSp - levelStart) / (levelEnd - levelStart).toDouble())
            .toFloat()
            .coerceIn(
                SkillQueueDisplayConfig.PROGRESS_MIN,
                SkillQueueDisplayConfig.PROGRESS_MAX,
            )
    }
}

/**
 * Skill-queue snapshot for a character. [entries] stay ordered by ESI queue position
 * so the UI can advance to the next skill when the current one finishes.
 */
data class SkillQueueProgress(
    val state: SkillQueueTrainingState,
    val entries: List<SkillQueueEntry>,
) {
    fun snapshotAt(nowMs: Long): SkillQueueSnapshot {
        when (state) {
            SkillQueueTrainingState.IDLE -> {
                return SkillQueueSnapshot(
                    state = SkillQueueTrainingState.IDLE,
                    entry = null,
                    progress = SkillQueueDisplayConfig.PROGRESS_MIN,
                    remainingSeconds = 0L,
                )
            }
            SkillQueueTrainingState.PAUSED -> {
                val entry = entries.firstOrNull()
                return SkillQueueSnapshot(
                    state = SkillQueueTrainingState.PAUSED,
                    entry = entry,
                    progress = entry?.progressAt(nowMs) ?: SkillQueueDisplayConfig.PROGRESS_MIN,
                    remainingSeconds = 0L,
                )
            }
            SkillQueueTrainingState.TRAINING -> {
                val active = entries.firstOrNull { entry ->
                    val finishMs = entry.finishAtEpochMs ?: return@firstOrNull false
                    finishMs > nowMs
                }
                if (active == null) {
                    return SkillQueueSnapshot(
                        state = SkillQueueTrainingState.IDLE,
                        entry = null,
                        progress = SkillQueueDisplayConfig.PROGRESS_MIN,
                        remainingSeconds = 0L,
                    )
                }
                return SkillQueueSnapshot(
                    state = SkillQueueTrainingState.TRAINING,
                    entry = active,
                    progress = active.progressAt(nowMs),
                    remainingSeconds = active.remainingSecondsAt(nowMs),
                )
            }
        }
    }
}

data class SkillQueueSnapshot(
    val state: SkillQueueTrainingState,
    val entry: SkillQueueEntry?,
    val progress: Float,
    val remainingSeconds: Long,
)

internal object SkillQueueDisplayConfig {
    const val MILLIS_PER_SECOND = 1_000L
    const val UI_TICK_MS = MILLIS_PER_SECOND
    const val PROGRESS_MIN = 0f
    const val PROGRESS_MAX = 1f
    const val SHIMMER_DURATION_MS = 2_400
    const val SHIMMER_WIDTH_FRACTION = 0.4f
    const val SHIMMER_PEAK_ALPHA = 0.55f
}

data class LoggedInCharacter(
    val characterId: Long,
    val name: String,
    val portraitUrl: String?,
    val location: CharacterLocationInfo?,
    val walletBalance: String?,
    val totalSp: Long? = null,
    val totalSkillPoints: String?,
    val unallocatedSkillPoints: String?,
    val corporationName: String?,
    val corporationIconUrl: String?,
    val allianceName: String?,
    val allianceIconUrl: String?,
    val skillQueue: SkillQueueProgress?,
    /** Parsed SSO scopes for UI / feature gates; no token material. */
    val grantedScopes: Set<EveSsoScope> = emptySet(),
    val corporationId: Long? = null,
)
