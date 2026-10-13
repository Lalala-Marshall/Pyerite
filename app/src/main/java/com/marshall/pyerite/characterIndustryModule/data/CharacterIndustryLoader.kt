package com.marshall.pyerite.characterIndustryModule.data

import com.marshall.pyerite.characterIndustryModule.model.CharacterIndustryConfig
import com.marshall.pyerite.characterIndustryModule.model.CharacterIndustrySlotCapacity
import com.marshall.pyerite.esiModule.api.EsiCharacterApi
import com.marshall.pyerite.esiModule.api.EsiCorporationApi
import com.marshall.pyerite.esiModule.data.EsiPublicDataSource
import com.marshall.pyerite.esiModule.model.EsiCharacterSkillsDto
import com.marshall.pyerite.esiModule.model.EsiHttpStatus
import com.marshall.pyerite.esiModule.model.EsiIndustryJobDto
import com.marshall.pyerite.esiModule.model.EsiPagedQuery
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.industryCommon.data.IndustryJobEnricher
import com.marshall.pyerite.industryCommon.data.IndustryJobSource
import com.marshall.pyerite.industryCommon.model.IndustryAccessException
import com.marshall.pyerite.industryCommon.model.IndustryConfig
import com.marshall.pyerite.industryCommon.model.IndustryJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response

internal data class CharacterIndustryTarget(
    val characterId: Long,
    val name: String,
    val portraitUrl: String,
)

internal data class CharacterIndustryCharacterResult(
    val characterId: Long,
    val jobs: List<IndustryJob>,
    val slots: CharacterIndustrySlotCapacity?,
    val accessDenied: Boolean,
    val failed: Boolean,
)

