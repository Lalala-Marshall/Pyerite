package com.marshall.pyerite.corporationModule.assets.navHost

import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetLocationKind

internal object CorporationAssetsNavArgs {
    const val CHARACTER_ID = "characterId"
    const val LOCATION_KIND = "locationKind"
    const val LOCATION_ID = "locationId"
    const val FLAG = "flag"
    const val ITEM_ID = "itemId"
}

sealed class CorporationAssetsRoute(val route: String) {
    object Regions : CorporationAssetsRoute("corporation/assets/{${CorporationAssetsNavArgs.CHARACTER_ID}}") {
        fun create(characterId: Long) = "corporation/assets/$characterId"
    }

    object Location : CorporationAssetsRoute(
        "corporation/assets/{${CorporationAssetsNavArgs.CHARACTER_ID}}/" +
            "location/{${CorporationAssetsNavArgs.LOCATION_KIND}}/" +
            "{${CorporationAssetsNavArgs.LOCATION_ID}}",
    ) {
        internal fun create(
            characterId: Long,
            kind: CorporationAssetLocationKind,
            locationId: Long,
        ) = "corporation/assets/$characterId/location/${kind.routeValue}/$locationId"
    }

    object Folder : CorporationAssetsRoute(
        "corporation/assets/{${CorporationAssetsNavArgs.CHARACTER_ID}}/" +
            "folder/{${CorporationAssetsNavArgs.LOCATION_KIND}}/" +
            "{${CorporationAssetsNavArgs.LOCATION_ID}}/" +
            "{${CorporationAssetsNavArgs.FLAG}}",
    ) {
        internal fun create(
            characterId: Long,
            kind: CorporationAssetLocationKind,
            locationId: Long,
            flag: String,
        ) = "corporation/assets/$characterId/folder/${kind.routeValue}/$locationId/$flag"
    }

    object Container : CorporationAssetsRoute(
        "corporation/assets/{${CorporationAssetsNavArgs.CHARACTER_ID}}/" +
            "container/{${CorporationAssetsNavArgs.ITEM_ID}}",
    ) {
        /** Path segment cannot contain `-`; negative synthetic ids use an `n` prefix. */
        fun create(characterId: Long, itemId: Long): String {
            val token = if (itemId < 0L) "$NEGATIVE_ITEM_PREFIX${-itemId}" else itemId.toString()
            return "corporation/assets/$characterId/container/$token"
        }

        fun parseItemId(token: String): Long {
            if (token.startsWith(NEGATIVE_ITEM_PREFIX)) {
                return -token.removePrefix(NEGATIVE_ITEM_PREFIX).toLong()
            }
            return token.toLong()
        }

        private const val NEGATIVE_ITEM_PREFIX = "n"
    }
}
