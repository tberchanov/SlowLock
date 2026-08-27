package com.slowlock.feature.delay.domain

import com.slowlock.core.domain.DelayConfig
import com.slowlock.core.domain.DelayConfigRepository
import com.slowlock.core.domain.IconTreatment
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Which delay the screen opens on (contract U19).
 *
 * Getting it backwards is silent: the user drags to 45, the process is killed, and the screen comes
 * back showing the 10 that was on disk. Neither the compiler nor the device notices, which is why
 * this branch has a test of its own rather than only the holder's.
 */
class LoadDelayConfigUseCaseTest {

    /** The ordinary first open: nothing has been edited, so the saved value is what shows. */
    @Test
    fun `with no edit in hand the saved delay is used`() = runTest {
        val opening = useCase(DelayConfig(30, IconTreatment.Gray))(NOTES, editedSeconds = null)

        assertEquals(DelayConfig(30, IconTreatment.Gray), opening)
    }

    /** U19: an edit in hand wins. */
    @Test
    fun `an edit in hand wins over the saved delay`() = runTest {
        val opening = useCase(DelayConfig(10, IconTreatment.Original))(NOTES, editedSeconds = 45)

        assertEquals(
            "an edit made before the process died must not be replaced by the stale saved value",
            45,
            opening.delaySeconds,
        )
    }

    /**
     * U19's other half: the treatment always comes from the read, because it has no edited
     * counterpart. An implementation that skipped the read when an edit was present would lose it.
     */
    @Test
    fun `the treatment always comes from the read, even with an edit in hand`() = runTest {
        val opening = useCase(DelayConfig(10, IconTreatment.Invert))(NOTES, editedSeconds = 45)

        assertEquals(IconTreatment.Invert, opening.treatment)
    }

    /** FR-032: a package with nothing stored reads as the default rather than as a special case. */
    @Test
    fun `a package with no stored configuration reads as the default`() = runTest {
        val opening = useCase(DelayConfig.DEFAULT)(NOTES, editedSeconds = null)

        assertEquals(DelayConfig.DEFAULT, opening)
    }

    private fun useCase(stored: DelayConfig) = LoadDelayConfigUseCase(FakeConfig(stored))

    private class FakeConfig(private val stored: DelayConfig) : DelayConfigRepository {
        override suspend fun load(packageName: String): DelayConfig = stored
        override suspend fun save(packageName: String, config: DelayConfig) =
            error("opening the delay screen must never write")
    }

    private companion object {
        const val NOTES = "com.example.notes"
    }
}
