package com.marshall.pyerite.personalAssetsModule.data

import androidx.annotation.StringRes
import com.marshall.pyerite.esiModule.api.EsiCharacterApi
import com.marshall.pyerite.esiModule.api.EsiUniverseApi
import com.marshall.pyerite.esiModule.data.CharacterAssetListCache
import com.marshall.pyerite.esiModule.data.CharacterAssetListResult
import com.marshall.pyerite.esiModule.data.EsiPublicDataSource
import com.marshall.pyerite.esiModule.model.EsiCharacterAssetDto
import com.marshall.pyerite.esiModule.model.EsiCorporationAssetLocationDto
import com.marshall.pyerite.esiModule.model.EsiCorporationAssetNameDto
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetContainerView
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetContentSection
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetFlagFolder
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetFlags
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetItemRow
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetLocationKey
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetLocationKind
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetLocationView
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetPlaceRow
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetRegionKind
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetRegionSection
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetSlotSection
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetStationChild
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsAccessException
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsConfig
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsSnapshot
import com.marshall.pyerite.sdeModule.room.RoomProvider
import com.marshall.pyerite.sdeModule.room.map.SolarSystemPositionRow
import com.marshall.pyerite.sdeModule.room.type.TypeAssetRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.ArrayDeque
import kotlin.math.pow
import kotlin.math.sqrt

