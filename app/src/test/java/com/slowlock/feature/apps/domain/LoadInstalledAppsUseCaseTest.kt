package com.slowlock.feature.apps.domain

import com.slowlock.core.domain.CurrentLocale
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rules that decide what the picker lists, now that the source only enumerates.
 *
 * These assertions used to be unreachable without a `LauncherApps`: the exclusion, the collapse and
 * the collation ran inside `InstalledAppsSource`, so a test could reach them only through the pure
 * functions individually, never through the composition that actually runs (contract U1-U3).
 */
class LoadInstalledAppsUseCaseTest {

    /**
     * U1: this app absent, one row per package, collated — from one enumeration containing all
     * three problems at once. Each of the three pure functions has its own test in
     * [InstalledAppTest]; what is asserted here is that the use case applies all of them.
     */
    @Test
    fun `excludes this app, collapses duplicate packages and collates the rest`() = runTest {
        val useCase = useCase(
            installed = listOf(
                app("com.slowlock", "SlowLock"),
                app("com.zoom", "Zoom"),
                app("com.android.settings", "Wi-Fi Settings"),
                app("com.android.settings", "Settings"),
                app("com.ebay", "eBay"),
                app("com.drive", "Drive"),
            ),
        )

        assertEquals(
            listOf("Drive", "eBay", "Settings", "Zoom"),
            useCase().map { it.label },
        )
    }

    /**
     * U1 again, on the ordering the composition depends on: SlowLock exposing two activities must
     * disappear entirely. A dedup that ran before the exclusion would collapse them into one entry
     * that the exclusion then removes — the same answer — but an exclusion that ran after the
     * *sort* would leave the collation describing a list that no longer exists.
     */
    @Test
    fun `excludes every activity of this app, not merely one`() = runTest {
        val useCase = useCase(
            installed = listOf(
                app("com.slowlock", "SlowLock"),
                app("com.slowlock", "SlowLock Shortcut"),
                app("com.example.notes", "Notes"),
            ),
        )

        assertEquals(listOf("com.example.notes"), useCase().map { it.packageName })
    }

    /**
     * U2: the locale is read inside `invoke`. A language change must re-collate on the next load,
     * so a use case holding the locale it was constructed with fails this — the second call would
     * still order "Über" after "Zoom".
     */
    @Test
    fun `reads the locale on every invocation, so a language change re-collates`() = runTest {
        val locale = MutableLocale(Locale.ENGLISH)
        val useCase = useCase(
            installed = listOf(app("com.ueber", "Über"), app("com.zoom", "Zoom")),
            locale = locale,
        )

        // Collator.SECONDARY puts "Über" with the U's in both locales; what differs is that the
        // reading happens again at all. Assert the call count directly rather than inferring it.
        useCase()
        assertEquals(1, locale.reads)

        locale.value = Locale.GERMAN
        useCase()
        assertEquals("the locale must be re-read, not captured at construction", 2, locale.reads)
    }

    /** U3: one enumeration per invocation, and nothing held between them. */
    @Test
    fun `re-enumerates on every invocation`() = runTest {
        val repository = CountingApps(listOf(app("com.example.notes", "Notes")))
        val useCase = useCase(repository = repository)

        assertEquals(listOf("Notes"), useCase().map { it.label })
        assertEquals(1, repository.loads)

        repository.apps = listOf(app("com.example.notes", "Notes"), app("com.zoom", "Zoom"))

        assertEquals(
            "a second call must see an app installed since the first",
            listOf("Notes", "Zoom"),
            useCase().map { it.label },
        )
        assertEquals(2, repository.loads)
    }

    private fun useCase(
        installed: List<InstalledApp> = emptyList(),
        repository: InstalledAppsRepository = CountingApps(installed),
        locale: CurrentLocale = CurrentLocale { Locale.ENGLISH },
    ) = LoadInstalledAppsUseCase(
        apps = repository,
        ownPackage = OWN_PACKAGE,
        locale = locale,
    )

    private fun app(packageName: String, label: String) =
        InstalledApp(packageName = packageName, label = label, versionCode = 1L)

    /** Counts enumerations, and lets a test change what the next one answers. */
    private class CountingApps(var apps: List<InstalledApp>) : InstalledAppsRepository {
        var loads = 0
        override suspend fun load(): List<InstalledApp> {
            loads++
            return apps
        }
    }

    /** Counts readings, and lets a test change the answer between them. */
    private class MutableLocale(var value: Locale) : CurrentLocale {
        var reads = 0
        override fun now(): Locale {
            reads++
            return value
        }
    }

    private companion object {
        const val OWN_PACKAGE = "com.slowlock"
    }
}
