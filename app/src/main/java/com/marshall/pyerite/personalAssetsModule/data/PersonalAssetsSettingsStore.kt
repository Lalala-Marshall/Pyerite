package com.marshall.pyerite.personalAssetsModule.data

import android.content.Context
import androidx.core.content.edit
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsConfig
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class PersonalAssetsSettingsStore(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PersonalAssetsConfig.PREFS_NAME,
        Context.MODE_PRIVATE,
    )
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<PersonalAssetsSettings> = _settings.asStateFlow()

    fun current(): PersonalAssetsSettings = _settings.value

    fun save(settings: PersonalAssetsSettings) {
        prefs.edit {
            putBoolean(PersonalAssetsConfig.PREF_AGGREGATE, settings.aggregateCharacters)
            putBoolean(PersonalAssetsConfig.PREF_MERGE_LOCATIONS, settings.mergeSameLocation)
            putString(
                PersonalAssetsConfig.PREF_EXTRA_CHARACTERS,
                settings.extraCharacterIds.joinToString(PersonalAssetsConfig.PREF_ID_SEPARATOR),
            )
        }
        _settings.value = settings
    }

    private fun read(): PersonalAssetsSettings {
        val raw = prefs.getString(PersonalAssetsConfig.PREF_EXTRA_CHARACTERS, null).orEmpty()
        val ids = raw.split(PersonalAssetsConfig.PREF_ID_SEPARATOR)
            .mapNotNull { token -> token.toLongOrNull() }
            .toSet()
        return PersonalAssetsSettings(
            aggregateCharacters = prefs.getBoolean(PersonalAssetsConfig.PREF_AGGREGATE, false),
            mergeSameLocation = prefs.getBoolean(PersonalAssetsConfig.PREF_MERGE_LOCATIONS, false),
            extraCharacterIds = ids,
        )
    }
}
