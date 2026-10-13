package com.marshall.pyerite.characterIndustryModule.navHost

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.marshall.pyerite.characterIndustryModule.ui.CharacterIndustryPage
import com.marshall.pyerite.characterIndustryModule.viewModel.CharacterIndustryViewModel

fun NavGraphBuilder.characterIndustryNavGraph(
    navController: NavController,
) {
    composable(
        route = CharacterIndustryRoute.Industry.route,
        arguments = listOf(
            navArgument(CharacterIndustryViewModel.NAV_ARG_CHARACTER_ID) {
                type = NavType.LongType
            },
        ),
    ) {
        CharacterIndustryPage(navController = navController)
    }
}
