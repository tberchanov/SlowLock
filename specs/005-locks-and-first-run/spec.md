# Feature Specification: Locks Home & First Run (Phase 2)

**Feature Branch**: `005-locks-and-first-run`

**Created**: 2026-08-24

**Status**: Draft

**Input**: User description: "Need to implement the new screens from canvas according to the new
flow. Use the claude_design MCP to import this project: `SlowLock Redesign.dc.html` (+ `support.js`).
Implement: `SlowLock Redesign.dc.html`"

---

## Overview

Feature 004 restyled the five screens the app already had and drew a hard line around two artboards
it refused to build, because they are **new behaviour, not styling**:

> "Requires a persisted 'has been introduced' flag and a fourth root state." — 004 Out of Scope
>
> "Requires enumerating configured apps and deciding what a lock *is* once its shortcut has been
> removed from the home screen — a question 003 deliberately left unanswered because the launcher
> cannot be queried." — 004 Out of Scope

This feature answers those questions and builds the two screens, which changes the shape of the
app: SlowLock stops being a three-step wizard that opens on a list of every installed app, and
becomes a **home for the locks you have made**, from which that wizard is entered.

Three consequences follow, and they are the whole feature:

1. **A new root.** "Locks" replaces the app list as what the user sees on launch.
2. **The wizard becomes a wizard.** Because step 1 now has a predecessor, the app list gains a back
   control and all three steps gain the `1 / 3`, `2 / 3`, `3 / 3` counters the canvas draws — both
   of which 004 rejected precisely because the count "is a claim the app cannot honour" until Locks
   exists.
3. **The app learns what locks exist.** The one genuinely new capability, and the one place this
   feature can go wrong.

Nothing about the wait, the pinned shortcut, or the hand-off to the target app changes.

---

## Terminology

Adopted in 004 and binding here:

- **Lock** — the user's object: an app, plus how long it makes them wait, plus how its icon looks.
  This is the noun in every user-visible string.
- **Shortcut** — the Android mechanism only. Never shown to the user.

---

## Design Source

Seven artboards in `SlowLock Redesign.dc.html` (Claude Design project
`4fe7e35d-2bb5-4814-b99e-4ce3107bdbb0`, artboard group `1a`). Five were built by 004. This feature
builds the remaining two and adds the flow controls the other artboards carry:

| Artboard | Status |
|---|---|
| **New · First run** | **Built here** |
| **New · Locks** | **Built here** |
| New · App list | Built by 004 — gains the back control and `1 / 3` here |
| New · Delay | Built by 004 — gains `2 / 3` here |
| New · Icon | Built by 004 — gains `3 / 3` here |
| New · Wait | Built by 004 — **untouched** |
| New · Pin unsupported | Built by 004 — **untouched** |

The artboard group `2a` ("Launcher icon ideas") is five explicitly-unfinished sketches and remains
deferred with no phase assigned, exactly as 004 recorded it.

---

## Clarifications

### Session 2026-08-24

The first three were put to the maintainer directly; the answers below are theirs, not defaults.

- Q: What makes a lock exist, given the launcher cannot be queried? → A: **A lock exists from the
  moment the user taps "Add to home screen"** and stops existing only when the user removes it in
  SlowLock. Whether the icon is actually on the home screen is unknowable and is never claimed.
- Q: How is that recorded? → A: **A durable, ordered list of package names**, kept alongside the
  existing per-app configuration rather than replacing it. The configuration store keeps its frozen
  keys and gains no enumeration capability.
- Q: What does the Locks screen show when there are none? → A: **The first-run screen is the empty
  state.** No "has been introduced" flag is persisted; "have you been introduced" and "do you have
  any locks" are the same question, answered from the lock list.
- Q: What does tapping a lock row do? → A: **Re-enters the wizard on step 2** (delay), carrying the
  lock's saved delay and icon treatment, and finishing by re-pinning. Editing is the same three-step
  flow, entered one step in.
- Q: How is a lock removed? → A: **Long-press a row → a confirmation** that says plainly that the
  home-screen icon must be removed by the user, because SlowLock cannot remove it.
