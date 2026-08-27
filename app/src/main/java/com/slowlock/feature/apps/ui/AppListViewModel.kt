package com.slowlock.feature.apps.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowlock.R
import com.slowlock.feature.apps.domain.InstalledAppsRepository
import com.slowlock.feature.apps.domain.iconCacheKey
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.core.domain.AppTargetRepository
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
 * Owns the app list so it survives rotation without a reload flash.
 *
 * Every collaborator arrives through the constructor (FR-024, V1, V2). [AppTargetRepository] is the
 * seam a test substitutes to drive the null-resolution path the constitution requires.
 */
@HiltViewModel
class AppListViewModel @Inject constructor(
    private val apps: InstalledAppsRepository,
    private val targets: AppTargetRepository,
    /**
     * Exposed so each row loads its own icon lazily as it scrolls into view, and so [refresh] does
     * not wait for a rasterization it does not need. Icons never enter [uiState] — a bitmap in a
     * `StateFlow` is retained for as long as the state is.
     */
    val icons: AppIconRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppListUiState(query = savedState[QUERY_KEY] ?: ""))
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    private val _messages = Channel<Int>(Channel.BUFFERED)

    /**
     * One-shot messages for the screen to show and forget — currently only "that app is gone".
     *
     * A channel, not a field on [uiState] (FR-038, Principle IV): consume-once by construction, so
     * there is no flag left for a recomposition to re-read and no `onMessageShown()` for a caller
     * to forget.
     *
     * Each element is a string resource id, not a resolved string (V3) — resolving it here would
     * need a `Context` in the state holder. [AppListScreen] resolves it instead.
     *
     * `BUFFERED` rather than `RENDEZVOUS`: a tap landing while the screen is between compositions
     * must still be delivered when collection resumes.
     */
    val messages: Flow<Int> = _messages.receiveAsFlow()

    /**
     * Re-reads the installed apps, on every `ON_START`, so the list reflects installs and
     * uninstalls that happened while the screen was away (FR-013).
     *
     * A refresh over an already-populated list leaves `isLoading` false: the previous rows stay on
     * screen instead of flashing a spinner (FR-017).
     */
    fun refresh() {
        viewModelScope.launch {
            val loaded = apps.load()
            _uiState.update { it.copy(isLoading = false, apps = loaded) }
            icons.sweep(loaded.map { iconCacheKey(it.packageName, it.versionCode) })
        }
    }

    /**
     * Records the query; the filtering itself is derived in [AppListUiState.visibleApps]. Mirrored
     * into [SavedStateHandle] so it survives process death within one visit; the handle belongs to
     * this screen's back stack entry, so leaving the list for good takes the query with it
     * (FR-002(a), obligation G5).
     */
    fun onQueryChanged(query: String) {
        savedState[QUERY_KEY] = query
        _uiState.update { it.copy(query = query) }
    }

    /**
     * Resolves a tapped package and hands it forward (FR-009).
     *
     * [onResolved] is invoked only when the package still resolves (obligation P2). Nothing is
     * launched here: this decides *whether* the selection is valid, the caller decides what it
     * means.
     *
     * A package that no longer resolves was uninstalled since the last enumeration — not an error
     * state: the message is raised, the dead row dropped so it cannot be tapped again, and the user
     * stays on the list (FR-014).
     */
    fun onAppTapped(packageName: String, onResolved: (String) -> Unit) {
        viewModelScope.launch {
            if (targets.resolve(packageName) != null) {
                onResolved(packageName)
                return@launch
            }
            // The list mutation is state and stays in state; only the message is an event.
            _uiState.update { state ->
                state.copy(apps = state.apps.filterNot { it.packageName == packageName })
            }
            _messages.send(R.string.app_list_unavailable)
        }
    }

    private companion object {
        const val QUERY_KEY = "query"
    }
}
