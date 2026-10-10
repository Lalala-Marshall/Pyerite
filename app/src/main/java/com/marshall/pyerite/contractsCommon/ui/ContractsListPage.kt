package com.marshall.pyerite.contractsCommon.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R
import com.marshall.pyerite.contractsCommon.model.ContractAmountViewpoint
import com.marshall.pyerite.contractsCommon.model.CorporationContract
import com.marshall.pyerite.contractsCommon.model.CorporationContractDisplayLimit
import com.marshall.pyerite.contractsCommon.model.CorporationContractGroupBy
import com.marshall.pyerite.contractsCommon.model.CorporationContractSection
import com.marshall.pyerite.contractsCommon.model.CorporationContractSectionKind
import com.marshall.pyerite.contractsCommon.model.CorporationContractStatus
import com.marshall.pyerite.contractsCommon.model.CorporationContractType
import com.marshall.pyerite.contractsCommon.model.CorporationContractsDateFormatter
import com.marshall.pyerite.contractsCommon.model.CorporationContractsFilter
import com.marshall.pyerite.contractsCommon.model.toListResult
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarMenuItem
import com.marshall.pyerite.ui.golbalComponents.pyeritePullRefreshTopBarAction
import com.marshall.pyerite.ui.golbalComponents.rememberLazyListTitleCollapsed
import com.marshall.pyerite.util.NumberDisplayFormatter

