package com.marshall.pyerite.characterWalletJournalModule.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marshall.pyerite.R
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel

@Composable
fun MainPageCharacterWalletJournalItem(
    onClick: () -> Unit,
    showDivider: Boolean = false,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = R.drawable.ic_corporation_wallet,
            itemName = stringResource(R.string.corporation_wallet_filter_journal),
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}
