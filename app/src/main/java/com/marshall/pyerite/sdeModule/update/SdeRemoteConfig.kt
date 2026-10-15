package com.marshall.pyerite.sdeModule.update

object SdeRemoteConfig {
    const val GITHUB_HOST = "github.com"
    const val GITHUB_API_HOST = "api.github.com"
    const val GITEE_HOST = "gitee.com"
    const val GITHUB_BASE_URL = "https://$GITHUB_HOST/"
    const val GITHUB_API_BASE_URL = "https://$GITHUB_API_HOST/"
    const val GITEE_BASE_URL = "https://$GITEE_HOST/"

    const val GITHUB_REPO = "Lalala-Marshall/Pyerite"
    const val GITEE_OWNER = "lalala-marshall"
    const val GITEE_REPO = "pyerite-sde-source"

    const val LATEST_JSON_ASSET = "latest.json"
    const val ASSET_DB_ZH = "item_db_zh.sqlite"
    const val ASSET_DB_EN = "item_db_en.sqlite"
    const val ASSET_DB_ZH_GZ = "$ASSET_DB_ZH.gz"
    const val ASSET_DB_EN_GZ = "$ASSET_DB_EN.gz"
    const val ASSET_ICONS = "icons.zip"
    const val ASSET_ICON_PARTS = "icons.zip.parts"
    const val ICON_PART_DIGITS = 2
    const val MIN_ICON_PARTS = 2
    const val MAX_ICON_PARTS = 32
    const val RELEASE_TAG_PREFIX = "sde-build-"

    /** GitHub lists at most 100 releases per page. One page still reaches the newest sde-build-* tag. */
    const val RELEASE_LIST_QUERY = "per_page=100"

    const val LATEST_JSON_URL =
        "${GITHUB_BASE_URL}$GITHUB_REPO/releases/latest/download/$LATEST_JSON_ASSET"
    const val GITHUB_API_RELEASES_URL =
        "${GITHUB_API_BASE_URL}repos/$GITHUB_REPO/releases?$RELEASE_LIST_QUERY"
    const val GITEE_API_LATEST_RELEASE_URL =
        "${GITEE_BASE_URL}api/v5/repos/$GITEE_OWNER/$GITEE_REPO/releases/latest"

    const val METADATA_CONNECT_TIMEOUT_SECONDS = 20L
    const val METADATA_READ_TIMEOUT_SECONDS = 30L
    const val METADATA_SOURCE_TIMEOUT_MILLIS = 45_000L
    const val METADATA_SOURCE_ATTEMPTS = 2
    const val DOWNLOAD_CONNECT_TIMEOUT_SECONDS = 30L
    const val DOWNLOAD_READ_TIMEOUT_MINUTES = 30L
    const val DOWNLOAD_URL_ATTEMPTS = 2

    const val HEADER_RANGE = "Range"
    const val HTTP_OK = 200
    const val HTTP_PARTIAL = 206
    const val HTTP_RANGE_NOT_SATISFIABLE = 416
    const val HTTP_MOVED = 301
    const val HTTP_FOUND = 302
    const val HTTP_SEE_OTHER = 303
    const val HTTP_TEMPORARY_REDIRECT = 307
    const val HTTP_PERMANENT_REDIRECT = 308
    const val MAX_DOWNLOAD_REDIRECTS = 5
    const val HEADER_LOCATION = "Location"
    const val HTML_SNIFF_BYTES = 64

    /** Dummy base URL; calls use absolute retrofit `@Url` values. */
    const val RETROFIT_BASE_URL = GITHUB_BASE_URL

    fun releaseTag(buildNumber: String, tagName: String = ""): String =
        tagName.trim().ifBlank { "$RELEASE_TAG_PREFIX$buildNumber" }

    fun giteeDownloadUrl(tag: String, fileName: String): String =
        "${GITEE_BASE_URL}$GITEE_OWNER/$GITEE_REPO/releases/download/$tag/$fileName"

    fun githubDownloadUrl(tag: String, fileName: String): String =
        "${GITHUB_BASE_URL}$GITHUB_REPO/releases/download/$tag/$fileName"

    fun downloadUrls(tag: String, fileName: String): List<String> = listOf(
        giteeDownloadUrl(tag, fileName),
        githubDownloadUrl(tag, fileName),
    )

    /** Icon pieces are only attached to the Gitee release. */
    fun giteeDownloadUrls(tag: String, fileName: String): List<String> =
        listOf(giteeDownloadUrl(tag, fileName))

    fun isDownloadRedirect(code: Int): Boolean = code == HTTP_MOVED ||
        code == HTTP_FOUND ||
        code == HTTP_SEE_OTHER ||
        code == HTTP_TEMPORARY_REDIRECT ||
        code == HTTP_PERMANENT_REDIRECT

    fun iconPartName(index: Int): String {
        val suffix = index.toString().padStart(ICON_PART_DIGITS, '0')
        return "$ASSET_ICONS.part$suffix"
    }

    fun databaseDownloadUrls(tag: String, gzName: String, plainName: String): List<String> =
        downloadUrls(tag, gzName) + downloadUrls(tag, plainName)

    fun rangeValue(start: Long): String = "bytes=$start-"
}
