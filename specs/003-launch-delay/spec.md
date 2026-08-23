# Feature Specification: Launch Delay

**Feature Branch**: `003-launch-delay`

**Created**: 2026-08-23

**Status**: Draft

**Input**: User description: "Lets implement the delay functionality. When user clicks on app in the apps list screen the delay configurations screen is opened. On this screen user can choose the delay in seconds. The next screen is a shortcut configuration that is already implemented. When user opens the shortcut from launcher the "Please wait" screen is opened. As the goal is to push user to realize that they open the application and improve the consciousness the waiting screen should not catch attention via standart approaches with animations. Instead this screen should be static and borring (no animations, no countdown). If user waited the required time after delay the target application is opened and the SlowLock application process is finished. So it should not be visible in the collapsed applications. Different applications may have different delays. If user selects application from the application list the previously was configured, saved data is displayed (current delay and current shortcut configuration on appropriate screen). When user changes configurations and applies them again, new configurations should replace the current (new delay is used, old shortcut is replaced with new one)."

## Why this feature is next

Features 001 and 002 built a delivery mechanism with nothing to deliver: a pinned icon that
looks like the target app and opens it instantly. This feature puts the product's entire
proposition inside that mechanism — the pause between the reach and the app.

Two things make it more than "add a timer". First, **the pause is deliberately unrewarding**.
Every convention of screen design exists to hold attention: a countdown ticks, a ring fills, a
number falls toward zero. All of that turns waiting into something to watch, and watching is
still screen time — worse, it makes the wait feel like part of the app rather than an
interruption of it. The wait screen here must be genuinely boring: nothing moves, nothing
counts, there is nothing to look at. The user is left alone with the question of why they picked
up the phone, which is the only thing the product is actually selling.

Second, **the configuration becomes durable**. Until now SlowLock has persisted nothing; the
launcher owned everything the app produced. From here on, a delay outlives the screen that set
it, has to be found again when the user comes back to change it, and has to be readable at the
moment a shortcut is tapped, months later, with SlowLock not running.

## Clarifications

### Session 2026-08-23

- Q: What control does the user get for choosing a delay, and which values may they pick? → A: A
  slider over a fixed range, with the chosen value shown as a number beside it.
- Q: If the device screen locks while the wait screen is still the top screen, is the user still
  waiting? → A: No. A wait is abandoned the moment the wait screen stops being visible, for any
  reason including the screen turning off. The next tap starts a fresh, full wait.
- Q: What happens when a shortcut whose app has no saved configuration is tapped? → A: It waits
  the default delay — the same value the delay screen opens with for an app never configured
  before — and then opens the target.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Wait before the app opens (Priority: P1) 🎯 MVP

A user taps the SlowLock icon for an app on their home screen. Instead of the app, they get a
plain, still screen that says to wait. Nothing counts down and nothing moves. After the time
they configured has passed, the app they asked for opens by itself. SlowLock is not behind it
and is not in the recents list.

**Why this priority**: This is the product. Everything built so far exists to make this moment
possible, and nothing else in this feature has value without it.

**Independent Test**: Fully testable before the configuration screen exists, using the default
delay every unconfigured shortcut falls back to (FR-032) — tap a shortcut pinned by feature 002,
observe the wait, confirm the target opens afterwards and that SlowLock leaves nothing behind.
Delivers the interruption the whole product is for.

**Acceptance Scenarios**:

1. **Given** an app with a configured delay and a pinned shortcut, **When** the user taps the
   shortcut, **Then** a full-screen wait screen appears in place of the app
2. **Given** the wait screen is showing, **When** the user watches it for the whole wait,
   **Then** nothing on it changes — no countdown, no progress, no animation, no movement of any
   kind
3. **Given** the wait screen is showing, **When** the configured delay has passed, **Then** the
   target app opens without the user doing anything
4. **Given** the target app has opened, **When** the user opens the recents list, **Then** there
   is no SlowLock entry in it
