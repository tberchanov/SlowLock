---

description: "Task list for 004-visual-redesign"
---

# Tasks: Visual Redesign (Phase 1)

**Input**: Design documents from `/specs/004-visual-redesign/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/)

**Tests**: Two JVM test files, both required. The constitution mandates automated coverage for any
frozen persisted value and permits **JVM unit tests only** — instrumented suites are forbidden, and
an agent MUST NOT drive the connected device. Everything not covered by `SlowLockPaletteTest` or
`DelayRangeTest` is a numbered case in [manual-test-plan.md](./manual-test-plan.md), run by the
maintainer.

**Organization**: grouped by user story. Each story phase is a complete, independently shippable
increment.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallelizable — different file, no dependency on an incomplete task
- **[Story]**: US1–US5, mapping to the user stories in spec.md
- Every task names its exact file path

## Path Conventions

Single `:app` Gradle module. Sources under `app/src/main/java/com/slowlock/`, resources under
`app/src/main/res/`, JVM tests under `app/src/test/java/com/slowlock/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: get the typefaces into the build. Nothing else can be styled until they are there.

- [X] T001 Create the font resource directory `app/src/main/res/font/` and the licence directory `app/src/main/assets/licenses/`
- [X] T002 [P] Vendor Instrument Sans Regular, Medium and SemiBold from `Instrument/instrument-sans` (default branch **`master`**) `fonts/ttf/` into `app/src/main/res/font/` as `instrument_sans_regular.ttf`, `instrument_sans_medium.ttf`, `instrument_sans_semibold.ttf` (commands in quickstart.md §1)
- [X] T003 [P] Vendor JetBrains Mono Regular and Medium from the `JetBrains/JetBrainsMono` v2.304 release `fonts/ttf/` into `app/src/main/res/font/` as `jetbrains_mono_regular.ttf`, `jetbrains_mono_medium.ttf`
- [X] T004 [P] Vendor both SIL OFL 1.1 licence files into **`app/src/main/assets/licenses/`** as `OFL-InstrumentSans.txt` and `OFL-JetBrainsMono.txt` — **not** `res/font/`, which accepts only `.xml`/`.ttf`/`.ttc`/`.otf` and fails the resource merger on a `.txt`
- [X] T005 Verify the vendored payload totals 807,920 bytes and that `./gradlew assembleDebug` still passes (contract C7, SC-007)

> **Do not substitute Google Fonts as the source.** It publishes only variable builds of both
> families — no static weights exist there (research R1). Lowercase-underscore filenames are
> mandatory; Android rejects the resource names otherwise. Licences go in `assets/`, never
> `res/font/`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: the token set, the theme derived from it, and the four shared composables. Every user
story below depends on this phase.

**⚠️ CRITICAL**: no user story work begins until T017 passes.

