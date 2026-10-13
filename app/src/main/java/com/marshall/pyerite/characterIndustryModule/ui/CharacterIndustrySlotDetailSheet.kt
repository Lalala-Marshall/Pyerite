package com.marshall.pyerite.characterIndustryModule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R
import com.marshall.pyerite.characterIndustryModule.model.CharacterIndustrySlotSummary
import com.marshall.pyerite.industryCommon.ui.IndustryModalSheet
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.CharacterAvatar

internal data class CharacterIndustrySlotDetail(
    val characterId: Long,
    val name: String,
    val portraitUrl: String?,
    val summary: CharacterIndustrySlotSummary,
)

@Composable
internal fun CharacterIndustrySlotDetailSheet(
    characters: List<CharacterIndustrySlotDetail>,
    onDismiss: () -> Unit,
) {
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    IndustryModalSheet(
        title = stringResource(R.string.character_industry_slot_details),
        endLabel = stringResource(R.string.corporation_structures_sheet_done),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = dimensionResource(R.dimen.type_detail_bottom_padding)),
        ) {
            characters.forEach { character ->
                BaseContainer(
                    useSystemBarsPadding = false,
                    modifier = Modifier.padding(top = sectionGap),
                ) {
                    CharacterHeader(
                        name = character.name.ifBlank {
                            stringResource(R.string.corporation_industry_unknown_installer)
                        },
                        portraitUrl = character.portraitUrl,
                    )
                    HorizontalDivider(
                        thickness = dimensionResource(R.dimen.detail_divider_thickness),
                        color = colorResource(R.color.border),
                    )
                    CharacterIndustrySlotLines(summary = character.summary)
                }
            }
        }
    }
}

@Composable
private fun CharacterHeader(
    name: String,
    portraitUrl: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.detail_row_horizontal_padding),
                vertical = dimensionResource(R.dimen.detail_row_vertical_padding_single_line),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.detail_row_icon_gap),
        ),
    ) {
        CharacterAvatar(
            portraitUrl = portraitUrl,
            size = dimensionResource(R.dimen.corporation_industry_installer_avatar_size),
        )
        Text(
            text = name,
            color = colorResource(R.color.text_primary),
            fontSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
