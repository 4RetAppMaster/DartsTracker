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
 * Utility class that checks the '4RetAppMaster/DartsTracker' GitHub repository
 * (Releases, Tags, root APK files, and app/build.gradle.kts) for newer versions
 * compared to [BuildConfig.VERSION_NAME].
 */
object Updater {

    private const val REPO_PATH = "4RetAppMaster/DartsTracker"
    private const val GITHUB_LATEST_RELEASE_URL =
        "https://api.github.com/repos/$REPO_PATH/releases/latest"
    private const val GITHUB_RELEASES_LIST_URL =
        "https://api.github.com/repos/$REPO_PATH/releases?per_page=10"
    private const val GITHUB_TAGS_LIST_URL =
        "https://api.github.com/repos/$REPO_PATH/tags?per_page=10"
    private const val GITHUB_ROOT_CONTENTS_URL =
        "https://api.github.com/repos/$REPO_PATH/contents"
    private const val RAW_GRADLE_HEAD_URL =
        "https://raw.githubusercontent.com/$REPO_PATH/HEAD/app/build.gradle.kts"
    private const val DEFAULT_RELEASES_PAGE =
        "https://github.com/$REPO_PATH/releases"

    private const val TIMEOUT_MS = 10_000

    private val LEADING_VERSION_PREFIX_REGEX = Regex("^(?i)(ver\\.?\\s*|v\\s*)")
    private val EMBEDDED_SEMVER_REGEX = Regex("""(?i)(?:ver\.?\s*|v)?(\d+(?:\.\d+)+)""")
    private val GRADLE_VERSION_NAME_REGEX = Regex("""versionName\s*=?\s*["']([^"']+)["']""")

    suspend fun checkForUpdate(
        currentVersion: String = BuildConfig.VERSION_NAME,
        endpointUrl: String = GITHUB_LATEST_RELEASE_URL
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val normalizedCurrent = normalizeVersion(currentVersion)
        try {
            val primary = fetchText(endpointUrl)
            var bestKnownUpToDate: UpdateCheckResult.UpToDate? = null
            var lastErrorCode: Int? = null

            if (primary.responseCode in 200..299 && primary.body.isNotBlank()) {
                val primaryEval = evaluateReleaseJson(
                    releaseJson = JSONObject(primary.body),
                    normalizedCurrent = normalizedCurrent
                )
                if (primaryEval is UpdateCheckResult.UpdateAvailable) {
                    return@withContext primaryEval
                }
                if (primaryEval is UpdateCheckResult.UpToDate) {
                    bestKnownUpToDate = primaryEval
                }
            } else if (primary.responseCode != HttpURLConnection.HTTP_NOT_FOUND) {
                lastErrorCode = primary.responseCode
            }

            if (endpointUrl == GITHUB_LATEST_RELEASE_URL) {
                val candidates = mutableListOf<UpdateCheckResult>()

                checkReleasesList(normalizedCurrent)?.let { candidates.add(it) }
                checkTagsList(normalizedCurrent)?.let { candidates.add(it) }
                checkRootApkContents(normalizedCurrent)?.let { candidates.add(it) }
                checkRawGradleVersion(normalizedCurrent)?.let { candidates.add(it) }

                val newestAvailable = candidates
                    .filterIsInstance<UpdateCheckResult.UpdateAvailable>()
                    .maxWithOrNull { a, b -> compareVersions(a.latestVersion, b.latestVersion) }
                if (newestAvailable != null) {
                    return@withContext newestAvailable
                }

                val highestKnown = (candidates.filterIsInstance<UpdateCheckResult.UpToDate>() + listOfNotNull(bestKnownUpToDate))
                    .filter { it.latestVersion != null }
                    .maxWithOrNull { a, b -> compareVersions(a.latestVersion!!, b.latestVersion!!) }
                if (highestKnown != null) {
                    return@withContext highestKnown
                }

                if (primary.responseCode == HttpURLConnection.HTTP_NOT_FOUND || bestKnownUpToDate != null) {
                    return@withContext UpdateCheckResult.UpToDate(
                        currentVersion = normalizedCurrent,
                        latestVersion = null
                    )
                }
            } else if (primary.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext UpdateCheckResult.UpToDate(
                    currentVersion = normalizedCurrent,
                    latestVersion = null
                )
            } else if (bestKnownUpToDate != null) {
                return@withContext bestKnownUpToDate
            }

            return@withContext UpdateCheckResult.Error(
                currentVersion = normalizedCurrent,
                message = if (lastErrorCode != null) {
                    "Unable to check updates (HTTP $lastErrorCode)"
                } else {
                    "Unable to check updates"
                }
            )
        } catch (e: Exception) {
            UpdateCheckResult.Error(
                currentVersion = normalizedCurrent,
                message = e.localizedMessage ?: "Unable to reach update server"
            )
        }
    }

