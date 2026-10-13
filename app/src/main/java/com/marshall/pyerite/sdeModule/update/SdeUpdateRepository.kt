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

            val steps = buildList {
                if (needsDatabase) {
                    val dbZhUrl = remote.urlFor(SdeDatabase.ZH_FILE_NAME)
                        ?: error("Missing download URL for ${SdeDatabase.ZH_FILE_NAME}")
                    val dbEnUrl = remote.urlFor(SdeDatabase.EN_FILE_NAME)
                        ?: error("Missing download URL for ${SdeDatabase.EN_FILE_NAME}")
                    add(dbZhUrl to File(downloadDir, SdeDatabase.ZH_FILE_NAME))
                    add(dbEnUrl to File(downloadDir, SdeDatabase.EN_FILE_NAME))
                }
                if (needsIcons) {
                    val iconsUrl = remote.urlFor("icons.zip")
                        ?: error("Missing download URL for icons.zip")
                    add(iconsUrl to File(downloadDir, "icons.zip"))
                }
            }

            val perStep = 100f / steps.size
            steps.forEachIndexed { index, (url, dest) ->
                SdeUpdateLog.d("Repository", "downloading ${dest.name} (${index + 1}/${steps.size})")
                remoteDataSource.downloadToFile(url, dest) { stepProgress ->
                    val total = ((index * perStep) + (stepProgress * perStep)).roundToInt().coerceIn(0, 100)
                    onProgress(total)
                }
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
                val iconsFile = File(downloadDir, "icons.zip")
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
    }
}
