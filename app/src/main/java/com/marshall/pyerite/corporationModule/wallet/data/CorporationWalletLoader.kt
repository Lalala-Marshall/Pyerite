package com.marshall.pyerite.corporationModule.wallet.data

import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletAccessException
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletConfig
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletDateFormatter
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletDivision
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletJournalDescription
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletJournalDescriptionKind
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletJournalEntry
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletLedger
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletLocation
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletMarketTransaction
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletsSnapshot
import com.marshall.pyerite.esiModule.api.EsiCorporationApi
import com.marshall.pyerite.esiModule.api.EsiUniverseApi
import com.marshall.pyerite.esiModule.data.EsiPublicDataSource
import com.marshall.pyerite.esiModule.model.EsiCorporationWalletJournalDto
import com.marshall.pyerite.esiModule.model.EsiCorporationWalletTransactionDto
import com.marshall.pyerite.esiModule.model.EsiHttpStatus
import com.marshall.pyerite.esiModule.model.EsiPagedQuery
import com.marshall.pyerite.esiModule.model.parseEsiDateMillis
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.sdeModule.room.RoomProvider
import com.marshall.pyerite.sdeModule.room.type.TypeDisplayIconRow
import com.marshall.pyerite.walletJournal.model.WalletJournalCategory
import com.marshall.pyerite.walletJournal.model.WalletJournalRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response

