package com.slowlock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.slowlock.apps.AppListScreen
import com.slowlock.delay.DelayConfigScreen
import com.slowlock.delay.DelayConfigStore
import com.slowlock.locks.IntroScreen
import com.slowlock.locks.LocksScreen
import com.slowlock.locks.LocksViewModel
import com.slowlock.shortcut.IconTreatment
import com.slowlock.shortcut.PinSupport
import com.slowlock.shortcut.PinUnsupportedScreen
import com.slowlock.shortcut.ShortcutConfigScreen
import com.slowlock.shortcut.pinSupport
import kotlinx.coroutines.launch

/**
 * Arbitrates which screen is showing.
 *
 * It lives at the top level rather than inside `apps/`, `delay/`, or `shortcut/` because it
 * belongs to none of them — it is what connects them. Navigation is a `when` over root state
 * with no navigation library: the app has a handful of root states and one transition each way,
 * and `navigation-compose` is a dependency whose graph, routes, and argument encoding exist to
 * solve problems this app does not have (research.md R9, Constitution II). Feature 005 added the
 * fourth stage — `Home` — and that is still the whole of the navigation story.
 *
 * **[Stage.Home] is what the app opens on and returns to** (005 N2, FR-017). It renders the intro
 * when the user has no locks and the Locks screen when they have some, and **which one is derived
 * from the lock list rather than stored**: there is no "has been introduced" flag, because "has
 * this user been introduced?" and "does this user have any locks?" are the same question
 * (FR-019a). `Stage.List` is now a step inside the flow rather than the app's front door.
 *
 * A row tap opens the **delay** screen (003 FR-001), which replaced feature 002's tap-to-pin
 * exactly as that replaced feature 001's interim launch. Neither swap needed an edit to
 * `AppListScreen`: it reports a selection and stops there, which is what
 * `contracts/selection-handoff.md` was written to guarantee before either consumer existed.
 *
 * **The tap reads [DelayConfigStore] before it navigates** (N1, research.md R3). Both
 * configuration screens open on the app's saved values, so reopening a configured app shows what
 * was chosen last time rather than the default (FR-012, FR-013). This is the root's read, not
 * either screen's: `DelayConfigScreen` is forbidden to touch the store at all (D9), and
 * `ShortcutConfigScreen` only ever writes.
 *
 * **The chosen delay and treatment live in [Stage], not in the screens that show them.** That is
 * obligation N2 and the whole reason the stage is a data class rather than a package name: back
 * from the shortcut screen must return to the delay screen showing the value the user chose on
 * the way through, not the one on disk (FR-014). A screen that owned it would have lost it when
 * it left composition, and one that re-read the store would show the saved value instead
 * (research.md R9).
 *
 * `rememberSaveable` so a rotation or a process death on either configuration screen returns to
 * that screen — with its value — rather than dumping the user back at the list (FR-008).
 *
 * A [rememberSaveableStateHolder] wraps each branch, which is what makes FR-011 hold. The app
 * list and the search query already survive on their own — the list lives in `AppListViewModel`,
 * scoped to the Activity's `ViewModelStore`, and the query is mirrored into its `SavedStateHandle`
 * — but **scroll position is the exception**: `rememberLazyListState` saves through
 * `rememberSaveable`, which is discarded when `AppListScreen` leaves composition unless something
 * retains it. The holder is that something; it is the same mechanism `NavHost` uses internally
 * (research.md R9). Without it the user returns to the top of the list every time — now across
 * two screens rather than one, which is why the list's entry is retained while the other two are
 * dropped on the way out (N3).
 *
 * Pin support gates all four branches (002 FR-029, 003 FR-004, 005 N1). It is re-read on every
 * `ON_START` and held in a plain [remember] — deliberately **not** `rememberSaveable`, because a
 * saved answer is exactly the stale one FR-028 forbids: it would be restored from the bundle of
 * a process that died under a different launcher.
 */