@Composable
internal fun ContractsListPage(
    pageTitle: String,
    contracts: List<CorporationContract>,
    totalCount: Int,
    filter: CorporationContractsFilter,
    isLoading: Boolean,
    loadFailed: Boolean,
    permissionDenied: Boolean,
    permissionDeniedMessage: String,
    loadFailedMessage: String,
    emptyMessage: String,
    onRefresh: () -> Unit,
    onBack: (() -> Unit)?,
    onContractClick: (CorporationContract) -> Unit,
    onGroupBy: (CorporationContractGroupBy) -> Unit,
    onMinPriceChange: (String) -> Unit,
    onMaxPriceChange: (String) -> Unit,
    onToggleType: (CorporationContractType) -> Unit,
    onToggleAllTypes: () -> Unit,
    onToggleStatus: (CorporationContractStatus) -> Unit,
    onToggleAllStatuses: () -> Unit,
    onDisplayLimit: (CorporationContractDisplayLimit) -> Unit,
    scopeFilter: (@Composable () -> Unit)? = null,
    amountViewpoint: ContractAmountViewpoint = ContractAmountViewpoint.CORPORATION_ISSUER,
    characterId: Long = 0L,
) {
    val listState = rememberLazyListState()
    val showCollapsedTitle = rememberLazyListTitleCollapsed(listState)
    val listResult = contracts.toListResult(filter)
    val nowMs = System.currentTimeMillis()
    val showBody = !permissionDenied &&
        (totalCount > 0 || (!isLoading && !loadFailed))
    val showEmpty = showBody && listResult.sections.isEmpty()
    var priceExpanded by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var sheetPage by remember { mutableStateOf(CorporationContractsSheetPage.SETTINGS) }
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val refreshAction = pyeritePullRefreshTopBarAction(
        isRefreshing = isLoading,
        refreshFailed = loadFailed,
        onRefresh = onRefresh,
    )
    val groupAction = PyeriteTopBarActionItem(
        onClick = {},
        icon = Icons.AutoMirrored.Filled.Sort,
        contentDescription = stringResource(R.string.corporation_contracts_group),
        menuItems = listOf(
            PyeriteTopBarMenuItem(
                label = stringResource(R.string.corporation_contracts_group_issued),
                trailingIcon = if (filter.groupBy == CorporationContractGroupBy.ISSUED) {
                    Icons.Filled.Check
                } else {
                    null
                },
                onClick = { onGroupBy(CorporationContractGroupBy.ISSUED) },
            ),
            PyeriteTopBarMenuItem(
                label = stringResource(R.string.corporation_contracts_group_completed),
                trailingIcon = if (filter.groupBy == CorporationContractGroupBy.COMPLETED) {
                    Icons.Filled.Check
                } else {
                    null
                },
                showDividerBelow = false,
                onClick = { onGroupBy(CorporationContractGroupBy.COMPLETED) },
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
    val totalText = NumberDisplayFormatter.format(
        totalCount.toLong(),
        NumberDisplayFormatter.Style.FULL,
    )
    val filteredText = NumberDisplayFormatter.format(
        listResult.filteredCount.toLong(),
        NumberDisplayFormatter.Style.FULL,
    )
    val shownText = NumberDisplayFormatter.format(
        listResult.shownCount.toLong(),
        NumberDisplayFormatter.Style.FULL,
    )
    val displayLimit = filter.displayLimit.maxCount
    val countHint = when {
        displayLimit != null && listResult.filteredCount > displayLimit -> stringResource(
            R.string.contracts_list_count_hint,
            totalText,
            filteredText,
            shownText,
        )
        listResult.filteredCount < totalCount -> stringResource(
            R.string.contracts_list_count_filtered,
            totalText,
            filteredText,
        )
        else -> stringResource(R.string.contracts_list_count_total, totalText)
    }

    PyeritePageScaffold(
        title = pageTitle,
        showCollapsedTitle = showCollapsedTitle,
        onBack = onBack,
        endActions = listOfNotNull(refreshAction, groupAction, settingsAction),
    ) { topBarPadding ->
        PyeritePullToRefreshBox(
            onRefresh = onRefresh,
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
                if (permissionDenied || loadFailed) {
                    item(key = "status") {
                        ContractsStatusBanner(
                            message = if (permissionDenied) {
                                permissionDeniedMessage
                            } else {
                                loadFailedMessage
                            },
                            showRetry = loadFailed && !permissionDenied,
                            onRetry = onRefresh,
                        )
                    }
                }
                val scope = scopeFilter
                if (showBody && scope != null) {
                    item(key = "scope_filter") {
                        Column(modifier = Modifier.padding(top = sectionGap)) {
                            scope()
                        }
                    }
                }
                if (showBody) {
                    item(key = "price_filter") {
                        Column(modifier = Modifier.padding(top = sectionGap)) {
                            CorporationContractsPriceFilter(
                                expanded = priceExpanded,
                                minPriceText = filter.minPriceText,
                                maxPriceText = filter.maxPriceText,
                                onToggle = { priceExpanded = !priceExpanded },
                                onMinPriceChange = onMinPriceChange,
                                onMaxPriceChange = onMaxPriceChange,
                            )
                        }
                    }
                    item(key = "count_hint") {
                        Text(
                            text = countHint,
                            color = colorResource(R.color.hint_text),
                            fontSize = dimensionResource(R.dimen.type_detail_body_text_size).value.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = dimensionResource(R.dimen.detail_card_horizontal_padding),
                                    end = dimensionResource(R.dimen.detail_card_horizontal_padding),
                                    top = dimensionResource(R.dimen.corporation_contracts_price_title_gap),
                                ),
                        )
                    }
                }
                if (showEmpty) {
                    item(key = "empty") {
                        Text(
                            text = emptyMessage,
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
                    listResult.sections.forEach { section ->
                        item(key = section.id) {
                            Column(modifier = Modifier.padding(top = sectionGap)) {
                    ContractSection(
                        section = section,
                        nowMs = nowMs,
                        amountViewpoint = amountViewpoint,
                        characterId = characterId,
                        onContractClick = onContractClick,
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
            filter = filter,
            onOpenDisplayLimit = { sheetPage = CorporationContractsSheetPage.DISPLAY_LIMIT },
            onBack = { sheetPage = CorporationContractsSheetPage.SETTINGS },
            onSelectDisplayLimit = { limit ->
                onDisplayLimit(limit)
                sheetPage = CorporationContractsSheetPage.SETTINGS
            },
            onToggleType = onToggleType,
            onToggleAllTypes = onToggleAllTypes,
            onToggleStatus = onToggleStatus,
            onToggleAllStatuses = onToggleAllStatuses,
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
    amountViewpoint: ContractAmountViewpoint,
    characterId: Long,
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
                amountViewpoint = amountViewpoint,
                characterId = characterId,
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
    message: String,
    showRetry: Boolean,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = message,
            color = colorResource(R.color.text_primary),
            modifier = Modifier.weight(1f),
        )
        if (showRetry) {
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.character_sheet_retry))
            }
        }
    }
}
