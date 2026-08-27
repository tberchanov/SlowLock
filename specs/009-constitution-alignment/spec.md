# Feature Specification: Constitution Alignment Refactor

**Feature Branch**: `009-constitution-alignment`

**Created**: 2026-08-26

**Status**: Draft

**Input**: User description: "Refactor application code taking into account new constitution principles. User facing features should not be changed. Only if issues detected, they can be fixed in scope of refactoring after real confirmation of mainteiner."

## Context

The project constitution was rewritten to version 2.1.0 on 2026-08-26. Its seven principles are
new or substantially restated: modern stack and tooling currency, layered architecture with
repository interfaces and injected dependencies, feature-first packaging with layers inside,
structured concurrency, SOLID/single-source-of-truth/KISS, tests that earn their keep, and
version control reserved to the maintainer.

The application code was written under the previous constitution, whose principles were about
product stance — cooperative user, permission minimalism, platform idiom — rather than internal
structure. It therefore predates every structural rule now in force. Principle III says so
explicitly and requires that bringing it forward be **a separate, separately-approved task**
rather than a side effect of other work.

This specification is that task. It changes how the code is arranged, wired, and built. It
changes nothing a user of the app can observe.

## Clarifications

### Session 2026-08-26

- **Q: How far does the package rearrangement reach in this feature?**
  A: All four capabilities are rearranged in this feature, as one dedicated move step containing
  no logic change. The codebase ends in a single style rather than two.

- **Q: What single mechanism supplies dependencies?**
  A: **Hilt**. It is the project's one declared dependency-injection mechanism. It brings a
  build-time code-generation step (KSP) and the Hilt Gradle plugin alongside the runtime library.
  The existing reflection-found ViewModel constructors and their `@JvmOverloads` workarounds are
  replaced by it.

- **Q: When do dependency versions move, given a version change can alter behaviour?**
  A: **First, before any structural work.** Refactoring against APIs that are about to be replaced
  is double work, and the chosen injection mechanism requires a toolchain the current versions do
  not support — so the upgrade has to lead regardless. It is its own step with its own manual
  verification pass, run against a behaviour baseline captured before it, so that any regression
  it causes is attributed to it rather than to the structural work that follows.

- **Q: Which version line?**
  A: **Latest stable only** — no alpha, beta, or release candidate anywhere. As verified against
  Google Maven and Maven Central on 2026-08-26:

  | | current | target |
  |---|---|---|
  | Compose BOM | 2026.02.01 | 2026.08.00 |
  | androidx.core:core-ktx | 1.10.1 | 1.19.0 |
  | androidx.lifecycle (runtime-ktx, viewmodel-compose, runtime-compose) | 2.6.1 | 2.11.0 |
  | androidx.activity:activity-compose | 1.8.0 | 1.13.0 |
  | Kotlin, and the Compose compiler plugin that tracks it | 2.2.10 | 2.3.21 |
  | Android Gradle Plugin | 9.3.1 | 9.3.2 |
  | Hilt (dagger) | — | 2.60.1 |
  | androidx.hilt | — | 1.4.0 |
  | KSP | — | 2.3.11 |
  | kotlinx-coroutines | undeclared, transitive | 1.11.0, declared |
  | JUnit4 | 4.13.2 | 4.13.2 — current, not stale |

  Kotlin 2.3 and above drop the first-generation annotation processor entirely, so the Kotlin
  bump is a precondition of the injection mechanism rather than an independent choice.

  **Kotlin target corrected on 2026-08-26, during planning, and confirmed by the maintainer.** The
  answer originally recorded 2.4.10 — the newest stable Kotlin. Phase 0 research then read the
  published artifacts and found that the newest release of the code-generation step the injection
  mechanism depends on is built against Kotlin 2.3.20, and the injection library itself against
  2.3.21: there is no release of that step on the Kotlin 2.4 line at all. 2.3.21 is a stable
  release on a maintained line, so the "latest stable only, no prereleases" instruction is
  satisfied; it is one minor line back from the newest Kotlin. See plan.md research R2.

