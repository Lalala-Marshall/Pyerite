package com.marshall.pyerite.regionMarketModule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.ui.golbalComponents.topBarActionSurface
import com.marshall.pyerite.R
import com.marshall.pyerite.esiModule.data.portraitUrl
import com.marshall.pyerite.eveAuthModule.model.EveSessionIdentity
import com.marshall.pyerite.iconModule.manager.IconManager
import com.marshall.pyerite.regionMarketModule.model.MarketPlaceName
import com.marshall.pyerite.regionMarketModule.model.MarketSelection
import com.marshall.pyerite.regionMarketModule.model.MarketStructureHit
import com.marshall.pyerite.regionMarketModule.model.MarketSystemOption
import com.marshall.pyerite.regionMarketModule.model.SavedMarketStructure
import com.marshall.pyerite.regionMarketModule.viewModel.MarketLocationViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.ItemDivider
import org.koin.compose.koinInject

private enum class MarketSheetPage {
    Picker,
    AddStructure,
}

private const val SHEET_HEIGHT_FRACTION = 0.94f
private const val DRAG_STEP_PX = 72f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarketLocationSheet(
    viewModel: MarketLocationViewModel,
    onDismiss: () -> Unit,
    highlightedSelection: MarketSelection? = null,
    onPlaceSelected: ((MarketSelection) -> Unit)? = null,
) {
    val ui by viewModel.ui.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var page by rememberSaveable { mutableStateOf(MarketSheetPage.Picker) }
    val iconManager: IconManager = koinInject()
    val sheetCorner = dimensionResource(R.dimen.skill_plan_add_skill_sheet_corner)
    val sheetBackground = colorResource(R.color.search_field_idle_background)

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.setEditing(false)
            onDismiss()
        },
        sheetState = sheetState,
        sheetGesturesEnabled = page == MarketSheetPage.Picker && !ui.editing,
        shape = RoundedCornerShape(topStart = sheetCorner, topEnd = sheetCorner),
        containerColor = sheetBackground,
        scrimColor = colorResource(R.color.search_scrim),
        tonalElevation = 0.dp,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(SHEET_HEIGHT_FRACTION)
                .background(sheetBackground)
                .navigationBarsPadding(),
        ) {
        when (page) {
            MarketSheetPage.Picker -> PickerPage(
                viewModel = viewModel,
                editing = ui.editing,
                onDone = { viewModel.setEditing(false) },
                onAddStructure = {
                    viewModel.refreshCharacters()
                    viewModel.clearStructureSearch()
                    page = MarketSheetPage.AddStructure
                },
                onSelect = { selection ->
                    if (onPlaceSelected != null) {
                        onPlaceSelected(selection)
                    } else {
                        viewModel.select(selection)
                    }
                    onDismiss()
                },
                highlightedSelection = highlightedSelection,
                iconManager = iconManager,
            )
            MarketSheetPage.AddStructure -> AddStructurePage(
                viewModel = viewModel,
                onBack = { page = MarketSheetPage.Picker },
            )
        }
        }
    }
}

