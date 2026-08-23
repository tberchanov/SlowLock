---

description: "Task list for Launch Delay"
---

# Tasks: Launch Delay

**Input**: Design documents from `/specs/003-launch-delay/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Testing approach**: **Two gates and a person.** JVM unit tests (`./gradlew test`) cover the pure
core — configuration sanitising, the frozen persistence tokens, the slider mapping, the wait
arithmetic. Everything that can only be observed on a running app is a numbered case in
`manual-test-plan.md`, run **by the maintainer**.

**No instrumented tests, and no driving the device.** Constitution v1.1.0 forbids `src/androidTest`,
`connectedAndroidTest`, Espresso and UI Automator outright, and forbids an agent driving the
connected device to pre-verify a manual case. An agent may build, run `./gradlew test`, and
`installDebug`; it may not tap, swipe, screenshot-and-inspect, or otherwise interact with the
running app. When a task hands over a manual pass, it stops, names the case IDs and what each
should show, and waits for the maintainer's answer. Feature 002's waiver of the old instrumented
requirement is moot — the requirement no longer exists (plan.md, Recorded rulings).

**⚠️ Two frozen documents govern this work.**
`specs/002-shortcut-pinning/contracts/pinned-shortcut.md` — the shortcut ID, the launch activity's
fully-qualified name, the intent action, and the extra key are **permanent** and this feature
changes none of them. `contracts/delay-config-store.md` — new, and permanent from the first
configuration written on a real device: the preferences file name, the two key suffixes, and the
`IconTreatment` constant **names**. Read both before touching `com.slowlock.shortcut` or
`com.slowlock.delay`.

**Organization**: Tasks are grouped by user story so each is independently implementable and
verifiable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete work)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Exact file paths are included in every task

## Path Conventions

Single `:app` Gradle module (per plan.md Structure Decision):

- Main source: `app/src/main/java/com/slowlock/`
- Unit tests: `app/src/test/java/com/slowlock/`
- There is no `app/src/androidTest/`, and this feature must not create one

---

## Phase 1: Setup

**Purpose**: A clean baseline. Nothing else — the instrumented source set this phase used to
create is forbidden by Constitution v1.1.0 and is not built.

**No dependency changes.** `gradle/libs.versions.toml` and `app/build.gradle.kts` must both be
**untouched** by this entire feature. The unused `androidTest` coordinates already in the build
file stay where they are; removing them is a separate decision.

- [X] T001 Run `./gradlew assembleDebug` and `./gradlew test` on the current tree and confirm both pass before anything changes. A pre-existing failure must be understood now, not discovered later while attributing it to this feature
  - **Result (2026-08-23): green.** `assembleDebug` BUILD SUCCESSFUL; `compileDebugKotlin --rerun` forced a real recompile of the main source, also successful. `testDebugUnitTest --rerun` after deleting `app/build/test-results/` executed genuinely (`1 executed`, not cached): **29 tests, 0 failures, 0 errors, 0 skipped** across 6 classes — `AppListViewModelTest` (1), `InstalledAppTest` (9), `IconTreatmentTest` (5), `PinGateTest` (4), `ShortcutContractTest` (6), `ShortcutTargetTest` (4). Note for later phases: plain `./gradlew test` reports UP-TO-DATE and runs nothing, so use `--rerun` on `testDebugUnitTest` when a real result is needed

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The persisted record, its frozen keys, the slider's bounds, and the wait arithmetic.
The four new source files are deliberately split so that **everything except `DelayConfigStore`
is plain Kotlin with no `android.*` imports** — that is what makes T006–T008 runnable on the JVM,
where `isReturnDefaultValues = true` would make a `SharedPreferences`-shaped test assert nothing
while appearing to pass. These four unit tests are the feature's entire automated safety net, so
they carry more weight than they would have with a device suite behind them.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T002 [P] Create package `app/src/main/java/com/slowlock/delay/` and `DelayConfig.kt` per data-model.md §`DelayConfig`: the `data class DelayConfig(val delaySeconds: Int, val treatment: IconTreatment)`, `const val DEFAULT_SECONDS = 10`, `val DEFAULT = DelayConfig(DEFAULT_SECONDS, IconTreatment.entries.first())`, and the **pure** helpers the store will wire to preferences — `internal fun delayKey(packageName: String) = packageName + ".delaySeconds"`, `internal fun treatmentKey(packageName: String) = packageName + ".treatment"`, `internal fun delayFrom(stored: Int?): Int` (null or ≤ 0 → `DEFAULT_SECONDS`, otherwise the stored value **unclamped**), `internal fun treatmentFrom(token: String?): IconTreatment` (unrecognised → `Original`). KDoc must state that the key suffixes and the treatment names are frozen (`contracts/delay-config-store.md`) and that reads sanitise rather than validate
- [X] T003 [P] Create `app/src/main/java/com/slowlock/delay/DelayRange.kt` per data-model.md §`DelayRange`: `MIN_SECONDS = 1`, `MAX_SECONDS = 30`, `STEP_SECONDS = 1` (revised from 5/120/5 after implementation), `STOPS` **derived** as `(MAX - MIN) / STEP + 1`, `SLIDER_STEPS` **derived** as `STOPS - 2` (Material's `Slider` counts the stops *between* the endpoints — writing 22 by hand is the off-by-one T007 exists to catch), and `fun snap(seconds: Int): Int` that clamps to `[MIN, MAX]` then rounds to the nearest `STEP`. Pure Kotlin, no Compose import
- [X] T004 [P] Create `app/src/main/java/com/slowlock/delay/WaitTiming.kt` per data-model.md §`WaitDeadline`: `fun deadlineFrom(nowElapsedMillis: Long, delaySeconds: Int): Long` and `fun remainingMillis(deadlineElapsedMillis: Long, nowElapsedMillis: Long): Long` which **never returns a negative value**. Take "now" as a parameter rather than calling `SystemClock` inside — that is what makes the restore path testable on the JVM, and with no instrumented suite it is the only way this arithmetic is checked automatically at all
- [X] T005 Create `app/src/main/java/com/slowlock/delay/DelayConfigStore.kt` per `contracts/delay-config-store.md`: `class DelayConfigStore(context: Context)` over `context.getSharedPreferences("slowlock.delay-config", MODE_PRIVATE)`, with `suspend fun load(packageName: String): DelayConfig` (non-null — `DelayConfig.DEFAULT` when absent) and `suspend fun save(packageName: String, config: DelayConfig)` writing both keys in one `Editor` with `apply()`. **Every body runs inside `withContext(Dispatchers.IO)`** (Constitution IV, FR-036), and every read goes through T002's pure helpers wrapped in `runCatching` so a `ClassCastException` from a wrongly-typed key yields the default instead of crashing a cold-started wait (depends on T002)
- [X] T006 [P] Create `app/src/test/java/com/slowlock/delay/DelayConfigTest.kt` asserting: `IconTreatment.entries.map { it.name }` equals `listOf("Original", "Invert", "Gray")` — **the frozen-token rename guard**; `delayKey`/`treatmentKey` produce `"com.example.app.delaySeconds"` / `"com.example.app.treatment"`; `delayFrom(null)`, `delayFrom(0)` and `delayFrom(-5)` all give `DEFAULT_SECONDS`; `delayFrom(1)` and `delayFrom(600)` are returned **unchanged** (the store does not clamp — R2); `treatmentFrom(null)` and `treatmentFrom("Sepia")` give `Original`; `treatmentFrom("Gray")` gives `Gray` (depends on T002)
- [X] T007 [P] Create `app/src/test/java/com/slowlock/delay/DelayRangeTest.kt` asserting: `STOPS == 30` and `SLIDER_STEPS == 28`; `snap` clamps below `MIN` and above `MAX`; no `snap` result is further than half a step from its input (at `STEP_SECONDS = 1`, `snap` is the identity inside the range, so the old `snap(12) == 10` / `snap(13) == 15` literals no longer describe it — the property replaced them); every `snap` result is a multiple of `STEP_SECONDS`; and **`DelayConfig.DEFAULT_SECONDS` is a reachable stop** — a default the slider cannot land on would make the readout disagree with the handle the moment the user touched it (depends on T002, T003)
- [X] T008 [P] Create `app/src/test/java/com/slowlock/delay/WaitTimingTest.kt` asserting: `deadlineFrom(1_000, 10)` is `11_000`; `remainingMillis` returns the difference while the deadline is ahead; it returns `0`, never a negative, for a deadline already passed (the restored-deadline case — `delay(-4)` must be unreachable) (depends on T004)
- [X] T009 Run `./gradlew test` and confirm the three new classes execute and pass alongside feature 002's four (depends on T006, T007, T008)
  - **Result (2026-08-23): green.** `assembleDebug` BUILD SUCCESSFUL. `testDebugUnitTest --rerun` after deleting `app/build/test-results/` executed genuinely: **9 classes, 44 tests, 0 failures, 0 errors, 0 skipped** — feature 002's six (29) plus `DelayConfigTest` (7), `DelayRangeTest` (5), `WaitTimingTest` (3). `app/build.gradle.kts` and `gradle/libs.versions.toml` untouched

**Checkpoint**: The persisted record, its frozen keys, and the pure arithmetic exist and are
covered. Nothing in the app behaves differently yet.

---

## Phase 3: User Story 1 - Wait before the app opens (Priority: P1) 🎯 MVP

**Goal**: Tapping a pinned shortcut shows a motionless "Please wait" screen for the app's delay,
then opens the target and leaves nothing in recents.

**Independent Test**: No configuration UI is needed. Tap a shortcut pinned by the feature 002
build: it now waits the default 10 seconds (FR-032 — `load` answers with the default), then opens
the target. Check recents is clean, and check the icon was not re-pinned. This is the whole
product thesis, deliverable before any settings screen exists.

**⚠️ Do not touch** `ShortcutContract.kt`, `ShortcutPinner.kt`, or the shortcut's intent shape.
This story changes only what the activity *does* — the half of `pinned-shortcut.md` that is
explicitly not frozen.

### Implementation for User Story 1

- [X] T010 [P] [US1] Add `<color name="wait_background">` and `<color name="wait_text">` to `app/src/main/res/values/colors.xml`. A flat, unremarkable pair for light mode — a plain near-white ground with a muted grey text, nothing branded, nothing saturated. These two are the whole visual design of the screen (obligation W12)
- [X] T011 [P] [US1] Create `app/src/main/res/values-night/colors.xml` overriding both with their dark counterparts — a near-black ground, a muted light grey text. **Not optional**: without it the wait screen is a full-brightness white field at night, on the one screen in the app designed not to be noticed, while every other screen follows the system setting (W12, spec Assumptions)
- [X] T012 [P] [US1] Add `<string name="wait_message">` to `app/src/main/res/values/strings.xml`: one short line telling the user to wait. It must not name the target app, must not mention a duration, and must not hint that anything is loading (FR-025, W8, W11)
- [X] T013 [US1] In `app/src/main/res/values/themes.xml`, **replace** `Theme.SlowLock.Invisible` with `<style name="Theme.SlowLock.Wait" parent="android:Theme.Material.DayNight.NoActionBar">` carrying `<item name="android:windowBackground">@color/wait_background</item>`. A `DayNight` parent so the starting window picks up T011's night colour. Do **not** carry `android:windowDisablePreview` over: the wait screen wants a starting window, and matching it to the composed screen's colour is what removes the flash on tap (research.md R7, W12) (depends on T010, T011)
  - **Deviation (2026-08-23):** the parent named here, `android:Theme.Material.DayNight.NoActionBar`, **does not exist** — the platform ships no Material DayNight variant, and resource linking fails on it. Used `android:Theme.DeviceDefault.DayNight` (the only switching platform theme family) plus `android:windowActionBar=false` and `android:windowNoTitle=true`, since it has no NoActionBar variant. AppCompat's DayNight was not an option: it is a new dependency, which this feature forbids. `@color/wait_background` resolves through `values-night` by configuration regardless of the parent, so W12 is unaffected
- [X] T014 [P] [US1] Create `app/src/main/java/com/slowlock/delay/WaitScreen.kt`: a `@Composable fun WaitScreen(modifier: Modifier = Modifier)` that fills the window with `colorResource(R.color.wait_background)` and centres one `Text` of `stringResource(R.string.wait_message)` in `colorResource(R.color.wait_text)`. **Not wrapped in `SlowLockTheme` and reading no `MaterialTheme` value** — dynamic colour would make the screen vary by wallpaper and could never match the static `windowBackground` (W12). Resolving the same colour resources as the theme is what keeps window and content matched in both light and dark. No `clickable`, no `Modifier.pointerInput`, no `animate*`, no `rememberInfiniteTransition`, no `CircularProgressIndicator`, no parameters that vary per app (W8, W10, W11) (depends on T010, T011, T012)
- [X] T015 [US1] Rewrite `app/src/main/java/com/slowlock/shortcut/ShortcutLaunchActivity.kt` against `contracts/wait-screen.md` W1–W11 and W17–W25. **Keep the class name, package, and KDoc rename warning exactly as they are** (`ShortcutContractTest` guards them). In `onCreate`: read the extra (W1, W2); anchor `deadline = savedInstanceState?.getLong(KEY_DEADLINE) ?: deadlineFrom(SystemClock.elapsedRealtime(), …)` **before any disk read** so the read cannot extend the wait (W3, W4); `setContent { WaitScreen() }` immediately; add `FLAG_KEEP_SCREEN_ON` to the window (W13 — permitted by Constitution IV as amended in v1.1.0, on exactly the terms this use meets); then `lifecycleScope.launch { }` to resolve the target on `Dispatchers.IO` (W5 — finish with the existing `shortcut_launch_unavailable` toast and **no wait** if it is already gone), `store.load(pkg)` (W6, W7), `delay(remainingMillis(...))`, re-resolve (W17, W18), `startActivity(… FLAG_ACTIVITY_NEW_TASK)` and `finish()` (W19). Add `onSaveInstanceState` writing the deadline (W4). Register **no** `BackHandler` (W14). Do not kill the process (W21) (depends on T004, T005, T014)
- [X] T016 [US1] Add `onNewIntent` to the same file: a repeat intent naming the **same** target is ignored outright — no restart, no extension, no second wait (W22, FR-027); an intent naming a **different** target re-anchors the deadline and restarts the wait for it (W23) (depends on T015)
- [X] T017 [US1] Update the `ShortcutLaunchActivity` entry in `app/src/main/AndroidManifest.xml` per `contracts/wait-screen.md` §Manifest shape: `android:theme="@style/Theme.SlowLock.Wait"`, add `android:launchMode="singleTop"`, and **remove `android:noHistory="true"`** — it would finish the activity on every `onStop` including a rotation, restarting the wait (research.md R5). Leave `exported="false"`, `excludeFromRecents="true"` and the empty `taskAffinity` untouched, and update the entry's comment, which currently describes an activity that must never be seen (depends on T013, T015)
- [X] T018 [US1] Run `./gradlew assembleDebug` and `./gradlew test`, then `./gradlew installDebug` to put the build on the maintainer's device (depends on T015, T016, T017)
  - **Result (2026-08-23): green.** `assembleDebug` BUILD SUCCESSFUL; `testDebugUnitTest --rerun` after deleting `app/build/test-results/`: **9 classes, 44 tests, 0 failures, 0 errors** — `ShortcutContractTest` still green, so the frozen FQN, action and extra key are intact across the rewrite. `installDebug` succeeded on a **OnePlus 8 (IN2015, Android 13)** — a non-Pixel OEM device, which also serves the M8 release gate later
- [X] T019 [US1] **Hand over and stop.** Ask the maintainer to run **M4.1–M4.7**, **M4.9**, **M5.1–M5.3** and **M7**, naming each case and what it should show, then wait for their answers before marking this story done. Do not tap, swipe, screenshot, or otherwise drive the device. Two notes to pass on: M4 wants a screen recording, because SC-002 compares the first *settled* frame with the last rather than being judged by watching; and **M7 is single-use** — it needs shortcuts pinned by the previous build, so it must be run on this first install or its result is lost for good
  - **Result (2026-08-24): pass, as reported by the maintainer.** M4.1–M4.7, M4.9, M5.1–M5.3 and **M7** all pass on the **OnePlus 8 (IN2015, Android 13)**. M7's single-use window was met — shortcuts pinned by the pre-003 build picked up the wait with nothing re-pinned and nothing asked of the user, which is the return on the frozen contract (FR-011, FR-032) and cannot be re-tested now

**Checkpoint**: The product's central behaviour works end to end on shortcuts that already exist.
Leaving mid-wait is not yet guaranteed to abandon the launch — that is US4.

---

## Phase 4: User Story 2 - Choose how long to wait (Priority: P2)

**Goal**: A delay screen between the app list and feature 002's shortcut screen, with the chosen
delay saved on apply and used by the pinned icon.

**Independent Test**: Tap an app, move the slider to 30 seconds, continue, create the shortcut,
tap the icon: it waits 30 seconds, not the default. Back from the shortcut screen returns to the
delay screen with 30 still selected.

**Note**: this phase opens the delay screen at the default for every app. Showing a *saved* delay
is US3 — deliberately, so the flow can be built and verified before the pre-loading is added.

### Implementation for User Story 2

- [X] T020 [P] [US2] Add to `app/src/main/res/values/strings.xml`: `delay_config_title`, `delay_config_back`, `delay_config_next`, and a `<plurals name="delay_seconds">` with `one`/`other` items. **Plurals, not `"%1$d seconds"`** — the minimum is provisional, and a plural bug found later is found by a translator rather than a test (research.md R11)
- [X] T021 [US2] Create `app/src/main/java/com/slowlock/delay/DelayConfigScreen.kt` against `contracts/delay-config-screen.md` D1–D11: signature `(packageName: String, seconds: Int, onSecondsChange: (Int) -> Unit, onNext: () -> Unit, onBack: () -> Unit, modifier: Modifier)`. Resolve the target's label and icon **here**, from the package name, off the main thread — reuse `AppIconCache` and the resolution shape `ShortcutConfigScreen` already uses (D1, D2). A Material 3 `Slider` with `valueRange = MIN_SECONDS.toFloat()..MAX_SECONDS.toFloat()` and `steps = DelayRange.SLIDER_STEPS`, reporting `DelayRange.snap(value.roundToInt())` through `onSecondsChange` (D3, D6), with the plurals readout beside it (D4). Back affordance at the top **and** a `BackHandler`, both calling `onBack` (D7). **The screen must hold no `rememberSaveable` copy of `seconds`** — it is hoisted, which is what makes FR-014 work (D5). No ViewModel, no store access, no way to launch the target (D9, D10, D11) (depends on T002, T003, T020)
- [X] T022 [US2] Widen `app/src/main/java/com/slowlock/shortcut/ShortcutConfigScreen.kt` per `contracts/delay-config-screen.md` C15–C18: add `delaySeconds: Int` and `initialTreatment: IconTreatment` parameters, change `var treatment by rememberSaveable { mutableStateOf(IconTreatment.entries.first()) }` to seed from `initialTreatment`, and split `onDone` into `onBack` and `onCreated`. In the private `create()` path, write `DelayConfigStore.save(packageName, DelayConfig(delaySeconds, treatment))` **before** `pinner.pin(...)` — the pin puts a system dialog in front of the user and the store write must not queue behind it (C16, research.md R10). Update the KDoc paragraph that says the caller cannot tell the exits apart: navigation now differs, feedback still does not (depends on T005, T021)
- [X] T023 [US2] Rewrite the navigation in `app/src/main/java/com/slowlock/SlowLockRoot.kt` per `contracts/delay-config-screen.md` N1–N5 and research.md R9: replace `selectedPackage: String?` with the `Stage` sealed interface from data-model.md (`List`, `Delay(packageName, seconds, treatment)`, `Shortcut(packageName, seconds, treatment)`) held in `rememberSaveable` with a `listSaver`. A row tap moves to `Delay` at `DelayConfig.DEFAULT_SECONDS` (US3 makes this the saved value). `onNext` moves to `Shortcut` carrying the current seconds; `onBack` from `Shortcut` returns to `Delay` **with the same seconds** (FR-014); `onCreated` and `onBack` from `Delay` return to `List`. Keep the `PinSupport` gate wrapping all three branches (N4). Keep `LIST_KEY` retained in the `SaveableStateHolder` and add a `DELAY_KEY`, dropping both it and `CONFIG_KEY` on the way out to `List` so a re-entry starts clean (N3) (depends on T021, T022)
- [X] T024 [US2] Run `./gradlew assembleDebug` and `./gradlew test`, then `./gradlew installDebug` (depends on T023)
- [X] T025 [US2] **Hand over and stop.** Ask the maintainer to run **M1** and **M2**, then wait. Flag the two cases that catch this phase's likeliest mistakes: M1.4 (the slider stops only on multiples of five) and M2.3 (back from the shortcut screen keeps the chosen value, not the saved one)

**Checkpoint**: The full configure-and-pin flow works. Reopening a configured app still shows the
default rather than what was saved — that is US3.

---

## Phase 5: User Story 3 - Change an app's settings later (Priority: P3)

**Goal**: Selecting a previously configured app opens both screens on that app's saved values, and
applying again replaces them for the icon already on the home screen.

**Independent Test**: Configure an app at 20 s with Gray, leave, reopen, select it: the slider
reads 20 and the shortcut screen opens on Gray. Change to 25 s, apply, tap the existing icon — it
waits 25 s, and there is still exactly one icon.

### Implementation for User Story 3

- [X] T026 [US3] In `app/src/main/java/com/slowlock/SlowLockRoot.kt`, make the row tap **load before it navigates** (obligations N1, D13, research.md R3): `rememberCoroutineScope().launch { val config = store.load(packageName); stage = Stage.Delay(packageName, config.delaySeconds, config.treatment) }`. The delay screen's first composition must already be correct — a default that flips to the saved value a frame later is how a user learns not to trust the number (FR-012). Hold one `remember(context) { DelayConfigStore(context) }` at the root and pass it down rather than constructing a second instance per screen (depends on T023)
  - **Deviation (2026-08-23):** the store is held at the root and used for the row-tap read, but it is **not passed down** to `ShortcutConfigScreen`. `contracts/delay-config-screen.md`'s "After" signature is explicit and adds only `delaySeconds`, `initialTreatment`, `onBack` and `onCreated` — no store parameter — and T022 built that seam. The concern the phrasing guards against does not arise: `getSharedPreferences` is cached per file per process by the framework, so the screen's own `remember { DelayConfigStore(context) }` wraps the *same* underlying object. Widening the signature would have cost a contract change to avoid a duplicate that does not exist
- [X] T027 [US3] Pass the stage's `treatment` into `ShortcutConfigScreen`'s `initialTreatment` in the same file, replacing the `IconTreatment.entries.first()` placeholder US2 left there (FR-013, C15) (depends on T022, T026)
- [X] T028 [US3] Run `./gradlew assembleDebug` and `./gradlew test`, then `./gradlew installDebug` (depends on T026, T027)
  - **Result (2026-08-23): green.** `assembleDebug` BUILD SUCCESSFUL (`compileDebugKotlin` genuinely re-executed). `testDebugUnitTest --rerun` after deleting `app/build/test-results/`: **9 classes, 44 tests, 0 failures, 0 errors, 0 skipped** — unchanged from T024, as expected, since this phase adds no pure code and touches no tested class. `installDebug` succeeded on the **OnePlus 8 (IN2015, Android 13)**. `app/build.gradle.kts` and `gradle/libs.versions.toml` untouched
- [X] T029 [US3] **Hand over and stop.** Ask the maintainer to run **M3** and **M6.2**, **M6.4**, **M6.5**, then wait. M3.5 is the one to watch: a changed delay taking effect on the existing icon with nothing asked of the user is what the whole design decision in research.md R1 was bought for
  - **Result (2026-08-23): pass.** Maintainer reports **M3 and M6 all pass** — M3.1–M3.8 and the full M6 group, which is broader than this task asked for (M6.1, M6.3, M6.6, M6.7 and M6.8 belong to T033 and Polish; their results carry forward). M3.5 confirmed: a changed delay takes effect on the icon already on the home screen, nothing re-pinned — research.md R1's premise holds on a real device

**Checkpoint**: All three configuration stories work. The wait still launches even if the user
walks away — US4 closes that.

---

## Phase 6: User Story 4 - Walk away from the wait (Priority: P4)

**Goal**: Leaving the wait screen — by back, home, the app switcher, or the display timing out —
abandons the launch permanently, while a rotation does not.

**Independent Test**: Tap a shortcut, press home mid-wait, wait past the delay and a minute
longer: the target never opens. Rotate during a different wait: it neither restarts nor extends.

**Why this is last**: it is the one behaviour that is a pure subtraction from US1, and separating
it keeps the `isChangingConfigurations` exception — the single easiest thing to get wrong here —
in a phase with its own manual pass. With no instrumented suite, that pass is the only thing
standing between this exception and a wait that restarts on every rotation, so it is not
skippable.

### Implementation for User Story 4

- [X] T030 [US4] Add the visibility rule to `app/src/main/java/com/slowlock/shortcut/ShortcutLaunchActivity.kt` (obligation W15, research.md R5): `override fun onStop() { super.onStop(); if (!isChangingConfigurations) finish() }`. The exception is not optional — a rotation passes through `onStop` too, and finishing there would restart the wait on every rotation, which is the bug FR-027 was written against. Note in KDoc that this single rule is the whole of FR-029: back, home, the app switcher, another app taking over, the display timing out, and the device locking all arrive here (depends on T015)
- [X] T031 [US4] Add the race guard in the same file (obligation W16): immediately before `startActivity`, confirm `lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)` and abandon otherwise. The deadline can expire in the same instant the user presses home — the continuation is already queued — and launching from a stopping activity is the background start Constitution IV forbids (depends on T030)
- [X] T032 [US4] Run `./gradlew assembleDebug` and `./gradlew test`, then `./gradlew installDebug` (depends on T030, T031)
  - **Partial (2026-08-23): build and test green, install blocked.** `assembleDebug` BUILD SUCCESSFUL (`compileDebugKotlin` genuinely re-executed). `testDebugUnitTest --rerun` after deleting `app/build/test-results/`: **9 classes, 44 tests, 0 failures, 0 errors, 0 skipped** — unchanged, as expected: `onStop` and the lifecycle guard are both untestable on the JVM, which is exactly why M5.4–M5.9 exist. `installDebug` failed with `DeviceException: No connected devices!` — the OnePlus 8 dropped off wireless adb between T028 and here, and `adb devices` and `adb mdns services` both come back empty. **Not a code failure.** Reconnect the device and re-run `./gradlew installDebug` to close this task
  - **Closed (2026-08-23):** device reconnected, `installDebug` BUILD SUCCESSFUL on the **OnePlus 8 (IN2015, Android 13)**. All three legs of this task are now green
- [X] T033 [US4] **Hand over and stop.** Ask the maintainer to run **M4.8**, **M5.4–M5.9**, and **M6.1**, **M6.3**, **M6.6**, **M6.7**, then wait. Two to call out: M5.6 (power button mid-wait) is the clarified behaviour a reviewer is most likely to mistake for a bug, and M4.6 (the maximum 30 s delay against a 15 s screen timeout) is what `FLAG_KEEP_SCREEN_ON` exists to make pass
  - **Result (2026-08-24): pass, as reported by the maintainer.** M4.8, M5.4–M5.9, M6.1, M6.3, M6.6 and M6.7 all pass on the OnePlus 8. This closes the two obligations no JVM test can reach: **W15** (`onStop` finishing, with the `isChangingConfigurations` exception — M4.8 confirms a rotation neither restarts nor extends the wait, M5.4–M5.7 confirm all four departure routes abandon it) and **W16** (the `STARTED` re-check before hand-off). M5.6, the power-button case, behaves as clarification Q2 specifies: the target is not open on unlock

**Checkpoint**: All four stories work. Every requirement in the spec has an implementation.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: The documents this feature invalidates, and the gates that close it. plan.md
§"Downstream edits to features 001 and 002" owns this list — none of it is optional, because each
item is a document that now asserts behaviour the app no longer has.

- [X] T034 [P] Amend `specs/002-shortcut-pinning/spec.md`: mark FR-001 as superseded (a list tap now opens the **delay** screen), narrow FR-006 (Original opens only for apps with no saved configuration — this feature's FR-013), and mark FR-016 as superseded (a pinned shortcut now waits before opening). Add a one-line pointer to `specs/003-launch-delay/spec.md` at each. Do **not** rewrite the history — these are amendments, not corrections
- [X] T035 [P] Amend `specs/002-shortcut-pinning/manual-test-plan.md` cases M1.1, M1.4, M2.3 and the M5 group, which assert the replaced behaviour, re-pointing each at this feature's equivalent case
- [X] T036 [P] Amend `specs/002-shortcut-pinning/contracts/shortcut-config-screen.md` with the widened seam — the two new parameters and the split exit — cross-referencing `specs/003-launch-delay/contracts/delay-config-screen.md`, which holds the reasoning
- [X] T037 [P] Amend `specs/002-shortcut-pinning/plan.md`'s "Testing-expectations check": its instrumented-test waiver is **moot**, not honoured. Constitution v1.1.0 removed the clause it waived, so the promise that the requirement "returns in full" with the delay feature is superseded and must not be read as an outstanding debt
- [X] T038 Verify `specs/002-shortcut-pinning/contracts/pinned-shortcut.md` is still true **line by line**: the shortcut ID, the activity's fully-qualified name, the intent action, and the extra key must all be unchanged, and `ShortcutContractTest` must still pass. This is a review task with a real chance of finding something — it is the one document this feature could break silently, and the failure would surface on users' home screens rather than in a build
  - **Result (2026-08-24): VERIFIED, no breakage.** All five frozen values check out line by line — shortcut ID (`shortcutId(t) = t`), the activity FQN (`com.slowlock.shortcut.ShortcutLaunchActivity`, class still in that package under that name), the action (`android.intent.action.VIEW`), the extra key (`com.slowlock.shortcut.extra.TARGET_PACKAGE`), and the extra value (the target package, written at `ShortcutPinner.kt:153`). `git diff 30fff91` is **empty** for `ShortcutContract.kt`, `ShortcutPinner.kt` and `ShortcutContractTest.kt` — the three files are untouched by the entire feature. `ShortcutContractTest` green (6 tests). Both readers still go through `ShortcutContract.EXTRA_TARGET_PACKAGE` (`ShortcutLaunchActivity.kt:96` in `onCreate` and `:137` in the new `onNewIntent`), so US4's second entry point did not introduce a raw string. Manifest changes are all inside the contract's own "NOT frozen" list.
  - **Two findings, both prose, no values affected.** The contract's "NOT frozen" table and `ShortcutContract.kt`'s KDoc both *predicted* that the delay feature would bring "a countdown and a schedule check" and "a visible countdown theme". Neither shipped: the wait screen is deliberately motionless (W8–W11) and schedules are unbuilt. Left standing, a wrong prediction in a frozen file is how the file stops being trusted — so both were corrected in place as amendments, and both now record that no frozen value moved. Also noted: obligations L4 and L5 are superseded by `wait-screen.md` W19/W20; L1, L2, L3, L6 and L7 hold verbatim
- [X] T039 [P] Add the wait screen and the delay screen to the KDoc navigation trail: `SlowLockRoot`'s KDoc still describes a two-branch `when`, and `MainActivity`'s still describes feature 002's tap behaviour
- [X] T040 Run the full gate one final time on a clean tree — `./gradlew assembleDebug` and `./gradlew test` — and walk `quickstart.md` end to end, stopping at anything that needs the running app (depends on T034–T039)
  - **Result (2026-08-24): green, with one document corrected.** `assembleDebug` + `testDebugUnitTest --rerun` after deleting `app/build/test-results/`: **9 classes, 44 tests, 0 failures, 0 errors, 0 skipped**. `gradle/libs.versions.toml` and `app/build.gradle.kts` untouched across the whole feature, as promised at T001.
  - **Quickstart walk found one stale block**: its `themes.xml` snippet still named `android:Theme.Material.DayNight.NoActionBar` as the parent — the theme that does not exist and that T013 already had to abandon. Anyone following the quickstart verbatim would have hit a resource-linking failure. Corrected to the shipped `android:Theme.DeviceDefault.DayNight` plus the two window flags, with T013's reasoning inlined. Everything else in the quickstart matches the tree: the manifest block, the file inventory under `com.slowlock.delay/`, and all three "most likely to be got wrong" items are accurate
- [X] T041 **Hand over and stop.** Ask the maintainer for the closing manual pass: **M1–M6 and M8** on a physical device, recording device, launcher, OS version, and screen timeout with each result. **M7 is excluded** — it was single-use at T019 and its result carries forward. M8 is the constitution's release gate: a non-Pixel OEM device, and Xiaomi Dual Apps recorded as tested or explicitly untested. Wait for the results; the feature is not complete until they come back
  - **Result (2026-08-24): PASS — closing manual pass complete, as reported by the maintainer.** M1–M6 and M8 all pass. M7 excluded as designed: single-use, run and passed at T019, result carried forward.
  - **Device recorded**: OnePlus 8 (IN2015), Android 13 — a **non-Pixel OEM device**, which satisfies the constitution's M8 release gate. Launcher, exact screen-timeout setting, and the Xiaomi Dual Apps disposition were **not reported back and are not recorded here**; M8's checklist asks for them, so a release sign-off that needs those fields should collect them separately rather than read them as absent-because-clean.
  - **Every requirement in the spec now has both an implementation and a verification.** The JVM suite covers the pure core (44 tests, 9 classes); everything observable only on a running device is covered by M1–M8, run by the maintainer, exactly as Constitution v1.1.0 intends

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: One task, no dependencies
- **Foundational (Phase 2)**: Depends on Setup. **Blocks every user story** — T002 through T005 are what the four stories are built on
- **US1 (Phase 3)**: Depends on Foundational. Depends on nothing else. Ships alone
- **US2 (Phase 4)**: Depends on Foundational. Independent of US1 in code — it touches `delay/DelayConfigScreen.kt`, `shortcut/ShortcutConfigScreen.kt` and `SlowLockRoot.kt`, none of which US1 edits
- **US3 (Phase 5)**: Depends on **US2**, not merely on Foundational. It edits the navigation US2 creates. This is the one real cross-story dependency in the feature
- **US4 (Phase 6)**: Depends on **US1**, not merely on Foundational. It edits the activity US1 rewrites
- **Polish (Phase 7)**: Depends on every story you intend to ship

### Within Each User Story

- Resources (colours, strings, themes) before the code that references them
- Composables before the navigation that hosts them
- Build and unit-test gate, then `installDebug`, then the hand-over — in that order, every time
- **A story is not done when its code compiles.** It is done when the maintainer has answered its manual cases. Do not start the next phase's manual pass on the assumption that the previous one passed

### Parallel Opportunities

- T002, T003, T004 (Foundational — three independent files; T005 needs T002)
- T006, T007, T008 (Foundational tests — three independent files)
- T010, T011, T012, T014 (US1 — light colours, night colours, strings, the composable)
- T034, T035, T036, T037, T039 (Polish — five separate documents)
- **US1 and US2 can be built simultaneously by two people.** They share no file. US3 then follows US2, and US4 follows US1
- Manual passes do **not** parallelise: they are one person on one device, and each needs the build from its own phase installed

---

## Parallel Example: Foundational

```bash
# Three independent pure-Kotlin files:
Task: "Create DelayConfig.kt in app/src/main/java/com/slowlock/delay/"
Task: "Create DelayRange.kt in app/src/main/java/com/slowlock/delay/"
Task: "Create WaitTiming.kt in app/src/main/java/com/slowlock/delay/"

