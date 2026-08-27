<!--
SYNC IMPACT REPORT
==================
Version change: 2.2.0 → 3.0.0 (MAJOR — Principle III redefined backward-incompatibly)

Modified principles:
- III. Feature First, Layers Inside — package shape changed from
  `com.slowlock.<feature>.{ui, domain, data}` to
  `com.slowlock.feature.<feature>.{ui, domain, data}`. The "governs new code only"
  grandfather clause is REMOVED: packages predating the shape MUST now be migrated, so
  code that was compliant under 2.2.0 is non-compliant under 3.0.0. Two clauses added:
  shared homes (`core`, `ui`) are explicitly excluded from the `feature` namespace, and a
  frozen fully-qualified name outranks the shape.

Added sections:
- (none)

Removed sections:
- (none)

Templates requiring updates:
- ✅ .specify/templates/plan-template.md — Constitution Check gate is generic; no principle
  enumeration and no package-shape reference to change.
- ✅ .specify/templates/spec-template.md — no principle or package references.
- ✅ .specify/templates/tasks-template.md — no principle or package references.
- ✅ .specify/templates/checklist-template.md — no principle or package references.
- ✅ CLAUDE.md — points at the active plan only; restates no principles (per Governance).
- ✅ README.md — product-facing; no package-shape references.
- ✅ .specify/memory/constitution.md — principle count in the Constitution Check gate is
  unchanged at eight.

Follow-up TODOs:
- ✅ specs/009-constitution-alignment — the in-flight package migration had landed files at
  `com.slowlock.<feature>.{ui, domain, data}` under version 2.2.0. Per Governance
  ("work already in flight MUST be re-checked against the new text before completion"), it
  was re-pathed under `com.slowlock.feature.` in that feature's Phase 9 (T087–T096), and its
  design documents were updated to match. `ShortcutLaunchActivity` stayed at
  `com.slowlock.shortcut` under the frozen-name carve-out.
- ⚠ Nothing is committed. The amendment and the re-path both sit in the working tree for the
  maintainer to review (Principle VII).
-->

# SlowLock Constitution

## Core Principles

### I. Modern Stack, Current Tooling

SlowLock is built on the current Android toolchain and the current way of doing things, not
on what was idiomatic three years ago. Binding rules:

- Kotlin + Jetpack Compose + Material 3 for all UI. XML layouts MUST NOT be used for new
  screens. Views-era patterns (Fragments as UI containers, `findViewById`, data binding) MUST
  NOT be introduced.
- Every dependency version MUST be declared in `gradle/libs.versions.toml`. Hardcoded
  coordinates or versions in a `build.gradle.kts` are prohibited. Compose artifacts MUST come
  from the Compose BOM rather than pinned individually.
- Libraries MUST be on a currently maintained release line. A dependency that is deprecated,
  archived, or superseded by a first-party Jetpack replacement MUST NOT be adopted, and one
  that becomes so MUST be scheduled for replacement rather than pinned indefinitely.
- Prefer the first-party solution where one exists and fits: Jetpack over a third-party
  equivalent, `kotlinx` over an ad-hoc utility, a platform API over a wrapper.
- Adding a dependency is a plan-level decision that MUST be recorded, naming what breaks
  without it. Adding one that pulls in a runtime the project does not otherwise have (a
  network stack, a database engine, an analytics SDK) is an amendment, not a plan decision.

**Rationale:** the project is small enough that staying current is cheap and falling behind is
expensive. A stale dependency is a migration debt that compounds silently, and every
non-first-party library is a bet on someone else's maintenance.

### II. Layered Architecture: Clean, MVVM, Repository, DI

Application code MUST be organized into explicit layers with a one-directional dependency
rule. Binding rules:

- **Layers.** Presentation (Compose UI + ViewModel), domain (models, use cases, repository
  interfaces), and data (repository implementations, platform and persistence sources).
  Dependencies point inward only: presentation → domain ← data. The domain layer MUST NOT
  import `android.*` (Android-free by construction), and the UI layer MUST NOT reach past the
  domain into a data source.
