package com.marshall.pyerite.settingsModule.navHost

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.marshall.pyerite.settingsModule.ui.LanguageSettingsPage
import com.marshall.pyerite.settingsModule.ui.NotificationSettingsPage
import com.marshall.pyerite.settingsModule.ui.SettingsPage

fun NavGraphBuilder.settingsNavGraph(navController: NavController) {
    composable(route = SettingsRoute.Settings.route) {
        SettingsPage(navController = navController)
    }
    composable(route = SettingsRoute.Language.route) {
        LanguageSettingsPage(navController = navController)
    }
    composable(route = SettingsRoute.Notifications.route) {
        NotificationSettingsPage(navController = navController)
    }
}
