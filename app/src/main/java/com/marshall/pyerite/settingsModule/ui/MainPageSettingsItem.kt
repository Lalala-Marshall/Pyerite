package com.marshall.pyerite.settingsModule.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marshall.pyerite.R
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel

@Composable
fun MainPageSettingsItem(
    onClick: (() -> Unit)?,
    showDivider: Boolean = false,
    showChevron: Boolean = true,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = R.drawable.ic_settings,
            itemName = stringResource(R.string.settings),
            showChevron = showChevron,
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}
