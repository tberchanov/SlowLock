# Phase 0 Research: Legible system bar and a redesigned Locks screen

**Feature**: `specs/007-locks-screen-polish` | **Date**: 2026-08-25

Every unknown the Technical Context raised, resolved. Nothing here is a preference; each entry
names what was chosen, why, and what was rejected.

---

## R1 — Why the system indicators turn white, and the one-line fix

**Decision**: `MainActivity` keeps calling `enableEdgeToEdge()`, but passes an explicit
`statusBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT)` instead of taking the default.

**Rationale**: `enableEdgeToEdge()` with no arguments applies `SystemBarStyle.auto(...)`, whose
`detectDarkMode` reads the device's night-mode configuration. The app is light-only by design
(feature 004 FR-008), so on a device set to dark mode the platform is told "this is a dark
surface" and draws light system icons — over the app's bone background. That is exactly the
reported defect, and it is a two-argument fix rather than a theming problem.

`SystemBarStyle.light(...)` is the variant whose `detectDarkMode` is a constant `false`: it
declares the bar's *background* light, which is what makes the platform draw the glyphs dark. The
name reads backwards at first glance and is worth a comment at the call site.

Both scrims are transparent because the app draws its own bone ground behind the bar and a scrim
would be a second colour over it. Dark status-bar icons have existed since API 23, well under this
project's `minSdk 26`, so no scrim is ever needed as a fallback for the status bar.

**Alternatives considered**:

- **`WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true`** —
  rejected. It sets the same platform bit by hand, after `enableEdgeToEdge()` has already set it
  from the configuration. Two writers to one bit, with the correct value depending on call order,
  is how this comes back on the next `androidx.activity` upgrade.
- **A `values-night` theme override** — rejected. It re-admits the device's night setting as an
  input to the app's appearance, which is the thing FR-002 forbids.
- **Painting the status bar strip solid black** — rejected as a reading of the request; see the
  spec's Assumptions. The artboards draw that strip as part of the bone screen with near-black
  glyphs, and this decision reproduces the artboard.

---

## R2 — The navigation bar, and the one tier that cannot follow

**Decision**: apply the same `SystemBarStyle.light(TRANSPARENT, TRANSPARENT)` to the navigation
bar. Accept, and record, that API 26 devices keep light navigation-bar icons.

**Rationale**: leaving the navigation bar on the default `auto` style would leave half the defect
in place — on a dark-mode device the platform would still paint its default translucent dark scrim
along the bottom of a bone screen, and the app's appearance would still change with a system
setting. Treating both bars the same way is what makes FR-002 true of the whole window.

The platform bit for dark navigation-bar icons (`isAppearanceLightNavigationBars`) arrived in
API 27. This project's `minSdk` is 26, so on that single API level the request cannot be honoured
and the icons stay light over bone. The alternative — passing a dark scrim so that tier gets a
black strip instead — would trade an invisible-icon problem on API 26 for a visible black bar on
API 26 **and** on 27–28, where `androidx` uses the same scrim path even though those levels *can*
draw dark icons. That is a worse trade for a much larger group.

This is recorded the way Constitution I records the other accepted limitations: named in the manual
test plan as untested-and-accepted rather than quietly left to be rediscovered.