@Composable
private fun PickerPage(
    viewModel: MarketLocationViewModel,
    editing: Boolean,
    onDone: () -> Unit,
    onAddStructure: () -> Unit,
    onSelect: (MarketSelection) -> Unit,
    highlightedSelection: MarketSelection?,
    iconManager: IconManager,
) {
    val ui by viewModel.ui.collectAsState()
    val selectedPlace = highlightedSelection ?: ui.selected
    val query = ui.query
    val filteredSystems = ui.majorSystems.filter { system ->
        query.isBlank() || viewModel.placeName(system.systemName, system.systemZhName, system.systemEnName)
            .contains(query, ignoreCase = true)
    }
    val filteredStructures = ui.structures.filter { structure ->
        query.isBlank() || structure.name.contains(query, ignoreCase = true)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.skill_plan_add_skill_sheet_horizontal_padding),
                end = dimensionResource(R.dimen.skill_plan_add_skill_sheet_horizontal_padding),
                top = dimensionResource(R.dimen.skill_plan_add_skill_header_top_padding),
            ),
        ) {
            MarketSheetHeader(
                title = stringResource(R.string.market_select_region),
                showBack = false,
                showDone = editing,
                onBack = {},
                onDone = onDone,
            )
            MarketSheetSearchField(
                query = query,
                onQueryChange = viewModel::setQuery,
                placeholder = stringResource(R.string.market_search_region),
                modifier = Modifier.padding(
                    top = dimensionResource(R.dimen.skill_plan_add_skill_search_top_gap),
                    bottom = dimensionResource(R.dimen.skill_plan_add_skill_search_bottom_gap),
                ),
            )
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            item(key = "pinned_section") {
                MarketSectionHeader(title = stringResource(R.string.market_pinned_locations), addTopGap = true)
                if (ui.pinned.isNotEmpty() || !editing) {
                    MarketSheetCard {
                    ui.pinned.forEachIndexed { index, place ->
                        val label = viewModel.label(place)
                        PlaceRow(
                            title = label.title,
                            subtitle = label.subtitle,
                            security = label.security,
                            iconFileName = label.iconFileName,
                            iconManager = iconManager,
                            selected = selectedPlace.persistKey == place.persistKey,
                            editing = editing,
                            pinned = true,
                            onClick = { onSelect(place) },
                            onPin = {},
                            onUnpin = { viewModel.unpin(place) },
                            onDrag = { deltaSteps ->
                                val target = (index + deltaSteps).coerceIn(0, ui.pinned.lastIndex)
                                if (target != index) viewModel.movePinned(index, target)
                            },
                        )
                        if (index < ui.pinned.lastIndex || !editing) {
                            ItemDivider(
                                leadingIconSize = if (label.iconFileName != null) {
                                    dimensionResource(R.dimen.base_lazy_column_item_icon_size)
                                } else {
                                    0.dp
                                },
                            )
                        }
                    }
                    if (!editing) {
                        BaseLazyColumnItem(
                            model = BaseLazyColumnItemModel(
                                showLeadingIcon = false,
                                itemName = stringResource(R.string.market_add_location),
                                itemNameColor = colorResource(R.color.hyperlink_text),
                                showChevron = false,
                                onClick = { viewModel.setEditing(true) },
                            ),
                            showDivider = false,
                        )
                    }
                    }
                }
            }
            item(key = "structures_section") {
                MarketSectionHeader(title = stringResource(R.string.market_select_structure), addTopGap = true)
                MarketSheetCard {
                    if (filteredStructures.isEmpty()) {
                        BaseLazyColumnItem(
                            model = BaseLazyColumnItemModel(
                                showLeadingIcon = false,
                                itemName = stringResource(
                                    if (ui.structures.isEmpty()) {
                                        R.string.market_setup_structure
                                    } else {
                                        R.string.market_add_structure
                                    },
                                ),
                                showChevron = true,
                                onClick = onAddStructure,
                            ),
                            showDivider = false,
                        )
                    } else {
                        filteredStructures.forEachIndexed { index, structure ->
                            StructureRow(
                                structure = structure,
                                systemName = viewModel.systemName(structure.systemId),
                                iconManager = iconManager,
                                selected = (selectedPlace as? MarketSelection.Structure)?.structureId ==
                                    structure.structureId,
                                editing = editing,
                                pinned = ui.pinned.any {
                                    (it as? MarketSelection.Structure)?.structureId == structure.structureId
                                },
                                onClick = { onSelect(MarketSelection.Structure(structure.structureId)) },
                                onPin = { viewModel.pin(MarketSelection.Structure(structure.structureId)) },
                                onUnpin = { viewModel.unpin(MarketSelection.Structure(structure.structureId)) },
                            )
                            if (index < filteredStructures.lastIndex) ItemDivider()
                        }
                        ItemDivider()
                        BaseLazyColumnItem(
                            model = BaseLazyColumnItemModel(
                                showLeadingIcon = false,
                                itemName = stringResource(R.string.market_add_structure),
                                showChevron = true,
                                onClick = onAddStructure,
                            ),
                            showDivider = false,
                        )
                    }
                }
            }
            if (filteredSystems.isNotEmpty()) {
                item(key = "hubs_section") {
                    MarketSectionHeader(title = stringResource(R.string.market_major_systems), addTopGap = true)
                    MarketSheetCard {
                        filteredSystems.forEachIndexed { index, system ->
                            SystemRow(
                                system = system,
                                viewModel = viewModel,
                                selected = (selectedPlace as? MarketSelection.System)?.systemId == system.systemId,
                                editing = editing,
                                pinned = ui.pinned.any {
                                    (it as? MarketSelection.System)?.systemId == system.systemId
                                },
                                onClick = { onSelect(MarketSelection.System(system.systemId)) },
                                onPin = { viewModel.pin(MarketSelection.System(system.systemId)) },
                                onUnpin = { viewModel.unpin(MarketSelection.System(system.systemId)) },
                            )
                            if (index < filteredSystems.lastIndex) ItemDivider(leadingIconSize = 0.dp)
                        }
                    }
                }
            }
            ui.regionSections.forEach { section ->
                item(key = "letter:${section.letter}") {
                    MarketSectionHeader(title = section.letter, addTopGap = true)
                    MarketSheetCard {
                        section.regions.forEachIndexed { index, region ->
                            RegionRow(
                                region = region,
                                viewModel = viewModel,
                                selected = (selectedPlace as? MarketSelection.Region)?.regionId == region.regionId,
                                editing = editing,
                                onClick = { onSelect(MarketSelection.Region(region.regionId)) },
                                onPin = { viewModel.pin(MarketSelection.Region(region.regionId)) },
                            )
                            if (index < section.regions.lastIndex) ItemDivider(leadingIconSize = 0.dp)
                        }
                    }
                }
            }
            item(key = "sheet_bottom") { MarketListBottomSpacer() }
        }
    }
}