internal class PersonalAssetsLoader(
    private val tokenManager: EveTokenManager,
    private val characterApi: EsiCharacterApi,
    private val universeApi: EsiUniverseApi,
    private val publicEsi: EsiPublicDataSource,
    private val roomProvider: RoomProvider,
    private val assetListCache: CharacterAssetListCache,
) {
    suspend fun load(characterId: Long, forceRefresh: Boolean): PersonalAssetsSnapshot =
        withContext(Dispatchers.IO) {
            val assets = when (val listed = assetListCache.load(characterId, forceRefresh)) {
                is CharacterAssetListResult.Ready -> listed.assets
                CharacterAssetListResult.Forbidden -> throw PersonalAssetsAccessException()
                CharacterAssetListResult.Failed -> error("Character assets request failed")
            }
            if (assets.isEmpty()) return@withContext PersonalAssetsSnapshot.EMPTY
            assemble(characterId, assets)
        }

    private suspend fun assemble(
        characterId: Long,
        assets: List<EsiCharacterAssetDto>,
    ): PersonalAssetsSnapshot {
        val tree = AssetTree(assets)
        val safetyDockableIds = tree.safetyRoots.map { it.locationId }.filter { locationId ->
            locationId != PersonalAssetsConfig.ASSET_SAFETY_LOCATION_ID && !isSolarSystemId(locationId)
        }
        val dockableIds = (
            tree.stationRoots.keys +
                tree.unknownRoots.keys.filterNot { isSolarSystemId(it) } +
                safetyDockableIds
            ).distinct()
        val dockables = loadDockables(characterId, dockableIds)
        val safetyBuckets = classifySafety(
            roots = tree.safetyRoots,
            dockables = dockables,
            coordinateSystemByItem = resolveSafetySystems(
                characterId = characterId,
                roots = tree.safetyRoots.filter { root -> safetyAnchor(root, dockables) == null },
            ),
        )
        val spaceSystemIds = (tree.systemRoots.keys + safetyBuckets.bySystem.keys).distinct()
        val systemIds = (spaceSystemIds + dockables.values.mapNotNull { it.solarSystemId }).distinct()
        val systems = loadSystems(systemIds)
        val starTypeIds = loadStarTypeIds(spaceSystemIds)
        val types = loadTypes(
            assetTypeIds = assets.map { it.typeId },
            stationTypeIds = dockables.values.mapNotNull { it.typeId },
            starTypeIds = starTypeIds.values,
        )
        val assetNames = loadAssetNames(
            characterId = characterId,
            tree = tree,
            types = types,
            itemIds = nameCandidateIds(tree, types),
        )
        return Assembler(
            ownerCharacterId = characterId,
            tree = tree,
            types = types,
            customNames = assetNames,
            dockables = dockables,
            systems = systems,
            starTypeIds = starTypeIds,
            safetyByStation = safetyBuckets.byStation,
            safetyBySystem = safetyBuckets.bySystem,
            safetyUnresolved = safetyBuckets.unresolved,
        ).assemble()
    }

    private suspend fun loadDockables(
        characterId: Long,
        locationIds: List<Long>,
    ): Map<Long, ResolvedDockable> {
        if (locationIds.isEmpty()) return emptyMap()
        val resolved = LinkedHashMap<Long, ResolvedDockable>()
        val dao = roomProvider.getDatabase().mapDao()
        val npcIds = locationIds.filter { isNpcStationId(it) }
        npcIds.chunked(PersonalAssetsConfig.QUERY_CHUNK).flatMap { chunk ->
            runCatching { dao.getStations(chunk) }.getOrDefault(emptyList())
        }.forEach { row ->
            resolved[row.stationId] = ResolvedDockable(
                name = row.name?.takeIf { it.isNotBlank() },
                typeId = row.typeId,
                solarSystemId = row.solarSystemId?.toLong(),
            )
        }
        locationIds.filter { it !in resolved }.forEach { locationId ->
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
        val fromSde = systemIds.chunked(PersonalAssetsConfig.QUERY_CHUNK).flatMap { chunk ->
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
        systemIds.chunked(PersonalAssetsConfig.STAR_LOOKUP_CONCURRENCY).forEach { chunk ->
            val pairs = coroutineScope {
                chunk.map { systemId ->
                    async {
                        val typeId = publicEsi.fetchSolarSystemStarTypeId(systemId)
                            ?: PersonalAssetsConfig.FALLBACK_SUN_TYPE_ID
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
        return ids.chunked(PersonalAssetsConfig.QUERY_CHUNK).flatMap { chunk ->
            runCatching { dao.getTypesForAssets(chunk) }.getOrDefault(emptyList())
        }.associateBy { it.id }
    }

    private fun safetyAnchor(
        root: EsiCharacterAssetDto,
        dockables: Map<Long, ResolvedDockable>,
    ): SafetyAnchor? {
        val locationId = root.locationId
        if (isSolarSystemId(locationId)) {
            return SafetyAnchor(stationId = null, systemId = locationId)
        }
        if (locationId != PersonalAssetsConfig.ASSET_SAFETY_LOCATION_ID) {
            val dockable = dockables[locationId]
            if (dockable?.solarSystemId != null) {
                return SafetyAnchor(stationId = locationId, systemId = dockable.solarSystemId)
            }
        }
        return null
    }

    private fun classifySafety(
        roots: List<EsiCharacterAssetDto>,
        dockables: Map<Long, ResolvedDockable>,
        coordinateSystemByItem: Map<Long, Long>,
    ): SafetyBuckets {
        val byStation = LinkedHashMap<Long, MutableList<EsiCharacterAssetDto>>()
        val bySystem = LinkedHashMap<Long, MutableList<EsiCharacterAssetDto>>()
        val unresolved = ArrayList<EsiCharacterAssetDto>()
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
        return SafetyBuckets(byStation, bySystem, unresolved)
    }

    private suspend fun resolveSafetySystems(
        characterId: Long,
        roots: List<EsiCharacterAssetDto>,
    ): Map<Long, Long> {
        if (roots.isEmpty()) return emptyMap()
        val locations = loadAssetLocations(characterId, roots.map { it.itemId })
        if (locations.isEmpty()) return emptyMap()
        val positions = runCatching {
            roomProvider.getDatabase().mapDao().getSolarSystemPositions()
        }.getOrDefault(emptyList())
        if (positions.isEmpty()) return emptyMap()
        return locations.mapNotNull { location ->
            val position = location.position
            if (position.x == 0.0 && position.y == 0.0 && position.z == 0.0) return@mapNotNull null
            val systemId = nearestSolarSystemId(position.x, position.y, position.z, positions)
                ?: return@mapNotNull null
            location.itemId to systemId
        }.toMap()
    }

    private suspend fun loadAssetLocations(
        characterId: Long,
        itemIds: List<Long>,
    ): List<EsiCorporationAssetLocationDto> {
        if (itemIds.isEmpty()) return emptyList()
        val out = ArrayList<EsiCorporationAssetLocationDto>()
        itemIds.distinct().chunked(PersonalAssetsConfig.ASSET_NAMES_CHUNK).forEach { chunk ->
            val fetched = runCatching {
                tokenManager.executeWithAuthRetry(characterId) { auth ->
                    characterApi.fetchAssetLocations(characterId, auth, chunk)
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
        locationId in PersonalAssetsConfig.NPC_STATION_ID_MIN..PersonalAssetsConfig.NPC_STATION_ID_MAX

    private fun isSolarSystemId(locationId: Long): Boolean =
        locationId in PersonalAssetsConfig.SOLAR_SYSTEM_ID_MIN..PersonalAssetsConfig.SOLAR_SYSTEM_ID_MAX

    private fun nameCandidateIds(tree: AssetTree, types: Map<Int, TypeAssetRow>): List<Long> =
        tree.assets.mapNotNull { asset ->
            if (!asset.isSingleton) return@mapNotNull null
            val type = types[asset.typeId]
            val namedCategory = type?.categoryId == PersonalAssetsConfig.CATEGORY_SHIP ||
                type?.categoryId == PersonalAssetsConfig.CATEGORY_STRUCTURE ||
                type?.categoryId == PersonalAssetsConfig.CATEGORY_STARBASE
            val worthNaming = asset.locationType == PersonalAssetFlags.LOCATION_SOLAR_SYSTEM ||
                tree.childrenByParent.containsKey(asset.itemId) ||
                namedCategory
            if (worthNaming) asset.itemId else null
        }

    private suspend fun loadAssetNames(
        characterId: Long,
        tree: AssetTree,
        types: Map<Int, TypeAssetRow>,
        itemIds: List<Long>,
    ): Map<Long, String> {
        val names = LinkedHashMap<Long, String>()
        itemIds.distinct().chunked(PersonalAssetsConfig.ASSET_NAMES_CHUNK).forEach { chunk ->
            names.putAll(collectNames(characterId, chunk))
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
        asset: EsiCharacterAssetDto?,
        types: Map<Int, TypeAssetRow>,
    ): Boolean {
        val categoryId = asset?.let { types[it.typeId]?.categoryId } ?: return false
        return categoryId == PersonalAssetsConfig.CATEGORY_STRUCTURE ||
            categoryId == PersonalAssetsConfig.CATEGORY_STARBASE
    }

    /**
     * ESI rejects the whole names request when any id cannot be named.
     * Split until a single id so one failure does not drop the rest.
     */
    private suspend fun collectNames(
        characterId: Long,
        itemIds: List<Long>,
    ): Map<Long, String> {
        if (itemIds.isEmpty()) return emptyMap()
        val fetched = runCatching {
            tokenManager.executeWithAuthRetry(characterId) { auth ->
                characterApi.fetchAssetNames(characterId, auth, itemIds)
            }
        }.getOrNull()
        if (fetched != null) return fetched.toNameMap()
        if (itemIds.size == 1) return emptyMap()
        val mid = itemIds.size / 2
        val merged = LinkedHashMap<Long, String>()
        merged.putAll(collectNames(characterId, itemIds.subList(0, mid)))
        merged.putAll(collectNames(characterId, itemIds.subList(mid, itemIds.size)))
        return merged
    }

    private fun List<EsiCorporationAssetNameDto>.toNameMap(): Map<Long, String> =
        mapNotNull { dto ->
            val name = dto.name.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            dto.itemId to name
        }.toMap()

    private class AssetTree(val assets: List<EsiCharacterAssetDto>) {
        val byId: Map<Long, EsiCharacterAssetDto> = assets.associateBy { it.itemId }
        val childrenByParent: Map<Long, List<EsiCharacterAssetDto>>
        val stationRoots: Map<Long, List<EsiCharacterAssetDto>>
        val systemRoots: Map<Long, List<EsiCharacterAssetDto>>
        val safetyRoots: List<EsiCharacterAssetDto>
        val unknownRoots: Map<Long, List<EsiCharacterAssetDto>>

        init {
            val childBuckets = LinkedHashMap<Long, MutableList<EsiCharacterAssetDto>>()
            val rootBucket = ArrayList<EsiCharacterAssetDto>()
            assets.forEach { asset ->
                val parent = byId[asset.locationId]
                if (parent != null && parent.itemId != asset.itemId) {
                    childBuckets.getOrPut(asset.locationId) { ArrayList() }.add(asset)
                } else {
                    rootBucket.add(asset)
                }
            }
            childrenByParent = childBuckets
            val stations = LinkedHashMap<Long, MutableList<EsiCharacterAssetDto>>()
            val systems = LinkedHashMap<Long, MutableList<EsiCharacterAssetDto>>()
            val safety = ArrayList<EsiCharacterAssetDto>()
            val unknown = LinkedHashMap<Long, MutableList<EsiCharacterAssetDto>>()
            rootBucket.forEach { asset ->
                when {
                    asset.locationId == PersonalAssetsConfig.ASSET_SAFETY_LOCATION_ID ||
                        asset.locationFlag == PersonalAssetFlags.ASSET_SAFETY -> safety.add(asset)
                    asset.locationType == PersonalAssetFlags.LOCATION_STATION ->
                        stations.getOrPut(asset.locationId) { ArrayList() }.add(asset)
                    asset.locationType == PersonalAssetFlags.LOCATION_SOLAR_SYSTEM ->
                        systems.getOrPut(asset.locationId) { ArrayList() }.add(asset)
                    else -> unknown.getOrPut(asset.locationId) { ArrayList() }.add(asset)
                }
            }
            stationRoots = stations
            systemRoots = systems
            safetyRoots = safety
            unknownRoots = unknown
        }

        fun descendants(rootId: Long): List<EsiCharacterAssetDto> {
            val out = ArrayList<EsiCharacterAssetDto>()
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

        fun forest(roots: List<EsiCharacterAssetDto>): List<EsiCharacterAssetDto> {
            val out = ArrayList<EsiCharacterAssetDto>(roots.size)
            out.addAll(roots)
            roots.forEach { root -> out.addAll(descendants(root.itemId)) }
            return out
        }
    }

    private class Assembler(
        private val ownerCharacterId: Long,
        private val tree: AssetTree,
        private val types: Map<Int, TypeAssetRow>,
        private val customNames: Map<Long, String>,
        private val dockables: Map<Long, ResolvedDockable>,
        private val systems: Map<Long, ResolvedSystem>,
        private val starTypeIds: Map<Long, Int>,
        private val safetyByStation: Map<Long, List<EsiCharacterAssetDto>>,
        private val safetyBySystem: Map<Long, List<EsiCharacterAssetDto>>,
        private val safetyUnresolved: List<EsiCharacterAssetDto>,
    ) {
        private val locations = LinkedHashMap<PersonalAssetLocationKey, PersonalAssetLocationView>()
        private val containers = LinkedHashMap<Long, PersonalAssetContainerView>()
        private val regionPlaces = LinkedHashMap<RegionIdentity, MutableList<PersonalAssetPlaceRow>>()

        fun assemble(): PersonalAssetsSnapshot {
            val dockableUnknownIds = HashSet<Long>()
            val stationIds = (tree.stationRoots.keys + safetyByStation.keys).toSet()
            stationIds.forEach { stationId ->
                val roots = tree.stationRoots[stationId].orEmpty() + safetyByStation[stationId].orEmpty()
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
                val roots = tree.systemRoots[systemId].orEmpty() + safetyBySystem[systemId].orEmpty()
                if (roots.isNotEmpty()) addSolarSystem(systemId, roots)
            }
            tree.unknownRoots.forEach { (locationId, roots) ->
                if (locationId !in dockableUnknownIds && locationId !in stationIds) {
                    addUnknown(locationId, roots)
                }
            }
            if (safetyUnresolved.isNotEmpty()) {
                addUnknown(PersonalAssetsConfig.ASSET_SAFETY_LOCATION_ID, safetyUnresolved)
            }
            tree.assets.forEach { asset ->
                if (isContainer(asset) && asset.itemId !in containers) {
                    containers[asset.itemId] = buildContainer(asset)
                }
            }
            return PersonalAssetsSnapshot(
                regions = regionPlaces.map { (identity, places) ->
                    PersonalAssetRegionSection(
                        kind = identity.kind,
                        zhName = identity.zhName,
                        enName = identity.enName,
                        name = identity.name,
                        places = places,
                    )
                },
                locations = locations,
                containers = containers,
            )
        }

        private fun addDockable(locationId: Long, roots: List<EsiCharacterAssetDto>) {
            val dockable = dockables[locationId]
            val system = dockable?.solarSystemId?.let { systems[it] }
            val type = dockable?.typeId?.let { types[it] }
            val key = locationKey(PersonalAssetLocationKind.STATION, locationId)
            val icon = type?.iconFilename
            addPlace(
                identity = regionIdentity(system),
                place = PersonalAssetPlaceRow(
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
            locations[key] = PersonalAssetLocationView(
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

        private fun addSolarSystem(systemId: Long, roots: List<EsiCharacterAssetDto>) {
            val system = systems[systemId]
            val key = locationKey(PersonalAssetLocationKind.SOLAR_SYSTEM, systemId)
            val icon = starTypeIds[systemId]?.let { types[it]?.iconFilename }
            addPlace(
                identity = regionIdentity(system),
                place = PersonalAssetPlaceRow(
                    key = key,
                    iconFilename = icon,
                    securityStatus = system?.securityStatus,
                    zhName = system?.systemZhName,
                    enName = system?.systemEnName,
                    name = system?.systemName,
                    itemCount = roots.size,
                ),
            )
            locations[key] = PersonalAssetLocationView(
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

        private fun addUnknown(locationId: Long, roots: List<EsiCharacterAssetDto>) {
            val key = locationKey(PersonalAssetLocationKind.UNKNOWN, locationId)
            addPlace(
                identity = RegionIdentity(PersonalAssetRegionKind.UNKNOWN, null, null, null),
                place = PersonalAssetPlaceRow(
                    key = key,
                    iconFilename = null,
                    securityStatus = null,
                    zhName = null,
                    enName = null,
                    name = null,
                    itemCount = roots.size,
                ),
            )
            locations[key] = PersonalAssetLocationView(
                key = key,
                iconFilename = null,
                securityStatus = null,
                zhName = null,
                enName = null,
                name = null,
                typeCount = distinctTypeCount(tree.forest(roots)),
                itemCount = roots.size,
                showsItems = true,
                stationChildren = emptyList(),
                items = roots.map { toItemRow(it) },
            )
        }

        private fun buildStationCategories(
            key: PersonalAssetLocationKey,
            roots: List<EsiCharacterAssetDto>,
        ): List<PersonalAssetStationChild> {
            val grouped = roots.groupBy { PersonalAssetFlags.folderRoute(it.locationFlag) }
            return grouped.map { (routeFlag, items) ->
                val forest = tree.forest(items)
                val labelRes = PersonalAssetFlags.folderLabelRes(routeFlag)
                val division = PersonalAssetFlags.sagDivision(routeFlag)
                val containerItemId = putCategoryContainer(
                    locationId = key.locationId,
                    routeFlag = routeFlag,
                    labelRes = labelRes,
                    hangarDivision = division,
                    kids = items,
                )
                PersonalAssetFlagFolder(
                    routeFlag = routeFlag,
                    labelRes = labelRes,
                    hangarDivision = division,
                    hangarCustomName = null,
                    typeCount = distinctTypeCount(forest),
                    containerItemId = containerItemId,
                    sortOrder = items.minOf { PersonalAssetFlags.contentSortOrder(it.locationFlag) },
                )
            }.sortedWith(
                compareBy<PersonalAssetFlagFolder> { it.sortOrder }.thenBy { it.routeFlag },
            )
        }

        private fun putCategoryContainer(
            locationId: Long,
            routeFlag: String,
            @StringRes labelRes: Int,
            hangarDivision: Int?,
            kids: List<EsiCharacterAssetDto>,
        ): Long {
            val containerId = PersonalAssetsConfig.categoryContainerId(
                ownerCharacterId = ownerCharacterId,
                locationId = locationId,
                routeFlag = routeFlag,
            )
            containers[containerId] = PersonalAssetContainerView(
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
                showsCapacityHeader = false,
            )
            return containerId
        }

        private fun buildContainer(asset: EsiCharacterAssetDto): PersonalAssetContainerView {
            val type = types[asset.typeId]
            val kids = tree.childrenByParent[asset.itemId].orEmpty()
            val isStructure = type?.categoryId == PersonalAssetsConfig.CATEGORY_STRUCTURE ||
                type?.categoryId == PersonalAssetsConfig.CATEGORY_STARBASE
            val slotKids = if (isStructure) {
                kids.filter { PersonalAssetFlags.slotGroup(it.locationFlag) != null }
            } else {
                emptyList()
            }
            val slotIds = slotKids.map { it.itemId }.toSet()
            val contentKids = kids.filter { it.itemId !in slotIds }
            val slotSections = slotKids.groupBy { child ->
                PersonalAssetFlags.slotGroup(child.locationFlag)
            }.mapNotNull { (group, grouped) ->
                if (group == null) return@mapNotNull null
                PersonalAssetSlotSection(group = group, items = grouped.map { toItemRow(it) })
            }.sortedBy { it.group.order }
            return PersonalAssetContainerView(
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
            )
        }

        private fun contentSections(
            kids: List<EsiCharacterAssetDto>,
        ): List<PersonalAssetContentSection> {
            if (kids.isEmpty()) return emptyList()
            val grouped = LinkedHashMap<ContentGroupKey, MutableList<EsiCharacterAssetDto>>()
            kids.forEach { asset ->
                val division = PersonalAssetFlags.sagDivision(asset.locationFlag)
                val slot = PersonalAssetFlags.slotGroup(asset.locationFlag)
                val key = when {
                    division != null -> ContentGroupKey(
                        sortOrder = PersonalAssetFlags.contentSortOrder(asset.locationFlag),
                        titleRes = PersonalAssetFlags.contentLabelRes(asset.locationFlag),
                        hangarDivision = division,
                    )
                    slot != null -> ContentGroupKey(
                        sortOrder = PersonalAssetFlags.contentSortOrder(asset.locationFlag),
                        titleRes = slot.labelRes,
                        hangarDivision = null,
                    )
                    else -> {
                        val route = PersonalAssetFlags.folderRoute(asset.locationFlag)
                        ContentGroupKey(
                            sortOrder = PersonalAssetFlags.contentSortOrder(asset.locationFlag),
                            titleRes = PersonalAssetFlags.folderLabelRes(route),
                            hangarDivision = null,
                        )
                    }
                }
                grouped.getOrPut(key) { ArrayList() }.add(asset)
            }
            return grouped.map { (key, groupedAssets) ->
                PersonalAssetContentSection(
                    titleRes = key.titleRes,
                    hangarDivision = key.hangarDivision,
                    hangarCustomName = null,
                    sortOrder = key.sortOrder,
                    items = groupedAssets.map { toItemRow(it) },
                )
            }.sortedBy { it.sortOrder }
        }

        private fun toItemRow(asset: EsiCharacterAssetDto): PersonalAssetItemRow {
            val type = types[asset.typeId]
            return PersonalAssetItemRow(
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
                contentTypeCount = distinctTypeCount(tree.descendants(asset.itemId)),
            )
        }

        private fun isContainer(asset: EsiCharacterAssetDto): Boolean {
            if (tree.childrenByParent.containsKey(asset.itemId)) return true
            if (!asset.isSingleton) return false
            val type = types[asset.typeId] ?: return false
            if (type.categoryId == PersonalAssetsConfig.CATEGORY_STRUCTURE ||
                type.categoryId == PersonalAssetsConfig.CATEGORY_STARBASE
            ) {
                return true
            }
            val capacity = type.capacity
            return capacity != null && capacity > PersonalAssetsConfig.UNLIMITED_CAPACITY_MAX
        }

        private fun iconFilename(asset: EsiCharacterAssetDto, type: TypeAssetRow?): String? =
            if (asset.isBlueprintCopy) type?.bpcIconFilename ?: type?.iconFilename else type?.iconFilename

        private fun customName(asset: EsiCharacterAssetDto, type: TypeAssetRow?): String? {
            val raw = customNames[asset.itemId]?.takeIf { it.isNotBlank() } ?: return null
            if (raw == type?.name || raw == type?.zhName || raw == type?.enName) return null
            return raw
        }

        /**
         * One level of direct children. Ships count only Cargo.
         * Every other container counts every child, including fitted modules.
         */
        private fun containedVolume(
            parent: EsiCharacterAssetDto,
            children: List<EsiCharacterAssetDto>,
        ): Double {
            val cargoOnly = types[parent.typeId]?.categoryId == PersonalAssetsConfig.CATEGORY_SHIP
            return children.sumOf { child ->
                if (cargoOnly && child.locationFlag != PersonalAssetFlags.CARGO) 0.0 else stackVolume(child)
            }
        }

        private fun stackVolume(asset: EsiCharacterAssetDto): Double {
            val type = types[asset.typeId] ?: return 0.0
            val unit = if (asset.isSingleton) type.volume else type.repackagedVolume ?: type.volume
            return (unit ?: return 0.0) * asset.quantity.coerceAtLeast(0)
        }

        private fun distinctTypeCount(assets: List<EsiCharacterAssetDto>): Int =
            assets.map { it.typeId }.toSet().size

        private fun addPlace(identity: RegionIdentity, place: PersonalAssetPlaceRow) {
            regionPlaces.getOrPut(identity) { ArrayList() }.add(place)
        }

        private fun regionIdentity(system: ResolvedSystem?): RegionIdentity {
            if (system == null || !system.hasRegionName()) {
                return RegionIdentity(PersonalAssetRegionKind.UNKNOWN, null, null, null)
            }
            return RegionIdentity(
                kind = PersonalAssetRegionKind.REGION,
                zhName = system.regionZhName?.takeIf { it.isNotBlank() },
                enName = system.regionEnName?.takeIf { it.isNotBlank() },
                name = system.regionName?.takeIf { it.isNotBlank() },
            )
        }

        private fun locationKey(kind: PersonalAssetLocationKind, locationId: Long) =
            PersonalAssetLocationKey(
                ownerCharacterId = ownerCharacterId,
                kind = kind,
                locationId = locationId,
            )
    }

    private data class SafetyAnchor(val stationId: Long?, val systemId: Long?)

    private data class SafetyBuckets(
        val byStation: Map<Long, List<EsiCharacterAssetDto>>,
        val bySystem: Map<Long, List<EsiCharacterAssetDto>>,
        val unresolved: List<EsiCharacterAssetDto>,
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
        val kind: PersonalAssetRegionKind,
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