- Q: The canvas subtitle reads "3 ON YOUR HOME SCREEN". Is that shipped verbatim? → A: **No.** It
  asserts something the app cannot verify (Constitution I, honesty about bypass paths). The subtitle
  states the count only.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The app states its idea once, to someone who has never used it (Priority: P1)

Someone installs SlowLock and opens it. Instead of an undifferentiated list of every app on their
phone — which explains nothing and asks them to guess what tapping a row will do — they get one
screen that says what the app is for and one button that starts. Tapping it drops them into the
flow they already had.

**Why this priority**: It is the only story that can ship alone and still be worth shipping. It is
also the lowest-risk half of the feature: one new screen, one new root state, and no new durable
data beyond what US2 introduces.

**Independent Test**: Install on a device with no SlowLock data. The intro appears; "Set up a lock"
opens the app list; the whole 004 flow works unchanged from there.

**Acceptance Scenarios**:

1. **Given** a fresh install with no locks, **When** the user opens SlowLock, **Then** the intro
   screen is shown, not the app list.
2. **Given** the intro screen, **When** the user taps "Set up a lock", **Then** the app list opens.
3. **Given** the intro screen, **When** the device is rotated or the process is killed and restored,
   **Then** the intro screen is still what is shown.
4. **Given** the intro screen, **When** the user presses system back, **Then** SlowLock exits — the
   intro is a root, not a step.

---

### User Story 2 - The locks I made have somewhere to live (Priority: P1)

A user who has created three locks opens SlowLock. Today they are shown the same list of every
installed app they saw the first time, with no trace of the work they did. After this story they
see their three locks — each with its app, its delay, and how its icon was treated — and one button
to make another.

**Why this priority**: It is the reason the feature exists, and it is what turns the wizard into a
flow with a home. It is P1 alongside US1 because the intro is the empty state of this screen: the
two are one decision about what the app opens on.

**Independent Test**: Create two locks through the existing flow, leave the app, and reopen it. Both
appear with the right app, delay and treatment. Create a third; it appears too.

**Acceptance Scenarios**:

1. **Given** a user with one or more locks, **When** they open SlowLock, **Then** the Locks screen
   is shown with one row per lock.
2. **Given** the Locks screen, **When** the user reads a row, **Then** it shows that app's icon, its
   current name, its saved delay, and its saved icon treatment.
3. **Given** the Locks screen, **When** the user taps "+ New lock", **Then** the app list opens.
4. **Given** a user who completes the flow for an app that already has a lock, **When** they return
   to Locks, **Then** there is still exactly one row for that app, showing the new values.
5. **Given** a user who completes the flow for an app with no lock, **When** they return to Locks,
   **Then** a new row is present.
6. **Given** the last lock is removed, **When** the user is returned to Locks, **Then** the intro
   screen is shown in its place.
7. **Given** a lock whose app has since been uninstalled, **When** the Locks screen is shown,
   **Then** the row does not appear as a working lock and the screen does not crash.

---

### User Story 3 - Getting out of the wizard, and knowing where I am in it (Priority: P2)

A user taps "+ New lock", lands on the app list, and changes their mind. Today the only way out is
the system back gesture, because in 004 the list *was* the root and a back control would have
duplicated it. Now there is somewhere to go back to, so the control the canvas draws is drawn — and
each of the three steps says which step it is.

**Why this priority**: It completes the canvas but delivers nothing on its own; it is only
meaningful once US1 and US2 have given step 1 a predecessor. Shipped after them, it is a small,
self-contained polish increment.

**Independent Test**: From Locks, tap "+ New lock", then the back control on the app list — Locks
is shown. Walk all three steps and confirm the counter reads `1 / 3`, `2 / 3`, `3 / 3`.

**Acceptance Scenarios**:

1. **Given** the app list opened from Locks, **When** the user taps the back control, **Then**
   Locks is shown.
