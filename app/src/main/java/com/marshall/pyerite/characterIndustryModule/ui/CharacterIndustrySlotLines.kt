package com.marshall.pyerite.characterIndustryModule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R
import com.marshall.pyerite.characterIndustryModule.model.CharacterIndustrySlotLine
import com.marshall.pyerite.characterIndustryModule.model.CharacterIndustrySlotSummary
import com.marshall.pyerite.util.NumberDisplayFormatter

@Composable
internal fun CharacterIndustrySlotLines(
    summary: CharacterIndustrySlotSummary,
) {
    SlotLine(
        label = stringResource(R.string.corporation_industry_summary_manufacturing),
        line = summary.manufacturing,
        valueColor = colorResource(R.color.corporation_industry_manufacturing),
        showDivider = true,
    )
    SlotLine(
        label = stringResource(R.string.corporation_industry_summary_research),
        line = summary.research,
        valueColor = colorResource(R.color.corporation_industry_research),
        showDivider = true,
    )
    SlotLine(
        label = stringResource(R.string.corporation_industry_summary_reaction),
        line = summary.reaction,
        valueColor = colorResource(R.color.corporation_industry_reaction),
        showDivider = false,
    )
}

@Composable
private fun SlotLine(
    label: String,
    line: CharacterIndustrySlotLine,
    valueColor: Color,
    showDivider: Boolean,
) {
    val labelSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp
    val labelLineHeight = dimensionResource(R.dimen.sub_menu_label_line_height).value.sp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.detail_row_horizontal_padding),
                vertical = dimensionResource(R.dimen.detail_row_vertical_padding_single_line),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = colorResource(R.color.text_primary),
            fontSize = labelSize,
            lineHeight = labelLineHeight,
        )
        SlotFraction(
            used = line.used,
            max = line.max,
            valueColor = valueColor,
            fontSize = labelSize,
            lineHeight = labelLineHeight,
        )
    }
    if (showDivider) {
        HorizontalDivider(
            thickness = dimensionResource(R.dimen.detail_divider_thickness),
            color = colorResource(R.color.border),
        )
    }
}

@Composable
private fun SlotFraction(
    used: Int,
    max: Int?,
    valueColor: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
) {
    val hint = colorResource(R.color.hint_text)
    val usedText = NumberDisplayFormatter.format(used.toLong(), NumberDisplayFormatter.Style.FULL)
    val maxText = max?.let { cap ->
        NumberDisplayFormatter.format(cap.toLong(), NumberDisplayFormatter.Style.FULL)
    } ?: stringResource(R.string.character_industry_slots_unknown)
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = hint)) {
                append(usedText)
                append("/")
            }
            withStyle(SpanStyle(color = valueColor)) {
                append(maxText)
            }
        },
        fontSize = fontSize,
        lineHeight = lineHeight,
    )
}