5. **Given** the target app has opened, **When** the user presses back out of it, **Then** they
   do not land on the wait screen
6. **Given** two apps configured with different delays, **When** the user taps each shortcut in
   turn, **Then** each waits its own app's delay
7. **Given** SlowLock has been force-stopped or the device rebooted, **When** the user taps a
   shortcut, **Then** the wait and the launch behave exactly as before

---

### User Story 2 - Choose how long to wait (Priority: P2)

Setting an app up now starts with deciding how long the pause should be. The user taps an app in
the list, picks a delay in seconds, moves on to the shortcut screen they already know, and
creates the icon. The delay they picked is what that icon will impose.

**Why this priority**: Without it, US1 works but every app waits the same arbitrary amount.
Different apps deserve different friction — a messenger the user genuinely needs is not
Instagram — and the user's own choice is what makes the delay feel self-imposed rather than
inflicted, which the constitution's first principle rests on.

**Independent Test**: Testable by picking an app, choosing a delay, continuing to the shortcut
screen, creating the shortcut, and confirming the shortcut waits the chosen amount and not a
default.

**Acceptance Scenarios**:

1. **Given** the installed-apps list is showing, **When** the user taps an app row, **Then** the
   delay configuration screen for that app opens — not the shortcut configuration screen
2. **Given** the delay screen is open for an app never configured before, **When** it renders,
   **Then** the slider sits at the default delay and the user can continue without touching it
3. **Given** the delay screen is open, **When** the user moves the slider, **Then** the number
   beside it updates to the value the slider is now on
4. **Given** a delay is selected, **When** the user continues, **Then** the shortcut
   configuration screen from feature 002 opens for the same app
5. **Given** the shortcut has been created, **When** the user taps it on the home screen,
   **Then** the wait lasts the delay chosen on the delay screen
6. **Given** the delay screen is open, **When** the user presses back, **Then** the
   installed-apps list returns and nothing has been saved or created

---

### User Story 3 - Change an app's settings later (Priority: P3)

The user decides ten seconds was not enough. They open SlowLock, tap the same app, and find
their current delay and their current icon treatment already selected — not blank, not reset to
defaults. They change the delay, keep the icon as it was, and apply. The icon already on their
home screen now waits the new amount; no second icon appears and they do not have to remove the
old one.

**Why this priority**: Anyone who uses this app for a week will want to tune it. Getting it
wrong means the settings screens lie to the user about the state of their own device — a delay
that reads 10 while the icon actually waits 30 is worse than no display at all. P3 because US1
and US2 are usable without it, if only once per app.

**Independent Test**: Configure an app, leave and reopen SlowLock, select the same app, and
confirm both screens show what was saved; change the delay, apply, and confirm the existing
home-screen icon now waits the new amount.

**Acceptance Scenarios**:

1. **Given** an app configured earlier, **When** the user selects it in the list, **Then** the
   delay screen opens with the saved delay selected
2. **Given** the user continues to the shortcut screen for that app, **When** it renders,
   **Then** the icon treatment saved earlier is the one selected and previewed
3. **Given** the user changes the delay and creates the shortcut again, **When** they tap the
   existing home-screen icon, **Then** it waits the new delay
4. **Given** the user creates the shortcut again, **When** they look at their home screen,
   **Then** there is still exactly one icon for that app, carrying the newly chosen treatment
5. **Given** the user changes the delay for one app, **When** they tap another app's shortcut,
   **Then** that app's delay is unaffected
6. **Given** the user opens the delay screen for a configured app and presses back without
   applying, **When** they tap that app's shortcut, **Then** the previously saved delay is still
   in force
7. **Given** the device has been rebooted or SlowLock force-stopped, **When** the user reopens a
   configured app in the list, **Then** the saved delay and treatment are still shown

---

### User Story 4 - Walk away from the wait (Priority: P4)

Halfway through the pause the user realises they did not actually want the app. They press back,
or the home button. The app does not open — then, or later.

