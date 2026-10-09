package com.marshall.pyerite.regionMarketModule.watchlist.model

internal object MarketWatchlistConfig {
    const val PREFS_NAME = "pyerite_market_watchlists"
    const val KEY_LISTS = "lists"

    const val DEFAULT_QUANTITY = 1L
    const val QUANTITY_MAX_DIGITS = 12

    /** Largest quantity the edit field can hold (QUANTITY_MAX_DIGITS nines). */
    const val MAX_QUANTITY = 999_999_999_999L

    /** Parallel ESI order lookups while a watchlist detail page is loading prices. */
    const val QUOTE_PARALLELISM = 4

    /**
     * Any non-PLEX type id. Place-name resolution only special-cases PLEX,
     * so this keeps the list's own market label.
     */
    const val LOCATION_RESOLVE_TYPE_ID = 0

    /** Same height as the market location sheet. */
    const val SHEET_HEIGHT_FRACTION = 0.94f

    /** Same width as the skill-plan create dialog. */
    const val DIALOG_WIDTH_FRACTION = 0.82f

    /** Order-type filter is half again as wide as its content. */
    const val ORDER_SIDE_WIDTH_SCALE = 1.5f

    /** Keep the delete action open after a swipe past this fraction of its width. */
    const val DELETE_REVEAL_FRACTION = 0.5f

    const val IMPORT_SEPARATOR = '\t'
}
