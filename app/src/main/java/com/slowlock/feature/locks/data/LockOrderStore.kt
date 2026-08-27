package com.slowlock.feature.locks.data

import android.content.Context
import android.content.SharedPreferences
import com.slowlock.core.domain.IoDispatcher
import com.slowlock.feature.locks.domain.LOCKS_FILE
import com.slowlock.feature.locks.domain.LOCKS_KEY
import com.slowlock.feature.locks.domain.LockOrderRepository
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
 * the user's, on their home screen (FR-021). What the order *should* be is decided by
 * [com.slowlock.feature.locks.domain.LoadLocksUseCase]; this reads and writes it.
 *
 * Every function suspends onto the injected [IoDispatcher] (FR-040, D2), so callers stay main-safe
 * without having to know it (O2).
 *
 * A lock record holds the package name and nothing else (L2, Constitution V): the delay and the
 * treatment stay in `DelayConfigStore`, so there is one copy of each value on disk (FR-005).
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
     * Read on every visit: it supplies the order the launcher cannot, and it is the whole answer on
     * the visit where the launcher could not be asked at all, where the last known good list beats
     * both an empty screen and a crash.
     */
    override suspend fun loadOrder(): List<String> = withContext(io) {
        locksFrom(prefs.stringOrNull(LOCKS_KEY))
    }

    /**
     * Replaces the stored order with [order].
     *
     * `DelayConfigStore` is untouched: an app whose lock comes and goes with its icon should find
     * what the user last chose rather than a default (FR-005).
     */
    override suspend fun saveOrder(order: List<String>) = withContext(io) {
        prefs.edit().putString(LOCKS_KEY, encodeLocks(order)).apply()
    }
}

/**
 * `null` when the key is absent or holds the wrong type. The `runCatching` is not defensive habit:
 * a key written as a `StringSet` by some other build throws `ClassCastException` from
 * [SharedPreferences.getString], and the honest answer then is none, not a crash on launch (L4).
 */
private fun SharedPreferences.stringOrNull(key: String): String? =
    runCatching { getString(key, null) }.getOrNull()
