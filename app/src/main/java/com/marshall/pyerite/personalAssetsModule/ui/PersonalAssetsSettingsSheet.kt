package com.marshall.pyerite.personalAssetsModule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R
import com.marshall.pyerite.charactersListModule.model.LoggedInCharacter
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsConfig
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsSettings
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.CharacterAvatar
import com.marshall.pyerite.ui.golbalComponents.PyeriteIconShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PersonalAssetsSettingsSheet(
    initial: PersonalAssetsSettings,
    anchorCharacterId: Long,
    characters: List<LoggedInCharacter>,
    onCommit: (PersonalAssetsSettings) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var aggregate by remember { mutableStateOf(initial.aggregateCharacters) }
    var mergeSameLocation by remember { mutableStateOf(initial.mergeSameLocation) }
    var checkedIds by remember {
        mutableStateOf(initial.extraCharacterIds + anchorCharacterId)
    }
    val commit = {
        onCommit(
            PersonalAssetsSettings(
                aggregateCharacters = aggregate,
                mergeSameLocation = mergeSameLocation,
                extraCharacterIds = checkedIds - anchorCharacterId,
            ),
        )
    }
    val sheetCorner = dimensionResource(R.dimen.character_mail_compose_sheet_corner)
    val sheetBackground = colorResource(R.color.search_field_idle_background)
    ModalBottomSheet(
        onDismissRequest = commit,
        sheetState = sheetState,
        sheetGesturesEnabled = true,
        shape = RoundedCornerShape(topStart = sheetCorner, topEnd = sheetCorner),
        containerColor = sheetBackground,
        scrimColor = colorResource(R.color.search_scrim),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(PersonalAssetsConfig.SETTINGS_SHEET_HEIGHT_FRACTION)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            PersonalAssetsSheetHeader(
                title = stringResource(R.string.personal_assets_settings),
                doneLabel = stringResource(R.string.personal_assets_settings_done),
                onDone = commit,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = dimensionResource(R.dimen.type_detail_bottom_padding)),
            ) {
                BaseContainer(
                    useSystemBarsPadding = false,
                    modifier = Modifier.padding(
                        top = dimensionResource(R.dimen.type_detail_section_gap),
                    ),
                ) {
                    PersonalAssetsSwitchRow(
                        title = stringResource(R.string.personal_assets_aggregate),
                        hint = stringResource(R.string.personal_assets_aggregate_hint),
                        checked = aggregate,
                        showDivider = aggregate,
                        onCheckedChange = { aggregate = it },
                    )
                    if (aggregate) {
                        PersonalAssetsSwitchRow(
                            title = stringResource(R.string.personal_assets_merge_locations),
                            hint = stringResource(R.string.personal_assets_merge_locations_hint),
                            checked = mergeSameLocation,
                            showDivider = false,
                            onCheckedChange = { mergeSameLocation = it },
                        )
                    }
                }
                if (aggregate) {
                    val listedIds = characters.map { it.characterId }.toSet()
                    val allSelected = listedIds.isNotEmpty() && listedIds.all { characterId ->
                        characterId == anchorCharacterId || characterId in checkedIds
                    }
                    BaseContainer(
                        title = stringResource(R.string.personal_assets_select_characters),
                        titleTrailingContent = {
                            PersonalAssetsSelectAllButton(
                                allSelected = allSelected,
                                onClick = {
                                    checkedIds = if (allSelected) {
                                        setOf(anchorCharacterId)
                                    } else {
                                        listedIds + anchorCharacterId
                                    }
                                },
                            )
                        },
                        useSystemBarsPadding = false,
                        modifier = Modifier.padding(
                            top = dimensionResource(R.dimen.type_detail_section_gap),
                        ),
                    ) {
                        characters.forEachIndexed { index, character ->
                            val locked = character.characterId == anchorCharacterId
                            val selected = locked || character.characterId in checkedIds
                            PersonalAssetsCharacterRow(
                                character = character,
                                selected = selected,
                                locked = locked,
                                showDivider = index < characters.lastIndex,
                                onClick = if (locked) {
                                    null
                                } else {
                                    {
                                        checkedIds = if (character.characterId in checkedIds) {
                                            checkedIds - character.characterId
                                        } else {
                                            checkedIds + character.characterId
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalAssetsSelectAllButton(
    allSelected: Boolean,
    onClick: () -> Unit,
) {
    val labelGap = dimensionResource(R.dimen.corporation_wallet_select_all_gap)
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics { role = Role.Checkbox },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = stringResource(R.string.personal_assets_select_all),
            color = colorResource(R.color.text_primary),
            fontSize = dimensionResource(R.dimen.list_section_subheader_text_size).value.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.width(labelGap))
        PersonalAssetsSelectAllMark(selected = allSelected)
    }
}

@Composable
private fun PersonalAssetsSelectAllMark(selected: Boolean) {
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
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = colorResource(R.color.white),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun PersonalAssetsSheetHeader(
    title: String,
    doneLabel: String,
    onDone: () -> Unit,
) {
    val buttonHeight = dimensionResource(R.dimen.top_bar_back_button_size)
    val actionColor = colorResource(R.color.hyperlink_text)
    val actionTextSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp
    val horizontal = dimensionResource(R.dimen.character_mail_compose_sheet_horizontal_padding)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = horizontal,
                end = horizontal,
                top = dimensionResource(R.dimen.character_mail_compose_header_top_padding),
            )
            .height(buttonHeight),
    ) {
        Text(
            text = title,
            color = colorResource(R.color.text_primary),
            fontSize = dimensionResource(R.dimen.list_section_header_text_size).value.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = buttonHeight),
        )
        Text(
            text = doneLabel,
            color = actionColor,
            fontSize = actionTextSize,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDone,
                )
                .semantics { role = Role.Button },
        )
    }
}

@Composable
private fun PersonalAssetsSwitchRow(
    title: String,
    hint: String,
    checked: Boolean,
    showDivider: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = title,
            itemHint = hint,
            showChevron = false,
            onClick = { onCheckedChange(!checked) },
        ),
        showDivider = showDivider,
        trailingContent = {
            val checkedTrack = colorResource(R.color.character_status_positive)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = checkedTrack,
                    checkedBorderColor = checkedTrack,
                    checkedThumbColor = colorResource(R.color.white),
                ),
            )
        },
    )
}

@Composable
private fun PersonalAssetsCharacterRow(
    character: LoggedInCharacter,
    selected: Boolean,
    locked: Boolean,
    showDivider: Boolean,
    onClick: (() -> Unit)?,
) {
    val checkSize = dimensionResource(R.dimen.detail_row_chevron_size)
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = character.name,
            showChevron = false,
            onClick = onClick,
        ),
        showDivider = showDivider,
        leadingContent = { iconSize ->
            CharacterAvatar(
                portraitUrl = character.portraitUrl,
                size = iconSize,
                shape = PyeriteIconShape.shape,
            )
        },
        trailingContent = if (!selected) {
            null
        } else {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colorResource(
                        if (locked) R.color.hint_text else R.color.hyperlink_text,
                    ),
                    modifier = Modifier.size(checkSize),
                )
            }
        },
    )
}
