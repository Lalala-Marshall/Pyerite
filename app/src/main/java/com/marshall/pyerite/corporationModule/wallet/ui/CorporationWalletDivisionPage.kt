package com.marshall.pyerite.corporationModule.wallet.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletDateFormatter
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletDayRow
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletDivisionTab
import com.marshall.pyerite.corporationModule.wallet.navHost.CorporationWalletRoute
import com.marshall.pyerite.corporationModule.wallet.viewModel.CorporationWalletDivisionViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.PyeriteSegmentedControl
import com.marshall.pyerite.ui.golbalComponents.PyeriteSegmentedOption
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.rememberScrollTitleCollapsed
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CorporationWalletDivisionPage(
    navController: NavController,
    viewModel: CorporationWalletDivisionViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val pageTitle = corporationWalletDivisionTitle(
        division = viewModel.division,
        name = uiState.divisionName,
    )
    val onBack = navController.rememberNavigateUpAction()
    val showCollapsedTitle = rememberScrollTitleCollapsed(scrollState)
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val bottomPadding = dimensionResource(R.dimen.type_detail_bottom_padding)
    var showJournalFilterSheet by remember { mutableStateOf(false) }
    var showTransactionSettingsSheet by remember { mutableStateOf(false) }
    val endActions = when (uiState.selectedTab) {
        CorporationWalletDivisionTab.JOURNAL -> listOf(
            PyeriteTopBarActionItem(
                onClick = { showJournalFilterSheet = true },
                icon = if (uiState.journalFilter.isDefault) {
                    Icons.Outlined.FilterList
                } else {
                    Icons.Filled.FilterList
                },
                contentDescription = stringResource(R.string.corporation_wallet_journal_filter_title),
            ),
            PyeriteTopBarActionItem(
                onClick = viewModel::forceRefreshJournal,
                icon = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.corporation_wallet_force_refresh),
                iconTint = colorResource(R.color.character_delete),
                enabled = !uiState.isLoading,
                spinning = uiState.isLoading,
            ),
        )
        CorporationWalletDivisionTab.TRANSACTIONS -> listOf(
            PyeriteTopBarActionItem(
                onClick = { showTransactionSettingsSheet = true },
                icon = Icons.Default.Settings,
                contentDescription = stringResource(
                    R.string.corporation_wallet_transaction_settings_title,
                ),
            ),
        )
    }

    PyeritePageScaffold(
        title = pageTitle,
        showCollapsedTitle = showCollapsedTitle,
        onBack = onBack,
        endActions = endActions,
    ) { topBarPadding ->
        PyeritePullToRefreshBox(
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(topBarPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = bottomPadding),
            ) {
                PageTitle(text = pageTitle)
                PyeriteSegmentedControl(
                    options = listOf(
                        PyeriteSegmentedOption(
                            value = CorporationWalletDivisionTab.JOURNAL,
                            label = stringResource(R.string.corporation_wallet_filter_journal),
                        ),
                        PyeriteSegmentedOption(
                            value = CorporationWalletDivisionTab.TRANSACTIONS,
                            label = stringResource(R.string.corporation_wallet_filter_transactions),
                        ),
                    ),
                    selected = uiState.selectedTab,
                    onSelect = { tab ->
                        showJournalFilterSheet = false
                        showTransactionSettingsSheet = false
                        viewModel.onTabSelected(tab)
                    },
                )
                if (uiState.permissionDenied || uiState.loadFailed) {
                    CorporationWalletStatusBanner(
                        permissionDenied = uiState.permissionDenied,
                        loadFailed = uiState.loadFailed,
                        onRetry = viewModel::refresh,
                    )
                    Spacer(modifier = Modifier.height(sectionGap))
                }
                if (uiState.permissionDenied) return@Column
                when (uiState.selectedTab) {
                    CorporationWalletDivisionTab.JOURNAL -> {
                        BaseContainer(
                            title = stringResource(R.string.corporation_wallet_summary),
                            titleTrailingContent = {
                                Text(
                                    text = walletSummaryWindowLabel(uiState.summaryWindow),
                                    color = colorResource(R.color.hyperlink_text),
                                    modifier = Modifier.clickable(onClick = viewModel::cycleSummaryWindow),
                                )
                            },
                            useSystemBarsPadding = false,
                        ) {
                            SummaryAmountRow(
                                title = stringResource(R.string.corporation_wallet_summary_income),
                                amount = uiState.periodSummary.income,
                                signedValue = uiState.periodSummary.income,
                                showDivider = true,
                            )
                            SummaryAmountRow(
                                title = stringResource(R.string.corporation_wallet_summary_expense),
                                amount = uiState.periodSummary.expense,
                                signedValue = -uiState.periodSummary.expense,
                                showDivider = true,
                            )
                            SummaryAmountRow(
                                title = stringResource(R.string.corporation_wallet_summary_net),
                                amount = uiState.periodSummary.net,
                                signedValue = uiState.periodSummary.net,
                                showDivider = false,
                            )
                        }
                        Spacer(modifier = Modifier.height(sectionGap))
                        if (uiState.journalDays.isEmpty() && !uiState.isLoading) {
                            Text(
                                text = stringResource(R.string.corporation_wallet_journal_empty),
                                color = colorResource(R.color.hint_text),
                                modifier = Modifier.padding(
                                    horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding),
                                ),
                            )
                        } else if (uiState.journalDays.isNotEmpty()) {
                            BaseContainer(
                                title = stringResource(R.string.corporation_wallet_journal_dates),
                                useSystemBarsPadding = false,
                            ) {
                                uiState.journalDays.forEachIndexed { index, day ->
                                    WalletDayRow(
                                        day = day,
                                        dateLabel = CorporationWalletDateFormatter.displayDate(day.dayKey),
                                        hint = stringResource(
                                            R.string.corporation_wallet_journal_count,
                                            day.entryCount,
                                        ),
                                        showDivider = index < uiState.journalDays.lastIndex,
                                        onClick = {
                                            navController.navigate(
                                                CorporationWalletRoute.JournalDay.create(
                                                    characterId = viewModel.characterId,
                                                    division = viewModel.division,
                                                    dayKey = day.dayKey,
                                                ),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                    CorporationWalletDivisionTab.TRANSACTIONS -> {
                        if (uiState.transactionDays.isEmpty() && !uiState.isLoading) {
                            Text(
                                text = stringResource(R.string.corporation_wallet_transactions_empty),
                                color = colorResource(R.color.hint_text),
                                modifier = Modifier.padding(
                                    horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding),
                                ),
                            )
                        } else if (uiState.transactionDays.isNotEmpty()) {
                            BaseContainer(useSystemBarsPadding = false) {
                                uiState.transactionDays.forEachIndexed { index, day ->
                                    WalletDayRow(
                                        day = day,
                                        dateLabel = CorporationWalletDateFormatter.displayDate(day.dayKey),
                                        hint = stringResource(
                                            R.string.corporation_wallet_transactions_hint,
                                            day.buyCount,
                                            day.sellCount,
                                        ),
                                        showDivider = index < uiState.transactionDays.lastIndex,
                                        onClick = {
                                            navController.navigate(
                                                CorporationWalletRoute.TransactionDay.create(
                                                    characterId = viewModel.characterId,
                                                    division = viewModel.division,
                                                    dayKey = day.dayKey,
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
    }
    if (showJournalFilterSheet && uiState.selectedTab == CorporationWalletDivisionTab.JOURNAL) {
        CorporationWalletJournalFilterSheet(
            filter = uiState.journalFilter,
            onFilterChange = viewModel::onJournalFilterChange,
            onDismiss = { showJournalFilterSheet = false },
        )
    }
    if (showTransactionSettingsSheet &&
        uiState.selectedTab == CorporationWalletDivisionTab.TRANSACTIONS
    ) {
        CorporationWalletTransactionSettingsSheet(
            mergeSimilarTransactions = uiState.mergeSimilarTransactions,
            onMergeSimilarTransactionsChange = viewModel::onMergeSimilarTransactionsChange,
            onDismiss = { showTransactionSettingsSheet = false },
        )
    }
}

@Composable
private fun SummaryAmountRow(
    title: String,
    amount: Double,
    signedValue: Double,
    showDivider: Boolean,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = title,
            trailingValue = formatWalletIsk(amount, NumberDisplayFormatter.Style.FULL),
            trailingValueColor = walletSignedColor(signedValue),
            showChevron = false,
            onClick = null,
        ),
        showDivider = showDivider,
    )
}

@Composable
private fun WalletDayRow(
    day: CorporationWalletDayRow,
    dateLabel: String,
    hint: String,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = dateLabel,
            itemHint = hint,
            trailingValue = formatWalletIsk(day.netIsk, NumberDisplayFormatter.Style.COMPACT),
            trailingValueColor = walletSignedColor(day.netIsk),
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}
