package com.slowlock.shortcut

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.slowlock.R
import com.slowlock.core.domain.IoDispatcher
import com.slowlock.feature.shortcut.domain.ShortcutContract
import com.slowlock.feature.shortcut.ui.WaitEvent
import com.slowlock.feature.shortcut.ui.WaitScreen
import com.slowlock.feature.shortcut.ui.WaitViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The activity every pinned shortcut points at. **Its fully-qualified name is frozen** — see
 * [ShortcutContract.LAUNCH_ACTIVITY] and `contracts/pinned-shortcut.md`.
 *
 * Do not rename this class or move it to another package. The FQN is written into the persisted
 * intent of every shortcut already on every home screen; changing it compiles clean, pins *new*
 * shortcuts at the new name, and kills all the old ones silently at the moment the user taps them.
 * `ShortcutContractTest` asserts the runtime name against the frozen constant so that failure lands
 * in `./gradlew test` instead. If a rename ever becomes unavoidable the remedy is an
 * `<activity-alias>` under the old name, never a re-pin.
 *
 * That is why this class sits in `com.slowlock.shortcut` while the rest of its feature lives
 * in `com.slowlock.feature.shortcut`: a frozen fully-qualified name outranks the package shape
 * (constitution, Principle III).
 *
 * `excludeFromRecents`, the empty `taskAffinity`, `FLAG_ACTIVITY_NEW_TASK` and finishing at
 * hand-off are what keep a SlowLock entry out of recents and the wait screen out from behind the
 * target (FR-031, obligations W19–W21, research R8).
 *
 * The design is subtractive and the absences are load-bearing: no countdown, no progress, no sound,
 * no back handler, nothing tappable (see `contracts/wait-screen.md` W8–W14).
 *
 * Nothing here reads in-process state, so a pinned shortcut works with SlowLock force-stopped and
 * after a reboot (FR-033, W24). The delay comes off disk through a repository and the target off
 * the intent.
 *
 * Resolution, the configuration read and the delay belong to [WaitViewModel] (FR-022, research
 * R10). What stays is what needs a window: the [onStop] rule, [onNewIntent] de-duplication, the
 * unavailable toast, and the three things [handOff] does that no state holder can — check this
 * window is still visible, build a platform `Intent`, and start it.
 */
@AndroidEntryPoint
class ShortcutLaunchActivity : ComponentActivity() {

    private val viewModel: WaitViewModel by viewModels()

    /**
     * For the one package-manager lookup this class still makes — a binder call on the path a user
     * is already waiting on, so it must stay off the main thread. Injected rather than named, like
     * every other dispatcher in the project (obligation D1).
     */
    @Inject
    @IoDispatcher
    lateinit var io: CoroutineDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // W1: the target comes from the extra and from nowhere else — not the intent's data, not
        // its action, not its component (Constitution V).
        val target = intent?.getStringExtra(ShortcutContract.EXTRA_TARGET_PACKAGE)

        // W2: a shortcut carrying no extra is treated exactly like an uninstalled target, and
        // finished before anything is drawn so there is no wait and no flash.
        if (target == null) {
            Log.w(TAG, "Shortcut carried no target package")
            reportUnavailable()
            finish()
            return
        }

        // W6: the screen shows the same fixed text for every app and every delay, so the first
        // frame owes nothing to the disk and the read happens underneath it (FR-022, research R3).
        setContent { WaitScreen() }

