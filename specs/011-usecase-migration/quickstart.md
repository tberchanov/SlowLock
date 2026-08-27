# Quickstart: Use Cases Hold the Logic

**Plan**: [plan.md](./plan.md) · **Contracts**: [use-cases.md](./contracts/use-cases.md) ·
[layer-boundaries.md](./contracts/layer-boundaries.md)

How to run the four stages and the five gates. Nothing here drives a device — the constitution
prohibits it, and the manual cases at the end are the maintainer's to run.

---

## Commands

```bash
./gradlew test            # the whole JVM suite; the gate that runs after every stage
./gradlew assembleDebug   # the build gate
./gradlew testDebugUnitTest --tests '*UseCaseTest'   # just the new seams
```

---

## Stages

Each stage leaves the app building, the suite green, and the app behaving identically. They map to
the spec's three user stories plus a closing pass.

### Stage 1 — Repositories become read-write (User Story 1, P1)

1. Add `@OwnPackageName` to `core/domain/Dispatchers.kt`; add `CurrentLocale` to `core/domain` and
   `CurrentLocaleSource` to `core/data`; bind both in `CoreDataModule` (R9).
2. Add `LoadInstalledAppsUseCase`. Strip `InstalledAppsSource.load()` back to the enumeration and
   delete its `currentLocale()`. Point `AppListViewModel` at the use case.
3. Replace `LockOrderRepository.deriveOrder` with `saveOrder`. Add `LoadLocksUseCase`. Point
   `LocksViewModel.refresh()` at it.
**Stop here and the app is already better**: two repositories hold no rule, and the two clearest
defects the amendment named are gone.

`ShortcutPinner`'s two repository reads (FR-009) are **not** in this stage, even though they are a
repository-layer defect. Its only caller is `ShortcutConfigViewModel.create()`, so changing
`requestPin`'s signature without `CreateLockUseCase` would leave the tree not compiling — and every
stage must end building and green. It moves as step 6.

### Stage 2 — Holders stop deciding (User Story 2, P2)

5. `WaitDecisionUseCase`, then `WaitViewModel.run()` reduced to handle reads, the call, and the
   `delay()`.
6. `CreateLockUseCase` (taking `pinWhenSupported` out of `ShortcutPinner.kt` with it), then
   `ShortcutPinRepository.requestPin` changed to take the icon and return `Unit`, then
   `ShortcutPinner` stripped of `icons` and `support`, then `ShortcutConfigViewModel.create()`
   reduced to the call and the result mapping. **One atomic step** — the signature and its only
   caller change together (R1).
7. `LoadDelayConfigUseCase`, then `DelayConfigViewModel.start()`'s branch replaced by the call.

### Stage 3 — The filter leaves the state class (User Story 3, P3)

8. `FilterAppsUseCase`. Introduce `AppListInputs`; make `AppListViewModel.uiState` a derived
   `StateFlow` (R5). Remove `visibleApps`'s getter and add it as a constructor parameter. Move
   `InstalledAppTest`'s four `visibleApps` cases to `FilterAppsUseCaseTest`.

### Stage 4 — Comments, tests, record

9. Correct or delete every comment the earlier stages made false (FR-021 — the plan lists eighteen
   sites).
10. Move and split the tests per R11; add the six use case tests.
11. Re-run the Constitution Check and record what actually shipped in plan.md.

---

## Gates

Run in order. A gate that fails stops the stage rather than being noted for later.

### Gate 1 — No rule survives in a `data` file (B1, B3, B4, B5)

Read every file under `**/data/**`. For each, answer: what source does this serve, and does anything
here decide something a requirement states?

```bash
# A repository holding another repository — must return nothing.
grep -rn "Repository," app/src/main/java/com/slowlock/*/data app/src/main/java/com/slowlock/feature/*/data

# Collection operations in the data layer — every hit must be justified as decoding under B2.
# The project's own pure-function names are in the pattern: a plain `filter|sortedBy` regex misses
# `excludeSelf`, `dedupeByPackage` and `deriveLocks`, which are three of the four things moving.
grep -rnE "\.(filter|filterNot|sortedBy|sortedWith|distinct|groupBy|minOf|mapTo|associate|excludeSelf|dedupeByPackage)|deriveLocks|assembleLocks" \
  app/src/main/java/com/slowlock/core/data app/src/main/java/com/slowlock/feature/*/data
```

Run against the current tree this returns thirteen lines. **Exactly three may survive**, and they
are the three that read one source's own answer:

| Survivor | Why it is not a rule |
|---|---|
| `AppTargetSource` — `minOfOrNull` in the label lambda | Picks the lowest-labelled launcher activity: how `LauncherApps` is read for one package. |
| `AppIconCache` — `mapTo` in `sweep` | Builds the keep-set for a file walk over its own cache directory. |
| `PinnedShortcutsSource` — `mapTo` | Turns `ShortcutInfo`s into their IDs: decoding one call's answer. |

