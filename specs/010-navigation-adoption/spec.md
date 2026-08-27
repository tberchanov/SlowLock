# Feature Specification: Navigation Adoption

**Feature Branch**: `main` (no feature branch; Principle VII reserves branch creation to the maintainer)

**Created**: 2026-08-27

**Status**: Draft

**Input**: User description: "Based on the previous plan. I approved all recommended decisions." — the audit of `app/src` against constitution v4.0.0, its defect register (N-1 to N-3, S-1 to S-4, B-1, C-1) and its seven decisions (D1-D7).

## Context

The constitution was amended to version 4.0.0 on 2026-08-27. Three obligations are new:

- **Principle II** now requires a screen's state holder to be scoped to its navigation entry, living
  while that entry is on the back stack and cleared when it is popped. A holder scoped to the
  activity by default is a defect. `SavedStateHandle` MUST NOT be used to carry state across
  separate visits.
- **Principle V** now ranks *standard solutions over bespoke ones* above KISS. A hand-rolled
  equivalent of a solved problem is a defect even where it is smaller and carries no dependency.
- **Additional Constraints** now require screen navigation to come from the first-party Jetpack
  navigation library, and name the bespoke forms — a `when` over a sealed stage, a hand-managed
  back stack — as prohibited.

Feature 009 shipped under version 3.0.0, which sanctioned all three of the things now prohibited.
Its work is not retroactively non-compliant, but the amendment's own Sync Impact Report names three
of its Complexity Tracking entries as superseded: the bespoke `when` navigation, the
`rememberSaveable` navigation stage, and the owner of the shortcut screen's treatment selection.
It states that "the feature that adopts navigation resolves all three". This specification is that
feature.

The audit found that every violation traces to one decision — 009 built navigation by hand — and
that the two surviving Complexity Tracking deviations and two open findings (F-05, F-06) exist
*only* as workarounds for activity-scoped holders. Adopting the navigation library and scoping
holders to entries are not separable: entry-scoped holders have no entries to scope to until the
graph exists. This is one feature.

Unlike 009, this feature is not behaviour-neutral. Three differences a user can observe are
approved in advance and recorded in Clarifications; everything else must be identical.

## Clarifications

### Session 2026-08-27

- **Q: Which navigation artifact, given the constitution defers it to the plan?**
  A: **The latest stable release line of the first-party Jetpack navigation library, verified
  against the published artifacts at plan time.** The choice between the established Compose
  navigation artifact and the newer back-stack-as-state generation is made in the plan, from what is
  actually published on the day, not from recall. The newer generation is taken only if it has a
  stable release line; a prerelease is not eligible, in keeping with 009's "latest stable only"
  rule and Principle I's "maintained release line". The decision is recorded in the plan with the
  evidence.

- **Q: How do destination arguments travel, given the stage carried five fields?**
  A: **Type-safe routes.** Route identity and its arguments are declared as types the compiler
  checks, replacing the hand-written bundle serialiser and its string discriminants. This adds the
  first-party Kotlin serialization plugin and its core runtime as dependencies; both are recorded
  in the plan with what breaks without them. The alternative — string routes with declared
  arguments — adds no dependency but loses the exhaustiveness that today makes a forgotten stage a
  compile error, and it is a hand-rolled encoding of exactly the kind Principle V now rules out.

- **Q: Does the injection artifact change?**
  A: **No, unless a holder turns out to need a parent entry's scope.** Inside a destination the
  ambient state-holder owner is already the navigation entry, so holders resolve to the correct
  scope with no change at any call site. The artifact that exists only to reach a *parent* entry is
  adopted only if the fallback in the refresh question below is taken. Either way, the version
  catalog's comment declaring that this project does not want the navigation runtime becomes false
  and is corrected in the same change.

- **Q: Where does the "this launcher cannot pin" state live?**
  A: **A gate above the graph, not a destination.** It is a whole-app precondition re-read on every
  return to the foreground, not a screen the user navigates to, and pushing and popping a
  destination in response to a lifecycle signal is more mechanism than the behaviour needs — which
  is what Principle V's conflict order leaves KISS in charge of. Its existing behaviour is
  preserved exactly: it takes over the whole app, and the user's place returns with support. The
  gate MUST carry a comment naming why it is not a destination, because the next reader will
  otherwise mistake it for the construct this feature removed.

