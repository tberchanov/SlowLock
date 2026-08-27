package com.slowlock.feature.apps.ui

import com.slowlock.feature.apps.domain.InstalledApp

/**
 * What the screen is currently showing, plus the query. [apps] and [query] are the single source of
 * truth; the visible list and the four display states are derived so they cannot contradict it.
 *
 * Every field here is state that persists. The "this app is gone" message is not — it travels on
 * [AppListViewModel.messages], because a one-shot event held in an observable field is a sentinel
 * someone has to remember to clear (FR-038).
 */
data class AppListUiState(
    val isLoading: Boolean = true,
    /** Full, sorted, deduplicated, self excluded. */
    val apps: List<InstalledApp> = emptyList(),
    val query: String = "",
) {
    /**
     * The rows to render: [apps] narrowed to labels containing [query], ignoring case (FR-007).
     * Substring, not prefix — "tagram" finds Instagram.
     *
     * The filter walks the already-collated list and never re-sorts, so clearing the query restores
     * the original order for free (FR-008). At ~150 entries in memory it is sub-millisecond, which
     * is why there is no debounce.
     */
    val visibleApps: List<InstalledApp>
        get() = if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }

    val isEmpty: Boolean
        get() = !isLoading && apps.isEmpty()

    val hasNoResults: Boolean
        get() = !isLoading && apps.isNotEmpty() && visibleApps.isEmpty()

    val isPopulated: Boolean
        get() = !isLoading && visibleApps.isNotEmpty()
}
