<!--
SYNC IMPACT REPORT
==================
Version change: [TEMPLATE] → 1.0.0 (initial ratification)
Rationale: First concrete constitution. No prior version existed; the file was an
unfilled template. MAJOR bump to 1.0.0 establishes the baseline.

Principles defined (all five slots filled):
  - (new) I. Cooperative User, Not Adversary
  - (new) II. Simplicity First (YAGNI)
  - (new) III. Permission & Policy Minimalism
  - (new) IV. Platform-Idiomatic Android
  - (new) V. Stable Identifiers

Sections added:
  - Additional Constraints & Technology Standards (replaces [SECTION_2_NAME])
  - Development Workflow & Quality Gates (replaces [SECTION_3_NAME])
  - Governance

Templates checked for consistency:
  ✅ .specify/templates/plan-template.md — "Constitution Check" gate derives gates from
     this file at plan time ("[Gates determined based on constitution file]"); no edit
     required.
  ✅ .specify/templates/spec-template.md — no constitution references; scope/requirements
     structure compatible as-is.
  ✅ .specify/templates/tasks-template.md — no constitution references; task
     categorization compatible as-is.
  ✅ .specify/templates/checklist-template.md — no constitution references.
  ✅ CLAUDE.md — Spec Kit stub, points at the current plan; no principle references to
     update.
  N/A .specify/templates/commands/ — directory does not exist in this installation
     (commands ship as skills under .claude/skills/).

Deferred / follow-up TODOs:
  - TODO(PRODUCT_NAME): highlevel_spec.md names the app "Slowdown" / "Launch Delay";
    the Gradle project, applicationId and namespace are "SlowLock" / com.slowlock.
    Pick one user-facing name and reconcile before the first release spec.
  - TODO(SUCCESS_METRIC): highlevel_spec.md §6 leaves the success metric open. No
    principle enshrines a measurement stance until that is decided.
  - TODO(VCS): repository is not under version control (initialized with --no-git).
    Governance compliance review below is written to work without git; revisit the
    review mechanics if a repo is created.
-->

# SlowLock Constitution

## Core Principles

### I. Cooperative User, Not Adversary

SlowLock inserts friction between impulse and action; it MUST NOT attempt enforcement.
The user is a willing participant nudging their own behaviour, not a subject to be
policed. Consequences that are binding on every feature:

- Any user-visible delay MUST be escapable (home, back, or an explicit skip).
- Features MUST NOT be justified by "the user could bypass this" — bypassability is the
  design, not a defect.
- The bypass paths catalogued in `highlevel_spec.md` §5 (original icon, recents, deep
  links, launcher search, shortcut removal) are ACCEPTED LIMITATIONS. A spec MUST NOT
  treat them as bugs, and MUST NOT propose closing them without new information that
  overturns the §4 trade-off table.

**Rationale:** The product thesis is interrupting an unconscious reach. Enforcement
mechanisms cost dangerous permissions, Play Store policy risk, and battery — buying
coverage the thesis does not require.

### II. Simplicity First (YAGNI)

The smallest thing that delivers the behaviour wins. Binding rules:

- New third-party dependencies MUST be justified in the plan's Complexity Tracking
  section, naming what breaks without them. The default answer is no.
- New Gradle modules, abstraction layers, DI frameworks, and persistence engines are
  each a justified deviation, not a default. The project is one `:app` module until a
  concrete need proves otherwise.
- Features not required by an accepted spec MUST NOT be built ahead of need.
- v2 ideas (`UsageStatsManager` polling, hybrid detection) MUST stay out of v1 specs.

**Rationale:** A solo-maintained v1 whose entire advantage is "days, not weeks to ship"
is destroyed by speculative architecture.

### III. Permission & Policy Minimalism

The app ships with the fewest permissions that make the feature work. Binding rules:

- `QUERY_ALL_PACKAGES` MUST NOT be requested. App enumeration MUST use a manifest
  `<queries>` declaration for `ACTION_MAIN` + `CATEGORY_LAUNCHER`.
- `AccessibilityService` MUST NOT be used for non-accessibility purposes.
- `PACKAGE_USAGE_STATS` MUST NOT be requested in v1.
- Any new permission MUST be listed explicitly in the plan with its Play Store policy
  standing, and MUST be approved before implementation begins.
- The app MUST remain functional (degrading gracefully) when the user declines an
  optional system dialog, including the pin-shortcut dialog.

**Rationale:** Restricted permissions require written justification at Play review and
are a leading rejection cause. Every permission avoided is onboarding friction and
review risk avoided.

### IV. Platform-Idiomatic Android

Work with the platform, never around it. Binding rules:

- Kotlin + Jetpack Compose + Material 3 for all UI. No XML layouts for new screens.
- System APIs that can fail MUST be handled at the call site, not assumed:
  `getLaunchIntentForPackage()` can return null; `isRequestPinShortcutSupported()` MUST
  gate every pin attempt.
