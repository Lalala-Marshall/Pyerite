package com.marshall.pyerite.sdeModule.update

import android.content.Context
import com.marshall.pyerite.localization.SdeDatabase
import com.marshall.pyerite.sdeModule.room.RoomProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Replaces the live SDE files only after a synced copy exists, and records that
 * swap in a marker so the next launch can finish or undo it. The previous
 * database stays in place until that marker says the new files are complete.
 */
internal object SdeInstallJournal {

    fun discardArtifacts(context: Context) {
        val zh = paths(context, SdeDatabase.ZH_FILE_NAME)
        val en = paths(context, SdeDatabase.EN_FILE_NAME)
        listOf(zh.staging, zh.backup, en.staging, en.backup).forEach { file ->
            if (file.exists()) file.delete()
        }
        cleanupFinishedInstall(zh, en)
        val iconStaging = File(context.filesDir, SdeAssetFiles.ICONS_DIR_NAME + SdeAssetFiles.STAGING_SUFFIX)
        val iconBackup = File(context.filesDir, SdeAssetFiles.ICONS_DIR_NAME + SdeAssetFiles.BACKUP_SUFFIX)
        if (iconStaging.exists()) iconStaging.deleteRecursively()
        if (iconBackup.exists()) iconBackup.deleteRecursively()
    }

    fun recoverInterruptedInstall(context: Context, versionStore: SdeVersionStore) {
        try {
            SdeAssetFiles.recoverIncompleteIcons(context)
            val zh = paths(context, SdeDatabase.ZH_FILE_NAME)
            val en = paths(context, SdeDatabase.EN_FILE_NAME)
            deleteTempMarkers(zh.live.parentFile)
            val swapped = readMarker(markerFile(zh.live.parentFile, MARKER_SWAPPED))
            if (swapped != null && swapped.phase == PHASE_SWAPPED && sizesMatch(zh, en, swapped)) {
                versionStore.save(swapped.meta)
                cleanupFinishedInstall(zh, en)
                SdeUpdateLog.d("InstallJournal", "finished interrupted install ${swapped.meta.buildNumber}")
                return
            }
            if (markerFile(zh.live.parentFile, MARKER_STAGED).exists() || swapped != null) {
                SdeUpdateLog.w("InstallJournal", "rolling back an unfinished database install")
                restore(zh)
                restore(en)
            } else {
                restoreIfMissing(zh)
                restoreIfMissing(en)
            }
            cleanupFinishedInstall(zh, en)
        } catch (error: Exception) {
            SdeUpdateLog.w("InstallJournal", "recovery failed", error)
        }
    }

    fun installDatabases(
        context: Context,
        roomProvider: RoomProvider,
        versionStore: SdeVersionStore,
        zhSource: File,
        enSource: File,
        meta: SdeReleaseMeta,
    ) {
        val zh = paths(context, SdeDatabase.ZH_FILE_NAME)
        val en = paths(context, SdeDatabase.EN_FILE_NAME)
        SdeAssetFiles.copySynced(zhSource, zh.staging)
        SdeAssetFiles.copySynced(enSource, en.staging)
        val parent = zh.live.parentFile ?: error("missing database directory")
        writeMarker(
            markerFile(parent, MARKER_STAGED),
            PHASE_STAGED,
            meta,
            zh.staging.length(),
            en.staging.length(),
        )
        try {
            roomProvider.replaceOpenDatabases {
                try {
                    promote(zh)
                    promote(en)
                    writeMarker(
                        markerFile(parent, MARKER_SWAPPED),
                        PHASE_SWAPPED,
                        meta,
                        zh.live.length(),
                        en.live.length(),
                    )
                } catch (error: Exception) {
                    restore(zh)
                    restore(en)
                    markerFile(parent, MARKER_SWAPPED).delete()
                    throw error
                }
            }
        } catch (error: Exception) {
            zh.staging.delete()
            en.staging.delete()
            markerFile(parent, MARKER_STAGED).delete()
            throw error
        }
        versionStore.save(meta)
        cleanupFinishedInstall(zh, en)
        SdeUpdateLog.d("InstallJournal", "installed build=${meta.buildNumber}")
    }

    private fun cleanupFinishedInstall(zh: DatabasePaths, en: DatabasePaths) {
        val parent = zh.live.parentFile
        markerFile(parent, MARKER_STAGED).delete()
        markerFile(parent, MARKER_SWAPPED).delete()
        deleteTempMarkers(parent)
        zh.staging.delete()
        en.staging.delete()
        if (zh.live.exists()) zh.backup.delete()
        if (en.live.exists()) en.backup.delete()
    }

    private fun sizesMatch(zh: DatabasePaths, en: DatabasePaths, marker: InstallMarker): Boolean {
        return zh.live.exists() &&
            en.live.exists() &&
            zh.live.length() == marker.zhBytes &&
            en.live.length() == marker.enBytes &&
            marker.zhBytes > 0L &&
            marker.enBytes > 0L
    }

