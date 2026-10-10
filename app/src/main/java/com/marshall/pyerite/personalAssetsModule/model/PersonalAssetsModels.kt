package com.marshall.pyerite.personalAssetsModule.model

import androidx.annotation.StringRes
import com.marshall.pyerite.localization.ContentLanguage
import com.marshall.pyerite.localization.LocalizableName
import com.marshall.pyerite.localization.localizedName
import kotlinx.serialization.Serializable

internal class PersonalAssetsAccessException : Exception()

@Serializable
internal enum class PersonalAssetLocationKind(val routeValue: String) {
    STATION("station"),
    SOLAR_SYSTEM("solar_system"),
    ASSET_SAFETY("asset_safety"),
    UNKNOWN("unknown"),
    ;

    companion object {
        fun fromRoute(value: String): PersonalAssetLocationKind =
            entries.firstOrNull { it.routeValue == value } ?: UNKNOWN
    }
}

@Serializable
internal enum class PersonalAssetRegionKind {
    REGION,
    ASSET_SAFETY,
    UNKNOWN,
}

@Serializable
internal data class PersonalAssetLocationKey(
    /** Null when the place merges every selected character. */
    val ownerCharacterId: Long?,
    val kind: PersonalAssetLocationKind,
    val locationId: Long,
)

@Serializable
internal data class PersonalAssetOwnerRef(
    val characterId: Long,
    val name: String,
)

@Serializable
internal data class PersonalAssetPlaceRow(
    val key: PersonalAssetLocationKey,
    val iconFilename: String?,
    val securityStatus: Double?,
    val typeZhName: String? = null,
    val typeEnName: String? = null,
    val typeName: String? = null,
    override val zhName: String?,
    override val enName: String?,
    override val name: String?,
    val itemCount: Int,
    /** Set when the same place is listed once per character. */
    val ownerName: String? = null,
    /** Characters who have assets at this place. Empty when aggregation is off. */
    val owners: List<PersonalAssetOwnerRef> = emptyList(),
) : LocalizableName

@Serializable
internal data class PersonalAssetRegionSection(
    val kind: PersonalAssetRegionKind,
    override val zhName: String?,
    override val enName: String?,
    override val name: String?,
    val places: List<PersonalAssetPlaceRow>,
) : LocalizableName

@Serializable
internal sealed interface PersonalAssetStationChild {
    val sortOrder: Int
}

@Serializable
internal data class PersonalAssetFlagFolder(
    val routeFlag: String,
    @param:StringRes val labelRes: Int,
    val hangarDivision: Int?,
    val hangarCustomName: String?,
    val typeCount: Int,
    val iconFilename: String? = null,
    val containerItemId: Long,
    override val sortOrder: Int,
) : PersonalAssetStationChild

@Serializable
internal data class PersonalAssetLocationView(
    val key: PersonalAssetLocationKey,
    val iconFilename: String?,
    val securityStatus: Double?,
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
    val stationChildren: List<PersonalAssetStationChild>,
    val items: List<PersonalAssetItemRow>,
    val ownerName: String? = null,
) : LocalizableName

@Serializable
internal data class PersonalAssetItemRow(
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
    val contentTypeCount: Int = 0,
) : LocalizableName

@Serializable
internal data class PersonalAssetSlotSection(
    val group: PersonalAssetSlotGroup,
    val items: List<PersonalAssetItemRow>,
)

@Serializable
internal data class PersonalAssetContentSection(
    @param:StringRes val titleRes: Int,
    val hangarDivision: Int?,
    val hangarCustomName: String?,
    val sortOrder: Int,
    val items: List<PersonalAssetItemRow>,
)

@Serializable
internal data class PersonalAssetContainerView(
    val itemId: Long,
    val typeId: Int,
    val iconFilename: String?,
    override val zhName: String?,
    override val enName: String?,
    override val name: String?,
    val customName: String?,
    val usedVolume: Double,
    val capacity: Double?,
    val slotSections: List<PersonalAssetSlotSection>,
    val contentSections: List<PersonalAssetContentSection>,
    @param:StringRes val categoryTitleRes: Int? = null,
    val hangarDivision: Int? = null,
    val hangarCustomName: String? = null,
    val showsCapacityHeader: Boolean = true,
) : LocalizableName

@Serializable
internal data class PersonalAssetsSnapshot(
    val regions: List<PersonalAssetRegionSection>,
    val locations: Map<PersonalAssetLocationKey, PersonalAssetLocationView>,
    val containers: Map<Long, PersonalAssetContainerView>,
) {
    companion object {
        val EMPTY = PersonalAssetsSnapshot(
            regions = emptyList(),
            locations = emptyMap(),
            containers = emptyMap(),
        )
    }
}

internal fun PersonalAssetItemRow.sortedName(language: ContentLanguage): String =
    localizedName(zhName, enName, name, language)

internal fun List<PersonalAssetItemRow>.sortedForDisplay(
    language: ContentLanguage,
): List<PersonalAssetItemRow> = sortedWith(
    compareBy<PersonalAssetItemRow, String>(String.CASE_INSENSITIVE_ORDER) { it.sortedName(language) }
        .thenBy { it.itemId },
)

internal fun List<PersonalAssetPlaceRow>.sortedPlaces(
    language: ContentLanguage,
): List<PersonalAssetPlaceRow> = sortedWith(
    compareBy<PersonalAssetPlaceRow, String>(String.CASE_INSENSITIVE_ORDER) {
        localizedName(it.zhName, it.enName, it.name, language)
    }.thenBy { it.ownerName.orEmpty() }
        .thenBy { it.key.locationId },
)

internal fun List<PersonalAssetRegionSection>.sortedRegions(
    language: ContentLanguage,
): List<PersonalAssetRegionSection> = sortedWith(
    compareBy<PersonalAssetRegionSection> { it.kind.ordinal }
        .thenBy(String.CASE_INSENSITIVE_ORDER) {
            localizedName(it.zhName, it.enName, it.name, language)
        },
)

internal fun List<PersonalAssetStationChild>.sortedStationChildren(): List<PersonalAssetStationChild> =
    sortedWith(
        compareBy<PersonalAssetStationChild> { it.sortOrder }
            .thenBy { (it as? PersonalAssetFlagFolder)?.routeFlag.orEmpty() },
    )

internal fun PersonalAssetsSnapshot.withOwner(
    characterId: Long,
    ownerName: String?,
): PersonalAssetsSnapshot {
    val name = ownerName?.takeIf { it.isNotBlank() }
    val owners = listOf(PersonalAssetOwnerRef(characterId = characterId, name = name.orEmpty()))
    return copy(
        regions = regions.map { region ->
            region.copy(
                places = region.places.map { place ->
                    place.copy(ownerName = name, owners = owners)
                },
            )
        },
        locations = locations.mapValues { (_, view) -> view.copy(ownerName = name) },
    )
}

internal fun PersonalAssetsSnapshot.locationsAt(
    kind: PersonalAssetLocationKind,
    locationId: Long,
): List<PersonalAssetLocationView> = locations.values
    .filter { it.key.kind == kind && it.key.locationId == locationId }
    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.ownerName.orEmpty() })

internal fun PersonalAssetsSnapshot.placeAt(
    kind: PersonalAssetLocationKind,
    locationId: Long,
    ownerCharacterId: Long?,
): PersonalAssetPlaceRow? = regions.asSequence()
    .flatMap { it.places.asSequence() }
    .firstOrNull { place ->
        place.key.kind == kind &&
            place.key.locationId == locationId &&
            place.key.ownerCharacterId == ownerCharacterId
    }
