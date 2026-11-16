package com.example

import com.example.util.Updater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {

    @Test
    fun normalizeVersion_stripsPrefixesAndWhitespace() {
        assertEquals("1.0", Updater.normalizeVersion("1.0"))
        assertEquals("1.1", Updater.normalizeVersion("v1.1"))
        assertEquals("1.1", Updater.normalizeVersion("V1.1"))
        assertEquals("1.1", Updater.normalizeVersion("Ver 1.1"))
        assertEquals("1.2", Updater.normalizeVersion("ver. 1.2"))
    }

    @Test
    fun isNewerVersion_comparesSemanticSegments() {
        assertTrue(Updater.isNewerVersion("1.1", "1.0"))
        assertTrue(Updater.isNewerVersion("v1.1", "1.0"))
        assertTrue(Updater.isNewerVersion("Ver 1.10", "1.9"))
        assertTrue(Updater.isNewerVersion("2.0", "1.9"))
        assertTrue(Updater.isNewerVersion("1.0.1", "1.0"))

        assertFalse(Updater.isNewerVersion("1.0", "1.0"))
        assertFalse(Updater.isNewerVersion("v1.0", "1.0"))
        assertFalse(Updater.isNewerVersion("0.9", "1.0"))
        assertFalse(Updater.isNewerVersion("1.0.0", "1.0"))
    }
}
