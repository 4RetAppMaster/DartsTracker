package com.example.util

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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

    private const val REPO_PATH = "4RetAppMaster/DartsTracker"
    private const val GITHUB_LATEST_RELEASE_URL =
        "https://api.github.com/repos/$REPO_PATH/releases/latest"
    private const val GITHUB_RELEASES_LIST_URL =
        "https://api.github.com/repos/$REPO_PATH/releases?per_page=10"
    private const val GITHUB_TAGS_LIST_URL =
        "https://api.github.com/repos/$REPO_PATH/tags?per_page=10"
    private const val DEFAULT_RELEASES_PAGE =
        "https://github.com/$REPO_PATH/releases"

    private const val TIMEOUT_MS = 10_000

    private val LEADING_VERSION_PREFIX_REGEX = Regex("^(?i)(ver\\.?\\s*|v\\s*)")
    private val EMBEDDED_SEMVER_REGEX = Regex("""(?i)(?:ver\.?\s*|v)?(\d+(?:\.\d+)+)""")

    suspend fun checkForUpdate(
        currentVersion: String = BuildConfig.VERSION_NAME,
        endpointUrl: String = GITHUB_LATEST_RELEASE_URL
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val normalizedCurrent = normalizeVersion(currentVersion)
        try {
            val primary = fetchJson(endpointUrl)
            if (primary.responseCode in 200..299 && primary.body.isNotBlank()) {
                return@withContext evaluateReleaseJson(
                    releaseJson = JSONObject(primary.body),
                    normalizedCurrent = normalizedCurrent
                )
            }

            if (primary.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                // If hitting the default GitHub /releases/latest endpoint, fall back to
                // all releases (e.g. pre-releases) or tags before concluding up-to-date.
                if (endpointUrl == GITHUB_LATEST_RELEASE_URL) {
                    checkFallbackReleasesOrTags(normalizedCurrent)?.let { return@withContext it }
                }
                return@withContext UpdateCheckResult.UpToDate(
                    currentVersion = normalizedCurrent,
                    latestVersion = null
                )
            }

            return@withContext UpdateCheckResult.Error(
                currentVersion = normalizedCurrent,
                message = "GitHub returned HTTP ${primary.responseCode}"
            )
        } catch (e: Exception) {
            UpdateCheckResult.Error(
                currentVersion = normalizedCurrent,
                message = e.localizedMessage ?: "Unable to reach GitHub"
            )
        }
    }

    private fun checkFallbackReleasesOrTags(normalizedCurrent: String): UpdateCheckResult? {
        val releasesResp = runCatching { fetchJson(GITHUB_RELEASES_LIST_URL) }.getOrNull()
        if (releasesResp != null && releasesResp.responseCode in 200..299 && releasesResp.body.isNotBlank()) {
            val releasesArray = JSONArray(releasesResp.body)
            var bestResult: UpdateCheckResult? = null
            for (i in 0 until releasesArray.length()) {
                val releaseObj = releasesArray.optJSONObject(i) ?: continue
                if (releaseObj.optBoolean("draft", false)) continue
                val candidate = evaluateReleaseJson(releaseObj, normalizedCurrent)
                if (candidate is UpdateCheckResult.UpdateAvailable) {
                    return candidate
                }
                if (bestResult == null && candidate is UpdateCheckResult.UpToDate && candidate.latestVersion != null) {
                    bestResult = candidate
                }
            }
            if (bestResult != null) return bestResult
        }

        val tagsResp = runCatching { fetchJson(GITHUB_TAGS_LIST_URL) }.getOrNull()
        if (tagsResp != null && tagsResp.responseCode in 200..299 && tagsResp.body.isNotBlank()) {
            val tagsArray = JSONArray(tagsResp.body)
            var bestTagVersion: String? = null
            var bestTagName: String? = null
            for (i in 0 until tagsArray.length()) {
                val tagObj = tagsArray.optJSONObject(i) ?: continue
                val tagName = tagObj.optString("name", "").trim()
                if (tagName.isBlank()) continue
                val normalizedTag = normalizeVersion(tagName)
                if (parseNumericParts(normalizedTag).isEmpty()) continue
                if (bestTagVersion == null || isNewerVersion(normalizedTag, bestTagVersion)) {
                    bestTagVersion = normalizedTag
                    bestTagName = tagName
                }
            }
            if (bestTagVersion != null && bestTagName != null) {
                return if (isNewerVersion(bestTagVersion, normalizedCurrent)) {
                    UpdateCheckResult.UpdateAvailable(
                        currentVersion = normalizedCurrent,
                        latestVersion = bestTagVersion,
                        tagName = bestTagName,
                        releaseUrl = "https://github.com/$REPO_PATH/releases/tag/$bestTagName",
                        releaseNotes = null
                    )
                } else {
                    UpdateCheckResult.UpToDate(
                        currentVersion = normalizedCurrent,
                        latestVersion = bestTagVersion
                    )
                }
            }
        }
        return null
    }

    internal fun evaluateReleaseJson(
        releaseJson: JSONObject,
        normalizedCurrent: String
    ): UpdateCheckResult {
        val rawTagName = releaseJson.optString("tag_name", "").trim()
        val rawReleaseName = releaseJson.optString("name", "").trim()
        val htmlUrl = releaseJson.optString("html_url", DEFAULT_RELEASES_PAGE)
            .ifBlank { DEFAULT_RELEASES_PAGE }
        val releaseNotes = releaseJson.optString("body", "").takeIf { it.isNotBlank() }

        // Prefer attached APK download URL if present in release assets
        var downloadUrl = htmlUrl
        val assets = releaseJson.optJSONArray("assets")
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

        val versionCandidate = when {
            parseNumericParts(normalizeVersion(rawTagName)).isNotEmpty() -> rawTagName
            parseNumericParts(normalizeVersion(rawReleaseName)).isNotEmpty() -> rawReleaseName
            else -> ""
        }

        if (versionCandidate.isBlank()) {
            return UpdateCheckResult.UpToDate(
                currentVersion = normalizedCurrent,
                latestVersion = null
            )
        }

        val normalizedLatest = normalizeVersion(versionCandidate)
        val displayTag = rawTagName.ifBlank { rawReleaseName }

        return if (isNewerVersion(normalizedLatest, normalizedCurrent)) {
            UpdateCheckResult.UpdateAvailable(
                currentVersion = normalizedCurrent,
                latestVersion = normalizedLatest,
                tagName = displayTag,
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

    private data class HttpResponse(val responseCode: Int, val body: String)

    private fun fetchJson(endpointUrl: String): HttpResponse {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "DartsTracker-Android/${BuildConfig.VERSION_NAME}")
            }
            val code = connection.responseCode
            val body = if (code in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                ""
            }
            return HttpResponse(code, body)
        } finally {
            connection?.disconnect()
        }
    }

    internal fun normalizeVersion(raw: String): String {
        val stripped = raw.trim().replace(LEADING_VERSION_PREFIX_REGEX, "").trim()
        if (stripped.isNotEmpty() && stripped.first().isDigit()) {
            return stripped
        }
        val embedded = EMBEDDED_SEMVER_REGEX.find(raw)?.groupValues?.getOrNull(1)
        return embedded ?: stripped
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
        if (clean.isEmpty() || clean.none { it.isDigit() }) return emptyList()
        return clean.split(".")
            .mapNotNull { part ->
                part.filter { it.isDigit() }.toIntOrNull()
            }
    }
}
