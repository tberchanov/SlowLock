# Implementation Plan: Use Cases Hold the Logic

**Branch**: `main` (no feature branch; Principle VII reserves branch creation to the maintainer) | **Date**: 2026-08-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/011-usecase-migration/spec.md`

## Summary

Constitution v5.0.0 moved where logic is allowed to live. This feature moves the code to match, and
changes nothing a user can see.

Six use cases are added, one per rule that is currently spread across a repository implementation, a
screen holder, or a UI state class. Three repository implementations are stripped back to reading
and writing. Two interfaces change shape. One new seam pair — a package-name qualifier and a locale
interface — exists because two rules that must leave the data layer need two readings that only the
data layer can take.

The technical approach is constrained by what must stay identical rather than by what must move. The
pure functions the rules are made of (`assembleLocks`, `deriveLocks`, `sortedByLabel`,
`resolveTarget`, `deadlineFrom`, `pinSupport`) already exist in `domain` and none is rewritten; what
moves is the *calling* — the code that picks the sources, orders the calls, and chooses between the
answers. That is why this is a relocation with a parity test plan rather than a redesign.

Two questions carried real trade-offs. The spec deferred one — whether the shortcut icon may cross
the repository boundary — and research found the premise did not hold: `AppIconRepository` already
returns `ImageBitmap` across a `core/domain` interface under an obligation that permits it in as
many words, so there is no boundary to trade against (R1). The other was not anticipated: taking the
search filter out of `AppListUiState` collides with Principle V's ban on storing derived state
beside its inputs, and is resolved by deriving the screen's state from the holder's inputs rather
than storing a fourth field (R5).

The work runs in four stages, matching the spec's three user stories plus a closing pass. Stages 1
and 2 are the only ones that could change behaviour, and neither is permitted to.

## Technical Context

**Language/Version**: Kotlin 2.3.21, JVM target 11, JDK 17 toolchain. Unchanged.

**Primary Dependencies**: Compose (BOM 2026.08.00), Material 3, androidx.lifecycle 2.11.0,
navigation-compose 2.10.0, Hilt 2.60.1 with KSP 2.3.11, kotlinx-coroutines 1.11.0,
kotlinx-serialization-core 1.11.0. **Nothing added, nothing moved** — verified in research R12: a
use case needs `javax.inject.Inject` and `kotlinx.coroutines`, both already declared, and
`java.util.Locale` is the JDK.

**Storage**: two `SharedPreferences` files, both frozen, both untouched. `LockOrderStore` loses
`deriveOrder` and gains `saveOrder`, which changes who decides what to write, not what is written or
under which key.

**Testing**: JVM unit tests only (`./gradlew test`), JUnit4 4.13.2 with kotlinx-coroutines-test
1.11.0. Six test files added, one moved, five split. No test drives a device; no instrumented suite
is introduced.

**Target Platform**: Android, `minSdk 26`, `targetSdk`/`compileSdk` 37. Unchanged.

**Project Type**: Single-module Android application (`:app`).

**Performance Goals**: no numeric budget, and no path this feature touches is on a hot loop. Two
costs are accepted and bounded: `LoadLocksUseCase` makes two dispatcher hops where `deriveOrder`
made one (a `getString` and an `apply()`, neither blocking — R4), and `FilterAppsUseCase` runs
inside a `map` on the holder's context, exactly where the `visibleApps` getter ran before, over the
same ~150 entries.

**Constraints**: no user-visible change of any kind (FR-014); four frozen persisted values survive
byte-identical (FR-015); no new dependency, permission or capability (FR-016); no file changes
package (FR-017).

**Scale/Scope**: 59 Kotlin source files, ~5,400 lines; 16 test files, ~1,900 lines. **Added**: 6 use
cases, 4 small types, 1 qualifier, 1 interface with its implementation, 6 test files. **Edited**: 3
repository implementations, 2 repository interfaces, 5 holders, 2 UI state classes, 1 DI module.
**Moved**: 1 test file. **Split**: 6 test files. **Deleted**: no file.

## Constitution Check

*GATE: evaluated before Phase 0 research; re-evaluated after Phase 1 design. Both passes recorded.
Constitution version 5.0.0, dated 2026-08-27 — amended by this feature's own predecessor step, so
this is the first plan evaluated against it.*

| Principle | Pre-Phase 0 | Post-Phase 1 | Notes |
|---|---|---|---|
| I. Modern Stack, Current Tooling | PASS | PASS | No dependency added, no version moved, no coordinate hardcoded. Verified rather than assumed (R12): the six use cases need only `javax.inject` and `kotlinx.coroutines`, both already in `gradle/libs.versions.toml`, and the one new interface returns a JDK type. |
| II. Layered Architecture | PASS | PASS | This feature *is* II's new rules. After it: no repository implementation filters, sorts, merges or reads another repository (B1, B3); decoding stays with the source that owns it (B2); every relocated rule is an injectable class in its feature's `domain` with `operator fun invoke` and constructor-injected collaborators. Holders keep a repository only where the call carries no rule — which is the principle's own clause, not an exemption from it. |
| III. Feature First, Layers Inside | PASS | PASS | No file changes package. Every new file lands in a package that already exists: `feature/{apps,locks,shortcut,delay}/domain`, `core/domain`, `core/data`. No `usecase` subpackage — `domain` is already the address (R2). One file *moves*: `PinGateTest` from `feature/shortcut/data` to `feature/shortcut/domain`, which **fixes** a standing one-file-one-layer defect rather than creating one, because `pinWhenSupported` is a pure decision function declared in a `data` file today. Tests still mirror main package for package. |
| IV. Structured Concurrency | PASS | PASS | No new asynchrony and no new scope. Use cases are `suspend` and call repositories that are already main-safe (O2); none names a dispatcher, which Gate 2 greps for. `stateIn(viewModelScope, …)` in R5 runs in a scope with a real lifecycle. `FilterAppsUseCase` is deliberately not `suspend` — it touches no source — and runs where the getter it replaces ran. `Dispatchers.IO` and `Dispatchers.Default` stay named exactly once, in `CoreDataModule` (D1). |
| V. Standard Solutions, SOLID, SoC, SSoT, KISS | PASS | PASS (2 recorded) | The standard solution wins on mechanism twice: use cases as injectable classes, and UI state derived from internal state rather than stored beside it. Single source of truth strictly improves — R5's whole argument. Recorded below: `CreateLockUseCase`'s five collaborators, and the second state type `AppListInputs`. Both are cases where the compliant answer costs something and a reader is owed the reason. Gate 3 checks the opposite failure: no use case that only forwards. |
| VI. Tests That Earn Their Keep | PASS | PASS | Mandated coverage is preserved and none of its subjects moves (R11): `WaitTimingTest` plus `WaitDecisionUseCaseTest` for schedule and time-window evaluation, `AppTargetTest` for the null launch-intent path, `LockListTest`/`DelayConfigTest`/`ShortcutContractTest` for the frozen literals. Six new test files, each covering obligations from `contracts/use-cases.md`, and no test may assert that a use case called its repository. Gate 4 verifies by mutation rather than by claim. |
| VII. Version Control Is the Maintainer's | PASS | PASS | No branch created, no commit, push or tag. Work is left in the working tree and offered. Any `tasks.md` entry saying to commit is a note to the maintainer and stays unchecked. |
| VIII. Comments That Earn Their Place | PASS | PASS | Eighteen comment sites are made false by this work and are corrected or deleted in the same change (listed below). Two new comments are required, because the code cannot say either reason: why `FilterAppsUseCase` alone is not `suspend`, and why `CreateLockUseCase`'s pin result is deliberately unread. Gate 5 greps for the phrases the false ones share. |

**Gate result: PASS.** Complexity Tracking carries two entries. Neither is a rule left unsatisfied;
both are places where satisfying it costs a shape a reader would otherwise question.

**Re-evaluated after Phase 1 design.** The design added no violation and resolved the spec's one
deferral (R1) by finding the constraint did not exist. It surfaced one requirement the spec did not
anticipate — the own-package name and locale seams (R9) — which is new structure, not a violation:
both follow precedents this codebase already set in `Dispatchers.kt` and `WaitTiming.kt`, and the
choice between a qualifier and an interface is made on the same grounds obligation D4 states.

**Re-evaluated after implementation (T054).** What actually survived, against what was anticipated:

| Principle | Shipped | What differed from the anticipation |
|---|---|---|
| I | PASS | Nothing. No dependency added, no version moved; `gradle/libs.versions.toml` is untouched. |
| II | PASS | Nothing structural. Six use cases, three repositories stripped, two interfaces reshaped. `AppListViewModel`, `ShortcutConfigViewModel` and `DelayConfigViewModel` each still hold a repository, exactly as FR-011 anticipated. |
| III | PASS | One deviation, deliberate: `@OwnPackageName` went into its own `core/domain/OwnPackageName.kt` rather than into `Dispatchers.kt` as the tree above says. That file's own KDoc frames it as being about dispatchers and cites obligation D4 for why; a package-name qualifier inside it would have contradicted the document it sits in. `PinGateTest`'s move to `domain` landed as planned and git recorded it as a rename. |
| IV | PASS | Nothing. No new scope, no dispatcher named outside `CoreDataModule`. `FilterAppsUseCase` is the one non-suspend use case, commented as such. |
| V | PASS (2 recorded) | Both anticipated entries shipped unchanged: `CreateLockUseCase` takes five collaborators, and `AppListInputs` is a second state type. Gate 3 found no use case that only forwards. |
| VI | PASS | 95 tests before, 119 after. Six new test files; `LocksViewModelTest` shrank from 139 lines to 51 as its rule cases moved to `LoadLocksUseCaseTest`; `InstalledAppTest` lost its four `visibleApps` cases to `FilterAppsUseCaseTest`. Two task-level anticipations were wrong — see below. |
| VII | PASS | No branch, no commit, no push, no tag. T056 is left unchecked. |
| VIII | PASS | Three stale comments beyond the eighteen anticipated, all caught by Gates 1, 2 and 5 rather than by inspection — `PinnedShortcutsSource`'s claim that `LockOrderStore` orders the rows, `Lock.kt`'s pointer at `LocksViewModelTest`, and `AppsDataModule`'s "every `load()` re-enumerates and re-collates". Both required new comments were written. |

**Three task-level anticipations were wrong, and were corrected while executing:**

- **T033** planned to split `PinGateTest`, keeping its `pinSupport` cases and moving its `pinWhenSupported` cases. There was nothing to split: all four cases drive `pinWhenSupported`, and `pinSupport` appears only as an argument to it. The file moved wholesale.
- **T034** planned to move `WaitViewModelTest`'s rule-asserting cases to `WaitDecisionUseCaseTest`. None moved. Each of the five asserts holder behaviour over virtual time — that the hand-off is *withheld*, that a repeat tap does not extend, that a new holder resumes — which the use case cannot see. `WaitDecisionUseCaseTest` instead asserts what only became reachable: that the configuration is not consulted when a deadline is restored, proved by a throwing fake.
- **T053's "exactly one case turns red"** was too strict. All six mutations turned something red, which is the failure the gate exists to catch. Three turned one; three turned two, each a genuinely independent behaviour resting on the same branch (a use case case plus a holder case, or two distinct properties). None was the "cases restating the implementation" pattern the criterion was aimed at.

**Mutation results (T053), for the record:**

| Inverted branch | Red |
|---|---|
| U2 — cache the locale | 1 · `LoadInstalledAppsUseCaseTest.reads the locale on every invocation` |
| U9 — always write the order | 1 · `LoadLocksUseCaseTest.an unchanged order is not written back` |
| U13 — recompute the deadline | 2 · `WaitDecisionUseCaseTest` × 2 |
| U17 — pin before saving | 1 · `CreateLockUseCaseTest.the configuration is written before the launcher is asked` |
| U18 — pin while support is Unknown | 2 · `PinGateTest`, `CreateLockUseCaseTest` |
| U19 — prefer the stored delay | 2 · `LoadDelayConfigUseCaseTest`, `DelayConfigViewModelTest` |

### Comment sites made false (FR-021, Principle VIII)

Eighteen, in the change that falsifies each:

`InstalledAppsSource` class KDoc and `load()` KDoc · `InstalledAppsRepository`'s obligations list ·
`AppsDataModule.installedAppsRepository`'s "re-enumerates and re-collates" · `LockOrderStore` class
KDoc and `deriveOrder` KDoc · `LockOrderRepository.deriveOrder` KDoc · `LocksViewModel` class KDoc
and the derivation comments inside `refresh()` · `AppListViewModel.onQueryChanged`'s "the filtering
itself is derived in `AppListUiState.visibleApps`" · `AppListUiState` class KDoc and the
`visibleApps` KDoc · `ShortcutPinRepository` class KDoc ("the source icon is loaded by the
implementation rather than handed in") and its `requestPin` KDoc · `ShortcutPinner` class KDoc,
`pinWhenSupported` KDoc and `requestPin` KDoc · `ShortcutConfigViewModel.create` KDoc ·
`WaitViewModel` class KDoc's four rules · `DelayConfigViewModel.start` KDoc.

## Project Structure

### Documentation (this feature)

```text
specs/011-usecase-migration/
├── plan.md                       # This file
├── spec.md                       # The approved specification
├── research.md                   # Phase 0 — 12 decisions, each with what was rejected
├── data-model.md                 # Phase 1 — use cases, types, interface changes, deletions
├── quickstart.md                 # Phase 1 — the four stages, five gates, 14 manual cases
├── contracts/
│   ├── use-cases.md              # Obligations U1-U19, and what no test may assert
│   └── layer-boundaries.md       # Obligations B1-B12, and how each is checked
├── checklists/
│   └── requirements.md           # Spec quality checklist (from /speckit-specify)
└── tasks.md                      # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

