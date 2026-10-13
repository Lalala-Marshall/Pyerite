package com.marshall.pyerite.characterIndustryModule.navHost

sealed class CharacterIndustryRoute(val route: String) {
    data object Industry : CharacterIndustryRoute("character/industry/{characterId}") {
        fun create(characterId: Long) = "character/industry/$characterId"
    }
}
