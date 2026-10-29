package com.marshall.pyerite.sdeModule.update

data class SdeUpdateVersionVisibility(
    val showDatabase: Boolean,
    val showIcons: Boolean,
) {
    companion object {
        val All = SdeUpdateVersionVisibility(showDatabase = true, showIcons = true)
    }
}

sealed interface SdeUpdateUiState {
    data object Idle : SdeUpdateUiState
    data object Checking : SdeUpdateUiState
    data object UpToDate : SdeUpdateUiState

    data class UpdateReady(
        val installedBuild: String,
        val remoteBuild: String,
        val installedIconVersion: Int?,
        val remoteIconVersion: Int?,
        val versionVisibility: SdeUpdateVersionVisibility,
    ) : SdeUpdateUiState

    data class CheckFailed(
        val installedBuild: String?,
        val installedIconVersion: Int?,
        val message: String,
    ) : SdeUpdateUiState

    data class Sheet(
        val content: SdeUpdateSheetContent,
        val instanceId: Int,
    ) : SdeUpdateUiState
}

sealed interface SdeUpdateSheetContent {
    data class Prompt(
        val installedBuild: String,
        val remoteBuild: String,
        val installedIconVersion: Int?,
        val remoteIconVersion: Int?,
        val versionVisibility: SdeUpdateVersionVisibility,
    ) : SdeUpdateSheetContent

    data class Downloading(
        val installedBuild: String,
        val remoteBuild: String,
        val installedIconVersion: Int?,
        val remoteIconVersion: Int?,
        val versionVisibility: SdeUpdateVersionVisibility,
        val progressPercent: Int,
        val applying: Boolean = false,
    ) : SdeUpdateSheetContent

    data class Completed(
        val previousBuild: String,
        val newBuild: String,
        val previousIconVersion: Int?,
        val newIconVersion: Int?,
        val versionVisibility: SdeUpdateVersionVisibility,
    ) : SdeUpdateSheetContent

    data class DownloadFailed(
        val installedBuild: String,
        val remoteBuild: String,
        val installedIconVersion: Int?,
        val remoteIconVersion: Int?,
        val versionVisibility: SdeUpdateVersionVisibility,
        val message: String,
        val cancelled: Boolean,
    ) : SdeUpdateSheetContent

    data class Checking(
        val installedBuild: String?,
        val installedIconVersion: Int?,
    ) : SdeUpdateSheetContent

    data class CheckFailed(
        val installedBuild: String?,
        val installedIconVersion: Int?,
        val message: String,
    ) : SdeUpdateSheetContent

    data class Info(
        val installedBuild: String?,
        val installedIconVersion: Int?,
    ) : SdeUpdateSheetContent
}
