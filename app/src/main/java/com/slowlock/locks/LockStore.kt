package com.slowlock.locks

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The only route to the two values behind the lock list (`contracts/lock-store.md`, L3 — **no
 * other class opens [LOCKS_FILE]**, the same obligation [com.slowlock.delay.DelayConfigStore]
 * carries for `slowlock.delay-config`).
 *
 * **The stored value is not the record of what a lock is.** A lock exists exactly when its
 * shortcut is pinned (FR-003a); [derive] is where that is worked out. [LOCKS_KEY] holds only what
 * the launcher cannot answer — the **order** the rows were last seen in — and doubles as the
 * fallback for when the launcher cannot be asked at all.
 *
 * There is no `add` and no `remove`. Creating a lock is the pin request's job and removing one is
 * the user's, on their home screen (FR-021), so this class writes exactly one value and only ever
 * as a consequence of what the launcher reported.
 *
 * **Every function suspends and does its work on [Dispatchers.IO]** (FR-040, Constitution IV).
 * [load] runs on the path a user is waiting to see their locks on, so neither a read nor a write
 * may touch the main thread.
 *
 * A lock record holds the package name and nothing else (L2, Constitution V). The delay and the
 * treatment stay in `DelayConfigStore` and are read from there, so there is exactly one copy of
 * each value on disk and the two screens cannot disagree (FR-005).
 *
 * This is the one file in the `locks` package with `android.*` imports, and it is deliberately
 * left with only the wiring: everything decidable — what a missing or malformed value reads as,
 * where a new lock goes, what a removal takes — lives in [LockList.kt][locksFrom] as pure
 * functions, because the JVM suite runs with `isReturnDefaultValues = true` and a test written
 * against `SharedPreferences` here would assert nothing while appearing to pass.
 */
class LockStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(LOCKS_FILE, Context.MODE_PRIVATE)

    /**
     * The last derived list, in order — **never null, never throwing** (FR-007, L4, L5).
     *
     * This is the fallback, not the source of truth. [derive] is the ordinary path; this answers
     * only when the launcher could not be asked, where the last known good list beats both an
     * empty screen and a crash.
     */
    suspend fun load(): List<String> = withContext(Dispatchers.IO) {
        locksFrom(prefs.stringOrNull(LOCKS_KEY))
    }

    /**
     * The lock list for [pinned], and the one call that keeps the stored order in step with it.
     *
     * The decision is [deriveLocks]'s — pure, and unit-tested. This is the wiring: read, derive,
     * and write back only when the order actually changed, so the ordinary visit to the Locks
     * screen costs a read and no write.
     *
     * **Never call this with an empty set standing in for "could not ask"** — an empty set is the
     * claim that the launcher holds no shortcuts at all, and acting on it would empty the screen.
     * [pinnedShortcutIds] answers `null` for that, and the caller falls back to [load].
     *
     * `DelayConfigStore` is untouched, here and everywhere in this class: the delay and the
     * treatment are the single copy of those values (FR-005), and an app whose lock comes and goes
     * with its icon should find what the user last chose rather than a default.
     */
    suspend fun derive(pinned: Set<String>): List<String> = withContext(Dispatchers.IO) {
        val cached = locksFrom(prefs.stringOrNull(LOCKS_KEY))
        val locks = deriveLocks(cached, pinned)
        if (locks != cached) {
            prefs.edit().putString(LOCKS_KEY, encodeLocks(locks)).apply()
        }
        locks
    }
}

/**
 * `null` when the key is absent **or holds the wrong type**, mirroring `DelayConfigStore`'s own
 * guard.
 *
 * The `runCatching` is not defensive habit: a key written as a `StringSet` by some earlier or
 * later build throws `ClassCastException` from [SharedPreferences.getString], and the honest
 * answer to "which locks does the user have?" in that case is none, not a crash on launch (L4).
 */
private fun SharedPreferences.stringOrNull(key: String): String? =
    runCatching { getString(key, null) }.getOrNull()
