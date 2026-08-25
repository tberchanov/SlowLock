---
description: "Task list for 007-locks-screen-polish"
---

# Tasks: Legible system bar and a redesigned Locks screen

**Input**: Design documents from `/specs/007-locks-screen-polish/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/)

**Tests**: No automated test tasks. This feature is presentation without branching; none of the
constitution's three mandatory automated-coverage areas (schedule logic, target resolution, frozen
persisted values) applies, and the existing `SlowLockPaletteTest` already covers this feature's
design-system obligations without modification (research R8). Instrumented tests are forbidden
outright. Everything device-observable is a numbered case in
[manual-test-plan.md](./manual-test-plan.md), run by the maintainer.

**Organization**: One phase per user story, in priority order. Each phase ends in a state the
maintainer can build, run, and judge on its own.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallelizable — different file, no dependency on an incomplete task
- **[Story]**: US1 / US2 / US3, mapping to the user stories in `spec.md`

## Path Conventions

Single `:app` module. Kotlin under `app/src/main/java/com/slowlock/`, resources under
`app/src/main/res/`, JVM tests under `app/src/test/java/com/slowlock/`.

---

## Phase 1: Setup

**Purpose**: know the tree was green before anything moved, so a later failure is attributable.

- [X] T001 Run `./gradlew test assembleDebug` from the repository root `/Users/anatolii/Projects/SlowLock` and confirm both pass before editing anything

---

## Phase 2: Foundational (Blocking Prerequisites)

**None.** This feature adds no schema, no framework, no shared abstraction, and no dependency.
Every file it touches already exists and each user story edits its own. Go straight to Phase 3.

---

## Phase 3: User Story 1 — The clock and battery stay readable (Priority: P1) 🎯 MVP

**Goal**: the system indicators render dark over the app's bone ground on every app screen, and
stop changing with the device's light/dark setting.

**Independent test**: set the device to dark mode, open the app, read the clock and battery. Then
switch to light mode — nothing changes. Covers manual cases M1–M4.

- [X] T002 [US1] In `app/src/main/java/com/slowlock/MainActivity.kt`, replace the bare `enableEdgeToEdge()` call with `enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT), navigationBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT))`, importing `androidx.activity.SystemBarStyle` and `android.graphics.Color.TRANSPARENT`
- [X] T003 [US1] In `app/src/main/java/com/slowlock/MainActivity.kt`, add the call-site comment contract S2/S3 requires: that `light` names the bar's *background* and is therefore what produces dark glyphs, and that the device's night setting is deliberately not an input because the app is light-only (004 FR-008)
- [X] T004 [US1] Grep `app/src/main/` for `isAppearanceLight`, `statusBarColor`, `navigationBarColor`, `SystemBarStyle` and `isSystemInDarkTheme` and confirm `MainActivity.kt` is the only writer (contract S1); confirm `app/src/main/java/com/slowlock/shortcut/ShortcutLaunchActivity.kt` still does not call `enableEdgeToEdge` (contract S5, FR-004)
- [X] T005 [US1] Run `./gradlew assembleDebug` from `/Users/anatolii/Projects/SlowLock`
- [ ] T006 [US1] Hand manual cases M1, M2, M3 and M4 in `specs/007-locks-screen-polish/manual-test-plan.md` to the maintainer and wait — do not drive the device (constitution, Testing expectations)

**Checkpoint**: User Story 1 is shippable on its own. Nothing below is required for it.

---

## Phase 4: User Story 2 — The Locks screen reads like the design (Priority: P2)

**Goal**: the Locks screen gets its own large title and mono count caption in place of the flow
screens' `ScreenHeader`.

**Independent test**: open the app with at least one lock and hold the `New · Locks` artboard beside
the heading block. Covers manual case M5.

- [X] T007 [P] [US2] In `app/src/main/java/com/slowlock/ui/theme/Type.kt`, add `TitleDisplay` (Instrument Sans, Medium, 30sp, −0.015em) and `Count` (JetBrains Mono, Regular, 12sp, +0.06em) to `SlowLockType`, per `data-model.md` §2, each with a KDoc naming its one caller and why it is a sibling of `Title` / `Eyebrow` rather than an edit to it (research R3)
- [X] T008 [P] [US2] In `app/src/main/res/values/strings.xml`, change `locks_title` from "Your locks" to "Locks" (FR-007) and update its comment to point at the `New · Locks` artboard
- [X] T009 [US2] In `app/src/main/java/com/slowlock/locks/LocksScreen.kt`, replace the `ScreenHeader(...)` call and the existing count `Text` with the heading block from `data-model.md` §4: title in `TitleDisplay` on `onBackground`, 4dp gap, caption in `Count` on `outline`, 20dp gap before the list
- [X] T010 [US2] In `app/src/main/res/values/strings.xml` add the capitalised plural `locks_count_caption`, and in `app/src/main/java/com/slowlock/locks/LocksScreen.kt` draw it while setting `contentDescription` to the ordinary `locks_count`, so a screen reader hears words rather than capitals (contract L4, FR-008, FR-012) — **amended during implementation**: this task originally called for `uppercase()` at draw time, which feature 004's contract C8 forbids as a locale trap; two resources is what C8 requires and what shipped
- [X] T011 [US2] In `app/src/main/java/com/slowlock/locks/LocksScreen.kt`, update the file's KDoc to record that this screen now draws its own heading and that `ScreenHeader` was deliberately not generalised (contract L1), and that the caption still states the count alone (contract L3, 005 FR-011)
- [X] T012 [US2] Confirm `app/src/main/java/com/slowlock/ui/components/ScreenHeader.kt` is unmodified and that `git diff --stat` shows no other screen touched (contract L12, FR-025)
- [X] T013 [US2] Run `./gradlew test assembleDebug` from `/Users/anatolii/Projects/SlowLock` — `SlowLockPaletteTest`'s source scan must still pass against the new code
- [ ] T014 [US2] Hand manual case M5 in `specs/007-locks-screen-polish/manual-test-plan.md` to the maintainer and wait

**Checkpoint**: the heading matches the artboard. The rows are still the old ones, and the screen is
coherent in that state.

---

## Phase 5: User Story 3 — Each lock reads as a card with its delay called out (Priority: P2)

**Goal**: the row's second line carries the treatment alone and the delay moves into an amber badge
at the trailing edge, at the artboard's sizes.

**Independent test**: with three locks at different delays and treatments, compare a row against the
artboard. Covers manual cases M6–M11.

- [X] T015 [P] [US3] In `app/src/main/java/com/slowlock/ui/theme/Type.kt`, add `RowTitle` (Instrument Sans, Medium, 17sp) and `Badge` (JetBrains Mono, Medium, 15sp) to `SlowLockType`, per `data-model.md` §2, with a KDoc on `RowTitle` stating why `RowLabel` is left at Regular for the app list (research R3, FR-025)
- [X] T016 [P] [US3] In `app/src/main/java/com/slowlock/ui/theme/Shape.kt`, add `val Badge = RoundedCornerShape(9.dp)` beside `Pill`, outside the five Material slots, with a KDoc citing the `Pill` precedent and contract C9 (research R4)
- [X] T017 [P] [US3] In `app/src/main/res/values/strings.xml`, add `locks_delay_badge` as `%1$ds` with a comment explaining it is the badge's compact form, that it is translatable because the unit abbreviation is not universal, and that the spoken form is the existing `delay_wait` plural
- [X] T018 [US3] In `app/src/main/java/com/slowlock/locks/LocksScreen.kt`, change `AvailableRowText` so line 1 uses `SlowLockType.RowTitle` and line 2 states `stringResource(lock.treatment.labelRes)` alone, with a 3dp gap (contract L9, FR-014)
- [X] T019 [US3] In `app/src/main/java/com/slowlock/locks/LocksScreen.kt`, add a private `DelayBadge` composable — `Badge` shape, `primaryContainer` fill (AmberWash), `onPrimaryContainer` text (AmberDark), 9dp × 5dp padding, `SlowLockType.Badge` — and place it at the trailing edge of `LockRow` for available rows only, after an 8dp spacer, non-shrinking so a long app name yields first (FR-015, FR-017, contract L6, L10)
- [X] T020 [US3] In `app/src/main/java/com/slowlock/locks/LocksScreen.kt`, give the badge a `contentDescription` holding the existing `delay_wait` plural so the row speaks "10 second wait" while showing "10s" (FR-018, contract L7, research R6)
- [X] T021 [US3] In `app/src/main/java/com/slowlock/locks/LocksScreen.kt`, set the row's internal padding to 14dp, the icon-to-body gap to 14dp, `ICON_SIZE` to 48dp, and clip the icon *placeholder* with `MaterialTheme.shapes.small`; leave the loaded bitmap unclipped and leave `ROW_MIN_HEIGHT` a `heightIn` minimum (data-model §5, research R5, contract L11)
- [X] T022 [US3] In `app/src/main/res/values/strings.xml`, delete `locks_row_detail` and its comment, then grep `app/src/main/` to confirm it has no remaining caller (research R7, contract L9)
- [X] T023 [US3] In `app/src/main/java/com/slowlock/locks/LocksScreen.kt`, update the `LockRow` and `AvailableRowText` KDoc to record that the delay moved to the badge, that this amends feature 005's contract K row layout, and that the unavailable row is deliberately untouched (contract L9, L10, FR-020)
- [X] T024 [US3] Run `./gradlew test assembleDebug` from `/Users/anatolii/Projects/SlowLock`
- [ ] T025 [US3] Hand manual cases M6, M7, M8, M9, M10 and M11 in `specs/007-locks-screen-polish/manual-test-plan.md` to the maintainer and wait

**Checkpoint**: all three stories are in. The screen matches the artboard.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T026 [P] Hand manual case M12 in `specs/007-locks-screen-polish/manual-test-plan.md` to the maintainer — every other screen pixel-identical apart from its system bars (FR-025, contract L12)
- [ ] T027 [P] Hand manual case M13 in `specs/007-locks-screen-polish/manual-test-plan.md` to the maintainer — RTL puts the icon right and the badge left
- [ ] T028 [P] Hand manual case M14 in `specs/007-locks-screen-polish/manual-test-plan.md` to the maintainer — the API 26 navigation bar, to be recorded as tested or as untested-and-accepted (contract S7)
- [ ] T029 Fill in the Results table in `specs/007-locks-screen-polish/manual-test-plan.md` with what the maintainer reports, marking anything not run as not run rather than as passing
- [X] T030 Run `./gradlew test assembleDebug` from `/Users/anatolii/Projects/SlowLock` one final time and report the actual output
- [X] T031 Report to the maintainer what changed, in which files, and stop — leave the work uncommitted. Committing, pushing, branching and tagging are the maintainer's, and a task list is not authorization (constitution 1.2.0, "Version control is the maintainer's, not the agent's")

---

## Dependencies

```text
Setup (T001)
   ↓
