package com.slowlock.feature.shortcut.ui

import androidx.lifecycle.SavedStateHandle
import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.DelayConfig
import com.slowlock.core.domain.DelayConfigRepository
import com.slowlock.core.domain.IconTreatment
import com.slowlock.feature.shortcut.domain.ElapsedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The wait's deadline behaviour, driven on virtual time: `WaitTimingTest` covers the arithmetic,
 * this covers that the hand-off is *withheld* until the deadline and delivered once when it arrives
 * (FR-039).
 *
 * `setMain` with a [StandardTestDispatcher] puts the wait on the test's own scheduler, where
 * `delay` consumes virtual time. Nothing here sleeps.
 *
 * The clock is injected separately, because [ElapsedClock] answers `SystemClock.elapsedRealtime()`
 * in production and the test scheduler knows nothing about it. The fake below keeps the two in
 * step.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WaitViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /**
     * FR-022, W6/W7: nothing is handed off before the configured delay is up, and it is handed off
     * once when it is. The assertion one millisecond short is the point — without it, an
     * implementation that never waited at all would pass the second half just as happily.
     */
    @Test
    fun `the hand-off waits for the configured delay and then arrives once`() = runTest(dispatcher) {
        val savedState = SavedStateHandle()
        val viewModel = viewModel(savedState, delaySeconds = 10)
        val events = collectEvents(viewModel)

        viewModel.start(NOTES)

        advanceTimeBy(10_000L)
        assertEquals("the wait ended early", emptyList<WaitEvent>(), events.received)

        advanceTimeBy(1L)
        assertEquals(listOf(WaitEvent.HandOff(NOTES)), events.received)

        // And it stays exactly one: the channel is consume-once, and the wait does not repeat.
        advanceTimeBy(60_000L)
        assertEquals(listOf(WaitEvent.HandOff(NOTES)), events.received)
        events.stop()
    }

    /**
     * W5: resolve before waiting. An app that was already gone when the icon was tapped must not
     * cost the user the full delay before it says so.
     */
    @Test
    fun `an unresolvable target reports unavailable without waiting`() = runTest(dispatcher) {
        val viewModel = viewModel(SavedStateHandle(), delaySeconds = 30, resolves = false)
        val events = collectEvents(viewModel)

        viewModel.start(NOTES)
        advanceTimeBy(1L)

        assertEquals(listOf(WaitEvent.Unavailable), events.received)
        events.stop()
        assertTrue("no delay should have been scheduled", currentTime < 30_000L)
    }

    /**
     * FR-027, W4: a rotation must not restart the wait. The holder survives a configuration change,
     * so the recreated activity calls [start] again with the same target and the wait in flight is
     * left alone — the hand-off still lands at the original deadline.
     */
    @Test
    fun `a repeat start for the same target does not extend the wait`() = runTest(dispatcher) {
        val viewModel = viewModel(SavedStateHandle(), delaySeconds = 10)
        val events = collectEvents(viewModel)

        viewModel.start(NOTES)
        advanceTimeBy(6_000L)
        viewModel.start(NOTES)

        advanceTimeBy(4_000L)
        assertEquals("the repeat tap restarted the wait", emptyList<WaitEvent>(), events.received)

        advanceTimeBy(1L)
        assertEquals(listOf(WaitEvent.HandOff(NOTES)), events.received)
        events.stop()
    }

    /**
     * FR-027 across process death: the deadline is restored, not recomputed. The second holder is a
     * genuinely new object, but it reads the deadline the first one stored — six seconds were
     * served, so four remain. A recomputed `now + delay` would give ten, and the user would wait
     * sixteen.
     */
    @Test
    fun `a restored deadline resumes rather than restarting`() = runTest(dispatcher) {
        val savedState = SavedStateHandle()
        val first = viewModel(savedState, delaySeconds = 10)
        first.start(NOTES)
        advanceTimeBy(6_000L)

        val second = viewModel(savedState, delaySeconds = 10)
        val events = collectEvents(second)
        second.start(NOTES)

        advanceTimeBy(4_000L)
        assertEquals("the restored wait restarted", emptyList<WaitEvent>(), events.received)

        advanceTimeBy(1L)
        assertEquals(listOf(WaitEvent.HandOff(NOTES)), events.received)
        events.stop()
    }

    /** W23: a different target is a different wait, re-anchored from the moment of the new tap. */
    @Test
    fun `a different target re-anchors the wait`() = runTest(dispatcher) {
        val viewModel = viewModel(SavedStateHandle(), delaySeconds = 10)
        val events = collectEvents(viewModel)

        viewModel.start(NOTES)
        advanceTimeBy(6_000L)
        viewModel.start(MAIL)

        advanceTimeBy(4_001L)
        assertEquals("the new target inherited the old deadline", emptyList<WaitEvent>(), events.received)

        advanceTimeBy(6_000L)
        assertEquals(listOf(WaitEvent.HandOff(MAIL)), events.received)
        events.stop()
    }

    private fun TestScope.viewModel(
        savedState: SavedStateHandle,
        delaySeconds: Int,
        resolves: Boolean = true,
    ) = WaitViewModel(
        targets = FakeTargets(resolves),
        config = FakeConfig(delaySeconds),
        // Virtual time is the elapsed-realtime clock here, so the deadline arithmetic and
        // `advanceTimeBy` talk about the same milliseconds.
        clock = ElapsedClock { currentTime },
        savedState = savedState,
    )

    /** A running collector of [WaitViewModel.events] and what it has received so far. */
    private class Events(private val collector: Job) {
        val received = mutableListOf<WaitEvent>()
        fun stop() = collector.cancel()
    }

    /**
     * Starts collecting [WaitViewModel.events]. A collector on a channel never completes on its
     * own, so [Events.stop] is what lets `runTest` finish rather than waiting for it forever.
     */
    private fun TestScope.collectEvents(viewModel: WaitViewModel): Events {
        lateinit var events: Events
        val collector = launch { viewModel.events.collect { events.received += it } }
        events = Events(collector)
        return events
    }

    private class FakeTargets(private val resolves: Boolean) : AppTargetRepository {
        override suspend fun resolve(packageName: String): AppTarget? =
            if (resolves) AppTarget(packageName, LABEL, VERSION) else null
    }

    /** Answers the same delay for every package; which one was asked is not asserted. */
    private class FakeConfig(private val delaySeconds: Int) : DelayConfigRepository {
        override suspend fun load(packageName: String) =
            DelayConfig(delaySeconds, IconTreatment.Original)

        override suspend fun save(packageName: String, config: DelayConfig) =
            error("the wait path must never write")
    }

    private companion object {
        const val NOTES = "com.example.notes"
        const val MAIL = "com.example.mail"
        const val LABEL = "Notes"
        const val VERSION = 42L
    }
}