- **Q: Which test dependencies are added?**
  A: **`kotlinx-coroutines-test` 1.11.0 only.** It is what lets an injected dispatcher be driven
  by a test scheduler in the JVM suite. No injection-framework test runtime is added: constructor
  injection means unit tests build their subjects directly and never need the object graph, and
  the constitution bans the device-driven suites where it would otherwise be used.

- **Q: Which capability owns the wait/launch path, given it is split across two today?**
  A: **The shortcut capability owns the whole tap-to-launch path.** `WaitScreen` and `WaitTiming`
  move into it; the pinned shortcut's entry point stays exactly where it is, so the frozen
  activity identity is preserved with no alias and no risk to icons already pinned. The delay
  capability is left owning per-app configuration only. Consequence: the delay configuration type
  and its repository interface are shared by both capabilities and therefore move to the named
  shared home rather than staying in either.

- **Q: What happens when the upgrade changes how something renders because a library default
  changed, rather than because our code changed?**
  A: **The new default is accepted unless it reads as a regression.** Each observed difference is
  recorded and ruled on individually by the maintainer. This is the one carve-out from the
  otherwise absolute rule that nothing a user can see may differ: a rendering shift that comes
  from the library's own evolution is allowed through on approval, while any difference
  originating in this project's own code is not.

- **Q: How much manual verification runs at each gate?**
  A: **The six app-relevant legacy manual test plans (001-005, 007) are run in full once, at the
  end.** The post-upgrade gate gets a smoke pass only, not a full regression run. Accepted
  consequence, recorded deliberately: a regression introduced by the upgrade and missed by the
  smoke pass will surface only after the structural work is complete, which weakens the
  attribution the upgrade-first ordering was meant to buy. The bisect back to the upgrade step
  remains available because the upgrade is a step of its own containing no structural change,
  which is what keeps this affordable rather than merely cheap.

- **Q: How is FR-001's timing clause stated and checked, given the project has no performance
  instrumentation?**
  A: **As a qualitative bar, judged by the maintainer.** Tapping a pinned icon must still show the
  wait screen without a perceptible pause, and the wait must still end at the configured moment.
  No numeric budget is set and no measurement method is built. A perceptible regression becomes a
  recorded finding under the confirmation rule. This matters chiefly because the pinned entry point
  is a cold start on a path the user is already waiting on, and the injection mechanism puts graph
  construction in front of it.

- **Q: Where does the root arbiter's state live after the refactor?**
  A: **Split by kind.** A root state holder takes the persisted-configuration read and the
  platform pin-support check — the two things the no-logic-in-a-composable rule actually targets.
  The navigation stage stays where it is, in composition-scoped saveable state, because which
  screen is showing is presentation state rather than a business rule, and the existing mechanism
  is what already delivers the process-death restore that features 003 and 005 specified. After
  this, no composable anywhere touches a data source or a platform service.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The app a user already has keeps working, unchanged (Priority: P1)

Someone already running SlowLock updates to a build produced after this work. Every lock they
created is still listed with the same delay and the same icon treatment. Every icon already on
their home screen still opens the wait screen and still hands off to the right app. Every screen
looks and behaves exactly as it did. Nothing asks them to re-create anything, and nothing they
configured is silently reset.

**Why this priority**: this is the acceptance bar for the whole feature, not one slice of it. A
refactor that improves the code and loses a user's locks is a failed refactor. Every story below
is only allowed to ship on top of this one holding.

**Independent Test**: install the pre-refactor build, create locks across the full flow, pin
shortcuts, then install the post-refactor build over it and walk the manual test plans of the
existing features. Every case passes with no data loss and no re-pinning.

**Acceptance Scenarios**:

1. **Given** an install carrying configured locks and pinned shortcuts, **When** the post-refactor
   build replaces it in place, **Then** every lock, its delay and its icon treatment are still
   present and unchanged.
2. **Given** a shortcut pinned by the pre-refactor build, **When** the user taps it after the
   update, **Then** the wait screen appears for the configured delay and the target app launches,
   exactly as before.
3. **Given** any screen in the app, **When** it is compared against the pre-refactor build,
   **Then** its layout, wording, and interaction are identical.
