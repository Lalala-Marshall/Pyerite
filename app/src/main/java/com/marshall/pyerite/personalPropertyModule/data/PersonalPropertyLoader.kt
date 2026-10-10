package com.marshall.pyerite.personalPropertyModule.data

import com.marshall.pyerite.esiModule.api.EsiCharacterApi
import com.marshall.pyerite.esiModule.api.EsiMarketApi
import com.marshall.pyerite.esiModule.data.CharacterAssetListCache
import com.marshall.pyerite.esiModule.data.CharacterAssetListResult
import com.marshall.pyerite.esiModule.model.EsiCharacterAssetDto
import com.marshall.pyerite.esiModule.model.EsiCharacterContractDto
import com.marshall.pyerite.esiModule.model.EsiCharacterOrderDto
import com.marshall.pyerite.esiModule.model.EsiContractItemDto
import com.marshall.pyerite.esiModule.model.EsiContractStatusValue
import com.marshall.pyerite.esiModule.model.EsiContractTypeValue
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.personalPropertyModule.model.PersonalPropertyBucket
import com.marshall.pyerite.personalPropertyModule.model.PersonalPropertyConfig
import com.marshall.pyerite.personalPropertyModule.model.PersonalPropertyDecoratedItem
import com.marshall.pyerite.personalPropertyModule.model.PersonalPropertyDecoratedRanking
import com.marshall.pyerite.personalPropertyModule.model.PersonalPropertyRankedItem
import com.marshall.pyerite.personalPropertyModule.model.PersonalPropertyRanking
import com.marshall.pyerite.personalPropertyModule.model.PersonalPropertySnapshot
import com.marshall.pyerite.personalPropertyModule.model.PersonalPropertySummary
import com.marshall.pyerite.sdeModule.room.RoomProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Aggregates wallet, assets, plugged implants, market orders, and item-exchange
 * contracts into a net-worth summary, plus top-N type rankings.
 */
