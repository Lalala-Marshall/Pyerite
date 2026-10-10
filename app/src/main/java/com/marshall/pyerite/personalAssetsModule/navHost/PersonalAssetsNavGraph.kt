package com.marshall.pyerite.personalAssetsModule.navHost

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.marshall.pyerite.personalAssetsModule.ui.PersonalAssetContainerPage
import com.marshall.pyerite.personalAssetsModule.ui.PersonalAssetLocationPage
import com.marshall.pyerite.personalAssetsModule.ui.PersonalAssetsPage

fun NavGraphBuilder.personalAssetsNavGraph(
    navController: NavController,
) {
    composable(
        route = PersonalAssetsRoute.Regions.route,
        arguments = listOf(
            navArgument(PersonalAssetsNavArgs.CHARACTER_ID) { type = NavType.LongType },
        ),
    ) {
        PersonalAssetsPage(navController = navController)
    }
    composable(
        route = PersonalAssetsRoute.Location.route,
        arguments = listOf(
            navArgument(PersonalAssetsNavArgs.CHARACTER_ID) { type = NavType.LongType },
            navArgument(PersonalAssetsNavArgs.OWNER_KEY) { type = NavType.StringType },
            navArgument(PersonalAssetsNavArgs.LOCATION_KIND) { type = NavType.StringType },
            navArgument(PersonalAssetsNavArgs.LOCATION_ID) { type = NavType.LongType },
        ),
    ) {
        PersonalAssetLocationPage(navController = navController)
    }
    composable(
        route = PersonalAssetsRoute.Container.route,
        arguments = listOf(
            navArgument(PersonalAssetsNavArgs.CHARACTER_ID) { type = NavType.LongType },
            navArgument(PersonalAssetsNavArgs.ITEM_ID) { type = NavType.StringType },
        ),
    ) {
        PersonalAssetContainerPage(navController = navController)
    }
}
