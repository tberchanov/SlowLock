package com.slowlock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.slowlock.R
import com.slowlock.ui.theme.SlowLockType

/**
 * A screen's title, optionally preceded by a back tile.
 *
 * Contract U1. Three screens use it and they differ in exactly one way — whether there is anywhere
 * to go back to — so that is the only parameter beyond the title.
 *
 * **`onBack == null` renders no tile and no leading space.** The app list is the app's root in
 * Phase 1, so it has no back target; drawing a disabled tile, or leaving an empty 40dp gap where
 * one would be, would both be worse than the title simply starting at the content edge (FR-010).
 *
 * **[step] is the counter the design draws (`1 / 3`).** Feature 005 gave it callers: the Locks
 * screen is now the root, so step 1 genuinely has a predecessor and the count is a claim the app
 * can honour (005 FR-029, contract K5). `null` renders nothing, which is what the two root
 * screens pass and what keeps this a header rather than a wizard header.
 *
 * The `3` lives in the string resource, not here — the flow has three steps because the design
 * says so, not because the app happens to hold three stages, and a computed count would silently
 * relabel every step the day a fourth one is added (research R7).
 *
 * **The counter is announced as part of the title, not as a stop of its own.** It is information,
 * so hiding it from a screen reader would withhold the very thing US3 exists to give; but it is
 * not a control, and a separate focus stop reading "1 / 3" is a worse way to say "step 1 of 3".
 * Merging the title group's semantics gives one stop that reads both.
 */
@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)?,
    step: Int? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            BackTile(onBack = onBack)
        }
        // The title and the counter merge into **one** stop; the back tile deliberately sits
        // outside this group. A merge that reached the whole header row would absorb the tile's
        // `clickable` semantics too and cost the user their separate, actionable back target —
        // so the group starts after it.
        //
        // `weight(1f)` with no counter changes nothing that is visible: the title still draws at
        // the content edge, it merely has the rest of the row to draw into.
        Row(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = SlowLockType.Title,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (step != null) {
                val stepDescription =
                    stringResource(R.string.step_counter_description, step)
                // Pushed to the trailing edge, so the counter sits opposite the title however
                // long the title is.
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.step_counter, step),
                    style = SlowLockType.Footnote,
                    // Ink40 on Bone (C1, C3). Mono, because it is a number the user reads (C6).
                    color = MaterialTheme.colorScheme.outline,
                    // The spoken form replaces the glyphs the visible form uses; the merge above
                    // then reads it directly after the title, as one phrase.
                    modifier = Modifier.semantics {
                        contentDescription = stepDescription
                    },
                )
            }
        }
    }
}

/**
 * The back tile.
 *
 * It **draws** at 40dp and is **tappable** at 48dp. Drawn size and touch size are independent on
 * Android, and the back tile is not one of the two control groups FR-045 exempts from the
 * accessibility floor — so it meets the floor without the design paying for it (contract C10).
 *
 * `sizeIn` on the outer box rather than `size` on the tile: the touch target grows the hit area
 * without displacing the drawn tile, so the header still lines up with the artboard.
 */
@Composable
private fun BackTile(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.extraSmall
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp),
                role = Role.Button,
                onClick = onBack,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