- **MVVM.** Every non-trivial screen is driven by a `ViewModel` that exposes UI state as an
  observable stream and receives user intent as function calls. Composables MUST be stateless
  with respect to business logic: they render state and emit events, nothing more. Business
  rules MUST NOT live in a composable, an `Activity`, or a `remember` block.
- **Repository.** Every source of data outside the process — `PackageManager`, DataStore or
  other persistence, the filesystem, the clock — MUST be reached through a repository whose
  interface is declared in the domain layer and whose implementation lives in the data layer.
  Callers depend on the interface. Platform types MUST NOT leak across that boundary; a
  repository returns domain models.
- **Dependency injection.** Dependencies MUST be injected through constructors, never
  constructed inline, looked up from a static holder, or fetched from a global singleton.
  Where a DI framework is used it MUST be the project's single, declared one; hand-rolled
  service locators are prohibited.
- **Use cases** are warranted when logic is shared across ViewModels or is genuinely a domain
  rule. A ViewModel MAY call a repository directly when a use case would add nothing but a
  forwarding call — see Principle V (KISS).

**Rationale:** these boundaries are what make the code testable on the JVM (Principle VI),
replaceable when a platform API changes underneath it, and legible to someone — human or
agent — arriving at one file with no memory of the rest. The layer rule is the mechanism;
testability and replaceability are the point.

### III. Feature First, Layers Inside

Source is organized by feature under a single `feature` namespace, with Principle II's layers
as subpackages inside each feature. Principle II says what belongs in a layer; this principle
says where it lives. Binding rules:

- **Shape.** `com.slowlock.feature.<feature>.{ui, domain, data}`. A feature package is named
  for a user-facing capability (`locks`, `apps`, `delay`, `shortcut`), never for a layer, a
  pattern, or a grab bag — `utils`, `helpers`, `managers`, `models`, and `common` are
  prohibited as package names.
- **Layer subpackages are earned, not mandatory.** A feature holding files from more than one
  layer MUST separate them into `ui`, `domain`, and `data`. A feature small enough to hold one
  layer's worth of files MAY stay flat until it grows — see Principle V (KISS).
- **One file, one layer.** A file MUST NOT hold both pure logic and platform access. Where a
  helper exists in both forms, the pure form belongs in `domain` and the
  `Context`-or-framework-taking form in `data`, in separate files.
- **Features do not reach into each other.** A feature MUST NOT import another feature's `ui`
  or `data`. Where two features genuinely share a domain type, it moves to `core`; anything
  else crossing a feature boundary is a signal the boundary is wrong.
- **Shared homes are named, and sit outside `feature`.** `com.slowlock.core` holds
  cross-feature domain types and platform compatibility shims. `com.slowlock.ui.{components,
  theme}` holds the design system and is the one package every feature's `ui` layer MAY depend
  on. Neither belongs under `feature`: the prefix marks a package as a capability that can be
  read, shipped, or deleted whole, and these two are neither. Application entry points
  (`Activity` classes, the root composable) sit at the root package or in the feature they
  serve.
- **Tests mirror main exactly.** A test lives in the same package path as the code it covers,
  under `src/test`.
- **A frozen fully-qualified name outranks this shape.** Where a class's fully-qualified name
  is itself a frozen persisted value (Principle VI), the class MUST stay at that name and MUST
  NOT be moved to satisfy the shape above; the deviation MUST be commented at the class
  (Principle VIII). `com.slowlock.shortcut.ShortcutLaunchActivity` is such a class — its name
  is written into every pinned shortcut already sitting on a user's launcher, and renaming its
  package would break every one of them — so it stays outside `com.slowlock.feature.shortcut`.
- **This principle governs all code, not only new code.** A package that does not match this
  shape is a defect to be migrated, not a grandfathered state to be worked around. New code
  MUST NOT be added to a non-conforming package.

