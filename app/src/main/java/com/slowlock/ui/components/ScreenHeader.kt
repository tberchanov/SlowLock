package com.slowlock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
 * **The step counter the design draws (`1 / 3`) is deliberately absent.** It belongs to Phase 2,
 * where the Locks screen becomes the root and the flow genuinely has a step 1 with a predecessor.
 * Until then the count would be a claim the app cannot honour, and a `step: Int?` parameter added
 * now would be a parameter with no caller.
 */
@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)?,
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
        Text(
            text = title,
            style = SlowLockType.Title,
            color = MaterialTheme.colorScheme.onBackground,
        )
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
