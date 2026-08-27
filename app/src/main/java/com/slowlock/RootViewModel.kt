package com.slowlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * The launcher's pin support: whether this launcher will take a pinned shortcut at all.
 *
 * **The one holder scoped above the graph, deliberately** (FR-016). Pin support decides whether the
 * graph renders at all and is re-read on every return to the foreground, because the user can
 * change launcher while the app is away. It belongs to no screen, so there is no entry to scope it
 * to — see the plan's Complexity Tracking for the two alternatives that were rejected.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val pinSupport: PinSupportRepository,
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
}