@Composable
fun SlowLockRoot(modifier: Modifier = Modifier) {
    var stage by rememberSaveable(stateSaver = StageSaver) { mutableStateOf<Stage>(Stage.Home) }
    val stateHolder = rememberSaveableStateHolder()

    val context = LocalContext.current
    var support by remember { mutableStateOf<PinSupport>(PinSupport.Unknown) }

    // The locks, their configuration and their display facts — read once per `ON_START` by the
    // view model, which owns them so they survive a rotation without three sources being read
    // again (N8).
    //
    // Scoped to the Activity's `ViewModelStore`, so this is the same instance across every
    // recomposition and every configuration change. Its state is deliberately **not** saved into
    // the instance-state bundle: a saved list is a stale list, and this one is one disk read away
    // from being right — the same argument `support` below is held in a plain [remember] for.
    val locksViewModel: LocksViewModel = viewModel()
    val locksState by locksViewModel.uiState.collectAsStateWithLifecycle()

    // The root's own store, and the one the delay stage is loaded through (N1). It is held here
    // rather than inside the list branch so the read outlives the branch that started it: the
    // scope below is cancelled when the composable that remembered it leaves composition, and
    // the whole point of this read is that it *ends* by leaving the list behind.
    //
    // `ShortcutConfigScreen` still remembers its own instance for the apply write, and that is
    // not a duplicate in any sense that costs anything: `getSharedPreferences` is cached per file
    // per process by the framework, so both wrappers hold the same underlying object. Handing the
    // store across that seam would widen a signature `contracts/delay-config-screen.md` fixes.
    val store = remember(context) { DelayConfigStore(context) }
    val scope = rememberCoroutineScope()

    // FR-028: `ON_START` is the one hook that fires both on first launch and on every return to
    // the foreground, which is the pair of moments the requirement names — and it costs one
    // binder call, with nothing left running while the app is away (research.md R2). Checking
    // once in `onCreate` would be cheaper still and would strand a user who switched launcher
    // until they force-stopped the app.
    //
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        support = pinSupport(context)
    }

    // **`ON_RESUME`, not `ON_START`, and the pin dialog is why** (N8). The launcher's confirmation
    // is a dialog over this activity, so it pauses the app without stopping it: `ON_START` does
    // not fire when it closes, and a list refreshed only there would not show the lock the user
    // just accepted until they next backgrounded the app.
    //
    // `ON_RESUME` covers that, and everything `ON_START` did — first launch, the return to the
    // foreground, the return from an uninstall, a language change — for one disk read and one
    // binder call, with nothing left running while the app is away (SC-013). No polling, no
    // observer, no service.
    //
    // It costs a few more calls than `ON_START` would, and that is the trade: the alternative is
    // a screen that disagrees with the home screen for as long as the user does not think to
    // background the app.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        locksViewModel.refresh()
    }

    when (support) {
        // Not an answer, and rendered as one would flash the wrong screen at somebody: the list
        // on a device that cannot use it, or an error at everyone else. The gap lasts one binder
        // call, so there is nothing worth showing in it either.
        PinSupport.Unknown -> Unit

        // In place of the list, not alongside it — with the whole root taken over, none of the
        // four branches is reachable by any route, including a stage still sitting in `stage`
        // from before the launcher changed (002 FR-029, 003 FR-004, N4). That stage is left
        // untouched rather than cleared: if support comes back, the user returns to the screen
        // they were on, delay and all.
        PinSupport.Unsupported -> PinUnsupportedScreen(
            // The same evaluation `ON_START` runs, so the button and the automatic re-check
            // cannot drift apart (FR-031, U5).
            onRecheck = { support = pinSupport(context) },
            modifier = modifier,
        )

        // FR-032: no restart needed. The user switched launcher, came back, `ON_START` fired,
        // and this branch simply wins the recomposition.
        PinSupport.Supported -> when (val current = stage) {
            // N2: **which screen this renders is derived from the lock list, never stored.** The
            // intro is the empty state, so "has this user been introduced?" and "does this user
            // have any locks?" are one question asked once (FR-019a).
            Stage.Home -> stateHolder.SaveableStateProvider(HOME_KEY) {
                when {
                    // Nothing, on the **first** read only — the same rule `PinSupport.Unknown`
                    // follows above, and for the same reason: the gap is one disk read, and a
                    // flashed intro on a device that has ten locks is worse than a blank frame
                    // (research R4). `loaded` latches, so no later refresh can blank this.
                    !locksState.loaded -> Unit

                    locksState.showsIntro -> IntroScreen(
                        onStart = { stage = Stage.List },
                        modifier = modifier,
                    )

                    else -> LocksScreen(
                        state = locksState,
                        iconCache = locksViewModel.iconCache,
                        // FR-014: the one route from here back into the flow, and the same
                        // destination the intro's action has — there is one way to make a lock.
                        onNewLock = { stage = Stage.List },
                        // FR-023: a row tap opens the delay step on the lock's **saved** values,
                        // carrying `Origin.Home` so back returns here rather than to the app
                        // list.
                        //
                        // **No re-read of `DelayConfigStore`** — unlike the app-list tap below,
                        // which has to load before it can navigate. The read is already done:
                        // `LocksViewModel.refresh()` resolved every row's delay and treatment out
                        // of the same store on `ON_START`, and N6 permits using them directly.
                        // Re-reading here would be a second trip to disk for an answer already in
                        // hand, and would open a frame in which the two could disagree.
                        //
                        // Only available rows call this; `LocksScreen` attaches no click modifier
                        // to an unavailable one (K3), so there is no need to filter here.
                        onEdit = { packageName ->
                            val lock = locksState.locks.firstOrNull {
                                it.packageName == packageName
                            } ?: return@LocksScreen
                            stage = Stage.Delay(
                                packageName = lock.packageName,
                                seconds = lock.delaySeconds,
                                treatment = lock.treatment,
                                origin = Origin.Home,
                            )
                        },
                        // FR-021: the long press and the accessibility action open an
                        // **explanation**, not a confirmation — SlowLock cannot remove a lock, so
                        // there is nothing here for the root to do and nothing for the view model
                        // to write. Removal happens on the home screen, and the list catches up on
                        // the next `ON_RESUME` like any other change to the pinned set (FR-003a).
                        //
                        // Removing the last icon therefore empties the list, `showsIntro` turns
                        // true, and this same `Stage.Home` branch renders the intro with no code
                        // path of its own (N2, US5 scenario 5).
                        onExplainRemoval = locksViewModel::onExplainRemoval,
                        onDismissExplanation = locksViewModel::onDismissExplanation,
                        modifier = modifier,
                    )
                }
            }

            Stage.List -> stateHolder.SaveableStateProvider(LIST_KEY) {
                AppListScreen(
                    // **The load finishes before the transition, not after it** (N1, D12, D13,
                    // research.md R3). The delay screen's first composition already carries the
                    // app's saved values; there is no frame in which it shows the default and
                    // then corrects itself, which is how a user learns not to trust the number
                    // (FR-012, FR-013).
                    //
                    // `load` is never null — an app with nothing stored answers
                    // `DelayConfig.DEFAULT`, so "configured" and "unconfigured" are the same code
                    // path here and there is no branch to forget (FR-006, FR-032).
                    //
                    // The gap this opens is a `SharedPreferences` first load on `Dispatchers.IO`,
                    // measured in milliseconds, during which the list simply stays up and stays
                    // interactive. That is the alternative R3 chose over a spinner or a layout
                    // shift on the screen being navigated to.
                    onAppSelected = { packageName ->
                        scope.launch {
                            val config = store.load(packageName)
                            stage = Stage.Delay(
                                packageName = packageName,
                                seconds = config.delaySeconds,
                                treatment = config.treatment,
                                // The flow was entered from the list, so back out of the delay
                                // step returns there rather than to the root (N3, FR-023).
                                origin = Origin.List,
                            )
                        }
                    },
                    // FR-028/FR-030: the way out of step 1, and the destination is `Home` —
                    // the screen the flow was entered from.
                    //
                    // `LIST_KEY` is **not** dropped on the way out, unlike the two configuration
                    // branches: the scroll position and the query are exactly what must survive
                    // the round trip to the Locks screen and back (N3, N4, 003 FR-011). A user
                    // who scrolls to the Ws, backs out to check something and returns should not
                    // have to scroll again.
                    onBack = { stage = Stage.Home },
                    modifier = modifier,
                )
            }

            is Stage.Delay -> stateHolder.SaveableStateProvider(DELAY_KEY) {
                DelayConfigScreen(
                    packageName = current.packageName,
                    seconds = current.seconds,
                    // N2/D5: the value lands here, on the stage, and nowhere else. This is the
                    // single line that makes FR-014 hold.
                    onSecondsChange = { stage = current.copy(seconds = it) },
                    // The treatment rides along unchanged — the delay screen never sees it, and
                    // the shortcut screen needs it as its opening selection (FR-013, C15).
                    onNext = {
                        stage = Stage.Shortcut(
                            packageName = current.packageName,
                            seconds = current.seconds,
                            treatment = current.treatment,
                            // Carried, not re-derived: the icon step needs it for one thing only
                            // — handing it back to the delay step on a back press (N3).
                            origin = current.origin,
                        )
                    },
                    // FR-023 / US4 scenario 3: **back goes where the flow was entered from.**
                    // Creating a lock came through the app list and returns to it, mid-scroll and
                    // mid-query; editing one came from the Locks screen and returns there. This
                    // is the only thing [Origin] decides, and the only place it is read.
                    //
                    // `DELAY_KEY` is dropped either way — the configuration screens' state is
                    // never carried across an exit (N4).
                    onBack = { stage = leaveDelay(stateHolder, current.origin) },
                    modifier = modifier,
                )
            }

            is Stage.Shortcut -> stateHolder.SaveableStateProvider(CONFIG_KEY) {
                ShortcutConfigScreen(
                    packageName = current.packageName,
                    delaySeconds = current.seconds,
                    // FR-013/C15: the app's **saved** treatment, loaded on the row tap and
                    // carried here on the stage — not `IconTreatment.entries.first()`. Original
                    // is still what an unconfigured app opens on, but only because that is what
                    // the store answered for it, which is why this screen cannot tell the two
                    // cases apart and does not need to.
                    initialTreatment = current.treatment,
                    // FR-014: back returns to the delay screen carrying **the same seconds**,
                    // not a re-read of the store — the value the user chose on the way through
                    // is the one they get back. `CONFIG_KEY` is dropped so an abandoned
                    // treatment does not survive the round trip and reappear, for a different
                    // app, the next time this screen opens (N3).
                    onBack = {
                        stateHolder.removeState(CONFIG_KEY)
                        stage = Stage.Delay(
                            packageName = current.packageName,
                            seconds = current.seconds,
                            treatment = current.treatment,
                            // The origin rides back too, so a second back from the delay step
                            // still lands where the flow began rather than defaulting to one of
                            // the two (N3).
                            origin = current.origin,
                        )
                    },
                    // The save and the pin request have both been issued (C17). Nothing is said
                    // to the user here or anywhere on this path — navigation is the only
                    // difference between this exit and a back (N5, 002 FR-012).
                    //
                    // **The refresh finishes before the transition** — the same rule the app-list
                    // tap follows above, and for the same reason (N1, R3).
                    //
                    // This is not the refresh that shows a newly accepted pin; `ON_RESUME` does
                    // that when the launcher's dialog closes. It is for the case with **no
                    // dialog**: re-pinning an app that already has a lock succeeds silently, so
                    // nothing pauses this activity and no lifecycle event fires — and an edit's
                    // new delay and treatment would otherwise land on a row still showing the old
                    // ones until something else happened to refresh it.
                    onCreated = {
                        scope.launch {
                            locksViewModel.refresh().join()
                            stage = returnHome(stateHolder)
                        }
                    },
                    modifier = modifier,
                )
            }
        }
    }
}

