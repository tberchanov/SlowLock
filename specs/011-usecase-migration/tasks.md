---
description: "Task list for 011-usecase-migration"
---

# Tasks: Use Cases Hold the Logic

**Input**: Design documents from `/specs/011-usecase-migration/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: Included. FR-018, FR-019 and FR-020 mandate JVM coverage, and `contracts/use-cases.md`
states the obligations each test answers to. **Test-first is RECOMMENDED, never mandated**
(Constitution VI) — the template's "write tests FIRST and watch them fail" is advice here, not a
gate. What *is* a gate is T053: every obligation marked **(new seam)** must fail when its branch is
inverted.

**Organization**: by user story, matching the four stages in [quickstart.md](./quickstart.md).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallelizable — different files, no dependency on an incomplete task
- **[Story]**: US1 / US2 / US3, mapping to the spec's user stories
- Paths are repository-relative. Kotlin sources live under `app/src/main/java/com/slowlock/`,
  tests under `app/src/test/java/com/slowlock/` — written below as `main/…` and `test/…`.

**Constraint on every task**: no user-visible change (FR-014), no frozen persisted value touched
(FR-015), no dependency added (FR-016), no file changes package (FR-017).

---

## Phase 1: Setup (Baseline)

**Purpose**: establish the parity baseline. This feature's entire success criterion is *nothing
changed*, which is only checkable against a recorded before.

- [X] T001 Run `./gradlew assembleDebug test` and confirm both green before any edit; record the test count in the run log
- [X] T002 [P] Capture the current data-layer gate output as the baseline for Gate 1, per the grep in `specs/011-usecase-migration/quickstart.md` (expect 13 lines, of which 3 may survive)
- [X] T003 [P] Confirm `git status` is clean, so every later diff is this feature's

**Checkpoint**: baseline recorded. Any later behaviour difference is attributable.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: the two seams R9 identified. `LoadInstalledAppsUseCase` cannot exist without them —
`excludeSelf` needs the own-package name and `sortedByLabel` needs the load-time locale, and neither
is reachable from `domain`.

**⚠️ BLOCKS User Story 1.**

- [X] T004 Add the `@OwnPackageName` qualifier beside `@IoDispatcher` in `main/core/domain/Dispatchers.kt`, with a KDoc stating why it is a qualifier and not an interface (obligation D4)
- [X] T005 [P] Create `main/core/domain/CurrentLocale.kt` as `fun interface CurrentLocale { fun now(): Locale }`, mirroring `ElapsedClock`'s shape and rationale
- [X] T006 [P] Create `main/core/data/CurrentLocaleSource.kt` reading `context.resources.configuration.locales.get(0)` with a `Locale.getDefault()` fallback — the exact body being deleted from `InstalledAppsSource`
- [X] T007 In `main/core/data/CoreDataModule.kt` add `@Binds` for `CurrentLocale` and `@Provides @OwnPackageName fun ownPackageName(): String` from `context.packageName`; leave `Dispatchers.IO` and `Dispatchers.Default` named exactly once (D1)
- [X] T008 Run `./gradlew assembleDebug test` — both seams wired, nothing yet consumes them

**Checkpoint**: foundation ready. User Story 1 can begin.

---

## Phase 3: User Story 1 — Repositories read and write, and nothing else (Priority: P1) 🎯 MVP

**Goal**: no repository implementation filters, sorts, merges two sources, or decides something a
requirement states. Satisfies FR-001, FR-002, FR-003, FR-004 and FR-010.

**Independent Test**: read `InstalledAppsSource` and `LockOrderStore` — each holds a read, a write,
and the decoding between the stored form and the domain type, and nothing else. The app list and the
Locks screen behave identically (manual cases M1, M4, M5, M6, M7).

> **Note**: `ShortcutPinner`'s two repository reads are FR-009 and are *not* in this story. Its only
> caller is `ShortcutConfigViewModel.create()`, so its signature and that caller must change
> together or the tree stops compiling. It is T027–T030 in User Story 2.

### Tests for User Story 1

- [X] T009 [P] [US1] Create `test/feature/apps/domain/LoadInstalledAppsUseCaseTest.kt` covering U1 (exclude, dedupe, sort in that order), U2 (the locale is read inside `invoke`, once per call), U3 (one repository call per invocation, nothing cached)
- [X] T010 [P] [US1] Create `test/feature/locks/domain/LoadLocksUseCaseTest.kt` covering U7 (`null` pinned set returns the stored order and does **not** write), U8 (a set reconciles via `deriveLocks`), U9 (`saveOrder` only when the order changed), U10 (an unresolvable package stays a row in position), U11 (an empty set is a real answer and does empty the list)

### Implementation for User Story 1

- [X] T011 [US1] Create `main/feature/apps/domain/LoadInstalledAppsUseCase.kt` taking `InstalledAppsRepository`, `@OwnPackageName String` and `CurrentLocale`; call the existing `excludeSelf`, `dedupeByPackage`, `sortedByLabel` in that order, reading the locale inside `invoke`
- [X] T012 [US1] Strip `main/feature/apps/data/InstalledAppsSource.kt` to the enumeration: delete the three-call chain, the `currentLocale()` method and the three domain imports. Keep `VersionCodeLookup` — it memoizes one source's own reads
- [X] T013 [US1] Rewrite the obligations list in `main/feature/apps/domain/InstalledAppsRepository.kt`: `load()` now returns the raw enumeration, SlowLock included, in platform order (its KDoc states the opposite today)
- [X] T014 [US1] Point `main/feature/apps/ui/AppListViewModel.kt` at `LoadInstalledAppsUseCase` in place of `InstalledAppsRepository`; `refresh()` and the `icons.sweep` call are otherwise unchanged
- [X] T015 [US1] Update `test/feature/apps/ui/AppListViewModelTest.kt`'s `FakeInstalledApps` to the new contract, and move any case asserting exclusion, dedup or ordering to `LoadInstalledAppsUseCaseTest`
- [X] T016 [US1] Replace `deriveOrder` with `suspend fun saveOrder(order: List<String>)` in `main/feature/locks/domain/LockOrderRepository.kt`, moving the "never pass an empty set for *could not ask*" warning to where the mistake is now possible
- [X] T017 [US1] In `main/feature/locks/data/LockOrderStore.kt` replace `deriveOrder` with `saveOrder`: delete the `deriveLocks` call, the read-then-compare and the `deriveLocks` import. `loadOrder` is unchanged, and the store stays the only writer of `slowlock.locks`
- [X] T018 [US1] Create `main/feature/locks/domain/LoadLocksUseCase.kt` taking `LockOrderRepository`, `PinnedShortcutsRepository`, `DelayConfigRepository` and `AppTargetRepository`; hold the `null`-vs-set choice, the `deriveLocks` reconciliation, the write-only-if-changed rule, and the `assembleLocks` call
- [X] T019 [US1] Reduce `main/feature/locks/ui/LocksViewModel.kt`'s `refresh()` to one `LoadLocksUseCase` call and `_uiState.update { it.withLocks(...) }`; drop the four repository parameters, keep `icons` exposed for lazy rows
- [X] T020 [US1] Move the rule-asserting cases out of `test/feature/locks/ui/LocksViewModelTest.kt` into `LoadLocksUseCaseTest` — most of that file already targets `assembleLocks` and `LocksUiState` directly, per its own KDoc — and keep the latch case, which is `LocksUiState`'s
- [X] T021 [US1] Run `./gradlew assembleDebug test`

**Checkpoint**: two repositories hold no rule. The app list and the Locks screen are unchanged.
Stopping here is a coherent delivery.

---

## Phase 4: User Story 2 — A screen holder wires, it does not decide (Priority: P2)

**Goal**: no `ViewModel` holds a branch a requirement states. Satisfies FR-005, FR-006, FR-007 and
FR-009.

**Independent Test**: each affected holder's body is repository or use-case calls and
`_uiState.update`. Manual cases M8–M12 pass unchanged.

### Tests for User Story 2

- [X] T022 [P] [US2] Create `test/feature/shortcut/domain/WaitDecisionUseCaseTest.kt` covering U12 (unresolvable target returns `Unavailable` before any config read), U13 (a stored deadline is returned unchanged and the config is not consulted for it), U14 (a null stored deadline yields `deadlineFrom(anchor, delaySeconds)`), U15 (remaining is never negative)
- [X] T023 [P] [US2] Create `test/feature/shortcut/domain/CreateLockUseCaseTest.kt` covering U16 (unresolvable package writes nothing and pins nothing), U17 (**the config is written before `requestPin` is called** — order, not merely both), U18 (`requestPin` only on `Supported` and only with an icon)
- [X] T024 [P] [US2] Create `test/feature/delay/domain/LoadDelayConfigUseCaseTest.kt` covering U19 (a non-null `editedSeconds` wins; the treatment always comes from the read; the read happens on both paths)

### Implementation for User Story 2 — the wait

- [X] T025 [US2] Create `main/feature/shortcut/domain/WaitDecisionUseCase.kt` plus the `WaitDecision` sealed type (`Unavailable` | `Wait(deadlineMillis, remainingMillis)`), taking `AppTargetRepository`, `DelayConfigRepository` and `ElapsedClock`, with `invoke(target, anchorMillis, storedDeadlineMillis)`
- [X] T026 [US2] Reduce `main/feature/shortcut/ui/WaitViewModel.kt`'s `run()` to: read the handle, call the use case, write the deadline back, `delay()`, send the event. Keep every `SavedStateHandle` access, both `start()` branches (W22, W23), `waitJob` and the channel — those are the screen's lifetime, not rules

### Implementation for User Story 2 — the pin path (T027–T030 are one atomic change)

- [X] T027 [US2] Create `main/feature/shortcut/domain/CreateLockUseCase.kt` plus `CreateLockResult` (`Created(pin: PinRequestResult)` | `TargetMissing`), taking `AppTargetRepository`, `DelayConfigRepository`, `PinSupportRepository`, `AppIconRepository` and `ShortcutPinRepository`; move `pinWhenSupported` into it from `ShortcutPinner.kt`; hold re-resolve → write → gate → icon → pin in that order
- [X] T028 [US2] Change `main/feature/shortcut/domain/ShortcutPinRepository.kt`'s `requestPin` to take `icon: ImageBitmap` and return `Unit`, and rewrite its class KDoc — the "the source icon is loaded by the implementation rather than handed in" claim becomes false, while obligation O1 stays satisfied because `ImageBitmap` is Compose's type, not `android.*` (R1)
- [X] T029 [US2] Strip `main/feature/shortcut/data/ShortcutPinner.kt`: delete the `support` and `icons` constructor parameters, the `pinWhenSupported` call, the `IconUnavailable` branch and the now-moved free function. Keep `bake` and `request` — drawing and pinning are how a shortcut is written
- [X] T030 [US2] Reduce `main/feature/shortcut/ui/ShortcutConfigViewModel.kt`'s `create()` to one `CreateLockUseCase` call and the result mapping. Add the required comment for why `Created`'s pin result is deliberately unread (FR-014 preserves an existing choice; the code cannot say why). Leave `start()` alone — two logic-free reads (FR-011)

### Implementation for User Story 2 — the delay screen

- [X] T031 [US2] Create `main/feature/delay/domain/LoadDelayConfigUseCase.kt` taking `DelayConfigRepository`, with `invoke(packageName, editedSeconds)` holding the edited-wins rule
- [X] T032 [US2] Replace the `edited ?: saved.delaySeconds` branch in `main/feature/delay/ui/DelayConfigViewModel.kt`'s `start()` with the use case call. Leave the target and icon reads — single logic-free calls (FR-011)

### Test migration for User Story 2

- [X] T033 [P] [US2] Move `test/feature/shortcut/data/PinGateTest.kt` to `test/feature/shortcut/domain/PinGateTest.kt`; its `pinWhenSupported` cases go to `CreateLockUseCaseTest`, its `pinSupport` cases stay. The move also resolves a standing Principle III defect — a pure decision function declared in a `data` file
- [X] T034 [P] [US2] Move the rule-asserting cases from `test/feature/shortcut/ui/WaitViewModelTest.kt` to `WaitDecisionUseCaseTest`; keep the handle, rotation and repeat-tap cases, which are the holder's
- [X] T035 [P] [US2] Move the rule-asserting cases from `test/feature/shortcut/ui/ShortcutConfigViewModelTest.kt` to `CreateLockUseCaseTest`; update its fake `requestPin` to the new signature; keep the treatment-restore cases
- [X] T036 [P] [US2] Move the edited-wins cases from `test/feature/delay/ui/DelayConfigViewModelTest.kt` to `LoadDelayConfigUseCaseTest`; keep the state-mapping cases
- [X] T037 [US2] Run `./gradlew assembleDebug test`

**Checkpoint**: no holder decides. Five repositories and five holders are compliant; only the search
filter remains.

---

## Phase 5: User Story 3 — The search filter is a rule, not a view (Priority: P3)

**Goal**: `AppListUiState` applies no rule to the data. Satisfies FR-008 and FR-012.

**Independent Test**: `AppListUiState` holds fields and display-state booleans, and no filtering.
Manual cases M2 and M3 pass unchanged.

### Tests for User Story 3

- [X] T038 [P] [US3] Create `test/feature/apps/domain/FilterAppsUseCaseTest.kt` covering U4 (blank or whitespace query returns the input untouched), U5 (case-insensitive substring, not prefix — "tagram" matches "Instagram"), U6 (input order preserved, never re-sorted)

### Implementation for User Story 3

- [X] T039 [US3] Create `main/feature/apps/domain/FilterAppsUseCase.kt` — **not** `suspend`, since it takes no repository and touches no source. Add the required comment for that asymmetry with the other five
- [X] T040 [US3] Add `AppListInputs(isLoading, apps, query)` to `main/feature/apps/ui/AppListUiState.kt`; change `visibleApps` from a getter to a constructor parameter; keep `isEmpty`, `hasNoResults` and `isPopulated` derived from it (FR-012)
- [X] T041 [US3] In `main/feature/apps/ui/AppListViewModel.kt` hold `MutableStateFlow<AppListInputs>` privately and expose `uiState` as `.map { … filterApps(it.apps, it.query) }.stateIn(viewModelScope, SharingStarted.Eagerly, AppListUiState())` (R5). `Eagerly`, not `WhileSubscribed` — the upstream is hot and the mapping is an in-memory filter
- [X] T042 [US3] Confirm `main/feature/apps/ui/AppListScreen.kt:123` still reads `state.visibleApps` unchanged — it becomes a field rather than a getter, so no edit should be needed. If one is, the shape in T040 is wrong
- [X] T043 [US3] Move the four `visibleApps` cases (lines 87–126) from `test/feature/apps/domain/InstalledAppTest.kt` to `FilterAppsUseCaseTest`; they exercise a `ui` class from a `domain` test file today, which the move corrects. Keep the `excludeSelf`/`dedupeByPackage`/`sortedByLabel` cases
- [X] T044 [US3] Update `test/feature/apps/ui/AppListViewModelTest.kt` for the derived-state shape; assert the query still round-trips through `SavedStateHandle`
- [X] T045 [US3] Run `./gradlew assembleDebug test`

**Checkpoint**: all three stories complete. Constitution v5.0.0 is satisfied in code.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: the comment sweep, the gates, and the record. Nothing here changes behaviour.

- [X] T046 Correct or delete the eighteen comment sites listed in `plan.md` under "Comment sites made false" — each in the change that falsified it where still possible, otherwise now (FR-021)
- [X] T047 [P] Run Gate 5: `grep -rn "decidable\|only the wiring\|derived in\|handed in as a\|re-collates\|deriveOrder" app/src/main/java/com/slowlock` — every hit corrected or deleted
- [X] T048 [P] Run Gate 1 (data-layer scan) from `quickstart.md`; confirm exactly three survivors — `AppTargetSource`'s `minOfOrNull`, `AppIconCache`'s `sweep` `mapTo`, `PinnedShortcutsSource`'s `mapTo`
- [X] T049 [P] Run Gate 2 (domain purity): the four greps for `android.*`, `SavedStateHandle`/`ViewModel`/Compose runtime, and dispatcher names in `**/domain/**` must all return nothing
- [X] T050 Run Gate 3: read the six `invoke` bodies; any that is a single repository call with no branch, combination or transformation must be deleted and its caller pointed at the repository (FR-011, SC-006)
- [X] T051 [P] Verify SC-004 by inspection: "how is the app list ordered?", "what is the lock list?", "when does the wait end?", "what does the search box match?" each answered in exactly one file
- [X] T052 Run `./gradlew assembleDebug` and `./gradlew test` — the constitution's build gate (FR-022)
- [X] T053 Run Gate 4 (mutation): invert each of the six **(new seam)** branches — U2 cache the locale, U9 always write, U13 recompute the deadline, U17 pin before saving, U18 pin on `Unknown`, U19 prefer the stored delay — and confirm **exactly one** case turns red each time. Restore after each
- [X] T054 Record what actually shipped against what was anticipated in `specs/011-usecase-migration/plan.md`, as a post-implementation row set in the Constitution Check (the shape feature 010's plan uses)
- [X] T055 Hand the maintainer the manual test plan — the 14 cases M1–M14 in `quickstart.md`. **An agent MUST NOT drive the device to pre-verify any of them** (Constitution: no automated test may drive a device); state which cases need running and wait
- [ ] T056 Report the work as staged and ready. **Do not commit, push, branch or tag** — Principle VII reserves all of it to the maintainer, and this task stays unchecked

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (T001–T003)**: no dependencies
- **Foundational (T004–T008)**: needs Setup. **Blocks User Story 1** — `LoadInstalledAppsUseCase` cannot be written without both seams
- **US1 (T009–T021)**: needs Foundational
- **US2 (T022–T037)**: needs Foundational only. Independent of US1 — different files, no shared symbol
- **US3 (T038–T045)**: needs Foundational; **soft dependency on US1** (T014 edits `AppListViewModel`, T041 rewrites it — sequence them or expect a conflict)
- **Polish (T046–T056)**: needs every story that is being delivered

### Within User Story 2

T027 → T028 → T029 → T030 are **one atomic change** and must land together; the tree does not compile
between T028 and T030. T025–T026 and T031–T032 are independent of that group and of each other.

### Parallel Opportunities

- T002, T003 together
- T005, T006 together (T004 first — T007 needs all three)
- T009, T010 together
- T022, T023, T024 together
- T033, T034, T035, T036 together (four different test files)
- T047, T048, T049, T051 together (read-only gates)
- **US1 and US2 can be worked in parallel** by two people. US3 should follow US1.

## Parallel Example: User Story 2

```bash
# The three use case tests — different files, no shared symbol:
Task: "Create test/feature/shortcut/domain/WaitDecisionUseCaseTest.kt covering U12-U15"
Task: "Create test/feature/shortcut/domain/CreateLockUseCaseTest.kt covering U16-U18"
Task: "Create test/feature/delay/domain/LoadDelayConfigUseCaseTest.kt covering U19"

# The four test migrations, after their implementations land:
Task: "Move test/feature/shortcut/data/PinGateTest.kt to test/feature/shortcut/domain/"
Task: "Move rule cases from WaitViewModelTest to WaitDecisionUseCaseTest"
Task: "Move rule cases from ShortcutConfigViewModelTest to CreateLockUseCaseTest"
Task: "Move edited-wins cases from DelayConfigViewModelTest to LoadDelayConfigUseCaseTest"
```

---

## Implementation Strategy

### MVP (User Story 1 only)

T001–T021. Delivers the half of the principle the maintainer stated first: the two clearest defects
— an app list that filters, dedupes and sorts inside its own source, and a lock store that merges the
launcher's answer with its own record — are gone. The app is unchanged, the suite is green, and the
codebase is constitutionally better. A coherent stopping point.

### Incremental Delivery

1. Setup + Foundational → both seams wired, nothing consuming them
2. + US1 → repositories read and write (MVP)
3. + US2 → holders stop deciding; five more sites compliant
4. + US3 → the filter leaves the state class; v5.0.0 fully satisfied
5. + Polish → comments true, gates passed, record written

Only all three stories satisfy constitution v5.0.0. Stopping earlier leaves a known, named defect
rather than an unknown one — but Principle III says a non-conforming site must not receive new code,
so anything left is a constraint on the *next* feature.

---

## Notes

- `[P]` = different files, no dependency on an incomplete task
- Every task is a relocation. If one turns into a rewrite, the parity requirement (FR-014) is at
  risk and the task should stop rather than continue
- Verify a test fails before implementing where you choose to work test-first; Constitution VI
  recommends it and does not mandate it
- **Committing is the maintainer's call** — never commit, push, branch or tag unless asked for that
  specific action in that specific message (Principle VII). T056 stays unchecked