**Rationale:** feature-first keeps everything about one capability in one directory, so a
feature can be read, changed, or deleted whole, and so Principle II's dependency rule is
checkable by looking at a directory instead of grepping the tree. Layer-first packaging buys
the same separation while scattering each feature across three distant trees. The explicit
`feature` namespace is what makes the split legible at the root: one glance separates the
capabilities from the shared foundation, an import naming another feature is visible as a
`com.slowlock.feature.` line in a file that has no business holding one, and the boundary
survives contact with a reader — human or agent — who has never seen the rest of the tree.
It is also already module-shaped: when a feature earns its own Gradle module, the directory
is promoted rather than rearranged.

### IV. Structured Concurrency with Coroutines and Flow

All asynchronous and background work uses coroutines and Flow, under structured concurrency.
Binding rules:

- Coroutines MUST run in a scope with a real lifecycle: `viewModelScope`, a repository scope
  owned by its holder, or a scope tied to the caller. `GlobalScope` MUST NOT be used.
- **Suspend functions MUST be main-safe.** A suspend function MUST be callable from the main
  dispatcher without blocking it; the function itself moves to `Dispatchers.IO` or
  `Dispatchers.Default` via `withContext` as needed. Callers MUST NOT be required to know
  which dispatcher a function needs.
- Dispatchers MUST be injected (Principle II), not referenced as hardcoded globals inside the
  code that uses them, so tests can substitute a test dispatcher.
- **UI state is a `StateFlow`.** ViewModels expose an immutable, non-null-by-default
  `StateFlow<UiState>`; one-shot events use a channel or an equivalent consume-once mechanism,
  never a `StateFlow` with a sentinel. Compose collects with `collectAsStateWithLifecycle()`;
  a bare `collectAsState()` on a lifecycle-scoped stream is prohibited.
- Cold flows MUST be cold: no side effects on collection, no work started at construction.
  Operators MUST run on the intended dispatcher via `flowOn`, never by switching context
  inside the flow builder.
- **Cancellation MUST be respected.** Long-running loops check for activity, cleanup uses
  `NonCancellable` only when genuinely required, and `CancellationException` MUST NOT be
  swallowed by a broad `catch`. Flow errors are handled with `catch`, not a `try` around
  `collect`.
- Blocking calls (`Thread.sleep`, blocking I/O, `runBlocking`) MUST NOT appear in production
  code. `runBlocking` is permitted in tests only.

**Rationale:** the app's core behaviour is a timed hand-off between processes. Leaked work,
swallowed cancellation, or a blocked main thread show up as a frozen delay screen or a launch
that never happens — the exact failures the product cannot afford.

### V. SOLID, Separation of Concerns, Single Source of Truth, KISS

Design decisions are judged against four rules, in this order when they conflict. Binding
rules:

- **SOLID.** One reason to change per class; extend by adding a type, not by adding a branch
  to a `when` that keeps growing; a subtype MUST be usable wherever its supertype is; an
  interface MUST NOT force implementers to stub methods they do not need; depend on
  abstractions across layer boundaries, concretes within one.
- **Separation of concerns.** Each unit does one kind of thing. UI does not parse, parsing
  does not persist, persistence does not decide policy. A file that mixes layers MUST be
  split rather than sectioned with comments.
- **Single source of truth.** Each piece of state has exactly one owner, and everything else
  derives from it or observes it. Derived state MUST be computed, never stored in parallel and
  kept in sync by hand. A screen renders from one state object; two competing state holders for
  the same screen is a defect, not a style choice.
- **KISS.** The simplest design that satisfies the requirement wins. Features not required by
  an accepted spec MUST NOT be built ahead of need. An abstraction with exactly one
  implementation and no test seam or layer boundary justifying it MUST be inlined. Speculative
  generality — a parameter no caller passes, a hook nothing implements — is a defect.
- **When KISS and Principle II appear to conflict**, the layer boundaries of Principle II are
  structural and stay; KISS governs what fills them. Keeping the presentation/domain/data seam
  is never "over-engineering"; adding a use case that only forwards, a mapper between two
  identical shapes, or an interface with one implementation on the same side of a seam is.