/**
 * Leaves both configuration screens behind and returns to the root (N3).
 *
 * The destination is `Home`, not `List`: finishing or abandoning the flow puts the user back on
 * their locks — or on the intro, if the flow was abandoned and they still have none — rather than
 * on the list of every installed app they just left.
 *
 * `removeState` first, and this is not incidental. The holder's whole job is to carry a branch's
 * `rememberSaveable` state across it leaving composition — which is what the list wants and what
 * neither configuration screen may have. Left in, the holder would restore the previous
 * treatment the next time the shortcut screen opened, for a different app. Dropping the entries
 * on the way out is how a state holder is told a destination is being popped rather than merely
 * hidden; both screens' state still survives rotation and process death (FR-008), which never
 * leave composition in the first place.
 *
 * `LIST_KEY` is deliberately **not** dropped: it holds the scroll position and is the one thing
 * that must survive the round trip (FR-011). Neither is `HOME_KEY`, for the same reason — the
 * Locks list has a scroll position too, and the user is returning *to* it rather than leaving it
 * (N4).
 */
private fun returnHome(stateHolder: SaveableStateHolder): Stage {
    stateHolder.removeState(DELAY_KEY)
    stateHolder.removeState(CONFIG_KEY)
    return Stage.Home
}

/**
 * Leaves the delay step for wherever the flow was entered from (N3, FR-023).
 *
 * The only consumer of [Origin], and the only reason it exists. Creating a lock started at the app
 * list and goes back to it — with its scroll position and query intact, because `LIST_KEY` is
 * retained; editing one started at the Locks screen and goes back there.
 *
 * The state-holder rules are the same on both paths: `DELAY_KEY` and `CONFIG_KEY` are dropped on
 * any exit from the flow, so an abandoned delay or treatment cannot reappear for a different app.
 * That is why this drops them itself rather than only the `Home` path doing it through
 * [returnHome].
 *
 * Nothing is written on the way out. `Stage` is transient and the only writes in the feature are
 * in `ShortcutConfigScreen`'s `create()`, so an abandoned edit leaves both the configuration
 * record and the lock record exactly as they were, with no rollback path to get wrong (N7,
 * FR-023a).
 */
