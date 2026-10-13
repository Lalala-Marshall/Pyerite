package com.marshall.pyerite.characterIndustryModule.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marshall.pyerite.R
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel

@Composable
fun MainPageCharacterIndustryItem(
    onClick: () -> Unit,
    showDivider: Boolean = false,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = R.drawable.ic_corporation_industry,
            itemName = stringResource(R.string.character_industry),
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}
