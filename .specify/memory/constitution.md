<!--
SYNC IMPACT REPORT — v3.0.0 → v4.0.0 (MAJOR)

Modified principles:
- II — state-holder scoping rule added: a screen's holder MUST be scoped to its navigation entry
  and cleared on pop. Activity-scoped holders, compliant under 3.0.0, are now defects.
- V — "Standard solutions over bespoke ones" added, ranked above KISS on choice of mechanism.
  Hand-rolled equivalents of solved problems, previously sanctioned by KISS, are now defects.

Added: Governance → Style (binds this file and every future amendment).
       Additional Constraints → Navigation (first-party Jetpack library; bespoke prohibited).

Removed: no binding rule. This revision restates 3.0.0 under the new Style rule — rationales
capped, duplicated rules stated once, migration narrative dropped.

Effect on existing specs: specs/009-constitution-alignment is shipped and not retroactively
non-compliant, but three of its Complexity Tracking entries are superseded — the bespoke
`when` navigation, the `rememberSaveable` stage, and the `ShortcutConfigScreen` treatment
owner. The feature that adopts navigation resolves all three.

Templates: ✅ plan-template.md (generic gate, still eight principles) · ✅ spec-template.md ·
✅ tasks-template.md · ✅ checklist-template.md · ✅ CLAUDE.md · ✅ README.md

Deferred: which Jetpack navigation artifact to adopt is a plan-level decision under Principle I.
-->

# SlowLock Constitution

## Core Principles

### I. Modern Stack, Current Tooling

The project runs on the current Android toolchain.

- Kotlin, Compose and Material 3 for all UI. XML layouts MUST NOT be used for new screens.
  Fragments as UI containers, `findViewById` and data binding MUST NOT be introduced.
- Every dependency version MUST be declared in `gradle/libs.versions.toml`; hardcoded coordinates
  or versions in a `build.gradle.kts` are prohibited. Compose artifacts come from the BOM.
- A dependency MUST be on a maintained release line. One that is deprecated, archived or
  superseded by a first-party replacement MUST NOT be adopted; one that becomes so MUST be
  scheduled for replacement, not pinned indefinitely.
- Prefer the first-party solution where one fits: Jetpack over a third-party equivalent,
  `kotlinx` over an ad-hoc utility, a platform API over a wrapper.
- Adding a dependency is a plan-level decision, recorded with what breaks without it. Adding one
  that pulls in a runtime the project lacks — network stack, database engine, analytics SDK — is
  an amendment.

### II. Layered Architecture: Clean, MVVM, Repository, DI

- **Layers.** Presentation (Compose UI + ViewModel), domain (models, use cases, repository
  interfaces), data (implementations, platform and persistence sources). Dependencies point inward
  only: presentation → domain ← data. The domain MUST NOT import `android.*`. The UI MUST NOT
  reach past the domain into a data source.
- **MVVM.** Every non-trivial screen is driven by a `ViewModel` exposing state as an observable
  stream and receiving intent as function calls. Business rules MUST NOT live in a composable, an
  `Activity`, or a `remember` block.
- **State holders are scoped to their screen.** A holder MUST be scoped to its navigation entry:
  state lives while the entry is on the back stack, and is cleared when it is popped. A holder
  shared by several screens MUST be scoped to the narrowest entry covering them, never to the
  `Activity` by default. `SavedStateHandle` survives process death and MUST NOT be used to carry
  state across separate visits. State outliving its screen MUST be deliberate and commented,
  naming the behaviour requiring it.
- **Repository.** Every source outside the process — `PackageManager`, persistence, the
  filesystem, the clock — MUST be reached through a repository whose interface is in the domain
  and whose implementation is in the data layer. Callers depend on the interface; platform types
  MUST NOT cross it.
- **Dependency injection.** Constructor injection only. Inline construction, static holders,
  global singletons and hand-rolled service locators are prohibited; the project's single declared
  framework is the only mechanism.
- **Use cases** are warranted when logic is shared across ViewModels or is genuinely a domain
  rule. A ViewModel MAY call a repository directly where a use case would only forward.

