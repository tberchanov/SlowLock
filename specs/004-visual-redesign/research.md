# Phase 0 Research: Visual Redesign (Phase 1)

**Feature**: `004-visual-redesign` | **Date**: 2026-08-24 | **Plan**: [plan.md](./plan.md)

Fourteen decisions. Each names what was chosen, why, and what was rejected. Three of them
(R1, R3, R9) record a fact that contradicts something assumed earlier; those are marked
**correction**.

---

## R1 — Where the fonts come from, and what they actually cost

**Correction.** The spec's Q5 clarification was decided against an estimate of ~1.2MB for five
static weight files, which is why SC-007's cap moved from 800KB to 1.5MB. That estimate was
wrong — it assumed Instrument Sans weighed roughly what JetBrains Mono does. Measured:

| File | Source | Bytes |
|---|---|---|
| `InstrumentSans-Regular.ttf` | `Instrument/instrument-sans` → `fonts/ttf/` | 86,232 |
| `InstrumentSans-Medium.ttf` | same | 86,924 |
| `InstrumentSans-SemiBold.ttf` | same | 87,004 |
| `JetBrainsMono-Regular.ttf` | `JetBrains/JetBrainsMono` release v2.304 → `fonts/ttf/` | 273,900 |
| `JetBrainsMono-Medium.ttf` | same | 273,860 |
| **Total, uncompressed** | | **807,920 (789 KiB)** |

Font resources are deflated in the APK, so installed growth is below this. **The five statics
would have fitted the original 800KB budget.** The decision to ship statics is unchanged and
correct; the budget raised to accommodate them is now headroom rather than necessity.

**Decision**: vendor the five files above into `app/src/main/res/font/`, renamed to the
lowercase-underscore form Android resource names require. Vendor `OFL.txt` from each project
beside them.

**Rationale**: both projects publish static weight builds under the SIL Open Font License 1.1.
Checked-in binaries need no build step, no `fonttools`, and no network at build time.

**Alternatives rejected**:

- **Google Fonts (`google/fonts` repo)** — *cannot satisfy the decision*. It ships **only**
  variable fonts for both families: `InstrumentSans[wdth,wght].ttf` (194,336 B) and
  `JetBrainsMono[wght].ttf` (187,208 B). No static weights exist there. This is why the upstream
  project repositories are the source.
- **The two variable fonts** (381KB total, every weight) — rejected by the maintainer in the Q5
  clarification in favour of named-weight files. Worth knowing it remains a ~50% payload saving if
  that trade is ever re-opened.
- **Downloading at build time** — a network dependency in the build, for files that change never.

**Verification**: `assembleDebug` APK size compared against the previous release; expected growth
≈ 790KB uncompressed, well inside SC-007's 1.5MB.

---

## R2 — Fonts must be on screen at first paint, not swapped in

**Decision**: declare the faces as ordinary resource fonts (`Font(R.font.…)`) and rely on
Compose's default loading strategy for them, which is **blocking**.

**Rationale**: FR-003 forbids substitution after first paint, and FR-029 forbids the wait screen
changing at all once it is up. A blocking resource font is resolved before the text is measured,
so there is no frame in which a fallback face is drawn and then replaced. Resource fonts are read
from the APK, so "blocking" costs a local read, not a network round trip.

**Alternatives rejected**:

- **Downloadable fonts** (`GoogleFont` provider) — asynchronous by construction, needs Play
  Services, and can fail offline. It would put a font swap on the one screen that must not move,
  and would breach "no network requirement" (FR-039).
- **`FontLoadingStrategy.OptionalLocal` / `Async`** — permits the swap FR-003 forbids.

---

## R3 — App labels in scripts the bundled faces do not cover

**Correction to an assumption in the spec.** FR-005 requires graceful fallback for characters a
face does not carry. Instrument Sans covers Latin, and JetBrains Mono covers Latin plus Cyrillic
and Greek. **Neither covers CJK, Arabic, Hebrew, Thai, or Devanagari** — and app labels are
supplied by other apps, so the app does not control them.

**Decision**: draw app labels with the bundled proportional face and rely on the platform's own
font fallback for code points it lacks; add a **manual test case** with at least one
non-Latin-labelled app installed; and hold one contingency ready.

**Contingency, if the manual case shows missing-glyph boxes**: render *app labels only* with the
platform default family, leaving every other string in the bundled face. This is a change to two
call sites — the list row and the two preview surfaces. FR-005 explicitly permits it; FR-004's
"app labels in the proportional face" yields to it, because a label the user cannot read is worse
than a label in a different face.