4. **Given** the refactor is complete, **When** the maintainer diffs user-visible text and
   persisted values, **Then** nothing has been added, removed, or renamed.

---

### User Story 2 - The refactor targets current APIs, not ones about to be replaced (Priority: P2)

The build sits on current, maintained versions of everything it uses, and it does so **before** a
single file is rearranged. Whoever does the structural work writes against the APIs the project
will still be on afterwards, rather than writing against something stale and then having to
rewrite it when the versions move.

**Why this priority**: it goes first among the work stories for two reasons. Refactoring against
APIs due for replacement is work done twice. And the chosen injection mechanism cannot run on the
current toolchain at all — the language version it needs is a precondition, not a preference. It
is also the story most able to change behaviour by accident, so isolating it at the front is what
keeps any regression attributable.

**Independent Test**: with a behaviour baseline captured first, apply only the version changes and
nothing else, build, run the automated suite, and run a manual verification pass. Nothing a user
can see differs.

**Acceptance Scenarios**:

1. **Given** the behaviour baseline captured on the pre-upgrade build, **When** only the versions
   are changed, **Then** the project builds, the automated suite passes, and the manual pass finds
   no user-visible difference.
2. **Given** the dependency declarations, **When** they are inspected, **Then** every version is
   declared in the single central catalog and none is hardcoded at the point of use.
3. **Given** the versions after the upgrade, **When** each is checked against its publisher,
   **Then** every one is a stable release on a maintained line — no prereleases.
4. **Given** the project's prohibition on device-driving automated tests, **When** the build
   configuration is inspected, **Then** no configuration or dependency exists solely to support
   such tests.
5. **Given** the upgrade is complete, **When** the structural work begins, **Then** it is written
   against the upgraded APIs and never against the versions that were replaced.

---

### User Story 3 - Layer boundaries make the code testable and replaceable (Priority: P3)

A contributor — human or agent — needs to change where a piece of data comes from, or to unit
test a decision, without a device. Business decisions live behind interfaces owned by the domain,
platform access sits behind those interfaces, and every collaborator arrives through the
constructor, so a decision can be exercised in isolation and a source can be swapped without
touching the screen that shows it.

**Why this priority**: this is the principle the constitution itself names as the mechanism behind
everything else — testability and replaceability. It is also where the code is furthest from the
rules: data sources are concrete classes constructed at their point of use, some are constructed
inside composables, and the top-level screen arbiter reads persisted data and calls platform
services directly.

**Independent Test**: pick any persisted or platform-backed value and substitute a test double for
its source in a plain JVM test, without a device, an emulator, or a new framework. Then confirm no
UI file reaches a data source directly.

**Acceptance Scenarios**:

1. **Given** any screen's logic, **When** a contributor writes a JVM test for it, **Then** every
   external source it depends on can be replaced through the constructor with no reflection and no
   platform stubbing.
2. **Given** any composable in the app, **When** its imports and body are inspected, **Then** it
   neither constructs nor calls a data source and holds no business rule.
3. **Given** any type in a domain layer, **When** its imports are inspected, **Then** none of them
   are platform framework imports.
4. **Given** a caller of any external data source, **When** its declaration is inspected, **Then**
   it depends on an interface declared in the domain layer and receives domain values, not platform
   types.
5. **Given** the whole application, **When** its dependency wiring is inspected, **Then** there is
   exactly one mechanism supplying dependencies and no hand-rolled service locator anywhere.

---

### User Story 4 - Everything about one capability lives in one place (Priority: P4)

A contributor arriving with no memory of the codebase opens the directory for a capability and
finds all of it — its screens, its rules, and its storage — separated into layers inside that one
directory. Nothing about that capability lives anywhere else, and no capability reaches into
another's screens or storage.

**Why this priority**: it is the arrangement rule, and it depends on Story 3 having established
what a layer is. It is high value for legibility and is the largest single source of churn, so it
follows rather than leads.

**Independent Test**: for each capability, list the files under its directory and confirm the set
matches everything the capability owns; then confirm no cross-capability reach exists outside the
shared homes the constitution names.

**Acceptance Scenarios**:

