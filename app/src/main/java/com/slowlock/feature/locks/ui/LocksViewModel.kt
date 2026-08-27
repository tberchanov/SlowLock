package com.slowlock.feature.locks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.feature.locks.domain.LoadLocksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the Locks screen's state so it survives rotation without re-reading the sources behind it.
 *
 * Every collaborator arrives through the constructor (FR-024, V1, V2). What a row is and how the
 * list is derived belong to [LoadLocksUseCase]; the latch belongs to [LocksUiState.withLocks]. What
 * is left here is the call and the dialog.
 */
@HiltViewModel
class LocksViewModel @Inject constructor(
    private val loadLocks: LoadLocksUseCase,
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
     * Called on the Home entry's every `ON_RESUME`, which covers first launch, the return to the
     * foreground, the return from an uninstall, a language change, and the pop back from a
     * completed flow — including the case that needed its own call before: the launcher's pin
     * dialog does not reliably stop the activity, so `ON_START` alone would leave a new lock unseen
     * until the app was next backgrounded (N8, research R9).
     *
     * A refresh over a populated list replaces the rows in place and never clears `loaded` (FR-016)
     * — see [LocksUiState.withLocks], where that rule lives so a test can reach it.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.withLocks(loadLocks()) }
        }
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
