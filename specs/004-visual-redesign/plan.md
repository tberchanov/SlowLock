# Implementation Plan: Visual Redesign (Phase 1)

**Branch**: `main` (no feature branch; this installation registers no branch hook) | **Date**: 2026-08-24 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-visual-redesign/spec.md`

## Summary

SlowLock currently borrows its colours from the user's wallpaper. This feature replaces Material
3's dynamic colour with a fixed warm palette, bundles two typefaces, and re-weights the five
existing screens around the delay — the one number the product exists to sell. It adds exactly one
behaviour: three one-tap delay presets.

Four decisions carry it. **The design system is an M3 theme override plus four local composables**
(research R4) — not a bespoke system and not a theme alone, because M3's 40dp pill button and
`FilterChip` cannot become the canvas's 56dp action and swatch tiles, while `Slider`, `Scaffold`
and `LazyColumn` should keep the behaviour they already have. **Fonts are blocking resource
fonts** (R2), which is what lets FR-003 forbid substitution after first paint and lets the wait
screen arrive complete in a single frame (R6). **`SlowLockTheme` loses its `dynamicColor` and
`darkTheme` parameters outright** rather than defaulting them (R5) — a parameter left in place is
one a later edit passes `true` to. **The rename to "lock" is confined to string values** (R12):
`ShortcutLaunchActivity`'s fully-qualified name is written into every pinned shortcut already on a
home screen, so the word the user reads and the identifier the system stores must be allowed to
differ.

Two findings changed the shape of the work after the spec was clarified, and both are corrections
recorded in research rather than quietly absorbed. **Google Fonts ships neither family as static
weights** — only variable fonts — so the five files come from the two upstream project
repositories (R1). And those five measure **790 KiB**, not the ~1.2MB estimate that justified
raising SC-007's budget to 1.5MB; the budget is now headroom rather than necessity. Separately,
**building the preset and treatment tiles as custom surfaces is what produces the sub-48dp touch
targets FR-045 accepts** (R9): M3 components would have met the accessibility floor for free. The
maintainer chose the drawn size knowingly; the plan implements that and states the cost.

Technical shape: no new dependency, no new module, no persisted value touched, no contract
unfrozen. One new `ui/components` package of four composables, one new `ui/theme` token set, five
screens restyled, five font files and two licences vendored, and two JVM test files — one of
which turns SC-008 and SC-009 from review items into build failures.

## Technical Context

**Language/Version**: Kotlin 2.2.10, Java/JVM target 11

**Primary Dependencies**: **No new dependencies.** Existing set only — Jetpack Compose (BOM
2026.02.01), Material 3, `core-ktx`, `activity-compose`, `lifecycle-*`. `gradle/libs.versions.toml`
is untouched. Platform and toolkit APIs newly used by this feature: resource fonts
(`androidx.compose.ui.text.font.Font`), Material 3 `Slider`'s `track`/`thumb` slots,
`Modifier.selectable` / `Modifier.selectableGroup`, auto-sizing text, and
`android:windowBackground` on the main activity's theme.

**New binary assets**: five static TTFs (789 KiB total) plus two `OFL.txt` licence files, vendored
into `res/font/`. Sourced from `Instrument/instrument-sans` and `JetBrains/JetBrainsMono` — **not**
from `google/fonts`, which ships only variable builds of both (research R1).

**Storage**: **Unchanged.** No persisted value, key, file name or token is read differently or
written differently by this feature. `DelayConfigStore` and `ShortcutContract` are untouched, and
their existing tests are the mechanical guard (FR-038, FR-042).

**Testing**: **JVM unit tests only, plus a manual pass run by the maintainer.** Constitution v1.1.0
forbids instrumented suites and forbids an agent driving the connected device. Automated coverage
is the pure core: the preset arithmetic, and a palette test that asserts the eleven frozen colour
literals and computes WCAG contrast for every declared pairing (research R13). Everything that can
only be seen running — first-paint timing, the dark wait screen, glyph fallback for non-Latin app
labels, layout at large font scales, screen-reader announcements — is a numbered case in
`manual-test-plan.md`. Gates: `./gradlew assembleDebug` and `./gradlew test`.

**Target Platform**: Android, `minSdk 33`, `targetSdk`/`compileSdk 37`

**Project Type**: Mobile app — single `:app` Gradle module, `com.slowlock`

**Performance Goals**: wait-screen ground on the tap's own frame and the complete screen within
500ms with nothing changing afterwards (FR-029, FR-030, SC-005); no font substitution after first
paint (FR-003); list → configured icon still under 45 seconds and ≤5 taps (SC-004)

**Constraints**: zero new permissions and zero prompts (FR-039); no network (FR-039); no new
dependency or module (FR-039); installed size growth ≤1.5MB, expected ≈790KB (SC-007); no colour
outside the eleven in FR-002 (SC-009); every text pairing ≥4.5:1 (SC-008); `contracts/pinned-shortcut.md`
and `contracts/delay-config-store.md` stay byte-for-byte true (FR-038)

**Scale/Scope**: Five screens restyled, one new component package, one theme rewritten, one
behaviour added. Roughly nine new source files, nine changed, five binary assets, two new test
files, one manual test plan.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Constitution v1.1.0.** Evaluated pre-research and re-evaluated post-design; both shown.

| Principle | Binding rule as it applies here | Pre-Phase 0 | Post-Phase 1 |
|---|---|---|---|
| **I. Cooperative User, Not Adversary** | Nothing about escapability changes. The wait screen is restyled, not re-mechanised: back and home still leave it, abandonment is still a first-class outcome, and every bypass path in `highlevel_spec.md` §5 is still open. The redesign makes the delay *more* legible before it is chosen — presets and a large readout — which serves a willing participant rather than policing one. No new friction is added anywhere. | ✅ PASS | ✅ PASS |
| **II. Simplicity First (YAGNI)** | **Zero new dependencies**, zero new modules, no DI, no ViewModel added, no abstraction layer. The four composables in `ui/components` are shared UI, not an architectural seam — each is one function with a parameter list, and each exists because it is used on two or more screens. Fonts are vendored resource files, not a dependency coordinate. Two screens the canvas draws are **deliberately not built** (Out of Scope), which is YAGNI applied to a design rather than to code. | ✅ PASS | ✅ PASS |
| **III. Permission & Policy Minimalism** | **Zero** new `<uses-permission>` elements, zero prompts, zero new system dialogs. No network access is introduced — the fonts are in the APK, which is exactly why downloadable fonts were rejected (R2). Nothing about the pin dialog, its gating, or its declined path changes. | ✅ PASS | ✅ PASS |
| **IV. Platform-Idiomatic Android** | Kotlin + Compose + **Material 3 retained** — this feature brands M3 through its documented theming surface (`ColorScheme`, `Typography`, `Shapes`) and its documented slot APIs (`Slider`'s `track`/`thumb`), rather than replacing M3 or working around it (R4, R8). No XML layouts for screens; the XML touched is resource files (`colors.xml`, `themes.xml`, `strings.xml`), which is where those values belong. No new I/O of any kind, so nothing new can reach the main thread. Selection state is expressed with `Modifier.selectable`/`selectableGroup`, the platform's own idiom, rather than by colour alone (R9). | ✅ PASS | ✅ PASS |
| **V. Stable Identifiers** | `packageName` remains the only persisted identifier and is not touched. **The rename is the hazard, and it is closed by construction**: FR-042 confines "lock" to string *values*, and R12 enumerates them. No resource name, class name, package name, persisted key or frozen token changes. `ShortcutContractTest` and `DelayConfigTest` are unchanged and must still pass — a rename that reached an identifier would fail the build rather than a user's home screen. | ✅ PASS | ✅ PASS |

**Technology Standards check**: fixed stack honoured (Kotlin/Compose/M3, single module, Java 11,
minSdk 33, targetSdk 37, `com.slowlock`). No dependency added, so `gradle/libs.versions.toml` is
untouched and `app/build.gradle.kts` is unchanged. No backend, no network, no analytics, no
third-party SDK. Font files are OFL 1.1 and their licences are vendored beside them.

**Scope boundary check**: the constitution's v1 boundary covers "per-app schedule and delay
configuration" and "a delay screen that launches the target". The presets sit inside the first.
The wait screen's restyle sits inside the sentence v1.1.0 added — *how that screen presents is a
product decision for the feature that builds it* — which is the clause that authorises FR-037's
amendment of feature 003 without a further constitutional change. Nothing here reaches a non-goal:
no enforcement, no usage statistics, no sync, no parental controls.

### Recorded rulings

**On amending feature 003 (FR-037).** 003 justified the wait screen's appearance as *unbranded by
construction* — "anything with character here is a defect". This feature gives it the app's ground
colour, an accent rule and the app's monospaced face. The ruling: **003's binding property was
always that the screen is static and not worth reading twice, and the unbranded wording was one
means to that end, not the end itself.** Constitution v1.1.0 places presentation with the feature
that builds it, so this needs no amendment above the spec level. Every other 003 obligation is
carried forward verbatim — escapability, the `onStop` rule, the timing arithmetic, no background
launch, and the no-flash starting window (FR-029 through FR-032, FR-040).

**On sub-minimum touch targets (FR-045).** The delay presets and treatment tiles ship at 44dp,
below Android's 48dp accessibility floor. This is **not a constitutional matter** — the
constitution says nothing about accessibility — so it does not belong in Complexity Tracking. It
is recorded here because it is a real cost, knowingly taken: the concern was raised during
clarification and the maintainer chose design fidelity. R9 notes the sharp edge, which is that
building these controls from M3 components would have met the floor for free; the custom surfaces
this plan specifies are what produce the shortfall. The remedy, if it is re-decided, is one
dimension constant.

**On SC-007's budget.** The clarification that raised the cap from 800KB to 1.5MB rested on an
estimate of ~1.2MB for five static font files. Measured, they are **790 KiB** (R1) — they would
have fitted the original cap. The decision to ship statics stands and the cap is left at 1.5MB;
the plan simply records that the headroom is now generous rather than needed, so that a future
reader does not infer the payload is larger than it is.

**Gate result**: **PASS with zero deviations.** Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/004-visual-redesign/
├── plan.md                        # This file
├── research.md                    # Phase 0 — 14 decisions, 3 of them corrections
├── data-model.md                  # Phase 1 — the token set and the preset
├── quickstart.md                  # Phase 1
├── manual-test-plan.md            # Phase 1 — primary verification artifact
├── contracts/
│   ├── design-tokens.md           # FROZEN palette, type roles, shapes, control metrics
│   ├── ui-components.md           # The four shared composables and their seams
│   └── screen-inventory.md        # Per-screen obligations, and what must NOT change
├── checklists/
│   └── requirements.md            # From /speckit-specify, re-validated by /speckit-clarify
└── tasks.md                       # /speckit-tasks output — not created here
```

