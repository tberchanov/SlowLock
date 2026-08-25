# Contract: Root Navigation

**Feature**: `005-locks-and-first-run` | **File**: `com.slowlock.SlowLockRoot`

`SlowLockRoot` is the one file `004/contracts/screen-inventory.md` §S6 forbade touching, on the
grounds that "no stage is added — Phase 2's work". This is Phase 2. Everything below is the shape
that work must take.

Still no navigation library. Four stages, one transition each way, and `navigation-compose` solves
problems this app does not have (Constitution II, 003 research R9).

---

## N1 — Pin support still takes the whole root, ahead of everything

Unchanged from 003 U4 / 002 FR-029, and restated because this feature adds two screens it must
also win against: when `pinSupport()` answers `Unsupported`, `PinUnsupportedScreen` renders **in
place of** the entire `when (stage)` — the intro and Locks included (FR-025).

`PinSupport.Unknown` still renders nothing. The stage is still left untouched rather than cleared,
so support returning puts the user back where they were.

## N2 — `Stage.Home` is the initial stage, and it renders two screens

`Stage.Home` replaces `Stage.List` as the initial value and as the target of every "return to the
root" transition. Which screen it shows is **derived, never stored** (FR-019a):

| `LocksUiState` | Rendered |
|---|---|
| `!loaded` | Nothing — the **first** read only. One binder-and-disk read, and a flashed intro is worse than a blank frame (research R4). A later refresh never blanks the screen (FR-016) |
| `locks.isEmpty()` | `IntroScreen` (FR-017) |
| otherwise | `LocksScreen` (FR-009) |

Removing the last lock therefore shows the intro with no code path of its own (US5 scenario 5,
US2 scenario 6). There is no `Stage.Intro`, and adding one is a defect.

## N3 — Transitions

| From | Action | To |
|---|---|---|
| Home (intro) | "Set up a lock" | `List` |
| Home (locks) | "+ New lock" | `List` |
| Home (locks) | tap an **available** row | `Delay(pkg, saved, saved, origin = Home)` — after the store read completes (N6) |
| Home (locks) | tap an unavailable row | nothing (K3) |
| Home | system back | leave the app (FR-031) |
| `List` | back control **or** system back | `Home` (FR-028, FR-030) |
| `List` | row tap | `Delay(pkg, …, origin = List)` |
| `Delay` | back **or** system back | `List` if `origin == List`, else `Home` (FR-023, US4 scenario 3) |
| `Delay` | "Choose the icon" | `Shortcut(pkg, seconds, treatment, origin)` |
| `Shortcut` | back **or** system back | `Delay(pkg, seconds, treatment, origin)` |
| `Shortcut` | created | `Home` |

**System back must do exactly what the on-screen control does, on every screen that has one**
(FR-030). That means a `BackHandler` per stage that calls the same lambda the control does — not a
parallel implementation, and not the default activity behaviour.

## N4 — The state holder rules survive unchanged

`LIST_KEY` is retained across the round trip; `DELAY_KEY` and `CONFIG_KEY` are dropped on every
exit to the root and on a back out of the icon step. This is what keeps the app list's scroll
position and search query alive (003 FR-011, FR-032) and stops an abandoned treatment reappearing
for a different app.

A `HOME_KEY` entry joins them and is **retained**, for the Locks list's scroll position, for the
same reason `LIST_KEY` is.

## N5 — The delay value still lives on the stage

Obligation N2 of 003, restated because `Origin` now rides beside it: the seconds and the treatment
the user chose on the way through live in `Stage`, not in the screens. A back from the icon step
returns the value chosen, not the one on disk (003 FR-014). `DelayConfigScreen` still never touches
the store; `ShortcutConfigScreen` still only writes.

## N6 — The store read still finishes before the transition

Both entries into `Stage.Delay` — the app-list row tap and the **lock row tap** — read
`DelayConfigStore` first and navigate with the values in hand (003 N1, D12, D13). There is no frame
in which the delay screen shows a default and then corrects itself.

For a lock row the values are already in `LocksUiState`, so the read is already done: the
transition may use them directly rather than re-reading. Either is compliant; re-reading is not.

## N7 — Abandoning an edit writes nothing

`Stage` is transient. Nothing on the delay or icon screen writes; the only writes are in
`create()`. An edit abandoned with back therefore leaves both the configuration and the lock record
exactly as they were (FR-023a), with no rollback and no partial-write path to get wrong.

## N8 — The lock list is refreshed on `ON_RESUME`, and on completing the flow

`LocksViewModel.refresh()` on `Lifecycle.Event.ON_RESUME`. `pinSupport` keeps its own `ON_START`
effect (FR-028); the two are no longer the same hook.