Only the deltas. Every file not listed is unchanged, and **no file moves except one test**.

```text
app/src/main/java/com/slowlock/
├── core/
│   ├── domain/
│   │   ├── Dispatchers.kt              EDIT   + @OwnPackageName qualifier
│   │   └── CurrentLocale.kt            NEW    fun interface { fun now(): Locale }
│   └── data/
│       ├── CurrentLocaleSource.kt      NEW    reads configuration.locales
│       └── CoreDataModule.kt           EDIT   + @Binds CurrentLocale, + @Provides @OwnPackageName
├── feature/apps/
│   ├── domain/
│   │   ├── LoadInstalledAppsUseCase.kt NEW
│   │   ├── FilterAppsUseCase.kt        NEW
│   │   └── InstalledAppsRepository.kt  EDIT   contract inverts: raw enumeration
│   ├── data/InstalledAppsSource.kt     EDIT   filter/dedupe/sort and currentLocale() removed
│   └── ui/
│       ├── AppListViewModel.kt         EDIT   inputs flow + derived uiState (R5)
│       └── AppListUiState.kt           EDIT   visibleApps becomes a parameter; + AppListInputs
├── feature/locks/
│   ├── domain/
│   │   ├── LoadLocksUseCase.kt         NEW
│   │   └── LockOrderRepository.kt      EDIT   deriveOrder → saveOrder
│   ├── data/LockOrderStore.kt          EDIT   merge and write-decision removed
│   └── ui/LocksViewModel.kt            EDIT   refresh() reduced to one call
├── feature/shortcut/
│   ├── domain/
│   │   ├── WaitDecisionUseCase.kt      NEW    + WaitDecision
│   │   ├── CreateLockUseCase.kt        NEW    + CreateLockResult, + pinWhenSupported (moved in)
│   │   └── ShortcutPinRepository.kt    EDIT   icon handed in; returns Unit
│   ├── data/ShortcutPinner.kt          EDIT   two repository params and the gate removed
│   └── ui/
│       ├── WaitViewModel.kt            EDIT   run() reduced to handle reads + call + delay()
│       └── ShortcutConfigViewModel.kt  EDIT   create() reduced to call + result mapping
└── feature/delay/
    ├── domain/LoadDelayConfigUseCase.kt NEW
    └── ui/DelayConfigViewModel.kt       EDIT   edited-wins branch removed

app/src/test/java/com/slowlock/
├── feature/apps/domain/
│   ├── LoadInstalledAppsUseCaseTest.kt  NEW      U1-U3
│   ├── FilterAppsUseCaseTest.kt         NEW      U4-U6 (4 cases moved from InstalledAppTest)
│   └── InstalledAppTest.kt              EDIT     visibleApps cases move out; the rest stays
├── feature/locks/domain/LoadLocksUseCaseTest.kt  NEW      U7-U11
├── feature/shortcut/domain/
│   ├── WaitDecisionUseCaseTest.kt       NEW      U12-U15
│   ├── CreateLockUseCaseTest.kt         NEW      U16-U18
│   └── PinGateTest.kt                   MOVED    from feature/shortcut/data/
├── feature/delay/domain/LoadDelayConfigUseCaseTest.kt  NEW  U19
└── feature/*/ui/*ViewModelTest.kt       EDIT     five split; rule cases move out, wiring stays
```

