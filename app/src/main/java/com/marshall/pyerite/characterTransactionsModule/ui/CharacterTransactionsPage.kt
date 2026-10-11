package com.marshall.pyerite.characterTransactionsModule.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
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
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionDayRow
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionSideFilter
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsDateFormatter
import com.marshall.pyerite.characterTransactionsModule.navHost.CharacterTransactionsRoute
import com.marshall.pyerite.characterTransactionsModule.viewModel.CharacterTransactionsViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarMenuItem
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.rememberScrollTitleCollapsed
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CharacterTransactionsPage(
    navController: NavController,
    viewModel: CharacterTransactionsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val pageTitle = stringResource(R.string.character_transactions)
    val onBack = navController.rememberNavigateUpAction()
    val showCollapsedTitle = rememberScrollTitleCollapsed(scrollState)
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val bottomPadding = dimensionResource(R.dimen.type_detail_bottom_padding)
    var showSettingsSheet by remember { mutableStateOf(false) }
    val filterDescription = stringResource(R.string.corporation_wallet_journal_filter_title)
    val filterLabels = CharacterTransactionSideFilter.entries.associateWith { filter ->
        stringResource(sideFilterLabel(filter))
    }
    val endActions = listOf(
        PyeriteTopBarActionItem(
            onClick = {},
            icon = if (uiState.sideFilter == CharacterTransactionSideFilter.ALL) {
                Icons.Outlined.FilterList
            } else {
                Icons.Filled.FilterList
            },
            contentDescription = filterDescription,
            menuItems = CharacterTransactionSideFilter.entries.map { filter ->
                PyeriteTopBarMenuItem(
                    label = filterLabels.getValue(filter),
                    trailingIcon = if (filter == uiState.sideFilter) Icons.Filled.Check else null,
                    showDividerBelow = filter != CharacterTransactionSideFilter.BUY,
                    onClick = { viewModel.onSideFilterChange(filter) },
                )
            },
        ),
        PyeriteTopBarActionItem(
            onClick = { showSettingsSheet = true },
            icon = Icons.Default.Settings,
            contentDescription = stringResource(
                R.string.corporation_wallet_transaction_settings_title,
            ),
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
                    CharacterTransactionsStatusBanner(
                        permissionDenied = uiState.permissionDenied,
                        loadFailed = uiState.loadFailed,
                        onRetry = viewModel::refresh,
                    )
                    Spacer(modifier = Modifier.height(sectionGap))
                }
                if (uiState.permissionDenied) return@Column
                when {
                    uiState.days.isEmpty() && !uiState.isLoading -> {
                        Text(
                            text = stringResource(R.string.corporation_wallet_transactions_empty),
                            color = colorResource(R.color.hint_text),
                            modifier = Modifier.padding(
                                horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding),
                            ),
                        )
                    }
                    uiState.days.isNotEmpty() -> {
                        BaseContainer(useSystemBarsPadding = false) {
                            uiState.days.forEachIndexed { index, day ->
                                TransactionDayRow(
                                    day = day,
                                    dateLabel = CharacterTransactionsDateFormatter.displayDate(day.dayKey),
                                    hint = stringResource(
                                        R.string.corporation_wallet_transactions_hint,
                                        day.buyCount,
                                        day.sellCount,
                                    ),
                                    showDivider = index < uiState.days.lastIndex,
                                    onClick = {
                                        navController.navigate(
                                            CharacterTransactionsRoute.Day.create(
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
    }
    if (showSettingsSheet) {
        CharacterTransactionSettingsSheet(
            mergeSimilarTransactions = uiState.mergeSimilarTransactions,
            onMergeSimilarTransactionsChange = viewModel::onMergeSimilarTransactionsChange,
            onDismiss = { showSettingsSheet = false },
        )
    }
}

@StringRes
private fun sideFilterLabel(filter: CharacterTransactionSideFilter): Int = when (filter) {
    CharacterTransactionSideFilter.ALL -> R.string.character_mastery_filter_all
    CharacterTransactionSideFilter.SELL -> R.string.corporation_wallet_sell
    CharacterTransactionSideFilter.BUY -> R.string.corporation_wallet_buy
}

@Composable
private fun TransactionDayRow(
    day: CharacterTransactionDayRow,
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
            trailingValue = formatTransactionIsk(day.netIsk, NumberDisplayFormatter.Style.COMPACT),
            trailingValueColor = transactionSignedColor(day.netIsk),
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}
