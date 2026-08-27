package com.slowlock.feature.apps.domain

import javax.inject.Inject

/**
 * Narrows the app list to what the user typed.
 *
 * Substring rather than prefix, ignoring case — "tagram" finds Instagram (FR-007). The input's
 * order is kept and never re-sorted, so clearing the query restores the collated list for free
 * (FR-008).
 *
 * **The one use case here that does not suspend**, because it reaches no source: it is a pure
 * narrowing of a list the caller already holds. At roughly 150 entries in memory it is
 * sub-millisecond, which is why there is no debounce behind the search field.
 */
class FilterAppsUseCase @Inject constructor() {

    operator fun invoke(apps: List<InstalledApp>, query: String): List<InstalledApp> =
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
}