    private fun checkReleasesList(normalizedCurrent: String): UpdateCheckResult? {
        val resp = runCatching { fetchText(GITHUB_RELEASES_LIST_URL) }.getOrNull() ?: return null
        if (resp.responseCode !in 200..299 || resp.body.isBlank()) return null

        val releasesArray = JSONArray(resp.body)
        var bestAvailable: UpdateCheckResult.UpdateAvailable? = null
        var bestUpToDate: UpdateCheckResult.UpToDate? = null
        for (i in 0 until releasesArray.length()) {
            val releaseObj = releasesArray.optJSONObject(i) ?: continue
            if (releaseObj.optBoolean("draft", false)) continue
            when (val eval = evaluateReleaseJson(releaseObj, normalizedCurrent)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    if (bestAvailable == null || isNewerVersion(eval.latestVersion, bestAvailable.latestVersion)) {
                        bestAvailable = eval
                    }
                }
                is UpdateCheckResult.UpToDate -> {
                    if (eval.latestVersion != null &&
                        (bestUpToDate?.latestVersion == null || isNewerVersion(eval.latestVersion, bestUpToDate.latestVersion!!))
                    ) {
                        bestUpToDate = eval
                    }
                }
                is UpdateCheckResult.Error -> Unit
            }
        }
        return bestAvailable ?: bestUpToDate
    }

    private fun checkTagsList(normalizedCurrent: String): UpdateCheckResult? {
        val resp = runCatching { fetchText(GITHUB_TAGS_LIST_URL) }.getOrNull() ?: return null
        if (resp.responseCode !in 200..299 || resp.body.isBlank()) return null

        val tagsArray = JSONArray(resp.body)
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
        if (bestTagVersion == null || bestTagName == null) return null

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

    private fun checkRootApkContents(normalizedCurrent: String): UpdateCheckResult? {
        val resp = runCatching { fetchText(GITHUB_ROOT_CONTENTS_URL) }.getOrNull() ?: return null
        if (resp.responseCode !in 200..299 || resp.body.isBlank()) return null
        return evaluateRootContentsJson(JSONArray(resp.body), normalizedCurrent)
    }

    internal fun evaluateRootContentsJson(
        contentsArray: JSONArray,
        normalizedCurrent: String
    ): UpdateCheckResult? {
        var bestVersion: String? = null
        var bestName: String? = null
        var bestDownloadUrl: String? = null

        for (i in 0 until contentsArray.length()) {
            val entry = contentsArray.optJSONObject(i) ?: continue
            if (entry.optString("type", "") != "file") continue
            val name = entry.optString("name", "").trim()
            if (!name.endsWith(".apk", ignoreCase = true)) continue

            val version = normalizeVersion(name.removeSuffix(".apk"))
            if (parseNumericParts(version).isEmpty()) continue

            if (bestVersion == null || isNewerVersion(version, bestVersion)) {
                bestVersion = version
                bestName = name
                val rawDownloadUrl = entry.optString("download_url", "").trim()
                val htmlUrl = entry.optString("html_url", "").trim()
                bestDownloadUrl = rawDownloadUrl.ifBlank {
                    htmlUrl.ifBlank { "https://github.com/$REPO_PATH/raw/HEAD/$name" }
                }
            }
        }

        if (bestVersion == null || bestName == null || bestDownloadUrl == null) return null
        return if (isNewerVersion(bestVersion, normalizedCurrent)) {
            UpdateCheckResult.UpdateAvailable(
                currentVersion = normalizedCurrent,
                latestVersion = bestVersion,
                tagName = "Ver $bestVersion",
                releaseUrl = bestDownloadUrl,
                releaseNotes = null
            )
        } else {
            UpdateCheckResult.UpToDate(
                currentVersion = normalizedCurrent,
                latestVersion = bestVersion
            )
        }
    }

    internal fun parseGradleVersionName(gradleText: String): String? {
        val match = GRADLE_VERSION_NAME_REGEX.find(gradleText) ?: return null
        val rawVersion = match.groupValues.getOrNull(1)?.trim() ?: return null
        val normalized = normalizeVersion(rawVersion)
        return normalized.takeIf { parseNumericParts(it).isNotEmpty() }
    }

    private fun checkRawGradleVersion(normalizedCurrent: String): UpdateCheckResult? {
        val resp = runCatching { fetchText(RAW_GRADLE_HEAD_URL, acceptHeader = "text/plain") }.getOrNull()
            ?: return null
        if (resp.responseCode !in 200..299 || resp.body.isBlank()) return null

        val normalizedLatest = parseGradleVersionName(resp.body) ?: return null

        return if (isNewerVersion(normalizedLatest, normalizedCurrent)) {
            val candidateApkUrl = "https://github.com/$REPO_PATH/raw/HEAD/DartTracker-Ver$normalizedLatest.apk"
            val apkExists = runCatching { urlExists(candidateApkUrl) }.getOrDefault(false)
            val targetUrl = if (apkExists) candidateApkUrl else "https://github.com/$REPO_PATH"
            UpdateCheckResult.UpdateAvailable(
                currentVersion = normalizedCurrent,
                latestVersion = normalizedLatest,
                tagName = "Ver $normalizedLatest",
                releaseUrl = targetUrl,
                releaseNotes = null
            )
        } else {
            UpdateCheckResult.UpToDate(
                currentVersion = normalizedCurrent,
                latestVersion = normalizedLatest
            )
        }
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

    private fun fetchText(
        endpointUrl: String,
        acceptHeader: String = "application/vnd.github+json"
    ): HttpResponse {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                useCaches = false
                instanceFollowRedirects = true
                setRequestProperty("Accept", acceptHeader)
                setRequestProperty("Cache-Control", "no-cache")
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

    private fun urlExists(url: String): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                useCaches = false
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "DartsTracker-Android/${BuildConfig.VERSION_NAME}")
            }
            connection.responseCode in 200..299
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

    internal fun compareVersions(a: String, b: String): Int {
        val aParts = parseNumericParts(a)
        val bParts = parseNumericParts(b)
        val maxLen = maxOf(aParts.size, bParts.size)
        for (i in 0 until maxLen) {
            val av = aParts.getOrElse(i) { 0 }
            val bv = bParts.getOrElse(i) { 0 }
            if (av != bv) return av.compareTo(bv)
        }
        return 0
    }

    internal fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = parseNumericParts(latest)
        val currentParts = parseNumericParts(current)
        if (latestParts.isEmpty() || currentParts.isEmpty()) return false
        return compareVersions(latest, current) > 0
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
