package com.marshall.pyerite.characterIndustryModule.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.marshall.pyerite.R
import com.marshall.pyerite.characterIndustryModule.model.CharacterIndustrySettings
import com.marshall.pyerite.charactersListModule.model.LoggedInCharacter
import com.marshall.pyerite.industryCommon.ui.IndustryModalSheet
import com.marshall.pyerite.industryCommon.ui.IndustrySelectAllButton
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.CharacterAvatar
import com.marshall.pyerite.ui.golbalComponents.PyeriteIconShape

@Composable
internal fun CharacterIndustrySettingsSheet(
    initial: CharacterIndustrySettings,
    anchorCharacterId: Long,
    characters: List<LoggedInCharacter>,
    onCommit: (CharacterIndustrySettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var aggregate by remember { mutableStateOf(initial.aggregateCharacters) }
    var checkedIds by remember {
        mutableStateOf(initial.extraCharacterIds + anchorCharacterId)
    }
    val commit = {
        onCommit(
            CharacterIndustrySettings(
                aggregateCharacters = aggregate,
                extraCharacterIds = checkedIds - anchorCharacterId,
            ),
        )
        onDismiss()
    }
    val listedIds = characters.map { it.characterId }.toSet()
    val allSelected = listedIds.isNotEmpty() && listedIds.all { characterId ->
        characterId == anchorCharacterId || characterId in checkedIds
    }
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    IndustryModalSheet(
        title = stringResource(R.string.character_industry_settings),
        endLabel = stringResource(R.string.corporation_structures_sheet_done),
        onDismiss = commit,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = dimensionResource(R.dimen.type_detail_bottom_padding)),
        ) {
            BaseContainer(useSystemBarsPadding = false) {
                BaseLazyColumnItem(
                    model = BaseLazyColumnItemModel(
                        showLeadingIcon = false,
                        itemName = stringResource(R.string.character_industry_aggregate),
                        itemHint = stringResource(R.string.character_industry_aggregate_hint),
                        showChevron = false,
                        onClick = { aggregate = !aggregate },
                    ),
                    showDivider = false,
                    trailingContent = {
                        val checkedTrack = colorResource(R.color.character_status_positive)
                        Switch(
                            checked = aggregate,
                            onCheckedChange = { aggregate = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = checkedTrack,
                                checkedBorderColor = checkedTrack,
                                checkedThumbColor = colorResource(R.color.white),
                            ),
                        )
                    },
                )
            }
            if (aggregate && characters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(sectionGap))
                BaseContainer(
                    title = stringResource(R.string.character_industry_select_characters),
                    titleTrailingContent = {
                        IndustrySelectAllButton(
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
                ) {
                    characters.forEachIndexed { index, character ->
                        val locked = character.characterId == anchorCharacterId
                        val selected = locked || character.characterId in checkedIds
                        CharacterIndustryCharacterRow(
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

@Composable
private fun CharacterIndustryCharacterRow(
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