1. **Given** a capability, **When** its directory is listed, **Then** its files are separated into
   the layers the constitution names, and no file belonging to it sits outside that directory.
2. **Given** two capabilities, **When** their imports are inspected, **Then** neither reaches into
   the other's screen or storage layer.
3. **Given** a type genuinely shared by two capabilities, **When** its location is inspected,
   **Then** it sits in the named shared home rather than in either capability.
4. **Given** any file, **When** its contents are inspected, **Then** it holds either pure logic or
   platform access, never both.
5. **Given** the automated test suite, **When** its layout is compared against the application
   code, **Then** each test sits in the same package path as the code it covers.
6. **Given** an entity whose identity is frozen because a shipped build wrote it into a launcher
   or onto disk, **When** the rearrangement reaches it, **Then** its identity is unchanged and the
   reason is recorded where a future reader would otherwise undo it.

---

### User Story 5 - One screen, one state owner, one asynchrony pattern (Priority: P5)

Every screen renders from exactly one state object owned by exactly one holder. Derived facts are
computed rather than stored alongside. Things that should happen once happen once, by a mechanism
that cannot re-fire. Background work runs where it is told to run, and where it runs is decided by
the caller in a test rather than hardcoded.

**Why this priority**: the code is largely already here — state is exposed as an observable stream,
collected lifecycle-aware, with derived values computed — so this narrows a gap rather than
rebuilding. What remains: screens whose state is held in the screen itself, at least one one-shot
message carried as a nullable field a caller has to clear by hand, and background dispatchers named
inline at every call site instead of supplied.

**Independent Test**: for each screen, name its single state owner; for each one-shot event, confirm
it cannot be observed twice; for each background hop, confirm a test can redirect it.

**Acceptance Scenarios**:

1. **Given** any screen, **When** its state holders are counted, **Then** there is exactly one.
2. **Given** a message or effect meant to occur once, **When** the screen is recreated or the state
   re-observed, **Then** it does not occur again, and no caller has to clear a flag by hand for
   that to be true.
3. **Given** any operation that moves work off the main path, **When** a test exercises it, **Then**
   the test can supply where that work runs.
4. **Given** any long-running operation, **When** the screen that started it goes away, **Then** the
   operation stops, and no cancellation is swallowed or reported as a failure.

---

### User Story 6 - No test in the suite that would not catch a defect (Priority: P6)

The automated suite contains only tests that fail when the behaviour they cover breaks. Everything
the constitution mandates coverage for is still covered. Tests sit where the code they cover sits.

**Why this priority**: it is the smallest blast radius and it is best judged last, once the code
under test has stopped moving. Doing it earlier would mean re-judging the same tests after every
rearrangement.

**Independent Test**: break each mandated behaviour deliberately, one at a time, and confirm the
suite goes red each time; then confirm no retained test passes regardless of the implementation.

**Acceptance Scenarios**:

1. **Given** any retained automated test, **When** the behaviour it covers is deliberately broken,
   **Then** that test fails.
2. **Given** the coverage the constitution mandates, **When** the suite is inspected after the
   refactor, **Then** every mandated area is still covered, including each frozen persisted value
   asserted against a literal.
3. **Given** a test that asserts only what a constructor was given, restates the implementation, or
   covers generated or framework behaviour, **When** the suite is reviewed, **Then** it is gone.

---

### Edge Cases

- **A move would break something already on a user's device.** The identity of the entry point a
  pinned shortcut targets, the names of the stored preference files and their keys, the shortcut
  identifier scheme, and the intent extra names are all frozen: they were written into launchers
  and onto disk by builds already in the field, and renaming one orphans every shortcut or resets
  every configuration silently, with the failure surfacing only when a user taps an icon. Where a
  structural rule would move one of these, the value MUST be preserved as it stands — by leaving
  the entity where it is, or by keeping the old name resolvable — and never by re-creating what
  the user already has.
