package com.marshall.pyerite.regionMarketModule.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.marshall.pyerite.R
import com.marshall.pyerite.regionMarketModule.model.MarketConfig
import com.marshall.pyerite.util.NumberDisplayFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
internal fun marketPricePair(value: Double?): String {
    if (value == null) return stringResource(R.string.type_detail_market_placeholder_value)
    val full = NumberDisplayFormatter.format(value, NumberDisplayFormatter.Style.FULL)
    if (abs(value) < MarketConfig.FULL_PRICE_EXCLUSIVE_MAX) {
        return stringResource(R.string.market_price_isk, full)
    }
    return stringResource(
        R.string.market_price_pair,
        NumberDisplayFormatter.format(value, NumberDisplayFormatter.Style.COMPACT),
        full,
    )
}

@Composable
internal fun marketSecurityColor(security: Double): Color = when {
    security <= MarketConfig.SECURITY_NEGATIVE_MAX -> colorResource(R.color.character_security_negative)
    security < MarketConfig.SECURITY_LOW_MAX -> colorResource(R.color.character_security_low)
    else -> colorResource(R.color.character_security_high)
}

@Composable
internal fun marketLocationHint(security: Double?, placeName: String): AnnotatedString {
    val fallback = stringResource(R.string.market_unknown_location)
    val name = placeName.ifBlank { fallback }
    val textColor = colorResource(R.color.hint_text)
    return buildAnnotatedString {
        if (security != null) {
            withStyle(SpanStyle(color = marketSecurityColor(security), fontWeight = FontWeight.Bold)) {
                append(String.format(Locale.US, MarketConfig.SECURITY_FORMAT, security))
            }
            withStyle(SpanStyle(color = textColor)) {
                append(" ")
                append(name)
            }
        } else {
            withStyle(SpanStyle(color = textColor)) { append(name) }
        }
    }
}
