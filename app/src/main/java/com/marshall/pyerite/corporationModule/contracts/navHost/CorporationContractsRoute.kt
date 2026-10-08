package com.marshall.pyerite.corporationModule.contracts.navHost

sealed class CorporationContractsRoute(val route: String) {
    object Contracts : CorporationContractsRoute("corporation/contracts/{characterId}") {
        fun create(characterId: Long) = "corporation/contracts/$characterId"
    }

    object Detail : CorporationContractsRoute(
        "corporation/contracts/{characterId}/detail/{contractId}",
    ) {
        fun create(characterId: Long, contractId: Long) =
            "corporation/contracts/$characterId/detail/$contractId"
    }
}
