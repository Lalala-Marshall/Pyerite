package com.marshall.pyerite.regionMarketModule.navHost

internal sealed class RegionMarketRoute(val route: String) {
    data object Root : RegionMarketRoute("region_market")

    data object Group : RegionMarketRoute("region_market/group/{groupId}") {
        fun create(groupId: Int) = "region_market/group/$groupId"
    }

    data object Types : RegionMarketRoute("region_market/types/{groupId}") {
        fun create(groupId: Int) = "region_market/types/$groupId"
    }

    data object Detail : RegionMarketRoute("region_market/type/{typeId}") {
        fun create(typeId: Int) = "region_market/type/$typeId"
    }

    data object Orders : RegionMarketRoute("region_market/orders/{typeId}") {
        fun create(typeId: Int) = "region_market/orders/$typeId"
    }
}