- **Q: The saved delay is read *before* navigation today, so the delay screen never shows a default
  and then corrects itself. Where does that read go?**
  A: **Into the delay screen's own state holder, with the editable value withheld until the read
  completes.** The screen already renders nothing while it resolves the target, so withholding one
  more element until loaded is the pattern already in place rather than a new one. A manual case
  covers it: opening the delay screen for a configured app MUST NOT show a default value first.
  **Fallback, if a flash proves visible on device**: keep the read on the holder of the screen that
  initiated it and pass the loaded value as a route argument. The fallback is taken only on
  evidence from the device, and recorded if it is.

- **Q: Completing the flow waits for the lock list to be re-read before it returns home, so a new
  lock is present in the first frame. What replaces that once holders are entry-scoped?**
  A: **The home entry's own return to the resumed state triggers the re-read, and the explicit wait
  is dropped.** A shortcut entry can no longer reach the home entry's holder, and it should not.
  A one-frame stale list is accepted in principle and MUST be verified on device.
  **Fallback, if the stale frame is visible**: scope the lock-list holder to the entry covering the
  whole graph — permitted by Principle II as "the narrowest entry covering them", and wider than
  the behaviour needs, so it is taken only on evidence and recorded as a deviation if it is.

- **Q: Principle V now names persistence among the solved problems. Does the stored configuration
  move to a different mechanism?**
  A: **No, and the reasoning is recorded so it is not re-litigated.** The stores use a platform API,
  which Principle I prefers over a wrapper; they are not a bespoke mechanism; and they are already
  thin wiring over pure functions the test suite reaches directly. Every file name and key they use
  is a frozen persisted value, so moving them is the highest-risk change available in this codebase
  for no user-visible gain. Adopting a different persistence library is a separate question for a
  separate specification.

- **Q: Adopting a navigation graph makes deep links available. Are any introduced?**
  A: **No.** The constitution's scope boundary fixes what the app covers, and its accepted
  limitations record that deep links bypass the launcher and are deliberately uncovered. No
  destination is given a deep link, and the entry point a pinned shortcut opens MUST NOT be routed
  through the graph — it is a separate entry point with its own task affinity and a frozen
  identity, and it hosts a single screen with no navigation of its own.

- **Q: 009's defect-handling rules were specific to that feature. Do they carry?**
  A: **Yes, unchanged.** A defect found while working is recorded with what it breaks and what a
  fix would change for a user, and is not fixed before the maintainer confirms that specific fix.
  An unconfirmed defect is left exactly as it is. A step containing a fix is never reported as
  behaviour-preserving. The open findings from 009 that this feature does not resolve stay open.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Everything a user already has keeps working (Priority: P1)

Someone running SlowLock updates to a build produced after this work. Every lock they created is
still listed with the same delay and the same icon treatment. Every icon already on their home
screen still opens the wait screen and still hands off to the right app. Every screen looks the
same, every step of the flow leads where it led, and back goes where back went. Nothing asks them
to re-create anything.

**Why this priority**: this is the acceptance bar for the whole feature. A refactor that improves
the code and orphans a pinned shortcut is a failed refactor. Every story below ships only on top of
this one holding.

**Independent Test**: install the pre-change build, create locks across the full flow, pin
shortcuts, then install the post-change build over it and run the manual test plans of features
001-005 and 007. Every case passes with no data loss and no re-pinning, except the three
differences named in FR-002.

**Acceptance Scenarios**:

1. **Given** an install carrying configured locks and pinned shortcuts, **When** the post-change
   build replaces it in place, **Then** every lock, its delay and its icon treatment are unchanged.
2. **Given** a shortcut pinned by the pre-change build, **When** the user taps it after the update,
   **Then** the wait screen appears for the configured delay and the target app launches.
3. **Given** any screen in the app, **When** it is compared against the pre-change build,
   **Then** its layout, wording and interactions are identical.
4. **Given** the user is anywhere in the three-step flow, **When** the device rotates or the process
   is killed and the app reopened, **Then** the user is returned to the step they were on with the
   choices they had made.

---

### User Story 2 - Moving between screens is the library's job (Priority: P2)

Which screen is showing, what a back press does, and what survives a round trip are all decided by
the navigation library rather than by application code. There is no hand-written stage type, no
hand-written serialiser for it, no hand-managed record of which screen is retained, and no
per-screen interception of the system back gesture.

**Why this priority**: it is the prohibited construct, named verbatim in the constitution, and
every other story in this feature depends on the graph existing first.

**Independent Test**: walk every path through the app — including both routes into the delay step
and back out of it — and confirm each lands where the pre-change build landed. Then confirm no
application file decides which screen is showing.

