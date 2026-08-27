package com.slowlock.feature.apps.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Covers only the logic that is invisible on screen: a silent sorting, dedup or cache-staleness bug
 * looks exactly like correct behaviour during manual testing. Everything observable is verified by
 * `manual-test-plan.md` instead.
 *
 * What the search box matches moved to [FilterAppsUseCaseTest] — it is a rule of its own, and it
 * used to be reached from here through a UI state class.
 */
class InstalledAppTest {

    /** FR-004: one row per package, even when a package exposes several launcher activities. */
    @Test
    fun `dedupeByPackage keeps one entry per package`() {
        val apps = listOf(
            app("com.android.settings", "Settings"),
            app("com.android.settings", "Wi-Fi Settings"),
            app("com.example.notes", "Notes"),
        )

        val deduped = apps.dedupeByPackage()

        assertEquals(
            listOf("com.android.settings", "com.example.notes"),
            deduped.map { it.packageName },
        )
    }

    /** FR-003: SlowLock never lists itself. */
    @Test
    fun `excludeSelf removes the app's own package`() {
        val apps = listOf(
            app("com.slowlock", "SlowLock"),
            app("com.example.notes", "Notes"),
        )

        val others = apps.excludeSelf(ownPackage = "com.slowlock")

        assertEquals(listOf("com.example.notes"), others.map { it.packageName })
    }

    /** FR-005: a lowercase initial belongs with its own letter, not after Z. */
    @Test
    fun `sortedByLabel places a lowercase initial among its own letter`() {
        val apps = listOf(
            app("com.zoom", "Zoom"),
            app("com.ebay", "eBay"),
            app("com.files", "Files"),
            app("com.drive", "Drive"),
        )

        val sorted = apps.sortedByLabel(Locale.ENGLISH)

        assertEquals(listOf("Drive", "eBay", "Files", "Zoom"), sorted.map { it.label })
    }

    /** FR-005: collation, not `lowercase()` — an umlaut sorts with its base letter. */
    @Test
    fun `sortedByLabel orders an umlaut with its base letter in German`() {
        val apps = listOf(
            app("com.zoom", "Zoom"),
            app("com.uhr", "Uhr"),
            app("com.ueber", "Über"),
            app("com.apfel", "Apfel"),
        )

        val sorted = apps.sortedByLabel(Locale.GERMAN)

        assertEquals(listOf("Apfel", "Über", "Uhr", "Zoom"), sorted.map { it.label })
    }

    /** FR-012 / Constitution V: an app update must miss the cache. */
    @Test
    fun `iconCacheKey changes when only the versionCode changes`() {
        assertNotEquals(
            iconCacheKey("com.example.notes", versionCode = 1L),
            iconCacheKey("com.example.notes", versionCode = 2L),
        )
    }

    private fun app(packageName: String, label: String) =
        InstalledApp(packageName = packageName, label = label, versionCode = 1L)
}
