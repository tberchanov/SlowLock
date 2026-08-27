package com.slowlock.feature.locks.domain

import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.DelayConfigRepository
import javax.inject.Inject

/**
 * The Locks screen's rows, in one pass over the four things they are made of.
 *
 * The list is derived from the launcher, not read from a record (FR-003a): a lock exists exactly
 * when its shortcut is pinned, which is what makes the screen agree with the home screen — a
 * declined pin dialog never creates one, and an icon dragged off takes its lock.
 *
 * **`null` from [PinnedShortcutsRepository.pinnedIds] is not an empty set.** It means the launcher
 * could not be asked, and the only safe reading of "we don't know" is the last list that was known
 * good. Passing an empty set here in its place would empty the user's screen — which is why the
 * store no longer offers a call that could be handed one.
 */
class LoadLocksUseCase @Inject constructor(
    private val lockOrder: LockOrderRepository,
    private val pinnedShortcuts: PinnedShortcutsRepository,
    private val config: DelayConfigRepository,
    private val targets: AppTargetRepository,
) {

    suspend operator fun invoke(): List<Lock> {
        val pinned = pinnedShortcuts.pinnedIds()
        val cached = lockOrder.loadOrder()
        val packages = if (pinned == null) cached else reconcile(cached, pinned)
        return assembleLocks(packages, config::load, targets::resolve)
    }

    /**
     * The launcher's answer, ordered by what was last seen. Written back only when the order
     * actually changed, so an ordinary visit costs a read and no write.
     */
    private suspend fun reconcile(cached: List<String>, pinned: Set<String>): List<String> =
        deriveLocks(cached, pinned).also { if (it != cached) lockOrder.saveOrder(it) }
}
