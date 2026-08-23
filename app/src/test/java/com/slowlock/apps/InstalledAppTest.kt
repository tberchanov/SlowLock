package com.slowlock.apps

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Covers only the logic that is invisible on screen — a silent sorting, dedup, or
 * cache-staleness bug looks exactly like correct behaviour during manual testing.
 * Everything observable is verified by `manual-test-plan.md` instead.
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

    /** FR-007: the query is matched ignoring case, in either direction. */
    @Test
    fun `visibleApps matches a mixed-case query`() {
        val state = stateOf(app("com.instagram.android", "Instagram"), app("com.example.notes", "Notes"))

        assertEquals(listOf("Instagram"), state.copy(query = "INSTA").visibleApps.map { it.label })
        assertEquals(listOf("Instagram"), state.copy(query = "insta").visibleApps.map { it.label })
        assertEquals(listOf("Instagram"), state.copy(query = "InStA").visibleApps.map { it.label })
    }

    /** FR-007: substring, not prefix — the middle of a name matches too. */
    @Test
    fun `visibleApps matches a substring from the middle of a label`() {
        val state = stateOf(app("com.instagram.android", "Instagram"), app("com.example.notes", "Notes"))

        assertEquals(listOf("Instagram"), state.copy(query = "tagram").visibleApps.map { it.label })
    }

    /** FR-008: a blank query is not a filter — the full list comes back. */
    @Test
    fun `visibleApps returns every app for a blank query`() {
        val apps = listOf(app("com.instagram.android", "Instagram"), app("com.example.notes", "Notes"))
        val state = AppListUiState(isLoading = false, apps = apps)

        assertEquals(apps, state.copy(query = "").visibleApps)
        assertEquals(apps, state.copy(query = "   ").visibleApps)
    }

    /**
     * FR-008: filtering never re-sorts. "eBay" must stay between "Drive" and "Files" — the
     * position collation gave it — rather than sliding after "Files" or back to the front.
     */
    @Test
    fun `visibleApps preserves the collated order of the full list`() {
        val sorted = listOf(
            app("com.zoom", "Zoom"),
            app("com.ebay", "eBay"),
            app("com.files", "Files"),
            app("com.drive", "Drive"),
        ).sortedByLabel(Locale.ENGLISH)

        val filtered = AppListUiState(isLoading = false, apps = sorted, query = "e").visibleApps

        assertEquals(listOf("Drive", "eBay", "Files"), filtered.map { it.label })
    }

    private fun app(packageName: String, label: String) =
        InstalledApp(packageName = packageName, label = label, versionCode = 1L)

    private fun stateOf(vararg apps: InstalledApp) =
        AppListUiState(isLoading = false, apps = apps.toList())
}