**Rationale**: Android's text stack appends a system fallback chain to custom typefaces, so this
is expected to work — but "expected" is not "verified", and the constitution forbids an agent
driving the device to find out. A named contingency with a known blast radius is the honest way
to carry an unverified assumption into implementation.

**Alternatives rejected**:

- **Bundling a CJK or pan-Unicode face** — tens of megabytes, against SC-007 and against YAGNI.
- **Assuming Latin-only labels** — false on any device with a non-English app installed.

---

## R4 — How the design system is expressed

**Decision**: override Material 3's `ColorScheme`, `Typography` and `Shapes` with SlowLock's
tokens, and add a **thin layer of four local composables** in a new `com.slowlock.ui.components`
package for the patterns M3 has no equivalent of: `ScreenHeader`, `PrimaryAction`,
`SecondaryAction`, and `SelectableTile`. Every other control stays an M3 control and takes its
appearance from the theme.

**Rationale**: Constitution IV fixes Material 3 as the UI toolkit, and the theme override is how
M3 is meant to be branded. But M3's `Button` is a 40dp pill and FR-006 wants 56dp at 16dp radius;
`FilterChip` cannot become a swatch tile (FR-023). Four small composables is less code than
fighting those defaults on five screens, and each is a single-purpose unit with an obvious
interface.

**Alternatives rejected**:

- **Theme override alone** — every screen would carry local overrides of the same four patterns,
  which is the duplication the component layer exists to prevent.
- **A full bespoke design system** on `CompositionLocal` tokens, abandoning M3 components —
  triples the code, discards `Slider`, `Scaffold`, `OutlinedTextField` and `LazyColumn` behaviour
  that already works, and sits against Constitution IV's fixed stack.

---

## R5 — Light-only, and the second window that has to agree

**Decision**: delete the `dynamicColor` parameter from `SlowLockTheme` outright and pass the light
scheme unconditionally; the `darkTheme` parameter goes with it. **Additionally, set
`android:windowBackground` on `Theme.SlowLock` to the screen ground colour.**

**Rationale**: FR-001 and FR-008. Deleting the parameters rather than defaulting them to `false`
is what makes FR-001 enforceable — a parameter left in place is a parameter someone passes `true`
to later. The `windowBackground` half is easy to miss and is the same class of defect FR-030
guards on the wait screen: without it, launching SlowLock paints the platform theme's white
starting window and then flips to bone one frame later. `Theme.SlowLock`'s parent is already a
light theme, so only the background attribute changes.

**Alternatives rejected**:

- **`darkTheme = false` as a default** — leaves the dark scheme reachable and the dynamic-colour
  branch alive, so a future edit can silently reintroduce both.
- **Deleting the dark scheme definitions** — Phase 3 needs the shape of them; they are replaced by
  the token-derived light scheme now and rebuilt from tokens later.

---

## R6 — The wait screen arrives in one frame

**Decision**: `Theme.SlowLock.Wait` keeps `android:windowBackground = @color/wait_background`; the
composable paints that same colour, the accent rule and the message in a single composition with
no asynchronous state of any kind.

**Rationale**: the Q2 clarification and FR-029/FR-030. The starting window covers the interval
before any app code runs; the first composed frame carries all three elements together because the
resource font is blocking (R2) and nothing on the screen is loaded, resolved or measured
asynchronously. `WaitScreen` today has no `produceState`, no `LaunchedEffect` and no remote read,
and this feature must not add one.

**Binding constraint for implementation**: nothing in `WaitScreen`'s composition may depend on a
value that arrives after the first frame. That rules out reading the target app's label, its icon,
or anything from the configuration store — none of which the screen is allowed to show anyway
(FR-032).

**Alternatives rejected**:

- **Drawing the whole screen into the window background as a layer-list drawable** — considered in
  the Q3 options and rejected: the message would become a picture, losing translation and font
  scaling.
- **Fading the message in** — an animation, forbidden by FR-029.

---

## R7 — The dark wait screen

**Decision**: `values-night` variants of all three wait values — background `#14120E`, message
`#8A857A`, rule `#C9821F` (the accent, unchanged).

**Rationale**: FR-031, and feature 003's rule that a bright field at night on this screen is a
defect. `#14120E` is the warm near-black the light ground implies rather than a neutral one, so
the two modes read as the same product. Measured contrast: message on background **5.09:1**, rule
on background **5.96:1** — both clear of SC-008's floor, and the rule is decorative regardless.

