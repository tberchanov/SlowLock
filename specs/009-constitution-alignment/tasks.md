---

description: "Task list for the Constitution Alignment Refactor"
---

# Tasks: Constitution Alignment Refactor

**Input**: Design documents from `/specs/009-constitution-alignment/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: This feature's test tasks are not TDD scaffolding — they are the constitution's mandated
coverage (Principle VI) plus the frozen-value literal assertions that make the refactor safe. They
are required, not optional.

**Organization**: grouped by user story. **Unlike a normal feature, these stories are strictly
sequential** — see Dependencies.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to
- **[MAINTAINER]**: The maintainer performs this on a device. The agent does not drive the device
  and does not pre-verify (constitution, Development Workflow). It states the case and waits.

## Path Conventions

Single Android module. Sources at `app/src/main/java/com/slowlock/`, tests at
`app/src/test/java/com/slowlock/`, build config at `app/build.gradle.kts` and
`gradle/libs.versions.toml`.

**File-creation rule for Phases 4–5**: new files are created at their **final** target path from
[data-model.md](./data-model.md). Existing files are modified in place during Phase 4 and moved in
Phase 5, which is what keeps Phase 5 a pure move (FR-035).

---

## Phase 1: Setup

**Purpose**: establish the green reference and the documents the later gates check against.

- [X] T001 Confirm the reference build is green on unmodified `main`: run `./gradlew assembleDebug` and `./gradlew test` from the repository root and record both outcomes
- [X] T002 [P] Create the feature's manual test plan at `specs/009-constitution-alignment/manual-test-plan.md` with numbered cases traceable to FR-001 through FR-005 and FR-001b, marking device-required cases
- [X] T003 [P] Audit each frozen value F1–F4 in `specs/009-constitution-alignment/contracts/frozen-values.md` against current source and record which already carry a literal assertion and which do not

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: make "nothing changed" checkable before anything changes. This is User Story 1's
groundwork — the acceptance bar every later phase is judged against.

**⚠️ CRITICAL**: no phase below may begin until T008 is complete.

**Known gap found during planning**: the delay-config preferences file name `slowlock.delay-config`
is a `private const val` inside `DelayConfigStore.kt` and therefore has no literal assertion,
contrary to Principle VI's requirement that every frozen persisted value be asserted against a
literal. T004–T005 close it. This is a test addition, not a behaviour change.

- [X] T004 [US1] Move the preferences file name from the private constant in `app/src/main/java/com/slowlock/delay/DelayConfigStore.kt` to an `internal const val` beside `delayKey`/`treatmentKey` in `app/src/main/java/com/slowlock/delay/DelayConfig.kt`, and reference it from the store — value unchanged, behaviour unchanged. This source edit legitimately precedes the upgrade under FR-013a: it is the minimum visibility change needed to make a frozen value reachable from a test
- [X] T005 [US1] Assert the delay-config file name against the literal `"slowlock.delay-config"` in `app/src/test/java/com/slowlock/delay/DelayConfigTest.kt`
- [X] T006 [P] [US1] Assert the `IconTreatment` constant names against the literals `"Original"`, `"Invert"`, `"Gray"` in `app/src/test/java/com/slowlock/shortcut/IconTreatmentTest.kt` — they are the persisted treatment token (contracts/frozen-values.md F2)
- [X] T007 [P] [US1] Assert `LOCKS_SEPARATOR` against the literal `"\n"` in `app/src/test/java/com/slowlock/locks/LockListTest.kt` if not already covered — **already covered**: `LockListTest.store file, key and separator are frozen` asserts all three of F3's values against literals. No new assertion added (see `frozen-value-audit.md`)
- [X] T008 [US1] [MAINTAINER] Capture the behaviour baseline per `specs/009-constitution-alignment/quickstart.md` — install current `main`, create two locks with different delays and treatments, pin both, screenshot every screen, and keep the install as the final gate's in-place-update fixture (FR-008, R14)

**Checkpoint**: the frozen values are all guarded by literal assertions and a device baseline
exists. Stage work can begin.

---

## Phase 3: User Story 2 - The refactor targets current APIs (Priority: P2) 🎯 First increment

**Goal**: the build sits on current maintained versions and the injection mechanism is wired,
before a single structural change is made.

**Independent Test**: apply only the version and plumbing changes, build, run the suite, run the
smoke pass. Nothing a user can see differs.

**⚠️ FR-053b**: this phase contains **no structural change of any kind**. It must stay bisectable
on its own, because the smoke pass is not a full regression run.

- [X] T009 [US2] Update `gradle/libs.versions.toml`: `kotlin` 2.2.10 → 2.3.21, `agp` 9.3.1 → 9.3.2, `coreKtx` 1.10.1 → 1.19.0, `lifecycleRuntimeKtx` 2.6.1 → 2.11.0, `activityCompose` 1.8.0 → 1.13.0, `composeBom` 2026.02.01 → 2026.08.00
- [X] T010 [US2] Add new catalog entries to `gradle/libs.versions.toml`: `ksp` 2.3.11, `hilt` 2.60.1, `androidxHilt` 1.4.0, `coroutines` 1.11.0, with library aliases for `hilt-android`, `hilt-android-compiler`, `androidx-hilt-lifecycle-viewmodel-compose`, `kotlinx-coroutines-core`, `kotlinx-coroutines-test`, and plugin aliases for `ksp` and `hilt`
- [X] T011 [US2] Register the KSP and Hilt Gradle plugins `apply false` in `build.gradle.kts` and apply them in `app/build.gradle.kts`
- [X] T012 [US2] Add the runtime and processor dependencies in `app/build.gradle.kts`: `implementation(libs.hilt.android)`, `ksp(libs.hilt.android.compiler)`, `implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)`, `implementation(libs.kotlinx.coroutines.core)`, `testImplementation(libs.kotlinx.coroutines.test)`
- [X] T013 [US2] Remove the dead instrumented-test configuration from `app/build.gradle.kts`: the `testInstrumentationRunner` line and the `androidTestImplementation`/`debugImplementation(libs.androidx.compose.ui.test.manifest)` entries, plus their now-unused catalog aliases in `gradle/libs.versions.toml` (FR-016)
- [X] T014 [US2] Create `app/src/main/java/com/slowlock/SlowLockApplication.kt` annotated `@HiltAndroidApp`, holding nothing else
- [X] T015 [US2] Add `android:name=".SlowLockApplication"` to the `<application>` tag in `app/src/main/AndroidManifest.xml`, leaving every other attribute untouched
- [X] T016 [P] [US2] Annotate `app/src/main/java/com/slowlock/MainActivity.kt` with `@AndroidEntryPoint`
- [X] T017 [P] [US2] Annotate `app/src/main/java/com/slowlock/shortcut/ShortcutLaunchActivity.kt` with `@AndroidEntryPoint` — do not move or rename the file (contracts/frozen-values.md F1)
- [X] T018 [US2] Run `./gradlew assembleDebug` and `./gradlew test`; resolve any API break introduced by the version moves without changing behaviour — **passed first attempt; no API break to resolve.** KSP2 (`kspDebugKotlin`) and Hilt (`hiltAggregateDepsDebug`, `hiltJavaCompileDebug`) both ran clean. One pre-existing pattern newly flagged by Kotlin 2.3.21 (`SlowLockPaletteTest.kt:69`, unreachable `fail(...) as Nothing`) recorded as finding F-02 and left in place per FR-011
- [X] T019 [US2] Run `./gradlew assembleDebug` a second time and confirm the configuration cache is still served; if Hilt's plugin or KSP2 breaks it, record it as a finding rather than disabling the cache in `gradle.properties` (R13) — **configuration cache confirmed reused** (`Configuration cache entry reused`, third run of the same task set). R13's risk did not materialise: neither Hilt's Gradle plugin nor KSP2 breaks it, and `gradle.properties` is untouched. Note that `assembleDebug test` and `assembleDebug` key *different* cache entries, so the first single-task run legitimately stores rather than reuses
- [X] T020 [US2] Verify `-keepnames class com.slowlock.shortcut.ShortcutLaunchActivity` in `app/src/main/keepRules/rules.keep` still matches, and confirm against a release build's mapping output that the class is not renamed — **verified against the release mapping.** `com.slowlock.shortcut.ShortcutLaunchActivity -> com.slowlock.shortcut.ShortcutLaunchActivity:` (identity), while every other class in the package is obfuscated (`IconTreatment -> o60`, `ShortcutPinner -> f81`), which proves the keep rule is doing the work rather than R8 declining to obfuscate. Keep rule present exactly once
- [X] T021 [US2] [MAINTAINER] Run the smoke pass defined in FR-018 and `specs/009-constitution-alignment/quickstart.md`; record any rendering difference for a ruling under FR-001a and judge the pinned-icon tap against FR-001b — **passed**: no perceptible pause on the pinned-icon tap (FR-001b, SC-015), no rendering difference raised for an FR-001a ruling

**Checkpoint**: toolchain current, injection plumbed, nothing structural touched. The upgrade is
independently bisectable from here on.

---

## Phase 4: User Story 3 - Layer boundaries make the code testable (Priority: P3)

**Goal**: every external data source sits behind a domain interface, every collaborator arrives
through a constructor, and no composable or activity touches data or the platform.

**Independent Test**: substitute a test double for any persisted or platform-backed value through
a constructor in a plain JVM test, with no device and no reflection. Then confirm the three grep
gates in `quickstart.md` return nothing.

**Note**: files stay at their current paths in this phase. New files are created at their final
target path from `data-model.md`.

### Domain interfaces

- [X] T022 [P] [US3] Create the `@IoDispatcher` and `@DefaultDispatcher` qualifiers in `app/src/main/java/com/slowlock/core/domain/Dispatchers.kt`
- [X] T023 [P] [US3] Create `DelayConfigRepository` in `app/src/main/java/com/slowlock/core/domain/DelayConfigRepository.kt` per contracts/repository-interfaces.md
- [X] T024 [P] [US3] Create `AppTargetRepository` in `app/src/main/java/com/slowlock/core/domain/AppTargetRepository.kt`
- [X] T025 [P] [US3] Create `AppIconRepository` in `app/src/main/java/com/slowlock/core/domain/AppIconRepository.kt`
- [X] T026 [P] [US3] Create `InstalledAppsRepository` in `app/src/main/java/com/slowlock/feature/apps/domain/InstalledAppsRepository.kt`
- [X] T027 [P] [US3] Create `LockOrderRepository` in `app/src/main/java/com/slowlock/feature/locks/domain/LockOrderRepository.kt`
- [X] T028 [P] [US3] Create `PinnedShortcutsRepository` in `app/src/main/java/com/slowlock/feature/locks/domain/PinnedShortcutsRepository.kt`, preserving the `null`-means-"could not ask" contract in its documentation
- [X] T029 [P] [US3] Create `PinSupportRepository` in `app/src/main/java/com/slowlock/feature/shortcut/domain/PinSupportRepository.kt`
- [X] T030 [P] [US3] Create `ShortcutPinRepository` in `app/src/main/java/com/slowlock/feature/shortcut/domain/ShortcutPinRepository.kt`

### Implementations, in place

- [X] T031 [US3] Make `app/src/main/java/com/slowlock/delay/DelayConfigStore.kt` implement `DelayConfigRepository` with a constructor-injected `@IoDispatcher`, replacing `withContext(Dispatchers.IO)`; frozen file name and key shapes unchanged
- [X] T032 [US3] Split `app/src/main/java/com/slowlock/shortcut/ShortcutTarget.kt` per FR-033: extract the `PackageManager`/`LauncherApps` resolution into a new `app/src/main/java/com/slowlock/core/data/AppTargetSource.kt` implementing `AppTargetRepository`, leaving the data class in place for Phase 5
- [X] T033 [US3] Make `app/src/main/java/com/slowlock/apps/AppIconCache.kt` implement `AppIconRepository` with an injected dispatcher
- [X] T034 [US3] Make `app/src/main/java/com/slowlock/apps/InstalledAppsSource.kt` implement `InstalledAppsRepository` with an injected dispatcher
- [X] T035 [US3] Make `app/src/main/java/com/slowlock/locks/LockStore.kt` implement `LockOrderRepository` with an injected dispatcher; frozen file name and key unchanged
- [X] T036 [US3] Convert the top-level `pinnedShortcutIds` function in `app/src/main/java/com/slowlock/locks/PinnedShortcuts.kt` into a class implementing `PinnedShortcutsRepository`, preserving the direct-boot `IllegalStateException` guard and the `null` return
- [X] T037 [US3] Split `app/src/main/java/com/slowlock/shortcut/PinSupport.kt` per FR-033: move the `Context` overload into a new `app/src/main/java/com/slowlock/feature/shortcut/data/PinSupportSource.kt` implementing `PinSupportRepository`, leaving the sealed type and the pure overload in place
- [X] T038 [US3] Make `app/src/main/java/com/slowlock/shortcut/ShortcutPinner.kt` implement `ShortcutPinRepository` with an injected dispatcher, keeping `isRequestPinShortcutSupported()` gating every attempt

### Hilt modules

- [X] T039 [P] [US3] Create `app/src/main/java/com/slowlock/core/data/CoreDataModule.kt` binding the three core repositories and providing the two dispatcher qualifiers — the only file in the project that may name `Dispatchers.IO` or `Dispatchers.Default`
- [X] T040 [P] [US3] Create `app/src/main/java/com/slowlock/feature/apps/data/AppsDataModule.kt`
- [X] T041 [P] [US3] Create `app/src/main/java/com/slowlock/feature/locks/data/LocksDataModule.kt`
- [X] T042 [P] [US3] Create `app/src/main/java/com/slowlock/feature/shortcut/data/ShortcutDataModule.kt`

### State holders

- [X] T043 [US3] Convert `app/src/main/java/com/slowlock/apps/AppListViewModel.kt` to a plain `@HiltViewModel ViewModel` taking `InstalledAppsRepository`, `AppTargetRepository`, `AppIconRepository` and `SavedStateHandle`; drop `AndroidViewModel`, `@JvmOverloads` and the injected lambdas
- [X] T044 [US3] Convert `app/src/main/java/com/slowlock/locks/LocksViewModel.kt` the same way, replacing its four injected lambdas with `LockOrderRepository`, `PinnedShortcutsRepository`, `DelayConfigRepository`, `AppTargetRepository` and `AppIconRepository`
- [X] T045 [US3] Create `app/src/main/java/com/slowlock/RootViewModel.kt` owning the pin-support check and the pre-navigation delay-config read
- [X] T046 [US3] Create `app/src/main/java/com/slowlock/feature/shortcut/ui/ShortcutConfigViewModel.kt` owning the treatment selection, the pin request and the configuration write
- [X] T047 [US3] Create `app/src/main/java/com/slowlock/feature/shortcut/ui/WaitViewModel.kt` holding the anchor and deadline in a `SavedStateHandle` and owning resolve → read → wait → hand off — **highest-risk task in the feature**, see research R10

### Composables and activity emptied

- [X] T048 [US3] Rework `app/src/main/java/com/slowlock/SlowLockRoot.kt` to consume `RootViewModel`: remove the `remember { DelayConfigStore(context) }` construction and the `pinSupport(context)` call, keep `stage` in `rememberSaveable`, and preserve the `SaveableStateHolder` keys and the `Origin` back-navigation rule exactly (FR-023a, R9)
- [X] T049 [US3] Rework `app/src/main/java/com/slowlock/shortcut/ShortcutConfigScreen.kt` to consume `ShortcutConfigViewModel`, removing the three `remember`-constructed collaborators
- [X] T050 [US3] Rework `app/src/main/java/com/slowlock/delay/DelayConfigScreen.kt` so the icon arrives through `AppIconRepository` rather than a constructed `AppIconCache`; it gains **no** state holder (FR-023, injection-graph.md V4)
- [X] T051 [US3] Rework the per-row icon loading in `app/src/main/java/com/slowlock/apps/AppListScreen.kt` and `app/src/main/java/com/slowlock/locks/LocksScreen.kt` to go through the repository, keeping lazy per-row loading and keeping icons out of UI state
- [X] T052 [US3] Reduce `app/src/main/java/com/slowlock/shortcut/ShortcutLaunchActivity.kt` to window-lifecycle duties only — the `onStop` finish-unless-changing-configurations rule, `onNewIntent` de-duplication and the unavailable toast — delegating everything else to `WaitViewModel`, and carrying the not-`STARTED` race guard across verbatim
- [X] T053 [US3] Update the tests that drove the old lambda seams — `app/src/test/java/com/slowlock/apps/AppListViewModelTest.kt`, `app/src/test/java/com/slowlock/locks/LocksViewModelTest.kt` and `app/src/test/java/com/slowlock/shortcut/ShortcutTargetTest.kt` — to use fake repository implementations, preserving the null-resolution coverage the constitution mandates (FR-050) — `ShortcutTargetTest` and `LocksViewModelTest` needed **no change**: their seams are the lambdas of `resolveTarget` and `assembleLocks`, which are the units under test and still take them. `AppListViewModelTest` was rewritten against fake repositories and gained four assertions; `PinGateTest` follows `pinWhenSupported`'s richer return type
- [X] T054 [US3] Run the three grep gates from `specs/009-constitution-alignment/quickstart.md` and confirm all return nothing, then run `./gradlew assembleDebug` and `./gradlew test` — all three gates **pass on code**. Every other match is prose inside KDoc describing what was removed; gate 3's one code match is `MutableInteractionSource` in `ScreenHeader.kt`, a Compose primitive the regex catches by accident (finding F-11)

**Checkpoint**: every data source is behind a seam, nothing is constructed at a point of use, and
no composable or activity reaches the platform. Files are still in their old packages.

---

## Phase 5: User Story 4 - Everything about one capability in one place (Priority: P4)

**Goal**: the four capabilities each own one directory with layers inside.

**Independent Test**: list each capability's directory and confirm it holds everything the
capability owns and nothing else; confirm no cross-capability `ui`/`data` import remains.

**⚠️ FR-035**: this phase contains **no logic change**. Every task is a move or a rename that the
compiler proves complete.

- [X] T055 [P] [US4] Move `app/src/main/java/com/slowlock/delay/DelayConfig.kt` and `app/src/main/java/com/slowlock/shortcut/IconTreatment.kt` to `app/src/main/java/com/slowlock/core/domain/` — both move whole; `IconTreatment.kt` needs no split, its colour matrices are deliberately literal constants with no `android.*` import
- [X] T056 [US4] Move the `ShortcutTarget` data class to `app/src/main/java/com/slowlock/core/domain/AppTarget.kt`, renaming the type per research R11
- [X] T057 [P] [US4] Move `app/src/main/java/com/slowlock/delay/DelayConfigStore.kt` to `app/src/main/java/com/slowlock/core/data/DelayConfigStore.kt`
- [X] T058 [P] [US4] Move `app/src/main/java/com/slowlock/apps/AppIconCache.kt` to `app/src/main/java/com/slowlock/core/data/AppIconCache.kt`
- [X] T059 [US4] Move `app/src/main/java/com/slowlock/compat/PackageCompat.kt` to `app/src/main/java/com/slowlock/core/data/PackageCompat.kt` and delete the now-empty `compat` package (FR-032)
- [X] T060 [US4] Move the `apps` capability into layers: `AppListScreen.kt`, `AppListViewModel.kt`, `AppListUiState.kt` to `feature/apps/ui/`; `InstalledApp.kt` to `feature/apps/domain/`; `InstalledAppsSource.kt` to `feature/apps/data/`
- [X] T061 [US4] Move the `delay` capability into layers: `DelayConfigScreen.kt` to `feature/delay/ui/`; `DelayRange.kt` to `feature/delay/domain/`
- [X] T062 [US4] Move the `locks` capability into layers: screens, view model and UI state to `feature/locks/ui/`; `Lock.kt` and `LockList.kt` to `feature/locks/domain/`; `LockStore.kt` to `feature/locks/data/LockOrderStore.kt` and `PinnedShortcuts.kt` to `feature/locks/data/PinnedShortcutsSource.kt`, both renamed with their persisted names untouched
- [X] T063 [US4] Move the `shortcut` capability into layers per FR-029a: `WaitScreen.kt` and `WaitTiming.kt` from `delay/` into `feature/shortcut/ui/` and `feature/shortcut/domain/`; config and pin-unsupported screens to `feature/shortcut/ui/`; `ShortcutContract.kt` and the pure `PinSupport.kt` to `feature/shortcut/domain/`; `ShortcutPinner.kt` to `feature/shortcut/data/` — the icon baking is already its `private fun bake`, so nothing is extracted. **`ShortcutLaunchActivity.kt` does not move**
- [X] T064 [US4] Move every file under `app/src/test/java/com/slowlock/` to mirror its subject's new package, changing no assertion (FR-034)
- [X] T065 [US4] Run the three layering gates from `specs/009-constitution-alignment/quickstart.md` against `app/src/main/java/com/slowlock/` and confirm each returns nothing: no `android.*` import inside any `domain` package (FR-025), no `domain` file importing a `ui` or `data` package (FR-026), and no capability importing another capability's `ui` or `data` (FR-030, SC-008) — **all three pass.** Gate A needed its pattern corrected to `^import android\.`: as written it also matches `androidx`, and its only hit was the `ImageBitmap` that `repository-interfaces.md` itself declares (finding F-12). Gate A did catch one real violation carried over from Phase 4 — `Intent` in `core/domain/AppTarget.kt` — fixed and recorded as finding F-13
- [X] T066 [US4] Check every value in `specs/009-constitution-alignment/contracts/frozen-values.md` by hand against the moved source — the compiler cannot catch a constant changed consistently everywhere — **verified by diff against `HEAD`, not by eye.** All nine frozen string literals and the three `IconTreatment` constant names are byte-identical before and after the move, and each literal appears in exactly one main source file
- [X] T067 [US4] Confirm `grep -rn "com.slowlock.compat" app/src` returns nothing and the keep rule in `app/src/main/keepRules/rules.keep` still names the unmoved activity, then run `./gradlew assembleDebug` and `./gradlew test` — `com.slowlock.compat` returns nothing and the directory is gone; the keep rule is present exactly once; a fresh release build confirms `ShortcutLaunchActivity` is still mapped to itself while its new `domain`/`ui`/`data` neighbours are obfuscated

**Checkpoint**: feature-first packaging complete, frozen values verified by hand, suite green.

---

## Phase 6: User Story 5 - One state owner, one asynchrony pattern (Priority: P5)

**Goal**: one-shot events cannot re-fire, each screen has one state owner, cancellation is honest.

**Independent Test**: recreate a screen after a one-shot message and confirm it does not reappear;
confirm no caller clears a flag by hand.

- [X] T068 [US5] Remove `unavailableAppMessage` from `app/src/main/java/com/slowlock/feature/apps/ui/AppListUiState.kt` and replace it with a consume-once event channel in `app/src/main/java/com/slowlock/feature/apps/ui/AppListViewModel.kt`, carrying a string resource id rather than a resolved string (FR-038, R7)
- [X] T069 [US5] Consume the event flow in `app/src/main/java/com/slowlock/feature/apps/ui/AppListScreen.kt`, resolving the same resource so the text is identical, and delete `onUnavailableMessageShown`
- [X] T070 [US5] Move the snackbar messages in `app/src/main/java/com/slowlock/feature/shortcut/ui/ShortcutConfigViewModel.kt` onto the same consume-once mechanism
- [X] T071 [US5] Walk every screen and confirm exactly one state owner each (FR-036); document the root arbiter's navigation-stage exception in `app/src/main/java/com/slowlock/SlowLockRoot.kt` where a future reader would otherwise undo it
- [X] T072 [US5] Confirm no `CancellationException` is swallowed by a broad `catch` and that flow errors are handled with `catch` rather than a `try` around `collect` across `app/src/main/java/com/slowlock/`
- [X] T073 [US5] Run `./gradlew assembleDebug` and `./gradlew test`

**Checkpoint**: state and asynchrony conform; no sentinel one-shots remain.

---

## Phase 7: User Story 6 - No test that would not catch a defect (Priority: P6)

**Goal**: the suite contains only tests that fail when behaviour breaks, and all mandated coverage
survives.

**Independent Test**: break each mandated behaviour deliberately, one at a time, and confirm the
suite goes red each time.

- [X] T074 [US6] Audit every file under `app/src/test/java/com/slowlock/` against FR-048 and remove tests that assert only what a constructor was given, restate the implementation, or cover framework behaviour — **removed two**: `AppTargetTest.the resolved target keeps the package name it was asked about` (asserted only its own input and was subsumed by the full-equality assertion two tests above) and `LockDeriveTest.declining leaves the locks that already existed untouched` (byte-identical call and assertion to `there is no way for the app to hide a pinned lock`; folded its reading into that test's comment). Every other test in the 13 files was read and earns its keep
- [X] T075 [US6] Confirm the constitution's mandated coverage is intact: time-window and boundary evaluation in `app/src/test/java/com/slowlock/feature/shortcut/domain/WaitTimingTest.kt`, target resolution including the null `getLaunchIntentForPackage()` path, and every frozen persisted value asserted against a literal — **intact.** Time-window and boundary evaluation: `WaitTimingTest` plus the new `WaitViewModelTest`. Target resolution including the null path: `AppTargetTest`, `AppListViewModelTest`, `LocksViewModelTest`. Frozen values against literals: F1 `ShortcutContractTest`, F2 `IconTreatmentTest`, F3 `LockListTest`, F4 `DelayConfigTest`
- [X] T076 [US6] Add tests driving injected dispatchers with `kotlinx-coroutines-test` where the dispatcher now matters — at minimum the wait path's deadline behaviour in `app/src/test/java/com/slowlock/feature/shortcut/ui/WaitViewModelTest.kt` — created `app/src/test/java/com/slowlock/feature/shortcut/ui/WaitViewModelTest.kt`: five tests on virtual time covering the withheld hand-off, the unresolvable target, the rotation repeat, the restored deadline and the re-anchored target
- [X] T077 [US6] Remove `testOptions.unitTests.isReturnDefaultValues = true` from `app/build.gradle.kts` if nothing in the suite still needs it, and run `./gradlew test` to confirm — **removed.** Nothing in the suite needed it; 88 tests pass without it. The seven KDocs that cited the flag as their reason for keeping logic pure were reworded to cite the absent framework instead
- [X] T078 [US6] Spot-check SC-013 across `app/src/test/java/com/slowlock/`: for each mandated coverage area, break the behaviour in its source deliberately, confirm `./gradlew test` fails, and restore — **all ten mutations went red**, including an IDE-style consistent `Gray`→`Grey` rename that compiles clean and is caught only by the literal assertions. Working tree restored and re-verified green

**Checkpoint**: every remaining test earns its keep.

---

## Phase 8: Polish, Verification & Hand-off

**Purpose**: close out User Story 1 — the acceptance bar — and hand the work over.

- [X] T079 [P] Extend `specs/009-constitution-alignment/manual-test-plan.md` with cases for the two riskiest changes: the wait's rotation and abandonment behaviour (R10) and the root's scroll/query retention and back-origin rule (R9) — added **M4b** (12 cases, the wait's rotation and abandonment) and **M3b** (13 cases, the root's scroll, query and back-origin) to `manual-test-plan.md`, plus four new traceability rows (FR-023a, FR-038, R9, R10). 68 cases total
- [X] T080 [P] Record every surviving deviation from a constitution rule in the Complexity Tracking table of `specs/009-constitution-alignment/plan.md`, naming the simpler alternative and why it was rejected (FR-054) — Complexity Tracking now carries five rows: the two from planning plus `DelayConfigScreen`'s absent state holder and `ShortcutConfigScreen`'s second state owner, with the navigation stage recorded as sanctioned-but-looks-like-one. The Constitution Check table and gate result were reconciled to match
- [X] T081 [P] Record every finding raised during the refactor that the maintainer has not ruled on in `specs/009-constitution-alignment/findings.md`, left open rather than silently fixed (FR-010, FR-011) — `findings.md` gains **F-14** (a one-shot message no longer survives a rotation — the FR-038 vs FR-001 tension, and the one item here a user can see), **F-15** (T074's two deletions), **F-16** (the `isReturnDefaultValues` removal and the seven KDocs that cited it) and **F-17** (snackbar text resolved through `resources`). F-04 records T074's ruling. **Nothing below F-01 has been acted on**
- [X] T082 Run `./gradlew assembleDebug` and `./gradlew test` one final time and record both outcomes (FR-052) — **both green.** `./gradlew clean assembleDebug test`: BUILD SUCCESSFUL, **88 tests, 0 failures, 0 errors, 0 skipped**
- [ ] T083 [US1] [MAINTAINER] Install the final build **over** the baseline install from T008 — not a fresh install — following the final gate in `specs/009-constitution-alignment/quickstart.md`, and confirm every lock retains its delay and treatment and every pinned icon still launches its target (SC-002)
- [ ] T084 [US1] [MAINTAINER] Run `specs/009-constitution-alignment/manual-test-plan.md` in full
- [ ] T085 [US1] [MAINTAINER] Run the six app-relevant legacy plans in full: `specs/001-installed-apps-list/manual-test-plan.md`, `specs/002-shortcut-pinning/`, `specs/003-launch-delay/`, `specs/004-visual-redesign/`, `specs/005-locks-and-first-run/` and `specs/007-locks-screen-polish/`. The 006 and 008 plans cover the marketing site and are out of scope (FR-053a)
- [X] T086 Summarise what changed across `app/` and `specs/009-constitution-alignment/` and offer the commit. **This task is a note to the maintainer, not an authorization** — the agent leaves the work in the working tree and does not commit, push, branch or tag (Principle VII, FR-055) — summarised to the maintainer. **Nothing was committed, pushed, branched or tagged**; all work is in the working tree

---

## Phase 9: Re-path to the `feature` namespace (constitution v3.0.0)

**Purpose**: absorb the constitution amendment that landed after Phase 8. Principle III's shape
changed from `com.slowlock.<feature>.{ui, domain, data}` to
`com.slowlock.feature.<feature>.{ui, domain, data}`, and the "governs new code only" grandfather
clause was removed. Governance requires work in flight to be re-checked against the new text before
completion, and none of this feature's work is committed, so the re-path belongs here rather than in
a follow-up spec.

**This is a pure move, exactly like Phase 5** — package declarations, imports and document paths
only. No logic changes, no signature changes, no test-content changes (FR-035).

**⚠️ Frozen-value boundary**: `ShortcutLaunchActivity` does **not** move. Its FQN is F1, and
Principle III now states outright that a frozen fully-qualified name outranks the package shape. It
stays at `com.slowlock.shortcut`, outside `feature/` entirely, and so does the R8 keep rule naming
it. `ShortcutContract.LAUNCH_ACTIVITY` and `EXTRA_TARGET_PACKAGE` are string literals that merely
*look* like package paths (F4) and MUST NOT be rewritten either.

- [X] T087 [US4] Move `apps`, `delay` and `locks` and the `ui`/`domain`/`data` subpackages of `shortcut` from `app/src/main/java/com/slowlock/` to `app/src/main/java/com/slowlock/feature/`, leaving `ShortcutLaunchActivity.kt` at `com/slowlock/shortcut/` — moved; `com/slowlock/shortcut/` now holds that one file and nothing else
- [X] T088 [US4] Mirror the same move under `app/src/test/java/com/slowlock/` so every test keeps the package path of its subject (FR-034) — moved; the now-empty `test/.../shortcut/` directory was removed
- [X] T089 [US4] Rewrite `package` and `import` lines across `app/src` for the four capabilities and their layer subpackages, rewriting **only** `com.slowlock.<capability>.<layer>` — 48 files rewritten. The two frozen literals were diffed before and after and are byte-identical
- [X] T090 [US4] Comment the deviation at `ShortcutLaunchActivity`, naming the Principle III clause that permits it (Principle VIII: a deliberate deviation names the requirement it serves) — the existing frozen-FQN KDoc gains one paragraph saying why the class sits outside `feature/`
- [X] T091 [P] [US4] Update the target paths and package names in `data-model.md`, `plan.md` (structure tree and Complexity Tracking), `research.md`, `findings.md`, `contracts/frozen-values.md`, `contracts/injection-graph.md` and `contracts/repository-interfaces.md`. **Pre-refactor paths in the "Current" columns are history and stay as they are** — only post-Phase-5 targets move — and the F1/F4 literals stay untouched
- [X] T092 [P] [US4] Add a Principle III gate to the layering gates in `quickstart.md`: nothing but `core`, `feature`, `shortcut`, `ui` and root-package entry points directly under `com/slowlock/`, and nothing but `ShortcutLaunchActivity.kt` under `shortcut/` — added, and the existing FR-025/FR-026/FR-030 gates were re-pathed through `feature/`
- [X] T093 [US4] Re-run every gate in `quickstart.md`'s Stage 3 block — all clean. The one FR-025 hit, `androidx.compose.ui.graphics.ImageBitmap` in `core/domain/AppIconRepository.kt`, is **F-11, pre-existing and already ruled on**; the amendment neither caused it nor changes it
- [X] T094 [US4] Re-verify the R8 keep rule survives the re-path: `-keepnames class com.slowlock.shortcut.ShortcutLaunchActivity` present exactly once, and a release mapping confirming the class is still mapped to itself while its relocated neighbours are obfuscated (re-doing T020/T067 against the new tree) — **verified.** The keep rule is present exactly once, and a fresh release mapping shows `com.slowlock.shortcut.ShortcutLaunchActivity -> com.slowlock.shortcut.ShortcutLaunchActivity:` (identity) while every relocated neighbour is obfuscated (`feature.shortcut.data.ShortcutPinner -> q81`, `feature.shortcut.ui.WaitViewModel -> fp1`, `core.domain.IconTreatment -> p60`)
- [X] T095 [US4] Re-run `./gradlew assembleDebug` and `./gradlew test`, superseding T082's green — **both green.** `assembleDebug`, `assembleRelease` and `test --rerun-tasks`: BUILD SUCCESSFUL, **88 tests, 0 failures, 0 errors, 0 skipped** — the same 88 as T082, which is what a pure move must produce
- [ ] T096 [US1] [MAINTAINER] T083–T085 were not run before the re-path and are unaffected by it in principle — a pure move cannot change behaviour — but they must still be run against **this** tree, not the Phase 8 one. Nothing here needs re-running twice; run T083–T085 once, now

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: depends on Setup. **Blocks everything** — T008's baseline is what
  every later "unchanged" claim is measured against.
- **Phases 3–7**: strictly sequential. See below.
- **Phase 8**: depends on Phase 7.

### User Story Dependencies — sequential by design, not by accident

The template's usual "stories can proceed in parallel" does **not** apply here. Each story
transforms the same files the next one operates on:

```
US1 (baseline)  ──►  US2 (toolchain)  ──►  US3 (seams)  ──►  US4 (move)  ──►  US5 (settle)  ──►  US6 (tests)
   Phase 2            Phase 3               Phase 4           Phase 5          Phase 6           Phase 7
                                                                                                     │
                                                              US1 (verification) ◄───────────────────┘
                                                                    Phase 8
