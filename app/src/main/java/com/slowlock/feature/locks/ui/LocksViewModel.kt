package com.slowlock.feature.locks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.DelayConfigRepository
import com.slowlock.feature.locks.domain.LockOrderRepository
import com.slowlock.feature.locks.domain.PinnedShortcutsRepository
import com.slowlock.feature.locks.domain.assembleLocks
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the Locks screen's state so it survives rotation without re-reading three sources.
 *
 * Every collaborator arrives through the constructor (FR-024, V1, V2); [AppTargetRepository] is the
 * seam [LocksViewModelTest] substitutes to drive the null-resolution path (FR-020).
 *
 * This class holds only the wiring. Everything decidable lives outside it — the rows in
 * [assembleLocks], the latch in [LocksUiState.withLocks] — because `viewModelScope` dispatches on
 * `Dispatchers.Main`, so nothing worth asserting may live behind it.
 */
@HiltViewModel
class LocksViewModel @Inject constructor(
    private val lockOrder: LockOrderRepository,
    private val pinnedShortcuts: PinnedShortcutsRepository,
    private val config: DelayConfigRepository,
    private val targets: AppTargetRepository,
    /**
     * Exposed so each row loads its own icon lazily as it scrolls into view, and so [refresh] does
     * not wait for a rasterization it does not need (FR-015). Icons never enter [uiState].
     */
    val icons: AppIconRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocksUiState())
    val uiState: StateFlow<LocksUiState> = _uiState.asStateFlow()

    /**
     * Re-reads the locks, their configuration and their display facts — one pass, off the main
     * thread (FR-040).
     *
     * Called on every `ON_START` — which covers first launch, the return to the foreground, the
     * return from an uninstall and a language change, with nothing left running while the app is
     * away (SC-013) — and on completing the flow (N8). The latter needs its own call because it is
     * *not* a lifecycle event: the launcher's pin dialog does not reliably stop the activity, so a
     * new lock would otherwise sit unseen until the app was next backgrounded. The returned [Job]
     * lets the root wait for the read before it navigates, so the lock is there in the first frame.
     *
     * A refresh over a populated list replaces the rows in place and never clears `loaded` (FR-016)
     * — see [LocksUiState.withLocks], where that rule lives so a test can reach it.
     */
    fun refresh(): Job = viewModelScope.launch {
        // The list is derived from the launcher, not read from a record (FR-003a): a lock exists
        // exactly when its shortcut is pinned, which is what makes the screen agree with the home
        // screen — a declined pin dialog never creates one, and an icon dragged off takes its lock.
        //
        // `null` is not an empty set. It means the launcher could not be asked, and the only safe
        // reading of "we don't know" is the last list that was known good.
        val pinned = pinnedShortcuts.pinnedIds()
        val packages =
            if (pinned == null) lockOrder.loadOrder() else lockOrder.deriveOrder(pinned)
        val locks = assembleLocks(packages, config::load, targets::resolve)
        _uiState.update { it.withLocks(locks) }
    }

    /**
     * Opens the "how do I remove this?" explanation for one lock (FR-021, contract K4).
     *
     * There is no confirming counterpart: a lock is its pinned shortcut (FR-003a), Android offers
     * no way to unpin one, and an in-app "Remove" that merely hid the row would leave the icon
     * still opening the app while the list started lying about what a lock is.
     *
     * At most one is ever open, because there is only one place for a package to sit.
     */
    fun onExplainRemoval(packageName: String) {
        _uiState.update { it.copy(explainingRemoval = packageName) }
    }

    /** Closes the explanation. Nothing else happens, because nothing else was pending. */
    fun onDismissExplanation() {
        _uiState.update { it.copy(explainingRemoval = null) }
    }
}
