package com.marshall.pyerite.agentsModule.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import com.marshall.pyerite.R
import com.marshall.pyerite.agentsModule.model.AgentChoice
import com.marshall.pyerite.agentsModule.model.AgentsConfig
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel

@Composable
internal fun AgentsChoiceRow(
    label: String,
    selectedLabel: String,
    options: List<AgentChoice>,
    anyLabel: String,
    showDivider: Boolean,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorBottomPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val view = LocalView.current
    val edgeGap = dimensionResource(R.dimen.search_bar_vertical_padding)
    val valueTextSize = dimensionResource(R.dimen.sub_menu_value_text_size).value.sp
    val chevronSize = dimensionResource(R.dimen.detail_row_chevron_size)
    val trailingColor = colorResource(R.color.hint_text)
    val expandedChevronColor = colorResource(R.color.text_primary)
    val spaceBelow = with(density) { (view.height - anchorBottomPx).toDp() } - edgeGap
    val maxMenuHeight = spaceBelow.coerceAtLeast(0.dp)
    val menuWidth = dimensionResource(R.dimen.top_bar_dropdown_wide_min_width)
        .coerceAtMost(with(density) { view.width.toDp() } - edgeGap)
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = label,
            showChevron = false,
            onClick = { expanded = true },
        ),
        showDivider = showDivider,
        trailingContent = {
            Box(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    val bottom = coordinates.positionInWindow().y + coordinates.size.height
                    anchorBottomPx = bottom.roundToInt()
                },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selectedLabel,
                        color = trailingColor,
                        fontSize = valueTextSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.detail_row_trailing_gap)))
                    Icon(
                        modifier = Modifier.size(chevronSize),
                        imageVector = if (expanded) {
                            Icons.Filled.KeyboardArrowDown
                        } else {
                            Icons.AutoMirrored.Filled.KeyboardArrowRight
                        },
                        contentDescription = null,
                        tint = if (expanded) expandedChevronColor else trailingColor,
                    )
                }
                if (expanded) {
                    Popup(
                        popupPositionProvider = BelowButtonPopupPositionProvider,
                        onDismissRequest = { expanded = false },
                        properties = PopupProperties(focusable = true),
                    ) {
                        val menuShape = RoundedCornerShape(
                            dimensionResource(R.dimen.top_bar_dropdown_corner),
                        )
                        Column(
                            modifier = Modifier
                                .width(menuWidth)
                                .heightIn(max = maxMenuHeight)
                                .shadow(
                                    elevation = dimensionResource(R.dimen.copy_name_menu_elevation),
                                    shape = menuShape,
                                )
                                .clip(menuShape)
                                .background(colorResource(R.color.second_background))
                                .verticalScroll(rememberScrollState()),
                        ) {
                            AgentsMenuItem(
                                text = anyLabel,
                                onClick = {
                                    onSelect(null)
                                    expanded = false
                                },
                            )
                            options.forEach { option ->
                                AgentsMenuItem(
                                    text = option.name,
                                    onClick = {
                                        onSelect(option.id)
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun AgentsMenuItem(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = colorResource(R.color.text_primary),
        fontSize = dimensionResource(R.dimen.sub_menu_value_text_size).value.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.top_bar_dropdown_divider_horizontal_padding),
                vertical = dimensionResource(R.dimen.detail_row_vertical_padding_single_line),
            ),
    )
}

private object BelowButtonPopupPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val aligned = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.right - popupContentSize.width
        } else {
            anchorBounds.left
        }
        return IntOffset(aligned.coerceIn(0, maxX), anchorBounds.bottom)
    }
}

@StringRes
internal fun agentTypeNameRes(typeId: Int): Int = when (typeId) {
    AgentsConfig.TYPE_NON_AGENT -> R.string.agents_type_non_agent
    AgentsConfig.TYPE_BASIC -> R.string.agents_type_basic
    AgentsConfig.TYPE_TUTORIAL -> R.string.agents_type_tutorial
    AgentsConfig.TYPE_RESEARCH -> R.string.agents_type_research
    AgentsConfig.TYPE_CONCORD -> R.string.agents_type_concord
    AgentsConfig.TYPE_GENERIC_STORYLINE -> R.string.agents_type_generic_storyline
    AgentsConfig.TYPE_STORYLINE -> R.string.agents_type_storyline
    AgentsConfig.TYPE_EVENT -> R.string.agents_type_event
    AgentsConfig.TYPE_FACTION_WARFARE -> R.string.agents_type_faction_warfare
    AgentsConfig.TYPE_EPIC_ARC -> R.string.agents_type_epic_arc
    AgentsConfig.TYPE_AURA -> R.string.agents_type_aura
    AgentsConfig.TYPE_CAREER -> R.string.agents_type_career
    AgentsConfig.TYPE_HERALDRY -> R.string.agents_type_heraldry
    else -> R.string.agents_type_other
}

