# Contract: Design Tokens — FROZEN

**Feature**: `004-visual-redesign` | **Status**: frozen on merge

This file is the authority on SlowLock's visual constants. Feature 005 (First run, Locks) and
Phase 3 (dark palette) build on it and MUST NOT redefine anything here.

**Frozen** means: the values below may be *added to* only where this file says a set is open, and
may be *changed* only by amending this file in the same commit. A colour literal appearing anywhere
in screen code is a defect, not a shortcut.

---

## C1 — The palette is closed at eleven

```
Paper      #EFEDEA      Bone       #F3F0EA      Card       #FBF9F5
Ink        #17150F      Ink60      #4A463C      Ink40      #6F6A5E
Amber      #C9821F      AmberDark  #8A5610      AmberWash  #F2E4CE
Line       #E3DED3      Fill       #E7E2D7
```

- **Exactly eleven.** `SlowLockPaletteTest` asserts the count and each literal. A twelfth token
  fails the build (FR-002, SC-009).
- No screen, component or resource file may declare a colour value of its own.
- **One substitution against the canvas, recorded deliberately**: the artboard's slider inactive
  track `#E4DFD4` is rendered with `Fill` `#E7E2D7` — three points per channel apart, visually
  identical, and not worth a twelfth token when `SlowLockPaletteTest` enforces the count.
- These are the *source*; the Material 3 `ColorScheme` is derived from them, never the reverse.

## C2 — Amber is never a glyph on the ground

`Amber` on `Bone` measures **2.76:1**, below the 4.5:1 floor. It is a fill, a border and a rule.

Where accent-coloured text is wanted, the token is **`AmberDark`** (5.40:1 on `Bone`, 5.84:1 on
`Card`). This is binding (FR-009) and is asserted by the contrast calculation in
`SlowLockPaletteTest`.

## C3 — Every text pairing clears 4.5:1

Measured (WCAG 2.1). The thinnest margin is `Ink40` on `Bone` at **4.74:1**, used only for
captions, eyebrows and unit labels — never for body copy, which takes `Ink60` at 8.27:1.

Adding a pairing means adding it to the test. A pairing that is not in the test is not permitted
on screen.

## C4 — Light-only, with one exception

The app list, delay, icon and unsupported-launcher screens render in the light palette regardless
of the system setting (FR-008). `SlowLockTheme` takes **no `darkTheme` parameter and no
`dynamicColor` parameter** — they are deleted, not defaulted (research R5). Reintroducing either
is a Phase 3 decision, not an implementation detail.

**The wait screen is the exception** and keeps a light/dark pair of all three of its values:

| Resource | Light | Dark |
|---|---|---|
| `wait_background` | `#F3F0EA` | `#14120E` |
| `wait_text` | `#6F6A5E` | `#8A857A` |
| `wait_rule` | `#C9821F` | `#C9821F` |

Deleting `values-night/colors.xml` is a defect, not a simplification (FR-031).

## C5 — Two window backgrounds must agree with the palette

| Theme | `android:windowBackground` | Why |
|---|---|---|
| `Theme.SlowLock` | `Bone` | Without it, launching SlowLock paints a white starting window and flips to bone a frame later (research R5) |
| `Theme.SlowLock.Wait` | `@color/wait_background` | The tap must land on the final colour with nothing to flash (FR-030) |

`Theme.SlowLock.Wait` keeps its DayNight parent. `Theme.SlowLock` keeps its light parent.

## C6 — Type roles are fixed to families

| Role | Family / weight | | Role | Family / weight |
|---|---|---|---|---|
| `display` | Instrument Sans SemiBold | | `action` | Instrument Sans SemiBold |
| `title` | Instrument Sans Medium | | `readout` | JetBrains Mono Medium |
| `body` | Instrument Sans Regular | | `mono` | JetBrains Mono Regular |
| `label` | Instrument Sans Medium | | | |

**Sizes, weights and letter-spacing are fixed in `data-model.md` §2** and are read off the
artboards. They are part of this contract: a screen that uses the right family at the wrong size is
not aligned with the canvas.

- **Every number the user reads is `readout` or `mono`.** No numeral in the proportional face.
- Five weight files ship; **no weight may be synthesised** from another (FR-003).
- Fonts are blocking resource fonts. No role may render in a substitute and swap (research R2).

## C7 — Shipped font files

Vendored into `app/src/main/res/font/`, from the upstream project repositories — **not** from
`google/fonts`, which publishes only variable builds of both families (research R1).

