package com.marshall.pyerite.settingsModule.data

import android.content.Context
import java.io.File
import java.util.Locale
import kotlin.math.round

internal class AppCacheCleaner(context: Context) {

    private val cacheDir = context.applicationContext.cacheDir
    private val filesDir = context.applicationContext.filesDir

    fun measureBytes(): Long = directorySize(cacheDir) + temporaryFileSize(filesDir)

    fun clear() {
        cacheDir.listFiles()?.forEach { child -> child.deleteRecursively() }
        if (!filesDir.exists()) return
        filesDir.walkTopDown()
            .filter { file -> file.isFile && file.name.endsWith(AppCacheConfig.TMP_SUFFIX) }
            .forEach { file -> file.delete() }
    }

    fun formatMegabytes(bytes: Long): String {
        val megabytes = bytes / AppCacheConfig.BYTES_PER_MEBIBYTE
        val rounded = round(megabytes * AppCacheConfig.DECIMAL_PLACES_FACTOR) /
            AppCacheConfig.DECIMAL_PLACES_FACTOR
        return String.format(Locale.US, AppCacheConfig.MEGABYTE_PATTERN, rounded)
    }

    private fun directorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun temporaryFileSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown()
            .filter { file -> file.isFile && file.name.endsWith(AppCacheConfig.TMP_SUFFIX) }
            .sumOf { it.length() }
    }
}

internal object AppCacheConfig {
    /** Atomic-write leftover used by feature disk caches. */
    const val TMP_SUFFIX = ".tmp"
    const val BYTES_PER_KIBIBYTE = 1024.0
    const val KIBIBYTES_PER_MEBIBYTE = 1024.0
    const val DECIMAL_PLACES_FACTOR = 10.0
    const val MEGABYTE_PATTERN = "%.1f"
    const val BYTES_PER_MEBIBYTE = BYTES_PER_KIBIBYTE * KIBIBYTES_PER_MEBIBYTE
}