US1 (T002–T006)   ── independent, shippable alone ── 🎯 MVP
   ↓
US2 (T007–T014)   ── heading; touches Type.kt + LocksScreen.kt + strings.xml
   ↓
US3 (T015–T025)   ── rows;    touches Type.kt + LocksScreen.kt + strings.xml + Shape.kt
   ↓
Polish (T026–T031)
```

**Why US2 precedes US3 rather than running beside it**: both edit
`app/src/main/java/com/slowlock/ui/theme/Type.kt`, `app/src/main/java/com/slowlock/locks/LocksScreen.kt`
and `app/src/main/res/values/strings.xml`. They are independent in *value* — either can ship without
the other and each has its own manual case — but not in *file*, so they are ordered.

**US1 depends on nothing** and shares no file with either. It can be done first, last, or by
someone else entirely.

## Parallel execution

Within a phase, the `[P]` tasks touch different files and can be done together:

- **US2**: T007 (`Type.kt`) and T008 (`strings.xml`) in parallel; T009–T011 then follow in
  `LocksScreen.kt`.
- **US3**: T015 (`Type.kt`), T016 (`Shape.kt`) and T017 (`strings.xml`) in parallel; T018–T023 then
  follow, all in `LocksScreen.kt` and so strictly sequential among themselves.
- **Polish**: T026, T027 and T028 are three independent manual cases and can be run in any order or
  together.

Across phases, nothing is parallel — see Dependencies.

## Implementation strategy

**MVP is User Story 1 alone.** It is one call site, it fixes the defect the user actually reported
first, it is verifiable in thirty seconds on a dark-mode phone, and it carries no design risk. Ship
or review it before starting the redesign.

**Then US2, then US3.** Each ends at a checkpoint where the app builds, runs, and looks coherent —
the heading can match the artboard while the rows still do not, and that intermediate state is not
broken, just partly redesigned.

**Nothing here is complete until the maintainer has run the manual plan.** Every case is on a real
device, by the maintainer, and no agent may pre-verify one by driving the device.