- [X] T006 Rewrite `app/src/main/java/com/slowlock/ui/theme/Color.kt` with the eleven tokens from contract C1, deleting the `Purple*`/`Pink*`/`PurpleGrey*` scaffolding entirely
- [X] T007 [P] Create `app/src/main/java/com/slowlock/ui/theme/Shape.kt` mapping the design's five radii onto Material 3's five slots — `extraSmall` 12dp, `small` 14dp, `medium` 16dp, `large` 18dp, `extraLarge` 24dp — plus a separate `Pill` (contract C9)
- [X] T008 [P] Add `android:windowBackground` pointing at the screen ground to `Theme.SlowLock` in `app/src/main/res/values/themes.xml`, leaving its light parent unchanged (contract C5, research R5)
- [X] T009 Rewrite `app/src/main/java/com/slowlock/ui/theme/Type.kt`: declare both `FontFamily` values from the vendored resource fonts, build the Material 3 `Typography` in Instrument Sans, and expose the `readout` and `mono` roles as explicit `TextStyle`s — **sizes, weights and letter-spacing per the table in data-model §2**, which is part of contract C6 (contract C6, data-model §2)
- [X] T010 Rewrite `app/src/main/java/com/slowlock/ui/theme/Theme.kt`: **delete the `dynamicColor` and `darkTheme` parameters outright**, derive a single light `ColorScheme` from the tokens, and wire `Typography` and `Shapes` into `MaterialTheme` (FR-001, FR-008, contract C4, research R5)
- [X] T011 Create `app/src/test/java/com/slowlock/ui/theme/SlowLockPaletteTest.kt` asserting the palette contains exactly eleven tokens, each against its literal hex, and computing WCAG 2.1 contrast for every declared text-on-surface pairing with a ≥4.5:1 floor (FR-002, FR-009, SC-008, SC-009, research R13/R14)
- [X] T012 Verify `SlowLockPaletteTest` fails when a twelfth token is added or a literal is altered, then restore — this test is the enforcement mechanism for SC-009, and a test that cannot fail enforces nothing
- [X] T013 [P] Create `app/src/main/java/com/slowlock/ui/components/ScreenHeader.kt` per contract U1: 40dp square tile at `small` radius with a `Card` fill **and a `Line` hairline border**, title at 22sp / −0.01em, `onBack: (() -> Unit)?` where null renders no tile and no leading space, and a 48dp touch target on the 40dp tile
- [X] T014 [P] Create `app/src/main/java/com/slowlock/ui/components/Actions.kt` with `PrimaryAction` (56dp, `medium` radius, amber fill, ink label) and `SecondaryAction` (52dp, outlined) per contract U2/U3
- [X] T015 [P] Create `app/src/main/java/com/slowlock/ui/components/SelectableTile.kt` per contract U4: `Modifier.selectable` with `Role.RadioButton`, a **mandatory** `contentDescription`, and the drawn size with no 48dp enforcement (FR-043, FR-044, FR-045, research R9)
- [X] T016 Confirm no file in `app/src/main/java/com/slowlock/ui/components/` declares a colour, dimension or type literal of its own (contract U5, C1)
- [X] T017 Run `./gradlew assembleDebug` and `./gradlew test` — both must pass, including every pre-existing test

**Checkpoint**: the whole app has changed character. Material 3 components across all five screens
now render in the palette even before any screen is re-laid-out.

---

## Phase 3: User Story 1 — The app looks like itself (Priority: P1) 🎯 MVP

**Goal**: the app has its own identity on every device, and the app list is laid out to the canvas.

**Independent Test**: install on two devices with different wallpapers and system accent colours —
every screen renders identically. Switch the system to dark mode; the four in-app screens stay
light and stay legible. In airplane mode on first launch, the SlowLock typefaces are used with no
reflow.

- [X] T018 [US1] Replace the header row in `app/src/main/java/com/slowlock/apps/AppListScreen.kt` with `ScreenHeader(title = "Choose an app", onBack = null)` — **no back tile and no step counter**, both of which are Phase 2 of the redesign (FR-010, contract S1)
- [X] T019 [US1] Restyle `SearchField` in `app/src/main/java/com/slowlock/apps/AppListScreen.kt` to 52dp at the `field` radius with a card fill, a `Line` hairline border and a 16sp `Ink40` placeholder, keeping the existing placeholder and clear affordance (FR-011)
- [X] T020 [US1] Restyle `AppRow` in `app/src/main/java/com/slowlock/apps/AppListScreen.kt` to 64dp with a 44dp icon at `small` radius, a 17sp label, and a **1dp `Fill` divider** between rows — the canvas uses `#E7E2D7` here, *not* the `Line` hairline used elsewhere (FR-012, contract C9)
- [X] T021 [US1] Restyle the loading, empty and no-results states in `app/src/main/java/com/slowlock/apps/AppListScreen.kt` to the new palette and type roles, and point the icon placeholder at the `Fill` token — **wording unchanged** (FR-013)
- [X] T022 [US1] Confirm `app/src/main/java/com/slowlock/apps/AppListViewModel.kt`, `InstalledAppsSource.kt` and `AppIconCache.kt` are untouched in the diff (contract S6)
- [ ] T023 [US1] **MAINTAINER** — run manual cases **M1.1–M1.8** and **M2.1–M2.8** on a device, recording each pass or fail. The constitution forbids an agent driving the device, so this is not an agent task
- [ ] T024 [US1] **BLOCKED ON T023** — if M1.5 shows missing-glyph boxes on a non-Latin app label, apply the research R3 contingency — app labels only fall back to the platform default family — and re-run M1.5 (FR-005)

