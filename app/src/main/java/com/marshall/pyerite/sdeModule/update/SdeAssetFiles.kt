package com.marshall.pyerite.sdeModule.update

import android.content.Context
import com.marshall.pyerite.localization.SdeDatabase
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

object SdeAssetFiles {

    const val ICONS_DIR_NAME = "icons"
    private const val GZIP_MAGIC_FIRST = 0x1f
    private const val GZIP_MAGIC_SECOND = 0x8b

    fun deleteDatabase(context: Context, dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        listOf(
            dbFile,
            File("${dbFile.absolutePath}-wal"),
            File("${dbFile.absolutePath}-shm"),
        ).forEach { file ->
            if (file.exists()) file.delete()
        }
    }

    fun deleteAllDatabases(context: Context) {
        deleteDatabase(context, SdeDatabase.ZH_FILE_NAME)
        deleteDatabase(context, SdeDatabase.EN_FILE_NAME)
    }

    fun deleteIcons(context: Context) {
        val iconDir = File(context.filesDir, ICONS_DIR_NAME)
        if (iconDir.exists()) iconDir.deleteRecursively()
    }

    fun copyDatabaseFromAssets(context: Context, dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) return

        dbFile.parentFile?.mkdirs()
        context.assets.open("db/$dbName").use { input ->
            FileOutputStream(dbFile).use { output ->
                input.copyTo(output)
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

    fun installDatabaseFromFile(context: Context, dbName: String, source: File) {
        deleteDatabase(context, dbName)
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        source.copyTo(dbFile, overwrite = true)
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
        deleteIcons(context)
        val iconDir = File(context.filesDir, ICONS_DIR_NAME)
        iconDir.mkdirs()
        zipFile.inputStream().use { input ->
            extractIconsZip(input, iconDir)
        }
    }

    /** Unpack bundled [assets/icons/icons.zip] into filesDir/icons. */
    fun extractIconsFromAssets(context: Context) {
        deleteIcons(context)
        val iconDir = File(context.filesDir, ICONS_DIR_NAME)
        iconDir.mkdirs()
        context.assets.open("icons/icons.zip").use { input ->
            extractIconsZip(input, iconDir)
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
