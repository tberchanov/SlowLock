package com.slowlock.feature.shortcut.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slowlock.R
import com.slowlock.ui.components.PrimaryAction
import com.slowlock.ui.components.SecondaryAction
import com.slowlock.ui.theme.SlowLockType

/**
 * Shown *in place of* the app list on a device whose launcher refuses pin requests (FR-029).
 *
 * Every control in this feature ends in a pinned lock, so on such a device every one of them is
 * dead. A replacement rather than a banner or a disabled button, so the user is not walked through
 * the whole flow to a button that can only fail silently (SC-009).
 *
 * The wording is deliberately plain — no error codes, no API names (FR-030, U2): the user cannot
 * act on the mechanism, only on the launcher.
 *
 * Left-aligned, not centred (FR-034): a centred block with two centred buttons is the shape of a
 * system error dialog, which this is not.
 *
 * [onRecheck] re-runs the evaluation the root performs on `ON_START`, so the button and the
 * return-to-foreground path cannot drift apart. Largely redundant with that automatic re-check, and
 * present because a screen that only explains reads as a dead end.
 */
@Composable
fun PinUnsupportedScreen(
    onRecheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // U4: shown inline and staying put, rather than in a snackbar that would be gone by the time
    // the user has read the sentence above it. Cleared on the next attempt.
    var settingsFailed by remember { mutableStateOf(false) }

    // A [Scaffold] for the inset padding: `MainActivity` calls `enableEdgeToEdge()`, so without
    // `contentPadding` the controls here can sit under the system bars.
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            ) {
                Text(
                    text = stringResource(R.string.pin_unsupported_eyebrow),
                    style = SlowLockType.Eyebrow,
                    // `secondary` is the scheme's AmberDark slot, named through the scheme so the
                    // screen holds no colour of its own (C1).
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = stringResource(R.string.pin_unsupported_message),
                    style = SlowLockType.Message,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                // U4/U5: the failure disables nothing — re-check in particular keeps working.
                if (settingsFailed) {
                    Text(
                        text = stringResource(R.string.pin_unsupported_settings_failed),
                        style = SlowLockType.Body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrimaryAction(
                    label = stringResource(R.string.pin_unsupported_open_settings),
                    onClick = {
                        // Catch rather than probe: `resolveActivity()` returns null under package
                        // visibility unless a `<queries>` entry is added to the manifest — a
                        // permanent change bought to answer what failing answers for free. Some OEM
                        // and managed builds genuinely do not expose this screen.
                        settingsFailed = runCatching {
                            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                        }.isFailure
                    },
                )
                SecondaryAction(
                    label = stringResource(R.string.pin_unsupported_recheck),
                    onClick = onRecheck,
                )
            }
        }
    }
}

private val SCREEN_PADDING = 20.dp
