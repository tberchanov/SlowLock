package com.slowlock.locks

/**
 * What the root's [com.slowlock.Stage.Home] branch is currently showing (data-model.md §4).
 *
 * Three fields, and every screen decision is **derived** from them rather than stored beside them
 * (N2, FR-019a):
 *
 * | State | Rendered |
 * |---|---|
 * | `!loaded` | Nothing — the **first** read only |
 * | `loaded && locks.isEmpty()` | [IntroScreen] (FR-017) |
 * | `loaded && locks.isNotEmpty()` | [LocksScreen], one row per lock (FR-009, FR-010) |
 *
 * That is what removes any need for a persisted "has been introduced" flag: "has this user been
 * introduced?" and "does this user have any locks?" are the same question, and removing the last
 * lock brings the intro back with no code path of its own (US5 scenario 5).
 */
data class LocksUiState(
    /**
     * False until the first read completes, and **never false again** — it latches, and
     * [withLocks] is the only thing that sets it (FR-016).
     *
     * A refresh over an already-populated list leaves its rows on screen: the root renders nothing
     * only before there has *ever* been an answer, which is the rule `PinSupport.Unknown` follows
     * above it and for the same reason (research R4). Without the latch, every `ON_START` would
     * blank the screen for the length of a disk read on the way back from the flow.
     */
    val loaded: Boolean = false,
    /** The rows, in the lock record's insertion order (FR-006). */
    val locks: List<Lock> = emptyList(),
    /**
     * The package whose "how do I remove this?" explanation is showing, or `null` (FR-021,
     * contract K4).
     *
     * Not a pending action — **nothing is awaiting confirmation, because the app cannot remove a
     * lock at all.** A lock is its pinned shortcut (FR-003a), and only the user can take an icon
     * off their home screen, so the dialog this opens explains that rather than offering to do it.
     *
     * It lives here rather than in the row so the dialog survives recomposition and so at most one
     * can ever be open. Deliberately transient: it is not expected to survive process death, and
     * the manual test plan says so rather than the code pretending otherwise.
     */
    val explainingRemoval: String? = null,
) {

    /** `loaded && locks.isEmpty()` — the intro's condition, named so the root does not spell it. */
    val showsIntro: Boolean get() = loaded && locks.isEmpty()

    /** `loaded && locks.isNotEmpty()` — the Locks screen's condition. */
    val showsLocks: Boolean get() = loaded && locks.isNotEmpty()

    /**
     * The state after a completed read.
     *
     * The rows are **replaced in place** and [loaded] is set rather than toggled, which is the
     * whole of FR-016: a second read cannot blank a populated screen because there is no code path
     * here that can put [loaded] back to false.
     */
    fun withLocks(locks: List<Lock>): LocksUiState = copy(loaded = true, locks = locks)
}