@Composable
private fun MarketSheetCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.detail_card_corner_radius))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding))
            .clip(shape)
            .background(colorResource(R.color.second_background), shape),
        content = { content() },
    )
}

@Composable
private fun MarketSheetHeader(
    title: String,
    showBack: Boolean,
    showDone: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val buttonHeight = dimensionResource(R.dimen.top_bar_back_button_size)
    val pillShape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(buttonHeight),
    ) {
        if (showBack) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(buttonHeight)
                    .topBarActionSurface(pillShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    )
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
            fontSize = dimensionResource(R.dimen.list_section_header_text_size).value.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = buttonHeight),
        )
        if (showDone) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(buttonHeight)
                    .topBarActionSurface(pillShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDone,
                    )
                    .semantics { role = Role.Button }
                    .padding(horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.character_done),
                    color = colorResource(R.color.text_primary),
                    fontSize = dimensionResource(R.dimen.segmented_control_text_size).value.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun MarketSheetSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val barHeight = dimensionResource(R.dimen.search_bar_height)
    val hintColor = colorResource(R.color.hint_text)
    val textColor = colorResource(R.color.text_primary)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(colorResource(R.color.second_background)),
        singleLine = true,
        textStyle = TextStyle(
            color = textColor,
            fontSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp,
        ),
        cursorBrush = SolidColor(textColor),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = hintColor,
                    modifier = Modifier.size(dimensionResource(R.dimen.top_bar_dropdown_item_icon_size)),
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.top_bar_dropdown_item_icon_gap)))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = hintColor,
                            fontSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp,
                        )
                    }
                    innerTextField()
                }
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.top_bar_dropdown_item_icon_gap)))
                    trailing()
                }
            }
        },
    )
}