**Why this priority**: The constitution requires every delay to be escapable, and this is the
outcome the product is hoping for: the pause worked. Small in implementation, but the feature is
not shippable without it. P4 because it is the least valuable of the four in isolation.

**Independent Test**: Tap a shortcut, press back during the wait, confirm the target never
opens and that nothing appears later.

**Acceptance Scenarios**:

1. **Given** the wait screen is showing, **When** the user presses the system back gesture or
   button, **Then** the wait ends, the target app does not open, and the user is back where they
   were
2. **Given** the wait screen is showing, **When** the user presses home, **Then** the target app
   does not open — not immediately, and not when the delay would have elapsed
3. **Given** the wait screen is showing, **When** the device screen turns off or the device
   locks, **Then** the wait is abandoned and the target app does not open, before or after
   unlocking
4. **Given** the user abandoned a wait, **When** they open the recents list, **Then** there is no
   SlowLock entry in it
5. **Given** the user abandoned a wait, **When** they tap the same shortcut again, **Then** a
   fresh full wait begins from the start

---

### Edge Cases

- **The user leaves the wait screen before the delay elapses.** The launch is abandoned. Nothing
  is scheduled, nothing fires later, and nothing appears on top of whatever the user moved on to.
- **The device screen turns off during the wait, or the device locks.** The wait is abandoned
  exactly as if the user had pressed home (FR-029). Nothing opens behind the lock screen and
  nothing opens on unlocking.
- **The user taps the same shortcut again while its wait is already running.** No second wait
  starts, and the running one is neither restarted nor extended.
- **The user taps a different app's shortcut during a wait.** The first wait is abandoned; the
  second app's wait begins.
- **A shortcut pinned by feature 002 has no saved delay.** It waits the default delay and then
  opens the target (FR-032), so an icon pinned before this feature existed gains a pause without
  the user touching it.
- **The target app is uninstalled between pinning and tapping.** The user is told the app is
  unavailable and nothing crashes — feature 002's behaviour, unchanged, and it must not be
  preceded by a pointless wait if that can be known up front.
- **The target app is uninstalled during the wait.** When the wait ends the launch fails
  gracefully: the user is told, nothing crashes.
- **The user's saved configuration is lost** (app data cleared, SlowLock reinstalled) while the
  shortcuts remain on the home screen. Every one of those icons falls back to the default delay
  (FR-032) until the user sets it up again — so a 25-second pause silently becomes the default
  one. The icons keep working; only the chosen durations are gone.
- **The user configures an app but declines the launcher's pin dialog.** The configuration is
  saved with no icon to use it — see Accepted limitations.
- **The user deletes the home-screen icon but keeps the configuration.** Setting the app up again
  shows the saved values and re-pins.
- **The screen is rotated during the wait.** The wait neither restarts nor extends.
- **The longest delay the slider allows is configured.** The wait screen must survive the whole
  of it in the foreground without the system reclaiming it. The maximum is chosen partly to make
  this safe (see Assumptions).
- **The user changes an app's delay while a wait for that app is in progress.** Not reachable in
  practice — SlowLock's own UI is not on screen during a wait — but the running wait keeps the
  value it started with.

## Requirements *(mandatory)*

### Functional Requirements

#### Entering the delay configuration screen

- **FR-001**: Tapping an app in the installed-apps list MUST open the **delay configuration
  screen** for that app. This **replaces** feature 002's FR-001, where a tap opened the shortcut
  configuration screen directly.
- **FR-002**: The delay configuration screen MUST identify its target app by package name only.
- **FR-003**: The screen MUST show which app is being configured, using the app's own icon and
  label re-resolved from the package name.
- **FR-004**: The screen MUST be reachable only where shortcut pinning is supported — feature
  002's startup gate (its FR-028 to FR-032) continues to guard the whole flow.

#### Choosing the delay

- **FR-005**: The screen MUST let the user choose a delay with a **slider** over a fixed range of
  whole seconds. Every value the slider can land on MUST be a whole number of seconds, and the
  range MUST have a minimum above zero and a maximum, both fixed by the app rather than by the
  user.
