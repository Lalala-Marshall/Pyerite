package com.marshall.pyerite.sdeModule.update

import android.content.Context
import android.os.PowerManager
import com.marshall.pyerite.sdeModule.room.RoomProvider
import com.marshall.pyerite.iconModule.manager.IconManager
import com.marshall.pyerite.sdeModule.network.SdeRemoteDataSource
import com.marshall.pyerite.localization.SdeDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class SdeUpdateRepository(
    private val context: Context,
    private val remoteDataSource: SdeRemoteDataSource,
    private val versionStore: SdeVersionStore,
    private val roomProvider: RoomProvider,
    private val iconManager: IconManager,
) {

    private val _contentRefreshed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val contentRefreshed: SharedFlow<Unit> = _contentRefreshed.asSharedFlow()

    fun installedBuildNumber(): String? = versionStore.load()?.buildNumber

    internal fun installedRelease(): SdeReleaseMeta? = versionStore.load()

    fun installedIconVersion(): Int? = versionStore.load()?.iconVersion

    /** Debug only: drop a downloaded SDE and copy the databases shipped in the app. */
    internal suspend fun reinstallBundledAssets(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            SdeInstallJournal.discardArtifacts(context)
            val bundled = SdeReleaseMeta.fromAssets(context) ?: error("bundled SDE metadata missing")
            roomProvider.replaceOpenDatabases {
                SdeAssetFiles.deleteAllDatabases(context)
                SdeAssetFiles.copyDatabaseFromAssets(context, SdeDatabase.ZH_FILE_NAME)
                SdeAssetFiles.copyDatabaseFromAssets(context, SdeDatabase.EN_FILE_NAME)
            }
            SdeAssetFiles.extractIconsFromAssets(context)
            iconManager.reload()
            versionStore.save(bundled)
            _contentRefreshed.tryEmit(Unit)
            SdeUpdateLog.d("Repository", "reinstalled bundled build=${bundled.buildNumber}")
        }
    }

    fun resolveVersionVisibility(remote: SdeRemotePackage): SdeUpdateVersionVisibility {
        val installed = versionStore.load()
        return SdeUpdateVersionVisibility(
            showDatabase = SdeVersionComparator.needsDatabaseUpgrade(remote.meta, installed),
            showIcons = SdeVersionComparator.needsIconsUpgrade(remote.meta, installed),
        )
    }

    suspend fun checkForRemoteUpdate(): SdeRemotePackage? = withContext(Dispatchers.IO) {
        val installed = versionStore.load()
        SdeUpdateLog.d(
            "Repository",
            "checkForRemoteUpdate installed=${installed?.buildNumber} icons=${installed?.iconVersion}",
        )

        var lastError: Exception? = null
        repeat(CHECK_MAX_ATTEMPTS) { attemptIndex ->
            val attempt = attemptIndex + 1
            try {
                SdeUpdateLog.d("Repository", "check attempt $attempt/$CHECK_MAX_ATTEMPTS")
                val remote = remoteDataSource.fetchLatestPackage()
                val needsDatabase = SdeVersionComparator.needsDatabaseUpgrade(remote.meta, installed)
                val needsIcons = SdeVersionComparator.needsIconsUpgrade(remote.meta, installed)
                val needsUpgrade = needsDatabase || needsIcons
                val iconsVersionAligned = SdeVersionComparator.iconsVersionAligned(remote.meta, installed)
                val iconsShaAligned = SdeVersionComparator.iconsSha256Aligned(remote.meta, installed)
                SdeUpdateLog.d(
                    "Repository",
                    "remote=${remote.meta.buildNumber} icons=${remote.meta.iconVersion} " +
                        "sha=${remote.meta.iconSha256} needsDatabase=$needsDatabase needsIcons=$needsIcons " +
                        "iconsVersionAligned=$iconsVersionAligned iconsShaAligned=$iconsShaAligned " +
                        "needsUpgrade=$needsUpgrade",
                )
                return@withContext if (needsUpgrade) remote else null
            } catch (error: CancellationException) {
                if (error is TimeoutCancellationException) {
                    lastError = IOException("Update check timed out", error)
                    if (attempt < CHECK_MAX_ATTEMPTS) {
                        SdeUpdateLog.d(
                            "Repository",
                            "attempt $attempt timed out, retrying in ${CHECK_RETRY_DELAY_MS}ms",
                        )
                        delay(CHECK_RETRY_DELAY_MS.milliseconds)
                    }
                } else {
                    throw error
                }
            } catch (error: Exception) {
                lastError = error
                if (attempt < CHECK_MAX_ATTEMPTS) {
                    SdeUpdateLog.w(
                        "Repository",
                        "attempt $attempt failed, retrying in ${CHECK_RETRY_DELAY_MS}ms",
                        error,
                    )
                    delay(CHECK_RETRY_DELAY_MS.milliseconds)
                }
            }
        }

        SdeUpdateLog.w(
            "Repository",
            "check failed after $CHECK_MAX_ATTEMPTS attempts",
            lastError,
        )
        throw lastError ?: IOException("Unable to check for remote update")
    }

    suspend fun downloadAndApply(
        remote: SdeRemotePackage,
        onProgress: (percent: Int, applying: Boolean) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        SdeUpdateLog.d(
            "Repository",
            "downloadAndApply build=${remote.meta.buildNumber} icons=${remote.meta.iconVersion}",
        )
        runCatching {
            val wakeLock = acquireInstallWakeLock()
            try {
                downloadAndApplyHoldingWakeLock(remote, onProgress)
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
            }
        }.onFailure { error ->
            SdeUpdateLog.w("Repository", "downloadAndApply failed", error)
        }
    }

    private suspend fun downloadAndApplyHoldingWakeLock(
        remote: SdeRemotePackage,
        onProgress: (percent: Int, applying: Boolean) -> Unit,
    ) {
            SdeInstallJournal.recoverInterruptedInstall(context, versionStore)
            val installed = versionStore.load()
            val needsDatabase = SdeVersionComparator.needsDatabaseUpgrade(remote.meta, installed)
            val needsIcons = SdeVersionComparator.needsIconsUpgrade(remote.meta, installed)
            SdeUpdateLog.d(
                "Repository",
                "download plan database=$needsDatabase icons=$needsIcons " +
                    "installedIcons=${installed?.iconVersion}",
            )
            if (!needsDatabase && !needsIcons) {
                SdeUpdateLog.d("Repository", "nothing to apply, skipping")
                onProgress(COMPLETE_PROGRESS_PERCENT, false)
                return
            }

            val downloadDir = File(context.cacheDir, "sde_download").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }

            val tag = remote.tagName.ifBlank {
                SdeRemoteConfig.releaseTag(remote.meta.buildNumber)
            }
            val iconPartCount = if (needsIcons) {
                readRemoteIconPartCount(tag, downloadDir)
            } else {
                0
            }
            val steps = buildList {
                if (needsDatabase) {
                    add(
                        DownloadStep(
                            urls = SdeRemoteConfig.databaseDownloadUrls(
                                tag,
                                SdeRemoteConfig.ASSET_DB_ZH_GZ,
                                SdeRemoteConfig.ASSET_DB_ZH,
                            ),
                            downloaded = File(downloadDir, "${SdeRemoteConfig.ASSET_DB_ZH}.download"),
                            installed = File(downloadDir, SdeDatabase.ZH_FILE_NAME),
                            expectedSha256 = remote.meta.dbZhSha256,
                        ),
                    )
                    add(
                        DownloadStep(
                            urls = SdeRemoteConfig.databaseDownloadUrls(
                                tag,
                                SdeRemoteConfig.ASSET_DB_EN_GZ,
                                SdeRemoteConfig.ASSET_DB_EN,
                            ),
                            downloaded = File(downloadDir, "${SdeRemoteConfig.ASSET_DB_EN}.download"),
                            installed = File(downloadDir, SdeDatabase.EN_FILE_NAME),
                            expectedSha256 = remote.meta.dbEnSha256,
                        ),
                    )
                }
                if (needsIcons) {
                    val iconsFile = File(downloadDir, SdeRemoteConfig.ASSET_ICONS)
                    add(
                        DownloadStep(
                            urls = SdeRemoteConfig.downloadUrls(tag, SdeRemoteConfig.ASSET_ICONS),
                            downloaded = iconsFile,
                            installed = iconsFile,
                            expectedSha256 = remote.meta.iconSha256,
                            iconPartCount = iconPartCount,
                        ),
                    )
                }
            }

            val perStep = 100f / steps.size
            steps.forEachIndexed { index, step ->
                SdeUpdateLog.d("Repository", "downloading ${step.installed.name} (${index + 1}/${steps.size})")
                val report = { stepProgress: Float ->
                    val total = ((index * perStep) + (stepProgress * perStep)).roundToInt()
                        .coerceIn(0, COMPLETE_PROGRESS_PERCENT)
                    onProgress(scaleDownloadProgress(total), false)
                }
                if (step.iconPartCount >= SdeRemoteConfig.MIN_ICON_PARTS) {
                    try {
                        downloadIconParts(tag, step.iconPartCount, downloadDir, step.installed, report)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        SdeUpdateLog.w(
                            "Repository",
                            "icon parts failed, downloading ${SdeRemoteConfig.ASSET_ICONS}",
                            error,
                        )
                        if (step.installed.exists()) step.installed.delete()
                        remoteDataSource.downloadFirstAvailable(step.urls, step.downloaded, report)
                    }
                } else {
                    remoteDataSource.downloadFirstAvailable(step.urls, step.downloaded, report)
                }
                if (step.downloaded != step.installed) {
                    SdeAssetFiles.materializeSqlite(step.downloaded, step.installed)
                    step.downloaded.delete()
                }
                requireSha256(step.installed, step.expectedSha256, step.installed.name)
            }

            onProgress(APPLY_PROGRESS_PERCENT, true)
            if (needsIcons) {
                val iconsFile = File(downloadDir, SdeRemoteConfig.ASSET_ICONS)
                SdeAssetFiles.extractIconsFromFile(context, iconsFile)
                iconManager.reload()
            } else {
                SdeUpdateLog.d(
                    "Repository",
                    "icons version and icon_sha256 aligned, skipping icons download " +
                        "(version=${installed?.iconVersion} sha=${installed?.iconSha256})",
                )
            }
            if (needsDatabase) {
                val dbZhFile = File(downloadDir, SdeDatabase.ZH_FILE_NAME)
                val dbEnFile = File(downloadDir, SdeDatabase.EN_FILE_NAME)
                SdeInstallJournal.installDatabases(
                    context = context,
                    roomProvider = roomProvider,
                    versionStore = versionStore,
                    zhSource = dbZhFile,
                    enSource = dbEnFile,
                    meta = remote.meta,
                )
            } else {
                versionStore.save(remote.meta)
            }
            _contentRefreshed.tryEmit(Unit)
            downloadDir.deleteRecursively()
            SdeUpdateLog.d("Repository", "downloadAndApply completed")
    }

    private fun acquireInstallWakeLock(): PowerManager.WakeLock {
        val powerManager = requireNotNull(context.getSystemService(PowerManager::class.java))
        return powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            SdeRemoteConfig.INSTALL_WAKE_LOCK_TAG,
        ).also { wakeLock ->
            wakeLock.setReferenceCounted(false)
            wakeLock.acquire(
                TimeUnit.HOURS.toMillis(SdeRemoteConfig.INSTALL_WAKE_LOCK_TIMEOUT_HOURS),
            )
        }
    }

    private suspend fun readRemoteIconPartCount(tag: String, downloadDir: File): Int {
        val manifest = File(downloadDir, SdeRemoteConfig.ASSET_ICON_PARTS)
        return try {
            remoteDataSource.downloadFirstAvailable(
                SdeRemoteConfig.giteeDownloadUrls(tag, SdeRemoteConfig.ASSET_ICON_PARTS),
                manifest,
            ) { _ -> }
            val count = manifest.readText().trim().toInt()
            if (count in SdeRemoteConfig.MIN_ICON_PARTS..SdeRemoteConfig.MAX_ICON_PARTS) count else 0
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            SdeUpdateLog.d("Repository", "icon parts manifest unavailable, using ${SdeRemoteConfig.ASSET_ICONS}")
            0
        }
    }

    private suspend fun downloadIconParts(
        tag: String,
        partCount: Int,
        downloadDir: File,
        assembled: File,
        onProgress: (Float) -> Unit,
    ) {
        if (assembled.exists()) assembled.delete()
        assembled.outputStream().use { output ->
            for (index in 0 until partCount) {
                val partName = SdeRemoteConfig.iconPartName(index)
                val part = File(downloadDir, partName)
                remoteDataSource.downloadFirstAvailable(
                    SdeRemoteConfig.giteeDownloadUrls(tag, partName),
                    part,
                ) { partProgress ->
                    onProgress(((index + partProgress) / partCount.toFloat()).coerceIn(0f, 1f))
                }
                part.inputStream().use { input -> input.copyTo(output) }
                part.delete()
            }
        }
    }

    companion object {
        private const val CHECK_MAX_ATTEMPTS = 3
        private const val CHECK_RETRY_DELAY_MS = 2_000L
        private const val DOWNLOAD_PROGRESS_CEILING = 90
        private const val APPLY_PROGRESS_PERCENT = 95
        private const val COMPLETE_PROGRESS_PERCENT = 100

        private fun scaleDownloadProgress(percent: Int): Int {
            return (percent.toLong() * DOWNLOAD_PROGRESS_CEILING / COMPLETE_PROGRESS_PERCENT)
                .toInt()
                .coerceIn(0, DOWNLOAD_PROGRESS_CEILING)
        }
        private const val SHA_256 = "SHA-256"
        private const val HEX_RADIX = 16
        private const val HEX_DIGITS = 2
        private const val BYTE_MASK = 0xff

        private fun requireSha256(file: File, expected: String?, label: String) {
            val want = expected?.trim()?.takeIf { it.isNotEmpty() } ?: return
            val digest = MessageDigest.getInstance(SHA_256)
            file.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val got = digest.digest().joinToString(separator = "") { byte ->
                (byte.toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(HEX_DIGITS, '0')
            }
            if (!got.equals(want, ignoreCase = true)) {
                error("$label sha256 mismatch")
            }
        }
    }

    private data class DownloadStep(
        val urls: List<String>,
        val downloaded: File,
        val installed: File,
        val expectedSha256: String?,
        val iconPartCount: Int = 0,
    )
}
