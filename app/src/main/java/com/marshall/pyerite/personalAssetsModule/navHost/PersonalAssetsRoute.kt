package com.marshall.pyerite.personalAssetsModule.navHost

import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetLocationKind
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsConfig

internal object PersonalAssetsNavArgs {
    const val CHARACTER_ID = "characterId"
    const val OWNER_KEY = "ownerKey"
    const val LOCATION_KIND = "locationKind"
    const val LOCATION_ID = "locationId"
    const val ITEM_ID = "itemId"
}

sealed class PersonalAssetsRoute(val route: String) {
    object Regions : PersonalAssetsRoute(
        "character/assets/{${PersonalAssetsNavArgs.CHARACTER_ID}}",
    ) {
        fun create(characterId: Long) = "character/assets/$characterId"
    }

    object Location : PersonalAssetsRoute(
        "character/assets/{${PersonalAssetsNavArgs.CHARACTER_ID}}/" +
            "location/{${PersonalAssetsNavArgs.OWNER_KEY}}/" +
            "{${PersonalAssetsNavArgs.LOCATION_KIND}}/" +
            "{${PersonalAssetsNavArgs.LOCATION_ID}}",
    ) {
        internal fun create(
            characterId: Long,
            ownerKey: String,
            kind: PersonalAssetLocationKind,
            locationId: Long,
        ) = "character/assets/$characterId/location/$ownerKey/${kind.routeValue}/$locationId"
    }

    object Container : PersonalAssetsRoute(
        "character/assets/{${PersonalAssetsNavArgs.CHARACTER_ID}}/" +
            "container/{${PersonalAssetsNavArgs.ITEM_ID}}",
    ) {
        /** Path segment cannot contain `-`; negative synthetic ids use an `n` prefix. */
        fun create(characterId: Long, itemId: Long): String {
            val token = if (itemId < 0L) "$NEGATIVE_ITEM_PREFIX${-itemId}" else itemId.toString()
            return "character/assets/$characterId/container/$token"
        }

        fun parseItemId(token: String): Long {
            if (token.startsWith(NEGATIVE_ITEM_PREFIX)) {
                return -token.removePrefix(NEGATIVE_ITEM_PREFIX).toLong()
            }
            return token.toLong()
        }

        private const val NEGATIVE_ITEM_PREFIX = "n"
    }

    companion object {
        fun parseOwnerKey(token: String): Long? =
            if (token == PersonalAssetsConfig.MERGED_OWNER_KEY) null else token.toLong()
    }
}
