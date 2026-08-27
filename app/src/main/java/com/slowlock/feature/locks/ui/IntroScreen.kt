package com.slowlock.feature.locks.ui

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
 * Not an onboarding step, and there is no flag behind it: whether it shows is derived from the lock
 * list alone (FR-019a). It is the Locks screen's empty state wearing better copy, which is why
 * removing the last lock brings it back with no code path of its own (N2).
 *
 * A root, not a step (FR-031): no `ScreenHeader`, no back tile, no step counter and no
 * `BackHandler` — system back leaves the app, which is the default activity behaviour.
 *
 * The copy states the limits as well as the promise — nothing is blocked and nothing is counted
 * (FR-018). That is a product commitment: the permanent Out of Scope forbids statistics of any
 * kind, and Constitution I forbids claiming powers the app does not have. This file must not
 * paraphrase it away.
 *
 * Stateless in the strict sense of U5: it reads nothing, holds nothing, and does not know why it is
 * showing.
 */
@Composable
fun IntroScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The Scaffold brings the inset padding: `MainActivity` calls `enableEdgeToEdge()`, so the
    // action below would otherwise sit under the navigation bar.
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
            // SC-008: the body is the longest copy in the app and will not fit at the largest font
            // scale, so it scrolls. The action stays outside this column to stay reachable.
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
                    // The scheme's AmberDark slot, named through the scheme so the screen holds no
                    // colour of its own (C1).
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
                    // Ink60: supporting copy. `onSurface` is Ink and would flatten the two.
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
