package com.slowlock.feature.locks.domain

/**
 * The only route to `slowlock.locks` (F3).
 *
 * The stored value is not the record of what a lock is: a lock exists exactly when its shortcut is
 * pinned, and this holds only what the launcher cannot answer — the *order* the rows were last seen
 * in — doubling as the fallback for when the launcher cannot be asked at all.
 *
 * There is no `add` and no `remove`: creating a lock is the pin request's job and removing one is
 * the user's, on their home screen.
 */
interface LockOrderRepository {

    /**
     * The last derived list, in order — never null, never throwing. The fallback, not the source of
     * truth: it answers only when the launcher could not be asked.
     */
    suspend fun loadOrder(): List<String>

    /**
     * The lock list for [pinned], keeping the stored order in step with it. Writes back only when
     * the order actually changed, so an ordinary visit costs a read and no write.
     *
     * **Never call this with an empty set standing in for "could not ask"** — an empty set claims
     * the launcher holds no shortcuts at all, and acting on it would empty the screen.
     * [PinnedShortcutsRepository.pinnedIds] answers `null` for that.
     */
    suspend fun deriveOrder(pinned: Set<String>): List<String>
}
