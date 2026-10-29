package com.marshall.pyerite.agentsModule.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.agentsModule.model.AgentChoice
import com.marshall.pyerite.agentsModule.model.AgentsConfig
import com.marshall.pyerite.agentsModule.model.AgentsSecurityBand
import com.marshall.pyerite.agentsModule.navHost.AgentsRoute
import com.marshall.pyerite.agentsModule.navHost.rememberAgentsRootBackStackEntry
import com.marshall.pyerite.agentsModule.viewModel.AgentsViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.rememberLazyListTitleCollapsed
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun AgentsSearchPage(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    viewModel: AgentsViewModel = koinViewModel(
        viewModelStoreOwner = rememberAgentsRootBackStackEntry(navController, backStackEntry),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val title = stringResource(R.string.agents)
    val anyLabel = stringResource(R.string.agents_any)
    val onBack = navController.rememberNavigateUpAction()
    val filter = uiState.filter
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val cardPadding = dimensionResource(R.dimen.detail_card_horizontal_padding)

    PyeritePageScaffold(
        title = title,
        showCollapsedTitle = rememberLazyListTitleCollapsed(listState),
        onBack = onBack,
    ) { topBarPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topBarPadding.calculateTopPadding()),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
            ) {
                item { PageTitle(text = title) }
                item {
                    BaseContainer(
                        title = stringResource(R.string.agents_filters),
                        useSystemBarsPadding = false,
                    ) {
                        AgentsChoiceRow(
                            label = stringResource(R.string.agents_filter_level),
                            selectedLabel = filter.level?.let {
                                stringResource(R.string.agents_level_name, it)
                            } ?: anyLabel,
                            options = (AgentsConfig.LEVEL_MIN..AgentsConfig.LEVEL_MAX).map { level ->
                                AgentChoice(level.toLong(), stringResource(R.string.agents_level_name, level))
                            },
                            anyLabel = anyLabel,
                            showDivider = true,
                            onSelect = { viewModel.setLevel(it?.toInt()) },
                        )
                        AgentsChoiceRow(
                            label = stringResource(R.string.agents_filter_security),
                            selectedLabel = filter.security.securityLabel() ?: anyLabel,
                            options = AgentsSecurityBand.entries.map { band ->
                                AgentChoice(band.ordinal.toLong(), band.securityLabel().orEmpty())
                            },
                            anyLabel = anyLabel,
                            showDivider = true,
                            onSelect = { id ->
                                viewModel.setSecurity(
                                    id?.let { AgentsSecurityBand.entries[it.toInt()] },
                                )
                            },
                        )
                        AgentsChoiceRow(
                            label = stringResource(R.string.agents_filter_faction),
                            selectedLabel = uiState.factions
                                .firstOrNull { it.id == filter.factionId?.toLong() }
                                ?.name ?: anyLabel,
                            options = uiState.factions,
                            anyLabel = anyLabel,
                            showDivider = true,
                            onSelect = { viewModel.setFaction(it?.toInt()) },
                        )
                        if (filter.factionId != null) {
                            AgentsChoiceRow(
                                label = stringResource(R.string.agents_filter_corporation),
                                selectedLabel = uiState.corporations
                                    .firstOrNull { it.id == filter.corporationId }
                                    ?.name ?: anyLabel,
                                options = uiState.corporations,
                                anyLabel = anyLabel,
                                showDivider = true,
                                onSelect = { viewModel.setCorporation(it) },
                            )
                        }
                        AgentsChoiceRow(
                            label = stringResource(R.string.agents_filter_division),
                            selectedLabel = uiState.divisions
                                .firstOrNull { it.id == filter.divisionId?.toLong() }
                                ?.name ?: anyLabel,
                            options = uiState.divisions,
                            anyLabel = anyLabel,
                            showDivider = true,
                            onSelect = { viewModel.setDivision(it?.toInt()) },
                        )
                        AgentsChoiceRow(
                            label = stringResource(R.string.agents_filter_type),
                            selectedLabel = filter.agentType?.let { stringResource(agentTypeNameRes(it)) } ?: anyLabel,
                            options = uiState.agentTypes.map { typeId ->
                                AgentChoice(typeId.toLong(), stringResource(agentTypeNameRes(typeId)))
                            },
                            anyLabel = anyLabel,
                            showDivider = false,
                            onSelect = { viewModel.setAgentType(it?.toInt()) },
                        )
                    }
                }
                item {
                    BaseContainer(
                        title = null,
                        useSystemBarsPadding = false,
                        modifier = Modifier.padding(top = sectionGap),
                    ) {
                        AgentsSwitchRow(
                            label = stringResource(R.string.agents_locator_only),
                            hint = stringResource(R.string.agents_locator_hint),
                            checked = filter.locatorOnly,
                            showDivider = true,
                            onCheckedChange = viewModel::setLocatorOnly,
                        )
                        AgentsSwitchRow(
                            label = stringResource(R.string.agents_space_only),
                            hint = stringResource(R.string.agents_space_hint),
                            checked = filter.spaceOnly,
                            showDivider = false,
                            onCheckedChange = viewModel::setSpaceOnly,
                        )
                    }
                }
                item {
                    BaseContainer(
                        title = stringResource(R.string.agents_location_filter),
                        useSystemBarsPadding = false,
                        modifier = Modifier.padding(top = sectionGap),
                    ) {
                        BaseLazyColumnItem(
                            model = BaseLazyColumnItemModel(
                                showLeadingIcon = false,
                                itemName = stringResource(R.string.agents_filter_region),
                                trailingValue = uiState.selectedRegionName
                                    ?: stringResource(R.string.agents_all_regions),
                                onClick = { navController.navigate(AgentsRoute.Regions.route) },
                            ),
                            showDivider = true,
                        )
                        BaseLazyColumnItem(
                            model = BaseLazyColumnItemModel(
                                showLeadingIcon = false,
                                itemName = stringResource(R.string.agents_filter_system),
                                trailingValue = uiState.selectedSystemName ?: if (filter.regionId != null) {
                                    stringResource(R.string.agents_all_systems_in_region)
                                } else {
                                    stringResource(R.string.agents_all_systems)
                                },
                                onClick = { navController.navigate(AgentsRoute.Systems.route) },
                            ),
                            showDivider = false,
                        )
                    }
                }
            }
            Button(
                onClick = {
                    viewModel.showAgents()
                    navController.navigate(AgentsRoute.Results.create())
                },
                enabled = !uiState.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = cardPadding, vertical = sectionGap),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.agents_show_button),
                    contentColor = colorResource(R.color.agents_show_button_text),
                    disabledContainerColor = colorResource(R.color.agents_show_button),
                    disabledContentColor = colorResource(R.color.agents_show_button_text),
                ),
            ) {
                Text(text = stringResource(R.string.agents_show))
            }
        }
    }
}

@Composable
private fun AgentsSecurityBand?.securityLabel(): String? = when (this) {
    AgentsSecurityBand.HIGH -> stringResource(R.string.agents_security_high)
    AgentsSecurityBand.LOW -> stringResource(R.string.agents_security_low)
    AgentsSecurityBand.NULL -> stringResource(R.string.agents_security_null)
    null -> null
}

@Composable
private fun AgentsSwitchRow(
    label: String,
    hint: String,
    checked: Boolean,
    showDivider: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val checkedTrack = colorResource(R.color.character_status_positive)
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = label,
            itemHint = hint,
            showChevron = false,
            onClick = { onCheckedChange(!checked) },
        ),
        showDivider = showDivider,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = checkedTrack,
                    checkedBorderColor = checkedTrack,
                    checkedThumbColor = colorResource(R.color.white),
                ),
            )
        },
    )
}