2. **Given** the app list, **When** it is displayed, **Then** the step counter reads `1 / 3`.
3. **Given** the delay screen, **When** it is displayed, **Then** the step counter reads `2 / 3`.
4. **Given** the icon screen, **When** it is displayed, **Then** the step counter reads `3 / 3`.
5. **Given** any of the three steps, **When** the user presses system back, **Then** it does exactly
   what the on-screen back control does.

---

### User Story 4 - Changing a lock I already made (Priority: P2)

A user set Messages to ten seconds a week ago and it is no longer enough. They open SlowLock, tap
the Messages row, and land on the delay screen with ten already showing. They drag to thirty,
continue, keep the icon treatment they had, and add it to the home screen again.

**Why this priority**: Without it a lock is a read-only receipt, and the only way to change a delay
is to walk the full flow from the app list and hope you find the same app. It is P2 rather than P1
because the flow it reuses already exists in full — this story only supplies the entry point.

**Independent Test**: Create a lock at 10s. From Locks, tap it, change to 30s, finish. The row reads
30s, and there is still only one row for that app.

**Acceptance Scenarios**:

1. **Given** the Locks screen, **When** the user taps a lock row, **Then** the delay screen opens
   showing that lock's saved delay.
2. **Given** the delay screen entered from a lock row, **When** the user continues, **Then** the
   icon screen opens with that lock's saved treatment selected.
3. **Given** the delay screen entered from a lock row, **When** the user goes back, **Then** Locks
   is shown — not the app list, which was never in this path.
4. **Given** an edit in progress, **When** the user abandons it with back, **Then** the lock keeps
   the values it had.
5. **Given** a lock is edited and finished, **When** Locks is shown, **Then** the row shows the new
   values and the list order is unchanged.

---

### User Story 5 - Removing a lock (Priority: P3)

A user no longer wants the Gallery lock. They long-press its row, confirm, and it leaves the list.
The confirmation tells them plainly that the icon on their home screen is theirs to remove —
SlowLock put it there but cannot take it back.

**Why this priority**: A list that only grows is a defect, but it is the last one that hurts. It is
also the story with the most honesty risk: removing the row while an amber-labelled icon still sits
on the home screen, launching into a wait, is the one outcome that would make the app feel broken —
so the wording is the deliverable, not the deletion.

**Independent Test**: Create two locks, long-press one, confirm. It leaves the list; the other
remains. Tapping the removed lock's still-present home-screen icon still waits and still opens the
app.

**Acceptance Scenarios**:

1. **Given** the Locks screen, **When** the user long-presses a row, **Then** a confirmation is
   shown naming the app and stating that the home-screen icon must be removed by the user.
2. **Given** the confirmation, **When** the user confirms, **Then** the row leaves the list.
3. **Given** the confirmation, **When** the user dismisses it, **Then** nothing changes.
4. **Given** a removed lock, **When** its home-screen icon is tapped, **Then** it still waits and
   still opens the app (Constitution I — nothing is enforced, and this is an accepted limitation).
5. **Given** the last lock is removed, **Then** the intro screen is shown.

---

### Edge Cases

- **An app is uninstalled while it has a lock.** The row cannot show an icon or a name. It MUST NOT
  crash the screen and MUST NOT show a blank row that behaves like a working lock (FR-020).
- **An app is renamed by an update, or the device language changes.** Rows show the app's *current*
  label, resolved fresh. No label is ever stored (Constitution V).
- **The user creates a lock for an app that already has one.** One row, updated in place — not two
  (FR-013).
- **The user declines the launcher's pin dialog.** No lock is created (FR-003a, FR-011a). The app
  never has to be told the outcome, because it never asks: nothing was pinned, so nothing is
  derived.
- **The user removes the home-screen icon by dragging it off.** **The lock goes with it**, on the
  next foreground entry (FR-003a). The shortcut IDs are package names, so the pinned set is the
  lock list.
- **A launcher that does not unpin on icon removal.** Some do not. The shortcut keeps reporting as
  pinned, so the lock stays and the user removes it in SlowLock by hand. This is the harmless
  failure direction and is why FR-011 still holds.
