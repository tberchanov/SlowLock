package com.slowlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.DelayConfig
import com.slowlock.core.domain.DelayConfigRepository
import com.slowlock.feature.shortcut.domain.PinSupport
import com.slowlock.feature.shortcut.domain.PinSupportRepository
import com.slowlock.feature.shortcut.domain.pinSupport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The launcher's pin support, and the configuration read that precedes navigation.
 *
 * The navigation stage is deliberately not here: which screen is showing is presentation state, and
 * `rememberSaveable` in [SlowLockRoot] already delivers the specified process-death restore. Moving
 * it to a `SavedStateHandle` would put that at risk for no principle gained (FR-023a).
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val config: DelayConfigRepository,
    private val pinSupport: PinSupportRepository,
    /**
     * Handed to `DelayConfigScreen`, which deliberately has no state holder of its own (FR-023,
     * V4). It still has to resolve a label and an icon, and the root is the only place those can
     * come from without the screen constructing a data source at a point of use (FR-024).
     */
    val targets: AppTargetRepository,
    val icons: AppIconRepository,
) : ViewModel() {

    private val _support = MutableStateFlow<PinSupport>(PinSupport.Unknown)

    /**
     * The launcher's answer, or [PinSupport.Unknown] until it has been asked.
     *
     * `Unknown` is not an answer and must never be rendered as one — showing the list on a device
     * that cannot pin, or an error to everyone else, is the flash it exists to prevent. Held rather
     * than saved, because a saved answer could be restored under a different launcher (FR-028).
     */
    val support: StateFlow<PinSupport> = _support.asStateFlow()

    /**
     * Re-reads support on every `ON_START` — the one hook that fires both on first launch and on
     * every return to the foreground, the pair of moments FR-028 names.
     */
    fun refreshSupport() {
        viewModelScope.launch { _support.value = pinSupport.current() }
    }

    /**
     * The saved configuration for [packageName], read *before* the root navigates (N1), so the
     * delay screen's first composition already carries the saved values and never shows the default
     * and then corrects itself.
     *
     * Never null: an app with nothing stored answers [DelayConfig.DEFAULT], so "configured" and
     * "unconfigured" are one code path with no branch to forget.
     */
    suspend fun configFor(packageName: String): DelayConfig = config.load(packageName)
}
