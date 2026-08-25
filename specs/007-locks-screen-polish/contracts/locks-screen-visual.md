# Contract L — The Locks screen's appearance

**Feature**: `specs/007-locks-screen-polish` | **Owner**: `locks/LocksScreen.kt`

This contract governs appearance only. Every behavioural rule feature 005 wrote in
`005-locks-and-first-run/contracts/locks-screen.md` — K2 through K6 — remains in force, and this
file amends exactly one of them, named in L9.

---

**L1 — The heading is this screen's own, not `ScreenHeader`.** The Locks screen renders a title and
a caption itself. `ScreenHeader` is not modified, not parameterised, and not extended: it belongs to
the three flow screens, which keep it unchanged. A "make `ScreenHeader` support a large variant"
refactor is out of scope and would put this screen's design decisions into a component with three
other callers.

**L2 — The title is `TitleDisplay`, the caption is `Count`.** Values in `data-model.md` §2 and §4.
No screen-local `TextStyle`.

**L3 — The caption states the count and nothing else.** Carried forward verbatim from 005 FR-011:
the artboard reads "3 ON YOUR HOME SCREEN" and the second half is not shipped, because Android
cannot tell the app whether those icons are still there. This feature adopts the line's *styling*
only. Restoring the claim requires overturning Constitution I, not a design review.

**L4 — The capitals live in the resource, never in a transform.** Two plurals: `locks_count_caption`
is drawn, `locks_count` is spoken via the caption's `contentDescription`. `uppercase()` at display
time is forbidden by feature 004's contract C8 — it is a locale trap (Turkish dotted and dotless i)
and it takes the capitalisation decision away from the translator, who is the only person who knows
whether their script has case. The duplication is the price of both, and it is the price this
project already pays for `SECONDS` and `ICON`.

*Amended during implementation.* This contract originally said the opposite — uppercase at draw
time, one resource. C8 already settled the question for this codebase and was found while editing
`strings.xml`; the earlier wording is superseded, and research R6 is corrected to match.

**L5 — The heading block holds no controls.** No back tile, no step counter, no menu, no search.
The Locks screen is the app's root and there is nowhere to go back to.

**L6 — The row's trailing element is the delay badge.** Trailing, so it moves to the leading edge
under RTL. It is `flex: none` in the artboard's terms: the app name yields to it, never the reverse.

**L7 — The badge speaks in words.** It draws the compact form and carries the existing `delay_wait`
plural as its description. The compact form must never be the only thing a screen reader can say.

**L8 — Every colour, type role and radius comes from the theme.** No literal in a screen. The
existing `SlowLockPaletteTest` source scan enforces the colour half of this and fails the build,
not the review.

**L9 — Amendment to 005 contract K, row content.** Feature 005's row put the delay and the
treatment on one joined second line. This feature moves the delay to the badge and leaves the
treatment alone on line 2; the joiner resource `locks_row_detail` is removed with its last caller.
The row still carries app name, delay and treatment, so nothing 005 promised the *user* is
withdrawn — only the layout that carried it. All other row rules from K are unchanged, including:
tap opens the lock for editing, long press opens the removal explanation, the removal explanation is
also a custom accessibility action on every row, and an unavailable row has no click modifier at
all.

**L10 — The unavailable row is untouched.** No badge, no restyle. The artboards do not draw it, and
inventing an artboard is not this feature's job (FR-020).

**L11 — Rows grow, never clip.** `heightIn(min = …)` rather than a fixed height, so the largest
system font scale produces a taller row rather than cut text. Unchanged from 005 and restated
because the row's contents changed.

**L12 — Nothing outside this screen changes.** The app list, the delay screen, the icon screen, the
first-run screen and the unsupported-launcher screen must be pixel-identical after this feature,
apart from their system bars (contract S). A diff that touches `ScreenHeader.kt`,
`SelectableTile.kt`, `Actions.kt`, or any existing value in `Type.kt` or `Shape.kt` has broken this
rule.