- Package enumeration, icon rasterization, and disk I/O MUST run off the main thread.
- Activity launches MUST originate from a user-initiated foreground context. Background
  activity starts MUST NOT be attempted.
- No persistent foreground services, no polling loops, no wake locks in v1. Battery cost
  at rest MUST be zero.
- OEM battery-management behaviour MUST NOT be fought with hacks or whitelisting
  prompts.

**Rationale:** Every rejected alternative in `highlevel_spec.md` §4 failed on platform
friction — killed services, OEM interference, policy exposure. Staying idiomatic is what
makes the shortcut approach cheap and reliable.

### V. Stable Identifiers

Identity is matched on what the platform guarantees stable. Binding rules:

- The package name is the ONLY persisted identifier for a target app.
- Launcher activity names and `ComponentName`s MUST NOT be persisted or matched against;
  they are renamed across app updates and fail silently.
- Localized labels are display-only and MUST NOT be used as keys, in matching, or in
  comparisons.
- Cached icons MUST be keyed by `packageName` + `versionCode` so an app update
  invalidates the entry.

**Rationale:** These failures are silent — the app appears to work while pointing at a
target that no longer exists. Silent breakage is the most expensive kind to diagnose.

## Additional Constraints & Technology Standards

**Fixed stack.** Kotlin, Jetpack Compose (BOM-managed), Material 3, AGP, single `:app`
module, Java/JVM target 11, `minSdk 33`, `targetSdk`/`compileSdk` 37, namespace and
applicationId `com.slowlock`. Dependency versions MUST be declared in
`gradle/libs.versions.toml`; hardcoded coordinates in `build.gradle.kts` are prohibited.

**No backend.** v1 has no server, no account, and no network requirement. Introducing
network access, analytics, or a third-party SDK is a constitutional amendment, not a
plan-level decision.

**Scope boundary.** v1 covers exactly: app enumeration and picking, per-app schedule and
delay configuration, pinned shortcut creation with mirrored icon and label, and a
countdown `DelayActivity` that launches the target. Anything beyond this requires an
approved spec.

**Non-goals.** Parental controls, usage enforcement, usage statistics, and cross-device
sync are out of scope and MUST be rejected at spec review.

## Development Workflow & Quality Gates

**Spec-driven flow is mandatory.** Feature work follows
`/speckit-specify` → `/speckit-clarify` (when ambiguity exists) → `/speckit-plan` →
`/speckit-tasks` → `/speckit-implement`. Implementation MUST NOT begin before an
approved plan exists.

**Constitution Check gate.** Every plan MUST evaluate all five principles before Phase 0
research and re-check after Phase 1 design. Any violation MUST appear in Complexity
Tracking with the simpler alternative named and the reason it was rejected. An
unjustified violation blocks implementation.

**Build gate.** `./gradlew assembleDebug` and `./gradlew test` MUST pass before a feature
is considered complete. Work MUST NOT be reported as done on an unverified build.

**Testing expectations.** Test-first is RECOMMENDED but not mandated. Regardless of
order, the following MUST have automated coverage before a feature is complete:

- Schedule/time-window evaluation logic — unit tests, including boundary times, weekday
  handling, and the outside-window immediate-launch path.
- Target resolution and the null `getLaunchIntentForPackage()` path — unit tests.
- `DelayActivity` countdown and hand-off to the target app — instrumented test.

Pure-Compose presentation without branching logic MAY ship without tests.

**Manual verification.** Before any release, shortcut pinning MUST be verified on at
least one non-Pixel OEM device, and behaviour under Xiaomi Dual Apps MUST be recorded as
tested or explicitly untested.

## Governance

This constitution supersedes ad-hoc practice. Where guidance conflicts, the order is:
this constitution → `highlevel_spec.md` → the active plan → individual preference.

**Amendments.** Any change to a principle MUST be made in this file, with a Sync Impact
Report recorded in the HTML comment at the top, and MUST state its effect on existing
specs. Amendments take effect immediately on write; work already in flight under the
prior version MUST be re-checked against the new text before completion.

**Versioning.** Semantic versioning applies to this document:

- MAJOR — a principle is removed or redefined in a backward-incompatible way.
- MINOR — a principle or section is added, or guidance is materially expanded.
- PATCH — clarification, wording, or typo fixes with no change in obligation.

**Compliance review.** Each `/speckit-plan` run is a compliance checkpoint via the
Constitution Check gate. Each `/speckit-analyze` run MUST report principle violations
found across spec, plan, and tasks. Complexity that survives review MUST be documented,
never silently accepted.

**Runtime guidance.** `CLAUDE.md` at the repository root carries agent-facing runtime
guidance and points to the active plan. It is subordinate to this file and MUST NOT
restate principles — only reference them.

**Version**: 1.0.0 | **Ratified**: 2026-08-15 | **Last Amended**: 2026-08-15
