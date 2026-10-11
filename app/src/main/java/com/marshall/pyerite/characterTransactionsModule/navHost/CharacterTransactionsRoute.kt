package com.marshall.pyerite.characterTransactionsModule.navHost

sealed class CharacterTransactionsRoute(val route: String) {
    data object List : CharacterTransactionsRoute("character/transactions/{characterId}") {
        fun create(characterId: Long) = "character/transactions/$characterId"
    }

    data object Day : CharacterTransactionsRoute(
        "character/transactions/{characterId}/day/{dayKey}",
    ) {
        fun create(characterId: Long, dayKey: String) =
            "character/transactions/$characterId/day/$dayKey"
    }
}