**Rationale:** state that outlives its screen is invisible until a user returns and finds a
previous visit's answer waiting; scoping it to the entry makes lifetime structural rather than a
`clear()` someone has to remember.

### III. Feature First, Layers Inside

Source is organized by feature, with Principle II's layers as subpackages inside each.

- **Shape.** `com.slowlock.feature.<feature>.{ui, domain, data}`. A feature is named for a
  user-facing capability (`locks`, `apps`, `delay`, `shortcut`), never a layer or a pattern;
  `utils`, `helpers`, `managers`, `models` and `common` are prohibited as package names.
- **Layer subpackages are earned.** A feature spanning more than one layer MUST separate them; one
  small enough to hold a single layer MAY stay flat until it grows.
- **One file, one layer.** A file MUST NOT hold both pure logic and platform access; the pure form
  belongs in `domain`, the framework-taking form in `data`, in separate files.
- **Features do not reach into each other.** A feature MUST NOT import another's `ui` or `data`. A
  domain type two features genuinely share moves to `core`.
- **Shared homes sit outside `feature`.** `com.slowlock.core` holds cross-feature domain types and
  platform shims. `com.slowlock.ui.{components, theme}` holds the design system and is the one
  package every feature's `ui` MAY depend on. Entry points sit at the root package or in the
  feature they serve.
- **Tests mirror main exactly**, package for package, under `src/test`.
- **A frozen fully-qualified name outranks this shape.** Where a class's FQN is itself a frozen
  persisted value it MUST stay at that name, with the deviation commented at the class.
  `com.slowlock.shortcut.ShortcutLaunchActivity` is such a class: its name is written into every
  pinned shortcut already on a user's launcher.
- **This governs all code, not only new code.** A non-conforming package is a defect to migrate,
  and MUST NOT receive new code.

**Rationale:** a capability that lives in one directory can be read, changed or deleted whole, and
the `feature` prefix makes an illegal cross-feature import visible as a line in the file holding it.

### IV. Structured Concurrency with Coroutines and Flow

- Coroutines MUST run in a scope with a real lifecycle: `viewModelScope`, a repository scope owned
  by its holder, or one tied to the caller. `GlobalScope` MUST NOT be used.
- **Suspend functions MUST be main-safe**, moving to `Dispatchers.IO` or `Dispatchers.Default`
  themselves via `withContext`; callers MUST NOT need to know which dispatcher a function wants.
  Package enumeration, icon rasterization and disk I/O fall under this rule.
- Dispatchers MUST be injected, never referenced as hardcoded globals.
- **UI state is a `StateFlow`**, immutable and non-null by default. One-shot events use a channel
  or equivalent consume-once mechanism, never a `StateFlow` with a sentinel. Compose collects with
  `collectAsStateWithLifecycle()`; a bare `collectAsState()` on a lifecycle-scoped stream is
  prohibited.
- Cold flows MUST stay cold: no side effects on collection, no work at construction. Operators run
  on the intended dispatcher via `flowOn`, never by switching context inside the builder.
- **Cancellation MUST be respected.** Long-running loops check for activity, `NonCancellable` is
  used only where required, and `CancellationException` MUST NOT be swallowed by a broad `catch`.
  Flow errors are handled with `catch`, not a `try` around `collect`.
- `Thread.sleep`, blocking I/O and `runBlocking` MUST NOT appear in production code.
  `runBlocking` is permitted in tests only.

**Rationale:** the core behaviour is a timed hand-off between processes, so leaked work, swallowed
cancellation or a blocked main thread surface as a frozen delay screen or a launch that never
happens.

### V. Standard Solutions, SOLID, SoC, Single Source of Truth, KISS

- **Standard solutions over bespoke ones.** Where a problem is already solved by an established
  Android pattern or a maintained first-party library — navigation, dependency injection, state
  holding, persistence — that solution MUST be used, even where a hand-rolled equivalent would be
  smaller or carry no dependency. A bespoke mechanism MUST NOT be introduced on the grounds that
  the current feature set is small enough to make one viable.
