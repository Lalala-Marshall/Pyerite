package com.marshall.pyerite.localization

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class LocaleController(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _appLanguage = MutableStateFlow(readAppLanguage())
    private val _databaseLanguage = MutableStateFlow(readDatabaseLanguage())

    val appLanguage: StateFlow<AppLanguagePreference> = _appLanguage.asStateFlow()

    /** Explicit SDE / bilingual-name language. Does not follow the system after first launch. */
    val databaseLanguage: StateFlow<ContentLanguage> = _databaseLanguage.asStateFlow()

    /** Resolved language for SDE database selection and bilingual entity name fields. */
    val contentLanguage: ContentLanguage
        get() = _databaseLanguage.value

    /**
     * Language of app chrome (`R.string`), including [AppLanguagePreference.FOLLOW_SYSTEM].
     * Duration text inside a notification should follow this, so it matches the notification sentence.
     */
    fun resolvedAppContentLanguage(): ContentLanguage = when (_appLanguage.value) {
        AppLanguagePreference.CHINESE -> ContentLanguage.CHINESE
        AppLanguagePreference.ENGLISH -> ContentLanguage.ENGLISH
        AppLanguagePreference.FOLLOW_SYSTEM -> ContentLanguage.fromLocale(systemLocale())
    }

    fun resolveDatabaseFileName(): String = SdeDatabase.fileName(contentLanguage)

    fun setAppLanguage(value: AppLanguagePreference) {
        if (_appLanguage.value == value) return
        prefs.edit { putString(KEY_APP_LANGUAGE, value.name) }
        _appLanguage.value = value
        applyAppLanguage()
    }

    /** @return true when the database file selection changed and Room must be reopened. */
    fun setDatabaseLanguage(value: ContentLanguage): Boolean {
        if (_databaseLanguage.value == value) return false
        prefs.edit { putString(KEY_DATABASE_LANGUAGE, value.name) }
        _databaseLanguage.value = value
        return true
    }

    fun applyAppLanguage() {
        AppCompatDelegate.setApplicationLocales(_appLanguage.value.toLocaleList())
    }

    private fun readAppLanguage(): AppLanguagePreference {
        val stored = prefs.getString(KEY_APP_LANGUAGE, null)
        if (stored != null) return AppLanguagePreference.fromStoredName(stored)
        val migrated = legacyPreference()
        prefs.edit { putString(KEY_APP_LANGUAGE, migrated.name) }
        return migrated
    }

    private fun readDatabaseLanguage(): ContentLanguage {
        val stored = prefs.getString(KEY_DATABASE_LANGUAGE, null)
        if (stored != null) {
            return ContentLanguage.entries.firstOrNull { it.name == stored }
                ?: ContentLanguage.fromLocale(systemLocale())
        }
        val migrated = when (legacyPreference()) {
            AppLanguagePreference.CHINESE -> ContentLanguage.CHINESE
            AppLanguagePreference.ENGLISH -> ContentLanguage.ENGLISH
            AppLanguagePreference.FOLLOW_SYSTEM -> ContentLanguage.fromLocale(systemLocale())
        }
        prefs.edit { putString(KEY_DATABASE_LANGUAGE, migrated.name) }
        return migrated
    }

    private fun legacyPreference(): AppLanguagePreference {
        if (!prefs.contains(KEY_LEGACY_PREFERENCE)) return AppLanguagePreference.FOLLOW_SYSTEM
        return AppLanguagePreference.entries.getOrNull(
            prefs.getInt(KEY_LEGACY_PREFERENCE, AppLanguagePreference.FOLLOW_SYSTEM.ordinal),
        ) ?: AppLanguagePreference.FOLLOW_SYSTEM
    }

    private fun systemLocale(): Locale =
        LocaleListCompat.getAdjustedDefault()[0] ?: Locale.getDefault()

    companion object {
        private const val PREFS_NAME = "pyerite_locale"
        private const val KEY_LEGACY_PREFERENCE = "language_preference"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_DATABASE_LANGUAGE = "database_language"
    }
}
