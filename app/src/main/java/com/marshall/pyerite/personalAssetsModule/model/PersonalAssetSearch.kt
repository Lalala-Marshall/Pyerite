package com.marshall.pyerite.personalAssetsModule.model

/**
 * One indexed asset, plus the place and containers above it.
 * [parentItemId] is the container this row sits in, when it has one.
 */
internal data class PersonalAssetSearchEntry(
    val row: PersonalAssetItemRow,
    val locationKey: PersonalAssetLocationKey,
    val place: PersonalAssetPlaceRow,
    val ancestors: List<PersonalAssetItemRow>,
    val parentItemId: Long?,
)

internal fun PersonalAssetItemRow.matchesAssetQuery(query: String): Boolean {
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
internal fun PersonalAssetsSnapshot.searchEntries(): List<PersonalAssetSearchEntry> {
    val snapshot = this
    val places = regions.flatMap { it.places }.associateBy { it.key }
    val seen = HashSet<Long>()
    val entries = ArrayList<PersonalAssetSearchEntry>()
    locations.values
        .sortedBy { it.key.kind.ordinal }
        .forEach { location ->
            val place = places[location.key] ?: places.values.firstOrNull { candidate ->
                candidate.key.kind == location.key.kind &&
                    candidate.key.locationId == location.key.locationId
            }?.copy(
                key = location.key,
                ownerName = location.ownerName,
            ) ?: return@forEach
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
                location.stationChildren.filterIsInstance<PersonalAssetFlagFolder>().forEach { folder ->
                    val container = containers[folder.containerItemId] ?: return@forEach
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
    return entries
}

/** Matching rows stay separate. Stacks from different characters are not quantity-merged. */
internal fun List<PersonalAssetSearchEntry>.matchingAssetQuery(
    query: String,
): List<PersonalAssetSearchEntry> {
    if (query.isBlank()) return emptyList()
    return filter { it.row.matchesAssetQuery(query) }
}

private fun PersonalAssetsSnapshot.walkSearchItem(
    row: PersonalAssetItemRow,
    locationKey: PersonalAssetLocationKey,
    place: PersonalAssetPlaceRow,
    ancestors: List<PersonalAssetItemRow>,
    parentItemId: Long?,
    seen: MutableSet<Long>,
    entries: MutableList<PersonalAssetSearchEntry>,
) {
    if (!seen.add(row.itemId)) return
    entries += PersonalAssetSearchEntry(
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

private fun PersonalAssetContainerView.childRows(): List<PersonalAssetItemRow> =
    slotSections.flatMap { it.items } + contentSections.flatMap { it.items }
