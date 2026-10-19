package com.marshall.pyerite.charactersListModule.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.marshall.pyerite.charactersListModule.model.CharacterLocationInfo
import com.marshall.pyerite.charactersListModule.model.CharacterLocationPresence
import com.marshall.pyerite.charactersListModule.model.LoggedInCharacter
import com.marshall.pyerite.charactersListModule.model.SkillQueueEntry
import com.marshall.pyerite.charactersListModule.model.SkillQueueProgress
import com.marshall.pyerite.charactersListModule.model.SkillQueueTrainingState
import com.marshall.pyerite.infra.network.PyeriteJson
import com.marshall.pyerite.esiModule.data.portraitUrl
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.eveAuthModule.model.EveStoredSession
import kotlinx.serialization.Serializable

/**
 * Disk cache of non-secret [LoggedInCharacter] snapshots for instant UI on cold start.
 * Never stores tokens.
 */
internal class CharacterProfileCache(
    context: Context,
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(characterId: Long): LoggedInCharacter? {
        val raw = prefs.getString(keyFor(characterId), null) ?: return null
        return runCatching {
            PyeriteJson.decodeFromString<CachedCharacterProfile>(raw).toLoggedInCharacter()
        }.getOrNull()
    }

    fun save(character: LoggedInCharacter) {
        val encoded = PyeriteJson.encodeToString(CachedCharacterProfile.from(character))
        prefs.edit { putString(keyFor(character.characterId), encoded) }
    }

    fun remove(characterId: Long) {
        prefs.edit { remove(keyFor(characterId)) }
    }

    private fun keyFor(characterId: Long): String = "$KEY_PREFIX$characterId"

    private companion object {
        const val PREFS_NAME = "pyerite_character_profile_cache"
        const val KEY_PREFIX = "profile_"
    }
}

@Serializable
private data class CachedCharacterProfile(
    val characterId: Long,
    val name: String,
    val portraitUrl: String? = null,
    val location: CachedLocationInfo? = null,
    val walletBalance: String? = null,
    val totalSp: Long? = null,
    val totalSkillPoints: String? = null,
    val unallocatedSkillPoints: String? = null,
    val corporationName: String? = null,
    val corporationIconUrl: String? = null,
    val allianceName: String? = null,
    val allianceIconUrl: String? = null,
    val skillQueue: CachedSkillQueueProgress? = null,
    val grantedScopeValues: List<String> = emptyList(),
    val corporationId: Long? = null,
) {
    fun toLoggedInCharacter(): LoggedInCharacter = LoggedInCharacter(
        characterId = characterId,
        name = name,
        portraitUrl = portraitUrl,
        location = location?.toModel(),
        walletBalance = walletBalance,
        totalSp = totalSp,
        totalSkillPoints = totalSkillPoints,
        unallocatedSkillPoints = unallocatedSkillPoints,
        corporationName = corporationName,
        corporationIconUrl = corporationIconUrl,
        allianceName = allianceName,
        allianceIconUrl = allianceIconUrl,
        skillQueue = skillQueue?.toModel(),
        grantedScopes = EveSsoScope.parseGranted(grantedScopeValues),
        corporationId = corporationId,
    )

    companion object {
        fun from(character: LoggedInCharacter): CachedCharacterProfile = CachedCharacterProfile(
            characterId = character.characterId,
            name = character.name,
            portraitUrl = character.portraitUrl,
            location = character.location?.let(CachedLocationInfo::from),
            walletBalance = character.walletBalance,
            totalSp = character.totalSp,
            totalSkillPoints = character.totalSkillPoints,
            unallocatedSkillPoints = character.unallocatedSkillPoints,
            corporationName = character.corporationName,
            corporationIconUrl = character.corporationIconUrl,
            allianceName = character.allianceName,
            allianceIconUrl = character.allianceIconUrl,
            skillQueue = character.skillQueue?.let(CachedSkillQueueProgress::from),
            grantedScopeValues = character.grantedScopes.map { it.apiValue },
            corporationId = character.corporationId,
        )
    }
}

@Serializable
private data class CachedLocationInfo(
    val systemSecurityStatus: Double,
    val systemName: String,
    val regionName: String,
    val presence: String,
) {
    fun toModel(): CharacterLocationInfo = CharacterLocationInfo(
        systemSecurityStatus = systemSecurityStatus,
        systemName = systemName,
        regionName = regionName,
        presence = runCatching { CharacterLocationPresence.valueOf(presence) }
            .getOrDefault(CharacterLocationPresence.IN_SPACE),
    )

    companion object {
        fun from(info: CharacterLocationInfo): CachedLocationInfo = CachedLocationInfo(
            systemSecurityStatus = info.systemSecurityStatus,
            systemName = info.systemName,
            regionName = info.regionName,
            presence = info.presence.name,
        )
    }
}

@Serializable
private data class CachedSkillQueueProgress(
    val state: String,
    val entries: List<CachedSkillQueueEntry> = emptyList(),
) {
    fun toModel(): SkillQueueProgress = SkillQueueProgress(
        state = runCatching { SkillQueueTrainingState.valueOf(state) }
            .getOrDefault(SkillQueueTrainingState.IDLE),
        entries = entries.map { it.toModel() },
    )

    companion object {
        fun from(queue: SkillQueueProgress): CachedSkillQueueProgress = CachedSkillQueueProgress(
            state = queue.state.name,
            entries = queue.entries.map(CachedSkillQueueEntry::from),
        )
    }
}

@Serializable
private data class CachedSkillQueueEntry(
    val skillName: String,
    val level: Int,
    val startAtEpochMs: Long? = null,
    val finishAtEpochMs: Long? = null,
    val trainingStartSp: Long? = null,
    val levelStartSp: Long? = null,
    val levelEndSp: Long? = null,
) {
    fun toModel(): SkillQueueEntry = SkillQueueEntry(
        skillName = skillName,
        level = level,
        startAtEpochMs = startAtEpochMs,
        finishAtEpochMs = finishAtEpochMs,
        trainingStartSp = trainingStartSp,
        levelStartSp = levelStartSp,
        levelEndSp = levelEndSp,
    )

    companion object {
        fun from(entry: SkillQueueEntry): CachedSkillQueueEntry = CachedSkillQueueEntry(
            skillName = entry.skillName,
            level = entry.level,
            startAtEpochMs = entry.startAtEpochMs,
            finishAtEpochMs = entry.finishAtEpochMs,
            trainingStartSp = entry.trainingStartSp,
            levelStartSp = entry.levelStartSp,
            levelEndSp = entry.levelEndSp,
        )
    }
}

/** Minimal local session row — must show before any network. */
internal fun localLoggedInCharacter(
    session: EveStoredSession,
    grantedScopes: Set<EveSsoScope>,
): LoggedInCharacter = LoggedInCharacter(
    characterId = session.characterId,
    name = session.characterName.ifBlank { session.characterId.toString() },
    portraitUrl = portraitUrl(session.characterId),
    location = null,
    walletBalance = null,
    totalSp = null,
    totalSkillPoints = null,
    unallocatedSkillPoints = null,
    corporationName = null,
    corporationIconUrl = null,
    allianceName = null,
    allianceIconUrl = null,
    skillQueue = null,
    grantedScopes = grantedScopes,
)
