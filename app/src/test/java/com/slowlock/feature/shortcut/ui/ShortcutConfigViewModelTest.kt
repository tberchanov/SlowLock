package com.slowlock.feature.shortcut.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.DelayConfig
import com.slowlock.core.domain.DelayConfigRepository
import com.slowlock.core.domain.IconTreatment
import com.slowlock.feature.shortcut.domain.PinRequestResult
import com.slowlock.feature.shortcut.domain.ShortcutPinRepository
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The one branch this feature adds that a test can get wrong (research R8): a treatment restored
 * from the saved-state handle must win over the route argument.
 *
 * Getting it backwards is silent — the user picks Gray, the process is killed, and the screen comes
 * back showing the Original that was already on disk. Neither the compiler nor the device notices.
 *
 * Nothing here asserts that the library navigates, retains an entry or restores a history; that is
 * framework behaviour, which Principle VI prohibits testing (FR-034).
 */
class ShortcutConfigViewModelTest {

    /** The ordinary first open: the handle holds only the route's argument, so the route wins. */
    @Test
    fun `a handle with no selection opens on the route's treatment`() {
        val viewModel = viewModel(
            SavedStateHandle(mapOf(ROUTE_TREATMENT to IconTreatment.Invert)),
        )

        assertEquals(IconTreatment.Invert, viewModel.uiState.value.treatment)
    }

    /**
     * Process death mid-choice. The route argument is still the app's saved treatment and is still
     * in the handle; the *selection* is what the user was in the middle of, and it must survive.
     */
    @Test
    fun `a restored selection wins over the route's treatment`() {
        val viewModel = viewModel(
            SavedStateHandle(
                mapOf(
                    ROUTE_TREATMENT to IconTreatment.Original,
                    SELECTION to IconTreatment.Gray,
                ),
            ),
        )

        assertEquals(
            "a choice made before the process died must not be replaced by the saved value",
            IconTreatment.Gray,
            viewModel.uiState.value.treatment,
        )
    }

    /**
     * The write side of the same branch: a selection has to reach the handle, or there is nothing
     * for the test above to restore.
     */
    @Test
    fun `a selection is mirrored into the handle`() {
        val handle = SavedStateHandle(mapOf(ROUTE_TREATMENT to IconTreatment.Original))
        val viewModel = viewModel(handle)

        viewModel.onTreatmentSelected(IconTreatment.Gray)

        assertEquals(IconTreatment.Gray, handle.get<IconTreatment>(SELECTION))
        assertEquals(IconTreatment.Gray, viewModel.uiState.value.treatment)
    }

    private fun viewModel(savedState: SavedStateHandle) = ShortcutConfigViewModel(
        targets = FakeTargets,
        icons = FakeIcons,
        config = FakeConfig,
        pins = FakePins,
        savedState = savedState,
    )

    /** None of the four collaborators is reached by the branch under test. */
    private object FakeTargets : AppTargetRepository {
        override suspend fun resolve(packageName: String): AppTarget? = null
    }

    private object FakeIcons : AppIconRepository {
        override suspend fun icon(packageName: String, versionCode: Long): ImageBitmap? = null
        override suspend fun sweep(keep: List<String>) = Unit
    }

    private object FakeConfig : DelayConfigRepository {
        override suspend fun load(packageName: String): DelayConfig = DelayConfig.DEFAULT
        override suspend fun save(packageName: String, config: DelayConfig) = Unit
    }

    private object FakePins : ShortcutPinRepository {
        override suspend fun requestPin(
            target: AppTarget,
            treatment: IconTreatment,
        ): PinRequestResult = PinRequestResult.Requested
    }

    private companion object {
        const val ROUTE_TREATMENT = "treatment"
        const val SELECTION = "selectedTreatment"
    }
}
