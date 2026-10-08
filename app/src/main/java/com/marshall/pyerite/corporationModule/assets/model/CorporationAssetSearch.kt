package com.marshall.pyerite.corporationModule.assets.model

/**
 * One indexed asset, plus the place and containers above it.
 * [parentItemId] is the container this row sits in, when it has one.
 */
internal data class CorporationAssetSearchEntry(
    val row: CorporationAssetItemRow,
    val locationKey: CorporationAssetLocationKey,
    val place: CorporationAssetPlaceRow,
    val ancestors: List<CorporationAssetItemRow>,
    val parentItemId: Long?,
)

internal fun CorporationAssetItemRow.matchesAssetQuery(query: String): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) return false
    return listOfNotNull(zhName, enName, name, customName).any { field ->
        field.contains(needle, ignoreCase = true)
    }
}

/**
 * Every item the asset pages can open.
 * Station and structure places are walked before solar systems so a hull that also
 * sits in space keeps the building path.
 */
internal fun CorporationAssetsSnapshot.searchEntries(): List<CorporationAssetSearchEntry> {
    val snapshot = this
    val places = regions.flatMap { it.places }.associateBy { it.key }
    val seen = HashSet<Long>()
    val entries = ArrayList<CorporationAssetSearchEntry>()
    locations.values
        .sortedBy { it.key.kind.ordinal }
        .forEach { location ->
            val place = places[location.key] ?: return@forEach
            if (location.showsItems) {
                location.items.forEach { item ->
                    snapshot.walkSearchItem(
                        row = item,
                        locationKey = location.key,
                        place = place,
                        ancestors = emptyList(),
                        parentItemId = null,
                        seen = seen,
                        entries = entries,
                    )
                }
            } else {
                location.stationChildren.filterIsInstance<CorporationAssetFlagFolder>().forEach { folder ->
                    val container = containers[folder.containerItemId] ?: return@forEach
                    if (container.typeId > 0) {
                        snapshot.walkSearchItem(
                            row = container.toSearchRow(),
                            locationKey = location.key,
                            place = place,
                            ancestors = emptyList(),
                            parentItemId = null,
                            seen = seen,
                            entries = entries,
                        )
                    } else {
                        container.childRows().forEach { item ->
                            snapshot.walkSearchItem(
                                row = item,
                                locationKey = location.key,
                                place = place,
                                ancestors = emptyList(),
                                parentItemId = null,
                                seen = seen,
                                entries = entries,
                            )
                        }
                    }
                }
            }
        }
    return entries
}

/**
 * Same type in the same container collapses to one row. Containers stay separate.
 */
internal fun List<CorporationAssetSearchEntry>.matchingAssetQuery(
    query: String,
): List<CorporationAssetSearchEntry> {
    if (query.isBlank()) return emptyList()
    val matched = filter { it.row.matchesAssetQuery(query) }
    val containers = matched.filter { it.row.isContainer }
    val stacks = matched
        .filter { !it.row.isContainer }
        .groupBy { entry ->
            AssetSearchMergeKey(
                typeId = entry.row.typeId,
                customName = entry.row.customName?.takeIf { it.isNotBlank() },
                parentItemId = entry.parentItemId,
                locationKey = entry.locationKey,
            )
        }
        .map { (_, group) ->
            val first = group.minBy { it.row.itemId }
            val quantity = group.sumOf { it.row.quantity.coerceAtLeast(0L) }
            first.copy(row = first.row.copy(quantity = quantity))
        }
    return containers + stacks
}

private data class AssetSearchMergeKey(
    val typeId: Int,
    val customName: String?,
    val parentItemId: Long?,
    val locationKey: CorporationAssetLocationKey,
)

private fun CorporationAssetsSnapshot.walkSearchItem(
    row: CorporationAssetItemRow,
    locationKey: CorporationAssetLocationKey,
    place: CorporationAssetPlaceRow,
    ancestors: List<CorporationAssetItemRow>,
    parentItemId: Long?,
    seen: MutableSet<Long>,
    entries: MutableList<CorporationAssetSearchEntry>,
) {
    if (!seen.add(row.itemId)) return
    entries += CorporationAssetSearchEntry(
        row = row,
        locationKey = locationKey,
        place = place,
        ancestors = ancestors,
        parentItemId = parentItemId,
    )
    if (!row.isContainer) return
    val container = containers[row.itemId] ?: return
    val nextAncestors = ancestors + row
    container.childRows().forEach { child ->
        this@walkSearchItem.walkSearchItem(
            row = child,
            locationKey = locationKey,
            place = place,
            ancestors = nextAncestors,
            parentItemId = row.itemId,
            seen = seen,
            entries = entries,
        )
    }
}

private fun CorporationAssetContainerView.childRows(): List<CorporationAssetItemRow> =
    slotSections.flatMap { it.items } + contentSections.flatMap { it.items }

private fun CorporationAssetContainerView.toSearchRow(): CorporationAssetItemRow =
    CorporationAssetItemRow(
        itemId = itemId,
        typeId = typeId,
        iconFilename = iconFilename,
        zhName = zhName,
        enName = enName,
        name = name,
        customName = customName,
        quantity = 1L,
        isSingleton = true,
        isContainer = true,
        isOffice = officeContentTypeCount != null,
        contentTypeCount = officeContentTypeCount ?: 0,
    )
