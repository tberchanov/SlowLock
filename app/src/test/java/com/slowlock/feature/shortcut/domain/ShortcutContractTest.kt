package com.slowlock.feature.shortcut.domain

import com.slowlock.core.domain.AppTarget
import com.slowlock.shortcut.ShortcutLaunchActivity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * **The most load-bearing test in this codebase**, and the cheapest.
 *
 * `ComponentName(context, ShortcutLaunchActivity::class.java)` resolves at build time, so a
 * refactor that renames or moves that class compiles clean, pins *new* shortcuts under the new
 * name, and leaves every shortcut already on a home screen pointing at a component that no longer
 * exists — unenumerable and unrepairable by SlowLock.
 *
 * If one of these goes red, the fix is to put the name back, or add an `<activity-alias>` under the
 * old name. Updating the constant to match the new code is never the fix.
 */
class ShortcutContractTest {

    /**
     * The rename guard. Two assertions, because they catch different failures: the first catches
     * the class and the constant drifting apart either way, the second catches an IDE "Move class"
     * that updates *both* consistently — the one that ships green and dead-ends every pinned icon.
     * The literal is the only thing in front of it, which is why it is written out rather than
     * derived.
     */
    @Test
    fun `the launch activity's fully-qualified name is frozen`() {
        assertEquals(ShortcutContract.LAUNCH_ACTIVITY, ShortcutLaunchActivity::class.java.name)
        assertEquals(
            "com.slowlock.shortcut.ShortcutLaunchActivity",
            ShortcutContract.LAUNCH_ACTIVITY,
        )
    }

    /**
     * The persisted payload. A renamed key means [ShortcutLaunchActivity] reads nothing from
     * shortcuts pinned by earlier builds.
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
     * FR-025: the ID *is* the package name, verbatim, which makes re-pinning idempotent with no
     * bookkeeping (FR-027). Any generated scheme would orphan every existing shortcut (FR-026).
     */
    @Test
    fun `the shortcut id is the target package name`() {
        assertEquals(PACKAGE, ShortcutContract.shortcutId(PACKAGE))
    }

    /** The pure derivation the pin path uses, so the frozen values are asserted end to end. */
    @Test
    fun `the spec carries the package name as both id and payload`() {
        val spec = shortcutSpec(AppTarget(PACKAGE, LABEL, VERSION))

        assertEquals(ShortcutSpec(id = PACKAGE, label = LABEL, targetPackage = PACKAGE), spec)
    }

    /** The label is copied verbatim — no suffix, no marker. */
    @Test
    fun `the spec does not decorate the label`() {
        assertEquals(LABEL, shortcutSpec(AppTarget(PACKAGE, LABEL, VERSION)).label)
    }

    private companion object {
        const val PACKAGE = "com.example.notes"
        const val LABEL = "Notes"
        const val VERSION = 42L
    }
}
