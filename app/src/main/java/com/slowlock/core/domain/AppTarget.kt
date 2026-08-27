package com.slowlock.core.domain


/**
 * The resolved facts about the app being configured — the boundary past which everything is plain
 * data, assertable without a device. There is deliberately no icon field: icons are loaded through
 * `AppIconCache` and never travel inside state.
 */
data class AppTarget(
    /** The identity, and the only value that arrived across the selection seam. */
    val packageName: String,
    /**
     * Localized, display only. Becomes the shortcut's label verbatim, with no suffix. Never a key,
     * never matched on, never compared (Constitution V).
     */
    val label: String,
    /** Icon-cache staleness marker (Constitution V). Not identity. */
    val versionCode: Long,
)

/**
 * Re-resolves [packageName] against the platform, or returns `null` if the app is gone. Display
 * data is re-resolved here rather than carried across the seam (obligation C3); the one `String`
 * that crossed is all this needs.
 *
 * `null` is the FR-015 path — uninstalled while the configuration screen was open — and an ordinary
 * outcome, not an error: the caller tells the user and stays put.
 *
 * The lookups are injected lambdas, not a `PackageManager` reached for inline. That is the seam
 * that lets `AppTargetTest` exercise the null `getLaunchIntentForPackage()` path with no device. Do
 * not inline them. [isLaunchable] answers a question rather than handing back an `Intent`, which
 * keeps this file free of `android.*` (FR-025, O1).
 *
 * This function names no dispatcher and does no thread hop: `AppTargetSource` supplies the lambdas
 * and owns the move to its injected dispatcher (D1, D2).
 */
suspend fun resolveTarget(
    packageName: String,
    isLaunchable: (String) -> Boolean,
    loadLabel: (String) -> String?,
    loadVersionCode: (String) -> Long,
): AppTarget? {
    // A package with no launch intent cannot be started, so a shortcut to it would be a dead icon.
    if (!isLaunchable(packageName)) return null

    val label = loadLabel(packageName) ?: return null

    return AppTarget(
        packageName = packageName,
        label = label,
        versionCode = loadVersionCode(packageName),
    )
}
