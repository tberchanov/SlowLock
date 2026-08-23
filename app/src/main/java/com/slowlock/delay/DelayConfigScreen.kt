package com.slowlock.delay

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowlock.R
import com.slowlock.apps.AppIconCache
import com.slowlock.shortcut.ShortcutTarget
import com.slowlock.shortcut.resolveShortcutTarget
import kotlin.math.roundToInt

/**
 * How long the pinned icon should wait before the target opens: a slider, a readout, and a
 * "next" that carries the value on to feature 002's shortcut screen.
 *
 * **The screen does not own the chosen value** (obligation D5). It arrives as [seconds] and
 * leaves through [onSecondsChange]; there is deliberately no `rememberSaveable` copy of it here.
 * That is what makes FR-014 work — the value survives the trip to the shortcut screen and back
 * because it never lived on this screen to be lost. `SlowLockRoot` holds it in the stage
 * (research.md R9), which is also what carries it through rotation and process death (FR-008).
 *
 * One `String` comes in, exactly as feature 001's `contracts/selection-handoff.md` hands it
 * across. Label and icon are **re-resolved here** rather than carried (D1, D2) — the app can be
 * uninstalled while this screen is open, and a carried label would go on describing an app that
 * is gone.
 *
 * Nothing here reads or writes [DelayConfigStore] (D9): the caller has already read it, before
 * the transition, so the first composition is correct rather than corrected (D13, research.md
 * R3). Nothing here can launch the target either (D10) — no screen in SlowLock's UI can.
 *
 * No ViewModel (research.md R10, D11). One async resolution and one hoisted `Int`.
 */
@Composable
fun DelayConfigScreen(
    packageName: String,
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // D7/FR-010: the system gesture and the affordance are the same exit, through the same
    // callback, so neither can grow a meaning the other lacks. Neither saves anything — the
    // store is not reachable from this screen at all (FR-020, D9).
    BackHandler { onBack() }

    // Its own instance, the same trade feature 002's screen makes: the ViewModel's cache is not
    // reachable from here without widening `AppListScreen`'s contract, and the tier that matters
    // — the WebP files in cacheDir — is shared anyway, keyed identically (Constitution V).
    val iconCache = remember(context) { AppIconCache(context) }

    val targetState by produceState<TargetState>(TargetState.Resolving, packageName) {
        value = TargetState.Resolving
        value = resolveShortcutTarget(context, packageName)
            ?.let(TargetState::Resolved)
            ?: TargetState.Missing
    }
    val target = (targetState as? TargetState.Resolved)?.target

    val icon by produceState<ImageBitmap?>(null, target) {
        value = null
        val resolved = target ?: return@produceState
        value = iconCache.icon(resolved.packageName, resolved.versionCode)
    }

    Scaffold(modifier = modifier.fillMaxSize()) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.delay_config_back),
                    )
                }
                Text(
                    text = stringResource(R.string.delay_config_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    // Uninstalled while the screen was open. Saying so here is not a
                    // confirmation of anything (FR-012) — it is the answer to "which app is
                    // this?", which is the one question this half of the screen exists to
                    // answer. The slider keeps working; the shortcut screen re-resolves and
                    // refuses to create anything for an app that is gone (002 FR-015).
                    targetState is TargetState.Missing ->
                        Message(stringResource(R.string.app_list_unavailable))

                    // Resolving: no spinner for a package-manager lookup, exactly as the
                    // shortcut screen does nothing here either.
                    target == null -> Unit

                    else -> Target(label = target.label, icon = icon)
                }
            }

            DelaySlider(
                seconds = seconds,
                onSecondsChange = onSecondsChange,
                modifier = Modifier.fillMaxWidth(),
            )

            // D8/FR-009: nothing is written here. Applying happens on the shortcut screen, and
            // this action only moves the chosen value forward. It stays enabled while the
            // target is still resolving — the delay is choosable without knowing the app's
            // name, and the next screen resolves for itself.
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
            ) {
                Text(stringResource(R.string.delay_config_next))
            }
        }
    }
}

/**
 * The slider and its readout (D3, D4, D6).
 *
 * `steps` counts the stops *between* the endpoints, which is why [DelayRange.SLIDER_STEPS] is
 * two less than the number of reachable values — the off-by-one `DelayRangeTest` exists to
 * catch (research.md R11).
 *
 * The `Float` the slider reports is rounded and then put through [DelayRange.snap] before it
 * leaves, so the readout, the value the caller holds, and the handle's position cannot disagree
 * (D6). Snapping rather than trusting the discrete stops is deliberate: the stops constrain what
 * the user can drag to, not what floating-point arithmetic hands back — and at a step of one
 * second that rounding is the *only* thing standing between the slider and a readout of
 * "17 seconds" for a handle sitting at 16.999.
 */
@Composable
private fun DelaySlider(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // FR-007: a slider without a readout lets the user choose a value they cannot name.
        // Plurals rather than concatenation (D4) — see the resource's own comment.
        Text(
            text = pluralStringResource(R.plurals.delay_seconds, seconds, seconds),
            style = MaterialTheme.typography.headlineSmall,
        )
        Slider(
            value = seconds.toFloat(),
            onValueChange = { onSecondsChange(DelayRange.snap(it.roundToInt())) },
            valueRange = DelayRange.MIN_SECONDS.toFloat()..DelayRange.MAX_SECONDS.toFloat(),
            steps = DelayRange.SLIDER_STEPS,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Which app is being configured (D2): its own icon above its own label.
 *
 * The same shape feature 002's preview uses, for the same reason — the user should recognise the
 * app here as the one they tapped. It is *not* a preview of the shortcut, though: no treatment
 * is chosen yet on this screen, so the icon is shown untreated and nothing here suggests what
 * will land on the home screen.
 */
@Composable
private fun Target(
    label: String,
    icon: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(ICON_SIZE)) {
            if (icon == null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            } else {
                Image(
                    bitmap = icon,
                    contentDescription = stringResource(R.string.app_list_icon_description, label),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = LABEL_WIDTH),
        )
    }
}

@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Whether the target's display facts are known yet, and whether it still exists. */
private sealed interface TargetState {
    data object Resolving : TargetState
    data class Resolved(val target: ShortcutTarget) : TargetState
    data object Missing : TargetState
}

private val ICON_SIZE = 96.dp
private val LABEL_WIDTH = 160.dp