**Acceptance Scenarios**:

1. **Given** the user is on the delay step having arrived from the app list, **When** they go back,
   **Then** they return to the app list.
2. **Given** the user is on the delay step having arrived by editing an existing lock, **When** they
   go back, **Then** they return to the lock list.
3. **Given** the user is on any screen with a back control, **When** they use the system back
   gesture instead, **Then** exactly the same thing happens.
4. **Given** the user is on either root screen, **When** they go back, **Then** they leave the app.
5. **Given** the application source, **When** it is inspected, **Then** no branch over a
   hand-written screen type decides what is rendered, and no code adds to, removes from, or
   serialises a screen history by hand.

---

### User Story 3 - What a screen holds dies with the screen (Priority: P3)

State belonging to a screen lives exactly as long as that screen is reachable. Leaving a screen for
good discards what it held; going deeper and coming back does not. Nothing a user abandoned
reappears later, and nothing they were in the middle of is lost to a rotation or to the process
being killed.

**Why this priority**: this is the amended Principle II, and it is what removes both workarounds
that 009 had to record — the icon treatment held outside its state holder, and the two extra
sources exposed on the root holder to compensate for a screen that had none.

**Independent Test**: for each screen, leave it in a non-default state, exit the flow, and re-enter
for a different app; nothing carried over. Then repeat with a rotation and a process kill instead of
an exit; nothing was lost.

**Acceptance Scenarios**:

1. **Given** the user chose an icon treatment and then left the flow, **When** they configure a
   different app, **Then** the treatment shown is that app's saved treatment, not the abandoned one.
2. **Given** the user chose an icon treatment, **When** the device rotates or the process is killed
   and the app reopened on that screen, **Then** their choice is still selected.
3. **Given** the user changed the delay and moved on to the icon step, **When** they go back,
   **Then** the delay they chose on the way through is what the screen shows.
4. **Given** the user searched the app list and went forward into the flow, **When** they come back,
   **Then** their search text and scroll position are where they left them.
5. **Given** the user searched the app list and then left it entirely, **When** they open the app
   list again, **Then** it opens fresh, with no search text and at the top.

---

### User Story 4 - The delay screen owns what it edits (Priority: P4)

The screen where a delay is chosen holds that delay itself, resolves the app it is configuring
itself, and reads that app's saved configuration itself. The root of the app holds only the one
thing that belongs to no screen.

**Why this priority**: it is the last of 009's structural workarounds, and it closes two open
findings at once. It is separable from story 3 only in review, not in time.

**Independent Test**: open the delay screen for a configured app and confirm it shows the saved
value with no default flash; then confirm the root holds nothing on any screen's behalf.

**Acceptance Scenarios**:

1. **Given** an app with a saved delay of 30 seconds, **When** its delay screen opens,
   **Then** 30 is the first value shown — no default appears and is then corrected.
2. **Given** an app with nothing saved, **When** its delay screen opens, **Then** the standard
   default is shown, by the same path and with no extra branch.
3. **Given** the root of the app, **When** its state holder is inspected, **Then** it holds only
   the launcher's pin support, and that deliberate outliving of every screen is commented with the
   behaviour requiring it.

---

### User Story 5 - The record is closed and the comments are true (Priority: P5)

Every comment that explained a mechanism this feature removes is gone or corrected in the same
change that removes it. The two findings this work resolves are ruled on and closed. The
superseded entries from the previous feature are named as superseded rather than silently left.

**Why this priority**: smallest blast radius, and best done once the code has stopped moving. A
stale comment is worse than none, because it is believed.

**Independent Test**: read every comment in each file this feature touches and confirm none
describes a mechanism that is no longer there.

**Acceptance Scenarios**:

1. **Given** any file changed by this feature, **When** its comments are read, **Then** none
   describes the removed navigation mechanism or activity-scoped holders as a current fact or as a
   reason for a current decision.
2. **Given** the two findings this feature resolves, **When** the findings record is read,
   **Then** each carries a ruling and is closed, and the findings this feature does not touch are
   still open and unchanged.
3. **Given** the runtime guidance file, **When** it is read, **Then** it names this feature's plan
   as the active one.

---

### Edge Cases

- **A user is deep in the flow when the process is killed.** The screen they were on, and the
  choices carried into it, come back. This is behaviour features 003 and 005 already specify, now
  delivered by the library's own saved history rather than by a hand-written serialiser — so the
  restore must be verified on device for every step, not assumed.
