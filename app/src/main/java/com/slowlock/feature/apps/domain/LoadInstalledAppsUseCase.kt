package com.slowlock.feature.apps.domain

import com.slowlock.core.domain.CurrentLocale
import com.slowlock.core.domain.OwnPackageName
import javax.inject.Inject

/**
 * The app list as the picker shows it: this app absent, one row per package, collated.
 *
 * The three steps run in this order because each narrows what the next sees — exclusion before
 * dedup so a discarded entry cannot resurrect the app's own package, and sorting last so it collates
 * the final set rather than one that is about to shrink.
 *
 * The locale is read inside [invoke] and never held, which is the whole of FR-005: a language change
 * re-collates on the next load rather than on the next process.
 */
class LoadInstalledAppsUseCase @Inject constructor(
    private val apps: InstalledAppsRepository,
    @param:OwnPackageName private val ownPackage: String,
    private val locale: CurrentLocale,
) {

    /** Every launchable app but this one, deduplicated by package and collated for the reader. */
    suspend operator fun invoke(): List<InstalledApp> =
        apps.load()
            .excludeSelf(ownPackage)
            .dedupeByPackage()
            .sortedByLabel(locale.now())
}
