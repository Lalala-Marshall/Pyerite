package com.marshall.pyerite.agentsModule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.agentsModule.data.agentsSectionLetter
import com.marshall.pyerite.agentsModule.data.matchesQuery
import com.marshall.pyerite.agentsModule.model.AgentSystemOption
import com.marshall.pyerite.agentsModule.navHost.rememberAgentsRootBackStackEntry
import com.marshall.pyerite.agentsModule.viewModel.AgentsViewModel
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.rememberLazyListTitleCollapsed
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun AgentsRegionPage(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    viewModel: AgentsViewModel = koinViewModel(
        viewModelStoreOwner = rememberAgentsRootBackStackEntry(navController, backStackEntry),
    ),
    localeController: LocaleController = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    val language = localeController.contentLanguage
    val filtered = uiState.regions.filter { it.matchesQuery(query) }
    val sections = filtered
        .groupBy { agentsSectionLetter(it.name, language) }
        .toSortedMap()
    AgentsPlacePage(
        title = stringResource(R.string.agents_pick_region),
        hint = stringResource(R.string.agents_search_region),
        query = query,
        onQueryChange = { query = it },
        navController = navController,
    ) {
        item {
            PlaceAllRow(
                label = stringResource(R.string.agents_all_regions),
                selected = uiState.filter.regionId == null,
                showDivider = sections.isNotEmpty(),
                onClick = {
                    viewModel.setRegion(null)
                    navController.navigateUp()
                },
            )
        }
        sections.forEach { (letter, regions) ->
            item(key = "region-header-$letter") {
                PlaceSectionHeader(letter)
            }
            items(regions, key = { "region-${it.id}" }) { region ->
                PlaceChoiceRow(
                    label = region.name,
                    selected = uiState.filter.regionId == region.id,
                    showDivider = region != regions.last(),
                    onClick = {
                        viewModel.setRegion(region.id)
                        navController.navigateUp()
                    },
                )
            }
        }
    }
}

@Composable
internal fun AgentsSystemPage(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    viewModel: AgentsViewModel = koinViewModel(
        viewModelStoreOwner = rememberAgentsRootBackStackEntry(navController, backStackEntry),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    val regionId = uiState.filter.regionId
    val systems = uiState.systems
        .filter { regionId == null || it.regionId == regionId }
        .filter { it.matchesQuery(query) }
    val allLabel = if (regionId == null) {
        stringResource(R.string.agents_all_systems)
    } else {
        stringResource(R.string.agents_all_systems_in_region)
    }
    AgentsPlacePage(
        title = stringResource(R.string.agents_pick_system),
        hint = stringResource(R.string.agents_search_system),
        query = query,
        onQueryChange = { query = it },
        navController = navController,
    ) {
        item {
            PlaceAllRow(
                label = allLabel,
                selected = uiState.filter.solarSystemId == null,
                showDivider = systems.isNotEmpty(),
                onClick = {
                    viewModel.setSolarSystem(null)
                    navController.navigateUp()
                },
            )
        }
        items(systems, key = { "system-${it.id}" }) { system ->
            SystemChoiceRow(
                system = system,
                selected = uiState.filter.solarSystemId == system.id,
                showDivider = system != systems.last(),
                onClick = {
                    viewModel.setSolarSystem(system.id)
                    navController.navigateUp()
                },
            )
        }
    }
}

@Composable
private fun AgentsPlacePage(
    title: String,
    hint: String,
    query: String,
    onQueryChange: (String) -> Unit,
    navController: NavController,
    rows: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()
    val onBack = navController.rememberNavigateUpAction()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    PyeritePageScaffold(
        title = title,
        showCollapsedTitle = rememberLazyListTitleCollapsed(listState),
        onBack = onBack,
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
                item { PageTitle(text = title) }
                rows()
            }
            AgentsPlaceSearchBar(
                hint = hint,
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensionResource(R.dimen.search_bar_vertical_padding)),
            )
        }
    }
}

@Composable
private fun AgentsPlaceSearchBar(
    hint: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barHeight = dimensionResource(R.dimen.search_bar_height)
    val corner = dimensionResource(R.dimen.detail_card_corner_radius)
    val iconSize = dimensionResource(R.dimen.detail_row_chevron_size)
    val fieldBackground = colorResource(R.color.search_field_background)
    val textColor = colorResource(R.color.text_primary)
    val hintColor = colorResource(R.color.hint_text)
    val textSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp
    Box(
        modifier = modifier.padding(horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding)),
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(corner))
                .background(fieldBackground),
            textStyle = TextStyle(color = textColor, fontSize = textSize),
            singleLine = true,
            cursorBrush = SolidColor(textColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = dimensionResource(R.dimen.search_bar_cancel_gap)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = hintColor,
                        modifier = Modifier.size(iconSize),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = dimensionResource(R.dimen.search_bar_cancel_gap)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(text = hint, color = hintColor, fontSize = textSize)
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(dimensionResource(R.dimen.top_bar_back_button_size))
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onQueryChange("") },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.search_clear),
                                tint = colorResource(R.color.search_clear_icon),
                                modifier = Modifier.size(iconSize),
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun PlaceSectionHeader(letter: String) {
    Text(
        text = letter,
        color = colorResource(R.color.text_primary),
        fontSize = dimensionResource(R.dimen.list_section_header_text_size).value.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(
            start = dimensionResource(R.dimen.type_detail_page_title_start_padding),
            top = dimensionResource(R.dimen.type_detail_section_gap),
            bottom = dimensionResource(R.dimen.list_section_header_bottom_padding),
        ),
    )
}

@Composable
private fun PlaceAllRow(
    label: String,
    selected: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    PlaceChoiceRow(label = label, selected = selected, showDivider = showDivider, onClick = onClick)
}

@Composable
private fun PlaceChoiceRow(
    label: String,
    selected: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = label,
            showChevron = false,
            onClick = onClick,
        ),
        showDivider = showDivider,
        trailingContent = if (selected) {
            { SelectedMark() }
        } else {
            null
        },
    )
}

@Composable
private fun SystemChoiceRow(
    system: AgentSystemOption,
    selected: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemNameAnnotated = agentsPlaceLabel(system.security, system.name),
            itemName = system.name,
            showChevron = false,
            onClick = onClick,
        ),
        showDivider = showDivider,
        trailingContent = if (selected) {
            { SelectedMark() }
        } else {
            null
        },
    )
}

@Composable
private fun SelectedMark() {
    Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = stringResource(R.string.agents_selected),
        tint = colorResource(R.color.hyperlink_text),
    )
}