**Checkpoint**: US1 is shippable on its own. The app is warm bone and amber on every device.

---

## Phase 4: User Story 2 — The delay is the point of the screen (Priority: P2)

**Goal**: the chosen delay is the largest thing on screen, with three one-tap presets.

**Independent Test**: open the delay screen, tap `10s` — the readout reads 10 and the slider has
moved. Drag to 17 — no preset is highlighted. This is the only story with new behaviour.

- [X] T025 [P] [US2] Add `PRESETS: List<Int>` (5, 10, 30) and `presetFor(seconds: Int): Int?` to `app/src/main/java/com/slowlock/delay/DelayRange.kt` as **additive** API — `MIN_SECONDS`, `MAX_SECONDS`, `STEP_SECONDS`, `STOPS`, `SLIDER_STEPS` and `snap` are unchanged (FR-017, FR-019, contract S2)
- [X] T026 [US2] Extend `app/src/test/java/com/slowlock/delay/DelayRangeTest.kt`: assert `PRESETS` against the literals `[5, 10, 30]`, that every preset lies within `MIN_SECONDS..MAX_SECONDS`, that every preset is `snap`-stable (`snap(p) == p`), and that `presetFor` returns null for a non-preset value (data-model §4)
- [X] T027 [P] [US2] In `app/src/main/res/values/strings.xml`, change `delay_config_next` to "Choose the icon" and add the `SECONDS` caption, the three preset labels, and a content description per preset — capitalised strings stored capitalised, with a translator comment saying so (FR-020, contract C8, research R11/R12)
- [X] T028 [US2] Replace the header in `app/src/main/java/com/slowlock/delay/DelayConfigScreen.kt` with `ScreenHeader("Wait before opening", onBack)` (contract S2)
- [X] T029 [US2] Add the app pill above the readout in `app/src/main/java/com/slowlock/delay/DelayConfigScreen.kt` — `pill` shape, card fill, hairline border, icon plus label (FR-015)
- [X] T030 [US2] Replace the readout in `app/src/main/java/com/slowlock/delay/DelayConfigScreen.kt` with an auto-sizing `readout`-role numeral inside the `weight`-bounded centre block, capped at 104sp, with the `SECONDS` caption beneath (FR-014, FR-014a, contract C11, research R10)
- [X] T031 [US2] Pass custom `track` and `thumb` slot content to the existing Material 3 `Slider` in `app/src/main/java/com/slowlock/delay/DelayConfigScreen.kt` — 6dp track at 3dp radius with an amber active portion and a `Fill` inactive portion, and a 26dp card-filled thumb with a 3dp amber ring — plus `1s`/`30s` end labels in `mono` 11sp. **Do not replace the `Slider`** (FR-016, research R8, contract C9)
- [X] T032 [US2] Add the preset row to `app/src/main/java/com/slowlock/delay/DelayConfigScreen.kt`: a `Row` carrying `Modifier.selectableGroup()` with three `SelectableTile`s, each selected iff `DelayRange.presetFor(seconds) == its value`. **Derive selection; store no "selected preset" state** (FR-017, FR-018, contract S2)
- [X] T033 [US2] Swap the primary action in `app/src/main/java/com/slowlock/delay/DelayConfigScreen.kt` for `PrimaryAction` labelled from `delay_config_next` (FR-020)
- [X] T034 [US2] Confirm `DelayConfigScreen` still owns no state, still never touches `DelayConfigStore`, and still routes every value change through `onSecondsChange` (contract S2, FR-021)
- [X] T035 [US2] `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` pass (55 tests, 0 failures)
- [ ] T035a [US2] **MAINTAINER** — run manual cases **M3.1–M3.12** on a device, recording each pass or fail

**Checkpoint**: US2 is shippable. Setting a common delay is one tap (SC-003).

---

