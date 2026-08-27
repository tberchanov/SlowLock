# Phase 1 Data Model: Navigation Adoption

**Date**: 2026-08-27 | **Plan**: [plan.md](./plan.md) | **Research**: [research.md](./research.md)

What this feature adds, what it deletes, and where every piece of state ends up living. No
persisted value changes; nothing here reaches disk.

---

## 1. Routes

Four `@Serializable` declarations in `com.slowlock.Routes.kt` (research R7). These replace the
`Stage` sealed interface, its `listSaver`, and the `Origin` enum.

| Route | Arguments | Replaces |
|---|---|---|
| `Home` | none | `Stage.Home` |
| `AppList` | none | `Stage.List` |
| `DelayConfig` | `packageName: String` | `Stage.Delay(packageName, seconds, treatment, origin)` |
| `ShortcutConfig` | `packageName: String`, `delaySeconds: Int`, `treatment: IconTreatment` | `Stage.Shortcut(packageName, seconds, treatment, origin)` |

**`DelayConfig` carries only the package name.** The delay and the treatment it used to carry were
loaded by the root before it navigated; under D5 the destination's own holder loads them. The
saved-configuration read has exactly one caller and one shape on both routes in.

**`Origin` has no successor.** It existed to answer "where does back from the delay step go", and
the back stack answers that: whichever entry is beneath this one.

**`IconTreatment` crosses as a route argument** by name, the same token `DelayConfigStore` persists.
It is an enum in `core/domain` and needs no bespoke encoding.

---

## 2. State holders and their scope

| Holder | Scope after this feature | Owns | Change |
|---|---|---|---|
| `RootViewModel` | **Activity** — deliberate, commented (FR-016) | the launcher's pin support | loses `targets` and `icons`; loses `configFor()` |
| `LocksViewModel` | `Home` entry | the lock list, the removal explanation | unchanged but for its `refresh()` KDoc |
| `AppListViewModel` | `AppList` entry | the installed list, the query, the one-shot message | unchanged in code; its saved query now dies with the entry |
| `DelayConfigViewModel` | `DelayConfig` entry | **new** — the delay being edited, the target, the icon, the saved treatment | new file |
| `ShortcutConfigViewModel` | `ShortcutConfig` entry | the target, the icon, the pin, **and now the treatment selection** | gains the treatment |
| `WaitViewModel` | `ShortcutLaunchActivity` | the wait | untouched — outside the graph (research R14) |

`RootViewModel` is the only holder not scoped to an entry, and the only state in the app that
outlives every screen. It sits above the graph because pin support is a whole-app precondition, not
a screen's state — see [contracts/state-scope.md](./contracts/state-scope.md) and the plan's
Complexity Tracking.

---

## 3. `DelayConfigViewModel` — the new holder

```
feature/delay/ui/DelayConfigViewModel.kt      # holder + its UI state, as ShortcutConfigViewModel does
```

Constructor: `AppTargetRepository`, `AppIconRepository`, `DelayConfigRepository`,
`SavedStateHandle`. All four already exist and are already bound.

**What it owns**

| Value | Source on first open | Kept across rotation | Kept across process death |
|---|---|---|---|
| `delaySeconds` | `DelayConfigRepository.load(packageName)` | holder survives | saved-state handle |
| `treatment` | the same load; handed to `ShortcutConfig` as a route argument on Next | holder survives | not needed — re-read is identical |
| `target` | `AppTargetRepository.resolve(packageName)` | holder survives | re-resolved |
| `icon` | `AppIconRepository.icon(...)` | holder survives | re-loaded; never enters saved state |
| `loaded` | false until the configuration read returns | — | — |

**The one branch (research R8)**: the disk read happens only when the handle holds no delay. A
restored handle wins over disk, or an edit made before the process died is silently replaced by the
stale saved value.

**`loaded` is why FR-002(c) is a difference and not a defect**: the readout is withheld until the
read returns, rather than showing the default and correcting itself. The screen already withholds
the app pill while the target resolves, so this is the pattern already on the screen.

---

## 4. `ShortcutConfigViewModel` — the treatment moves in

The selection leaves `rememberSaveable` in `ShortcutConfigScreen` and becomes holder state seeded
from the route argument, persisted through the holder's saved-state handle. Finding F-05's four
obligations are then met by scope rather than by placement:

| Event | Required | Delivered by |
|---|---|---|
| Rotation | keep the choice | the holder survives the configuration change |
| Process death on the screen | keep the choice | the saved-state handle |
| Back out, re-enter the same app | discard the choice | the entry is popped and the holder cleared |
| Configure a different app next | discard the choice | a different visit is a different entry |

`initialTreatment` disappears from the screen's parameter list; the holder reads it from the route.

---

## 5. What is deleted

| Symbol | File | Why |
|---|---|---|
| `Stage` (sealed interface, 4 members) | `SlowLockRoot.kt` | FR-008 — the prohibited construct |
| `StageSaver` (`listSaver`) and its four tags | `SlowLockRoot.kt` | a hand-written back stack serialiser |
| `Origin` (enum) and `treatmentNamed` / `originNamed` | `SlowLockRoot.kt` | the back stack answers it |
| `rememberSaveableStateHolder()` and `HOME_KEY` / `LIST_KEY` / `DELAY_KEY` / `CONFIG_KEY` | `SlowLockRoot.kt` | per-entry retention is the library's |
| `returnHome()`, `leaveDelay()` | `SlowLockRoot.kt` | hand-managed pops |
| `RootViewModel.targets`, `.icons`, `.configFor()` | `RootViewModel.kt` | FR-020 — closes finding F-06 |
| `BackHandler { onBack() }` ×3 | app list, delay, shortcut screens | FR-010 |
| `targets` / `icons` parameters | `DelayConfigScreen.kt` | the screen has a holder now |
| `initialTreatment` parameter, `var treatment by rememberSaveable` | `ShortcutConfigScreen.kt` | FR-018 — closes finding F-05 |
| `locksViewModel.refresh().join()` before returning home | `SlowLockRoot.kt` | research R9 |

---

## 6. What is added

| Item | Where |
|---|---|
| `Home`, `AppList`, `DelayConfig`, `ShortcutConfig` route types | `com.slowlock.Routes.kt` (new) |
| `NavHost` with four destinations and no transitions (R10) | `SlowLockRoot.kt` |
| `DelayConfigViewModel` + `DelayConfigUiState` | `feature/delay/ui/DelayConfigViewModel.kt` (new) |
| `navigation-compose`, the serialization plugin, `kotlinx-serialization-core` | `gradle/libs.versions.toml`, `app/build.gradle.kts` |
| `DelayConfigViewModelTest`, `ShortcutConfigViewModelTest` — the R8 branch only | `src/test/.../feature/delay/ui/`, `.../feature/shortcut/ui/` |

Package shape is unchanged. No file moves, no capability gains or loses a layer, and the test tree
still mirrors main package for package.

---

## 7. Unchanged, and load-bearing

- Every persisted value (research R14). Nothing this feature touches reaches disk.
- The intro-versus-locks choice stays derived from the lock list inside the `Home` destination.
  It does not become a destination and no "has been introduced" flag appears (FR-011).
- The pin-support gate stays above the graph (FR-012).
- `ShortcutLaunchActivity`, `WaitViewModel`, `WaitScreen`: outside the graph entirely.
- Every repository interface, dispatcher qualifier, Hilt module and one-shot channel from 009.
