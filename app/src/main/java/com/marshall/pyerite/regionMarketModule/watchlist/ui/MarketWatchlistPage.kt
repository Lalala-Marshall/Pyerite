package com.marshall.pyerite.regionMarketModule.watchlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.iconModule.manager.IconManager
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.regionMarketModule.navHost.RegionMarketRoute
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistConfig
import com.marshall.pyerite.regionMarketModule.watchlist.viewModel.MarketWatchlistRowUi
import com.marshall.pyerite.regionMarketModule.watchlist.viewModel.MarketWatchlistViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.rememberLazyListTitleCollapsed
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.search.SearchNoResultsItem
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun MarketWatchlistPage(
    navController: NavController,
    viewModel: MarketWatchlistViewModel = koinViewModel(),
) {
    val rows by viewModel.rows.collectAsState()
    val localeController: LocaleController = koinInject()
    val iconManager: IconManager = koinInject()
    val listState = rememberLazyListState()
    val pageTitle = stringResource(R.string.market_watchlist)
    val onBack = navController.rememberNavigateUpAction()
    val showCollapsedTitle = rememberLazyListTitleCollapsed(listState)
    var query by rememberSaveable { mutableStateOf("") }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var menuListId by remember { mutableStateOf<String?>(null) }
    var renameRow by remember { mutableStateOf<MarketWatchlistRowUi?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val searchPlaceholder = stringResource(R.string.market_watchlist_search_title)
    val visible = if (query.isBlank()) {
        rows
    } else {
        rows.filter { it.title.contains(query, ignoreCase = true) }
    }

    LaunchedEffect(localeController.contentLanguage) {
        viewModel.onContentLanguage(localeController.contentLanguage)
    }

    if (showAddDialog) {
        MarketWatchlistAddDialog(
            heading = stringResource(R.string.market_watchlist_add),
            onDismiss = { showAddDialog = false },
            onConfirm = { title ->
                val id = viewModel.create(title)
                showAddDialog = false
                navController.navigate(RegionMarketRoute.WatchlistDetail.create(id))
            },
        )
    }
    renameRow?.let { row ->
        MarketWatchlistAddDialog(
            heading = stringResource(R.string.market_watchlist_rename),
            initialTitle = row.title,
            onDismiss = { renameRow = null },
            onConfirm = { title ->
                viewModel.rename(row.id, title)
                renameRow = null
            },
        )
    }

    PyeritePageScaffold(
        title = pageTitle,
        showCollapsedTitle = showCollapsedTitle,
        onBack = onBack,
        endActions = listOf(
            PyeriteTopBarActionItem(
                onClick = { showAddDialog = true },
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.market_watchlist_add),
            ),
        ),
    ) { topBarPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(topBarPadding)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                item(key = "title") { PageTitle(text = pageTitle) }
                when {
                    query.isNotBlank() && visible.isEmpty() -> {
                        item(key = "no_results") { SearchNoResultsItem() }
                    }
                    visible.isEmpty() -> {
                        item(key = "empty") {
                            BaseContainer(title = null, useSystemBarsPadding = false) {
                                BaseLazyColumnItem(
                                    model = BaseLazyColumnItemModel(
                                        showLeadingIcon = false,
                                        itemName = stringResource(R.string.market_watchlist_empty),
                                        showChevron = false,
                                        onClick = null,
                                    ),
                                    showDivider = false,
                                )
                            }
                        }
                    }
                    else -> {
                        item(key = "lists") {
                            BaseContainer(title = null, useSystemBarsPadding = false) {
                                visible.forEachIndexed { index, row ->
                                    val iconFile = row.iconFileName?.let(iconManager::getIconFile)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        BaseLazyColumnItem(
                                            model = BaseLazyColumnItemModel(
                                                iconRes = R.drawable.ic_market_watchlist,
                                                iconFile = iconFile,
                                                iconTint = if (iconFile == null) {
                                                    Color.Unspecified
                                                } else {
                                                    null
                                                },
                                                itemName = row.title,
                                                itemHint = row.locationName,
                                                onClick = {
                                                    navController.navigate(
                                                        RegionMarketRoute.WatchlistDetail.create(row.id),
                                                    )
                                                },
                                                onLongClick = { menuListId = row.id },
                                            ),
                                            showDivider = index != visible.lastIndex,
                                        )
                                        Box(modifier = Modifier.matchParentSize()) {
                                            WatchlistRowMenu(
                                                expanded = menuListId == row.id,
                                                onDismiss = { menuListId = null },
                                                onRename = {
                                                    menuListId = null
                                                    renameRow = row
                                                },
                                                onDelete = {
                                                    menuListId = null
                                                    viewModel.delete(row.id)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            MarketWatchlistBottomSearchBar(
                query = query,
                onQueryChange = { query = it },
                placeholder = searchPlaceholder,
                onSearch = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                },
                onClearQuery = { query = "" },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensionResource(R.dimen.search_bar_vertical_padding)),
            )
        }
    }
}

@Composable
private fun WatchlistRowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    if (!expanded) return
    Popup(
        popupPositionProvider = HorizontallyCenteredPopupPositionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .width(dimensionResource(R.dimen.top_bar_dropdown_min_width))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.top_bar_dropdown_corner)))
                .background(colorResource(R.color.second_background)),
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.market_watchlist_rename),
                        color = colorResource(R.color.text_primary),
                    )
                },
                onClick = onRename,
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.market_watchlist_delete_list),
                        color = colorResource(R.color.text_primary),
                    )
                },
                onClick = onDelete,
            )
        }
    }
}

private object HorizontallyCenteredPopupPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = ((windowSize.width - popupContentSize.width) / 2).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        val y = anchorBounds.bottom.coerceIn(0, maxY)
        return IntOffset(x, y)
    }
}

@Composable
private fun MarketWatchlistAddDialog(
    heading: String,
    initialTitle: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    val canConfirm = title.isNotBlank()
    val nameLabel = stringResource(R.string.market_watchlist_name)
    val primaryText = colorResource(R.color.text_primary)
    val hintText = colorResource(R.color.hint_text)
    val disabledColor = colorResource(R.color.text_caption)
    val controlBackground = colorResource(R.color.skill_plan_dialog_button_background)
    val cursorColor = colorResource(R.color.hyperlink_text)
    val contentPadding = dimensionResource(R.dimen.skill_plan_dialog_content_padding)
    val labelFieldGap = dimensionResource(R.dimen.skill_plan_dialog_label_field_gap)
    val buttonGap = dimensionResource(R.dimen.skill_plan_dialog_button_gap)
    val fieldHeight = dimensionResource(R.dimen.skill_plan_dialog_field_height)
    val fieldCorner = dimensionResource(R.dimen.skill_plan_dialog_field_corner)
    val fieldHorizontalPadding = dimensionResource(R.dimen.skill_plan_dialog_field_horizontal_padding)
    val buttonCorner = dimensionResource(R.dimen.skill_plan_dialog_button_corner)
    val buttonHorizontalPadding = dimensionResource(R.dimen.skill_plan_dialog_button_horizontal_padding)
    val dialogCorner = dimensionResource(R.dimen.skill_plan_dialog_corner)
    val fieldShape = RoundedCornerShape(fieldCorner)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(MarketWatchlistConfig.DIALOG_WIDTH_FRACTION),
            shape = RoundedCornerShape(dialogCorner),
            color = colorResource(R.color.main_background),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
            ) {
                Text(
                    text = heading,
                    color = primaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = nameLabel,
                    color = primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = contentPadding),
                )
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = labelFieldGap)
                        .height(fieldHeight)
                        .clip(fieldShape)
                        .background(controlBackground)
                        .padding(horizontal = fieldHorizontalPadding),
                    singleLine = true,
                    textStyle = TextStyle(color = primaryText, fontSize = 14.sp),
                    cursorBrush = SolidColor(cursorColor),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                            if (title.isEmpty()) {
                                Text(text = nameLabel, color = hintText, fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    },
                )
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = contentPadding),
                    horizontalArrangement = Arrangement.spacedBy(buttonGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DialogActionButton(
                        label = stringResource(R.string.market_cancel),
                        labelColor = primaryText,
                        backgroundColor = controlBackground,
                        corner = buttonCorner,
                        horizontalPadding = buttonHorizontalPadding,
                        enabled = true,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    DialogActionButton(
                        label = stringResource(R.string.market_done),
                        labelColor = if (canConfirm) primaryText else disabledColor,
                        backgroundColor = controlBackground,
                        corner = buttonCorner,
                        horizontalPadding = buttonHorizontalPadding,
                        enabled = canConfirm,
                        onClick = {
                            val trimmed = title.trim()
                            if (trimmed.isNotEmpty()) onConfirm(trimmed)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogActionButton(
    label: String,
    labelColor: Color,
    backgroundColor: Color,
    corner: Dp,
    horizontalPadding: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = horizontalPadding, vertical = dimensionResource(R.dimen.skill_plan_dialog_button_gap)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = labelColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}
