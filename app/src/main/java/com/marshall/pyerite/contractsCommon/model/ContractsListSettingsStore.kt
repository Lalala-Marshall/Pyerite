package com.marshall.pyerite.contractsCommon.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Shared grouping, type, status, and display-limit preferences for both contract lists. */
internal class ContractsListSettingsStore {
    private val _settings = MutableStateFlow(ContractsListSettings())
    val settings: StateFlow<ContractsListSettings> = _settings.asStateFlow()

    fun update(transform: (ContractsListSettings) -> ContractsListSettings) {
        _settings.update(transform)
    }
}