@Composable
private fun PlaceRow(
    title: String,
    subtitle: String?,
    security: Double?,
    iconFileName: String?,
    iconManager: IconManager,
    selected: Boolean,
    editing: Boolean,
    pinned: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onDrag: (Int) -> Unit = {},
) {
    var dragAccum by remember { mutableFloatStateOf(0f) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        BaseLazyColumnItem(
            modifier = Modifier.weight(1f),
            model = BaseLazyColumnItemModel(
                showLeadingIcon = iconFileName != null,
                iconFile = iconFileName?.let { iconManager.getIconFile(it) },
                itemName = title,
                itemNameColor = if (selected && !editing) colorResource(R.color.hyperlink_text) else null,
                itemHint = subtitle.orEmpty(),
                showChevron = false,
                onClick = if (editing) null else onClick,
            ),
            showDivider = false,
            titleTrailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (security != null && subtitle.isNullOrBlank()) {
                        Text(
                            text = marketLocationHint(security, "").text,
                            color = marketSecurityColor(security),
                            fontSize = dimensionResource(R.dimen.detail_row_label_subtitle_text_size).value.sp,
                        )
                    }
                    if (editing && pinned) {
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = null,
                            modifier = Modifier.pointerInput(title) {
                                detectVerticalDragGestures(
                                    onDragEnd = { dragAccum = 0f },
                                    onVerticalDrag = { _, amount ->
                                        dragAccum += amount
                                        val steps = (dragAccum / DRAG_STEP_PX).toInt()
                                        if (steps != 0) {
                                            onDrag(steps)
                                            dragAccum = 0f
                                        }
                                    },
                                )
                            },
                        )
                    }
                }
            },
        )
        EditAction(editing = editing, pinned = pinned, selected = selected, onPin = onPin, onUnpin = onUnpin)
    }
}

@Composable
private fun StructureRow(
    structure: SavedMarketStructure,
    systemName: String,
    iconManager: IconManager,
    selected: Boolean,
    editing: Boolean,
    pinned: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
) {
    val hint = marketLocationHint(structure.security, systemName.ifBlank { structure.name })
    Row(verticalAlignment = Alignment.CenterVertically) {
        BaseLazyColumnItem(
            modifier = Modifier.weight(1f),
            model = BaseLazyColumnItemModel(
                iconFile = iconManager.getIconFile(structure.iconFileName),
                itemName = structure.name,
                itemNameColor = if (selected && !editing) colorResource(R.color.hyperlink_text) else null,
                itemHints = listOf(
                    com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemHint(annotatedText = hint),
                ),
                showChevron = false,
                onClick = if (editing) null else onClick,
            ),
            showDivider = false,
        )
        EditAction(editing = editing, pinned = pinned, selected = selected, onPin = onPin, onUnpin = onUnpin)
    }
}

@Composable
private fun SystemRow(
    system: MarketSystemOption,
    viewModel: MarketLocationViewModel,
    selected: Boolean,
    editing: Boolean,
    pinned: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
) {
    PlaceRow(
        title = viewModel.placeName(system.systemName, system.systemZhName, system.systemEnName),
        subtitle = viewModel.placeName(system.regionName, system.regionZhName, system.regionEnName),
        security = null,
        iconFileName = null,
        iconManager = koinInject(),
        selected = selected,
        editing = editing,
        pinned = pinned,
        onClick = onClick,
        onPin = onPin,
        onUnpin = onUnpin,
    )
}

@Composable
private fun RegionRow(
    region: MarketPlaceName,
    viewModel: MarketLocationViewModel,
    selected: Boolean,
    editing: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit,
) {
    PlaceRow(
        title = viewModel.placeName(region.name, region.zhName, region.enName),
        subtitle = null,
        security = null,
        iconFileName = null,
        iconManager = koinInject(),
        selected = selected,
        editing = editing,
        pinned = false,
        onClick = onClick,
        onPin = onPin,
        onUnpin = {},
    )
}

@Composable
private fun EditAction(
    editing: Boolean,
    pinned: Boolean,
    selected: Boolean,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
) {
    when {
        editing && pinned -> IconButton(onClick = onUnpin) {
            Icon(Icons.Filled.Remove, contentDescription = null, tint = colorResource(R.color.character_security_negative))
        }
        editing && !pinned -> IconButton(onClick = onPin) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = colorResource(R.color.character_security_high))
        }
        selected -> Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = colorResource(R.color.hyperlink_text),
            modifier = Modifier.padding(end = dimensionResource(R.dimen.detail_card_horizontal_padding)),
        )
    }
}

