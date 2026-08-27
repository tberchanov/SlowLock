package com.slowlock.feature.shortcut.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowlock.R
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.DelayConfig
import com.slowlock.core.domain.DelayConfigRepository
import com.slowlock.core.domain.IconTreatment
import com.slowlock.feature.shortcut.domain.ShortcutPinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the shortcut screen's resolution, its icon, and the write-then-pin that ends the flow. Every
 * collaborator arrives through the constructor (FR-021, FR-024).
 *
 * The treatment selection is deliberately not held here — it stays `rememberSaveable` in
 * [ShortcutConfigScreen], because its drop-on-exit lifetime is specified behaviour delivered by the
 * root's `SaveableStateHolder`, and a holder scoped to the Activity would carry an abandoned choice
 * into the next app the user configures.
 */
@HiltViewModel
class ShortcutConfigViewModel @Inject constructor(
    private val targets: AppTargetRepository,
    private val icons: AppIconRepository,
    private val config: DelayConfigRepository,
    private val pins: ShortcutPinRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShortcutConfigUiState())
    val uiState: StateFlow<ShortcutConfigUiState> = _uiState.asStateFlow()

    private val _messages = Channel<Int>(Channel.BUFFERED)

    /**
     * One-shot messages for the screen to show and forget (FR-038). Taking a value from the channel
     * removes it, so no `onMessageShown()` clear survives for a caller to forget. Each element is a
     * string resource id, never a resolved string (V3), so this class needs no `Context`.
     */
    val messages: Flow<Int> = _messages.receiveAsFlow()

    /**
     * Resolves [packageName] and loads its icon. Idempotent per package: called from a
     * `LaunchedEffect` keyed on the package, so a rotation lands on the same answer.
     */
    fun start(packageName: String) {
        viewModelScope.launch {
            _uiState.value = ShortcutConfigUiState()
            val target = targets.resolve(packageName)
            if (target == null) {
                _uiState.update { it.copy(resolving = false, target = null, missing = true) }
                return@launch
            }
            _uiState.update { it.copy(resolving = false, target = target) }

            val icon = icons.icon(target.packageName, target.versionCode)
            _uiState.update { it.copy(iconLoading = false, icon = icon, iconFailed = icon == null) }
        }
    }

    /**
     * Re-resolve, save, pin, then report — in that order, and the order is the contract.
     *
     * The target may have been uninstalled while this screen sat open, so the resolution at tap
     * time is the one that counts. The configuration is written *before* the pin request goes out,
     * so a launcher that pins asynchronously can never fire the shortcut before its delay exists on
     * disk.
     *
     * Creating a lock is not a write: a lock exists exactly when its shortcut is pinned, so the pin
     * request creates it — and only if the user accepts the launcher's dialog.
     */
    fun create(packageName: String, delaySeconds: Int, treatment: IconTreatment, onCreated: () -> Unit) {
        _uiState.update { it.copy(creating = true) }
        viewModelScope.launch {
            val fresh = targets.resolve(packageName)
            if (fresh == null) {
                // The button comes back to life (state) and the user is told once (event).
                _uiState.update { it.copy(creating = false) }
                _messages.send(R.string.shortcut_target_unavailable)
                return@launch
            }
            config.save(packageName, DelayConfig(delaySeconds, treatment))
            // The result is deliberately not acted on: the launcher owns whether an icon appears
            // and never tells the app, so there is nothing to honestly report (FR-012).
            pins.requestPin(fresh, treatment)
            onCreated()
        }
    }
}

/**
 * What the shortcut screen is showing.
 *
 * Everything here is state the screen keeps showing. The snackbar message is not — it fires once
 * and is over, so it travels on [ShortcutConfigViewModel.messages] rather than as a nullable field
 * something has to remember to null out (FR-038).
 */
data class ShortcutConfigUiState(
    val resolving: Boolean = true,
    val target: AppTarget? = null,
    /** The package did not resolve. Distinct from [resolving]: "not yet" against "gone". */
    val missing: Boolean = false,
    val iconLoading: Boolean = true,
    /** Never enters a `StateFlow` that outlives the screen — this holder dies with it. */
    val icon: ImageBitmap? = null,
    val iconFailed: Boolean = false,
    val creating: Boolean = false,
) {

    /** The create action is live only with a target, an icon, and nothing already in flight. */
    val canCreate: Boolean get() = target != null && icon != null && !creating
}
