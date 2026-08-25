# Implementation Plan: Legible system bar and a redesigned Locks screen

**Branch**: `main` (no feature branch; version control is the maintainer's — constitution 1.2.0) | **Date**: 2026-08-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/007-locks-screen-polish/spec.md`

## Summary

Two presentation defects, one root cause each, five files.

The system indicators turn white on dark-mode devices because `enableEdgeToEdge()` defaults to a
style that reads the device's night setting — and the app is light-only by design. Passing an
explicit `SystemBarStyle.light(...)` for both bars removes the device setting from the decision
entirely. That is the whole of User Story 1.

The Locks screen is then rebuilt from the `New · Locks` artboard: its own large title and mono
count caption in place of the flow screens' `ScreenHeader`, and a row whose second line carries the
treatment alone while the delay moves into an amber badge at the trailing edge. Four type roles and
one corner radius are added centrally; **no colour is added**, because every pairing the new screen
needs is already declared and already passing the contrast test.

No behaviour, navigation, storage, permission or dependency changes. No entity changes.

## Technical Context

**Language/Version**: Kotlin 2.2.10, JVM target 11

**Primary Dependencies**: Jetpack Compose (BOM 2026.02.01), Material 3, `androidx.activity:activity-compose:1.8.0`. **Nothing is added** — `SystemBarStyle` has shipped in `androidx.activity` since 1.8.0.

**Storage**: N/A — this feature persists nothing and reads nothing new.

**Testing**: JVM unit tests only (`./gradlew test`). The existing `SlowLockPaletteTest` covers this feature's design-system obligations without modification; no new test file is required (research R8). Instrumented tests are forbidden by the constitution. Everything device-observable goes to `manual-test-plan.md`.

**Target Platform**: Android, `minSdk 26`, `targetSdk`/`compileSdk` 37.

**Project Type**: Mobile app, single `:app` module.

**Performance Goals**: No regression. The row gains one small `Text`; the list is expected to hold tens of rows, not hundreds.

**Constraints**: Eleven-colour palette, frozen (contract C1, FR-022). No screen may declare a colour, type style, or radius of its own (FR-024). No screen outside the Locks screen may change appearance apart from its system bars (FR-025, contract L12). The wait screen is out of scope and must stay out (FR-004, contract S5).

**Scale/Scope**: One screen redesigned, one activity line changed, two theme files extended, three string resources touched.

## Constitution Check

*Evaluated against constitution 1.2.0 before Phase 0, and re-checked after Phase 1 — see the second column.*

| Principle | Pre-Phase 0 | Post-Phase 1 | Basis |
|---|---|---|---|
| **I. Cooperative User, Not Adversary** | PASS | PASS | Nothing is enforced, blocked or counted. The one place this principle bites is the count caption: the artboard reads "3 ON YOUR HOME SCREEN" and contract L3 keeps the second half unshipped, because the app cannot know it. The design was adopted for its styling and refused for its claim. |
| **II. Simplicity First (YAGNI)** | PASS | PASS | No dependency, no module, no abstraction. `ScreenHeader` is explicitly *not* generalised into a large-title variant (contract L1) — the Locks screen draws its own heading, which is smaller than parameterising a component with three other callers. Four type roles and one radius are additions to existing central files, following the precedent `Pill` already set. |
| **III. Permission & Policy Minimalism** | PASS | PASS | No permission is added, requested, or implied. |
| **IV. Platform-Idiomatic Android** | PASS | PASS | Compose + Material 3 throughout, no XML layout. The bar fix uses `androidx.activity`'s own API rather than writing platform bits by hand (research R1, contract S1). No service, no polling, no wake lock, no background start. The API 26 navigation-bar gap is accepted and recorded rather than hacked around (contract S7). |
| **V. Stable Identifiers** | PASS | PASS | No identifier is persisted, matched, or compared. The row still keys on `packageName`; the label is still display-only and still resolved fresh; the icon cache is still keyed by package + version. |

**Build gate**: `./gradlew assembleDebug` and `./gradlew test` must pass before this feature is
complete.

**Testing expectations**: the constitution's three mandatory automated-coverage areas — schedule
evaluation, target resolution, frozen persisted values — are all absent from this feature, which is
presentation without branching. `SlowLockPaletteTest` continues to enforce the palette count, the
frozen literals, every declared pairing's contrast, and the no-inline-`Color(0x…)` source scan
against the new code for free. No instrumented test is added; none may be.

**Manual verification**: `manual-test-plan.md` ships with fifteen numbered cases traceable to
requirements. The maintainer runs them.

**Version control**: no commit, push, branch or tag is part of this plan. Work ends staged in the
working tree.

**Observation, not a violation**: the constitution's fixed-stack section says `minSdk 33`; the
build declares `minSdk 26`. That divergence predates this feature and is not introduced by it — but
it is the reason contract S7 exists, so it is worth the maintainer's attention. Reconciling the two
(amending the constitution, or raising `minSdk`) is a separate decision and is **not** taken here.

## Project Structure

### Documentation (this feature)

```text
specs/007-locks-screen-polish/
├── plan.md                        # This file
├── spec.md
├── research.md                    # Phase 0 — R1..R8
├── data-model.md                  # Phase 1 — the design model (no entities change)
├── quickstart.md                  # Phase 1
├── manual-test-plan.md            # Phase 1 — M1..M15
├── contracts/
│   ├── system-bars.md             # Contract S — S1..S7
│   └── locks-screen-visual.md     # Contract L — L1..L12
├── checklists/
│   └── requirements.md
└── tasks.md                       # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

```text
app/src/main/
├── java/com/slowlock/
│   ├── MainActivity.kt                    # CHANGED — explicit SystemBarStyle for both bars
│   ├── locks/
│   │   └── LocksScreen.kt                 # CHANGED — heading block, redesigned available row
│   └── ui/
│       ├── theme/
│       │   ├── Type.kt                    # CHANGED — + TitleDisplay, Count, RowTitle, Badge
│       │   ├── Shape.kt                   # CHANGED — + Badge (9dp), beside Pill
│       │   └── Color.kt                   # UNCHANGED — no colour is added
│       └── components/
│           └── ScreenHeader.kt            # UNCHANGED — see contract L1
└── res/values/
    └── strings.xml                        # CHANGED — locks_title; + locks_delay_badge, locks_count_caption; − locks_row_detail

app/src/test/java/com/slowlock/
└── ui/theme/SlowLockPaletteTest.kt        # UNCHANGED — already covers this feature's obligations
```

**Structure Decision**: unchanged. Single `:app` module, package-by-feature under
`com.slowlock`, with shared design tokens in `ui/theme` and shared components in `ui/components`.
This feature adds no directory and no file — it edits five and leaves everything else alone, which
is what contract L12 makes checkable in a diff.

## Phase 2 preview

`/speckit-tasks` will decompose this into three independently shippable slices, matching the spec's
user stories:

1. **US1 — the bars.** `MainActivity` only. Shippable and verifiable on its own (M1–M4).
2. **US2 — the heading.** `Type.kt` (`TitleDisplay`, `Count`), `strings.xml` (`locks_title`, `locks_count_caption`),
   `LocksScreen.kt` heading block (M5).
3. **US3 — the rows.** `Type.kt` (`RowTitle`, `Badge`), `Shape.kt`, `strings.xml`
   (`locks_delay_badge`, removing `locks_row_detail`), `LocksScreen.kt` row (M6–M11).

US2 and US3 both touch `Type.kt` and `LocksScreen.kt`, so they are ordered rather than parallel;
US1 is independent of both.

## Complexity Tracking

> No Constitution Check violations. This section is intentionally empty.
