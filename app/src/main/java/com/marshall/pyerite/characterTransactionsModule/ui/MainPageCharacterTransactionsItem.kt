package com.marshall.pyerite.characterTransactionsModule.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marshall.pyerite.R
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel

@Composable
fun MainPageCharacterTransactionsItem(
    onClick: () -> Unit,
    showDivider: Boolean = false,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = R.drawable.ic_character_transactions,
            itemName = stringResource(R.string.character_transactions),
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}
