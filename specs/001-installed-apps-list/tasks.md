---

description: "Task list for Installed Applications List"
---

# Tasks: Installed Applications List

**Input**: Design documents from `/specs/001-installed-apps-list/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Testing approach**: **Manual-first.** This feature is verified by hand against
`manual-test-plan.md`. Automated tests are limited to six assertions in two JVM unit test
files, each covering logic that cannot be observed on screen — silent sorting/dedup bugs,
cache staleness that only appears after a real app update, and the null
`getLaunchIntentForPackage()` path that Constitution §"Testing expectations" requires as a
unit test. There are **no Compose UI tests and no `connectedAndroidTest`**: the
constitution explicitly permits pure-Compose presentation without branching logic to ship
untested, and every display state is covered by the manual plan.

**Organization**: Tasks are grouped by user story so each is independently implementable
and verifiable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Exact file paths are included in every task

## Path Conventions

Single `:app` Gradle module (per plan.md Structure Decision):

- Main source: `app/src/main/java/com/slowlock/`
- Unit tests: `app/src/test/java/com/slowlock/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Manifest, dependency, and resource groundwork. No feature logic.

- [X] T001 [P] Add `<queries>` element declaring `ACTION_MAIN` + `CATEGORY_LAUNCHER` as a direct child of `<manifest>` in `app/src/main/AndroidManifest.xml` (FR-015, Constitution III — do NOT add `QUERY_ALL_PACKAGES` or any `<uses-permission>`)
- [X] T002 [P] Add `androidx-lifecycle-viewmodel-compose` and `androidx-lifecycle-runtime-compose` library entries under the existing `lifecycleRuntimeKtx` version ref in `gradle/libs.versions.toml` (per quickstart.md; hardcoded coordinates are prohibited)
- [X] T003 Add the two `implementation(libs.androidx.lifecycle.*)` lines to the dependencies block in `app/build.gradle.kts` (depends on T002)
- [X] T004 [P] Add string resources for the search hint, empty state, no-results state, unavailable-app message, and icon content description in `app/src/main/res/values/strings.xml`
- [X] T005 [P] Delete the Android Studio template leftovers `app/src/test/java/com/slowlock/ExampleUnitTest.kt` and `app/src/androidTest/java/com/slowlock/ExampleInstrumentedTest.kt` so the build gate reports only real tests
- [X] T006 [P] ~~Enable `StrictMode` with `detectDiskReads`/`detectDiskWrites`/`detectNetwork` and `penaltyLog` in a `BuildConfig.DEBUG` guard in `app/src/main/java/com/slowlock/MainActivity.kt`~~ — **reverted during feature 002**: the policy fired on `Context.getCacheDir()` inside `AppIconCache.<init>`, a framework-internal disk touch rather than the app blocking on I/O, so its violations were false positives rather than FR-011 failures. FR-011 is now held structurally — every package-manager and icon call site runs on `Dispatchers.IO` — and verified by responsiveness in manual case T2.4
- [X] T007 Run `./gradlew assembleDebug` to confirm the dependency and manifest changes build clean before any feature code lands (depends on T001, T003, T004)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The data shapes and the wiring every user story depends on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T008 Create package `app/src/main/java/com/slowlock/apps/` and the `InstalledApp` data class (`packageName: String`, `label: String`, `versionCode: Long`) in `app/src/main/java/com/slowlock/apps/InstalledApp.kt` per data-model.md §1 — no icon field, no `ComponentName` field (Constitution V)
- [X] T009 [P] Create `AppListUiState` (`isLoading`, `apps`, `query`, `unavailableAppMessage`) in `app/src/main/java/com/slowlock/apps/AppListUiState.kt` per data-model.md §2, with the four display states derived rather than stored (FR-006)
- [X] T010 Create `AppListViewModel` in `app/src/main/java/com/slowlock/apps/AppListViewModel.kt` as `class AppListViewModel(app: Application, private val savedState: SavedStateHandle, private val resolveLaunchIntent: (String) -> Intent? = { app.packageManager.getLaunchIntentForPackage(it) }) : AndroidViewModel(app)`. Construct `InstalledAppsSource` and `AppIconCache` in `init` from `getApplication()`. Expose `val uiState: StateFlow<AppListUiState>` plus empty `refresh()` / `onQueryChanged()` / `onAppTapped()` stubs. **The `resolveLaunchIntent` lambda is the seam that makes T031's requirement testable without a device** — do not inline the `PackageManager` call (depends on T009)

