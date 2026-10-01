package com.marshall.pyerite.corporationModule.industry.navHost

sealed class CorporationIndustryRoute(val route: String) {
    object Industry : CorporationIndustryRoute("corporation/industry/{characterId}") {
        fun create(characterId: Long) = "corporation/industry/$characterId"
    }
}
