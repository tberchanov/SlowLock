package com.slowlock.core.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The matrices are invisible until they are permanent: a wrong one looks plausible in the preview,
 * gets baked into a pinned shortcut, and stays on the user's home screen where SlowLock can neither
 * find it nor fix it. Manual testing catches a blatantly wrong icon, not a subtly wrong one.
 *
 * These assertions are also what makes the "literal constants, no framework call" rule in
 * [IconTreatment] enforceable: a derived `Gray` would be unassertable on the JVM.
 */
class IconTreatmentTest {

    /** FR-005, FR-006: exactly three, in display order, with Original first. */
    @Test
    fun `the treatments are exactly Original Invert Gray in that order`() {
        assertEquals(
            listOf(IconTreatment.Original, IconTreatment.Invert, IconTreatment.Gray),
            IconTreatment.entries,
        )
        assertEquals(IconTreatment.Original, IconTreatment.entries.first())
    }

    /**
     * The constant names are the persisted treatment token (F2). `DelayConfigStore` writes
     * `IconTreatment.name` and `treatmentFrom` matches on it, so a rename here is not a rename — it
     * is an unrecognised token that reads as [IconTreatment.Original] and reverts every configured
     * icon. Adding a fourth treatment is allowed by the contract; renaming one is not.
     */
    @Test
    fun `the constant names are frozen`() {
        assertEquals(
            listOf("Original", "Invert", "Gray"),
            IconTreatment.entries.map { it.name },
        )
    }

    /** `Original` applies no filter at all, rather than an identity one. */
    @Test
    fun `Original has no matrix`() {
        assertNull(IconTreatment.Original.matrix)
    }

    /**
     * The alpha-row trap: an inverted alpha row turns transparent icon corners opaque black and
     * makes every inverted icon a solid square.
     */
    @Test
    fun `Invert negates the colour channels and leaves alpha identity`() {
        val matrix = requireNotNull(IconTreatment.Invert.matrix)

        assertEquals(-1f, matrix[RED_TO_RED], 0f)
        assertEquals(-1f, matrix[GREEN_TO_GREEN], 0f)
        assertEquals(-1f, matrix[BLUE_TO_BLUE], 0f)
        assertEquals(255f, matrix[RED_OFFSET], 0f)
        assertEquals(255f, matrix[GREEN_OFFSET], 0f)
        assertEquals(255f, matrix[BLUE_OFFSET], 0f)
        assertArrayEquals(IDENTITY_ALPHA_ROW, alphaRowOf(matrix), 0f)
    }

    /** The literal coefficients `ColorMatrix.setSaturation(0f)` produces, on all three rows. */
    @Test
    fun `Gray applies the luminance coefficients to every colour row`() {
        val matrix = requireNotNull(IconTreatment.Gray.matrix)

        repeat(COLOUR_ROWS) { row ->
            assertArrayEquals(LUMINANCE_ROW, rowOf(matrix, row), 0f)
        }
        assertArrayEquals(IDENTITY_ALPHA_ROW, alphaRowOf(matrix), 0f)
    }

    /** A matrix of the wrong length throws at the point of use, which is a device away. */
    @Test
    fun `every non-null matrix is exactly twenty floats`() {
        IconTreatment.entries.mapNotNull { it.matrix }.forEach { matrix ->
            assertEquals(ROWS * COLUMNS, matrix.size)
        }
    }

    private fun rowOf(matrix: FloatArray, row: Int): FloatArray =
        matrix.copyOfRange(row * COLUMNS, (row + 1) * COLUMNS)

    private fun alphaRowOf(matrix: FloatArray): FloatArray = rowOf(matrix, ALPHA_ROW)

    private companion object {
        const val ROWS = 4
        const val COLUMNS = 5
        const val COLOUR_ROWS = 3
        const val ALPHA_ROW = 3

        const val RED_TO_RED = 0
        const val RED_OFFSET = 4
        const val GREEN_TO_GREEN = 6
        const val GREEN_OFFSET = 9
        const val BLUE_TO_BLUE = 12
        const val BLUE_OFFSET = 14

        val IDENTITY_ALPHA_ROW = floatArrayOf(0f, 0f, 0f, 1f, 0f)
        val LUMINANCE_ROW = floatArrayOf(0.213f, 0.715f, 0.072f, 0f, 0f)
    }
}
