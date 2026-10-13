package com.marshall.pyerite.settingsModule.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.infra.notification.NotificationMasterChoice
import com.marshall.pyerite.infra.notification.NotificationPreferences
import com.marshall.pyerite.infra.prefs.HomeLayoutPreferences
import com.marshall.pyerite.localization.AppLanguagePreference
import com.marshall.pyerite.localization.ContentLanguage
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.sdeModule.room.RoomProvider
import com.marshall.pyerite.settingsModule.data.AppCacheCleaner
import com.marshall.pyerite.ui.theme.ThemeModeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SettingsViewModel(
    private val themeModeStore: ThemeModeStore,
    private val localeController: LocaleController,
    private val homeLayoutPreferences: HomeLayoutPreferences,
    private val notificationPreferences: NotificationPreferences,
    private val cacheCleaner: AppCacheCleaner,
    private val roomProvider: RoomProvider,
) : ViewModel() {

    val themeMode = themeModeStore.mode
    val appLanguage = localeController.appLanguage
    val databaseLanguage = localeController.databaseLanguage
    val showCorporationAffairs = homeLayoutPreferences.showCorporationAffairs
    val notificationMaster = notificationPreferences.master
    val calendarNotificationsEnabled = notificationPreferences.calendarEnabled
    val skillQueueNotificationsEnabled = notificationPreferences.skillQueueEnabled

    private val _cacheMegabytes = MutableStateFlow<String?>(null)
    val cacheMegabytes: StateFlow<String?> = _cacheMegabytes.asStateFlow()

    private val _clearingCache = MutableStateFlow(false)
    val clearingCache: StateFlow<Boolean> = _clearingCache.asStateFlow()

    init {
        refreshCacheSize()
    }

    fun cycleTheme() = themeModeStore.cycle()

    fun setAppLanguage(value: AppLanguagePreference) {
        localeController.setAppLanguage(value)
    }

    fun setDatabaseLanguage(value: ContentLanguage): Boolean {
        val changed = localeController.setDatabaseLanguage(value)
        if (changed) roomProvider.closeAndInvalidate()
        return changed
    }

    fun setShowCorporationAffairs(show: Boolean) {
        homeLayoutPreferences.setShowCorporationAffairs(show)
    }

    fun setNotificationMaster(choice: NotificationMasterChoice) {
        notificationPreferences.setMaster(choice)
    }

    fun setCalendarNotificationsEnabled(enabled: Boolean) {
        notificationPreferences.setCalendarEnabled(enabled)
    }

    fun setSkillQueueNotificationsEnabled(enabled: Boolean) {
        notificationPreferences.setSkillQueueEnabled(enabled)
    }

    fun hasRequestedPostNotifications(): Boolean =
        notificationPreferences.hasRequestedPostNotifications()

    fun markPostNotificationsRequested() {
        notificationPreferences.markPostNotificationsRequested()
    }

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            _cacheMegabytes.value = cacheCleaner.formatMegabytes(cacheCleaner.measureBytes())
        }
    }

    fun clearCache() {
        if (_clearingCache.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _clearingCache.value = true
            try {
                cacheCleaner.clear()
                _cacheMegabytes.value = cacheCleaner.formatMegabytes(cacheCleaner.measureBytes())
            } finally {
                _clearingCache.value = false
            }
        }
    }
}
