package com.slowlock.feature.apps.domain

import java.text.Collator
import java.util.Locale

/**
 * A launchable app on the current user profile, already deduplicated and ready to display.
 *
 * The icon is deliberately absent: icons are loaded lazily per visible row and cached separately,
 * so they never travel inside the list state. So are `ComponentName` and the launcher activity name
 * — [packageName] is the only identifier persisted or handed off.
 */
data class InstalledApp(
    /** Stable identity. Non-empty, unique across the list. */
    val packageName: String,
    /** Localized, display only — never a key, never used in matching. */
    val label: String,
    /** Icon-cache staleness marker. Not identity. */
    val versionCode: Long,
)

/** Drops SlowLock itself from the list (FR-003). */
fun List<InstalledApp>.excludeSelf(ownPackage: String): List<InstalledApp> =
    filterNot { it.packageName == ownPackage }

/**
 * Collapses the several launcher activities a package may expose down to one entry (FR-004).
 * Identity is the package, never the activity (Constitution V). The lowest label wins within a
 * package — a tiebreaker for determinism only, since `LauncherApps` promises no activity order.
 */
fun List<InstalledApp>.dedupeByPackage(): List<InstalledApp> =
    groupBy { it.packageName }
        .map { (_, entries) -> entries.minByOrNull { it.label } ?: entries.first() }

/**
 * Orders labels the way a reader of [locale] expects (FR-005). [Collator.SECONDARY] ignores case
 * but respects accents, so "eBay" lands among the E's and "Über" among the U's;
 * `String.lowercase()` would order by UTF-16 code unit and push both after Z.
 */
fun List<InstalledApp>.sortedByLabel(locale: Locale): List<InstalledApp> {
    val collator = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }
    return sortedWith(compareBy(collator) { it.label })
}

/**
 * The two-tier icon cache key (Constitution V, FR-012). Carrying the version code makes
 * invalidation implicit: an app update produces a new key, which misses both tiers, so a stale icon
 * is never read.
 */
fun iconCacheKey(packageName: String, versionCode: Long): String = "$packageName:$versionCode"