- **FR-006**: A delay MUST always be selected. The screen MUST open with the app's saved delay
  if it has one, and with a single fixed **default delay** otherwise; the user MUST never be able
  to continue with nothing chosen, and MUST be able to continue without touching the slider.
- **FR-007**: The currently chosen delay MUST be shown as a number of seconds beside the slider,
  and MUST update as the slider moves so the user always knows the exact value they are choosing
  — the slider's position alone is not enough.
- **FR-008**: The chosen delay MUST survive screen recreation, including rotation and process
  death.
- **FR-009**: The screen MUST present a "next" action that opens feature 002's shortcut
  configuration screen for the same app, carrying the chosen delay forward.
- **FR-010**: The screen MUST present a back affordance at the top, and MUST honour the system
  back gesture or button. Both MUST return to the installed-apps list, saving nothing and
  creating nothing.
- **FR-011**: Returning to the list MUST preserve the scroll position and search query the user
  left it in — feature 002's FR-022, now applying across two screens rather than one.

#### Showing what is already configured

- **FR-012**: Opening the delay screen for an app that has a saved configuration MUST pre-select
  that app's saved delay.
- **FR-013**: Opening the shortcut configuration screen for an app that has a saved configuration
  MUST pre-select and preview that app's saved icon treatment. This **narrows** feature 002's
  FR-006: Original remains the opening selection only for apps with no saved configuration.
- **FR-014**: Pressing back on the shortcut configuration screen MUST return to the delay screen
  for the same app — not to the list — with the delay the user chose on the way through still
  selected, whether or not it matches what is saved.

#### Applying a configuration

- **FR-015**: Activating "Create shortcut" MUST save the chosen delay and the chosen icon
  treatment for the target app, replacing any values previously saved for that app.
- **FR-016**: Saved configuration MUST be per app and keyed by package name alone. Configuring
  one app MUST NOT change any other app's delay or treatment.
- **FR-017**: Saved configuration MUST survive SlowLock being force-stopped, the device being
  rebooted, and SlowLock being updated.
- **FR-018**: A changed delay MUST take effect on the very next tap of the app's existing
  home-screen icon, with nothing asked of the user — no removing the icon, no re-adding it, no
  reopening SlowLock. The delay MUST therefore be read at the moment the shortcut is tapped
  rather than fixed into the shortcut when it is created.
- **FR-019**: Applying again for an app that already has an icon MUST update that icon in place
  with the newly chosen treatment and MUST NOT add a second one — feature 002's FR-026, unchanged.
- **FR-020**: Leaving either screen without activating "Create shortcut" MUST leave any
  previously saved configuration exactly as it was.

#### The wait

- **FR-021**: Tapping a pinned shortcut whose app has a saved delay MUST show a full-screen wait
  screen instead of opening the target app. This **replaces** feature 002's FR-016 (open
  immediately).
- **FR-022**: The wait screen's background MUST be on screen within 200 ms of the tap, and its
  message within 500 ms — soon enough that the user never wonders whether their tap registered,
  and stated as a number so a tester can judge it.
- **FR-023**: The wait screen MUST be visually static for the entire wait. It MUST NOT show a
  countdown, a remaining or elapsed time, a progress bar or ring, a spinner, an animation, a
  transition, a blinking or pulsing element, or anything else that changes while the user
  watches.
- **FR-024**: The wait screen MUST NOT use sound, vibration, haptics, or a notification at any
  point.
- **FR-025**: The wait screen MUST show a short, fixed message telling the user to wait, and
  nothing that varies over the course of the wait or between target apps.
- **FR-026**: The wait screen MUST NOT offer any control that shortens, skips, or ends the wait
  early, and tapping anywhere on it MUST do nothing.
- **FR-027**: A wait in progress MUST NOT be restarted or extended by anything that leaves it in
  front of the user: rotating the device or any other screen recreation MUST NOT reset it, and
  tapping the same shortcut again while its wait is running MUST NOT start a second wait,
  restart the running one, or extend it. Recreation of this kind MUST NOT count as the screen
  ceasing to be visible under FR-029.

