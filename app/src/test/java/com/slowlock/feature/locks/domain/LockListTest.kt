package com.slowlock.feature.locks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The lock record's frozen shape and its sanitising reads.
 *
 * Every failure these catch is silent: a renamed file or key compiles clean, reads as absent, and
 * empties every user's Locks screen while their home-screen icons keep working, so the app looks
 * fine and the locks are simply gone (L1). There is no build-time check and no migration.
 *
 * These are pure functions by design — [LockOrderStore] holds only the wiring, because a test
 * written against `SharedPreferences` would throw on the first unmocked call.
 */
class LockListTest {

    /**
     * The rename guard (L1). These two strings are on disk on real devices, and the separator
     * decides whether an existing multi-lock value still splits into rows.
     */
    @Test
    fun `store file, key and separator are frozen`() {
        assertEquals("slowlock.locks", LOCKS_FILE)
        assertEquals("packages", LOCKS_KEY)
        assertEquals("\n", LOCKS_SEPARATOR)
    }

    /** FR-007: absent, empty and whitespace-only all mean the same thing — no locks. */
    @Test
    fun `absent, empty and blank read as no locks`() {
        assertEquals(emptyList<String>(), locksFrom(null))
        assertEquals(emptyList<String>(), locksFrom(""))
        assertEquals(emptyList<String>(), locksFrom("   "))
        assertEquals(emptyList<String>(), locksFrom("\n\n"))
    }

    /** FR-007: blank entries are dropped and surrounding whitespace is trimmed, never kept. */
    @Test
    fun `blank entries are dropped and entries are trimmed`() {
        assertEquals(
            listOf("com.example.a", "com.example.b"),
            locksFrom("  com.example.a  \n\n   \ncom.example.b\n"),
        )
    }

    /**
     * FR-013: a duplicate collapses to its *first* position. Later is not newer — a row that moved
     * would look to the user like the lock had been re-made.
     */
    @Test
    fun `duplicates collapse keeping the first position`() {
        assertEquals(
            listOf("com.example.a", "com.example.b"),
            locksFrom("com.example.a\ncom.example.b\ncom.example.a"),
        )
    }

    /** FR-006: order is insertion order, and decoding an encode changes nothing. */
    @Test
    fun `encode and decode round trip preserving order`() {
        val packages = listOf("com.example.c", "com.example.a", "com.example.b")
        assertEquals(packages, locksFrom(encodeLocks(packages)))
    }

    /** The encoded form is the frozen separator — not a comma, not a JSON array. */
    @Test
    fun `encode joins with the frozen separator`() {
        assertEquals(
            "com.example.a\ncom.example.b",
            encodeLocks(listOf("com.example.a", "com.example.b")),
        )
        assertEquals("", encodeLocks(emptyList()))
    }

    /**
     * FR-007, L4: nothing on the read path throws, and whatever comes back is already clean. A
     * malformed value is somebody's real device on a cold start, and the honest answer is a list —
     * possibly empty — never an exception.
     */
    @Test
    fun `the read path sanitises rather than throwing`() {
        val malformed = listOf(
            "\n",
            "\n\n\n",
            "   \n   ",
            "com.example.a\n\n\ncom.example.a\n   ",
            "{\"packages\":[\"com.example.a\"]}",
            " ",
            "\t\n \t",
        )
        malformed.forEach { stored ->
            val locks = locksFrom(stored)
            assertTrue(
                "blank or untrimmed entry survived: $locks",
                locks.none { it.isBlank() || it != it.trim() },
            )
            assertEquals("duplicates survived: $locks", locks.distinct(), locks)
        }
        assertEquals(emptyList<String>(), locksFrom("\n\n\n"))
        assertEquals(listOf("com.example.a"), locksFrom("com.example.a\n\n\ncom.example.a\n   "))
    }
}
