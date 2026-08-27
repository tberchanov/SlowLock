package com.slowlock.feature.delay.domain

import com.slowlock.core.domain.DelayConfig
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The slider's bounds and the value it produces. Nothing here needs a device, which is the point:
 * the [DelayRange.SLIDER_STEPS] off-by-one produces a slider that looks entirely correct and lands
 * on the wrong seconds, and no amount of looking at a phone reliably catches it.
 */
class DelayRangeTest {

    /**
     * Material's `Slider` counts the stops *between* the endpoints, so `steps` is two fewer than
     * the number of reachable values. Both are derived in [DelayRange]; these literals are the
     * independent statement of what they should come to.
     */
    @Test
    fun `the stop count and the slider step count agree with the range`() {
        assertEquals(30, DelayRange.STOPS)
        assertEquals(28, DelayRange.SLIDER_STEPS)
    }

    @Test
    fun `snap clamps outside the range`() {
        assertEquals(DelayRange.MIN_SECONDS, DelayRange.snap(0))
        assertEquals(DelayRange.MIN_SECONDS, DelayRange.snap(-30))
        assertEquals(DelayRange.MAX_SECONDS, DelayRange.snap(1_000))
    }

    /**
     * Nearest, not floor — stated as a property over [DelayRange.STEP_SECONDS] rather than with
     * literals, because at a step of one second `snap` is the identity inside the range and there
     * is no 12-belongs-to-10 to write down. It survives the range changing again.
     */
    @Test
    fun `snap rounds to the nearest step`() {
        (DelayRange.MIN_SECONDS..DelayRange.MAX_SECONDS).forEach { seconds ->
            val snapped = DelayRange.snap(seconds)
            assertTrue(
                "snap($seconds) = $snapped moved further than half a step",
                abs(snapped - seconds) <= DelayRange.STEP_SECONDS / 2,
            )
        }
    }

    /** FR-005: there is no reachable value that is not a whole multiple of the step. */
    @Test
    fun `every snapped value is a multiple of the step`() {
        (-10..200).forEach { seconds ->
            val snapped = DelayRange.snap(seconds)
            assertEquals("snap($seconds) = $snapped", 0, snapped % DelayRange.STEP_SECONDS)
            assertTrue(
                "snap($seconds) = $snapped is outside the range",
                snapped in DelayRange.MIN_SECONDS..DelayRange.MAX_SECONDS,
            )
        }
    }

    /**
     * The default has to be a value the handle can rest on, or the readout disagrees with the
     * handle the moment the user touches it.
     */
    @Test
    fun `the default delay is a reachable slider stop`() {
        assertEquals(DelayConfig.DEFAULT_SECONDS, DelayRange.snap(DelayConfig.DEFAULT_SECONDS))
    }

    // ---- Presets (FR-017 to FR-019) ---------------------------------------------------------

    @Test
    fun `the presets are the three the design names`() {
        assertEquals(listOf(5, 10, 30), DelayRange.PRESETS)
    }

    /**
     * FR-019: a preset is a convenience over the range, never an alternative to it. One the slider
     * could not also reach would be a second, competing answer to what a legal delay is.
     */
    @Test
    fun `every preset is inside the range`() {
        DelayRange.PRESETS.forEach { preset ->
            assertTrue(
                "preset $preset is outside ${DelayRange.MIN_SECONDS}..${DelayRange.MAX_SECONDS}",
                preset in DelayRange.MIN_SECONDS..DelayRange.MAX_SECONDS,
            )
        }
    }

    /**
     * The bug this catches: tapping a preset and watching the slider move somewhere else. The
     * screen renders from the same value it sets, so a preset `snap` would shift cannot be
     * selected.
     */
    @Test
    fun `every preset is snap-stable`() {
        DelayRange.PRESETS.forEach { preset ->
            assertEquals("preset $preset is not a reachable stop", preset, DelayRange.snap(preset))
        }
    }

    @Test
    fun `presetFor finds a preset and only a preset`() {
        DelayRange.PRESETS.forEach { preset ->
            assertEquals(preset, DelayRange.presetFor(preset))
        }
        listOf(1, 4, 6, 9, 11, 17, 29).forEach { seconds ->
            assertNull("$seconds is not a preset", DelayRange.presetFor(seconds))
        }
    }

    /**
     * Dragging to a non-preset highlights nothing. Selection is derived rather than stored, so "no
     * preset selected" is a normal state with no flag anywhere that could fail to clear.
     */
    @Test
    fun `most values in the range are not presets`() {
        val presets = (DelayRange.MIN_SECONDS..DelayRange.MAX_SECONDS)
            .count { DelayRange.presetFor(it) != null }
        assertEquals(DelayRange.PRESETS.size, presets)
    }
}
