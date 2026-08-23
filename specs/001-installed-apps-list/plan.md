# Implementation Plan: Installed Applications List

**Branch**: `001-installed-apps-list` | **Date**: 2026-08-22 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-installed-apps-list/spec.md`

## Summary

A single Compose screen listing every launchable app on the current user profile — icon,
localized label, one row per package, SlowLock excluded — with type-to-filter search and a
tap that opens the chosen app.

Opening the app directly is a deliberate feasibility proof: it exercises the
resolve-a-stored-package-name-and-start-it mechanism the entire product depends on, before
any configuration, scheduling, or shortcut-pinning is built on top. **No delay or countdown
logic is in scope here** (FR-018) — that is a separate feature. The seam is kept explicit
(`onAppSelected(packageName)` handled in `MainActivity`) so swapping the launch for
navigation to the future configuration screen is a one-line change.

Technical approach: enumerate via `LauncherApps.getActivityList` off the main thread, map
to a plain `InstalledApp` at the boundary, then keep deduplication, `Collator`-based
sorting, and filtering as pure Kotlin functions. Icons load lazily per visible row through
a two-tier cache (memory `LruCache` + WebP files in `cacheDir`) keyed by
`packageName + versionCode`, so an app update invalidates its entry implicitly. State
lives in one `ViewModel` exposing a single `StateFlow`, which is what keeps rotation from
triggering a reload. No permissions, no new persistence engine, no third-party libraries.

## Technical Context

**Language/Version**: Kotlin 2.2.10, Java/JVM target 11

**Primary Dependencies**: Jetpack Compose (BOM 2026.02.01), Material 3, AndroidX
`core-ktx`, `activity-compose`, `lifecycle-runtime-ktx`; adding
`lifecycle-viewmodel-compose` and `lifecycle-runtime-compose` (see Complexity Tracking).
Platform APIs: `LauncherApps`, `PackageManager`, `java.text.Collator`.

**Storage**: No database. Icon file cache only — WebP files under
`context.cacheDir/app-icons/`. Search query in `SavedStateHandle`.

**Testing**: **Manual-first** — `manual-test-plan.md` is the primary verification
artifact. Automated coverage is six JUnit4 assertions in `app/src/test` over pure
functions (dedup, collation, filter, icon-cache key) plus the null
`getLaunchIntentForPackage()` path required as a unit test by the constitution. No
instrumented suite: display states are verified by eye, per the constitution's
"pure-Compose presentation may ship untested" allowance. The off-main-thread rule is
held structurally at the call sites (`Dispatchers.IO`) and verified by responsiveness in
manual testing; a `StrictMode` gate was tried and withdrawn as false-positive-prone. Gates: `./gradlew assembleDebug` and `./gradlew test`.

**Target Platform**: Android, `minSdk 33`, `targetSdk`/`compileSdk 37`

**Project Type**: Mobile app — single `:app` Gradle module, `com.slowlock`

**Performance Goals**: List visible and scrollable within 1s on a device with 150
launchable apps (SC-001); no dropped frames or mismatched rows while scrolling (SC-003);
second open at least 2× faster than the first via icon caching (SC-005)

**Constraints**: Zero permission prompts (SC-006, FR-015); no main-thread enumeration,
rasterization, or disk I/O (FR-011); no `QUERY_ALL_PACKAGES`; no network; scroll position
and query survive recreation without a reload flash (FR-017)

**Scale/Scope**: One screen, ~6 new source files, ~150–300 list entries typical, current
user profile only

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution v1.0.0. Evaluated pre-research and re-evaluated post-design; both results
shown.

| Principle | Binding rule as it applies here | Pre-Phase 0 | Post-Phase 1 |
|---|---|---|---|
| **I. Cooperative User, Not Adversary** | Screen imposes no friction and no enforcement; it is a picker. No bypass path is treated as a defect. | ✅ PASS | ✅ PASS |
| **II. Simplicity First (YAGNI)** | Single `:app` module retained. No DI framework, no persistence engine, no repository interface — concrete classes constructed directly. No live package observation, no debounce, no multi-select, no "already configured" badge. Two AndroidX Jetpack additions, tracked below. | ✅ PASS | ⚠️ PASS with tracked deviation |
| **III. Permission & Policy Minimalism** | Manifest `<queries>` for `ACTION_MAIN` + `CATEGORY_LAUNCHER`; `QUERY_ALL_PACKAGES` not requested; no `AccessibilityService`; no `PACKAGE_USAGE_STATS`; **zero** new `<uses-permission>` elements; no system dialog required (FR-015, SC-006). | ✅ PASS | ✅ PASS |
| **IV. Platform-Idiomatic Android** | Kotlin + Compose + Material 3, no XML layouts. `getLaunchIntentForPackage()` null handled at the call site (R9, FR-014). Enumeration, rasterization, and disk I/O on `Dispatchers.IO` (R8). No services, no polling, no wake locks — zero battery cost at rest. | ✅ PASS | ✅ PASS |
| **V. Stable Identifiers** | `packageName` is the only value persisted or handed off; `ComponentName`/activity names never persisted or matched; labels display-only, never keys (dedup is by package, not label — see the duplicate-label edge case); icon cache keyed `packageName + versionCode`. | ✅ PASS | ✅ PASS |

**Technology Standards check**: fixed stack honoured (Kotlin/Compose/M3, single module,
Java 11, minSdk 33, targetSdk 37, `com.slowlock`). New dependency versions declared in
`gradle/libs.versions.toml`, never hardcoded in `build.gradle.kts`. No backend, no
network, no analytics, no third-party SDK.

**Scope boundary check**: "app enumeration and picking" is the first of the four items v1
covers. Configuration, shortcut pinning, and `DelayActivity` are explicitly left out and
recorded in `spec.md` Assumptions.

**Gate result**: PASS. One deviation, documented in Complexity Tracking. Nothing blocks
implementation.

## Project Structure

### Documentation (this feature)

```text
specs/001-installed-apps-list/
├── plan.md                       # This file (/speckit-plan output)
├── spec.md                       # Feature specification
├── research.md                   # Phase 0 output — R1–R10 decisions
├── data-model.md                 # Phase 1 output — InstalledApp, AppListUiState, CachedIcon
├── quickstart.md                 # Phase 1 output — build, test, manifest/dep changes
├── manual-test-plan.md           # Primary verification artifact — tiered device test cases
├── contracts/                    # Phase 1 output
│   ├── app-list-screen.md        # UI contract: signature, C1–C15 observable behaviour
│   └── selection-handoff.md      # The packageName seam to the next feature
├── checklists/
│   └── requirements.md           # 16/16 passing
└── tasks.md                      # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

