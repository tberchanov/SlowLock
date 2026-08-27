package com.slowlock

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.slowlock.feature.apps.ui.AppListScreen
import com.slowlock.feature.delay.ui.DelayConfigScreen
import com.slowlock.feature.locks.ui.IntroScreen
import com.slowlock.feature.locks.ui.LocksScreen
import com.slowlock.feature.locks.ui.LocksViewModel
import com.slowlock.feature.shortcut.domain.PinSupport
import com.slowlock.feature.shortcut.ui.PinUnsupportedScreen
import com.slowlock.feature.shortcut.ui.ShortcutConfigScreen

/**
 * Hosts the graph. It belongs to no capability, so it sits at the root package.
 *
 * Every destination obtains its own state holder, so the back stack entry is that holder's scope
 * and a popped screen takes its state with it (FR-015). The one holder above the graph is
 * [RootViewModel], whose declaration says why.
 *
 * Destinations cross-fade in both directions (G11). The library's own default is a slide, which is
 * a stronger statement of hierarchy than a three-step flow with a step counter needs.
 */
@Composable
fun SlowLockRoot(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    val rootViewModel: RootViewModel = hiltViewModel()
    val support by rootViewModel.support.collectAsStateWithLifecycle()

    // FR-028: fires on first launch and on every return to the foreground, for one binder call.
    // The Activity's lifecycle, not an entry's: pin support is a whole-app precondition, and
    // checking once in `onCreate` would strand a user who switched launcher.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        rootViewModel.refreshSupport()
    }

    // A `when` over a **precondition, not over a screen** (FR-012). None of the three outcomes is
    // somewhere the user navigates to, and none belongs in a back stack — which is why this is not
    // a destination, and why it is not the construct the graph below replaced.
    when (support) {
        // Not an answer. Rendering one would flash the wrong screen for the one binder call it
        // takes to get a real one.
        PinSupport.Unknown -> Unit

        // Takes over the whole app, so no destination is reachable by any route. The back stack is
        // left untouched rather than cleared: if support returns, so does the user's place.
        PinSupport.Unsupported -> PinUnsupportedScreen(
            // The same evaluation `ON_START` runs, so button and auto-recheck cannot drift (U5).
            onRecheck = rootViewModel::refreshSupport,
            modifier = modifier,
        )

        PinSupport.Supported -> NavHost(
            navController = navController,
            startDestination = Home,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() },
        ) {
            composable<Home> {
                val locksViewModel: LocksViewModel = hiltViewModel()
                val locksState by locksViewModel.uiState.collectAsStateWithLifecycle()

                // This entry's lifecycle, not the Activity's, so the re-read also fires when the
                // flow pops back here — which is what replaces the explicit wait the flow used to
                // do before it returned (research R9).
                //
                // `ON_RESUME`, not `ON_START`: the launcher's pin dialog pauses without stopping
                // the app, so `ON_START` does not fire when it closes and a newly accepted lock
                // would not appear (N8).
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    locksViewModel.refresh()
                }

                when {
                    // N2, FR-019a: intro vs. Locks is **derived from the lock list, never
                    // stored**. There is no "has been introduced" flag, because that is the same
                    // question as "any locks?".
                    //
                    // First read only. A flashed intro on a device with ten locks is worse than a
                    // blank frame; `loaded` latches, so no later refresh can blank this.
                    !locksState.loaded -> Unit

                    locksState.showsIntro -> IntroScreen(
                        onStart = { navController.navigate(AppList) },
                        modifier = modifier,
                    )

                    else -> LocksScreen(
                        state = locksState,
                        icons = locksViewModel.icons,
                        onNewLock = { navController.navigate(AppList) },
                        // No re-read of the store: `LocksViewModel.refresh()` already resolved
                        // every row's delay and treatment on `ON_RESUME` (N6). Only available rows
                        // call this — `LocksScreen` attaches no click modifier to the others (K3).
                        onEdit = { packageName ->
                            val lock = locksState.locks.firstOrNull {
                                it.packageName == packageName
                            } ?: return@LocksScreen
                            navController.navigate(DelayConfig(lock.packageName))
                        },
                        // FR-021: an explanation, not a confirmation — SlowLock cannot remove a
                        // lock, so there is nothing for the root to do. Removal happens on the
                        // home screen and the list catches up on the next `ON_RESUME`.
                        onExplainRemoval = locksViewModel::onExplainRemoval,
                        onDismissExplanation = locksViewModel::onDismissExplanation,
                        modifier = modifier,
                    )
                }
            }

            composable<AppList> {
                AppListScreen(
                    onAppSelected = { packageName ->
                        navController.navigate(DelayConfig(packageName))
                    },
                    onBack = { navController.popBackStack() },
                    modifier = modifier,
                )
            }

            composable<DelayConfig> { entry ->
                val route = entry.toRoute<DelayConfig>()

                DelayConfigScreen(
                    packageName = route.packageName,
                    // The delay and the treatment come off the holder, which is the only thing
                    // that read them (FR-020).
                    onNext = { delaySeconds, treatment ->
                        navController.navigate(
                            ShortcutConfig(
                                packageName = route.packageName,
                                delaySeconds = delaySeconds,
                                treatment = treatment,
                            ),
                        )
                    },
                    onBack = { navController.popBackStack() },
                    modifier = modifier,
                )
            }

            composable<ShortcutConfig> { entry ->
                val route = entry.toRoute<ShortcutConfig>()

                ShortcutConfigScreen(
                    packageName = route.packageName,
                    delaySeconds = route.delaySeconds,
                    onBack = { navController.popBackStack() },
                    // Everything above `Home` goes, so back from there cannot re-enter the flow
                    // that was just finished (G9). The lock list catches up on the `ON_RESUME`
                    // this pop delivers to the `Home` entry.
                    onCreated = { navController.popBackStack<Home>(inclusive = false) },
                    modifier = modifier,
                )
            }
        }
    }
}
