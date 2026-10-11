package com.marshall.pyerite.characterWalletJournalModule.ui

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
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalDateFormatter
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalDayRow
import com.marshall.pyerite.characterWalletJournalModule.navHost.CharacterWalletJournalRoute
import com.marshall.pyerite.characterWalletJournalModule.viewModel.CharacterWalletJournalViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.rememberScrollTitleCollapsed
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CharacterWalletJournalPage(
    navController: NavController,
    viewModel: CharacterWalletJournalViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val pageTitle = stringResource(R.string.corporation_wallet_filter_journal)
    val onBack = navController.rememberNavigateUpAction()
    val showCollapsedTitle = rememberScrollTitleCollapsed(scrollState)
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val bottomPadding = dimensionResource(R.dimen.type_detail_bottom_padding)
    var showJournalFilterSheet by remember { mutableStateOf(false) }
    val endActions = listOf(
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
                if (uiState.permissionDenied || uiState.loadFailed) {
                    CharacterWalletJournalStatusBanner(
                        permissionDenied = uiState.permissionDenied,
                        loadFailed = uiState.loadFailed,
                        onRetry = viewModel::refresh,
                    )
                    Spacer(modifier = Modifier.height(sectionGap))
                }
                if (uiState.permissionDenied) return@Column
                BaseContainer(
                    title = stringResource(R.string.corporation_wallet_summary),
                    titleTrailingContent = {
                        Text(
                            text = journalSummaryWindowLabel(uiState.summaryWindow),
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
                            JournalDayRow(
                                day = day,
                                dateLabel = CharacterWalletJournalDateFormatter.displayDate(day.dayKey),
                                hint = stringResource(
                                    R.string.corporation_wallet_journal_count,
                                    day.entryCount,
                                ),
                                showDivider = index < uiState.journalDays.lastIndex,
                                onClick = {
                                    navController.navigate(
                                        CharacterWalletJournalRoute.Day.create(
                                            characterId = viewModel.characterId,
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
    if (showJournalFilterSheet) {
        CharacterWalletJournalFilterSheet(
            filter = uiState.journalFilter,
            onFilterChange = viewModel::onJournalFilterChange,
            onDismiss = { showJournalFilterSheet = false },
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
            trailingValue = formatJournalIsk(amount, NumberDisplayFormatter.Style.FULL),
            trailingValueColor = journalSignedColor(signedValue),
            showChevron = false,
            onClick = null,
        ),
        showDivider = showDivider,
    )
}

@Composable
private fun JournalDayRow(
    day: CharacterWalletJournalDayRow,
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
            trailingValue = formatJournalIsk(day.netIsk, NumberDisplayFormatter.Style.COMPACT),
            trailingValueColor = journalSignedColor(day.netIsk),
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}
