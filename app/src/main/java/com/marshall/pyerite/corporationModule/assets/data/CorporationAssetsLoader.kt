package com.marshall.pyerite.corporationModule.assets.data

import androidx.annotation.StringRes
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetContainerView
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetContentSection
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetFlagFolder
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetFlags
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetFolderKey
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetFolderView
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetItemRow
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetLocationKey
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetLocationKind
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetLocationView
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetPlaceRow
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetRegionKind
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetRegionSection
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetSlotSection
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetStationChild
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetsAccessException
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetsConfig
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetsSnapshot
import com.marshall.pyerite.esiModule.api.EsiCorporationApi
import com.marshall.pyerite.esiModule.api.EsiUniverseApi
import com.marshall.pyerite.esiModule.data.EsiPublicDataSource
import com.marshall.pyerite.esiModule.model.EsiCorporationAssetDto
import com.marshall.pyerite.esiModule.model.EsiCorporationAssetLocationDto
import com.marshall.pyerite.esiModule.model.EsiCorporationAssetNameDto
import com.marshall.pyerite.esiModule.model.EsiHttpStatus
import com.marshall.pyerite.esiModule.model.EsiPagedQuery
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.sdeModule.room.RoomProvider
import com.marshall.pyerite.sdeModule.room.map.SolarSystemPositionRow
import com.marshall.pyerite.sdeModule.room.type.TypeAssetRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.util.ArrayDeque
import kotlin.math.pow
import kotlin.math.sqrt

