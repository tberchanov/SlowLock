package com.slowlock.feature.shortcut.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.slowlock.R

/**
 * The screen a pinned lock shows for the length of its delay. Three structural rules govern it:
 *
 * 1. **Nothing asynchronous.** No `LaunchedEffect`, no store read, no icon load, no label lookup:
 *    the screen must reach its complete appearance in a single frame, with no state in which some
 *    parts are drawn and others are not (FR-029). It is forbidden to display any of that anyway
 *    (FR-032). The blocking resource font is what makes this achievable.
 * 2. **Nothing animates.** No fade, no pulse, no progress, no countdown — that is the point of it.
 * 3. **It resolves its own colours and type.** No `MaterialTheme` is in scope and none may be
 *    introduced. The duplication below — a font family `Type.kt` also declares — is deliberate:
 *    FR-033 requires that a change to the app's theme cannot reach this screen by accident, and the
 *    only way to guarantee that is for the screen not to read it.
 */
@Composable
fun WaitScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.wait_background))
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        // A static mark, not an indicator: it does not fill, move or represent progress. It is
        // there so the screen reads as composed rather than broken.
        Box(
            Modifier
                .width(RULE_WIDTH)
                .height(RULE_HEIGHT)
                .alpha(RULE_ALPHA)
                .background(colorResource(R.color.wait_rule)),
        )
        BasicText(
            text = stringResource(R.string.wait_message),
            style = WaitMessageStyle.copy(color = colorResource(R.color.wait_text)),
        )
    }
}

/**
 * The screen's only text style, declared here rather than taken from `SlowLockType` — the isolation
 * FR-033 asks for, so nothing done to the theme can alter the one screen that must not change.
 */
private val WaitMessageStyle = TextStyle(
    fontFamily = FontFamily(Font(R.font.jetbrains_mono_regular, FontWeight.Normal)),
    fontWeight = FontWeight.Normal,
    fontSize = 19.sp,
    letterSpacing = 0.02.em,
    textAlign = TextAlign.Center,
)

private val RULE_WIDTH = 40.dp
private val RULE_HEIGHT = 2.dp
private const val RULE_ALPHA = 0.55f