### Source Code (repository root)

```text
app/src/main/java/com/slowlock/
├── MainActivity.kt                     # unchanged
├── SlowLockRoot.kt                     # unchanged — no new stage (Out of Scope)
├── ui/theme/
│   ├── Color.kt                        # REWRITTEN — the eleven tokens, purple scaffolding deleted
│   ├── Theme.kt                        # CHANGED — dynamicColor and darkTheme parameters deleted (R5)
│   ├── Type.kt                         # REWRITTEN — M3 scale in Instrument Sans + mono roles
│   └── Shape.kt                        # NEW — 12/16/18/24dp
├── ui/components/                      # NEW package (R4)
│   ├── ScreenHeader.kt                 # back tile + title
│   ├── Actions.kt                      # PrimaryAction, SecondaryAction
│   └── SelectableTile.kt               # preset chips and treatment tiles (R9)
├── apps/AppListScreen.kt               # CHANGED — header, search box, 64dp rows
├── delay/
│   ├── DelayRange.kt                   # CHANGED — PRESETS and presetFor (FR-017–FR-019)
│   ├── DelayConfigScreen.kt            # CHANGED — readout, pill, slider slots, preset row
│   └── WaitScreen.kt                   # CHANGED — ground, accent rule, lower-case message
└── shortcut/
    ├── ShortcutConfigScreen.kt         # CHANGED — preview card, treatment tiles
    └── PinUnsupportedScreen.kt         # CHANGED — eyebrow, left alignment, action pair

app/src/main/res/
├── font/                               # NEW — 5 TTFs + 2 OFL.txt (R1)
├── values/colors.xml                   # CHANGED — wait colours move to the palette
├── values-night/colors.xml             # CHANGED — the dark wait triple (R7)
├── values/themes.xml                   # CHANGED — windowBackground on Theme.SlowLock (R5)
└── values/strings.xml                  # CHANGED — the rename inventory (R12) + new labels

app/src/test/java/com/slowlock/
├── ui/theme/SlowLockPaletteTest.kt     # NEW — eleven literals + computed contrast (R13)
├── delay/DelayRangeTest.kt             # CHANGED — preset cases
└── (all other tests unchanged and must still pass — the guard on FR-038/FR-042)

# No app/src/androidTest. Constitution v1.1.0 forbids instrumented suites.
```

