package com.slowlock.locks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slowlock.R
import com.slowlock.ui.components.PrimaryAction
import com.slowlock.ui.theme.SlowLockType

/**
 * What a fresh install opens on, and the only place the app explains itself (FR-017, contract K1).
 *
 * **It is not an onboarding step and there is no flag behind it.** Whether it shows is derived
 * from the lock list alone — no locks means this screen, and making a lock means it is not seen
 * again (FR-019a). It is the Locks screen's empty state wearing better copy, which is why removing
 * the last lock brings it back with no code path of its own (N2). There is no `Stage.Intro`.
 *
 * **It is a root, not a step** (FR-031): no [com.slowlock.ui.components.ScreenHeader], no back
 * tile, no step counter, and no `BackHandler` — system back leaves the app, which is the default
 * activity behaviour and is exactly what the requirement asks for.
 *
 * The copy states the limits as well as the promise: nothing is blocked and nothing is counted
 * (FR-018). That second half is a product commitment rather than a phrasing choice — the spec's
 * permanent Out of Scope forbids statistics of any kind, and Constitution I forbids the app
 * claiming powers it does not have. The strings carry it; this file must not paraphrase it away.
 *
 * Stateless in the strict sense of U5: it reads nothing, holds nothing, persists nothing, and does
 * not know why it is showing. Its one action reports the tap and stops there.
 *
 * The layout is the unsupported-launcher screen's, deliberately: left-aligned down the same edge,
 * the block vertically centred, the action pinned at the bottom. The eyebrow takes `AmberDark`
 * through the scheme's `secondary` slot rather than `Amber`, which is a fill and never a glyph
 * (C2).
 */
@Composable
fun IntroScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // As every other screen: it paints the ground and brings the inset padding, which matters
    // because `MainActivity` calls `enableEdgeToEdge()` and the action below would otherwise sit
    // under the navigation bar. `containerColor` is stated so this screen grounds itself on the
    // same token as the other four (C4).
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = SCREEN_PADDING),
        ) {
            // SC-008: the body is the longest copy in the app, and at the largest font scale on
            // the smallest supported screen it will not fit. It scrolls rather than clipping or
            // pushing the action off the bottom — the action stays outside this column so it
            // stays reachable at every scale.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.intro_eyebrow),
                    style = SlowLockType.Eyebrow,
                    // The scheme's AmberDark slot (C1). Named through the scheme rather than
                    // imported directly, so the screen holds no colour of its own.
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = TOP_PADDING),
                )
                Text(
                    text = stringResource(R.string.intro_title),
                    style = SlowLockType.Message,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.intro_body),
                    style = SlowLockType.Body,
                    // The scheme's Ink60 slot (C1, K1) — supporting copy under the sentence above
                    // it. `onSurface` is Ink and would flatten the two into one weight.
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Exactly one (U2), and the only thing on this screen that does anything (FR-019).
            PrimaryAction(
                label = stringResource(R.string.intro_start),
                onClick = onStart,
                modifier = Modifier.padding(top = 20.dp, bottom = 20.dp),
            )
        }
    }
}

private val SCREEN_PADDING = 20.dp
private val TOP_PADDING = 32.dp