- **The upgrade changes what a screen looks like.** The smoke pass catches it if it is on one of
  the paths that pass covers; otherwise it surfaces at the final gate, after the structural work,
  and is bisected back to the upgrade step — which is possible only because that step holds no
  structural change (FR-053b). Where the cause is an
  upgraded library's own changed default, the difference is recorded and put to the maintainer,
  who either accepts the new default or calls it a regression to be corrected (FR-001a). It is
  never absorbed silently, and the difference is never assumed acceptable because it was the
  library's doing.
- **A defect is found while refactoring.** The refactor stops at that file, the defect is written
  down with what it breaks and what fixing it would change, and nothing is fixed until the
  maintainer confirms. A defect the maintainer does not confirm is left in place, recorded, and
  carried forward exactly as it was.
- **A constitution rule cannot be satisfied without changing what a user sees.** The rule is not
  applied. The conflict is recorded with the alternative that was rejected, and the maintainer
  decides.
- **A rule is satisfied only at a cost the constitution itself calls a defect** — an abstraction
  with one implementation and no seam behind it, a forwarding-only indirection. The simpler form
  wins and the reasoning is recorded.
- **The work is interrupted part-way.** Every reviewable step leaves the project building, its
  automated checks passing, and the app behaving identically. There is no step whose only valid end
  state is "finish the next one too".
- **Behaviour is observable only on a device.** It is added to a written manual test plan for the
  maintainer to run. It is never verified by driving the maintainer's device automatically.
- **Two rules conflict** — for example, keeping a capability's files together versus keeping a
  frozen entry-point identity. The constitution's own precedence applies, the frozen value wins
  over the arrangement, and the exception is recorded where a reader will find it.

## Requirements *(mandatory)*

### Functional Requirements

#### Behaviour preservation (the acceptance bar)

- **FR-001**: The refactor MUST NOT change any user-visible behaviour: no screen, layout, wording,
  interaction, timing, or ordering may differ from the pre-refactor build.
- **FR-001b**: FR-001's timing clause means, concretely: tapping a pinned icon still shows the
  wait screen without a perceptible pause, and the wait still ends at the configured moment. It is
  judged by the maintainer at both verification gates. No numeric threshold is set and no
  measurement tooling is introduced. A pause a user would notice is a recorded finding, not an
  acceptable cost of the injection mechanism.
- **FR-001a**: The single exception is a rendering difference originating in an upgraded library's
  own changed default. Such a difference MAY be accepted, but only after it has been recorded and
  the maintainer has approved that specific difference. A difference the maintainer reads as a
  regression MUST be corrected rather than accepted, and a difference originating in this
  project's own code is never covered by this exception.
- **FR-002**: The refactor MUST NOT add, remove, or reword any user-facing string.
- **FR-003**: Every value already persisted on a user's device — preference file names, key names,
  stored value formats, and the identifiers under which they are stored — MUST survive the refactor
  byte-identical, so an in-place update loses nothing.
- **FR-004**: Every shortcut already pinned on a user's home screen MUST keep working: the entity
  it targets MUST remain resolvable under the exact identity recorded at pin time, and the payload
  it carries MUST still be read under the same name.
- **FR-005**: The refactor MUST NOT require a user to re-create, re-pin, or re-configure anything.
- **FR-006**: The refactor MUST NOT add a capability, remove one, or change the scope boundary the
  constitution fixes.
- **FR-007**: Where a structural rule and a frozen value conflict, the frozen value MUST win, and
  the exception MUST be recorded in the code at the point a future reader would otherwise undo it.
- **FR-008**: A behaviour baseline MUST be captured from the current `main` build before any change
  is made, so "unchanged" is checkable rather than asserted.

#### Defect handling

- **FR-009**: Any defect discovered during the refactor MUST be recorded — what it is, what it
  breaks, and what fixing it would change for a user — and MUST NOT be fixed before the maintainer
  explicitly confirms that specific fix.
- **FR-010**: A confirmed fix MUST be carried out separately from the structural change around it,
  so the two are reviewable apart.
- **FR-011**: An unconfirmed defect MUST be preserved exactly as it is, with the recorded finding
  left open. Silent correction "while in the file" is prohibited.
- **FR-012**: A refactor step MUST NOT be reported as behaviour-preserving if it also contains a
  behaviour fix, confirmed or not.

