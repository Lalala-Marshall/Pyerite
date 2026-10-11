package com.marshall.pyerite.characterTransactionsModule.navHost

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.marshall.pyerite.characterTransactionsModule.ui.CharacterTransactionDayPage
import com.marshall.pyerite.characterTransactionsModule.ui.CharacterTransactionsPage
import com.marshall.pyerite.characterTransactionsModule.viewModel.CharacterTransactionDayViewModel
import com.marshall.pyerite.characterTransactionsModule.viewModel.CharacterTransactionsViewModel

fun NavGraphBuilder.characterTransactionsNavGraph(
    navController: NavController,
) {
    composable(
        route = CharacterTransactionsRoute.List.route,
        arguments = listOf(
            navArgument(CharacterTransactionsViewModel.NAV_ARG_CHARACTER_ID) {
                type = NavType.LongType
            },
        ),
    ) {
        CharacterTransactionsPage(navController = navController)
    }
    composable(
        route = CharacterTransactionsRoute.Day.route,
        arguments = listOf(
            navArgument(CharacterTransactionDayViewModel.NAV_ARG_CHARACTER_ID) {
                type = NavType.LongType
            },
            navArgument(CharacterTransactionDayViewModel.NAV_ARG_DAY_KEY) {
                type = NavType.StringType
            },
        ),
    ) {
        CharacterTransactionDayPage(navController = navController)
    }
}
