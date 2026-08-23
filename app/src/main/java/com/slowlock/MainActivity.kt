package com.slowlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.slowlock.ui.theme.SlowLockTheme

/**
 * Hosts the whole app. All navigation lives in [SlowLockRoot].
 *
 * Feature 001's `launchApp()` is gone: a row tap now opens the **delay** screen rather than
 * launching the target (003 FR-001). It was always the interim consumer of the selection seam,
 * and `specs/001-installed-apps-list/contracts/selection-handoff.md` named its replacement — which
 * came twice, first as feature 002's shortcut configuration screen and then as feature 003's
 * delay screen in front of it. Neither swap needed an edit to `AppListScreen`, which is what that
 * contract was written to guarantee.
 *
 * The tap now **reads the configuration store before it navigates**, so both configuration
 * screens open on the app's saved values (003 FR-012, FR-013). [SlowLockRoot] owns that read.
 *
 * This activity hosts the configuration flow only. The wait a pinned shortcut shows belongs to
 * `ShortcutLaunchActivity`, which is a separate entry point with its own theme and no route from
 * here — nothing in this UI ever opens a target app.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SlowLockTheme {
                SlowLockRoot()
            }
        }
    }
}
