package com.marshall.pyerite.corporationModule.contracts.data

import com.marshall.pyerite.corporationModule.contracts.model.CorporationContract
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractDetail
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractOfferedItem
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractParty
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractPlace
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractStatus
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractType
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractsAccessException
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractsConfig
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractsSnapshot
import com.marshall.pyerite.corporationModule.contracts.model.corporationContractSignedIsk
import com.marshall.pyerite.esiModule.api.EsiCorporationApi
import com.marshall.pyerite.esiModule.api.EsiUniverseApi
import com.marshall.pyerite.esiModule.data.EsiPublicDataSource
import com.marshall.pyerite.esiModule.data.allianceLogoUrl
import com.marshall.pyerite.esiModule.data.corporationLogoUrl
import com.marshall.pyerite.esiModule.data.portraitUrl
import com.marshall.pyerite.esiModule.model.EsiCorporationContractDto
import com.marshall.pyerite.esiModule.model.EsiHttpStatus
import com.marshall.pyerite.esiModule.model.EsiPagedQuery
import com.marshall.pyerite.esiModule.model.EsiUniverseNameCategory
import com.marshall.pyerite.esiModule.model.parseEsiDateMillis
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.sdeModule.room.RoomProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response

internal class CorporationContractsLoader(
    private val tokenManager: EveTokenManager,
    private val corporationApi: EsiCorporationApi,
    private val universeApi: EsiUniverseApi,
    private val publicEsi: EsiPublicDataSource,
    private val roomProvider: RoomProvider,
) {
    suspend fun loadContracts(characterId: Long): CorporationContractsSnapshot =
        withContext(Dispatchers.IO) {
            requireContractsScope(characterId)
            val corporationId = resolveCorporationId(characterId)
            val contracts = fetchContracts(characterId, corporationId).mapNotNull { dto ->
                toContract(dto, corporationId)
            }
            CorporationContractsSnapshot(contracts = contracts)
        }

    suspend fun loadContractDetail(
        characterId: Long,
        contract: CorporationContract,
    ): CorporationContractDetail = withContext(Dispatchers.IO) {
        val corporationId = resolveCorporationId(characterId)
        coroutineScope {
            val itemsDeferred = async {
                runCatching {
                    fetchOfferedItems(characterId, corporationId, contract.contractId)
                }
            }
            val placeDeferred = async { resolvePlace(characterId, contract.startLocationId) }
            val issuerDeferred = async { resolveIssuer(contract.issuerId) }
            val assigneeDeferred = async { resolveAssignee(contract.assigneeId) }
            val itemsResult = itemsDeferred.await()
            CorporationContractDetail(
                contract = contract,
                place = placeDeferred.await(),
                issuer = issuerDeferred.await(),
                assignee = assigneeDeferred.await(),
                assigneeIsPublic = contract.assigneeId <= 0L,
                items = itemsResult.getOrDefault(emptyList()),
                itemsLoadFailed = itemsResult.isFailure,
            )
        }
    }

    private fun requireContractsScope(characterId: Long) {
        val granted = tokenManager.grantedScopes(characterId)
        if (EveSsoScope.CONTRACTS_CORPORATION !in granted) {
            throw CorporationContractsAccessException()
        }
    }

    private suspend fun resolveCorporationId(characterId: Long): Long {
        val corporationId = publicEsi.fetchCharacter(characterId).corporationId
        if (corporationId == null || corporationId <= 0L) {
            throw CorporationContractsAccessException()
        }
        return corporationId
    }

    private suspend fun fetchContracts(
        characterId: Long,
        corporationId: Long,
    ): List<EsiCorporationContractDto> {
        val byId = LinkedHashMap<Long, EsiCorporationContractDto>()
        var page = CorporationContractsConfig.FIRST_PAGE
        var totalPages = CorporationContractsConfig.FIRST_PAGE
        while (page <= totalPages && page <= CorporationContractsConfig.CONTRACTS_MAX_PAGES) {
            val response = fetchContractsPage(characterId, corporationId, page)
            if (!response.isSuccessful) {
                if (page > CorporationContractsConfig.FIRST_PAGE &&
                    response.code() == EsiHttpStatus.NOT_FOUND
                ) {
                    break
                }
                throw mapPagedError(response)
            }
            val chunk = response.body().orEmpty()
            chunk.forEach { entry -> byId[entry.contractId] = entry }
            totalPages = resolveTotalPages(
                headerPages = response.headers()[EsiPagedQuery.PAGES_HEADER]?.toIntOrNull(),
                chunkSize = chunk.size,
                page = page,
            )
            page++
        }
        return byId.values.toList()
    }

    private suspend fun fetchContractsPage(
        characterId: Long,
        corporationId: Long,
        page: Int,
    ): Response<List<EsiCorporationContractDto>> {
        return tokenManager.executeWithAuthRetry(characterId) { auth ->
            val response = corporationApi.fetchContracts(
                corporationId = corporationId,
                authorization = auth,
                page = page,
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
        chunkSize < CorporationContractsConfig.CONTRACTS_PAGE_SIZE -> page
        else -> page + 1
    }

    private fun <T> mapPagedError(response: Response<T>): Throwable {
        if (response.code() == EsiHttpStatus.FORBIDDEN) {
            return CorporationContractsAccessException()
        }
        return HttpException(response)
    }

    private fun toContract(
        dto: EsiCorporationContractDto,
        corporationId: Long,
    ): CorporationContract? {
        if (!dto.forCorporation || dto.issuerCorporationId != corporationId) return null
        val type = CorporationContractType.fromWire(dto.type) ?: return null
        val status = CorporationContractStatus.fromWire(dto.status) ?: return null
        val issuedAtMs = parseEsiDateMillis(dto.dateIssued) ?: return null
        return CorporationContract(
            contractId = dto.contractId,
            type = type,
            status = status,
            title = dto.title?.trim().orEmpty(),
            signedIsk = corporationContractSignedIsk(
                type = type,
                price = dto.price,
                reward = dto.reward,
                buyout = dto.buyout,
            ),
            volume = dto.volume,
            issuedAtMs = issuedAtMs,
            expiresAtMs = parseEsiDateMillis(dto.dateExpired),
            completedAtMs = parseEsiDateMillis(dto.dateCompleted),
            issuerId = dto.issuerId,
            assigneeId = dto.assigneeId,
            startLocationId = dto.startLocationId,
        )
    }

    private suspend fun resolvePlace(
        characterId: Long,
        locationId: Long,
    ): CorporationContractPlace? {
        if (locationId <= 0L) return null
        val building = if (locationId >= CorporationContractsConfig.PLAYER_STRUCTURE_ID_MIN) {
            fetchStructureLocation(characterId, locationId)
        } else {
            fetchStationLocation(locationId)
        } ?: return null
        val security = building.solarSystemId?.let { loadSecurity(it) }
        val iconFileName = building.typeId?.let { loadTypeIcon(it) }
        if (building.name.isBlank() && security == null && iconFileName.isNullOrBlank()) return null
        return CorporationContractPlace(
            name = building.name,
            securityStatus = security,
            iconFileName = iconFileName,
        )
    }

    private suspend fun fetchStationLocation(locationId: Long): ContractBuildingLocation? {
        val dao = roomProvider.getDatabase().mapDao()
        val station = runCatching { dao.getStation(locationId) }.getOrNull()
        val needsEsi = station == null ||
            station.name.isNullOrBlank() ||
            station.typeId == null ||
            station.solarSystemId == null
        val esi = if (needsEsi) publicEsi.fetchStation(locationId) else null
        val name = station?.name?.takeIf { it.isNotBlank() } ?: esi?.name.orEmpty()
        val typeId = station?.typeId ?: esi?.typeId
        val solarSystemId = station?.solarSystemId?.toLong()?.takeIf { it > 0L }
            ?: esi?.systemId?.takeIf { it > 0L }
        if (name.isBlank() && typeId == null && solarSystemId == null) return null
        return ContractBuildingLocation(
            name = name,
            typeId = typeId,
            solarSystemId = solarSystemId,
        )
    }

    private suspend fun fetchStructureLocation(
        characterId: Long,
        locationId: Long,
    ): ContractBuildingLocation? {
        val structure = runCatching {
            tokenManager.executeWithAuthRetry(characterId) { auth ->
                universeApi.fetchStructure(locationId, auth)
            }
        }.getOrNull() ?: return null
        return ContractBuildingLocation(
            name = structure.name,
            typeId = structure.typeId,
            solarSystemId = structure.solarSystemId?.takeIf { it > 0L },
        )
    }

    private suspend fun loadSecurity(solarSystemId: Long): Double? {
        val dao = roomProvider.getDatabase().mapDao()
        val fromSde = runCatching {
            dao.getSolarSystemLocations(listOf(solarSystemId))
        }.getOrNull()?.firstOrNull()?.securityStatus
        return fromSde ?: publicEsi.fetchSolarSystemSecurity(solarSystemId)
    }

    private suspend fun loadTypeIcon(typeId: Int): String? {
        if (typeId <= 0) return null
        return runCatching {
            roomProvider.getDatabase().sdeTypeDao().getTypeIconFilename(typeId)
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private suspend fun resolveIssuer(issuerId: Long): CorporationContractParty? {
        if (issuerId <= 0L) return null
        val character = runCatching { publicEsi.fetchCharacter(issuerId) }.getOrNull()
        val name = character?.name?.takeIf { it.isNotBlank() }
            ?: publicEsi.fetchUniverseName(issuerId).orEmpty()
        val affiliation = character?.corporationId?.takeIf { it > 0L }?.let { corporationId ->
            runCatching { publicEsi.fetchCorporation(corporationId).name }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        }
        return CorporationContractParty(
            name = name,
            affiliation = affiliation,
            iconUrl = portraitUrl(issuerId),
        )
    }

    private suspend fun resolveAssignee(assigneeId: Long): CorporationContractParty? {
        if (assigneeId <= 0L) return null
        val named = publicEsi.fetchUniverseNames(listOf(assigneeId))
            .firstOrNull { it.id == assigneeId }
        val iconUrl = when (named?.category) {
            EsiUniverseNameCategory.CHARACTER -> portraitUrl(assigneeId)
            EsiUniverseNameCategory.CORPORATION -> corporationLogoUrl(assigneeId)
            EsiUniverseNameCategory.ALLIANCE -> allianceLogoUrl(assigneeId)
            else -> null
        }
        return CorporationContractParty(
            name = named?.name?.takeIf { it.isNotBlank() }.orEmpty(),
            affiliation = null,
            iconUrl = iconUrl,
        )
    }

    private suspend fun fetchOfferedItems(
        characterId: Long,
        corporationId: Long,
        contractId: Long,
    ): List<CorporationContractOfferedItem> {
        val included = tokenManager.executeWithAuthRetry(characterId) { auth ->
            corporationApi.fetchContractItems(
                corporationId = corporationId,
                contractId = contractId,
                authorization = auth,
            )
        }.filter { it.isIncluded && it.typeId > 0 }
            .groupBy { it.typeId }
            .map { (typeId, rows) -> typeId to rows.sumOf { it.quantity.toLong() } }
            .filter { (_, quantity) -> quantity > 0L }
        if (included.isEmpty()) return emptyList()
        val types = loadTypes(included.map { it.first })
        return included.map { (typeId, quantity) ->
            val type = types[typeId]
            CorporationContractOfferedItem(
                typeId = typeId,
                quantity = quantity,
                zhName = type?.zhName,
                enName = type?.enName,
                name = type?.name,
                iconFileName = type?.iconFilename,
            )
        }
    }

    private suspend fun loadTypes(typeIds: List<Int>) =
        typeIds.filter { it > 0 }.distinct()
            .chunked(CorporationContractsConfig.TYPE_QUERY_CHUNK)
            .flatMap { chunk ->
                runCatching {
                    roomProvider.getDatabase().sdeTypeDao().getTypesForDisplay(chunk)
                }.getOrDefault(emptyList())
            }
            .associateBy { it.id }
}

private data class ContractBuildingLocation(
    val name: String,
    val typeId: Int?,
    val solarSystemId: Long?,
)