- **SOLID.** One reason to change per class; extend by adding a type, not a branch to a growing
  `when`; a subtype MUST be usable wherever its supertype is; an interface MUST NOT force
  implementers to stub what they do not need; depend on abstractions across layer boundaries,
  concretes within one.
- **Separation of concerns.** UI does not parse, parsing does not persist, persistence does not
  decide policy. A file mixing layers MUST be split, not sectioned with comments.
- **Single source of truth.** Each piece of state has exactly one owner; everything else derives
  from it or observes it. Derived state MUST be computed, never stored in parallel and synced by
  hand. Two competing state holders for one screen is a defect.
- **KISS.** The simplest design satisfying the requirement wins. Nothing is built ahead of an
  accepted spec. An abstraction with one implementation and no test seam or layer boundary
  justifying it MUST be inlined. Speculative generality — a parameter no caller passes, a hook
  nothing implements — is a defect.
- **Conflict order.** Principle II's boundaries are structural and stay. The standard solution
  wins on choice of mechanism. KISS then governs how much of that mechanism is used, and what
  fills the boundaries. Keeping the presentation/domain/data seam is not over-engineering, nor is
  adopting the navigation library; wrapping that library in a project-specific abstraction, adding
  a forwarding-only use case, or mapping between two identical shapes is.

**Rationale:** a bespoke mechanism must be understood before it can be changed, while a standard
one is already known to whoever arrives and is maintained by someone else. Its cost is not paid
when it is written, where it genuinely is smaller, but at every later change and again when it is
replaced by the standard solution anyway.

### VI. Tests That Earn Their Keep

- **Coverage is not a goal and MUST NOT be cited as evidence of quality.** A test that would still
  pass against a different-but-wrong implementation MUST NOT be written.
- Prohibited: asserting a getter returns what the constructor was given; mocking a unit's
  collaborators so thoroughly the test asserts only the mock's script; testing generated code,
  data-class `equals`, or framework behaviour; assertions restating the implementation line by line.
- Test **behaviour at a seam**. A test SHOULD survive a behaviour-preserving refactor and MUST fail
  when behaviour changes.
- Automated coverage means **JVM unit tests only** (`./gradlew test`). Required before a feature is
  complete:
  - Schedule and time-window evaluation, including boundary times, weekday handling, and the
    outside-window immediate-launch path.
  - Target resolution, including the null `getLaunchIntentForPackage()` path.
  - Every frozen persisted value, asserted against a literal, so a rename fails the build instead
    of a user's device.
- Pure-Compose presentation without branching logic MAY ship untested, and SHOULD.
- Test-first is RECOMMENDED, never mandated.

**Rationale:** a padded suite costs time to write and maintain, and buys false confidence that
stops anyone looking harder at the parts that break.

### VII. Version Control Is the Maintainer's

- An agent MUST NOT run `git commit`, `push`, `merge`, `rebase` or `tag`, and MUST NOT create,
  switch or delete branches, unless the maintainer asked for that specific action in that specific
  message.
- A general go-ahead is not permission. "Proceed", "continue", "sounds good", approving a plan, or
  answering an unrelated question MUST NOT be read as authorization.
- Permission does not persist: being asked to commit once authorizes that commit, not the next.
- A `tasks.md` entry saying to commit is a note to the maintainer. The agent MUST leave it
  unchecked and report the work as staged and ready.
- Read-only git commands and writing files in the working tree are always permitted; leaving
  changes uncommitted is the expected end state.
- On completion the agent MUST report what changed and stop, offering the commit rather than
  performing it.

**Rationale:** a commit is the maintainer's signature and a push to `main` on a public repository
is effectively irreversible; reading "proceed" as "publish" is the failure this prevents.

### VIII. Comments That Earn Their Place

- **A comment explains why, never what.** One paraphrasing the line beneath it MUST NOT be written,
  and MUST be deleted when found.
