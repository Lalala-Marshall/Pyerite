package com.marshall.pyerite.corporationModule.assets.model

import androidx.annotation.StringRes
import com.marshall.pyerite.localization.ContentLanguage
import com.marshall.pyerite.localization.LocalizableName
import com.marshall.pyerite.localization.localizedName
import kotlinx.serialization.Serializable

internal class CorporationAssetsAccessException : Exception()

@Serializable
internal enum class CorporationAssetLocationKind(val routeValue: String) {
    STATION("station"),
    SOLAR_SYSTEM("solar_system"),
    ASSET_SAFETY("asset_safety"),
    UNKNOWN("unknown"),
    ;

    companion object {
        fun fromRoute(value: String): CorporationAssetLocationKind =
            entries.firstOrNull { it.routeValue == value } ?: UNKNOWN
    }
}

@Serializable
internal enum class CorporationAssetRegionKind {
    REGION,
    ASSET_SAFETY,
    UNKNOWN,
}

@Serializable
internal data class CorporationAssetLocationKey(
    val kind: CorporationAssetLocationKind,
    val locationId: Long,
)

@Serializable
internal data class CorporationAssetFolderKey(
    val kind: CorporationAssetLocationKind,
    val locationId: Long,
    val routeFlag: String,
)

@Serializable
internal data class CorporationAssetPlaceRow(
    val key: CorporationAssetLocationKey,
    val iconFilename: String?,
    val securityStatus: Double?,
    /** Station / structure type names (shown before building name). */
    val typeZhName: String? = null,
    val typeEnName: String? = null,
    val typeName: String? = null,
    /** Place or building name (system name, or structure/station name in brackets). */
    override val zhName: String?,
    override val enName: String?,
    override val name: String?,
    val itemCount: Int,
) : LocalizableName

@Serializable
internal data class CorporationAssetRegionSection(
    val kind: CorporationAssetRegionKind,
    override val zhName: String?,
    override val enName: String?,
    override val name: String?,
    val places: List<CorporationAssetPlaceRow>,
) : LocalizableName

@Serializable
internal sealed interface CorporationAssetStationChild {
    val sortOrder: Int
}

@Serializable
internal data class CorporationAssetFlagFolder(
    val routeFlag: String,
    @param:StringRes val labelRes: Int,
    val hangarDivision: Int?,
    val hangarCustomName: String?,
    val typeCount: Int,
    /** SDE type icon. Offices use the office-type icon (question mark in the icon pack). */
    val iconFilename: String? = null,
    /** Opens [CorporationAssetContainerView] (real office item or synthetic category). */
    val containerItemId: Long,
    override val sortOrder: Int,
) : CorporationAssetStationChild

@Serializable
internal data class CorporationAssetLocationView(
    val key: CorporationAssetLocationKey,
    val iconFilename: String?,
    val securityStatus: Double?,
    /** Station / structure type names shown before the building name. */
    val typeZhName: String? = null,
    val typeEnName: String? = null,
    val typeName: String? = null,
    override val zhName: String?,
    override val enName: String?,
    override val name: String?,
    val typeCount: Int,
    val itemCount: Int,
    /** True for solar-system / asset-safety places that list root assets directly. */
    val showsItems: Boolean,
    /** Category rows on a station/structure location page (offices, deliveries, …). */
    val stationChildren: List<CorporationAssetStationChild>,
    val items: List<CorporationAssetItemRow>,
) : LocalizableName

@Serializable
internal data class CorporationAssetFolderView(
    @param:StringRes val labelRes: Int,
    val hangarDivision: Int?,
    val hangarCustomName: String?,
    val items: List<CorporationAssetItemRow>,
)

@Serializable
internal data class CorporationAssetItemRow(
    val itemId: Long,
    val typeId: Int,
    val iconFilename: String?,
    override val zhName: String?,
    override val enName: String?,
    override val name: String?,
    val customName: String?,
    val quantity: Long,
    val isSingleton: Boolean,
    val isContainer: Boolean,
    /** Corporation office folder. Container page shows how many items it holds. */
    val isOffice: Boolean = false,
    /** Distinct types inside this item. */
    val contentTypeCount: Int = 0,
) : LocalizableName