**Structure Decision**: single `:app` module, unchanged. `ui/theme` grows a `Shape.kt` and has its
other three files rewritten; `ui/components` is new and holds only composables used by two or more
screens. No screen moves package, and `SlowLockRoot.kt` is deliberately untouched — adding a stage
is Phase 2's work, and a diff that does not touch the navigation file is the cheapest proof that
this feature stayed inside its scope.

## Phase ordering

Nine steps, each leaving the app building and running, each a candidate commit:

| # | Step | Gate |
|---|---|---|
| 1 | Vendor fonts and licences; `colors.xml` / `values-night` / `themes.xml` token resources | `assembleDebug` |
| 2 | `Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt` — parameters deleted; `SlowLockPaletteTest` | `test` |
| 3 | `ui/components` — the four composables | `assembleDebug` |
| 4 | App list restyle | manual M2 |
| 5 | `DelayRange` presets + `DelayRangeTest`, then the delay screen | `test`, manual M3 |
| 6 | Icon step: preview card, treatment tiles | manual M4 |
| 7 | Pin-unsupported screen | manual M6 |
| 8 | Wait screen + its dark variant | manual M5 |
| 9 | String rename pass (R12), full manual plan | `test`, `assembleDebug`, manual all |

Step 2 is the only one that can break every screen at once, which is why the palette test lands
with it. Step 5 is the only step carrying new behaviour. Step 8 is last because it is the screen
with the tightest constraints and the least room to absorb a surprise.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**None.** The Constitution Check passes on all five principles with no deviation, pre-research and
post-design. This feature adds no dependency, no module, no persistence, no permission and no
abstraction layer; it removes a capability (dynamic colour) and adds assets.

The two costs worth knowing about are **not** constitutional violations and are recorded under
Recorded rulings instead: the 44dp touch targets FR-045 accepts, and the 790KB of vendored font
binaries.
