package com.marshall.pyerite.personalAssetsModule.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.esiModule.data.portraitUrl
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetFlagFolder
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetLocationView
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetOwnerRef
import com.marshall.pyerite.personalAssetsModule.model.locationsAt
import com.marshall.pyerite.personalAssetsModule.model.placeAt
import com.marshall.pyerite.personalAssetsModule.model.sortedForDisplay
import com.marshall.pyerite.personalAssetsModule.model.sortedStationChildren
import com.marshall.pyerite.personalAssetsModule.navHost.PersonalAssetsRoute
import com.marshall.pyerite.personalAssetsModule.viewModel.PersonalAssetLocationViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun PersonalAssetLocationPage(
    navController: NavController,
    viewModel: PersonalAssetLocationViewModel = koinViewModel(),
    localeController: LocaleController = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val language = localeController.contentLanguage
    val merged = viewModel.locationKey.ownerCharacterId == null
    val ownerLocations = remember(uiState.snapshot, viewModel.locationKey) {
        if (!merged) {
            emptyList()
        } else {
            uiState.snapshot?.locationsAt(
                kind = viewModel.locationKey.kind,
                locationId = viewModel.locationKey.locationId,
            ).orEmpty()
        }
    }
    val location = if (merged) {
        null
    } else {
        uiState.snapshot?.locations?.get(viewModel.locationKey)
    }
    val mergedPlace = if (!merged) {
        null
    } else {
        uiState.snapshot?.placeAt(
            kind = viewModel.locationKey.kind,
            locationId = viewModel.locationKey.locationId,
            ownerCharacterId = null,
        )
    }
    val pageTitle = when {
        location != null -> personalAssetPlaceName(location, location.key.kind, localeController)
        mergedPlace != null -> personalAssetPlaceName(
            mergedPlace,
            mergedPlace.key.kind,
            localeController,
        )
        ownerLocations.isNotEmpty() -> personalAssetPlaceName(
            ownerLocations.first(),
            ownerLocations.first().key.kind,
            localeController,
        )
        else -> stringResource(R.string.personal_assets_page_title)
    }
    val children = remember(location) { location?.stationChildren.orEmpty().sortedStationChildren() }
    val items = remember(location, language) {
        location?.items.orEmpty().sortedForDisplay(language)
    }
    val showMerged = merged && (mergedPlace != null || ownerLocations.isNotEmpty())
    val showEmpty = uiState.snapshot != null &&
        !uiState.permissionDenied &&
        !uiState.isLoading &&
        location == null &&
        !showMerged &&
        !uiState.loadFailed
    val headerItemCount = when {
        location != null -> location.itemCount
        mergedPlace != null -> mergedPlace.itemCount
        else -> ownerLocations.sumOf { it.itemCount }
    }
    val itemCountText = NumberDisplayFormatter.format(
        headerItemCount.toLong(),
        NumberDisplayFormatter.Style.FULL,
    )
    val headerOwners = when {
        location != null -> location.ownerRefs()
        mergedPlace != null -> mergedPlace.owners
        else -> ownerLocations.mapNotNull { it.ownerRef() }
    }
    val headerIcon = location?.iconFilename ?: mergedPlace?.iconFilename
        ?: ownerLocations.firstOrNull()?.iconFilename
    val headerSecurity = location?.securityStatus ?: mergedPlace?.securityStatus
        ?: ownerLocations.firstOrNull()?.securityStatus
    val itemsSectionTitle = stringResource(R.string.corporation_assets_items_section)
    PersonalAssetPageScaffold(
        title = pageTitle,
        isLoading = uiState.isLoading,
        loadFailed = uiState.loadFailed,
        permissionDenied = uiState.permissionDenied,
        showLoadingIcon = uiState.showLoadingIcon,
        onRefresh = viewModel::refresh,
        onBack = navController.rememberNavigateUpAction(),
    ) {
        if (showEmpty) {
            item(key = "empty") { PersonalAssetEmptyText() }
        }
        if ((location != null || showMerged) && !uiState.permissionDenied) {
            item(key = "location-section") {
                BaseContainer(
                    title = stringResource(R.string.corporation_assets_location_section),
                    useSystemBarsPadding = false,
                    modifier = Modifier.padding(
                        top = dimensionResource(R.dimen.type_detail_section_gap),
                    ),
                ) {
                    PersonalAssetPlaceLine(
                        iconFilename = headerIcon,
                        securityStatus = headerSecurity,
                        placeName = pageTitle,
                        owners = headerOwners,
                        avatarsOnly = merged && headerOwners.isNotEmpty(),
                        hint = stringResource(
                            R.string.corporation_assets_contains_items,
                            itemCountText,
                        ),
                        showChevron = false,
                        showDivider = false,
                        onClick = null,
                    )
                }
            }
            if (showMerged && ownerLocations.isNotEmpty()) {
                item(key = "character-categories") {
                    BaseContainer(
                        title = stringResource(R.string.corporation_assets_categories_section),
                        useSystemBarsPadding = false,
                        modifier = Modifier.padding(
                            top = dimensionResource(R.dimen.type_detail_section_gap),
                        ),
                    ) {
                        ownerLocations.forEachIndexed { index, ownerLocation ->
                            val ownerId = ownerLocation.key.ownerCharacterId ?: return@forEachIndexed
                            PersonalAssetCharacterLine(
                                name = ownerLocation.ownerName.orEmpty(),
                                portraitUrl = portraitUrl(ownerId),
                                hint = stringResource(
                                    R.string.corporation_assets_contains_types,
                                    NumberDisplayFormatter.format(
                                        ownerLocation.typeCount.toLong(),
                                        NumberDisplayFormatter.Style.FULL,
                                    ),
                                ),
                                showDivider = index < ownerLocations.lastIndex,
                                onClick = {
                                    navController.navigate(
                                        PersonalAssetsRoute.Location.create(
                                            characterId = viewModel.characterId,
                                            ownerKey = ownerId.toString(),
                                            kind = ownerLocation.key.kind,
                                            locationId = ownerLocation.key.locationId,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
            if (children.isNotEmpty()) {
                item(key = "categories-section") {
                    BaseContainer(
                        title = stringResource(R.string.corporation_assets_categories_section),
                        useSystemBarsPadding = false,
                        modifier = Modifier.padding(
                            top = dimensionResource(R.dimen.type_detail_section_gap),
                        ),
                    ) {
                        children.forEachIndexed { index, child ->
                            when (child) {
                                is PersonalAssetFlagFolder -> PersonalAssetPlaceLine(
                                    iconFilename = child.iconFilename,
                                    securityStatus = null,
                                    placeName = personalAssetSectionLabel(
                                        labelRes = child.labelRes,
                                        hangarDivision = child.hangarDivision,
                                        hangarCustomName = child.hangarCustomName,
                                    ),
                                    hint = stringResource(
                                        R.string.corporation_assets_contains_types,
                                        NumberDisplayFormatter.format(
                                            child.typeCount.toLong(),
                                            NumberDisplayFormatter.Style.FULL,
                                        ),
                                    ),
                                    showChevron = true,
                                    showDivider = index < children.lastIndex,
                                    onClick = {
                                        navController.navigate(
                                            PersonalAssetsRoute.Container.create(
                                                characterId = viewModel.characterId,
                                                itemId = child.containerItemId,
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
            if (location?.showsItems == true && items.isNotEmpty()) {
                item(key = "space-items") {
                    BaseContainer(
                        title = itemsSectionTitle,
                        useSystemBarsPadding = false,
                        modifier = Modifier.padding(
                            top = dimensionResource(R.dimen.type_detail_section_gap),
                        ),
                    ) {
                        items.forEachIndexed { index, row ->
                            val typeName = row.displayName(localeController).ifBlank {
                                stringResource(R.string.corporation_assets_unknown_type)
                            }
                            val itemName = row.customName?.takeIf { it.isNotBlank() } ?: typeName
                            PersonalAssetPlaceLine(
                                iconFilename = row.iconFilename,
                                securityStatus = null,
                                placeName = itemName,
                                hint = stringResource(
                                    R.string.corporation_assets_contains_types,
                                    NumberDisplayFormatter.format(
                                        row.contentTypeCount.toLong(),
                                        NumberDisplayFormatter.Style.FULL,
                                    ),
                                ),
                                showChevron = true,
                                showDivider = index < items.lastIndex,
                                onClick = {
                                    navController.openPersonalAssetItem(viewModel.characterId, row)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun PersonalAssetLocationView.ownerRef(): PersonalAssetOwnerRef? {
    val ownerId = key.ownerCharacterId ?: return null
    val name = ownerName?.takeIf { it.isNotBlank() } ?: return null
    return PersonalAssetOwnerRef(characterId = ownerId, name = name)
}

private fun PersonalAssetLocationView.ownerRefs(): List<PersonalAssetOwnerRef> =
    listOfNotNull(ownerRef())
