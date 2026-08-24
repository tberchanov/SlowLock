package com.slowlock.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import kotlin.math.pow

/**
 * Turns three review items into build failures.
 *
 * FR-002 fixes eleven colours, SC-008 sets a 4.5:1 contrast floor, and SC-009 forbids a twelfth
 * colour reaching the screen. All three were things a reviewer had to check by eye; this file
 * checks them by arithmetic and by reading the source tree.
 *
 * It runs on the JVM with no Android framework and no Robolectric, because
 * `androidx.compose.ui.graphics.Color` is a pure-Kotlin value class — the contrast maths is just
 * maths (research R13).
 */
class SlowLockPaletteTest {

    // ---- The eleven, asserted against literals -------------------------------------------

    @Test
    fun `palette holds exactly eleven tokens`() {
        assertEquals(
            "The palette is frozen at eleven (FR-002, contract C1). Adding a token is a change " +
                "to contracts/design-tokens.md, not just to Color.kt.",
            11,
            Palette.size,
        )
    }

    @Test
    fun `every token holds the value the contract froze`() {
        val frozen = mapOf(
            "Paper" to 0xFFEFEDEA,
            "Bone" to 0xFFF3F0EA,
            "Card" to 0xFFFBF9F5,
            "Ink" to 0xFF17150F,
            "Ink60" to 0xFF4A463C,
            "Ink40" to 0xFF6F6A5E,
            "Amber" to 0xFFC9821F,
            "AmberDark" to 0xFF8A5610,
            "AmberWash" to 0xFFF2E4CE,
            "Line" to 0xFFE3DED3,
            "Fill" to 0xFFE7E2D7,
        )
        assertEquals("Token names drifted from the contract", frozen.keys, Palette.keys)
        frozen.forEach { (name, argb) ->
            assertEquals("$name", Color(argb), Palette.getValue(name))
        }
    }

    /**
     * Guards the resource copy of the ground colour.
     *
     * `Theme.SlowLock`'s `windowBackground` is an XML resource, so it cannot reference the Kotlin
     * token (contract C5). The duplication is unavoidable; the drift is not. If these two ever
     * disagree, every cold launch flashes one colour and settles on another.
     */
    @Test
    fun `the window background resource matches the Bone token`() {
        val colors = resFile("values/colors.xml").readText()
        val hex = Regex("""<color name="screen_ground">#([0-9A-Fa-f]{8})</color>""")
            .find(colors)?.groupValues?.get(1)
            ?: fail("screen_ground is missing from values/colors.xml") as Nothing
        assertEquals(
            "screen_ground must equal the Bone token, or cold launch flashes (contract C5)",
            Bone,
            Color(hex.toLong(16)),
        )
    }

    // ---- Contrast, computed rather than eyeballed ----------------------------------------

    @Test
    fun `every declared text pairing clears the 4_5 to 1 floor`() {
        val failures = TextPairings.mapNotNull { (name, text, surface) ->
            val ratio = contrastRatio(text, surface)
            if (ratio < 4.5) "$name = %.2f:1".format(ratio) else null
        }
        assertTrue(
            "Pairings below SC-008's 4.5:1 floor: $failures",
            failures.isEmpty(),
        )
    }

    /**
     * The rule that gives `AmberDark` a reason to exist.
     *
     * Bright amber on the ground measures 2.76:1. C2 makes it a fill, a border and a rule — never
     * a glyph — and this test is what stops someone quietly adding it to [TextPairings] to make a
     * design read "warmer".
     */
    @Test
    fun `bright amber is not a legible text colour on any ground`() {
        listOf("Bone" to Bone, "Card" to Card, "Paper" to Paper).forEach { (name, surface) ->
            assertTrue(
                "Amber on $name measures ${"%.2f".format(contrastRatio(Amber, surface))}:1 — " +
                    "it is a fill, not a glyph. Use AmberDark (contract C2).",
                contrastRatio(Amber, surface) < 4.5,
            )
        }
        assertTrue(
            "AmberDark is the accent text token and must clear the floor on the ground",
            contrastRatio(AmberDark, Bone) >= 4.5,
        )
    }

    // ---- SC-009: no colour outside the palette reaches a screen --------------------------

    /**
     * Scans the source tree for colour literals.
     *
     * This is the only mechanism that actually enforces SC-009. Asserting the palette's size
     * proves the *declared* set is eleven; it proves nothing about a screen that writes
     * `Color(0xFFBADA55)` inline. Only `Color.kt` may hold a literal.
     */
    @Test
    fun `no source file outside Color_kt declares a colour literal`() {
        val root = File("src/main/java/com/slowlock")
        assertTrue(
            "Source root not found at ${root.absolutePath} — this test cannot silently pass",
            root.isDirectory,
        )

        val literal = Regex("""Color\(\s*0x[0-9A-Fa-f]{6,8}""")
        val offenders = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.name == "Color.kt" }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) -> literal.containsMatchIn(line) }
                    .map { (i, line) -> "${file.path}:${i + 1}  ${line.trim()}" }
            }
            .toList()

        assertTrue(
            "Colour literals must live in Color.kt and be referenced by token " +
                "(SC-009, contract C1):\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    // ---- WCAG 2.1 relative luminance ------------------------------------------------------

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun channel(v: Float): Double {
            val c = v.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun resFile(path: String): File {
        val file = File("src/main/res/$path")
        assertTrue("Resource not found at ${file.absolutePath}", file.isFile)
        return file
    }
}
