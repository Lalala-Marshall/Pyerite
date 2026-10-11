package com.marshall.pyerite.characterWalletJournalModule.ui

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
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalConfig
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalDescription
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalDescriptionKind
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalSummaryWindow
import com.marshall.pyerite.util.NumberDisplayFormatter
import kotlin.math.abs

@Composable
internal fun formatJournalIsk(
    value: Double,
    style: NumberDisplayFormatter.Style,
): String {
    val formatted = NumberDisplayFormatter.format(abs(value), style)
    return stringResource(R.string.personal_property_isk_value, formatted)
}

@Composable
internal fun journalSignedColor(signedValue: Double): Color {
    return when {
        signedValue > CharacterWalletJournalConfig.ZERO_ISK ->
            colorResource(R.color.wallet_income)
        signedValue < CharacterWalletJournalConfig.ZERO_ISK ->
            colorResource(R.color.wallet_expense)
        else -> colorResource(R.color.hint_text)
    }
}

@Composable
internal fun journalSummaryWindowLabel(window: CharacterWalletJournalSummaryWindow): String {
    val res = when (window) {
        CharacterWalletJournalSummaryWindow.DAYS_30 -> R.string.corporation_wallet_window_30d
        CharacterWalletJournalSummaryWindow.DAYS_7 -> R.string.corporation_wallet_window_7d
        CharacterWalletJournalSummaryWindow.DAYS_1 -> R.string.corporation_wallet_window_1d
    }
    return stringResource(res)
}

@Composable
internal fun journalDescriptionText(
    description: CharacterWalletJournalDescription,
): String {
    val placeholder = stringResource(R.string.character_sheet_value_placeholder)
    val first = description.firstPartyName?.takeIf { it.isNotBlank() } ?: placeholder
    val second = description.secondPartyName?.takeIf { it.isNotBlank() } ?: placeholder
    return when (description.kind) {
        CharacterWalletJournalDescriptionKind.INDUSTRY_PROJECT -> {
            val contextId = description.contextId
            if (contextId != null) {
                stringResource(
                    R.string.corporation_wallet_journal_hint_industry,
                    first,
                    second,
                    contextId.toString(),
                )
            } else {
                stringResource(
                    R.string.corporation_wallet_journal_hint_industry_no_id,
                    first,
                    second,
                )
            }
        }
        CharacterWalletJournalDescriptionKind.FALLBACK ->
            description.fallbackText.ifBlank { placeholder }
    }
}

@Composable
internal fun CharacterWalletJournalStatusBanner(
    permissionDenied: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
) {
    val messageRes = when {
        permissionDenied -> R.string.character_wallet_journal_permission_denied
        loadFailed -> R.string.character_wallet_journal_load_failed
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
