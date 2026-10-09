package com.marshall.pyerite.regionMarketModule.watchlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R

@Composable
internal fun MarketWatchlistBottomSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onSearch: () -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barHeight = dimensionResource(R.dimen.search_bar_height)
    val corner = dimensionResource(R.dimen.detail_card_corner_radius)
    val fieldBackground = colorResource(R.color.search_field_background)
    val textColor = colorResource(R.color.text_primary)
    val hintColor = colorResource(R.color.hint_text)

    Box(
        modifier = modifier.padding(horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding)),
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(corner))
                .background(fieldBackground),
            textStyle = TextStyle(color = textColor, fontSize = 16.sp),
            singleLine = true,
            cursorBrush = SolidColor(textColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = SearchBarMetrics.FIELD_START, end = SearchBarMetrics.FIELD_END),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = hintColor,
                        modifier = Modifier.size(SearchBarMetrics.ICON),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = SearchBarMetrics.ICON_TEXT_GAP),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(text = placeholder, color = hintColor, fontSize = 16.sp)
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(SearchBarMetrics.CLEAR)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onClearQuery,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(SearchBarMetrics.CLEAR_PLATE)
                                    .clip(CircleShape)
                                    .background(colorResource(R.color.search_clear_button_background)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.search_clear),
                                    tint = colorResource(R.color.search_clear_icon),
                                    modifier = Modifier.size(SearchBarMetrics.CLEAR_ICON),
                                )
                            }
                        }
                    }
                }
            },
        )
    }
}

/** Matches the people-and-places bottom search field. */
private object SearchBarMetrics {
    val ICON = 20.dp
    val CLEAR = 28.dp
    val CLEAR_PLATE = 20.dp
    val CLEAR_ICON = 12.dp
    val FIELD_START = 12.dp
    val FIELD_END = 4.dp
    val ICON_TEXT_GAP = 8.dp
}
