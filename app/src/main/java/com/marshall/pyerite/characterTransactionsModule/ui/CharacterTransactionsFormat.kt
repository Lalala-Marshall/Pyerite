package com.marshall.pyerite.characterTransactionsModule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.marshall.pyerite.R
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsConfig
import com.marshall.pyerite.util.NumberDisplayFormatter
import kotlin.math.abs

@Composable
internal fun formatTransactionIsk(
    value: Double,
    style: NumberDisplayFormatter.Style,
): String {
    val formatted = NumberDisplayFormatter.format(abs(value), style)
    return stringResource(R.string.personal_property_isk_value, formatted)
}

@Composable
internal fun transactionSignedColor(signedValue: Double): Color = when {
    signedValue > CharacterTransactionsConfig.ZERO_ISK -> colorResource(R.color.wallet_income)
    signedValue < CharacterTransactionsConfig.ZERO_ISK -> colorResource(R.color.wallet_expense)
    else -> colorResource(R.color.hint_text)
}

@Composable
internal fun CharacterTransactionsStatusBanner(
    permissionDenied: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
) {
    val messageRes = when {
        permissionDenied -> R.string.character_transactions_permission_denied
        loadFailed -> R.string.character_transactions_load_failed
        else -> return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(messageRes),
            color = colorResource(R.color.text_primary),
            modifier = Modifier.weight(1f),
        )
        if (loadFailed && !permissionDenied) {
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.character_sheet_retry))
            }
        }
    }
}
