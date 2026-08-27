package com.slowlock.core.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The null `getLaunchIntentForPackage()` path — **the unit test the constitution requires**
 * (FR-015, FR-018).
 *
 * The injected lookups are what make this reachable without a device: no `PackageManager`, no
 * emulator, no instrumented suite. **No framework type appears at all** — the launch-intent seam
 * answers a `Boolean`, so nothing here reaches for platform behaviour the JVM suite does not have,
 * and `core/domain` stays free of `android.*` (FR-025).
 */
class AppTargetTest {

    /** FR-015: the app was uninstalled while the configuration screen was open. */
    @Test
    fun `an unresolvable package produces no target`() = runBlocking {
        val target = resolveTarget(
            packageName = UNINSTALLED,
            isLaunchable = { false },
            loadLabel = { error("must not be asked for a label once resolution has failed") },
            loadVersionCode = { error("must not be asked for a version once resolution failed") },
        )

        assertNull(target)
    }

    /** The ordinary path: a resolvable package yields its display facts. */
    @Test
    fun `a resolvable package produces a populated target`() = runBlocking {
        val target = resolveTarget(
            packageName = INSTALLED,
            isLaunchable = { true },
            loadLabel = { LABEL },
            loadVersionCode = { VERSION },
        )

        assertEquals(AppTarget(INSTALLED, LABEL, VERSION), target)
    }

    /**
     * A package can resolve to a launch intent and still have no readable label — the label
     * lookup throws `NameNotFoundException` if the app disappears between the two calls. That is
     * the same FR-015 outcome, not a crash and not a shortcut labelled with a placeholder.
     */
    @Test
    fun `a package with no readable label produces no target`() = runBlocking {
        val target = resolveTarget(
            packageName = INSTALLED,
            isLaunchable = { true },
            loadLabel = { null },
            loadVersionCode = { VERSION },
        )

        assertNull(target)
    }

    private companion object {
        const val UNINSTALLED = "com.example.uninstalled"
        const val INSTALLED = "com.example.notes"
        const val LABEL = "Notes"
        const val VERSION = 42L
    }
}