- **The launcher stops supporting pinning while the user is mid-flow.** The gate takes over the
  whole app exactly as it does today, and the user's place returns when support does.
- **A back press arrives on the first screen.** It leaves the app, as feature 005 requires. The
  graph must not leave an unpoppable entry behind it.
- **The user leaves the app list and comes back.** It opens fresh. This is a change from today, it
  is what the amended Principle II requires, and no earlier specification says otherwise — 001, 002,
  003 and 005 all scope their retention requirement to the round trip through the flow, which is
  preserved.
- **A newly pinned lock is not on the list in the first frame after the flow returns.** Accepted in
  principle and verified on device; if visible, the recorded fallback is taken rather than a bespoke
  wait being reintroduced.
- **Adopting a graph makes deep links available.** None is added. The scope boundary is fixed by
  the constitution, and the accepted limitation that deep links bypass the launcher is settled.
- **The entry point a pinned shortcut opens looks like it belongs in the graph.** It does not. It
  has a frozen identity, its own task affinity and one screen, and routing it through the graph
  would put a frozen value at risk for nothing.
- **A defect is found while working.** It is written down with what it breaks and what a fix would
  change for a user, and nothing is fixed until the maintainer confirms that specific fix.
- **A constitution rule cannot be satisfied without changing what a user sees.** Beyond the three
  differences approved in FR-002, the rule is not applied; the conflict is recorded with the
  rejected alternative, and the maintainer decides.

## Requirements *(mandatory)*

### Functional Requirements

#### Behaviour preservation, and the three approved differences

- **FR-001**: Except as FR-002 allows, this feature MUST NOT change any user-visible behaviour: no
  screen, layout, wording, interaction, ordering or timing may differ from the pre-change build.
- **FR-002**: Exactly three user-visible differences are approved in advance. Each MUST be verified
  on device, and any difference beyond these three is a defect, not an acceptable cost:
  - **(a)** Leaving the app list entirely and opening it again presents it fresh — no retained
    search text, scrolled to the top. Retention across the round trip through the flow is unchanged.
  - **(b)** The lock list MAY be one frame behind when the flow returns home after a lock is
    created, where today an explicit wait guaranteed otherwise.
  - **(c)** The delay screen MAY withhold its editable value for the duration of the configuration
    read, where today the read completed before the screen appeared. It MUST NOT show a default and
    then correct itself, which is a defect under FR-013.
- **FR-003**: Every value already persisted on a user's device — preference file names, key names,
  stored value formats and the identifiers they are stored under — MUST survive byte-identical.
- **FR-004**: Every shortcut already pinned MUST keep working: the entity it targets MUST remain
  resolvable under the exact identity recorded at pin time, and its payload MUST still be read under
  the same name.
- **FR-005**: No user-facing string may be added, removed or reworded.
- **FR-006**: No capability is added or removed, and the scope boundary the constitution fixes is
  unchanged.

#### Navigation

- **FR-007**: Screen navigation MUST be provided by the first-party Jetpack navigation library, at
  the artifact and release line recorded in the plan under Clarifications.
- **FR-008**: The hand-written screen-state type, its bundle serialiser, the enumeration recording
  where the flow was entered from, the hand-managed per-screen retention holder and its keys, and
  the helpers that dropped retained screens by hand MUST all be removed. None may be replaced by an
  equivalent written in this project.
- **FR-009**: Destination arguments MUST travel as compiler-checked route types, not as a
  hand-written encoding.
- **FR-010**: The system back gesture MUST be handled by the navigation library. Per-screen
  interception of it MUST be removed, and each screen's on-screen back control MUST resolve to the
  same navigation action, so the two cannot drift.
- **FR-011**: The four screens of the configuration app MUST each be a destination: the root screen,
  the app list, the delay step, and the icon step. The choice between the intro and the lock list
  stays inside the root destination and stays derived from the lock list — it MUST NOT become a
  destination of its own, and no "has been introduced" flag may be introduced.
- **FR-012**: The launcher-cannot-pin state MUST remain a gate above the graph rather than a
  destination, MUST preserve its current behaviour exactly, and MUST carry a comment naming why it
  is not a destination.
- **FR-013**: Every navigation behaviour the earlier features specify MUST hold: back from the icon
  step returns the delay chosen on the way through; back from the delay step returns to whichever
  screen the flow was entered from; the app list's scroll position and search query survive the
  round trip; rotation and process death return the user to the step they were on; back on either
  root screen leaves the app.
- **FR-014**: No destination may declare a deep link, and the entry point a pinned shortcut opens
  MUST NOT be routed through the graph.

