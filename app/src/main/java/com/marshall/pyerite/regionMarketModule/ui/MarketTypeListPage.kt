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
import com.marshall.pyerite.regionMarketModule.navHost.RegionMarketRoute
import com.marshall.pyerite.regionMarketModule.viewModel.MarketTypeListViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.search.PyeriteListSearchHost
import com.marshall.pyerite.ui.golbalComponents.search.SearchNoResultsItem
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
internal fun MarketTypeListPage(
    navController: NavController,
    marketGroupId: Int,
) {
    val viewModel: MarketTypeListViewModel = koinViewModel { parametersOf(marketGroupId) }
    val ui by viewModel.ui.collectAsState()
    val search by viewModel.search.collectAsState()
    val iconManager: IconManager = koinInject()
    val localeController: LocaleController = koinInject()
    val listState = remember(marketGroupId) { LazyListState() }
    val ungrouped = stringResource(R.string.market_ungrouped)
    val unpublished = stringResource(R.string.unpublished)
    val onBack = navController.rememberNavigateUpAction()
    val hasRows = ui.sections.any { it.types.isNotEmpty() }

    PyeriteListSearchHost(
        searchState = search,
        onActivateSearch = { viewModel.setSearchActive(true) },
        onQueryChange = viewModel::setSearchQuery,
        onCancelSearch = viewModel::cancelSearch,
        listState = listState,
        navTitle = ui.title,
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
        title = { PageTitle(text = ui.title) },
    ) {
        if (search.query.isNotBlank() && !hasRows) {
            item(key = "search_empty") { SearchNoResultsItem() }
        }
        ui.sections.forEachIndexed { sectionIndex, section ->
            val header = when {
                section.unpublished -> unpublished
                section.title.isBlank() -> ungrouped
                else -> section.title
            }
            item(key = "header:${section.id}") {
                MarketSectionHeader(title = header, addTopGap = sectionIndex > 0)
            }
            items(items = section.types, key = { "type:${section.id}:${it.id}" }) { type ->
                val index = section.types.indexOf(type)
                MarketSectionItem(
                    model = BaseLazyColumnItemModel(
                        iconFile = iconManager.getIconFile(type.iconFilename),
                        itemName = type.displayName(localeController),
                        onClick = {
                            navController.navigate(RegionMarketRoute.Detail.create(type.id))
                        },
                    ),
                    indexInSection = index,
                    sectionItemCount = section.types.size,
                    showDivider = index < section.types.lastIndex,
                )
            }
        }
        item(key = "bottom") { MarketListBottomSpacer() }
    }
}
