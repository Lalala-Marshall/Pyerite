package com.marshall.pyerite.infra.prefs

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeLayoutPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _showCorporationAffairs = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_CORPORATION_AFFAIRS, DEFAULT_SHOW_CORPORATION_AFFAIRS),
    )

    val showCorporationAffairs: StateFlow<Boolean> = _showCorporationAffairs.asStateFlow()

    fun setShowCorporationAffairs(show: Boolean) {
        if (_showCorporationAffairs.value == show) return
        prefs.edit { putBoolean(KEY_SHOW_CORPORATION_AFFAIRS, show) }
        _showCorporationAffairs.value = show
    }

    companion object {
        private const val PREFS_NAME = "pyerite_home_layout"
        private const val KEY_SHOW_CORPORATION_AFFAIRS = "show_corporation_affairs"
        private const val DEFAULT_SHOW_CORPORATION_AFFAIRS = true
    }
}
