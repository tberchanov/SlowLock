# Feature Specification: Visual Redesign (Phase 1)

**Feature Branch**: `004-visual-redesign`

**Created**: 2026-08-24

**Status**: Draft

**Input**: User description: "Implement the SlowLock redesign from the Claude Design canvas
(`SlowLock Redesign.dc.html`), split into phases. Phase 1 is the visual layer over the screens
that already exist. Specify that, and do not forget to mention what is out of scope for phase 2."

---

## Overview

Features 001–003 built a working three-step flow — pick an app, choose a delay, pick an icon
treatment and pin — on Material 3's stock look and on **dynamic colour**, which means SlowLock
currently borrows its identity from whatever wallpaper the user happens to have. It has no visual
voice of its own, and the delay — the one number the whole product exists to sell — is a caption
above a slider.

This feature gives the app its own identity and re-weights the screens around that number. It is
a **presentation change plus one small behavioural addition** (delay presets). It moves no data,
changes no persisted value, and changes no contract that a pinned shortcut depends on.

The design is defined by seven artboards in `SlowLock Redesign.dc.html`. **Two of them are not
built here** — see [Out of Scope](#out-of-scope).

---

## Clarifications

### Session 2026-08-24

- Q: What is the canonical user-facing noun for the thing a user creates — an app plus its delay
  plus its home-screen icon? → A: **"Lock"**, adopted now. "Shortcut" is retained only where it
  names the Android mechanism, not the user's object.
- Q: When must the wait screen be visually complete, and from what moment is it frozen? → A:
  **Complete in one frame.** The starting window paints the ground; the first composed frame
  carries the accent rule and the message together (≤500ms from tap). The freeze applies from
  that frame onward. No intermediate state in which the rule is up but the message is not.
- Q: The delay presets (44dp as drawn) and the treatment tiles are new interactive controls below
  Android's 48dp accessibility floor. How should that be resolved? → A: **Ship as drawn at 44dp.**
  Design fidelity is preferred over the floor here; the shortfall is an accepted limitation
  recorded in FR-045, not an oversight.
- Q: At the largest system font scale on a small screen, the delay screen cannot fit the oversized
  numeral and all its controls. What gives? → A: **The numeral yields.** All text scales with the
  system setting, but the readout shrinks to whatever the available centre space allows rather
  than pushing the slider, presets or primary action off screen.
- Q: Five static font files would exceed the 800KB size budget. How should the typefaces be
  packaged? → A: **Static weight files; the budget rises.** Five separate files (three sans, two
  mono), and SC-007's cap moves from 800KB to 1.5MB. Simplicity of setup is preferred over the
  smaller payload a variable font would give.

---

## Out of Scope

This section is binding. A reviewer should be able to reject work that lands anything below.

### Deferred to Phase 2 — a separate feature (`005`)

The canvas draws two screens the app has no equivalent of. Both are **new behaviour**, not
styling, and both need state the app does not currently keep — a durable notion of *which locks
exist*, distinct from *which packages have a saved configuration*.

| Deferred item | Why it is not Phase 1 |
|---|---|
| **First-run screen** ("A pause between you and the app.") | Requires a persisted "has been introduced" flag and a fourth root state. No existing screen it restyles. |
| **Locks home screen** (list of created locks, `+ New lock`) | Requires enumerating configured apps and deciding what a lock *is* once its shortcut has been removed from the home screen — a question 003 deliberately left unanswered because the launcher cannot be queried. |
| **Back tile on the app list** | Only has a target once Locks is the root. In Phase 1 the list *is* the root, so the control would duplicate the system back gesture. |
| **`1 / 3`, `2 / 3`, `3 / 3` step counters** | The counter implies a wizard entered from somewhere. Until Locks exists, step 1 has no predecessor and the count is a claim the app cannot honour. |
| **Anything that enumerates or edits existing locks** | Editing, deleting, and re-pinning are Phase 2's subject matter entirely. |

Phase 1 MUST NOT add a "has been introduced" flag, MUST NOT add an enumeration capability to the
configuration store, and MUST NOT add a root state beyond the three that exist today.

### Deferred to Phase 3

| Deferred item | Why |
|---|---|
| **Dark palette for the four in-app screens** | Every artboard is light. A dark ramp derived without a reviewed design is a guess, and the maintainer wants to see the light build on a device first. Phase 1 pins those screens to light regardless of the system setting (FR-008). |

### Deferred, phase not yet assigned

| Deferred item | Why |
|---|---|
| **The launcher icon** | The design source carries five icon directions — Pause, Half-filled dot, The gap, Open ring, Three beats — explicitly labelled *"sketches for direction, not finished artwork"*. Choosing one is a branding decision, not a restyle, and none of the five is resolved enough to build. The app keeps its current launcher icon through Phase 1. |

Picking a direction, drawing it as a vector, and handling the adaptive-icon foreground, background
and monochrome layers is its own small feature. **Phase 1 MUST NOT touch `res/mipmap-*` or
`res/drawable/ic_launcher_*`.**

**The wait screen is the single exception** and keeps its light/dark pair (FR-028). Feature 003
classifies a full-brightness field at night on that screen as a defect, and this feature does not
regress it.

### Out of scope permanently (this feature)

- No change to any persisted value, key, file name, or frozen token.
- No change to what a pinned shortcut carries or which class it targets.
- No new permission, no new third-party dependency, no network access.
- No change to how apps are enumerated, how icons are rasterized or cached, or how the hand-off
  to the target app works.
- No new screen of any kind.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The app looks like itself (Priority: P1)

A user opens SlowLock on a phone with a bright green wallpaper. Today the app is tinted green;
on a different phone it is tinted blue. After this story it is the same warm, quiet, paper-and-
amber app on both — its colours, its typefaces, its shapes, and nothing borrowed from the device.

**Why this priority**: It is the foundation every other story is drawn on. It also stands alone:
shipped by itself, the whole app changes character even before a single screen is re-laid-out.

**Independent Test**: Install on two devices with different wallpapers and system accent colours.
Every screen renders identically on both. Switch the system to dark mode; the four in-app screens
stay light and stay legible.

**Acceptance Scenarios**:

1. **Given** a device whose system accent colour is blue, **When** the user opens any screen,
   **Then** no blue appears anywhere and the primary action is amber on paper.
2. **Given** a device set to dark mode, **When** the user opens the app list, **Then** the screen
   renders in the light palette and all text meets the contrast floor.
3. **Given** the device is in airplane mode on first launch, **When** any screen renders,
   **Then** the SlowLock typefaces are used — no fallback to a system face, no reflow after
   first paint.
4. **Given** a device with the system font size set to its largest step, **When** the user opens
   any screen, **Then** text scales with the setting and no primary action is clipped or pushed
   off-screen — on the delay screen the numeral shrinks to make that true.

---

### User Story 2 - The delay is the point of the screen (Priority: P2)

A user picking a delay sees the number they are choosing as the largest thing on the screen, set
in the same numeral face used everywhere a number appears in the app. Below it a slider, and
below that three one-tap presets for the values people actually pick.

**Why this priority**: This is the screen that carries the product's thesis, it is the one the
canvas re-weights most, and it is the only story with new behaviour to test.

**Independent Test**: Open the delay screen for any app, tap `10s`, confirm the readout reads
`10` and the slider thumb has moved. Drag the slider to a value no preset matches; confirm no
preset is highlighted.

**Acceptance Scenarios**:

1. **Given** the delay screen is open at 10 seconds, **When** the user reads the screen,
   **Then** `10` is displayed as a large numeral with a `SECONDS` caption beneath it, and the
   target app is shown as a small pill above.
2. **Given** the delay screen is open at 10 seconds, **When** the user taps the `30s` preset,
   **Then** the readout becomes `30`, the slider moves to match, and `30s` becomes the
   highlighted preset.
3. **Given** the user has dragged the slider to 17 seconds, **When** they look at the preset row,
   **Then** none of the three presets is highlighted and the readout reads `17`.
4. **Given** any delay is chosen, **When** the user taps the primary action, **Then** it is
   labelled "Choose the icon" and the icon step opens carrying that value.
5. **Given** the user came back from the icon step, **When** the delay screen reopens, **Then**
   it shows the value they chose on the way through — unchanged from today's behaviour.

---

### User Story 3 - The icon step reads as a preview and a choice (Priority: P3)

The last step shows the shortcut roughly as it will appear on the home screen — a card holding
the icon, the app's name and the wait it will impose — with the three treatments as tiles the
user picks between rather than filter chips.

**Why this priority**: It is the confirmation step before an irreversible-feeling action, and the
canvas's card makes the outcome legible in a way a floating icon does not. No new behaviour.

**Independent Test**: Open the icon step, tap each treatment in turn, confirm the preview icon
recolours and the delay line beneath the label matches the value chosen on the previous screen.

**Acceptance Scenarios**:

1. **Given** the icon step is open for an app configured at 10 seconds, **When** the user reads
   the preview card, **Then** it shows the icon, the app's name, and "10 second wait".
2. **Given** the icon step is open, **When** the user taps "Inverted", **Then** that tile becomes
   the selected one and the preview icon inverts.
3. **Given** a treatment is selected, **When** the user taps the primary action, **Then** it is
   labelled "Add to home screen" and a line beneath it says the launcher will ask to confirm.
4. **Given** the target app's icon cannot be loaded, **When** the step renders, **Then** the
   existing explanatory message appears and the primary action stays disabled.

---

### User Story 4 - The wait is quiet, but it is SlowLock's (Priority: P4)

Tapping a pinned icon lands on the same paper background as the rest of the app, a small amber
rule, and the words `please wait`. Nothing moves for the whole delay.

**Why this priority**: It is the screen the user meets most often, and it is the one with the
tightest constraints — so it is worth landing last, on foundations the other stories proved.

**Independent Test**: Tap a pinned shortcut in a dark room with the phone in dark mode; the
screen is dark, not a white field. Tap one in daylight in light mode; the screen is paper. In
both, watch the whole delay and confirm nothing changes.

**Acceptance Scenarios**:

1. **Given** the device is in light mode, **When** the user taps a pinned shortcut, **Then** the
   final background colour is on screen from the first frame — no flash of another colour.
2. **Given** the device is in dark mode, **When** the user taps a pinned shortcut, **Then** the
   screen is the dark variant of the palette, with no white frame at any point.
3. **Given** a 30-second delay, **When** the user watches the whole wait, **Then** the screen
   arrives complete — rule and message together, never one before the other — and after that
   nothing changes: no countdown, no progress, no animation, no pulse.
4. **Given** the wait screen is showing, **When** the user reads it, **Then** it does not name the
   app, state a duration, or suggest that anything is loading.

---

### User Story 5 - The unsupported-launcher screen speaks the same language (Priority: P5)

A user whose launcher refuses pinned shortcuts sees the explanation in the redesigned voice — a
small mono heading, a clear sentence, and the two actions styled as primary and secondary.

**Why this priority**: Rare, but it is a dead end, and a dead end in the old visual language
reads like a system error rather than a message from the app.

**Independent Test**: Set a launcher that does not support pinning, open the app, confirm the
screen renders in the new palette with both actions and unchanged behaviour.

**Acceptance Scenarios**:

1. **Given** the launcher does not support pinning, **When** the app opens, **Then** the screen
   shows a small uppercase heading, the explanation, an amber primary action and an outlined
   secondary action.
2. **Given** that screen, **When** the user taps the secondary action, **Then** support is
   re-checked and the flow proceeds if it now succeeds — unchanged from today.

---

### Edge Cases

- **A long or non-Latin app label.** Labels come from other apps; SlowLock does not control them.
  A CJK, Cyrillic, Arabic, or emoji-bearing label MUST render in a face that has the glyphs
  rather than showing tofu, and MUST truncate rather than overflow in the app-list row, the delay
  screen's pill, and the icon preview card.
- **The largest system font scale on a small screen.** The delay numeral yields first: it shrinks
  to the space available so the slider, presets and primary action stay on screen and reachable
  (FR-014a). Every other screen's text scales normally.
- **A short or small screen.** The delay screen's centre block MUST yield space before the
  controls beneath it do.
- **A user relying on a screen reader.** Selecting a preset or a treatment MUST be announced as a
  selection change; the delay readout MUST be readable as a value with its unit, not as a bare
  numeral floating above an unrelated caption.
- **Right-to-left layout.** The app declares RTL support; the header, preset row, treatment tiles,
  and preview card MUST mirror.
- **A delay that matches no preset.** No preset is highlighted; this is a normal state, not an
  error.
- **A saved delay outside the visible range.** Handled as today by the existing sanitising rule;
  the redesign MUST NOT introduce a second, different clamp.
- **The device is in dark mode.** The four in-app screens stay light by design in this phase; the
  wait screen does not.
- **A shortcut pinned before this feature shipped.** MUST behave identically and MUST show the new
  wait screen without being re-created.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Identity and foundations

- **FR-001**: The app MUST use a single fixed palette on every screen and MUST NOT derive any
  colour from the device wallpaper, system accent, or any other device-supplied source.
- **FR-002**: The palette MUST be exactly these eleven values, and no screen may introduce a
  twelfth: canvas `#EFEDEA`; screen ground `#F3F0EA`; raised surface `#FBF9F5`; ink `#17150F`;
  secondary ink `#4A463C`; muted ink `#6F6A5E`; accent `#C9821F`; accent-dark `#8A5610`;
  accent-wash `#F2E4CE`; hairline `#E3DED3`; fill `#E7E2D7`. The design source draws the slider's
  inactive track at `#E4DFD4`, three points per channel from the fill token and indistinguishable
  from it; that one value is rendered with the fill token rather than admitted as a twelfth.
- **FR-003**: The app MUST ship with its own typefaces — Instrument Sans for prose and JetBrains
  Mono for numerals and labels — packaged with the app so they render identically on every device
  with no network access and no substitution after first paint. Each weight used MUST be shipped
  as its own font file; no weight may be synthesised from another.
- **FR-004**: The monospaced face MUST be used for every numeric value, unit caption, uppercase
  eyebrow label, and footnote. The proportional face MUST be used for headings, body copy, app
  labels, and action labels.
- **FR-005**: Both faces MUST fall back gracefully for characters they do not cover, so that an
  app label in any script renders in a face that has the glyphs rather than as missing-glyph
  boxes.
- **FR-006**: A screen's primary action MUST be a full-width amber button with ink-coloured label,
  56dp tall with 16dp corners. A screen MUST have at most one primary action.
- **FR-007**: A screen's secondary action MUST be a 52dp outlined button with a transparent fill
  and a hairline border.
- **FR-008**: The app list, delay, icon and unsupported-launcher screens MUST render in the light
  palette regardless of the device's light/dark setting. (Phase 1 only — see Out of Scope.)
- **FR-009**: All text MUST meet a contrast ratio of at least 4.5:1 against the surface it sits
  on. The accent colour MUST NOT be used as body text on the screen ground; it is a fill, a
  border, and a rule.

- **FR-043**: Every control that has a selected state — each delay preset and each icon-treatment
  tile — MUST expose that state to assistive technology, not signal it by colour alone. Replacing
  the previous chip controls MUST NOT lose the selection semantics they carried.
- **FR-044**: Every control MUST carry a meaningful accessible label. Controls whose visible text
  is a bare value ("5s", "10s", "30s") MUST read as the action they perform.
- **FR-045**: The delay presets and the icon-treatment tiles ship at the sizes the canvas draws
  them, which places their touch targets below the 48dp accessibility floor. **This is an accepted
  limitation, decided deliberately in favour of design fidelity** (Clarifications, 2026-08-24).
  It applies to these two control groups only: every other interactive element — the primary and
  secondary actions, the back tile, list rows, the slider, and the search field — MUST meet 48dp.

#### App list

- **FR-010**: The list MUST show the title "Choose an app" with no back control and no step
  counter.
- **FR-011**: The search field MUST be a 52dp filled box with 14dp corners, a hairline border, and
  the existing placeholder text.
- **FR-012**: List rows MUST be 64dp tall with a 44dp icon at 12dp corners, separated by a
  hairline divider.
- **FR-013**: The loading, empty, and no-results states MUST keep their current behaviour and
  wording, restyled to the new palette and typefaces.

#### Delay configuration

- **FR-014**: The chosen delay MUST be displayed as a large monospaced numeral, no smaller than
  96sp at default font scale, with an uppercase letter-spaced `SECONDS` caption beneath it.
- **FR-014a**: The numeral MUST scale with the system font setting, but MUST shrink to fit the
  space the screen can spare rather than displacing anything below it. At every font scale and on
  every supported screen size, the slider, the preset row and the primary action MUST all remain
  fully visible and reachable without scrolling, and the numeral MUST remain the largest element
  on the screen.
- **FR-015**: The target app MUST be shown above the numeral as a compact pill containing its
  icon and label.
- **FR-016**: The slider MUST show an amber active track and a ring-style thumb, with the range
  endpoints labelled beneath it.
- **FR-017**: The screen MUST offer three one-tap presets — 5, 10 and 30 seconds. Tapping one MUST
  set the delay to that value.
- **FR-018**: The preset matching the current delay MUST be visibly selected. When no preset
  matches, none MUST be selected.
- **FR-019**: Presets MUST NOT change the selectable range, its minimum, its maximum, or its step.
  Every preset MUST be a value the slider can also reach.
- **FR-020**: The primary action MUST be labelled "Choose the icon".
- **FR-021**: All existing delay-screen behaviour MUST be preserved: the value carried forward,
  the value restored when returning from the icon step, back returning to the list, and the
  handling of a target that has become unavailable.

#### Icon and creation

- **FR-022**: The shortcut preview MUST be presented inside a bordered card containing the icon at
  96dp, the app's label, and the chosen delay written out in the monospaced face.
- **FR-023**: The three icon treatments MUST be presented as three equal-width tiles, each showing
  a swatch and its name, beneath an uppercase `ICON` label.
- **FR-024**: The selected treatment's tile MUST be filled with the accent wash and bordered in
  the accent colour.
- **FR-025**: The primary action MUST be labelled "Add to home screen", with a footnote beneath it
  stating that the launcher will ask the user to confirm.
- **FR-026**: All existing behaviour MUST be preserved: the treatment the screen opens on, the
  disabled state while the icon is unavailable, the messages shown when the target or its icon
  cannot be resolved, and what is saved and pinned when the action is taken.

#### The wait screen

- **FR-027**: The wait screen MUST use the redesigned palette: the screen ground as its
  background, a short horizontal accent rule above the message, and the message in the
  monospaced face in muted ink.
- **FR-028**: The message MUST read `please wait` in lower case.
- **FR-029**: The wait screen MUST reach its complete appearance — ground, accent rule and
  message together — in a single frame, with no intermediate state in which some elements are
  drawn and others are not. From that frame until the wait ends, **nothing on the screen may
  change**: no countdown, no progress indication, no animation, and no change of colour or
  opacity.
- **FR-030**: The window's starting background MUST be the same colour the wait screen paints, so
  the tap lands on the final background with nothing to flash.
- **FR-031**: The wait screen MUST provide a dark variant of all three of its values — background,
  rule, and message — selected by the device's light/dark setting. This is the one surface in
  Phase 1 that follows that setting.
- **FR-032**: The message MUST NOT name the target app, state a duration, or suggest that anything
  is loading.
- **FR-033**: The wait screen MUST resolve its own colours and type independently of the rest of
  the app's styling, so that a change made elsewhere cannot alter it by accident.

#### Unsupported launcher

- **FR-034**: The screen MUST show an uppercase monospaced heading above the explanation, both
  left-aligned.
- **FR-035**: The two actions MUST be a primary "Choose home screen app" and a secondary outlined
  "Check again", in that order.
- **FR-036**: The existing behaviour MUST be preserved, including the fallback message shown when
  the system settings screen cannot be opened.

#### Terminology

- **FR-041**: "Lock" MUST be the only noun used in user-visible copy for what the user creates.
  "Shortcut" MUST NOT appear in any string the user reads, except where it quotes the platform's
  own wording.
- **FR-042**: Renaming is confined to display text. No persisted key, resource identifier, class
  name, package name, contract filename, or frozen token may be renamed in service of this
  change (see FR-038).

#### Boundaries and amendments

- **FR-037**: Feature 003's requirement that the wait screen be *unbranded* MUST be amended. The
  binding property becomes that the screen is **static and not worth reading twice**; its colours
  and type are this feature's to set. Every other wait-screen obligation — escapability, no
  background launch, the timing rules, and the no-flash rule — is carried forward unchanged.
- **FR-038**: The frozen pinned-shortcut contract and the frozen configuration-store contract MUST
  NOT change. No persisted key, file name, token, or target class name may be altered.
- **FR-039**: This feature MUST NOT add a permission, a third-party dependency, network access, or
  a Gradle module.
- **FR-040**: Shortcuts pinned before this feature MUST continue to work and MUST show the
  redesigned wait screen without being re-created.

### Key Entities

- **Lock**: The user-facing name for what a user creates — one target app, the delay chosen for
  it, the icon treatment chosen for it, and the home-screen icon that fires it. This is the
  canonical noun in all user-visible copy from this feature onward. "Shortcut" is **not** a
  synonym for it: that word now refers only to the Android mechanism a lock is built on, and
  survives only in places the user does not read — internal contract names, the launcher's own
  confirmation dialog, and platform API discussion.
- **Design token**: A named, fixed visual value — a colour, a type role, a corner radius, or a
  control height — referenced by name rather than repeated literally. The complete set is closed:
  FR-002 fixes the colours, and no screen may introduce a token outside it.
- **Delay preset**: A named shortcut to a commonly chosen delay. Three exist (5, 10, 30 seconds).
  A preset is a convenience over the existing range, never an alternative to it, and holds no
  state of its own — whether one appears selected is derived from the current delay.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Comparing each built screen against its artboard side by side, the maintainer finds
  **zero unexplained deviations** in palette, typeface role, control height, or corner radius.
- **SC-002**: On two devices with different wallpapers and different system accent colours, every
  screen renders **identically**.
- **SC-003**: Setting a delay to 5, 10 or 30 seconds takes **exactly one tap**, down from a drag
  that must be landed on the right stop.
- **SC-004**: The full journey from opening the app to a configured icon on the home screen still
  completes in **under 45 seconds and at most 5 taps**, unchanged from feature 003.
- **SC-005**: The wait screen's final background is on screen within **200ms** of the tap, the
  screen is visually complete within **500ms**, and nothing on it changes at any point after
  that.
- **SC-006**: Across a full 30-second wait, a frame captured at the start and one captured at the
  end are **indistinguishable**.
- **SC-007**: Installed app size grows by **no more than 1.5MB** relative to the previous release.
  Typography is the whole of that growth; anything beyond it is unaccounted for and MUST be
  investigated before release.
- **SC-008**: Every text-on-surface pairing in the shipped palette measures at least **4.5:1**
  contrast, verified by calculation for each pairing used.
- **SC-009**: No screen renders **any** colour outside the eleven in FR-002 — verified by
  reviewing the named token set and confirming that no screen defines a colour of its own.
- **SC-010**: On the smallest supported screen at the largest system font scale, all five screens
  present every interactive control fully visible without scrolling.
- **SC-011**: With a screen reader active, every interactive control announces a meaningful label
  and, where applicable, its selected state — verified by walking all five screens.
- **SC-012**: The build gates — a debug build and the unit-test suite — pass, and the numbered
  manual test plan is executed by the maintainer with every case recorded pass or fail.

---

## Assumptions

- **Typeface licensing.** Instrument Sans and JetBrains Mono are both offered under the SIL Open
  Font License, which permits bundling in an application. The licence files are vendored
  alongside the fonts. If either licence turns out to differ, the affected face is replaced rather
  than the design being shipped without it.
- **Font weights and packaging.** Three weights of the proportional face (regular, medium,
  semibold) and two of the monospaced face (regular, medium) cover every use in the canvas, and
  each is bundled as its **own static font file** — five files in total. Additional weights are
  not bundled, and no weight may be synthesised by the renderer from a face that was not shipped.
  Variable fonts were considered and rejected: they would have cut the payload to roughly a third,
  but a named-weight file per weight is the simpler arrangement and the size cost was accepted
  instead (Clarifications, 2026-08-24). This is the single largest contributor to SC-007.
- **Artboard geometry.** The artboards are drawn at 412×892, a common modern phone. Dimensions in
  this spec are read as density-independent and MUST adapt to other sizes rather than being
  reproduced as fixed pixel positions.
- **Preset values.** 5, 10 and 30 seconds are taken directly from the canvas. They sit inside the
  existing 1–30 second range, and 30 is deliberately its maximum.
- **String changes are intentional.** "Next" becomes "Choose the icon" and "Create shortcut"
  becomes "Add to home screen" because the canvas names the outcome rather than the mechanism.
  "Please wait" becomes lower-case `please wait` as a literal string rather than by transforming
  the case at display time, so no locale can transform it unexpectedly.
- **Dark mode is a deliberate gap, not an oversight.** The four in-app screens are pinned to light
  in this phase; a user in dark mode gets a bright app until Phase 3. The wait screen — the
  surface most likely to be met in a dark room — is exempt.
- **The canvas's runtime is not a deliverable.** `support.js` in the design project is the
  generated engine that renders the artboards in a browser. It defines how the mock's data
  bindings and repeats are read; none of it is ported.
- **Sub-minimum touch targets are a known, chosen cost.** The delay presets and treatment tiles
  are below Android's 48dp floor by design (FR-045). Anyone auditing this app's accessibility will
  find it; the spec states it rather than leaving it to be discovered. Raising the preset row to
  48dp remains the single-line fix if that trade is ever re-decided.
- **Three places the build deliberately differs from the design source**, so that a
  side-by-side comparison does not read them as defects: the icon step's title reads "New lock"
  where the artboard still reads "New shortcut" (the terminology decision post-dates the
  artboard); the slider's inactive track uses the fill token rather than the artboard's
  near-identical `#E4DFD4`; and the app-list back tile and step counters drawn on the artboard are
  not built at all (Out of Scope). Everything else follows the artboards, including sizes and
  letter-spacing.
- **No verification on a device by an agent.** Everything that can only be seen running — the
  no-flash first frame, the dark wait screen, font rendering, layout at large font scales — is a
  numbered case in the manual test plan and is run by the maintainer.

---

## Dependencies

- Features 001, 002 and 003 are complete and their screens exist; this feature restyles them.
- Feature 003's spec and its wait-screen contract require the amendment described in FR-037.
  That amendment is part of this feature's work, not a prerequisite met elsewhere.
- Phase 2 (`005`) depends on this feature: the First-run and Locks screens are drawn in the
  palette, typefaces and components this feature establishes.
