package com.marshall.pyerite.regionMarketModule.navHost

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.marshall.pyerite.regionMarketModule.ui.MarketBrowserPage
import com.marshall.pyerite.regionMarketModule.ui.MarketDetailPage
import com.marshall.pyerite.regionMarketModule.ui.MarketOrdersPage
import com.marshall.pyerite.regionMarketModule.ui.MarketTypeListPage

internal fun NavGraphBuilder.regionMarketNavGraph(navController: NavController) {
    composable(RegionMarketRoute.Root.route) {
        MarketBrowserPage(navController = navController)
    }
    composable(
        route = RegionMarketRoute.Group.route,
        arguments = listOf(navArgument("groupId") { type = NavType.IntType }),
    ) { entry ->
        MarketBrowserPage(
            navController = navController,
            parentGroupId = entry.arguments?.getInt("groupId") ?: return@composable,
        )
    }
    composable(
        route = RegionMarketRoute.Types.route,
        arguments = listOf(navArgument("groupId") { type = NavType.IntType }),
    ) { entry ->
        MarketTypeListPage(
            navController = navController,
            marketGroupId = entry.arguments?.getInt("groupId") ?: return@composable,
        )
    }
    composable(
        route = RegionMarketRoute.Detail.route,
        arguments = listOf(navArgument("typeId") { type = NavType.IntType }),
    ) { entry ->
        MarketDetailPage(
            navController = navController,
            typeId = entry.arguments?.getInt("typeId") ?: return@composable,
        )
    }
    composable(
        route = RegionMarketRoute.Orders.route,
        arguments = listOf(navArgument("typeId") { type = NavType.IntType }),
    ) { entry ->
        MarketOrdersPage(
            navController = navController,
            typeId = entry.arguments?.getInt("typeId") ?: return@composable,
        )
    }
}
