package com.slowlock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.slowlock.apps.AppListScreen
import com.slowlock.shortcut.PinSupport
import com.slowlock.shortcut.PinUnsupportedScreen
import com.slowlock.shortcut.ShortcutConfigScreen
import com.slowlock.shortcut.pinSupport

/**
 * Arbitrates which screen is showing.
 *
 * It lives at the top level rather than inside `apps/` or `shortcut/` because it belongs to
 * neither — it is what connects them. Navigation is a `when` over root state with no navigation
 * library: the app has a handful of root states and one transition each way, and
 * `navigation-compose` is a dependency whose graph, routes, and argument encoding exist to solve
 * problems this app does not have (research.md R9, Constitution II).
 *
 * A row tap sets the selection, which **replaces feature 001's interim launch** (FR-001). That
 * swap needed no edit to `AppListScreen` at all — it reports a selection and stops there, which
 * is exactly what `contracts/selection-handoff.md` was written to guarantee before this consumer
 * existed.
 *
 * `rememberSaveable` so a rotation or a process death on the configuration screen returns to the
 * configuration screen rather than dumping the user back at the list.
 *
 * A [rememberSaveableStateHolder] wraps each branch, which is what makes FR-022 hold. The app
 * list and the search query already survive on their own — the list lives in `AppListViewModel`,
 * scoped to the Activity's `ViewModelStore`, and the query is mirrored into its `SavedStateHandle`
 * — but **scroll position is the exception**: `rememberLazyListState` saves through
 * `rememberSaveable`, which is discarded when `AppListScreen` leaves composition unless something
 * retains it. The holder is that something; it is the same mechanism `NavHost` uses internally
 * (research.md R9). Without it the user returns to the top of the list every time.
 *
 * Pin support gates all of the above (FR-029). It is re-read on every `ON_START` and held in a
 * plain [remember] — deliberately **not** `rememberSaveable`, because a saved answer is exactly
 * the stale one FR-028 forbids: it would be restored from the bundle of a process that died under
 * a different launcher.
 */
@Composable
fun SlowLockRoot(modifier: Modifier = Modifier) {
    var selectedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    val stateHolder = rememberSaveableStateHolder()

    val context = LocalContext.current
    var support by remember { mutableStateOf<PinSupport>(PinSupport.Unknown) }

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

        // In place of the list, not alongside it — with the whole root taken over, neither the
        // list nor the configuration screen is reachable by any route, including a selection
        // still sitting in `selectedPackage` from before the launcher changed (FR-029, SC-009).
        // That selection is left untouched rather than cleared: if support comes back, the user
        // returns to the screen they were on.
        PinSupport.Unsupported -> PinUnsupportedScreen(
            // The same evaluation `ON_START` runs, so the button and the automatic re-check
            // cannot drift apart (FR-031, U5).
            onRecheck = { support = pinSupport(context) },
            modifier = modifier,
        )

        // FR-032: no restart needed. The user switched launcher, came back, `ON_START` fired,
        // and this branch simply wins the recomposition.
        PinSupport.Supported -> when (val selected = selectedPackage) {
            null -> stateHolder.SaveableStateProvider(LIST_KEY) {
                AppListScreen(
                    onAppSelected = { selectedPackage = it },
                    modifier = modifier,
                )
            }

            else -> stateHolder.SaveableStateProvider(CONFIG_KEY) {
                ShortcutConfigScreen(
                    packageName = selected,
                    // Every exit is the same event: created, backed out, system back. The screen
                    // shows no confirmation (FR-012), so the root cannot tell them apart and must
                    // not need to.
                    //
                    // `removeState` first, and this is not incidental. The holder's whole job is
                    // to carry a branch's `rememberSaveable` state across it leaving composition
                    // — which is what the list wants and what the configuration screen must not
                    // have. Left in, the holder would restore the previous treatment the next
                    // time the screen opened, for a different app, breaking FR-006's "Original
                    // when the screen first opens". Dropping the entry on the way out is how a
                    // state holder is told a destination is being popped rather than merely
                    // hidden; the treatment still survives rotation and process death (FR-008),
                    // which never leave composition in the first place.
                    onDone = {
                        stateHolder.removeState(CONFIG_KEY)
                        selectedPackage = null
                    },
                    modifier = modifier,
                )
            }
        }
    }
}

// Distinct from any package name, which cannot contain a colon — the configuration branch is
// keyed per-root rather than per-package because its state is dropped on every exit anyway.
private const val LIST_KEY = "root:app-list"
private const val CONFIG_KEY = "root:shortcut-config"
