package com.slowlock.feature.locks.domain

import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.DelayConfig
import com.slowlock.core.domain.IconTreatment
import com.slowlock.core.domain.resolveTarget

/**
 * One row of the Locks screen, assembled at read time from three sources.
 *
 * Not persisted, and deliberately not persistable. The delay and the treatment are read rather than
 * copied into the lock record (FR-005), so there is exactly one copy of each on disk and the Locks
 * screen and the delay screen cannot disagree about what a lock waits.
 *
 * There is no icon field: icons travel through `AppIconCache` keyed by package and version, never
 * inside state, because a bitmap in a `StateFlow` is retained for as long as the state is.
 */
data class Lock(
    /** Identity, straight from [LockOrderStore]. The only value that was ever written to disk. */
    val packageName: String,
    /**
     * The app's *current* label, resolved fresh on every read (FR-012, SC-006) — never stored,
     * never matched on (Constitution V).
     *
     * `null` means the package did not resolve: uninstalled, disabled, or on an unavailable
     * profile. That is an ordinary row state (FR-020, K3), not a reason to drop the lock — the
     * user's home screen may still carry its icon.
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
 * Whether this lock's app is still on the device. The label is the marker because it is what the
 * row cannot be drawn without — with none, only the package name is left, which is what K3 shows.
 */
val Lock.isAvailable: Boolean get() = label != null

/**
 * Turns the recorded package names into rows, in one pass (FR-040).
 *
 * A free suspend function taking its two lookups as lambdas, which is what lets
 * `LocksViewModelTest` drive the null-resolution path without a device or an instrumented suite.
 *
 * It names no dispatcher: both lookups are repository calls and already main-safe (O2), so a
 * `withContext` here would be a second opinion about where work belongs (D1, D2).
 *
 * Order is the lock list's order (FR-006). Nothing sorts, filters or drops — an unresolvable
 * package becomes a row with a null label and stays where it was.
 *
 * No icon is loaded here (FR-015): the rows must be on screen before the icons are.
 */
internal suspend fun assembleLocks(
    packages: List<String>,
    loadConfig: suspend (String) -> DelayConfig,
    resolveTarget: suspend (String) -> AppTarget?,
): List<Lock> =
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

/** A package that no longer resolves still gets a usable, stable icon-cache key. */
private const val UNKNOWN_VERSION = 0L