**Checkpoint**: Data shapes and dependency wiring exist and compile

---

## Phase 3: User Story 1 - Browse launchable apps (Priority: P1) 🎯 MVP

**Goal**: A scrollable, alphabetically ordered list of every launchable app on the current
profile, each row showing the app's own icon and localized label, with SlowLock excluded
and each package appearing exactly once.

**Manual verification**: `manual-test-plan.md` §2.A (T1.1–T1.6) and §3.E–G

### Automated tests for User Story 1 ⚠️

> **Write first, confirm they fail.** Four assertions, one file, no device.

- [X] T011 [P] [US1] Unit tests in `app/src/test/java/com/slowlock/apps/InstalledAppTest.kt` covering exactly: (a) `dedupeByPackage` keeps one entry when a package exposes several launcher activities (FR-004); (b) `excludeSelf` removes SlowLock (FR-003); (c) `sortedByLabel` places a lowercase-initial label ("eBay") among the E's, not after Z (FR-005); (d) `sortedByLabel(Locale.GERMAN)` orders an umlaut label with its base letter (FR-005, collation not `lowercase()`); (e) `iconCacheKey` differs when only `versionCode` changes (FR-012, Constitution V)

### Implementation for User Story 1

- [X] T012 [US1] Add pure `List<InstalledApp>.excludeSelf(ownPackage: String)` and `dedupeByPackage()` to `app/src/main/java/com/slowlock/apps/InstalledApp.kt` per data-model.md construction rules steps 2–3 (FR-003, FR-004)
- [X] T013 [US1] Add pure `sortedByLabel(locale: Locale)` using `Collator.getInstance(locale)` at `Collator.SECONDARY` strength to `app/src/main/java/com/slowlock/apps/InstalledApp.kt` (FR-005, research.md R3 — do not use `String.lowercase()` ordering)
- [X] T014 [US1] Add pure `iconCacheKey(packageName: String, versionCode: Long): String` to `app/src/main/java/com/slowlock/apps/InstalledApp.kt` (Constitution V, FR-012)
- [X] T015 [P] [US1] Create `InstalledAppsSource` in `app/src/main/java/com/slowlock/apps/InstalledAppsSource.kt` — `class InstalledAppsSource(context: Context)` with `suspend fun load(): List<InstalledApp>` on `Dispatchers.IO` calling `LauncherApps.getActivityList(null, Process.myUserHandle())`, reading `longVersionCode` per package, reading the locale from the current configuration at load time, and applying T012/T013 (FR-001, FR-005, FR-011, research.md R1)
- [X] T016 [P] [US1] Create `AppIconCache` memory tier in `app/src/main/java/com/slowlock/apps/AppIconCache.kt` — `LruCache<String, ImageBitmap>` (~40 entries) keyed by T014's `iconCacheKey`, miss path calling `getIcon(densityDpi)` → `toBitmap()` off the main thread (FR-012)
- [X] T017 [US1] Add the disk tier to `app/src/main/java/com/slowlock/apps/AppIconCache.kt` — WebP files at `cacheDir/app-icons/<packageName>-<versionCode>.webp`, written after first rasterization. Expose `suspend fun sweep(installed: List<InstalledApp>)` deleting files whose stem is absent from the passed list, and call it **after** the first successful load rather than at cache construction (FR-012, SC-005; depends on T015, T016)
- [X] T018 [US1] Implement `refresh()` in `app/src/main/java/com/slowlock/apps/AppListViewModel.kt` — launch on `viewModelScope`, call `InstalledAppsSource.load()`, emit `isLoading = false` with the result, then trigger `AppIconCache.sweep()`. A refresh on an already-populated list must not set `isLoading = true` (FR-013, FR-017; depends on T015, T017)
- [X] T019 [US1] Create `AppListScreen` in `app/src/main/java/com/slowlock/apps/AppListScreen.kt` — `Scaffold` + `LazyColumn` keyed by `packageName`, `rememberLazyListState()` for scroll preservation, state via `collectAsStateWithLifecycle()`, `refresh()` on `ON_START` (FR-013, FR-017)
- [X] T020 [US1] Add the `AppRow` composable to `app/src/main/java/com/slowlock/apps/AppListScreen.kt` — fixed row height, fixed 48dp icon box, single-line label with `TextOverflow.Ellipsis` (FR-002; manual case T2.9)
- [X] T021 [US1] Wire per-row icon loading in `app/src/main/java/com/slowlock/apps/AppListScreen.kt` — `LaunchedEffect` keyed by `packageName` reading from `AppIconCache`, cancelled with the row's scope; neutral placeholder until resolved and on failure, row stays selectable (FR-016, SC-003; manual cases T2.11, T1.6; depends on T017, T020)
- [X] T022 [US1] Add the loading and empty display states to `app/src/main/java/com/slowlock/apps/AppListScreen.kt` (FR-006)
- [X] T023 [US1] Replace the `Greeting`/`GreetingPreview` scaffold content in `app/src/main/java/com/slowlock/MainActivity.kt` with `AppListScreen`, passing a no-op `onAppSelected` for now (T035 supplies the real one), and delete the unused `Greeting` composables

