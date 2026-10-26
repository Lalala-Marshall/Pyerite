package com.marshall.pyerite.mainPageModule.model

/** Hideable home-page rows. Storage keys are the enum names. */
internal enum class HomePageEntry {
    CHARACTER_SHEET,
    CLONE_STATUS,
    SKILL_QUEUE,
    MAIL,
    CALENDAR,
    PERSONAL_PROPERTY,
    LOYALTY_POINTS,
    PEOPLE_PLACES,
    CHARACTER_MASTERY,
    CORPORATION_WALLET,
    CORPORATION_MEMBERS,
    CORPORATION_STRUCTURES,
    CORPORATION_INDUSTRY,
    CORPORATION_ASSETS,
    CORPORATION_CONTRACTS,
    DATABASE,
    REGION_MARKET,
    MARKET_WATCHLIST,
    AGENTS,
    PERSONAL_ASSETS,
    CONTRACT_LIST,
    CHARACTER_TRANSACTIONS,
    WALLET_JOURNAL,
    CHARACTER_INDUSTRY,
    ;

    companion object {
        val characterSection: List<HomePageEntry> = listOf(
            CHARACTER_SHEET,
            CLONE_STATUS,
            SKILL_QUEUE,
            MAIL,
            CALENDAR,
            PERSONAL_PROPERTY,
            LOYALTY_POINTS,
            PEOPLE_PLACES,
            CHARACTER_MASTERY,
        )
        val corporationSection: List<HomePageEntry> = listOf(
            CORPORATION_WALLET,
            CORPORATION_MEMBERS,
            CORPORATION_STRUCTURES,
            CORPORATION_INDUSTRY,
            CORPORATION_ASSETS,
            CORPORATION_CONTRACTS,
        )
        val dataSection: List<HomePageEntry> = listOf(
            DATABASE,
            AGENTS,
            REGION_MARKET,
            MARKET_WATCHLIST,
        )
        val businessSection: List<HomePageEntry> = listOf(
            PERSONAL_ASSETS,
            CONTRACT_LIST,
            CHARACTER_TRANSACTIONS,
            WALLET_JOURNAL,
            CHARACTER_INDUSTRY,
        )

        fun fromStorageKey(key: String): HomePageEntry? =
            entries.firstOrNull { it.name == key }
    }
}
