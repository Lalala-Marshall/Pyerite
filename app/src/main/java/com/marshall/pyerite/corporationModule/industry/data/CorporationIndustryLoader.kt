package com.marshall.pyerite.corporationModule.industry.data

import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryAccessException
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryActivity
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryConfig
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryJob
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryJobStatus
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustrySnapshot
import com.marshall.pyerite.esiModule.api.EsiCorporationApi
import com.marshall.pyerite.esiModule.api.EsiUniverseApi
import com.marshall.pyerite.esiModule.data.EsiPublicDataSource
import com.marshall.pyerite.esiModule.data.portraitUrl
import com.marshall.pyerite.esiModule.model.EsiCorporationIndustryJobDto
import com.marshall.pyerite.esiModule.model.EsiHttpStatus
import com.marshall.pyerite.esiModule.model.EsiPagedQuery
import com.marshall.pyerite.esiModule.model.parseEsiDateMillis
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.sdeModule.room.RoomProvider
import com.marshall.pyerite.sdeModule.room.map.SolarSystemLookupRow
import com.marshall.pyerite.sdeModule.room.type.TypeDisplayIconRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response

internal class CorporationIndustryLoader(
    private val tokenManager: EveTokenManager,
    private val corporationApi: EsiCorporationApi,
    private val universeApi: EsiUniverseApi,
    private val publicEsi: EsiPublicDataSource,
    private val roomProvider: RoomProvider,
) {
    suspend fun loadJobs(characterId: Long): CorporationIndustrySnapshot =
        withContext(Dispatchers.IO) {
            requireIndustryScope(characterId)
            val corporationId = resolveCorporationId(characterId)
            val dtos = fetchJobs(characterId, corporationId)
            val typeIds = dtos.flatMap { dto ->
                listOfNotNull(dto.blueprintTypeId.takeIf { it > 0 }, dto.productTypeId?.takeIf { it > 0 })
            }
            val types = loadTypes(typeIds)
            val facilities = resolveFacilities(characterId, dtos.map { it.facilityId })
            val systems = loadSystems(facilities.values.mapNotNull { it.solarSystemId })
            val names = publicEsi.fetchUniverseNames(dtos.map { it.installerId })
                .associate { it.id to it.name }
            CorporationIndustrySnapshot(
                jobs = dtos.mapNotNull { dto ->
                    toJob(
                        dto = dto,
                        types = types,
                        facility = facilities[dto.facilityId],
                        systems = systems,
                        installerNames = names,
                    )
                },
            )
        }

    private fun requireIndustryScope(characterId: Long) {
        val granted = tokenManager.grantedScopes(characterId)
        if (EveSsoScope.INDUSTRY_CORPORATION_JOBS !in granted) {
            throw CorporationIndustryAccessException()
        }
    }

    private suspend fun resolveCorporationId(characterId: Long): Long {
        val corporationId = publicEsi.fetchCharacter(characterId).corporationId
        if (corporationId == null || corporationId <= 0L) {
            throw CorporationIndustryAccessException()
        }
        return corporationId
    }

    private suspend fun fetchJobs(
        characterId: Long,
        corporationId: Long,
    ): List<EsiCorporationIndustryJobDto> {
        val byId = LinkedHashMap<Long, EsiCorporationIndustryJobDto>()
        var page = CorporationIndustryConfig.FIRST_PAGE
        var totalPages = CorporationIndustryConfig.FIRST_PAGE
        while (page <= totalPages && page <= CorporationIndustryConfig.JOBS_MAX_PAGES) {
            val response = fetchJobsPage(characterId, corporationId, page)
            if (!response.isSuccessful) {
                if (page > CorporationIndustryConfig.FIRST_PAGE &&
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
    ): Response<List<EsiCorporationIndustryJobDto>> {
        return tokenManager.executeWithAuthRetry(characterId) { auth ->
            val response = corporationApi.fetchIndustryJobs(
                corporationId = corporationId,
                authorization = auth,
                page = page,
                includeCompleted = CorporationIndustryConfig.INCLUDE_COMPLETED,
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
        chunkSize < CorporationIndustryConfig.JOBS_PAGE_SIZE -> page
        else -> page + 1
    }

    private fun <T> mapPagedError(response: Response<T>): Throwable {
        if (response.code() == EsiHttpStatus.FORBIDDEN) {
            return CorporationIndustryAccessException()
        }
        return HttpException(response)
    }

    private suspend fun loadTypes(typeIds: List<Int>): Map<Int, TypeDisplayIconRow> {
        val distinct = typeIds.filter { it > 0 }.distinct()
        if (distinct.isEmpty()) return emptyMap()
        val dao = roomProvider.getDatabase().sdeTypeDao()
        return distinct.chunked(CorporationIndustryConfig.QUERY_CHUNK).flatMap { chunk ->
            runCatching { dao.getTypesForDisplay(chunk) }.getOrDefault(emptyList())
        }.associateBy { it.id }
    }

    private suspend fun resolveFacilities(
        characterId: Long,
        facilityIds: List<Long>,
    ): Map<Long, FacilityPlace> {
        val ids = facilityIds.filter { it > 0L }.distinct()
        if (ids.isEmpty()) return emptyMap()
        val stationIds = ids.filter { it < CorporationIndustryConfig.PLAYER_STRUCTURE_ID_MIN }
        val structureIds = ids.filter { it >= CorporationIndustryConfig.PLAYER_STRUCTURE_ID_MIN }
        return resolveStations(stationIds) + resolveStructures(characterId, structureIds)
    }

    private suspend fun resolveStations(stationIds: List<Long>): Map<Long, FacilityPlace> {
        if (stationIds.isEmpty()) return emptyMap()
        val dao = roomProvider.getDatabase().mapDao()
        val fromSde = stationIds.chunked(CorporationIndustryConfig.QUERY_CHUNK).flatMap { chunk ->
            runCatching { dao.getStations(chunk) }.getOrDefault(emptyList())
        }.associate { station ->
            station.stationId to FacilityPlace(
                solarSystemId = station.solarSystemId?.toLong()?.takeIf { it > 0L },
                name = station.name.orEmpty(),
            )
        }
        val incomplete = stationIds.filter { id ->
            val place = fromSde[id]
            place == null || !place.isResolved()
        }
        if (incomplete.isEmpty()) return fromSde
        val fromEsi = incomplete.chunked(CorporationIndustryConfig.STRUCTURE_LOOKUP_PARALLELISM)
            .flatMap { chunk ->
                coroutineScope {
                    chunk.map { stationId ->
                        async {
                            val station = publicEsi.fetchStation(stationId)
                            val existing = fromSde[stationId]
                            stationId to FacilityPlace(
                                solarSystemId = existing?.solarSystemId
                                    ?: station?.systemId?.takeIf { it > 0L },
                                name = existing?.name?.takeIf { it.isNotBlank() }
                                    ?: station?.name.orEmpty(),
                            )
                        }
                    }.awaitAll()
                }
            }.toMap()
        return fromSde + fromEsi
    }

    private suspend fun resolveStructures(
        characterId: Long,
        structureIds: List<Long>,
    ): Map<Long, FacilityPlace> {
        if (structureIds.isEmpty()) return emptyMap()
        return structureIds.chunked(CorporationIndustryConfig.STRUCTURE_LOOKUP_PARALLELISM)
            .flatMap { chunk ->
                coroutineScope {
                    chunk.map { structureId ->
                        async {
                            structureId to fetchStructurePlace(characterId, structureId)
                        }
                    }.awaitAll()
                }
            }.toMap()
    }

    private suspend fun fetchStructurePlace(
        characterId: Long,
        structureId: Long,
    ): FacilityPlace {
        val structure = runCatching {
            tokenManager.executeWithAuthRetry(characterId) { auth ->
                universeApi.fetchStructure(structureId, auth)
            }
        }.getOrNull() ?: return FacilityPlace(solarSystemId = null, name = "")
        return FacilityPlace(
            solarSystemId = structure.solarSystemId?.takeIf { it > 0L },
            name = structure.name,
        )
    }

    private suspend fun loadSystems(systemIds: List<Long>): Map<Long, SolarSystemLookupRow> {
        val ids = systemIds.filter { it > 0L }.distinct()
        if (ids.isEmpty()) return emptyMap()
        val dao = roomProvider.getDatabase().mapDao()
        val fromSde = ids.chunked(CorporationIndustryConfig.QUERY_CHUNK).flatMap { chunk ->
            runCatching { dao.getSolarSystemLocations(chunk) }.getOrDefault(emptyList())
        }.associateBy { it.solarSystemId }
        val missing = ids.filter { it !in fromSde }
        if (missing.isEmpty()) return fromSde
        val fromEsi = missing.mapNotNull { systemId ->
            val name = publicEsi.fetchSolarSystemName(systemId)?.takeIf { it.isNotBlank() }
            val security = publicEsi.fetchSolarSystemSecurity(systemId)
            if (name == null && security == null) return@mapNotNull null
            systemId to SolarSystemLookupRow(
                solarSystemId = systemId,
                systemName = name,
                systemZhName = null,
                systemEnName = null,
                securityStatus = security,
            )
        }.toMap()
        return fromSde + fromEsi
    }

    private fun toJob(
        dto: EsiCorporationIndustryJobDto,
        types: Map<Int, TypeDisplayIconRow>,
        facility: FacilityPlace?,
        systems: Map<Long, SolarSystemLookupRow>,
        installerNames: Map<Long, String>,
    ): CorporationIndustryJob? {
        val activity = CorporationIndustryActivity.fromId(dto.activityId) ?: return null
        val status = CorporationIndustryJobStatus.fromWire(dto.status) ?: return null
        val blueprint = types[dto.blueprintTypeId]
        val product = dto.productTypeId?.let { types[it] }
        val display = blueprint ?: product
        val solarSystemId = facility?.solarSystemId
        val system = solarSystemId?.let { systems[it] }
        return CorporationIndustryJob(
            jobId = dto.jobId,
            activity = activity,
            status = status,
            runs = dto.runs,
            detailTypeId = display?.id
                ?: dto.blueprintTypeId.takeIf { it > 0 }
                ?: dto.productTypeId
                ?: 0,
            blueprintZhName = display?.zhName,
            blueprintEnName = display?.enName,
            blueprintName = display?.name,
            blueprintIconFilename = display?.iconFilename,
            installerId = dto.installerId,
            installerName = installerNames[dto.installerId].orEmpty(),
            installerPortraitUrl = portraitUrl(dto.installerId),
            facilityName = facility?.name.orEmpty(),
            solarSystemId = solarSystemId,
            systemZhName = system?.systemZhName,
            systemEnName = system?.systemEnName,
            systemName = system?.systemName,
            securityStatus = system?.securityStatus,
            startDateMillis = parseEsiDateMillis(dto.startDate),
            endDateMillis = parseEsiDateMillis(dto.endDate),
            pauseDateMillis = parseEsiDateMillis(dto.pauseDate),
            completedDateMillis = parseEsiDateMillis(dto.completedDate),
        )
    }
}

private data class FacilityPlace(
    val solarSystemId: Long?,
    val name: String,
) {
    fun isResolved(): Boolean = solarSystemId != null && name.isNotBlank()
}
