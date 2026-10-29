package com.marshall.pyerite.sdeModule.update

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class SdeUpdateViewModel(
    private val controller: SdeUpdateController,
) : ViewModel() {

    val uiState: StateFlow<SdeUpdateUiState> = controller.uiState
    val isUpdateCheckInFlight: StateFlow<Boolean> = controller.isUpdateCheckInFlight

    fun repairStaleCheckingState() = controller.repairStaleCheckingState()
    fun refreshUpdateCheck() = controller.refreshUpdateCheck()
    fun openUpdateSheet() = controller.openUpdateSheet()
    fun dismissSheet() = controller.dismissSheet()
    fun startDownload() = controller.startDownload()
    fun retryDownload() = controller.retryDownload()
    fun retryCheck() = controller.retryCheck()
    fun reinstallBundledForDebug() = controller.reinstallBundledForDebug()
}
