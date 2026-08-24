package com.slowlock.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SlowLock's palette. **Eleven colours, and that is the whole of it.**
 *
 * `contracts/design-tokens.md` C1 freezes this set; FR-002 fixes the values; SC-009 forbids a
 * twelfth. `SlowLockPaletteTest` asserts the count, each literal, and the contrast of every
 * pairing in [TextPairings] — so a colour added here without a pairing, or a literal nudged by a
 * digit, fails the build rather than a review.
 *
 * These are the *source*. The Material 3 `ColorScheme` in `Theme.kt` is derived from them, never
 * the reverse, and no screen may declare a colour of its own — `SlowLockPaletteTest` scans the
 * source tree for that too.
 *
 * Feature 004 replaced the `Purple80`/`Pink40` scaffolding the project was generated with. That
 * was never a palette; it was the Compose template's placeholder, and dynamic colour meant it
 * barely rendered anyway.
 */

/** The canvas behind everything. The app's outermost ground. */
val Paper = Color(0xFFEFEDEA)

/** Every screen's ground, and the wait screen's light background. */
val Bone = Color(0xFFF3F0EA)

/** Raised surfaces: the search field, the preview card, unselected tiles, the slider thumb. */
val Card = Color(0xFFFBF9F5)

/** Headings, body copy, and the label on the amber primary action. */
val Ink = Color(0xFF17150F)

/** Secondary body copy. */
val Ink60 = Color(0xFF4A463C)

/** Captions, eyebrows, unit labels, the wait message. Never body copy — see [TextPairings]. */
val Ink40 = Color(0xFF6F6A5E)

/**
 * The accent. A fill, a border, and a rule — **never a glyph on the ground**.
 *
 * Amber on [Bone] measures 2.76:1, well under the 4.5:1 floor (C2, research R14). It is legible
 * only the other way round: [Ink] on amber is 5.82:1, which is what the primary action uses. Where
 * an accent-coloured *word* is wanted, the token is [AmberDark].
 */
val Amber = Color(0xFFC9821F)

/** The only accent token permitted as text: 5.40:1 on [Bone], 5.84:1 on [Card]. */
val AmberDark = Color(0xFF8A5610)

/** The selected tile's fill, and the delay badge's. */
val AmberWash = Color(0xFFF2E4CE)

/** Hairline borders. */
val Line = Color(0xFFE3DED3)

/**
 * Icon placeholders, the slider's inactive track, and the app-list row divider.
 *
 * Two things here differ from a naive reading of the design source, both deliberate (C1, C9):
 * the artboard draws the inactive slider track at `#E4DFD4` — three points per channel from this
 * token and indistinguishable from it — and the row divider at this value rather than at [Line].
 */
val Fill = Color(0xFFE7E2D7)

/**
 * The declared set, by name. Exactly eleven.
 *
 * Exists so the count is a thing a test can assert rather than a thing a reviewer must count.
 */
val Palette: Map<String, Color> = mapOf(
    "Paper" to Paper,
    "Bone" to Bone,
    "Card" to Card,
    "Ink" to Ink,
    "Ink60" to Ink60,
    "Ink40" to Ink40,
    "Amber" to Amber,
    "AmberDark" to AmberDark,
    "AmberWash" to AmberWash,
    "Line" to Line,
    "Fill" to Fill,
)

/**
 * Every text-on-surface pairing the design actually uses.
 *
 * C3 makes this list binding in both directions: each pairing must clear 4.5:1, **and a pairing
 * that is not in this list is not permitted on screen**. Adding a text colour to a surface means
 * adding the pair here first and letting the test judge it.
 *
 * `Amber to Bone` is deliberately absent. It fails at 2.76:1, which is the whole reason
 * [AmberDark] exists.
 */
val TextPairings: List<Triple<String, Color, Color>> = listOf(
    Triple("Ink on Card", Ink, Card),
    Triple("Ink on Bone", Ink, Bone),
    Triple("Ink on Paper", Ink, Paper),
    Triple("Ink60 on Card", Ink60, Card),
    Triple("Ink60 on Bone", Ink60, Bone),
    Triple("Ink on Amber", Ink, Amber),
    Triple("AmberDark on Card", AmberDark, Card),
    Triple("AmberDark on Bone", AmberDark, Bone),
    Triple("Ink40 on Card", Ink40, Card),
    Triple("AmberDark on AmberWash", AmberDark, AmberWash),
    Triple("Ink40 on Bone", Ink40, Bone),
)
