package com.marshall.pyerite.personalAssetsModule.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.charactersListModule.viewModel.CharacterViewModel
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetRegionKind
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetSearchEntry
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsConfig
import com.marshall.pyerite.personalAssetsModule.model.matchingAssetQuery
import com.marshall.pyerite.personalAssetsModule.model.searchEntries
import com.marshall.pyerite.personalAssetsModule.model.sortedName
import com.marshall.pyerite.personalAssetsModule.model.sortedPlaces
import com.marshall.pyerite.personalAssetsModule.model.sortedRegions
import com.marshall.pyerite.personalAssetsModule.navHost.PersonalAssetsRoute
import com.marshall.pyerite.personalAssetsModule.viewModel.PersonalAssetsViewModel
import com.marshall.pyerite.personalAssetsModule.viewModel.ownerRouteKey
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.search.SearchNoResultsItem
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun PersonalAssetsPage(
    navController: NavController,
    viewModel: PersonalAssetsViewModel = koinViewModel(),
    characterViewModel: CharacterViewModel = koinViewModel(),
    localeController: LocaleController = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val loggedInCharacters by characterViewModel.loggedInCharacters.collectAsState()
    val query by viewModel.query.collectAsState()
    val searching = query.isNotBlank()
    val pageTitle = stringResource(R.string.personal_assets_page_title)
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
            compareBy<PersonalAssetSearchEntry, String>(String.CASE_INSENSITIVE_ORDER) {
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
    var showSettings by remember { mutableStateOf(false) }
    val authorizedCharacters = remember(loggedInCharacters) {
        loggedInCharacters.filter { EveSsoScope.ASSETS_READ in it.grantedScopes }
    }
    DockPersonalAssetsSearchField()
    PersonalAssetPageScaffold(
        title = pageTitle,
        isLoading = uiState.isLoading,
        loadFailed = uiState.loadFailed,
        permissionDenied = uiState.permissionDenied,
        showLoadingIcon = uiState.showLoadingIcon,
        onRefresh = viewModel::refresh,
        onBack = navController.rememberNavigateUpAction(),
        scrollResultsToTop = searching,
        extraEndActions = listOf(
            PyeriteTopBarActionItem(
                onClick = { showSettings = true },
                icon = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.personal_assets_settings),
            ),
        ),
        bottomBar = {
            PersonalAssetsSearchBar(
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
            item(key = "empty") { PersonalAssetEmptyText() }
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
                    PersonalAssetRegionKind.REGION ->
                        region.displayName(localeController).ifBlank {
                            stringResource(R.string.corporation_assets_unknown_region)
                        }
                    PersonalAssetRegionKind.ASSET_SAFETY ->
                        stringResource(R.string.corporation_assets_asset_safety)
                    PersonalAssetRegionKind.UNKNOWN ->
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
                        val placeName = personalAssetPlaceName(
                            place = place,
                            kind = place.key.kind,
                            localeController = localeController,
                        )
                        PersonalAssetPlaceLine(
                            iconFilename = place.iconFilename,
                            securityStatus = place.securityStatus,
                            placeName = placeName,
                            owners = place.owners,
                            avatarsOnly = place.key.ownerCharacterId == null &&
                                place.owners.isNotEmpty(),
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
                                    PersonalAssetsRoute.Location.create(
                                        characterId = viewModel.characterId,
                                        ownerKey = place.key.ownerRouteKey(),
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
                key = { _, hit -> "${hit.locationKey.ownerCharacterId}-${hit.row.itemId}" },
            ) { index, hit ->
                PersonalAssetCard(isFirst = index == 0, isLast = index == hits.lastIndex) {
                    PersonalAssetItemLine(
                        row = hit.row,
                        showDivider = index < hits.lastIndex,
                        pathHint = hit.pathLabel(localeController),
                        onClick = {
                            navController.openPersonalAssetItem(viewModel.characterId, hit.row)
                        },
                    )
                }
            }
        }
    }
    if (showSettings) {
        PersonalAssetsSettingsSheet(
            initial = settings,
            anchorCharacterId = viewModel.characterId,
            characters = authorizedCharacters,
            onCommit = { draft ->
                showSettings = false
                viewModel.applySettings(draft)
            },
        )
    }
}

@Composable
private fun PersonalAssetSearchEntry.pathLabel(localeController: LocaleController): String {
    val placeName = personalAssetPlaceName(
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
    return buildList {
        place.ownerName?.takeIf { it.isNotBlank() }?.let(::add)
        add(placeName)
        addAll(ancestorNames)
    }.joinToString(PersonalAssetsConfig.SEARCH_PATH_SEPARATOR)
}
