package com.slowlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.slowlock.ui.theme.SlowLockTheme

/**
 * Hosts the whole app. All navigation lives in [SlowLockRoot].
 *
 * Feature 001's `launchApp()` is gone: a row tap now opens the shortcut configuration screen
 * rather than launching the target (FR-001). It was always the interim consumer of the selection
 * seam, and `specs/001-installed-apps-list/contracts/selection-handoff.md` named this feature as
 * the one that would replace it.
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
