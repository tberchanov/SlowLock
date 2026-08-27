package com.slowlock

import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.slowlock.ui.theme.SlowLockTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Hosts the whole app. All navigation lives in [SlowLockRoot].
 *
 * This activity hosts the configuration flow only. The wait a pinned shortcut shows belongs to
 * `ShortcutLaunchActivity`, a separate entry point with its own theme and no route from here —
 * nothing in this UI ever opens a target app.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // `light` describes the BAR'S BACKGROUND, which is why it produces DARK glyphs — the name
        // reads backwards, and that is the reason this comment exists.
        //
        // The device's night setting is deliberately not an input (S3, FR-002). `auto(...)`, the
        // default, reads the system configuration and drew white indicators over this app's bone
        // ground on a phone set to dark. The app is light-only by design (FR-008).
        //
        // Both scrims are transparent because the app paints its own ground behind the bars (S4); a
        // scrim would be a twelfth colour on top of it.
        //
        // This is the app's ONLY writer of system bar appearance (S1) — two writers to one platform
        // bit is a defect that surfaces only in whatever order the calls happen to run.
        // `ShortcutLaunchActivity` never calls this and follows the device setting on purpose (S5).
        //
        // Accepted limitation: dark navigation-bar icons arrived in API 27 and this app supports
        // 26, so that one tier keeps light icons over bone (S7, manual case M14).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
        )
        setContent {
            SlowLockTheme {
                SlowLockRoot()
            }
        }
    }
}