**Checkpoint**: Run `manual-test-plan.md` §6 smoke test. US1 is shippable on its own.

---

## Phase 4: User Story 2 - Find a specific app quickly (Priority: P2)

**Goal**: Type-to-filter narrowing, a distinct no-results state, and a clear affordance
that restores the full list in its original order.

**Manual verification**: `manual-test-plan.md` §2.B (T1.7–T1.11)

### Automated tests for User Story 2 ⚠️

- [X] T024 [P] [US2] Add filter assertions to `app/src/test/java/com/slowlock/apps/InstalledAppTest.kt`: mixed-case query matches (FR-007), a mid-name substring matches (FR-007, not prefix-only), blank query returns the full list, and filtering preserves the collated order (FR-008)

### Implementation for User Story 2

- [X] T025 [US2] Add the derived `visibleApps` filter to `app/src/main/java/com/slowlock/apps/AppListUiState.kt` — `contains(query, ignoreCase = true)` over the already-sorted list, blank query returns `apps` untouched, no re-sorting (FR-007, FR-008, research.md R4 — no debounce)
- [X] T026 [US2] Implement `onQueryChanged()` in `app/src/main/java/com/slowlock/apps/AppListViewModel.kt`, backing the query with `SavedStateHandle` so it survives process death (FR-017)
- [X] T027 [US2] Add the search text field to `app/src/main/java/com/slowlock/apps/AppListScreen.kt`, rendering `visibleApps` instead of `apps` in the `LazyColumn` (FR-007)
- [X] T028 [US2] Add the clear-query affordance to the search field in `app/src/main/java/com/slowlock/apps/AppListScreen.kt` (FR-008)
- [X] T029 [US2] Add the no-results state to `app/src/main/java/com/slowlock/apps/AppListScreen.kt` — text naming the query, distinct from the empty state, shown when `apps` is non-empty but `visibleApps` is empty (FR-006)

**Checkpoint**: Manual cases T1.7–T1.11 pass. US1 and US2 both work.

---

## Phase 5: User Story 3 - Open the selected app (Priority: P3)

**Goal**: Tapping a row opens that app immediately — a feasibility proof of the
resolve-and-start mechanism the whole product depends on. **No delay, countdown, or
schedule logic** belongs in this phase (FR-018); that is a separate feature with its own
spec.

**Manual verification**: `manual-test-plan.md` T1.12, T1.13, T1.16, and **T2.8** (the crash
case)

### Automated tests for User Story 3 ⚠️

> Required as a **unit** test by Constitution §"Testing expectations". T010's
> `resolveLaunchIntent` seam is what makes this possible without a device.

