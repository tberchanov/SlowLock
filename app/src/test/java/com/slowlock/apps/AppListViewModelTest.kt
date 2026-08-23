package com.slowlock.apps

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The one ViewModel path the constitution requires as a unit test: a package that no longer
 * resolves must never be handed across the selection seam.
 *
 * The `resolveLaunchIntent` seam is what makes this testable without a device — no
 * `PackageManager`, no emulator, no instrumented suite. Everything else about tapping a row
 * is observable on screen and is covered by `manual-test-plan.md` T1.12 and T2.8 instead.
 */
class AppListViewModelTest {

    /** FR-014, `selection-handoff.md` obligation P2: an unresolvable package is not handed off. */
    @Test
    fun `a package that does not resolve is not handed off`() {
        val viewModel = AppListViewModel(
            app = Application(),
            savedState = SavedStateHandle(),
            unavailableMessage = UNAVAILABLE,
            resolveLaunchIntent = { null },
        )
        var handedOff: String? = null

        viewModel.onAppTapped("com.example.uninstalled") { handedOff = it }

        assertNull(handedOff)
        assertNotNull(viewModel.uiState.value.unavailableAppMessage)
    }

    private companion object {
        const val UNAVAILABLE = "That app is no longer available."
    }
}
