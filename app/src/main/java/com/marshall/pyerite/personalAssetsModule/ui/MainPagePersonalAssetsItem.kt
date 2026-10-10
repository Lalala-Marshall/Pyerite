package com.marshall.pyerite.personalAssetsModule.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marshall.pyerite.R
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel

@Composable
fun MainPagePersonalAssetsItem(
    onClick: () -> Unit,
    showDivider: Boolean = false,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = R.drawable.ic_corporation_assets,
            itemName = stringResource(R.string.personal_assets),
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}