- [X] T030 [P] [US3] Unit test in `app/src/test/java/com/slowlock/apps/AppListViewModelTest.kt`: construct the ViewModel with a fake `resolveLaunchIntent` returning `null`, call `onAppTapped`, and assert the hand-off callback is **not** invoked and `unavailableAppMessage` is set (FR-014, hand-off contract obligation P2)

### Implementation for User Story 3

- [X] T031 [US3] Implement `onAppTapped(packageName, onResolved)` in `app/src/main/java/com/slowlock/apps/AppListViewModel.kt` — invoke `onResolved` only when `resolveLaunchIntent(packageName) != null`, handling the null return at the call site (FR-009, FR-014, Constitution IV)
- [X] T032 [US3] Add the unavailable path to `app/src/main/java/com/slowlock/apps/AppListViewModel.kt` — on a null result set `unavailableAppMessage`, drop that entry from `apps`, and expose `onUnavailableMessageShown()` to consume it (FR-014; depends on T031)
- [X] T033 [US3] Add the `onAppSelected: (packageName: String) -> Unit` parameter to `AppListScreen` and wire each row's `onClick` through `viewModel.onAppTapped` in `app/src/main/java/com/slowlock/apps/AppListScreen.kt` — the screen reports the selection and does **not** launch anything itself (FR-009, FR-010, hand-off contract)
- [X] T034 [US3] Add the `SnackbarHost` to `app/src/main/java/com/slowlock/apps/AppListScreen.kt` showing the "no longer available" message, calling `onUnavailableMessageShown()` after display (FR-014)
- [X] T035 [US3] Implement `onAppSelected` in `app/src/main/java/com/slowlock/MainActivity.kt` to **launch the target app**: `getLaunchIntentForPackage(packageName)?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }` then `startActivity`, wrapped in `runCatching` so an `ActivityNotFoundException` from an app uninstalled between resolve and start is reported rather than crashing. No delay, countdown, or schedule check (FR-009, FR-018, FR-014, `contracts/selection-handoff.md`)
- [X] T036 [US3] Confirm the launch call site sits in the tap handler and is reachable only from a foreground, user-initiated tap — Constitution IV forbids background activity starts, and this is the call site the future `DelayActivity` will inherit
- [X] T037 [US3] Verify scroll position and active query survive the tap-and-return round trip in `app/src/main/java/com/slowlock/apps/AppListScreen.kt`, hoisting state as needed. Returning from another app is a real backgrounding round trip, so this now exercises FR-017 more thoroughly than rotation does (manual cases T1.13, T1.16)

**Checkpoint**: All three user stories functional.

---

## Phase 6: Verification & Gates

**Purpose**: The constitution's build gate, then the real verification — the manual plan.

- [X] T038 Run `./gradlew test` — six unit assertions across `InstalledAppTest.kt` and `AppListViewModelTest.kt` must pass (constitution build gate)
- [X] T039 Run `./gradlew assembleDebug` — clean build (constitution build gate)
- [X] T040 Execute **Tier 1** of `specs/001-installed-apps-list/manual-test-plan.md` (T1.1–T1.17) on a prepared device. All must pass
- [X] T041 Execute **Tier 2** of `specs/001-installed-apps-list/manual-test-plan.md` (T2.1–T2.13), including the adb-measured SC-001/SC-003/SC-005 checks
- [X] T042 ~~Check logcat for `StrictMode` violations during T040/T041~~ — **superseded during feature 002** along with T006. FR-011 is verified by the app staying responsive through T040/T041 with no stall or dropped frames, not by a `StrictMode` log
- [X] T043 Execute **Tier 3** of `specs/001-installed-apps-list/manual-test-plan.md` on a non-Pixel OEM device, and record the Xiaomi Dual Apps / Secure Folder result as tested or explicitly untested (Constitution, manual verification requirement)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **User Stories (Phases 3–5)**: Each depends on Foundational. US1 has no dependency on
  US2/US3
- **Verification (Phase 6)**: Depends on all desired stories being complete

### User Story Dependencies

Each story is independently *verifiable* and independently *shippable* — US1 alone is a
working product, and dropping US2 does not invalidate US1. They are not independently
*parallelizable*: all three modify `AppListScreen.kt` and `AppListViewModel.kt`. This is a
solo-maintained single-module app.