**Alternatives rejected**:

- **Reusing 003's `#101010` / `#9E9E9E`** — neutral grey, belongs to the pre-redesign palette.
- **Pure black** — cheaper on OLED, but reads as a dead screen rather than a quiet one, and the
  amber rule on pure black is harsher than intended.

---

## R8 — Restyling the slider without replacing it

**Decision**: keep Material 3's `Slider` and pass custom `track` and `thumb` slot content — an
amber active track over a `Fill`-coloured inactive track, and a 26dp `Card`-filled circle with a
3dp amber border.

**Rationale**: FR-016 asks for an appearance change, not a behaviour change. `Slider` already
carries the drag handling, keyboard and accessibility semantics, and the step snapping the
existing `DelayRange` mapping depends on. The slot API exists precisely for this, so no custom
gesture code is written and FR-021's "existing behaviour preserved" is free.

**Alternatives rejected**:

- **A hand-built slider** — re-implements drag, snapping and accessibility to change two colours
  and a shape; guarantees a regression against FR-021.
- **`SliderColors` alone** — reaches the track colours but not the ring-style thumb (FR-016).

---

## R9 — Selection semantics, and the 44dp opt-out

**Decision**: build the preset row and the treatment row as `Row`s carrying
`Modifier.selectableGroup()`, with each child a plain surface carrying
`Modifier.selectable(selected = …, role = Role.RadioButton, onClick = …)`.

**Rationale**: FR-043 requires the selected state to reach assistive technology, and FR-044
requires a meaningful label. `selectable` + `selectableGroup` is the platform's own expression of
single-choice selection, and it is what `FilterChip` used internally — so replacing the chips with
tiles keeps the semantics the chips carried instead of losing them. `Role.RadioButton` handles the
preset row's legitimate "nothing selected" state without special-casing.

**Correction, and a note for whoever implements this.** Material 3 components enforce a 48dp
minimum touch target automatically. Custom surfaces built with `Modifier.selectable` **do not** —
so the 44dp sizes FR-045 accepts arrive by default here, with no opt-out to write. The
implication is worth stating plainly: had these controls been built from M3 components, the
accessibility floor would have been met for free. FR-045 records the maintainer's decision to
prefer the drawn size, and this plan implements it — but the cost is real and the fix, if it is
ever re-decided, is one dimension constant.

**Alternatives rejected**:

- **`Modifier.clickable` + a colour change** — signals selection by colour alone, which FR-043
  forbids outright.
- **Keeping `FilterChip`** — cannot render a swatch above a name (FR-023).

---

## R10 — The delay numeral yields, and how

**Decision**: render the readout with `autoSize` text within a `weight`-bounded centre block. The
block gets whatever vertical space the header, slider, preset row and primary action leave; the
numeral takes the largest size that fits it, capped at 104sp and floored so it stays the largest
element on the screen.

**Rationale**: FR-014a. The controls beneath it are laid out as fixed-height siblings and the
centre block is the only flexible one, so the numeral is structurally the thing that yields —
there is no arrangement in which the primary action can be pushed off screen. This holds for large
font scales and small screens through the same mechanism, which is why FR-014a names both.

**Alternatives rejected**:

- **A vertically scrolling screen** — offered as a Q4 option and declined; the primary action can
  fall below the fold.
- **A fixed non-scaling numeral** — declined in Q4; a user who enlarged their font gets no benefit
  on the screen's most important number.
- **Manual breakpoints on screen height** — a table of magic numbers that goes stale on the next
  device size.

---

## R11 — Labels that are drawn in capitals

**Decision**: store them capitalised in the string resources — `SECONDS`, `ICON`, `NO ROOM ON THE
HOME SCREEN` — with a translator comment on each explaining that the capitalisation is
stylistic. Letter-spacing is applied by the text style, not by inserting spaces.

**Rationale**: consistent with the spec's existing ruling on lower-case `please wait`: the string
resource holds exactly what is drawn, so no locale-sensitive case transform runs at display time.
Turkish dotted/dotless `i` is the standing example of why `uppercase()` on user-visible text is a
trap. A translator working into a script with no letter case simply returns text without case,
which is the correct result.

**Alternatives rejected**:

- **Natural case in resources, uppercased at display** — reintroduces the locale hazard and makes
  the drawn string differ from the stored one.
