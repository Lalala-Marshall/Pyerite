package com.marshall.pyerite.personalAssetsModule.model

/**
 * Joins per-character snapshots.
 * Merge shares one place row and keeps each character's location view.
 */
internal fun List<PersonalAssetsSnapshot>.combine(mergeSameLocation: Boolean): PersonalAssetsSnapshot {
    if (isEmpty()) return PersonalAssetsSnapshot.EMPTY
    if (size == 1 && !mergeSameLocation) return first()
    return if (mergeSameLocation) mergeLocations() else concatenate()
}

private fun List<PersonalAssetsSnapshot>.concatenate(): PersonalAssetsSnapshot {
    val locations = LinkedHashMap<PersonalAssetLocationKey, PersonalAssetLocationView>()
    val containers = LinkedHashMap<Long, PersonalAssetContainerView>()
    forEach { snapshot ->
        locations.putAll(snapshot.locations)
        containers.putAll(snapshot.containers)
    }
    return PersonalAssetsSnapshot(
        regions = mergeRegionSections(flatMap { it.regions }),
        locations = locations,
        containers = containers,
    )
}

private fun List<PersonalAssetsSnapshot>.mergeLocations(): PersonalAssetsSnapshot {
    val locations = LinkedHashMap<PersonalAssetLocationKey, PersonalAssetLocationView>()
    val containers = LinkedHashMap<Long, PersonalAssetContainerView>()
    forEach { snapshot ->
        locations.putAll(snapshot.locations)
        containers.putAll(snapshot.containers)
    }
    val grouped = LinkedHashMap<PlaceIdentity, MutableList<PersonalAssetLocationView>>()
    locations.values.forEach { location ->
        val identity = PlaceIdentity(location.key.kind, location.key.locationId)
        grouped.getOrPut(identity) { ArrayList() }.add(location)
    }
    val mergedPlaces = ArrayList<PersonalAssetPlaceRow>()
    grouped.forEach { (identity, views) ->
        val first = views.first()
        val owners = views.mapNotNull { view ->
            val ownerId = view.key.ownerCharacterId ?: return@mapNotNull null
            PersonalAssetOwnerRef(characterId = ownerId, name = view.ownerName.orEmpty())
        }.distinctBy { it.characterId }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        mergedPlaces += PersonalAssetPlaceRow(
            key = PersonalAssetLocationKey(
                ownerCharacterId = null,
                kind = identity.kind,
                locationId = identity.locationId,
            ),
            iconFilename = first.iconFilename,
            securityStatus = first.securityStatus,
            typeZhName = first.typeZhName,
            typeEnName = first.typeEnName,
            typeName = first.typeName,
            zhName = first.zhName,
            enName = first.enName,
            name = first.name,
            itemCount = views.sumOf { it.itemCount },
            owners = owners,
        )
    }
    val regions = mergeRegionSections(
        mergedPlaces.groupBy { place ->
            val region = sourceRegion(place, this)
            RegionIdentity(
                kind = region?.kind ?: PersonalAssetRegionKind.UNKNOWN,
                zhName = region?.zhName,
                enName = region?.enName,
                name = region?.name,
            )
        }.map { (identity, places) ->
            PersonalAssetRegionSection(
                kind = identity.kind,
                zhName = identity.zhName,
                enName = identity.enName,
                name = identity.name,
                places = places,
            )
        },
    )
    return PersonalAssetsSnapshot(
        regions = regions,
        locations = locations,
        containers = containers,
    )
}

private fun mergeRegionSections(
    regions: List<PersonalAssetRegionSection>,
): List<PersonalAssetRegionSection> {
    val grouped = LinkedHashMap<RegionIdentity, MutableList<PersonalAssetPlaceRow>>()
    regions.forEach { region ->
        val identity = RegionIdentity(region.kind, region.zhName, region.enName, region.name)
        grouped.getOrPut(identity) { ArrayList() }.addAll(region.places)
    }
    return grouped.map { (identity, places) ->
        PersonalAssetRegionSection(
            kind = identity.kind,
            zhName = identity.zhName,
            enName = identity.enName,
            name = identity.name,
            places = places,
        )
    }
}

private fun sourceRegion(
    place: PersonalAssetPlaceRow,
    snapshots: List<PersonalAssetsSnapshot>,
): PersonalAssetRegionSection? = snapshots.firstNotNullOfOrNull { snapshot ->
    snapshot.regions.firstOrNull { region ->
        region.places.any { candidate ->
            candidate.key.kind == place.key.kind &&
                candidate.key.locationId == place.key.locationId
        }
    }
}

private data class PlaceIdentity(
    val kind: PersonalAssetLocationKind,
    val locationId: Long,
)

private data class RegionIdentity(
    val kind: PersonalAssetRegionKind,
    val zhName: String?,
    val enName: String?,
    val name: String?,
)

