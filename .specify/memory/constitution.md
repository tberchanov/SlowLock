<!--
SYNC IMPACT REPORT
==================
Version change: 1.1.0 → 1.2.0 (amendment, 2026-08-25)
Rationale: MINOR. A new binding rule is added to an existing workflow section; no principle
is removed or redefined, and nothing that complied with 1.1.0 becomes non-compliant in its
own terms. It materially expands guidance rather than clarifying wording, so not PATCH.
Requested by the maintainer after an agent committed and pushed six times during
/speckit-implement of 006-site-publishing on the strength of a bare "proceed".

Modified principles: none.

Modified sections:
  - Development Workflow & Quality Gates — ADDED "Version control is the maintainer's, not
    the agent's": no commit, push, merge, rebase, tag, or branch create/switch/delete unless
    the maintainer asks for that action in that message. A general go-ahead ("proceed",
    "continue", plan approval) is explicitly not authorization; permission does not persist
    across actions; a commit/push step written into tasks.md is a note to the maintainer, not
    an authorization. Read-only git and working-tree edits stay unrestricted, and leaving
    changes uncommitted is named as the expected end state.

Added sections: none (the rule lives inside an existing section).
Removed sections: none.

Deferred TODO resolved:
  - TODO(VCS) from 1.0.0 is CLOSED. The repository is now under git with a GitHub remote
    (origin: tberchanov/SlowLock), which is what makes this rule necessary and enforceable.
    The two remaining 1.0.0 TODOs — PRODUCT_NAME and SUCCESS_METRIC — are unchanged and
    still open.

