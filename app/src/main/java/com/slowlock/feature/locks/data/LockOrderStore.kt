package com.slowlock.feature.locks.data

import android.content.Context
import android.content.SharedPreferences
import com.slowlock.core.domain.IoDispatcher
import com.slowlock.feature.locks.domain.LOCKS_FILE
import com.slowlock.feature.locks.domain.LOCKS_KEY
import com.slowlock.feature.locks.domain.LockOrderRepository
import com.slowlock.feature.locks.domain.deriveLocks
import com.slowlock.feature.locks.domain.encodeLocks
import com.slowlock.feature.locks.domain.locksFrom
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The only route to the two values behind the lock list — no other class opens [LOCKS_FILE] (L3).
 *
 * The stored value is not the record of what a lock is. A lock exists exactly when its shortcut is
 * pinned (FR-003a); [LOCKS_KEY] holds only what the launcher cannot answer — the *order* the rows
 * were last seen in — and doubles as the fallback for when the launcher cannot be asked at all.
 *
 * There is no `add` and no `remove`: creating a lock is the pin request's job and removing one is
 * the user's, on their home screen (FR-021).
 *
 * Every function suspends onto the injected [IoDispatcher] (FR-040, D2), so callers stay main-safe
 * without having to know it (O2).
 *
 * A lock record holds the package name and nothing else (L2, Constitution V): the delay and the
 * treatment stay in `DelayConfigStore`, so there is one copy of each value on disk (FR-005).
 *
 * This file is left with only the wiring — everything decidable lives in `LockList.kt` as pure
 * functions the JVM suite can reach without a framework.
 */
@Singleton
class LockOrderStore @Inject constructor(
    @ApplicationContext context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : LockOrderRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(LOCKS_FILE, Context.MODE_PRIVATE)

    /**
     * The last derived list, in order — never null, never throwing (FR-007, L4, L5).
     *
     * The fallback, not the source of truth: [deriveOrder] is the ordinary path, and this answers
     * only when the launcher could not be asked, where the last known good list beats both an empty
     * screen and a crash.
     */
    override suspend fun loadOrder(): List<String> = withContext(io) {
        locksFrom(prefs.stringOrNull(LOCKS_KEY))
    }

    /**
     * The lock list for [pinned], and the one call that keeps the stored order in step with it. The
     * decision is [deriveLocks]'s; this writes back only when the order actually changed, so the
     * ordinary visit to the Locks screen costs a read and no write.
     *
     * **Never call this with an empty set standing in for "could not ask"** — an empty set claims
     * the launcher holds no shortcuts at all, and acting on it would empty the screen.
     * [com.slowlock.feature.locks.domain.PinnedShortcutsRepository.pinnedIds] answers `null` for that.
     *
     * `DelayConfigStore` is untouched here: an app whose lock comes and goes with its icon should
     * find what the user last chose rather than a default (FR-005).
     */
    override suspend fun deriveOrder(pinned: Set<String>): List<String> = withContext(io) {
        val cached = locksFrom(prefs.stringOrNull(LOCKS_KEY))
        val locks = deriveLocks(cached, pinned)
        if (locks != cached) {
            prefs.edit().putString(LOCKS_KEY, encodeLocks(locks)).apply()
        }
        locks
    }
}

/**
 * `null` when the key is absent or holds the wrong type. The `runCatching` is not defensive habit:
 * a key written as a `StringSet` by some other build throws `ClassCastException` from
 * [SharedPreferences.getString], and the honest answer then is none, not a crash on launch (L4).
 */
private fun SharedPreferences.stringOrNull(key: String): String? =
    runCatching { getString(key, null) }.getOrNull()
