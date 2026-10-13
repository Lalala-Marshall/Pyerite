package com.marshall.pyerite.ui.theme

import androidx.appcompat.app.AppCompatDelegate

/** Day / night selection. Copy in settings says light / dark; resources stay `values` / `values-night`. */
enum class ThemeMode {
    LIGHT,
    DARK,
    FOLLOW_SYSTEM,
    ;

    fun nightMode(): Int = when (this) {
        LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        DARK -> AppCompatDelegate.MODE_NIGHT_YES
        FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    /** 浅色模式 → 深色模式 → 跟随系统. */
    fun next(): ThemeMode = when (this) {
        LIGHT -> DARK
        DARK -> FOLLOW_SYSTEM
        FOLLOW_SYSTEM -> LIGHT
    }
}
