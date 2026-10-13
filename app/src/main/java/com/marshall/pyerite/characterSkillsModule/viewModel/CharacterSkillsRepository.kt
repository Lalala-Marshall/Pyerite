package com.marshall.pyerite.characterSkillsModule.viewModel

import com.marshall.pyerite.characterSkillsModule.data.CharacterSkillsCache
import com.marshall.pyerite.characterSkillsModule.data.CharacterSkillsLoader
import com.marshall.pyerite.characterSkillsModule.model.CharacterAttributes
import com.marshall.pyerite.characterSkillsModule.model.CharacterSkillPoints
import com.marshall.pyerite.characterSkillsModule.model.CharacterSkillQueueState
import com.marshall.pyerite.characterSkillsModule.model.CharacterSkillQueueStatus
import com.marshall.pyerite.characterSkillsModule.model.ImplantAttributeBonuses
import com.marshall.pyerite.characterSkillsModule.model.SkillCatalogGroup
import com.marshall.pyerite.characterSkillsModule.model.SkillCatalogLoadResult
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.infra.notification.SkillQueueExpiryScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class CharacterSkillsRepository internal constructor(
    private val skillsLoader: CharacterSkillsLoader,
    private val skillsCache: CharacterSkillsCache,
    private val tokenManager: EveTokenManager,
    private val skillQueueExpiryScheduler: SkillQueueExpiryScheduler,
) {
    private val statusByCharacterId = ConcurrentHashMap<Long, CharacterSkillQueueStatus>()
    private val attributesByCharacterId = ConcurrentHashMap<Long, CharacterAttributes>()
    private val catalogByCharacterId = ConcurrentHashMap<Long, List<SkillCatalogGroup>>()
    private val skillPointsByCharacterId = ConcurrentHashMap<Long, CharacterSkillPoints>()
    private val implantBonusesByCharacterId = ConcurrentHashMap<Long, ImplantAttributeBonuses>()

    /** Memory first, then disk — for instant main-page hint. */
    fun cachedStatus(characterId: Long): CharacterSkillQueueStatus? {
        statusByCharacterId[characterId]?.let { return it }
        return skillsCache.get(characterId)?.also { cached ->
            statusByCharacterId[characterId] = cached
        }
    }

    suspend fun loadStatus(characterId: Long): CharacterSkillQueueStatus =
        withContext(Dispatchers.IO) {
            val loaded = skillsLoader.load(characterId)
            statusByCharacterId[characterId] = loaded
            skillsCache.save(loaded)
            reportSkillQueue(loaded)
            loaded
        }

    fun cachedAttributes(characterId: Long): CharacterAttributes? {
        attributesByCharacterId[characterId]?.let { return it }
        return skillsCache.getAttributes(characterId)?.also { cached ->
            attributesByCharacterId[characterId] = cached
        }
    }

    suspend fun loadAttributes(characterId: Long): CharacterAttributes =
        withContext(Dispatchers.IO) {
            val loaded = skillsLoader.loadAttributes(characterId)
            attributesByCharacterId[characterId] = loaded
            skillsCache.saveAttributes(loaded)
            loaded
        }

    fun cachedCatalog(characterId: Long): List<SkillCatalogGroup>? =
        catalogByCharacterId[characterId]

    fun cachedSkillPoints(characterId: Long): CharacterSkillPoints? {
        skillPointsByCharacterId[characterId]?.let { return it }
        return skillsCache.getSkillPoints(characterId)?.also { cached ->
            skillPointsByCharacterId[characterId] = cached
        }
    }

    suspend fun loadCatalog(characterId: Long): SkillCatalogLoadResult =
        withContext(Dispatchers.IO) {
            val loaded = skillsLoader.loadCatalog(characterId)
            catalogByCharacterId[characterId] = loaded.groups
            skillPointsByCharacterId[characterId] = loaded.skillPoints
            skillsCache.saveSkillPoints(loaded.skillPoints)
            loaded
        }

    fun cachedImplantAttributeBonuses(characterId: Long): ImplantAttributeBonuses? =
        implantBonusesByCharacterId[characterId]

    suspend fun loadImplantAttributeBonuses(characterId: Long): ImplantAttributeBonuses =
        withContext(Dispatchers.IO) {
            val loaded = skillsLoader.loadImplantAttributeBonuses(characterId)
            implantBonusesByCharacterId[characterId] = loaded
            loaded
        }

    private fun reportSkillQueue(status: CharacterSkillQueueStatus) {
        val name = tokenManager.sessionIdentities()
            .firstOrNull { it.characterId == status.characterId }
            ?.characterName
            .orEmpty()
        skillQueueExpiryScheduler.report(
            characterId = status.characterId,
            characterName = name,
            queueEndEpochMs = queueEndEpochMs(status),
        )
    }

    private fun queueEndEpochMs(status: CharacterSkillQueueStatus): Long? {
        if (status.state != CharacterSkillQueueState.TRAINING) return null
        return status.trainingFinishAtEpochMs.maxOrNull()
    }
}