The other ten are the `excludeSelf`/`dedupeByPackage`/`sortedByLabel` chain and its imports in
`InstalledAppsSource`, the `deriveLocks` call and import in `LockOrderStore`, and two KDoc mentions
that Gate 5 removes.

### Gate 2 — No rule survives in a holder or a UI state class (B9, B10, B11)

Read the six holders and the four UI state classes. A holder's method body should be calls,
`_uiState.update`, and nothing that answers a question a requirement asked.

```bash
# The domain must stay clean of presentation and platform types.
grep -rn "^import android\." app/src/main/java/com/slowlock/*/domain app/src/main/java/com/slowlock/feature/*/domain
grep -rn "SavedStateHandle\|ViewModel\|androidx.compose.runtime" \
  app/src/main/java/com/slowlock/*/domain app/src/main/java/com/slowlock/feature/*/domain

# No use case may name a dispatcher (B8). The trailing filter drops KDoc and comment lines —
# without it, `Lock.kt`'s explanation of why it has no `withContext` is a false positive.
grep -rnE "withContext|Dispatchers\.|flowOn" \
  app/src/main/java/com/slowlock/core/domain app/src/main/java/com/slowlock/feature/*/domain \
  | grep -vE ":[0-9]+: *(\*|//)"
```

All four must return nothing, and all four already do today except where this feature adds code —
so any hit is something this feature introduced. `ImageBitmap` in `domain` is expected and
permitted (B6).

### Gate 3 — No use case only forwards (B12, SC-006)

Read the six `invoke` bodies. Each must contain at least one branch, combination or transformation
beyond a single repository call. A body that is one call and a return is the abstraction Principle V
inlines — delete it and let the holder call the repository.

### Gate 4 — Every new test fails against a wrong implementation (FR-019, U-obligations)

For each obligation marked **(new seam)** in [contracts/use-cases.md](./contracts/use-cases.md),
invert the branch it covers, run the suite, and confirm **exactly one** case turns red. Restore.
A branch inversion that turns nothing red means the obligation is unasserted; one that turns five
cases red means the cases are restating the implementation rather than driving behaviour.

The six to mutate: U2 (cache the locale), U9 (always write), U13 (recompute the deadline), U17
(pin before saving), U18 (pin on `Unknown` support), U19 (prefer the stored delay).

### Gate 5 — No comment is left false (FR-021, Principle VIII)

```bash
grep -rn "decidable\|only the wiring\|derived in\|handed in as a\|re-collates\|deriveOrder" \
  app/src/main/java/com/slowlock
```

Every hit is either corrected in the change that falsified it, or deleted. A stale comment is worse
than none, because it is believed.

---

## Manual test plan

Not automatable — the constitution prohibits an agent driving the device, and every case below
depends on a real launcher. Each is a parity check: the expected result is *what the current build
does*, so run the case before the change if the answer is not already known.

| # | Requirement | Case | Expected |
|---|---|---|---|
| M1 | FR-001, U1 | Open the app list on a device with apps exposing several launcher activities | Each app once, SlowLock absent, same alphabetical order as before |
| M2 | FR-008, U4–U6 | Type a mid-word fragment, then clear it | Substring matches appear; clearing restores the full list in its original order |
| M3 | FR-008 | Type a query matching nothing | The no-results state, distinct from the empty-list state |
| M4 | FR-001, U2 | Change the device language, return to the app list | The list re-collates under the new locale |
| M5 | FR-002, U8 | Pin a lock, return to the Locks screen | The new lock appears, appended after the existing ones |
| M6 | FR-004, U10 | Uninstall a locked app, return to the Locks screen | Its row stays in position with no label and its saved delay |
| M7 | FR-003, U7 | Reboot and open the app before unlocking, then unlock | No lock is lost |
| M8 | FR-006, U16, U17 | Configure a delay and treatment, tap Create, accept the launcher dialog | The icon appears; tapping it waits the delay just chosen |
| M9 | FR-006, U16 | Uninstall the target while the icon step sits open, then tap Create | The unavailable message, no write, no pin request |
| M10 | FR-005, U13 | Start a wait, kill the process mid-wait, reopen the shortcut | The hand-off happens at the original deadline, not one delay later |
| M11 | FR-005, U12 | Tap a shortcut whose target was uninstalled | The unavailable outcome, with no wait served first |
| M12 | FR-007, U19 | Edit a delay, kill the process, reopen the delay screen | The edited value, not the saved one |
| M13 | FR-015 | Upgrade over an existing install | Every lock, delay and treatment exactly as left |
| M14 | FR-014 | Rotate on each screen | No state lost, no reload flash, no restarted wait |
