# Specification Quality Checklist: Visual Redesign (Phase 1)

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-08-24 · **Re-validated**: 2026-08-24 after `/speckit-clarify`
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

**16/16 pass.** Three items were argued rather than waved through, and the reasoning is recorded
here because a later reader will otherwise re-open them.

**On "no implementation details".** The spec names hex values, control heights, corner radii and
two typefaces by name. For a visual redesign these are the *requirement*, not a leaked choice of
how to build it — FR-002's eleven colours are as much the subject of this feature as a retention
period would be for a data feature. What the spec deliberately does not name is any framework,
language, component library, file, class, or resource mechanism. The one place this was tightened
during review: FR-033 originally said the wait screen must render "without depending on the
app-wide theme being in scope", which described a code arrangement; it now states the outcome —
the screen resolves its own colours and type so a change elsewhere cannot alter it by accident.
SC-009 was tightened the same way.

**On measurement units.** Dimensions are written in `dp`/`sp`. These are density-independent
units, not pixels, and the Assumptions section states that the artboards' 412×892 geometry is to
be adapted rather than reproduced positionally. A stakeholder reading "56dp tall" is being told
the button's physical size, which is a product decision.

**On scope bounding.** The Out of Scope section is unusually long and is the answer to the
maintainer's explicit instruction. It is binding in three directions: what Phase 2 owns (first
run, the Locks home, the back tile and the step counters, and anything that enumerates locks),
what Phase 3 owns (the dark palette for the four in-app screens), and what this feature must
never touch (persisted values, the frozen contracts, permissions, dependencies). Three negative
requirements back it up in the Requirements section — FR-038, FR-039 and the Phase 1 MUST NOTs
under Out of Scope — so the boundary is testable and not merely narrative.

**One requirement changes existing accepted scope** and is flagged rather than buried: FR-037
amends feature 003's "unbranded by construction" rule for the wait screen. The amendment is
itself part of this feature's deliverable, is named in Dependencies, and is authorised by the
constitution's scope boundary, which places a delay screen's presentation with the feature that
builds it. Every other 003 wait-screen obligation is carried forward verbatim (FR-029 through
FR-032, FR-040).

---

## Re-validation after `/speckit-clarify` (2026-08-24)

**16/16 → 16/16.** No item changed state. Five clarifications were integrated; three of them
strengthened items that were already passing, and one repaired a defect this checklist had missed.

**Repaired defect — "Requirements are testable and unambiguous".** The original FR-029 said
nothing on the wait screen may change "for the whole duration of the wait", while FR-030 and
SC-005 bound only the *background* to the first frame. Read together they contradicted: the accent
rule and the message appearing after the ground is a visible change. The item was marked passing
before, and should not have been. FR-029 now requires the screen to arrive complete in a single
frame and freezes from that frame onward; SC-005 carries both timings. The contradiction is gone
rather than documented.

**Strengthened.** FR-014a settles what yields on the delay screen at large font scales, which the
Edge Cases section had raised as a risk without resolving. FR-041/FR-042 make "lock" the canonical
user-facing noun and confine the rename to display text. FR-043/FR-044 and SC-011 cover assistive
technology, which the original spec touched only through the contrast floor.

**One new accepted limitation, stated rather than hidden.** FR-045 records that the delay presets
and treatment tiles ship below Android's 48dp touch-target floor. This was raised as a concern,
and the maintainer chose design fidelity over the floor. It is written into the requirements, the
Assumptions section, and the Clarifications log so that an accessibility review finds it declared
— but it remains a real shortfall, and the spec names the one-line fix if the trade is re-decided.

**On "no implementation details" — the item that came closest to regressing.** FR-003 now requires
each weight to ship as its own font file and forbids synthesised weights, and the Assumptions
section records that variable fonts were considered and rejected. That is closer to a packaging
decision than the rest of the spec allows itself. It is kept for two reasons: the maintainer chose
it explicitly, and it is what makes SC-007's 1.5MB budget a number rather than a guess. It names
no framework, language, library, or API, so the item stands — but it is the spec's thinnest margin
on that rule and should not be widened.
