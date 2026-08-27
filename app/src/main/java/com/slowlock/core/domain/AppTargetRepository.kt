package com.slowlock.core.domain

/**
 * Re-resolves a package against the platform (contracts/repository-interfaces.md).
 *
 * **`null` is an ordinary outcome, not an error** — the app is gone, disabled, or on a profile
 * that is no longer available. It is the constitution's mandated null-`getLaunchIntentForPackage()`
 * path, and it must stay unit-testable without a device: a package with no launch intent resolves
 * to `null`, and every caller has to have a branch for it.
 *
 * Display facts are re-resolved rather than carried across a seam, so a label always reflects what
 * the app is called *now* rather than what it was called when a lock was made.
 */
interface AppTargetRepository {

    /** The resolved facts for [packageName], or `null` if it cannot be launched. */
    suspend fun resolve(packageName: String): AppTarget?
}
