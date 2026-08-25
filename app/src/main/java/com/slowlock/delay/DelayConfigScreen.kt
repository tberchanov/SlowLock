package com.slowlock.delay

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowlock.R
import com.slowlock.apps.AppIconCache
import com.slowlock.shortcut.ShortcutTarget
import com.slowlock.shortcut.resolveShortcutTarget
import com.slowlock.ui.components.PrimaryAction
import com.slowlock.ui.components.ScreenHeader
import com.slowlock.ui.components.SelectableTile
import com.slowlock.ui.theme.Pill
import com.slowlock.ui.theme.SlowLockType
import kotlin.math.roundToInt

/**
 * How long to wait before the target opens.
 *
 * Feature 004 re-weighted this screen around the number it exists to set: the delay is now the
 * largest thing on it, in the mono face every number in the app uses, above a slider and three
 * one-tap presets.
 *
 * **The screen still owns no state.** [onSecondsChange] remains the only way a value leaves it,
 * and it still never touches `DelayConfigStore` — the root loads the saved value before it
 * navigates and hands it in (003 D9, `contracts/screen-inventory.md` S2). The presets did not
 * change that: tapping one calls [onSecondsChange] like a slider drag does.
 *
 * **Which preset appears selected is derived, never stored.** There is no "selected preset"
 * variable anywhere; the row asks `DelayRange.presetFor(seconds)` at composition time. That is
 * what makes "dragged to 17 seconds, so nothing is highlighted" correct by construction rather
 * than by remembering to clear a flag (FR-018).
 *
 * **The numeral is the element that yields.** The header, slider, preset row and action are fixed
 * height; the centre block takes what is left and the readout auto-sizes into it. So at the
 * largest font scale on the smallest screen, the button is still reachable and the screen never
 * needs to scroll (FR-014a, contract C11, research R10).
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
    BackHandler { onBack() }

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // The system-bar inset, and the reason this screen is a `Scaffold` at all.
                // Every other screen in the app takes its insets from one; this was the only
                // one built on a bare `Column`, so its header drew under the status bar.
                .padding(contentPadding)
                .padding(horizontal = SCREEN_PADDING),
        ) {
            // Step 2 of 3 (005 FR-029). The `BackHandler` this screen already had is what FR-030
            // asks for and is unchanged.
            ScreenHeader(
                title = stringResource(R.string.delay_config_title),
                onBack = onBack,
                step = 2,
            )

            // The flexible middle. Everything below it is fixed height, which is the whole mechanism
            // behind FR-014a: there is no arrangement in which the action can be pushed off screen.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
            ) {
                when {
                    targetState is TargetState.Missing ->
                        Message(stringResource(R.string.app_list_unavailable))
                    // Resolving: nothing, deliberately. A spinner for a package-manager lookup that
                    // takes milliseconds is a flash, not feedback (003).
                    target == null -> Unit
                    else -> AppPill(label = target.label, icon = icon)
                }
                Readout(seconds = seconds, modifier = Modifier.weight(1f, fill = false))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                DelaySlider(seconds = seconds, onSecondsChange = onSecondsChange)
                PresetRow(seconds = seconds, onSecondsChange = onSecondsChange)
                PrimaryAction(
                    label = stringResource(R.string.delay_config_next),
                    onClick = onNext,
                    modifier = Modifier.padding(bottom = 20.dp),
                )
            }
        }
    }
}

/**
 * The delay, as large as the space allows.
 *
 * 104sp is a **ceiling, not a size**. `TextAutoSize.StepBased` fits the numeral to whatever the
 * centre block was left after the fixed-height controls took theirs, so a large system font scale
 * makes everything else bigger and this smaller — which is the trade FR-014a chose, because the
 * alternative was a screen whose primary action falls off the bottom.
 *
 * The floor is 32sp: below that it stops being the focal point and the design has failed anyway.
 */
