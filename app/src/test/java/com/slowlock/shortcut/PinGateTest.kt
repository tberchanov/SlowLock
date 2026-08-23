package com.slowlock.shortcut

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FR-013 and Constitution IV: `isRequestPinShortcutSupported()` MUST gate **every** pin attempt.
 *
 * The root already refuses to show the configuration screen on an unsupported launcher (FR-029),
 * so it is tempting to treat this second check as redundant. It is not: support is re-read at the
 * moment of the pin because the user can change launcher while the screen is open, and the
 * root's answer was true when the screen opened, not now.
 *
 * Driven through the injectable seams from `PinSupport` and [pinWhenSupported], so both answers
 * are reachable with no launcher, no `Context`, and no instrumented suite.
 */
class PinGateTest {

    @Test
    fun `no pin is attempted when the launcher does not support pinning`() = runBlocking {
        var attempted = false

        val requested = pinWhenSupported(
            support = { pinSupport { false } },
            pin = { attempted = true },
        )

        assertFalse(requested)
        assertFalse(attempted)
    }

    @Test
    fun `the pin is attempted when the launcher supports pinning`() = runBlocking {
        var attempted = false

        val requested = pinWhenSupported(
            support = { pinSupport { true } },
            pin = { attempted = true },
        )

        assertTrue(requested)
        assertTrue(attempted)
    }

    /**
     * `Unknown` means the question has not been asked, and is never either answer
     * (data-model.md §`PinSupport`). Guessing `Supported` here would put a pin request in front
     * of a launcher that may refuse it.
     */
    @Test
    fun `no pin is attempted while support is still unknown`() = runBlocking {
        var attempted = false

        val requested = pinWhenSupported(
            support = { PinSupport.Unknown },
            pin = { attempted = true },
        )

        assertFalse(requested)
        assertFalse(attempted)
    }

    /** The check is made at the moment of the pin, not read from a value captured earlier. */
    @Test
    fun `support is re-read on every attempt`() = runBlocking {
        var checks = 0
        val support = { pinSupport { checks++; true } }

        pinWhenSupported(support) {}
        pinWhenSupported(support) {}

        assertTrue("expected one check per attempt, got $checks", checks == 2)
    }
}
