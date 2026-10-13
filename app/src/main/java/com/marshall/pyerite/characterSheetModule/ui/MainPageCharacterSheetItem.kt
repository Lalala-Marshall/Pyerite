package com.marshall.pyerite.characterSheetModule.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.marshall.pyerite.R
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel

/** Home-page Character Sheet row (pair with clone status inside the Character section card). */
@Composable
fun MainPageCharacterSheetItem(
    skillPointsHint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = R.drawable.ic_character_sheet,
            itemName = stringResource(R.string.character_sheet),
            itemHint = skillPointsHint,
            onClick = onClick,
        ),
        showDivider = showDivider,
        modifier = modifier,
    )
}
