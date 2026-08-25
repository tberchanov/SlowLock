package com.slowlock

import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
        // **`light` describes the BAR'S BACKGROUND, which is why it produces DARK glyphs.** The
        // name reads backwards and is the reason this comment exists: telling the platform the
        // surface behind the clock and the battery is light is what makes it draw them in ink.
        //
        // **The device's night setting is deliberately not an input** (contract S3, 007 FR-002).
        // The bare `enableEdgeToEdge()` this replaces defaulted to `SystemBarStyle.auto(...)`,
        // whose `detectDarkMode` reads the system configuration — so on a phone set to dark mode
        // the platform was told "dark surface" and drew white indicators over this app's bone
        // ground. The app is light-only by design (004 FR-008), so the bars follow the app rather
        // than the phone.
        //
        // Both scrims are transparent because the app paints its own ground behind the bars
        // (contract S4); a scrim would be a twelfth colour sitting on top of it.
        //
        // This is the app's ONLY writer of system bar appearance (contract S1). Nothing else may
        // set `isAppearanceLight*`, `statusBarColor` or `navigationBarColor` — two writers to one
        // platform bit is a defect that only surfaces in whatever order the calls happen to run.
        // `ShortcutLaunchActivity` is not a second writer: it never calls this, and it follows the
        // device's light/dark setting on purpose (contract S5, 004 FR-031).
        //
        // Accepted limitation: dark navigation-bar icons arrived in API 27 and this app supports
        // 26, so that one tier keeps light icons over bone (contract S7, manual case M14). It is
        // recorded rather than patched — a scrim would put a dark strip on 26 through 28 to solve
        // something only 26 has.
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
