package com.slowlock.shortcut

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
 * Shown **in place of** the app list on a device whose launcher refuses pin requests (FR-029).
 *
 * Every control in this feature ends in a pinned lock, so on such a device every one of them
 * is dead. Rather than let the user walk the list, choose an app, pick a treatment and press a
 * button that can only fail silently, the root hands the whole screen to this one — which is why
 * this is not a banner or a disabled button but a replacement (SC-009).
 *
 * The wording is deliberately plain: no error codes, no API names, nothing about
 * `isRequestPinShortcutSupported` (FR-030, U2). The user did not do anything wrong and cannot
 * act on the mechanism — only on the one thing that fixes it, which is the launcher.
 *
 * **The layout is left-aligned, not centred** (FR-034). A centred block of text with two centred
 * buttons is the shape of a system error dialog, which is exactly what this screen is not: it is
 * SlowLock speaking, in SlowLock's own type, and it reads down the same left edge as every other
 * screen in the app. The eyebrow above the sentence is the redesign's only accented one — it
 * takes `AmberDark` rather than the muted ink the `SECONDS` and `ICON` eyebrows use (data-model
 * §2), because this is the one place the app raises its voice.
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

    // A [Scaffold] for the same reason the other screens have one: it paints the screen ground,
    // and it brings the inset padding — `MainActivity` calls `enableEdgeToEdge()`, so without
    // `contentPadding` the controls here can sit under the system bars.
    //
    // `containerColor` is stated rather than left to the default so this screen grounds itself on
    // the same token as the other three, which is now a single light scheme with no `values-night`
    // variant to diverge from (contract C4).
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
                    // `secondary` is the scheme's AmberDark slot (contract C1). Named through the
                    // scheme rather than imported directly so the screen holds no colour of its own.
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = stringResource(R.string.pin_unsupported_message),
                    style = SlowLockType.Message,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                // U4/U5: the failure disables nothing. The screen stays usable and re-check in
                // particular keeps working — the user may well fix the launcher by another route.
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
                        // Catch rather than probe (research.md R11). `resolveActivity()` would be
                        // the obvious pre-flight check, but under package visibility it returns
                        // null unless a `<queries>` entry is added to the manifest — a permanent
                        // manifest change bought to answer a question that trying and failing
                        // answers for free (Constitution III). Some OEM and managed builds
                        // genuinely do not expose this screen, and a device odd enough to refuse
                        // pin requests is exactly where that is likeliest.
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