private fun leaveDelay(stateHolder: SaveableStateHolder, origin: Origin): Stage = when (origin) {
    Origin.List -> {
        stateHolder.removeState(DELAY_KEY)
        stateHolder.removeState(CONFIG_KEY)
        Stage.List
    }

    Origin.Home -> returnHome(stateHolder)
}

/**
 * Which screen is showing, and what the user has chosen so far (data-model.md, research.md R9).
 *
 * Transient: it survives rotation and process death through [StageSaver] and nothing more. The
 * durable copy of these two values is `DelayConfigStore`'s, written only by "Create shortcut".
 */
sealed interface Stage {

    /**
     * The root the app opens on and returns to (N2, FR-017).
     *
     * **It renders two screens, and which one is derived rather than stored**: the intro when
     * there are no locks, the Locks screen when there are. There is deliberately no `Stage.Intro`
     * — adding one would need a "has been introduced" flag to decide between them, which FR-019a
     * forbids, and would make removing the last lock a special case instead of the same question
     * asked again.
     */
    data object Home : Stage

    data object List : Stage
    data class Delay(
        val packageName: String,
        val seconds: Int,
        val treatment: IconTreatment,
        val origin: Origin,
    ) : Stage

    data class Shortcut(
        val packageName: String,
        val seconds: Int,
        val treatment: IconTreatment,
        val origin: Origin,
    ) : Stage
}

