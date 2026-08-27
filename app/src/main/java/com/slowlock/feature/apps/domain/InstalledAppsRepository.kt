package com.slowlock.feature.apps.domain

/**
 * The launchable apps on the current user profile
 * (contracts/repository-interfaces.md, `apps/domain`).
 *
 * Obligations:
 *
 * - Excludes SlowLock itself, deduplicates by package, and sorts by label under the **current**
 *   locale read at load time — not a cached one, so a language change re-collates the list on the
 *   next load.
 * - Uses `LauncherApps`: no permission, no dialog, and never `QUERY_ALL_PACKAGES`
 *   (constitution, Permission and policy minimalism).
 * - Main-safe (O2); enumeration runs off the main thread.
 */
interface InstalledAppsRepository {

    /** Every launchable app, already excluded, deduplicated and collated. */
    suspend fun load(): List<InstalledApp>
}
