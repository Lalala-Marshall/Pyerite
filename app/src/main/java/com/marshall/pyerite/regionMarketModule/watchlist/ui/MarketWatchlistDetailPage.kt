package com.marshall.pyerite.regionMarketModule.watchlist.ui

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.iconModule.manager.IconManager
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.regionMarketModule.model.MarketSelection
import com.marshall.pyerite.regionMarketModule.navHost.RegionMarketRoute
import com.marshall.pyerite.regionMarketModule.ui.MarketLocationSheet
import com.marshall.pyerite.regionMarketModule.viewModel.MarketLocationViewModel
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchItem
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchOrderSide
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistConfig
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistImportMode
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistExportResult
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistImportResult
import com.marshall.pyerite.regionMarketModule.watchlist.viewModel.MarketWatchlistDetailUi
import com.marshall.pyerite.regionMarketModule.watchlist.viewModel.MarketWatchlistDetailViewModel
import com.marshall.pyerite.regionMarketModule.watchlist.viewModel.MarketWatchlistItemUi
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemHint
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.rememberLazyListTitleCollapsed
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.util.NumberDisplayFormatter
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
internal fun MarketWatchlistDetailPage(
    navController: NavController,
    listId: String,
) {
    val viewModel: MarketWatchlistDetailViewModel = koinViewModel { parametersOf(listId) }
    val locationViewModel: MarketLocationViewModel = koinViewModel()
    val ui by viewModel.ui.collectAsState()
    val localeController: LocaleController = koinInject()
    val iconManager: IconManager = koinInject()
    val listState = rememberLazyListState()
    val showCollapsedTitle = rememberLazyListTitleCollapsed(listState)
    val onBack = navController.rememberNavigateUpAction()
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPlacePicker by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var messageRes by remember { mutableStateOf<Int?>(null) }
    var pendingImport by remember { mutableStateOf<List<MarketWatchItem>?>(null) }
    var revealedTypeId by remember { mutableStateOf<Int?>(null) }
    val placeholder = stringResource(R.string.type_detail_market_placeholder_value)
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val importLabel = stringResource(R.string.market_watchlist_import)
    val exportLabel = stringResource(R.string.market_watchlist_export)

    LaunchedEffect(ui.missing) {
        if (ui.missing) navController.navigateUp()
    }
    var appliedLanguage by remember { mutableStateOf(localeController.contentLanguage) }
    LaunchedEffect(localeController.contentLanguage) {
        val language = localeController.contentLanguage
        if (language == appliedLanguage) return@LaunchedEffect
        appliedLanguage = language
        viewModel.onContentLanguageChanged()
    }

    if (showPlacePicker && ui.marketKey.isNotBlank()) {
        MarketLocationSheet(
            viewModel = locationViewModel,
            highlightedSelection = MarketSelection.parse(ui.marketKey),
            onPlaceSelected = viewModel::setMarket,
            onDismiss = { showPlacePicker = false },
        )
    }
    if (showAddSheet) {
        MarketWatchlistAddItemSheet(
            onDismiss = { typeIds ->
                showAddSheet = false
                if (typeIds.isNotEmpty()) viewModel.addTypes(typeIds)
            },
        )
    }
    messageRes?.let { resId ->
        WatchlistMessageDialog(
            message = stringResource(resId),
            onDismiss = { messageRes = null },
        )
    }
    pendingImport?.let { items ->
        WatchlistImportModeDialog(
            onOverwrite = {
                viewModel.applyImport(items, MarketWatchlistImportMode.Overwrite)
                pendingImport = null
            },
            onAppend = {
                viewModel.applyImport(items, MarketWatchlistImportMode.Append)
                pendingImport = null
            },
            onDismiss = { pendingImport = null },
        )
    }

    PyeritePullToRefreshBox(onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
        PyeritePageScaffold(
            title = ui.title,
            showCollapsedTitle = showCollapsedTitle,
            onBack = onBack,
            endActions = listOf(
                PyeriteTopBarActionItem(
                    onClick = {
                        scope.launch {
                            when (val result = viewModel.prepareImport(readClipboard(clipboard, context))) {
                                is MarketWatchlistImportResult.Ready -> pendingImport = result.items
                                MarketWatchlistImportResult.ClipboardEmpty -> {
                                    messageRes = R.string.market_watchlist_clipboard_empty
                                }
                                MarketWatchlistImportResult.ParseFailed -> {
                                    messageRes = R.string.market_watchlist_clipboard_parse_failed
                                }
                            }
                        }
                    },
                    icon = Icons.Filled.Download,
                    contentDescription = importLabel,
                ),
                PyeriteTopBarActionItem(
                    onClick = {
                        scope.launch {
                            when (val result = viewModel.exportText()) {
                                is MarketWatchlistExportResult.Success -> {
                                    clipboard.setClipEntry(
                                        ClipEntry(ClipData.newPlainText(exportLabel, result.text)),
                                    )
                                }
                                MarketWatchlistExportResult.Empty -> {
                                    messageRes = R.string.market_watchlist_export_empty
                                }
                            }
                        }
                    },
                    icon = Icons.Filled.Upload,
                    contentDescription = exportLabel,
                ),
                PyeriteTopBarActionItem(
                    onClick = { showAddSheet = true },
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.market_watchlist_add_item),
                ),
            ),
        ) { topBarPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(topBarPadding)
                    .imePadding()
                    .navigationBarsPadding(),
            ) {
                item(key = "title") { PageTitle(text = ui.title) }
                item(key = "basic") {
                    BaseContainer(
                        title = stringResource(R.string.market_watchlist_basic_info),
                        useSystemBarsPadding = false,
                    ) {
                        BaseLazyColumnItem(
                            model = BaseLazyColumnItemModel(
                                showLeadingIcon = false,
                                itemName = stringResource(R.string.market_watchlist_market_place),
                                trailingValue = ui.locationName.ifBlank { placeholder },
                                onClick = { showPlacePicker = true },
                            ),
                            showDivider = true,
                        )
                        BaseLazyColumnItem(
                            model = BaseLazyColumnItemModel(
                                showLeadingIcon = false,
                                itemName = stringResource(R.string.market_watchlist_order_side),
                                showChevron = false,
                                onClick = null,
                            ),
                            showDivider = true,
                            trailingContent = {
                                OrderSideToggle(
                                    side = ui.orderSide,
                                    onSelect = viewModel::setOrderSide,
                                )
                            },
                        )
                        BaseLazyColumnItem(
                            model = BaseLazyColumnItemModel(
                                showLeadingIcon = false,
                                itemName = stringResource(R.string.market_watchlist_market_price),
                                trailingValue = marketPriceText(ui, placeholder),
                                showChevron = false,
                                onClick = null,
                            ),
                            showDivider = true,
                        )
                        BaseLazyColumnItem(
                            model = BaseLazyColumnItemModel(
                                showLeadingIcon = false,
                                itemName = stringResource(R.string.market_watchlist_total_volume),
                                trailingValue = volumeText(ui.totalVolume, placeholder),
                                showChevron = false,
                                onClick = null,
                            ),
                            showDivider = false,
                        )
                    }
                }
                item(key = "gap") { Spacer(Modifier.height(sectionGap)) }
                item(key = "items") {
                    BaseContainer(
                        title = stringResource(R.string.market_watchlist_items, ui.items.size),
                        titleTrailingContent = if (ui.items.isEmpty() && !ui.editingQuantities) {
                            null
                        } else {
                            {
                                EditQuantityAction(
                                    editing = ui.editingQuantities,
                                    onBegin = viewModel::beginEditing,
                                    onFinish = viewModel::finishEditing,
                                )
                            }
                        },
                        useSystemBarsPadding = false,
                    ) {
                        if (ui.items.isEmpty()) {
                            BaseLazyColumnItem(
                                model = BaseLazyColumnItemModel(
                                    showLeadingIcon = false,
                                    itemName = stringResource(R.string.market_watchlist_items_empty),
                                    showChevron = false,
                                    onClick = null,
                                ),
                                showDivider = false,
                            )
                        } else {
                            ui.items.forEachIndexed { index, item ->
                                val revealed = revealedTypeId == item.typeId
                                WatchlistItemRow(
                                    item = item,
                                    iconManager = iconManager,
                                    editing = ui.editingQuantities,
                                    draft = ui.quantityDrafts[item.typeId].orEmpty(),
                                    placeholder = placeholder,
                                    showDivider = index != ui.items.lastIndex,
                                    revealed = revealed,
                                    onOpenMarket = {
                                        navController.navigate(
                                            RegionMarketRoute.Detail.create(item.typeId, ui.marketKey),
                                        )
                                    },
                                    onDraftChange = { viewModel.updateQuantityDraft(item.typeId, it) },
                                    onReveal = { revealedTypeId = item.typeId },
                                    onHide = { if (revealedTypeId == item.typeId) revealedTypeId = null },
                                    onDelete = {
                                        if (revealedTypeId == item.typeId) revealedTypeId = null
                                        viewModel.removeItem(item.typeId)
                                    },
                                )
                            }
                        }
                    }
                }
                item(key = "bottom") {
                    Spacer(Modifier.height(dimensionResource(R.dimen.type_detail_bottom_padding)))
                }
            }
        }
    }
}

