package com.marshall.pyerite.localization

import com.marshall.pyerite.sdeModule.room.dogma.TypeTraitDetail

fun localizedName(    zh: String?,
    en: String?,
    fallback: String?,
    language: ContentLanguage,
): String {
    val value = when (language) {
        ContentLanguage.CHINESE -> zh ?: fallback ?: en
        ContentLanguage.ENGLISH -> en ?: fallback ?: zh
    }
    return value.orEmpty()
}

fun LocalizableName.displayName(controller: LocaleController): String =
    localizedName(zhName, enName, name, controller.contentLanguage)

/** Name in the other content language: Chinese copies English, English copies Chinese. */
fun LocalizableName.translatedName(language: ContentLanguage): String = when (language) {
    ContentLanguage.CHINESE -> enName.orEmpty()
    ContentLanguage.ENGLISH -> zhName.orEmpty()
}

/** The other of the two content languages. */
fun ContentLanguage.opposite(): ContentLanguage = when (this) {
    ContentLanguage.CHINESE -> ContentLanguage.ENGLISH
    ContentLanguage.ENGLISH -> ContentLanguage.CHINESE
}

/** Room @Query projection; cannot implement [LocalizableName] (no [LocalizableName.name] column). */
fun TypeTraitDetail.displayName(controller: LocaleController): String =
    localizedName(zhName, enName, null, controller.contentLanguage)