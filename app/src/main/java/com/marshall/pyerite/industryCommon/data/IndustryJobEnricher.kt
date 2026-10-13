package com.marshall.pyerite.industryCommon.data

import com.marshall.pyerite.esiModule.api.EsiUniverseApi
import com.marshall.pyerite.esiModule.data.EsiPublicDataSource
import com.marshall.pyerite.esiModule.model.EsiIndustryJobDto
import com.marshall.pyerite.esiModule.model.parseEsiDateMillis
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.industryCommon.model.IndustryActivity
import com.marshall.pyerite.industryCommon.model.IndustryConfig
import com.marshall.pyerite.industryCommon.model.IndustryJob
import com.marshall.pyerite.industryCommon.model.IndustryJobStatus
import com.marshall.pyerite.sdeModule.room.RoomProvider
import com.marshall.pyerite.sdeModule.room.map.SolarSystemLookupRow
import com.marshall.pyerite.sdeModule.room.type.TypeDisplayIconRow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** One ESI industry job plus the character shown as its installer. */
internal data class IndustryJobSource(
    val dto: EsiIndustryJobDto,
    val installerId: Long,
    val installerName: String,
    val installerPortraitUrl: String,
)

/**
 * Resolves blueprint names, facilities, and solar systems for industry jobs.
 * Callers supply the installer identity; corporation jobs look it up, personal jobs use the owner.
 */
internal class IndustryJobEnricher(
    private val tokenManager: EveTokenManager,
    private val universeApi: EsiUniverseApi,
    private val publicEsi: EsiPublicDataSource,
    private val roomProvider: RoomProvider,
) {
    suspend fun enrich(
        authCharacterId: Long,
        sources: List<IndustryJobSource>,
    ): List<IndustryJob> {
        if (sources.isEmpty()) return emptyList()
        val typeIds = sources.flatMap { source ->
            listOfNotNull(
                source.dto.blueprintTypeId.takeIf { it > 0 },
                source.dto.productTypeId?.takeIf { it > 0 },
            )
        }
        val types = loadTypes(typeIds)
        val facilities = resolveFacilities(authCharacterId, sources.map { it.dto.facilityId })
        val systems = loadSystems(facilities.values.mapNotNull { it.solarSystemId })
        return sources.mapNotNull { source ->
            toJob(
                source = source,
                types = types,
                facility = facilities[source.dto.facilityId],
                systems = systems,
            )
        }
    }

    private suspend fun loadTypes(typeIds: List<Int>): Map<Int, TypeDisplayIconRow> {
        val distinct = typeIds.filter { it > 0 }.distinct()
        if (distinct.isEmpty()) return emptyMap()
        val dao = roomProvider.getDatabase().sdeTypeDao()
        return distinct.chunked(IndustryConfig.QUERY_CHUNK).flatMap { chunk ->
            runCatching { dao.getTypesForDisplay(chunk) }.getOrDefault(emptyList())
        }.associateBy { it.id }
    }

    private suspend fun resolveFacilities(
        characterId: Long,
        facilityIds: List<Long>,
    ): Map<Long, FacilityPlace> {
        val ids = facilityIds.filter { it > 0L }.distinct()
        if (ids.isEmpty()) return emptyMap()
        val stationIds = ids.filter { it < IndustryConfig.PLAYER_STRUCTURE_ID_MIN }
        val structureIds = ids.filter { it >= IndustryConfig.PLAYER_STRUCTURE_ID_MIN }
        return resolveStations(stationIds) + resolveStructures(characterId, structureIds)
    }

    private suspend fun resolveStations(stationIds: List<Long>): Map<Long, FacilityPlace> {
        if (stationIds.isEmpty()) return emptyMap()
        val dao = roomProvider.getDatabase().mapDao()
        val fromSde = stationIds.chunked(IndustryConfig.QUERY_CHUNK).flatMap { chunk ->
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
        val fromEsi = incomplete.chunked(IndustryConfig.STRUCTURE_LOOKUP_PARALLELISM)
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
        return structureIds.chunked(IndustryConfig.STRUCTURE_LOOKUP_PARALLELISM)
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
        val fromSde = ids.chunked(IndustryConfig.QUERY_CHUNK).flatMap { chunk ->
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
        source: IndustryJobSource,
        types: Map<Int, TypeDisplayIconRow>,
        facility: FacilityPlace?,
        systems: Map<Long, SolarSystemLookupRow>,
    ): IndustryJob? {
        val dto = source.dto
        val activity = IndustryActivity.fromId(dto.activityId) ?: return null
        val status = IndustryJobStatus.fromWire(dto.status) ?: return null
        val blueprint = types[dto.blueprintTypeId]
        val product = dto.productTypeId?.let { types[it] }
        val display = blueprint ?: product
        val solarSystemId = facility?.solarSystemId
        val system = solarSystemId?.let { systems[it] }
        return IndustryJob(
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
            installerId = source.installerId,
            installerName = source.installerName,
            installerPortraitUrl = source.installerPortraitUrl,
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