- **A very long app label, at the largest system font scale.** Rows truncate the label; the delay
  and the treatment stay legible.
- **The last lock is removed, or all locked apps are uninstalled.** The app returns to the intro.
- **Locks exist from a build before this feature.** They are recovered, because they were never
  lost: the shortcuts are still pinned and pinned is what a lock means (FR-003a, FR-024). Their
  delay and treatment come from the configuration store those users already have.
- **The launcher stops supporting pinning while locks exist.** The unsupported-launcher screen still
  takes over the whole root, ahead of Locks and the intro alike (FR-025).
- **The device is rotated, or the process dies, mid-flow.** The user returns to the step they were
  on, with the values they had chosen — including on the two new screens.

---

## Requirements *(mandatory)*

### Functional Requirements

#### The lock list — what a lock is

- **FR-001**: The system MUST keep a durable record of which locks exist, independent of which apps
  happen to have a saved configuration.
- **FR-002**: A lock MUST be identified by the target app's package name and nothing else. No label,
  no activity name, no component name (Constitution V).
- **FR-003**: *(superseded by FR-003a.)* A lock came into existence at the moment the user
  completed the flow, independent of the launcher dialog's outcome. That made a declined dialog
  leave a lock with no icon, and the row appeared behind the still-open dialog — which is what
  FR-003a replaces it with.
- **FR-003a**: **A lock exists exactly when its shortcut is pinned and the user has not removed it
  in SlowLock.** The list MUST be derived from `ShortcutManager.getPinnedShortcuts()` rather than
  from a record written when the user tapped "Add to home screen". Consequences, all of them
  required behaviour:
  - Accepting the launcher's dialog creates the lock; **declining it creates nothing.**
  - Removing the icon from the home screen removes the lock.
  - A launcher that pins without reporting back still produces a lock, because nothing waits on a
    report. This is why the pin request's `IntentSender` is **not** the mechanism: that callback
    fires on success and is silent on cancel, so it can confirm a pin but never deny one.
  - Completing the flow MUST NOT write a lock record. The only write on that path clears any
    tombstone left by a previous in-app removal (FR-021a).
- **FR-004**: A lock MUST stop existing only when its shortcut stops being pinned, or when the
  user removes it in SlowLock (FR-021). Nothing else — not an uninstall, not a failed resolution,
  not a launcher change — may remove a lock from the list.
- **FR-004a**: A failure to read the pinned shortcuts MUST be distinguished from an empty result.
  "We could not ask" is not "the launcher holds none": on a failed read the app MUST fall back to
  the last derived list rather than showing an empty screen. The stored list is a cache and an
  ordering record (FR-006), never the definition of what a lock is.
- **FR-005**: The record MUST hold no copy of the delay or the icon treatment. Those live in the
  existing configuration store, and a lock's displayed values MUST be read from there so the two
  cannot disagree.
- **FR-006**: The order of the list MUST be stable across launches and MUST NOT reorder itself when
  a lock is edited.
- **FR-007**: Reading the record MUST sanitise rather than fail: an absent, empty, or malformed
  record reads as "no locks", and an entry naming an app that cannot be resolved is handled by
  FR-020. Nothing on this path may throw.
- **FR-008**: The configuration store's persisted keys, file name, and value formats MUST NOT
  change (004 FR-038, `002`/`003` frozen contracts).

#### The Locks screen

- **FR-009**: When at least one lock exists, SlowLock MUST open on the Locks screen.
- **FR-010**: The screen MUST show a title, a count of locks, and one row per lock.
- **FR-011**: The count MUST NOT claim the locks are on the home screen. Deriving from the pinned
  set (FR-003a) gets much closer than a written record did, but "pinned" is still not "visible on a
  home screen": a launcher that does not unpin on icon removal keeps reporting a shortcut the user
  deleted. Constitution I forbids asserting coverage the mechanism does not have.