#### State holders and their scope

- **FR-015**: Every screen's state holder MUST be scoped to that screen's navigation entry: it lives
  while the entry is on the back stack and is cleared when the entry is popped. No holder may be
  scoped to the activity by default.
- **FR-016**: State that must outlive every screen MUST be deliberate and commented, naming the
  behaviour that requires it. After this feature the only such state is the launcher's pin support.
- **FR-017**: The mechanism that carries state through process death MUST NOT be used to carry state
  across separate visits to a screen. It remains what survives process death within one visit.
- **FR-018**: The icon treatment selection MUST be owned by the icon step's state holder, and MUST
  survive rotation and process death while being discarded when the flow is left. It MUST NOT
  remain in composition-scoped saveable state.
- **FR-019**: The delay step MUST have its own state holder, scoped to its entry, owning the delay
  being edited, the resolution of the app being configured, and its icon.
- **FR-020**: The root state holder MUST NOT expose a data source on another screen's behalf. The
  two it exposes today MUST be removed from it, along with the parameters that carried them into the
  delay screen.
- **FR-021**: A state holder MUST NOT be introduced that only forwards. Where a screen genuinely
  owns nothing and decides nothing, it stays without one.

#### Dependencies

- **FR-022**: Every version MUST be declared in the single central catalog; none may be hardcoded at
  the point of use.
- **FR-023**: Every version added MUST be a stable release on a maintained line, verified against
  the published artifacts at plan time rather than from recall.
- **FR-024**: Every dependency added MUST be recorded in the plan with what breaks without it.
- **FR-025**: No dependency may be added that introduces a runtime the project lacks — no network
  stack, database engine or analytics runtime. Doing so would be an amendment, not a plan decision.

#### Persistence

- **FR-026**: The stored per-app configuration and the stored lock order MUST NOT change mechanism,
  file name, key shape or value format in this feature. The reasoning MUST be recorded so the
  question is not reopened without new information.

#### Comments and the record

- **FR-027**: A comment made false by a change in this feature MUST be corrected or deleted in that
  same change. This covers, at minimum: the file-level explanation of the removed navigation
  mechanism; the note declaring that the navigation stage deliberately stays outside a holder; the
  argument that a holder here would be activity-scoped; the declaration that the delay screen owns
  no state; the note attaching back handling to each screen; and the catalog entry declaring that
  this project does not want the navigation runtime.
- **FR-028**: The two findings this feature resolves MUST be ruled on and closed in the findings
  record, stating that they are resolved by construction rather than by a fix. Findings this feature
  does not touch MUST be left open and unchanged.
- **FR-029**: The previous feature's plan MUST NOT be rewritten. This feature's plan MUST record
  which of its Complexity Tracking entries are superseded and by what.
- **FR-030**: The runtime guidance file MUST be repointed at this feature's plan.

#### Defect handling

- **FR-031**: A defect discovered while working MUST be recorded — what it is, what it breaks, and
  what fixing it would change for a user — and MUST NOT be fixed before the maintainer confirms that
  specific fix.
- **FR-032**: An unconfirmed defect MUST be preserved exactly as it is, with the finding left open.
- **FR-033**: A step MUST NOT be reported as behaviour-preserving if it also contains a fix.

#### Tests

- **FR-034**: No automated test may assert that the navigation library moves between destinations,
  retains an entry, or restores a history. That is framework behaviour, which the constitution
  prohibits testing.
- **FR-035**: Every area the constitution mandates coverage for MUST still be covered, including
  each frozen persisted value asserted against a literal.
- **FR-036**: Automated coverage MUST be added only where this feature introduces branching a test
  could get wrong. A holder that only forwards gets none.
- **FR-037**: The two written verification checks that the findings record flags as matching things
  they were not written to match MUST be corrected or removed; the one that scans for a mechanism
  this feature deletes MUST be removed.

#### Process and verification

- **FR-038**: The work MUST be divided into steps that each leave the project building and its
  automated checks passing.
- **FR-039**: The project's build and its automated test suite MUST both pass before the work is
  reported complete.
- **FR-040**: Behaviour observable only on a running device MUST be captured in a written, numbered
  manual test plan traceable to these requirements, and MUST be verified by the maintainer. No
  automated test may drive a device, and no agent may drive the maintainer's device to pre-verify a
  manual case.
- **FR-041**: The manual plan MUST include, at minimum: both routes out of the delay step; process
  death on each destination; the round trip retention and the fresh re-entry of FR-002(a); the
  treatment discard and the treatment restore; FR-002(b); FR-002(c); back on the root screen leaving
  the app; and pin support lost and regained.