    private fun promote(paths: DatabasePaths) {
        SdeAssetFiles.deleteSidecars(paths.live)
        if (paths.backup.exists()) paths.backup.delete()
        if (paths.live.exists() && !paths.live.renameTo(paths.backup)) {
            error("failed to move ${paths.live.name} aside")
        }
        if (!paths.staging.renameTo(paths.live)) {
            if (paths.backup.exists()) paths.backup.renameTo(paths.live)
            error("failed to install ${paths.live.name}")
        }
    }

    private fun restore(paths: DatabasePaths) {
        if (!paths.backup.exists()) return
        SdeAssetFiles.deleteSidecars(paths.live)
        if (paths.live.exists()) paths.live.delete()
        if (!paths.backup.renameTo(paths.live)) {
            SdeUpdateLog.w("InstallJournal", "failed to restore ${paths.live.name}")
        }
    }

    private fun restoreIfMissing(paths: DatabasePaths) {
        if (paths.live.exists() || !paths.backup.exists()) return
        restore(paths)
    }

    private fun paths(context: Context, dbName: String): DatabasePaths {
        val live = context.getDatabasePath(dbName)
        val parent = live.parentFile ?: error("missing database directory")
        parent.mkdirs()
        return DatabasePaths(
            live = live,
            staging = File(parent, dbName + SdeAssetFiles.STAGING_SUFFIX),
            backup = File(parent, dbName + SdeAssetFiles.BACKUP_SUFFIX),
        )
    }

    private fun markerFile(parent: File?, name: String): File {
        return File(parent, name)
    }

    private fun deleteTempMarkers(parent: File?) {
        if (parent == null) return
        File(parent, "$MARKER_STAGED$TEMP_SUFFIX").delete()
        File(parent, "$MARKER_SWAPPED$TEMP_SUFFIX").delete()
    }

    private fun writeMarker(
        file: File,
        phase: String,
        meta: SdeReleaseMeta,
        zhBytes: Long,
        enBytes: Long,
    ) {
        val parent = file.parentFile ?: error("missing database directory")
        parent.mkdirs()
        val json = JSONObject()
            .put(KEY_PHASE, phase)
            .put(KEY_BUILD_NUMBER, meta.buildNumber)
            .put(KEY_RELEASE_DATE, meta.releaseDate)
            .put(KEY_COMPLETION_TIME, meta.completionTime)
            .put(KEY_ZH_BYTES, zhBytes)
            .put(KEY_EN_BYTES, enBytes)
        if (meta.iconVersion != null) json.put(KEY_ICON_VERSION, meta.iconVersion)
        if (meta.iconSha256 != null) json.put(KEY_ICON_SHA256, meta.iconSha256)
        if (meta.sdeSha256 != null) json.put(KEY_SDE_SHA256, meta.sdeSha256)
        val tmp = File(parent, file.name + TEMP_SUFFIX)
        FileOutputStream(tmp).use { output ->
            output.write(json.toString().toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        if (file.exists()) file.delete()
        check(tmp.renameTo(file)) { "failed to write ${file.name}" }
    }

    private fun readMarker(file: File): InstallMarker? {
        if (!file.exists()) return null
        return try {
            val json = JSONObject(file.readText())
            val iconVersion = if (json.has(KEY_ICON_VERSION)) json.getInt(KEY_ICON_VERSION) else null
            InstallMarker(
                phase = json.getString(KEY_PHASE),
                meta = SdeReleaseMeta(
                    buildNumber = json.getString(KEY_BUILD_NUMBER),
                    releaseDate = json.optString(KEY_RELEASE_DATE),
                    completionTime = json.optString(KEY_COMPLETION_TIME),
                    iconVersion = iconVersion,
                    iconSha256 = json.optString(KEY_ICON_SHA256).ifBlank { null },
                    sdeSha256 = json.optString(KEY_SDE_SHA256).ifBlank { null },
                ),
                zhBytes = json.getLong(KEY_ZH_BYTES),
                enBytes = json.getLong(KEY_EN_BYTES),
            )
        } catch (error: Exception) {
            SdeUpdateLog.w("InstallJournal", "unreadable marker ${file.name}", error)
            null
        }
    }

    private data class DatabasePaths(
        val live: File,
        val staging: File,
        val backup: File,
    )

    private data class InstallMarker(
        val phase: String,
        val meta: SdeReleaseMeta,
        val zhBytes: Long,
        val enBytes: Long,
    )

    private const val MARKER_STAGED = "sde-install.staged"
    private const val MARKER_SWAPPED = "sde-install.swapped"
    private const val TEMP_SUFFIX = ".tmp"
    private const val PHASE_STAGED = "staged"
    private const val PHASE_SWAPPED = "swapped"
    private const val KEY_PHASE = "phase"
    private const val KEY_BUILD_NUMBER = "buildNumber"
    private const val KEY_RELEASE_DATE = "releaseDate"
    private const val KEY_COMPLETION_TIME = "completionTime"
    private const val KEY_ICON_VERSION = "iconVersion"
    private const val KEY_ICON_SHA256 = "iconSha256"
    private const val KEY_SDE_SHA256 = "sdeSha256"
    private const val KEY_ZH_BYTES = "zhBytes"
    private const val KEY_EN_BYTES = "enBytes"
}
