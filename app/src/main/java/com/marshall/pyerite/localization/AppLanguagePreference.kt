package com.marshall.pyerite.localization

import androidx.core.os.LocaleListCompat

enum class AppLanguagePreference {
    CHINESE,
    ENGLISH,
    FOLLOW_SYSTEM,
    ;

    fun toLocaleList(): LocaleListCompat = when (this) {
        CHINESE -> LocaleListCompat.forLanguageTags(LANGUAGE_TAG_CHINESE)
        ENGLISH -> LocaleListCompat.forLanguageTags(LANGUAGE_TAG_ENGLISH)
        FOLLOW_SYSTEM -> LocaleListCompat.getEmptyLocaleList()
    }

    companion object {
        const val LANGUAGE_TAG_CHINESE = "zh"
        const val LANGUAGE_TAG_ENGLISH = "en"

        fun fromStoredName(name: String?): AppLanguagePreference =
            entries.firstOrNull { it.name == name } ?: FOLLOW_SYSTEM
    }
}
