package com.marshall.pyerite.contractListModule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.contractListModule.viewModel.ContractListDetailViewModel
import com.marshall.pyerite.contractsCommon.model.ContractAmountViewpoint
import com.marshall.pyerite.contractsCommon.ui.ContractDetailScreen
import com.marshall.pyerite.databaseHierarchyModule.navHost.DatabaseRoute
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun ContractListDetailPage(
    navController: NavController,
    viewModel: ContractListDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    ContractDetailScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onBack = navController.rememberNavigateUpAction(),
        onItemClick = { typeId ->
            navController.navigate(DatabaseRoute.TypeDetail.create(typeId))
        },
        permissionDeniedMessage = stringResource(R.string.contracts_list_permission_denied),
        loadFailedMessage = stringResource(R.string.contracts_list_detail_load_failed),
        amountViewpoint = ContractAmountViewpoint.CHARACTER,
        characterId = viewModel.characterId,
    )
}