**Alternatives considered**: a dark scrim (rejected above); an `Ink`-coloured navigation bar on all
levels (rejected — it contradicts the artboard, which draws the gesture pill as dark ink on bone);
`minSdk 27` (rejected — a supported-device decision is not this feature's to make).

---

## R3 — Where the new type roles go

**Decision**: four roles are added to `SlowLockType` in `ui/theme/Type.kt`. No existing role
changes value.

| New role | Value | Drawn by |
|---|---|---|
| `TitleDisplay` | Instrument Sans, Medium, 30sp, −0.015em | the Locks title |
| `Count` | JetBrains Mono, Regular, 12sp, +0.06em | the caption under it |
| `RowTitle` | Instrument Sans, **Medium**, 17sp | a lock's app name |
| `Badge` | JetBrains Mono, Medium, 15sp | the delay badge |

**Rationale**: all four are read straight off the `New · Locks` artboard, which is what contract C6
means by "the design assigns type by role". `RowTitle` is deliberately a *sibling* of `RowLabel`
rather than an edit to it: `RowLabel` is the app list's row style, the artboard draws that list at
Regular, and changing it in place would silently restyle a screen this feature is not allowed to
touch (FR-025). `Count` is likewise a sibling of `Eyebrow` — same family and size, different
tracking, because the artboard tracks the caption at 0.06em and the unsupported-launcher eyebrow at
0.14em.

**Alternatives considered**: reusing `Title` (22sp) for the heading — rejected, it is visibly not
the artboard and is the style the *flow* screens use, which is the distinction User Story 2 exists
to draw. Declaring the styles inline in `LocksScreen.kt` — rejected by FR-024 and by the same
reasoning that keeps colours out of screens.

---

## R4 — A sixth corner radius

**Decision**: add `val Badge = RoundedCornerShape(9.dp)` to `ui/theme/Shape.kt`, alongside the
existing `Pill`, and leave the five Material slots untouched.

**Rationale**: contract C9 maps five design radii onto Material's five slots, and all five are
spoken for. `Shape.kt` already holds one shape outside that mapping — `Pill`, for the delay
screen's app chip — with the stated reason that M3 has nowhere sensible to put it. The badge is the
same situation: 9dp is a real value on the artboard and there is no free slot for it. Following the
existing precedent costs nothing; forcing the badge into `extraSmall` (12dp) would put a shape on
screen that the design does not contain.

**Alternatives considered**: reusing `extraSmall` (rejected — visibly wrong at badge size);
renumbering the M3 slots to make room (rejected — it would restyle every screen).

---

## R5 — The row's icon

**Decision**: the icon box grows from 44dp to 48dp; the *placeholder* is clipped with
`shapes.small` (14dp) instead of `shapes.extraSmall` (12dp). The loaded bitmap is drawn unclipped,
exactly as today.

**Rationale**: 48dp and a 14dp corner are the artboard's values. Clipping only the placeholder is
existing, deliberate behaviour: a real launcher icon already carries its own mask and shape, and
clipping it a second time would shave adaptive icons that are meant to be round. The artboard draws
a rounded square because that is what a placeholder looks like, not because the app should re-mask
the platform's icons.

---

## R6 — Saying "10s" to the eye and "10 second wait" to a screen reader

**Decision**: the badge draws the compact form and carries a `contentDescription` holding the
existing `delay_wait` plural. The row's existing merge (a `combinedClickable` node merges its
descendants) then produces one stop reading name, treatment, and the delay in full words.

**Rationale**: FR-018. "10s" is right for the eye and wrong for the ear — a screen reader may read
it as "ten s" — and the app already owns the words it should say instead, in the plural
`delay_wait` reuses across the preview card. Overriding the description on the one node that needs
it is smaller and less brittle than composing a whole-row description by hand, which would have to
be kept in step with three separate texts.

**The caption is the same idea with the opposite mechanism, and this entry was corrected during
implementation.** The first version of R6 said the screen should uppercase the count at draw time
and keep one sentence-case resource. That is forbidden by feature 004's **contract C8**, which is
written on `delay_config_seconds_caption` in `strings.xml`: `uppercase()` on user-visible text is a
locale trap — Turkish dotted and dotless i is the standing example — and it takes the
capitalisation decision away from the translator, who is the only person who knows whether their
script has case at all. C8 is an existing project contract and it wins.

So the caption ships as **two plurals**: `locks_count_caption` (capitalised) is drawn, and
`locks_count` (unchanged, sentence-case) is what the caption's `contentDescription` hands to a
screen reader. Translators choose whether to capitalise; TalkBack hears "3 locks" and is never
given capitals to spell out.

**Alternatives considered**: `uppercase()` at draw time (rejected by C8, as above — this was the
original decision here and it was wrong); a hand-built row description (rejected as above).

---

## R7 — Strings

**Decision**: three changes in `res/values/strings.xml`.

- `locks_title`: "Your locks" → **"Locks"** (FR-007, matching the artboard).
- **New** `locks_delay_badge`: `%1$ds` — the badge's compact form, translatable because the unit
  abbreviation is not universal.
- **Removed** `locks_row_detail` (`%1$s · %2$s`). The row's second line is now the treatment alone,
  so the joiner has no caller.

**Rationale and the contract it touches**: feature 005's FR-010 and
`005-locks-and-first-run/contracts/locks-screen.md` describe a second line carrying the delay *and*
the treatment. This feature supersedes that half of it — the delay moves to the badge, the
information is all still on the row, and nothing about what the row *claims* changes. That
supersession is recorded here and in this feature's contract rather than left as a silent
disagreement between two specs. Feature 005's wording obligations that are not about layout — the
count line saying only the count, the removal explanation's text — are untouched and remain
binding.

---

## R8 — What this feature does not need

- **No new dependency.** `SystemBarStyle` has shipped in `androidx.activity` since 1.8.0, which is
  the version already pinned in `gradle/libs.versions.toml`.
- **No new colour.** Every pairing the redesigned screen puts on screen — `Ink on Bone`,
  `Ink40 on Bone`, `Ink on Card`, `Ink40 on Card`, `AmberDark on AmberWash` — is already declared
  in `TextPairings` and already passes `SlowLockPaletteTest`.
- **No new unit test is mandated.** The constitution's three automated-coverage obligations are
  schedule logic, target resolution, and frozen persisted values; this feature has none of the
  three, and is presentation without branching. The existing `SlowLockPaletteTest` continues to
  enforce the palette and the no-inline-literal rule against the new code for free.
- **No instrumented test.** Forbidden outright by the constitution. Everything observable only on a
  running device goes to the manual test plan.