#### Toolchain currency, and its position in the order of work

- **FR-013**: The dependency upgrade MUST be completed before any structural change begins, and
  MUST be a step of its own containing no structural change.
- **FR-013a**: Adding an assertion over a frozen value, and the minimum visibility change needed to
  make that value reachable from a test, is NOT a structural change and MAY precede the upgrade.
  It is how a frozen value is guarded before anything is in a position to move it. Nothing else
  qualifies: the exemption covers a test and the constant it reads, and no behaviour may differ.
- **FR-014**: Every version MUST be moved to the target recorded in Clarifications, and each target
  MUST be a stable release — no alpha, beta, or release candidate.
- **FR-015**: Every dependency version MUST be declared in the single central catalog; none may be
  hardcoded at the point of use.
- **FR-016**: Configuration and dependencies existing solely to support automated tests the project
  has prohibited MUST be removed.
- **FR-017**: Every dependency added MUST be recorded with what breaks without it.
- **FR-018**: The upgrade MUST be verified against the behaviour baseline by a manual smoke pass
  before structural work starts. The smoke pass MUST cover, at minimum: the app launching; the
  intro and the locks list rendering; one complete pass through the create-a-lock flow including
  the pin dialog; and one tap of an already-pinned icon through the wait to the hand-off, judged
  against FR-001b's timing bar. It is deliberately not a full regression run — see Clarifications for the accepted consequence.
- **FR-019**: Structural work MUST be written against the upgraded APIs; it MUST NOT introduce a
  usage that the upgrade has just superseded.

#### Layered architecture, dependency direction, injection

- **FR-020**: Every source of data outside the process MUST be reached through an interface declared
  in a domain layer, with its implementation in a data layer, and MUST return domain values rather
  than platform types.
- **FR-021**: No user-interface file MUST reach past the domain into a data source, construct one,
  or call a platform service directly.
- **FR-022**: No business rule MUST live in a composable, an activity, or a composition-scoped memo.
- **FR-023**: Every non-trivial screen MUST be driven by a state holder that exposes its state as an
  observable stream and receives user intent as function calls. A screen counts as non-trivial when
  it reads or writes anything outside the process, decides anything a test could get wrong, or owns
  state that must outlive a configuration change.
- **FR-023a**: The root arbiter MUST hand its persisted-configuration read and its platform
  support check to a state holder. Its navigation stage MAY remain composition-scoped saveable
  state: which screen is showing is presentation state, and the existing mechanism is what
  delivers the process-death restore behaviour already specified by earlier features. That
  behaviour MUST be preserved exactly, including scroll and query retention across the round trip
  and the rule that back returns to whichever screen the flow was entered from.
- **FR-024**: Every collaborator MUST arrive through a constructor. Inline construction at the point
  of use, static holders, and global lookups are prohibited.
- **FR-025**: A domain-layer file MUST NOT import the platform framework.
- **FR-026**: Dependency direction MUST be one-way — presentation and data both depend on domain,
  and domain depends on neither.
- **FR-027**: Dependencies MUST be supplied by the single mechanism recorded in Clarifications, used
  consistently across every entry point. No second mechanism and no hand-rolled service locator may
  remain anywhere in the codebase.
- **FR-028**: Constructor-finding workarounds that exist only because dependencies were not injected
  MUST be removed once the mechanism is in place.

#### Feature-first arrangement

- **FR-029**: Each user-facing capability MUST own one top-level directory holding all of its files,
  with layer subdirectories inside it. All four existing capabilities MUST be rearranged in this
  feature; none may be left in the old shape.
- **FR-029a**: The tap-to-launch path — the wait screen, its timing logic, and the entry point a
  pinned shortcut opens — MUST end up wholly inside the shortcut capability. The delay capability
  MUST be left owning per-app configuration only.
- **FR-029b**: The rearrangement MUST NOT move the entity carrying the frozen pinned-shortcut
  identity. No alias is to be introduced, because no rename is to occur.
- **FR-030**: A capability MUST NOT import another capability's presentation or data layer.
- **FR-031**: A type genuinely shared by two capabilities MUST live in the named shared home, not in
  either capability.