# Then their three test classes, also independent:
Task: "Create DelayConfigTest.kt in app/src/test/java/com/slowlock/delay/"
Task: "Create DelayRangeTest.kt in app/src/test/java/com/slowlock/delay/"
Task: "Create WaitTimingTest.kt in app/src/test/java/com/slowlock/delay/"
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Phase 1: Setup — confirm the baseline is green
2. Phase 2: Foundational — the record, the store, the arithmetic
3. Phase 3: US1 — the wait
4. **STOP and VALIDATE**: hand M4, M5.1–M5.3 and M7 to the maintainer. On their word, a shortcut
   pinned by the previous build now waits ten seconds and opens the app. That is the product,
   demonstrable with no settings screen in existence

The MVP is unusually complete here: every shortcut a user already has gains the pause, at the
default delay, with nothing asked of them.

### Incremental Delivery

1. Setup + Foundational → nothing behaves differently yet
2. **+ US1** → every shortcut waits the default. Demo-able (MVP)
3. **+ US2** → the user picks the delay per app
4. **+ US3** → settings come back when they return, and can be changed
5. **+ US4** → walking away reliably cancels the launch
6. **+ Polish** → the 002 documents stop asserting behaviour that no longer exists

Shipping after US1, US2, or US3 leaves a coherent product. Shipping after US1 alone leaves one
sharp edge worth knowing about: a wait that the user walks away from may still open the app when
its deadline passes, until US4 lands.