- **FR-011a**: A lock whose pin dialog the user declined MUST NOT exist (FR-003a). Nothing is
  pinned, so nothing is derived — and because no record was written at the tap, there is nothing
  left behind to clean up either.
- **FR-012**: Each row MUST show the target app's icon, its current label, its saved delay, and its
  saved icon treatment.
- **FR-013**: Completing the flow for an app that already has a lock MUST update that lock in place.
  There MUST NOT be two rows for one package.
- **FR-014**: The screen MUST offer one primary action that opens the app list to create a new lock.
- **FR-015**: The screen MUST remain usable while icons and labels are still resolving, and MUST NOT
  block on that resolution.
- **FR-016**: Rows MUST reflect a change made in the flow by the time the user is returned to the
  screen — no stale delay, no stale treatment.

#### The intro screen

- **FR-017**: When no locks exist, SlowLock MUST open on the intro screen instead of the Locks
  screen.
- **FR-018**: The intro MUST state what the app does in plain language, and MUST NOT overstate it —
  it MUST say that nothing is blocked and nothing is counted.
- **FR-019**: The intro MUST offer one action, which opens the app list.
- **FR-019a**: No "has been introduced" flag may be persisted. Whether the intro is shown MUST be
  derived from the lock list alone.

#### Locks whose app is gone

- **FR-020**: A lock whose package cannot be resolved MUST be shown in a distinct, clearly
  unavailable state that names what is wrong and offers removal. It MUST NOT be silently hidden —
  the user's home screen may still carry its icon — and it MUST NOT be tappable into the edit flow.

#### Editing and removing

- **FR-021**: *(revised.)* SlowLock MUST NOT offer to remove a lock. A lock is its pinned shortcut
  (FR-003a) and Android offers no way to unpin one, so the only real removal is the user taking the
  icon off their home screen — and the list follows on the next foreground entry. What the Locks
  screen offers instead is an **explanation**, reachable from any row, saying exactly that.
  - An in-app "Remove" that only hid the row would be worse than nothing: the icon would stay,
    still waiting and still opening the app, while the list stopped meaning what it says.
  - It would also need a tombstone record to keep the row hidden from the next derivation — a
    persistence layer built to fake a capability the platform does not have. There MUST be no such
    record.
- **FR-022**: The explanation MUST name the app, MUST say that removing the icon from the home
  screen is what removes the lock, and MUST say the user does that themselves. It MUST NOT imply
  SlowLock can remove the icon, MUST NOT imply anything happens when the dialog is dismissed, and
  MUST NOT suggest the app is uninstalled. It carries exactly one button.
- **FR-023**: Users MUST be able to tap a lock to change its delay and its icon treatment, entering
  the existing flow at the delay step with that lock's saved values, and leaving it the same way a
  newly created lock does.
- **FR-023a**: Abandoning an edit MUST leave the lock exactly as it was — no partial write.

#### Upgrades and the existing app

- **FR-024**: A user upgrading from a build without this feature MUST see **their existing locks**,
  and their already-pinned icons MUST continue to work unchanged. This reverses the original
  requirement, which said they would see the intro because the app had never recorded those locks —
  under FR-003a there is nothing to record: the pinned shortcuts *are* the locks, and they were
  always there. The delay and treatment come from the configuration store, which those users
  already have. A user with no pinned icons still sees the intro.
- **FR-024a**: The system MUST NOT reconstruct locks from stored configurations. A configuration
  exists for any app whose flow was walked; a lock exists only where a shortcut is pinned. Deriving
  from the pinned set is not reconstruction — nothing is guessed, and every entry is something the
  launcher named.
- **FR-025**: The unsupported-launcher screen MUST continue to take over the entire root, ahead of
  both new screens (002 FR-029, 003 FR-004).
- **FR-026**: Nothing a pinned shortcut carries, and no class it targets, may change (002
  `contracts/pinned-shortcut.md`).
- **FR-027**: The wait screen MUST NOT be touched by this feature in any respect.

#### The flow

