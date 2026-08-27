# Implementation Plan: Constitution Alignment Refactor

**Branch**: `main` (no feature branch; Principle VII reserves branch creation to the maintainer) | **Date**: 2026-08-26 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/009-constitution-alignment/spec.md`

## Summary

Bring the existing application code into conformance with constitution v2.1.0 without changing
anything a user can observe. The work runs in four ordered stages: **upgrade the toolchain first**
(so the structural work is written against APIs that will still be there afterwards, and because
the chosen injection mechanism cannot run on the current language version), then **introduce layer
boundaries and Hilt**, then **rearrange all four capabilities into feature-first packages**, then
**settle state, asynchrony and the test suite**. Every stage leaves the build green and the app
behaving identically; the maintainer verifies on device at two gates and commits.

The technical approach is constrained more by what must *not* move than by what must. Four values
are frozen into launchers and onto users' disks — the pinned entry point's fully-qualified name,
two `SharedPreferences` file names and their keys, the shortcut ID scheme, and the intent extra
name. The package layout below is designed around them.

## Technical Context

**Language/Version**: Kotlin **2.3.21** with the matching `compose-compiler-gradle-plugin`,
JVM target 11, JDK 17 toolchain. Confirmed by the maintainer on 2026-08-26; the spec's
Clarifications were corrected to match. See research R2 for the evidence.

**Primary Dependencies**: Jetpack Compose (BOM 2026.08.00), Material 3, androidx.core-ktx 1.19.0,
androidx.lifecycle 2.11.0, androidx.activity-compose 1.13.0, Hilt 2.60.1,
androidx.hilt:hilt-lifecycle-viewmodel-compose 1.4.0, KSP 2.3.11, kotlinx-coroutines 1.11.0

**Storage**: two `SharedPreferences` files, both frozen — `slowlock.delay-config` and the locks
order file. No database, no network, no backend.

**Testing**: JVM unit tests only (`./gradlew test`), JUnit4 4.13.2 plus kotlinx-coroutines-test
1.11.0. Instrumented suites are prohibited by the constitution; device behaviour is verified
manually by the maintainer.

**Target Platform**: Android, `minSdk 26`, `targetSdk`/`compileSdk` 37 (unchanged by this work)

**Project Type**: Single-module Android application (`:app`)

**Performance Goals**: No numeric budget. FR-001b's qualitative bar: tapping a pinned icon still
shows the wait screen with no perceptible pause, and the wait still ends at the configured moment.

**Constraints**: Zero user-visible change except approved library-default rendering differences
(FR-001a). Four frozen values must survive byte-identical. Battery cost at rest stays zero: no
services, no polling, no wake locks.

**Scale/Scope**: ~40 Kotlin source files, ~6,000 lines, 13 test files, 4 capabilities, 2 entry
points, 6 screens.

## Constitution Check

*GATE: evaluated before Phase 0 research; re-evaluated after Phase 1 design. Both passes recorded.*

| Principle | Pre-Phase 0 | Post-Phase 1 | Notes |
|---|---|---|---|
| I. Modern Stack, Current Tooling | PASS | PASS | Stage 1 is this principle. All versions move to latest stable, all declared in `gradle/libs.versions.toml`, Compose via BOM. Two new dependencies (Hilt, KSP) recorded below with what breaks without them. Dead instrumented-test config removed. |
| II. Layered Architecture | PASS | PASS (2 deviations) | Stages 2-3 are this principle. Repository interfaces in `domain`, implementations in `data`, constructor injection via Hilt everywhere. Deviations: shared repository *implementations* land in `core/data`; `DelayConfigScreen` reaches two repositories from the composable because it has no state to hold — both in Complexity Tracking. |
| III. Feature First, Layers Inside | PASS | PASS | This feature is the "separate, separately-approved task" the principle requires. Two entry points stay outside layer subpackages, which the principle explicitly sanctions ("Application entry points sit at the root package or in the feature they serve"). |
| IV. Structured Concurrency | PASS | PASS (1 deviation) | Dispatchers injected via qualifiers, one-shot events moved off `StateFlow` sentinels onto `Channel`/`receiveAsFlow`, `collectAsStateWithLifecycle` already in use and stays. No `GlobalScope`, `runBlocking` or blocking I/O exists today or is introduced; every surviving `runCatching` wraps a non-suspending platform call, so no `CancellationException` is swallowed. Deviation: `ShortcutConfigScreen` has two state owners, the treatment selection being the second — in Complexity Tracking. |
| V. SOLID / SoC / SSoT / KISS | PASS | PASS (1 deviation) | Deviation: adopting a DI framework in a 4-capability single-module app is a KISS question the maintainer has decided — see Complexity Tracking. Otherwise: no forwarding-only use cases, no one-implementation interfaces without a seam, `DelayConfigScreen` deliberately gets no state holder. |
| VI. Tests That Earn Their Keep | PASS | PASS | Mandated coverage (schedule/time-window, target resolution incl. the null path, every frozen persisted value against a literal) is preserved through the move. Injected-lambda test seams are replaced by injected repositories, coverage carried across. |
| VII. Version Control Is the Maintainer's | PASS | PASS | No branch created for this feature. No commit, push or tag will be performed. Work is left in the working tree and offered. Any `tasks.md` entry that says to commit is a note to the maintainer. |

**Gate result: PASS.** Complexity Tracking carries five entries, each naming the simpler
alternative and why it was rejected: four are deviations (Hilt, `core/data`, `DelayConfigScreen`'s
absent state holder, `ShortcutConfigScreen`'s second state owner) and the fifth — the navigation
stage staying in `rememberSaveable` — is recorded because it reads like one, though FR-023a
sanctions it outright. The Kotlin version question raised by Phase 0 research was put to the
maintainer and settled at 2.3.21 on 2026-08-26.

**Re-evaluated after implementation (T080).** The rows below are what actually survived into the
code, not what was anticipated. The two added after the fact — `DelayConfigScreen` and the
treatment selection — are both cases where the structurally tidy answer would have been a
forwarding-only indirection (FR-045) or a state holder whose lifetime is wrong for the specified
behaviour. Neither was introduced. The treatment selection is also open as finding F-05.

## Project Structure

### Documentation (this feature)

```text
specs/009-constitution-alignment/
├── plan.md              # This file
├── research.md          # Phase 0 output — 14 decisions
├── data-model.md        # Phase 1 output — the target package tree, entity by entity
├── quickstart.md        # Phase 1 output — how to run the stages and verify
├── contracts/
│   ├── frozen-values.md         # The four values that must survive byte-identical
│   ├── repository-interfaces.md # The domain seams every data source is reached through
│   └── injection-graph.md       # What Hilt provides, where each module lives
├── checklists/
│   └── requirements.md  # Spec quality checklist (from /speckit-specify)
└── tasks.md             # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

