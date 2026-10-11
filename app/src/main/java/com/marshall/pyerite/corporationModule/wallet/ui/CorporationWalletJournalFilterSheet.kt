package com.marshall.pyerite.corporationModule.wallet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.marshall.pyerite.R
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletConfig
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletJournalDirection
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletJournalFilter
import com.marshall.pyerite.corporationModule.wallet.model.CorporationWalletJournalFilterType
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.walletJournal.ui.WalletJournalCheckRow
import com.marshall.pyerite.walletJournal.ui.WalletJournalModalSheet
import com.marshall.pyerite.walletJournal.ui.WalletJournalSelectAllButton

@Composable
internal fun CorporationWalletJournalFilterSheet(
    filter: CorporationWalletJournalFilter,
    onFilterChange: (CorporationWalletJournalFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val bottomPadding = dimensionResource(R.dimen.type_detail_bottom_padding)
    WalletJournalModalSheet(
        title = stringResource(R.string.corporation_wallet_journal_filter_title),
        onDismiss = onDismiss,
        heightFraction = CorporationWalletConfig.FILTER_SHEET_HEIGHT_FRACTION,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomPadding),
        ) {
            BaseContainer(
                title = stringResource(R.string.corporation_wallet_filter_direction_section),
                titleTrailingContent = {
                    WalletJournalSelectAllButton(
                        allSelected = filter.allDirectionsSelected,
                        onClick = { onFilterChange(filter.toggleAllDirections()) },
                    )
                },
                useSystemBarsPadding = false,
            ) {
                CorporationWalletJournalDirection.entries.forEachIndexed { index, direction ->
                    WalletJournalCheckRow(
                        title = stringResource(direction.titleRes),
                        selected = direction in filter.directions,
                        onClick = { onFilterChange(filter.toggleDirection(direction)) },
                        showDivider = index < CorporationWalletJournalDirection.entries.lastIndex,
                    )
                }
            }
            Spacer(modifier = Modifier.height(sectionGap))
            BaseContainer(
                title = stringResource(R.string.corporation_wallet_filter_type_section),
                titleTrailingContent = {
                    WalletJournalSelectAllButton(
                        allSelected = filter.allTypesSelected,
                        onClick = { onFilterChange(filter.toggleAllTypes()) },
                    )
                },
                useSystemBarsPadding = false,
            ) {
                CorporationWalletJournalFilterType.entries.forEachIndexed { index, type ->
                    WalletJournalCheckRow(
                        title = stringResource(type.titleRes),
                        selected = type in filter.types,
                        onClick = { onFilterChange(filter.toggleType(type)) },
                        showDivider = index < CorporationWalletJournalFilterType.entries.lastIndex,
                    )
                }
            }
        }
    }
}
