package com.marshall.pyerite.corporationModule.contracts.data

import com.marshall.pyerite.contractsCommon.data.ContractContextResolver
import com.marshall.pyerite.contractsCommon.model.CorporationContract
import com.marshall.pyerite.contractsCommon.model.CorporationContractDetail
import com.marshall.pyerite.contractsCommon.model.CorporationContractStatus
import com.marshall.pyerite.contractsCommon.model.CorporationContractType
import com.marshall.pyerite.contractsCommon.model.CorporationContractsAccessException
import com.marshall.pyerite.contractsCommon.model.CorporationContractsConfig
import com.marshall.pyerite.contractsCommon.model.CorporationContractsSnapshot
import com.marshall.pyerite.contractsCommon.model.corporationContractSignedIsk
import com.marshall.pyerite.esiModule.api.EsiCorporationApi
import com.marshall.pyerite.esiModule.data.EsiPublicDataSource
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

internal class CorporationContractsLoader(
    private val tokenManager: EveTokenManager,
    private val corporationApi: EsiCorporationApi,
    private val publicEsi: EsiPublicDataSource,
    private val resolver: ContractContextResolver,
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
        val itemsResult = runCatching {
            fetchOfferedItems(characterId, corporationId, contract.contractId)
        }
        resolver.buildDetail(characterId, contract, itemsResult)
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
            price = dto.price,
            reward = dto.reward,
            acceptorId = dto.acceptorId,
            volume = dto.volume,
            issuedAtMs = issuedAtMs,
            expiresAtMs = parseEsiDateMillis(dto.dateExpired),
            completedAtMs = parseEsiDateMillis(dto.dateCompleted),
            issuerId = dto.issuerId,
            assigneeId = dto.assigneeId,
            startLocationId = dto.startLocationId,
        )
    }

    private suspend fun fetchOfferedItems(
        characterId: Long,
        corporationId: Long,
        contractId: Long,
    ): List<EsiContractItemDto> = tokenManager.executeWithAuthRetry(characterId) { auth ->
        corporationApi.fetchContractItems(
            corporationId = corporationId,
            contractId = contractId,
            authorization = auth,
        )
    }
}
