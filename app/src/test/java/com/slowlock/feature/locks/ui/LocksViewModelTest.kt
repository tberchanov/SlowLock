package com.slowlock.feature.locks.ui

import com.slowlock.core.domain.IconTreatment
import com.slowlock.feature.locks.domain.Lock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The latch, which is the one thing [LocksUiState] decides.
 *
 * What a row is and how the list is derived moved to
 * [com.slowlock.feature.locks.domain.LoadLocksUseCaseTest] — they are rules, and they are now
 * reachable through the composition that actually runs rather than through the pure functions
 * underneath it. What is left here is state shape, which is why nothing below constructs a
 * [LocksViewModel]: `viewModelScope` dispatches on `Dispatchers.Main`.
 */
class LocksViewModelTest {

    /** FR-017: no locks is the intro's condition, and it is derived, never stored. */
    @Test
    fun `an empty lock list is the intro condition`() {
        val state = LocksUiState().withLocks(emptyList())

        assertTrue(state.loaded)
        assertTrue(state.showsIntro)
        assertFalse(state.showsLocks)
    }

    /**
     * FR-016: the latch. There is no path back to `loaded = false`, so no `ON_START` can blank the
     * screen on the way back from the flow.
     */
    @Test
    fun `a second read never returns loaded to false`() {
        val first = LocksUiState().withLocks(listOf(lock()))
        val second = first.withLocks(emptyList())

        assertTrue(first.loaded)
        assertTrue(second.loaded)
        assertTrue(second.showsIntro)
        assertFalse(second.showsLocks)
    }

    private fun lock() = Lock(
        packageName = "com.example.notes",
        label = "Notes",
        versionCode = 42L,
        delaySeconds = 30,
        treatment = IconTreatment.entries.first(),
    )
}