Target tree after Stage 3. Files not listed keep their current path.

```text
app/src/main/java/com/slowlock/
├── SlowLockApplication.kt          # NEW — @HiltAndroidApp; manifest gains android:name
├── MainActivity.kt                 # @AndroidEntryPoint; entry point, stays at root package
├── SlowLockRoot.kt                 # Root arbiter composable; stage stays rememberSaveable
├── RootViewModel.kt                # NEW — owns the pin-support check and the pre-nav config read
├── core/
│   ├── domain/                     # Cross-feature types; no android.* imports
│   │   ├── DelayConfig.kt          # model + pure parsing/sanitising
│   │   ├── IconTreatment.kt
│   │   ├── AppTarget.kt            # was shortcut/ShortcutTarget.kt (renamed; not frozen)
│   │   ├── DelayConfigRepository.kt
│   │   ├── AppTargetRepository.kt
│   │   └── AppIconRepository.kt
│   └── data/
│       ├── DelayConfigStore.kt     # impl; FROZEN file name + key shapes
│       ├── AppTargetSource.kt      # impl; PackageManager/LauncherApps
│       ├── AppIconCache.kt         # impl
│       ├── PackageCompat.kt        # was compat/PackageCompat.kt
│       └── CoreDataModule.kt       # Hilt bindings + dispatcher qualifiers
├── shortcut/
│   └── ShortcutLaunchActivity.kt   # FROZEN FQN — stays exactly here, @AndroidEntryPoint,
│                                   # outside feature/ because the name outranks the shape
├── feature/
│   ├── apps/
│   │   ├── ui/    AppListScreen.kt, AppListViewModel.kt, AppListUiState.kt
│   │   ├── domain/ InstalledApp.kt, InstalledAppsRepository.kt
│   │   └── data/  InstalledAppsSource.kt, AppsDataModule.kt
│   ├── delay/
│   │   ├── ui/    DelayConfigScreen.kt          # no state holder, deliberately (KISS)
│   │   └── domain/ DelayRange.kt
│   ├── locks/
│   │   ├── ui/    LocksScreen.kt, IntroScreen.kt, LocksViewModel.kt, LocksUiState.kt
│   │   ├── domain/ Lock.kt, LockList.kt, LockOrderRepository.kt, PinnedShortcutsRepository.kt
│   │   └── data/  LockOrderStore.kt, PinnedShortcutsSource.kt, LocksDataModule.kt
│   └── shortcut/
│       ├── ui/    ShortcutConfigScreen.kt, ShortcutConfigViewModel.kt, PinUnsupportedScreen.kt,
│       │          WaitScreen.kt, WaitViewModel.kt
│       ├── domain/ ShortcutContract.kt, PinSupport.kt, WaitTiming.kt,
│       │           ShortcutPinRepository.kt, PinSupportRepository.kt
│       └── data/  ShortcutPinner.kt, PinSupportSource.kt, ShortcutDataModule.kt
└── ui/
    ├── components/  Actions.kt, ScreenHeader.kt, SelectableTile.kt
    └── theme/       Color.kt, Shape.kt, Theme.kt, Type.kt

app/src/test/java/com/slowlock/    # mirrors the tree above, package for package
```

