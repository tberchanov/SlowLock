package com.slowlock.locks

import android.content.Context
import android.content.pm.ShortcutManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Which of this app's shortcuts a launcher currently holds pinned.
 *
 * The IDs come back as **package names**, because `ShortcutContract.shortcutId(pkg)` *is* the
 * package name — the same key [LockStore] orders its rows by. That is what makes [deriveLocks] a set
 * operation rather than a mapping exercise, and it is a direct dividend
 * of feature 002 deriving shortcut identity from the target instead of generating one.
 *
 * **`null` means "no answer", and no answer must never prune anything.** It is returned when there
 * is no `ShortcutManager` at all, and when the call throws — which it does on a device that is
 * still locked after a reboot (`IllegalStateException`, direct-boot), a state this app can
 * genuinely be started in. An empty set and a null are opposite claims: the first says "the
 * launcher holds none of them", the second says "we could not ask", and treating the second as the
 * first would empty the user's entire list on a bad read.
 *
 * A caveat worth knowing rather than discovering: the framework tracks pinning **per launcher**,
 * and this returns shortcuts pinned by *any* of them. Switching launcher therefore does not unpin
 * anything — the previous launcher keeps its pins as long as it is installed — so a launcher change
 * does not empty this list. Conversely a launcher that never unpins on icon removal keeps reporting
 * a shortcut that is no longer on any home screen, and its lock stays until the user removes it
 * by hand.
 */
suspend fun pinnedShortcutIds(context: Context): Set<String>? = withContext(Dispatchers.IO) {
    val manager = context.getSystemService(ShortcutManager::class.java) ?: run {
        Log.w(TAG, "No ShortcutManager; cannot reconcile locks against the launcher")
        return@withContext null
    }
    runCatching { manager.pinnedShortcuts.mapTo(mutableSetOf()) { it.id } }
        .onFailure { Log.w(TAG, "Could not read pinned shortcuts; leaving locks untouched", it) }
        .getOrNull()
}

private const val TAG = "SlowLock"
