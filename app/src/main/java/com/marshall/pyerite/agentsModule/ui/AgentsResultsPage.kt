package com.marshall.pyerite.agentsModule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.agentsModule.data.resolveAgentsPage
import com.marshall.pyerite.agentsModule.model.AgentGroupRow
import com.marshall.pyerite.agentsModule.model.AgentsConfig
import com.marshall.pyerite.agentsModule.model.AgentsResolvedPage
import com.marshall.pyerite.agentsModule.model.AgentsResultLevel
import com.marshall.pyerite.agentsModule.navHost.AgentsNavArgs
import com.marshall.pyerite.agentsModule.navHost.AgentsRoute
import com.marshall.pyerite.agentsModule.navHost.rememberAgentsRootBackStackEntry
import com.marshall.pyerite.agentsModule.navHost.toOptionalAgentId
import com.marshall.pyerite.agentsModule.viewModel.AgentsViewModel
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.util.NumberDisplayFormatter
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.rememberLazyListTitleCollapsed
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun AgentsResultsPage(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    viewModel: AgentsViewModel = koinViewModel(
        viewModelStoreOwner = rememberAgentsRootBackStackEntry(navController, backStackEntry),
    ),
    localeController: LocaleController = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val args = backStackEntry.arguments
    val factionId = args?.getInt(AgentsNavArgs.FACTION_ID)?.toOptionalAgentId()
    val corporationId = args?.getLong(AgentsNavArgs.CORPORATION_ID)?.toOptionalAgentId()
    val divisionId = args?.getInt(AgentsNavArgs.DIVISION_ID)?.toOptionalAgentId()
    val agentLevel = args?.getInt(AgentsNavArgs.AGENT_LEVEL)?.toOptionalAgentId()
    val levelNames = HashMap<Int, String>(AgentsConfig.LEVEL_MAX)
    for (level in AgentsConfig.LEVEL_MIN..AgentsConfig.LEVEL_MAX) {
        levelNames[level] = stringResource(R.string.agents_level_name, level)
    }
    val page = resolveAgentsPage(
        agents = uiState.results,
        factionId = factionId,
        corporationId = corporationId,
        divisionId = divisionId,
        agentLevel = agentLevel,
        levelName = { level -> levelNames[level] ?: level.toString() },
        language = localeController.contentLanguage,
    )
    val title = page.title ?: stringResource(R.string.agents_results)
    val listState = rememberLazyListState()
    val onBack = navController.rememberNavigateUpAction()
    val listEmpty = if (page.level == AgentsResultLevel.AGENT) {
        page.agents.isEmpty()
    } else {
        page.groups.isEmpty()
    }

    PyeritePageScaffold(
        title = title,
        showCollapsedTitle = rememberLazyListTitleCollapsed(listState),
        onBack = onBack,
    ) { topBarPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topBarPadding.calculateTopPadding()),
        ) {
            item { PageTitle(text = title) }
            when {
                uiState.loading && uiState.results.isEmpty() -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(R.dimen.type_detail_section_gap)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = colorResource(R.color.hyperlink_text))
                    }
                }
                listEmpty -> item {
                    Text(
                        text = stringResource(R.string.agents_empty),
                        color = colorResource(R.color.hint_text),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(R.dimen.type_detail_section_gap)),
                    )
                }
                page.level == AgentsResultLevel.AGENT -> items(
                    page.agents,
                    key = { "agent-${it.id}" },
                ) { agent ->
                    ResultCardRow(first = agent == page.agents.first(), last = agent == page.agents.last()) {
                        AgentResultRow(
                            agent = agent,
                            showDivider = agent != page.agents.last(),
                        )
                    }
                }
                else -> items(page.groups, key = { "group-${page.level}-${it.id}" }) { group ->
                    ResultCardRow(first = group == page.groups.first(), last = group == page.groups.last()) {
                        AgentGroupListRow(
                            group = group,
                            showDivider = group != page.groups.last(),
                            onClick = { navController.navigate(routeForGroup(page, group)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultCardRow(
    first: Boolean,
    last: Boolean,
    content: @Composable () -> Unit,
) {
    val radius = dimensionResource(R.dimen.detail_card_corner_radius)
    val shape = RoundedCornerShape(
        topStart = if (first) radius else 0.dp,
        topEnd = if (first) radius else 0.dp,
        bottomStart = if (last) radius else 0.dp,
        bottomEnd = if (last) radius else 0.dp,
    )
    Box(
        modifier = Modifier
            .padding(horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding))
            .clip(shape)
            .background(colorResource(R.color.second_background)),
    ) {
        content()
    }
}

@Composable
private fun AgentGroupListRow(
    group: AgentGroupRow,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    val count = NumberDisplayFormatter.format(group.count.toLong(), NumberDisplayFormatter.Style.FULL)
    val levelNumber = group.levelNumber
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = !group.iconFileName.isNullOrBlank(),
            iconFileName = group.iconFileName,
            itemName = group.name,
            itemHint = stringResource(R.string.agents_count, count),
            onClick = onClick,
        ),
        showDivider = showDivider,
        leadingContent = if (levelNumber != null) {
            { iconSize ->
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(CircleShape)
                        .background(agentLevelColor(levelNumber)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = levelNumber.toString(),
                        color = colorResource(R.color.agents_tag_on_solid),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp,
                    )
                }
            }
        } else {
            null
        },
    )
}

private fun routeForGroup(page: AgentsResolvedPage, group: AgentGroupRow): String = when (page.level) {
    AgentsResultLevel.FACTION -> AgentsRoute.Results.create(factionId = group.id.toInt())
    AgentsResultLevel.CORPORATION -> AgentsRoute.Results.create(
        factionId = page.factionId ?: AgentsConfig.UNSET_ID,
        corporationId = group.id,
    )
    AgentsResultLevel.DIVISION -> AgentsRoute.Results.create(
        factionId = page.factionId ?: AgentsConfig.UNSET_ID,
        corporationId = page.corporationId ?: AgentsConfig.UNSET_ID.toLong(),
        divisionId = group.id.toInt(),
    )
    AgentsResultLevel.LEVEL -> AgentsRoute.Results.create(
        factionId = page.factionId ?: AgentsConfig.UNSET_ID,
        corporationId = page.corporationId ?: AgentsConfig.UNSET_ID.toLong(),
        divisionId = page.divisionId ?: AgentsConfig.UNSET_ID,
        agentLevel = group.id.toInt(),
    )
    AgentsResultLevel.AGENT -> AgentsRoute.Results.create()
}
