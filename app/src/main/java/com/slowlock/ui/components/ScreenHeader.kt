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
 * A screen's title, optionally preceded by a back tile (contract U1).
 *
 * `onBack == null` renders no tile and no leading space: a disabled tile or an empty 40dp gap would
 * both be worse than the title starting at the content edge (FR-010).
 *
 * [step] is the counter the design draws (`1 / 3`); `null` renders nothing, which is what the root
 * screens pass. The `3` lives in the string resource, not here — a computed count would silently
 * relabel every step the day a fourth one is added.
 *
 * The counter is announced as part of the title rather than as a stop of its own: it is
 * information, not a control, and a separate focus stop reading "1 / 3" is a worse way to say "step
 * 1 of 3".
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
        // The title and the counter merge into one stop, and the back tile deliberately sits
        // outside the group: a merge reaching the whole row would absorb the tile's `clickable`
        // semantics and cost the user their separate, actionable back target.
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
                // Trailing edge, so the counter sits opposite the title however long it is.
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.step_counter, step),
                    style = SlowLockType.Footnote,
                    color = MaterialTheme.colorScheme.outline,
                    // The spoken form replaces the visible glyphs; the merge above then reads it
                    // directly after the title, as one phrase.
                    modifier = Modifier.semantics {
                        contentDescription = stepDescription
                    },
                )
            }
        }
    }
}

/**
 * The back tile: drawn at 40dp, tappable at 48dp. It is not one of the two control groups FR-045
 * exempts from the accessibility floor, so it meets the floor without the design paying for it
 * (C10).
 *
 * `sizeIn` on the outer box rather than `size` on the tile, so the touch target grows the hit area
 * without displacing the drawn tile.
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
