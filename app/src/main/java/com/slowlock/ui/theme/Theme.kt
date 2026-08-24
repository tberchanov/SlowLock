package com.slowlock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * The app's theme. One scheme, no parameters.
 *
 * **`dynamicColor` and `darkTheme` are deleted, not defaulted** (FR-001, FR-008, contract C4,
 * research R5). Before feature 004 this function took both, and dynamic colour was on by default —
 * which meant SlowLock's appearance was decided by the user's wallpaper and differed on every
 * device. A parameter left in place with a `false` default would be a parameter someone passes
 * `true` to later; removing them is what makes FR-001 enforceable rather than merely stated.
 *
 * Light-only is a **Phase 1 position, not a permanent one**. Every artboard in the design source
 * is light, and a dark ramp derived without a reviewed design would be a guess. Phase 3 builds it
 * from the same tokens and restores a `darkTheme` path here.
 *
 * The one surface that still follows the system light/dark setting is the wait screen — it is not
 * in this theme's scope at all, resolves its own colours through `values-night`, and does so
 * precisely so a change here cannot reach it (FR-031, FR-033).
 */
@Composable
fun SlowLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SlowLockColorScheme,
        typography = SlowLockTypography,
        shapes = Shapes,
        content = content,
    )
}

/**
 * Material 3's roles, filled from the eleven tokens in `Color.kt`.
 *
 * The mapping is one-directional and stated in `data-model.md` §1: tokens are the source, this
 * scheme is derived. Reading a colour back out of the scheme to define a token would invert that
 * and is how a twelfth colour gets in without anyone deciding to add one.
 *
 * `onPrimary` is [Ink] rather than a light colour, which is unusual and deliberate: the primary
 * action is amber with a near-black label, measuring 5.82:1. White on amber would be 2.4:1 and
 * illegible (C2, research R14).
 *
 * The `container` roles that the design has no equivalent for are pointed at the nearest honest
 * token rather than left to Material's defaults, which would introduce purples the palette does
 * not contain.
 */
private val SlowLockColorScheme = lightColorScheme(
    primary = Amber,
    onPrimary = Ink,
    primaryContainer = AmberWash,
    onPrimaryContainer = AmberDark,

    secondary = AmberDark,
    onSecondary = Bone,
    secondaryContainer = AmberWash,
    onSecondaryContainer = AmberDark,

    tertiary = AmberDark,
    onTertiary = Bone,
    tertiaryContainer = AmberWash,
    onTertiaryContainer = AmberDark,

    background = Bone,
    onBackground = Ink,

    surface = Bone,
    onSurface = Ink,
    surfaceVariant = Fill,
    onSurfaceVariant = Ink60,

    surfaceContainerLowest = Card,
    surfaceContainerLow = Card,
    surfaceContainer = Card,
    surfaceContainerHigh = Bone,
    surfaceContainerHighest = Fill,

    inverseSurface = Ink,
    inverseOnSurface = Bone,

    outline = Ink40,
    outlineVariant = Line,

    scrim = Ink,

    // Material's defaults here are reds, and a red is a twelfth colour waiting for the first
    // component that decides to show an error state. The app has no error-state UI today, so
    // pointing these at the palette costs nothing and closes the hole SC-009 would otherwise
    // leave open.
    error = AmberDark,
    onError = Bone,
    errorContainer = AmberWash,
    onErrorContainer = AmberDark,
)