**Structure Decision**: Single `:app` module with feature-first packages and layer subpackages
inside each, exactly as Principle III prescribes. `core` gains a `data` subpackage alongside
`domain` because three capabilities share the delay configuration, app-target resolution and icon
loading, and a shared implementation has nowhere else to live that does not create a
feature-to-feature `data` import (FR-030). Module splitting is not warranted: no layer boundary
here needs a compile-time wall, and Principle V's KISS rule governs what fills the seams.

Two files sit outside a layer subpackage on purpose, and neither is a deviation. `MainActivity`
and `SlowLockRoot` sit at the root package because they belong to no capability — the
constitution's entry-point clause permits exactly that. `ShortcutLaunchActivity` sits directly in
`com.slowlock.shortcut`, outside `feature/` altogether, because its fully-qualified name is written
into the persisted intent of every pinned shortcut in the field; Principle III names this class as
the case where a frozen fully-qualified name outranks the package shape.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| **Hilt** — a DI framework, a KSP code-generation step and a Gradle plugin, in a single-module app with 4 capabilities and roughly 12 injectable collaborators | The maintainer chose it as the project's one declared mechanism (spec, Clarifications). It removes the reflection-found `AndroidViewModel` constructors and the `@JvmOverloads` workarounds that exist only because dependencies were never injected, and it makes the "one declared mechanism, no service locators" rule enforceable by the compiler rather than by review. | Hand-wired constructor injection through a single composition root was offered and not chosen. It would have added no dependency and no build step, which is what Principle V's KISS rule would otherwise indicate at this size. Recorded as a decision taken with the cost understood, not as an oversight. |
| **`core/data`** — shared repository *implementations* in `core`, which the constitution describes as holding "cross-feature domain types and platform compatibility shims" | `DelayConfigStore` is written by `delay` and read by `shortcut` at tap time; `AppIconCache` and `AppTargetSource` are used by three capabilities each. The interfaces belong in `core/domain` uncontroversially, but the implementations must live somewhere that no capability owns. | Duplicating the store into both `feature/delay/data` and `feature/shortcut/data` was rejected: it would put two writers on one frozen preferences file, breaking single-source-of-truth (Principle V) and the store's own "no other class opens this file" obligation. Placing it in `feature/delay/data` and letting `shortcut` import it was rejected as a direct FR-030 violation. |
| **`DelayConfigScreen` has no state holder** and reaches two repositories through `produceState` inside the composable, rather than observing a `ViewModel` (Principle II's "UI observes a state holder") | The screen owns no state and decides nothing: every value it shows is either handed in by the root or read straight through, and the delay it edits lives in the root's `Stage` because back must return it (obligation N2). A `ViewModel` here would hold nothing and forward everything. | A `DelayConfigViewModel` was considered and rejected as exactly the forwarding-only indirection FR-045 prohibits, introduced to satisfy a structural rule rather than a need. The repositories still arrive as parameters, so the seam a test would use is intact; there is simply no state for a holder to own. Recorded because "the composable calls the repository" is the shape Principle II otherwise rules out, and a reader is owed the reason. |
| **`ShortcutConfigScreen` keeps the treatment selection in `rememberSaveable`** rather than in its state holder, so that screen has two state owners rather than one (FR-036) | The selection's *lifetime* is specified behaviour: the root drops the screen's `SaveableStateHolder` entry on every exit from the flow, which is what stops an abandoned choice reappearing the next time the screen opens for a different app (root N3). `rememberSaveable` inside that holder delivers exactly that — survives rotation and process death, dies when the flow is left. | Moving it into `ShortcutConfigViewModel` was rejected: `hiltViewModel()` there scopes to the Activity's store, which outlives the exit and would carry the abandoned treatment forward into the next app configured. Keying a reset on the package name was rejected too — it would still lose the user's choice across process death, where the saveable keeps it. FR-023a is the precedent this follows. Raised as finding F-05; ruling pending. |
| **`SlowLockRoot` keeps the navigation `stage` in `rememberSaveable`**, so the arbiter holds presentation state in composition rather than in `RootViewModel` | FR-023a sanctions this explicitly, so it is **not** a constitutional deviation — it is recorded here because it reads like one. `rememberSaveable` is already the mechanism delivering the process-death restore, the scroll and query retention across the round trip, and the rule that back returns to whichever screen the flow was entered from. | Moving the stage into a `SavedStateHandle` was rejected: it would put all three specified behaviours at risk and gain no principle. The declaration site carries an inline note saying so, because a future reader tidying the last `remember` out of that file is the exact person who would undo it (T071, research R9). |

### Resolved during planning

**Kotlin 2.3.21, not 2.4.10 — confirmed by the maintainer, 2026-08-26.** Phase 0 research found
that the newest KSP release, 2.3.11, is built against Kotlin 2.3.20 and Hilt 2.60.1 against Kotlin
2.3.21, so the toolchain the injection decision requires has no release on the Kotlin 2.4 line.
2.3.21 is a stable release on a maintained line and satisfies both Principle I and the "latest
stable only, no prereleases" instruction. The spec's Clarifications were corrected to match rather
than left contradicting this plan. Not recorded as a constitutional deviation: nothing in
Principle I asks for the newest possible Kotlin, only for a maintained line. See research.md R2.

**Consequence to watch**: when KSP publishes on the 2.4 line, moving Kotlin forward becomes an
ordinary maintenance bump. Nothing in this feature's design pins the project to 2.3.