/**
 * Where the flow was entered from (data-model.md §5, N3).
 *
 * **It decides exactly one thing**: where a back from the delay step goes — the app list when the
 * user is creating a lock, the Locks screen when they are editing one (FR-023, US4 scenario 3).
 *
 * It carries no other meaning, and deliberately so. The two paths are otherwise identical: both
 * steps read `2 / 3` and `3 / 3` either way (FR-029), both screens show the same saved values,
 * and finishing writes the same record through the same `create()` — which is why "edit" needs no
 * mode, no flag on the record, and no second code path. `Stage.Shortcut` carries it only to hand
 * it back to `Stage.Delay` on a back press.
 */
enum class Origin { List, Home }

/**
 * Saves [Stage] into the instance-state bundle.
 *
 * A `listSaver` rather than the default: `Stage` is a sealed interface, and while
 * [IconTreatment] is `Serializable` and the other two fields are primitives, the *type* of the
 * stage is not a value the default saver could carry. The discriminant is written explicitly and
 * read back through an exhaustive `when`, so a fifth stage added without a line here fails to
 * compile rather than restoring as the list.
 *
 * The treatment is stored by **name**, matching `DelayConfigStore` — the same frozen tokens, for
 * the same reason (`contracts/delay-config-store.md`, Constitution V). An unrecognised token
 * sanitises to `Original` rather than throwing: a bundle written by a previous build is exactly
 * the case the store's own read rule was written for. [Origin] rides along under the same rule
 * (N9), which is what lets a rotation mid-edit still know where back goes.
 */