`CurrentLocaleSource` gets no test: it is one platform read with no branch, and a test over it would
assert the framework (Principle VI). `InstalledAppsSource` keeps none for the same reason once its
rules have left.


**Structure Decision**: unchanged from feature 010 — single `:app` module, feature-first packages
with Principle II's layers as subpackages. This feature adds no package and no module. Use cases sit
flat in each feature's existing `domain`, which Constitution II already names as their layer (R2).

## Complexity Tracking

> Two entries. Both are shapes the compliant answer forces, where a reader would reasonably ask why.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| `CreateLockUseCase` takes five collaborators — more than any other class in the project | It holds one coherent rule with one ordered chain: re-resolve, write, gate, load icon, pin (U16-U18). FR-006 requires the ordering to be a stated obligation rather than an incidental line order, and FR-009 requires the gate and the icon read to leave `ShortcutPinner`. All five are needed to state it once. | Splitting out a `PinShortcutUseCase` for the gate and the icon would give it exactly one caller — this one. Principle V inlines an abstraction with one implementation and no seam justifying it, and the seam a test needs is `CreateLockUseCase`'s own constructor (R7). Two use cases would also split the write-before-pin ordering across two files, which is the one thing FR-006 exists to keep together. |
| `AppListInputs` — a second state type for one screen, where every other screen has one | Principle V forbids storing derived state beside its inputs and syncing it by hand. Taking the filter out of `AppListUiState` (FR-008) leaves `visibleApps` needing a home that is recomputed with every state it appears in. Splitting stored inputs from derived output is what makes that structural (R5). | Storing `visibleApps` as a fourth field recomputed inside every `update` is the parallel copy the principle names: a later third call site can set `apps` without it and nothing fails. Filtering in the composable puts a rule in a composable (Principle II) and re-filters per recomposition. Keeping the getter and injecting the use case into the state class leaves the filtering in the state class, so FR-008 fails outright. |

## Phase 2

Not produced here. `/speckit-tasks` generates `tasks.md` from the four stages in
[quickstart.md](./quickstart.md), the obligations in `contracts/`, and the file deltas above.
