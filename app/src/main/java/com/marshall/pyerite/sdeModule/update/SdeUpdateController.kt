package com.marshall.pyerite.sdeModule.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SdeUpdateController(
    private val repository: SdeUpdateRepository,
    private val scope: CoroutineScope,
) {

    private val _uiState = MutableStateFlow<SdeUpdateUiState>(SdeUpdateUiState.Idle)
    val uiState: StateFlow<SdeUpdateUiState> = _uiState.asStateFlow()

    private val _isUpdateCheckInFlight = MutableStateFlow(false)
    val isUpdateCheckInFlight: StateFlow<Boolean> = _isUpdateCheckInFlight.asStateFlow()

    private var pendingUpdate: SdeRemotePackage? = null
    private var downloadJob: Job? = null
    private var checkJob: Job? = null
    private var checkGeneration = 0
    private var downloadGeneration = 0
    private var sheetInstanceId = 0

    init {
        scope.launch {
            _uiState
                .map(::describeUiState)
                .distinctUntilChanged()
                .collect { summary ->
                    SdeUpdateLog.d("uiState -> $summary | ${describeJobs()}")
                }
        }
    }

    fun checkForUpdates(inSheet: Boolean = false) {
        logAction("checkForUpdates", "inSheet=$inSheet ${describeJobs()}")
        if (downloadJob?.isActive == true) {
            logAction("checkForUpdates", "skipped: download active")
            return
        }

        if (!inSheet) {
            repairStaleCheckingState()
            if (checkJob?.isActive == true) {
                logAction("checkForUpdates", "skipped: background check already active")
                return
            }
            if (_uiState.value !is SdeUpdateUiState.Idle) {
                logAction(
                    "checkForUpdates",
                    "skipped: already resolved uiState=${describeUiState(_uiState.value)}",
                )
                return
            }
        } else {
            logAction("checkForUpdates", "cancelling previous check job for in-sheet retry")
            checkJob?.cancel()
        }

        val installedForSheet = resolveInstalledSnapshotForSheetCheck(inSheet)

        val generation = ++checkGeneration
        logAction("checkForUpdates", "starting generation=$generation inSheet=$inSheet")
        val job = scope.launch {
            if (inSheet) {
                setCheckUiState(generation) {
                    presentSheet(
                        SdeUpdateSheetContent.Checking(
                            installedBuild = installedForSheet.build,
                            installedIconVersion = installedForSheet.iconVersion,
                        ),
                    )
                }
            }
            try {
                logAction("checkForUpdates", "generation=$generation fetching remote metadata")
                val remote = repository.checkForRemoteUpdate()
                if (!isCheckGenerationActive(generation)) {
                    logAction("checkForUpdates", "generation=$generation stale after fetch, ignoring result")
                    return@launch
                }
                if (remote == null) {
                    pendingUpdate = null
                    logAction("checkForUpdates", "generation=$generation result=UpToDate")
                    val info = SdeUpdateSheetContent.Info(
                        installedBuild = repository.installedBuildNumber(),
                        installedIconVersion = repository.installedIconVersion(),
                    )
                    _uiState.value = if (checkResultStaysInSheet(inSheet)) {
                        updateSheet(info)
                    } else {
                        SdeUpdateUiState.UpToDate
                    }
                } else {
                    pendingUpdate = remote
                    val installedBuild = repository.installedBuildNumber().orEmpty()
                    val installedIconVersion = repository.installedIconVersion()
                    val versionVisibility = repository.resolveVersionVisibility(remote)
                    logAction(
                        "checkForUpdates",
                        "generation=$generation result=UpdateReady " +
                            "installed=$installedBuild icons=$installedIconVersion " +
                            "remote=${remote.meta.buildNumber} icons=${remote.meta.iconVersion}",
                    )
                    setCheckUiState(generation) {
                        if (checkResultStaysInSheet(inSheet)) {
                            updateSheet(
                                SdeUpdateSheetContent.Prompt(
                                    installedBuild = installedBuild,
                                    remoteBuild = remote.meta.buildNumber,
                                    installedIconVersion = installedIconVersion,
                                    remoteIconVersion = remote.meta.iconVersion,
                                    versionVisibility = versionVisibility,
                                ),
                            )
                        } else {
                            SdeUpdateUiState.UpdateReady(
                                installedBuild = installedBuild,
                                remoteBuild = remote.meta.buildNumber,
                                installedIconVersion = installedIconVersion,
                                remoteIconVersion = remote.meta.iconVersion,
                                versionVisibility = versionVisibility,
                            )
                        }
                    }
                }
            } catch (error: CancellationException) {
                if (error is TimeoutCancellationException && isCheckGenerationActive(generation)) {
                    pendingUpdate = null
                    val installedBuild = repository.installedBuildNumber()
                    val installedIconVersion = repository.installedIconVersion()
                    val message = error.message.orEmpty()
                    logAction("checkForUpdates", "generation=$generation timed out: $message", error)
                    applyCheckFailure(
                        generation = generation,
                        inSheet = inSheet,
                        installedForSheet = installedForSheet,
                        installedBuild = installedBuild,
                        installedIconVersion = installedIconVersion,
                        message = message.ifBlank { "Update check timed out" },
                    )
                } else {
                    logAction("checkForUpdates", "generation=$generation cancelled")
                    throw error
                }
            } catch (error: Exception) {
                if (!isCheckGenerationActive(generation)) {
                    logAction("checkForUpdates", "generation=$generation stale after failure, ignoring")
                    return@launch
                }
                pendingUpdate = null
                val installedBuild = repository.installedBuildNumber()
                val installedIconVersion = repository.installedIconVersion()
                val message = error.message.orEmpty()
                logAction("checkForUpdates", "generation=$generation failed: $message", error)
                applyCheckFailure(
                    generation = generation,
                    inSheet = inSheet,
                    installedForSheet = installedForSheet,
                    installedBuild = installedBuild,
                    installedIconVersion = installedIconVersion,
                    message = message,
                )
            }
        }
        checkJob = job
        job.invokeOnCompletion { cause ->
            if (checkJob === job) {
                checkJob = null
                _isUpdateCheckInFlight.value = false
            }
            logAction(
                "checkForUpdates",
                "generation=$generation completed cause=${cause?.javaClass?.simpleName ?: "none"} " +
                    describeJobs(),
            )
        }
    }

    private fun applyCheckFailure(
        generation: Int,
        inSheet: Boolean,
        installedForSheet: InstalledVersionSnapshot,
        installedBuild: String?,
        installedIconVersion: Int?,
        message: String,
    ) {
        setCheckUiState(generation) {
            if (checkResultStaysInSheet(inSheet)) {
                updateSheet(
                    SdeUpdateSheetContent.CheckFailed(
                        installedBuild = installedForSheet.build ?: installedBuild,
                        installedIconVersion = installedForSheet.iconVersion ?: installedIconVersion,
                        message = message,
                    ),
                )
            } else {
                SdeUpdateUiState.CheckFailed(
                    installedBuild = installedBuild,
                    installedIconVersion = installedIconVersion,
                    message = message,
                )
            }
        }
    }

    fun openUpdateSheet() {
        logAction("openUpdateSheet", describeUiState(_uiState.value))
        when (val state = _uiState.value) {
            is SdeUpdateUiState.UpdateReady -> {
                _uiState.value = presentSheet(
                    SdeUpdateSheetContent.Prompt(
                        installedBuild = state.installedBuild,
                        remoteBuild = state.remoteBuild,
                        installedIconVersion = state.installedIconVersion,
                        remoteIconVersion = state.remoteIconVersion,
                        versionVisibility = state.versionVisibility,
                    ),
                )
            }
            is SdeUpdateUiState.CheckFailed -> {
                _uiState.value = presentSheet(
                    SdeUpdateSheetContent.CheckFailed(
                        installedBuild = state.installedBuild,
                        installedIconVersion = state.installedIconVersion,
                        message = state.message,
                    ),
                )
            }
            is SdeUpdateUiState.UpToDate,
            is SdeUpdateUiState.Idle,
            is SdeUpdateUiState.Checking -> {
                _uiState.value = presentInstalledInfoSheet()
            }
            is SdeUpdateUiState.Sheet -> Unit
        }
    }

    /** Keep an already-open info sheet in place when a background check finishes. */
    private fun checkResultStaysInSheet(inSheet: Boolean): Boolean {
        if (inSheet) return true
        val content = (_uiState.value as? SdeUpdateUiState.Sheet)?.content
        return content is SdeUpdateSheetContent.Info || content is SdeUpdateSheetContent.Checking
    }

    private fun presentInstalledInfoSheet(): SdeUpdateUiState.Sheet {
        return presentSheet(
            SdeUpdateSheetContent.Info(
                installedBuild = repository.installedBuildNumber(),
                installedIconVersion = repository.installedIconVersion(),
            ),
        )
    }

    fun dismissSheet() {
        when (val state = _uiState.value) {
            is SdeUpdateUiState.Sheet -> when (val content = state.content) {
                is SdeUpdateSheetContent.Downloading -> cancelDownload()
                is SdeUpdateSheetContent.Prompt -> {
                    _uiState.value = SdeUpdateUiState.UpdateReady(
                        installedBuild = content.installedBuild,
                        remoteBuild = content.remoteBuild,
                        installedIconVersion = content.installedIconVersion,
                        remoteIconVersion = content.remoteIconVersion,
                        versionVisibility = content.versionVisibility,
                    )
                }
                is SdeUpdateSheetContent.Completed -> {
                    _uiState.value = SdeUpdateUiState.UpToDate
                }
                is SdeUpdateSheetContent.DownloadFailed -> {
                    _uiState.value = SdeUpdateUiState.UpdateReady(
                        installedBuild = content.installedBuild,
                        remoteBuild = content.remoteBuild,
                        installedIconVersion = content.installedIconVersion,
                        remoteIconVersion = content.remoteIconVersion,
                        versionVisibility = content.versionVisibility,
                    )
                }
                is SdeUpdateSheetContent.Checking -> {
                    invalidateCheckWork()
                    _uiState.value = SdeUpdateUiState.CheckFailed(
                        installedBuild = content.installedBuild,
                        installedIconVersion = content.installedIconVersion,
                        message = "",
                    )
                }
                is SdeUpdateSheetContent.CheckFailed -> {
                    _uiState.value = SdeUpdateUiState.CheckFailed(
                        installedBuild = content.installedBuild,
                        installedIconVersion = content.installedIconVersion,
                        message = content.message,
                    )
                }
                is SdeUpdateSheetContent.Info -> {
                    _uiState.value = SdeUpdateUiState.UpToDate
                }
            }
            else -> Unit
        }
    }

    fun startDownload() {
        val remote = pendingUpdate ?: return
        if (downloadJob?.isActive == true) return

        val installedBuild = repository.installedBuildNumber().orEmpty()
        val remoteBuild = remote.meta.buildNumber
        val installedIconVersion = repository.installedIconVersion()
        val remoteIconVersion = remote.meta.iconVersion
        val versionVisibility = repository.resolveVersionVisibility(remote)

        downloadJob?.cancel()
        val generation = ++downloadGeneration
        val job = scope.launch {
            setDownloadUiState(generation) {
                updateSheet(
                    SdeUpdateSheetContent.Downloading(
                        installedBuild = installedBuild,
                        remoteBuild = remoteBuild,
                        installedIconVersion = installedIconVersion,
                        remoteIconVersion = remoteIconVersion,
                        versionVisibility = versionVisibility,
                        progressPercent = 0,
                    ),
                )
            }
            try {
                repository.downloadAndApply(remote) { progress ->
                    setDownloadUiState(generation) {
                        updateSheet(
                            SdeUpdateSheetContent.Downloading(
                                installedBuild = installedBuild,
                                remoteBuild = remoteBuild,
                                installedIconVersion = installedIconVersion,
                                remoteIconVersion = remoteIconVersion,
                                versionVisibility = versionVisibility,
                                progressPercent = progress,
                            ),
                        )
                    }
                }.getOrThrow()
                if (!isDownloadGenerationActive(generation)) return@launch
                pendingUpdate = null
                val newBuild = repository.installedBuildNumber().orEmpty()
                val newIconVersion = repository.installedIconVersion()
                setDownloadUiState(generation) {
                    updateSheet(
                        SdeUpdateSheetContent.Completed(
                            previousBuild = installedBuild,
                            newBuild = newBuild,
                            previousIconVersion = installedIconVersion,
                            newIconVersion = newIconVersion,
                            versionVisibility = versionVisibility,
                        ),
                    )
                }
            } catch (_: CancellationException) {
                if (!isDownloadGenerationActive(generation)) return@launch
                pendingUpdate = remote
                setDownloadUiState(generation) {
                    updateSheet(
                        SdeUpdateSheetContent.DownloadFailed(
                            installedBuild = installedBuild,
                            remoteBuild = remoteBuild,
                            installedIconVersion = installedIconVersion,
                            remoteIconVersion = remoteIconVersion,
                            versionVisibility = versionVisibility,
                            message = "",
                            cancelled = true,
                        ),
                    )
                }
            } catch (error: Exception) {
                if (!isDownloadGenerationActive(generation)) return@launch
                pendingUpdate = remote
                setDownloadUiState(generation) {
                    updateSheet(
                        SdeUpdateSheetContent.DownloadFailed(
                            installedBuild = installedBuild,
                            remoteBuild = remoteBuild,
                            installedIconVersion = installedIconVersion,
                            remoteIconVersion = remoteIconVersion,
                            versionVisibility = versionVisibility,
                            message = error.message.orEmpty(),
                            cancelled = false,
                        ),
                    )
                }
            }
        }
        downloadJob = job
        job.invokeOnCompletion {
            if (downloadJob === job) {
                downloadJob = null
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
    }

    fun retryDownload() {
        dismissSheetFromFailure()
        startDownload()
    }

    fun retryCheck() {
        checkForUpdates(inSheet = true)
    }

    /**
     * Main-page pull-to-refresh: re-run a background update check when idle,
     * already up-to-date, or the last check failed. Clears to Idle first so
     * [checkForUpdates] can start; MainPage keeps the PTR spinner via
     * [isUpdateCheckInFlight] until the result arrives.
     * Leaves UpdateReady / sheet / in-progress download alone.
     */
    fun refreshUpdateCheck() {
        logAction("refreshUpdateCheck", describeJobs())
        if (downloadJob?.isActive == true) {
            logAction("refreshUpdateCheck", "skipped: download active")
            return
        }
        if (checkJob?.isActive == true) {
            logAction("refreshUpdateCheck", "skipped: check already active")
            return
        }
        when (val state = _uiState.value) {
            is SdeUpdateUiState.Idle,
            is SdeUpdateUiState.UpToDate,
            is SdeUpdateUiState.CheckFailed -> {
                logAction(
                    "refreshUpdateCheck",
                    "resetting ${describeUiState(state)} -> Idle then checkForUpdates",
                )
                _uiState.value = SdeUpdateUiState.Idle
                _isUpdateCheckInFlight.value = true
                checkForUpdates(inSheet = false)
            }
            else -> {
                logAction(
                    "refreshUpdateCheck",
                    "skipped: uiState=${describeUiState(state)}",
                )
            }
        }
    }

    private fun presentSheet(content: SdeUpdateSheetContent): SdeUpdateUiState.Sheet {
        sheetInstanceId++
        return SdeUpdateUiState.Sheet(content = content, instanceId = sheetInstanceId)
    }

    private fun updateSheet(content: SdeUpdateSheetContent): SdeUpdateUiState.Sheet {
        val instanceId = (_uiState.value as? SdeUpdateUiState.Sheet)?.instanceId
            ?: return presentSheet(content)
        return SdeUpdateUiState.Sheet(content = content, instanceId = instanceId)
    }

    private fun invalidateCheckWork() {
        checkGeneration++
        checkJob?.cancel()
        checkJob = null
        _isUpdateCheckInFlight.value = false
    }

    fun repairStaleCheckingState() {
        if (_uiState.value is SdeUpdateUiState.Checking && checkJob?.isActive != true) {
            logAction("repairStaleCheckingState", "resetting orphan Checking -> Idle")
            _uiState.value = SdeUpdateUiState.Idle
        }
    }

    private fun isCheckGenerationActive(generation: Int): Boolean {
        return generation == checkGeneration
    }

    private fun isDownloadGenerationActive(generation: Int): Boolean {
        return generation == downloadGeneration
    }

    private fun setCheckUiState(generation: Int, state: () -> SdeUpdateUiState) {
        if (isCheckGenerationActive(generation)) {
            _uiState.value = state()
        }
    }

    private fun setDownloadUiState(generation: Int, state: () -> SdeUpdateUiState) {
        if (isDownloadGenerationActive(generation)) {
            _uiState.value = state()
        }
    }

    private fun dismissSheetFromFailure() {
        val state = _uiState.value as? SdeUpdateUiState.Sheet ?: return
        val content = state.content as? SdeUpdateSheetContent.DownloadFailed ?: return
        _uiState.value = SdeUpdateUiState.UpdateReady(
            installedBuild = content.installedBuild,
            remoteBuild = content.remoteBuild,
            installedIconVersion = content.installedIconVersion,
            remoteIconVersion = content.remoteIconVersion,
            versionVisibility = content.versionVisibility,
        )
    }

    private fun resolveInstalledSnapshotForSheetCheck(inSheet: Boolean): InstalledVersionSnapshot {
        if (!inSheet) {
            return InstalledVersionSnapshot(
                build = repository.installedBuildNumber(),
                iconVersion = repository.installedIconVersion(),
            )
        }
        return when (val state = _uiState.value) {
            is SdeUpdateUiState.Sheet -> when (val content = state.content) {
                is SdeUpdateSheetContent.CheckFailed -> InstalledVersionSnapshot(
                    build = content.installedBuild,
                    iconVersion = content.installedIconVersion,
                )
                is SdeUpdateSheetContent.Checking -> InstalledVersionSnapshot(
                    build = content.installedBuild,
                    iconVersion = content.installedIconVersion,
                )
                else -> InstalledVersionSnapshot(
                    build = repository.installedBuildNumber(),
                    iconVersion = repository.installedIconVersion(),
                )
            }
            is SdeUpdateUiState.CheckFailed -> InstalledVersionSnapshot(
                build = state.installedBuild,
                iconVersion = state.installedIconVersion,
            )
            else -> InstalledVersionSnapshot(
                build = repository.installedBuildNumber(),
                iconVersion = repository.installedIconVersion(),
            )
        }
    }

    private data class InstalledVersionSnapshot(
        val build: String?,
        val iconVersion: Int?,
    )

    private fun describeJobs(): String {
        return buildString {
            append("checkGen=$checkGeneration")
            append(" checkActive=${checkJob?.isActive == true}")
            append(" downloadGen=$downloadGeneration")
            append(" downloadActive=${downloadJob?.isActive == true}")
            append(" pendingBuild=${pendingUpdate?.meta?.buildNumber}")
        }
    }

    private fun describeUiState(state: SdeUpdateUiState): String {
        return when (state) {
            is SdeUpdateUiState.Idle -> "Idle"
            is SdeUpdateUiState.Checking -> "Checking"
            is SdeUpdateUiState.UpToDate -> "UpToDate"
            is SdeUpdateUiState.UpdateReady -> {
                "UpdateReady(build=${state.installedBuild}->${state.remoteBuild}, " +
                    "icons=${state.installedIconVersion}->${state.remoteIconVersion}, " +
                    "showDb=${state.versionVisibility.showDatabase} showIcons=${state.versionVisibility.showIcons})"
            }
            is SdeUpdateUiState.CheckFailed -> {
                "CheckFailed(build=${state.installedBuild}, icons=${state.installedIconVersion}, " +
                    "message=${state.message})"
            }
            is SdeUpdateUiState.Sheet -> {
                "Sheet#${state.instanceId}(${describeSheetContent(state.content)})"
            }
        }
    }

    private fun describeSheetContent(content: SdeUpdateSheetContent): String {
        return when (content) {
            is SdeUpdateSheetContent.Checking -> "Checking"
            is SdeUpdateSheetContent.CheckFailed -> "CheckFailed"
            is SdeUpdateSheetContent.Prompt -> "Prompt"
            is SdeUpdateSheetContent.Downloading -> "Downloading(${content.progressPercent}%)"
            is SdeUpdateSheetContent.Completed -> "Completed"
            is SdeUpdateSheetContent.DownloadFailed -> {
                if (content.cancelled) "DownloadCancelled" else "DownloadFailed"
            }
            is SdeUpdateSheetContent.Info -> "Info"
        }
    }

    private fun logAction(action: String, message: String, error: Throwable? = null) {
        if (error == null) {
            SdeUpdateLog.d(action, message)
        } else {
            SdeUpdateLog.w(action, message, error)
        }
    }
}