**Rationale:** these four are what keep a codebase changeable at the fifth feature rather than
just the first. Single source of truth in particular prevents the whole class of bugs where the
screen and the store disagree and neither is obviously wrong.

### VI. Tests That Earn Their Keep

Tests exist to catch defects, not to produce a coverage number. Binding rules:

- **Coverage is not a goal and MUST NOT be cited as evidence of quality.** A test that would
  still pass if the logic it covers were replaced with a different-but-wrong implementation is
  worthless and MUST NOT be written.
- The following are prohibited: tests that assert a getter returns what the constructor was
  given; tests that mock the unit under test's own collaborators so thoroughly that they assert
  only the mock's script; tests over generated code, data-class `equals`, or framework
  behaviour; a test whose assertions restate the implementation line for line.
- Test **behaviour at a seam**, not implementation detail. A test SHOULD survive a refactor
  that preserves behaviour, and MUST fail when behaviour changes.
- Automated coverage in this project means **JVM unit tests only** (`./gradlew test`).
  Regardless of test-first or test-after, these MUST have automated coverage before a feature
  is complete:
  - Schedule and time-window evaluation logic, including boundary times, weekday handling, and
    the outside-window immediate-launch path.
  - Target resolution, including the null `getLaunchIntentForPackage()` path.
  - Every frozen persisted value — asserted against a literal, so a rename fails the build
    instead of a user's device.
- Pure-Compose presentation without branching logic MAY ship with no tests, and SHOULD.
- Test-first is RECOMMENDED, never mandated.

**Rationale:** a padded suite is worse than a thin one — it costs time to write, time to
maintain, and it buys false confidence that stops anyone from looking harder at the parts that
actually break.

### VII. Version Control Is the Maintainer's

An agent MUST NOT run `git commit`, `git push`, `git merge`, `git rebase`, `git tag`, or
create, switch or delete branches unless the maintainer has asked for that specific action in
that specific message. Binding clarifications:

- A general go-ahead is NOT a version-control instruction. "Proceed", "continue", "go ahead",
  "sounds good", approving a plan, or answering an unrelated question MUST NOT be read as
  permission to commit or push.
- Permission does not persist. Being asked to commit once authorizes that commit, not the
  next one. Each commit, push, or branch creation needs its own ask.
- A task in `tasks.md` that says to commit or push is a note to the maintainer, not an
  authorization. The agent MUST leave it unchecked and say the work is staged and ready.
- The agent MAY freely run read-only git commands (`status`, `diff`, `log`, `show`) and MAY
  write files in the working tree. Leaving changes uncommitted is the expected end state.
- When work is complete, the agent MUST report what changed and stop, offering the commit
  rather than performing it.

**Rationale:** a commit is the maintainer's signature and a push is publication — both are
outward-facing, and a push to `main` is effectively irreversible on a public repository. An
agent that commits on a general go-ahead takes an authorship decision that was never delegated,
and buries the maintainer's chance to review the diff first. Reading "proceed" as "publish" is
the specific failure this rule exists to prevent.

### VIII. Comments That Earn Their Place

A comment is code that cannot be executed or tested, so it MUST pay for itself. Binding rules:

- **A comment explains why, never what.** The code states what it does. A comment that
  paraphrases the line beneath it MUST NOT be written, and MUST be deleted when found.
- **A comment MUST be as short as the reason allows.** Where one sentence carries the reason,
  a paragraph is a defect. Narrative prose — which feature changed this, what an earlier
  version did, what a comment used to predict — MUST NOT be committed; git holds history.
- The following MUST be commented, because the code cannot say them:
  - Every frozen persisted value, naming what breaks on a rename (Principle VI).
  - A platform behaviour that reads as a bug — an API whose name means the opposite of what it
    does, a call that must not be pre-flighted, an off-by-one the framework requires.
  - A deliberate deviation from an approved design or spec, naming the requirement it serves.
  - An absence that is load-bearing: a guard deliberately not added, a callback deliberately
    not observed, a value deliberately not cached.
- The following MUST NOT be committed: commented-out code; banner comments sectioning a file
  that Principle V says to split; a comment restating a requirement identifier's text where the
  identifier alone would do; a KDoc that only expands the member's own name.
