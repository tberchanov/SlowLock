package com.slowlock.apps

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.slowlock.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the app list so it survives rotation without a reload flash.
 *
 * [resolveLaunchIntent] and [unavailableMessage] are seams, not conveniences: together they
 * let the null `getLaunchIntentForPackage()` path be unit-tested without a device. Do not
 * inline the `PackageManager` call, and do not resolve the string at the point of use.
 *
 * `@JvmOverloads` is what keeps the default `SavedStateViewModelFactory` able to find the
 * `(Application, SavedStateHandle)` constructor it looks for by reflection.
 */
class AppListViewModel @JvmOverloads constructor(
    app: Application,
    private val savedState: SavedStateHandle,
    private val unavailableMessage: String = app.getString(R.string.app_list_unavailable),
    private val resolveLaunchIntent: (String) -> Intent? = {
        app.packageManager.getLaunchIntentForPackage(it)
    },
) : AndroidViewModel(app) {

    // Both reach for system services, so they are built on first use rather than in `init`:
    // that keeps the ViewModel constructible in a plain JVM test, where the only thing
    // exercised is the tap path.
    private val source by lazy { InstalledAppsSource(getApplication()) }

    /** Exposed so each row can load its own icon lazily as it scrolls into view. */
    val iconCache by lazy { AppIconCache(getApplication()) }

    private val _uiState = MutableStateFlow(AppListUiState(query = savedState[QUERY_KEY] ?: ""))
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    /**
     * Re-reads the installed apps. Called on every `ON_START`, so the list reflects installs
     * and uninstalls that happened while the screen was away (FR-013).
     *
     * A refresh over an already-populated list leaves `isLoading` false: the previous rows
     * stay on screen instead of flashing a spinner (FR-017).
     */
    fun refresh() {
        viewModelScope.launch {
            val apps = source.load()
            _uiState.update { it.copy(isLoading = false, apps = apps) }
            iconCache.sweep(apps)
        }
    }

    /**
     * Narrows the list as the user types. The filtering itself is derived in
     * [AppListUiState.visibleApps]; this only records the query.
     *
     * Mirrored into [SavedStateHandle] so the query survives process death, not just
     * rotation — the ViewModel itself does not outlive the former (FR-017).
     */
    fun onQueryChanged(query: String) {
        savedState[QUERY_KEY] = query
        _uiState.update { it.copy(query = query) }
    }

    /**
     * Resolves a tapped package and hands it forward (FR-009).
     *
     * [onResolved] is invoked only when the package still resolves to a launch intent —
     * obligation P2 of `contracts/selection-handoff.md`. Nothing is launched here: the
     * ViewModel decides *whether* the selection is valid, the caller decides what it means.
     *
     * A package that no longer resolves was uninstalled since the last enumeration. That is
     * not an error state for the screen: the message is raised, the dead row is dropped so it
     * cannot be tapped again, and the user stays on the list (FR-014).
     */
    fun onAppTapped(packageName: String, onResolved: (String) -> Unit) {
        if (resolveLaunchIntent(packageName) != null) {
            onResolved(packageName)
            return
        }
        _uiState.update { state ->
            state.copy(
                apps = state.apps.filterNot { it.packageName == packageName },
                unavailableAppMessage = unavailableMessage,
            )
        }
    }

    /**
     * Clears the message once it has been displayed, so it is shown exactly once rather than
     * again on the next recomposition (FR-014).
     */
    fun onUnavailableMessageShown() {
        _uiState.update { it.copy(unavailableAppMessage = null) }
    }

    private companion object {
        const val QUERY_KEY = "query"
    }
}
