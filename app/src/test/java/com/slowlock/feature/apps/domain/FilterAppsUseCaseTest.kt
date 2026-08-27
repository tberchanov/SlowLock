package com.slowlock.feature.apps.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What the search box matches (contract U4-U6).
 *
 * These cases moved out of [InstalledAppTest], where they reached the rule through a UI state class
 * from a domain test file. The rule is the same; what changed is that it now has a home the
 * assertion can name.
 */
class FilterAppsUseCaseTest {

    private val filterApps = FilterAppsUseCase()

    /** U5: the query is matched ignoring case, in either direction. */
    @Test
    fun `a mixed-case query matches`() {
        val apps = listOf(app("com.instagram.android", "Instagram"), app("com.example.notes", "Notes"))

        assertEquals(listOf("Instagram"), filterApps(apps, "INSTA").map { it.label })
        assertEquals(listOf("Instagram"), filterApps(apps, "insta").map { it.label })
        assertEquals(listOf("Instagram"), filterApps(apps, "InStA").map { it.label })
    }

    /** U5: substring, not prefix — the middle of a name matches too. */
    @Test
    fun `a substring from the middle of a label matches`() {
        val apps = listOf(app("com.instagram.android", "Instagram"), app("com.example.notes", "Notes"))

        assertEquals(listOf("Instagram"), filterApps(apps, "tagram").map { it.label })
    }

    /** U4: a blank query is not a filter — the full list comes back, untouched. */
    @Test
    fun `a blank query returns every app`() {
        val apps = listOf(app("com.instagram.android", "Instagram"), app("com.example.notes", "Notes"))

        assertEquals(apps, filterApps(apps, ""))
        assertEquals(apps, filterApps(apps, "   "))
    }

    /**
     * U6: filtering never re-sorts. "eBay" must stay where collation put it, between "Drive" and
     * "Files", rather than sliding after "Files" or back to the front.
     */
    @Test
    fun `the collated order of the full list is preserved`() {
        val sorted = listOf(
            app("com.zoom", "Zoom"),
            app("com.ebay", "eBay"),
            app("com.files", "Files"),
            app("com.drive", "Drive"),
        ).sortedByLabel(Locale.ENGLISH)

        assertEquals(listOf("Drive", "eBay", "Files"), filterApps(sorted, "e").map { it.label })
    }

    /** A query nothing matches is an empty list, not the full one. */
    @Test
    fun `a query matching nothing returns no apps`() {
        val apps = listOf(app("com.example.notes", "Notes"))

        assertEquals(emptyList<InstalledApp>(), filterApps(apps, "zzz"))
    }

    private fun app(packageName: String, label: String) =
        InstalledApp(packageName = packageName, label = label, versionCode = 1L)
}
