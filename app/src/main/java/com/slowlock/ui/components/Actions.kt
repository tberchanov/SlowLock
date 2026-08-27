package com.slowlock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.slowlock.ui.theme.SlowLockType

/**
 * The two action shapes the design uses (contracts U2 and U3).
 *
 * Both wrap Material 3 buttons rather than replacing them: M3 already carries the ripple, the focus
 * and hover states, the disabled semantics and the 48dp minimum touch target. Only the height, the
 * radius and the colours are overridden.
 *
 * They live in one file because they are a pair. Split across two, they drift.
 */

/**
 * A screen's single most important action. Amber, full width, 56dp, 16dp corners.
 *
 * The label is Ink, not white: ink on amber measures 5.82:1, white on amber 2.4:1 (C2). That is why
 * the scheme maps `onPrimary` to the ink token.
 *
 * At most one per screen. No code enforces that, but a second one means the design has drifted.
 */
@Composable
fun PrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            // Material's defaults derive the disabled pair from `onSurface` at fixed alphas, which
            // lands outside the palette. Stating them keeps it inside the eleven (SC-009).
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Text(text = label, style = SlowLockType.Action)
    }
}

/**
 * The quieter half of a pair. Outlined, full width, 52dp. Here rather than in its one caller's file
 * because it is always shown with [PrimaryAction], and keeping them together stops one being
 * restyled without the other.
 */
@Composable
fun SecondaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            disabledContentColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(text = label, style = SlowLockType.ActionSecondary)
    }
}