- **A comment is part of the change it describes.** A comment made false by an edit MUST be
  corrected or deleted in the same change. A stale comment is worse than none, because it is
  believed.
- Public domain and repository declarations SHOULD carry KDoc where the contract is not evident
  from the signature — what the caller may assume, what is returned on failure, which
  dispatcher rule applies. Where the signature already says it, the KDoc is noise.

**Rationale:** the reason behind a decision is the one thing a reader cannot recover from the
code, and it is exactly what this project's frozen values, accepted limitations and deliberate
absences depend on. Every other comment competes with it for attention and dilutes it, and
none of them are checked by the compiler or the suite — so volume is not thoroughness, it is
unverified text that ages badly.

## Additional Constraints & Technology Standards

**Fixed stack.** Kotlin, Jetpack Compose (BOM-managed), Material 3, AGP, single `:app` module,
`minSdk 26`, namespace and applicationId `com.slowlock`.
Module structure MAY grow when a layer boundary from Principle II justifies it;
package-level layering inside `:app` satisfies Principles II and III and is the expected
starting point.

**No backend.** There is no server, no account, and no network requirement. Introducing network
access, analytics, or a telemetry SDK is a constitutional amendment, not a plan-level decision.

**Scope boundary.** The app covers exactly: app enumeration and picking, per-app schedule and
delay configuration, pinned shortcut creation with mirrored icon and label, and a delay screen
that launches the target. Anything beyond this requires an approved spec. How that screen
presents — countdown, progress, or deliberately static — is a product decision for the feature
that builds it, not a constitutional one.

**Non-goals.** Parental controls, usage enforcement, usage statistics, and cross-device sync
are out of scope and MUST be rejected at spec review.

**Product invariants (from the v1 thesis).** SlowLock inserts friction between impulse and
action; it MUST NOT attempt enforcement. The user is a willing participant nudging their own
behaviour, not a subject to be policed.

- Any user-visible delay MUST be escapable (home, back, or an explicit skip).
- A feature MUST NOT be justified by "the user could bypass this" — bypassability is the
  design, not a defect.
- The following are ACCEPTED LIMITATIONS, weighed deliberately. A spec MUST NOT treat them as
  bugs, and MUST NOT propose closing them without new information:
  - The target app's original icon still exists in the drawer and still launches it directly.
  - Recents, deep links (a link tapped in a messenger, a notification, a share-sheet target),
    and launcher search (Pixel search bar, Samsung Finder) all bypass the launcher entirely
    and are therefore uncovered.
  - The delay is not enforceable — home or back leaves it — and removing a pinned shortcut is
    a long-press and a drag.
  - Some launchers, Pixel Launcher included, badge pinned shortcuts, so the icon will not look
    perfectly native. Each pin costs its own system dialog; there is no batch pinning.
  - Dual-app clones (Xiaomi Dual Apps, Samsung Secure Folder) run the same package under a
    different user ID; behaviour there is untested. Uninstalling SlowLock may leave dead
    shortcuts behind.
- Covering those paths requires an `AccessibilityService` or `UsageStatsManager` polling. Both
  were considered and rejected: they cost a restricted permission and Play policy exposure,
  they drain battery from a background service that OEM battery managers kill silently, and
  they turn a one-dialog onboarding into a permission-plus-whitelisting flow. That rejection
  is settled — see "Permission and policy minimalism" below, which bans both outright.

**Permission and policy minimalism.** The app ships with the fewest permissions that make the
feature work.

- `QUERY_ALL_PACKAGES` MUST NOT be requested. App enumeration MUST use a manifest `<queries>`
  declaration for `ACTION_MAIN` + `CATEGORY_LAUNCHER`.
- `AccessibilityService` MUST NOT be used for non-accessibility purposes.
- `PACKAGE_USAGE_STATS` MUST NOT be requested.
- Any new permission MUST be listed explicitly in the plan with its Play Store policy standing,
  and MUST be approved before implementation begins.
