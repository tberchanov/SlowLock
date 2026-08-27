package com.slowlock.feature.shortcut.domain

import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.DelayConfig
import com.slowlock.core.domain.DelayConfigRepository
import com.slowlock.core.domain.IconTreatment
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a wait owes, asked directly.
 *
 * `WaitTimingTest` covers the arithmetic and `WaitViewModelTest` covers that the screen withholds
 * the hand-off over real (virtual) time. What is only reachable here is *which* inputs the decision
 * consults — that a restored deadline is honoured without going back to disk, and that an
 * unresolvable target is refused before the configuration is read at all (contract U12-U15).
 */
class WaitDecisionUseCaseTest {

    /**
     * U12: resolve first. Asserted by the configuration read *throwing* — an implementation that
     * loaded the delay before checking the target would fail here rather than merely being slower,
     * which is the difference between this and the holder's timing test.
     */
    @Test
    fun `an unresolvable target is refused before the configuration is read`() = runTest {
        val decision = useCase(resolves = false, config = ThrowingConfig)(
            target = NOTES,
            anchorMillis = 1_000L,
            storedDeadlineMillis = null,
        )

        assertEquals(WaitDecision.Unavailable, decision)
    }

    /**
     * U13: a stored deadline wins and the configuration is not consulted for it. This is the rule a
     * restored process depends on, and the throwing fake is what makes "not consulted" an assertion
     * rather than an assumption — a recomputed `now + delay` would have to read the delay first.
     */
    @Test
    fun `a stored deadline is honoured without reading the configuration`() = runTest {
        val decision = useCase(config = ThrowingConfig)(
            target = NOTES,
            anchorMillis = 1_000L,
            storedDeadlineMillis = 9_000L,
        )

        assertEquals(WaitDecision.Wait(deadlineMillis = 9_000L, remainingMillis = 4_000L), decision)
    }

    /**
     * U14: with no stored deadline the wait is anchored where the caller says, not where the clock
     * is now. The anchor is 1_000 and the clock is 5_000, so an implementation anchoring on `now`
     * would answer 15_000 rather than 11_000.
     */
    @Test
    fun `a fresh wait is anchored on the caller's anchor, not on now`() = runTest {
        val decision = useCase(delaySeconds = 10)(
            target = NOTES,
            anchorMillis = 1_000L,
            storedDeadlineMillis = null,
        )

        assertEquals(
            WaitDecision.Wait(deadlineMillis = 11_000L, remainingMillis = 6_000L),
            decision,
        )
    }

    /** U15: a deadline already past owes zero, never a negative that `delay()` would reject. */
    @Test
    fun `a deadline already past owes nothing rather than a negative`() = runTest {
        val decision = useCase()(
            target = NOTES,
            anchorMillis = 0L,
            storedDeadlineMillis = 2_000L,
        )

        assertEquals(WaitDecision.Wait(deadlineMillis = 2_000L, remainingMillis = 0L), decision)
        assertTrue((decision as WaitDecision.Wait).remainingMillis >= 0L)
    }

    private fun useCase(
        resolves: Boolean = true,
        delaySeconds: Int = 30,
        config: DelayConfigRepository = FakeConfig(delaySeconds),
        nowMillis: Long = NOW,
    ) = WaitDecisionUseCase(
        targets = FakeTargets(resolves),
        config = config,
        clock = ElapsedClock { nowMillis },
    )

    private class FakeTargets(private val resolves: Boolean) : AppTargetRepository {
        override suspend fun resolve(packageName: String): AppTarget? =
            if (resolves) AppTarget(packageName, LABEL, VERSION) else null
    }

    private class FakeConfig(private val delaySeconds: Int) : DelayConfigRepository {
        override suspend fun load(packageName: String) =
            DelayConfig(delaySeconds, IconTreatment.Original)

        override suspend fun save(packageName: String, config: DelayConfig) =
            error("the wait path must never write")
    }

    /** Turns "the configuration was not read" into a failing test rather than a silent pass. */
    private object ThrowingConfig : DelayConfigRepository {
        override suspend fun load(packageName: String): DelayConfig =
            error("the configuration must not be read on this path")

        override suspend fun save(packageName: String, config: DelayConfig) =
            error("the wait path must never write")
    }

    private companion object {
        const val NOTES = "com.example.notes"
        const val LABEL = "Notes"
        const val VERSION = 42L
        const val NOW = 5_000L
    }
}