```

- **US2 before US3** — FR-013 and FR-019: the structural work must be written against the upgraded
  APIs, and Hilt cannot run on the pre-upgrade language version at all.
- **US3 before US4** — a diff that moves a file *and* changes it reads as a rewrite. Separating
  them is what makes both reviewable (research R15).
- **US4 before US5** — settling state in files that are about to move means doing it twice.
- **US6 last** — judging a test is only worth doing once the code under it has stopped moving.
- **US1 spans everything** — its groundwork is Phase 2, its verification is Phase 8.

### Within Each Phase

- Interfaces before implementations, implementations before the modules that bind them, modules
  before the state holders that consume them, state holders before the composables that read them.
- In Phase 5, moves may proceed in any order the compiler tolerates, but T064 (tests) follows the
  sources and T065–T067 (the layering gates, the frozen-value check, the build) come last.

### Parallel Opportunities

- **Phase 1**: T002 and T003 together.
- **Phase 2**: T006 and T007 together, after T004–T005.
- **Phase 3**: T016 and T017 together.
- **Phase 4**: all nine interface tasks T022–T030 together — separate new files, no dependencies.
  Then all four module tasks T039–T042 together.
- **Phase 5**: T055, T057 and T058 together; the four capability moves T060–T063 are large and
  touch imports across the tree, so they are safest one at a time.
- **Phase 8**: T079, T080 and T081 together.

---

## Parallel Example: Phase 4 domain interfaces

```bash
# All nine are new files in different packages with no dependency on each other:
Task: "Create dispatcher qualifiers in core/domain/Dispatchers.kt"
Task: "Create DelayConfigRepository in core/domain/DelayConfigRepository.kt"
Task: "Create AppTargetRepository in core/domain/AppTargetRepository.kt"
Task: "Create AppIconRepository in core/domain/AppIconRepository.kt"
Task: "Create InstalledAppsRepository in feature/apps/domain/InstalledAppsRepository.kt"
Task: "Create LockOrderRepository in feature/locks/domain/LockOrderRepository.kt"
Task: "Create PinnedShortcutsRepository in feature/locks/domain/PinnedShortcutsRepository.kt"
Task: "Create PinSupportRepository in feature/shortcut/domain/PinSupportRepository.kt"
Task: "Create ShortcutPinRepository in feature/shortcut/domain/ShortcutPinRepository.kt"
```

---

## Implementation Strategy

### First increment (User Story 2 only)

1. Phase 1: Setup.
2. Phase 2: Foundational — **T008 is a maintainer task; the agent stops and waits.**
3. Phase 3: the toolchain upgrade.
4. **STOP and VALIDATE**: T021's smoke pass. This is a complete, shippable state — current
   dependencies, injection wired, zero structural change.

### Incremental delivery

Each phase from 4 onward ends green and behaviourally identical, so any of them is a valid stopping
point if the maintainer wants to pause. The one thing that must not be split across a pause is
Phase 4: leaving half the codebase behind seams and half not is the two-styles state FR-029 exists
to avoid.

### Not a parallel-team feature

Every phase rewrites what the next one operates on. Splitting this across developers would produce
merge conflicts on nearly every file. One worker, in order.

---

## Notes

- **[P]** = different files, no dependencies.
- **[MAINTAINER]** tasks are run by the maintainer on a device. The agent states the case and waits;
  it never drives the device and never pre-verifies a manual case.
- Every task ends with the project building and `./gradlew test` passing (FR-051). A task that
  cannot is split until it can.
- A defect found mid-task is recorded and left in place until the maintainer confirms the fix
  (FR-009, FR-010). It is never fixed "while in the file".
- Committing is the maintainer's call — never commit, push, or create a branch unless asked for
  that specific action (constitution, Principle VII). T086 is a note, not an authorization.
