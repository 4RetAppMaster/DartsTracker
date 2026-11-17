package com.example

import com.example.util.UpdateCheckResult
import com.example.util.Updater
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UpdaterTest {

    @Test
    fun normalizeVersion_stripsPrefixesAndExtractsEmbeddedSemver() {
        assertEquals("1.0", Updater.normalizeVersion("1.0"))
        assertEquals("1.1", Updater.normalizeVersion("v1.1"))
        assertEquals("1.1", Updater.normalizeVersion("V1.1"))
        assertEquals("1.1", Updater.normalizeVersion("Ver 1.1"))
        assertEquals("1.2", Updater.normalizeVersion("ver. 1.2"))
        assertEquals("2.0", Updater.normalizeVersion("DartTracker-Ver2.0"))
        assertEquals("2.0", Updater.normalizeVersion("Release v2.0"))
    }

    @Test
    fun isNewerVersion_comparesSemanticSegments() {
        assertTrue(Updater.isNewerVersion("1.1", "1.0"))
        assertTrue(Updater.isNewerVersion("v1.1", "1.0"))
        assertTrue(Updater.isNewerVersion("Ver 1.10", "1.9"))
        assertTrue(Updater.isNewerVersion("DartTracker-Ver2.0", "1.9"))
        assertTrue(Updater.isNewerVersion("2.0", "1.9"))
        assertTrue(Updater.isNewerVersion("1.0.1", "1.0"))

        assertFalse(Updater.isNewerVersion("1.0", "1.0"))
        assertFalse(Updater.isNewerVersion("v1.0", "1.0"))
        assertFalse(Updater.isNewerVersion("0.9", "1.0"))
        assertFalse(Updater.isNewerVersion("1.0.0", "1.0"))
    }

    @Test
    fun evaluateReleaseJson_prefersApkAssetAndFallsBackToReleaseName() {
        val release = JSONObject().apply {
            put("tag_name", "latest")
            put("name", "DartTracker Ver 2.1")
            put("html_url", "https://github.com/4RetAppMaster/DartsTracker/releases/tag/latest")
            put("assets", JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "DartTracker-Ver2.1.apk")
                    put("browser_download_url", "https://github.com/4RetAppMaster/DartsTracker/releases/download/v2.1/DartTracker-Ver2.1.apk")
                })
            })
        }

        val result = Updater.evaluateReleaseJson(release, "2.0")
        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val available = result as UpdateCheckResult.UpdateAvailable
        assertEquals("2.1", available.latestVersion)
        assertEquals(
            "https://github.com/4RetAppMaster/DartsTracker/releases/download/v2.1/DartTracker-Ver2.1.apk",
            available.releaseUrl
        )
    }
}
