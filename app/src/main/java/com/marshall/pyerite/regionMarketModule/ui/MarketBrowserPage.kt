package com.marshall.pyerite.regionMarketModule.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.iconModule.manager.IconManager
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.regionMarketModule.model.MarketConfig
import com.marshall.pyerite.regionMarketModule.navHost.RegionMarketRoute
import com.marshall.pyerite.regionMarketModule.viewModel.MarketBrowserViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.search.PyeriteListSearchHost
import com.marshall.pyerite.ui.golbalComponents.search.SearchNoResultsItem
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
internal fun MarketBrowserPage(
    navController: NavController,
    parentGroupId: Int = MarketConfig.NO_PARENT_GROUP,
) {
    val viewModel: MarketBrowserViewModel = koinViewModel { parametersOf(parentGroupId) }
    val ui by viewModel.ui.collectAsState()
    val search by viewModel.search.collectAsState()
    val iconManager: IconManager = koinInject()
    val localeController: LocaleController = koinInject()
    val listState = remember(parentGroupId) { LazyListState() }
    val rootTitle = stringResource(R.string.region_market)
    val title = ui.title.ifBlank { rootTitle }
    val onBack = navController.rememberNavigateUpAction()
    val query = search.query.trim()
    val hasRows = ui.groups.isNotEmpty() || ui.searchTypes.isNotEmpty()

    PyeriteListSearchHost(
        searchState = search,
        onActivateSearch = { viewModel.setSearchActive(true) },
        onQueryChange = viewModel::setSearchQuery,
        onCancelSearch = viewModel::cancelSearch,
        listState = listState,
        navTitle = title,
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
        title = { PageTitle(text = title) },
    ) {
        if (query.isNotEmpty() && !hasRows && !ui.searching) {
            item(key = "search_empty") { SearchNoResultsItem() }
        }
        items(items = ui.groups, key = { "group:${it.id}" }) { group ->
            val index = ui.groups.indexOf(group)
            val hasChildren = group.id in ui.childIdsWithChildren
            MarketSectionItem(
                model = BaseLazyColumnItemModel(
                    iconFile = iconManager.getIconFile(group.iconName),
                    itemName = group.name.orEmpty(),
                    onClick = {
                        if (hasChildren) {
                            navController.navigate(RegionMarketRoute.Group.create(group.id))
                        } else {
                            navController.navigate(RegionMarketRoute.Types.create(group.id))
                        }
                    },
                ),
                indexInSection = index,
                sectionItemCount = ui.groups.size,
                showDivider = index < ui.groups.lastIndex,
            )
        }
        if (ui.searchTypes.isNotEmpty()) {
            item(key = "search_types_header") {
                MarketSectionHeader(
                    title = stringResource(R.string.type),
                    addTopGap = ui.groups.isNotEmpty(),
                )
            }
            items(items = ui.searchTypes, key = { "type:${it.id}" }) { type ->
                val index = ui.searchTypes.indexOf(type)
                MarketSectionItem(
                    model = BaseLazyColumnItemModel(
                        iconFile = iconManager.getIconFile(type.iconFilename),
                        itemName = type.displayName(localeController),
                        onClick = {
                            navController.navigate(RegionMarketRoute.Detail.create(type.id))
                        },
                    ),
                    indexInSection = index,
                    sectionItemCount = ui.searchTypes.size,
                    showDivider = index < ui.searchTypes.lastIndex,
                )
            }
        }
        item(key = "bottom") { MarketListBottomSpacer() }
    }
}
