---

description: "Task list for Navigation Adoption"
---

# Tasks: Navigation Adoption

**Input**: Design documents from `/specs/010-navigation-adoption/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: Two test tasks appear below. They are not TDD scaffolding and not coverage padding — they
cover the single branch research R8 identifies as the only new logic in this feature that a test can
get wrong. Principle VI prohibits asserting that the navigation library navigates, so there is no
graph test and no destination test (FR-034).

**Organization**: grouped by user story. **These stories are strictly sequential**, and two of them
are ordered against their priority — see Dependencies for why.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to
- **[MAINTAINER]**: The maintainer performs this on a device. The agent does not drive the device
  and does not pre-verify (constitution, Development Workflow). It states the case and waits.

## Path Conventions

Single Android module. Sources at `app/src/main/java/com/slowlock/`, tests at
`app/src/test/java/com/slowlock/`, build config at `app/build.gradle.kts` and
`gradle/libs.versions.toml`.

**No file moves.** Every task edits a file in place or creates one at its final path.

---

## Phase 1: Setup

**Purpose**: establish the green reference and the fixture every later gate is judged against.

- [X] T001 Confirm the reference build is green on unmodified `main`: run `./gradlew assembleDebug` and `./gradlew test` from the repository root and record both outcomes
- [X] T002 [P] Create this feature's manual test plan at `specs/010-navigation-adoption/manual-test-plan.md`, with numbered cases covering every row of the table in [quickstart.md](./quickstart.md) §"Stage 4 — Verification", each traceable to its `G`-obligation in [contracts/navigation-graph.md](./contracts/navigation-graph.md) or to its FR
- [X] T003 [P] [MAINTAINER] Capture the baseline per [quickstart.md](./quickstart.md) §"Before anything is changed": install current `main`, create two locks with different delays and different treatments, pin both, screenshot every screen, and note how the app list looks when left and re-entered (the one deliberate change, G5). **Keep that install** — it is the in-place-update fixture T047 needs

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: get the dependencies and the route declarations in, with the build still green and no
behaviour touched. Everything below depends on this.

**⚠️ CRITICAL**: no user story phase may begin until T007 is complete.

- [X] T004 Add to `gradle/libs.versions.toml`: the `navigation` version `2.10.0`, the `serialization` plugin id at version ref `kotlin`, and `kotlinxSerialization` `1.11.0`; declare `androidx-navigation-compose`, `kotlinx-serialization-core` and the `kotlin-serialization` plugin. Each entry carries a comment saying what breaks without it (FR-024, research R1 and R2)
- [X] T005 Correct the catalog comment on `androidx-hilt-lifecycle-viewmodel-compose` in `gradle/libs.versions.toml`: it currently declares that this project does not want `navigation-compose`, which this feature makes false (FR-027, defect C-1). State instead why the *navigation* Hilt artifact is still not needed — the entry supplies the state-holder owner, so `hiltViewModel()` already scopes correctly (research R3)
- [X] T006 Apply `alias(libs.plugins.kotlin.serialization)` and add `implementation(libs.androidx.navigation.compose)` and `implementation(libs.kotlinx.serialization.core)` in `app/build.gradle.kts`
- [X] T007 Confirm the dependency change alone is inert: `./gradlew assembleDebug` and `./gradlew test` both pass with no source file changed. A failure here is a toolchain problem, not a design problem, and is resolved before any source is touched
- [X] T008 Create `app/src/main/java/com/slowlock/Routes.kt` with four `@Serializable` route types per [data-model.md](./data-model.md) §1: `Home`, `AppList`, `DelayConfig` and `ShortcutConfig`. **`DelayConfig` carries `packageName`, `initialSeconds` and `treatment` for now** — the shape `Stage.Delay` carries today. T027 shrinks it to `packageName` once the delay screen has a holder to own the rest

**Checkpoint**: dependencies resolve, routes compile, nothing in the app behaves differently.

---

## Phase 3: User Story 2 - Moving between screens is the library's job (Priority: P2)

**Goal**: the navigation library decides which screen is showing, what back does, and what survives a
round trip. Every hand-written equivalent is deleted.

**Independent Test**: walk every path through the app, including both routes into the delay step and
back out of it, and confirm each lands where the pre-change build landed. Then confirm no application
file decides which screen is showing.

- [X] T009 [US2] Rewrite `app/src/main/java/com/slowlock/SlowLockRoot.kt`: the pin-support gate above a `NavHost` with the four destinations and edges from [contracts/navigation-graph.md](./contracts/navigation-graph.md). Set **all four** transition parameters to none (G11, research R10) — a bare `NavHost` animates every step where today nothing does. This edit deletes `Stage`, `StageSaver`, `Origin`, `treatmentNamed`, `originNamed`, the four `*_TAG` constants, the four `root:*` keys, `rememberSaveableStateHolder`, `SaveableStateProvider`, `returnHome()` and `leaveDelay()` (FR-008)
- [X] T010 [US2] In the same file, move `LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { locksViewModel.refresh() }` onto the `Home` destination so it observes that entry's lifecycle, and delete the `locksViewModel.refresh().join()` that preceded the return home (research R9). `refreshSupport()` on `ON_START` stays **above** the graph, on the Activity's lifecycle — it is a whole-app precondition (contract, "Lifecycle hooks")
- [X] T011 [US2] In the `DelayConfig` destination, hold the edited delay in a `rememberSaveable` seeded from the route's `initialSeconds`, and pass it to `DelayConfigScreen`'s existing `seconds` / `onSecondsChange` parameters. **Mark it `TEMPORARY — removed by T027`** in a comment naming the task: it is the value's staging post between the deleted `Stage` and the holder Phase 5 gives it, and leaving it behind would be exactly the defect S5 forbids
- [X] T012 [P] [US2] Remove `BackHandler { onBack() }` from `app/src/main/java/com/slowlock/feature/apps/ui/AppListScreen.kt`; the `ScreenHeader` back control keeps calling `onBack`, which the destination now resolves to a pop (FR-010, G7)
- [X] T013 [P] [US2] Remove `BackHandler { onBack() }` from `app/src/main/java/com/slowlock/feature/delay/ui/DelayConfigScreen.kt`
- [X] T014 [P] [US2] Remove `BackHandler { onBack() }` from `app/src/main/java/com/slowlock/feature/shortcut/ui/ShortcutConfigScreen.kt`
- [X] T015 [US2] Run Gate 1 and Gate 2 from [quickstart.md](./quickstart.md): all four Gate 1 greps return nothing, and the `NavHost` call sets every transition parameter
- [X] T016 [US2] `./gradlew assembleDebug` and `./gradlew test` both pass
- [ ] T017 [US2] [MAINTAINER] **Re-run for G11 only** — the rest passed on 2026-08-27, before transitions were enabled. Device checkpoint: obligations G1, G2, G3, G4, G7, G8, G9, G11 and G12 from [contracts/navigation-graph.md](./contracts/navigation-graph.md). G5 is expected to differ from the baseline and is the approved FR-002(a) — confirm it differs in exactly that way and no other

**Checkpoint**: navigation is the library's. Every screen still holds what it held before.

---

## Phase 4: User Story 3 - What a screen holds dies with the screen (Priority: P3)

**Goal**: state lives exactly as long as its screen is reachable. Nothing abandoned reappears;
nothing in flight is lost.

**Independent Test**: leave each screen in a non-default state, exit the flow, and re-enter for a
different app — nothing carries over. Repeat with a rotation and a process kill instead of an exit —
nothing is lost.

**Note**: `AppListViewModel` and `ShortcutConfigViewModel` become entry-scoped with **no code
change**, because `hiltViewModel()` reads the owner the destination supplies (research R3, R5). The
work below is what that scope makes possible, plus the two comments the new scoping requires.

- [X] T018 [US3] Move the treatment selection into `app/src/main/java/com/slowlock/feature/shortcut/ui/ShortcutConfigViewModel.kt`: seed it from the route's `treatment` argument via `SavedStateHandle`, expose it on `ShortcutConfigUiState`, and accept the selection through a function. Apply the R8 rule — **a restored handle wins over the route argument**, or a choice made before process death is silently replaced (contract S4)
- [X] T019 [US3] Remove the `initialTreatment` parameter and `var treatment by rememberSaveable` from `app/src/main/java/com/slowlock/feature/shortcut/ui/ShortcutConfigScreen.kt`; the screen reads the selection off the state and reports changes to the holder (FR-018)
- [X] T020 [US3] Add `app/src/test/java/com/slowlock/feature/shortcut/ui/ShortcutConfigViewModelTest.kt` covering the R8 branch only: a holder built with an empty handle takes the route argument; a holder built with a handle already carrying a treatment keeps it and does **not** re-take the argument. Verify by mutation that swapping the branch turns the suite red
- [X] T021 [US3] Add the comment Principle II requires on `RootViewModel` in `app/src/main/java/com/slowlock/RootViewModel.kt`: it is the one holder that outlives every screen, and the comment names the behaviour requiring it — pin support decides whether the graph renders at all and is re-read on every return to the foreground, so it belongs to no entry (FR-016, plan Complexity Tracking)
- [X] T022 [US3] Add the comment FR-012 requires on the pin-support gate in `app/src/main/java/com/slowlock/SlowLockRoot.kt`: this `when` is over a precondition, not over a screen, which is why it is not a destination — the next reader will otherwise mistake it for the construct T009 deleted
- [X] T023 [US3] Run Gate 3 and Gate 4 from [quickstart.md](./quickstart.md): the only `hiltViewModel()` outside a destination is `RootViewModel`, and no `rememberSaveable` under `feature/` holds a domain value
- [X] T024 [US3] `./gradlew assembleDebug` and `./gradlew test` both pass
- [X] T025 [US3] [MAINTAINER] Device checkpoint: treatment kept across rotation and across process death; treatment discarded on backing out and on configuring a different app next; app list opens fresh after being left entirely (FR-002(a))

**Checkpoint**: every screen holder but the root's is scoped to its entry, and the treatment
workaround from feature 009 is gone.

---

## Phase 5: User Story 4 - The delay screen owns what it edits (Priority: P4)

**Goal**: the delay screen holds its own delay, resolves its own target and reads its own saved
configuration. The root holds only what belongs to no screen.

**Independent Test**: open the delay screen for a configured app and confirm it shows the saved value
with no default flash; then confirm the root holds nothing on any screen's behalf.

- [X] T026 [US4] Create `app/src/main/java/com/slowlock/feature/delay/ui/DelayConfigViewModel.kt` with `DelayConfigUiState` in the same file, per [data-model.md](./data-model.md) §3. Constructor: `AppTargetRepository`, `AppIconRepository`, `DelayConfigRepository`, `SavedStateHandle`. Apply the R8 rule — **the disk read happens only when the handle holds no delay** (contract S4)
- [X] T027 [US4] Shrink `DelayConfig` in `app/src/main/java/com/slowlock/Routes.kt` to `packageName` alone, and remove the `rememberSaveable` bridge T011 left in the destination in `app/src/main/java/com/slowlock/SlowLockRoot.kt`. The `Next` action now reads the delay and treatment off the holder to build the `ShortcutConfig` route
- [X] T028 [US4] Rewrite the top of `app/src/main/java/com/slowlock/feature/delay/ui/DelayConfigScreen.kt` to observe the holder: drop the `seconds`, `onSecondsChange`, `targets` and `icons` parameters and both `produceState` blocks, and withhold the readout until `loaded` (FR-002(c), FR-019). Nothing below the screen's top-level composable changes
- [X] T029 [US4] Remove `targets`, `icons` and `configFor()` from `app/src/main/java/com/slowlock/RootViewModel.kt`, leaving pin support alone (FR-020). This closes finding F-06
- [X] T030 [US4] Add `app/src/test/java/com/slowlock/feature/delay/ui/DelayConfigViewModelTest.kt` covering the R8 branch only: an empty handle loads the saved delay from the repository; a handle already carrying a delay keeps it and the repository is **not** read for it. Verify by mutation that swapping the branch turns the suite red
- [X] T031 [US4] `./gradlew assembleDebug` and `./gradlew test` both pass
- [X] T032 [US4] [MAINTAINER] Device checkpoint: the delay screen opens on the saved value and never shows a default first, on both routes in — from the app list and by editing an existing lock (FR-002(c)). If a flash is visible, take the recorded D5 fallback and record it (FR-042)

**Checkpoint**: no holder exposes a repository on another screen's behalf, and finding F-06 is
resolved by construction.

---

## Phase 6: User Story 5 - The record is closed and the comments are true (Priority: P5)

**Goal**: no comment describes a mechanism that is no longer there, and the two findings this feature
resolves are ruled on.

**Independent Test**: read every comment in every file this feature touched and confirm none
describes the removed navigation mechanism or activity-scoped holders as a current fact.

- [X] T033 [P] [US5] Correct or delete the comments in `app/src/main/java/com/slowlock/SlowLockRoot.kt` and `app/src/main/java/com/slowlock/RootViewModel.kt` that describe the removed mechanism: the file KDoc's "Navigation is a `when` over Stage with no navigation library", the FR-023a block, and "The navigation stage is deliberately not here" (FR-027)
- [X] T034 [P] [US5] Correct or delete the comments in `app/src/main/java/com/slowlock/feature/shortcut/ui/ShortcutConfigScreen.kt` and `ShortcutConfigViewModel.kt` whose whole argument is that a holder here would be activity-scoped — the `rememberSaveable` rationale block and "The treatment selection is deliberately not held here" (FR-027)
- [X] T035 [P] [US5] Correct the comments in `app/src/main/java/com/slowlock/feature/delay/ui/DelayConfigScreen.kt`: "The screen owns no state" and the FR-024 parameter KDoc are both false once the screen has a holder (FR-027)
- [X] T036 [P] [US5] Correct the FR-030 `BackHandler` comment in `app/src/main/java/com/slowlock/feature/apps/ui/AppListScreen.kt`, and add the note research R6 asks for: the `ON_START` refresh now also fires when the user pops back into the list, which is a redundant read accepted deliberately rather than a defect
- [X] T037 [P] [US5] Update the KDoc on `LocksViewModel.refresh()` in `app/src/main/java/com/slowlock/feature/locks/ui/LocksViewModel.kt` — the sentence about the returned `Job` letting the root wait before it navigates is false once T010 drops the join — and on `AppListViewModel.onQueryChanged` in `.../feature/apps/ui/AppListViewModel.kt`, whose saved query now dies with the entry (FR-027)
- [X] T038 [US5] Rule and close **F-05** and **F-06** in `specs/009-constitution-alignment/findings.md`, stating that each is resolved by construction — by the scope the entry gives the holder — rather than by a fix. Leave every other finding open and unchanged (FR-028)
- [X] T039 [US5] In `specs/009-constitution-alignment/quickstart.md`, correct the two verification checks findings F-11 and F-12 flag as matching what they were not written to match, and remove the `remember...Source(` check outright — the holder it scanned for no longer exists (FR-037)
- [X] T040 [US5] Confirm `CLAUDE.md` points at `specs/010-navigation-adoption/plan.md` (done during planning — verify, do not repeat) (FR-030)
- [X] T041 [US5] Run Gate 5 from [quickstart.md](./quickstart.md), reading each match rather than counting them: `grep -rni 'stage'` legitimately matches the `1 / 3` step wording, and the catalog now names `navigation-compose` as a dependency — what must not survive is the sentence saying this project does not want it (T005)

**Checkpoint**: the code says what it does, and the record says what was decided.

---

## Phase 7: User Story 1 - Everything a user already has keeps working (Priority: P1) 🎯 The acceptance bar

**Goal**: the post-change build loses nothing and looks the same, but for the three differences
FR-002 approves.

**Independent Test**: install the post-change build over the baseline fixture from T003 and run the
manual test plans of features 001-005 and 007. Every case passes with no data loss and no re-pinning.

**Why this story is last rather than first**: it is not a slice of functionality, it is the bar every
other phase is judged against. Its groundwork is T003's baseline; its verification cannot run until
there is something to verify.

- [X] T042 [US1] `./gradlew assembleDebug` and `./gradlew test` both pass
- [X] T042a [US1] Run Gate 6 from [quickstart.md](./quickstart.md): `git diff` over `app/src/main/res/values*/strings.xml` is empty. No string is added, removed or reworded by this feature (FR-005, SC-003)
- [X] T043 [US1] `./gradlew assembleRelease` succeeds. This gate exists because R8 can strip a generated route serializer and the failure surfaces when a user navigates, not when the project builds (research R13). Any keep rule this needs goes in `app/src/main/keepRules/rules.keep`
- [X] T044 [US1] [MAINTAINER] Run one complete create-a-lock flow on a **release** build, and tap a pinned icon through to the hand-off. A route that resolves in debug and fails here is R13's failure mode
- [ ] T045 [US1] [MAINTAINER] **Re-run section N2 only** — the rest passed on 2026-08-27; N2 was rewritten when transitions were enabled and its old result does not carry over. Run this feature's manual test plan from T002 in full
- [X] T046 [US1] [MAINTAINER] Run the manual test plans of features 001-005 and 007 in full. **Exactly three cases may differ from their recorded expected result**, and each must be one of FR-002's three. Any fourth difference is a finding (SC-001, SC-004)
- [X] T047 [US1] [MAINTAINER] Install the post-change build in place over the T003 baseline fixture: every lock keeps its delay and treatment, and every pinned icon still launches its target (SC-002)
- [X] T048 [US1] Record which of the two pre-approved fallbacks fired, if either, and record it as a deviation in [plan.md](./plan.md)'s Complexity Tracking (FR-042). If neither fired, record that

**Checkpoint**: the feature is behaviourally complete and verified.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T049 Re-evaluate all eight principles against the shipped code and update the post-implementation note in [plan.md](./plan.md)'s Constitution Check with what actually survived, not what was anticipated
- [X] T050 [P] Open `specs/010-navigation-adoption/findings.md` for anything noticed and not fixed during the work, following the same rules 009's findings file states (FR-031, FR-032). If nothing was found, do not create the file
- [X] T051 [P] Confirm every deviation surviving the feature is in Complexity Tracking with the simpler alternative named and the reason it was rejected (FR-043)
- [X] T052 Final gate: `./gradlew assembleDebug` and `./gradlew test` from a clean state, both green
- [ ] T053 Report to the maintainer what changed, and **offer** the commit. Do not perform it — this line is a note to the maintainer, not an authorization (Principle VII, FR-044)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies. T003 is a maintainer task and the agent waits for it before Phase 7 needs it, not before Phase 2
- **Foundational (Phase 2)**: blocks everything. T008's routes are what Phase 3 renders
- **US2 (Phase 3)**: depends on Phase 2. **Blocks US3 and US4** — entry-scoped holders have no entries to scope to until the graph exists
- **US3 (Phase 4)**: depends on US2
- **US4 (Phase 5)**: depends on US2, and on T011's bridge existing to be removed. Independent of US3 in substance; sequenced after it by priority
- **US5 (Phase 6)**: depends on US2, US3 and US4 — it corrects the comments those phases falsify
- **US1 (Phase 7)**: depends on every phase above. It is the acceptance bar, not an increment
- **Polish (Phase 8)**: depends on Phase 7

### Ordering against priority, and why

The template expects P1 first. Two stories are deliberately out of priority order:

- **US1 (P1) runs last.** "Nothing a user has breaks" is not a slice that can be built; it is the bar
  the others are measured against. Its only buildable part is T003's baseline, which runs first.
- **US2 (P2) runs first** among the buildable stories, because it is the graph every other story
  needs.

### Within each phase

- The deletions in T009 happen inside the rewrite, not after it: `Stage` and the `NavHost` cannot
  both decide what is rendered, so removing one and adding the other is a single edit
- Each holder's test follows the holder it covers, and each is verified by mutation rather than by
  passing (Principle VI)
- Every task ends with the project building and `./gradlew test` passing. A task that cannot is split
  until it can

### Parallel Opportunities

- **T002 and T003** — different files, and T003 is the maintainer's
- **T012, T013, T014** — three different screen files, one identical deletion in each
- **T033 through T037** — five comment tasks across seven files with no overlap
- **T050 and T051** — different documents

Nothing else parallelises. Every structural phase rewrites what the next one operates on.

---

## Parallel Example: User Story 2

```bash
# The three BackHandler removals touch three different files and share no state:
Task: "Remove BackHandler from feature/apps/ui/AppListScreen.kt"
Task: "Remove BackHandler from feature/delay/ui/DelayConfigScreen.kt"
Task: "Remove BackHandler from feature/shortcut/ui/ShortcutConfigScreen.kt"
```

## Parallel Example: User Story 5

```bash
# Five comment tasks, seven files, no overlap:
Task: "Correct the removed-mechanism comments in SlowLockRoot.kt and RootViewModel.kt"
Task: "Correct the activity-scope comments in ShortcutConfigScreen.kt and ShortcutConfigViewModel.kt"
Task: "Correct the no-state comments in DelayConfigScreen.kt"
Task: "Correct the BackHandler comment and add the R6 note in AppListScreen.kt"
Task: "Correct the refresh() and onQueryChanged() KDocs in LocksViewModel.kt and AppListViewModel.kt"
```

---

## Implementation Strategy

### First increment (through Phase 3)

1. Phase 1: Setup — T003 is the maintainer's; the agent states it and waits.
2. Phase 2: Foundational — T007 proves the dependencies are inert before any source moves.
3. Phase 3: the graph replaces the stage machine.
4. **STOP and VALIDATE**: T017's device checkpoint.

This is a complete, shippable state: navigation is the library's, holders are already entry-scoped
by construction, and the only thing left in composition that shouldn't be is T011's marked bridge.

### Incremental delivery

Phases 4, 5 and 6 each end green and each is a valid stopping point. The one thing that must not be
split across a pause is **T011 and T027**: leaving the temporary bridge in place indefinitely is
exactly the defect S5 forbids, and the comment naming T027 is what stops it becoming permanent.

### Not a parallel-team feature

`SlowLockRoot.kt` is touched by four of the six phases. Splitting this across developers would
produce conflicts on the one file the whole feature turns on. One worker, in order.

---

## Notes

- **[P]** = different files, no dependencies.
- **[MAINTAINER]** tasks are run by the maintainer on a device. The agent states the case and waits;
  it never drives the device and never pre-verifies a manual case (constitution, Development
  Workflow).
- A defect found mid-task is recorded and left in place until the maintainer confirms that specific
  fix (FR-031, FR-032). It is never fixed "while in the file", and a step containing a fix is never
  reported as behaviour-preserving (FR-033).
- Two fallbacks are pre-approved and both are taken **only on device evidence**: the graph-scoped
  lock-list holder (R9) and the route-carried loaded delay (R8/D5). Anything else a user can see is a
  finding, not a fallback.
- The frozen values are untouched by every task above. If `ShortcutContractTest` goes red, stop —
  something moved that must not have.
- Committing is the maintainer's call. T053 is a note, not an authorization (Principle VII).
