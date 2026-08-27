package com.slowlock.feature.apps.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import com.slowlock.R
import com.slowlock.feature.apps.domain.FilterAppsUseCase
import com.slowlock.feature.apps.domain.InstalledApp
import com.slowlock.feature.apps.domain.InstalledAppsRepository
import com.slowlock.feature.apps.domain.LoadInstalledAppsUseCase
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.CurrentLocale
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The one ViewModel path the constitution requires as a unit test: a package that no longer
 * resolves must never be handed across the selection seam. Everything else about tapping a row is
 * observable on screen and is covered by `manual-test-plan.md`.
 *
 * `viewModelScope` dispatches on `Dispatchers.Main`, which a JVM test has no main looper for, so
 * `setMain` with a test dispatcher is what makes the tap path reachable now that resolution
 * suspends.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** FR-014, `selection-handoff.md` obligation P2: an unresolvable package is not handed off. */
    @Test
    fun `a package that does not resolve is not handed off`() = runTest(dispatcher) {
        val viewModel = viewModel(resolves = null)
        val messages = collectMessages(viewModel)
        var handedOff: String? = null

        viewModel.onAppTapped(UNINSTALLED) { handedOff = it }
        testScheduler.advanceUntilIdle()

        assertNull("an unresolvable package must not cross the seam", handedOff)
        assertEquals(
            "the user is told, or the tap appears to do nothing",
            listOf(R.string.app_list_unavailable),
            messages.received,
        )
        messages.stop()
    }

    /**
     * The dead row is dropped so it cannot be tapped a second time (FR-014). Asserted separately
     * from the hand-off: one is about what the caller is told, the other about what the list
     * offers.
     */
    @Test
    fun `an unresolvable package is removed from the list`() = runTest(dispatcher) {
        val viewModel = viewModel(resolves = null, installed = listOf(app(UNINSTALLED), app(NOTES)))
        viewModel.refresh()
        testScheduler.advanceUntilIdle()

        viewModel.onAppTapped(UNINSTALLED) {}
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(NOTES), viewModel.uiState.value.apps.map { it.packageName })
    }

    /** And the ordinary path, so the two tests above are not passing vacuously. */
    @Test
    fun `a package that resolves is handed off and raises no message`() = runTest(dispatcher) {
        val viewModel = viewModel(resolves = AppTarget(NOTES, LABEL, VERSION))
        val messages = collectMessages(viewModel)
        var handedOff: String? = null

        viewModel.onAppTapped(NOTES) { handedOff = it }
        testScheduler.advanceUntilIdle()

        assertEquals(NOTES, handedOff)
        assertEquals(emptyList<Int>(), messages.received)
        messages.stop()
    }

    /**
     * FR-038: the message is delivered once per tap and cannot be re-read — the property the
     * `StateFlow` sentinel could not give without a manual clear. The second collector is the
     * recomposition that used to re-show the snackbar; it arrives after the value was taken.
     */
    @Test
    fun `an unavailable message is delivered once and cannot be re-read`() = runTest(dispatcher) {
        val viewModel = viewModel(resolves = null)

        val first = mutableListOf<Int>()
        val firstCollector = launch { viewModel.messages.collect { first += it } }
        viewModel.onAppTapped(UNINSTALLED) {}
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(R.string.app_list_unavailable), first)
        firstCollector.cancel()

        // The screen leaving and re-entering composition, which is when the sentinel used to
        // re-fire. The consumed value is gone, so there is nothing to see.
        val second = mutableListOf<Int>()
        val secondCollector = launch { viewModel.messages.collect { second += it } }
        testScheduler.advanceUntilIdle()
        assertEquals("a consumed event must not be delivered again", emptyList<Int>(), second)
        secondCollector.cancel()
    }

    /** FR-017: a refresh over a populated list leaves the rows up, not a spinner. */
    @Test
    fun `a completed load clears the loading flag and keeps the rows`() = runTest(dispatcher) {
        val viewModel = viewModel(resolves = null, installed = listOf(app(NOTES)))

        viewModel.refresh()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isPopulated)
        assertEquals(listOf(NOTES), state.apps.map { it.packageName })
    }

    private fun viewModel(
        resolves: AppTarget?,
        installed: List<InstalledApp> = emptyList(),
    ) = AppListViewModel(
        loadInstalledApps = LoadInstalledAppsUseCase(
            apps = FakeInstalledApps(installed),
            ownPackage = "com.slowlock",
            locale = CurrentLocale { Locale.ENGLISH },
        ),
        filterApps = FilterAppsUseCase(),
        targets = FakeTargets(resolves),
        icons = FakeIcons,
        savedState = SavedStateHandle(),
    )

    private fun app(packageName: String) = InstalledApp(packageName, packageName, VERSION)

    /**
     * A running collector of [AppListViewModel.messages] and what it has received so far. A channel
     * collector never completes on its own, so without [stop] `runTest` waits for it forever.
     */
    private class Messages(private val collector: Job) {
        val received = mutableListOf<Int>()
        fun stop() = collector.cancel()
    }

    /**
     * Starts collecting [AppListViewModel.messages] into a list that later assertions read. It must
     * start *before* the action under test: a channel delivers to whoever is receiving.
     */
    private fun TestScope.collectMessages(viewModel: AppListViewModel): Messages {
        lateinit var messages: Messages
        val collector = launch { viewModel.messages.collect { messages.received += it } }
        messages = Messages(collector)
        return messages
    }

    /**
     * The real use case runs over this, rather than a fake of it: the holder's contract is what
     * lands in state after a load, and substituting the use case would assert the substitution.
     * What the use case itself decides is covered by [com.slowlock.feature.apps.domain.LoadInstalledAppsUseCaseTest].
     */
    private class FakeInstalledApps(private val apps: List<InstalledApp>) : InstalledAppsRepository {
        override suspend fun load(): List<InstalledApp> = apps
    }

    /** Answers the same way for every package, which is all this seam is asked to decide here. */
    private class FakeTargets(private val target: AppTarget?) : AppTargetRepository {
        override suspend fun resolve(packageName: String): AppTarget? = target
    }

    /** Icons are never asserted on: they do not travel in state and no branch depends on them. */
    private object FakeIcons : AppIconRepository {
        override suspend fun icon(packageName: String, versionCode: Long): ImageBitmap? = null
        override suspend fun sweep(keep: List<String>) = Unit
    }

    private companion object {
        const val UNINSTALLED = "com.example.uninstalled"
        const val NOTES = "com.example.notes"
        const val LABEL = "Notes"
        const val VERSION = 42L
    }
}