#### Ending the wait

- **FR-028**: When the configured delay has elapsed with the wait screen visible in front of the
  user, the target app MUST open with no further action from the user.
- **FR-029**: A wait MUST be abandoned the moment its screen stops being visible, for any reason
  — the system back gesture or button, home, the app switcher, another app taking over, the
  device screen turning off, or the device locking. In every one of those cases the target app
  MUST NOT open: not at that moment, not when the delay would have elapsed, and not when the
  user returns or unlocks. Time spent away MUST NOT count towards any wait, and tapping the
  shortcut again afterwards MUST begin a fresh, full wait from the start.
- **FR-030**: The target MUST be resolved from its package name at the moment of launching, not
  at the moment the wait began. If it no longer resolves, the user MUST be told the app is
  unavailable and nothing MUST crash — feature 002's FR-018, unchanged.
- **FR-031**: Once the target has opened, or the wait has been abandoned, SlowLock MUST leave no
  visible screen of its own and no entry in the recents list, and backing out of the target app
  MUST NOT return the user to the wait screen. This carries feature 002's FR-019 across a screen
  that is now visible.
- **FR-032**: Tapping a shortcut whose app has **no** saved configuration MUST show the wait
  screen for the **default delay** of FR-006 and then open the target, behaving in every other
  respect exactly like a configured app. The default MUST be one value used in both places, so
  the pause an unconfigured icon imposes is always the one the delay screen would have offered
  for that app.
- **FR-033**: The wait and the launch MUST work with SlowLock force-stopped beforehand and after
  a device reboot, without relying on anything already running.

#### Constraints

- **FR-034**: The feature MUST NOT require any new permission and MUST NOT present any permission
  prompt.
- **FR-035**: The feature MUST NOT introduce a background service, an ongoing notification, a
  polling loop, or any power-management lock. Nothing MUST keep running once a wait has ended, in
  either outcome, and SlowLock's cost while the user is not looking at it MUST stay zero. **One
  exception, bounded**: the display MUST be kept awake for the length of a wait, so that a delay
  longer than the device's screen timeout can be completed at all (FR-029 abandons a wait when the
  screen goes off). That MUST last no longer than the wait screen is visible, and MUST end with it.
- **FR-036**: Reading and writing saved configuration MUST NOT make any screen unresponsive, and
  MUST NOT delay the wait screen's appearance (FR-022).
- **FR-037**: The target app MUST NOT open before the configured delay has elapsed, and MUST open
  promptly once it has.

### Key Entities

- **App Delay Configuration**: What SlowLock remembers about one target app — the delay in
  seconds and the chosen icon treatment, identified by the target's package name and nothing
  else. At most one per app. Created or replaced when the user applies; survives reboots,
  force-stops, and updates; independent of whether an icon for that app is currently on the home
  screen.
- **Wait**: One occurrence of the pause — a target app and the moment its delay is up. Exists
  only while the wait screen is in front of the user; nothing about it is stored, and abandoning
  it leaves no trace.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can go from the installed-apps list to a home-screen icon with a delay of
  their choosing in under 45 seconds, with no more than five taps beyond selecting the app.
- **SC-002**: Once the wait screen has settled — its message rendered, within 500 ms of the tap —
  nothing on it changes until the target app opens, on 100% of waits. Verified by comparing the
  first settled frame of a screen recording with its last. (The frames before it settles are the
  screen arriving, not the screen changing, and are excluded deliberately.)
- **SC-003**: With the user staying on the wait screen, the target app opens no earlier than the
  configured delay and no later than one second after it, on 100% of taps.
- **SC-004**: After the target app opens, SlowLock has no entry in the recents list on 100% of
  launches, on every launcher tested.
- **SC-005**: Leaving the wait screen before the delay elapses results in the target app not
  opening, on 100% of attempts — back, home, the app switcher, and the screen turning off all
  measured separately.