**`ON_RESUME`, not `ON_START`, and the pin dialog is why.** The launcher's confirmation is a dialog
over this activity: it pauses the app without stopping it, so `ON_START` does not fire when it
closes. Since a lock is created by the pin itself (FR-003a), a list refreshed only on `ON_START`
would not show the lock the user just accepted until they next backgrounded the app — which is the
bug this replaced. `ON_RESUME` also covers everything `ON_START` did: first launch, return to the
foreground, return from an uninstall, a language change (FR-016, SC-006).

**Completing the flow still needs its own call**, for the case with *no* dialog: re-pinning an app
that already has a lock succeeds silently, nothing pauses the activity, and no lifecycle event
fires — so an edit's new delay and treatment would land on a row still showing the old ones. That
refresh **completes before the transition**, as the app-list tap's store read does (N1, N6,
research R3).

These two are the only call sites, plus `onConfirmRemove`'s own re-read. No polling, no observer,
no service — the cost at rest stays zero (SC-013, Constitution IV).

## N9 — `StageSaver` grows, keeping its rules

The discriminant is written explicitly and read back through an exhaustive `when`, so a fifth stage
added without a line here fails to compile. `IconTreatment` and `Origin` are saved by `Enum.name`
and sanitise to their first entry on an unrecognised token. **An unrecognised or absent
discriminant restores as `Home`**, not `List`.

## N10 — Files this feature is allowed to touch

Everything else is out of scope, and this list is a review aid.

| File | Change |
|---|---|
| `SlowLockRoot.kt` | Stages, transitions, back handling, `LocksViewModel` wiring |
| `locks/` (new) | `LockList.kt`, `LockStore.kt`, `Lock.kt`, `LocksUiState.kt`, `LocksViewModel.kt`, `LocksScreen.kt`, `IntroScreen.kt` |
| `ui/components/ScreenHeader.kt` | `step: Int?` (K5) |
| `apps/AppListScreen.kt` | `onBack` and `step = 1` **only** — no change to the view model, the query, the rows, or the snackbar |
| `delay/DelayConfigScreen.kt` | `step = 2`, and the `Scaffold` wrapper that supplies its window insets (N11) |
| `shortcut/ShortcutConfigScreen.kt` | `step = 3` only — 005 briefly added a `LockStore` call inside `create()` and then removed it again (FR-003a, L6) |
| `res/values/strings.xml` | New strings only; no existing value changes |
| `app/src/test/.../locks/` (new) | `LockListTest`, `LocksViewModelTest` |

**Must not appear in the diff**: `ShortcutContract.kt`, `ShortcutPinner.kt`, `ShortcutTarget.kt`,
`PinSupport.kt`, `DelayConfig.kt`, `DelayConfigStore.kt`, `WaitScreen.kt`, `WaitTiming.kt`,
`ShortcutLaunchActivity.kt`, `AppListViewModel.kt`, `InstalledAppsSource.kt`, `AppIconCache.kt`,
`ui/theme/Color.kt`, `AndroidManifest.xml`, `gradle/libs.versions.toml`, `app/build.gradle.kts`,
`res/mipmap-*`, `res/drawable/ic_launcher_*`, `res/values-night/`, and the ten existing test files.

## N11 — Every screen takes its window insets from a `Scaffold`

**Every full-screen composable in this app is rooted in a `Scaffold`, and applies the
`contentPadding` it hands back before any padding of its own.** The five are the intro, the Locks
screen, the app list, the delay step and the icon step.

```kotlin
Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
) { contentPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)          // system bars — first, and never skipped
            .padding(horizontal = SCREEN_PADDING),
    ) { … }
}
```

This is written down because it was violated. `DelayConfigScreen` was built on a bare `Column` with
`.background(...)` and no inset handling, so its `ScreenHeader` — back tile, title and step counter
alike — drew **underneath the status bar** while steps 1 and 3 looked correct. A user walking the
flow met it as one broken screen in the middle of three good ones.

Two consequences follow, and they are the reason this is a contract rather than a fixed bug:

- **`containerColor` replaces a `.background(...)` on the root modifier**, rather than joining it.
  A screen that sets both has two grounds that can disagree.
- **A one-off `windowInsetsPadding` is not an acceptable alternative.** It fixes the symptom and
  leaves the app with two patterns for the same problem, which is what let one screen drift in the
  first place. If a screen genuinely cannot use a `Scaffold`, that is a contract change, not a
  local decision.

`SCREEN_PADDING` and the header's own metrics are unaffected — insets are applied *outside* them,
so nothing about the design's spacing changes.