- **A comment MUST be as short as the reason allows.** Where one sentence carries the reason, a
  paragraph is a defect. Narrative — which feature changed this, what an earlier version did —
  MUST NOT be committed; git holds history.
- MUST be commented, because the code cannot say them: every frozen persisted value, naming what
  breaks on a rename; a platform behaviour that reads as a bug (an API named for the opposite of
  what it does, a call that must not be pre-flighted, an off-by-one the framework requires); a
  deliberate deviation from an approved design, naming the requirement it serves; a load-bearing
  absence (a guard deliberately not added, a callback not observed, a value not cached).
- MUST NOT be committed: commented-out code; banner comments sectioning a file Principle V says to
  split; a comment restating a requirement identifier's text; a KDoc that only expands the member's
  own name.
- **A comment is part of the change it describes.** One made false by an edit MUST be corrected or
  deleted in the same change; a stale comment is worse than none, because it is believed.
- Public domain and repository declarations SHOULD carry KDoc where the contract is not evident
  from the signature — what the caller may assume, what is returned on failure, which dispatcher
  rule applies. Where the signature says it, the KDoc is noise.

## Additional Constraints & Technology Standards

**Fixed stack.** Kotlin, Compose (BOM-managed), Material 3, AGP, single `:app` module,
`minSdk 26`, namespace and applicationId `com.slowlock`. Module structure MAY grow when a
Principle II boundary justifies it; package-level layering inside `:app` is the expected start.

**Navigation.** Screen navigation MUST be provided by the first-party Jetpack navigation library.
Bespoke navigation — a `when` over a sealed stage, a hand-managed back stack — MUST NOT be used.
The artifact and release line are a plan-level decision under Principle I.

**No backend.** No server, no account, no network requirement. Introducing network access,
analytics or telemetry is an amendment.

**Scope boundary.** The app covers exactly: app enumeration and picking, per-app schedule and delay
configuration, pinned shortcut creation with mirrored icon and label, and a delay screen that
launches the target. Anything beyond requires an approved spec. How the delay screen presents —
countdown, progress, or deliberately static — is a product decision for the feature building it.

**Non-goals.** Parental controls, usage enforcement, usage statistics and cross-device sync MUST be
rejected at spec review.

**Product invariants.** SlowLock inserts friction between impulse and action; it MUST NOT attempt
enforcement.

- Any user-visible delay MUST be escapable (home, back, or an explicit skip).
- A feature MUST NOT be justified by "the user could bypass this".
- ACCEPTED LIMITATIONS — a spec MUST NOT treat these as bugs, or propose closing them without new
  information:
  - The target app's original icon still exists in the drawer and still launches it directly.
  - Recents, deep links and launcher search bypass the launcher entirely and are uncovered.
  - The delay is not enforceable, and removing a pinned shortcut is a long-press and a drag.
  - Some launchers badge pinned shortcuts, so the icon will not look perfectly native. Each pin
    costs its own system dialog; there is no batch pinning.
  - Dual-app clones (Xiaomi Dual Apps, Samsung Secure Folder) are untested, and uninstalling
    SlowLock may leave dead shortcuts behind.

**Rationale:** the user is a willing participant nudging their own behaviour, not a subject to be
policed, so bypassability is the design rather than a defect.

**Permission and policy minimalism.** The app ships the fewest permissions that make the feature
work.

- `QUERY_ALL_PACKAGES` MUST NOT be requested; enumeration MUST use a manifest `<queries>`
  declaration for `ACTION_MAIN` + `CATEGORY_LAUNCHER`.
- `AccessibilityService` MUST NOT be used for non-accessibility purposes, and `PACKAGE_USAGE_STATS`
  MUST NOT be requested. Both were weighed as ways to cover the uncovered launch paths above and
  rejected — a restricted permission, Play policy exposure, battery drain from a service OEM
  managers kill silently, and a permission-plus-whitelisting onboarding. That rejection is settled.
- Any new permission MUST be listed in the plan with its Play Store policy standing and approved
  before implementation.
