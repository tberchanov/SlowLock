# Specification Quality Checklist: Use Cases Hold the Logic

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-08-27
**Feature**: [spec.md](../spec.md)

## Content Quality

- [~] No implementation details (languages, frameworks, APIs) — **documented deviation**, see Notes
- [x] Focused on user value and business needs
- [~] Written for non-technical stakeholders — **documented deviation**, see Notes
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [~] Success criteria are technology-agnostic — **documented deviation**, see Notes
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [~] No implementation details leak into specification — **documented deviation**, see Notes

## Notes

**The three deviations are one deviation.** This feature's subject *is* internal structure: it is
the code-side half of constitution amendment v5.0.0, and it changes nothing a user or a
non-technical stakeholder can observe. A version of this spec that named no code site would have no
testable requirement in it — "logic should live in use cases" is not a requirement, "the ordering of
the installed-app list MUST be performed outside the repository that enumerates them" is. The
audience is the maintainer and the next reader of the codebase, and they are named as the user in
User Scenarios rather than the deviation being left implicit.

The line the spec does hold: it names **where a rule currently is** and **that it must move**, and
declines to name what it moves *into*. No use case is named, no signature is given, no package is
specified beyond what the constitution already fixes, and the one genuine design trade-off — the
`ShortcutPinner` icon boundary — is stated as a trade-off and deferred to the plan rather than
resolved here.

**Zero clarification markers.** Four decisions that would otherwise have been markers were settled
with the maintainer before the spec was written: the vehicle (amend the constitution first, then
spec the migration), the form of a use case (injectable class with `operator fun invoke`), whether
decoding stays with the repository (it does), and whether presentation-side filtering is in scope
(it is). All four are recorded in Assumptions.

**Status: ready for `/speckit-plan`.** `/speckit-clarify` has nothing to ask.
