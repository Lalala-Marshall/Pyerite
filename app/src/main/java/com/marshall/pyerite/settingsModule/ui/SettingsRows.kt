package com.marshall.pyerite.settingsModule.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import com.marshall.pyerite.R
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemHint
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel

@Composable
internal fun SettingsSwitchRow(
    title: String,
    hint: String,
    checked: Boolean,
    showDivider: Boolean,
    wrapHint: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = title,
            itemHint = if (wrapHint) "" else hint,
            itemHints = if (wrapHint && hint.isNotEmpty()) {
                listOf(BaseLazyColumnItemHint(text = hint, wrap = true))
            } else {
                emptyList()
            },
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
internal fun SettingsIconRow(
    title: String,
    hint: String,
    icon: ImageVector,
    iconContentDescription: String,
    showDivider: Boolean,
    showChevron: Boolean = true,
    onClick: () -> Unit,
) {
    val iconSize = dimensionResource(R.dimen.settings_trailing_icon_size)
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = title,
            itemHint = hint,
            showChevron = showChevron,
            onClick = onClick,
        ),
        showDivider = showDivider,
        trailingContent = {
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                tint = colorResource(R.color.text_primary),
                modifier = Modifier.size(iconSize),
            )
        },
    )
}

@Composable
internal fun SettingsCheckRow(
    title: String,
    selected: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    val iconSize = dimensionResource(R.dimen.settings_trailing_icon_size)
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = title,
            showChevron = false,
            onClick = onClick,
        ),
        showDivider = showDivider,
        trailingContent = if (!selected) {
            null
        } else {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colorResource(R.color.hyperlink_text),
                    modifier = Modifier.size(iconSize),
                )
            }
        },
    )
}
