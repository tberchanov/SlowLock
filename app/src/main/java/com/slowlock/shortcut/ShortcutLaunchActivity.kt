package com.slowlock.shortcut

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.slowlock.R

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
 * What this activity *does* is deliberately not frozen, and is the whole return on routing the
 * tap through SlowLock rather than at the target's own launch intent (research.md R4). Today it
 * resolves and starts immediately (FR-016); when the delay feature ships it gains a countdown
 * and a schedule check, and every already-pinned shortcut picks that up with nothing asked of
 * the user (FR-011).
 *
 * It draws nothing. No layout, no `setContent` — the transparent theme, `noHistory`,
 * `excludeFromRecents` and the empty `taskAffinity` in the manifest, plus finishing inside
 * [onCreate], are together what keeps a SlowLock screen from flashing and a SlowLock entry out
 * of recents (FR-019, obligation L5).
 *
 * Nothing here reads any in-process state, so a pinned shortcut works with SlowLock force-stopped
 * and after a reboot (FR-017, obligation L6). That is what makes the design viable: there is
 * nothing to restore and nothing to keep running.
 */
class ShortcutLaunchActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obligation L1: the target comes from the extra and from nowhere else — not the
        // intent's data, not its action. A shortcut pinned by an older build that somehow
        // carries no extra is treated exactly like an uninstalled target rather than crashing.
        val targetPackage = intent?.getStringExtra(ShortcutContract.EXTRA_TARGET_PACKAGE)

        launchTarget(targetPackage)

        // Obligation L4: finish immediately, in every branch — including the toast path, where
        // the toast outlives the activity because it is shown by the system, not drawn here.
        finish()
    }

    /**
     * Obligations L2–L4: re-resolve at tap time, tell the user and stop if the app is gone,
     * otherwise start it in its own task.
     *
     * Re-resolution is not optional. The pin may have happened months ago and the target may
     * have been uninstalled since (FR-018) — a shortcut is a durable thing that outlives the
     * facts it was created from.
     *
     * `FLAG_ACTIVITY_NEW_TASK` is required because this activity declares an empty
     * `taskAffinity` and is about to finish; it also gives the target its own task, so backing
     * out of the target does not unwind into SlowLock.
     */
    private fun launchTarget(targetPackage: String?) {
        if (targetPackage == null) {
            Log.w(TAG, "Shortcut carried no target package")
            reportUnavailable()
            return
        }

        val launch = packageManager.getLaunchIntentForPackage(targetPackage)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        if (launch == null) {
            reportUnavailable()
            return
        }

        // Resolving is not a guarantee: the target can be uninstalled between the two calls,
        // and a disabled or locked profile app can throw. Caught, never crashed (L3).
        runCatching { startActivity(launch) }.onFailure {
            Log.w(TAG, "Could not start $targetPackage", it)
            reportUnavailable()
        }
    }

    /**
     * The one thing this activity is allowed to put on screen, and only because it replaces a
     * tap that would otherwise appear to do nothing at all (FR-018).
     *
     * A toast rather than a screen: there is nothing here for the user to act on, and a
     * dismissible SlowLock screen in front of a home-screen tap is exactly what FR-019 forbids.
     */
    private fun reportUnavailable() {
        Toast.makeText(this, R.string.shortcut_launch_unavailable, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val TAG = "SlowLock"
    }
}