internal class CorporationAssetsLoader(
    private val tokenManager: EveTokenManager,
    private val corporationApi: EsiCorporationApi,
    private val universeApi: EsiUniverseApi,
    private val publicEsi: EsiPublicDataSource,
    private val roomProvider: RoomProvider,
) {
    suspend fun load(characterId: Long): CorporationAssetsSnapshot = withContext(Dispatchers.IO) {
        requireAssetsScope(characterId)
        val corporationId = resolveCorporationId(characterId)
        val assets = fetchAssets(characterId, corporationId)
        if (assets.isEmpty()) return@withContext CorporationAssetsSnapshot.EMPTY

        val tree = AssetTree(assets)
        val structureDetails = loadStructureDetails(characterId, corporationId)
        val safetyDockableIds = tree.safetyRoots.map { it.locationId }.filter { locationId ->
            locationId != CorporationAssetsConfig.ASSET_SAFETY_LOCATION_ID && !isSolarSystemId(locationId)
        }
        val dockableIds = (
            tree.stationRoots.keys +
                tree.unknownRoots.keys.filterNot { isSolarSystemId(it) } +
                safetyDockableIds
            ).distinct()
        val dockables = loadDockables(characterId, dockableIds, structureDetails)
        val safetyBuckets = classifySafety(
            roots = tree.safetyRoots,
            dockables = dockables,
            coordinateSystemByItem = resolveSafetySystems(
                characterId = characterId,
                corporationId = corporationId,
                roots = tree.safetyRoots.filter { root ->
                    safetyAnchor(root, dockables) == null
                },
            ),
        )
        val spaceSystemIds = (tree.systemRoots.keys + safetyBuckets.bySystem.keys).distinct()
        val systemIds = (
            spaceSystemIds + dockables.values.mapNotNull { it.solarSystemId }
            ).distinct()
        val systems = loadSystems(systemIds)
        val starTypeIds = loadStarTypeIds(spaceSystemIds)
        val types = loadTypes(
            assetTypeIds = assets.map { it.typeId },
            stationTypeIds = dockables.values.mapNotNull { it.typeId },
            starTypeIds = starTypeIds.values,
        )
        val hangarNames = loadHangarNames(characterId, corporationId)
        val assetNames = loadAssetNames(
            characterId = characterId,
            corporationId = corporationId,
            tree = tree,
            types = types,
            itemIds = nameCandidateIds(tree, types),
            structureNames = structureDetails.mapNotNull { (structureId, dockable) ->
                dockable.name?.takeIf { it.isNotBlank() }?.let { structureId to it }
            }.toMap(),
        )
        Assembler(
            tree = tree,
            types = types,
            customNames = assetNames,
            hangarNames = hangarNames,
            dockables = dockables,
            systems = systems,
            starTypeIds = starTypeIds,
            safetyByStation = safetyBuckets.byStation,
            safetyBySystem = safetyBuckets.bySystem,
            safetyUnresolved = safetyBuckets.unresolved,
        ).assemble()
    }

    private fun requireAssetsScope(characterId: Long) {
        if (EveSsoScope.ASSETS_READ_CORPORATION !in tokenManager.grantedScopes(characterId)) {
            throw CorporationAssetsAccessException()
        }
    }

    private suspend fun resolveCorporationId(characterId: Long): Long {
        val corporationId = publicEsi.fetchCharacter(characterId).corporationId
        if (corporationId == null || corporationId <= 0L) {
            throw CorporationAssetsAccessException()
        }
        return corporationId
    }

    private suspend fun fetchAssets(
        characterId: Long,
        corporationId: Long,
    ): List<EsiCorporationAssetDto> {
        val byId = LinkedHashMap<Long, EsiCorporationAssetDto>()
        var page = CorporationAssetsConfig.FIRST_PAGE
        var totalPages = CorporationAssetsConfig.FIRST_PAGE
        while (page <= totalPages && page <= CorporationAssetsConfig.ASSETS_MAX_PAGES) {
            val response = fetchAssetsPage(characterId, corporationId, page)
            if (!response.isSuccessful) {
                if (page > CorporationAssetsConfig.FIRST_PAGE &&
                    response.code() == EsiHttpStatus.NOT_FOUND
                ) {
                    break
                }
                throw mapAssetsError(response)
            }
            val chunk = response.body().orEmpty()
            chunk.forEach { asset -> byId[asset.itemId] = asset }
            totalPages = resolveTotalPages(
                headerPages = response.headers()[EsiPagedQuery.PAGES_HEADER]?.toIntOrNull(),
                chunkSize = chunk.size,
                page = page,
            )
            page++
        }
        return byId.values.toList()
    }

    private suspend fun fetchAssetsPage(
        characterId: Long,
        corporationId: Long,
        page: Int,
    ): Response<List<EsiCorporationAssetDto>> {
        return tokenManager.executeWithAuthRetry(characterId) { auth ->
            val response = corporationApi.fetchAssets(
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

    private fun resolveTotalPages(headerPages: Int?, chunkSize: Int, page: Int): Int = when {
        headerPages != null -> headerPages
        chunkSize < CorporationAssetsConfig.ASSETS_PAGE_SIZE -> page
        else -> page + 1
    }

    private fun mapAssetsError(response: Response<*>): Throwable {
        if (response.code() == EsiHttpStatus.FORBIDDEN) {
            return CorporationAssetsAccessException()
        }
        return HttpException(response)
    }

    private suspend fun loadDockables(
        characterId: Long,
        locationIds: List<Long>,
        structureDetails: Map<Long, ResolvedDockable>,
    ): Map<Long, ResolvedDockable> {
        if (locationIds.isEmpty()) return emptyMap()
        val resolved = LinkedHashMap<Long, ResolvedDockable>()
        structureDetails.forEach { (id, dockable) ->
            if (id in locationIds) resolved[id] = dockable
        }
        val needLookup = locationIds.filter { it !in resolved }
        if (needLookup.isEmpty()) return resolved
        val dao = roomProvider.getDatabase().mapDao()
        val npcIds = needLookup.filter { isNpcStationId(it) }
        npcIds.chunked(CorporationAssetsConfig.QUERY_CHUNK).flatMap { chunk ->
            runCatching { dao.getStations(chunk) }.getOrDefault(emptyList())
        }.forEach { row ->
            resolved[row.stationId] = ResolvedDockable(
                name = row.name?.takeIf { it.isNotBlank() },
                typeId = row.typeId,
                solarSystemId = row.solarSystemId?.toLong(),
            )
        }
        val stillMissing = needLookup.filter { it !in resolved }
        stillMissing.forEach { locationId ->
            if (isNpcStationId(locationId)) {
                val station = publicEsi.fetchStation(locationId)
                if (station != null) {
                    resolved[locationId] = ResolvedDockable(
                        name = station.name.takeIf { it.isNotBlank() },
                        typeId = station.typeId,
                        solarSystemId = station.systemId,
                    )
                    return@forEach
                }
            }
            val structure = fetchUniverseStructure(characterId, locationId) ?: return@forEach
            resolved[locationId] = structure
        }
        return resolved
    }

    private suspend fun fetchUniverseStructure(
        characterId: Long,
        structureId: Long,
    ): ResolvedDockable? {
        if (EveSsoScope.UNIVERSE_READ_STRUCTURES !in tokenManager.grantedScopes(characterId)) {
            return null
        }
        val dto = runCatching {
            tokenManager.executeWithAuthRetry(characterId) { auth ->
                universeApi.fetchStructure(structureId, auth)
            }
        }.getOrNull() ?: return null
        return ResolvedDockable(
            name = dto.name.takeIf { it.isNotBlank() },
            typeId = dto.typeId,
            solarSystemId = dto.solarSystemId,
        )
    }

    private suspend fun loadSystems(systemIds: List<Long>): Map<Long, ResolvedSystem> {
        if (systemIds.isEmpty()) return emptyMap()
        val dao = roomProvider.getDatabase().mapDao()
        val fromSde = systemIds.chunked(CorporationAssetsConfig.QUERY_CHUNK).flatMap { chunk ->
            runCatching { dao.getSolarSystemRegionLocations(chunk) }.getOrDefault(emptyList())
        }.mapNotNull { row ->
            val resolved = ResolvedSystem(
                systemZhName = row.systemZhName,
                systemEnName = row.systemEnName,
                systemName = row.systemName,
                regionZhName = row.regionZhName,
                regionEnName = row.regionEnName,
                regionName = row.regionName,
                securityStatus = row.securityStatus,
            )
            if (!resolved.hasSystemName() && resolved.securityStatus == null && !resolved.hasRegionName()) {
                return@mapNotNull null
            }
            row.solarSystemId to resolved
        }.toMap()
        val missing = systemIds.filter { it !in fromSde }
        if (missing.isEmpty()) return fromSde
        val fromEsi = missing.mapNotNull { systemId ->
            val name = publicEsi.fetchSolarSystemName(systemId)?.takeIf { it.isNotBlank() }
            val security = publicEsi.fetchSolarSystemSecurity(systemId)
            if (name == null && security == null) return@mapNotNull null
            systemId to ResolvedSystem(
                systemZhName = null,
                systemEnName = null,
                systemName = name,
                regionZhName = null,
                regionEnName = null,
                regionName = null,
                securityStatus = security,
            )
        }.toMap()
        return fromSde + fromEsi
    }

    private suspend fun loadStarTypeIds(systemIds: List<Long>): Map<Long, Int> {
        if (systemIds.isEmpty()) return emptyMap()
        val resolved = LinkedHashMap<Long, Int>()
        systemIds.chunked(CorporationAssetsConfig.STAR_LOOKUP_CONCURRENCY).forEach { chunk ->
            val pairs = coroutineScope {
                chunk.map { systemId ->
                    async {
                        val typeId = publicEsi.fetchSolarSystemStarTypeId(systemId)
                            ?: CorporationAssetsConfig.FALLBACK_SUN_TYPE_ID
                        systemId to typeId
                    }
                }.awaitAll()
            }
            pairs.forEach { (systemId, typeId) -> resolved[systemId] = typeId }
        }
        return resolved
    }

    private suspend fun loadTypes(
        assetTypeIds: List<Int>,
        stationTypeIds: List<Int>,
        starTypeIds: Collection<Int>,
    ): Map<Int, TypeAssetRow> {
        val ids = (assetTypeIds + stationTypeIds + starTypeIds).filter { it > 0 }.distinct()
        if (ids.isEmpty()) return emptyMap()
        val dao = roomProvider.getDatabase().sdeTypeDao()
        return ids.chunked(CorporationAssetsConfig.QUERY_CHUNK).flatMap { chunk ->
            runCatching { dao.getTypesForAssets(chunk) }.getOrDefault(emptyList())
        }.associateBy { it.id }
    }

    private suspend fun loadHangarNames(characterId: Long, corporationId: Long): Map<Int, String> {
        if (EveSsoScope.CORPORATIONS_READ_DIVISIONS !in tokenManager.grantedScopes(characterId)) {
            return emptyMap()
        }
        val divisions = runCatching {
            tokenManager.executeWithAuthRetry(characterId) { auth ->
                corporationApi.fetchDivisions(corporationId, auth)
            }
        }.getOrNull() ?: return emptyMap()
        return divisions.hangar.mapNotNull { division ->
            val name = division.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            division.division to name
        }.toMap()
    }

    private suspend fun loadStructureDetails(
        characterId: Long,
        corporationId: Long,
    ): Map<Long, ResolvedDockable> {
        if (EveSsoScope.CORPORATIONS_READ_STRUCTURES !in tokenManager.grantedScopes(characterId)) {
            return emptyMap()
        }
        return try {
            fetchStructureDetails(characterId, corporationId)
        } catch (_: HttpException) {
            emptyMap()
        }
    }

    private suspend fun fetchStructureDetails(
        characterId: Long,
        corporationId: Long,
    ): Map<Long, ResolvedDockable> {
        val byId = LinkedHashMap<Long, ResolvedDockable>()
        var page = CorporationAssetsConfig.FIRST_PAGE
        var totalPages = CorporationAssetsConfig.FIRST_PAGE
        while (page <= totalPages && page <= CorporationAssetsConfig.ASSETS_MAX_PAGES) {
            val response = tokenManager.executeWithAuthRetry(characterId) { auth ->
                val result = corporationApi.fetchStructures(
                    corporationId = corporationId,
                    authorization = auth,
                    page = page,
                )
                if (result.code() == EsiHttpStatus.UNAUTHORIZED) throw HttpException(result)
                result
            }
            if (!response.isSuccessful) {
                if (page > CorporationAssetsConfig.FIRST_PAGE &&
                    response.code() == EsiHttpStatus.NOT_FOUND
                ) {
                    break
                }
                throw HttpException(response)
            }
            val chunk = response.body().orEmpty()
            chunk.forEach { structure ->
                byId[structure.structureId] = ResolvedDockable(
                    name = structure.name?.takeIf { it.isNotBlank() },
                    typeId = structure.typeId.takeIf { it > 0 },
                    solarSystemId = structure.systemId.takeIf { it > 0L },
                )
            }
            totalPages = resolveTotalPages(
                headerPages = response.headers()[EsiPagedQuery.PAGES_HEADER]?.toIntOrNull(),
                chunkSize = chunk.size,
                page = page,
            )
            page++
        }
        return byId
    }

    /**
     * Prefer the asset's own location id (original station/structure or system).
     * Coordinates are only a fallback for the asset-safety holding location (2004).
     */
    private fun safetyAnchor(
        root: EsiCorporationAssetDto,
        dockables: Map<Long, ResolvedDockable>,
    ): SafetyAnchor? {
        val locationId = root.locationId
        if (isSolarSystemId(locationId)) {
            return SafetyAnchor(stationId = null, systemId = locationId)
        }
        if (locationId != CorporationAssetsConfig.ASSET_SAFETY_LOCATION_ID) {
            val dockable = dockables[locationId]
            if (dockable?.solarSystemId != null) {
                return SafetyAnchor(stationId = locationId, systemId = dockable.solarSystemId)
            }
        }
        return null
    }

    private fun classifySafety(
        roots: List<EsiCorporationAssetDto>,
        dockables: Map<Long, ResolvedDockable>,
        coordinateSystemByItem: Map<Long, Long>,
    ): SafetyBuckets {
        val byStation = LinkedHashMap<Long, MutableList<EsiCorporationAssetDto>>()
        val bySystem = LinkedHashMap<Long, MutableList<EsiCorporationAssetDto>>()
        val unresolved = ArrayList<EsiCorporationAssetDto>()
        roots.forEach { root ->
            val anchor = safetyAnchor(root, dockables)
            when {
                anchor?.stationId != null ->
                    byStation.getOrPut(anchor.stationId) { ArrayList() }.add(root)
                anchor?.systemId != null ->
                    bySystem.getOrPut(anchor.systemId) { ArrayList() }.add(root)
                else -> {
                    val systemId = coordinateSystemByItem[root.itemId]
                    if (systemId == null) unresolved.add(root)
                    else bySystem.getOrPut(systemId) { ArrayList() }.add(root)
                }
            }
        }
        return SafetyBuckets(
            byStation = byStation,
            bySystem = bySystem,
            unresolved = unresolved,
        )
    }

    private suspend fun resolveSafetySystems(
        characterId: Long,
        corporationId: Long,
        roots: List<EsiCorporationAssetDto>,
    ): Map<Long, Long> {
        if (roots.isEmpty()) return emptyMap()
        val locations = loadAssetLocations(
            characterId = characterId,
            corporationId = corporationId,
            itemIds = roots.map { it.itemId },
        )
        if (locations.isEmpty()) return emptyMap()
        val positions = runCatching {
            roomProvider.getDatabase().mapDao().getSolarSystemPositions()
        }.getOrDefault(emptyList())
        if (positions.isEmpty()) return emptyMap()
        return locations.mapNotNull { location ->
            val position = location.position
            if (position.x == 0.0 && position.y == 0.0 && position.z == 0.0) {
                return@mapNotNull null
            }
            val systemId = nearestSolarSystemId(position.x, position.y, position.z, positions)
                ?: return@mapNotNull null
            location.itemId to systemId
        }.toMap()
    }

    private suspend fun loadAssetLocations(
        characterId: Long,
        corporationId: Long,
        itemIds: List<Long>,
    ): List<EsiCorporationAssetLocationDto> {
        if (itemIds.isEmpty()) return emptyList()
        val out = ArrayList<EsiCorporationAssetLocationDto>()
        itemIds.distinct().chunked(CorporationAssetsConfig.ASSET_NAMES_CHUNK).forEach { chunk ->
            val fetched = runCatching {
                tokenManager.executeWithAuthRetry(characterId) { auth ->
                    corporationApi.fetchAssetLocations(
                        corporationId = corporationId,
                        authorization = auth,
                        itemIds = chunk,
                    )
                }
            }.getOrNull().orEmpty()
            out.addAll(fetched)
        }
        return out
    }

    private fun nearestSolarSystemId(
        x: Double,
        y: Double,
        z: Double,
        positions: List<SolarSystemPositionRow>,
    ): Long? {
        var bestId: Long? = null
        var bestDistance = Double.POSITIVE_INFINITY
        positions.forEach { row ->
            val px = row.x ?: return@forEach
            val py = row.y ?: return@forEach
            val pz = row.z ?: return@forEach
            val distance = sqrt((x - px).pow(2) + (y - py).pow(2) + (z - pz).pow(2))
            if (distance < bestDistance) {
                bestDistance = distance
                bestId = row.solarSystemId
            }
        }
        return bestId
    }

    private fun isNpcStationId(locationId: Long): Boolean =
        locationId in CorporationAssetsConfig.NPC_STATION_ID_MIN..CorporationAssetsConfig.NPC_STATION_ID_MAX

    private fun isSolarSystemId(locationId: Long): Boolean =
        locationId in CorporationAssetsConfig.SOLAR_SYSTEM_ID_MIN..CorporationAssetsConfig.SOLAR_SYSTEM_ID_MAX

    private fun nameCandidateIds(tree: AssetTree, types: Map<Int, TypeAssetRow>): List<Long> =
        tree.assets.mapNotNull { asset ->
            if (!asset.isSingleton) return@mapNotNull null
            val type = types[asset.typeId]
            val namedCategory = type?.categoryId == CorporationAssetsConfig.CATEGORY_SHIP ||
                type?.categoryId == CorporationAssetsConfig.CATEGORY_STRUCTURE ||
                type?.categoryId == CorporationAssetsConfig.CATEGORY_STARBASE
            val worthNaming = asset.locationType == CorporationAssetFlags.LOCATION_SOLAR_SYSTEM ||
                asset.locationFlag == CorporationAssetFlags.OFFICE_FOLDER ||
                tree.childrenByParent.containsKey(asset.itemId) ||
                namedCategory
            if (worthNaming) asset.itemId else null
        }

    private suspend fun loadAssetNames(
        characterId: Long,
        corporationId: Long,
        tree: AssetTree,
        types: Map<Int, TypeAssetRow>,
        itemIds: List<Long>,
        structureNames: Map<Long, String>,
    ): Map<Long, String> {
        val names = LinkedHashMap<Long, String>()
        names.putAll(structureNames)
        if (itemIds.isNotEmpty()) {
            itemIds.distinct().chunked(CorporationAssetsConfig.ASSET_NAMES_CHUNK).forEach { chunk ->
                names.putAll(
                    collectNames(
                        characterId = characterId,
                        corporationId = corporationId,
                        itemIds = chunk,
                    ),
                )
            }
        }
        val stillMissing = itemIds.filter { id ->
            names[id].isNullOrBlank() && isStructureLike(tree.byId[id], types)
        }
        stillMissing.forEach { itemId ->
            val structure = fetchUniverseStructure(characterId, itemId) ?: return@forEach
            structure.name?.takeIf { it.isNotBlank() }?.let { names[itemId] = it }
        }
        return names
    }

    private fun isStructureLike(
        asset: EsiCorporationAssetDto?,
        types: Map<Int, TypeAssetRow>,
    ): Boolean {
        val categoryId = asset?.let { types[it.typeId]?.categoryId } ?: return false
        return categoryId == CorporationAssetsConfig.CATEGORY_STRUCTURE ||
            categoryId == CorporationAssetsConfig.CATEGORY_STARBASE
    }

    /**
     * ESI rejects the whole names request when any id cannot be named.
     * Split until a single id so one failure does not drop the rest.
     */
    private suspend fun collectNames(
        characterId: Long,
        corporationId: Long,
        itemIds: List<Long>,
    ): Map<Long, String> {
        if (itemIds.isEmpty()) return emptyMap()
        val fetched = runCatching {
            tokenManager.executeWithAuthRetry(characterId) { auth ->
                corporationApi.fetchAssetNames(
                    corporationId = corporationId,
                    authorization = auth,
                    itemIds = itemIds,
                )
            }
        }.getOrNull()
        if (fetched != null) return fetched.toNameMap()
        if (itemIds.size == 1) return emptyMap()
        val mid = itemIds.size / 2
        val merged = LinkedHashMap<Long, String>()
        merged.putAll(
            collectNames(
                characterId = characterId,
                corporationId = corporationId,
                itemIds = itemIds.subList(0, mid),
            ),
        )
        merged.putAll(
            collectNames(
                characterId = characterId,
                corporationId = corporationId,
                itemIds = itemIds.subList(mid, itemIds.size),
            ),
        )
        return merged
    }

    private fun List<EsiCorporationAssetNameDto>.toNameMap(): Map<Long, String> =
        mapNotNull { dto ->
            val name = dto.name.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            dto.itemId to name
        }.toMap()

    private class AssetTree(val assets: List<EsiCorporationAssetDto>) {
        val byId: Map<Long, EsiCorporationAssetDto> = assets.associateBy { it.itemId }
        val childrenByParent: Map<Long, List<EsiCorporationAssetDto>>
        val roots: List<EsiCorporationAssetDto>
        val stationRoots: Map<Long, List<EsiCorporationAssetDto>>
        val systemRoots: Map<Long, List<EsiCorporationAssetDto>>
        val safetyRoots: List<EsiCorporationAssetDto>
        val unknownRoots: Map<Long, List<EsiCorporationAssetDto>>

        init {
            val childBuckets = LinkedHashMap<Long, MutableList<EsiCorporationAssetDto>>()
            val rootBucket = ArrayList<EsiCorporationAssetDto>()
            assets.forEach { asset ->
                val parent = byId[asset.locationId]
                if (parent != null && parent.itemId != asset.itemId) {
                    childBuckets.getOrPut(asset.locationId) { ArrayList() }.add(asset)
                } else {
                    rootBucket.add(asset)
                }
            }
            childrenByParent = childBuckets
            roots = rootBucket
            val stations = LinkedHashMap<Long, MutableList<EsiCorporationAssetDto>>()
            val systems = LinkedHashMap<Long, MutableList<EsiCorporationAssetDto>>()
            val safety = ArrayList<EsiCorporationAssetDto>()
            val unknown = LinkedHashMap<Long, MutableList<EsiCorporationAssetDto>>()
            roots.forEach { asset ->
                when {
                    asset.locationId == CorporationAssetsConfig.ASSET_SAFETY_LOCATION_ID ||
                        asset.locationFlag == CorporationAssetFlags.ASSET_SAFETY -> safety.add(asset)
                    asset.locationType == CorporationAssetFlags.LOCATION_STATION ->
                        stations.getOrPut(asset.locationId) { ArrayList() }.add(asset)
                    asset.locationType == CorporationAssetFlags.LOCATION_SOLAR_SYSTEM ->
                        systems.getOrPut(asset.locationId) { ArrayList() }.add(asset)
                    else -> unknown.getOrPut(asset.locationId) { ArrayList() }.add(asset)
                }
            }
            stationRoots = stations
            systemRoots = systems
            safetyRoots = safety
            unknownRoots = unknown
        }

        fun descendants(rootId: Long): List<EsiCorporationAssetDto> {
            val out = ArrayList<EsiCorporationAssetDto>()
            val pending = ArrayDeque<Long>()
            val seen = HashSet<Long>()
            pending.add(rootId)
            seen.add(rootId)
            while (pending.isNotEmpty()) {
                val id = pending.removeFirst()
                childrenByParent[id].orEmpty().forEach { child ->
                    if (seen.add(child.itemId)) {
                        out.add(child)
                        pending.add(child.itemId)
                    }
                }
            }
            return out
        }

        fun forest(roots: List<EsiCorporationAssetDto>): List<EsiCorporationAssetDto> {
            val out = ArrayList<EsiCorporationAssetDto>(roots.size)
            out.addAll(roots)
            roots.forEach { root -> out.addAll(descendants(root.itemId)) }
            return out
        }
    }

    private class Assembler(
        private val tree: AssetTree,
        private val types: Map<Int, TypeAssetRow>,
        private val customNames: Map<Long, String>,
        private val hangarNames: Map<Int, String>,
        private val dockables: Map<Long, ResolvedDockable>,
        private val systems: Map<Long, ResolvedSystem>,
        private val starTypeIds: Map<Long, Int>,
        private val safetyByStation: Map<Long, List<EsiCorporationAssetDto>>,
        private val safetyBySystem: Map<Long, List<EsiCorporationAssetDto>>,
        private val safetyUnresolved: List<EsiCorporationAssetDto>,
    ) {
        private val locations = LinkedHashMap<CorporationAssetLocationKey, CorporationAssetLocationView>()
        private val folders = LinkedHashMap<CorporationAssetFolderKey, CorporationAssetFolderView>()
        private val containers = LinkedHashMap<Long, CorporationAssetContainerView>()
        private val regionPlaces = LinkedHashMap<RegionIdentity, MutableList<CorporationAssetPlaceRow>>()

        fun assemble(): CorporationAssetsSnapshot {
            val dockableUnknownIds = HashSet<Long>()
            val stationIds = (tree.stationRoots.keys + safetyByStation.keys).toSet()
            stationIds.forEach { stationId ->
                val roots = tree.stationRoots[stationId].orEmpty() +
                    safetyByStation[stationId].orEmpty()
                if (roots.isNotEmpty()) addDockable(stationId, roots)
            }
            tree.unknownRoots.forEach { (locationId, roots) ->
                if (locationId in dockables && locationId !in stationIds) {
                    dockableUnknownIds.add(locationId)
                    addDockable(locationId, roots)
                }
            }
            val systemIds = (tree.systemRoots.keys + safetyBySystem.keys).toSet()
            systemIds.forEach { systemId ->
                val roots = tree.systemRoots[systemId].orEmpty() +
                    safetyBySystem[systemId].orEmpty()
                if (roots.isNotEmpty()) addSolarSystem(systemId, roots)
            }
            tree.unknownRoots.forEach { (locationId, roots) ->
                if (locationId !in dockableUnknownIds && locationId !in stationIds) {
                    addUnknown(locationId, roots)
                }
            }
            if (safetyUnresolved.isNotEmpty()) {
                addUnknown(CorporationAssetsConfig.ASSET_SAFETY_LOCATION_ID, safetyUnresolved)
            }
            tree.assets.forEach { asset ->
                if (isContainer(asset) && asset.itemId !in containers) {
                    containers[asset.itemId] = buildContainer(asset)
                }
            }
            val regions = regionPlaces.map { (identity, places) ->
                CorporationAssetRegionSection(
                    kind = identity.kind,
                    zhName = identity.zhName,
                    enName = identity.enName,
                    name = identity.name,
                    places = places,
                )
            }
            return CorporationAssetsSnapshot(
                regions = regions,
                locations = locations,
                folders = folders,
                containers = containers,
            )
        }

        private fun addDockable(locationId: Long, roots: List<EsiCorporationAssetDto>) {
            val dockable = dockables[locationId]
            val system = dockable?.solarSystemId?.let { systems[it] }
            val type = dockable?.typeId?.let { types[it] }
            val key = CorporationAssetLocationKey(CorporationAssetLocationKind.STATION, locationId)
            val icon = type?.iconFilename
            addPlace(
                identity = regionIdentity(system),
                place = CorporationAssetPlaceRow(
                    key = key,
                    iconFilename = icon,
                    securityStatus = system?.securityStatus,
                    typeZhName = type?.zhName,
                    typeEnName = type?.enName,
                    typeName = type?.name,
                    zhName = null,
                    enName = null,
                    name = dockable?.name,
                    itemCount = roots.size,
                ),
            )
            locations[key] = CorporationAssetLocationView(
                key = key,
                iconFilename = icon,
                securityStatus = system?.securityStatus,
                typeZhName = type?.zhName,
                typeEnName = type?.enName,
                typeName = type?.name,
                zhName = null,
                enName = null,
                name = dockable?.name,
                typeCount = distinctTypeCount(tree.forest(roots)),
                itemCount = roots.size,
                showsItems = false,
                stationChildren = buildStationCategories(key, roots),
                items = emptyList(),
            )
        }

        private fun addSolarSystem(systemId: Long, roots: List<EsiCorporationAssetDto>) {
            val system = systems[systemId]
            val key = CorporationAssetLocationKey(CorporationAssetLocationKind.SOLAR_SYSTEM, systemId)
            val icon = starTypeIds[systemId]?.let { types[it]?.iconFilename }
            addPlace(
                identity = regionIdentity(system),
                place = CorporationAssetPlaceRow(
                    key = key,
                    iconFilename = icon,
                    securityStatus = system?.securityStatus,
                    zhName = system?.systemZhName,
                    enName = system?.systemEnName,
                    name = system?.systemName,
                    itemCount = roots.size,
                ),
            )
            locations[key] = CorporationAssetLocationView(
                key = key,
                iconFilename = icon,
                securityStatus = system?.securityStatus,
                zhName = system?.systemZhName,
                enName = system?.systemEnName,
                name = system?.systemName,
                typeCount = distinctTypeCount(tree.forest(roots)),
                itemCount = roots.size,
                showsItems = true,
                stationChildren = emptyList(),
                items = roots.map { toItemRow(it) },
            )
        }

        private fun addUnknown(locationId: Long, roots: List<EsiCorporationAssetDto>) {
            val key = CorporationAssetLocationKey(CorporationAssetLocationKind.UNKNOWN, locationId)
            addPlace(
                identity = RegionIdentity(CorporationAssetRegionKind.UNKNOWN, null, null, null),
                place = CorporationAssetPlaceRow(
                    key = key,
                    iconFilename = null,
                    securityStatus = null,
                    zhName = null,
                    enName = null,
                    name = null,
                    itemCount = roots.size,
                ),
            )
            locations[key] = itemLocation(
                key = key,
                forest = tree.forest(roots),
                roots = roots,
            )
        }

        private fun itemLocation(
            key: CorporationAssetLocationKey,
            forest: List<EsiCorporationAssetDto>,
            roots: List<EsiCorporationAssetDto>,
        ) = CorporationAssetLocationView(
            key = key,
            iconFilename = null,
            securityStatus = null,
            zhName = null,
            enName = null,
            name = null,
            typeCount = distinctTypeCount(forest),
            itemCount = roots.size,
            showsItems = true,
            stationChildren = emptyList(),
            items = roots.map { toItemRow(it) },
        )

        /**
         * Station/structure location page lists categories only (offices, deliveries, …).
         * Each category opens a container page with sectioned contents.
         */
        private fun buildStationCategories(
            key: CorporationAssetLocationKey,
            roots: List<EsiCorporationAssetDto>,
        ): List<CorporationAssetStationChild> {
            val children = ArrayList<CorporationAssetStationChild>()
            val officeRoots = roots.filter {
                it.locationFlag == CorporationAssetFlags.OFFICE_FOLDER
            }
            if (officeRoots.isNotEmpty()) {
                val officeItems = officeRoots.flatMap { office ->
                    tree.childrenByParent[office.itemId].orEmpty()
                }
                val routeFlag = CorporationAssetFlags.OFFICE_FOLDER
                val containerItemId = if (officeRoots.size == 1) {
                    val office = officeRoots.first()
                    containers[office.itemId] = buildContainer(office).copy(
                        categoryTitleRes = CorporationAssetFlags.folderLabelRes(routeFlag),
                    )
                    office.itemId
                } else {
                    putCategoryContainer(
                        locationId = key.locationId,
                        routeFlag = routeFlag,
                        labelRes = CorporationAssetFlags.folderLabelRes(routeFlag),
                        hangarDivision = null,
                        hangarCustomName = null,
                        kids = officeItems,
                    )
                }
                val officeType = officeRoots.firstNotNullOfOrNull { types[it.typeId] }
                children.add(
                    CorporationAssetFlagFolder(
                        routeFlag = routeFlag,
                        labelRes = CorporationAssetFlags.folderLabelRes(routeFlag),
                        hangarDivision = null,
                        hangarCustomName = null,
                        typeCount = distinctTypeCount(tree.forest(officeItems)),
                        iconFilename = officeType?.iconFilename,
                        containerItemId = containerItemId,
                        sortOrder = CorporationAssetFlags.contentSortOrder(routeFlag),
                    ),
                )
            }
            val grouped = roots
                .filter { it.locationFlag != CorporationAssetFlags.OFFICE_FOLDER }
                .groupBy { CorporationAssetFlags.folderRoute(it.locationFlag) }
            grouped.forEach { (routeFlag, items) ->
                val division = CorporationAssetFlags.sagDivision(routeFlag)
                val forest = tree.forest(items)
                val labelRes = CorporationAssetFlags.folderLabelRes(routeFlag)
                val hangarCustomName = division?.let { hangarNames[it] }
                val containerItemId = putCategoryContainer(
                    locationId = key.locationId,
                    routeFlag = routeFlag,
                    labelRes = labelRes,
                    hangarDivision = division,
                    hangarCustomName = hangarCustomName,
                    kids = items,
                )
                children.add(
                    CorporationAssetFlagFolder(
                        routeFlag = routeFlag,
                        labelRes = labelRes,
                        hangarDivision = division,
                        hangarCustomName = hangarCustomName,
                        typeCount = distinctTypeCount(forest),
                        containerItemId = containerItemId,
                        sortOrder = items.minOf {
                            CorporationAssetFlags.contentSortOrder(it.locationFlag)
                        },
                    ),
                )
            }
            return children.sortedWith(
                compareBy<CorporationAssetStationChild> { it.sortOrder }
                    .thenBy { (it as CorporationAssetFlagFolder).routeFlag },
            )
        }

        private fun putCategoryContainer(
            locationId: Long,
            routeFlag: String,
            @StringRes labelRes: Int,
            hangarDivision: Int?,
            hangarCustomName: String?,
            kids: List<EsiCorporationAssetDto>,
        ): Long {
            val containerId = CorporationAssetsConfig.categoryContainerId(locationId, routeFlag)
            containers[containerId] = CorporationAssetContainerView(
                itemId = containerId,
                typeId = 0,
                iconFilename = null,
                zhName = null,
                enName = null,
                name = null,
                customName = null,
                usedVolume = kids.sumOf { stackVolume(it) },
                capacity = null,
                slotSections = emptyList(),
                contentSections = contentSections(kids),
                categoryTitleRes = labelRes,
                hangarDivision = hangarDivision,
                hangarCustomName = hangarCustomName,
                showsCapacityHeader = false,
            )
            return containerId
        }

        private fun buildContainer(asset: EsiCorporationAssetDto): CorporationAssetContainerView {
            val type = types[asset.typeId]
            val kids = tree.childrenByParent[asset.itemId].orEmpty()
            val isOffice = asset.locationFlag == CorporationAssetFlags.OFFICE_FOLDER
            val isStructure = type?.categoryId == CorporationAssetsConfig.CATEGORY_STRUCTURE ||
                type?.categoryId == CorporationAssetsConfig.CATEGORY_STARBASE
            val useSlots = isStructure && !isOffice
            val slotKids = if (useSlots) {
                kids.filter { CorporationAssetFlags.slotGroup(it.locationFlag) != null }
            } else {
                emptyList()
            }
            val slotIds = slotKids.map { it.itemId }.toSet()
            val contentKids = kids.filter { it.itemId !in slotIds }
            val slotSections = slotKids.groupBy { assetInSlot ->
                CorporationAssetFlags.slotGroup(assetInSlot.locationFlag)
            }.mapNotNull { (group, grouped) ->
                if (group == null) return@mapNotNull null
                CorporationAssetSlotSection(
                    group = group,
                    items = grouped.map { toItemRow(it) },
                )
            }.sortedBy { it.group.order }
            return CorporationAssetContainerView(
                itemId = asset.itemId,
                typeId = asset.typeId,
                iconFilename = iconFilename(asset, type),
                zhName = type?.zhName,
                enName = type?.enName,
                name = type?.name,
                customName = customName(asset, type),
                usedVolume = containedVolume(asset, kids),
                capacity = type?.capacity,
                slotSections = slotSections,
                contentSections = contentSections(contentKids),
                officeContentTypeCount = if (isOffice) {
                    distinctTypeCount(tree.descendants(asset.itemId))
                } else {
                    null
                },
            )
        }

        private fun contentSections(
            kids: List<EsiCorporationAssetDto>,
        ): List<CorporationAssetContentSection> {
            if (kids.isEmpty()) return emptyList()
            val grouped = LinkedHashMap<ContentGroupKey, MutableList<EsiCorporationAssetDto>>()
            kids.forEach { asset ->
                val division = CorporationAssetFlags.sagDivision(asset.locationFlag)
                val slot = CorporationAssetFlags.slotGroup(asset.locationFlag)
                val key = when {
                    division != null -> ContentGroupKey(
                        sortOrder = CorporationAssetFlags.contentSortOrder(asset.locationFlag),
                        titleRes = CorporationAssetFlags.contentLabelRes(asset.locationFlag),
                        hangarDivision = division,
                    )
                    slot != null -> ContentGroupKey(
                        sortOrder = CorporationAssetFlags.contentSortOrder(asset.locationFlag),
                        titleRes = slot.labelRes,
                        hangarDivision = null,
                    )
                    else -> {
                        val route = CorporationAssetFlags.folderRoute(asset.locationFlag)
                        ContentGroupKey(
                            sortOrder = CorporationAssetFlags.contentSortOrder(asset.locationFlag),
                            titleRes = CorporationAssetFlags.folderLabelRes(route),
                            hangarDivision = null,
                        )
                    }
                }
                grouped.getOrPut(key) { ArrayList() }.add(asset)
            }
            return grouped.map { (key, groupedAssets) ->
                CorporationAssetContentSection(
                    titleRes = key.titleRes,
                    hangarDivision = key.hangarDivision,
                    hangarCustomName = key.hangarDivision?.let { hangarNames[it] },
                    sortOrder = key.sortOrder,
                    items = groupedAssets.map { toItemRow(it) },
                )
            }.sortedBy { it.sortOrder }
        }

        private fun toItemRow(asset: EsiCorporationAssetDto): CorporationAssetItemRow {
            val type = types[asset.typeId]
            val isOffice = asset.locationFlag == CorporationAssetFlags.OFFICE_FOLDER
            return CorporationAssetItemRow(
                itemId = asset.itemId,
                typeId = asset.typeId,
                iconFilename = iconFilename(asset, type),
                zhName = type?.zhName,
                enName = type?.enName,
                name = type?.name,
                customName = customName(asset, type),
                quantity = asset.quantity.toLong().coerceAtLeast(0L),
                isSingleton = asset.isSingleton,
                isContainer = isContainer(asset),
                isOffice = isOffice,
                contentTypeCount = distinctTypeCount(tree.descendants(asset.itemId)),
            )
        }

        private fun isContainer(asset: EsiCorporationAssetDto): Boolean {
            if (asset.locationFlag == CorporationAssetFlags.OFFICE_FOLDER) return true
            if (tree.childrenByParent.containsKey(asset.itemId)) return true
            if (!asset.isSingleton) return false
            val type = types[asset.typeId] ?: return false
            if (type.categoryId == CorporationAssetsConfig.CATEGORY_STRUCTURE ||
                type.categoryId == CorporationAssetsConfig.CATEGORY_STARBASE
            ) {
                return true
            }
            val capacity = type.capacity
            return capacity != null && capacity > CorporationAssetsConfig.UNLIMITED_CAPACITY_MAX
        }

        private fun iconFilename(asset: EsiCorporationAssetDto, type: TypeAssetRow?): String? =
            if (asset.isBlueprintCopy) {
                type?.bpcIconFilename ?: type?.iconFilename
            } else {
                type?.iconFilename
            }

        private fun customName(asset: EsiCorporationAssetDto, type: TypeAssetRow?): String? {
            val raw = customNames[asset.itemId]?.takeIf { it.isNotBlank() } ?: return null
            if (raw == type?.name || raw == type?.zhName || raw == type?.enName) return null
            return raw
        }

        /**
         * One level of direct children. Ships count only Cargo.
         * Every other container counts every child, including fitted modules and the office item.
         */
        private fun containedVolume(
            parent: EsiCorporationAssetDto,
            children: List<EsiCorporationAssetDto>,
        ): Double {
            val cargoOnly = types[parent.typeId]?.categoryId == CorporationAssetsConfig.CATEGORY_SHIP
            return children.sumOf { child ->
                if (cargoOnly && child.locationFlag != CorporationAssetFlags.CARGO) {
                    0.0
                } else {
                    stackVolume(child)
                }
            }
        }

        private fun stackVolume(asset: EsiCorporationAssetDto): Double {
            val type = types[asset.typeId] ?: return 0.0
            val unit = if (asset.isSingleton) {
                type.volume
            } else {
                type.repackagedVolume ?: type.volume
            } ?: return 0.0
            return unit * asset.quantity.coerceAtLeast(0)
        }

        private fun distinctTypeCount(assets: List<EsiCorporationAssetDto>): Int =
            assets.map { it.typeId }.toSet().size

        private fun addPlace(identity: RegionIdentity, place: CorporationAssetPlaceRow) {
            regionPlaces.getOrPut(identity) { ArrayList() }.add(place)
        }

        private fun regionIdentity(system: ResolvedSystem?): RegionIdentity {
            if (system == null || !system.hasRegionName()) {
                return RegionIdentity(CorporationAssetRegionKind.UNKNOWN, null, null, null)
            }
            return RegionIdentity(
                kind = CorporationAssetRegionKind.REGION,
                zhName = system.regionZhName?.takeIf { it.isNotBlank() },
                enName = system.regionEnName?.takeIf { it.isNotBlank() },
                name = system.regionName?.takeIf { it.isNotBlank() },
            )
        }
    }

    private data class SafetyAnchor(
        val stationId: Long?,
        val systemId: Long?,
    )

    private data class SafetyBuckets(
        val byStation: Map<Long, List<EsiCorporationAssetDto>>,
        val bySystem: Map<Long, List<EsiCorporationAssetDto>>,
        val unresolved: List<EsiCorporationAssetDto>,
    )

    private data class ResolvedDockable(
        val name: String?,
        val typeId: Int?,
        val solarSystemId: Long?,
    )

    private data class ResolvedSystem(
        val systemZhName: String?,
        val systemEnName: String?,
        val systemName: String?,
        val regionZhName: String?,
        val regionEnName: String?,
        val regionName: String?,
        val securityStatus: Double?,
    ) {
        fun hasSystemName(): Boolean =
            !systemZhName.isNullOrBlank() || !systemEnName.isNullOrBlank() || !systemName.isNullOrBlank()

        fun hasRegionName(): Boolean =
            !regionZhName.isNullOrBlank() || !regionEnName.isNullOrBlank() || !regionName.isNullOrBlank()
    }

    private data class RegionIdentity(
        val kind: CorporationAssetRegionKind,
        val zhName: String?,
        val enName: String?,
        val name: String?,
    )

    private data class ContentGroupKey(
        val sortOrder: Int,
        val titleRes: Int,
        val hangarDivision: Int?,
    )
}
