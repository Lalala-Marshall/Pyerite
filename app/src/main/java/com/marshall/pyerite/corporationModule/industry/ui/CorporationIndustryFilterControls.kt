package com.marshall.pyerite.corporationModule.industry.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R

@Composable
internal fun CorporationIndustrySelectAllButton(
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
            text = stringResource(R.string.corporation_wallet_filter_select_all),
            color = colorResource(R.color.text_primary),
            fontSize = dimensionResource(R.dimen.list_section_subheader_text_size).value.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.width(labelGap))
        CorporationIndustrySelectAllMark(selected = allSelected)
    }
}

@Composable
private fun CorporationIndustrySelectAllMark(selected: Boolean) {
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
internal fun CorporationIndustryItemCheck(selected: Boolean) {
    val checkSize = dimensionResource(R.dimen.corporation_wallet_item_check_size)
    Box(
        modifier = Modifier.size(checkSize),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = colorResource(R.color.hyperlink_text),
                modifier = Modifier.size(checkSize),
            )
        }
    }
}
