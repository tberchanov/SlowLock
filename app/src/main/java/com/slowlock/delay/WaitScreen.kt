package com.slowlock.delay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.slowlock.R

/**
 * The wait screen: one flat colour and one fixed line of text, for the length of the delay
 * (`contracts/wait-screen.md` W8–W12, research.md R7).
 *
 * **This composable's design is subtractive, and the absences are the specification.** Everything
 * a screen normally does to hold attention is a defect here, so the list of what must never
 * appear in this file is the important part:
 *
 * - no countdown, elapsed time, progress bar, ring, or spinner (FR-023, W8);
 * - no `animate*`, no `rememberInfiniteTransition`, no `AnimatedVisibility`, no `Crossfade`;
 * - no `clickable`, `pointerInput`, `toggleable`, or focus target — a tap does nothing (FR-026, W10);
 * - no parameter that varies per app: no target name, no target icon, no delay (W11).
 *
 * A future contributor adding any of them will be making the screen better in every way except
 * the one that matters.
 *
 * **Not wrapped in `SlowLockTheme`, and it reads no `MaterialTheme` value** (W12). Dynamic colour
 * would tint this screen from the user's wallpaper, making it vary per device and per app-switch
 * — and, worse, making it impossible to match the static `windowBackground` that
 * `Theme.SlowLock.Wait` paints. Resolving the *same* two colour resources the theme resolves is
 * what keeps the starting window and the composed screen indistinguishable in both light and
 * dark, so the tap lands on the final background and the only thing that ever changes is the
 * text arriving.
 *
 * [Text] is Material 3's, but only as a text primitive: every value it would otherwise inherit
 * from a theme is passed explicitly here.
 */
@Composable
fun WaitScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.wait_background)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.wait_message),
            color = colorResource(R.color.wait_text),
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}
