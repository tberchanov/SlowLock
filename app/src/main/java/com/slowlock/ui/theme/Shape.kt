package com.slowlock.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii, per C9. The design has five radii and Material 3 has five slots:
 *
 * | Slot | Radius | Used by |
 * |---|---|---|
 * | `extraSmall` | 12dp | List-row icons, delay presets, the back tile |
 * | `small` | 14dp | The search field, the icon-treatment tiles |
 * | `medium` | 16dp | Primary and secondary actions |
 * | `large` | 18dp | Cards in a list (the Locks rows) |
 * | `extraLarge` | 24dp | The shortcut preview card |
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** The app pill on the delay screen. Fully rounded at any height. */
val Pill = RoundedCornerShape(percent = 50)

/**
 * The delay badge on a Locks row (FR-015). Outside [Shapes] for the same reason [Pill] is: C9's
 * five radii already fill M3's five slots. Forcing 9dp into `extraSmall` would put a corner on
 * screen the design does not contain, and renumbering the slots would restyle every screen.
 */
val Badge = RoundedCornerShape(9.dp)
