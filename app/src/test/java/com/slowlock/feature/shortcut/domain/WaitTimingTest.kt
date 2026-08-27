package com.slowlock.feature.shortcut.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The wait's arithmetic, including the case that only ever happens on a real device: coming back
 * to a deadline that has already passed.
 *
 * This project has no instrumented suite (Constitution: "No automated test may drive a device"),
 * so these three assertions are the entire automated check on the timing. They are possible at all
 * because [deadlineFrom] and [remainingMillis] take "now" as a parameter instead of reading
 * `SystemClock` themselves.
 */
class WaitTimingTest {

    @Test
    fun `the deadline is now plus the delay in milliseconds`() {
        assertEquals(11_000L, deadlineFrom(nowElapsedMillis = 1_000L, delaySeconds = 10))
    }

    @Test
    fun `remaining time counts down towards a deadline still ahead`() {
        assertEquals(
            7_500L,
            remainingMillis(deadlineElapsedMillis = 11_000L, nowElapsedMillis = 3_500L),
        )
    }

    /**
     * The restored-deadline case: a rotation, or a return from the background, after the wait
     * would already have ended. `delay(-4)` must be unreachable, so this is `0` and never negative.
     */
    @Test
    fun `a deadline already passed has no time remaining`() {
        assertEquals(
            0L,
            remainingMillis(deadlineElapsedMillis = 11_000L, nowElapsedMillis = 11_004L),
        )
        assertEquals(
            0L,
            remainingMillis(deadlineElapsedMillis = 11_000L, nowElapsedMillis = 900_000L),
        )
    }
}
