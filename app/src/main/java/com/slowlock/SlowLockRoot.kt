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
import com.slowlock.apps.AppListScreen
import com.slowlock.delay.DelayConfigScreen
import com.slowlock.delay.DelayConfigStore
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
 * solve problems this app does not have (research.md R9, Constitution II).
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
 * Pin support gates all three branches (002 FR-029, 003 FR-004). It is re-read on every
 * `ON_START` and held in a plain [remember] — deliberately **not** `rememberSaveable`, because a
 * saved answer is exactly the stale one FR-028 forbids: it would be restored from the bundle of
 * a process that died under a different launcher.
 */
@Composable
fun SlowLockRoot(modifier: Modifier = Modifier) {
    var stage by rememberSaveable(stateSaver = StageSaver) { mutableStateOf<Stage>(Stage.List) }
    val stateHolder = rememberSaveableStateHolder()

    val context = LocalContext.current
    var support by remember { mutableStateOf<PinSupport>(PinSupport.Unknown) }

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
    LifecycleEventEffect(Lifecycle.Event.ON_START) { support = pinSupport(context) }

    when (support) {
        // Not an answer, and rendered as one would flash the wrong screen at somebody: the list
        // on a device that cannot use it, or an error at everyone else. The gap lasts one binder
        // call, so there is nothing worth showing in it either.
        PinSupport.Unknown -> Unit

        // In place of the list, not alongside it — with the whole root taken over, none of the
        // three branches is reachable by any route, including a stage still sitting in `stage`
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
                            )
                        }
                    },
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
                        )
                    },
                    onBack = { stage = returnToList(stateHolder) },
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
                        )
                    },
                    // The save and the pin request have both been issued (C17). Nothing is said
                    // to the user here or anywhere on this path — navigation is the only
                    // difference between this exit and a back (N5, 002 FR-012).
                    onCreated = { stage = returnToList(stateHolder) },
                    modifier = modifier,
                )
            }
        }
    }
}

/**
 * Leaves both configuration screens behind (N3).
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
 * that must survive the round trip (FR-011).
 */
private fun returnToList(stateHolder: SaveableStateHolder): Stage {
    stateHolder.removeState(DELAY_KEY)
    stateHolder.removeState(CONFIG_KEY)
    return Stage.List
}

/**
 * Which screen is showing, and what the user has chosen so far (data-model.md, research.md R9).
 *
 * Transient: it survives rotation and process death through [StageSaver] and nothing more. The
 * durable copy of these two values is `DelayConfigStore`'s, written only by "Create shortcut".
 */
sealed interface Stage {
    data object List : Stage
    data class Delay(
        val packageName: String,
        val seconds: Int,
        val treatment: IconTreatment,
    ) : Stage

    data class Shortcut(
        val packageName: String,
        val seconds: Int,
        val treatment: IconTreatment,
    ) : Stage
}

/**
 * Saves [Stage] into the instance-state bundle.
 *
 * A `listSaver` rather than the default: `Stage` is a sealed interface, and while
 * [IconTreatment] is `Serializable` and the other two fields are primitives, the *type* of the
 * stage is not a value the default saver could carry. The discriminant is written explicitly and
 * read back through an exhaustive `when`, so a fourth stage added without a line here fails to
 * compile rather than restoring as the list.
 *
 * The treatment is stored by **name**, matching `DelayConfigStore` — the same frozen tokens, for
 * the same reason (`contracts/delay-config-store.md`, Constitution V). An unrecognised token
 * sanitises to `Original` rather than throwing: a bundle written by a previous build is exactly
 * the case the store's own read rule was written for.
 */
private val StageSaver = listSaver<Stage, Any>(
    save = { stage ->
        when (stage) {
            Stage.List -> listOf(LIST_TAG)
            is Stage.Delay ->
                listOf(DELAY_TAG, stage.packageName, stage.seconds, stage.treatment.name)
            is Stage.Shortcut ->
                listOf(SHORTCUT_TAG, stage.packageName, stage.seconds, stage.treatment.name)
        }
    },
    restore = { saved ->
        when (saved.firstOrNull()) {
            DELAY_TAG -> Stage.Delay(
                packageName = saved[1] as String,
                seconds = saved[2] as Int,
                treatment = treatmentNamed(saved[3] as String),
            )

            SHORTCUT_TAG -> Stage.Shortcut(
                packageName = saved[1] as String,
                seconds = saved[2] as Int,
                treatment = treatmentNamed(saved[3] as String),
            )

            else -> Stage.List
        }
    },
)

private fun treatmentNamed(token: String): IconTreatment =
    IconTreatment.entries.firstOrNull { it.name == token } ?: IconTreatment.entries.first()

private const val LIST_TAG = "list"
private const val DELAY_TAG = "delay"
private const val SHORTCUT_TAG = "shortcut"

// Distinct from any package name, which cannot contain a colon — the two configuration branches
// are keyed per-root rather than per-package because their state is dropped on every exit anyway.
private const val LIST_KEY = "root:app-list"
private const val DELAY_KEY = "root:delay-config"
private const val CONFIG_KEY = "root:shortcut-config"
