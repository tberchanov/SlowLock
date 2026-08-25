package com.slowlock.locks

/**
 * **FROZEN** (`contracts/lock-store.md`, L1). A renamed file is an empty file: every lock a user
 * has made disappears from the Locks screen at once, silently, after an update they did not ask
 * for — while their home-screen icons keep waiting and keep working, so nothing looks broken.
 *
 * Separate from `slowlock.delay-config`, which feature 003 froze and which this feature never
 * writes a key into (L1, FR-008).
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
 * The stored value as an ordered list of package names — **never null, never throwing** (FR-007,
 * L4).
 *
 * Absent, empty, whitespace-only and malformed all read as no locks. Blank entries are dropped,
 * entries are trimmed, and duplicates collapse keeping their **first** position (FR-013): later is
 * not newer, and a row that moved would look to the user like the lock had been re-made.
 *
 * A package that no longer resolves is *not* a read failure. It stays in the list and becomes a
 * row; FR-020 says what that row looks like.
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
 * **A lock exists exactly when its shortcut is pinned** (FR-003a). That single rule replaces the
 * record-keeping this file used to do, and it is what makes the list agree with the home screen
 * without the app ever being told anything:
 *
 * | Event | Why the list is right |
 * |---|---|
 * | the user taps **Add** | the shortcut becomes pinned, so the lock appears |
 * | the user taps **Cancel** | nothing is pinned, so **no lock is ever created** |
 * | the user drags the icon off | the shortcut is unpinned, so the lock goes |
 * | the launcher pins without reporting back | the shortcut is pinned, so the lock appears anyway |
 *
 * The last row is why this derives from the pinned set rather than from the pin request's
 * `IntentSender`: that callback fires on success and stays **silent on cancel**, so it can tell
 * the app a pin worked but never that one did not. Deriving needs no callback and has no silence
 * to interpret.
 *
 * There is deliberately **no in-app removal to subtract** (FR-021). Removing a lock means removing
 * its icon, which only the user can do, so the rule has one term rather than two — and there is no
 * tombstone to keep, expire, or clear when a lock is re-made.
 *
 * [cached] supplies **order only** (FR-006). `getPinnedShortcuts()` answers in no particular order,
 * so previously-seen packages keep the position they had and genuinely new ones are appended in a
 * deterministic order rather than the framework's.
 *
 * Pure and total: nothing here throws, and nothing is invented — a package is in the result only
 * if the launcher named it.
 */
internal fun deriveLocks(cached: List<String>, pinned: Set<String>): List<String> {
    val known = cached.toSet()
    val kept = cached.filter { it in pinned }
    // Sorted, not in the set's iteration order: `getPinnedShortcuts()` makes no ordering promise,
    // and a list that reshuffled itself between launches would fail FR-006 in a way that looks
    // like the app losing track of the user's locks.
    val appended = pinned.filter { it !in known }.sorted()
    return kept + appended
}
