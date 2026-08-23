package com.slowlock.shortcut

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slowlock.R

/**
 * Shown **in place of** the app list on a device whose launcher refuses pin requests (FR-029).
 *
 * Every control in this feature ends in a pinned shortcut, so on such a device every one of them
 * is dead. Rather than let the user walk the list, choose an app, pick a treatment and press a
 * button that can only fail silently, the root hands the whole screen to this one — which is why
 * this is not a banner or a disabled button but a replacement (SC-009).
 *
 * The wording is deliberately plain: no error codes, no API names, nothing about
 * `isRequestPinShortcutSupported` (FR-030, U2). The user did not do anything wrong and cannot
 * act on the mechanism — only on the one thing that fixes it, which is the launcher.
 *
 * [onRecheck] re-runs the very evaluation the root performs on `ON_START`, so the button and the
 * return-to-foreground path cannot drift apart. It is largely redundant with that automatic
 * re-check (research.md R11) and exists because a screen that only explains, with nothing to
 * press, reads as a dead end.
 */
@Composable
fun PinUnsupportedScreen(
    onRecheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // U4: the failure is shown inline and stays put, rather than in a snackbar that would be
    // gone by the time the user has finished reading the sentence above it. Cleared on the next
    // attempt so a device that recovers does not keep showing a stale complaint.
    var settingsFailed by remember { mutableStateOf(false) }

    // A [Scaffold] for the same reason the other two screens have one: it paints
    // `colorScheme.background`, so the screen follows the system light/dark setting. A bare
    // Column paints nothing, leaving the Activity's window background showing through — and that
    // comes from `Theme.SlowLock`, whose parent is `android:Theme.Material.Light.NoActionBar`
    // with no `values-night` variant, so it is light whatever the system is set to. Themed text
    // on an unthemed window is the mismatch.
    //
    // It also brings the inset padding the other screens get: `MainActivity` calls
    // `enableEdgeToEdge()`, so without `contentPadding` the controls here can sit under the
    // system bars.
    Scaffold(modifier = modifier.fillMaxSize()) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = stringResource(R.string.pin_unsupported_message),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )

            Button(
                onClick = {
                    // Catch rather than probe (research.md R11). `resolveActivity()` would be the
                    // obvious pre-flight check, but under package visibility it returns null unless
                    // a `<queries>` entry is added to the manifest — a permanent manifest change
                    // bought to answer a question that trying and failing answers for free
                    // (Constitution III). Some OEM and managed builds genuinely do not expose this
                    // screen, and a device odd enough to refuse pin requests is exactly where that
                    // is likeliest.
                    settingsFailed = runCatching {
                        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                    }.isFailure
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.pin_unsupported_open_settings))
            }

            // U4/U5: the failure disables nothing. The screen stays usable and re-check in
            // particular keeps working — the user may well fix the launcher by another route.
            if (settingsFailed) {
                Text(
                    text = stringResource(R.string.pin_unsupported_settings_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            TextButton(onClick = onRecheck) {
                Text(stringResource(R.string.pin_unsupported_recheck))
            }
        }
    }
}
