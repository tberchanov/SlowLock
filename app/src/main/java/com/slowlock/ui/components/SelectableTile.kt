package com.slowlock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * One choice in a single-choice row (contract U4), backing both the delay presets and the icon
 * treatments: they look different but *select* identically.
 *
 * A component rather than a styled `Box` because `Modifier.selectable` with [Role.RadioButton] is
 * what carries the selected state to assistive technology. A hand-rolled tile signalling selection
 * by fill colour alone would silently lose it, which FR-043 forbids.
 *
 * `contentDescription` is required, not optional: a preset's visible text is a bare value, and "5s"
 * would otherwise announce as two characters rather than as the action it performs (FR-044).
 *
 * This tile ships below the 48dp accessibility floor, deliberately — it draws at the size the
 * design specifies and applies no minimum touch target. FR-045 records the decision; C10 scopes it
 * to this component and the delay presets only. If re-decided, the fix is one `sizeIn` below.
 *
 * The *caller* wraps the row in `Modifier.selectableGroup()`.
 */
@Composable
fun SelectableTile(
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.small
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val outline = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val foreground = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .background(container)
            .border(BorderStroke(1.dp, outline), shape)
            // Merged into one node so the tile announces the caller's sentence once rather than
            // reading its children out separately. `selectable` still supplies role and state.
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            }
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        CompositionLocalProvider(LocalContentColor provides foreground, content = { content() })
    }
}
