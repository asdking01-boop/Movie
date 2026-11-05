package com.example.core.network

import com.example.core.database.ContentSourceEntity
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

object NetworkClient {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Validates that a URL uses HTTPS (or internal kras:// protocol for verified open feeds).
     * Rejects insecure cleartext HTTP by default per Security & Content Permissions policy.
     */
    fun isSecureUrl(url: String): Boolean {
        val trimmed = url.trim().lowercase()
        return trimmed.startsWith("https://") || trimmed.startsWith("kras://")
    }

    fun buildAuthorizedRequest(
        source: ContentSourceEntity,
        overrideUrl: String? = null,
        acceptHeader: String = "application/json, application/xml, text/xml, audio/x-mpegurl, text/html, */*"
    ): Request {
        val targetUrl = (overrideUrl ?: source.url).trim()
        val builder = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", "KrasMoviesAndroid/1.0 (Authorized Catalog Client; +https://krasmovies.example)")
            .header("Accept", acceptHeader)
            .header("Accept-Language", source.preferredLanguage.ifBlank { "en-US,en;q=0.9,ar;q=0.8" })

        if (source.apiKey.isNotBlank()) {
            if (source.apiKey.startsWith("Bearer ", ignoreCase = true)) {
                builder.header("Authorization", source.apiKey.trim())
            } else {
                builder.header("Authorization", "Bearer ${source.apiKey.trim()}")
                builder.header("X-API-Key", source.apiKey.trim())
            }
        } else if (source.username.isNotBlank() && source.password.isNotBlank()) {
            builder.header("Authorization", Credentials.basic(source.username.trim(), source.password.trim()))
        }

        return builder.build()
    }

    /**
     * Inspects an HTTP response for anti-bot, CAPTCHA, paywall, DRM, or authentication restrictions.
     * Never bypasses restrictions; returns a clear human-readable diagnostic message.
     */
    fun inspectForAccessRestrictions(response: Response, bodyPreview: String): String? {
        val code = response.code
        val server = response.header("Server").orEmpty().lowercase()
        val lowerBody = bodyPreview.take(2000).lowercase()

        if (code == 401) {
            return "Authentication required (HTTP 401). Please provide a valid API key or authorized login credentials."
        }
        if (code == 403) {
            if (server.contains("cloudflare") || lowerBody.contains("cf-chl") || lowerBody.contains("attention required")) {
                return "Access blocked by anti-bot protection / CAPTCHA (HTTP 403). Kras Movies does not bypass anti-bot protections."
            }
            return "Access forbidden or paywall restriction detected (HTTP 403). Verify that your account has permission to access this feed."
        }
        if (code == 429) {
            return "Rate limit exceeded (HTTP 429). Sync Engine will back off and retry automatically."
        }
        if (lowerBody.contains("g-recaptcha") || lowerBody.contains("hcaptcha") || lowerBody.contains("turnstile")) {
            return "CAPTCHA challenge detected. Kras Movies respects website access controls and cannot import from CAPTCHA-gated pages."
        }
        if (lowerBody.contains("widevine") && lowerBody.contains("license") && !lowerBody.contains("authorized")) {
            return "Protected DRM stream without authorized license contract detected. DRM circumvention is strictly prohibited."
        }
        return null
    }
}
