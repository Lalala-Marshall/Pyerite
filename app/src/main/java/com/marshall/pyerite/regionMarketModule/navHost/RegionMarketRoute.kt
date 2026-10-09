package com.marshall.pyerite.regionMarketModule.navHost

import android.net.Uri

internal sealed class RegionMarketRoute(val route: String) {
    data object Root : RegionMarketRoute("region_market")

    data object Group : RegionMarketRoute("region_market/group/{groupId}") {
        fun create(groupId: Int) = "region_market/group/$groupId"
    }

    data object Types : RegionMarketRoute("region_market/types/{groupId}") {
        fun create(groupId: Int) = "region_market/types/$groupId"
    }

    data object Detail : RegionMarketRoute("region_market/type/{typeId}?$ARG_PLACE={$ARG_PLACE}") {
        fun create(typeId: Int, placeKey: String? = null): String {
            val base = "region_market/type/$typeId"
            if (placeKey.isNullOrBlank()) return base
            return "$base?$ARG_PLACE=${Uri.encode(placeKey)}"
        }
    }

    data object Orders : RegionMarketRoute("region_market/orders/{typeId}?$ARG_PLACE={$ARG_PLACE}") {
        fun create(typeId: Int, placeKey: String? = null): String {
            val base = "region_market/orders/$typeId"
            if (placeKey.isNullOrBlank()) return base
            return "$base?$ARG_PLACE=${Uri.encode(placeKey)}"
        }
    }

    data object Watchlists : RegionMarketRoute("region_market/watchlists")

    data object WatchlistDetail : RegionMarketRoute("region_market/watchlists/{$ARG_LIST_ID}") {
        fun create(listId: String) = "region_market/watchlists/${Uri.encode(listId)}"
    }

    companion object {
        const val ARG_PLACE = "place"
        const val ARG_LIST_ID = "listId"
    }
}