        // W13: a window flag, never a PowerManager wake lock. Without it any delay longer than the
        // device's screen timeout is unreachable — the display sleeps, the wait is abandoned, and a
        // 30s delay could never complete on a phone that sleeps at 15. Scoped to this window and
        // released with it, which is the whole of what Constitution IV permits (research R6).
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // On `lifecycleScope`, so a rotation cancels this collector and the recreated activity
        // starts a new one. The channel is buffered, so an outcome landing in that gap survives.
        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is WaitEvent.HandOff -> handOff(event.packageName)
                    WaitEvent.Unavailable -> {
                        reportUnavailable()
                        finish()
                    }
                }
            }
        }

        viewModel.start(target)
    }

    /**
     * Obligations W22 and W23: what a second tap does.
     *
     * `launchMode="singleTop"` routes a repeat tap here instead of creating a second activity. A
     * repeat naming the *same* target is ignored outright — no restart, no extension, no second
     * wait (FR-027): impatience is the state this feature exists to sit with, and a tap that
     * visibly reset the timer would teach the user that tapping does something. A *different*
     * target re-anchors and starts over, because the deadline in flight belongs to an app the user
     * is no longer asking for.
     *
     * Both rules live in [WaitViewModel.start], which is why this only forwards. The early return
     * also covers a repeat carrying no extra: a valid wait is already running, and nothing here is
     * a reason to disturb it.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val target = intent.getStringExtra(ShortcutContract.EXTRA_TARGET_PACKAGE) ?: return
        viewModel.start(target)
    }

    /**
     * Obligation W15, and the whole of FR-029 in one line.
     *
     * The requirement lists six ways to leave a wait — back, home, the app switcher, another app
     * taking over, the display timing out, the device locking — and every one arrives here, because
     * `onStop` is precisely when this window stops being visible (research R5). That is also why
     * there is deliberately no `BackHandler` anywhere in the feature (W14).
     *
     * Finishing rather than merely cancelling leaves nothing to come back to (FR-031): re-tapping
     * the icon starts a fresh full-length wait rather than resuming a part-served one, closing the
     * "pocket the phone" bypass.
     *
     * The [isChangingConfigurations] exception is not optional — a rotation passes through `onStop`
     * too, and finishing there would restart the wait on every rotation. It is also why
     * `android:noHistory` had to come off the manifest entry: that delivers this behaviour for
     * free, but with no exception for a configuration change (R5).
     *
     * `finish()` clears [WaitViewModel] and with it the `delay` in flight, so there is no timer to
     * stop; a configuration change does not clear it, which is what carries the wait across a
     * rotation now that no bundle does.
     */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) finish()
    }

    /**
     * Obligations W16–W21: re-resolve, check we are still visible, start, finish.
     *
     * None of the three can move into the state holder, which is why the hand-off arrives here as
     * an event: re-resolution produces a platform `Intent`, which may not cross out of the domain;
     * the visibility check reads *this window's* lifecycle; and only an `Activity` can start
     * another one.
     *
     * Re-resolving at the hand-off rather than at the anchor is not belt-and-braces — the target
     * can be uninstalled *during* the wait, and on a thirty-second delay that is a real window
     * (FR-030).
     *
     * `FLAG_ACTIVITY_NEW_TASK` is required because this activity declares an empty `taskAffinity`
     * and is about to finish; it also gives the target its own task, so backing out of the target
     * does not unwind into the wait screen (FR-031).
     *
     * The process is never killed by hand (W21, research R8): that would abort the
     * `SharedPreferences` write queue mid-flight.
     */
    private suspend fun handOff(target: String) {
        val launch = resolveLaunchIntent(target)?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        if (launch == null) {
            // W18: never crash. The user waited for an app that is no longer there.
            reportUnavailable()
            finish()
            return
        }

        // W16: the deadline can expire in the same instant the user presses home. The continuation
        // is already queued on the main thread by then and cancellation cannot unqueue it, so
        // starting the target from here would be the background start Constitution IV forbids.
        // Abandoned silently — nothing failed, and the user is not here to read a toast.
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Log.i(TAG, "Wait for $target completed after the screen was left; abandoning")
            finish()
            return
        }

        // Resolving is not a guarantee: the target can be uninstalled between the two calls, and a
        // disabled or locked-profile app can throw. Caught, never crashed.
        runCatching { startActivity(launch) }.onFailure {
            Log.w(TAG, "Could not start $target", it)
            reportUnavailable()
        }

        finish()
    }

    /** The package-manager lookup, off the main thread (Constitution IV, FR-036). */
    private suspend fun resolveLaunchIntent(target: String): Intent? = withContext(io) {
        runCatching { packageManager.getLaunchIntentForPackage(target) }.getOrNull()
    }

    /**
     * The one thing this activity says beyond the wait message, and only because it replaces a tap
     * that would otherwise appear to do nothing at all (FR-030).
     *
     * A toast rather than a screen: there is nothing to act on, and it outlives the activity
     * because the system shows it, not this window.
     */
    private fun reportUnavailable() {
        Toast.makeText(this, R.string.shortcut_launch_unavailable, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val TAG = "SlowLock"
    }
}