@Composable
private fun Readout(seconds: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText(
            text = seconds.toString(),
            style = SlowLockType.Readout.copy(color = MaterialTheme.colorScheme.onBackground),
            autoSize = TextAutoSize.StepBased(
                minFontSize = 32.sp,
                maxFontSize = SlowLockType.Readout.fontSize,
            ),
            maxLines = 1,
        )
        Text(
            text = stringResource(R.string.delay_config_seconds_caption),
            style = SlowLockType.Caption,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/** The target app, named once, above the number. */
@Composable
private fun AppPill(label: String, icon: ImageBitmap?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(Pill)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), Pill)
            .padding(start = 8.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(PILL_ICON)) {
            if (icon == null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.extraSmall)
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
            style = SlowLockType.PillLabel,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Material 3's [Slider], restyled through its slot API.
 *
 * Deliberately **not** a hand-built control: the drag handling, the keyboard support, the
 * accessibility semantics and the step snapping all already work, and none of them is what the
 * design changes. Only the track and the thumb are overridden (FR-016, research R8).
 *
 * The slot API is still `@ExperimentalMaterial3Api`, so this opts in. The risk is contained: an
 * API change here breaks the two private composables below at compile time, and the fallback is
 * `SliderDefaults.Track` with `SliderColors`, which reaches the track colours but not the
 * ring-style thumb. That is a smaller loss than re-implementing drag and accessibility would be.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DelaySlider(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Slider(
            value = seconds.toFloat(),
            onValueChange = { onSecondsChange(DelayRange.snap(it.roundToInt())) },
            valueRange = DelayRange.MIN_SECONDS.toFloat()..DelayRange.MAX_SECONDS.toFloat(),
            steps = DelayRange.SLIDER_STEPS,
            track = { state -> Track(state) },
            thumb = { Thumb() },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.delay_config_range_min),
                style = SlowLockType.Tick,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = stringResource(R.string.delay_config_range_max),
                style = SlowLockType.Tick,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Track(state: SliderState, modifier: Modifier = Modifier) {
    val fraction = with(state) {
        val span = valueRange.endInclusive - valueRange.start
        if (span == 0f) 0f else ((value - valueRange.start) / span).coerceIn(0f, 1f)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TRACK_HEIGHT)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(TRACK_HEIGHT)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun Thumb(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(THUMB_SIZE)
            .clip(Pill)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(BorderStroke(THUMB_RING, MaterialTheme.colorScheme.primary), Pill),
    )
}

/**
 * Three one-tap delays.
 *
 * A `selectableGroup` of [SelectableTile]s, so a screen reader announces this as a single-choice
 * set and reads the selection change rather than leaving it to colour (FR-043, contract U4).
 *
 * **"Nothing selected" is a legitimate state here**, unlike the icon-treatment row: any delay that
 * is not 5, 10 or 30 leaves all three unhighlighted, and that is normal rather than an error.
 * `Role.RadioButton` expresses both cases without the tile knowing which caller it has.
 *
 * These tiles ship at 44dp, below the 48dp accessibility floor — the deliberate trade recorded in
 * FR-045 and scoped by contract C10 to this row and the treatment tiles alone.
 */
@Composable
private fun PresetRow(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = DelayRange.presetFor(seconds)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DelayRange.PRESETS.forEach { preset ->
            val isSelected = preset == selected
            SelectableTile(
                selected = isSelected,
                onClick = { onSecondsChange(preset) },
                contentDescription = stringResource(
                    R.string.delay_config_preset_description,
                    preset,
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(PRESET_HEIGHT),
            ) {
                Text(
                    text = stringResource(R.string.delay_config_preset, preset),
                    style = SlowLockType.Preset.copy(
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}

@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = SlowLockType.Body,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

private sealed interface TargetState {
    data object Resolving : TargetState
    data class Resolved(val target: ShortcutTarget) : TargetState
    data object Missing : TargetState
}

private val SCREEN_PADDING = 20.dp
private val PILL_ICON = 32.dp
private val TRACK_HEIGHT = 6.dp
private val THUMB_SIZE = 26.dp
private val THUMB_RING = 3.dp
private val PRESET_HEIGHT = 44.dp