private val StageSaver = listSaver<Stage, Any>(
    save = { stage ->
        when (stage) {
            Stage.Home -> listOf(HOME_TAG)
            Stage.List -> listOf(LIST_TAG)
            is Stage.Delay -> listOf(
                DELAY_TAG, stage.packageName, stage.seconds,
                stage.treatment.name, stage.origin.name,
            )

            is Stage.Shortcut -> listOf(
                SHORTCUT_TAG, stage.packageName, stage.seconds,
                stage.treatment.name, stage.origin.name,
            )
        }
    },
    restore = { saved ->
        when (saved.firstOrNull()) {
            DELAY_TAG -> Stage.Delay(
                packageName = saved[1] as String,
                seconds = saved[2] as Int,
                treatment = treatmentNamed(saved[3] as String),
                origin = originNamed(saved[4] as String),
            )

            SHORTCUT_TAG -> Stage.Shortcut(
                packageName = saved[1] as String,
                seconds = saved[2] as Int,
                treatment = treatmentNamed(saved[3] as String),
                origin = originNamed(saved[4] as String),
            )

            LIST_TAG -> Stage.List

            // **`Home`, not `List`** (N9). An absent or unrecognised discriminant means a bundle
            // this build did not write — from an older version, or a newer one — and the honest
            // answer to "where was the user?" is then the root, which is where they would have
            // started. Restoring as the list would strand somebody with no locks on a screen the
            // app no longer opens on.
            else -> Stage.Home
        }
    },
)

private fun treatmentNamed(token: String): IconTreatment =
    IconTreatment.entries.firstOrNull { it.name == token } ?: IconTreatment.entries.first()

/**
 * The same sanitising rule the treatment follows (N9): an unrecognised token — a bundle written by
 * a build whose [Origin] entries differed — restores as the first entry rather than throwing.
 *
 * The worst case is a back press going to the app list instead of the Locks screen, once, after a
 * process death across an upgrade. That is the cheapest possible failure, and it is why this
 * sanitises rather than falling back to `Stage.Home`.
 */
private fun originNamed(token: String): Origin =
    Origin.entries.firstOrNull { it.name == token } ?: Origin.entries.first()

private const val HOME_TAG = "home"
private const val LIST_TAG = "list"
private const val DELAY_TAG = "delay"
private const val SHORTCUT_TAG = "shortcut"

// Distinct from any package name, which cannot contain a colon — the two configuration branches
// are keyed per-root rather than per-package because their state is dropped on every exit anyway.
private const val HOME_KEY = "root:home"
private const val LIST_KEY = "root:app-list"
private const val DELAY_KEY = "root:delay-config"
private const val CONFIG_KEY = "root:shortcut-config"
