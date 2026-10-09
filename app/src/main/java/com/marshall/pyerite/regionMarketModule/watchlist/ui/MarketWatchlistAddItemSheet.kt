package com.marshall.pyerite.regionMarketModule.watchlist.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R
import com.marshall.pyerite.iconModule.manager.IconManager
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.regionMarketModule.ui.MarketSectionItem
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistConfig
import com.marshall.pyerite.regionMarketModule.watchlist.viewModel.MarketWatchlistAddItemViewModel
import com.marshall.pyerite.regionMarketModule.watchlist.viewModel.MarketWatchlistPickerLevel
import com.marshall.pyerite.sdeModule.room.type.TypeEntity
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.search.SearchNoResultsItem
import com.marshall.pyerite.ui.golbalComponents.topBarActionSurface
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarketWatchlistAddItemSheet(
    onDismiss: (List<Int>) -> Unit,
    viewModel: MarketWatchlistAddItemViewModel = koinViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val iconManager: IconManager = koinInject()
    val localeController: LocaleController = koinInject()
    val sheetCorner = dimensionResource(R.dimen.skill_plan_add_skill_sheet_corner)
    val sheetBackground = colorResource(R.color.search_field_idle_background)
    val rootTitle = stringResource(R.string.region_market)
    val title = ui.title.ifBlank { rootTitle }
    val selected = ui.selectedTypeIds.toSet()

    fun dismiss() {
        scope.launch {
            sheetState.hide()
            onDismiss(viewModel.consumeSelection())
        }
    }

    BackHandler(enabled = ui.canGoBack) { viewModel.pop() }

    ModalBottomSheet(
        onDismissRequest = { onDismiss(viewModel.consumeSelection()) },
        sheetState = sheetState,
        sheetGesturesEnabled = true,
        shape = RoundedCornerShape(topStart = sheetCorner, topEnd = sheetCorner),
        containerColor = sheetBackground,
        scrimColor = colorResource(R.color.search_scrim),
        tonalElevation = 0.dp,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(MarketWatchlistConfig.SHEET_HEIGHT_FRACTION)
                .background(sheetBackground)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            PickerHeader(
                title = title,
                showBack = ui.canGoBack,
                onBack = viewModel::pop,
                onClose = ::dismiss,
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (ui.level is MarketWatchlistPickerLevel.Groups) {
                    if (ui.groups.isEmpty() && ui.searchTypes.isEmpty() && ui.query.isNotBlank()) {
                        item(key = "no_results") { SearchNoResultsItem() }
                    }
                    items(items = ui.groups, key = { "group:${it.id}" }) { group ->
                        val index = ui.groups.indexOf(group)
                        MarketSectionItem(
                            model = BaseLazyColumnItemModel(
                                iconFile = iconManager.getIconFile(group.iconName),
                                itemName = group.name.orEmpty(),
                                onClick = {
                                    viewModel.openGroup(
                                        groupId = group.id,
                                        hasChildren = group.id in ui.childIdsWithChildren,
                                    )
                                },
                            ),
                            indexInSection = index,
                            sectionItemCount = ui.groups.size,
                            showDivider = index < ui.groups.lastIndex,
                        )
                    }
                    if (ui.searchTypes.isNotEmpty()) {
                        item(key = "search_types_header") {
                            PickerSectionTitle(
                                title = stringResource(R.string.type),
                                addTopGap = ui.groups.isNotEmpty(),
                            )
                        }
                        items(items = ui.searchTypes, key = { "search:${it.id}" }) { type ->
                            val index = ui.searchTypes.indexOf(type)
                            TypeCheckRow(
                                type = type,
                                localeController = localeController,
                                iconManager = iconManager,
                                selected = type.id in selected,
                                index = index,
                                count = ui.searchTypes.size,
                                showDivider = index < ui.searchTypes.lastIndex,
                                onClick = { viewModel.toggleType(type.id) },
                            )
                        }
                    }
                } else {
                    if (ui.sections.isEmpty() && ui.query.isNotBlank()) {
                        item(key = "types_empty") { SearchNoResultsItem() }
                    }
                    ui.sections.forEachIndexed { sectionIndex, section ->
                        item(key = "header:${section.id}") {
                            val header = when {
                                section.unpublished -> stringResource(R.string.unpublished)
                                section.title.isBlank() -> stringResource(R.string.market_ungrouped)
                                else -> section.title
                            }
                            PickerSectionTitle(
                                title = header,
                                addTopGap = sectionIndex > 0,
                                trailing = {
                                    WatchlistSelectAllButton(
                                        allSelected = section.types.isNotEmpty() &&
                                            section.types.all { it.id in selected },
                                        onClick = { viewModel.toggleSection(section.types.map { it.id }) },
                                    )
                                },
                            )
                        }
                        items(items = section.types, key = { "type:${section.id}:${it.id}" }) { type ->
                            val index = section.types.indexOf(type)
                            TypeCheckRow(
                                type = type,
                                localeController = localeController,
                                iconManager = iconManager,
                                selected = type.id in selected,
                                index = index,
                                count = section.types.size,
                                showDivider = index < section.types.lastIndex,
                                onClick = { viewModel.toggleType(type.id) },
                            )
                        }
                    }
                }
            }
            MarketWatchlistBottomSearchBar(
                query = ui.query,
                onQueryChange = viewModel::setQuery,
                placeholder = stringResource(R.string.market_watchlist_search_items),
                onSearch = {},
                onClearQuery = { viewModel.setQuery("") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensionResource(R.dimen.search_bar_vertical_padding)),
            )
        }
    }
}

