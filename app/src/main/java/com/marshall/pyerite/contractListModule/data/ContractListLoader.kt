package com.marshall.pyerite.contractListModule.data

import com.marshall.pyerite.contractsCommon.data.ContractContextResolver
import com.marshall.pyerite.contractsCommon.model.ContractListScope
import com.marshall.pyerite.contractsCommon.model.CorporationContract
import com.marshall.pyerite.contractsCommon.model.CorporationContractDetail
import com.marshall.pyerite.contractsCommon.model.CorporationContractStatus
import com.marshall.pyerite.contractsCommon.model.CorporationContractType
import com.marshall.pyerite.contractsCommon.model.CorporationContractsAccessException
import com.marshall.pyerite.contractsCommon.model.CorporationContractsConfig
import com.marshall.pyerite.contractsCommon.model.CorporationContractsSnapshot
import com.marshall.pyerite.contractsCommon.model.corporationContractListScopes
import com.marshall.pyerite.contractsCommon.model.corporationContractSignedIsk
import com.marshall.pyerite.esiModule.api.EsiCharacterApi
import com.marshall.pyerite.esiModule.api.EsiCorporationApi
import com.marshall.pyerite.esiModule.data.EsiPublicDataSource
import com.marshall.pyerite.esiModule.model.EsiCharacterContractDto
import com.marshall.pyerite.esiModule.model.EsiContractItemDto
import com.marshall.pyerite.esiModule.model.EsiCorporationContractDto
import com.marshall.pyerite.esiModule.model.EsiHttpStatus
import com.marshall.pyerite.esiModule.model.EsiPagedQuery
import com.marshall.pyerite.esiModule.model.parseEsiDateMillis
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response