### Within Each User Story

- Unit tests first, confirmed failing
- Pure functions → sources → ViewModel → screen
- Complete and manually smoke-test a story before starting the next priority

### Parallel Opportunities

- Setup: T001, T002, T004, T005, T006 are five different files — run together
- Foundational: T009 runs alongside T008
- US1: T015 and T016 are different files — run together
- Phase 6: T039 and T040 are one device session; T042 needs different hardware

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup
2. Phase 2: Foundational (CRITICAL — blocks all stories)
3. Phase 3: User Story 1
4. **STOP and VALIDATE**: `./gradlew test assembleDebug`, then the §6 smoke test in
   `manual-test-plan.md`
5. Demonstrable product: the user can confirm SlowLock sees their apps correctly

### Incremental Delivery

Setup + Foundational → US1 (**MVP**, smoke test) → US2 (T1.7–T1.11) → US3 (T1.12, T2.8) →
Phase 6 full manual sweep

### Requirement Coverage

| Requirement | Implementation | Verified by |
|---|---|---|
| FR-001 enumerate launchable apps | T015 | Manual T1.1, T1.2 |
| FR-002 icon + localized label | T020, T021 | Manual T1.1, T1.6 |
| FR-003 exclude SlowLock | T012 | Unit T011(b), manual T1.3 |
| FR-004 one row per package | T012 | Unit T011(a), manual T1.4 |
| FR-005 collated ordering | T013, T015 | Unit T011(c)(d), manual T1.5, T2.12 |
| FR-006 loading/empty/no-results | T009, T022, T029 | Manual T1.11, T3.3 |
| FR-007 case-insensitive filter | T025, T027 | Unit T024, manual T1.7–T1.9 |
| FR-008 clear query, original order | T025, T028 | Unit T024, manual T1.10 |
| FR-009 tap opens the app | T031, T033, T035 | Manual T1.12 |
| FR-010 stable identifier resolves the launch | T008, T033, T035 | Manual T1.12 |
| FR-018 opens immediately, no delay logic | T035, T036 | Manual T1.16, T1.17 |
| FR-011 nothing blocks main thread | T015, T016, T021 | manual T2.4, T040/T041 responsiveness (StrictMode gate withdrawn — see T006) |
| FR-012 cache keyed pkg+versionCode | T014, T016, T017 | Unit T011(e), manual T2.3 |
| FR-013 re-read on each open | T018, T019 | Manual T2.6, T2.7 |
| FR-014 uninstalled app / failed launch no crash | T031, T032, T035 | **Unit T030**, manual T2.8 |
| FR-015 no permissions | T001 | Manual T1.14, T1.15 |
| FR-016 placeholder icon, selectable | T021 | Manual T2.11 |
| FR-017 scroll + query survive | T019, T026, T036 | Manual T1.13, T2.5 |
| SC-001 1s on 150 apps | — | Manual T2.1 (adb `am start -W`) |
| SC-002 matches app drawer | — | Manual T1.2 |
| SC-003 no stutter | — | Manual T2.2 (Profile GPU Rendering) |
| SC-004 app found in <5s | — | Post-launch usability outcome — no task, no test |
| SC-005 2× faster second open | — | Manual T2.3 (adb, cache cleared then warm) |
| SC-006 zero permission prompts | T001 | Manual T1.14 |
| SC-007 95% correct first selection | — | Post-launch usability outcome — no task, no test |

---

## Notes

- `[P]` tasks touch different files and have no incomplete dependencies
- **Six unit assertions total.** If a proposed test does not cover logic that is invisible
  on screen, it does not belong here — the manual plan covers it instead
- Constitution V governs T008, T014, T016, T017, T033, T035: `packageName` is the only
  identifier persisted or handed off; labels are display-only; cache keys carry
  `versionCode`
- Constitution III governs T001 and manual cases T1.14/T1.15: zero permissions
- Do not add Coil, Accompanist, Hilt, Room, or DataStore — each was considered and
  rejected in research.md
