package com.marshall.pyerite.contractListModule.navHost

sealed class ContractListRoute(val route: String) {
    object List : ContractListRoute("contracts/list/{characterId}") {
        fun create(characterId: Long) = "contracts/list/$characterId"
    }

    object Detail : ContractListRoute(
        "contracts/list/{characterId}/detail/{contractId}",
    ) {
        fun create(characterId: Long, contractId: Long) =
            "contracts/list/$characterId/detail/$contractId"
    }
}
