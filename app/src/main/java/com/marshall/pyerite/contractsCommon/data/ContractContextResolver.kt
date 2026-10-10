package com.marshall.pyerite.contractsCommon.data

import com.marshall.pyerite.contractsCommon.model.CorporationContract
import com.marshall.pyerite.contractsCommon.model.CorporationContractDetail
import com.marshall.pyerite.contractsCommon.model.CorporationContractOfferedItem
import com.marshall.pyerite.contractsCommon.model.CorporationContractParty
import com.marshall.pyerite.contractsCommon.model.CorporationContractPlace
import com.marshall.pyerite.contractsCommon.model.CorporationContractsConfig
import com.marshall.pyerite.esiModule.api.EsiUniverseApi
import com.marshall.pyerite.esiModule.data.EsiPublicDataSource
import com.marshall.pyerite.esiModule.data.allianceLogoUrl
import com.marshall.pyerite.esiModule.data.corporationLogoUrl
import com.marshall.pyerite.esiModule.data.portraitUrl
import com.marshall.pyerite.esiModule.model.EsiContractItemDto
import com.marshall.pyerite.esiModule.model.EsiUniverseNameCategory
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.sdeModule.room.RoomProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal class ContractContextResolver(
    private val tokenManager: EveTokenManager,
    private val universeApi: EsiUniverseApi,
    private val publicEsi: EsiPublicDataSource,
    private val roomProvider: RoomProvider,
) {
    suspend fun buildDetail(
        characterId: Long,
        contract: CorporationContract,
        itemsResult: Result<List<EsiContractItemDto>>,
    ): CorporationContractDetail = coroutineScope {
        val placeDeferred = async { resolvePlace(characterId, contract.startLocationId) }
        val issuerDeferred = async { resolveIssuer(contract.issuerId) }
        val assigneeDeferred = async { resolveAssignee(contract.assigneeId) }
        val offered = if (itemsResult.isFailure) {
            MappedOfferedItems(items = emptyList(), loadFailed = true)
        } else {
            try {
                MappedOfferedItems(
                    items = mapOfferedItems(itemsResult.getOrThrow()),
                    loadFailed = false,
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                MappedOfferedItems(items = emptyList(), loadFailed = true)
            }
        }
        CorporationContractDetail(
            contract = contract,
            place = placeDeferred.await(),
            issuer = issuerDeferred.await(),
            assignee = assigneeDeferred.await(),
            assigneeIsPublic = contract.assigneeId <= 0L,
            items = offered.items,
            itemsLoadFailed = offered.loadFailed,
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

    private suspend fun mapOfferedItems(
        items: List<EsiContractItemDto>,
    ): List<CorporationContractOfferedItem> {
        val included = items.filter { it.isIncluded && it.typeId > 0 }
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

private data class MappedOfferedItems(
    val items: List<CorporationContractOfferedItem>,
    val loadFailed: Boolean,
)

private data class ContractBuildingLocation(
    val name: String,
    val typeId: Int?,
    val solarSystemId: Long?,
)
