package com.marshall.pyerite.characterTransactionsModule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.marshall.pyerite.R
import com.marshall.pyerite.characterTransactionsModule.model.CharacterMarketTransaction
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionLocation
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsConfig
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsDateFormatter
import com.marshall.pyerite.iconModule.manager.IconManager
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.localization.localizedName
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemLayout
import com.marshall.pyerite.ui.golbalComponents.PyeriteIconShape
import com.marshall.pyerite.ui.golbalComponents.baseLazyColumnItemAdaptiveIconSize
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.compose.koinInject
import java.util.Locale

private const val AMOUNT_HINT_LINE_COUNT = 1

@Composable
internal fun CharacterTransactionItem(
    transaction: CharacterMarketTransaction,
    showDivider: Boolean,
    onClick: () -> Unit,
    localeController: LocaleController = koinInject(),
    iconManager: IconManager = koinInject(),
) {
    val horizontalPadding = dimensionResource(R.dimen.detail_row_horizontal_padding)
    val iconGap = dimensionResource(R.dimen.detail_row_icon_gap)
    val trailingGap = dimensionResource(R.dimen.detail_row_trailing_gap)
    val chevronSize = dimensionResource(R.dimen.detail_row_chevron_size)
    val hintSpacing = dimensionResource(R.dimen.detail_row_label_subtitle_spacing)
    val textVerticalPadding = dimensionResource(R.dimen.detail_row_vertical_padding_multi_line)
    val iconVerticalPadding = dimensionResource(R.dimen.detail_row_icon_vertical_padding)
    val titleTextSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp
    val titleLineHeight = dimensionResource(R.dimen.sub_menu_label_line_height).value.sp
    val hintTextSize = dimensionResource(R.dimen.detail_row_label_subtitle_text_size).value.sp
    val hintLineHeight = dimensionResource(R.dimen.detail_row_label_subtitle_line_height).value.sp
    val hintColor = colorResource(R.color.hint_text)
    val titleColor = colorResource(R.color.text_primary)
    val iconSize = baseLazyColumnItemAdaptiveIconSize(
        contentLineCount = BaseLazyColumnItemLayout.TITLE_LINE_COUNT + AMOUNT_HINT_LINE_COUNT,
    )
    val typeName = transaction.displayName(localeController)
    val sideLabel = stringResource(
        if (transaction.isBuy) R.string.corporation_wallet_buy else R.string.corporation_wallet_sell,
    )
    val quantity = NumberDisplayFormatter.format(
        transaction.quantity,
        NumberDisplayFormatter.Style.FULL,
    )
    val unitPrice = formatTransactionIsk(transaction.unitPrice, NumberDisplayFormatter.Style.FULL)
    val fillLine = stringResource(
        R.string.corporation_wallet_transaction_line,
        sideLabel,
        quantity,
        unitPrice,
    )
    val timeLabel = CharacterTransactionsDateFormatter.displayTimeSecond(transaction.dateEpochMs)
    val locationText = transaction.location?.let { locationLine(it, localeController) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    TransactionTypeIcon(
                        iconFileName = transaction.iconFilename,
                        iconSize = iconSize,
                        iconVerticalPadding = iconVerticalPadding,
                        iconManager = iconManager,
                    )
                    Spacer(modifier = Modifier.width(iconGap))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = textVerticalPadding),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.wallet_transaction_type_quantity,
                                typeName,
                                quantity,
                            ),
                            color = titleColor,
                            fontSize = titleTextSize,
                            lineHeight = titleLineHeight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(hintSpacing))
                        Text(
                            text = formatTransactionIsk(
                                transaction.totalIsk,
                                NumberDisplayFormatter.Style.FULL,
                            ),
                            color = transactionSignedColor(transaction.signedNet),
                            fontSize = hintTextSize,
                            lineHeight = hintLineHeight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (locationText != null) {
                    Text(
                        text = locationText,
                        fontSize = hintTextSize,
                        lineHeight = hintLineHeight,
                        modifier = Modifier.padding(top = hintSpacing),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = hintSpacing, bottom = textVerticalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = fillLine,
                        color = hintColor,
                        fontSize = hintTextSize,
                        lineHeight = hintLineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(trailingGap))
                    Text(
                        text = timeLabel,
                        color = hintColor,
                        fontSize = hintTextSize,
                        lineHeight = hintLineHeight,
                        maxLines = 1,
                    )
                }
            }
            Spacer(modifier = Modifier.width(trailingGap))
            Icon(
                modifier = Modifier.size(chevronSize),
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = hintColor,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = horizontalPadding),
                thickness = dimensionResource(R.dimen.detail_divider_thickness),
                color = colorResource(R.color.border),
            )
        }
    }
}

@Composable
private fun TransactionTypeIcon(
    iconFileName: String?,
    iconSize: Dp,
    iconVerticalPadding: Dp,
    iconManager: IconManager,
) {
    val iconFile = iconFileName?.let { iconManager.getIconFile(it) }
    val shape = PyeriteIconShape.shape
    val modifier = Modifier
        .padding(vertical = iconVerticalPadding)
        .size(iconSize)
        .clip(shape)
    if (iconFile != null) {
        Box(
            modifier = modifier.background(colorResource(R.color.white)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = iconFile,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.base_lazy_column_item_icon_plate_padding))
                    .clip(shape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_character_transactions),
                error = painterResource(R.drawable.ic_character_transactions),
            )
        }
    } else {
        Icon(
            modifier = modifier,
            painter = painterResource(R.drawable.ic_character_transactions),
            contentDescription = null,
            tint = colorResource(R.color.hint_text),
        )
    }
}

@Composable
private fun locationLine(
    location: CharacterTransactionLocation,
    localeController: LocaleController,
): AnnotatedString = buildAnnotatedString {
    val security = location.securityStatus
    val systemName = localizedName(
        zh = location.systemZhName,
        en = location.systemEnName,
        fallback = location.systemName,
        language = localeController.contentLanguage,
    )
    val placeName = location.placeName?.takeIf { it.isNotBlank() }
    if (security != null) {
        withStyle(SpanStyle(color = systemSecurityColor(security))) {
            append(
                String.format(
                    Locale.US,
                    CharacterTransactionsConfig.SYSTEM_SECURITY_FORMAT,
                    security,
                ),
            )
        }
        append(CharacterTransactionsConfig.LOCATION_SEGMENT_GAP)
    }
    if (systemName.isNotBlank()) {
        withStyle(
            SpanStyle(
                color = colorResource(R.color.text_primary),
                fontWeight = FontWeight.Bold,
            ),
        ) {
            append(systemName)
        }
    }
    if (placeName != null) {
        withStyle(SpanStyle(color = colorResource(R.color.hint_text))) {
            append(CharacterTransactionsConfig.LOCATION_PLACE_SEPARATOR)
            append(placeName)
        }
    }
}

@Composable
private fun systemSecurityColor(security: Double): Color = when {
    security <= CharacterTransactionsConfig.SECURITY_NEGATIVE_MAX ->
        colorResource(R.color.character_security_negative)
    security < CharacterTransactionsConfig.SECURITY_LOW_MAX ->
        colorResource(R.color.character_security_low)
    else -> colorResource(R.color.character_security_high)
}