- **SC-006**: Reopening a previously configured app shows the saved delay and the saved icon
  treatment on 100% of attempts, including after a reboot and after a force-stop.
- **SC-007**: A changed delay is in force on the first tap of the existing home-screen icon after
  applying, on 100% of changes, with no icon removed or re-added by the user.
- **SC-008**: Three apps configured with three different delays each wait their own, on 100% of
  taps.
- **SC-009**: Zero permission dialogs are shown at any point in the flow.
- **SC-010**: SlowLock runs no process, service, or scheduled work outside an active wait,
  measured as zero SlowLock battery attribution over 24 hours of normal use, excluding the display
  time of the waits themselves.
- **SC-011**: A shortcut with no saved configuration waits the same duration the delay screen
  offers by default, then opens its target, on 100% of taps.

## Accepted limitations

Consequences of decisions taken deliberately. These are **not defects** and must not be filed as
bugs during manual testing:

- **The user cannot tell how much longer they have to wait.** That is the point — a countdown is
  something to watch, and watching is the behaviour the product is interrupting. Some users will
  read the still screen as frozen and leave. That outcome is a success for the product thesis,
  not a bug.
- **Every catalogued bypass path stays open.** The original app icon, recents, deep links,
  launcher search, and removing the shortcut all still open the target with no pause
  (`highlevel_spec.md` §5). The delay covers exactly one entry point: the SlowLock icon.
- **A configuration can exist for an app with no icon.** The settings are saved when the pin is
  requested, and SlowLock cannot observe whether the launcher honoured it or the user declined.
  A declined pin leaves a dormant configuration, which is harmless and is reused if the user sets
  the app up again.
- **Applying is still silent.** Feature 002's FR-012 stands: the screen closes and says nothing,
  so changing an existing app's delay produces no visible confirmation anywhere.
- **The display stays lit for the whole wait, and dark screens stay dark.** The wait keeps the
  screen awake so a long delay can be completed (FR-035), which means the longest pause the
  slider offers is half a minute of lit screen the user chose. It also means the only way the display goes off mid-wait
  is the power button — a deliberate act, which abandons the wait like any other departure.
- **A wait cannot be paused, and cannot survive the screen turning off.** A phone call, a
  notification the user taps, or the display timing out all abandon it, and the user starts over
  on their next tap. This is the cost of the single rule in FR-029, chosen so that no route
  exists to start a wait, put the phone down, and come back to a loaded app.
- **Losing SlowLock's data silently rewrites every delay to the default.** Clearing app data or
  reinstalling leaves the icons in place and working, so nothing looks wrong, but a carefully
  chosen 30 seconds becomes the default until the user notices and sets it again.
- **An unconfigured icon imposes a delay nobody chose.** Shortcuts pinned by feature 002 start
  pausing after this feature ships, for a duration their owner never agreed to. This is
  deliberate — an icon that says SlowLock and does nothing is worse — but it is a change in
  behaviour arriving with an update rather than with a user action.
- **The delay must be re-applied through the shortcut screen.** There is no way to change only
  the delay; the user passes through the shortcut screen and presses "Create shortcut" again,
  which is what commits both values.

## Assumptions

Reasonable defaults chosen where the description did not specify:

- **The slider's range, step, and default are 1 to 30 seconds, in steps of 1, defaulting to 10.**
  The clarification fixed the control, not the numbers. **Revised during implementation** from
  5 to 120 in steps of 5 — the original values were provisional and this is what "cheap to
  change" bought: one edit to `DelayRange`, two test literals, and the documents that quoted them.
  The current values are chosen so the shortest delay is a token pause the user can pick
  deliberately rather than a minimum imposed on them, the longest keeps the lit screen to half a
  minute and the wait screen well clear of being reclaimed, and the one-second step makes every
  whole second reachable so the readout is exact rather than rounded. They remain cheap to change
  and are not depended on by anything else in this spec — except that the default is a single
  value shared with FR-032, and that **one second being reachable is what makes the readout's
  plural forms load-bearing**.
