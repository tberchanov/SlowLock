package com.slowlock.feature.apps.domain

/**
 * The launchable apps on the current user profile
 * (contracts/repository-interfaces.md, `apps/domain`).
 *
 * Obligations:
 *
 * - **Returns the raw enumeration**: one entry per launcher activity, this app included, in
 *   whatever order the platform answered. Excluding, deduplicating and collating belong to
 *   [LoadInstalledAppsUseCase], because they are rules a requirement states rather than how the
 *   platform is read.
 * - Uses `LauncherApps`: no permission, no dialog, and never `QUERY_ALL_PACKAGES`
 *   (constitution, Permission and policy minimalism).
 * - Main-safe (O2); enumeration runs off the main thread.
 */
interface InstalledAppsRepository {

    /** Every launchable activity on the current profile, unfiltered and unordered. */
    suspend fun load(): List<InstalledApp>
}
