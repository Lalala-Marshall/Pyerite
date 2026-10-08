package com.marshall.pyerite.sdeModule.update

object SdeRemoteConfig {
    const val GITHUB_HOST = "github.com"
    const val GITHUB_API_HOST = "api.github.com"
    const val GITHUB_BASE_URL = "https://$GITHUB_HOST/"
    const val GITHUB_API_BASE_URL = "https://$GITHUB_API_HOST/"

    const val GITHUB_REPO = "Lalala-Marshall/Pyerite"
    const val LATEST_JSON_ASSET = "latest.json"
    const val LATEST_JSON_URL =
        "${GITHUB_BASE_URL}$GITHUB_REPO/releases/latest/download/$LATEST_JSON_ASSET"
    const val GITHUB_API_LATEST_RELEASE_URL =
        "${GITHUB_API_BASE_URL}repos/$GITHUB_REPO/releases/latest"

    /** Dummy base URL; calls use absolute retrofit `@Url` values. */
    const val RETROFIT_BASE_URL = GITHUB_BASE_URL
}
