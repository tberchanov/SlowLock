package com.slowlock.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii, per `contracts/design-tokens.md` C9.
 *
 * The design has five radii and Material 3 has five slots, so every token lands in one and nothing
 * needs to live outside the theme. The slot names are M3's; the meanings are the design's, and the
 * mapping below is the authority on which is which.
 *
 * | Slot | Radius | Used by |
 * |---|---|---|
 * | `extraSmall` | 12dp | List-row icons, delay presets, the back tile |
 * | `small` | 14dp | The search field, the icon-treatment tiles |
 * | `medium` | 16dp | Primary and secondary actions |
 * | `large` | 18dp | Cards in a list (feature 005's Locks rows) |
 * | `extraLarge` | 24dp | The shortcut preview card |
 *
 * [Pill] is the one shape with no slot: the app pill on the delay screen is its only user, and M3
 * has nowhere sensible to put a fully-rounded token.
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
 * The delay badge on a Locks row (007 FR-015).
 *
 * The second shape with no Material slot, and it is here for the same reason [Pill] is: C9's five
 * design radii already fill M3's five slots, 9dp is a real value on the `New · Locks` artboard, and
 * there is nowhere sensible to put it. Forcing it into [Shapes.extraSmall] (12dp) would put a
 * corner on screen that the design does not contain, and renumbering the slots to make room would
 * restyle every screen in the app.
 */
val Badge = RoundedCornerShape(9.dp)
