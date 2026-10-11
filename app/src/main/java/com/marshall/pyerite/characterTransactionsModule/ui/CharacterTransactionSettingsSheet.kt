package com.marshall.pyerite.characterTransactionsModule.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.marshall.pyerite.R
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsConfig
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel

@Composable
internal fun CharacterTransactionSettingsSheet(
    mergeSimilarTransactions: Boolean,
    onMergeSimilarTransactionsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val bottomPadding = dimensionResource(R.dimen.type_detail_bottom_padding)
    CharacterTransactionsModalSheet(
        title = stringResource(R.string.corporation_wallet_transaction_settings_title),
        onDismiss = onDismiss,
        heightFraction = CharacterTransactionsConfig.SETTINGS_SHEET_HEIGHT_FRACTION,
    ) {
        BaseContainer(
            useSystemBarsPadding = false,
            modifier = Modifier.padding(bottom = bottomPadding),
        ) {
            BaseLazyColumnItem(
                model = BaseLazyColumnItemModel(
                    showLeadingIcon = false,
                    itemName = stringResource(R.string.corporation_wallet_merge_similar_transactions),
                    itemHint = stringResource(
                        R.string.corporation_wallet_merge_similar_transactions_hint,
                    ),
                    showChevron = false,
                    onClick = { onMergeSimilarTransactionsChange(!mergeSimilarTransactions) },
                ),
                showDivider = false,
                trailingContent = {
                    val checkedTrack = colorResource(R.color.character_status_positive)
                    Switch(
                        checked = mergeSimilarTransactions,
                        onCheckedChange = onMergeSimilarTransactionsChange,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = checkedTrack,
                            checkedBorderColor = checkedTrack,
                            checkedThumbColor = colorResource(R.color.white),
                        ),
                    )
                },
            )
        }
    }
}
