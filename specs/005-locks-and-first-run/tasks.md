# Tasks: Locks Home & First Run (Phase 2)

**Input**: Design documents from `/specs/005-locks-and-first-run/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/](./contracts/)

**Tests**: JVM unit tests only. Two test files are **mandatory**, not optional — the constitution
requires every frozen persisted value to be asserted against a literal, and requires the null
`getLaunchIntentForPackage()` path to have automated coverage. Instrumented suites
(`src/androidTest`, `connectedAndroidTest`, Espresso, UI Automator) MUST NOT be added.

**Organization**: grouped by user story. US1 and US2 are both P1 and together are the MVP; US1
alone is shippable and is the smaller of the two.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: different file, no dependency on an incomplete task — safe to do in any order
- **[Story]**: US1–US5, mapping to spec.md

## Path Conventions

Single `:app` Android module. Main sources under
`app/src/main/java/com/slowlock/`, resources under `app/src/main/res/`, JVM tests under
`app/src/test/java/com/slowlock/`.

---

## Phase 1: Setup

**Purpose**: a clean baseline and the one new package.

- [X] T001 Verify the pre-change baseline is green: run `./gradlew assembleDebug` and `./gradlew test` from the repository root and record that all ten existing test files pass unmodified (FR-042, research R9)
- [X] T002 [P] Create the new source package directory `app/src/main/java/com/slowlock/locks/` and the test package directory `app/src/test/java/com/slowlock/locks/`
- [X] T003 [P] Cut and switch to branch `005-locks-and-first-run` from the current `004-visual-redesign` branch — plan.md's header already names this branch (the plan's branch note)

**Checkpoint**: baseline green, package exists, work is on its own branch.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: the durable record. Nothing about "what locks exist" can be built until this is done,
so this phase blocks every user story.

**⚠️ CRITICAL**: `contracts/lock-store.md` is the authority for every task in this phase. The three
literals it freezes must be asserted, not just written.

- [X] T004 Write `app/src/test/java/com/slowlock/locks/LockListTest.kt` **first**, covering: the frozen `LOCKS_FILE` (`"slowlock.locks"`), `LOCKS_KEY` (`"packages"`) and `LOCKS_SEPARATOR` (`"\n"`) asserted against literals (L1); `locksFrom(null)`, `locksFrom("")` and `locksFrom("   ")` each yielding an empty list; blank entries dropped and entries trimmed; duplicates collapsing to their **first** position; encode/decode round trip; `withLock` appending when absent and returning the list unchanged (same positions) when present; `withoutLock` removing every occurrence and no-op'ing on an absent package; and that nothing on the read path throws (L4, L5, FR-006, FR-007, FR-013). Confirm it fails to compile/run before T005
- [X] T005 Create `app/src/main/java/com/slowlock/locks/LockList.kt` — pure Kotlin, **no `android.*` imports**: the three frozen internal constants with KDoc marking them FROZEN and naming the consequence of a rename, plus `locksFrom`, `encodeLocks`, `withLock`, `withoutLock` per data-model.md §1. Run `./gradlew test` and confirm `LockListTest` passes
- [X] T006 Create `app/src/main/java/com/slowlock/locks/LockStore.kt` — wiring only, mirroring `DelayConfigStore`'s shape: `load()`, `add(packageName)`, `remove(packageName)`, every one suspending on `Dispatchers.IO`; read-modify-write through the T005 functions, one `Editor`, `apply()`; the wrong-type read guarded with `runCatching`. KDoc must state that this is the only class that opens `slowlock.locks` (L3, L4, FR-040)
- [X] T007 Add the lock record write to `app/src/main/java/com/slowlock/shortcut/ShortcutConfigScreen.kt`: instantiate `LockStore` beside the existing `DelayConfigStore` and call `lockStore.add(packageName)` inside the private `create()` function, **between `store.save(...)` and `pinner.pin(...)`**. Extend `create()`'s existing ordering KDoc to say why the record precedes the pin request and why nothing after `pin()` may condition it (L6, FR-003). Change nothing else in this file

**Checkpoint**: locks are recorded and readable. No screen shows them yet.

---

## Phase 3: User Story 1 — The app states its idea once (Priority: P1) 🎯 MVP

**Goal**: a fresh install opens on one screen that says what SlowLock is for, with one button into
the existing flow.

**Independent Test**: install on a device with no SlowLock data. The intro appears instead of the
app list; "Set up a lock" opens the app list; the whole 004 flow works unchanged from there;
rotating and killing the process still shows the intro; system back exits.

- [X] T008 [P] [US1] Add the intro screen's strings to `app/src/main/res/values/strings.xml`: title, body copy stating what the app does **and** that nothing is blocked and nothing is counted, and the action label "Set up a lock". Every string a resource, any stylistic capitalisation stored capitalised with a translator comment, no case transformation at runtime (FR-018, FR-036, FR-038, C8)
- [X] T009 [P] [US1] Create `app/src/main/java/com/slowlock/locks/IntroScreen.kt` — `IntroScreen(onStart: () -> Unit, modifier: Modifier)`: no `ScreenHeader`, no back tile, no step counter; body copy in the `Body` role at `Ink60`, any eyebrow in the `Eyebrow` role at **`AmberDark`** (never `Amber` for a glyph, C2); exactly one `PrimaryAction` from `ui/components`; stateless, reads nothing, persists nothing; no colour or `sp` literal (K1, FR-019, FR-033, K6)
- [X] T010 [US1] In `app/src/main/java/com/slowlock/SlowLockRoot.kt`, add `Stage.Home` to the `Stage` sealed interface and make it the **initial** stage in place of `Stage.List`; extend `StageSaver` with a `HOME_TAG` written and read through the exhaustive `when`, and change the unrecognised-discriminant fallback from `Stage.List` to `Stage.Home` (data-model.md §5, N9)
- [X] T011 [US1] In `SlowLockRoot.kt`, read the lock list at the root: hold a **latching** "has ever loaded" state alongside the list, load it through `LockStore` on `Lifecycle.Event.ON_START` beside the existing `pinSupport` re-read, and render the `Stage.Home` branch as — never loaded: **nothing** (the same rule `PinSupport.Unknown` follows, research R4); empty: `IntroScreen` with `onStart = { stage = Stage.List }`; non-empty: **set** `stage = Stage.List` as an explicit, commented **interim** that US2 replaces with a `LocksScreen` rendered inside the `Home` branch. Setting the stage rather than rendering the list from inside `Home` is deliberate: the list's `LIST_KEY` state provider belongs to the `Stage.List` branch (N2, N4, N8, FR-017, FR-019a)
- [X] T012 [US1] In `SlowLockRoot.kt`, add a `HOME_KEY` entry to the `SaveableStateHolder` and make every "return to the root" transition target `Stage.Home` instead of `Stage.List`, keeping the existing `removeState` rules for `DELAY_KEY` and `CONFIG_KEY` and keeping `LIST_KEY` retained. `HOME_KEY` is **retained**, not dropped (N4). Update the file's header KDoc, which currently states the root has three stages and that no stage is added
- [X] T013 [US1] Verify FR-025 survived the root edits: with `pinSupport()` answering `Unsupported`, `PinUnsupportedScreen` must still render **in place of** the entire `when (stage)` — `Stage.Home` included, and whether the lock list has loaded or not. The `Unsupported` branch must sit outside and ahead of the stage `when`, never inside a `Stage.Home` branch (N1, FR-025, 002 FR-029, 003 FR-004)
- [X] T014 [US1] Run `./gradlew assembleDebug` and `./gradlew test`; confirm all ten existing test files still pass unmodified

**Checkpoint**: US1 is complete and shippable on its own. A fresh install sees the intro; a user with
locks still lands on the app list, exactly as before.

---

## Phase 4: User Story 2 — The locks I made have somewhere to live (Priority: P1) 🎯 MVP

**Goal**: the Locks screen replaces the app list as what a returning user sees.

**Independent Test**: create two locks through the existing flow, leave the app, reopen it. Both
appear with the right app, delay and treatment. Create a third; it appears too. Re-run the flow for
an app that already has a lock; there is still exactly one row for it, with the new values.

**Depends on**: Phase 2 (the record) and US1 (`Stage.Home` and its derived branch).

- [X] T015 [P] [US2] Create `app/src/main/java/com/slowlock/locks/Lock.kt` — the `Lock` data class (`packageName`, `label: String?`, `versionCode`, `delaySeconds`, `treatment`) and the `isAvailable` extension. No icon field; KDoc must say why (data-model.md §3)
- [X] T016 [P] [US2] Create `app/src/main/java/com/slowlock/locks/LocksUiState.kt` — `loaded`, `locks`, `pendingRemoval`, with the three derived meanings from data-model.md §4 documented on the class. `loaded` is **latching**: false until the first read completes, never false again
- [X] T017 [US2] Write `app/src/test/java/com/slowlock/locks/LocksViewModelTest.kt` covering: an empty lock list yields the intro condition; a resolvable package yields an available row carrying the stored delay and treatment; **a package whose launch intent or label resolves to `null` yields an unavailable row (`label == null`) and does not throw** (FR-020, and the constitution's null-`getLaunchIntentForPackage()` obligation); a recorded package with nothing in the configuration store yields `DelayConfig.DEFAULT`'s values; that row order matches the lock list's order (FR-006); and that a second `refresh()` never returns `loaded` to false (FR-016)
- [X] T018 [US2] Create `app/src/main/java/com/slowlock/locks/LocksViewModel.kt` — an `AndroidViewModel` shaped like `AppListViewModel`: a `StateFlow<LocksUiState>`, a `refresh()` that in **one IO pass** reads `LockStore.load()`, then each package's `DelayConfig`, then each package's `ShortcutTarget` via the existing `resolveShortcutTarget`; an `iconCache` exposed for the rows; and the platform lookups injected as **lambdas with defaults**, not reached for inline, so T017 can drive them without a device. `refresh()` **sets `loaded = true` and never clears it**, replacing the rows in place so a return from the flow updates the list without blanking it (FR-016). Icons are **not** loaded in this pass (R5, FR-015, FR-040). Run `./gradlew test` and confirm T017 passes
- [X] T019 [P] [US2] Add the Locks screen's strings to `app/src/main/res/values/strings.xml`: the title, a **plural** resource for the count that states the number only — never "on your home screen" (FR-011) — the "+ New lock" action label, the row's delay and treatment lines, the row icon content description, and the unavailable-row message naming the package. All resources; no case transformation (FR-036, FR-038)
- [X] T020 [US2] Create `app/src/main/java/com/slowlock/locks/LocksScreen.kt` — the screen per K2: title, count, one row per lock, exactly one `PrimaryAction` ("+ New lock"); the row as a card at `MaterialTheme.shapes.large` (18dp) with `Card` fill and `Line` border, 44dp icon at `shapes.extraSmall` from `iconCache` with a `Fill` placeholder, label in `RowLabel`/`Ink` truncating with an ellipsis, delay and treatment in `Footnote` (mono)/`Ink40`, row height ≥64dp growing with the font scale. Stateless — every mutation leaves through a callback. No search, filter, sort, reorder, toggle or "pin again". No colour or `sp` literal (K2, K6, FR-010, FR-012, FR-014, FR-015)
- [X] T021 [US2] Implement the unavailable row inside `LocksScreen.kt` per K3: shown, never hidden; **not** tappable and carrying no click modifier; names what is wrong using the package name; `Fill` placeholder in the icon slot; must not crash the screen (FR-020)
- [X] T022 [US2] In `SlowLockRoot.kt`, replace T011's interim non-empty branch with `LocksScreen` rendered inside the `Stage.Home` branch, wired to `LocksViewModel` via `viewModel()`; move the lock-list read from the root's own `LockStore` call to the view model's `refresh()` on `ON_START`, and drive the `Stage.Home` intro/Locks/nothing choice from `LocksUiState` (N2, N8). Wire `onNewLock = { stage = Stage.List }` (FR-014)
- [X] T023 [US2] Verify update-in-place end to end: completing the flow for an app that already has a lock leaves exactly one row, showing the new delay and treatment, in its original position — this is `withLock`'s no-op-when-present rule (T005) plus FR-005's read-from-the-config-store, and needs no new code if both were done right. Fix whichever is wrong if it is not (FR-013, FR-016, US4 scenario 5)
- [X] T024 [US2] Run `./gradlew assembleDebug` and `./gradlew test`

**Checkpoint**: MVP complete. The app opens on the user's locks, or on the intro when there are none.

---

## Phase 5: User Story 3 — Getting out of the wizard, and knowing where I am in it (Priority: P2)

**Goal**: the app list gains a back control, and all three steps carry their counter.

**Independent Test**: from Locks, tap "+ New lock", then the back control on the app list — Locks is
shown. Walk all three steps and confirm the counter reads `1 / 3`, `2 / 3`, `3 / 3`. System back does
the same as each on-screen control on every step.

**Depends on**: US1 (there must be somewhere for back to go).

- [X] T025 [P] [US3] Add the step-counter string to `app/src/main/res/values/strings.xml` as `"%1$d / 3"` — the `3` is a **literal in the resource**, never a computed stage count (R7, FR-029)
- [X] T026 [US3] Add `step: Int? = null` to `ScreenHeader` in `app/src/main/java/com/slowlock/ui/components/ScreenHeader.kt`, rendering the counter in the `Footnote` role at `Ink40`, pushed to the trailing edge of the header row, and only when non-null. Merge the counter into the header's semantics so it is announced with the title ("Choose an app, step 1 of 3") rather than being decorative — it is information US3 exists to give, and it is not a control. Keep U1's existing rule that `onBack == null` renders no tile and no leading space. Replace the KDoc paragraph that defers this parameter to Phase 2 with what it now does (K5)
- [X] T027 [P] [US3] In `app/src/main/java/com/slowlock/apps/AppListScreen.kt`, add an `onBack: () -> Unit` parameter, pass it to `ScreenHeader` along with `step = 1`, and add `BackHandler { onBack() }` matching the pattern `DelayConfigScreen` and `ShortcutConfigScreen` already use. Change nothing else — not the view model, the query, the rows, the icon cache or the snackbar (N10, FR-028, FR-030)
- [X] T028 [P] [US3] In `app/src/main/java/com/slowlock/delay/DelayConfigScreen.kt`, pass `step = 2` to `ScreenHeader`. No other change; its `BackHandler` already exists (FR-029)
- [X] T029 [P] [US3] In `app/src/main/java/com/slowlock/shortcut/ShortcutConfigScreen.kt`, pass `step = 3` to `ScreenHeader`. No other change; its `BackHandler` already exists (FR-029)
- [X] T030 [US3] In `SlowLockRoot.kt`, wire the app list's `onBack = { stage = Stage.Home }`, keeping `LIST_KEY` retained so the scroll position and query survive the round trip (N3, N4, 003 FR-011)
- [X] T031 [US3] Confirm system back on `Stage.Home` still leaves the app — no `BackHandler` on the root branch, which is the default activity behaviour and is what FR-031 wants. Add nothing here; this task is the check, not a change
- [X] T032 [US3] Run `./gradlew assembleDebug` and `./gradlew test`

**Checkpoint**: the flow is a wizard with a way out and a sense of place.

---

## Phase 6: User Story 4 — Changing a lock I already made (Priority: P2)

**Goal**: tapping a lock row enters the existing flow at the delay step, carrying its saved values.

**Independent Test**: create a lock at 10s. From Locks, tap it, change to 30s, finish. The row reads
30s, there is still only one row for that app, and the list order is unchanged. Back out of an edit
and the lock keeps the values it had.

**Depends on**: US2 (there must be a row to tap).

- [X] T033 [US4] In `SlowLockRoot.kt`, add `enum class Origin { List, Home }` and an `origin: Origin` field to both `Stage.Delay` and `Stage.Shortcut`; extend `StageSaver` to write and read it by `Enum.name` with the same sanitise-to-first-entry rule the treatment already uses (data-model.md §5, N9)
- [X] T034 [US4] In `SlowLockRoot.kt`, make the delay step's back branch on `origin`: `Stage.List` when `origin == Origin.List`, `Stage.Home` when `origin == Origin.Home` — and keep the icon step's back returning to `Stage.Delay` carrying the same seconds, treatment **and** origin. Set `origin = Origin.List` on the app-list row tap (N3, FR-023, US4 scenario 3)
- [X] T035 [US4] In `SlowLockRoot.kt`, wire `LocksScreen`'s `onEdit` to `Stage.Delay(packageName, seconds, treatment, origin = Origin.Home)` using the values already resolved in `LocksUiState` — no re-read of `DelayConfigStore`, because the read is already done (N6, FR-023)
- [X] T036 [US4] Confirm no write happens on an abandoned edit: `Stage` is transient and the only writes are in `create()`, so backing out must leave both the configuration record and the lock record untouched, with no rollback path. Add nothing if that already holds (N7, FR-023a)
- [X] T037 [US4] Run `./gradlew assembleDebug` and `./gradlew test`

**Checkpoint**: a lock is editable in three taps and a drag from launch.

---

## Phase 7: User Story 5 — Removing a lock (Priority: P3)

**Goal**: a lock can leave the list, behind a confirmation whose wording is the deliverable.

**Independent Test**: create two locks, long-press one, confirm. It leaves the list; the other
remains. Tapping the removed lock's still-present home-screen icon still waits and still opens the
app. Removing the last lock returns to the intro.

**Depends on**: US2.

- [X] T038 [P] [US5] Add the removal strings to `app/src/main/res/values/strings.xml`: the custom accessibility action label ("Remove lock"), the confirmation title, the confirmation body — which MUST name the app and say plainly that the home-screen icon is **not** removed and the user must remove it themselves, MUST NOT imply SlowLock can remove it, and MUST NOT suggest the icon will stop working — and the confirm and dismiss labels (FR-022, SC-012, K4, Constitution I)
- [X] T039 [US5] In `LocksScreen.kt`, add `Modifier.combinedClickable(onClick = onEdit, onLongClick = onRequestRemove)` to **available** rows only (K4, FR-021)
- [X] T040 [US5] In `LocksScreen.kt`, add a custom accessibility action ("Remove lock") to **every** row via `Modifier.semantics { customActions = … }`, so removal is reachable without a long press, and add the **visible** remove control to unavailable rows, which have no tap target for a long press to attach to (FR-041, K3, K4, R6, SC-011)
- [X] T041 [US5] In `LocksScreen.kt`, show one confirmation dialog when `state.pendingRemoval != null`, with the container explicitly `Card` and the title/body explicitly `Ink`/`Ink60` — stated, not left to Material's derived defaults, which land outside the eleven. Confirm calls `onConfirmRemove`; dismiss calls `onDismissRemove` and changes nothing (K4, FR-033, SC-009, US5 scenario 3)
- [X] T042 [US5] In `LocksViewModel`, add `onRequestRemove`, `onDismissRemove` and `onConfirmRemove` — the last calling `LockStore.remove(packageName)` and then refreshing the state. This is the **only** call site for `remove` (L6, FR-021)
- [X] T043 [US5] Confirm removing the last lock shows the intro with no code path of its own — it is `Stage.Home` re-deriving from an empty list. Add nothing if that already holds (N2, FR-017, US5 scenario 5, US2 scenario 6)
- [X] T044 [US5] Run `./gradlew assembleDebug` and `./gradlew test`

**Checkpoint**: all five stories complete.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T045 Write `specs/005-locks-and-first-run/manual-test-plan.md` — numbered cases, each traceable to a requirement, covering at minimum: the fresh-install intro (US1); rotation and process death on both new screens, including that the removal confirmation is not expected to survive process death; every US2–US5 acceptance scenario; the uninstalled-app row (FR-020); the declined pin dialog still leaving a lock (FR-011); a removed lock's home-screen icon still waiting and still opening the app (SC-012); the upgrade path from a build without this feature (FR-024); the unsupported-launcher screen taking over ahead of both new screens (FR-025); both new screens against their artboards at default scale on 412×892 (SC-007); the largest system font scale on the smallest supported screen (SC-008); and TalkBack reading every row, hearing each step counter, and removing a lock without a long press (SC-011). **The maintainer runs this** — no agent may drive the connected device to pre-verify a case (Constitution, Manual verification)
- [X] T046 [P] Verify the palette is still closed at eleven, that no new text pairing was introduced outside `SlowLockPaletteTest`, and that both new screens render light regardless of the system setting (FR-037 — structural, since `SlowLockTheme` takes no `darkTheme` parameter): run `./gradlew test` and grep the source tree for colour literals (FR-033, FR-034, FR-037, SC-009, SC-010, C1, C3)
- [X] T047 [P] Verify no user-visible literal text remains in the two new screens and that no `uppercase()`/`lowercase()` runs on user-visible text anywhere in the diff (FR-036, FR-038, C8)
- [X] T048 [P] Review the diff against `contracts/root-navigation.md` §N10's must-not-appear list — `ShortcutContract.kt`, `ShortcutPinner.kt`, `ShortcutTarget.kt`, `PinSupport.kt`, `DelayConfig.kt`, `DelayConfigStore.kt`, `WaitScreen.kt`, `WaitTiming.kt`, `ShortcutLaunchActivity.kt`, `AppListViewModel.kt`, `InstalledAppsSource.kt`, `AppIconCache.kt`, `ui/theme/`, `AndroidManifest.xml`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `res/mipmap-*`, `res/drawable/ic_launcher_*`, `res/values-night/`, and the ten existing test files. Confirm in the same pass that nothing reconstructs locks from `slowlock.delay-config` (FR-024, L7, FR-026, FR-027, FR-039, FR-042)
- [X] T049 [P] Confirm no new permission and no new third-party dependency were added: diff `AndroidManifest.xml` and `gradle/libs.versions.toml` against the branch point (FR-039, Constitution II, III)
- [X] T050 [P] Verify SC-013: grep the diff for a `Service`, `WorkManager`, `AlarmManager`, `JobScheduler`, any timer or polling loop, `PowerManager`, and `FLAG_KEEP_SCREEN_ON`. There must be none — the lock list is read on `ON_START` and at no other time, and nothing runs while the app is away (SC-013, Constitution IV)
- [X] T051 Run the final build gate: `./gradlew assembleDebug` and `./gradlew test`, both green, and report the result rather than assuming it
- [ ] T052 Hand `manual-test-plan.md` to the maintainer, state which cases need a device, and **wait**. Do not report the feature complete until they have run it (Constitution, Manual verification)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies
- **Foundational (Phase 2)**: depends on Setup — **blocks every user story**
- **US1 (Phase 3)**: depends on Foundational
- **US2 (Phase 4)**: depends on Foundational **and US1** — it replaces the branch US1 introduces
- **US3 (Phase 5)**: depends on US1 (back needs a destination). Independent of US2, US4, US5
- **US4 (Phase 6)**: depends on US2 (a row to tap)
- **US5 (Phase 7)**: depends on US2 (a row to remove). Independent of US3 and US4
- **Polish (Phase 8)**: depends on every story that is being shipped

### Story Dependency Graph

```text
Foundational (T004–T007)
      │
      ▼
    US1 (T008–T014)  ── MVP half 1, shippable alone
      │        │
      │        └────────────► US3 (T025–T032)
      ▼
    US2 (T015–T024)  ── MVP half 2
      │        │
      ├────────┴──────────► US4 (T033–T037)
      └───────────────────► US5 (T038–T044)
