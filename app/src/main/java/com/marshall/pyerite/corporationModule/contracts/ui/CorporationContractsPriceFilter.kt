package com.marshall.pyerite.corporationModule.contracts.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel

@Composable
internal fun CorporationContractsPriceFilter(
    expanded: Boolean,
    minPriceText: String,
    maxPriceText: String,
    onToggle: () -> Unit,
    onMinPriceChange: (String) -> Unit,
    onMaxPriceChange: (String) -> Unit,
) {
    BaseContainer(useSystemBarsPadding = false) {
        BaseLazyColumnItem(
            model = BaseLazyColumnItemModel(
                showLeadingIcon = false,
                itemName = stringResource(R.string.corporation_contracts_price_filter),
                showChevron = false,
                onClick = onToggle,
            ),
            showDivider = expanded,
            leadingContent = { iconSize ->
                PriceFilterIcon(iconSize = iconSize)
            },
            trailingContent = {
                Icon(
                    imageVector = if (expanded) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = null,
                    tint = colorResource(R.color.text_primary),
                    modifier = Modifier.size(dimensionResource(R.dimen.detail_row_chevron_size)),
                )
            },
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            PriceFilterFields(
                minPriceText = minPriceText,
                maxPriceText = maxPriceText,
                onMinPriceChange = onMinPriceChange,
                onMaxPriceChange = onMaxPriceChange,
            )
        }
    }
}

@Composable
private fun PriceFilterIcon(iconSize: Dp) {
    Icon(
        imageVector = Icons.Filled.AttachMoney,
        contentDescription = null,
        tint = colorResource(R.color.text_primary),
        modifier = Modifier.size(iconSize),
    )
}

@Composable
private fun PriceFilterFields(
    minPriceText: String,
    maxPriceText: String,
    onMinPriceChange: (String) -> Unit,
    onMaxPriceChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.detail_row_horizontal_padding),
                end = dimensionResource(R.dimen.detail_row_horizontal_padding),
                top = dimensionResource(R.dimen.corporation_contracts_price_title_gap),
                bottom = dimensionResource(R.dimen.corporation_contracts_price_block_bottom_padding),
            ),
    ) {
        PriceField(
            title = stringResource(R.string.corporation_contracts_price_min),
            hint = stringResource(R.string.corporation_contracts_price_min_hint),
            value = minPriceText,
            onValueChange = onMinPriceChange,
            modifier = Modifier.weight(1f),
        )
        Spacer(
            modifier = Modifier.width(
                dimensionResource(R.dimen.corporation_contracts_price_field_gap),
            ),
        )
        PriceField(
            title = stringResource(R.string.corporation_contracts_price_max),
            hint = stringResource(R.string.corporation_contracts_price_max_hint),
            value = maxPriceText,
            onValueChange = onMaxPriceChange,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PriceField(
    title: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val textColor = colorResource(R.color.text_primary)
    val hintColor = colorResource(R.color.hint_text)
    val textSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp
    val lineHeight = dimensionResource(R.dimen.sub_menu_label_line_height).value.sp
    val shape = RoundedCornerShape(
        dimensionResource(R.dimen.corporation_contracts_price_field_corner),
    )
    Column(modifier = modifier) {
        Text(
            text = title,
            color = colorResource(R.color.text_caption),
            fontSize = dimensionResource(R.dimen.detail_row_label_subtitle_text_size).value.sp,
            lineHeight = dimensionResource(R.dimen.detail_row_label_subtitle_line_height).value.sp,
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.corporation_contracts_price_title_gap)))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                color = textColor,
                fontSize = textSize,
                lineHeight = lineHeight,
            ),
            singleLine = true,
            cursorBrush = SolidColor(textColor),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.corporation_contracts_price_field_height))
                        .clip(shape)
                        .border(
                            width = dimensionResource(R.dimen.corporation_contracts_price_field_border),
                            color = colorResource(R.color.border),
                            shape = shape,
                        )
                        .padding(
                            horizontal = dimensionResource(
                                R.dimen.corporation_contracts_price_field_horizontal_padding,
                            ),
                        ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            color = hintColor,
                            fontSize = textSize,
                            lineHeight = lineHeight,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}