## Phase 5: User Story 3 — The icon step reads as a preview and a choice (Priority: P3)

**Goal**: the last step shows the lock as it will appear, with treatments as tiles.

**Independent Test**: open the icon step, tap each treatment — the preview icon recolours, and the
delay line beneath the label matches the value chosen on the previous screen.

- [X] T036 [P] [US3] In `app/src/main/res/values/strings.xml`, change `shortcut_config_title` to "New lock", `shortcut_config_create` to "Add to home screen", and reword `shortcut_target_unavailable` and `shortcut_icon_unavailable` to say "lock" instead of "shortcut"; add the `ICON` eyebrow and the launcher-confirms footnote. **Every `name=` attribute stays exactly as it is** (FR-041, FR-042, research R12)
- [X] T037 [US3] Replace the header in `app/src/main/java/com/slowlock/shortcut/ShortcutConfigScreen.kt` with `ScreenHeader("New lock", onBack)` — **deliberately diverging from the canvas**, which still reads "New shortcut"; the terminology decision post-dates the artboard (FR-041, contract S3)
- [X] T038 [US3] Replace `Preview` in `app/src/main/java/com/slowlock/shortcut/ShortcutConfigScreen.kt` with a bordered card — `extraLarge` radius, card fill, hairline border, 96dp icon, label, and the chosen delay written out in the `mono` role at 12sp in **`AmberDark`** from the existing `delay_seconds` plural (FR-022)
- [X] T039 [US3] Replace `TreatmentRow`'s `FilterChip`s in `app/src/main/java/com/slowlock/shortcut/ShortcutConfigScreen.kt` with three equal-width `SelectableTile`s in a `selectableGroup` at `field` radius with 12dp padding, each a 36dp swatch at 11dp radius above a 13sp name, beneath the `ICON` eyebrow in `mono` 11sp / 0.14em (FR-023, FR-024, contract C9)
- [X] T040 [US3] Verify the treatment tiles announce their selected state to a screen reader — the semantics `FilterChip` carried must not be lost in the swap (FR-043, contract S3)
- [X] T041 [US3] Swap the primary action in `app/src/main/java/com/slowlock/shortcut/ShortcutConfigScreen.kt` for `PrimaryAction` labelled "Add to home screen", with the `mono` footnote beneath it (FR-025)
- [X] T042 [US3] Confirm the `create` path in `app/src/main/java/com/slowlock/shortcut/ShortcutConfigScreen.kt` is unchanged: resolve fresh → save → pin → `onCreated`, in that order, with both failure messages intact (FR-026, contract S3)
- [ ] T043 [US3] **MAINTAINER** — run manual cases **M4.1–M4.10** on a device, recording each pass or fail

**Checkpoint**: US3 is shippable. The full three-step flow is redesigned end to end.

---

## Phase 6: User Story 4 — The wait is quiet, but it is SlowLock's (Priority: P4)

**Goal**: the wait screen joins the palette without gaining a single moving part.

**Independent Test**: tap a pinned icon in a dark room in dark mode — the screen is dark, not a
white field. Tap one in daylight in light mode — paper. In both, watch the whole delay and confirm
nothing changes.

**⚠️ Tightest constraints in the feature.** Restyled, not re-mechanised.

