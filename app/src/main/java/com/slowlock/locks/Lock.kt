package com.slowlock.locks

import com.slowlock.delay.DelayConfig
import com.slowlock.shortcut.IconTreatment
import com.slowlock.shortcut.ShortcutTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One row of the Locks screen, assembled at read time from three sources (data-model.md §3).
 *
 * **Not persisted, and deliberately not persistable.** Its identity comes from [LockStore], its
 * values from `DelayConfigStore`, and its display text from `resolveShortcutTarget`. FR-005 is why
 * the delay and the treatment are read rather than copied into the lock record: there is exactly
 * one copy of each on disk, so the Locks screen and the delay screen cannot disagree about what a
 * lock waits.
 *
 * **There is no icon field** — the same rule [com.slowlock.apps.InstalledApp] and [ShortcutTarget]
 * already follow. Icons travel through `AppIconCache`, keyed by package and version, and never
 * inside state; a bitmap in a `StateFlow` is a bitmap retained for as long as the state is.
 */
data class Lock(
    /** Identity, straight from [LockStore]. The only value that was ever written to disk. */
    val packageName: String,
    /**
     * The app's **current** label, resolved fresh on every read (FR-012, SC-006) — never stored,
     * never matched on (Constitution V).
     *
     * `null` means the package did not resolve: uninstalled, disabled, or on a profile that is no
     * longer available. That is an ordinary row state (FR-020, contract K3), not an error and not
     * a reason to drop the lock — the user's home screen may still carry its icon.
     */
    val label: String?,
    /** Icon-cache staleness marker (Constitution V). Not identity. */
    val versionCode: Long,
    /** From `DelayConfigStore`; [DelayConfig.DEFAULT_SECONDS] when nothing was ever saved. */
    val delaySeconds: Int,
    /** From `DelayConfigStore`; the first entry when nothing was ever saved. */
    val treatment: IconTreatment,
)

/**
 * Whether this lock's app is still on the device.
 *
 * The label is the marker because it is the thing the row cannot be drawn without: a row with no
 * label has nothing to identify it by except its package name, which is exactly what contract K3
 * tells it to show.
 */
val Lock.isAvailable: Boolean get() = label != null

/**
 * Turns the recorded package names into rows, in **one pass off the main thread** (FR-040, R5).
 *
 * A free suspend function taking its two platform lookups as lambdas, rather than a method
 * reaching for a `PackageManager` inline — the same seam `resolveTarget` and `AppListViewModel`
 * already use, and for the same reason: it is what lets `LocksViewModelTest` drive the
 * null-resolution path the constitution requires without a device, an emulator, or an instrumented
 * suite.
 *
 * **Order is the lock list's order** (FR-006): decided by [deriveLocks] and by nothing else here.
 * Nothing sorts, filters, or drops — an unresolvable package becomes a row with a null label and
 * stays exactly where it was.
 *
 * **No icon is loaded here** (FR-015). The rows must be on screen before the icons are; each row
 * asks `AppIconCache` for its own as it comes into view.
 */
internal suspend fun assembleLocks(
    packages: List<String>,
    loadConfig: suspend (String) -> DelayConfig,
    resolveTarget: suspend (String) -> ShortcutTarget?,
): List<Lock> = withContext(Dispatchers.IO) {
    packages.map { packageName ->
        // The configuration is read for every lock, resolvable or not: an uninstalled app's row
        // still shows what it was waiting, and a reinstall must not silently lose the value.
        val config = loadConfig(packageName)
        val target = resolveTarget(packageName)
        Lock(
            packageName = packageName,
            label = target?.label,
            versionCode = target?.versionCode ?: UNKNOWN_VERSION,
            delaySeconds = config.delaySeconds,
            treatment = config.treatment,
        )
    }
}

/** A package that no longer resolves still gets a usable, stable icon-cache key. */
private const val UNKNOWN_VERSION = 0L
