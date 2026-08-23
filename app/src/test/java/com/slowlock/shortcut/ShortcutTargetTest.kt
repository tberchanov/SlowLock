package com.slowlock.shortcut

import android.content.Intent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The null `getLaunchIntentForPackage()` path — **the unit test the constitution requires**, and
 * feature 002's half of it (FR-015, FR-018).
 *
 * The injected lookups are what make this reachable without a device: no `PackageManager`, no
 * emulator, no instrumented suite. `Intent` appears only as a type in the lambda's signature —
 * none is ever constructed, so nothing here depends on framework behaviour that
 * `isReturnDefaultValues = true` would stub out.
 */
class ShortcutTargetTest {

    /** FR-015: the app was uninstalled while the configuration screen was open. */
    @Test
    fun `an unresolvable package produces no target`() = runBlocking {
        val target = resolveTarget(
            packageName = UNINSTALLED,
            resolveLaunchIntent = { null },
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
            resolveLaunchIntent = { Intent() },
            loadLabel = { LABEL },
            loadVersionCode = { VERSION },
        )

        assertEquals(ShortcutTarget(INSTALLED, LABEL, VERSION), target)
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
            resolveLaunchIntent = { Intent() },
            loadLabel = { null },
            loadVersionCode = { VERSION },
        )

        assertNull(target)
    }

    /** FR-025, Constitution V: the package name is carried through untouched. */
    @Test
    fun `the resolved target keeps the package name it was asked about`() = runBlocking {
        val target = resolveTarget(
            packageName = INSTALLED,
            resolveLaunchIntent = { Intent() },
            loadLabel = { LABEL },
            loadVersionCode = { VERSION },
        )

        assertEquals(INSTALLED, target?.packageName)
    }

    private companion object {
        const val UNINSTALLED = "com.example.uninstalled"
        const val INSTALLED = "com.example.notes"
        const val LABEL = "Notes"
        const val VERSION = 42L
    }
}
