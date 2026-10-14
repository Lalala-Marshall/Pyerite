package com.marshall.pyerite.sdeModule.update

import android.content.Context
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
        onProgress: (Int) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        SdeUpdateLog.d(
            "Repository",
            "downloadAndApply build=${remote.meta.buildNumber} icons=${remote.meta.iconVersion}",
        )
        runCatching {
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
                onProgress(100)
                return@runCatching
            }

            val downloadDir = File(context.cacheDir, "sde_download").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }

            val tag = remote.tagName.ifBlank {
                SdeRemoteConfig.releaseTag(remote.meta.buildNumber)
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
                        ),
                    )
                }
            }

            val perStep = 100f / steps.size
            steps.forEachIndexed { index, step ->
                SdeUpdateLog.d("Repository", "downloading ${step.installed.name} (${index + 1}/${steps.size})")
                remoteDataSource.downloadFirstAvailable(step.urls, step.downloaded) { stepProgress ->
                    val total = ((index * perStep) + (stepProgress * perStep)).roundToInt().coerceIn(0, 100)
                    onProgress(total)
                }
                if (step.downloaded != step.installed) {
                    SdeAssetFiles.materializeSqlite(step.downloaded, step.installed)
                    step.downloaded.delete()
                }
                requireSha256(step.installed, step.expectedSha256, step.installed.name)
            }

            if (needsDatabase) {
                val dbZhFile = File(downloadDir, SdeDatabase.ZH_FILE_NAME)
                val dbEnFile = File(downloadDir, SdeDatabase.EN_FILE_NAME)
                roomProvider.closeAndInvalidate()
                SdeAssetFiles.deleteAllDatabases(context)
                SdeAssetFiles.installDatabaseFromFile(context, SdeDatabase.ZH_FILE_NAME, dbZhFile)
                SdeAssetFiles.installDatabaseFromFile(context, SdeDatabase.EN_FILE_NAME, dbEnFile)
            }
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
            versionStore.save(remote.meta)
            _contentRefreshed.tryEmit(Unit)
            onProgress(100)
            downloadDir.deleteRecursively()
            SdeUpdateLog.d("Repository", "downloadAndApply completed")
        }.onFailure { error ->
            SdeUpdateLog.w("Repository", "downloadAndApply failed", error)
        }
    }

    companion object {
        private const val CHECK_MAX_ATTEMPTS = 3
        private const val CHECK_RETRY_DELAY_MS = 2_000L
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
    )
}
