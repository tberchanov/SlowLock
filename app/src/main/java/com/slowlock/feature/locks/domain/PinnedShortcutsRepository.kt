package com.slowlock.feature.locks.domain

/**
 * Which of this app's shortcuts a launcher currently holds pinned
 * (contracts/repository-interfaces.md, `locks/domain`).
 *
 * **`null` and the empty set are opposite claims, and conflating them empties the user's whole
 * lock list.** The empty set says "the launcher holds none of them". `null` says "we could not
 * ask" — returned when there is no `ShortcutManager`, and when the call throws, including the
 * direct-boot `IllegalStateException` a locked device after a reboot produces, which is a state
 * this app can genuinely be started in. A caller that reads `null` as empty prunes every lock the
 * user has.
 *
 * IDs come back as **package names**, because the shortcut ID *is* the package name (F4). That is
 * what makes deriving the lock list a set operation rather than a mapping exercise.
 */
interface PinnedShortcutsRepository {

    /** The pinned shortcut IDs, or `null` when the launcher could not be asked. */
    suspend fun pinnedIds(): Set<String>?
}
