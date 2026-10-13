package com.marshall.pyerite.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeModeStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(read())

    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun current(): ThemeMode = _mode.value

    fun cycle() {
        set(_mode.value.next())
    }

    fun applyCurrent() {
        AppCompatDelegate.setDefaultNightMode(_mode.value.nightMode())
    }

    private fun set(mode: ThemeMode) {
        if (_mode.value == mode) return
        prefs.edit { putString(KEY_MODE, mode.name) }
        _mode.value = mode
        AppCompatDelegate.setDefaultNightMode(mode.nightMode())
    }

    private fun read(): ThemeMode {
        val stored = prefs.getString(KEY_MODE, null)
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.FOLLOW_SYSTEM
    }

    companion object {
        private const val PREFS_NAME = "pyerite_theme"
        private const val KEY_MODE = "theme_mode"
    }
}
