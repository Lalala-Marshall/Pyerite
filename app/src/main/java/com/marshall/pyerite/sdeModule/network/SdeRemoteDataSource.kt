package com.marshall.pyerite.sdeModule.network

import com.marshall.pyerite.sdeModule.update.SdeRemoteConfig
import com.marshall.pyerite.sdeModule.update.SdeRemotePackage
import com.marshall.pyerite.sdeModule.update.SdeUpdateLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

class SdeRemoteDataSource(
    private val metadataApi: SdeRemoteApi,
    private val downloadClient: OkHttpClient,
) {

    suspend fun fetchLatestPackage(): SdeRemotePackage = withContext(Dispatchers.IO) {
        SdeUpdateLog.d("Remote", "fetchLatestPackage started")
        var lastError: Exception? = null
        for (source in metadataSources()) {
            repeat(SdeRemoteConfig.METADATA_SOURCE_ATTEMPTS) { attempt ->
                try {
                    val pkg = withTimeout(SdeRemoteConfig.METADATA_SOURCE_TIMEOUT_MILLIS.milliseconds) {
                        source.fetch()
                    }
                    if (pkg != null) {
                        SdeUpdateLog.d(
                            "Remote",
                            "metadata from ${source.name} build=${pkg.meta.buildNumber} tag=${pkg.tagName}",
                        )
                        return@withContext pkg
                    }
                    SdeUpdateLog.w("Remote", "${source.name} returned no package")
                } catch (error: TimeoutCancellationException) {
                    lastError = error
                    SdeUpdateLog.w("Remote", "${source.name} timed out (attempt ${attempt + 1})", error)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    lastError = error
                    SdeUpdateLog.w("Remote", "${source.name} failed (attempt ${attempt + 1})", error)
                }
            }
        }
        throw lastError ?: IOException("Unable to fetch remote SDE metadata")
    }

    suspend fun downloadFirstAvailable(
        urls: List<String>,
        dest: File,
        onProgress: (Float) -> Unit,
    ) = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (url in urls) {
            if (dest.exists()) dest.delete()
            repeat(SdeRemoteConfig.DOWNLOAD_URL_ATTEMPTS) { attempt ->
                try {
                    downloadOnce(url, dest, onProgress)
                    if (!dest.isFile || dest.length() <= 0L || fileLooksLikeHtml(dest)) {
                        dest.delete()
                        error("Unexpected response for $url")
                    }
                    return@withContext
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    lastError = error
                    SdeUpdateLog.w("Remote", "download failed ${dest.name} attempt ${attempt + 1}", error)
                }
            }
        }
        throw lastError ?: IOException("Unable to download ${dest.name}")
    }

    private fun metadataSources(): List<MetadataSource> = listOf(
        MetadataSource("Gitee") { fetchFromGitee() },
        MetadataSource("GitHub latest.json") { fetchFromLatestDownload() },
        MetadataSource("GitHub API") { fetchFromGitHubApi() },
    )

    private suspend fun fetchFromGitee(): SdeRemotePackage? {
        val release = metadataApi.fetchGiteeRelease(SdeRemoteConfig.GITEE_API_LATEST_RELEASE_URL)
        val tag = release.tagName.trim()
        if (tag.isEmpty()) return null
        val url = SdeRemoteConfig.giteeDownloadUrl(tag, SdeRemoteConfig.LATEST_JSON_ASSET)
        return validatePackage(metadataApi.fetchLatestPackage(url).toRemotePackage())
    }

    private suspend fun fetchFromLatestDownload(): SdeRemotePackage {
        return validatePackage(
            metadataApi.fetchLatestPackage(SdeRemoteConfig.LATEST_JSON_URL).toRemotePackage(),
        )
    }

    private suspend fun fetchFromGitHubApi(): SdeRemotePackage? {
        val release = metadataApi.fetchGitHubRelease(SdeRemoteConfig.GITHUB_API_LATEST_RELEASE_URL)
        val downloadUrl = release.assets
            .firstOrNull { it.name == SdeRemoteConfig.LATEST_JSON_ASSET }
            ?.browserDownloadUrl
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return validatePackage(metadataApi.fetchLatestPackage(downloadUrl).toRemotePackage())
    }

    private fun downloadOnce(startUrl: String, dest: File, onProgress: (Float) -> Unit) {
        var url = startUrl
        var redirects = 0
        val existing = dest.takeIf { it.isFile }?.length() ?: 0L
        while (true) {
            val request = Request.Builder().url(url).apply {
                if (existing > 0L && redirects == 0) {
                    header(SdeRemoteConfig.HEADER_RANGE, SdeRemoteConfig.rangeValue(existing))
                }
            }.build()
            downloadClient.newCall(request).execute().use { response ->
                if (SdeRemoteConfig.isDownloadRedirect(response.code)) {
                    val location = response.header(SdeRemoteConfig.HEADER_LOCATION)
                        ?: error("HTTP ${response.code} for $startUrl")
                    if (redirects >= SdeRemoteConfig.MAX_DOWNLOAD_REDIRECTS) {
                        error("HTTP ${response.code} for $startUrl")
                    }
                    redirects += 1
                    url = response.request.url.resolve(location).toString()
                    return@use
                }
                if (response.code == SdeRemoteConfig.HTTP_RANGE_NOT_SATISFIABLE && existing > 0L) {
                    onProgress(1f)
                    return
                }
                if (response.code != SdeRemoteConfig.HTTP_OK && response.code != SdeRemoteConfig.HTTP_PARTIAL) {
                    error("HTTP ${response.code} for $startUrl")
                }
                val body = response.body
                val append = response.code == SdeRemoteConfig.HTTP_PARTIAL && existing > 0L
                val bodyLength = body.contentLength().takeIf { it > 0L }
                val total = when {
                    append && bodyLength != null -> existing + bodyLength
                    bodyLength != null -> bodyLength
                    else -> null
                }
                dest.parentFile?.mkdirs()
                body.byteStream().use { input ->
                    FileOutputStream(dest, append).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var downloaded = if (append) existing else 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total != null) {
                                onProgress((downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
                if (total == null) onProgress(1f)
                return
            }
        }
    }

    private fun fileLooksLikeHtml(file: File): Boolean {
        if (!file.isFile || file.length() <= 0L) return false
        file.inputStream().use { input ->
            val buffer = ByteArray(SdeRemoteConfig.HTML_SNIFF_BYTES)
            val read = input.read(buffer)
            if (read <= 0) return false
            return buffer.decodeToString(endIndex = read).trimStart().startsWith("<")
        }
    }

    private fun validatePackage(pkg: SdeRemotePackage): SdeRemotePackage {
        val buildNumber = pkg.meta.buildNumber.toIntOrNull()
        require(buildNumber != null && buildNumber > 0) {
            "Invalid remote SDE build_number"
        }
        return pkg
    }

    private class MetadataSource(
        val name: String,
        val fetch: suspend () -> SdeRemotePackage?,
    )
}