@Composable
private fun EditQuantityAction(
    editing: Boolean,
    onBegin: () -> Unit,
    onFinish: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val onBeginUpdated by rememberUpdatedState(onBegin)
    val onFinishUpdated by rememberUpdatedState(onFinish)
    Text(
        text = stringResource(
            if (editing) R.string.market_done else R.string.market_watchlist_edit_quantity,
        ),
        color = colorResource(R.color.hyperlink_text),
        fontSize = dimensionResource(R.dimen.list_section_subheader_text_size).value.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .pointerInput(editing) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (up == null) return@awaitEachGesture
                    if ((up.position - down.position).getDistance() > slop) return@awaitEachGesture
                    up.consume()
                    focusManager.clearFocus(force = true)
                    if (editing) onFinishUpdated() else onBeginUpdated()
                }
            }
            .padding(end = dimensionResource(R.dimen.detail_card_horizontal_padding)),
    )
}

@Composable
private fun WatchlistItemRow(
    item: MarketWatchlistItemUi,
    iconManager: IconManager,
    editing: Boolean,
    draft: String,
    placeholder: String,
    showDivider: Boolean,
    revealed: Boolean,
    onOpenMarket: () -> Unit,
    onDraftChange: (String) -> Unit,
    onReveal: () -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit,
) {
    val quantity = NumberDisplayFormatter.format(item.quantity, NumberDisplayFormatter.Style.FULL)
    SwipeDeleteRow(
        revealed = revealed,
        onReveal = onReveal,
        onHide = onHide,
        onDelete = onDelete,
    ) {
        BaseLazyColumnItem(
            model = BaseLazyColumnItemModel(
                iconFile = iconManager.getIconFile(item.iconFileName),
                itemName = item.name,
                itemHints = listOf(
                    BaseLazyColumnItemHint(
                        text = stringResource(
                            R.string.market_watchlist_unit_price,
                            fullPrice(item.unitPrice, placeholder),
                        ),
                    ),
                    BaseLazyColumnItemHint(
                        text = stringResource(
                            R.string.market_watchlist_line_total,
                            fullPrice(item.lineTotal, placeholder),
                        ),
                    ),
                ),
                trailingValue = if (editing) "" else quantity,
                showChevron = !editing,
                onClick = when {
                    editing -> null
                    revealed -> onHide
                    else -> onOpenMarket
                },
                onLongClick = onReveal,
            ),
            showDivider = showDivider,
            trailingContent = if (editing) {
                {
                    QuantityField(
                        value = draft,
                        onValueChange = onDraftChange,
                    )
                }
            } else {
                null
            },
        )
    }
}

