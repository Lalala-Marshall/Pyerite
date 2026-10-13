package com.marshall.pyerite.corporationModule.industry.data

import com.marshall.pyerite.esiModule.api.EsiCorporationApi
import com.marshall.pyerite.esiModule.data.EsiPublicDataSource
import com.marshall.pyerite.esiModule.data.portraitUrl
import com.marshall.pyerite.esiModule.model.EsiHttpStatus
import com.marshall.pyerite.esiModule.model.EsiIndustryJobDto
import com.marshall.pyerite.esiModule.model.EsiPagedQuery
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.industryCommon.data.IndustryJobEnricher
import com.marshall.pyerite.industryCommon.data.IndustryJobSource
import com.marshall.pyerite.industryCommon.model.IndustryAccessException
import com.marshall.pyerite.industryCommon.model.IndustryConfig
import com.marshall.pyerite.industryCommon.model.IndustrySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response

internal class CorporationIndustryLoader(
    private val tokenManager: EveTokenManager,
    private val corporationApi: EsiCorporationApi,
    private val publicEsi: EsiPublicDataSource,
    private val enricher: IndustryJobEnricher,
) {
    suspend fun loadJobs(characterId: Long): IndustrySnapshot =
        withContext(Dispatchers.IO) {
            requireIndustryScope(characterId)
            val corporationId = resolveCorporationId(characterId)
            val dtos = fetchJobs(characterId, corporationId)
            val names = publicEsi.fetchUniverseNames(dtos.map { it.installerId })
                .associate { it.id to it.name }
            val jobs = enricher.enrich(
                authCharacterId = characterId,
                sources = dtos.map { dto ->
                    IndustryJobSource(
                        dto = dto,
                        installerId = dto.installerId,
                        installerName = names[dto.installerId].orEmpty(),
                        installerPortraitUrl = portraitUrl(dto.installerId),
                    )
                },
            )
            IndustrySnapshot(jobs = jobs)
        }

    private fun requireIndustryScope(characterId: Long) {
        val granted = tokenManager.grantedScopes(characterId)
        if (EveSsoScope.INDUSTRY_CORPORATION_JOBS !in granted) {
            throw IndustryAccessException()
        }
    }

    private suspend fun resolveCorporationId(characterId: Long): Long {
        val corporationId = publicEsi.fetchCharacter(characterId).corporationId
        if (corporationId == null || corporationId <= 0L) {
            throw IndustryAccessException()
        }
        return corporationId
    }

    private suspend fun fetchJobs(
        characterId: Long,
        corporationId: Long,
    ): List<EsiIndustryJobDto> {
        val byId = LinkedHashMap<Long, EsiIndustryJobDto>()
        var page = IndustryConfig.FIRST_PAGE
        var totalPages = IndustryConfig.FIRST_PAGE
        while (page <= totalPages && page <= IndustryConfig.JOBS_MAX_PAGES) {
            val response = fetchJobsPage(characterId, corporationId, page)
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
            totalPages = resolveTotalPages(
                headerPages = response.headers()[EsiPagedQuery.PAGES_HEADER]?.toIntOrNull(),
                chunkSize = chunk.size,
                page = page,
            )
            page++
        }
        return byId.values.toList()
    }

    private suspend fun fetchJobsPage(
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

    private fun resolveTotalPages(
        headerPages: Int?,
        chunkSize: Int,
        page: Int,
    ): Int = when {
        headerPages != null -> headerPages
        chunkSize < IndustryConfig.JOBS_PAGE_SIZE -> page
        else -> page + 1
    }

    private fun <T> mapPagedError(response: Response<T>): Throwable {
        if (response.code() == EsiHttpStatus.FORBIDDEN) {
            return IndustryAccessException()
        }
        return HttpException(response)
    }
}
