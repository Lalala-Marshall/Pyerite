package com.marshall.pyerite.regionMarketModule.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.databaseHierarchyModule.navHost.DatabaseRoute
import com.marshall.pyerite.iconModule.manager.IconManager
import com.marshall.pyerite.localization.ContentLanguage
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.regionMarketModule.model.MarketConfig
import com.marshall.pyerite.regionMarketModule.model.MarketHistoryRange
import com.marshall.pyerite.regionMarketModule.model.MarketSelection
import com.marshall.pyerite.regionMarketModule.navHost.RegionMarketRoute
import com.marshall.pyerite.regionMarketModule.viewModel.MarketDetailViewModel
import com.marshall.pyerite.regionMarketModule.viewModel.MarketLocationViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.PyeriteSegmentedControl
import com.marshall.pyerite.ui.golbalComponents.PyeriteSegmentedOption
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.rememberScrollTitleCollapsed
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import java.util.Locale

@Composable
internal fun MarketDetailPage(
    navController: NavController,
    typeId: Int,
    placeKey: String,
) {
    val viewModel: MarketDetailViewModel = koinViewModel { parametersOf(typeId, placeKey) }
    val locationViewModel: MarketLocationViewModel = koinViewModel()
    val ui by viewModel.ui.collectAsState()
    val iconManager: IconManager = koinInject()
    val localeController: LocaleController = koinInject()
    val scrollState = rememberScrollState()
    val showCollapsedTitle = rememberScrollTitleCollapsed(scrollState)
    val onBack = navController.rememberNavigateUpAction()
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val placeholder = stringResource(R.string.type_detail_market_placeholder_value)
    val caption = stringResource(
        R.string.market_type_caption,
        ui.categoryName,
        ui.groupName,
        typeId,
    )
    val locale = if (localeController.contentLanguage == ContentLanguage.CHINESE) {
        Locale.CHINESE
    } else {
        Locale.US
    }
    val backStackHandle = navController.currentBackStackEntry?.savedStateHandle
    if (backStackHandle != null) {
        val adopted by backStackHandle
            .getStateFlow(MarketConfig.PLACE_ADOPTED_KEY, false)
            .collectAsState()
        LaunchedEffect(adopted) {
            if (adopted) viewModel.followGlobal()
        }
    }

    PyeritePullToRefreshBox(onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
        PyeritePageScaffold(
            title = ui.typeName,
            showCollapsedTitle = showCollapsedTitle,
            onBack = onBack,
            endActions = if (ui.hideLocations) {
                emptyList()
            } else {
                listOf(
                    PyeriteTopBarActionItem(
                        onClick = { showPicker = true },
                        icon = Icons.Filled.Place,
                        contentDescription = stringResource(R.string.market_select_region),
                        label = ui.locationName.ifBlank { placeholder },
                        showIcon = false,
                        accentColor = colorResource(R.color.hyperlink_text),
                    ),
                )
            },
        ) { topBarPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(topBarPadding)
                    .verticalScroll(scrollState),
            ) {
                PageTitle(text = ui.typeName)
                val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
                BaseContainer(useSystemBarsPadding = false) {
                    BaseLazyColumnItem(
                        model = BaseLazyColumnItemModel(
                            iconFile = iconManager.getIconFile(ui.iconFileName),
                            iconSize = dimensionResource(R.dimen.type_detail_summary_icon_size),
                            itemName = ui.typeName,
                            itemNameBold = true,
                            itemHint = caption,
                            showChevron = true,
                            onClick = { navController.navigate(DatabaseRoute.TypeDetail.create(typeId)) },
                        ),
                        showDivider = false,
                    )
                }
                Spacer(modifier = Modifier.height(sectionGap))
                BaseContainer(useSystemBarsPadding = false) {
                    BaseLazyColumnItem(
                        model = BaseLazyColumnItemModel(
                            iconRes = R.drawable.ic_market_isk,
                            iconTint = Color.Unspecified,
                            itemName = stringResource(R.string.market_current_price),
                            itemHint = when {
                                ui.loading && ui.updatePageCount > 0 -> stringResource(
                                    R.string.market_update_progress,
                                    ui.updatePage,
                                    ui.updatePageCount,
                                )
                                ui.loading -> stringResource(R.string.market_updating)
                                ui.structureAccessDenied -> stringResource(R.string.market_structure_permission)
                                ui.failed -> stringResource(R.string.market_load_failed)
                                else -> marketPricePair(ui.lowestSell)
                            },
                            showChevron = false,
                            onClick = null,
                        ),
                        showDivider = true,
                        titleTrailingContent = {
                            MarketPriceRefreshButton(
                                updating = ui.loading,
                                onClick = viewModel::refresh,
                            )
                        },
                    )
                    BaseLazyColumnItem(
                        model = BaseLazyColumnItemModel(
                            showLeadingIcon = false,
                            itemName = stringResource(R.string.market_view_orders),
                            itemNameColor = if (ui.loading) colorResource(R.color.hint_text) else null,
                            showChevron = !ui.loading,
                            onClick = if (ui.loading) {
                                null
                            } else {
                                {
                                    val ordersPlace = if (ui.followingGlobal) null else ui.displayPlaceKey
                                    navController.navigate(
                                        RegionMarketRoute.Orders.create(typeId, ordersPlace),
                                    )
                                }
                            },
                        ),
                        showDivider = true,
                    )
                    BaseLazyColumnItem(
                        model = BaseLazyColumnItemModel(
                            showLeadingIcon = false,
                            itemName = stringResource(R.string.market_add_watchlist),
                            showChevron = false,
                            onClick = null,
                        ),
                        showDivider = false,
                    )
                }
                Spacer(modifier = Modifier.height(sectionGap))
                BaseContainer(
                    title = stringResource(R.string.market_price_history),
                    useSystemBarsPadding = false,
                ) {
                    HistoryRangePicker(
                        selected = ui.range,
                        onSelect = viewModel::setRange,
                    )
                    if (!ui.loading && ui.history.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = colorResource(R.color.hint_text),
                            modifier = Modifier.padding(
                                horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding),
                                vertical = dimensionResource(R.dimen.type_detail_section_inner_gap_medium),
                            ),
                        )
                    } else if (ui.history.isNotEmpty()) {
                        MarketHistoryChart(
                            history = ui.history,
                            range = ui.range,
                            locale = locale,
                            modifier = Modifier.padding(
                                start = dimensionResource(R.dimen.detail_card_horizontal_padding),
                                end = dimensionResource(R.dimen.detail_card_horizontal_padding),
                                bottom = dimensionResource(R.dimen.type_detail_section_inner_gap_medium),
                            ),
                        )
                    }
                }
            }
        }
    }

    if (showPicker && !ui.hideLocations) {
        MarketLocationSheet(
            viewModel = locationViewModel,
            highlightedSelection = if (ui.followingGlobal) {
                null
            } else {
                MarketSelection.parse(ui.displayPlaceKey)
            },
            onPlaceSelected = { selection ->
                val wasDetached = !ui.followingGlobal
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
private fun MarketPriceRefreshButton(
    updating: Boolean,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "market_price_refresh")
    val animated by transition.animateFloat(
        initialValue = MarketPriceRefresh.SPIN_START_DEGREES,
        targetValue = MarketPriceRefresh.SPIN_END_DEGREES,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = MarketPriceRefresh.SPIN_DURATION_MS,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "market_price_refresh_degrees",
    )
    Icon(
        imageVector = Icons.Filled.Refresh,
        contentDescription = stringResource(R.string.market_refresh),
        tint = colorResource(R.color.market_price_refresh_icon),
        modifier = Modifier
            .size(dimensionResource(R.dimen.market_price_refresh_icon_size))
            .clickable(
                enabled = !updating,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { role = Role.Button }
            .graphicsLayer {
                rotationZ = if (updating) animated else MarketPriceRefresh.SPIN_START_DEGREES
            },
    )
}

private object MarketPriceRefresh {
    const val SPIN_START_DEGREES = 0f
    const val SPIN_END_DEGREES = 360f
    const val SPIN_DURATION_MS = 1_000
}

@Composable
private fun HistoryRangePicker(
    selected: MarketHistoryRange,
    onSelect: (MarketHistoryRange) -> Unit,
) {
    PyeriteSegmentedControl(
        options = listOf(
            PyeriteSegmentedOption(
                MarketHistoryRange.MONTH,
                stringResource(R.string.market_history_1m),
            ),
            PyeriteSegmentedOption(
                MarketHistoryRange.QUARTER,
                stringResource(R.string.market_history_3m),
            ),
            PyeriteSegmentedOption(
                MarketHistoryRange.YEAR,
                stringResource(R.string.market_history_1y),
            ),
        ),
        selected = selected,
        onSelect = onSelect,
    )
}
