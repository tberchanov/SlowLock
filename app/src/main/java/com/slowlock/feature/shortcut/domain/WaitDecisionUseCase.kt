package com.slowlock.feature.shortcut.domain

import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.DelayConfigRepository
import javax.inject.Inject

/**
 * Whether a wait is owed, and how much of it is left.
 *
 * Three rules, in this order:
 *
 * - **Resolve first** (W5). An app that was already gone when the icon was tapped must not cost the
 *   user the full delay before anything says so.
 * - **The deadline is established once.** A deadline handed in wins over one computed here, which
 *   is what makes a wait survive process death: recomputing `now + delay` on the way back is
 *   precisely the bug FR-027 was written against.
 * - **What remains is measured against the clock**, never assumed to be the whole delay.
 *
 * The stored deadline arrives as a plain `Long?` rather than through a `SavedStateHandle`: where it
 * was kept is the screen's business, and taking it as a parameter is what puts the rule that decides
 * on it in reach of the JVM suite.
 */
class WaitDecisionUseCase @Inject constructor(
    private val targets: AppTargetRepository,
    private val config: DelayConfigRepository,
    private val clock: ElapsedClock,
) {

    /**
     * @param anchorMillis when the wait began, on [ElapsedClock]'s scale. Taken by the caller
     *   *before* this is called, so neither the resolution nor the configuration read can push the
     *   hand-off later than the delay the user chose.
     * @param storedDeadlineMillis a deadline already established for this target, or `null` for a
     *   wait that is starting now.
     */
    suspend operator fun invoke(
        target: String,
        anchorMillis: Long,
        storedDeadlineMillis: Long?,
    ): WaitDecision {
        if (targets.resolve(target) == null) return WaitDecision.Unavailable

        // No "unconfigured" branch: `load` answers with the default, which is the whole of FR-032.
        val deadline = storedDeadlineMillis
            ?: deadlineFrom(anchorMillis, config.load(target).delaySeconds)

        return WaitDecision.Wait(deadline, remainingMillis(deadline, clock.nowMillis()))
    }
}

/** What the wait owes. Neither case is an error; both are ordinary outcomes of a tap. */
sealed interface WaitDecision {

    /** The target could not be resolved. There is nothing to wait for. */
    data object Unavailable : WaitDecision

    /**
     * The wait is owed until [deadlineMillis], of which [remainingMillis] is left — never negative,
     * so it can go straight to `delay()`.
     *
     * [deadlineMillis] is returned rather than kept, because the caller is the only one that can
     * store it across process death.
     */
    data class Wait(val deadlineMillis: Long, val remainingMillis: Long) : WaitDecision
}
