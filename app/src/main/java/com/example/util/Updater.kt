package com.example.util

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val tagName: String,
        val releaseUrl: String,
        val releaseNotes: String?
    ) : UpdateCheckResult()

    data class UpToDate(
        val currentVersion: String,
        val latestVersion: String?
    ) : UpdateCheckResult()

    data class Error(
        val currentVersion: String,
        val message: String
    ) : UpdateCheckResult()
}

/**
 * Utility class that checks the '4RetAppMaster/DartsTracker' GitHub Releases API
 * for newer releases compared to [BuildConfig.VERSION_NAME].
 */
object Updater {

    private const val GITHUB_LATEST_RELEASE_URL =
        "https://api.github.com/repos/4RetAppMaster/DartsTracker/releases/latest"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    suspend fun checkForUpdate(
        currentVersion: String = BuildConfig.VERSION_NAME,
        endpointUrl: String = GITHUB_LATEST_RELEASE_URL
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(endpointUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "DartsTracker-Android/${BuildConfig.VERSION_NAME}")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return@withContext UpdateCheckResult.UpToDate(
                        currentVersion = currentVersion,
                        latestVersion = null
                    )
                }

                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error(
                        currentVersion = currentVersion,
                        message = "GitHub returned HTTP ${response.code}"
                    )
                }

                val bodyString = response.body?.string()
                if (bodyString.isNullOrBlank()) {
                    return@withContext UpdateCheckResult.Error(
                        currentVersion = currentVersion,
                        message = "Empty response from GitHub"
                    )
                }

                val json = JSONObject(bodyString)
                val tagName = json.optString("tag_name", "").trim()
                val htmlUrl = json.optString(
                    "html_url",
                    "https://github.com/4RetAppMaster/DartsTracker/releases"
                )
                val releaseNotes = json.optString("body", "").takeIf { it.isNotBlank() }

                // Prefer attached APK download URL if present in release assets
                var downloadUrl = htmlUrl
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        val assetUrl = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk", ignoreCase = true) && assetUrl.isNotBlank()) {
                            downloadUrl = assetUrl
                            break
                        }
                    }
                }

                if (tagName.isBlank()) {
                    return@withContext UpdateCheckResult.UpToDate(
                        currentVersion = currentVersion,
                        latestVersion = null
                    )
                }

                val normalizedLatest = normalizeVersion(tagName)
                val normalizedCurrent = normalizeVersion(currentVersion)

                if (isNewerVersion(normalizedLatest, normalizedCurrent)) {
                    UpdateCheckResult.UpdateAvailable(
                        currentVersion = normalizedCurrent,
                        latestVersion = normalizedLatest,
                        tagName = tagName,
                        releaseUrl = downloadUrl,
                        releaseNotes = releaseNotes
                    )
                } else {
                    UpdateCheckResult.UpToDate(
                        currentVersion = normalizedCurrent,
                        latestVersion = normalizedLatest
                    )
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(
                currentVersion = normalizeVersion(currentVersion),
                message = e.localizedMessage ?: "Unable to reach GitHub"
            )
        }
    }

    internal fun normalizeVersion(raw: String): String {
        return raw.trim()
            .replace(Regex("^(?i)(ver\\.?\\s*|v\\s*)"), "")
            .trim()
    }

    internal fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = parseNumericParts(latest)
        val currentParts = parseNumericParts(current)
        if (latestParts.isEmpty() || currentParts.isEmpty()) return false

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun parseNumericParts(version: String): List<Int> {
        val clean = normalizeVersion(version).substringBefore("-")
        return clean.split(".")
            .mapNotNull { part ->
                part.filter { it.isDigit() }.toIntOrNull()
            }
    }
}
