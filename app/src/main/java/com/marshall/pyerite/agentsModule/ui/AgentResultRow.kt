package com.marshall.pyerite.agentsModule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.marshall.pyerite.R
import com.marshall.pyerite.agentsModule.model.AgentItem
import com.marshall.pyerite.agentsModule.model.AgentsConfig
import com.marshall.pyerite.agentsModule.model.displaySecurity
import com.marshall.pyerite.agentsModule.model.formatDisplaySecurity
import com.marshall.pyerite.esiModule.data.portraitUrl
import com.marshall.pyerite.iconModule.manager.IconManager
import org.koin.compose.koinInject

@Composable
internal fun agentsSecurityColor(security: Double): Color {
    val display = displaySecurity(security)
    return when {
        display < AgentsConfig.SECURITY_NULL_EXCLUSIVE_MAX ->
            colorResource(R.color.character_security_negative)
        display < AgentsConfig.SECURITY_HIGH_MIN ->
            colorResource(R.color.character_security_low)
        else -> colorResource(R.color.character_security_high)
    }
}

@Composable
internal fun agentsPlaceLabel(security: Double?, placeName: String): AnnotatedString {
    val name = placeName.ifBlank { stringResource(R.string.agents_unknown) }
    val textColor = colorResource(R.color.hint_text)
    return buildAnnotatedString {
        if (security != null) {
            withStyle(SpanStyle(color = agentsSecurityColor(security), fontWeight = FontWeight.Bold)) {
                append(formatDisplaySecurity(security))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AgentResultRow(
    agent: AgentItem,
    showDivider: Boolean,
    iconManager: IconManager = koinInject(),
) {
    val portraitSize = dimensionResource(R.dimen.agents_portrait_size)
    val hintIcon = dimensionResource(R.dimen.base_lazy_column_item_hint_icon_size)
    val titleSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp
    val hintSize = dimensionResource(R.dimen.detail_row_label_subtitle_text_size).value.sp
    val hintColor = colorResource(R.color.hint_text)
    val horizontal = dimensionResource(R.dimen.detail_row_horizontal_padding)
    val vertical = dimensionResource(R.dimen.detail_row_vertical_padding_multi_line)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontal, vertical = vertical),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.detail_row_icon_gap)),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(portraitSize)
                    .clip(CircleShape)
                    .background(colorResource(R.color.border)),
            ) {
                AsyncImage(
                    model = portraitUrl(agent.id.toLong()),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.detail_row_label_subtitle_spacing),
                ),
            ) {
                Text(
                    text = agent.name.ifBlank { stringResource(R.string.agents_unknown) },
                    color = colorResource(R.color.text_primary),
                    fontSize = titleSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = agentsPlaceLabel(agent.security, agent.locationName),
                    fontSize = hintSize,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.base_lazy_column_item_hint_icon_gap),
                    ),
                ) {
                    AgentOrgIcon(agent.factionIcon, hintIcon, iconManager)
                    Text(
                        text = agent.factionName,
                        color = hintColor,
                        fontSize = hintSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = stringResource(R.string.agents_org_separator),
                        color = hintColor,
                        fontSize = hintSize,
                    )
                    AgentOrgIcon(agent.corporationIcon, hintIcon, iconManager)
                    Text(
                        text = agent.corporationName,
                        color = hintColor,
                        fontSize = hintSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.agents_tag_spacing)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.agents_tag_spacing)),
                ) {
                    AgentTag(
                        text = stringResource(R.string.agents_level_tag, agent.level),
                        background = agentLevelColor(agent.level),
                        content = colorResource(R.color.agents_tag_on_solid),
                    )
                    if (agent.divisionName.isNotBlank()) {
                        AgentTag(
                            text = agent.divisionName,
                            background = agentDivisionColor(agent.divisionId),
                            content = colorResource(R.color.agents_tag_on_solid),
                        )
                    }
                    if (agent.agentType != AgentsConfig.BASIC_AGENT_TYPE) {
                        AgentTag(
                            text = stringResource(agentTypeShortRes(agent.agentType)),
                            background = agentTypeColor(agent.agentType),
                            content = colorResource(R.color.agents_tag_on_solid),
                        )
                    }
                    if (agent.isLocator) {
                        AgentTag(
                            text = stringResource(R.string.agents_tag_locator),
                            background = colorResource(R.color.agents_tag_locator_background),
                            content = colorResource(R.color.agents_tag_locator_text),
                        )
                    }
                    if (agent.isSpaceAgent) {
                        AgentTag(
                            text = stringResource(R.string.agents_tag_space),
                            background = colorResource(R.color.agents_tag_space_background),
                            content = colorResource(R.color.agents_tag_space_text),
                        )
                    }
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = dimensionResource(R.dimen.detail_divider_thickness),
                color = colorResource(R.color.border),
            )
        }
    }
}

@Composable
private fun AgentOrgIcon(
    fileName: String?,
    size: androidx.compose.ui.unit.Dp,
    iconManager: IconManager,
) {
    val file = fileName?.let { iconManager.getIconFile(it) } ?: return
    AsyncImage(
        model = file,
        contentDescription = null,
        modifier = Modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}
