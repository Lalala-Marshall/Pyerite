package com.marshall.pyerite.characterWalletJournalModule.navHost

sealed class CharacterWalletJournalRoute(val route: String) {
    data object List : CharacterWalletJournalRoute("character/wallet/journal/{characterId}") {
        fun create(characterId: Long) = "character/wallet/journal/$characterId"
    }

    data object Day : CharacterWalletJournalRoute(
        "character/wallet/journal/{characterId}/day/{dayKey}",
    ) {
        fun create(characterId: Long, dayKey: String) =
            "character/wallet/journal/$characterId/day/$dayKey"
    }
}