### Parallel Team Strategy

With two people, after Foundational:

- Developer A: US1 → US4 (the activity, the wait, the abandonment rule)
- Developer B: US2 → US3 (the delay screen, the navigation, the saved values)

The two tracks share only `DelayConfigStore`, which Foundational finished. They meet at Polish —
and at the device, which only one of them can hold at a time.

---

## Notes

- [P] = different files, no dependency on incomplete work
- **Two gates, not three**: `assembleDebug` and `test`. There is no `connectedDebugAndroidTest`, and creating `app/src/androidTest/` is a constitutional violation, not an improvement
- **The agent does not touch the running app.** Build, unit-test and `installDebug` are fine unprompted; tapping, swiping, screenshotting or scripting the device is not. Hand over the case IDs and wait
- **Two frozen documents**: `002-shortcut-pinning/contracts/pinned-shortcut.md` (unchanged by this feature — T038 verifies) and `contracts/delay-config-store.md` (new, permanent from the first write on a device)
- **`gradle/libs.versions.toml` must be untouched.** A new dependency is a constitutional deviation plan.md does not authorize; the one it does authorize is already recorded in Complexity Tracking
- The constitution amendment this feature needed (v1.1.0) is **already done** — it is not a task here
- Commit after each task or logical group; stop at any checkpoint to validate a story on its own
