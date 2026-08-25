# Implementation Plan: Locks Home & First Run (Phase 2)

**Branch**: `005-locks-and-first-run` | **Date**: 2026-08-24 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-locks-and-first-run/spec.md`

> **Branch note**: this plan was generated while the working tree was still on
> `004-visual-redesign`; `.specify/feature.json` already points at
> `specs/005-locks-and-first-run`. Task T003 cuts the branch this header names.

## Summary

SlowLock stops opening on a list of every installed app and starts opening on **the locks the user
has made** — or, when there are none, on a single screen that says what the app is for. Both are
the same root state: the intro is the Locks screen's empty state, which is what removes any need
for a persisted "has been introduced" flag.

The technical shape is small and almost entirely additive. One new durable record — an ordered list
of package names in its own `SharedPreferences` file, `slowlock.locks` — answers the question 003
and 004 both deferred: *what is a lock, once its icon has left the app's sight?* A lock exists from
the moment "Add to home screen" is tapped, and stops existing only when the user removes it here.
Its delay and its treatment are **not** copied into that record; they are read from the existing
configuration store, so the two cannot disagree.

Everything else follows: `SlowLockRoot` gains a `Home` stage and an `Origin` on the two flow
stages, `ScreenHeader` gains the `step: Int?` parameter feature 004 named and deferred, and a new
`com.slowlock.locks` package holds the two screens and their view model. Nothing about the wait,
the pinned shortcut, the configuration store, or the eleven colour tokens changes.

## Technical Context

**Language/Version**: Kotlin, JVM target 11

**Primary Dependencies**: Jetpack Compose (BOM-managed), Material 3, `lifecycle-viewmodel-compose`,
`lifecycle-runtime-compose` — all already present. **No new dependency** (FR-039, Constitution II).

**Storage**: `SharedPreferences`. Existing `slowlock.delay-config` untouched; new `slowlock.locks`
holding one ordered key (research R1, `contracts/lock-store.md`).

**Testing**: JVM unit tests only — `./gradlew test`. Instrumented suites are forbidden by the
constitution. Manual verification against a written, numbered, requirement-traceable test plan.

**Target Platform**: Android, `minSdk 33`, `targetSdk`/`compileSdk` 37, single `:app` module

**Project Type**: Mobile app (Android, single module)

**Performance Goals**: Locks screen interactive before icons finish resolving (FR-015). Cost at rest
unchanged: no service, no polling, no wake lock (SC-013).

**Constraints**: Every disk read/write, package lookup and icon rasterization off the main thread
(FR-040). Palette closed at eleven (FR-033). Light-only (FR-037). Every user-visible string a
resource (FR-038). No new permission (FR-039).

**Scale/Scope**: Tens of locks. Two new screens, one new persisted key, one grown root state, seven
new source files, two new test files.

**Resolved unknowns**: all. See [research.md](./research.md) — R1 (where the list lives), R3 (when a
lock is recorded), R4 (intro without a flag), R5 (how rows resolve), R6 (removal without a long
press), R7 (the step counter). **R8 records the one limitation**: the canvas file is not in the
repository and no Claude Design tool was reachable from this session, so the two new screens'
metrics are derived from 004's frozen tokens rather than measured off the artboards. SC-007 is the
maintainer's side-by-side check and is the gate on that.

## Constitution Check

*Constitution v1.1.0. Evaluated before Phase 0 and re-checked after Phase 1 — both PASS.*

| Principle | Verdict | Evidence |
|---|---|---|
| **I. Cooperative User, Not Adversary** | **PASS** | The canvas subtitle "ON YOUR HOME SCREEN" is deliberately not shipped (FR-011) because the app cannot verify it. The removal confirmation states plainly that the icon stays and still works (FR-022, K4) rather than pretending SlowLock can reach it — the accepted-limitation stance, written into user-visible copy. The intro states that nothing is blocked and nothing is counted (FR-018). Nothing is enforced; nothing is closed. |
| **II. Simplicity First (YAGNI)** | **PASS** | No dependency, no module, no DI, no persistence engine, no navigation library. One `SharedPreferences` key. The lock row stays in its screen's file because only one screen needs it (U5). Every deferred item in the spec's Out of Scope stays deferred. |
| **III. Permission & Policy Minimalism** | **PASS** | No new permission. Enumeration is unchanged — this feature reads packages it already recorded, through `resolveShortcutTarget`, which the existing `<queries>` declaration already covers. |
| **IV. Platform-Idiomatic Android** | **PASS** | Compose + M3, no XML layouts. `LockStore` suspends on `Dispatchers.IO`; label resolution and icon rasterization stay off the main thread (FR-040). `resolveShortcutTarget` returning null is handled at the call site as an ordinary row state (FR-020), not assumed away. `isRequestPinShortcutSupported()` still gates the whole root (N1). No service, no polling, no wake lock, no `FLAG_KEEP_SCREEN_ON` on either new screen. |
| **V. Stable Identifiers** | **PASS** | The package name is the only persisted identifier (FR-002, L2). No label, activity name or `ComponentName` is stored or matched. Labels are resolved fresh on every read and are display-only (SC-006). Icons keep `AppIconCache`'s `packageName` + `versionCode` key. |

**Additional standards** — fixed stack unchanged; no backend, no network, no analytics; the feature
sits inside the v1 scope boundary (picking, configuring, pinning, waiting) and adds only a home for
what was already made. Non-goals respected: the permanent Out of Scope bans statistics outright,
and the intro screen says so to the user.

**Development workflow** — `/speckit-specify` → `/speckit-clarify` → `/speckit-plan` is where this
sits. The build gate (`assembleDebug` + `test`) applies. Unit-test obligations: the frozen file
name, key and separator are each asserted against a literal (`LockListTest`), and the null-target
path is covered by `LocksViewModelTest` through injected lambdas. **No instrumented suite is added
and no agent drives the device**; the manual test plan is a tasks-phase deliverable and is required
before the feature is complete.

**Complexity Tracking**: empty. No deviation to justify.

## Project Structure

### Documentation (this feature)

```text
specs/005-locks-and-first-run/
├── plan.md                      # This file
├── research.md                  # Phase 0 — R1..R9
├── data-model.md                # Phase 1
├── quickstart.md                # Phase 1
├── contracts/
│   ├── lock-store.md            # The new durable record — frozen on merge
│   ├── locks-screen.md          # The two new screens + ScreenHeader's `step`
│   └── root-navigation.md       # Stages, transitions, and the touchable-files list
├── checklists/requirements.md   # Existing — all items pass
├── manual-test-plan.md          # Tasks-phase deliverable (constitution)
└── tasks.md                     # /speckit-tasks output — NOT created here
```

### Source Code (repository root)

```text
app/src/main/java/com/slowlock/
├── SlowLockRoot.kt              # MODIFIED — Stage.Home, Origin, back handling, view-model wiring
├── MainActivity.kt              # unchanged
├── locks/                       # NEW PACKAGE
│   ├── LockList.kt              #   pure: frozen constants, encode/decode, withLock/withoutLock
│   ├── LockStore.kt             #   wiring only: SharedPreferences + Dispatchers.IO
│   ├── Lock.kt                  #   one assembled row
│   ├── LocksUiState.kt          #   loaded / locks / pendingRemoval
│   ├── LocksViewModel.kt        #   loads the list, configs and targets; injected lookups
│   ├── LocksScreen.kt           #   the Locks screen and its row
│   └── IntroScreen.kt           #   the first-run screen
├── apps/AppListScreen.kt        # MODIFIED — onBack + step = 1 only
├── delay/DelayConfigScreen.kt   # MODIFIED — step = 2 only
├── shortcut/ShortcutConfigScreen.kt  # MODIFIED — step = 3, LockStore.add inside create()
├── ui/components/ScreenHeader.kt     # MODIFIED — step: Int?
└── ui/theme/                    # unchanged — the palette stays closed at eleven

