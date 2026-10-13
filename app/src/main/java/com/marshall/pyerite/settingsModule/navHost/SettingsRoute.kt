package com.marshall.pyerite.settingsModule.navHost

sealed class SettingsRoute(val route: String) {
    object Settings : SettingsRoute("settings")
    object Language : SettingsRoute("settings/language")
    object Notifications : SettingsRoute("settings/notifications")
}
