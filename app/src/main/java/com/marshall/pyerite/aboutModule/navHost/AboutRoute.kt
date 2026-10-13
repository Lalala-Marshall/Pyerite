package com.marshall.pyerite.aboutModule.navHost

sealed class AboutRoute(val route: String) {
    data object About : AboutRoute("about")
}
