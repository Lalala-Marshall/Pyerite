package com.marshall.pyerite.ui.golbalComponents

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R

@Composable
fun PyeriteTopBarPortraitButton(
    portraitUrl: String?,
    contentDescription: String,
    onClick: () -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val size = dimensionResource(R.dimen.top_bar_back_button_size)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(
            initialScale = TopBarPortraitMotion.ENTER_SCALE,
            transformOrigin = TopBarPortraitMotion.ORIGIN,
        ),
        exit = fadeOut() + scaleOut(
            targetScale = TopBarPortraitMotion.ENTER_SCALE,
            transformOrigin = TopBarPortraitMotion.ORIGIN,
        ),
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .topBarActionSurface(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .semantics(mergeDescendants = true) { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            CharacterAvatar(
                portraitUrl = portraitUrl,
                size = size,
                contentDescription = contentDescription,
            )
        }
    }
}

@Immutable
data class PyeriteTopBarMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    @param:DrawableRes val iconRes: Int? = null,
    /** When false, [iconRes] is drawn without tint (bitmaps / multicolor assets). */
    val tintIcon: Boolean = true,
    val supportingLabel: String? = null,
    val trailingIcon: ImageVector? = null,
    val trailingContentDescription: String? = null,
    val onTrailingClick: (() -> Unit)? = null,
    val enabled: Boolean = true,
    /** When true and this is not the last item, a divider is drawn below the row. */
    val showDividerBelow: Boolean = true,
)

@Immutable
data class PyeriteTopBarActionItem(
    val onClick: () -> Unit,
    val icon: ImageVector,
    val contentDescription: String,
    val label: String? = null,
    val accentColor: Color = Color.Unspecified,
    val iconTint: Color = Color.Unspecified,
    val enabled: Boolean = true,
    val iconBadge: Boolean = false,
    val showIcon: Boolean = true,
    val spinning: Boolean = false,
    /** When non-empty, tapping the action opens an anchored dropdown instead of only [onClick]. */
    val menuItems: List<PyeriteTopBarMenuItem> = emptyList(),
)

/**
 * Top bar action cluster: circle for a lone icon-only action; pill when any label
 * is present or multiple actions are grouped on the same side.
 */
@Composable
fun PyeriteTopBarActions(
    actions: List<PyeriteTopBarActionItem>,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return

    val usePill = actions.size > 1 || actions.any { !it.label.isNullOrBlank() }
    val actionHeight = dimensionResource(R.dimen.top_bar_back_button_size)

    if (!usePill) {
        TopBarActionSegment(
            action = actions.single(),
            inPill = false,
            modifier = modifier
                .size(actionHeight)
                .topBarActionSurface(CircleShape),
        )
        return
    }

    val pillShape = RoundedCornerShape(percent = 50)
    val horizontalPadding = dimensionResource(R.dimen.top_bar_action_pill_horizontal_padding)

    Row(
        modifier = modifier
            .height(actionHeight)
            .topBarActionSurface(pillShape)
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.top_bar_action_pill_item_gap)),
    ) {
        actions.forEach { action ->
            TopBarActionSegment(
                action = action,
                inPill = true,
            )
        }
    }
}