```text
app/src/main/
├── AndroidManifest.xml                     # MODIFIED: add <queries> ACTION_MAIN + CATEGORY_LAUNCHER
├── res/values/strings.xml                  # MODIFIED: list/search/empty/no-results/unavailable strings
└── java/com/slowlock/
    ├── MainActivity.kt                     # MODIFIED: host AppListScreen, supply onAppSelected
    ├── apps/                               # NEW — this feature
    │   ├── InstalledApp.kt                 # data class + pure excludeSelf/deduplicate/sortedBy/filter
    │   ├── InstalledAppsSource.kt          # suspend enumeration via LauncherApps, IO dispatcher
    │   ├── AppIconCache.kt                 # LruCache + cacheDir/app-icons WebP tier, sweep on open
    │   ├── AppListUiState.kt               # state + derived loading/populated/empty/no-results
    │   ├── AppListViewModel.kt             # StateFlow, SavedStateHandle query, refresh(), onAppTapped()
    │   └── AppListScreen.kt                # Scaffold + SearchBar + LazyColumn + AppRow + states
    └── ui/theme/                           # UNCHANGED: Color.kt, Theme.kt, Type.kt

app/src/test/java/com/slowlock/apps/
├── InstalledAppTest.kt                     # dedup, self-exclusion, collated sort, filter, icon-cache key
└── AppListViewModelTest.kt                 # null getLaunchIntentForPackage() path (constitution MUST)

# No androidTest suite — verification is manual, per manual-test-plan.md

gradle/libs.versions.toml                   # MODIFIED: lifecycle-viewmodel-compose, lifecycle-runtime-compose
app/build.gradle.kts                        # MODIFIED: the two implementation() lines
```

**Structure Decision**: Single `:app` module, as the constitution requires until a
concrete need proves otherwise — no new Gradle module. Feature code is grouped in one flat
`com.slowlock.apps` package rather than split into `data`/`domain`/`ui` layers: six files
do not need a layer hierarchy, and layered packages would be the "abstraction layer" the
constitution counts as a deviation. The one seam that matters is testability — mapping
`LauncherApps` output to `InstalledApp` at the boundary so that dedup, sorting, and
filtering are pure functions unit-testable without a device (R10). Existing `ui/theme`
stays untouched.

## Phase Status

| Phase | Output | Status |
|---|---|---|
| Phase 0 — Research | `research.md` (R1–R10) | ✅ Complete, no `NEEDS CLARIFICATION` remaining |
| Phase 1 — Design & Contracts | `data-model.md`, `contracts/`, `quickstart.md`, agent context | ✅ Complete |
| Phase 2 — Tasks | `tasks.md` | ⏭ Not started — run `/speckit-tasks` |

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Two new dependencies: `androidx.lifecycle:lifecycle-viewmodel-compose` and `lifecycle-runtime-compose` (Constitution II — "new third-party dependencies MUST be justified… the default answer is no") | FR-017 requires scroll position and search query to survive screen recreation, and the spec's edge case adds "without a full reload flash" — meaning the enumerated list must outlive a rotation. `viewModel()` is the only idiomatic way to reach a `ViewModel` from Compose; `lifecycle-runtime-compose` supplies `collectAsStateWithLifecycle()` and the `ON_START` hook that FR-013 (re-read on each open) needs. Both are first-party AndroidX from the `lifecycle` group already in the dependency set — the same version ref, no new third party, no transitive surface beyond what `lifecycle-runtime-ktx` already pulls. | **Hoisted `rememberSaveable` state**: cannot hold the app list — parcelling ~150 entries per rotation risks `TransactionTooLargeException` and cannot carry bitmaps. **Re-enumerate on every recreation**: produces exactly the reload flash FR-017 forbids, and burns the SC-001 budget on every rotation. **`retain`/`rememberRetained` equivalents**: not available without a further (actually third-party) dependency. |

No other deviation. Notably *not* taken, and worth recording as refused: Coil/Glide for
icon loading (R5), Accompanist for `Drawable` rendering (R6), Hilt/Koin for wiring, Room
or DataStore for the icon cache, and a `LauncherApps.Callback` for live updates (R9) —
each rejected under Principle II with the simpler in-project alternative named in
`research.md`.
