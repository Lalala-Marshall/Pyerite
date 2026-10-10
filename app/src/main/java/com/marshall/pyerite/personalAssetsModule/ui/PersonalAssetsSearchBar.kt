package com.marshall.pyerite.personalAssetsModule.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R

/**
 * The bar is already placed with the IME inset. Propagating bring-into-view
 * asks the window to pan as well, which leaves a blank band above the keyboard.
 */
private fun Modifier.keepSearchFieldDocked(): Modifier = this.then(KeepSearchFieldDockedElement)

private object KeepSearchFieldDockedElement : ModifierNodeElement<KeepSearchFieldDockedNode>() {
    override fun create(): KeepSearchFieldDockedNode = KeepSearchFieldDockedNode()

    override fun update(node: KeepSearchFieldDockedNode) = Unit

    override fun hashCode(): Int = 0

    override fun equals(other: Any?): Boolean = other === this
}

private class KeepSearchFieldDockedNode : Modifier.Node(), BringIntoViewModifierNode {
    override suspend fun bringIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> Rect?,
    ) = Unit
}

@Composable
internal fun DockPersonalAssetsSearchField() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findHostActivity()?.window
        val previousMode = window?.attributes?.softInputMode
        if (window != null && previousMode != null) {
            val dockedMode = previousMode and
                WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST.inv() or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            window.setSoftInputMode(dockedMode)
        }
        onDispose {
            if (window != null && previousMode != null) {
                window.setSoftInputMode(previousMode)
            }
        }
    }
}

@Composable
internal fun PersonalAssetsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barHeight = dimensionResource(R.dimen.search_bar_height)
    val horizontalPadding = dimensionResource(R.dimen.detail_card_horizontal_padding)
    val corner = dimensionResource(R.dimen.detail_card_corner_radius)
    val iconSize = dimensionResource(R.dimen.detail_row_chevron_size)
    val fieldBackground = colorResource(R.color.search_field_background)
    val textColor = colorResource(R.color.text_primary)
    val hintColor = colorResource(R.color.hint_text)
    val textSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp

    Box(
        modifier = modifier
            .keepSearchFieldDocked()
            .padding(horizontal = horizontalPadding),
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(corner))
                .background(fieldBackground),
            textStyle = TextStyle(color = textColor, fontSize = textSize),
            singleLine = true,
            cursorBrush = SolidColor(textColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = dimensionResource(R.dimen.search_bar_cancel_gap)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = hintColor,
                        modifier = Modifier.size(iconSize),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = dimensionResource(R.dimen.search_bar_cancel_gap)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.corporation_assets_search_hint),
                                color = hintColor,
                                fontSize = textSize,
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(dimensionResource(R.dimen.top_bar_back_button_size))
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onQueryChange("") },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.search_clear),
                                tint = colorResource(R.color.search_clear_icon),
                                modifier = Modifier.size(iconSize),
                            )
                        }
                    }
                }
            },
        )
    }
}

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