```

### Within Each Story

`LockListTest` before `LockList.kt`, and `LocksViewModelTest` before `LocksViewModel.kt` — both are
constitutionally required coverage and both are cheapest written first. Strings before the screen
that reads them. Screens before the root wiring that calls them.

### Parallel Opportunities

- **Phase 1**: T002 and T003
- **Phase 3**: T008 and T009 (strings and screen are different files; the screen references
  `R.string` ids, so do T008 first if you want the build green between them)
- **Phase 4**: T015, T016 and T019
- **Phase 5**: T025 and T026, then T027, T028 and T029 together — three different screen files, one
  added argument each
- **Phase 7**: T038 alongside T039
- **Phase 8**: T046, T047, T048, T049 and T050 — all read-only checks

Note this is a solo-maintained project; `[P]` marks tasks that *may* be reordered freely, not a
staffing plan.

---

## Parallel Example: User Story 3

```text
# After T025 and T026 land, three independent one-line changes:
Task: "Pass step = 1 and onBack to ScreenHeader in apps/AppListScreen.kt"
Task: "Pass step = 2 to ScreenHeader in delay/DelayConfigScreen.kt"
Task: "Pass step = 3 to ScreenHeader in shortcut/ShortcutConfigScreen.kt"
```

---

## Implementation Strategy

### MVP (US1 + US2)

The spec makes both P1 and says why: the intro is the Locks screen's empty state, so the two are one
decision about what the app opens on. Ship them together.

1. Phase 1 → Phase 2 → Phase 3 → **stop and validate US1 on a fresh install**
2. Phase 4 → **stop and validate US2 with two or three real locks**

### Incremental Delivery After MVP

3. US3 — the smallest and most self-contained increment; the flow gains its counters and its exit
4. US4 — the entry point that turns a lock from a receipt into something editable
5. US5 — last, and the one whose **wording** is the deliverable rather than the deletion

Each stops at a checkpoint that can be validated on a device without the next one existing.

---

## Notes

- Commit after each task or logical group.
- `[P]` = different file, no dependency on an incomplete task.
- Four tasks (T013, T023, T036, T043) are **verifications that something already holds**. If they
  need code, something earlier was built wrong — fix it there rather than patching at the
  checkpoint.
- The two new screens' metrics are **derived** from 004's frozen tokens, not measured off the
  artboards (research R8). SC-007 in T045 is the check that settles them; a correction goes into
  `contracts/locks-screen.md`, never into `004/contracts/design-tokens.md`.
- No instrumented test. No agent drives the device. State which cases need running, and wait.
