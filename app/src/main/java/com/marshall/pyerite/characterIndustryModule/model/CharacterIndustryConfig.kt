package com.marshall.pyerite.characterIndustryModule.model

/** Skill type ids and persisted aggregate settings for personal industry jobs. */
internal object CharacterIndustryConfig {
    const val PREFS_NAME = "character_industry"
    const val PREF_AGGREGATE = "aggregate_characters"
    const val PREF_EXTRA_CHARACTERS = "extra_character_ids"
    const val PREF_ID_SEPARATOR = ","

    /** Untrained characters still have one line of each kind. */
    const val BASE_SLOTS = 1

    const val MASS_PRODUCTION_TYPE_ID = 3387
    const val ADVANCED_MASS_PRODUCTION_TYPE_ID = 24625
    const val LABORATORY_OPERATION_TYPE_ID = 3406
    const val ADVANCED_LABORATORY_OPERATION_TYPE_ID = 24624
    const val MASS_REACTIONS_TYPE_ID = 45748
    const val ADVANCED_MASS_REACTIONS_TYPE_ID = 45749
}
