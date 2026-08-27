package com.slowlock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * The app's theme. One scheme, no parameters.
 *
 * `dynamicColor` and `darkTheme` are deleted, not defaulted (FR-001, FR-008, C4). Dynamic colour
 * let the user's wallpaper decide SlowLock's appearance; a parameter left in place with a `false`
 * default is a parameter someone passes `true` to later.
 *
 * Light-only is a Phase 1 position, not a permanent one: every artboard is light, and a dark ramp
 * derived without a reviewed design would be a guess.
 *
 * The wait screen is not in this theme's scope at all — it resolves its own colours through
 * `values-night`, precisely so a change here cannot reach it (FR-031, FR-033).
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
 * Material 3's roles, filled from the eleven tokens in `Color.kt`. The mapping is one-directional:
 * tokens are the source, this scheme is derived, and reading a colour back out to define a token is
 * how a twelfth gets in without anyone deciding to add one.
 *
 * `onPrimary` is [Ink] rather than a light colour, deliberately: amber with a near-black label
 * measures 5.82:1, where white on amber would be 2.4:1 (C2).
 *
 * The `container` roles the design has no equivalent for point at the nearest honest token rather
 * than Material's defaults, which would introduce purples the palette does not contain.
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

    // Material's defaults here are reds — a twelfth colour waiting for the first component that
    // decides to show an error state. The app has no error-state UI, so this costs nothing
    // (SC-009).
    error = AmberDark,
    onError = Bone,
    errorContainer = AmberWash,
    onErrorContainer = AmberDark,
)