- **Configuration is saved at "Create shortcut", not on the way through.** Applying is one
  commit, and it commits both the delay and the treatment. This matches the description's "apply
  them again" and keeps a user who backs out from silently changing their existing setup (FR-020).
- **The wait screen names nothing.** It shows the same short line for every app, without the
  target's name or icon. A screen that names the app the user is craving is a screen that
  re-triggers the craving, and an identical screen every time is less interesting than one that
  varies.
- **The wait screen is plain and quiet by design** — no branding, no illustration, no colour that
  draws the eye. "Boring" is a requirement, not an aesthetic shortfall, and reviewers should not
  ask for it to be made more engaging.
- **The wait screen follows the device's light and dark setting.** A screen whose whole purpose is
  not to grab attention must not be a full-brightness white field at night — and the rest of the
  app already follows the system setting, so a wait screen that did not would be the one place
  SlowLock ignored it.
- **Time windows and schedules are not part of this feature.** The description asks only for a
  delay in seconds. The delay applies to every tap of the shortcut, at any hour. This deliberately
  builds less than the constitution's v1 scope boundary describes; schedules can be added later
  without changing anything specified here.
- **No route exists to a target app that skips the wait from inside SlowLock.** The app list is
  for configuring, not for launching — feature 001's interim tap-to-launch was already replaced
  by feature 002.
- **The installed-apps list does not mark which apps are configured.** Nothing in the description
  asks for it, and the list is currently a plain list of every launchable app. This is worth
  revisiting once several apps are typically configured.
- **The delay is not carried inside the shortcut** (FR-018). It is looked up by package name when
  the shortcut is tapped, which is what lets an existing icon pick up a changed delay and keeps
  feature 002's frozen shortcut shape untouched — no re-pinning, no migration.
- **Two constitutional points need recording in the plan, not resolving here.** First, the
  constitution's scope boundary and testing expectations both name a *countdown* `DelayActivity`;
  this feature deliberately has no countdown, and the plan should note that the wait screen is
  that clause's subject under a different presentation, rather than treat the wording as
  binding. Second, feature 002 waived the instrumented test for the hand-off "for this feature
  only" and stated it returns in full with the delay feature — so instrumented coverage of the
  wait and the hand-off is expected here and MUST NOT inherit that waiver.

## Dependencies

- **Feature 001 (`001-installed-apps-list`)** — the list this flow starts from, its
  `onAppSelected(packageName)` seam, and its icon cache.
- **Feature 002 (`002-shortcut-pinning`)** — the shortcut configuration screen this flow passes
  through, the pin-support gate that guards it, and the frozen pinned-shortcut contract
  (`specs/002-shortcut-pinning/contracts/pinned-shortcut.md`). This feature is the change that
  contract was frozen to allow: it alters what happens after a shortcut is tapped while leaving
  every persisted value in the shortcut untouched, so shortcuts already on users' home screens
  gain the delay with nothing asked of them (feature 002's FR-011).
- Feature 002's spec, manual test plan, and shortcut-screen behaviour need amending where this
  feature replaces them: its FR-001 (what a list tap opens), FR-006 (opening icon treatment),
  and FR-016 (tapping a shortcut opens the app immediately).

## Out of Scope

Explicitly not built here, and to be rejected if proposed during implementation:

- Time windows, schedules, weekday rules, and any notion of the delay applying only sometimes
- Any countdown, progress indicator, remaining-time display, or animation on the wait screen
- A skip, "open anyway", or "I really need this" control
- Delaying any entry point other than the SlowLock shortcut — recents, deep links, notifications,
  and launcher search stay uncovered
- A screen that lists configured apps, or removing a configuration from inside SlowLock
- Changing the delay without passing through the shortcut configuration screen
- Marking configured apps in the installed-apps list
- Statistics of any kind — how often a wait was abandoned, how long apps are used, streaks
- Escalating, randomised, or adaptive delays
