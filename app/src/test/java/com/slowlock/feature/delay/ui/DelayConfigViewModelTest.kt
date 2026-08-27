package com.slowlock.feature.delay.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.DelayConfig
import com.slowlock.core.domain.DelayConfigRepository
import com.slowlock.core.domain.IconTreatment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The one branch this holder adds that a test can get wrong (research R8): a delay restored from
 * the saved-state handle must not be overwritten by the value still on disk.
 *
 * Getting it backwards is silent — the user drags to 30, the process is killed, and the screen
 * comes back showing the 10 that was saved. Neither the compiler nor the device notices.
 *
 * `viewModelScope` dispatches on `Dispatchers.Main`, which a JVM test has no main looper for, so
 * `setMain` with a test dispatcher is what makes the load path reachable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DelayConfigViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** The ordinary first open: nothing has been edited, so the saved value is what shows. */
    @Test
    fun `an empty handle opens on the saved delay`() = runTest(dispatcher) {
        val viewModel = viewModel(SavedStateHandle(), saved = DelayConfig(30, IconTreatment.Gray))

        viewModel.start(NOTES)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(30, state.seconds)
        assertEquals(IconTreatment.Gray, state.treatment)
    }

    /**
     * Process death mid-edit. The read still happens — the treatment is not saved and re-reading it
     * gives the identical answer — but it must not supply the delay.
     */
    @Test
    fun `a restored delay is not overwritten by the saved one`() = runTest(dispatcher) {
        val viewModel = viewModel(
            SavedStateHandle(mapOf(DELAY to 45)),
            saved = DelayConfig(10, IconTreatment.Original),
        )

        viewModel.start(NOTES)
        testScheduler.advanceUntilIdle()

        assertEquals(
            "an edit made before the process died must not be replaced by the stale saved value",
            45,
            viewModel.uiState.value.seconds,
        )
        assertEquals(IconTreatment.Original, viewModel.uiState.value.treatment)
    }

    /**
     * The write side of the same branch: an edit has to reach the handle, or there is nothing for
     * the test above to restore.
     */
    @Test
    fun `an edited delay is mirrored into the handle`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val viewModel = viewModel(handle, saved = DelayConfig.DEFAULT)

        viewModel.onSecondsChanged(45)

        assertEquals(45, handle.get<Int>(DELAY))
        assertEquals(45, viewModel.uiState.value.seconds)
    }

    /** FR-002(c): nothing is shown until the read returns, so no default can flash. */
    @Test
    fun `the delay is withheld until the read returns`() = runTest(dispatcher) {
        val viewModel = viewModel(SavedStateHandle(), saved = DelayConfig(30, IconTreatment.Gray))

        assertEquals(false, viewModel.uiState.value.loaded)

        viewModel.start(NOTES)
        testScheduler.advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.loaded)
    }

    private fun viewModel(savedState: SavedStateHandle, saved: DelayConfig) = DelayConfigViewModel(
        targets = FakeTargets,
        icons = FakeIcons,
        config = FakeConfig(saved),
        savedState = savedState,
    )

    /** Resolution and the icon are not what this branch decides; both answer the same way. */
    private object FakeTargets : AppTargetRepository {
        override suspend fun resolve(packageName: String): AppTarget? =
            AppTarget(packageName, LABEL, VERSION)
    }

    private object FakeIcons : AppIconRepository {
        override suspend fun icon(packageName: String, versionCode: Long): ImageBitmap? = null
        override suspend fun sweep(keep: List<String>) = Unit
    }

    private class FakeConfig(private val stored: DelayConfig) : DelayConfigRepository {
        override suspend fun load(packageName: String): DelayConfig = stored
        override suspend fun save(packageName: String, config: DelayConfig) = Unit
    }

    private companion object {
        const val DELAY = "editedDelaySeconds"
        const val NOTES = "com.example.notes"
        const val LABEL = "Notes"
        const val VERSION = 42L
    }
}
