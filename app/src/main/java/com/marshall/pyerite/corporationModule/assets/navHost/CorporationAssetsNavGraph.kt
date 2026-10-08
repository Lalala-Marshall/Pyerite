package com.marshall.pyerite.corporationModule.assets.navHost

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.marshall.pyerite.corporationModule.assets.ui.CorporationAssetContainerPage
import com.marshall.pyerite.corporationModule.assets.ui.CorporationAssetFolderPage
import com.marshall.pyerite.corporationModule.assets.ui.CorporationAssetLocationPage
import com.marshall.pyerite.corporationModule.assets.ui.CorporationAssetsPage

fun NavGraphBuilder.corporationAssetsNavGraph(
    navController: NavController,
) {
    composable(
        route = CorporationAssetsRoute.Regions.route,
        arguments = listOf(
            navArgument(CorporationAssetsNavArgs.CHARACTER_ID) { type = NavType.LongType },
        ),
    ) {
        CorporationAssetsPage(navController = navController)
    }
    composable(
        route = CorporationAssetsRoute.Location.route,
        arguments = listOf(
            navArgument(CorporationAssetsNavArgs.CHARACTER_ID) { type = NavType.LongType },
            navArgument(CorporationAssetsNavArgs.LOCATION_KIND) { type = NavType.StringType },
            navArgument(CorporationAssetsNavArgs.LOCATION_ID) { type = NavType.LongType },
        ),
    ) {
        CorporationAssetLocationPage(navController = navController)
    }
    composable(
        route = CorporationAssetsRoute.Folder.route,
        arguments = listOf(
            navArgument(CorporationAssetsNavArgs.CHARACTER_ID) { type = NavType.LongType },
            navArgument(CorporationAssetsNavArgs.LOCATION_KIND) { type = NavType.StringType },
            navArgument(CorporationAssetsNavArgs.LOCATION_ID) { type = NavType.LongType },
            navArgument(CorporationAssetsNavArgs.FLAG) { type = NavType.StringType },
        ),
    ) {
        CorporationAssetFolderPage(navController = navController)
    }
    composable(
        route = CorporationAssetsRoute.Container.route,
        arguments = listOf(
            navArgument(CorporationAssetsNavArgs.CHARACTER_ID) { type = NavType.LongType },
            navArgument(CorporationAssetsNavArgs.ITEM_ID) { type = NavType.StringType },
        ),
    ) {
        CorporationAssetContainerPage(navController = navController)
    }
}