@Composable
private fun SwipeDeleteRow(
    revealed: Boolean,
    onReveal: () -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val deleteWidth = dimensionResource(R.dimen.market_watchlist_delete_action_width)
    val deletePx = with(LocalDensity.current) { deleteWidth.toPx() }
    var offset by remember { mutableFloatStateOf(0f) }
    val revealFraction = MarketWatchlistConfig.DELETE_REVEAL_FRACTION
    val reveal by rememberUpdatedState(onReveal)
    val hide by rememberUpdatedState(onHide)
    LaunchedEffect(revealed, deletePx) {
        offset = if (revealed) -deletePx else 0f
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds(),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(colorResource(R.color.character_security_negative)),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .width(deleteWidth)
                    .fillMaxHeight()
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.market_watchlist_delete),
                    color = colorResource(R.color.white),
                    fontWeight = FontWeight.Medium,
                    fontSize = dimensionResource(R.dimen.list_section_subheader_text_size).value.sp,
                )
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .background(colorResource(R.color.second_background))
                .pointerInput(deletePx) {
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            pass = PointerEventPass.Initial,
                            requireUnconsumed = false,
                        )
                        var dragging = false
                        var totalX = 0f
                        var totalY = 0f
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (dragging) {
                                    val open = offset < -deletePx * revealFraction
                                    offset = if (open) -deletePx else 0f
                                    if (open) reveal() else hide()
                                }
                                break
                            }
                            val delta = change.positionChange()
                            totalX += delta.x
                            totalY += delta.y
                            if (!dragging && (abs(totalX) > slop || abs(totalY) > slop)) {
                                dragging = abs(totalX) > abs(totalY)
                                if (!dragging) break
                            }
                            if (dragging) {
                                change.consume()
                                offset = (offset + delta.x).coerceIn(-deletePx, 0f)
                            }
                        }
                    }
                },
        ) {
            content()
        }
    }
}

