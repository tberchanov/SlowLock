# Phase 1 Data Model: Visual Redesign (Phase 1)

**Feature**: `004-visual-redesign` | **Date**: 2026-08-24 | **Plan**: [plan.md](./plan.md)

This feature persists nothing and changes no persisted shape. Its "data" is a closed set of design
tokens plus one derived value. Both are compile-time constants; neither has a lifecycle, an owner,
or a store.

**What is deliberately absent**: no entity is added for a *lock* as a stored object. FR-041 makes
"lock" the user-facing noun, but the thing behind it is still exactly what feature 003 persists —
a delay and a treatment keyed by package name — and this feature does not touch it. The Lock
entity as a first-class, enumerable object is Phase 2's work (Out of Scope).

---

## 1. Colour token

The complete palette. **Closed set**: FR-002 fixes it at eleven, SC-009 forbids a twelfth, and
`SlowLockPaletteTest` asserts both the count and each literal.

| Token | Hex | M3 role it fills | Used for |
|---|---|---|---|
| `Paper` | `#EFEDEA` | — | The canvas behind the phone in the design; the app's outermost ground |
| `Bone` | `#F3F0EA` | `background` | Every screen's ground; the wait screen's light background |
| `Card` | `#FBF9F5` | `surface` | Raised surfaces — search box, preview card, unselected tiles, slider thumb fill |
| `Ink` | `#17150F` | `onBackground`, `onSurface`, `onPrimary` | Headings, body, the primary action's label |
| `Ink60` | `#4A463C` | `onSurfaceVariant` | Secondary body copy |
| `Ink40` | `#6F6A5E` | `outline` | Captions, eyebrows, unit labels, the wait message |
| `Amber` | `#C9821F` | `primary` | Primary action fill, active slider track, thumb ring, selected tile border, the wait rule |
| `AmberDark` | `#8A5610` | `secondary` | Accent-coloured *text* — the only accent token permitted as a glyph |
| `AmberWash` | `#F2E4CE` | `primaryContainer` | Selected tile fill, the delay badge |
| `Line` | `#E3DED3` | `outlineVariant` | Hairline borders and dividers |
| `Fill` | `#E7E2D7` | `surfaceVariant` | Icon placeholders, inactive slider track, **app-list row divider** |

**One deliberate substitution.** The canvas draws the slider's inactive track at `#E4DFD4`, three
points per channel away from `Fill` `#E7E2D7` and visually identical. It is rendered with `Fill`
rather than admitted as a twelfth token — a distinction nobody can see is not worth a token, and
`SlowLockPaletteTest` enforces the count. This is the only colour in the canvas's app screens that
is not a token; recorded here so a pixel-comparison against the artboard does not read it as a
defect.

**Not tokens, and not colours at all.** The treatment swatches in the canvas (`#181D28` inverted,
`#BFBCB6` gray) stand in for the *target app's own icon* with a colour matrix applied. The real
screen draws the real icon. `#E2C79C`, `#9E6413`, `#171717` and `#2e2e2e` appear only in canvas
chrome — the phone bezel, the status-bar dot, a link hover, and section `2a`'s icon sketches.

**Validation rules**

- The set has exactly eleven members. A twelfth fails `SlowLockPaletteTest`.
- No screen may declare a colour literal. Colours are referenced by token only.
- `Amber` MUST NOT be used as text on `Bone`, `Card` or `Paper` — measured 2.76:1, below the floor
  (research R14). `AmberDark` is the accent text token at 5.40:1 on `Bone`.
- Every text-on-surface pairing actually used MUST measure ≥4.5:1, asserted by calculation in
  `SlowLockPaletteTest` rather than by review (SC-008).

**Dark counterparts** — three values only, and only for the wait screen (FR-031, research R7):

| Resource | Light | Dark |
|---|---|---|
| `wait_background` | `#F3F0EA` (`Bone`) | `#14120E` |
| `wait_text` | `#6F6A5E` (`Ink40`) | `#8A857A` |
| `wait_rule` | `#C9821F` (`Amber`) | `#C9821F` (unchanged) |

The four in-app screens have no dark counterpart in this phase by design (FR-008, Out of Scope).

---

## 2. Type role

Two families, five shipped weights, and a fixed assignment of role to family. The **role**, not the
size, decides the family (FR-004).

| Role | Family | Weight | Used for |
|---|---|---|---|
| `display` | Instrument Sans | SemiBold | The first-run headline (Phase 2); unused in Phase 1 |
| `title` | Instrument Sans | Medium | Screen titles — "Choose an app", "Wait before opening", "New lock" |
| `body` | Instrument Sans | Regular | Sentences, app labels, empty and error states |
| `label` | Instrument Sans | Medium | List rows, the app pill, tile names |
| `action` | Instrument Sans | SemiBold | Primary and secondary action labels |
| `readout` | JetBrains Mono | Medium | The delay numeral — the largest element on the delay screen |
| `mono` | JetBrains Mono | Regular | Unit captions, eyebrows, footnotes, the wait message, the delay badge |

**Sizes and letter-spacing, read off the artboards.** Without these, "restyle to the canvas" is not
verifiable. Sizes are `sp`; they scale with the system font setting (except as bounded by FR-014a).