- **FR-028**: The app list MUST show a back control that returns to whichever screen opened it.
- **FR-029**: Each of the three flow steps MUST show its position as `1 / 3`, `2 / 3`, `3 / 3`.
- **FR-030**: System back MUST do exactly what the on-screen back control does, on every screen that
  has one.
- **FR-031**: System back on either root screen MUST leave the app.
- **FR-032**: All existing flow guarantees MUST survive: the delay chosen on the way through is what
  a back from the icon step returns to (003 FR-014); the app list's scroll position and search query
  survive the round trip (003 FR-011); rotation and process death return the user to the step they
  were on (003 FR-008).

#### Visual conformance

- **FR-033**: Both new screens MUST be built from the frozen tokens in
  `004/contracts/design-tokens.md`. The palette MUST remain closed at eleven colours; a twelfth is a
  build failure, not a decision.
- **FR-034**: Accent-coloured text MUST use the dark accent token, never the accent itself (004 C2).
- **FR-035**: Both new screens MUST use the existing type roles and their fixed families. New sizes
  required by the new screens MUST be added to the type scale; no existing size, weight, or
  letter-spacing may change.
- **FR-036**: Stylistically capitalised strings MUST be stored capitalised in the string resource.
  No case transformation may run on user-visible text (004 C8).
- **FR-037**: Both new screens MUST render in the light palette regardless of the system setting,
  consistent with the four screens 004 pinned to light. A dark palette remains Phase 3.
- **FR-038**: Every user-visible string MUST be a string resource. No literal text in screen code.

#### Non-functional

- **FR-039**: No new permission and no new third-party dependency (Constitution II, III).
- **FR-040**: All disk reads and writes, package enumeration, and icon rasterization MUST stay off
  the main thread (Constitution IV).
- **FR-041**: Every list row and every new control MUST carry the selection, action, and label
  semantics assistive technology needs, including the long-press removal affordance, which MUST also
  be reachable without a long press.
- **FR-042**: The ten existing unit test files MUST keep passing unmodified — the eight listed in
  `004/contracts/screen-inventory.md` §S6, plus `DelayRangeTest` (003) and `SlowLockPaletteTest`
  (004).

### Key Entities

- **Lock** — the user's object: a target app, its delay, and its icon treatment. Its identity is the
  package name. Its existence is recorded by this feature; its values are the existing configuration
  record, unchanged.
- **Lock list** — the ordered set of packages the user has made a lock for. The only genuinely new
  durable data in the feature, and the answer to "what does the app open on".
- **Root state** — which top-level screen is showing: unsupported-launcher, intro, Locks, or the
  flow. The intro and Locks are **one** state answering to the lock list, not two: "have you been
  introduced" and "do you have any locks" are the same question (FR-019a), so an implementation is
  free to model them as a single root that renders either.
- **Flow entry point** — where the wizard was entered from, which decides where its back control
  goes and whether it starts at step 1 or step 2.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time user, having never seen the app, can state what SlowLock does after
  reading one screen, and reaches the app list in one tap.
- **SC-002**: A returning user with locks sees them on the first screen, with no tap and no search.
- **SC-003**: Changing an existing lock's delay takes at most three taps and one drag from launch;
  today it takes finding the app in a list of every installed app.
- **SC-004**: Every lock a user creates appears in the list, and no lock appears twice, across at
  least 20 create-and-edit operations.
- **SC-005**: Locks survive being force-stopped, the device being restarted, and the app being
  backgrounded for a day.
- **SC-006**: 100% of locks display the target app's current label and icon, including after that
  app is updated or the device language is changed.
- **SC-007**: Both new screens are visually indistinguishable from their artboards at default font
  scale on a 412×892 viewport, judged against the canvas side by side.
- **SC-008**: Both new screens remain fully usable at the largest system font scale on the smallest
  supported screen: no clipped text, no control pushed off screen, no overlap.
- **SC-009**: The colour palette still contains exactly eleven values, enforced by test.
- **SC-010**: Every text pairing on the new screens measures at least 4.5:1, enforced by test.
- **SC-011**: A screen-reader user can read every lock's app, delay and treatment, and can reach
  the removal explanation, without a long press.
