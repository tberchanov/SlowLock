package com.slowlock.feature.locks.ui

import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.DelayConfig
import com.slowlock.core.domain.IconTreatment
import com.slowlock.core.domain.resolveTarget
import com.slowlock.feature.locks.domain.Lock
import com.slowlock.feature.locks.domain.assembleLocks
import com.slowlock.feature.locks.domain.isAvailable
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the Locks screen shows, decided without a device.
 *
 * The null-resolution path is the test the constitution requires: a lock whose package no longer
 * resolves must become an ordinary row rather than an exception on the app's front door (FR-020).
 * The injected lookups in [assembleLocks] are what make it reachable with no `PackageManager`.
 *
 * These assertions target [assembleLocks] and [LocksUiState] rather than [LocksViewModel], because
 * `viewModelScope` dispatches on `Dispatchers.Main`. That is why both the row assembly and the
 * latch live outside the ViewModel as plain functions.
 */
class LocksViewModelTest {

    /** FR-017: no locks is the intro's condition, and it is derived, never stored. */
    @Test
    fun `an empty lock list is the intro condition`() = runBlocking {
        val state = LocksUiState().withLocks(assembleLocks(emptyList(), ::noConfig, ::noTarget))

        assertTrue(state.loaded)
        assertTrue(state.showsIntro)
        assertFalse(state.showsLocks)
    }

    /** FR-005: the row carries the delay and treatment the configuration store holds. */
    @Test
    fun `a resolvable package becomes an available row carrying its stored values`() = runBlocking {
        val locks = assembleLocks(
            packages = listOf(NOTES),
            loadConfig = { DelayConfig(30, IconTreatment.Gray) },
            resolveTarget = { AppTarget(it, LABEL, VERSION) },
        )

        assertEquals(
            listOf(Lock(NOTES, LABEL, VERSION, 30, IconTreatment.Gray)),
            locks,
        )
        assertTrue(locks.single().isAvailable)
    }

    /**
     * FR-020, and the constitution's null-`getLaunchIntentForPackage()` obligation: the row is
     * produced, not dropped and not thrown over — the home screen may still carry its icon.
     */
    @Test
    fun `an unresolvable package becomes an unavailable row and does not throw`() = runBlocking {
        val locks = assembleLocks(
            packages = listOf(NOTES),
            loadConfig = { DelayConfig(15, IconTreatment.Invert) },
            resolveTarget = { null },
        )

        val row = locks.single()
        assertNull(row.label)
        assertFalse(row.isAvailable)
        // Still the user's values: a reinstall must not silently lose what they chose.
        assertEquals(15, row.delaySeconds)
        assertEquals(IconTreatment.Invert, row.treatment)
        assertEquals(NOTES, row.packageName)
    }

    /** FR-032: a recorded package with nothing configured reads as [DelayConfig.DEFAULT]. */
    @Test
    fun `a package with no stored configuration reads as the default`() = runBlocking {
        val locks = assembleLocks(
            packages = listOf(NOTES),
            loadConfig = { DelayConfig.DEFAULT },
            resolveTarget = { AppTarget(it, LABEL, VERSION) },
        )

        assertEquals(DelayConfig.DEFAULT_SECONDS, locks.single().delaySeconds)
        assertEquals(IconTreatment.entries.first(), locks.single().treatment)
    }

    /**
     * FR-006: row order is the lock record's order and nothing re-sorts it. A row that moved would
     * look to the user like the lock had been re-made.
     */
    @Test
    fun `row order matches the lock list order`() = runBlocking {
        val packages = listOf(NOTES, MAIL, PHOTOS)

        val locks = assembleLocks(
            packages = packages,
            loadConfig = { DelayConfig.DEFAULT },
            // Mail is gone; the gap must not close and the order must not change.
            resolveTarget = { if (it == MAIL) null else AppTarget(it, LABEL, VERSION) },
        )

        assertEquals(packages, locks.map { it.packageName })
    }

    /**
     * FR-016: the latch. There is no path back to `loaded = false`, so no `ON_START` can blank the
     * screen on the way back from the flow.
     */
    @Test
    fun `a second read never returns loaded to false`() = runBlocking {
        val first = LocksUiState().withLocks(
            assembleLocks(listOf(NOTES), { DelayConfig.DEFAULT }, { AppTarget(it, LABEL, VERSION) }),
        )
        val second = first.withLocks(assembleLocks(emptyList(), ::noConfig, ::noTarget))

        assertTrue(first.loaded)
        assertTrue(second.loaded)
        assertTrue(second.showsIntro)
    }

    private companion object {
        const val NOTES = "com.example.notes"
        const val MAIL = "com.example.mail"
        const val PHOTOS = "com.example.photos"
        const val LABEL = "Notes"
        const val VERSION = 42L

        @Suppress("UNUSED_PARAMETER")
        fun noConfig(packageName: String): DelayConfig =
            error("an empty lock list must ask for no configuration")

        @Suppress("UNUSED_PARAMETER")
        fun noTarget(packageName: String): AppTarget? =
            error("an empty lock list must resolve nothing")
    }
}
