---

description: "Task list for Pinned Shortcut Creation"
---

# Tasks: Pinned Shortcut Creation

**Input**: Design documents from `/specs/002-shortcut-pinning/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Testing approach**: **Manual-first**, as in feature 001 — `manual-test-plan.md` is the primary
artifact, because the open questions are what real launchers on real devices do, and no suite can
install a second launcher, answer a system dialog, or reboot a phone. Automated coverage is four
JVM test classes, each earning its place: the treatment matrices (a silent, permanent error once
pinned), the **frozen shortcut contract including a rename guard** (turns a home-screen failure
into a build failure), the null `getLaunchIntentForPackage()` path (required as a unit test by the
constitution), and the pin-support gate (Constitution IV names this API explicitly). No Compose UI
tests, no `connectedAndroidTest`.

**⚠️ Before touching `com.slowlock.shortcut`**: read `contracts/pinned-shortcut.md`. The shortcut
ID, the launch activity's fully-qualified name, the intent action, and the extra key are
**permanent** from the first pin on a real device. Everything else in this feature is a draft.

**Organization**: Tasks are grouped by user story so each is independently implementable and
verifiable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete work)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Exact file paths are included in every task

## Path Conventions

Single `:app` Gradle module (per plan.md Structure Decision):

- Main source: `app/src/main/java/com/slowlock/`
- Unit tests: `app/src/test/java/com/slowlock/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Resource groundwork only. No feature logic.

**No dependency changes.** `gradle/libs.versions.toml` and `app/build.gradle.kts` must be
**untouched** by this entire feature — everything used is either a platform API or already on the
classpath. Adding a dependency here is a constitutional deviation that plan.md does not authorize.

