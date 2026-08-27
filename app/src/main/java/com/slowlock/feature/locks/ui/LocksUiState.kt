package com.slowlock.feature.locks.ui

import com.slowlock.feature.locks.domain.Lock

/**
 * What the `Home` destination is showing.
 *
 * Three fields, and every screen decision is derived from them rather than stored beside them (N2,
 * FR-019a): not loaded renders nothing, loaded-and-empty renders [IntroScreen] (FR-017), and loaded
 * with locks renders [LocksScreen].
 *
 * That removes any need for a persisted "has been introduced" flag — it is the same question as
 * "does this user have any locks?" — so removing the last lock brings the intro back with no code
 * path of its own.
 */
data class LocksUiState(
    /**
     * False until the first read completes and never false again — it latches, and [withLocks] is
     * the only thing that sets it (FR-016). Without the latch, every `ON_START` would blank the
     * screen for the length of a disk read on the way back from the flow.
     */
    val loaded: Boolean = false,
    /** The rows, in the lock record's insertion order (FR-006). */
    val locks: List<Lock> = emptyList(),
    /**
     * The package whose "how do I remove this?" explanation is showing, or `null` (FR-021, K4).
     *
     * Not a pending action: the app cannot remove a lock at all (FR-003a), so the dialog explains
     * rather than offers. It lives here rather than in the row so it survives recomposition and so
     * at most one can be open. Deliberately transient — it is not expected to survive process
     * death.
     */
    val explainingRemoval: String? = null,
) {

    /** `loaded && locks.isEmpty()` — the intro's condition, named so the root does not spell it. */
    val showsIntro: Boolean get() = loaded && locks.isEmpty()

    /** `loaded && locks.isNotEmpty()` — the Locks screen's condition. */
    val showsLocks: Boolean get() = loaded && locks.isNotEmpty()

    /**
     * The state after a completed read. Rows are replaced in place and [loaded] is set rather than
     * toggled, which is the whole of FR-016: nothing here can put [loaded] back to false.
     */
    fun withLocks(locks: List<Lock>): LocksUiState = copy(loaded = true, locks = locks)
}