internal class CharacterIndustryLoader(
    private val tokenManager: EveTokenManager,
    private val characterApi: EsiCharacterApi,
    private val corporationApi: EsiCorporationApi,
    private val publicEsi: EsiPublicDataSource,
    private val enricher: IndustryJobEnricher,
) {
    suspend fun loadCharacter(target: CharacterIndustryTarget): CharacterIndustryCharacterResult =
        withContext(Dispatchers.IO) {
            val granted = tokenManager.grantedScopes(target.characterId)
            if (EveSsoScope.INDUSTRY_CHARACTER_JOBS !in granted) {
                return@withContext denied(target.characterId)
            }
            coroutineScope {
                val jobsDeferred = async { fetchJobsResult(target) }
                val slotsDeferred = async { loadSlots(target.characterId, granted) }
                val jobs = jobsDeferred.await()
                val slots = slotsDeferred.await()
                jobs.fold(
                    onSuccess = { loaded ->
                        CharacterIndustryCharacterResult(
                            characterId = target.characterId,
                            jobs = loaded,
                            slots = slots,
                            accessDenied = false,
                            failed = false,
                        )
                    },
                    onFailure = { error ->
                        CharacterIndustryCharacterResult(
                            characterId = target.characterId,
                            jobs = emptyList(),
                            slots = slots,
                            accessDenied = error is IndustryAccessException,
                            failed = error !is IndustryAccessException,
                        )
                    },
                )
            }
        }

    private suspend fun fetchJobs(target: CharacterIndustryTarget): List<IndustryJob> {
        val personal = fetchJobPages(target.characterId)
        val forCorporation = fetchCorporationJobsInstalledBy(target.characterId)
        val dtos = (personal + forCorporation).distinctBy { it.jobId }
        return enricher.enrich(
            authCharacterId = target.characterId,
            sources = dtos.map { dto ->
                IndustryJobSource(
                    dto = dto,
                    installerId = target.characterId,
                    installerName = target.name,
                    installerPortraitUrl = target.portraitUrl,
                )
            },
        )
    }

    private suspend fun fetchJobPages(characterId: Long): List<EsiIndustryJobDto> {
        val byId = LinkedHashMap<Long, EsiIndustryJobDto>()
        var page = IndustryConfig.FIRST_PAGE
        var totalPages = IndustryConfig.FIRST_PAGE
        while (page <= totalPages && page <= IndustryConfig.JOBS_MAX_PAGES) {
            val response = fetchJobsPage(characterId, page)
            if (!response.isSuccessful) {
                if (page > IndustryConfig.FIRST_PAGE &&
                    response.code() == EsiHttpStatus.NOT_FOUND
                ) {
                    break
                }
                throw mapPagedError(response)
            }
            val chunk = response.body().orEmpty()
            chunk.forEach { entry -> byId[entry.jobId] = entry }
            val headerPages = response.headers()[EsiPagedQuery.PAGES_HEADER]?.toIntOrNull()
            totalPages = when {
                headerPages != null -> headerPages
                chunk.size < IndustryConfig.JOBS_PAGE_SIZE -> page
                else -> page + 1
            }
            page++
        }
        return byId.values.toList()
    }

    /**
     * Jobs this character started for their corporation live on the corporation
     * endpoint and still occupy that character's personal lines.
     * Missing corp-industry scope or a failed corp lookup leaves personal jobs intact.
     */
    private suspend fun fetchCorporationJobsInstalledBy(characterId: Long): List<EsiIndustryJobDto> {
        if (EveSsoScope.INDUSTRY_CORPORATION_JOBS !in tokenManager.grantedScopes(characterId)) {
            return emptyList()
        }
        val corporationId = try {
            publicEsi.fetchCharacter(characterId).corporationId
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        if (corporationId == null || corporationId <= 0L) return emptyList()
        return try {
            fetchCorporationJobPages(characterId, corporationId)
                .filter { it.installerId == characterId }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchCorporationJobPages(
        characterId: Long,
        corporationId: Long,
    ): List<EsiIndustryJobDto> {
        val byId = LinkedHashMap<Long, EsiIndustryJobDto>()
        var page = IndustryConfig.FIRST_PAGE
        var totalPages = IndustryConfig.FIRST_PAGE
        while (page <= totalPages && page <= IndustryConfig.JOBS_MAX_PAGES) {
            val response = fetchCorporationJobsPage(characterId, corporationId, page)
            if (!response.isSuccessful) {
                if (page > IndustryConfig.FIRST_PAGE &&
                    response.code() == EsiHttpStatus.NOT_FOUND
                ) {
                    break
                }
                throw mapPagedError(response)
            }
            val chunk = response.body().orEmpty()
            chunk.forEach { entry -> byId[entry.jobId] = entry }
            val headerPages = response.headers()[EsiPagedQuery.PAGES_HEADER]?.toIntOrNull()
            totalPages = when {
                headerPages != null -> headerPages
                chunk.size < IndustryConfig.JOBS_PAGE_SIZE -> page
                else -> page + 1
            }
            page++
        }
        return byId.values.toList()
    }

    private suspend fun fetchCorporationJobsPage(
        characterId: Long,
        corporationId: Long,
        page: Int,
    ): Response<List<EsiIndustryJobDto>> {
        return tokenManager.executeWithAuthRetry(characterId) { auth ->
            val response = corporationApi.fetchIndustryJobs(
                corporationId = corporationId,
                authorization = auth,
                page = page,
                includeCompleted = IndustryConfig.INCLUDE_COMPLETED,
            )
            if (response.code() == EsiHttpStatus.UNAUTHORIZED) {
                throw HttpException(response)
            }
            response
        }
    }

    private suspend fun fetchJobsPage(
        characterId: Long,
        page: Int,
    ): Response<List<EsiIndustryJobDto>> {
        return tokenManager.executeWithAuthRetry(characterId) { auth ->
            val response = characterApi.fetchIndustryJobs(
                characterId = characterId,
                authorization = auth,
                page = page,
                includeCompleted = IndustryConfig.INCLUDE_COMPLETED,
            )
            if (response.code() == EsiHttpStatus.UNAUTHORIZED) {
                throw HttpException(response)
            }
            response
        }
    }

    private fun <T> mapPagedError(response: Response<T>): Throwable {
        if (response.code() == EsiHttpStatus.FORBIDDEN) {
            return IndustryAccessException()
        }
        return HttpException(response)
    }

    private suspend fun fetchJobsResult(target: CharacterIndustryTarget): Result<List<IndustryJob>> =
        try {
            Result.success(fetchJobs(target))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }

    private suspend fun loadSlots(
        characterId: Long,
        granted: Set<EveSsoScope>,
    ): CharacterIndustrySlotCapacity? {
        if (EveSsoScope.SKILLS_READ !in granted) return null
        val skills = try {
            tokenManager.executeWithAuthRetry(characterId) { auth ->
                characterApi.fetchSkills(characterId, auth)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return null
        }
        return slotsFrom(skills)
    }

    private fun slotsFrom(dto: EsiCharacterSkillsDto): CharacterIndustrySlotCapacity {
        val levels = dto.skills.associate { skill -> skill.skillId to skill.activeSkillLevel }
        fun lines(primaryTypeId: Int, advancedTypeId: Int): Int =
            CharacterIndustryConfig.BASE_SLOTS +
                (levels[primaryTypeId] ?: 0) +
                (levels[advancedTypeId] ?: 0)
        return CharacterIndustrySlotCapacity(
            manufacturing = lines(
                CharacterIndustryConfig.MASS_PRODUCTION_TYPE_ID,
                CharacterIndustryConfig.ADVANCED_MASS_PRODUCTION_TYPE_ID,
            ),
            research = lines(
                CharacterIndustryConfig.LABORATORY_OPERATION_TYPE_ID,
                CharacterIndustryConfig.ADVANCED_LABORATORY_OPERATION_TYPE_ID,
            ),
            reaction = lines(
                CharacterIndustryConfig.MASS_REACTIONS_TYPE_ID,
                CharacterIndustryConfig.ADVANCED_MASS_REACTIONS_TYPE_ID,
            ),
        )
    }

    private fun denied(characterId: Long) = CharacterIndustryCharacterResult(
        characterId = characterId,
        jobs = emptyList(),
        slots = null,
        accessDenied = true,
        failed = false,
    )
}
