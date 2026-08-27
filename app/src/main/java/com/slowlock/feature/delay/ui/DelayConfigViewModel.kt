package com.slowlock.feature.delay.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.DelayConfigRepository
import com.slowlock.core.domain.IconTreatment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the delay being edited, the app it belongs to, and the treatment carried forward to the last
 * step. Every collaborator arrives through the constructor (FR-021, FR-024).
 *
 * Obtained inside the `DelayConfig` destination, so this holder outlives the trip to the icon step
 * and back — which is what returns the delay chosen on the way through rather than the one on disk
 * (G2) — and dies when that entry is popped.
 */
@HiltViewModel
class DelayConfigViewModel @Inject constructor(
    private val targets: AppTargetRepository,
    private val icons: AppIconRepository,
    private val config: DelayConfigRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DelayConfigUiState())
    val uiState: StateFlow<DelayConfigUiState> = _uiState.asStateFlow()

    /**
     * Resolves [packageName], loads its icon, and reads its saved configuration. Idempotent per
     * package: called from a `LaunchedEffect` keyed on the package, so a rotation lands on the same
     * answer.
     *
     * **A delay restored from the handle wins over the one on disk** (research R8, contract S4).
     * The handle only holds one after the user edited it, so letting the read supply the delay
     * would silently replace an edit made before the process died with the stale saved value.
     *
     * The read still happens on that path: the treatment has no reason to be saved, because
     * re-reading it gives the identical answer.
     */
    fun start(packageName: String) {
        viewModelScope.launch {
            val edited: Int? = savedState[DELAY_KEY]
            val saved = config.load(packageName)
            _uiState.update {
                it.copy(
                    seconds = edited ?: saved.delaySeconds,
                    treatment = saved.treatment,
                    loaded = true,
                )
            }

            val target = targets.resolve(packageName)
            if (target == null) {
                _uiState.update { it.copy(resolving = false, target = null, missing = true) }
                return@launch
            }
            _uiState.update { it.copy(resolving = false, target = target) }

            _uiState.update { it.copy(icon = icons.icon(target.packageName, target.versionCode)) }
        }
    }

    /**
     * Records the edited delay, mirrored into [SavedStateHandle] so it survives process death,
     * which this holder does not.
     */
    fun onSecondsChanged(seconds: Int) {
        savedState[DELAY_KEY] = seconds
        _uiState.update { it.copy(seconds = seconds) }
    }

    private companion object {

        /** This holder's own key. The route carries only the package name, so nothing collides. */
        const val DELAY_KEY = "editedDelaySeconds"
    }
}

/**
 * What the delay screen is showing.
 *
 * [loaded] is why the screen withholds its readout rather than showing a default and correcting
 * itself once the read returns (FR-002(c), FR-019). It is the same withholding the app pill already
 * does while the target resolves.
 */
data class DelayConfigUiState(
    val loaded: Boolean = false,
    /** Meaningless until [loaded]; the screen renders no readout before then. */
    val seconds: Int = 0,
    /** The app's saved treatment, carried to the icon step as a route argument on Next. */
    val treatment: IconTreatment = IconTreatment.entries.first(),
    val resolving: Boolean = true,
    val target: AppTarget? = null,
    /** The package did not resolve. Distinct from [resolving]: "not yet" against "gone". */
    val missing: Boolean = false,
    /** Never enters a `StateFlow` that outlives the screen — this holder dies with it. */
    val icon: ImageBitmap? = null,
)
