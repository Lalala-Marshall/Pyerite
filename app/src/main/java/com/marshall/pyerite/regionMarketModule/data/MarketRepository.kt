package com.marshall.pyerite.regionMarketModule.data

import com.marshall.pyerite.esiModule.api.EsiCharacterApi
import com.marshall.pyerite.esiModule.api.EsiMarketApi
import com.marshall.pyerite.esiModule.api.EsiUniverseApi
import com.marshall.pyerite.esiModule.model.EsiHttpStatus
import com.marshall.pyerite.esiModule.model.EsiMarketHistoryDto
import com.marshall.pyerite.esiModule.model.EsiMarketOrderDto
import com.marshall.pyerite.esiModule.model.EsiMarketQuery
import com.marshall.pyerite.esiModule.model.EsiPagedQuery
import com.marshall.pyerite.esiModule.model.EsiUniverseNameCategory
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.localization.ContentLanguage
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.localizedName
import com.marshall.pyerite.regionMarketModule.model.MarketConfig
import com.marshall.pyerite.regionMarketModule.model.MarketHistoryPoint
import com.marshall.pyerite.regionMarketModule.model.MarketOrder
import com.marshall.pyerite.regionMarketModule.model.MarketOrderLocation
import com.marshall.pyerite.regionMarketModule.model.MarketPlaceName
import com.marshall.pyerite.regionMarketModule.model.MarketQuote
import com.marshall.pyerite.regionMarketModule.model.MarketSelection
import com.marshall.pyerite.regionMarketModule.model.MarketStructureHit
import com.marshall.pyerite.regionMarketModule.model.MarketSystemOption
import com.marshall.pyerite.regionMarketModule.model.ResolvedMarket
import com.marshall.pyerite.regionMarketModule.model.SavedMarketStructure
import com.marshall.pyerite.sdeModule.room.RoomProvider
import com.marshall.pyerite.sdeModule.room.catalog.MetaGroupEntity
import com.marshall.pyerite.sdeModule.room.market.MarketGroupEntity
import com.marshall.pyerite.sdeModule.room.type.TypeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response

internal class MarketStructureAccessException : Exception()