| Where | Role | Size | Weight | Letter-spacing | Colour |
|---|---|---|---|---|---|
| Screen title | `title` | 22 | Medium | −0.01em | `Ink` |
| List row label | `label` | 17 | Regular | — | `Ink` |
| Action label | `action` | 16 | SemiBold | — | `Ink` |
| Secondary action label | `action` | 15 | Medium | — | `Ink` |
| Search placeholder | `body` | 16 | Regular | — | `Ink40` |
| Preview card label | `body` | 16 | Regular | — | `Ink` |
| App pill label | `label` | 15 | Medium | — | `Ink` |
| Treatment tile name | `label` | 13 | Medium selected / Regular not | — | `AmberDark` / `Ink60` |
| Unsupported message | `body` | 22 | Regular | −0.01em | `Ink` |
| **Delay readout** | `readout` | **104** | Medium | −0.03em | `Ink` |
| Wait message | `mono` | 19 | Regular | 0.02em | `wait_text` |
| Preset label | `mono` | 14 | Medium selected / Regular not | — | `AmberDark` / `Ink` |
| `SECONDS` caption | `mono` | 13 | Regular | 0.2em | `Ink40` |
| Footnote | `mono` | 12 | Regular | — | `Ink40` |
| Unsupported eyebrow | `mono` | 12 | Regular | 0.14em | **`AmberDark`** |
| Preview card delay | `mono` | 12 | Regular | — | **`AmberDark`** |
| `ICON` eyebrow | `mono` | 11 | Regular | 0.14em | `Ink40` |
| Slider end labels | `mono` | 11 | Regular | — | `Ink40` |

**Validation rules**

- Every numeric value the user reads is `readout` or `mono`. No number is set in the proportional
  face (FR-004).
- Every weight used is a shipped file; no weight is synthesised (FR-003, research R1).
- Fonts load blocking, so no role may render in a substitute face and then swap (FR-003, R2).
- Uppercase roles (`SECONDS`, `ICON`, the unsupported-launcher eyebrow) are stored capitalised in
  the string resource; letter-spacing comes from the text style, never from inserted spaces
  (research R11).
- **Fallback**: for app labels — which come from other apps and may be in any script — the
  platform's own font fallback applies. If the manual case shows missing-glyph boxes, app labels
  fall back to the platform default family; every other role stays bundled (FR-005, research R3).

---

## 3. Shape and metric tokens

| Token | Value | Applied to |
|---|---|---|
| `small` | 12dp | List-row icons, preset chips, the back tile |
| `field` | 14dp | The search field and the treatment tiles |
| `medium` | 16dp | Primary and secondary actions |
| `large` | 18dp | Cards in a list |
| `extraLarge` | 24dp | The shortcut preview card |
| `pill` | 999dp | The app pill on the delay screen |

| Metric | Value | Notes |
|---|---|---|
| Primary action height | 56dp | FR-006, at most one per screen |
| Secondary action height | 52dp | FR-007 |
| Search field height | 52dp | FR-011 |
| List row height | 64dp | FR-012 |
| List row icon | 44dp | FR-012 |
| Back tile | 40dp square, `Card` fill, **`Line` border** | FR-007; touch target expanded to 48dp |
| App pill icon | 32dp at 10dp radius | canvas 1a |
| Treatment swatch | 36dp at 11dp radius | canvas 1a |
| Preview icon radius | 26dp | canvas 1a |
| Row divider | 1dp **`Fill`** — not `Line` | canvas 1a |
| Slider track | 6dp tall, 3dp radius | canvas 1a |
| Preset chip height | **44dp** | FR-045 — **below the 48dp floor, accepted deliberately** |
| Treatment tile padding | 12dp | FR-045 — same acceptance |
| Slider thumb | 26dp, 3dp ring | FR-016 |
| Preview icon | 96dp | FR-022 |
| Delay readout | ≤104sp, shrinks to fit | FR-014, FR-014a |
| Wait rule | 40×2dp, 55% opacity | FR-027 |

---

## 4. Delay preset *(the only new value in the feature)*

A named shortcut to a commonly chosen delay.

**Fields**

| Field | Type | Value |
|---|---|---|
| `seconds` | Int | One of 5, 10, 30 |

**Relationships**: a preset is a convenience over the existing `DelayRange`, never an alternative
to it. Every preset is a value the slider can also reach.

**Validation rules** — each is a case in `DelayRangeTest` (FR-017–FR-019, research R13):

- `PRESETS == listOf(5, 10, 30)`, asserted against literals.
- Every preset satisfies `MIN_SECONDS <= p <= MAX_SECONDS`.
- Every preset is `snap`-stable: `snap(p) == p`. A preset the slider would move away from is a bug.
- `presetFor(seconds)` returns the matching preset or nothing. It is a pure lookup.

**State**: none. A preset holds nothing. Whether one renders as selected is **derived** from the
current delay at composition time — `presetFor(seconds) != null`. There is no "selected preset"
variable anywhere, which is what makes the "user dragged to 17 seconds, nothing highlighted" case
(US2 scenario 3) correct by construction rather than by a cleared flag.

**What presets must not do**: change `MIN_SECONDS`, `MAX_SECONDS`, `STEP_SECONDS`, `STOPS` or
`SLIDER_STEPS`; introduce a second clamp; or become the only way to reach 5, 10 or 30.

---

## 5. Glossary

| Term | Meaning |
|---|---|
| **Lock** | The user-facing name for what a user creates: a target app, its delay, its icon treatment, and the home-screen icon that fires it. Canonical in all user-visible copy (FR-041). |
| **Shortcut** | The Android mechanism a lock is built on. Survives only where the user does not read it — resource names, class names, contract filenames, and the launcher's own dialog (FR-042). |
| **Treatment** | How a lock's icon is altered: Original, Inverted, Gray. Unchanged from feature 002. |
| **The wait** | The interval between tapping a lock's icon and the target app opening. |
| **Token** | A named fixed visual value. The colour set is closed at eleven; type, shape and metric tokens are open only to values this document lists. |
