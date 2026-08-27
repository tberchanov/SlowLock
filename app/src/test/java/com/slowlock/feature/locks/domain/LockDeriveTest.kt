package com.slowlock.feature.locks.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rule that decides which locks exist: a lock exists exactly when its shortcut is pinned
 * (FR-003a) — including the case that matters most and is invisible on a happy path, a declined pin
 * dialog creating nothing.
 */
class LockDeriveTest {

    @Test
    fun `the frozen key is what the contract says`() {
        assertEquals("packages", LOCKS_KEY)
    }

    // ---- The pin dialog ------------------------------------------------------------------

    @Test
    fun `accepting the pin creates the lock`() {
        val result = deriveLocks(cached = emptyList(), pinned = setOf("com.a"))

        assertEquals(listOf("com.a"), result)
    }

    @Test
    fun `declining the pin creates nothing`() {
        // The case this design exists for: nothing was pinned, so there is nothing to show, and no
        // record was written at the tap that would have to be cleaned up.
        val result = deriveLocks(cached = emptyList(), pinned = emptySet())

        assertEquals(emptyList<String>(), result)
    }

    // ---- The home screen -----------------------------------------------------------------

    @Test
    fun `an unpinned shortcut takes its lock with it`() {
        val result = deriveLocks(
            cached = listOf("com.a", "com.b"),
            pinned = setOf("com.a"),
        )

        assertEquals(listOf("com.a"), result)
    }

    @Test
    fun `a shortcut pinned without the app being told still becomes a lock`() {
        // A launcher that pins but never fires the request's IntentSender. Deriving needs no
        // callback, so this needs no handling of its own.
        val result = deriveLocks(
            cached = listOf("com.a"),
            pinned = setOf("com.a", "com.silently.pinned"),
        )

        assertEquals(listOf("com.a", "com.silently.pinned"), result)
    }

    // ---- Removal is the user's, on their home screen ------------------------------------

    @Test
    fun `there is no way for the app to hide a pinned lock`() {
        // FR-021: removal happens on the home screen, so a pinned shortcut always yields a row.
        // This fails loudly if an in-app "remove" that merely hides the row is ever reintroduced.
        //
        // It doubles as the "declining leaves what was already there untouched" case: a pin dialog
        // dismissed for some other package changes neither input.
        val result = deriveLocks(cached = listOf("com.a"), pinned = setOf("com.a"))

        assertEquals(listOf("com.a"), result)
    }

    // ---- Order (FR-006) ------------------------------------------------------------------

    @Test
    fun `known packages keep their position and new ones are appended`() {
        val result = deriveLocks(
            cached = listOf("com.z", "com.a"),
            pinned = setOf("com.a", "com.z", "com.new"),
        )

        assertEquals(listOf("com.z", "com.a", "com.new"), result)
    }

    @Test
    fun `new packages are ordered deterministically rather than by the framework`() {
        val result = deriveLocks(
            cached = emptyList(),
            pinned = setOf("com.c", "com.a", "com.b"),
        )

        assertEquals(listOf("com.a", "com.b", "com.c"), result)
    }

    @Test
    fun `deriving twice from the same input changes nothing the second time`() {
        val once = deriveLocks(listOf("com.z"), setOf("com.z", "com.new"))
        val twice = deriveLocks(once, setOf("com.z", "com.new"))

        assertEquals(once, twice)
    }

    // ---- Totality ------------------------------------------------------------------------

    @Test
    fun `nothing pinned derives to no locks`() {
        val result = deriveLocks(listOf("com.a", "com.b"), emptySet())

        assertEquals(emptyList<String>(), result)
    }
}