- The app MUST remain functional, degrading gracefully, when the user declines an optional
  system dialog — including the pin-shortcut dialog.

**Platform behaviour.** Work with the platform, never around it.

- System APIs that can fail MUST be handled at the call site, not assumed:
  `getLaunchIntentForPackage()` can return null; `isRequestPinShortcutSupported()` MUST gate
  every pin attempt.
- Package enumeration, icon rasterization, and disk I/O MUST run off the main thread, via
  Principle IV's main-safety rule.
- Activity launches MUST originate from a user-initiated foreground context. Background
  activity starts MUST NOT be attempted.
- No persistent foreground services, no polling loops, and no `PowerManager` wake locks.
  Battery cost at rest MUST be zero.
- Keeping the display awake is permitted only as a window flag (`FLAG_KEEP_SCREEN_ON`), only on
  a screen the user is actively looking at, and only where the feature does not work without
  it. It MUST be released with that window, MUST NOT outlive it, and MUST NOT be reached for as
  a convenience. A `PowerManager` wake lock is never an acceptable substitute: it needs a
  permission, it is deprecated, and it can survive the screen that took it.
- OEM battery-management behaviour MUST NOT be fought with hacks or whitelisting prompts.

## Development Workflow & Quality Gates

**Spec-driven flow is mandatory.** Feature work follows `/speckit-specify` →
`/speckit-clarify` (when ambiguity exists) → `/speckit-plan` → `/speckit-tasks` →
`/speckit-implement`. Implementation MUST NOT begin before an approved plan exists.

**Constitution Check gate.** Every plan MUST evaluate all eight principles before Phase 0
research and re-check after Phase 1 design. Any violation MUST appear in Complexity Tracking
with the simpler alternative named and the reason it was rejected. An unjustified violation
blocks implementation.

**Build gate.** `./gradlew assembleDebug` and `./gradlew test` MUST pass before a feature is
considered complete. Work MUST NOT be reported as done on an unverified build.

**No automated test may drive a device.** Instrumented suites (`src/androidTest`,
`connectedAndroidTest`, Espresso, UI Automator) MUST NOT be added to this project. Where a
behaviour can only be observed on a running app — a delay screen's timing, the hand-off to a
target app, what a launcher does with a pin request — it MUST be verified **manually by the
maintainer** against the feature's written manual test plan.

**Rationale:** what these behaviours depend on is real launchers, real OEM builds, and real
timing on a real phone; a scripted approximation asserts the harness rather than the product,
and it spends the maintainer's device session without their say-so. An agent MUST NOT drive the
connected device to pre-verify a manual case. It states which cases need running, and waits.

**Manual verification.** Every feature MUST ship with a written manual test plan whose cases are
numbered and traceable to requirements. Before any release, shortcut pinning MUST be verified on
at least one device.

## Governance

This constitution supersedes ad-hoc practice. Where guidance conflicts, the order is: this
constitution → the active plan → individual preference.

**Amendments.** Any change to a principle MUST be made in this file, with a Sync Impact Report
recorded in the HTML comment at the top, and MUST state its effect on existing specs.
Amendments take effect immediately on write; work already in flight under the prior version
MUST be re-checked against the new text before completion. Shipped work is not retroactively
non-compliant.

**Versioning.** Semantic versioning applies to this document:

- MAJOR — a principle is removed or redefined in a backward-incompatible way.
- MINOR — a principle or section is added, or guidance is materially expanded.
- PATCH — clarification, wording, or typo fixes with no change in obligation.

**Compliance review.** Each `/speckit-plan` run is a compliance checkpoint via the Constitution
Check gate. Each `/speckit-analyze` run MUST report principle violations found across spec,
plan, and tasks. Complexity that survives review MUST be documented, never silently accepted.

**Runtime guidance.** `CLAUDE.md` at the repository root carries agent-facing runtime guidance
and points to the active plan. It is subordinate to this file and MUST NOT restate principles —
only reference them.

**Version**: 3.0.0 | **Ratified**: 2026-08-15 | **Last Amended**: 2026-08-27
