package com.marshall.pyerite.sdeModule.network

import com.marshall.pyerite.sdeModule.update.SdeRemoteConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

/** GitHub API headers for SDE release metadata / download. */
class SdeGitHubHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()
        if (request.url.host == SdeRemoteConfig.GITHUB_API_HOST) {
            builder.header("Accept", "application/vnd.github+json")
            builder.header("X-GitHub-Api-Version", "2022-11-28")
        }
        return chain.proceed(builder.build())
    }
}

fun createSdeOkHttpClient(
    connectTimeoutSeconds: Long,
    readTimeout: Long,
    readTimeoutUnit: TimeUnit = TimeUnit.SECONDS,
    base: OkHttpClient,
    followRedirects: Boolean = true,
): OkHttpClient {
    val builder = base.newBuilder()
        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(readTimeout, readTimeoutUnit)
        .followRedirects(followRedirects)
        .followSslRedirects(followRedirects)
        .addInterceptor(SdeGitHubHeadersInterceptor())
    if (!followRedirects) {
        builder.cookieJar(SdeRedirectCookieJar())
    }
    return builder.build()
}

/** Keeps Set-Cookie across the manual Gitee download redirects. */
private class SdeRedirectCookieJar : CookieJar {
    private val cookies = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(this.cookies) {
            this.cookies.removeAll { saved ->
                cookies.any { it.name == saved.name && it.domain == saved.domain }
            }
            this.cookies.addAll(cookies)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        synchronized(cookies) { cookies.filter { it.matches(url) } }
}
