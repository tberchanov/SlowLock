package com.slowlock.feature.shortcut.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.DelayConfigRepository
import com.slowlock.feature.shortcut.domain.ElapsedClock
import com.slowlock.feature.shortcut.domain.deadlineFrom
import com.slowlock.feature.shortcut.domain.remainingMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Resolve, read, wait, decide — the delay behaviour behind the wait screen (FR-022, research R10).
 *
 * Four rules it must preserve:
 *
 * - The anchor is taken before anything is read (W3), so neither the package-manager lookup nor the
 *   configuration read can push the hand-off later than the delay the user chose.
 * - A rotation must not restart the wait (W4, FR-027). The coroutine lives in `viewModelScope`,
 *   which survives a rotation, so there is nothing to restore because nothing was torn down; the
 *   deadline in [SavedStateHandle] covers process death.
 * - Abandonment must still cancel. `onStop` finishes the activity unless the configuration is
 *   changing, and finishing clears this holder, cancelling the `delay` in flight.
 * - A second tap of the same target changes nothing (W22); a different target re-anchors (W23).
 *
 * Not here: the `STARTED` race guard and the launch itself, which need the activity's own lifecycle
 * and a platform `Intent`. This holder decides *that* the hand-off is due; the window performs it.
 */
@HiltViewModel
class WaitViewModel @Inject constructor(
    private val targets: AppTargetRepository,
    private val config: DelayConfigRepository,
    private val clock: ElapsedClock,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _events = Channel<WaitEvent>(Channel.BUFFERED)

    /**
     * One-shot outcomes, consumed exactly once (Constitution IV). A `StateFlow` with a sentinel
     * would re-deliver a hand-off on every recomposition — here, launching the target twice.
     */
    val events: Flow<WaitEvent> = _events.receiveAsFlow()

    private var waitJob: Job? = null

    /** The target currently being waited on, or `null` before the first [start]. */
    val targetPackage: String? get() = savedState[KEY_TARGET]

    /**
     * Starts — or resumes — the wait for [target].
     *
     * Resumption is what makes a rotation invisible: the anchor and deadline are read back from
     * [SavedStateHandle], so a recreated activity continues the wait in flight. A recomputed
     * `now + delay` here is precisely the bug FR-027 was written against.
     *
     * A repeat naming the same target is ignored outright (W22): a tap that visibly reset the timer
     * would teach the user that tapping does something.
     */
    fun start(target: String) {
        // A rotation lands here with the same target and a holder that never died, so the wait in
        // flight is left alone. After process death `waitJob` is null and the wait restarts against
        // the restored anchor and deadline, never a fresh `now + delay` (W4, FR-027).
        if (target == targetPackage && waitJob != null) return

        if (target != targetPackage) {
            // W23: a different target is a different wait. The deadline in flight belongs to an
            // app the user is no longer asking for.
            savedState[KEY_TARGET] = target
            savedState[KEY_ANCHOR] = clock.nowMillis()
            savedState[KEY_DEADLINE] = null
        } else if (savedState.get<Long>(KEY_ANCHOR) == null) {
            savedState[KEY_ANCHOR] = clock.nowMillis()
        }

        waitJob?.cancel()
        waitJob = viewModelScope.launch { run(target) }
    }

    private suspend fun run(target: String) {
        // W5: resolve before waiting. Never make someone sit through a delay for an app that was
        // already gone when they tapped.
        if (targets.resolve(target) == null) {
            _events.send(WaitEvent.Unavailable)
            return
        }

        // W6, W7: the delay comes off disk underneath the visible screen. There is no
        // "unconfigured" branch — `load` answers with the default, which is the whole of FR-032.
        val anchor = savedState.get<Long>(KEY_ANCHOR) ?: clock.nowMillis()
        val deadline = savedState.get<Long>(KEY_DEADLINE)
            ?: deadlineFrom(anchor, config.load(target).delaySeconds)
                .also { savedState[KEY_DEADLINE] = it }

        delay(remainingMillis(deadline, clock.nowMillis()))

        // The hand-off is due; whether it happens is not decided here. The window re-resolves,
        // checks it is still visible, and starts the target.
        _events.send(WaitEvent.HandOff(target))
    }

    private companion object {
        const val KEY_TARGET = "com.slowlock.wait.TARGET_PACKAGE"
        const val KEY_ANCHOR = "com.slowlock.wait.ANCHOR_ELAPSED_MILLIS"
        const val KEY_DEADLINE = "com.slowlock.wait.DEADLINE_ELAPSED_MILLIS"
    }
}

/**
 * What the wait has decided. Each is delivered once and acted on by the window, and carries a
 * package name rather than an `Intent` — a platform type has no place crossing out of a holder.
 */
sealed interface WaitEvent {

    /** The wait is over and the target should be started, if the screen is still visible. */
    data class HandOff(val packageName: String) : WaitEvent

    /**
     * The target could not be resolved, before the wait or after it. Saying so is the one thing
     * this path may add beyond the wait message, because the alternative is a tap that appears
     * inert.
     */
    data object Unavailable : WaitEvent
}
