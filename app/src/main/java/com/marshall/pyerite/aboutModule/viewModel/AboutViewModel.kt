package com.marshall.pyerite.aboutModule.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.aboutModule.data.AboutConfig
import com.marshall.pyerite.sdeModule.update.SdeReleaseMeta
import com.marshall.pyerite.sdeModule.update.SdeUpdateController
import com.marshall.pyerite.sdeModule.update.SdeUpdateRepository
import com.marshall.pyerite.sdeModule.update.SdeUpdateSheetContent
import com.marshall.pyerite.sdeModule.update.SdeUpdateUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal enum class AboutSdeUpdateStatus {
    Checking,
    UpToDate,
    UpdateAvailable,
    CheckFailed,
    Unknown,
}

internal data class AboutUiState(
    val versionName: String,
    val sdeBuildLabel: String,
    val sdeUpdateStatus: AboutSdeUpdateStatus,
)

internal class AboutViewModel(
    versionName: String,
    private val repository: SdeUpdateRepository,
    controller: SdeUpdateController,
) : ViewModel() {

    private val installedRelease = MutableStateFlow(repository.installedRelease())

    val uiState: StateFlow<AboutUiState> = combine(
        installedRelease,
        controller.uiState,
        controller.isUpdateCheckInFlight,
    ) { meta, updateState, checkInFlight ->
        AboutUiState(
            versionName = versionName,
            sdeBuildLabel = formatInstalledSdeBuild(meta),
            sdeUpdateStatus = updateState.toAboutStatus(checkInFlight),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(UI_STATE_STOP_TIMEOUT_MILLIS),
        initialValue = AboutUiState(
            versionName = versionName,
            sdeBuildLabel = formatInstalledSdeBuild(repository.installedRelease()),
            sdeUpdateStatus = controller.uiState.value.toAboutStatus(
                controller.isUpdateCheckInFlight.value,
            ),
        ),
    )

    init {
        viewModelScope.launch {
            repository.contentRefreshed.collect {
                installedRelease.value = repository.installedRelease()
            }
        }
    }

    private companion object {
        const val UI_STATE_STOP_TIMEOUT_MILLIS = 5_000L
    }
}

internal fun formatInstalledSdeBuild(meta: SdeReleaseMeta?): String {
    if (meta == null) return ""
    val date = meta.releaseDate
        .substringBefore(AboutConfig.ISO_DATE_TIME_SEPARATOR)
        .takeIf { it.isNotBlank() }
    val build = meta.buildNumber.takeIf { it.isNotBlank() }
    return when {
        date != null && build != null -> "$date-$build"
        build != null -> build
        date != null -> date
        else -> ""
    }
}

private fun SdeUpdateUiState.toAboutStatus(checkInFlight: Boolean): AboutSdeUpdateStatus {
    return when (this) {
        is SdeUpdateUiState.Checking -> AboutSdeUpdateStatus.Checking
        is SdeUpdateUiState.UpToDate -> AboutSdeUpdateStatus.UpToDate
        is SdeUpdateUiState.UpdateReady -> AboutSdeUpdateStatus.UpdateAvailable
        is SdeUpdateUiState.CheckFailed -> AboutSdeUpdateStatus.CheckFailed
        is SdeUpdateUiState.Idle -> {
            if (checkInFlight) AboutSdeUpdateStatus.Checking else AboutSdeUpdateStatus.Unknown
        }
        is SdeUpdateUiState.Sheet -> content.toAboutStatus()
    }
}

private fun SdeUpdateSheetContent.toAboutStatus(): AboutSdeUpdateStatus {
    return when (this) {
        is SdeUpdateSheetContent.Checking -> AboutSdeUpdateStatus.Checking
        is SdeUpdateSheetContent.CheckFailed -> AboutSdeUpdateStatus.CheckFailed
        is SdeUpdateSheetContent.Prompt,
        is SdeUpdateSheetContent.Downloading,
        is SdeUpdateSheetContent.DownloadFailed,
        -> AboutSdeUpdateStatus.UpdateAvailable
        is SdeUpdateSheetContent.Completed,
        is SdeUpdateSheetContent.Info,
        -> AboutSdeUpdateStatus.UpToDate
    }
}