- [X] T001 [P] Add the eleven string resources listed in quickstart.md §"Strings added" (`shortcut_config_title`, `shortcut_config_back`, `shortcut_config_create`, `shortcut_treatment_original`, `shortcut_treatment_invert`, `shortcut_treatment_gray`, `shortcut_target_unavailable`, `shortcut_launch_unavailable`, `pin_unsupported_message`, `pin_unsupported_open_settings`, `pin_unsupported_recheck`, `pin_unsupported_settings_failed`) to `app/src/main/res/values/strings.xml`, changing no existing string. `pin_unsupported_message` MUST be a sentence or two of plain language with no error codes and no API names (FR-030)
- [X] T002 [P] Add `<style name="Theme.SlowLock.Invisible" parent="android:Theme.Translucent.NoTitleBar">` with `<item name="android:windowDisablePreview">true</item>` to `app/src/main/res/values/themes.xml` (research.md R6 — translucent, **not** `Theme.NoDisplay`, which throws on modern API levels if the activity does not finish before `onResume`)
- [X] T003 Run `./gradlew assembleDebug` to confirm the resource changes build clean before any feature code lands (depends on T001, T002)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The frozen constants, the pure data, and the seams every user story depends on. The
four **new** source files (T004–T007) are plain Kotlin — **no `android.*` imports, no Compose** — which
is what makes them testable on the JVM. T010 is the one exception: it widens an existing feature 001
file so the shortcut package can reach the icon cache without borrowing 001's list model.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Create package `app/src/main/java/com/slowlock/shortcut/` and `ShortcutContract.kt` holding the frozen values from `contracts/pinned-shortcut.md`: `const val LAUNCH_ACTIVITY = "com.slowlock.shortcut.ShortcutLaunchActivity"`, `const val EXTRA_TARGET_PACKAGE = "com.slowlock.shortcut.extra.TARGET_PACKAGE"`, `const val ACTION = "android.intent.action.VIEW"` (the string literal, so the file stays framework-free), `fun shortcutId(targetPackage: String) = targetPackage`, the `ShortcutSpec` data class (`id`, `label`, `targetPackage`), and the pure `fun shortcutSpec(target: ShortcutTarget): ShortcutSpec`. Document in KDoc that every constant is permanent and why
- [X] T005 [P] Create the `IconTreatment` enum (`Original`, `Invert`, `Gray`) with `val matrix: FloatArray?` in `app/src/main/java/com/slowlock/shortcut/IconTreatment.kt` per data-model.md §`IconTreatment`. `Original` is `null` (no filter at all, not an identity one). `Invert` is `-1,0,0,0,255 / 0,-1,0,0,255 / 0,0,-1,0,255 / 0,0,0,1,0` — **the alpha row stays identity**; inverting it turns transparent icon corners into opaque black and makes every inverted icon a solid square. `Gray` is `0.213,0.715,0.072,0,0` on each of the three colour rows with `0,0,0,1,0` for alpha. **Write these as literal constants — do NOT call `android.graphics.ColorMatrix().setSaturation(0f)`**: unit tests run with `isReturnDefaultValues = true`, under which that call returns an empty matrix and T008 would assert nothing while appearing to pass
- [X] T006 [P] Create the `ShortcutTarget` data class (`packageName`, `label`, `versionCode`) and `suspend fun resolveTarget(...): ShortcutTarget?` in `app/src/main/java/com/slowlock/shortcut/ShortcutTarget.kt` per data-model.md. Resolution runs on `Dispatchers.IO` (FR-024) and returns `null` when `getLaunchIntentForPackage()` returns null (FR-015). **Take the resolver as an injected lambda** — `resolveLaunchIntent: (String) -> Intent?` and a label/version lookup — exactly as `AppListViewModel` does, so T009 can exercise the null path with no device. No icon field: icons never travel inside state
- [X] T007 [P] Create `PinSupport` (sealed interface with `Unknown`, `Supported`, `Unsupported`) and a `fun pinSupport(context: Context): PinSupport` wrapping `ShortcutManager.isRequestPinShortcutSupported()` in `app/src/main/java/com/slowlock/shortcut/PinSupport.kt` per data-model.md. Take the underlying check as an injectable lambda so T016 can drive it. `Unknown` is the initial value and must never be treated as either answer
- [X] T008 [P] Create `app/src/test/java/com/slowlock/shortcut/IconTreatmentTest.kt` asserting: `Original.matrix` is null; `Invert.matrix` has an identity alpha row (`0,0,0,1,0`) and `-1` on the three colour diagonals; `Gray.matrix` rows are the luminance coefficients; all three non-null matrices are exactly 20 floats (depends on T005)
- [X] T009 [P] Create `app/src/test/java/com/slowlock/shortcut/ShortcutTargetTest.kt` asserting `resolveTarget` returns null when the injected resolver returns null, and returns a populated `ShortcutTarget` when it does not. **This is the constitution's required unit test** for the null `getLaunchIntentForPackage()` path (depends on T006)
- [X] T010 [P] Add a `suspend fun icon(packageName: String, versionCode: Long): ImageBitmap?` overload to `app/src/main/java/com/slowlock/apps/AppIconCache.kt` and make the existing `icon(app: InstalledApp)` delegate to it; change the private `rasterize`/`fileFor` helpers to take the two values. **This is a deliberate modification to a feature 001 file** — the cache only ever used `packageName` and `versionCode` (`AppIconCache.kt:41,82,85`), and `ShortcutTarget` is not an `InstalledApp`. Do **not** construct a throwaway `InstalledApp` inside the shortcut package instead: that would couple feature 002 to feature 001's list model for no reason. Cache keys, on-disk file naming, and sweep behaviour are unchanged (Constitution V)
- [X] T011 Run `./gradlew test` and confirm both new test classes execute and pass, and that `AppIconCache`'s existing callers still compile (depends on T008, T009, T010)

**Checkpoint**: The frozen contract and the pure core exist, compile, and are covered. No UI yet.

---

## Phase 3: User Story 1 - Pin a shortcut that opens the chosen app (Priority: P1) 🎯 MVP

**Goal**: Tapping an app opens a configuration screen showing a preview of the shortcut; pressing
"Create shortcut" pins it; tapping the pinned icon opens the target app.

**Independent Test**: Pick an app, create a shortcut with the unmodified icon, confirm one icon
appears on the home screen and opens the right app — the icon treatments (US2) and backing out
(US3) are not needed for this to be a complete, useful increment.

