package com.marshall.pyerite.corporationModule.assets.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetRegionKind
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetSearchEntry
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetsConfig
import com.marshall.pyerite.corporationModule.assets.model.matchingAssetQuery
import com.marshall.pyerite.corporationModule.assets.model.searchEntries
import com.marshall.pyerite.corporationModule.assets.model.sortedName
import com.marshall.pyerite.corporationModule.assets.model.sortedPlaces
import com.marshall.pyerite.corporationModule.assets.model.sortedRegions
import com.marshall.pyerite.corporationModule.assets.navHost.CorporationAssetsRoute
import com.marshall.pyerite.corporationModule.assets.viewModel.CorporationAssetsViewModel
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.search.SearchNoResultsItem
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun CorporationAssetsPage(
    navController: NavController,
    viewModel: CorporationAssetsViewModel = koinViewModel(),
    localeController: LocaleController = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()
    val searching = query.isNotBlank()
    val pageTitle = stringResource(R.string.corporation_assets_page_title)
    val language = localeController.contentLanguage
    val regions = remember(uiState.snapshot, language) {
        uiState.snapshot?.regions.orEmpty().sortedRegions(language).map { region ->
            region to region.places.sortedPlaces(language)
        }
    }
    val searchIndex = remember(uiState.snapshot) {
        uiState.snapshot?.searchEntries().orEmpty()
    }
    val hits = remember(searchIndex, query, language) {
        searchIndex.matchingAssetQuery(query).sortedWith(
            compareBy<CorporationAssetSearchEntry, String>(String.CASE_INSENSITIVE_ORDER) {
                it.row.sortedName(language)
            }.thenBy { it.row.itemId },
        )
    }
    val showEmpty = !searching &&
        uiState.snapshot != null &&
        !uiState.permissionDenied &&
        !uiState.isLoading &&
        regions.isEmpty() &&
        !uiState.loadFailed
    val showNoResults = searching &&
        !uiState.permissionDenied &&
        !uiState.isLoading &&
        uiState.snapshot != null &&
        hits.isEmpty()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    DockCorporationAssetsSearchField()
    CorporationAssetPageScaffold(
        title = pageTitle,
        isLoading = uiState.isLoading,
        loadFailed = uiState.loadFailed,
        permissionDenied = uiState.permissionDenied,
        onRefresh = viewModel::refresh,
        onBack = navController.rememberNavigateUpAction(),
        scrollResultsToTop = searching,
        bottomBar = {
            CorporationAssetsSearchBar(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensionResource(R.dimen.search_bar_vertical_padding)),
            )
        },
    ) {
        if (showEmpty) {
            item(key = "empty") { CorporationAssetEmptyText() }
        }
        if (showNoResults) {
            item(key = "search_no_results") { SearchNoResultsItem() }
        }
        if (!searching && !uiState.permissionDenied) {
            items(
                items = regions,
                key = { (region, _) ->
                    "region-${region.kind}-${region.zhName}-${region.enName}-${region.name}"
                },
            ) { (region, places) ->
                val regionTitle = when (region.kind) {
                    CorporationAssetRegionKind.REGION ->
                        region.displayName(localeController).ifBlank {
                            stringResource(R.string.corporation_assets_unknown_region)
                        }
                    CorporationAssetRegionKind.ASSET_SAFETY ->
                        stringResource(R.string.corporation_assets_asset_safety)
                    CorporationAssetRegionKind.UNKNOWN ->
                        stringResource(R.string.corporation_assets_unknown_region)
                }
                BaseContainer(
                    title = regionTitle,
                    useSystemBarsPadding = false,
                    modifier = Modifier.padding(
                        top = dimensionResource(R.dimen.type_detail_section_gap),
                    ),
                ) {
                    places.forEachIndexed { index, place ->
                        val placeName = corporationAssetPlaceName(
                            place = place,
                            kind = place.key.kind,
                            localeController = localeController,
                        )
                        CorporationAssetPlaceLine(
                            iconFilename = place.iconFilename,
                            securityStatus = place.securityStatus,
                            placeName = placeName,
                            hint = stringResource(
                                R.string.corporation_assets_contains_items,
                                NumberDisplayFormatter.format(
                                    place.itemCount.toLong(),
                                    NumberDisplayFormatter.Style.FULL,
                                ),
                            ),
                            showChevron = true,
                            showDivider = index < places.lastIndex,
                            onClick = {
                                navController.navigate(
                                    CorporationAssetsRoute.Location.create(
                                        characterId = viewModel.characterId,
                                        kind = place.key.kind,
                                        locationId = place.key.locationId,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
        if (searching && hits.isNotEmpty()) {
            item(key = "search_gap") {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.type_detail_section_gap)))
            }
            itemsIndexed(
                items = hits,
                key = { _, hit -> hit.row.itemId },
            ) { index, hit ->
                CorporationAssetCard(isFirst = index == 0, isLast = index == hits.lastIndex) {
                    CorporationAssetItemLine(
                        row = hit.row,
                        showDivider = index < hits.lastIndex,
                        pathHint = hit.pathLabel(localeController),
                        onClick = {
                            navController.openCorporationAssetItem(viewModel.characterId, hit.row)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CorporationAssetSearchEntry.pathLabel(localeController: LocaleController): String {
    val placeName = corporationAssetPlaceName(
        place = place,
        kind = locationKey.kind,
        localeController = localeController,
    )
    val ancestorNames = ancestors.map { ancestor ->
        ancestor.customName?.takeIf { it.isNotBlank() }
            ?: ancestor.displayName(localeController).ifBlank {
                stringResource(R.string.corporation_assets_unknown_type)
            }
    }
    return (listOf(placeName) + ancestorNames).joinToString(CorporationAssetsConfig.SEARCH_PATH_SEPARATOR)
}
