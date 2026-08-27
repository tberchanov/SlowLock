package com.slowlock.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The store's frozen shape and its sanitising reads.
 *
 * Every failure these catch is silent: a renamed key or enum constant compiles clean, reads as
 * absent, and hands the user back the default while the app looks like it is working. There is no
 * build-time check and no migration; this class is the check.
 */
class DelayConfigTest {

    /**
     * The rename guard. These three strings are on disk on real devices, and renaming one is the
     * kind of tidy-up that looks safe and reverts every configured icon. It freezes the *names*,
     * not the order and not the matrices — adding a fourth treatment is allowed; renaming one is
     * not.
     */
    @Test
    fun `treatment tokens are frozen`() {
        assertEquals(
            listOf("Original", "Invert", "Gray"),
            IconTreatment.entries.map { it.name },
        )
    }

    /**
     * The rename guard the store itself could not carry: a frozen value the JVM suite cannot read
     * is a frozen value nothing protects. A renamed file is an empty file, and every configured app
     * reverts to the default, silently, after an update nobody asked for.
     */
    @Test
    fun `the store's preferences file is frozen`() {
        assertEquals("slowlock.delay-config", DELAY_CONFIG_FILE)
    }

    /** The key shapes, asserted against literals rather than rebuilt from the same suffixes. */
    @Test
    fun `store keys are frozen`() {
        assertEquals("com.example.app.delaySeconds", delayKey("com.example.app"))
        assertEquals("com.example.app.treatment", treatmentKey("com.example.app"))
    }

    /** FR-032: a shortcut with no configuration waits the default because the store said so. */
    @Test
    fun `absent delay reads as the default`() {
        assertEquals(DelayConfig.DEFAULT_SECONDS, delayFrom(null))
    }

    /** Zero and negatives are not waits. Sanitise to the default rather than throwing. */
    @Test
    fun `non-positive delay reads as the default`() {
        assertEquals(DelayConfig.DEFAULT_SECONDS, delayFrom(0))
        assertEquals(DelayConfig.DEFAULT_SECONDS, delayFrom(-5))
    }

    /**
     * The store does **not** clamp to [DelayRange]. A value outside the slider's range comes back
     * as stored, so a later range change cannot silently rewrite a value the user chose.
     */
    @Test
    fun `a delay outside the slider range is returned unchanged`() {
        assertEquals(1, delayFrom(1))
        assertEquals(600, delayFrom(600))
    }

    /** An absent or unrecognised token is the default treatment, never an error. */
    @Test
    fun `unknown treatment token reads as Original`() {
        assertEquals(IconTreatment.Original, treatmentFrom(null))
        assertEquals(IconTreatment.Original, treatmentFrom("Sepia"))
    }

    /** And a recognised one round-trips, so the previous test is not passing vacuously. */
    @Test
    fun `a known treatment token reads back as itself`() {
        assertEquals(IconTreatment.Gray, treatmentFrom("Gray"))
    }
}