@StringRes
internal fun agentTypeShortRes(typeId: Int): Int = when (typeId) {
    AgentsConfig.TYPE_NON_AGENT -> R.string.agents_type_short_non_agent
    AgentsConfig.TYPE_BASIC -> R.string.agents_type_short_basic
    AgentsConfig.TYPE_TUTORIAL -> R.string.agents_type_short_tutorial
    AgentsConfig.TYPE_RESEARCH -> R.string.agents_type_short_research
    AgentsConfig.TYPE_CONCORD -> R.string.agents_type_short_concord
    AgentsConfig.TYPE_GENERIC_STORYLINE -> R.string.agents_type_short_generic_storyline
    AgentsConfig.TYPE_STORYLINE -> R.string.agents_type_short_storyline
    AgentsConfig.TYPE_EVENT -> R.string.agents_type_short_event
    AgentsConfig.TYPE_FACTION_WARFARE -> R.string.agents_type_short_faction_warfare
    AgentsConfig.TYPE_EPIC_ARC -> R.string.agents_type_short_epic_arc
    AgentsConfig.TYPE_AURA -> R.string.agents_type_short_aura
    AgentsConfig.TYPE_CAREER -> R.string.agents_type_short_career
    AgentsConfig.TYPE_HERALDRY -> R.string.agents_type_short_heraldry
    else -> R.string.agents_type_short_other
}

@Composable
internal fun agentLevelColor(level: Int): Color = colorResource(
    when (level) {
        1 -> R.color.agents_tag_level_1
        2 -> R.color.agents_tag_level_2
        3 -> R.color.agents_tag_level_3
        4 -> R.color.agents_tag_level_4
        5 -> R.color.agents_tag_level_5
        else -> R.color.agents_tag_level_1
    },
)

@Composable
internal fun agentDivisionColor(divisionId: Int): Color = colorResource(
    when (divisionId) {
        AgentsConfig.DIVISION_RESEARCH -> R.color.agents_tag_division_research
        AgentsConfig.DIVISION_DISTRIBUTION -> R.color.agents_tag_division_distribution
        AgentsConfig.DIVISION_MINING -> R.color.agents_tag_division_mining
        AgentsConfig.DIVISION_SECURITY -> R.color.agents_tag_division_security
        else -> R.color.agents_tag_division_other
    },
)

@Composable
internal fun agentTypeColor(typeId: Int): Color = colorResource(
    when (typeId) {
        AgentsConfig.TYPE_NON_AGENT -> R.color.agents_tag_type_gray
        AgentsConfig.TYPE_BASIC -> R.color.agents_tag_type_green
        AgentsConfig.TYPE_TUTORIAL -> R.color.agents_tag_type_blue
        AgentsConfig.TYPE_RESEARCH -> R.color.agents_tag_type_blue
        AgentsConfig.TYPE_CONCORD -> R.color.agents_tag_type_brown
        AgentsConfig.TYPE_GENERIC_STORYLINE -> R.color.agents_tag_type_green
        AgentsConfig.TYPE_STORYLINE -> R.color.agents_tag_type_green
        AgentsConfig.TYPE_EVENT -> R.color.agents_tag_type_yellow
        AgentsConfig.TYPE_FACTION_WARFARE -> R.color.agents_tag_type_cyan
        AgentsConfig.TYPE_EPIC_ARC -> R.color.agents_tag_type_mint
        AgentsConfig.TYPE_AURA -> R.color.agents_tag_type_indigo
        AgentsConfig.TYPE_CAREER -> R.color.agents_tag_type_teal
        AgentsConfig.TYPE_HERALDRY -> R.color.agents_tag_type_brown
        else -> R.color.agents_tag_type_gray
    },
)

@Composable
internal fun AgentTag(
    text: String,
    background: Color,
    content: Color,
) {
    val textSize = dimensionResource(R.dimen.agents_tag_text_size).value.sp
    Text(
        text = text,
        color = content,
        fontSize = textSize,
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.agents_tag_corner)))
            .background(background)
            .padding(
                horizontal = dimensionResource(R.dimen.agents_tag_horizontal_padding),
                vertical = dimensionResource(R.dimen.agents_tag_vertical_padding),
            ),
    )
}
