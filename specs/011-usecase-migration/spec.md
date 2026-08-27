# Feature Specification: Use Cases Hold the Logic

**Feature Branch**: `main` (no feature branch; Principle VII reserves branch creation to the maintainer)

**Created**: 2026-08-27

**Status**: Draft

**Input**: User description: "The pure framework independent logic should be delegated to usecases. The repositories implementation should be just data read-write. Filtering, sorting, merging multiple data sources, that is all should be inside usecases."

## Overview

Constitution v5.0.0 redefined where logic lives. This feature moves the code to match it.

Nothing a user can see changes. No screen, no wording, no timing, no persisted value. The whole
deliverable is that every rule the app applies — which apps appear and in what order, what the lock
list is, when a wait ends, what a search box narrows to — moves out of the repository
implementations, screen holders and UI state classes it is currently spread across, into use cases
that hold it and can be tested without a framework.

Today the project has **zero** use cases. The pure logic already exists as free functions in
`domain` (`assembleLocks`, `deriveLocks`, `sortedByLabel`, `resolveTarget`, `deadlineFrom`), which
is why this is a relocation rather than a rewrite: what has to move is the *calling* — the code that
picks the sources, orders the calls and chooses between the answers.

## User Scenarios & Testing *(mandatory)*

The user here is the maintainer and the next reader. Every story below is invisible from the app,
which is the point: each is verified by the app behaving exactly as it did before.

### User Story 1 - A repository implementation reads and writes, and nothing else (Priority: P1)

Opening any file under a `data` package answers one question — where does this value come from, and
how does it get back — and never a second one about which entries survive, in what order, or what
happens when two sources disagree.