app/src/main/res/values/strings.xml   # MODIFIED — new strings only

app/src/test/java/com/slowlock/locks/ # NEW
├── LockListTest.kt
└── LocksViewModelTest.kt
```

**Structure Decision**: single `:app` module, unchanged (Constitution II). The new code forms one
package, `com.slowlock.locks`, alongside the existing `apps`, `delay` and `shortcut` packages —
named for the user's noun, as 004's terminology decision requires. `SlowLockRoot` stays at the top
level because it belongs to none of the four packages; it is what connects them.

## Phase 2 — What `/speckit-tasks` will order

Recorded here so the task breakdown has a spine, not executed by this command.

1. **US1 + US2 foundation** — `LockList.kt` and `LockListTest` first (the frozen values, test-first
   is where it is cheapest), then `LockStore`, then the `add` call in `create()`.
2. **US1** — `IntroScreen`, `Stage.Home`, the root's derived intro/Locks branch. Ships alone.
3. **US2** — `LocksViewModel`, `LocksUiState`, `Lock`, `LocksScreen` and its row, `LocksViewModelTest`.
4. **US3** — `ScreenHeader`'s `step`, the three call sites, the app list's `onBack`, `BackHandler`
   per stage.
5. **US4** — `Origin` on the flow stages, the lock-row tap, the delay screen's back branch.
6. **US5** — the long press, the custom accessibility action, the confirmation dialog and its copy.
7. **Cross-cutting** — strings, `manual-test-plan.md`, and the build gate.

## Complexity Tracking

None. No constitutional deviation is claimed by this plan.
