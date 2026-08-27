package com.slowlock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowlock.feature.apps.ui.AppListScreen
import com.slowlock.core.domain.IconTreatment
import com.slowlock.feature.delay.ui.DelayConfigScreen
import com.slowlock.feature.locks.ui.IntroScreen
import com.slowlock.feature.locks.ui.LocksScreen
import com.slowlock.feature.locks.ui.LocksViewModel
import com.slowlock.feature.shortcut.domain.PinSupport
import com.slowlock.feature.shortcut.ui.PinUnsupportedScreen
import com.slowlock.feature.shortcut.ui.ShortcutConfigScreen
import kotlinx.coroutines.launch

/**
 * Arbitrates which screen is showing. It belongs to no capability, so it sits at the root package.
 *
 * Navigation is a `when` over [Stage] with no navigation library: a handful of states and one
 * transition each way (research R9).
 *
 * Two rules the rest of the file turns on:
 *
 * - **The chosen delay and treatment live in [Stage], not in the screens showing them**, so back
 *   from the shortcut screen returns the value chosen on the way through rather than the one on
 *   disk (FR-014).
 * - **The tap reads the saved configuration before it navigates**, so neither configuration screen
 *   ever shows a default and then corrects itself (FR-012, FR-013).
 */
@Composable
fun SlowLockRoot(modifier: Modifier = Modifier) {
    // FR-023a: the stage stays in `rememberSaveable` rather than moving to a holder. This is
    // already what delivers the process-death restore, the scroll and query retention across the
    // round trip, and the rule that back returns to whichever screen the flow was entered from —
    // moving it to a `SavedStateHandle` risks all three and gains no principle (research R9).
    var stage by rememberSaveable(stateSaver = StageSaver) { mutableStateOf<Stage>(Stage.Home) }
    val stateHolder = rememberSaveableStateHolder()

    val rootViewModel: RootViewModel = hiltViewModel()
    val support by rootViewModel.support.collectAsStateWithLifecycle()

    // Deliberately not saved into the instance-state bundle: a saved list is a stale list, and this
    // one is a disk read away from being right.
    val locksViewModel: LocksViewModel = hiltViewModel()
    val locksState by locksViewModel.uiState.collectAsStateWithLifecycle()

    // Remembered here rather than in the list branch so the pre-navigation read outlives the branch
    // that started it — the read ends by leaving the list behind.
    val scope = rememberCoroutineScope()

    // FR-028: fires on first launch and on every return to the foreground, for one binder call.
    // Checking once in `onCreate` would strand a user who switched launcher.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        rootViewModel.refreshSupport()
    }

    // `ON_RESUME`, not `ON_START`: the launcher's pin dialog pauses without stopping the app, so
    // `ON_START` does not fire when it closes and a newly accepted lock would not appear (N8).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        locksViewModel.refresh()
    }

    when (support) {
        // Not an answer. Rendering one would flash the wrong screen for the one binder call it
        // takes to get a real one.
        PinSupport.Unknown -> Unit

        // Takes over the whole root, so no stage left in `stage` is reachable by any route. The
        // stage is left untouched rather than cleared: if support returns, so does the user's
        // place.
        PinSupport.Unsupported -> PinUnsupportedScreen(
            // The same evaluation `ON_START` runs, so button and auto-recheck cannot drift (U5).
            onRecheck = rootViewModel::refreshSupport,
            modifier = modifier,
        )

        PinSupport.Supported -> when (val current = stage) {
            // N2, FR-019a: intro vs. Locks is **derived from the lock list, never stored**. There
            // is no "has been introduced" flag, because that is the same question as "any locks?".
            Stage.Home -> stateHolder.SaveableStateProvider(HOME_KEY) {
                when {
                    // First read only. A flashed intro on a device with ten locks is worse than a
                    // blank frame; `loaded` latches, so no later refresh can blank this.
                    !locksState.loaded -> Unit

                    locksState.showsIntro -> IntroScreen(
                        onStart = { stage = Stage.List },
                        modifier = modifier,
                    )

                    else -> LocksScreen(
                        state = locksState,
                        icons = locksViewModel.icons,
                        onNewLock = { stage = Stage.List },
                        // No re-read of the store: `LocksViewModel.refresh()` already resolved
                        // every row's delay and treatment on `ON_RESUME` (N6). Only available rows
                        // call this — `LocksScreen` attaches no click modifier to the others (K3).
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
                        // FR-021: an explanation, not a confirmation — SlowLock cannot remove a
                        // lock, so there is nothing for the root to do. Removal happens on the home
                        // screen and the list catches up on the next `ON_RESUME`.
                        onExplainRemoval = locksViewModel::onExplainRemoval,
                        onDismissExplanation = locksViewModel::onDismissExplanation,
                        modifier = modifier,
                    )
                }
            }

            Stage.List -> stateHolder.SaveableStateProvider(LIST_KEY) {
                AppListScreen(
                    // The load finishes **before** the transition (N1, research R3). `load` never
                    // returns null — an unconfigured app answers `DelayConfig.DEFAULT` — so there
                    // is no branch here to forget. The cost is a `SharedPreferences` first load
                    // during which the list stays up and interactive.
                    onAppSelected = { packageName ->
                        scope.launch {
                            val config = rootViewModel.configFor(packageName)
                            stage = Stage.Delay(
                                packageName = packageName,
                                seconds = config.delaySeconds,
                                treatment = config.treatment,
                                origin = Origin.List,
                            )
                        }
                    },
                    // `LIST_KEY` is deliberately not dropped here: scroll position and query are
                    // exactly what must survive the round trip (N3, N4).
                    onBack = { stage = Stage.Home },
                    modifier = modifier,
                )
            }

            is Stage.Delay -> stateHolder.SaveableStateProvider(DELAY_KEY) {
                DelayConfigScreen(
                    packageName = current.packageName,
                    seconds = current.seconds,
                    // D5: the value lands on the stage and nowhere else. This line is what makes
                    // FR-014 hold.
                    onSecondsChange = { stage = current.copy(seconds = it) },
                    onNext = {
                        stage = Stage.Shortcut(
                            packageName = current.packageName,
                            seconds = current.seconds,
                            treatment = current.treatment,
                            origin = current.origin,
                        )
                    },
                    onBack = { stage = leaveDelay(stateHolder, current.origin) },
                    // Handed down because this screen has no state holder (V4) and may not
                    // construct a data source at a point of use (FR-024).
                    targets = rootViewModel.targets,
                    icons = rootViewModel.icons,
                    modifier = modifier,
                )
            }

            is Stage.Shortcut -> stateHolder.SaveableStateProvider(CONFIG_KEY) {
                ShortcutConfigScreen(
                    packageName = current.packageName,
                    delaySeconds = current.seconds,
                    // The app's saved treatment, carried on the stage — not `entries.first()`.
                    initialTreatment = current.treatment,
                    // `CONFIG_KEY` is dropped so an abandoned treatment cannot reappear the next
                    // time this screen opens, for a different app (N3).
                    onBack = {
                        stateHolder.removeState(CONFIG_KEY)
                        stage = Stage.Delay(
                            packageName = current.packageName,
                            seconds = current.seconds,
                            treatment = current.treatment,
                            origin = current.origin,
                        )
                    },
                    // The refresh finishes before the transition (N1). This is not the refresh that
                    // shows a newly accepted pin — `ON_RESUME` does that. It is for the no-dialog
                    // case: re-pinning an app that already has a lock succeeds silently, so no
                    // lifecycle event fires and an edit's new values would land on a stale row.
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
 * `removeState` first: left in, the holder would restore the previous treatment the next time the
 * shortcut screen opened, for a different app. Both screens still survive rotation and process
 * death, which never leave composition.
 *
 * `LIST_KEY` and `HOME_KEY` are deliberately kept — both lists have a scroll position worth
 * returning to (N4, FR-011).
 */
private fun returnHome(stateHolder: SaveableStateHolder): Stage {
    stateHolder.removeState(DELAY_KEY)
    stateHolder.removeState(CONFIG_KEY)
    return Stage.Home
}

/**
 * Leaves the delay step for wherever the flow was entered from — the only consumer of [Origin].
 *
 * Drops `DELAY_KEY` and `CONFIG_KEY` itself rather than leaving it to [returnHome], because an
 * abandoned delay or treatment must not reappear for a different app on either path.
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
 * Which screen is showing, and what the user has chosen so far.
 *
 * Transient: it survives rotation and process death through [StageSaver] and nothing more. The
 * durable copy is `DelayConfigStore`'s, written only by "Create shortcut".
 */
sealed interface Stage {

    /**
     * Renders the intro when there are no locks and the Locks screen when there are.
     *
     * There is deliberately no `Stage.Intro`: it would need a "has been introduced" flag to choose
     * between them, which FR-019a forbids.
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
 * Where the flow was entered from.
 *
 * **It decides exactly one thing**: where a back from the delay step goes — the app list when
 * creating a lock, the Locks screen when editing one (FR-023). The two paths are otherwise
 * identical, which is why "edit" needs no mode and no second code path.
 */
enum class Origin { List, Home }

/**
 * Saves [Stage] into the instance-state bundle.
 *
 * A `listSaver` because the *type* of the stage is not something the default saver could carry. The
 * discriminant is read back through an exhaustive `when`, so a fifth stage added without a line
 * here fails to compile rather than restoring as the list.
 *
 * The treatment is stored by **name**, matching `DelayConfigStore`'s frozen tokens. An unrecognised
 * token sanitises rather than throwing — a bundle written by a previous build is exactly that case.
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

            // `Home`, not `List`: an unrecognised discriminant means a bundle this build did not
            // write, and restoring as the list would strand someone with no locks on a screen the
            // app no longer opens on (N9).
            else -> Stage.Home
        }
    },
)

private fun treatmentNamed(token: String): IconTreatment =
    IconTreatment.entries.firstOrNull { it.name == token } ?: IconTreatment.entries.first()

/**
 * Sanitises like the treatment does (N9). The worst case is one back press going to the app list
 * instead of the Locks screen after a process death across an upgrade — cheaper than falling back
 * to `Stage.Home` and losing the user's place entirely.
 */
private fun originNamed(token: String): Origin =
    Origin.entries.firstOrNull { it.name == token } ?: Origin.entries.first()

private const val HOME_TAG = "home"
private const val LIST_TAG = "list"
private const val DELAY_TAG = "delay"
private const val SHORTCUT_TAG = "shortcut"

// Distinct from any package name, which cannot contain a colon.
private const val HOME_KEY = "root:home"
private const val LIST_KEY = "root:app-list"
private const val DELAY_KEY = "root:delay-config"
private const val CONFIG_KEY = "root:shortcut-config"
