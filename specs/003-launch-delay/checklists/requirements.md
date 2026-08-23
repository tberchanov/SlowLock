# Specification Quality Checklist: Launch Delay

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-08-23
**Feature**: [spec.md](../spec.md)

## Content Quality

- [X] No implementation details (languages, frameworks, APIs)
- [X] Focused on user value and business needs
- [X] Written for non-technical stakeholders
- [X] All mandatory sections completed

## Requirement Completeness

- [X] No [NEEDS CLARIFICATION] markers remain
- [X] Requirements are testable and unambiguous
- [X] Success criteria are measurable
- [X] Success criteria are technology-agnostic (no implementation details)
- [X] All acceptance scenarios are defined
- [X] Edge cases are identified
- [X] Scope is clearly bounded
- [X] Dependencies and assumptions identified

## Feature Readiness

- [X] All functional requirements have clear acceptance criteria
- [X] User scenarios cover primary flows
- [X] Feature meets measurable outcomes defined in Success Criteria
- [X] No implementation details leak into specification

## Notes

- **16/16 pass.** First pass was 15/16 with three `[NEEDS CLARIFICATION]` markers; all three were
  put to the user and answered in the same session, and the spec was rewritten around the
  answers. Recorded under `## Clarifications`.
- **The three answers, and what each moved**:
  - *Delay control* → **a slider with a numeric readout** (FR-005, FR-007). The readout is a
    requirement in its own right: a slider without one lets the user choose a value they cannot
    name.
  - *What counts as waiting* → **strictly foreground-only** (FR-029). This became the single
    widest requirement in the spec — back, home, the app switcher, and the screen turning off are
    now one rule with one outcome, which is what makes it testable. FR-027 had to be tightened in
    the same pass to say that rotation is *not* the screen ceasing to be visible, or the two
    requirements would have contradicted each other.
  - *Unconfigured shortcuts* → **wait the default delay** (FR-032). This one reaches outside the
    feature: every shortcut pinned by feature 002 starts pausing when this ships, for a duration
    its owner never chose. Recorded in Accepted limitations rather than smoothed over.
- **Three items were re-checked after the rewrite rather than assumed to still hold**:
  *testable and unambiguous* (FR-027 vs FR-029, above), *edge cases* (screen-off, data loss, and
  the unconfigured shortcut each moved from open question to stated outcome), and *measurable
  success criteria* (SC-005 gained the four separate ways of leaving; SC-011 was added for the
  default-delay path).
- **`## Accepted limitations` is carried over from feature 002's spec**, outside the template's
  structure, for the same reason: a tester who does not know that the wait screen's dullness,
  the silence after applying, and the impossibility of pausing a wait are all deliberate will
  file three bugs.
- **Numbers the clarification did not settle are flagged as provisional**: the slider's range,
  step, and default are stated in Assumptions as starting values, cheap to change before release,
  with the one constraint that the default is a single value shared with FR-032. The range and
  step were duly revised during implementation — from 5–120 s in 5 s steps to **1–30 s in 1 s
  steps** — and the 10 s default was left alone. The flag did its job: nothing outside
  `DelayRange` had to move.
- **Two constitutional notes were recorded in Assumptions rather than resolved here**: the
  constitution named a *countdown* `DelayActivity`, which this feature deliberately does not
  build, and feature 002's instrumented-test waiver was said to expire with this feature. Both
  went to the Constitution Check gate in `/speckit-plan`, which ruled on them — and
  `/speckit-analyze` then judged those rulings to be reinterpreting binding text. **Settled by
  Constitution v1.1.0 (2026-08-23)**: "countdown `DelayActivity`" is now "delay screen", the
  instrumented requirement is gone entirely, and 002's waiver is moot.
- **Re-validated after the v1.1.0 amendment.** Five requirements changed and all 16 items still
  pass: FR-022 gained a measurable threshold (200 ms / 500 ms) where it previously said only
  "promptly"; FR-035 gained the bounded display-lock carve-out, resolving a spec/plan
  contradiction `/speckit-analyze` found; SC-002 was reworded to compare the first *settled*
  frame, since as written it was unachievable; SC-010 was scoped to exclude a wait's own screen
  time; and two Assumptions were added (the display stays lit for the wait, the screen follows
  light/dark). Before: 16/16. After: 16/16. The "measurable success criteria" and "testable and
  unambiguous" items were the ones actually re-checked rather than assumed.
- **This feature amends features 001 and 002 rather than only extending them.** Feature 002's
  FR-001, FR-006, and FR-016 are each replaced or narrowed, and its manual test plan asserts the
  old behaviour. The plan needs tasks for those edits.
