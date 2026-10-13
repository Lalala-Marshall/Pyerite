package com.marshall.pyerite.aboutModule.navHost

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.marshall.pyerite.aboutModule.ui.AboutPage

fun NavGraphBuilder.aboutNavGraph(navController: NavController) {
    composable(route = AboutRoute.About.route) {
        AboutPage(navController = navController)
    }
}
