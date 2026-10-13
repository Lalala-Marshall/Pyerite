package com.marshall.pyerite.characterIndustryModule.data

import android.content.Context
import androidx.core.content.edit
import com.marshall.pyerite.characterIndustryModule.model.CharacterIndustryConfig
import com.marshall.pyerite.characterIndustryModule.model.CharacterIndustrySettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class CharacterIndustrySettingsStore(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(
        CharacterIndustryConfig.PREFS_NAME,
        Context.MODE_PRIVATE,
    )
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<CharacterIndustrySettings> = _settings.asStateFlow()

    fun current(): CharacterIndustrySettings = _settings.value

    fun save(settings: CharacterIndustrySettings) {
        prefs.edit {
            putBoolean(CharacterIndustryConfig.PREF_AGGREGATE, settings.aggregateCharacters)
            putString(
                CharacterIndustryConfig.PREF_EXTRA_CHARACTERS,
                settings.extraCharacterIds.joinToString(CharacterIndustryConfig.PREF_ID_SEPARATOR),
            )
        }
        _settings.value = settings
    }

    private fun read(): CharacterIndustrySettings {
        val raw = prefs.getString(CharacterIndustryConfig.PREF_EXTRA_CHARACTERS, null).orEmpty()
        val ids = raw.split(CharacterIndustryConfig.PREF_ID_SEPARATOR)
            .mapNotNull { token -> token.toLongOrNull() }
            .toSet()
        return CharacterIndustrySettings(
            aggregateCharacters = prefs.getBoolean(CharacterIndustryConfig.PREF_AGGREGATE, false),
            extraCharacterIds = ids,
        )
    }
}
