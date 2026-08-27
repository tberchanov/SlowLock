package com.slowlock.feature.locks.data

import android.content.Context
import android.content.pm.ShortcutManager
import android.util.Log
import com.slowlock.core.domain.IoDispatcher
import com.slowlock.feature.locks.domain.PinnedShortcutsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Which of this app's shortcuts a launcher currently holds pinned.
 *
 * The IDs come back as package names, because `ShortcutContract.shortcutId(pkg)` *is* the package
 * name — the same key `LockOrderStore` orders its rows by, which makes `deriveLocks` a set
 * operation rather than a mapping exercise.
 *
 * `null` means "no answer", and no answer must never prune anything. It is returned when there is
 * no `ShortcutManager` and when the call throws, which it does on a device still locked after a
 * reboot (direct-boot). An empty set is the opposite claim — "the launcher holds none of them" —
 * and treating a null as one would empty the user's entire list on a bad read.
 *
 * A caveat worth knowing: the framework tracks pinning per launcher, and this returns shortcuts
 * pinned by *any* of them, so switching launcher does not empty this list. Conversely a launcher
 * that never unpins on icon removal keeps reporting a shortcut no longer on any home screen.
 */
@Singleton
class PinnedShortcutsSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : PinnedShortcutsRepository {

    override suspend fun pinnedIds(): Set<String>? = withContext(io) {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: run {
            Log.w(TAG, "No ShortcutManager; cannot reconcile locks against the launcher")
            return@withContext null
        }
        runCatching { manager.pinnedShortcuts.mapTo(mutableSetOf()) { it.id } }
            .onFailure { Log.w(TAG, "Could not read pinned shortcuts; leaving locks untouched", it) }
            .getOrNull()
    }

    private companion object {
        const val TAG = "SlowLock"
    }
}