**Why this priority**: it is the half of the principle the maintainer stated first, it is where the
two clearest defects sit (an app list that filters, dedupes and sorts inside its own source; a lock
store that merges the launcher's answer with its own record and writes the result back), and it can
ship alone.

**Independent Test**: read `InstalledAppsSource` and `LockOrderStore`; each holds a read, a write,
and the decoding between the stored form and the domain type — nothing else. The app list and the
Locks screen look identical on device.

**Acceptance Scenarios**:

1. **Given** a device with several apps exposing more than one launcher activity, **When** the app
   list is opened, **Then** each app appears exactly once, SlowLock itself is absent, and the order
   is the same locale-collated order as before — with the exclusion, the collapse and the ordering
   performed outside the source that enumerated them.
2. **Given** a stored lock order and a launcher reporting a different pinned set, **When** the Locks
   screen loads, **Then** the reconciled list is identical to the current behaviour — known packages
   keep position, new ones append sorted — with the reconciliation performed outside the store, and
   the store still the only writer of the order.
3. **Given** a launcher that cannot be asked (no answer, not an empty answer), **When** the Locks
   screen loads, **Then** the last known good list is shown and nothing is pruned, exactly as today.

---

### User Story 2 - A screen holder wires, it does not decide (Priority: P2)

A `ViewModel` reads as a list of calls and a mapping to state. The decision about which source wins,
what a missing answer means, or what order two writes must happen in is not in it.

**Why this priority**: it is the larger half by volume and it carries the app's two most
behaviour-critical rules — when a wait ends, and the order of write-then-pin — so it is worth
shipping after the safer relocation in Story 1 has proven the parity approach.

**Independent Test**: each affected holder's remaining body is repository or use-case calls and
`_uiState.update`. The manual test plan passes unchanged.

**Acceptance Scenarios**:

1. **Given** a lock list with one uninstalled package, **When** the Locks screen loads, **Then** the
   dead package is still a row with a null label and its saved delay, in its original position —
   with the four-source assembly performed by a use case rather than in the holder.
2. **Given** a shortcut tap for an app configured with a delay, **When** the wait screen is shown
   and the process is killed and restored mid-wait, **Then** the hand-off happens at the original
   deadline and not one delay later — the anchor-and-deadline rule having moved without changing.
3. **Given** the icon step with a treatment chosen, **When** Create is tapped, **Then** the
   configuration is written before the pin request is issued, as today, and a package uninstalled
   while the screen sat open produces the same message and no write.
4. **Given** a delay edited and the process killed before returning, **When** the delay screen is
   restored, **Then** the edited value wins over the value on disk, as today.

---

### User Story 3 - The search filter is a rule, not a view (Priority: P3)

Narrowing the app list by what the user typed is a rule about which apps match, and lives with the
other rules rather than as a computed property on the screen's state object.

**Why this priority**: it is one property and one behaviour, it is the smallest slice, and it is the
one the principle reaches only on a literal reading — so it is separable if the maintainer wants it
deferred.

**Independent Test**: `AppListUiState` holds fields and the display-state booleans, and no filtering.
Typing in the search box behaves identically.

**Acceptance Scenarios**:

1. **Given** the app list and a query, **When** the query matches part of a label in any case,
   **Then** the same rows appear as today — substring, case-insensitive, original order preserved —
   and clearing the query restores the full list without re-sorting.
2. **Given** a query matching nothing, **When** the list renders, **Then** the same
   no-results state appears as today, and the empty-list state is still distinct from it.

---

### Edge Cases

- **A frozen persisted value is touched.** Four values are frozen (`slowlock.locks` and its key and
  separator, and the delay-configuration file and key shapes). Any change to what is written or how
  it is read empties a user's list or resets their delays silently. Nothing in this feature may
  reach them.
- **A repository that must not read another still needs a second value.** `ShortcutPinner` currently
  reads two other repositories (pin support, and the icon it bakes the treatment into) before
  pinning. Moving those reads out is required, but the icon is a bitmap, and passing it through the
  repository interface would put a platform type across the domain boundary that the interface was
  deliberately shaped to keep out. Which side gives is a plan-level decision (see Assumptions).
- **A relocation that would create a forwarding-only use case.** Where a holder makes one repository
  call and reacts to it in presentation terms — resolving a tapped package and dropping a dead row —
  there is no rule to move, and Principle V prohibits wrapping it. Such sites stay as they are.
- **A caching repository.** `AppIconCache` chooses between a memory tier, a file tier and
  rasterization. That is how it reads, not a rule a requirement states, and it stays.
- **Behaviour that only looks like a rule.** Whether a screen is loading, empty, or showing results,
  and whether a button is enabled, are presentation shape and stay where they are.

## Requirements *(mandatory)*

### Functional Requirements

**What must move**

- **FR-001**: The enumeration, self-exclusion, per-package collapse and locale-collated ordering of
  installed apps MUST be performed outside the repository implementation that enumerates them, with
  the observable list unchanged.
- **FR-002**: The reconciliation of the stored lock order against the launcher's pinned set MUST be
  performed outside the store that holds the order, with the derived list and the conditional
  write-back unchanged in effect.
- **FR-003**: The choice between the stored order and the derived order when the launcher gives no
  answer MUST be performed outside both the store and the screen holder.
- **FR-004**: The assembly of lock rows from the package list, the saved configuration and the
  resolved target MUST be performed outside the screen holder.
- **FR-005**: The wait decision — resolve the target, establish the deadline once, and determine
  what remains — MUST be performed outside the screen holder, preserving the rule that a restored
  deadline wins over a freshly computed one.
- **FR-006**: The create sequence on the icon step — re-resolve, write the configuration, then
  request the pin, in that order — MUST be performed outside the screen holder, with the ordering
  preserved as a stated obligation rather than an incidental line order.
- **FR-007**: The delay screen's rule that an edited value wins over the saved value MUST be
  performed outside the screen holder.
- **FR-008**: The narrowing of the app list by the search query MUST be performed outside the UI
  state class, preserving substring matching, case-insensitivity, and the original ordering.
- **FR-009**: A repository implementation MUST NOT read another repository. Where one does today,
  the reads move to the caller.

**What must not move**

- **FR-010**: Decoding a stored or platform representation into a domain type, and encoding one back,
  MUST remain with the repository implementation that owns that source — including whatever the
  format itself requires to yield a well-formed value.
- **FR-011**: A site where a holder makes a single repository call carrying no rule MUST be left
  alone; no use case may be introduced that only forwards.
- **FR-012**: Presentation shape MUST stay in the holder or the UI state class: which of several
  states a screen is in, what a value reads as on screen, and whether a control is enabled.
- **FR-013**: `AppIconCache`'s tiering and sweep, `AppTargetSource`'s platform resolution, and
  `PinSupportSource`'s support read MUST remain where they are; each decodes one source.

**What must not change**

- **FR-014**: No user-visible behaviour may change. No screen, wording, ordering, timing, message,
  or navigation path differs from the current build.
- **FR-015**: The four frozen persisted values MUST survive byte-identical. An existing install's
  locks, delays and treatments MUST read back unchanged after upgrade.
- **FR-016**: No new dependency, permission, or capability may be introduced.
- **FR-017**: No file may change package, and no existing test may be deleted to accommodate a move.

**Form and verification**

- **FR-018**: Each relocated rule MUST be reachable by a JVM unit test that constructs no Android
  framework object.
- **FR-019**: Tests MUST cover behaviour at the new seams rather than restate the relocation. A test
  that would pass against a plausible wrong implementation MUST NOT be written; a test asserting a
  use case forwards to the repository it was given MUST NOT be written.
- **FR-020**: The constitution's mandated coverage MUST still hold after the move: schedule and
  time-window evaluation including boundary times and the outside-window path; target resolution
  including the null launch-intent path; and every frozen persisted value asserted against a literal.
- **FR-021**: Comments made false by a relocation MUST be corrected or deleted in the same change.
  Several currently name where logic lives — the app source's "leaves dedup, sorting and filtering as
  pure functions", the lock store's "everything decidable lives in `LockList.kt`", the locks holder's
  "this class holds only the wiring", the app list holder's "the filtering itself is derived in
  `AppListUiState.visibleApps`" — and each becomes wrong or misleading as its rule moves.
- **FR-022**: `./gradlew assembleDebug` and `./gradlew test` MUST pass before the feature is
  complete.
- **FR-023**: The feature MUST ship a numbered manual test plan traceable to these requirements,
  covering at minimum the app list, the Locks screen, the two configuration steps, the pin request,
  and a wait interrupted by process death.

### Key Entities

- **Use case**: a named rule the app applies, holding the logic and the collaborators it needs, and
  invocable without a framework. New to this project; none exist today.
- **Repository implementation**: the one route to a single source outside the process. After this
  feature it reads, writes, and translates between the source's representation and the domain type.
- **Screen holder**: owns a screen's state. After this feature it calls, maps and exposes.
- **UI state**: what a screen is currently showing. After this feature it holds values and the
  display states derived from them, and applies no rule to the data itself.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every case in the manual test plan produces an outcome indistinguishable from the
  current build — same rows, same order, same wording, same timing, same navigation.
- **SC-002**: An existing install upgraded in place finds every lock, delay and treatment exactly as
  it left them; zero locks disappear and zero delays reset.
- **SC-003**: Every rule named in FR-001 through FR-008 is exercised by a test that runs on the JVM
  with no device and no Android framework object.
- **SC-004**: A reader asking "how is the app list ordered?", "what is the lock list?", "when does
  the wait end?" or "what does the search box match?" finds each answer in exactly one file, named
  for the question.
- **SC-005**: No file under a `data` package contains a filter, a sort, a merge of two sources, or a
  read of another repository.
- **SC-006**: The count of use cases introduced equals the count of rules relocated; none exists that
  only forwards.

## Assumptions

- **Use cases are injectable classes.** Each is a class in its feature's `domain` package exposing
  `operator fun invoke`, taking its collaborators through the constructor — the form Constitution
  v5.0.0 states. The existing free functions in `domain` are not deleted; a use case calls them where
  one already holds the pure step, which is what keeps this a relocation of calling code.
- **Screen holders stop injecting repositories where a use case now covers the call**, and keep
  injecting one where the call carries no rule (FR-011). The icon repository stays exposed on the
  list holders, because rows load their own icons lazily and that is not a rule.
- **Decoding is read-write.** This is the maintainer's ruling on the boundary case: a store turning
  a raw preference value into a domain type, or a newline-joined string into a well-formed list, is
  reading. Only what a requirement states is a use case.
- **The `ShortcutPinner` boundary is deferred to the plan.** FR-009 requires its two repository reads
  to move out; whether the icon then crosses the repository interface as a platform type, or the
  interface changes shape, or the bake moves with the reads, is a plan-level design decision under
  Principle II with a real trade-off either way.
- **This is a whole-codebase migration, not an incremental one.** Principle III makes a
  non-conforming site a defect that must not receive new code, so the sites this spec names are
  migrated together rather than opportunistically.
- **No behavioural test debt is taken on.** Where a relocation creates a genuinely new branch, it is
  tested; where it moves an existing one, the existing test moves with it.
- **The three user stories are independently shippable** in priority order, each leaving the app
  fully working and constitutionally better than before, though only all three satisfy v5.0.0.