@Composable
private fun QuantityField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val shape = RoundedCornerShape(dimensionResource(R.dimen.skill_plan_dialog_field_corner))
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboard?.hide()
                focusManager.clearFocus(force = true)
            },
        ),
        textStyle = TextStyle(
            color = colorResource(R.color.text_primary),
            fontSize = dimensionResource(R.dimen.sub_menu_value_text_size).value.sp,
            textAlign = TextAlign.End,
        ),
        cursorBrush = SolidColor(colorResource(R.color.hyperlink_text)),
        modifier = Modifier
            .width(dimensionResource(R.dimen.market_watchlist_quantity_field_width))
            .height(dimensionResource(R.dimen.market_watchlist_quantity_field_height))
            .clip(shape)
            .background(colorResource(R.color.search_field_background))
            .padding(horizontal = dimensionResource(R.dimen.skill_plan_dialog_field_horizontal_padding)),
        decorationBox = { inner ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                inner()
            }
        },
    )
}

@Composable
private fun OrderSideToggle(
    side: MarketWatchOrderSide,
    onSelect: (MarketWatchOrderSide) -> Unit,
) {
    val shape = RoundedCornerShape(percent = 50)
    val inset = dimensionResource(R.dimen.segmented_control_bar_horizontal_padding)
    val sellLabel = stringResource(R.string.type_detail_market_sell_column_title)
    val buyLabel = stringResource(R.string.type_detail_market_buy_column_title)
    SubcomposeLayout(
        modifier = Modifier
            .clip(shape)
            .background(colorResource(R.color.search_field_idle_background), shape),
    ) { constraints ->
        val loose = Constraints(maxWidth = constraints.maxWidth)
        val natural = subcompose(OrderSideMeasurePass.Natural) {
            OrderSideChip(
                label = sellLabel,
                selected = side == MarketWatchOrderSide.SELL,
                onClick = { onSelect(MarketWatchOrderSide.SELL) },
            )
            OrderSideChip(
                label = buyLabel,
                selected = side == MarketWatchOrderSide.BUY,
                onClick = { onSelect(MarketWatchOrderSide.BUY) },
            )
        }.map { it.measure(loose) }
        val insetPx = inset.roundToPx()
        val targetWidth = ((natural.sumOf { it.width } + insetPx * 2) * MarketWatchlistConfig.ORDER_SIDE_WIDTH_SCALE)
            .roundToInt()
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val innerWidth = (targetWidth - insetPx * 2).coerceAtLeast(0)
        val optionWidth = innerWidth / natural.size
        val remainder = innerWidth - optionWidth * natural.size
        val placed = subcompose(OrderSideMeasurePass.Equal) {
            OrderSideChip(
                label = sellLabel,
                selected = side == MarketWatchOrderSide.SELL,
                onClick = { onSelect(MarketWatchOrderSide.SELL) },
            )
            OrderSideChip(
                label = buyLabel,
                selected = side == MarketWatchOrderSide.BUY,
                onClick = { onSelect(MarketWatchOrderSide.BUY) },
            )
        }.mapIndexed { index, measurable ->
            val width = optionWidth + if (index == natural.lastIndex) remainder else 0
            measurable.measure(Constraints(minWidth = width, maxWidth = width))
        }
        val height = (placed.maxOf { it.height } + insetPx * 2)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(targetWidth, height) {
            var x = insetPx
            placed.forEach { placeable ->
                placeable.placeRelative(x, insetPx)
                x += placeable.width
            }
        }
    }
}

