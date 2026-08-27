package com.slowlock.feature.apps.ui

import com.slowlock.feature.apps.domain.InstalledApp

/**
 * What the holder stores: the two values everything else is derived from, plus whether the first
 * read has returned.
 *
 * Separate from [AppListUiState] so the narrowed list cannot be stored beside the values it is
 * narrowed from and drift out of step with them (Constitution V). Nothing outside
 * [AppListViewModel] sees this type.
 */
data class AppListInputs(
    val isLoading: Boolean = true,
    /** Full, sorted, deduplicated, self excluded. */
    val apps: List<InstalledApp> = emptyList(),
    val query: String = "",
)

/**
 * What the screen is currently showing.
 *
 * Every field is derived from [AppListInputs] each time this is built, which is why there is no
 * default for [visibleApps]: constructing one without its filter applied would be a state that
 * contradicts itself.
 *
 * The "this app is gone" message is not here — it travels on [AppListViewModel.messages], because a
 * one-shot event held in an observable field is a sentinel someone has to remember to clear
 * (FR-038).
 */
data class AppListUiState(
    val isLoading: Boolean,
    /** Full, sorted, deduplicated, self excluded. */
    val apps: List<InstalledApp>,
    val query: String,
    /** [apps] narrowed by [query]. Which entries survive is `FilterAppsUseCase`'s (FR-007). */
    val visibleApps: List<InstalledApp>,
) {

    val isEmpty: Boolean
        get() = !isLoading && apps.isEmpty()

    val hasNoResults: Boolean
        get() = !isLoading && apps.isNotEmpty() && visibleApps.isEmpty()

    val isPopulated: Boolean
        get() = !isLoading && visibleApps.isNotEmpty()
}
