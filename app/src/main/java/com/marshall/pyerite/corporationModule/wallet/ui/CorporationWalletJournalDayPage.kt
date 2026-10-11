package com.marshall.pyerite.corporationModule.wallet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletDateFormatter
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletJournalEntry
import com.marshall.pyerite.corporationModule.wallet.viewModel.CorporationWalletJournalDayViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.pyeritePullRefreshTopBarAction
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.rememberScrollTitleCollapsed
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CorporationWalletJournalDayPage(
    navController: NavController,
    viewModel: CorporationWalletJournalDayViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val pageTitle = CorporationWalletDateFormatter.displayDate(uiState.dayKey)
    val onBack = navController.rememberNavigateUpAction()
    val showCollapsedTitle = rememberScrollTitleCollapsed(scrollState)
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val bottomPadding = dimensionResource(R.dimen.type_detail_bottom_padding)
    val endActions = listOfNotNull(
        pyeritePullRefreshTopBarAction(
            isRefreshing = uiState.isLoading,
            refreshFailed = uiState.loadFailed,
            onRefresh = viewModel::refresh,
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
                    CorporationWalletStatusBanner(
                        permissionDenied = uiState.permissionDenied,
                        loadFailed = uiState.loadFailed,
                        onRetry = viewModel::refresh,
                    )
                    Spacer(modifier = Modifier.height(sectionGap))
                }
                when {
                    uiState.permissionDenied -> Unit
                    uiState.entries.isEmpty() && !uiState.isLoading -> {
                        Text(
                            text = stringResource(R.string.corporation_wallet_journal_empty),
                            color = colorResource(R.color.hint_text),
                            modifier = Modifier.padding(
                                horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding),
                            ),
                        )
                    }
                    uiState.entries.isNotEmpty() -> {
                        BaseContainer(useSystemBarsPadding = false) {
                            uiState.entries.forEachIndexed { index, entry ->
                                JournalEntryRow(
                                    entry = entry,
                                    dateTimeLabel = CorporationWalletDateFormatter.displayDateTimeMinute(
                                        entry.dateEpochMs,
                                    ),
                                    showDivider = index < uiState.entries.lastIndex,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalEntryRow(
    entry: CorporationWalletJournalEntry,
    dateTimeLabel: String,
    showDivider: Boolean,
) {
    val hintSpacing = dimensionResource(R.dimen.detail_row_label_subtitle_spacing)
    val horizontalPadding = dimensionResource(R.dimen.detail_row_horizontal_padding)
    val hintTextSize = dimensionResource(R.dimen.detail_row_label_subtitle_text_size).value.sp
    val hintLineHeight = dimensionResource(R.dimen.detail_row_label_subtitle_line_height).value.sp
    val hintColor = colorResource(R.color.hint_text)
    val bottomPadding = dimensionResource(R.dimen.detail_row_vertical_padding_multi_line)
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = stringResource(entry.titleRes),
            itemNameBold = true,
            trailingValue = formatWalletIsk(entry.amount, NumberDisplayFormatter.Style.FULL),
            trailingValueColor = walletSignedColor(entry.amount),
            showChevron = false,
            onClick = null,
        ),
        showDivider = showDivider,
        omitContentBottomPadding = true,
        belowContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        bottom = bottomPadding,
                    ),
            ) {
                Text(
                    text = journalDescriptionText(entry.description),
                    color = hintColor,
                    fontSize = hintTextSize,
                    lineHeight = hintLineHeight,
                    modifier = Modifier.padding(top = hintSpacing),
                )
                val balance = entry.balance
                if (balance != null) {
                    Text(
                        text = stringResource(
                            R.string.corporation_wallet_balance_value,
                            formatWalletIsk(balance, NumberDisplayFormatter.Style.FULL),
                        ),
                        color = hintColor,
                        fontSize = hintTextSize,
                        lineHeight = hintLineHeight,
                        modifier = Modifier.padding(top = hintSpacing),
                    )
                }
                Text(
                    text = dateTimeLabel,
                    color = hintColor,
                    fontSize = hintTextSize,
                    lineHeight = hintLineHeight,
                    modifier = Modifier.padding(top = hintSpacing),
                )
            }
        },
    )
}
