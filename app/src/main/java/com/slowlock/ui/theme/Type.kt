package com.slowlock.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.slowlock.R

/**
 * SlowLock's two typefaces and the roles they fill.
 *
 * Sizes, weights and letter-spacing are read off the artboards and fixed in
 * `specs/004-visual-redesign/data-model.md` §2, which `contracts/design-tokens.md` C6 makes part
 * of the contract: a screen using the right family at the wrong size is not aligned with the
 * design.
 *
 * **Both families are ordinary resource fonts, which Compose loads with a blocking strategy**
 * (research R2). That is load-bearing rather than incidental — FR-003 forbids a substitute face
 * rendering and then swapping, and FR-029 forbids the wait screen changing at all once it is up.
 * A downloadable font would put exactly that swap on the one screen that must not move, and would
 * need a network the app does not have.
 *
 * Five weight files ship and **no weight is synthesised**: every [FontWeight] named below has a
 * file behind it (C7). Asking for a weight that was not shipped makes the renderer fake it, which
 * is why the families below list weights explicitly instead of leaving Compose to interpolate.
 */

/** Prose: headings, sentences, app labels, action labels. Latin coverage. */
val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans_regular, FontWeight.Normal),
    Font(R.font.instrument_sans_medium, FontWeight.Medium),
    Font(R.font.instrument_sans_semibold, FontWeight.SemiBold),
)

/**
 * Numerals and labels. **Every number the user reads is set in this face** (FR-004) — the delay
 * is the product's central value, and setting it in mono is how the design says so.
 */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

/**
 * The app's own type roles, named for what they are rather than for a Material size step.
 *
 * These exist alongside [SlowLockTypography] rather than inside it because the design assigns type
 * by *role* — a number is mono because it is a number, not because it sits at a particular step of
 * a scale. Stock Material components (the slider, the snackbar, the text field) read the M3
 * typography below; screens written for this design read these.
 *
 * **The wait screen deliberately uses none of them.** FR-033 requires it to resolve its own type
 * so a change here cannot reach it by accident; its one style is declared in `WaitScreen.kt`.
 */
object SlowLockType {

    /** Screen titles: "Choose an app", "Wait before opening", "New lock". */
    val Title = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        letterSpacing = (-0.01).em,
    )

    /** The unsupported-launcher screen's explanation. Same size as [Title], lighter weight. */
    val Message = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.01).em,
    )

    /** Sentences, the search placeholder, the preview card's app label, empty and error states. */
    val Body = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )

    /** App-list row labels. */
    val RowLabel = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
    )

    /** The app pill on the delay screen. */
    val PillLabel = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
    )

    /** Icon-treatment tile names. Medium when selected, Regular when not. */
    val TileName = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
    )

    /** The primary action's label. */
    val Action = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    )

    /** The secondary action's label. */
    val ActionSecondary = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
    )

    /**
     * The delay numeral — the largest thing on its screen.
     *
     * 104sp is the **cap**, not a fixed size: FR-014a makes this the element that yields when the
     * font scale grows or the screen shrinks, so the delay screen renders it with auto-sizing
     * bounded by this value (C11, research R10).
     */
    val Readout = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 104.sp,
        lineHeight = 104.sp,
        letterSpacing = (-0.03).em,
    )

    /** The `SECONDS` caption under the readout. */
    val Caption = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.2.em,
    )

    /** Delay preset labels: "5s", "10s", "30s". Medium when selected, Regular when not. */
    val Preset = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    )

    /** Footnotes: "Your launcher will ask you to confirm." Also the preview card's delay line. */
    val Footnote = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    )

    /** The unsupported-launcher screen's `NO ROOM ON THE HOME SCREEN`. */
    val Eyebrow = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.14.em,
    )

    /** The `ICON` label above the treatment tiles. */
    val EyebrowSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.14.em,
    )

    /** The slider's `1s` / `30s` end labels. */
    val Tick = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
    )
}

/**
 * Material 3's scale, in Instrument Sans.
 *
 * Its job is the components this app does not write itself — the slider, the snackbar, the text
 * field — so that nothing renders in the platform's default face. Screens written for this design
 * use [SlowLockType] instead, which is why only the slots those components actually reach are
 * overridden here.
 */
val SlowLockTypography = Typography(
    titleLarge = SlowLockType.Title,
    titleMedium = SlowLockType.PillLabel,
    bodyLarge = SlowLockType.Body,
    bodyMedium = SlowLockType.Body.copy(fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = SlowLockType.Body.copy(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = SlowLockType.Action,
    labelMedium = SlowLockType.ActionSecondary,
    labelSmall = SlowLockType.TileName,
)
