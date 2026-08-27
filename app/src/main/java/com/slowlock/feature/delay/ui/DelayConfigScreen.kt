package com.slowlock.feature.delay.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowlock.R
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.feature.delay.domain.DelayRange
import com.slowlock.ui.components.PrimaryAction
import com.slowlock.ui.components.ScreenHeader
import com.slowlock.ui.components.SelectableTile
import com.slowlock.ui.theme.Pill
import com.slowlock.ui.theme.SlowLockType
import kotlin.math.roundToInt

/**
 * How long to wait before the target opens.
 *
 * The screen owns no state: [onSecondsChange] is the only way a value leaves it, and it never
 * touches `DelayConfigStore` — the root loads the saved value before it navigates and hands it in
 * (D9, contract S2).
 *
 * Which preset appears selected is derived, never stored: the row asks `DelayRange.presetFor` at
 * composition time, which makes "dragged to 17 seconds, so nothing is highlighted" correct by
 * construction rather than by remembering to clear a flag (FR-018).
 *
 * The numeral is the element that yields. The header, slider, preset row and action are fixed
 * height; the centre block takes what is left and the readout auto-sizes into it, so at the largest
 * font scale on the smallest screen the button is still reachable and nothing scrolls (FR-014a,
 * C11).
 */
@Composable
fun DelayConfigScreen(
    packageName: String,
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    /**
     * Supplied by the root rather than constructed here (FR-024). This screen deliberately has no
     * state holder (FR-023, V4) — a `ViewModel` here would hold nothing and forward everything.
     */
    targets: AppTargetRepository,
    icons: AppIconRepository,
    modifier: Modifier = Modifier,
) {
    BackHandler { onBack() }

    val targetState by produceState<TargetState>(TargetState.Resolving, packageName) {
        value = TargetState.Resolving
        value = targets.resolve(packageName)
            ?.let(TargetState::Resolved)
            ?: TargetState.Missing
    }
    val target = (targetState as? TargetState.Resolved)?.target
    val icon by produceState<ImageBitmap?>(null, target) {
        value = null
        val resolved = target ?: return@produceState
        value = icons.icon(resolved.packageName, resolved.versionCode)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // The system-bar inset, and the reason this screen is a `Scaffold` at all.
                .padding(contentPadding)
                .padding(horizontal = SCREEN_PADDING),
        ) {
            ScreenHeader(
                title = stringResource(R.string.delay_config_title),
                onBack = onBack,
                step = 2,
            )

            // The flexible middle. Everything below it is fixed height, which is the mechanism
            // behind FR-014a: no arrangement can push the action off screen.
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
                    // Resolving: nothing, deliberately. A spinner for a package-manager lookup
                    // that takes milliseconds is a flash, not feedback.
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
 * 104sp is a ceiling, not a size: `TextAutoSize.StepBased` fits the numeral to whatever the centre
 * block was left, so a large system font scale makes everything else bigger and this smaller. That
 * is the trade FR-014a chose over a screen whose primary action falls off the bottom.
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
 * Material 3's [Slider], restyled through its slot API — deliberately not hand-built, since the
 * drag handling, keyboard support, accessibility semantics and step snapping already work and none
 * of them is what the design changes (FR-016, research R8).
 *
 * The slot API is still `@ExperimentalMaterial3Api`. The risk is contained: an API change breaks
 * the two private composables below at compile time, and the fallback is `SliderDefaults.Track`,
 * which reaches the track colours but not the ring-style thumb.
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
 * Three one-tap delays, as a `selectableGroup` of [SelectableTile]s so a screen reader announces a
 * single-choice set and reads the selection change rather than leaving it to colour (FR-043, U4).
 *
 * "Nothing selected" is legitimate here, unlike the icon-treatment row: any delay that is not 5, 10
 * or 30 leaves all three unhighlighted.
 *
 * These tiles ship at 44dp, below the 48dp accessibility floor — the deliberate trade recorded in
 * FR-045 and scoped by C10 to this row and the treatment tiles alone.
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
    data class Resolved(val target: AppTarget) : TargetState
    data object Missing : TargetState
}

private val SCREEN_PADDING = 20.dp
private val PILL_ICON = 32.dp
private val TRACK_HEIGHT = 6.dp
private val THUMB_SIZE = 26.dp
private val THUMB_RING = 3.dp
private val PRESET_HEIGHT = 44.dp