**⚠️ Note on this increment**: the configuration screen has no exit other than "Create shortcut"
until US3 lands — system back will leave the app. That is exactly the trap US3 exists to close, and
is acceptable only between these two checkpoints.

- [X] T012 [US1] Create `ShortcutLaunchActivity` in `app/src/main/java/com/slowlock/shortcut/ShortcutLaunchActivity.kt` per obligations L1–L7 of `contracts/pinned-shortcut.md`: read the target package from `ShortcutContract.EXTRA_TARGET_PACKAGE` and nowhere else (L1); re-resolve it via `getLaunchIntentForPackage()` at tap time (L2); on null, show the `shortcut_launch_unavailable` toast and `finish()` without crashing (L3, FR-018); otherwise start the target with `FLAG_ACTIVITY_NEW_TASK` and `finish()` immediately (L4, FR-016). No layout, no `setContent`, nothing drawn. **The class name and package are frozen** — do not rename or move this file, ever
- [X] T013 [US1] Register the activity in `app/src/main/AndroidManifest.xml` exactly as quickstart.md §"Manifest change" specifies: `android:name=".shortcut.ShortcutLaunchActivity"`, `android:exported="false"`, `android:excludeFromRecents="true"`, `android:noHistory="true"`, `android:taskAffinity=""`, `android:theme="@style/Theme.SlowLock.Invisible"`. `exported="false"` is correct and deliberate — the system starts the intent under SlowLock's own identity via `LauncherApps.startShortcut` (research.md R5). Add **no** `<uses-permission>` and **no** `<queries>` entry (depends on T002, T012)
- [X] T014 [P] [US1] Create `app/src/test/java/com/slowlock/shortcut/ShortcutContractTest.kt` with the rename guard `assertEquals(ShortcutContract.LAUNCH_ACTIVITY, ShortcutLaunchActivity::class.java.name)`, plus assertions that the extra key is `com.slowlock.shortcut.extra.TARGET_PACKAGE` and that `shortcutId(pkg) == pkg` (FR-025). **This test is the only thing standing between a routine refactor and every pinned shortcut on every device breaking silently** (depends on T004, T012)
- [X] T015 [US1] Create `ShortcutPinner` in `app/src/main/java/com/slowlock/shortcut/ShortcutPinner.kt`: take `(target, treatment: IconTreatment, sourceIcon: Bitmap)` — **non-null, so the compiler enforces C12's precondition that the pinner is unreachable without a real icon**; on `Dispatchers.IO` draw the source bitmap into a **new** bitmap sized to `ActivityManager.getLauncherLargeIconSize()` through a `ColorMatrixColorFilter` built from `treatment.matrix` (skipping the filter entirely when it is null), never mutating the cached bitmap (research.md R8, FR-024); build the `ShortcutInfo` from `shortcutSpec(target)` with `Icon.createWithBitmap`, short and long label, and the frozen intent (`ACTION_VIEW`, `ComponentName(context, ShortcutLaunchActivity::class.java)`, target package extra); then call `updateShortcuts(listOf(info))` **and** `requestPinShortcut(info, null)`, both unconditionally. **Pass no `IntentSender`** (FR-012) and record nothing about what has been pinned (FR-027). Take `treatment` as a parameter now even though US1 only ever passes `Original`, so US2 adds no signature change (depends on T004, T005, T007)
- [X] T016 [P] [US1] Create `app/src/test/java/com/slowlock/shortcut/PinGateTest.kt` asserting that the pin path is not entered when `PinSupport` reports `Unsupported`, and is entered when it reports `Supported` (FR-013; Constitution IV names `isRequestPinShortcutSupported()` as a gate on *every* pin attempt). Drive it through the injectable check from T007 (depends on T007, T015)
- [X] T017 [US1] Create `ShortcutConfigScreen(packageName, onDone, modifier)` in `app/src/main/java/com/slowlock/shortcut/ShortcutConfigScreen.kt` per `contracts/shortcut-config-screen.md` C1, C2, C8–C12, C14–C16: resolve the target with `produceState` keyed on `packageName` (T006), load the icon through feature 001's existing `AppIconCache`, show the centred preview of icon plus label at roughly home-screen proportions, and a "Create shortcut" button at the bottom. On create: re-check pin support (T007, FR-013), re-resolve the target and show `shortcut_target_unavailable` **without closing** if it is gone (FR-015, C11), otherwise call `ShortcutPinner` with `IconTreatment.Original` and invoke `onDone()`. **Show no confirmation of any kind on success** (FR-012, C9). Fall back to a neutral placeholder if the icon fails to load **and disable "Create shortcut" with a short explanation** (C12): a pinned shortcut is effectively permanent, so a placeholder icon on the home screen is worse than no shortcut and defeats the point of an icon that mirrors the target app. The failure is transient — `AppIconCache` deliberately does not cache failures — so reopening the screen retries. No ViewModel (research.md R10) (depends on T006, T015)
- [X] T018 [US1] Create `SlowLockRoot` in `app/src/main/java/com/slowlock/SlowLockRoot.kt`: hold `var selectedPackage: String? by rememberSaveable`, render `AppListScreen(onAppSelected = { selectedPackage = it })` when null and `ShortcutConfigScreen(packageName, onDone = { selectedPackage = null })` when set (FR-001 — this **replaces** feature 001's interim tap-to-launch). Leave the `PinSupport` branches for Phase 6 and the `SaveableStateHolder` for T029; both touch this file again (depends on T017)
- [X] T019 [US1] Rewrite `app/src/main/java/com/slowlock/MainActivity.kt` to host `SlowLockRoot()` inside `SlowLockTheme`. **Delete `launchApp()` and its interim-proof KDoc entirely** — its contract (`specs/001-installed-apps-list/contracts/selection-handoff.md`) named this feature as its intended consumer. The `BuildConfig.DEBUG` `StrictMode` block that feature 001 added here has since been **removed** — it fired on framework-internal disk touches (`Context.getCacheDir()`) rather than on the app blocking on I/O, making its violations false positives. FR-024 is held at the call sites instead (depends on T018)
- [X] T020 [US1] Confirm `app/src/main/java/com/slowlock/apps/AppListScreen.kt` is byte-for-byte unmodified (`git diff --stat app/src/main/java/com/slowlock/apps/AppListScreen.kt` is empty). Swapping the launch for navigation without touching this file is the 001 seam working as designed; if it needed editing, the change belongs in `SlowLockRoot` instead. Also confirm `app/src/main/java/com/slowlock/apps/AppIconCache.kt`'s only change is the T010 overload — no change to cache keys, on-disk file naming, or sweep behaviour
- [X] T021 [US1] Run `./gradlew assembleDebug` and `./gradlew test`, then `./gradlew installDebug` and work through manual cases M1.1, M1.2, M1.10 and M2.1–M2.7 in `manual-test-plan.md` — including tapping the pinned icon (M2.3) and confirming no SlowLock screen flashes and no SlowLock entry appears in recents (M2.4, M2.5, FR-019) (depends on T019)

**Checkpoint**: A user can pin a working home-screen shortcut and tap it to open the target app.
US1 is independently demonstrable.

---

## Phase 4: User Story 2 - Choose how the shortcut icon looks (Priority: P2)

**Goal**: The user cycles Original / Invert / Gray before creating, the preview updates instantly,
and the created shortcut carries whichever treatment was showing.

**Independent Test**: Open the configuration screen, cycle the treatments, confirm the preview
changes each time and that the created shortcut carries the treatment that was showing.

- [X] T022 [US2] Add a horizontally scrollable treatment row **above** the preview in `app/src/main/java/com/slowlock/shortcut/ShortcutConfigScreen.kt`, rendering `IconTreatment.entries` in declaration order with the strings from T001 — exactly Original, Invert, Gray, no more (FR-005, C3)
- [X] T023 [US2] Hold the selection in `rememberSaveable { mutableStateOf(IconTreatment.Original) }` in `app/src/main/java/com/slowlock/shortcut/ShortcutConfigScreen.kt` and apply it to the preview as `ColorFilter.colorMatrix(ColorMatrix(treatment.matrix))`, passing no filter at all when `matrix` is null. `Original` is selected on open (FR-006). **Apply a `ColorFilter` to the already-loaded `ImageBitmap` — do not bake a bitmap per tap**: SC-004 allows 100 ms with no flicker or layout shift, and baking allocates on every selection (research.md R7, C5, C6). A Kotlin enum is `Serializable`, so the default saver carries it through rotation (FR-008, C7) (depends on T022)
- [X] T024 [US2] Pass the selected treatment into the existing `ShortcutPinner` call in `app/src/main/java/com/slowlock/shortcut/ShortcutConfigScreen.kt`, replacing the hardcoded `IconTreatment.Original` from T017. The preview filter and the baked bitmap now derive from the same `IconTreatment.matrix`, which is what makes SC-003 structural rather than a thing to eyeball (depends on T023)
- [X] T025 [US2] Run `./gradlew assembleDebug` and `./gradlew test` (depends on T024)
- [X] T026 [US2] Work through manual cases M1.3–M1.8 and M2.6 in `manual-test-plan.md`, paying particular attention to M1.5 — **transparent icon areas must stay transparent under Invert**, not turn into a solid black square (depends on T025)

**Checkpoint**: US1 and US2 both work. The pinned icon matches the preview.

---

## Phase 5: User Story 3 - Back out without creating anything (Priority: P3)

**Goal**: A user who opens the configuration screen by mistake returns to the list with nothing
created and nothing lost.

**Independent Test**: Open the configuration screen, press back, confirm the list returns, no new
home-screen icon exists, and the scroll position and search query are where they were left.

- [X] T027 [US3] Add a back affordance at the top of `app/src/main/java/com/slowlock/shortcut/ShortcutConfigScreen.kt` that invokes `onDone()` without creating anything, with the `shortcut_config_back` content description (FR-020, C13)
- [X] T028 [US3] Add `BackHandler { onDone() }` in `app/src/main/java/com/slowlock/shortcut/ShortcutConfigScreen.kt` so the system back gesture and button behave identically to the affordance (FR-021, C13). `androidx.activity.compose.BackHandler` is already on the classpath (depends on T027)
- [X] T029 [US3] Wrap both root branches in `rememberSaveableStateHolder()` with `SaveableStateProvider` keys in `app/src/main/java/com/slowlock/SlowLockRoot.kt` (FR-022, C-root). **This is not optional polish**: the app list and search query already survive in `AppListViewModel` and its `SavedStateHandle`, but `rememberLazyListState` saves through `rememberSaveable`, which is discarded when `AppListScreen` leaves composition — without the holder the user returns to the top of the list every time (research.md R9) (depends on T028)
- [X] T030 [US3] Run `./gradlew assembleDebug` and `./gradlew test` (depends on T029)
- [X] T031 [US3] Work through manual cases M3.1–M3.8 in `manual-test-plan.md`, including M3.3 (scroll far down, search, tap a row, come back — **both** scroll position and query preserved) and M3.4/M3.5 (declining the launcher dialog five times leaves the app usable with no error shown) (depends on T030)

**Checkpoint**: All three user stories are independently functional.

---

## Phase 6: Launcher Support Gate (FR-028 – FR-032, SC-009)

**Purpose**: Cross-cutting. On a device whose launcher refuses pin requests, every control in this
feature is dead, so none of them may be reachable. Not tied to a single user story — it gates all
three.

- [X] T032 Create `PinUnsupportedScreen` in `app/src/main/java/com/slowlock/shortcut/PinUnsupportedScreen.kt` per contract U1–U6: the `pin_unsupported_message` explanation, a control that opens the default-launcher setting, and a control that re-checks. Plain language only — no error codes, no API names, no mention of `isRequestPinShortcutSupported` (FR-030, U2)
- [X] T033 Open the launcher setting with `runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }` in `app/src/main/java/com/slowlock/shortcut/PinUnsupportedScreen.kt`, showing `pin_unsupported_settings_failed` on `ActivityNotFoundException` while leaving the screen usable (FR-031, U3, U4). **Catch rather than probe with `resolveActivity()`** — probing returns null under package visibility unless a `<queries>` entry is added, which would buy a manifest change for nothing (research.md R11) (depends on T032)
- [X] T034 Add the `PinSupport` branches to `app/src/main/java/com/slowlock/SlowLockRoot.kt`: re-evaluate via `LifecycleEventEffect(Lifecycle.Event.ON_START)` on first launch **and on every return to the foreground**, never caching across a background trip (FR-028). `Unknown` renders nothing — not the list, not an error; `Unsupported` renders `PinUnsupportedScreen` **in place of** the list, making both the list and the configuration screen unreachable by any route (FR-029, SC-009); `Supported` proceeds with no restart needed (FR-032). Wire the re-check control from T032 to the same evaluation (depends on T032, T029)
- [X] T035 Run `./gradlew assembleDebug` and `./gradlew test` (depends on T034)
- [X] T036 Work through manual section M6 in `manual-test-plan.md` against a launcher that refuses pin requests, or a temporary `PinSupport` stub returning `Unsupported`. **The stub must not be committed.** M6.6 is the one most easily missed: switch to a supporting launcher and return **without restarting the app** (depends on T035)

**Checkpoint**: The feature behaves correctly on devices where it cannot work at all.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Reconcile feature 001's superseded artifacts, and run the verification the
constitution requires before this can be called complete.

- [X] T037 [P] Annotate FR-009 and FR-018 in `specs/001-installed-apps-list/spec.md` as superseded by feature 002 — a row tap now opens the shortcut configuration screen rather than launching the app (spec.md Assumptions)
- [X] T038 [P] Rewrite cases T1.12 and T1.16 in `specs/001-installed-apps-list/manual-test-plan.md` against the new tap behaviour: T1.12 becomes "a tap opens the configuration screen", T1.16 becomes "list state preserved after returning from the configuration screen"
- [X] T039 [P] Mark `specs/001-installed-apps-list/contracts/selection-handoff.md` as **consumed** by feature 002, superseding its "Interim implementation: launch the target directly" section. Record that the `onAppSelected(packageName: String)` shape itself did not change — which was the entire point of pinning it down before its consumer existed
- [X] T040 Confirm the constitution's technology standards hold: `git diff` shows `gradle/libs.versions.toml` and `app/build.gradle.kts` **untouched**, and `app/src/main/AndroidManifest.xml` gained exactly one `<activity>` and **zero** `<uses-permission>` and `<queries>` elements (Constitution II, III)
- [X] T041 Exercise the full flow on a device and confirm it stays responsive throughout — no stall or dropped frames during icon load, treatment application, or bitmap baking — and that **zero permission dialogs** appear anywhere in it (FR-024, SC-005). FR-024's off-main-thread rule is held structurally instead: icon rasterization, target resolution, and the bitmap bake each run on `Dispatchers.IO` at their call sites. **Do not reintroduce `StrictMode` as the gate** — it flags framework-internal disk touches (`Context.getCacheDir()` among them) that are not the app blocking on I/O, so its violations are false positives here rather than FR-024 failures
- [ ] T042 Run manual section M4 in `manual-test-plan.md` on **two launchers from different vendors**, filling in the results table with launcher name, version, pin honoured, dialog shown, and icon fidelity. Per SC-008 a recorded failure is a valid result, not a blocked test — this table is the feasibility answer the whole feature exists to produce (depends on T036)
- [X] T043 Run manual section M4.1–M4.4 specifically for re-pinning: pin an app as Original, then again as Gray, and confirm **exactly one** home-screen icon that is now grey, with `adb shell dumpsys shortcut | grep -A 20 com.slowlock` showing one shortcut whose ID is the package name (FR-025, FR-026, FR-027). No visible feedback on the second pin is expected and correct (depends on T042)
- [X] T044 Run manual section M5 in `manual-test-plan.md` on a **physical device**: `adb shell am force-stop com.slowlock` then tap the shortcut, and a real `adb reboot` then tap it again without opening SlowLock first (FR-017, SC-007). A failure here means the launch path depends on SlowLock's process state, which the design forbids (depends on T042)
- [X] T045 Run manual section M7 in `manual-test-plan.md` to confirm feature 001 has not regressed — enumeration, ordering, search, and the empty/no-results states all behave as before (depends on T031)
- [ ] T046 Record behaviour under Xiaomi Dual Apps as tested or explicitly untested in `specs/002-shortcut-pinning/manual-test-plan.md`, as the constitution's manual-verification clause requires (depends on T042)
- [ ] T047 Run the constitution's build gate one final time — `./gradlew assembleDebug` and `./gradlew test` must both pass — and tick the sign-off checklist at the foot of `specs/002-shortcut-pinning/manual-test-plan.md`. Work must not be reported as done on an unverified build (depends on T042, T043, T044, T045, T046)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (Phase 3)**: Depends on Foundational. No dependency on US2 or US3
- **US2 (Phase 4)**: Depends on US1 — it adds the treatment row to the screen US1 creates
- **US3 (Phase 5)**: Depends on US1 for the same reason. Independent of US2
- **Support gate (Phase 6)**: Depends on US1 (needs `SlowLockRoot`) and on T029 for the file it shares
- **Polish (Phase 7)**: Depends on all desired stories being complete

### User Story Dependencies

Unlike a typical feature, US2 and US3 are **not** startable in parallel with US1: both are edits to
`ShortcutConfigScreen.kt`, which US1 creates. This is a two-screen Android app, not a service with
independent endpoints — the honest dependency graph is mostly a line. US2 and US3 are independent
**of each other** and touch disjoint parts of that file, so they can be worked in either order or
in parallel by two people.

### Within Each User Story

- The frozen contract (T004) before anything that references it
- Pure data and seams before the code that consumes them
- Screens before the root that composes them
- Build and test gates before the manual pass

### Parallel Opportunities

- T001 and T002 (different resource files)
- T004–T007 — the whole foundational core, four separate files with no interdependencies except T004's use of `ShortcutTarget` from T006
- T010 alongside any of T004–T009 — it is the only task touching `app/src/main/java/com/slowlock/apps/`
- T008 and T009 (different test files)
- T014 and T016 (different test files), alongside T015 and T017
- T037, T038, and T039 — three separate feature-001 documents

---

## Parallel Example: Phase 2 Foundational

```bash
# Four independent source files, no shared state:
Task: "Create ShortcutContract.kt with the frozen constants"
Task: "Create IconTreatment.kt with the three literal matrices"
Task: "Create ShortcutTarget.kt with injectable resolution"
Task: "Create PinSupport.kt wrapping isRequestPinShortcutSupported()"

# Then both test files together:
Task: "Create IconTreatmentTest.kt"
Task: "Create ShortcutTargetTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup — strings and theme
2. Phase 2: Foundational — **the frozen contract lands here; get it right before anything pins**
3. Phase 3: US1 — configuration screen, pinner, launch activity, root navigation
4. **STOP and VALIDATE**: pin a shortcut on a real device and tap it. If this does not work on two
   launchers, the product thesis is in question and the remaining phases can wait — that is the
   entire reason this feature was built before the configuration screen it will sit inside

### Incremental Delivery

1. Setup + Foundational → frozen contract and pure core in place, covered by tests
2. US1 → a working home-screen shortcut (MVP)
3. US2 → the shortcut is visually distinguishable from the target app's own icon
4. US3 → the screen is no longer a trap
5. Phase 6 → the app behaves on devices where pinning cannot work
6. Phase 7 → feature 001 reconciled, device matrix recorded, gates green

### Suggested stopping points

Phase 3 and Phase 5 are both demonstrable. Phase 4 alone is not shippable — a screen you cannot
back out of is worse than one without icon treatments.

---

## Notes

- `contracts/pinned-shortcut.md` is frozen. Everything else here is a draft and is expected to be
  replaced by the delay-configuration feature
- The four automated tests are not a formality: T014's rename guard is the only thing that turns a
  refactor into a build failure instead of a silent, permanent home-screen breakage
- Do not add a success message, toast, snackbar, or `IntentSender` callback to the create path,
  however wrong the silence feels — FR-012 and the spec's Accepted limitations are explicit, and
  the draft would rather under-promise than claim a success it cannot verify
- Do not add tracking of which apps have shortcuts. FR-027 forbids it, and any such record goes
  stale the moment the user deletes a shortcut from their launcher
- Commit after each task or logical group