internal class CorporationWalletLoader(
    private val tokenManager: EveTokenManager,
    private val corporationApi: EsiCorporationApi,
    private val universeApi: EsiUniverseApi,
    private val publicEsi: EsiPublicDataSource,
    private val roomProvider: RoomProvider,
) {
    suspend fun loadWallets(characterId: Long): CorporationWalletsSnapshot =
        withContext(Dispatchers.IO) {
            requireWalletScope(characterId)
            val corporationId = resolveCorporationId(characterId)
            coroutineScope {
                val walletsDeferred = async { fetchWallets(characterId, corporationId) }
                val namesDeferred = async { fetchDivisionNames(characterId, corporationId) }
                val wallets = walletsDeferred.await()
                val names = namesDeferred.await()
                CorporationWalletsSnapshot(
                    characterId = characterId,
                    corporationId = corporationId,
                    divisions = wallets
                        .sortedBy { it.division }
                        .map { wallet ->
                            CorporationWalletDivision(
                                division = wallet.division,
                                name = names[wallet.division]?.takeIf { it.isNotBlank() },
                                balance = wallet.balance,
                            )
                        },
                )
            }
        }

    suspend fun loadLedger(characterId: Long, division: Int): CorporationWalletLedger =
        withContext(Dispatchers.IO) {
            requireWalletScope(characterId)
            val corporationId = resolveCorporationId(characterId)
            coroutineScope {
                val journalDeferred = async {
                    fetchJournal(characterId, corporationId, division)
                }
                val transactionsDeferred = async {
                    fetchTransactions(characterId, corporationId, division)
                }
                val journalDtos = journalDeferred.await()
                val transactionDtos = transactionsDeferred.await()
                val partyNames = loadPartyNames(journalDtos)
                val types = loadTypes(transactionDtos.map { it.typeId }.distinct())
                val locations = loadLocations(
                    characterId = characterId,
                    locationIds = transactionDtos.map { it.locationId }.distinct(),
                )
                CorporationWalletLedger(
                    characterId = characterId,
                    corporationId = corporationId,
                    division = division,
                    journal = journalDtos.mapNotNull { dto -> mapJournal(dto, partyNames) }
                        .sortedByDescending { it.dateEpochMs },
                    transactions = transactionDtos.mapNotNull { dto ->
                        mapTransaction(dto, types, locations)
                    }.sortedByDescending { it.dateEpochMs },
                )
            }
        }

    private fun requireWalletScope(characterId: Long) {
        val granted = tokenManager.grantedScopes(characterId)
        val allowed = EveSsoScope.WALLET_CORPORATION_WALLETS in granted ||
            EveSsoScope.WALLET_CORPORATION in granted
        if (!allowed) throw CorporationWalletAccessException()
    }

    private suspend fun resolveCorporationId(characterId: Long): Long {
        val corporationId = publicEsi.fetchCharacter(characterId).corporationId
        if (corporationId == null || corporationId <= 0L) {
            throw CorporationWalletAccessException()
        }
        return corporationId
    }

    private suspend fun fetchWallets(
        characterId: Long,
        corporationId: Long,
    ) = runCatching {
        tokenManager.executeWithAuthRetry(characterId) { auth ->
            corporationApi.fetchWallets(corporationId, auth)
        }
    }.getOrElse { error -> throw mapAccessError(error) }

    private suspend fun fetchDivisionNames(
        characterId: Long,
        corporationId: Long,
    ): Map<Int, String> {
        val granted = tokenManager.grantedScopes(characterId)
        if (EveSsoScope.CORPORATIONS_READ_DIVISIONS !in granted) return emptyMap()
        return runCatching {
            tokenManager.executeWithAuthRetry(characterId) { auth ->
                corporationApi.fetchDivisions(corporationId, auth)
            }.wallet.associate { it.division to it.name.orEmpty() }
        }.getOrDefault(emptyMap())
    }

    private suspend fun fetchJournal(
        characterId: Long,
        corporationId: Long,
        division: Int,
    ): List<EsiCorporationWalletJournalDto> {
        val byId = LinkedHashMap<Long, EsiCorporationWalletJournalDto>()
        var page = CorporationWalletConfig.FIRST_PAGE
        var totalPages = CorporationWalletConfig.FIRST_PAGE
        while (page <= totalPages && page <= CorporationWalletConfig.JOURNAL_MAX_PAGES) {
            val response = fetchJournalPage(characterId, corporationId, division, page)
            if (!response.isSuccessful) {
                if (page > CorporationWalletConfig.FIRST_PAGE &&
                    response.code() == EsiHttpStatus.NOT_FOUND
                ) {
                    break
                }
                throw mapPagedError(response)
            }
            val chunk = response.body().orEmpty()
            chunk.forEach { entry -> byId[entry.id] = entry }
            totalPages = resolveTotalPages(
                headerPages = response.headers()[EsiPagedQuery.PAGES_HEADER]?.toIntOrNull(),
                chunkSize = chunk.size,
                pageSize = CorporationWalletConfig.JOURNAL_PAGE_SIZE,
                page = page,
            )
            page++
        }
        return byId.values.toList()
    }

    private suspend fun fetchTransactions(
        characterId: Long,
        corporationId: Long,
        division: Int,
    ): List<EsiCorporationWalletTransactionDto> {
        val byId = LinkedHashMap<Long, EsiCorporationWalletTransactionDto>()
        var page = CorporationWalletConfig.FIRST_PAGE
        var totalPages = CorporationWalletConfig.FIRST_PAGE
        while (page <= totalPages && page <= CorporationWalletConfig.TRANSACTIONS_MAX_PAGES) {
            val response = fetchTransactionPage(characterId, corporationId, division, page)
            if (!response.isSuccessful) throw mapPagedError(response)
            val chunk = response.body().orEmpty()
            chunk.forEach { entry -> byId[entry.transactionId] = entry }
            totalPages = resolveTotalPages(
                headerPages = response.headers()[EsiPagedQuery.PAGES_HEADER]?.toIntOrNull(),
                chunkSize = chunk.size,
                pageSize = CorporationWalletConfig.TRANSACTIONS_PAGE_SIZE,
                page = page,
            )
            page++
        }
        return byId.values.toList()
    }

    private suspend fun fetchJournalPage(
        characterId: Long,
        corporationId: Long,
        division: Int,
        page: Int,
    ): Response<List<EsiCorporationWalletJournalDto>> {
        return tokenManager.executeWithAuthRetry(characterId) { auth ->
            val response = corporationApi.fetchWalletJournal(
                corporationId = corporationId,
                division = division,
                authorization = auth,
                page = page,
            )
            if (response.code() == EsiHttpStatus.UNAUTHORIZED) {
                throw HttpException(response)
            }
            response
        }
    }

    private suspend fun fetchTransactionPage(
        characterId: Long,
        corporationId: Long,
        division: Int,
        page: Int,
    ): Response<List<EsiCorporationWalletTransactionDto>> {
        return tokenManager.executeWithAuthRetry(characterId) { auth ->
            val response = corporationApi.fetchWalletTransactions(
                corporationId = corporationId,
                division = division,
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
        pageSize: Int,
        page: Int,
    ): Int = when {
        headerPages != null -> headerPages
        chunkSize < pageSize -> page
        else -> page + 1
    }

    private fun <T> mapPagedError(response: Response<T>): Throwable {
        if (response.code() == EsiHttpStatus.FORBIDDEN) {
            return CorporationWalletAccessException()
        }
        return HttpException(response)
    }

    private fun mapAccessError(error: Throwable): Throwable {
        val http = error as? HttpException
        if (http?.code() == EsiHttpStatus.FORBIDDEN) {
            return CorporationWalletAccessException()
        }
        return error
    }

    private suspend fun loadPartyNames(
        journal: List<EsiCorporationWalletJournalDto>,
    ): Map<Long, String> {
        val ids = buildList {
            journal.forEach { entry ->
                entry.firstPartyId?.let(::add)
                entry.secondPartyId?.let(::add)
            }
        }
        if (ids.isEmpty()) return emptyMap()
        return publicEsi.fetchUniverseNames(ids).associate { it.id to it.name }
    }

    private suspend fun loadTypes(typeIds: List<Int>): Map<Int, TypeDisplayIconRow> {
        if (typeIds.isEmpty()) return emptyMap()
        val dao = roomProvider.getDatabase().sdeTypeDao()
        return typeIds.chunked(CorporationWalletConfig.TYPE_QUERY_CHUNK).flatMap { chunk ->
            runCatching { dao.getTypesForDisplay(chunk) }.getOrDefault(emptyList())
        }.associateBy { it.id }
    }

    private suspend fun loadLocations(
        characterId: Long,
        locationIds: List<Long>,
    ): Map<Long, CorporationWalletLocation?> {
        if (locationIds.isEmpty()) return emptyMap()
        return locationIds.associateWith { id -> resolveLocation(characterId, id) }
    }

    private suspend fun resolveLocation(
        characterId: Long,
        locationId: Long,
    ): CorporationWalletLocation? {
        val station = runCatching {
            roomProvider.getDatabase().mapDao().getStation(locationId)
        }.getOrNull()
        if (station != null) {
            val solarSystemId = station.solarSystemId?.toLong()
            val system = solarSystemId?.let { loadSolarSystem(it) }
            return CorporationWalletLocation(
                securityStatus = system?.securityStatus ?: stationSecurity(solarSystemId),
                systemZhName = system?.systemZhName,
                systemEnName = system?.systemEnName,
                systemName = system?.systemName,
                placeName = station.name?.takeIf { it.isNotBlank() },
            )
        }
        val esiStation = if (locationId < CorporationWalletConfig.PLAYER_STRUCTURE_ID_MIN) {
            publicEsi.fetchStation(locationId)
        } else {
            null
        }
        if (esiStation != null) {
            val system = esiStation.systemId?.let { loadSolarSystem(it) }
            return CorporationWalletLocation(
                securityStatus = system?.securityStatus
                    ?: esiStation.systemId?.let { publicEsi.fetchSolarSystemSecurity(it) },
                systemZhName = system?.systemZhName,
                systemEnName = system?.systemEnName,
                systemName = system?.systemName
                    ?: esiStation.systemId?.let { publicEsi.fetchSolarSystemName(it) },
                placeName = esiStation.name.takeIf { it.isNotBlank() },
            )
        }
        val structure = runCatching {
            tokenManager.executeWithAuthRetry(characterId) { auth ->
                universeApi.fetchStructure(locationId, auth)
            }
        }.getOrNull() ?: return null
        val system = structure.solarSystemId?.let { loadSolarSystem(it) }
        return CorporationWalletLocation(
            securityStatus = system?.securityStatus
                ?: structure.solarSystemId?.let { publicEsi.fetchSolarSystemSecurity(it) },
            systemZhName = system?.systemZhName,
            systemEnName = system?.systemEnName,
            systemName = system?.systemName
                ?: structure.solarSystemId?.let { publicEsi.fetchSolarSystemName(it) },
            placeName = structure.name.takeIf { it.isNotBlank() },
        )
    }

    private suspend fun loadSolarSystem(solarSystemId: Long) = runCatching {
        roomProvider.getDatabase().mapDao().getSolarSystemLocation(solarSystemId)
    }.getOrNull()

    private suspend fun stationSecurity(solarSystemId: Long?): Double? {
        solarSystemId ?: return null
        return loadSolarSystem(solarSystemId)?.securityStatus
            ?: publicEsi.fetchSolarSystemSecurity(solarSystemId)
    }

    private fun mapJournal(
        dto: EsiCorporationWalletJournalDto,
        partyNames: Map<Long, String>,
    ): CorporationWalletJournalEntry? {
        val epochMs = parseEsiDateMillis(dto.date) ?: return null
        val category = WalletJournalRef.categoryFor(dto.refType)
        val firstName = dto.firstPartyId?.let { partyNames[it] }
        val secondName = dto.secondPartyId?.let { partyNames[it] }
        val fallback = dto.description?.takeIf { it.isNotBlank() }
            ?: dto.reason.orEmpty()
        val kind = if (category == WalletJournalCategory.INDUSTRY) {
            CorporationWalletJournalDescriptionKind.INDUSTRY_PROJECT
        } else {
            CorporationWalletJournalDescriptionKind.FALLBACK
        }
        return CorporationWalletJournalEntry(
            id = dto.id,
            dateEpochMs = epochMs,
            dayKey = CorporationWalletDateFormatter.dayKey(epochMs),
            amount = dto.amount,
            balance = dto.balance,
            refType = dto.refType,
            category = category,
            titleRes = WalletJournalRef.titleResFor(dto.refType),
            description = CorporationWalletJournalDescription(
                kind = kind,
                firstPartyName = firstName,
                secondPartyName = secondName,
                contextId = dto.contextId,
                fallbackText = fallback,
            ),
        )
    }

    private fun mapTransaction(
        dto: EsiCorporationWalletTransactionDto,
        types: Map<Int, TypeDisplayIconRow>,
        locations: Map<Long, CorporationWalletLocation?>,
    ): CorporationWalletMarketTransaction? {
        val epochMs = parseEsiDateMillis(dto.date) ?: return null
        val type = types[dto.typeId]
        return CorporationWalletMarketTransaction(
            transactionId = dto.transactionId,
            dateEpochMs = epochMs,
            dayKey = CorporationWalletDateFormatter.dayKey(epochMs),
            typeId = dto.typeId,
            locationId = dto.locationId,
            zhName = type?.zhName,
            enName = type?.enName,
            name = type?.name ?: dto.typeId.toString(),
            iconFilename = type?.iconFilename,
            quantity = dto.quantity,
            unitPrice = dto.unitPrice,
            isBuy = dto.isBuy,
            location = locations[dto.locationId],
        )
    }
}
