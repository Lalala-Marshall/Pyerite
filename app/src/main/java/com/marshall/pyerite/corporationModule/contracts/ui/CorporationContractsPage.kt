package com.marshall.pyerite.corporationModule.contracts.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.contractsCommon.model.ContractAmountViewpoint
import com.marshall.pyerite.contractsCommon.ui.ContractsListPage
import com.marshall.pyerite.corporationModule.contracts.navHost.CorporationContractsRoute
import com.marshall.pyerite.corporationModule.contracts.viewModel.CorporationContractsViewModel
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CorporationContractsPage(
    navController: NavController,
    viewModel: CorporationContractsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val onBack = navController.rememberNavigateUpAction()
    ContractsListPage(
        pageTitle = stringResource(R.string.corporation_contracts),
        contracts = uiState.contracts,
        totalCount = uiState.contracts.size,
        filter = uiState.filter,
        isLoading = uiState.isLoading,
        loadFailed = uiState.loadFailed,
        permissionDenied = uiState.permissionDenied,
        permissionDeniedMessage = stringResource(R.string.corporation_contracts_permission_denied),
        loadFailedMessage = stringResource(R.string.corporation_contracts_load_failed),
        emptyMessage = stringResource(R.string.corporation_contracts_empty),
        onRefresh = viewModel::refresh,
        onBack = onBack,
        onContractClick = { contract ->
            navController.navigate(
                CorporationContractsRoute.Detail.create(
                    viewModel.characterId,
                    contract.contractId,
                ),
            )
        },
        onGroupBy = viewModel::setGroupBy,
        onMinPriceChange = viewModel::setMinPriceText,
        onMaxPriceChange = viewModel::setMaxPriceText,
        onToggleType = viewModel::toggleType,
        onToggleAllTypes = viewModel::toggleAllTypes,
        onToggleStatus = viewModel::toggleStatus,
        onToggleAllStatuses = viewModel::toggleAllStatuses,
        onDisplayLimit = viewModel::setDisplayLimit,
        amountViewpoint = ContractAmountViewpoint.CORPORATION_ISSUER,
        characterId = viewModel.characterId,
    )
}