- The app MUST remain functional, degrading gracefully, when the user declines an optional system
  dialog — including the pin-shortcut dialog.

**Platform behaviour.** Work with the platform, never around it.

- System APIs that can fail MUST be handled at the call site: `getLaunchIntentForPackage()` can
  return null; `isRequestPinShortcutSupported()` MUST gate every pin attempt.
- Activity launches MUST originate from a user-initiated foreground context; background activity
  starts MUST NOT be attempted.
- No persistent foreground services, no polling loops, no `PowerManager` wake locks. Battery cost
  at rest MUST be zero.
- Keeping the display awake is permitted only as `FLAG_KEEP_SCREEN_ON`, only on a screen the user
  is actively looking at, and only where the feature does not work without it; it MUST be released
  with that window and MUST NOT outlive it. A `PowerManager` wake lock is never a substitute: it
  needs a permission, it is deprecated, and it can survive the screen that took it.
- OEM battery management MUST NOT be fought with hacks or whitelisting prompts.

## Development Workflow & Quality Gates

**Spec-driven flow is mandatory.** `/speckit-specify` → `/speckit-clarify` (where ambiguity exists)
→ `/speckit-plan` → `/speckit-tasks` → `/speckit-implement`. Implementation MUST NOT begin before
an approved plan exists.

**Constitution Check gate.** Every plan MUST evaluate all eight principles before Phase 0 and
re-check after Phase 1. Any violation MUST appear in Complexity Tracking with the simpler
alternative named and the reason it was rejected. An unjustified violation blocks implementation.

**Build gate.** `./gradlew assembleDebug` and `./gradlew test` MUST pass before a feature is
complete. Work MUST NOT be reported as done on an unverified build.

**No automated test may drive a device.** Instrumented suites (`src/androidTest`,
`connectedAndroidTest`, Espresso, UI Automator) MUST NOT be added. Behaviour observable only on a
running app — a delay screen's timing, the hand-off to a target app, what a launcher does with a
pin request — MUST be verified manually by the maintainer against the feature's manual test plan.
An agent MUST NOT drive the connected device to pre-verify a manual case; it states which cases
need running, and waits.

**Rationale:** these behaviours depend on real launchers, real OEM builds and real timing, so a
scripted approximation asserts the harness rather than the product, and spends the maintainer's
device session without their say-so.

**Manual verification.** Every feature MUST ship with a manual test plan whose cases are numbered
and traceable to requirements. Before any release, shortcut pinning MUST be verified on at least
one device.

## Governance

This constitution supersedes ad-hoc practice. Where guidance conflicts: this constitution → the
active plan → individual preference.

**Style.** This document states rules, not reasoning. Each principle is binding MUST/MUST NOT rules,
preceded by one sentence of scope only where the heading does not carry it. A rationale is written
only where a rule is counter-intuitive or has already been re-litigated once, and is at most two
sentences. Every rule appears exactly once; where two sections bear on the same rule, one states it
and the other references it by name. An amendment MUST NOT restate an existing rule for emphasis,
and MUST NOT record history the git log already holds.

**Amendments.** Any change to a principle MUST be made in this file, with a Sync Impact Report in
the HTML comment at the top covering that amendment only, and MUST state its effect on existing
specs. Amendments take effect on write; work in flight under the prior version MUST be re-checked
before completion. Shipped work is not retroactively non-compliant.

**Versioning.** MAJOR — a principle removed or redefined incompatibly. MINOR — a principle or
section added, or guidance materially expanded. PATCH — clarification with no change in obligation.

**Compliance review.** Each `/speckit-plan` run is a checkpoint via the Constitution Check gate.
Each `/speckit-analyze` run MUST report principle violations across spec, plan and tasks. Complexity
surviving review MUST be documented, never silently accepted.

**Runtime guidance.** `CLAUDE.md` carries agent-facing runtime guidance and points to the active
plan. It is subordinate to this file and MUST NOT restate principles — only reference them.

**Version**: 4.0.0 | **Ratified**: 2026-08-15 | **Last Amended**: 2026-08-27
