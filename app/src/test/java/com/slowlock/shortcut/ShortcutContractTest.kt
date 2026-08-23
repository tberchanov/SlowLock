package com.slowlock.shortcut

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * **The most load-bearing test in this codebase**, and the cheapest.
 *
 * `ComponentName(context, ShortcutLaunchActivity::class.java)` resolves at build time, so a
 * routine refactor that renames or moves that class compiles clean, pins *new* shortcuts under
 * the new name, and leaves every shortcut already on every home screen pointing at a component
 * that no longer exists. The user finds out by tapping an icon that does nothing, months later,
 * and SlowLock can neither enumerate those shortcuts nor repair them (`contracts/pinned-shortcut.md`).
 *
 * Asserting the runtime names against the frozen constants turns that from a permanent
 * home-screen failure into a `./gradlew test` failure. If one of these ever goes red, the fix is
 * to put the name back — or, if a rename is genuinely unavoidable, to add an `<activity-alias>`
 * under the old name. Updating the constant to match the new code is never the fix.
 */
class ShortcutContractTest {

    /** The rename guard. See the class KDoc for what it is standing between. */
    @Test
    fun `the launch activity's fully-qualified name is frozen`() {
        assertEquals(ShortcutContract.LAUNCH_ACTIVITY, ShortcutLaunchActivity::class.java.name)
    }

    /**
     * The persisted payload. A renamed key means [ShortcutLaunchActivity] reads nothing from
     * shortcuts pinned by earlier builds and every one of them dead-ends.
     */
    @Test
    fun `the target-package extra key is frozen`() {
        assertEquals(
            "com.slowlock.shortcut.extra.TARGET_PACKAGE",
            ShortcutContract.EXTRA_TARGET_PACKAGE,
        )
    }

    /** `ShortcutInfo.Builder` requires an action, and it is persisted with the intent. */
    @Test
    fun `the intent action is frozen`() {
        assertEquals("android.intent.action.VIEW", ShortcutContract.ACTION)
    }

    /**
     * FR-025: the ID **is** the package name, verbatim. This is what makes re-pinning idempotent
     * with no bookkeeping (FR-027) — any derived or generated scheme would orphan every existing
     * shortcut and add a second icon for every app (FR-026).
     */
    @Test
    fun `the shortcut id is the target package name`() {
        assertEquals(PACKAGE, ShortcutContract.shortcutId(PACKAGE))
    }

    /** The pure derivation the pin path uses, so the frozen values are asserted end to end. */
    @Test
    fun `the spec carries the package name as both id and payload`() {
        val spec = shortcutSpec(ShortcutTarget(PACKAGE, LABEL, VERSION))

        assertEquals(ShortcutSpec(id = PACKAGE, label = LABEL, targetPackage = PACKAGE), spec)
    }

    /** The label is copied verbatim — no suffix, no marker (spec, Assumptions). */
    @Test
    fun `the spec does not decorate the label`() {
        assertEquals(LABEL, shortcutSpec(ShortcutTarget(PACKAGE, LABEL, VERSION)).label)
    }

    private companion object {
        const val PACKAGE = "com.example.notes"
        const val LABEL = "Notes"
        const val VERSION = 42L
    }
}