Effect on existing specs (required by Governance):
  - 001-installed-apps-list, 002-shortcut-pinning, 003-launch-delay — unaffected; no
    version-control tasks.
  - 006-site-publishing — its T035 ("Commit the site, the workflow and the licence to `main`
    and push") is exactly the kind of task this rule reclassifies: it is a note to the
    maintainer, not an authorization. It was executed under 1.1.0, before this rule existed,
    and is left marked complete as an accurate record of what happened. Under 1.2.0 an agent
    reaching that task MUST stop and ask. Amendments bind work in flight, so the feature's
    one open task (T038) and any follow-up MUST proceed under the new rule.

Templates checked for consistency:
  ⚠→✅ .specify/templates/tasks-template.md — its Notes bullet "Commit after each task or
     logical group" contradicted the new rule and was rewritten to say the maintainer commits
     and the agent asks first.
  ✅ .specify/templates/plan-template.md — derives gates from this file at plan time; the
     "Branch" header field is a label, not an instruction to create one. No edit.
  ✅ .specify/templates/spec-template.md — "Feature Branch" header field is likewise a label.
     No edit.
  ✅ .specify/templates/checklist-template.md — no version-control references.
  ✅ .specify/templates/constitution-template.md — unfilled source template; no edit.
  ✅ CLAUDE.md — points at the active plan; no principle references to update.
  N/A .specify/templates/commands/ — directory does not exist in this installation
     (commands ship as skills under .claude/skills/).

Follow-up TODOs: PRODUCT_NAME and SUCCESS_METRIC from 1.0.0 remain open.

---

SYNC IMPACT REPORT
==================
Version change: 1.0.0 → 1.1.0 (amendment, 2026-08-23)
Rationale: MINOR. A principle's binding rule gains a bounded carve-out and a workflow
section's obligation is relaxed and replaced. Nothing that complied with 1.0.0 becomes
non-compliant, and no principle is removed or redefined against its purpose, so this is
not MAJOR; the changes alter obligations rather than wording, so it is not PATCH.
Requested by the maintainer during /speckit-analyze of feature 003-launch-delay.

Modified principles:
  - IV. Platform-Idiomatic Android (title unchanged) — "no wake locks in v1" narrowed to
    "no `PowerManager` wake locks", with an explicit, bounded allowance for
    FLAG_KEEP_SCREEN_ON: window-scoped, only on a screen the user is looking at, only
    where the feature does not work without it, released with the window. "Battery cost
    at rest MUST be zero" is unchanged, and PowerManager wake locks remain forbidden.

Modified sections:
  - Additional Constraints & Technology Standards → Scope boundary: "countdown
    `DelayActivity`" → "delay screen", plus a sentence putting the screen's presentation
    (countdown, progress, or deliberately static) in the feature's hands, not this file's.
  - Development Workflow & Quality Gates → Testing expectations: the mandated instrumented
    test for the delay hand-off is REMOVED. Automated coverage now means JVM unit tests
    only. A new binding rule forbids instrumented suites (src/androidTest,
    connectedAndroidTest, Espresso, UI Automator) outright, and forbids an agent driving
    the connected device to pre-verify a manual case. A third unit-test obligation is
    added: every frozen persisted value MUST be asserted against a literal.
  - Development Workflow & Quality Gates → Manual verification: every feature MUST now
    ship a written, numbered, requirement-traceable manual test plan. The existing
    non-Pixel OEM and Xiaomi Dual Apps release gate is unchanged.

Added sections: none. Removed sections: none.

Effect on existing specs (required by Governance):
  - 001-installed-apps-list — unaffected.
  - 002-shortcut-pinning — its recorded instrumented-test waiver (plan.md, "Testing-
     expectations check") is now MOOT: the clause it waived no longer exists. The waiver's
     closing promise that the requirement "returns in full" with the delay feature is
     superseded and MUST NOT be honoured. No other change; the feature is complete and
     its frozen contract is untouched.
  - 003-launch-delay — three consequences, all pending: FLAG_KEEP_SCREEN_ON stops being a
     deviation (plan.md Complexity Tracking row 2 to be removed, Principle IV row to be
     re-marked PASS); the instrumented suite, its stub activity, and the
     connectedDebugAndroidTest gate to be dropped from plan.md, tasks.md, research.md R12,
     contracts/wait-screen.md, quickstart.md and data-model.md, with the coverage they
     carried moved into manual-test-plan.md; the in-plan "countdown" ruling is superseded
     by this amendment and to be replaced with a pointer here. Its spec's FR-035 must also
     be amended to match the new Principle IV wording.

Templates checked for consistency:
  ✅ .specify/templates/plan-template.md — derives gates from this file at plan time; the
     new rules need no template edit.
  ✅ .specify/templates/spec-template.md — no constitution references.
  ✅ .specify/templates/tasks-template.md — no constitution references. Note its sample
     tasks mention contract/integration tests generically; no device-driving implication.
  ✅ .specify/templates/checklist-template.md — no constitution references.
  ✅ CLAUDE.md — points at the active plan; no principle references to update.

Follow-up TODOs: the three deferred items from 1.0.0 (below) are unchanged and still open.

---

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
- No persistent foreground services, no polling loops, and no `PowerManager` wake locks in
  v1. Battery cost at rest MUST be zero.
- **Keeping the display awake is permitted only as a window flag** (`FLAG_KEEP_SCREEN_ON`),
  only on a screen the user is actively looking at, and only where the feature does not
  work without it. It MUST be released with that window, MUST NOT outlive it, and MUST NOT
  be reached for as a convenience. A `PowerManager` wake lock is never an acceptable
  substitute: it needs a permission (Principle III), it is deprecated, and it can survive
  the screen that took it.
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
delay configuration, pinned shortcut creation with mirrored icon and label, and a delay
screen that launches the target. Anything beyond this requires an approved spec. How that
screen presents — countdown, progress, or deliberately static — is a product decision for
the feature that builds it, not a constitutional one.

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

**Testing expectations.** Test-first is RECOMMENDED but not mandated. Automated coverage
means **JVM unit tests only** (`./gradlew test`). Regardless of order, the following MUST
have automated coverage before a feature is complete:

- Schedule/time-window evaluation logic — unit tests, including boundary times, weekday
  handling, and the outside-window immediate-launch path.
- Target resolution and the null `getLaunchIntentForPackage()` path — unit tests.
- Any frozen persisted value — a unit test asserting it against a literal, so a rename
  fails the build instead of a user's device.

Pure-Compose presentation without branching logic MAY ship without tests.

**No automated test may drive a device.** Instrumented suites (`src/androidTest`,
`connectedAndroidTest`, Espresso, UI Automator) MUST NOT be added to this project. Where
a behaviour can only be observed on a running app — a delay screen's timing, the hand-off
to a target app, what a launcher does with a pin request — it MUST be verified **manually
by the maintainer** against the feature's written manual test plan.

**Rationale:** what these behaviours depend on is real launchers, real OEM builds, and real
timing on a real phone; a scripted approximation asserts the harness rather than the
product, and it spends the maintainer's device session without their say-so. An agent MUST
NOT drive the connected device to pre-verify a manual case. It states which cases need
running, and waits.

**Version control is the maintainer's, not the agent's.** An agent MUST NOT run
`git commit`, `git push`, `git merge`, `git rebase`, `git tag`, or create, switch or delete
branches unless the maintainer has asked for that specific action in that specific message.
Binding clarifications:

- A general go-ahead is NOT a version-control instruction. "Proceed", "continue", "go ahead",
  "sounds good", approving a plan, or answering an unrelated question MUST NOT be read as
  permission to commit or push.
- Permission does not persist. Being asked to commit once authorizes that commit, not the next
  one. Each commit, push, or branch creation needs its own ask.
- A task in `tasks.md` that says to commit or push is a note to the maintainer, not an
  authorization. The agent MUST leave it unchecked and say the work is staged and ready.
- The agent MAY freely run read-only git commands (`status`, `diff`, `log`, `show`) and MAY
  write files in the working tree. Leaving changes uncommitted is the expected end state.
- When work is complete, the agent MUST report what changed and stop, offering the commit
  rather than performing it.

**Rationale:** a commit is the maintainer's signature and a push is publication — both are
outward-facing, and a push to `main` is effectively irreversible on a public repository. An
agent that commits on a general go-ahead takes an authorship decision that was never
delegated, and buries the maintainer's chance to review the diff first. Reading "proceed" as
"publish" is the specific failure this rule exists to prevent.

**Manual verification.** Every feature MUST ship with a written manual test plan whose
cases are numbered and traceable to requirements. Before any release, shortcut pinning
MUST be verified on at least one non-Pixel OEM device, and behaviour under Xiaomi Dual
Apps MUST be recorded as tested or explicitly untested.

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

**Version**: 1.2.0 | **Ratified**: 2026-08-15 | **Last Amended**: 2026-08-25
