# Phase 1 Data Model: Legible system bar and a redesigned Locks screen

**Feature**: `specs/007-locks-screen-polish` | **Date**: 2026-08-25

**No entity is added, removed, or changed.** Nothing is persisted, derived, or migrated by this
feature. `Lock`, `LocksUiState`, `LockStore` and every record on disk are exactly what feature 005
left. What follows is the *design* model — the tokens and the layout values this feature adds — in
the same place a data model would sit, because these are the values the implementation must match
and the values a reviewer checks against the artboard.

---

## §1 Colour — unchanged

Eleven tokens, no additions (FR-022). The redesigned screen uses five pairings, and all five are
already declared in `TextPairings` and already pass the contrast floor:

| Where | Text | Surface | Declared as |
|---|---|---|---|
| Locks title | `Ink` | `Bone` | `Ink on Bone` |
| Count caption | `Ink40` | `Bone` | `Ink40 on Bone` |
| Row app name | `Ink` | `Card` | `Ink on Card` |
| Row treatment line | `Ink40` | `Card` | `Ink40 on Card` |
| Delay badge label | `AmberDark` | `AmberWash` | `AmberDark on AmberWash` |

Non-text uses, all existing tokens: row fill `Card`, row border `Line`, icon placeholder `Fill`,
badge fill `AmberWash`.

`SlowLockPaletteTest` is the enforcement, unchanged: eleven tokens, frozen literals, every declared
pairing over 4.5:1, and no `Color(0x…)` literal anywhere outside `Color.kt`.

---

## §2 Type — four roles added

Added to `SlowLockType`. No existing role changes value (FR-024, FR-025).

| Role | Family | Weight | Size | Tracking | Drawn by |
|---|---|---|---|---|---|
| `TitleDisplay` | Instrument Sans | Medium | 30sp | −0.015em | the Locks title |
| `Count` | JetBrains Mono | Regular | 12sp | +0.06em | the caption beneath it |
| `RowTitle` | Instrument Sans | Medium | 17sp | — | a lock's app name |
| `Badge` | JetBrains Mono | Medium | 15sp | — | the delay badge |

Every weight named here has a shipped font file behind it (contract C7): Instrument Sans Medium and
JetBrains Mono Medium are both already in `res/font`. Nothing is synthesised.

`RowTitle` and `RowLabel` coexist deliberately — see research R3. `Count` and `Eyebrow` likewise.

---

## §3 Shape — one radius added

| Token | Radius | Drawn by |
|---|---|---|
| `Badge` | 9dp | the delay badge |

Declared beside the existing `Pill`, outside the five Material slots, which are untouched.

---

## §4 The heading block

Read off the `New · Locks` artboard.

| Element | Value |
|---|---|
| Screen horizontal padding | 20dp (unchanged) |
| Title | `TitleDisplay`, `Ink` |
| Title → caption gap | 4dp |
| Caption | `Count`, `Ink40`, capitals from the resource (contract L4) |
| Caption → list gap | 20dp |
| Controls in the heading | none — no back tile, no step counter |

The block replaces this screen's use of `ScreenHeader`. `ScreenHeader` itself is **not modified**:
it stays exactly as it is for the three flow screens that use it (FR-025).

---

## §5 The lock row (available)

| Element | Value |
|---|---|
| Shape | `shapes.large` (18dp) — unchanged |
| Fill / border | `Card` / 1dp `Line` — unchanged |
| Padding | 14dp all round |
| Minimum height | 64dp, growing with content |
| Icon box | 48dp; placeholder clipped with `shapes.small` (14dp), fill `Fill` |
| Icon → body gap | 14dp |
| Body line 1 | app name, `RowTitle`, `Ink`, one line, ellipsised |
| Body line gap | 3dp |
| Body line 2 | icon treatment **only**, `Footnote`, `Ink40` |
| Body → badge gap | 8dp, badge never compressed |
| Badge | `Badge` shape, `AmberWash` fill, padding 9dp × 5dp |
| Badge label | compact delay (`10s`), `Badge` type, `AmberDark` |

The delay leaving line 2 for the badge is the one informational change; the row still carries app
name, delay and treatment, and still claims nothing about the home screen.

---

## §6 The lock row (app uninstalled) — unchanged

No badge, no two-line body, no tap target, the existing message and the existing visible
"How to remove" control. Feature 005 designed this row for a case the artboards do not cover, and
this feature leaves it alone (FR-020).

---

## §7 Strings

| Resource | Change |
|---|---|
| `locks_title` | "Your locks" → "Locks" |
| `locks_delay_badge` | **new**, `%1$ds` |
| `locks_count` | unchanged — now the *spoken* form only |
| `locks_count_caption` | **new**, capitalised plural, the *drawn* form (contract C8, L4) |
| `locks_row_detail` | **removed** — the joiner has no caller once line 2 is the treatment alone |
| everything else on this screen | unchanged |

---

## §8 Accessibility

| Node | Shown | Spoken |
|---|---|---|
| Caption | `3 LOCKS` (`locks_count_caption`) | `3 locks` (`locks_count`) |
| Badge | `10s` | `10 second wait` (the existing `delay_wait` plural) |
| Row | — | one stop: name, treatment, delay in words |
| Row actions | — | tap to edit, long press and a custom action for the removal explanation, all unchanged |
