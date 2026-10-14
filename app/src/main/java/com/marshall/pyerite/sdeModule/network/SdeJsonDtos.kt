package com.marshall.pyerite.sdeModule.network

import com.marshall.pyerite.sdeModule.update.SdeReleaseMeta
import com.marshall.pyerite.sdeModule.update.SdeRemoteConfig
import com.marshall.pyerite.sdeModule.update.SdeRemotePackage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SdeLatestJsonDto(
    @SerialName("build_number") val buildNumber: String = "0",
    @SerialName("release_date") val releaseDate: String = "",
    @SerialName("completion_time") val completionTime: String = "",
    @SerialName("icon_version") val iconVersion: Int? = null,
    @SerialName("icon_sha256") val iconSha256: String? = null,
    @SerialName("sde_sha256") val sdeSha256: String? = null,
    @SerialName("item_db_zh_sha256") val dbZhSha256: String? = null,
    @SerialName("item_db_en_sha256") val dbEnSha256: String? = null,
    @SerialName("tag_name") val tagName: String = "",
) {
    fun toReleaseMeta(): SdeReleaseMeta = SdeReleaseMeta(
        buildNumber = buildNumber,
        releaseDate = releaseDate,
        completionTime = completionTime,
        iconVersion = iconVersion?.takeIf { it >= 0 },
        iconSha256 = iconSha256?.ifBlank { null },
        sdeSha256 = sdeSha256?.ifBlank { null },
        dbZhSha256 = dbZhSha256?.ifBlank { null },
        dbEnSha256 = dbEnSha256?.ifBlank { null },
    )

    fun toRemotePackage(): SdeRemotePackage = SdeRemotePackage(
        meta = toReleaseMeta(),
        tagName = SdeRemoteConfig.releaseTag(buildNumber, tagName),
    )
}

@Serializable
data class GitHubReleaseDto(
    val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
data class GitHubAssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
)

@Serializable
data class GiteeReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
)