@Serializable
internal data class CorporationAssetSlotSection(
    val group: CorporationAssetSlotGroup,
    val items: List<CorporationAssetItemRow>,
)

@Serializable
internal data class CorporationAssetContentSection(
    @param:StringRes val titleRes: Int,
    val hangarDivision: Int?,
    val hangarCustomName: String?,
    val sortOrder: Int,
    val items: List<CorporationAssetItemRow>,
)

@Serializable
internal data class CorporationAssetContainerView(
    val itemId: Long,
    val typeId: Int,
    val iconFilename: String?,
    override val zhName: String?,
    override val enName: String?,
    override val name: String?,
    val customName: String?,
    val usedVolume: Double,
    val capacity: Double?,
    val slotSections: List<CorporationAssetSlotSection>,
    val contentSections: List<CorporationAssetContentSection>,
    /** When set, page title comes from this category label (synthetic category containers). */
    @param:StringRes val categoryTitleRes: Int? = null,
    val hangarDivision: Int? = null,
    val hangarCustomName: String? = null,
    val showsCapacityHeader: Boolean = true,
    /** Set for an office container: distinct item types inside, shown as a hint. */
    val officeContentTypeCount: Int? = null,
) : LocalizableName

@Serializable
internal data class CorporationAssetsSnapshot(
    val regions: List<CorporationAssetRegionSection>,
    val locations: Map<CorporationAssetLocationKey, CorporationAssetLocationView>,
    val folders: Map<CorporationAssetFolderKey, CorporationAssetFolderView>,
    val containers: Map<Long, CorporationAssetContainerView>,
) {
    companion object {
        val EMPTY = CorporationAssetsSnapshot(
            regions = emptyList(),
            locations = emptyMap(),
            folders = emptyMap(),
            containers = emptyMap(),
        )
    }
}

internal fun CorporationAssetItemRow.sortedName(language: ContentLanguage): String =
    localizedName(zhName, enName, name, language)

internal fun List<CorporationAssetItemRow>.sortedForDisplay(
    language: ContentLanguage,
): List<CorporationAssetItemRow> = sortedWith(
    compareBy<CorporationAssetItemRow, String>(String.CASE_INSENSITIVE_ORDER) { it.sortedName(language) }
        .thenBy { it.itemId },
)

/**
 * Merge non-container stacks of the same type (and custom name). Containers stay unique.
 */
internal fun List<CorporationAssetItemRow>.dedupedForDisplay(
    language: ContentLanguage,
): List<CorporationAssetItemRow> {
    if (isEmpty()) return emptyList()
    val containers = filter { it.isContainer }
    val mergedStacks = filter { !it.isContainer }
        .groupBy { row ->
            AssetItemDedupeKey(
                typeId = row.typeId,
                customName = row.customName?.takeIf { it.isNotBlank() },
            )
        }
        .map { (_, rows) ->
            val first = rows.minBy { it.itemId }
            first.copy(quantity = rows.sumOf { it.quantity.coerceAtLeast(0L) })
        }
    return (containers + mergedStacks).sortedForDisplay(language)
}

private data class AssetItemDedupeKey(
    val typeId: Int,
    val customName: String?,
)

internal fun List<CorporationAssetPlaceRow>.sortedPlaces(
    language: ContentLanguage,
): List<CorporationAssetPlaceRow> = sortedWith(
    compareBy<CorporationAssetPlaceRow, String>(String.CASE_INSENSITIVE_ORDER) {
        localizedName(it.zhName, it.enName, it.name, language)
    }.thenBy { it.key.locationId },
)

internal fun List<CorporationAssetRegionSection>.sortedRegions(
    language: ContentLanguage,
): List<CorporationAssetRegionSection> = sortedWith(
    compareBy<CorporationAssetRegionSection> { it.kind.ordinal }
        .thenBy(String.CASE_INSENSITIVE_ORDER) {
            localizedName(it.zhName, it.enName, it.name, language)
        },
)

internal fun List<CorporationAssetStationChild>.sortedStationChildren(): List<CorporationAssetStationChild> =
    sortedWith(
        compareBy<CorporationAssetStationChild> { it.sortOrder }
            .thenBy { (it as? CorporationAssetFlagFolder)?.routeFlag.orEmpty() },
    )
