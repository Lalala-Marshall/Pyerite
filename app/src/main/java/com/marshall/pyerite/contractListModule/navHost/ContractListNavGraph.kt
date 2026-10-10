package com.marshall.pyerite.contractListModule.navHost

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.marshall.pyerite.contractListModule.ui.ContractListDetailPage
import com.marshall.pyerite.contractListModule.ui.ContractListPage
import com.marshall.pyerite.contractListModule.viewModel.ContractListDetailViewModel
import com.marshall.pyerite.contractListModule.viewModel.ContractListViewModel

fun NavGraphBuilder.contractListNavGraph(
    navController: NavController,
) {
    composable(
        route = ContractListRoute.List.route,
        arguments = listOf(
            navArgument(ContractListViewModel.NAV_ARG_CHARACTER_ID) {
                type = NavType.LongType
            },
        ),
    ) {
        ContractListPage(navController = navController)
    }
    composable(
        route = ContractListRoute.Detail.route,
        arguments = listOf(
            navArgument(ContractListViewModel.NAV_ARG_CHARACTER_ID) {
                type = NavType.LongType
            },
            navArgument(ContractListDetailViewModel.NAV_ARG_CONTRACT_ID) {
                type = NavType.LongType
            },
        ),
    ) {
        ContractListDetailPage(navController = navController)
    }
}