- **FR-042**: Each fallback recorded in Clarifications MUST be taken only on evidence from the
  device, and MUST be recorded as a deviation if taken.
- **FR-043**: Every deviation from a constitution rule that survives MUST be recorded with the
  simpler alternative named and the reason it was rejected.
- **FR-044**: The agent MUST NOT commit, push, branch, merge, rebase or tag. Completed work is left
  in the working tree and offered to the maintainer.

### Key Entities

- **Destination**: one screen the user can be on, addressed by a route. The configuration app has
  four.
- **Route**: a destination's identity together with the arguments it needs — the app being
  configured, the delay chosen so far, the treatment chosen so far. Compiler-checked, and what
  replaces the hand-written screen-state type.
- **Navigation entry**: one occupancy of a destination in the history. It is the lifetime a state
  holder is scoped to: created when the destination is opened, cleared when it is popped. Two visits
  to the same destination are two entries and share nothing.
- **Frozen value**: any name or format already written into a launcher or onto a user's disk by a
  shipped build — the pinned entry point's identity, the preference file names, the stored key
  names, the shortcut identifier scheme, the intent extra names. Unchanged by this feature, and
  constraining what may move.
- **Approved behaviour difference**: one of the three differences FR-002 permits. Each is verified
  on device; anything else a user can see is a defect.
- **Recorded finding**: a defect discovered while working, held open until the maintainer rules.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the manual test cases in the plans of features 001-005 and 007 pass on the
  post-change build, with no case needing its expected result amended other than the three
  differences FR-002 approves.
- **SC-002**: A device carrying locks and pinned shortcuts, updated in place, retains 100% of its
  locks with their delays and treatments, and 100% of its pinned icons still launch their targets.
- **SC-003**: Zero user-facing strings differ between the pre- and post-change builds.
- **SC-004**: Zero user-visible differences ship beyond the three FR-002 names, and each of those
  three is verified on device and recorded as verified.
- **SC-005**: Zero application files decide which screen is showing; zero maintain a screen history
  by hand; zero intercept the system back gesture per screen.
- **SC-006**: 100% of screen state holders are scoped to a navigation entry. Exactly one piece of
  state outlives every screen, and it carries a comment naming the behaviour requiring it.
- **SC-007**: Configuring one app, abandoning the flow, and configuring a second app shows the
  second app's saved treatment on 100% of attempts — the abandoned choice carries over zero times.
- **SC-008**: Opening the delay screen for a configured app shows the saved value as the first value
  rendered on 100% of attempts; a default is shown and then corrected zero times.
- **SC-009**: 100% of the navigation behaviours the earlier features specify (FR-013) pass their
  manual cases.
- **SC-010**: Every reviewable step builds and passes its automated checks — zero steps end with a
  failing build or a failing suite.
- **SC-011**: 100% of added versions are stable releases on a maintained line, verified against the
  published artifacts on the day, and 100% are declared in the central catalog.
- **SC-012**: Zero comments describing a removed mechanism survive in any file this feature touches.
- **SC-013**: Zero behaviour fixes ship without a recorded maintainer confirmation naming that
  specific fix.
- **SC-014**: Every constitution rule is either satisfied or carries a recorded, justified deviation
  — zero unrecorded violations remain, and the two findings this feature resolves are closed.

## Assumptions

- The constitution at version 4.0.0, dated 2026-08-27, is the authority for every rule referenced
  here. If it is amended mid-flight, work in progress is re-checked before completion.
- Approving this specification approves the three user-visible differences FR-002 names, and nothing
  else. Any further difference found on device is a finding.
- "Application code" means the Android application source and its build configuration. The marketing
  site, the README and the specification history are out of scope.
- The project stays a single application module, at unchanged compile and target platform levels.
  Only library and build-plugin versions move.
- Feature 009's package arrangement, injection mechanism, dispatcher seams, repository interfaces
  and one-shot event mechanism are correct under version 4.0.0 and are not revisited. This feature
  changes navigation, holder scope and the two workarounds that depended on the old scope.
- Adopting the navigation library and the serialization plugin are plan-level dependency decisions,
  not amendments: neither introduces a network stack, database engine or analytics runtime.
- No new capability and no new permission is introduced.
- All device verification is performed by the maintainer against a written plan. The agent produces
  the plan and waits.
- Version control stays with the maintainer: the agent leaves changes uncommitted and offers them.