@Composable
private fun TopBarActionSegment(
    action: PyeriteTopBarActionItem,
    inPill: Boolean,
    modifier: Modifier = Modifier,
) {
    val buttonSize = dimensionResource(R.dimen.top_bar_back_button_size)
    val iconSize = dimensionResource(R.dimen.top_bar_icon_size)
    val badgeSize = dimensionResource(R.dimen.top_bar_update_badge_size)
    val badgeIconSize = dimensionResource(R.dimen.top_bar_update_badge_icon_size)
    val textStartPadding = dimensionResource(R.dimen.top_bar_update_text_start_padding)
    val textEndPadding = dimensionResource(R.dimen.top_bar_update_text_end_padding)
    val textEndGap = dimensionResource(R.dimen.top_bar_update_text_end_gap)
    val defaultIconTint = colorResource(R.color.text_primary)
    val accentColor = if (action.accentColor != Color.Unspecified) {
        action.accentColor
    } else {
        defaultIconTint
    }
    val iconTint = if (action.iconTint != Color.Unspecified) {
        action.iconTint
    } else {
        defaultIconTint
    }
    val hasLabel = !action.label.isNullOrBlank()
    val hasMenu = action.menuItems.isNotEmpty()
    var menuExpanded by remember { mutableStateOf(false) }
    val onActionClick: () -> Unit = {
        if (hasMenu) {
            menuExpanded = true
        }
        action.onClick()
    }

    val content: @Composable () -> Unit = {
        if (hasLabel) {
            Text(
                text = requireNotNull(action.label),
                color = accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.width(textEndGap))
        }

        if (action.iconBadge) {
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.contentDescription,
                    tint = colorResource(R.color.white),
                    modifier = Modifier.size(badgeIconSize),
                )
            }
        } else if (action.showIcon) {
            if (action.spinning) {
                SpinningTopBarIcon(
                    imageVector = action.icon,
                    contentDescription = action.contentDescription,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize),
                )
            } else {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.contentDescription,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }

    Box(modifier = modifier) {
        if (hasLabel) {
            Row(
                modifier = Modifier
                    .clickable(
                        enabled = action.enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onActionClick,
                    )
                    .semantics { role = Role.Button }
                    .padding(
                        start = if (inPill) textStartPadding else 0.dp,
                        end = if (inPill) textEndPadding else 0.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        } else {
            Box(
                modifier = Modifier
                    .then(
                        if (inPill) {
                            Modifier.size(buttonSize)
                        } else {
                            Modifier.fillMaxSize()
                        },
                    )
                    .clickable(
                        enabled = action.enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onActionClick,
                    )
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
        if (hasMenu) {
            val menuCorner = dimensionResource(R.dimen.top_bar_dropdown_corner)
            val menuMinWidth = if (action.menuItems.any { !it.supportingLabel.isNullOrBlank() }) {
                dimensionResource(R.dimen.top_bar_dropdown_wide_min_width)
            } else {
                dimensionResource(R.dimen.top_bar_dropdown_min_width)
            }
            val dividerHorizontalPadding =
                dimensionResource(R.dimen.top_bar_dropdown_divider_horizontal_padding)
            val menuIconSize = dimensionResource(R.dimen.top_bar_dropdown_item_icon_size)
            val menuIconTint = colorResource(R.color.text_primary)
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.width(menuMinWidth),
                shape = RoundedCornerShape(menuCorner),
                containerColor = colorResource(R.color.second_background),
            ) {
                action.menuItems.forEachIndexed { index, item ->
                    val hasLeading = item.icon != null || item.iconRes != null
                    DropdownMenuItem(
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = item.label,
                                    color = colorResource(R.color.text_primary),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Start,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                val supporting = item.supportingLabel
                                if (!supporting.isNullOrBlank()) {
                                    Text(
                                        text = supporting,
                                        color = colorResource(R.color.text_caption),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Start,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        },
                        leadingIcon = if (hasLeading) {
                            {
                                TopBarDropdownMenuIcon(
                                    item = item,
                                    size = menuIconSize,
                                    tint = menuIconTint,
                                )
                            }
                        } else {
                            null
                        },
                        trailingIcon = item.trailingIcon?.let { trailing ->
                            {
                                Icon(
                                    imageVector = trailing,
                                    contentDescription = item.trailingContentDescription,
                                    tint = menuIconTint,
                                    modifier = Modifier
                                        .size(menuIconSize)
                                        .clickable(
                                            enabled = item.onTrailingClick != null,
                                            onClick = {
                                                item.onTrailingClick?.invoke()
                                            },
                                        ),
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            item.onClick()
                        },
                        enabled = item.enabled,
                        contentPadding = MenuDefaults.DropdownMenuItemContentPadding,
                    )
                    if (index != action.menuItems.lastIndex && item.showDividerBelow) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = dividerHorizontalPadding),
                            thickness = dimensionResource(R.dimen.detail_divider_thickness),
                            color = colorResource(R.color.border),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarDropdownMenuIcon(
    item: PyeriteTopBarMenuItem,
    size: Dp,
    tint: Color,
) {
    when {
        item.icon != null -> {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (item.tintIcon) tint else Color.Unspecified,
                modifier = Modifier.size(size),
            )
        }
        item.iconRes != null -> {
            Icon(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                tint = if (item.tintIcon) tint else Color.Unspecified,
                modifier = Modifier.size(size),
            )
        }
    }
}

@Composable
private fun SpinningTopBarIcon(
    imageVector: ImageVector,
    contentDescription: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "top_bar_action_spin")
    val degrees by infiniteTransition.animateFloat(
        initialValue = TopBarActionSpinConfig.PROGRESS_MIN,
        targetValue = TopBarActionSpinConfig.FULL_CIRCLE_DEGREES,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = TopBarActionSpinConfig.DURATION_MS,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "top_bar_action_spin_degrees",
    )
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.graphicsLayer { rotationZ = degrees },
    )
}

private object TopBarActionSpinConfig {
    const val PROGRESS_MIN = 0f
    const val FULL_CIRCLE_DEGREES = 360f
    const val DURATION_MS = 1_000
}

private object TopBarPortraitMotion {
    const val ENTER_SCALE = 0.72f
    val ORIGIN = TransformOrigin(0f, 0.5f)
}

@Composable
fun Modifier.topBarActionSurface(shape: Shape): Modifier {
    return this
        .shadow(elevation = 2.dp, shape = shape, clip = false)
        .clip(shape)
        .background(colorResource(R.color.second_background))
}
