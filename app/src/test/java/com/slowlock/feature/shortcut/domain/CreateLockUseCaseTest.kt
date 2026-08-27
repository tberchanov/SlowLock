package com.slowlock.feature.shortcut.domain

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.DelayConfig
import com.slowlock.core.domain.DelayConfigRepository
import com.slowlock.core.domain.IconTreatment
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Making a lock, asserted as an ordered chain rather than as five calls that all happened.
 *
 * The ordering used to be split across a holder and a repository, with the gate inside the
 * repository — so no test could see that the configuration reaches disk before the launcher is
 * asked. That is the assertion this file exists for (contract U16-U18).
 */
class CreateLockUseCaseTest {

    /**
     * U16: the resolution at the moment of the attempt is the one that counts — the target may have
     * been uninstalled while the screen sat open. Nothing may be written and nothing pinned, or the
     * user gets a delay for an app that is gone, or an icon that opens nothing.
     */
    @Test
    fun `an unresolvable package writes nothing and pins nothing`() = runTest {
        val log = mutableListOf<String>()

        val result = useCase(log, resolves = false)(NOTES, 30, IconTreatment.Gray)

        assertEquals(CreateLockResult.TargetMissing, result)
        assertEquals(emptyList<String>(), log)
    }

    /**
     * U17, and the reason this use case is one class rather than two: the configuration is written
     * **before** the pin request goes out. A launcher that pins asynchronously could otherwise fire
     * the shortcut before its delay exists on disk, and the user would wait the default instead of
     * what they chose.
     */
    @Test
    fun `the configuration is written before the launcher is asked`() = runTest {
        val log = mutableListOf<String>()

        val result = useCase(log)(NOTES, 30, IconTreatment.Gray)

        assertEquals(listOf("save", "pin"), log)
        assertEquals(CreateLockResult.Created(PinRequestResult.Requested), result)
    }

    /** The saved record is what the user chose, not a default. */
    @Test
    fun `the delay and treatment the user chose are what is written`() = runTest {
        val config = RecordingConfig(mutableListOf())

        useCase(config = config)(NOTES, 45, IconTreatment.Invert)

        assertEquals(NOTES to DelayConfig(45, IconTreatment.Invert), config.saved)
    }

    /**
     * U18: the gate holds. The configuration is still written — that is today's behaviour and
     * FR-014 forbids changing it — but the launcher is never asked.
     */
    @Test
    fun `an unsupported launcher is never asked, though the configuration is still written`() =
        runTest {
            val log = mutableListOf<String>()

            val result = useCase(log, support = PinSupport.Unsupported)(NOTES, 30, IconTreatment.Gray)

            assertEquals(listOf("save"), log)
            assertEquals(CreateLockResult.Created(PinRequestResult.Unsupported), result)
        }

    /**
     * U18: `Unknown` is not an answer. Guessing `Supported` would put a pin request in front of a
     * launcher that may refuse it.
     */
    @Test
    fun `support that is still unknown does not open the gate`() = runTest {
        val log = mutableListOf<String>()

        val result = useCase(log, support = PinSupport.Unknown)(NOTES, 30, IconTreatment.Gray)

        assertEquals(listOf("save"), log)
        assertEquals(CreateLockResult.Created(PinRequestResult.Unsupported), result)
    }

    /**
     * U18: no icon, no request — and the launcher is not even asked about support. A pinned
     * shortcut is effectively permanent, so a placeholder on someone's home screen is worse than no
     * shortcut at all (C12).
     */
    @Test
    fun `a package whose icon cannot be produced is never pinned`() = runTest {
        val log = mutableListOf<String>()

        val result = useCase(log, icon = null)(NOTES, 30, IconTreatment.Gray)

        assertEquals(listOf("save"), log)
        assertEquals(CreateLockResult.Created(PinRequestResult.IconUnavailable), result)
    }

    private fun useCase(
        log: MutableList<String> = mutableListOf(),
        resolves: Boolean = true,
        support: PinSupport = PinSupport.Supported,
        icon: ImageBitmap? = StubIcon,
        config: DelayConfigRepository = RecordingConfig(log),
    ) = CreateLockUseCase(
        targets = FakeTargets(resolves),
        config = config,
        support = FakeSupport(support),
        icons = FakeIcons(icon),
        pins = RecordingPins(log),
    )

    private class FakeTargets(private val resolves: Boolean) : AppTargetRepository {
        override suspend fun resolve(packageName: String): AppTarget? =
            if (resolves) AppTarget(packageName, LABEL, VERSION) else null
    }

    /** Records both *that* a write happened, for ordering, and *what* was written. */
    private class RecordingConfig(private val log: MutableList<String>) : DelayConfigRepository {
        var saved: Pair<String, DelayConfig>? = null
        override suspend fun load(packageName: String): DelayConfig = DelayConfig.DEFAULT
        override suspend fun save(packageName: String, config: DelayConfig) {
            log += "save"
            saved = packageName to config
        }
    }

    private class FakeSupport(private val support: PinSupport) : PinSupportRepository {
        override suspend fun current(): PinSupport = support
    }

    private class FakeIcons(private val icon: ImageBitmap?) : AppIconRepository {
        override suspend fun icon(packageName: String, versionCode: Long): ImageBitmap? = icon
        override suspend fun sweep(keep: List<String>) = Unit
    }

    /** Shares [log] with the configuration fake, which is what makes the ordering assertable. */
    private class RecordingPins(private val log: MutableList<String>) : ShortcutPinRepository {
        override suspend fun requestPin(
            target: AppTarget,
            treatment: IconTreatment,
            icon: ImageBitmap,
        ) {
            log += "pin"
        }
    }

    /**
     * A non-null icon with no pixels behind it. `ImageBitmap(w, h)` needs a real Android graphics
     * stack; nothing here draws, so the identity of the icon is all that matters — only whether
     * one exists.
     */
    private object StubIcon : ImageBitmap {
        override val width = 1
        override val height = 1
        override val config = ImageBitmapConfig.Argb8888
        override val colorSpace: ColorSpace = ColorSpaces.Srgb
        override val hasAlpha = true
        override fun prepareToDraw() = Unit
        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int,
        ) = Unit
    }

    private companion object {
        const val NOTES = "com.example.notes"
        const val LABEL = "Notes"
        const val VERSION = 42L
    }
}