- **SC-012**: The user is told, in the app, that removing a lock means removing its home-screen
  icon and that only they can do it — verified by reading the explanation, not by inference. The
  app never claims it can remove an icon.
- **SC-013**: The app's cost at rest is unchanged: no service, no polling, no wake lock.
- **SC-014**: Zero change to any persisted delay or treatment value, verified by creating a lock on
  the previous build and reading it on this one.

---

## Assumptions

- **Feature 004 is merged and its tokens are frozen.** This feature builds on `004/contracts/design-tokens.md`
  and does not redefine anything in it. 004's nine open tasks are all maintainer-run manual
  verification; they gate 004's own release, not this specification.
- **The canvas is the visual authority for the two new screens**, and its content is
  authoritative except where it asserts something the app cannot know — the "ON YOUR HOME SCREEN"
  subtitle — which is corrected by FR-011.
- **The canvas draws no empty state for Locks**, because the intro screen is it. This is the
  reading adopted in the Clarifications, and it is what removes the need for a persisted flag.
- **The canvas draws no edit or remove affordance**, because it draws states, not gestures. Both are
  specified here from the flow the canvas implies (a home for existing locks), not invented beyond
  it.
- **The canvas app list still shows no back control in the `2a` sketches** — the back tile is drawn
  on the `1a` "New · App list" artboard and is adopted as drawn.
- **Locks are per-user-profile and per-install.** No backup, no sync, no export. Nothing in the app
  today crosses a device boundary and this feature does not start.
- **The lock list will stay small** — tens, not thousands. The screen is a plain scrolling list with
  no search, no filter, and no paging.
- **Android cannot report whether a pinned shortcut is still on the home screen**, cannot remove
  one, and does not reliably report the outcome of the pin dialog across OEMs. Every honesty
  requirement in this spec (FR-003, FR-011, FR-022, SC-012) follows from that and not from a
  preference.
- **The launcher icon remains the one the app ships today.** Choosing among the five `2a` sketches
  is still a separate, unassigned feature.

---

## Out of Scope

This section is binding. A reviewer should be able to reject work that lands anything below.

### Deferred to Phase 3

| Deferred item | Why |
|---|---|
| **Dark palette** | Unchanged from 004: every artboard is light, and the dark ramp waits for the light build to be seen on a device. The wait screen keeps its existing light/dark pair. |

### Deferred, phase not yet assigned

| Deferred item | Why |
|---|---|
| **The launcher icon** | Five unfinished sketches; a branding decision, not a build. `res/mipmap-*` and `res/drawable/ic_launcher_*` MUST NOT be touched. |

### Out of scope permanently (this feature)

- **No re-pin from the Locks screen without walking the flow.** A "pin again" shortcut on the row is
  a plausible future affordance and is not this feature.
- **No reordering, grouping, favouriting, searching or filtering of locks.**
- **No per-lock enable/disable.** A lock that should not fire is removed.
- **No bulk operations.**
- **No statistics of any kind** — no counts of how often a lock fired, how long was waited, or
  whether the user backed out. The intro screen promises "nothing is counted" and that is a
  product commitment, not copy.
- **No change to the wait screen**, its window, its activity, its timing, or its colours.
- **No change to what a pinned shortcut carries or which class it targets.**
- **No change to how apps are enumerated, how icons are rasterized or cached, or how the hand-off to
  the target app works.**
- **No change to any frozen persisted key, file name, or token.**
- **No new permission, no new third-party dependency, no network access.**
- **No recovery of locks created before this feature.**

---

## Dependencies

- **004-visual-redesign** — the design tokens, type scale, and shared components this feature builds
  its two new screens from. Hard dependency.
- **003-launch-delay** — the delay step, the configuration store, and the wait. Consumed unchanged.
- **002-shortcut-pinning** — the pin gate, the icon treatments, and the frozen pinned-shortcut
  contract. Consumed unchanged.
- **001-installed-apps-list** — the app list and its selection hand-off. Gains a back control only.