| File | Source | Bytes |
|---|---|---|
| `instrument_sans_regular.ttf` | `Instrument/instrument-sans` `fonts/ttf/` | 86,232 |
| `instrument_sans_medium.ttf` | same | 86,924 |
| `instrument_sans_semibold.ttf` | same | 87,004 |
| `jetbrains_mono_regular.ttf` | `JetBrains/JetBrainsMono` v2.304 `fonts/ttf/` | 273,900 |
| `jetbrains_mono_medium.ttf` | same | 273,860 |

Total 789 KiB uncompressed (**verified: 807,920 bytes**), against SC-007's 1.5MB cap.

Both SIL OFL 1.1 licence files ship in **`app/src/main/assets/licenses/`** — *not* beside the
fonts. `res/font/` accepts only `.xml`, `.ttf`, `.ttc` and `.otf`; a `.txt` there fails the
resource merger. `assets/` still packages them into the APK, which is what OFL 1.1 requires of a
redistribution.

Source note: Instrument Sans is fetched from the repository's default branch, which is
**`master`**, not `main`.

## C8 — Uppercase is stored, not computed

`SECONDS`, `ICON` and the unsupported-launcher eyebrow are stored **capitalised in the string
resource**, with a translator comment saying the capitalisation is stylistic. Letter-spacing comes
from the text style.

No `uppercase()` runs on user-visible text — the Turkish dotted/dotless `i` is why (research R11).
Same rule, same reason, as the lower-case `please wait`.

## C9 — Shapes and control metrics

Shapes map one-to-one onto Material 3's five slots, so nothing lives outside the theme. The slot
names are M3's; the meanings are the design's, and this table is the authority:

| M3 slot | Radius | Used by |
|---|---|---|
| `extraSmall` | 12dp | List-row icons, delay presets, the back tile |
| `small` | 14dp | The search field, the icon-treatment tiles |
| `medium` | 16dp | Primary and secondary actions |
| `large` | 18dp | Cards in a list (feature 005's Locks rows) |
| `extraLarge` | 24dp | The shortcut preview card |

`Pill` is declared separately — the app pill is its only user and M3 has no slot for a
fully-rounded token. (The earlier draft of this contract named a `field` token; it is M3's `small`
slot, and the table above supersedes that name.)

| Control | Metric | |
|---|---|---|
| Primary action | 56dp, `medium` radius, `Amber` fill, `Ink` label | at most one per screen |
| Secondary action | 52dp, `medium` radius, transparent, `Line` border | |
| Search field | 52dp, `field` radius, `Card` fill, `Line` border | |
| List row | 64dp, 44dp icon at `small` radius, 1dp **`Fill`** divider | not `Line` — canvas 1a |
| Back tile | 40dp square, `small` radius, `Card` fill, **`Line` border** | touch target 48dp |
| App pill | `pill` radius, 32dp icon at 10dp | |
| Treatment tile | `field` radius, 12dp padding, 36dp swatch at 11dp | |
| Slider track | 6dp tall, 3dp radius | |
| Preset chip | **44dp** | see C10 |
| Slider thumb | 26dp, `Card` fill, 3dp `Amber` ring | |
| Preview card | `extraLarge` radius, `Card` fill, `Line` border, 96dp icon | |
| Wait rule | 40×2dp `Amber` at 55% opacity | |
| Delay readout | ≤104sp, shrinks to fit | see C11 |

## C10 — Two control groups ship below the accessibility floor

The **delay presets** and the **icon-treatment tiles** ship at the sizes the canvas draws, placing
their touch targets under Android's 48dp minimum. This is a deliberate, recorded trade of
accessibility for design fidelity (FR-045, spec Clarifications 2026-08-24).

**It applies to these two groups only.** Every other interactive element — primary and secondary
actions, the back tile, list rows, the slider, the search field — MUST meet 48dp.

Implementers should know the shortfall arrives by default, not by effort: Material 3 components
enforce the floor automatically, and the custom surfaces this design specifies do not (research
R9). If the trade is re-decided, the fix is one dimension constant.

## C11 — The numeral yields; the controls do not

At every font scale and screen size, the slider, preset row and primary action MUST all be fully
visible and reachable **without scrolling**. The delay readout is the flexible element: it takes
the largest size that fits the space left over, capped at 104sp, and MUST remain the largest
element on the screen (FR-014a, research R10).

The delay screen MUST NOT become vertically scrollable to satisfy this.

## C12 — What this contract does NOT touch

Explicitly out of this contract's reach, and frozen elsewhere:

- Any persisted key, file name, or token — `contracts/delay-config-store.md` (feature 003).
- The pinned shortcut's intent, its target class, or its ID — `contracts/pinned-shortcut.md`
  (feature 002). **`ShortcutLaunchActivity`'s fully-qualified name is written into every icon
  already on a user's home screen.**
- Any resource *name*, class name or package name. The rename to "lock" reaches string **values**
  only (FR-042, research R12).