- **FR-032**: A directory MUST NOT be named for a layer, a pattern, or a catch-all.
- **FR-033**: A single file MUST NOT contain both pure logic and platform access; where both forms
  of a helper are needed, they MUST be separate files in their respective layers.
- **FR-034**: Every automated test MUST sit in the same package path as the code it covers.
- **FR-035**: The rearrangement MUST be carried out as a step containing no logic change, so it is
  reviewable as a move.

#### State, asynchrony, and cancellation

- **FR-036**: Each screen MUST have exactly one state owner. Two holders for one screen is a defect
  to be resolved, not a style to be preserved. Navigation stage held by the root arbiter under
  FR-023a is not a second owner of any screen's state: it decides which screen is shown, never
  what that screen shows.
- **FR-037**: Derived state MUST be computed from its source, never stored alongside it and kept in
  step by hand.
- **FR-038**: A one-shot event MUST use a consume-once mechanism, never an observable state field
  with a sentinel value that a caller has to clear.
- **FR-039**: Where background work runs MUST be supplied to the code that performs it, not named
  inline at the point of use, so a test can redirect it.
- **FR-040**: A function that may do slow work MUST be safe to call from the main path without
  blocking it; the caller MUST NOT be required to know where it needs to run.
- **FR-041**: All asynchronous work MUST run in a scope with a real lifecycle, and MUST stop when
  that lifecycle ends.
- **FR-042**: Cancellation MUST NOT be swallowed, reported as a failure, or converted into a
  user-visible error.
- **FR-043**: Blocking calls MUST NOT appear in application code.

#### Simplicity and single source of truth

- **FR-044**: An abstraction with exactly one implementation and no test seam or layer boundary
  behind it MUST be inlined rather than kept.
- **FR-045**: An indirection that only forwards MUST NOT be introduced to satisfy a structural rule.
- **FR-046**: Each piece of state MUST have exactly one owner; everything else observes or derives
  from it.
- **FR-047**: The refactor MUST NOT introduce a parameter no caller supplies, a hook nothing
  implements, or any other capability not required by an accepted specification.

#### Tests

- **FR-048**: A retained test MUST fail when the behaviour it covers breaks. Tests that assert only
  what a constructor was given, that restate the implementation, or that cover generated or
  framework behaviour MUST be removed.
- **FR-049**: Every area the constitution mandates coverage for MUST still be covered after the
  refactor, including the frozen persisted values asserted against literals.
- **FR-050**: A test seam that exists only because a dependency could not be injected MUST be
  replaced by the injected dependency, and its coverage MUST be preserved through the replacement.

#### Process and verification

- **FR-051**: The work MUST be divided into steps that each leave the project building and its
  automated checks passing.
- **FR-052**: Automated verification MUST pass before the work is reported complete.
- **FR-053**: Behaviour that can only be observed on a running device MUST be captured in a written,
  numbered manual test plan traceable to these requirements, and MUST be verified by the maintainer.
  No automated test may drive a device, and no agent may drive the maintainer's device to
  pre-verify a manual case.
- **FR-053a**: Manual verification runs at two gates. After the upgrade step: the smoke pass of
  FR-018. At the end of the feature: this feature's own manual test plan **and** the six
  app-relevant manual test plans of features 001-005 and 007, each in full. The plans covering the
  marketing site (006, 008) are out of scope and MUST NOT be run as part of this feature.
- **FR-053b**: Because the upgrade gate is a smoke pass rather than a full run, the upgrade MUST
  remain a step of its own containing no structural change, so a regression found at the final
  gate can still be bisected back to it.
- **FR-054**: Every deviation from a constitution rule that survives the refactor MUST be recorded
  with the simpler alternative named and the reason it was rejected.
- **FR-055**: The agent MUST NOT commit, push, branch, merge, rebase, or tag. Completed work is left
  in the working tree and offered to the maintainer.

### Key Entities

- **Frozen value**: any name or format already written into a launcher or onto a user's disk by a
  shipped build — the pinned entry point's identity, the preference file names, the stored key
  names, the shortcut identifier scheme, the intent extra names. Cannot change; constrains where
  the things carrying it may move.