@Composable
private fun AddStructurePage(
    viewModel: MarketLocationViewModel,
    onBack: () -> Unit,
) {
    val ui by viewModel.ui.collectAsState()
    var characterId by remember { mutableStateOf(ui.characters.firstOrNull()?.characterId) }
    var query by rememberSaveable { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.skill_plan_add_skill_sheet_horizontal_padding),
                end = dimensionResource(R.dimen.skill_plan_add_skill_sheet_horizontal_padding),
                top = dimensionResource(R.dimen.skill_plan_add_skill_header_top_padding),
            ),
        ) {
            MarketSheetHeader(
                title = stringResource(R.string.market_add_structure),
                showBack = true,
                showDone = false,
                onBack = onBack,
                onDone = {},
            )
        }
        if (ui.characters.isEmpty()) {
            Text(
                text = stringResource(R.string.market_no_characters),
                modifier = Modifier.padding(dimensionResource(R.dimen.detail_card_horizontal_padding)),
                color = colorResource(R.color.hint_text),
            )
            return
        }
        MarketSectionHeader(
            title = stringResource(R.string.market_select_character),
            addTopGap = true,
        )
        MarketSheetCard {
            ui.characters.forEachIndexed { index, character ->
                CharacterChoice(character = character, selected = character.characterId == characterId) {
                    characterId = character.characterId
                    viewModel.clearStructureSearch()
                }
                if (index < ui.characters.lastIndex) ItemDivider()
            }
        }
        if (ui.structureSearchDenied) {
            Text(
                text = stringResource(R.string.market_search_structure_permission),
                color = colorResource(R.color.character_security_negative),
                modifier = Modifier.padding(dimensionResource(R.dimen.detail_card_horizontal_padding)),
            )
        }
        if (ui.structureMarketDenied) {
            Text(
                text = stringResource(R.string.market_structure_permission),
                color = colorResource(R.color.character_security_negative),
                modifier = Modifier.padding(dimensionResource(R.dimen.detail_card_horizontal_padding)),
            )
        }
        MarketSheetSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.market_structure_search_hint),
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.skill_plan_add_skill_sheet_horizontal_padding),
                vertical = dimensionResource(R.dimen.skill_plan_add_skill_search_top_gap),
            ),
            trailing = {
                val enabled = query.isNotBlank() && !ui.searchingStructures
                Text(
                    text = stringResource(R.string.market_search_structure),
                    color = if (enabled) {
                        colorResource(R.color.hyperlink_text)
                    } else {
                        colorResource(R.color.hint_text)
                    },
                    fontSize = dimensionResource(R.dimen.segmented_control_text_size).value.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(enabled = enabled) {
                        val id = characterId ?: return@clickable
                        viewModel.searchStructures(id, query)
                    },
                )
            },
        )
        if (ui.structureHits.isNotEmpty()) {
            MarketSheetCard {
                ui.structureHits.forEachIndexed { index, hit ->
                    StructureHitRow(hit = hit) {
                        val id = characterId ?: return@StructureHitRow
                        viewModel.saveStructure(hit, id)
                        onBack()
                    }
                    if (index < ui.structureHits.lastIndex) {
                        ItemDivider(
                            leadingIconSize = if (hit.iconFileName.isNullOrBlank()) {
                                0.dp
                            } else {
                                dimensionResource(R.dimen.base_lazy_column_item_icon_size)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterChoice(
    character: EveSessionIdentity,
    selected: Boolean,
    onClick: () -> Unit,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconUrl = portraitUrl(character.characterId),
            itemName = character.characterName,
            itemNameColor = if (selected) colorResource(R.color.hyperlink_text) else null,
            showChevron = false,
            onClick = onClick,
        ),
        showDivider = false,
    )
}

@Composable
private fun StructureHitRow(
    hit: MarketStructureHit,
    onClick: () -> Unit,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = !hit.iconFileName.isNullOrBlank(),
            iconFileName = hit.iconFileName,
            itemName = hit.name,
            itemHints = listOf(
                com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemHint(
                    annotatedText = marketLocationHint(hit.security, hit.systemName),
                ),
            ),
            showChevron = false,
            onClick = onClick,
        ),
        showDivider = false,
    )
}
