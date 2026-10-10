package com.marshall.pyerite.contractListModule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.contractListModule.navHost.ContractListRoute
import com.marshall.pyerite.contractListModule.viewModel.ContractListViewModel
import com.marshall.pyerite.contractsCommon.model.ContractAmountViewpoint
import com.marshall.pyerite.contractsCommon.model.ContractListScope
import com.marshall.pyerite.contractsCommon.ui.ContractsListPage
import com.marshall.pyerite.ui.golbalComponents.PyeriteSegmentedControl
import com.marshall.pyerite.ui.golbalComponents.PyeriteSegmentedOption
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun ContractListPage(
    navController: NavController,
    viewModel: ContractListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val onBack = navController.rememberNavigateUpAction()
    val visible = uiState.contracts.filter { uiState.scope in it.scopes }
    val characterLabel = stringResource(R.string.contracts_list_scope_character)
    val corporationLabel = stringResource(R.string.contracts_list_scope_corporation)
    val allianceLabel = stringResource(R.string.contracts_list_scope_alliance)
    ContractsListPage(
        pageTitle = stringResource(R.string.contracts_list),
        contracts = visible,
        totalCount = visible.size,
        filter = uiState.filter,
        isLoading = uiState.isLoading,
        loadFailed = uiState.loadFailed,
        permissionDenied = uiState.permissionDenied,
        permissionDeniedMessage = stringResource(R.string.contracts_list_permission_denied),
        loadFailedMessage = stringResource(R.string.contracts_list_load_failed),
        emptyMessage = stringResource(R.string.corporation_contracts_empty),
        onRefresh = viewModel::refresh,
        onBack = onBack,
        onContractClick = { contract ->
            navController.navigate(
                ContractListRoute.Detail.create(
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
        amountViewpoint = ContractAmountViewpoint.CHARACTER,
        characterId = viewModel.characterId,
        scopeFilter = {
            PyeriteSegmentedControl(
                options = listOf(
                    PyeriteSegmentedOption(ContractListScope.CHARACTER, characterLabel),
                    PyeriteSegmentedOption(ContractListScope.CORPORATION, corporationLabel),
                    PyeriteSegmentedOption(ContractListScope.ALLIANCE, allianceLabel),
                ),
                selected = uiState.scope,
                onSelect = viewModel::setScope,
            )
        },
    )
}