private enum class OrderSideMeasurePass {
    Natural,
    Equal,
}

@Composable
private fun OrderSideChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(percent = 50)
    Text(
        text = label,
        color = colorResource(if (selected) R.color.text_primary else R.color.hint_text),
        fontSize = dimensionResource(R.dimen.segmented_control_text_size).value.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(colorResource(R.color.segmented_control_selected), shape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.segmented_control_bar_horizontal_padding),
                vertical = dimensionResource(R.dimen.segmented_control_bar_vertical_padding),
            ),
    )
}

@Composable
private fun marketPriceText(
    ui: MarketWatchlistDetailUi,
    placeholder: String,
): String = when {
    ui.items.isEmpty() -> placeholder
    ui.loadingPrices && ui.marketPrice == null -> stringResource(R.string.market_updating)
    ui.priceFailed && ui.marketPrice == null -> stringResource(R.string.market_load_failed)
    else -> compactPrice(ui.marketPrice, placeholder)
}

@Composable
private fun compactPrice(value: Double?, placeholder: String): String {
    if (value == null) return placeholder
    val compact = NumberDisplayFormatter.format(value, NumberDisplayFormatter.Style.COMPACT)
    return stringResource(R.string.market_price_isk, compact)
}

@Composable
private fun fullPrice(value: Double?, placeholder: String): String {
    if (value == null) return placeholder
    val full = NumberDisplayFormatter.format(value, NumberDisplayFormatter.Style.FULL)
    return stringResource(R.string.market_price_isk, full)
}

@Composable
private fun volumeText(value: Double?, placeholder: String): String {
    if (value == null) return placeholder
    val full = NumberDisplayFormatter.format(value, NumberDisplayFormatter.Style.FULL)
    return stringResource(R.string.market_watchlist_volume_value, full)
}

@Composable
private fun WatchlistImportModeDialog(
    onOverwrite: () -> Unit,
    onAppend: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(MarketWatchlistConfig.DIALOG_WIDTH_FRACTION),
            shape = RoundedCornerShape(dimensionResource(R.dimen.skill_plan_dialog_corner)),
            color = colorResource(R.color.main_background),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.skill_plan_dialog_content_padding)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.market_watchlist_import_mode),
                    color = colorResource(R.color.text_primary),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                ImportModeButton(
                    label = stringResource(R.string.market_watchlist_import_overwrite),
                    onClick = onOverwrite,
                )
                ImportModeButton(
                    label = stringResource(R.string.market_watchlist_import_append),
                    onClick = onAppend,
                )
                Text(
                    text = stringResource(R.string.market_cancel),
                    color = colorResource(R.color.hint_text),
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(top = dimensionResource(R.dimen.skill_plan_dialog_content_padding))
                        .clickable(onClick = onDismiss),
                )
            }
        }
    }
}

@Composable
private fun ImportModeButton(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = colorResource(R.color.hyperlink_text),
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.skill_plan_dialog_content_padding))
            .clickable(onClick = onClick),
    )
}

@Composable
private fun WatchlistMessageDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(dimensionResource(R.dimen.skill_plan_dialog_corner)),
            color = colorResource(R.color.main_background),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.skill_plan_dialog_content_padding)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = message,
                    color = colorResource(R.color.text_primary),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.market_watchlist_dialog_ok),
                    color = colorResource(R.color.hyperlink_text),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(top = dimensionResource(R.dimen.skill_plan_dialog_content_padding))
                        .clickable(onClick = onDismiss),
                )
            }
        }
    }
}

private suspend fun readClipboard(
    clipboard: androidx.compose.ui.platform.Clipboard,
    context: android.content.Context,
): String? {
    val clipEntry = clipboard.getClipEntry() ?: return null
    val data = clipEntry.clipData
    if (data.itemCount <= 0) return null
    return buildString {
        for (index in 0 until data.itemCount) {
            val text = data.getItemAt(index)?.coerceToText(context)?.toString()?.trim().orEmpty()
            if (text.isEmpty()) continue
            if (isNotEmpty()) append('\n')
            append(text)
        }
    }.ifEmpty { null }
}
