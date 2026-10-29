package com.marshall.pyerite.sdeModule.update

import android.content.Context
import com.marshall.pyerite.localization.SdeDatabase
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

object SdeAssetFiles {

    const val ICONS_DIR_NAME = "icons"
    internal const val STAGING_SUFFIX = ".staging"
    internal const val BACKUP_SUFFIX = ".bak"
    private const val GZIP_MAGIC_FIRST = 0x1f
    private const val GZIP_MAGIC_SECOND = 0x8b
    private const val WAL_SUFFIX = "-wal"
    private const val SHM_SUFFIX = "-shm"

    fun deleteDatabase(context: Context, dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) dbFile.delete()
        deleteSidecars(dbFile)
    }

    internal fun deleteSidecars(dbFile: File) {
        listOf(
            File(dbFile.absolutePath + WAL_SUFFIX),
            File(dbFile.absolutePath + SHM_SUFFIX),
        ).forEach { file ->
            if (file.exists()) file.delete()
        }
    }

    internal fun copySynced(source: File, dest: File) {
        dest.parentFile?.mkdirs()
        if (dest.exists()) dest.delete()
        FileOutputStream(dest).use { output ->
            source.inputStream().use { input -> input.copyTo(output) }
            output.fd.sync()
        }
        check(dest.length() > 0L && dest.length() == source.length()) {
            "failed to write ${dest.name}"
        }
    }

    fun deleteAllDatabases(context: Context) {
        deleteDatabase(context, SdeDatabase.ZH_FILE_NAME)
        deleteDatabase(context, SdeDatabase.EN_FILE_NAME)
    }

    internal fun recoverIncompleteIcons(context: Context) {
        val iconDir = File(context.filesDir, ICONS_DIR_NAME)
        val staging = File(context.filesDir, ICONS_DIR_NAME + STAGING_SUFFIX)
        val backup = File(context.filesDir, ICONS_DIR_NAME + BACKUP_SUFFIX)
        if (!iconDir.exists() && backup.exists()) {
            backup.renameTo(iconDir)
        }
        if (staging.exists()) staging.deleteRecursively()
        if (iconDir.exists() && backup.exists()) backup.deleteRecursively()
    }

    fun copyDatabaseFromAssets(context: Context, dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) return

        dbFile.parentFile?.mkdirs()
        context.assets.open("db/$dbName").use { input ->
            FileOutputStream(dbFile).use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
    }

    fun ensureDatabasesPresent(context: Context) {
        copyDatabaseFromAssets(context, SdeDatabase.ZH_FILE_NAME)
        copyDatabaseFromAssets(context, SdeDatabase.EN_FILE_NAME)
    }

    fun areDatabasesPresent(context: Context): Boolean {
        return context.getDatabasePath(SdeDatabase.ZH_FILE_NAME).exists() &&
            context.getDatabasePath(SdeDatabase.EN_FILE_NAME).exists()
    }

    internal fun materializeSqlite(source: File, dest: File) {
        dest.parentFile?.mkdirs()
        if (isGzip(source)) {
            GZIPInputStream(source.inputStream()).use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        } else {
            source.copyTo(dest, overwrite = true)
        }
    }

    private fun isGzip(file: File): Boolean {
        file.inputStream().use { input ->
            val first = input.read()
            val second = input.read()
            return first == GZIP_MAGIC_FIRST && second == GZIP_MAGIC_SECOND
        }
    }

    fun extractIconsFromFile(context: Context, zipFile: File) {
        replaceIcons(context) { staging ->
            zipFile.inputStream().use { input ->
                extractIconsZip(input, staging)
            }
        }
    }

    /** Unpack bundled [assets/icons/icons.zip] into filesDir/icons. */
    fun extractIconsFromAssets(context: Context) {
        replaceIcons(context) { staging ->
            context.assets.open("icons/icons.zip").use { input ->
                extractIconsZip(input, staging)
            }
        }
    }

    private fun replaceIcons(context: Context, populateStaging: (File) -> Unit) {
        val iconDir = File(context.filesDir, ICONS_DIR_NAME)
        val staging = File(context.filesDir, ICONS_DIR_NAME + STAGING_SUFFIX)
        val backup = File(context.filesDir, ICONS_DIR_NAME + BACKUP_SUFFIX)
        if (staging.exists()) staging.deleteRecursively()
        staging.mkdirs()
        try {
            populateStaging(staging)
            if (backup.exists()) backup.deleteRecursively()
            if (iconDir.exists() && !iconDir.renameTo(backup)) {
                error("failed to move current icons aside")
            }
            if (!staging.renameTo(iconDir)) {
                if (backup.exists()) backup.renameTo(iconDir)
                error("failed to install icons")
            }
            if (backup.exists()) backup.deleteRecursively()
        } catch (error: Exception) {
            if (!iconDir.exists() && backup.exists()) backup.renameTo(iconDir)
            if (staging.exists()) staging.deleteRecursively()
            throw error
        }
    }

    fun ensureIconsPresent(context: Context) {
        val iconDir = File(context.filesDir, ICONS_DIR_NAME)
        if (iconDir.exists() && iconDir.list()?.isNotEmpty() == true) return
        extractIconsFromAssets(context)
    }

    private fun extractIconsZip(input: java.io.InputStream, iconDir: File) {
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val outFile = File(iconDir, entry.name)
                outFile.parentFile?.mkdirs()
                outFile.outputStream().use { output ->
                    zip.copyTo(output)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
}
