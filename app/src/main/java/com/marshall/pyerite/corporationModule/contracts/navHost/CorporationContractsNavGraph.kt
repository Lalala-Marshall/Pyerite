package com.marshall.pyerite.corporationModule.contracts.navHost

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.marshall.pyerite.corporationModule.contracts.ui.CorporationContractDetailPage
import com.marshall.pyerite.corporationModule.contracts.ui.CorporationContractsPage
import com.marshall.pyerite.corporationModule.contracts.viewModel.CorporationContractDetailViewModel
import com.marshall.pyerite.corporationModule.contracts.viewModel.CorporationContractsViewModel

fun NavGraphBuilder.corporationContractsNavGraph(
    navController: NavController,
) {
    composable(
        route = CorporationContractsRoute.Contracts.route,
        arguments = listOf(
            navArgument(CorporationContractsViewModel.NAV_ARG_CHARACTER_ID) {
                type = NavType.LongType
            },
        ),
    ) {
        CorporationContractsPage(navController = navController)
    }
    composable(
        route = CorporationContractsRoute.Detail.route,
        arguments = listOf(
            navArgument(CorporationContractsViewModel.NAV_ARG_CHARACTER_ID) {
                type = NavType.LongType
            },
            navArgument(CorporationContractDetailViewModel.NAV_ARG_CONTRACT_ID) {
                type = NavType.LongType
            },
        ),
    ) {
        CorporationContractDetailPage(navController = navController)
    }
}
