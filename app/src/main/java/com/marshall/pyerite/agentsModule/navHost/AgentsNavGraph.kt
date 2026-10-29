package com.marshall.pyerite.agentsModule.navHost

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.marshall.pyerite.agentsModule.model.AgentsConfig
import com.marshall.pyerite.agentsModule.ui.AgentsRegionPage
import com.marshall.pyerite.agentsModule.ui.AgentsResultsPage
import com.marshall.pyerite.agentsModule.ui.AgentsSearchPage
import com.marshall.pyerite.agentsModule.ui.AgentsSystemPage

fun NavGraphBuilder.agentsNavGraph(navController: NavController) {
    navigation(
        route = AgentsRoute.Graph.route,
        startDestination = AgentsRoute.Search.route,
    ) {
        composable(AgentsRoute.Search.route) { entry ->
            AgentsSearchPage(navController = navController, backStackEntry = entry)
        }
        composable(AgentsRoute.Regions.route) { entry ->
            AgentsRegionPage(navController = navController, backStackEntry = entry)
        }
        composable(AgentsRoute.Systems.route) { entry ->
            AgentsSystemPage(navController = navController, backStackEntry = entry)
        }
        composable(
            route = AgentsRoute.Results.route,
            arguments = listOf(
                navArgument(AgentsNavArgs.FACTION_ID) {
                    type = NavType.IntType
                    defaultValue = AgentsConfig.UNSET_ID
                },
                navArgument(AgentsNavArgs.CORPORATION_ID) {
                    type = NavType.LongType
                    defaultValue = AgentsConfig.UNSET_ID.toLong()
                },
                navArgument(AgentsNavArgs.DIVISION_ID) {
                    type = NavType.IntType
                    defaultValue = AgentsConfig.UNSET_ID
                },
                navArgument(AgentsNavArgs.AGENT_LEVEL) {
                    type = NavType.IntType
                    defaultValue = AgentsConfig.UNSET_ID
                },
            ),
        ) { entry ->
            AgentsResultsPage(navController = navController, backStackEntry = entry)
        }
    }
}

@Composable
internal fun rememberAgentsRootBackStackEntry(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
): NavBackStackEntry = remember(backStackEntry) {
    navController.getBackStackEntry(AgentsRoute.Graph.route)
}
