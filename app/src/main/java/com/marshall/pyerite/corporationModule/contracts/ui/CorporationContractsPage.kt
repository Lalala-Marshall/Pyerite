package com.marshall.pyerite.corporationModule.contracts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContract
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractGroupBy
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractSection
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractSectionKind
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractsDateFormatter
import com.marshall.pyerite.corporationModule.contracts.model.toSections
import com.marshall.pyerite.corporationModule.contracts.navHost.CorporationContractsRoute
import com.marshall.pyerite.corporationModule.contracts.viewModel.CorporationContractsViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarMenuItem
import com.marshall.pyerite.ui.golbalComponents.pyeritePullRefreshTopBarAction
import com.marshall.pyerite.ui.golbalComponents.rememberLazyListTitleCollapsed
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CorporationContractsPage(
    navController: NavController,
    viewModel: CorporationContractsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val pageTitle = stringResource(R.string.corporation_contracts)
    val listState = rememberLazyListState()
    val showCollapsedTitle = rememberLazyListTitleCollapsed(listState)
    val onBack = navController.rememberNavigateUpAction()
    val sections = uiState.contracts.toSections(uiState.filter)
    val nowMs = System.currentTimeMillis()
    val showBody = !uiState.permissionDenied &&
        (uiState.contracts.isNotEmpty() || (!uiState.isLoading && !uiState.loadFailed))
    val showEmpty = showBody && sections.isEmpty()
    var priceExpanded by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var sheetPage by remember { mutableStateOf(CorporationContractsSheetPage.SETTINGS) }
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val refreshAction = pyeritePullRefreshTopBarAction(
        isRefreshing = uiState.isLoading,
        refreshFailed = uiState.loadFailed,
        onRefresh = viewModel::refresh,
    )
    val groupAction = PyeriteTopBarActionItem(
        onClick = {},
        icon = Icons.AutoMirrored.Filled.Sort,
        contentDescription = stringResource(R.string.corporation_contracts_group),
        menuItems = listOf(
            PyeriteTopBarMenuItem(
                label = stringResource(R.string.corporation_contracts_group_issued),
                trailingIcon = if (uiState.filter.groupBy == CorporationContractGroupBy.ISSUED) {
                    Icons.Filled.Check
                } else {
                    null
                },
                onClick = { viewModel.setGroupBy(CorporationContractGroupBy.ISSUED) },
            ),
            PyeriteTopBarMenuItem(
                label = stringResource(R.string.corporation_contracts_group_completed),
                trailingIcon = if (uiState.filter.groupBy == CorporationContractGroupBy.COMPLETED) {
                    Icons.Filled.Check
                } else {
                    null
                },
                showDividerBelow = false,
                onClick = { viewModel.setGroupBy(CorporationContractGroupBy.COMPLETED) },
            ),
        ),
    )
    val settingsAction = PyeriteTopBarActionItem(
        onClick = {
            sheetPage = CorporationContractsSheetPage.SETTINGS
            showSettings = true
        },
        icon = Icons.Filled.Settings,
        contentDescription = stringResource(R.string.corporation_contracts_settings),
    )

    PyeritePageScaffold(
        title = pageTitle,
        showCollapsedTitle = showCollapsedTitle,
        onBack = onBack,
        endActions = listOfNotNull(refreshAction, groupAction, settingsAction),
    ) { topBarPadding ->
        PyeritePullToRefreshBox(
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(topBarPadding)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "page_title") {
                    PageTitle(text = pageTitle)
                }
                if (uiState.permissionDenied || uiState.loadFailed) {
                    item(key = "status") {
                        ContractsStatusBanner(
                            permissionDenied = uiState.permissionDenied,
                            loadFailed = uiState.loadFailed,
                            onRetry = viewModel::refresh,
                        )
                    }
                }
                if (showBody) {
                    item(key = "price_filter") {
                        Column(modifier = Modifier.padding(top = sectionGap)) {
                            CorporationContractsPriceFilter(
                                expanded = priceExpanded,
                                minPriceText = uiState.filter.minPriceText,
                                maxPriceText = uiState.filter.maxPriceText,
                                onToggle = { priceExpanded = !priceExpanded },
                                onMinPriceChange = viewModel::setMinPriceText,
                                onMaxPriceChange = viewModel::setMaxPriceText,
                            )
                        }
                    }
                }
                if (showEmpty) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.corporation_contracts_empty),
                            color = colorResource(R.color.text_primary),
                            fontSize = dimensionResource(R.dimen.type_detail_body_text_size).value.sp,
                            modifier = Modifier.padding(
                                start = dimensionResource(R.dimen.type_detail_page_title_start_padding),
                                end = dimensionResource(R.dimen.detail_card_horizontal_padding),
                                top = sectionGap,
                            ),
                        )
                    }
                }
                if (showBody) {
                    sections.forEach { section ->
                        item(key = section.id) {
                            Column(modifier = Modifier.padding(top = sectionGap)) {
                                ContractSection(
                                    section = section,
                                    nowMs = nowMs,
                                    onContractClick = { contract ->
                                        navController.navigate(
                                            CorporationContractsRoute.Detail.create(
                                                viewModel.characterId,
                                                contract.contractId,
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        CorporationContractsSettingsSheet(
            page = sheetPage,
            filter = uiState.filter,
            onOpenDisplayLimit = { sheetPage = CorporationContractsSheetPage.DISPLAY_LIMIT },
            onBack = { sheetPage = CorporationContractsSheetPage.SETTINGS },
            onSelectDisplayLimit = { limit ->
                viewModel.setDisplayLimit(limit)
                sheetPage = CorporationContractsSheetPage.SETTINGS
            },
            onToggleType = viewModel::toggleType,
            onToggleAllTypes = viewModel::toggleAllTypes,
            onToggleStatus = viewModel::toggleStatus,
            onToggleAllStatuses = viewModel::toggleAllStatuses,
            onDismiss = {
                sheetPage = CorporationContractsSheetPage.SETTINGS
                showSettings = false
            },
        )
    }
}

@Composable
private fun ContractSection(
    section: CorporationContractSection,
    nowMs: Long,
    onContractClick: (CorporationContract) -> Unit,
) {
    BaseContainer(
        title = sectionTitle(section),
        useSystemBarsPadding = false,
    ) {
        section.contracts.forEachIndexed { index, contract ->
            CorporationContractRow(
                contract = contract,
                nowMs = nowMs,
                showDivider = index < section.contracts.lastIndex,
                onClick = { onContractClick(contract) },
            )
        }
    }
}

@Composable
private fun sectionTitle(
    section: CorporationContractSection,
): String {
    val dayText = section.dayEpochMs?.let {
        CorporationContractsDateFormatter.displayDate(it)
    }.orEmpty()
    val issued = stringResource(R.string.corporation_contracts_section_issued, dayText)
    val completed = stringResource(R.string.corporation_contracts_section_completed, dayText)
    val incomplete = stringResource(R.string.corporation_contracts_section_incomplete)
    return when (section.kind) {
        CorporationContractSectionKind.INCOMPLETE -> incomplete
        CorporationContractSectionKind.ISSUED_DAY -> issued
        CorporationContractSectionKind.COMPLETED_DAY -> completed
    }
}

@Composable
private fun ContractsStatusBanner(
    permissionDenied: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
) {
    val messageRes = when {
        permissionDenied -> R.string.corporation_contracts_permission_denied
        loadFailed -> R.string.corporation_contracts_load_failed
        else -> return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(messageRes),
            color = colorResource(R.color.text_primary),
            modifier = Modifier.weight(1f),
        )
        if (loadFailed && !permissionDenied) {
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.character_sheet_retry))
            }
        }
    }
}