- **Capability**: a user-facing area of the app that owns one top-level directory. The app has
  four, and their boundaries are settled in Clarifications: the installed-app list; the per-app
  delay configuration; the lock list; and the shortcut capability, which covers both creating a
  pinned icon and the whole tap-to-launch path behind it — the wait and the hand-off included.
- **Layer**: presentation, domain, or data — the subdivision inside a capability, and the unit the
  dependency-direction rule is stated over.
- **Behaviour baseline**: the pre-refactor build's observable behaviour, captured before the first
  change, against which every step is judged.
- **Recorded finding**: a defect discovered during the refactor, with what it breaks and what a fix
  would change, held open until the maintainer rules on it.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: At the final gate, 100% of the manual test cases in the six app-relevant existing
  plans (features 001-005 and 007) pass on the post-refactor build, with no case needing its
  expected result amended.
- **SC-002**: A device carrying locks and pinned shortcuts, updated in place to the post-refactor
  build, retains 100% of its locks with their delays and treatments, and 100% of its pinned icons
  still launch their targets.
- **SC-003**: Zero user-facing strings differ between the pre- and post-refactor builds.
- **SC-004**: Every reviewable step builds and passes its automated checks — zero steps end with a
  failing build or a failing suite.
- **SC-005**: Zero behaviour changes ship without a recorded maintainer confirmation naming that
  specific change. This includes every accepted library-default rendering difference: 100% are
  recorded and individually approved, and zero reach the build unrecorded.
- **SC-006**: The dependency upgrade completes and passes its manual smoke pass before the first
  structural change is made — zero structural changes precede it, and zero structural changes are
  contained within it. The frozen-value guards permitted by FR-013a are not counted, and there are
  zero changes ahead of the upgrade that are not covered by that exemption.
- **SC-007**: 100% of declared versions are stable releases on a maintained line, and 100% are
  declared in the central catalog.
- **SC-008**: For each capability, 100% of the files belonging to it sit under its own directory,
  and zero files belonging to it sit elsewhere.
- **SC-009**: Zero user-interface files construct or call a data source or platform service
  directly.
- **SC-010**: Exactly one dependency-supplying mechanism is present across the whole codebase.
- **SC-011**: Zero screens have more than one state owner.
- **SC-012**: Every constitution rule is either satisfied or carries a recorded, justified deviation
  — zero unrecorded violations remain.
- **SC-013**: Every retained automated test fails when the behaviour it covers is deliberately
  broken, verified by spot-check across each mandated coverage area.
- **SC-014**: A contributor can locate everything belonging to a named capability by opening one
  directory, without searching the tree.
- **SC-015**: At both verification gates, tapping a pinned icon shows the wait screen with no pause
  the maintainer perceives, and the wait ends at the configured moment — zero perceptible timing
  regressions ship unrecorded.

## Assumptions

- The constitution at version 2.1.0, dated 2026-08-26, is the authority for every rule this
  specification refers to. If it is amended mid-flight, work in progress is re-checked against the
  new text before completion.
- Approving this specification is the separate approval Principle III requires before existing
  packages are rearranged. No other work is authorised to rearrange them.
- "Application code" means the Android application source and its build configuration. The
  marketing site, the README, and the specification history are out of scope.
- The project stays a single application module. Structural rules are satisfied by arrangement
  inside it rather than by splitting it.
- The compile and target platform levels are not changed by this work. Only library, language, and
  build-plugin versions move.
- Adopting the chosen injection mechanism adds a build-time code-generation step and a runtime
  library the project does not have today. It is taken as a plan-level dependency decision, not a
  constitutional amendment, because it introduces no network stack, database engine, or analytics
  runtime. The plan's Constitution Check is expected to justify it explicitly against Principle V's
  simplicity rule, given the project's size — the maintainer's decision recorded in Clarifications
  is the authority for the choice itself.
- No new capability and no new permission is introduced.
- All device verification is performed by the maintainer against a written plan. The agent produces
  the plan and waits.
- Version control stays with the maintainer: the agent leaves changes uncommitted and offers them.
