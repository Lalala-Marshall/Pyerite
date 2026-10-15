package com.marshall.pyerite.sdeModule.network

import retrofit2.http.GET
import retrofit2.http.Url

interface SdeRemoteApi {

    @GET
    suspend fun fetchLatestPackage(@Url url: String): SdeLatestJsonDto

    @GET
    suspend fun fetchGitHubReleases(@Url url: String): List<GitHubReleaseDto>

    @GET
    suspend fun fetchGiteeRelease(@Url url: String): GiteeReleaseDto
}
