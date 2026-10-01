package com.marshall.pyerite.corporationModule.industry.navHost

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.marshall.pyerite.corporationModule.industry.ui.CorporationIndustryPage
import com.marshall.pyerite.corporationModule.industry.viewModel.CorporationIndustryViewModel

fun NavGraphBuilder.corporationIndustryNavGraph(
    navController: NavController,
) {
    composable(
        route = CorporationIndustryRoute.Industry.route,
        arguments = listOf(
            navArgument(CorporationIndustryViewModel.NAV_ARG_CHARACTER_ID) {
                type = NavType.LongType
            },
        ),
    ) {
        CorporationIndustryPage(navController = navController)
    }
}
