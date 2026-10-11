package com.marshall.pyerite.characterWalletJournalModule.navHost

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.marshall.pyerite.characterWalletJournalModule.ui.CharacterWalletJournalDayPage
import com.marshall.pyerite.characterWalletJournalModule.ui.CharacterWalletJournalPage
import com.marshall.pyerite.characterWalletJournalModule.viewModel.CharacterWalletJournalDayViewModel
import com.marshall.pyerite.characterWalletJournalModule.viewModel.CharacterWalletJournalViewModel

fun NavGraphBuilder.characterWalletJournalNavGraph(
    navController: NavController,
) {
    composable(
        route = CharacterWalletJournalRoute.List.route,
        arguments = listOf(
            navArgument(CharacterWalletJournalViewModel.NAV_ARG_CHARACTER_ID) {
                type = NavType.LongType
            },
        ),
    ) {
        CharacterWalletJournalPage(navController = navController)
    }
    composable(
        route = CharacterWalletJournalRoute.Day.route,
        arguments = listOf(
            navArgument(CharacterWalletJournalDayViewModel.NAV_ARG_CHARACTER_ID) {
                type = NavType.LongType
            },
            navArgument(CharacterWalletJournalDayViewModel.NAV_ARG_DAY_KEY) {
                type = NavType.StringType
            },
        ),
    ) {
        CharacterWalletJournalDayPage(navController = navController)
    }
}