internal class PersonalPropertyLoader(
    private val tokenManager: EveTokenManager,
    private val characterApi: EsiCharacterApi,
    private val marketApi: EsiMarketApi,
    private val roomProvider: RoomProvider,
    private val assetListCache: CharacterAssetListCache,
) {
    @Volatile
    private var cachedPrices: Map<Int, Double>? = null

    @Volatile
    private var cachedPricesAtMs: Long = 0L

    private val pricesLock = Mutex()

    suspend fun load(
        characterId: Long,
        forceRefresh: Boolean = false,
    ): PersonalPropertySnapshot = withContext(Dispatchers.IO) {
        coroutineScope {
            val pricesDeferred = async { loadPrices() }
            val walletDeferred = async { loadWallet(characterId) }
            val assetsDeferred = async { loadAssets(characterId, forceRefresh) }
            val implantsDeferred = async { loadImplantTypeIds(characterId) }
            val ordersDeferred = async { loadOrders(characterId) }
            val contractsDeferred = async { loadContracts(characterId) }

            val prices = pricesDeferred.await()
            val assets = assetsDeferred.await()
            val implants = implantsDeferred.await()
            val orders = ordersDeferred.await()
            val contracts = contractsDeferred.await()
            PersonalPropertySnapshot(
                summary = PersonalPropertySummary(
                    characterId = characterId,
                    walletIsk = walletDeferred.await(),
                    assets = valueAssets(assets, prices),
                    implants = valueImplants(implants, prices),
                    marketOrders = valueOrders(orders, prices),
                    contracts = valueContracts(contracts, prices),
                ),
                assetsRanking = rankAssets(assets, prices),
                implantsRanking = rankImplants(implants, prices),
                marketOrdersRanking = rankOrders(orders, prices),
                contractsRanking = rankContracts(contracts, prices),
            )
        }
    }

    suspend fun decorateRanking(ranking: PersonalPropertyRanking): PersonalPropertyDecoratedRanking {
        val typeIds = (ranking.priced + ranking.unpriced).map { it.typeId }.distinct()
        val byId = if (typeIds.isEmpty()) {
            emptyMap()
        } else {
            runCatching {
                roomProvider.getDatabase().sdeTypeDao().getTypesForDisplay(typeIds)
                    .associateBy { it.id }
            }.getOrDefault(emptyMap())
        }
        fun decorate(item: PersonalPropertyRankedItem): PersonalPropertyDecoratedItem {
            val row = byId[item.typeId]
            return PersonalPropertyDecoratedItem(
                typeId = item.typeId,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                zhName = row?.zhName,
                enName = row?.enName,
                name = row?.name,
                iconFilename = row?.iconFilename,
            )
        }
        return PersonalPropertyDecoratedRanking(
            priced = ranking.priced.map(::decorate),
            unpriced = ranking.unpriced.map(::decorate),
        )
    }

    private suspend fun loadPrices(): Map<Int, Double>? {
        pricesLock.withLock {
            val now = System.currentTimeMillis()
            val cached = cachedPrices
            if (cached != null &&
                now - cachedPricesAtMs < PersonalPropertyConfig.MARKET_PRICE_CACHE_TTL_MS
            ) {
                return cached
            }
            return runCatching {
                marketApi.fetchPrices().mapNotNull { dto ->
                    val average = dto.averagePrice ?: return@mapNotNull null
                    dto.typeId to average
                }.toMap()
            }.getOrNull()?.also { prices ->
                cachedPrices = prices
                cachedPricesAtMs = now
            }
        }
    }

    private suspend fun loadWallet(characterId: Long): Double? = runCatching {
        tokenManager.executeWithAuthRetry(characterId) { auth ->
            characterApi.fetchWallet(characterId, auth).use { body ->
                body.string().trim().toDouble()
            }
        }
    }.getOrNull()

    private suspend fun loadAssets(
        characterId: Long,
        forceRefresh: Boolean,
    ): List<EsiCharacterAssetDto>? =
        when (val result = assetListCache.load(characterId, forceRefresh)) {
            is CharacterAssetListResult.Ready -> result.assets
            CharacterAssetListResult.Forbidden,
            CharacterAssetListResult.Failed,
            -> null
        }

    private suspend fun loadImplantTypeIds(characterId: Long): List<Int>? {
        val active = runCatching {
            tokenManager.executeWithAuthRetry(characterId) { auth ->
                characterApi.fetchImplants(characterId, auth)
            }
        }.getOrNull()
        val jumpImplants = runCatching {
            tokenManager.executeWithAuthRetry(characterId) { auth ->
                characterApi.fetchClones(characterId, auth)
            }.jumpClones.flatMap { it.implants }
        }.getOrNull()
        if (active == null && jumpImplants == null) return null
        return active.orEmpty() + jumpImplants.orEmpty()
    }

    private suspend fun loadOrders(characterId: Long): List<EsiCharacterOrderDto>? = runCatching {
        tokenManager.executeWithAuthRetry(characterId) { auth ->
            characterApi.fetchOrders(characterId, auth)
        }.filter { !it.isCorporation }
    }.getOrNull()

    private suspend fun loadContracts(characterId: Long): ContractLoadResult? {
        val contracts = runCatching { paginateContracts(characterId) }.getOrNull() ?: return null
        val outstanding = contracts.filter { contract ->
            contract.type == EsiContractTypeValue.ITEM_EXCHANGE &&
                contract.status == EsiContractStatusValue.OUTSTANDING &&
                contract.issuerId == characterId &&
                !contract.forCorporation
        }
        if (outstanding.isEmpty()) {
            return ContractLoadResult(count = 0L, includedItems = emptyList())
        }
        val semaphore = Semaphore(PersonalPropertyConfig.CONTRACT_ITEMS_CONCURRENCY)
        val itemPages = coroutineScope {
            outstanding.map { contract ->
                async {
                    semaphore.withPermit {
                        runCatching {
                            tokenManager.executeWithAuthRetry(characterId) { auth ->
                                characterApi.fetchContractItems(
                                    characterId,
                                    contract.contractId,
                                    auth,
                                )
                            }
                        }.getOrNull()
                    }
                }
            }.awaitAll()
        }
        val included = itemPages.filterNotNull().flatten().filter { it.isIncluded }
        val allItemFetchesFailed = itemPages.all { it == null }
        return ContractLoadResult(
            count = outstanding.size.toLong(),
            includedItems = if (allItemFetchesFailed) null else included,
        )
    }

    private suspend fun paginateContracts(characterId: Long): List<EsiCharacterContractDto> {
        val all = ArrayList<EsiCharacterContractDto>()
        var page = PersonalPropertyConfig.FIRST_PAGE
        var pagesRemaining = PersonalPropertyConfig.CONTRACTS_MAX_PAGES
        while (pagesRemaining > 0) {
            pagesRemaining--
            val chunk = tokenManager.executeWithAuthRetry(characterId) { auth ->
                characterApi.fetchContracts(characterId, auth, page)
            }
            all.addAll(chunk)
            if (chunk.size < PersonalPropertyConfig.CONTRACTS_PAGE_SIZE) break
            page++
        }
        return all
    }

    private fun valueAssets(
        stacks: List<EsiCharacterAssetDto>?,
        prices: Map<Int, Double>?,
    ): PersonalPropertyBucket {
        if (stacks == null) return PersonalPropertyBucket()
        if (stacks.isEmpty()) return PersonalPropertyBucket(count = 0L, isk = 0.0)
        if (prices == null) return PersonalPropertyBucket()
        var count = 0L
        var isk = 0.0
        stacks.forEach { asset ->
            if (asset.isBlueprintCopy) return@forEach
            val price = prices[asset.typeId] ?: return@forEach
            count += 1
            isk += assetQuantity(asset).toDouble() * price
        }
        return PersonalPropertyBucket(count = count, isk = isk)
    }

    private fun valueImplants(
        typeIds: List<Int>?,
        prices: Map<Int, Double>?,
    ): PersonalPropertyBucket {
        if (typeIds == null) return PersonalPropertyBucket()
        if (typeIds.isEmpty()) return PersonalPropertyBucket(count = 0L, isk = 0.0)
        var isk = 0.0
        if (prices != null) {
            typeIds.forEach { typeId ->
                isk += prices.priceOf(typeId)
            }
        }
        return PersonalPropertyBucket(
            count = typeIds.size.toLong(),
            isk = if (prices == null) null else isk,
        )
    }

    private fun valueOrders(
        orders: List<EsiCharacterOrderDto>?,
        prices: Map<Int, Double>?,
    ): PersonalPropertyBucket {
        if (orders == null) return PersonalPropertyBucket()
        if (orders.isEmpty()) return PersonalPropertyBucket(count = 0L, isk = 0.0)
        val sellOrders = orders.filter { !it.isBuyOrder }
        if (prices == null && sellOrders.isNotEmpty()) {
            return PersonalPropertyBucket(count = orders.size.toLong(), isk = null)
        }
        var isk = 0.0
        orders.forEach { order ->
            if (order.isBuyOrder) {
                isk += order.escrow
            } else if (prices != null) {
                isk += order.volumeRemain.toDouble() * prices.priceOf(order.typeId)
            }
        }
        return PersonalPropertyBucket(count = orders.size.toLong(), isk = isk)
    }

    private fun valueContracts(
        result: ContractLoadResult?,
        prices: Map<Int, Double>?,
    ): PersonalPropertyBucket {
        if (result == null) return PersonalPropertyBucket()
        if (result.count == 0L) return PersonalPropertyBucket(count = 0L, isk = 0.0)
        val items = result.includedItems
        val isk = if (items == null || prices == null) {
            null
        } else {
            var total = 0.0
            items.forEach { item ->
                val quantity = if (item.quantity > 0) item.quantity.toLong() else 1L
                total += quantity.toDouble() * prices.priceOf(item.typeId)
            }
            total
        }
        return PersonalPropertyBucket(count = result.count, isk = isk)
    }

    private fun rankAssets(
        stacks: List<EsiCharacterAssetDto>?,
        prices: Map<Int, Double>?,
    ): PersonalPropertyRanking {
        if (stacks == null) return PersonalPropertyRanking.EMPTY
        val quantities = LinkedHashMap<Int, Long>()
        stacks.forEach { asset ->
            if (asset.isBlueprintCopy) return@forEach
            addQuantity(quantities, asset.typeId, assetQuantity(asset))
        }
        return rankTypes(quantities, prices)
    }

    private fun rankImplants(
        typeIds: List<Int>?,
        prices: Map<Int, Double>?,
    ): PersonalPropertyRanking {
        if (typeIds == null) return PersonalPropertyRanking.EMPTY
        val quantities = LinkedHashMap<Int, Long>()
        typeIds.forEach { typeId ->
            addQuantity(quantities, typeId, 1L)
        }
        return rankTypes(quantities, prices)
    }

    private fun rankOrders(
        orders: List<EsiCharacterOrderDto>?,
        prices: Map<Int, Double>?,
    ): PersonalPropertyRanking {
        if (orders == null) return PersonalPropertyRanking.EMPTY
        val quantities = LinkedHashMap<Int, Long>()
        orders.forEach { order ->
            val quantity = if (order.volumeRemain > 0) order.volumeRemain.toLong() else 1L
            addQuantity(quantities, order.typeId, quantity)
        }
        return rankTypes(quantities, prices)
    }

    private fun rankContracts(
        result: ContractLoadResult?,
        prices: Map<Int, Double>?,
    ): PersonalPropertyRanking {
        val items = result?.includedItems ?: return PersonalPropertyRanking.EMPTY
        val quantities = LinkedHashMap<Int, Long>()
        items.forEach { item ->
            val quantity = if (item.quantity > 0) item.quantity.toLong() else 1L
            addQuantity(quantities, item.typeId, quantity)
        }
        return rankTypes(quantities, prices)
    }

    private fun rankTypes(
        quantities: Map<Int, Long>,
        prices: Map<Int, Double>?,
    ): PersonalPropertyRanking {
        if (quantities.isEmpty()) return PersonalPropertyRanking.EMPTY
        val priced = ArrayList<PersonalPropertyRankedItem>()
        val unpriced = ArrayList<PersonalPropertyRankedItem>()
        quantities.forEach { (typeId, quantity) ->
            val unitPrice = prices?.get(typeId)
            val item = PersonalPropertyRankedItem(
                typeId = typeId,
                quantity = quantity,
                unitPrice = unitPrice,
            )
            if (unitPrice != null) {
                priced += item
            } else {
                unpriced += item
            }
        }
        return PersonalPropertyRanking(
            priced = priced.sortedWith(
                compareByDescending<PersonalPropertyRankedItem> { it.totalIsk }
                    .thenBy { it.typeId },
            ).take(PersonalPropertyConfig.RANKING_TOP_N),
            unpriced = unpriced.sortedWith(
                compareByDescending<PersonalPropertyRankedItem> { it.quantity }
                    .thenBy { it.typeId },
            ).take(PersonalPropertyConfig.RANKING_TOP_N),
        )
    }

    private fun addQuantity(into: MutableMap<Int, Long>, typeId: Int, quantity: Long) {
        into[typeId] = (into[typeId] ?: 0L) + quantity
    }

    private fun assetQuantity(asset: EsiCharacterAssetDto): Long {
        if (asset.isSingleton) return 1L
        val quantity = asset.quantity
        return if (quantity > 0) quantity.toLong() else 1L
    }

    private data class ContractLoadResult(
        val count: Long,
        val includedItems: List<EsiContractItemDto>?,
    )
}

private fun Map<Int, Double>.priceOf(typeId: Int): Double = this[typeId] ?: 0.0
