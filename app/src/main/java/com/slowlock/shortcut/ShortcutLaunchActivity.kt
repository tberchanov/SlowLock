package com.slowlock.shortcut

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.slowlock.R
import com.slowlock.delay.DelayConfigStore
import com.slowlock.delay.WaitScreen
import com.slowlock.delay.deadlineFrom
import com.slowlock.delay.remainingMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The activity every pinned shortcut points at. **Its fully-qualified name is frozen** —
 * see [ShortcutContract.LAUNCH_ACTIVITY] and `contracts/pinned-shortcut.md`.
 *
 * Do not rename this class, do not move it to another package, and do not change its package
 * declaration. The FQN is written into the persisted intent of every shortcut already on every
 * home screen; changing it compiles clean, pins *new* shortcuts at the new name, and kills all
 * the old ones silently at the moment the user taps them. `ShortcutContractTest` asserts the
 * runtime name against the frozen constant so that failure lands in `./gradlew test` instead.
 * If a rename ever becomes unavoidable the remedy is an `<activity-alias>` under the old name,
 * never a re-pin.
 *
 * What this activity *does* is deliberately not frozen, and this feature is the return on that:
 * it now **waits** before it hands off (`contracts/wait-screen.md`). Every shortcut pinned by
 * feature 002 picked the wait up with nothing asked of the user and nothing re-pinned (FR-011,
 * FR-032) — the frozen half of the shortcut contract is what bought that.
 *
 * It is now a screen, where feature 002's version drew nothing. The invisible theme, the
 * starting-window suppression and `noHistory` are gone; `excludeFromRecents`, the empty
 * `taskAffinity`, `FLAG_ACTIVITY_NEW_TASK` and finishing at hand-off are what still keep a
 * SlowLock entry out of recents and the wait screen out from behind the target (FR-031,
 * obligations W19–W21, research.md R8).
 *
 * **The design is subtractive and the absences are load-bearing.** No countdown, no progress, no
 * sound, no back handler, nothing tappable — see [WaitScreen] for the composable's half of that
 * and `contracts/wait-screen.md` W8–W14 for the whole list.
 *
 * Nothing here reads any in-process state, so a pinned shortcut works with SlowLock force-stopped
 * and after a reboot (FR-033, obligations W24 and 002's L6). The delay comes off disk through
 * [DelayConfigStore], and the target comes off the intent. There is nothing to restore and
 * nothing to keep running.
 *
 * **A wait is bound to being visible.** [onStop] finishes the activity unless the stop is a
 * configuration change, and that single rule is the whole of FR-029 (W15, research.md R5). The
 * `STARTED` re-check in [handOff] closes the one gap it leaves — the instant where the deadline
 * expires and the user leaves at the same time (W16).
 */
class ShortcutLaunchActivity : ComponentActivity() {

    private val store by lazy { DelayConfigStore(this) }

    /**
     * When the wait was anchored, on the elapsed-realtime clock (W3).
     *
     * Captured before the target is even read, so neither the package-manager lookup nor the
     * configuration read can push the hand-off later than the delay the user chose. Because the
     * anchor is still later than their tap, the observed wait is never *shorter* than the delay
     * either (FR-037, research.md R4).
     */
    private var anchorElapsedMillis: Long = 0L

    /** Null until the configuration read completes and the delay is known. */
    private var deadlineElapsedMillis: Long? = null

    private var targetPackage: String? = null

    private var waitJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // W3, W4: the first thing that happens, and restored in preference to a fresh anchor.
        // A rotation destroys and recreates this activity; one that recomputed `now + delay`
        // would silently restart the wait, which is the bug FR-027 is written against.
        anchorElapsedMillis =
            savedInstanceState?.takeIf { it.containsKey(KEY_ANCHOR) }?.getLong(KEY_ANCHOR)
                ?: SystemClock.elapsedRealtime()
        deadlineElapsedMillis =
            savedInstanceState?.takeIf { it.containsKey(KEY_DEADLINE) }?.getLong(KEY_DEADLINE)

        // W1: the target comes from the extra and from nowhere else — not the intent's data,
        // not its action, not its component (002 L1, Constitution V).
        val target = intent?.getStringExtra(ShortcutContract.EXTRA_TARGET_PACKAGE)

        // W2: a shortcut carrying no extra is treated exactly like an uninstalled target.
        // Reported and finished before anything is drawn, so there is no wait and no flash.
        if (target == null) {
            Log.w(TAG, "Shortcut carried no target package")
            reportUnavailable()
            finish()
            return
        }

        // W6: the first frame owes nothing to the disk. The screen shows the same fixed text for
        // every app and every delay, so it has nothing to wait for and the read happens
        // underneath it (FR-022, research.md R3).
        setContent { WaitScreen() }

        // W13: a window flag, never a PowerManager wake lock. Without it any delay longer than
        // the device's screen timeout is unreachable — the display sleeps, R5 abandons the wait,
        // and a 30 s delay could never complete on a phone that sleeps at 15. Scoped to this it,
        // window and released with it, which is the whole of what Constitution IV v1.1.0
        // permits (research.md R6).
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        startWait(target)
    }

    /**
     * Obligations W22 and W23: what a second tap does.
     *
     * `launchMode="singleTop"` routes a repeat tap here instead of creating a second activity.
     * A repeat naming the **same** target is ignored outright — no restart, no extension, no
     * second wait (FR-027). Impatience is the exact state this feature exists to sit with, and a
     * tap that visibly reset the timer would teach the user that tapping does something.
     *
     * A *different* target is a different wait: re-anchor and start over for that app, because
     * the deadline already in flight belongs to an app the user is no longer asking for.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val target = intent.getStringExtra(ShortcutContract.EXTRA_TARGET_PACKAGE)

        // W22. Also covers a repeat that somehow carries no extra: there is a wait already
        // running for a target that is still valid, and nothing here is a reason to disturb it.
        if (target == null || target == targetPackage) return

        // W23.
        anchorElapsedMillis = SystemClock.elapsedRealtime()
        deadlineElapsedMillis = null
        startWait(target)
    }

    /**
     * W4: carry the wait across a configuration change.
     *
     * The anchor is saved as well as the deadline, because a rotation in the window between
     * `onCreate` and the configuration read completing would otherwise land on a recreated
     * activity with no deadline to restore and re-anchor a full fresh delay.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_ANCHOR, anchorElapsedMillis)
        deadlineElapsedMillis?.let { outState.putLong(KEY_DEADLINE, it) }
    }

    /**
     * Obligation W15, and **the whole of FR-029 in one line**.
     *
     * The requirement lists six ways to leave a wait — back, home, the app switcher, another app
     * taking over, the display timing out, and the device locking. It is not six rules: every one
     * of them arrives here, because `onStop` is precisely the moment this window stops being
     * visible (research.md R5). There is deliberately no `BackHandler` anywhere in the feature
     * (W14) for the same reason — the system's back finishes the activity, which lands here too.
     *
     * Finishing rather than merely cancelling the wait is what leaves nothing to come back to,
     * which is also FR-031: the abandonment is permanent, and re-tapping the icon starts a fresh
     * full-length wait rather than resuming a part-served one. That is the "pocket the phone"
     * bypass the clarification closed.
     *
     * **The [isChangingConfigurations] exception is not optional.** A rotation passes through
     * `onStop` too, and finishing there would restart the wait on every rotation — the exact bug
     * FR-027 was written against. The single line and its single exception are also why
     * `android:noHistory` had to come off the manifest entry: it delivers this behaviour for
     * free, but with no exception for a configuration change and nothing here for a reviewer to
     * read (R5).
     *
     * The cancellation itself costs nothing: `finish()` destroys the activity, which cancels
     * [lifecycleScope] and with it the `delay` in flight. There is no timer to stop.
     */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) finish()
    }

    /**
     * Resolve, read, wait, hand off — in that order, with only the first two able to fail.
     *
     * `lifecycleScope` is what makes every abandonment path free: the coroutine is cancelled when
     * the activity is destroyed, so there is no timer to stop and no bookkeeping to unwind
     * (research.md R4). One suspension point, nothing ticking — which is what a screen with
     * nothing to update should cost.
     */
    private fun startWait(target: String) {
        targetPackage = target
        waitJob?.cancel()
        waitJob = lifecycleScope.launch {
            // W5: resolve before waiting. Never make someone sit through a delay for an app that
            // was already gone when they tapped.
            if (resolveLaunchIntent(target) == null) {
                reportUnavailable()
                finish()
                return@launch
            }

            // W6, W7: the delay comes off disk on Dispatchers.IO, underneath the visible screen.
            // There is no "unconfigured" branch here — `load` answers with the default, which is
            // the whole of FR-032 (`delay-config-store.md` S6).
            val deadline = deadlineElapsedMillis
                ?: deadlineFrom(anchorElapsedMillis, store.load(target).delaySeconds)
                    .also { deadlineElapsedMillis = it }

            delay(remainingMillis(deadline, SystemClock.elapsedRealtime()))

            handOff(target)
        }
    }

    /**
     * Obligations W16–W21: re-resolve, check we are still visible, start, finish.
     *
     * Re-resolution at the hand-off rather than at the anchor is not belt-and-braces: the target
     * can be uninstalled *during* the wait, and on a two-minute delay that is a real window
     * (FR-030, 002 L2).
     *
     * `FLAG_ACTIVITY_NEW_TASK` is required because this activity declares an empty `taskAffinity`
     * and is about to finish; it also gives the target its own task, so backing out of the target
     * does not unwind into the wait screen (FR-031).
     *
     * The process is never killed by hand (W21, research.md R8): that would abort the
     * `SharedPreferences` write queue mid-flight, and an empty process the system reclaims on
     * demand is not something the user can see.
     */
    private suspend fun handOff(target: String) {
        val launch = resolveLaunchIntent(target)?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        if (launch == null) {
            // W18: never crash. The user waited for an app that is no longer there.
            reportUnavailable()
            finish()
            return
        }

        // W16, the race guard. [onStop] finishing the activity cancels [lifecycleScope], and
        // every suspension point above — both `withContext` calls — is a cancellation check, so
        // a user who left seconds ago never reaches this line. What it catches is narrower and
        // real: the deadline expiring in the *same instant* the user presses home. The
        // continuation is already queued on the main thread by then, and cancellation cannot
        // unqueue it. Starting the target from here would launch an app the user is no longer
        // looking at, from a stopping activity — the background start Constitution IV forbids.
        //
        // Abandon silently. No toast: nothing failed, and the user is not here to read it
        // (contrast W18 above, which answers a user who waited and is still watching).
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Log.i(TAG, "Wait for $target completed after the screen was left; abandoning")
            finish()
            return
        }

        // Resolving is not a guarantee: the target can be uninstalled between the two calls, and
        // a disabled or locked-profile app can throw. Caught, never crashed (002 L3).
        runCatching { startActivity(launch) }.onFailure {
            Log.w(TAG, "Could not start $target", it)
            reportUnavailable()
        }

        finish()
    }

    /** The package-manager lookup, off the main thread (Constitution IV, FR-036). */
    private suspend fun resolveLaunchIntent(target: String): Intent? = withContext(Dispatchers.IO) {
        runCatching { packageManager.getLaunchIntentForPackage(target) }.getOrNull()
    }

    /**
     * The one thing this activity is allowed to say beyond the wait message, and only because it
     * replaces a tap that would otherwise appear to do nothing at all (FR-030).
     *
     * A toast rather than a screen: there is nothing here for the user to act on, and it outlives
     * the activity because the system shows it, not this window.
     */
    private fun reportUnavailable() {
        Toast.makeText(this, R.string.shortcut_launch_unavailable, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val TAG = "SlowLock"
        const val KEY_ANCHOR = "com.slowlock.wait.ANCHOR_ELAPSED_MILLIS"
        const val KEY_DEADLINE = "com.slowlock.wait.DEADLINE_ELAPSED_MILLIS"
    }
}
