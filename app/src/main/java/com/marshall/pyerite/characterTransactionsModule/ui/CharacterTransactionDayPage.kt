package com.marshall.pyerite.characterTransactionsModule.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsDateFormatter
import com.marshall.pyerite.characterTransactionsModule.viewModel.CharacterTransactionDayViewModel
import com.marshall.pyerite.regionMarketModule.navHost.RegionMarketRoute
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.pyeritePullRefreshTopBarAction
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.rememberScrollTitleCollapsed
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CharacterTransactionDayPage(
    navController: NavController,
    viewModel: CharacterTransactionDayViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val pageTitle = CharacterTransactionsDateFormatter.displayDate(uiState.dayKey)
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
                    CharacterTransactionsStatusBanner(
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
                            text = stringResource(R.string.corporation_wallet_transactions_empty),
                            color = colorResource(R.color.hint_text),
                            modifier = Modifier.padding(
                                horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding),
                            ),
                        )
                    }
                    uiState.entries.isNotEmpty() -> {
                        BaseContainer(useSystemBarsPadding = false) {
                            uiState.entries.forEachIndexed { index, entry ->
                                CharacterTransactionItem(
                                    transaction = entry,
                                    showDivider = index < uiState.entries.lastIndex,
                                    onClick = {
                                        navController.navigate(
                                            RegionMarketRoute.Detail.create(entry.typeId),
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
