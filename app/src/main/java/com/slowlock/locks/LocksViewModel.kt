package com.slowlock.locks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.slowlock.apps.AppIconCache
import com.slowlock.delay.DelayConfig
import com.slowlock.delay.DelayConfigStore
import com.slowlock.shortcut.ShortcutTarget
import com.slowlock.shortcut.resolveShortcutTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the Locks screen's state so it survives rotation without re-reading three sources, shaped
 * like [com.slowlock.apps.AppListViewModel] because it does the same job for the other list.
 *
 * **The lookups are injected lambdas, not a `PackageManager` reached for inline.** That is the
 * seam [LocksViewModelTest] drives — the null-resolution path the constitution requires a unit
 * test for (FR-020) — and it is why [assembleLocks] is a free function rather than a method here.
 * Do not inline them.
 *
 * **This class holds only the wiring.** Everything decidable lives outside it: the rows in
 * [assembleLocks], the latch in [LocksUiState.withLocks]. `viewModelScope` dispatches on
 * `Dispatchers.Main`, which a JVM test has no way to install without a new test dependency
 * (FR-039) — so nothing worth asserting is allowed to live behind it.
 *
 * `@JvmOverloads` is what keeps the default `SavedStateViewModelFactory` able to find the
 * `(Application)` constructor it looks for by reflection.
 */
class LocksViewModel @JvmOverloads constructor(
    app: Application,
    private val lockStore: LockStore = LockStore(app),
    private val loadConfig: suspend (String) -> DelayConfig = DelayConfigStore(app)::load,
    private val resolveTarget: suspend (String) -> ShortcutTarget? = {
        resolveShortcutTarget(app, it)
    },
    /**
     * The launcher's pinned shortcuts, or `null` when it could not be asked — injected on the same
     * seam and for the same reason as the two lookups above.
     */
    private val readPinned: suspend () -> Set<String>? = { pinnedShortcutIds(app) },
) : AndroidViewModel(app) {

    /**
     * Exposed so each row loads its own icon lazily as it scrolls into view, exactly as the app
     * list's rows do — and so [refresh] does not have to wait for a rasterization it does not need
     * (FR-015).
     *
     * Built on first use rather than in `init`: it reaches for `LauncherApps`, and the ViewModel
     * must stay constructible without one.
     */
    val iconCache by lazy { AppIconCache(getApplication()) }

    private val _uiState = MutableStateFlow(LocksUiState())
    val uiState: StateFlow<LocksUiState> = _uiState.asStateFlow()

    /**
     * Re-reads the locks, their configuration and their display facts — **one pass, off the main
     * thread** (FR-040, R5).
     *
     * Called on every `ON_START`, and on **completing the flow** (N8). `ON_START` covers first
     * launch, the return to the foreground, the return from an uninstall and a language change,
     * for one disk read and nothing left running while the app is away (SC-013). No polling, no
     * observer, no service.
     *
     * Finishing the flow needs its own call because it is **not** a lifecycle event: the launcher's
     * pin dialog does not reliably stop the activity, so a lock created and returned from would
     * otherwise sit unseen until the user next backgrounded the app. The returned [Job] is what
     * lets the root wait for the read before it navigates, so the new lock is on screen in the
     * first frame of the Locks screen rather than a moment later (R3, N1).
     *
     * A refresh over a populated list **replaces the rows in place and never clears `loaded`**
     * (FR-016) — see [LocksUiState.withLocks], where that rule lives so a test can reach it.
     */
    fun refresh(): Job = viewModelScope.launch {
        // **The list is derived from the launcher, not read from a record** (FR-003a). A lock
        // exists exactly when its shortcut is pinned and the user has not removed it, which is
        // what makes the screen agree with the home screen — a declined pin dialog never creates
        // one, and an icon dragged off takes its lock with it.
        //
        // `null` is not an empty set: it means the launcher could not be asked (no
        // `ShortcutManager`, or a direct-boot read that threw), and the only safe reading of
        // "we don't know" is the last list that was known good. Falling through to `load()` is
        // that reading.
        //
        // Which packages survive is [deriveLocks]'s decision, not this one.
        val pinned = readPinned()
        val packages = if (pinned == null) lockStore.load() else lockStore.derive(pinned)
        val locks = assembleLocks(packages, loadConfig, resolveTarget)
        _uiState.update { it.withLocks(locks) }
    }

    /**
     * Opens the "how do I remove this?" explanation for one lock (FR-021, contract K4).
     *
     * **There is no confirming counterpart.** SlowLock cannot remove a lock: a lock is its pinned
     * shortcut (FR-003a), Android offers no way to unpin one, and the user's home screen is the
     * only place the action exists. An in-app "Remove" that merely hid the row would be worse than
     * nothing — the icon would stay, still waiting, still opening the app, and the list would have
     * started lying about what a lock is.
     *
     * So this opens an explanation and the only other thing that can happen to it is
     * [onDismissExplanation]. At most one is ever open, because there is only one place for a
     * package to sit.
     */
    fun onExplainRemoval(packageName: String) {
        _uiState.update { it.copy(explainingRemoval = packageName) }
    }

    /** Closes the explanation. Nothing else happens, because nothing else was pending. */
    fun onDismissExplanation() {
        _uiState.update { it.copy(explainingRemoval = null) }
    }
}
