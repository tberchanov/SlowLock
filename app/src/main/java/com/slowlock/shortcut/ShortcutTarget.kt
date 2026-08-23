package com.slowlock.shortcut

import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The resolved facts about the app being configured.
 *
 * This is the boundary: past this point everything is plain data, resolvable and assertable
 * without a device. There is deliberately **no icon field** — icons are loaded separately
 * through feature 001's `AppIconCache` and never travel inside state, the same rule
 * `InstalledApp` follows for the same reason.
 */
data class ShortcutTarget(
    /** The identity, and the only value that arrived across the seam from feature 001. */
    val packageName: String,
    /**
     * Localized, display only. Becomes the shortcut's label verbatim, with no suffix. Never a
     * key, never matched on, never compared (Constitution V).
     */
    val label: String,
    /** Icon-cache staleness marker (Constitution V). Not identity. */
    val versionCode: Long,
)

/**
 * Re-resolves [packageName] against the platform, or returns `null` if the app is gone.
 *
 * Display data is re-resolved here rather than carried across the seam — obligation C3 of
 * feature 001's `contracts/selection-handoff.md`. The one `String` that crossed is all this
 * needs.
 *
 * `null` is the FR-015 path: the app was uninstalled while the configuration screen was open,
 * or between opening it and pressing "Create shortcut". It is an ordinary outcome, not an
 * error — the caller tells the user and stays put.
 *
 * The lookups are **injected lambdas, not a `PackageManager` reached for inline**, exactly as
 * `AppListViewModel` does. That is the seam that lets `ShortcutTargetTest` exercise the null
 * `getLaunchIntentForPackage()` path — the one the constitution requires a unit test for — with
 * no device and no instrumented suite. Do not inline them.
 *
 * Runs on [Dispatchers.IO]: every one of these lookups touches the package manager (FR-024).
 */
suspend fun resolveTarget(
    packageName: String,
    resolveLaunchIntent: (String) -> Intent?,
    loadLabel: (String) -> String?,
    loadVersionCode: (String) -> Long,
): ShortcutTarget? = withContext(Dispatchers.IO) {
    // A package with no launch intent cannot be started, so a shortcut to it would be a dead
    // icon. This is the same check feature 001 makes before handing the selection across.
    if (resolveLaunchIntent(packageName) == null) return@withContext null

    val label = loadLabel(packageName) ?: return@withContext null

    ShortcutTarget(
        packageName = packageName,
        label = label,
        versionCode = loadVersionCode(packageName),
    )
}
