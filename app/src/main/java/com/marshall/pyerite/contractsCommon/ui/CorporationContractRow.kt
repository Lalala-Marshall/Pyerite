package com.marshall.pyerite.contractsCommon.ui

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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R
import com.marshall.pyerite.contractsCommon.model.ContractAmountPart
import com.marshall.pyerite.contractsCommon.model.ContractAmountTone
import com.marshall.pyerite.contractsCommon.model.ContractAmountViewpoint
import com.marshall.pyerite.contractsCommon.model.CorporationContract
import com.marshall.pyerite.contractsCommon.model.CorporationContractStatus
import com.marshall.pyerite.contractsCommon.model.contractAmountParts
import com.marshall.pyerite.contractsCommon.model.CorporationContractStatusTone
import com.marshall.pyerite.contractsCommon.model.CorporationContractsConfig
import com.marshall.pyerite.contractsCommon.model.CorporationContractsDateFormatter
import com.marshall.pyerite.contractsCommon.model.showsIssueCountdown
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
    amountViewpoint: ContractAmountViewpoint,
    characterId: Long,
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
    val expiryText = contractIssueCountdownText(contract, nowMs)
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = stringResource(contract.type.titleRes),
            itemNameMaxLines = 1,
            itemHints = listOf(
                BaseLazyColumnItemHint(
                    text = titleText,
                    color = captionColor,
                    trailingAnnotated = contractAmountAnnotated(
                        parts = contractAmountParts(
                            contract = contract,
                            viewpoint = amountViewpoint,
                            characterId = characterId,
                        ),
                        withCompactPair = false,
                    ),
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
private fun contractIssueCountdownText(
    contract: CorporationContract,
    nowMs: Long,
): String {
    val issuedText = CorporationContractsDateFormatter.displayDateTime(contract.issuedAtMs)
    if (!contract.status.showsIssueCountdown) return issuedText
    val expiresAtMs = contract.expiresAtMs ?: return issuedText
    val days = CorporationContractsDateFormatter.daysUntil(expiresAtMs, nowMs)
    return stringResource(R.string.corporation_contracts_expires, issuedText, days)
}

@Composable
internal fun contractAmountAnnotated(
    parts: List<ContractAmountPart>,
    withCompactPair: Boolean,
): AnnotatedString {
    val separator = stringResource(R.string.corporation_contracts_amount_separator)
    return buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (index > 0) append(separator)
            val full = contractAmountBody(part, compact = false)
            val text = if (withCompactPair) {
                stringResource(
                    R.string.corporation_contracts_detail_price_value,
                    full,
                    contractAmountBody(part, compact = true),
                )
            } else {
                stringResource(R.string.personal_property_isk_value, full)
            }
            withStyle(SpanStyle(color = contractAmountColor(part.tone))) {
                append(text)
            }
        }
    }
}

@Composable
private fun contractAmountBody(
    part: ContractAmountPart,
    compact: Boolean,
): String {
    val magnitude = if (compact) {
        formatContractCompactNumber(part.amount)
    } else {
        formatContractWholeNumber(part.amount)
    }
    val positive = stringResource(R.string.corporation_contracts_amount_positive, magnitude)
    val negative = stringResource(R.string.corporation_contracts_amount_negative, magnitude)
    return when (part.tone) {
        ContractAmountTone.INCOME -> positive
        ContractAmountTone.EXPENSE -> negative
        ContractAmountTone.NEUTRAL,
        ContractAmountTone.OPEN,
        -> magnitude
    }
}

@Composable
private fun contractAmountColor(tone: ContractAmountTone): Color = when (tone) {
    ContractAmountTone.INCOME -> colorResource(R.color.wallet_income)
    ContractAmountTone.EXPENSE -> colorResource(R.color.wallet_expense)
    ContractAmountTone.NEUTRAL -> colorResource(R.color.hint_text)
    ContractAmountTone.OPEN -> colorResource(R.color.contract_amount_open)
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