- [X] T044 [P] [US4] In `app/src/main/res/values/colors.xml`, repoint `wait_background` to the screen ground and `wait_text` to muted ink, and add `wait_rule` as the accent — replacing the comment block that describes the screen as unbranded (contract C4)
- [X] T045 [P] [US4] In `app/src/main/res/values-night/colors.xml`, set the dark triple: `wait_background` `#14120E`, `wait_text` `#8A857A`, `wait_rule` `#C9821F`. **This file is not optional** (FR-031, research R7)
- [X] T046 [P] [US4] Change `wait_message` in `app/src/main/res/values/strings.xml` to the lower-case literal `please wait` — stored lower-case, never transformed at display time (FR-028, contract C8)
- [X] T047 [US4] Add the 40×2dp accent rule at 55% opacity above the message in `app/src/main/java/com/slowlock/delay/WaitScreen.kt`, and set the message in the `mono` role resolved directly from the font resource — **without `MaterialTheme` in scope** (FR-027, FR-033, contract S4)
- [X] T048 [US4] Verify `app/src/main/java/com/slowlock/delay/WaitScreen.kt` contains no `LaunchedEffect`, no `produceState`, no store read, no icon load, no label lookup, and no animation API — the screen must arrive complete in one frame (FR-029, research R6, contract S4)
- [X] T049 [US4] Verify `app/src/main/java/com/slowlock/delay/WaitScreen.kt` imports nothing from `com.slowlock.ui.components` — its isolation is deliberate and must survive refactoring (FR-033, contract U5)
- [X] T050 [US4] Confirm `app/src/main/java/com/slowlock/shortcut/ShortcutLaunchActivity.kt`, `app/src/main/java/com/slowlock/delay/WaitTiming.kt` and `app/src/main/AndroidManifest.xml` are untouched in the diff (contract S6)
- [X] T051 [US4] Amend feature 003: update FR-022 and FR-023 in `specs/003-launch-delay/spec.md` and the appearance obligations in `specs/003-launch-delay/contracts/wait-screen.md` so the binding property is **static and not worth reading twice** rather than *unbranded*, pointing at this feature's FR-037 (FR-037, plan Recorded rulings)
- [X] T052 [US4] Re-point any case in `specs/003-launch-delay/manual-test-plan.md` that asserts the old wait-screen appearance at this feature's M5 tier (FR-037)
- [ ] T053 [US4] **MAINTAINER** — run manual cases **M5.1–M5.13**, recording each pass or fail. M5.13 — an icon pinned before this feature — is the one that proves FR-040

**Checkpoint**: US4 is shippable. The screen users meet most is redesigned and still motionless.

---

## Phase 7: User Story 5 — The unsupported-launcher screen speaks the same language (Priority: P5)

**Goal**: the dead end reads as a message from the app rather than a system error.

**Independent Test**: set a launcher that does not support pinning, open the app — the screen
renders in the new palette with both actions and unchanged behaviour.

- [X] T054 [P] [US5] Add the capitalised `NO ROOM ON THE HOME SCREEN` eyebrow string to `app/src/main/res/values/strings.xml` with a translator comment noting the capitalisation is stylistic (FR-034, contract C8)
- [X] T055 [US5] Render that eyebrow in `mono` 12sp at 0.14em in **`AmberDark`** — the canvas accents it, it is not muted ink (data-model §2)
- [X] T056 [US5] Restyle `app/src/main/java/com/slowlock/shortcut/PinUnsupportedScreen.kt` to left alignment with the `mono` eyebrow above the message and the message in the `body` role at 22sp (FR-034)
- [X] T057 [US5] Replace the buttons in `app/src/main/java/com/slowlock/shortcut/PinUnsupportedScreen.kt` with `PrimaryAction` ("Choose home screen app") then `SecondaryAction` ("Check again"), in that order (FR-035, contracts U2/U3)
- [X] T058 [US5] Confirm the `runCatching` around the settings intent, the fallback message, and the re-check callback are unchanged (FR-036, contract S5)
- [ ] T059 [US5] Run manual cases **M6.1–M6.4**, recording each pass or fail