@Composable
private fun PickerHeader(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val buttonHeight = dimensionResource(R.dimen.top_bar_back_button_size)
    val pillShape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.skill_plan_add_skill_sheet_horizontal_padding),
                end = dimensionResource(R.dimen.skill_plan_add_skill_sheet_horizontal_padding),
                top = dimensionResource(R.dimen.skill_plan_add_skill_header_top_padding),
                bottom = dimensionResource(R.dimen.skill_plan_add_skill_search_top_gap),
            ),
    ) {
        if (showBack) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(buttonHeight)
                    .topBarActionSurface(pillShape)
                    .clickable(onClick = onBack)
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back),
                    tint = colorResource(R.color.text_primary),
                    modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
                )
            }
        }
        Text(
            text = title,
            color = colorResource(R.color.text_primary),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = buttonHeight),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(buttonHeight)
                .topBarActionSurface(pillShape)
                .clickable(onClick = onClose)
                .semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.market_watchlist_close),
                tint = colorResource(R.color.text_primary),
                modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
            )
        }
    }
}

@Composable
private fun PickerSectionTitle(
    title: String,
    addTopGap: Boolean,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.type_detail_page_title_start_padding),
                end = dimensionResource(R.dimen.detail_card_horizontal_padding),
                bottom = dimensionResource(R.dimen.list_section_header_bottom_padding),
                top = if (addTopGap) dimensionResource(R.dimen.type_detail_section_gap) else 0.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = dimensionResource(R.dimen.list_section_header_text_size).value.sp,
            fontWeight = FontWeight.Black,
            color = colorResource(R.color.text_primary),
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
private fun TypeCheckRow(
    type: TypeEntity,
    localeController: LocaleController,
    iconManager: IconManager,
    selected: Boolean,
    index: Int,
    count: Int,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    MarketSectionItem(
        model = BaseLazyColumnItemModel(
            iconFile = iconManager.getIconFile(type.iconFilename),
            itemName = type.displayName(localeController),
            showChevron = false,
            onClick = onClick,
        ),
        indexInSection = index,
        sectionItemCount = count,
        showDivider = showDivider,
        trailingContent = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colorResource(R.color.hyperlink_text),
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun WatchlistSelectAllButton(
    allSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics { role = Role.Checkbox },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = stringResource(R.string.corporation_wallet_filter_select_all),
            color = colorResource(R.color.text_primary),
            fontSize = dimensionResource(R.dimen.list_section_subheader_text_size).value.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.corporation_wallet_select_all_gap)))
        SelectAllMark(selected = allSelected)
    }
}

@Composable
private fun SelectAllMark(selected: Boolean) {
    val checkSize = dimensionResource(R.dimen.corporation_wallet_select_all_check_size)
    val iconSize = dimensionResource(R.dimen.corporation_wallet_select_all_icon_size)
    val borderWidth = dimensionResource(R.dimen.corporation_wallet_select_all_border)
    Box(
        modifier = Modifier
            .size(checkSize)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(colorResource(R.color.hyperlink_text))
                } else {
                    Modifier.border(
                        width = borderWidth,
                        color = colorResource(R.color.hint_text),
                        shape = CircleShape,
                    )
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = colorResource(R.color.white),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
