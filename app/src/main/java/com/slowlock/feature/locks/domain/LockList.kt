package com.slowlock.feature.locks.domain

/**
 * **FROZEN** (`contracts/lock-store.md`, L1). A renamed file is an empty file: every lock
 * disappears from the Locks screen at once, silently, while the home-screen icons keep waiting and
 * keep working — so nothing looks broken.
 */
internal const val LOCKS_FILE = "slowlock.locks"

/** **FROZEN** (L1). Renaming this empties the list exactly as renaming [LOCKS_FILE] does. */
internal const val LOCKS_KEY = "packages"

/**
 * **FROZEN** (L1). The one value on disk is package names joined by this. Changing it turns every
 * existing multi-lock record into one unresolvable entry, which then reads as a single broken row.
 */
internal const val LOCKS_SEPARATOR = "\n"

/**
 * The stored value as an ordered list of package names — never null, never throwing (FR-007, L4).
 *
 * Absent, empty, whitespace-only and malformed all read as no locks. Duplicates collapse keeping
 * their *first* position (FR-013): later is not newer, and a row that moved would look to the user
 * like the lock had been re-made.
 *
 * A package that no longer resolves is not a read failure — it stays in the list and becomes a row
 * (FR-020).
 */
internal fun locksFrom(stored: String?): List<String> =
    stored
        ?.split(LOCKS_SEPARATOR)
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.distinct()
        ?: emptyList()

/** The list as it goes to disk. The inverse of [locksFrom] for any list [locksFrom] produced. */
internal fun encodeLocks(packages: List<String>): String =
    packages.joinToString(LOCKS_SEPARATOR)

/**
 * The lock list, derived from the shortcuts the launcher actually holds.
 *
 * A lock exists exactly when its shortcut is pinned (FR-003a). That one rule is what makes the list
 * agree with the home screen without the app being told anything: Add pins, so the lock appears;
 * Cancel pins nothing, so no lock is created; dragging the icon off unpins, so the lock goes; and a
 * launcher that pins without reporting back still shows up.
 *
 * That last case is why this derives from the pinned set rather than the pin request's
 * `IntentSender`: that callback fires on success and stays *silent on cancel*, so it can say a pin
 * worked but never that one did not.
 *
 * There is deliberately no in-app removal to subtract (FR-021), so the rule has one term rather
 * than two and there is no tombstone to keep, expire, or clear when a lock is re-made.
 *
 * [cached] supplies order only (FR-006): `getPinnedShortcuts()` answers in no particular order, so
 * previously-seen packages keep their position and new ones are appended deterministically.
 */
internal fun deriveLocks(cached: List<String>, pinned: Set<String>): List<String> {
    val known = cached.toSet()
    val kept = cached.filter { it in pinned }
    // Sorted, not in the set's iteration order: `getPinnedShortcuts()` makes no ordering promise,
    // and a list that reshuffled between launches looks like the app losing track (FR-006).
    val appended = pinned.filter { it !in known }.sorted()
    return kept + appended
}