internal class MarketRepository(
    private val roomProvider: RoomProvider,
    private val localeController: LocaleController,
    private val marketApi: EsiMarketApi,
    private val characterApi: EsiCharacterApi,
    private val universeApi: EsiUniverseApi,
    private val tokenManager: EveTokenManager,
    private val structureStore: MarketStructureStore,
    private val quoteCache: MarketQuoteCache,
) {
    suspend fun visibleGroups(): List<MarketGroupEntity> = withContext(Dispatchers.IO) {
        roomProvider.getDatabase().marketGroupDao().getVisibleGroups()
    }

    suspend fun group(groupId: Int): MarketGroupEntity? = withContext(Dispatchers.IO) {
        roomProvider.getDatabase().marketGroupDao().getGroup(groupId)
    }

    suspend fun typesInGroup(marketGroupId: Int): List<TypeEntity> = withContext(Dispatchers.IO) {
        roomProvider.getDatabase().typeDao().getTypesByMarketGroup(marketGroupId)
    }

    suspend fun type(typeId: Int): TypeEntity? = withContext(Dispatchers.IO) {
        roomProvider.getDatabase().typeDao().getTypeById(typeId)
    }

    suspend fun metaGroups(): List<MetaGroupEntity> = withContext(Dispatchers.IO) {
        roomProvider.getDatabase().metaGroupDao().getAllMetaGroups()
    }

    suspend fun searchTypes(marketGroupIds: List<Int>, query: String): List<TypeEntity> =
        withContext(Dispatchers.IO) {
            val pattern = "%${query.trim()}%"
            if (marketGroupIds.isEmpty() || query.isBlank()) return@withContext emptyList()
            val dao = roomProvider.getDatabase().typeDao()
            val found = mutableListOf<TypeEntity>()
            for (chunk in marketGroupIds.distinct().chunked(MarketConfig.SQL_IN_CHUNK)) {
                if (found.size >= MarketConfig.SEARCH_RESULT_LIMIT) break
                val remaining = MarketConfig.SEARCH_RESULT_LIMIT - found.size
                found += dao.searchTypesInMarketGroups(chunk, pattern, remaining)
            }
            found.take(MarketConfig.SEARCH_RESULT_LIMIT)
        }

    suspend fun tradeRegions(): List<MarketPlaceName> = withContext(Dispatchers.IO) {
        roomProvider.getDatabase().mapDao()
            .getRegionsBelow(MarketConfig.TRADE_REGION_ID_EXCLUSIVE_MAX)
            .map { row ->
                MarketPlaceName(
                    regionId = row.regionId,
                    name = row.name.orEmpty(),
                    zhName = row.zhName,
                    enName = row.enName,
                )
            }
    }

    suspend fun majorSystems(): List<MarketSystemOption> = withContext(Dispatchers.IO) {
        val rows = roomProvider.getDatabase().mapDao()
            .getMarketSystemPlaces(MarketConfig.MAJOR_SYSTEM_IDS)
            .associateBy { it.solarSystemId }
        MarketConfig.MAJOR_SYSTEM_IDS.mapNotNull { systemId ->
            val row = rows[systemId] ?: return@mapNotNull null
            val regionId = row.regionId ?: return@mapNotNull null
            MarketSystemOption(
                systemId = systemId,
                regionId = regionId,
                systemName = row.systemName.orEmpty(),
                systemZhName = row.systemZhName,
                systemEnName = row.systemEnName,
                regionName = row.regionName.orEmpty(),
                regionZhName = row.regionZhName,
                regionEnName = row.regionEnName,
                security = row.securityStatus,
            )
        }
    }

    suspend fun systemOption(systemId: Int): MarketSystemOption? = withContext(Dispatchers.IO) {
        val row = roomProvider.getDatabase().mapDao().getMarketSystemPlace(systemId) ?: return@withContext null
        val regionId = row.regionId ?: return@withContext null
        MarketSystemOption(
            systemId = systemId,
            regionId = regionId,
            systemName = row.systemName.orEmpty(),
            systemZhName = row.systemZhName,
            systemEnName = row.systemEnName,
            regionName = row.regionName.orEmpty(),
            regionZhName = row.regionZhName,
            regionEnName = row.regionEnName,
            security = row.securityStatus,
        )
    }

    fun displayPlaceName(name: String, zhName: String?, enName: String?): String =
        localizedName(zhName, enName, name, localeController.contentLanguage).ifBlank { name }

    suspend fun resolve(selection: MarketSelection, typeId: Int): ResolvedMarket =
        withContext(Dispatchers.IO) {
            val language = localeController.contentLanguage
            val plex = typeId == MarketConfig.PLEX_TYPE_ID
            when (selection) {
                is MarketSelection.Region -> {
                    val region = regionName(selection.regionId, language)
                    ResolvedMarket(
                        selection = selection,
                        displayName = region,
                        orderRegionId = if (plex) MarketConfig.PLEX_REGION_ID else selection.regionId,
                        historyRegionId = if (plex) MarketConfig.PLEX_REGION_ID else selection.regionId,
                        systemFilterId = null,
                        structureId = null,
                        structureCharacterId = null,
                        hideLocations = plex,
                    )
                }
                is MarketSelection.System -> {
                    val place = roomProvider.getDatabase().mapDao().getMarketSystemPlace(selection.systemId)
                    val regionId = place?.regionId ?: MarketConfig.DEFAULT_REGION_ID
                    ResolvedMarket(
                        selection = selection,
                        displayName = localizedName(
                            place?.systemZhName,
                            place?.systemEnName,
                            place?.systemName,
                            language,
                        ).ifBlank { selection.systemId.toString() },
                        orderRegionId = if (plex) MarketConfig.PLEX_REGION_ID else regionId,
                        historyRegionId = if (plex) MarketConfig.PLEX_REGION_ID else regionId,
                        systemFilterId = if (plex) null else selection.systemId,
                        structureId = null,
                        structureCharacterId = null,
                        hideLocations = plex,
                    )
                }
                is MarketSelection.Structure -> {
                    val saved = structureStore.find(selection.structureId)
                    val regionId = saved?.regionId ?: MarketConfig.DEFAULT_REGION_ID
                    ResolvedMarket(
                        selection = selection,
                        displayName = saved?.name ?: selection.structureId.toString(),
                        orderRegionId = if (plex) MarketConfig.PLEX_REGION_ID else regionId,
                        historyRegionId = if (plex) MarketConfig.PLEX_REGION_ID else regionId,
                        systemFilterId = null,
                        structureId = if (plex) null else selection.structureId,
                        structureCharacterId = saved?.characterId,
                        hideLocations = plex,
                    )
                }
            }
        }

    suspend fun loadQuote(
        typeId: Int,
        selection: MarketSelection,
        includeHistory: Boolean,
        forceRefresh: Boolean,
        onPage: (page: Int, pageCount: Int) -> Unit = { _, _ -> },
    ): MarketQuote = withContext(Dispatchers.IO) {
        val cached = if (forceRefresh) null else quoteCache.get(typeId, selection)
        if (cached != null && (!includeHistory || cached.historyLoaded)) {
            return@withContext cached
        }
        val resolved = resolve(selection, typeId)
        val structureId = resolved.structureId
        val structureCharacterId = resolved.structureCharacterId
        var structureDenied = false
        val orders = when {
            cached != null -> cached.orders
            structureId != null -> {
                val characterId = structureCharacterId
                if (characterId == null) {
                    structureDenied = true
                    emptyList()
                } else {
                    try {
                        fetchStructureOrders(
                            structureId = structureId,
                            characterId = characterId,
                            typeId = typeId,
                            onPage = onPage,
                        )
                    } catch (_: MarketStructureAccessException) {
                        structureDenied = true
                        emptyList()
                    }
                }
            }
            else -> fetchRegionOrders(resolved.orderRegionId, typeId, onPage)
                .let { list ->
                    val systemId = resolved.systemFilterId
                    if (systemId == null) list else list.filter { it.systemId == systemId }
                }
        }
        val history = if (includeHistory) {
            fetchHistory(resolved.historyRegionId, typeId)
        } else {
            cached?.history.orEmpty()
        }
        val quote = MarketQuote(
            orders = orders,
            history = history,
            lowestSell = orders.filter { !it.isBuyOrder }.minOfOrNull { it.price },
            highestBuy = orders.filter { it.isBuyOrder }.maxOfOrNull { it.price },
            structureAccessDenied = structureDenied,
            historyLoaded = includeHistory,
        )
        quoteCache.put(typeId, selection, quote)
        quote
    }

    suspend fun orderLocations(
        orders: List<MarketOrder>,
        characterId: Long?,
    ): Map<Long, MarketOrderLocation> = withContext(Dispatchers.IO) {
        if (orders.isEmpty()) return@withContext emptyMap()
        val locationIds = orders.map { it.locationId }.distinct()
        val stationIds = locationIds.filter { it < MarketConfig.PLAYER_STRUCTURE_ID_MIN }
        val structureIds = locationIds.filter { it >= MarketConfig.PLAYER_STRUCTURE_ID_MIN }
        val stations = if (stationIds.isEmpty()) {
            emptyMap()
        } else {
            roomProvider.getDatabase().mapDao().getMarketStations(stationIds)
                .associate { row ->
                    row.stationId to MarketOrderLocation(
                        locationId = row.stationId,
                        security = row.security,
                        placeName = row.name.orEmpty(),
                    )
                }
        }
        val saved = structureIds.mapNotNull { id ->
            structureStore.find(id)?.let { saved ->
                id to MarketOrderLocation(
                    locationId = id,
                    security = saved.security,
                    placeName = saved.name,
                )
            }
        }.toMap()
        val unresolved = structureIds.filter { it !in saved }
        val resolverId = characterId ?: tokenManager.sessionIdentities().firstOrNull { identity ->
            EveSsoScope.UNIVERSE_READ_STRUCTURES in tokenManager.grantedScopes(identity.characterId)
        }?.characterId
        val fetched = if (unresolved.isEmpty() || resolverId == null) {
            emptyMap()
        } else if (EveSsoScope.UNIVERSE_READ_STRUCTURES !in tokenManager.grantedScopes(resolverId) &&
            EveSsoScope.MARKETS_STRUCTURE !in tokenManager.grantedScopes(resolverId)
        ) {
            emptyMap()
        } else {
            resolveStructures(unresolved, resolverId)
        }
        stations + saved + fetched
    }

    fun hasStructureSearchScope(characterId: Long): Boolean =
        EveSsoScope.SEARCH_STRUCTURES in tokenManager.grantedScopes(characterId)

    fun hasStructureMarketScope(characterId: Long): Boolean =
        EveSsoScope.MARKETS_STRUCTURE in tokenManager.grantedScopes(characterId)

    suspend fun searchStructures(characterId: Long, query: String): List<MarketStructureHit> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return@withContext emptyList()
            val result = tokenManager.executeWithAuthRetry(characterId) { auth ->
                characterApi.search(
                    characterId = characterId,
                    authorization = auth,
                    categories = EsiUniverseNameCategory.STRUCTURE,
                    search = trimmed,
                    strict = false,
                )
            }
            val hits = mutableListOf<MarketStructureHit>()
            for (structureId in result.structure) {
                if (hits.size >= MarketConfig.STRUCTURE_SEARCH_LIMIT) break
                val info = runCatching {
                    tokenManager.executeWithAuthRetry(characterId) { auth ->
                        universeApi.fetchStructure(structureId, auth)
                    }
                }.getOrNull() ?: continue
                if (info.typeId in MarketConfig.NON_MARKET_STRUCTURE_TYPE_IDS) continue
                if (!structureExposesMarket(characterId, structureId)) continue
                val systemId = info.solarSystemId?.toInt() ?: continue
                val place = roomProvider.getDatabase().mapDao().getMarketSystemPlace(systemId)
                val regionId = place?.regionId ?: continue
                val icon = info.typeId?.let { typeId ->
                    roomProvider.getDatabase().typeDao().getTypeIconFilename(typeId)
                }
                hits += MarketStructureHit(
                    structureId = structureId,
                    name = info.name,
                    systemId = systemId,
                    regionId = regionId,
                    systemName = displayPlaceName(
                        place.systemName.orEmpty(),
                        place.systemZhName,
                        place.systemEnName,
                    ),
                    security = place.securityStatus,
                    typeId = info.typeId,
                    iconFileName = icon,
                )
            }
            hits
        }

    fun saveStructure(hit: MarketStructureHit, characterId: Long) {
        structureStore.add(
            SavedMarketStructure(
                structureId = hit.structureId,
                name = hit.name,
                systemId = hit.systemId,
                regionId = hit.regionId,
                security = hit.security,
                typeId = hit.typeId,
                iconFileName = hit.iconFileName,
                characterId = characterId,
            ),
        )
    }

    private suspend fun regionName(regionId: Int, language: ContentLanguage): String {
        val row = roomProvider.getDatabase().mapDao().getMarketRegion(regionId)
        return localizedName(row?.zhName, row?.enName, row?.name, language).ifBlank { regionId.toString() }
    }

    private suspend fun fetchRegionOrders(
        regionId: Int,
        typeId: Int,
        onPage: (page: Int, pageCount: Int) -> Unit,
    ): List<MarketOrder> {
        val dtos = mutableListOf<EsiMarketOrderDto>()
        var page = MarketConfig.FIRST_PAGE
        var totalPages = MarketConfig.FIRST_PAGE
        while (page <= totalPages && page <= MarketConfig.MAX_ORDER_PAGES) {
            val response = marketApi.fetchRegionOrders(
                regionId = regionId,
                typeId = typeId,
                orderType = EsiMarketQuery.ORDER_TYPE_ALL,
                page = page,
            )
            if (!response.isSuccessful) {
                if (page > MarketConfig.FIRST_PAGE && response.code() == EsiHttpStatus.NOT_FOUND) break
                throw HttpException(response)
            }
            val chunk = response.body().orEmpty()
            dtos += chunk
            totalPages = totalPages(response, chunk.size, page)
            onPage(page, totalPages)
            page++
        }
        return dtos.map { it.toOrder() }
    }

    /** True when this character can read the structure's market, including an empty order book. */
    private suspend fun structureExposesMarket(characterId: Long, structureId: Long): Boolean {
        if (!hasStructureMarketScope(characterId)) return false
        val response = runCatching {
            tokenManager.executeWithAuthRetry(characterId) { auth ->
                val page = marketApi.fetchStructureOrders(
                    structureId = structureId,
                    authorization = auth,
                    page = MarketConfig.FIRST_PAGE,
                )
                if (page.code() == EsiHttpStatus.UNAUTHORIZED) throw HttpException(page)
                page
            }
        }.getOrNull() ?: return false
        return response.isSuccessful
    }

    private suspend fun fetchStructureOrders(
        structureId: Long,
        characterId: Long,
        typeId: Int,
        onPage: (page: Int, pageCount: Int) -> Unit,
    ): List<MarketOrder> {
        if (!hasStructureMarketScope(characterId)) throw MarketStructureAccessException()
        val matched = mutableListOf<MarketOrder>()
        var page = MarketConfig.FIRST_PAGE
        var totalPages = MarketConfig.FIRST_PAGE
        while (page <= totalPages && page <= MarketConfig.MAX_ORDER_PAGES) {
            val response = tokenManager.executeWithAuthRetry(characterId) { auth ->
                val pageResponse = marketApi.fetchStructureOrders(
                    structureId = structureId,
                    authorization = auth,
                    page = page,
                )
                if (pageResponse.code() == EsiHttpStatus.UNAUTHORIZED) throw HttpException(pageResponse)
                pageResponse
            }
            if (!response.isSuccessful) {
                if (response.code() == EsiHttpStatus.FORBIDDEN) throw MarketStructureAccessException()
                if (page > MarketConfig.FIRST_PAGE && response.code() == EsiHttpStatus.NOT_FOUND) break
                throw HttpException(response)
            }
            val chunk = response.body().orEmpty()
            matched += chunk.filter { it.typeId == typeId }.map { it.toOrder() }
            totalPages = totalPages(response, chunk.size, page)
            onPage(page, totalPages)
            page++
        }
        return matched
    }

    private suspend fun fetchHistory(regionId: Int, typeId: Int): List<MarketHistoryPoint> =
        marketApi.fetchRegionHistory(regionId, typeId)
            .map { it.toPoint() }
            .sortedBy { it.date }

    private suspend fun resolveStructures(
        structureIds: List<Long>,
        characterId: Long,
    ): Map<Long, MarketOrderLocation> {
        val resolved = mutableMapOf<Long, MarketOrderLocation>()
        for (structureId in structureIds) {
            val info = runCatching {
                tokenManager.executeWithAuthRetry(characterId) { auth ->
                    universeApi.fetchStructure(structureId, auth)
                }
            }.getOrNull() ?: continue
            val security = info.solarSystemId?.toInt()?.let { systemId ->
                roomProvider.getDatabase().mapDao().getMarketSystemPlace(systemId)?.securityStatus
            }
            resolved[structureId] = MarketOrderLocation(
                locationId = structureId,
                security = security,
                placeName = info.name,
            )
        }
        return resolved
    }

    private fun totalPages(response: Response<*>, chunkSize: Int, page: Int): Int {
        val header = response.headers()[EsiPagedQuery.PAGES_HEADER]?.toIntOrNull()
        return when {
            header != null -> header
            chunkSize < MarketConfig.ORDER_PAGE_SIZE -> page
            else -> page + 1
        }
    }

    private fun EsiMarketOrderDto.toOrder(): MarketOrder = MarketOrder(
        orderId = orderId,
        typeId = typeId,
        isBuyOrder = isBuyOrder,
        price = price,
        volumeRemain = volumeRemain,
        locationId = locationId,
        systemId = systemId,
    )

    private fun EsiMarketHistoryDto.toPoint(): MarketHistoryPoint = MarketHistoryPoint(
        date = date,
        average = average,
        volume = volume,
    )
}
