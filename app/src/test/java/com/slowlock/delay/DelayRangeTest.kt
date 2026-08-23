package com.slowlock.delay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The slider's bounds and the value it produces.
 *
 * Nothing here needs a device, which is the point: the [DelayRange.SLIDER_STEPS] off-by-one
 * produces a slider that looks entirely correct and lands on the wrong seconds, and no amount of
 * looking at a phone reliably catches it.
 */
class DelayRangeTest {

    /**
     * Material's `Slider` counts the stops **between** the endpoints, so `steps` is two fewer than
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
     * Nearest, not floor.
     *
     * Stated over [DelayRange.STEP_SECONDS] rather than with literals, because at a step of one
     * second every whole second is its own stop and `snap` is the identity inside the range —
     * there is no 12-belongs-to-10 to write down. The property is what actually matters and it
     * survives the range changing again: no value is ever moved further than half a step, which
     * a floor implementation would violate for any step above one.
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
     * The default has to be a value the handle can rest on. A default the slider cannot land on
     * would make the readout disagree with the handle the moment the user touched it.
     */
    @Test
    fun `the default delay is a reachable slider stop`() {
        assertEquals(DelayConfig.DEFAULT_SECONDS, DelayRange.snap(DelayConfig.DEFAULT_SECONDS))
    }
}