**Checkpoint**: all five screens redesigned.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T060 Sweep `app/src/main/res/values/strings.xml` for the word "shortcut" in any string **value**; confirm the only remaining occurrences are resource `name=` attributes (FR-041, FR-042, research R12)
- [X] T061 [P] Run `git diff --stat` against the feature's base commit and confirm every file in contract S6's must-not-appear list is absent — `SlowLockRoot.kt`, `ShortcutContract.kt`, `ShortcutPinner.kt`, `DelayConfigStore.kt`, `WaitTiming.kt`, `AndroidManifest.xml`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `res/mipmap-*/`, `res/drawable/ic_launcher_*`, and features 002/003 contracts
- [X] T062 [P] Run the frozen-value guard suite unmodified: `./gradlew testDebugUnitTest --tests '*ShortcutContractTest' --tests '*DelayConfigTest' --tests '*IconTreatmentTest' --tests '*WaitTimingTest'` (the aggregate `test` task rejects `--tests`) (FR-038, FR-042)
- [X] T063 Run the full gates: `./gradlew assembleDebug` and `./gradlew test`
- [X] T064 Compare the debug APK against one built from the base commit; confirm growth is under 790 KiB and that typography accounts for all of it (SC-007, quickstart §3)
- [ ] T065 Run manual cases **M7.1–M7.5** — largest font scale on the smallest device, RTL, and a very long app label (FR-014a, SC-010)
- [ ] T066 Run manual cases **M8.1–M8.7** with a screen reader active, recording M8.5's known 44dp limitation as an observation rather than a defect (FR-043–FR-045, SC-008, SC-011)
- [ ] T067 Record the release gate from the constitution: lock creation verified on at least one non-Pixel OEM device, and Xiaomi Dual Apps behaviour recorded as tested or explicitly untested

---

## Dependencies

```text
Phase 1 (Setup, T001-T005)
        │  fonts must exist before Type.kt can reference them
        ▼
Phase 2 (Foundational, T006-T017)   ⚠️ BLOCKS EVERYTHING
        │  T006 Color → T009 Type → T010 Theme  (strictly sequential)
        │  T007, T008 parallel with the above
        │  T011/T012 need T006; T013-T015 need T010
        ▼
   ┌────┴────┬─────────┬─────────┬─────────┐
   ▼         ▼         ▼         ▼         ▼
 US1(P1)   US2(P2)   US3(P3)   US4(P4)   US5(P5)
 T018-24   T025-35   T036-43   T044-53   T054-58
   └────┬────┴─────────┴─────────┴─────────┘
        ▼
Phase 8 (Polish, T060-T067)
```

**Story independence**: US1–US5 touch disjoint screen files and can be built in any order, or in
parallel by different people, once Phase 2 lands. The priority order given is the recommended
sequence, not a dependency chain.

**The one cross-story ordering worth respecting**: US4 (wait screen) last among the five. It has
the tightest constraints, the least room to absorb a surprise, and it is the only story that
amends another feature's spec (T051, T052).

---

## Parallel Execution Examples

**Phase 1** — T002, T003, T004 after T001: three independent downloads.

**Phase 2** — T007 (`Shape.kt`) and T008 (`themes.xml`) run alongside the strictly sequential
`Color.kt → Type.kt → Theme.kt` chain. After T010, the three component files T013, T014, T015 are
fully independent.

**Within each story** — the string-resource task is marked `[P]` because it touches
`strings.xml` while the screen tasks touch Kotlin files: T027, T036, T044/T045/T046, T054.

**Across stories** — after T017, five people could take one story each. In practice the string
resources are the only shared file, which is why each story's string edits are batched into a
single task.

---

## Implementation Strategy

**MVP = Phase 1 + Phase 2 + Phase 3 (US1)** — T001 through T024. That delivers the whole point of
the feature: SlowLock stops borrowing its colours from the wallpaper and becomes the same warm,
quiet app on every device. All five screens already change appearance at T017, because Material 3
components pick up the new scheme before any screen is re-laid-out.

**Incremental delivery**: each of Phases 4–7 is one screen, shippable alone, with its own manual
tier. Stop after any of them and the app is coherent.

**Two things to keep an eye on while building:**

- **T012 is not busywork.** `SlowLockPaletteTest` is the only mechanism enforcing SC-009's "no
  twelfth colour" rule. A test that cannot fail enforces nothing, so prove it fails before relying
  on it.
- **The rename is the sharpest edge.** `ShortcutLaunchActivity`'s fully-qualified name is written
  into every icon already sitting on a user's home screen. T060 and T062 exist so that a rename
  which reached an identifier fails the build rather than a stranger's phone.

**Out of scope — do not build**: the first-run screen, the Locks home screen, the app list's back
tile, the `1 / 3` step counters, anything that enumerates locks, a dark palette for the four
in-app screens, and **the launcher icon** — the design source's five icon directions are
unresolved sketches and no phase owns them yet. See spec.md **Out of Scope**.
