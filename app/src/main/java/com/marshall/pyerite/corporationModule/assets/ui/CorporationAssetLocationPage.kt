package com.marshall.pyerite.corporationModule.assets.ui

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
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetFlagFolder
import com.marshall.pyerite.corporationModule.assets.model.sortedForDisplay
import com.marshall.pyerite.corporationModule.assets.model.sortedStationChildren
import com.marshall.pyerite.corporationModule.assets.navHost.CorporationAssetsRoute
import com.marshall.pyerite.corporationModule.assets.viewModel.CorporationAssetLocationViewModel
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Location classification page:
 * - Building: Location section + Asset categories → container (sectioned)
 * - Solar system: Location section + space assets (incl. resolved asset-safety packages)
 */
@Composable
internal fun CorporationAssetLocationPage(
    navController: NavController,
    viewModel: CorporationAssetLocationViewModel = koinViewModel(),
    localeController: LocaleController = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val language = localeController.contentLanguage
    val location = uiState.snapshot?.locations?.get(viewModel.locationKey)
    val pageTitle = if (location == null) {
        stringResource(R.string.corporation_assets_page_title)
    } else {
        corporationAssetPlaceName(location, location.key.kind, localeController)
    }
    val children = remember(location) { location?.stationChildren.orEmpty().sortedStationChildren() }
    val items = remember(location, language) {
        location?.items.orEmpty().sortedForDisplay(language)
    }
    val showEmpty = uiState.snapshot != null &&
        !uiState.permissionDenied &&
        !uiState.isLoading &&
        location == null &&
        !uiState.loadFailed
    val itemCountText = NumberDisplayFormatter.format(
        (location?.itemCount ?: 0).toLong(),
        NumberDisplayFormatter.Style.FULL,
    )
    val itemsSectionTitle = stringResource(R.string.corporation_assets_items_section)
    CorporationAssetPageScaffold(
        title = pageTitle,
        isLoading = uiState.isLoading,
        loadFailed = uiState.loadFailed,
        permissionDenied = uiState.permissionDenied,
        onRefresh = viewModel::refresh,
        onBack = navController.rememberNavigateUpAction(),
    ) {
        if (showEmpty) {
            item(key = "empty") { CorporationAssetEmptyText() }
        }
        if (location != null && !uiState.permissionDenied) {
            item(key = "location-section") {
                BaseContainer(
                    title = stringResource(R.string.corporation_assets_location_section),
                    useSystemBarsPadding = false,
                    modifier = Modifier.padding(
                        top = dimensionResource(R.dimen.type_detail_section_gap),
                    ),
                ) {
                    CorporationAssetPlaceLine(
                        iconFilename = location.iconFilename,
                        securityStatus = location.securityStatus,
                        placeName = pageTitle,
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
                                is CorporationAssetFlagFolder -> CorporationAssetPlaceLine(
                                    iconFilename = child.iconFilename,
                                    securityStatus = null,
                                    placeName = corporationAssetSectionLabel(
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
                                            CorporationAssetsRoute.Container.create(
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
            if (location.showsItems && items.isNotEmpty()) {
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
                            CorporationAssetPlaceLine(
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
                                    navController.openCorporationAssetItem(
                                        viewModel.characterId,
                                        row,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
