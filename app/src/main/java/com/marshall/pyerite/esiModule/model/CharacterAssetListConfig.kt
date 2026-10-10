package com.marshall.pyerite.esiModule.model

/** Paging for `GET /characters/{id}/assets`, shared by property valuation and the asset tree. */
internal object CharacterAssetListConfig {
    const val FIRST_PAGE = 1
    /** ESI character-assets page size. */
    const val PAGE_SIZE = 1_000
    const val MAX_PAGES = 200
}