- **Spaces inserted between letters** — destroys the string for screen readers.

---

## R12 — The rename: exactly which strings change

FR-041 bars "shortcut" from user-visible text; FR-042 confines the change to display values.
The complete inventory of string **values** to reword — every `name=` attribute stays:

| Resource name (unchanged) | Now | Becomes |
|---|---|---|
| `shortcut_config_title` | "New shortcut" | "New lock" |
| `shortcut_config_create` | "Create shortcut" | "Add to home screen" |
| `shortcut_target_unavailable` | "…so no shortcut was created." | "…so no lock was created." |
| `shortcut_icon_unavailable` | "…so no shortcut can be created right now…" | "…so no lock can be created right now…" |
| `delay_config_next` | "Next" | "Choose the icon" |
| `wait_message` | "Please wait" | "please wait" |

**Decision**: resource names, the `shortcut` package, `ShortcutContract`, `ShortcutLaunchActivity`,
`contracts/pinned-shortcut.md` and every persisted key stay exactly as they are.

**Rationale**: FR-042 and Constitution V. `ShortcutLaunchActivity`'s fully-qualified name is
written into every pinned shortcut's intent and is asserted by `ShortcutContractTest`; renaming
anything in that chain to match a display-word change would break icons already on users' home
screens. The word the user reads and the identifier the system stores are allowed to differ, and
here they must.

---

## R13 — What can be tested without a device

Constitution v1.1.0 permits JVM unit tests only and forbids instrumented suites. This feature is
overwhelmingly presentation, but three things in it are pure logic and get tests:

| New test | Covers |
|---|---|
| `DelayRangeTest` (extended) | `PRESETS` are 5/10/30; every preset is inside `MIN..MAX`; every preset is `snap`-stable; `presetFor` returns nothing for a non-preset value (FR-017–FR-019) |
| `SlowLockPaletteTest` | The palette contains **exactly** the eleven tokens FR-002 names, each asserted against its literal hex; a WCAG contrast calculation over every declared text-on-surface pairing asserts ≥4.5:1 (FR-002, FR-009, SC-008, SC-009) |
| Existing `ShortcutContractTest`, `DelayConfigTest` | Unchanged, and must still pass — this is the mechanical guard on FR-038 and FR-042 |

`SlowLockPaletteTest` is the interesting one: it turns SC-008 and SC-009 from review items into
build failures. `androidx.compose.ui.graphics.Color` is a pure-Kotlin value class, so the
contrast arithmetic runs on the JVM with no Android framework and no Robolectric.

Everything else — first-paint timing, the dark wait screen, glyph fallback, layout at large font
scales, screen-reader announcements — is a numbered case in the manual test plan, run by the
maintainer.

---

## R14 — Contrast, computed

Every text-on-surface pairing the design uses, measured (WCAG 2.1 relative luminance):

| Text | On | Ratio | |
|---|---|---|---|
| Ink `#17150F` | Card `#FBF9F5` | 17.36 | ✅ |
| Ink | Bone `#F3F0EA` | 16.05 | ✅ |
| Ink | Paper `#EFEDEA` | 15.62 | ✅ |
| Ink60 `#4A463C` | Card | 8.95 | ✅ |
| Ink60 | Bone | 8.27 | ✅ |
| Ink `#17150F` | Amber `#C9821F` | 5.82 | ✅ primary action label |
| AmberDark `#8A5610` | Card | 5.84 | ✅ |
| AmberDark | Bone | 5.40 | ✅ |
| Ink40 `#6F6A5E` | Card | 5.12 | ✅ |
| AmberDark | AmberWash `#F2E4CE` | 4.90 | ✅ selected tile |
| Ink40 | Bone | 4.74 | ✅ **thinnest margin** |
| Wait message `#8A857A` | Wait dark `#14120E` | 5.09 | ✅ |
| — Amber `#C9821F` | Bone | **2.76** | ❌ **never text** |

**Decision**: the last row is why FR-009 exists. Amber is a fill, a border and a rule — never a
glyph on the ground. Where an accent-coloured word is wanted, `AmberDark` is the token (5.40 on
bone).

**Note on the thinnest margin**: muted ink on the screen ground clears the floor by 0.24. It is
used for captions, eyebrows and secondary readouts — never for body copy, which takes `Ink60` at
8.27. Darkening `Ink40` is the remedy if it ever proves uncomfortable in daylight; it is not
required to pass.