internal class ContractListLoader(
    private val tokenManager: EveTokenManager,
    private val characterApi: EsiCharacterApi,
    private val corporationApi: EsiCorporationApi,
    private val publicEsi: EsiPublicDataSource,
    private val resolver: ContractContextResolver,
) {
    suspend fun loadContracts(characterId: Long): CorporationContractsSnapshot =
        withContext(Dispatchers.IO) {
            requireCharacterContractsScope(characterId)
            val affiliation = runCatching { publicEsi.fetchCharacter(characterId) }.getOrNull()
            val corporationId = affiliation?.corporationId?.takeIf { it > 0L }
            val allianceId = affiliation?.allianceId?.takeIf { it > 0L }
            val byId = LinkedHashMap<Long, CorporationContract>()
            fetchCharacterContracts(characterId).forEach { dto ->
                val contract = toContract(
                    dto = dto,
                    scopes = setOf(ContractListScope.CHARACTER),
                ) ?: return@forEach
                byId[contract.contractId] = contract
            }
            if (corporationId != null && hasCorporationContractsScope(characterId)) {
                fetchCorporationContracts(characterId, corporationId).forEach { dto ->
                    val scopes = corporationContractListScopes(
                        corporationId = corporationId,
                        allianceId = allianceId,
                        assigneeId = dto.assigneeId,
                        status = dto.status,
                    )
                    if (scopes.isEmpty()) return@forEach
                    val existing = byId[dto.contractId]
                    if (existing != null) {
                        byId[dto.contractId] = existing.copy(scopes = existing.scopes + scopes)
                    } else {
                        val contract = toContract(
                            dto = dto,
                            scopes = scopes,
                        ) ?: return@forEach
                        byId[dto.contractId] = contract
                    }
                }
            }
            CorporationContractsSnapshot(contracts = byId.values.toList())
        }

    suspend fun loadContractDetail(
        characterId: Long,
        contract: CorporationContract,
    ): CorporationContractDetail = withContext(Dispatchers.IO) {
        requireCharacterContractsScope(characterId)
        val itemsResult = runCatching {
            if (contract.itemsViaCorporation) {
                fetchCorporationOfferedItems(characterId, contract.contractId)
            } else {
                fetchCharacterOfferedItems(characterId, contract.contractId)
            }
        }
        resolver.buildDetail(characterId, contract, itemsResult)
    }

    private fun requireCharacterContractsScope(characterId: Long) {
        val granted = tokenManager.grantedScopes(characterId)
        if (EveSsoScope.CONTRACTS_CHARACTER !in granted) {
            throw CorporationContractsAccessException()
        }
    }

    private fun hasCorporationContractsScope(characterId: Long): Boolean =
        EveSsoScope.CONTRACTS_CORPORATION in tokenManager.grantedScopes(characterId)

    private suspend fun fetchCharacterContracts(characterId: Long): List<EsiCharacterContractDto> {
        val byId = LinkedHashMap<Long, EsiCharacterContractDto>()
        var page = CorporationContractsConfig.FIRST_PAGE
        while (page <= CorporationContractsConfig.CONTRACTS_MAX_PAGES) {
            val chunk = fetchCharacterContractsPage(characterId, page)
            chunk.forEach { entry -> byId[entry.contractId] = entry }
            if (chunk.size < CorporationContractsConfig.CONTRACTS_PAGE_SIZE) break
            page++
        }
        return byId.values.toList()
    }

    private suspend fun fetchCharacterContractsPage(
        characterId: Long,
        page: Int,
    ): List<EsiCharacterContractDto> {
        return try {
            tokenManager.executeWithAuthRetry(characterId) { auth ->
                characterApi.fetchContracts(characterId, auth, page)
            }
        } catch (error: HttpException) {
            if (page > CorporationContractsConfig.FIRST_PAGE &&
                error.code() == EsiHttpStatus.NOT_FOUND
            ) {
                emptyList()
            } else if (error.code() == EsiHttpStatus.FORBIDDEN) {
                throw CorporationContractsAccessException()
            } else {
                throw error
            }
        }
    }

    private suspend fun fetchCorporationContracts(
        characterId: Long,
        corporationId: Long,
    ): List<EsiCorporationContractDto> {
        val byId = LinkedHashMap<Long, EsiCorporationContractDto>()
        var page = CorporationContractsConfig.FIRST_PAGE
        var totalPages = CorporationContractsConfig.FIRST_PAGE
        while (page <= totalPages && page <= CorporationContractsConfig.CONTRACTS_MAX_PAGES) {
            val response = fetchCorporationContractsPage(characterId, corporationId, page)
            if (!response.isSuccessful) {
                if (page > CorporationContractsConfig.FIRST_PAGE &&
                    response.code() == EsiHttpStatus.NOT_FOUND
                ) {
                    break
                }
                if (response.code() == EsiHttpStatus.FORBIDDEN) return emptyList()
                throw HttpException(response)
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

    private suspend fun fetchCorporationContractsPage(
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

    private fun toContract(
        dto: EsiCharacterContractDto,
        scopes: Set<ContractListScope>,
    ): CorporationContract? = toContract(
        contractId = dto.contractId,
        type = dto.type,
        status = dto.status,
        title = dto.title,
        price = dto.price,
        reward = dto.reward,
        buyout = dto.buyout,
        volume = dto.volume,
        dateIssued = dto.dateIssued,
        dateExpired = dto.dateExpired,
        dateCompleted = dto.dateCompleted,
        issuerId = dto.issuerId,
        assigneeId = dto.assigneeId,
        acceptorId = dto.acceptorId,
        startLocationId = dto.startLocationId,
        scopes = scopes,
        itemsViaCorporation = false,
    )

    private fun toContract(
        dto: EsiCorporationContractDto,
        scopes: Set<ContractListScope>,
    ): CorporationContract? = toContract(
        contractId = dto.contractId,
        type = dto.type,
        status = dto.status,
        title = dto.title,
        price = dto.price,
        reward = dto.reward,
        buyout = dto.buyout,
        volume = dto.volume,
        dateIssued = dto.dateIssued,
        dateExpired = dto.dateExpired,
        dateCompleted = dto.dateCompleted,
        issuerId = dto.issuerId,
        assigneeId = dto.assigneeId,
        acceptorId = dto.acceptorId,
        startLocationId = dto.startLocationId,
        scopes = scopes,
        itemsViaCorporation = true,
    )

    private fun toContract(
        contractId: Long,
        type: String,
        status: String,
        title: String?,
        price: Double,
        reward: Double,
        buyout: Double,
        volume: Double,
        dateIssued: String?,
        dateExpired: String?,
        dateCompleted: String?,
        issuerId: Long,
        assigneeId: Long,
        acceptorId: Long,
        startLocationId: Long,
        scopes: Set<ContractListScope>,
        itemsViaCorporation: Boolean,
    ): CorporationContract? {
        if (scopes.isEmpty()) return null
        val contractType = CorporationContractType.fromWire(type) ?: return null
        val contractStatus = CorporationContractStatus.fromWire(status) ?: return null
        val issuedAtMs = parseEsiDateMillis(dateIssued) ?: return null
        return CorporationContract(
            contractId = contractId,
            type = contractType,
            status = contractStatus,
            title = title?.trim().orEmpty(),
            signedIsk = corporationContractSignedIsk(
                type = contractType,
                price = price,
                reward = reward,
                buyout = buyout,
            ),
            price = price,
            reward = reward,
            acceptorId = acceptorId,
            volume = volume,
            issuedAtMs = issuedAtMs,
            expiresAtMs = parseEsiDateMillis(dateExpired),
            completedAtMs = parseEsiDateMillis(dateCompleted),
            issuerId = issuerId,
            assigneeId = assigneeId,
            startLocationId = startLocationId,
            scopes = scopes,
            itemsViaCorporation = itemsViaCorporation,
        )
    }

    private suspend fun fetchCharacterOfferedItems(
        characterId: Long,
        contractId: Long,
    ): List<EsiContractItemDto> = tokenManager.executeWithAuthRetry(characterId) { auth ->
        characterApi.fetchContractItems(
            characterId = characterId,
            contractId = contractId,
            authorization = auth,
        )
    }

    private suspend fun fetchCorporationOfferedItems(
        characterId: Long,
        contractId: Long,
    ): List<EsiContractItemDto> {
        val corporationId = publicEsi.fetchCharacter(characterId).corporationId
        if (corporationId == null || corporationId <= 0L) {
            throw CorporationContractsAccessException()
        }
        return tokenManager.executeWithAuthRetry(characterId) { auth ->
            corporationApi.fetchContractItems(
                corporationId = corporationId,
                contractId = contractId,
                authorization = auth,
            )
        }
    }
}
