package com.marshall.pyerite.regionMarketModule.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.regionMarketModule.model.MarketConfig
import com.marshall.pyerite.regionMarketModule.model.MarketOrder
import com.marshall.pyerite.regionMarketModule.model.MarketSelection
import com.marshall.pyerite.regionMarketModule.viewModel.MarketLocationViewModel
import com.marshall.pyerite.regionMarketModule.viewModel.MarketOrdersViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemHint
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.PyeriteSegmentedControl
import com.marshall.pyerite.ui.golbalComponents.PyeriteSegmentedOption
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.rememberLazyListTitleCollapsed
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val SELL_PAGE = 0
private const val BUY_PAGE = 1

@Composable
internal fun MarketOrdersPage(
    navController: NavController,
    typeId: Int,
    placeKey: String,
) {
    val viewModel: MarketOrdersViewModel = koinViewModel { parametersOf(typeId, placeKey) }
    val locationViewModel: MarketLocationViewModel = koinViewModel()
    val orders by viewModel.orders.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val title by viewModel.title.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val failed by viewModel.failed.collectAsState()
    val hideLocations by viewModel.hideLocations.collectAsState()
    val denied by viewModel.structureAccessDenied.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    val followingGlobal by viewModel.followingGlobal.collectAsState()
    val displayPlaceKey by viewModel.displayPlaceKey.collectAsState()
    val listState = rememberLazyListState()
    val showCollapsedTitle = rememberLazyListTitleCollapsed(listState)
    val onBack = navController.rememberNavigateUpAction()
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableIntStateOf(SELL_PAGE) }
    val sellOrders = orders.filter { !it.isBuyOrder }.sortedBy { it.price }
    val buyOrders = orders.filter { it.isBuyOrder }.sortedByDescending { it.price }
    val visible = if (page == BUY_PAGE) buyOrders else sellOrders
    val hintColor = colorResource(R.color.hint_text)
    val hintSize = dimensionResource(R.dimen.detail_row_label_subtitle_text_size).value.sp
    val unknown = stringResource(R.string.market_unknown_location)

    PyeritePullToRefreshBox(onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
        PyeritePageScaffold(
            title = title,
            showCollapsedTitle = showCollapsedTitle,
            onBack = onBack,
            endActions = if (hideLocations) {
                emptyList()
            } else {
                listOf(
                    PyeriteTopBarActionItem(
                        onClick = { showPicker = true },
                        icon = Icons.Filled.Place,
                        contentDescription = stringResource(R.string.market_select_region),
                        label = locationName.ifBlank { unknown },
                        showIcon = false,
                        accentColor = colorResource(R.color.hyperlink_text),
                    ),
                )
            },
        ) { topBarPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(topBarPadding),
            ) {
                PyeriteSegmentedControl(
                    options = listOf(
                        PyeriteSegmentedOption(
                            SELL_PAGE,
                            stringResource(R.string.market_orders_sell, sellOrders.size),
                        ),
                        PyeriteSegmentedOption(
                            BUY_PAGE,
                            stringResource(R.string.market_orders_buy, buyOrders.size),
                        ),
                    ),
                    selected = page,
                    onSelect = { page = it },
                )
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    item(key = "title") { PageTitle(text = title) }
                    if (denied) {
                        item(key = "denied") {
                            MarketSectionHeader(
                                title = stringResource(R.string.market_structure_permission),
                                addTopGap = false,
                            )
                        }
                    } else if (failed) {
                        item(key = "failed") {
                            MarketSectionHeader(
                                title = stringResource(R.string.market_load_failed),
                                addTopGap = false,
                            )
                        }
                    } else if (!loading && visible.isEmpty()) {
                        item(key = "empty") {
                            MarketSectionHeader(
                                title = stringResource(R.string.market_no_orders),
                                addTopGap = false,
                            )
                        }
                    }
                    items(items = visible, key = { it.orderId }) { order ->
                        val index = visible.indexOf(order)
                        OrderRow(
                            order = order,
                            placeName = locations[order.locationId]?.placeName ?: unknown,
                            security = locations[order.locationId]?.security,
                            hideLocation = hideLocations,
                            index = index,
                            count = visible.size,
                            showDivider = index < visible.lastIndex,
                            hintColor = hintColor,
                            hintSize = hintSize,
                        )
                    }
                    item(key = "bottom") { MarketListBottomSpacer() }
                }
            }
        }
    }
    if (showPicker && !hideLocations) {
        MarketLocationSheet(
            viewModel = locationViewModel,
            highlightedSelection = if (followingGlobal) {
                null
            } else {
                MarketSelection.parse(displayPlaceKey)
            },
            onPlaceSelected = { selection ->
                val wasDetached = !followingGlobal
                locationViewModel.select(selection)
                if (wasDetached) {
                    viewModel.followGlobal()
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        MarketConfig.PLACE_ADOPTED_KEY,
                        true,
                    )
                }
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun OrderRow(
    order: MarketOrder,
    placeName: String,
    security: Double?,
    hideLocation: Boolean,
    index: Int,
    count: Int,
    showDivider: Boolean,
    hintColor: androidx.compose.ui.graphics.Color,
    hintSize: androidx.compose.ui.unit.TextUnit,
) {
    val qty = stringResource(
        R.string.market_order_qty,
        NumberDisplayFormatter.format(order.volumeRemain, NumberDisplayFormatter.Style.FULL),
    )
    MarketSectionItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = marketPricePair(order.price),
            itemNameBold = true,
            itemNameMaxLines = 1,
            showChevron = false,
            onClick = null,
            itemHints = if (hideLocation) {
                emptyList()
            } else {
                listOf(BaseLazyColumnItemHint(annotatedText = marketLocationHint(security, placeName)))
            },
        ),
        indexInSection = index,
        sectionItemCount = count,
        showDivider = showDivider,
        titleTrailingContent = {
            Text(text = qty, color = hintColor, fontSize = hintSize, maxLines = 1)
        },
    )
}
