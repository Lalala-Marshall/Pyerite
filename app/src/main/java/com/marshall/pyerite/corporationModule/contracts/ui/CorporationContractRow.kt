package com.marshall.pyerite.corporationModule.contracts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContract
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractStatus
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractStatusTone
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractsConfig
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractsDateFormatter
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemHint
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.util.NumberDisplayFormatter
import kotlin.math.abs
import kotlin.math.floor

@Composable
internal fun CorporationContractRow(
    contract: CorporationContract,
    nowMs: Long,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    val captionColor = colorResource(R.color.hint_text)
    val titled = stringResource(
        R.string.corporation_contracts_title,
        contract.title.replace(
            CorporationContractsConfig.FORMAT_PERCENT,
            CorporationContractsConfig.FORMAT_PERCENT_ESCAPED,
        ),
    )
    val untitled = stringResource(R.string.corporation_contracts_title_none)
    val titleText = if (contract.title.isBlank()) untitled else titled
    val volumeText = stringResource(
        R.string.corporation_contracts_volume,
        formatContractWholeNumber(contract.volume),
    )
    val expiryText = contractExpiryText(contract.expiresAtMs, nowMs)
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = stringResource(contract.type.titleRes),
            itemNameMaxLines = 1,
            itemHints = listOf(
                BaseLazyColumnItemHint(
                    text = titleText,
                    color = captionColor,
                    trailingText = stringResource(
                        R.string.personal_property_isk_value,
                        corporationContractAmountBody(contract.signedIsk, compact = false),
                    ),
                    trailingColor = corporationContractAmountColor(contract.signedIsk),
                ),
                BaseLazyColumnItemHint(
                    text = volumeText,
                    color = captionColor,
                    trailingText = expiryText,
                    trailingColor = captionColor,
                ),
            ),
            showChevron = true,
            onClick = onClick,
        ),
        showDivider = showDivider,
        titleTrailingContent = { CorporationContractStatusTag(status = contract.status) },
    )
}

@Composable
internal fun CorporationContractStatusTag(
    status: CorporationContractStatus,
) {
    val (backgroundRes, textRes) = corporationContractStatusColorRes(status)
    val tagTextSize = dimensionResource(R.dimen.character_sheet_status_tag_text_size).value.sp
    Text(
        text = stringResource(status.titleRes),
        color = colorResource(textRes),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            fontSize = tagTextSize,
            lineHeight = tagTextSize,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    dimensionResource(R.dimen.character_sheet_status_tag_corner),
                ),
            )
            .background(colorResource(backgroundRes))
            .padding(
                horizontal = dimensionResource(
                    R.dimen.character_sheet_status_tag_horizontal_padding,
                ),
            ),
    )
}

@Composable
private fun contractExpiryText(
    expiresAtMs: Long?,
    nowMs: Long,
): String {
    val placeholder = stringResource(R.string.character_sheet_value_placeholder)
    val dateText = expiresAtMs?.let {
        CorporationContractsDateFormatter.displayDate(it)
    }.orEmpty()
    val days = expiresAtMs?.let {
        CorporationContractsDateFormatter.daysUntil(it, nowMs)
    } ?: 0
    val formatted = stringResource(R.string.corporation_contracts_expires, dateText, days)
    return if (expiresAtMs == null) placeholder else formatted
}

@Composable
internal fun corporationContractAmountBody(
    signedIsk: Double,
    compact: Boolean,
): String {
    val magnitude = if (compact) {
        formatContractCompactNumber(signedIsk)
    } else {
        formatContractWholeNumber(signedIsk)
    }
    val positive = stringResource(R.string.corporation_contracts_amount_positive, magnitude)
    val negative = stringResource(R.string.corporation_contracts_amount_negative, magnitude)
    return when {
        signedIsk > CorporationContractsConfig.ZERO_ISK -> positive
        signedIsk < CorporationContractsConfig.ZERO_ISK -> negative
        else -> magnitude
    }
}

internal fun formatContractWholeNumber(value: Double): String {
    val whole = floor(abs(value)).toLong()
    return NumberDisplayFormatter.format(whole, NumberDisplayFormatter.Style.FULL)
}

internal fun formatContractCompactNumber(value: Double): String {
    val whole = floor(abs(value)).toLong()
    val compact = NumberDisplayFormatter.format(whole, NumberDisplayFormatter.Style.COMPACT)
    val wholeText = NumberDisplayFormatter.format(whole, NumberDisplayFormatter.Style.FULL)
    // Compact below 1,000 falls back to a decimal FULL string; contracts show whole ISK.
    return if (compact.any { it.isLetter() }) compact else wholeText
}

@Composable
internal fun corporationContractStatusTextColor(status: CorporationContractStatus): Color =
    colorResource(corporationContractStatusColorRes(status).second)

private fun corporationContractStatusColorRes(
    status: CorporationContractStatus,
): Pair<Int, Int> = when (status.tone) {
    CorporationContractStatusTone.OUTSTANDING ->
        R.color.corporation_contract_status_outstanding_background to
            R.color.corporation_contract_status_outstanding_text
    CorporationContractStatusTone.IN_PROGRESS ->
        R.color.corporation_contract_status_progress_background to
            R.color.corporation_contract_status_progress_text
    CorporationContractStatusTone.FINISHED ->
        R.color.corporation_contract_status_finished_background to
            R.color.corporation_contract_status_finished_text
    CorporationContractStatusTone.CLOSED ->
        R.color.corporation_contract_status_closed_background to
            R.color.corporation_contract_status_closed_text
    CorporationContractStatusTone.DELETED ->
        R.color.corporation_contract_status_deleted_background to
            R.color.corporation_contract_status_deleted_text
}

@Composable
internal fun corporationContractAmountColor(signedIsk: Double): Color = when {
    signedIsk > CorporationContractsConfig.ZERO_ISK -> colorResource(R.color.wallet_income)
    signedIsk < CorporationContractsConfig.ZERO_ISK -> colorResource(R.color.wallet_expense)
    else -> colorResource(R.color.hint_text)
}
