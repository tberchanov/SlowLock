# Feature Specification: Pinned Shortcut Creation

**Feature Branch**: `002-shortcut-pinning`

**Created**: 2026-08-23

**Status**: Draft

**Input**: User description: "Pinned shortcut creation with icon modification (MVP draft, to be polished later). Purpose: prove the shortcut-pinning mechanism works end to end before building configuration around it. Tapping an app in the existing installed-apps list opens a shortcut configuration screen showing a centred live preview of the shortcut, a horizontally scrollable row of icon-modification buttons (Original, Invert, Gray), a back button at the top, and a 'Create shortcut' button at the bottom. Creating pins the shortcut with the selected modification and closes the screen. Tapping the pinned shortcut opens the target app immediately — draft launch behaviour to be enhanced later with countdown and schedule."

## Why this feature is next

This is a **feasibility draft**, deliberately built before the delay-configuration screen it
will eventually sit inside. Shortcut pinning is the least predictable surface in the whole
product: whether a pin request is honoured varies by launcher and by OEM, and the entire product
thesis — a home-screen icon that looks like the target app but interposes friction — is
worthless if pinning does not work reliably. Proving it now costs a few days; discovering it
after the configuration UI is built around it costs a rewrite.

Everything here is expected to be revisited. What must **not** be revisited is the shape of
the pinned shortcut itself, because shortcuts the user pins during this draft stay on their
home screen forever (see FR-011).

## Clarifications

### Session 2026-08-23

- Q: What identifies a pinned shortcut, given that re-pinning an existing ID updates it rather than adding a second icon? → A: The package name. One app has at most one shortcut; pinning again updates it in place.
- Q: What feedback does the app give after "Create shortcut" is pressed? → A: None. The screen closes and any confirmation is left to the launcher.
- Q: What happens on a device where the launcher does not support pinning? → A: Check at app start; if unsupported, show a short explanation screen with actions the user can take, in place of the app list.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Pin a shortcut that opens the chosen app (Priority: P1) 🎯 MVP

A user browsing their installed apps picks one, sees how its shortcut will look, presses
"Create shortcut", and finds a new icon on their home screen. Tapping that icon opens the
app.

**Why this priority**: This is the feasibility proof and the whole point of the feature. It
is the first time the product does something that persists outside its own UI. Without it,
nothing else in v1 has a delivery mechanism.

**Independent Test**: Fully testable on its own with the icon treatments omitted — pick an
app, create a shortcut with the unmodified icon, confirm it appears on the home screen and
opens the right app. Delivers a working home-screen launcher entry.

**Acceptance Scenarios**:

1. **Given** the installed-apps list is showing, **When** the user taps an app row, **Then**
   the shortcut configuration screen for that app opens instead of the app launching
2. **Given** the configuration screen is open, **When** it renders, **Then** a preview of the
   shortcut is displayed showing the target app's icon and its label
3. **Given** the configuration screen is open, **When** the user presses "Create shortcut",
   **Then** the launcher is asked to pin a shortcut carrying the previewed icon and label
4. **Given** the pin request has been issued, **When** it completes, **Then** the
   configuration screen closes and the user is back on the installed-apps list
5. **Given** a shortcut has been pinned, **When** the user taps it on the home screen,
   **Then** the target app opens immediately, with no countdown and no intermediate screen
6. **Given** a shortcut has been pinned, **When** the user taps it after SlowLock has been
   force-stopped or the device rebooted, **Then** the target app still opens

---

### User Story 2 - Choose how the shortcut icon looks (Priority: P2)

Before creating the shortcut, the user tries out different treatments of the app's icon so
the home-screen entry is visually distinguishable from the original app icon sitting
elsewhere on their launcher.

**Why this priority**: A shortcut that is pixel-identical to the target app's own icon is
confusing — the user cannot tell which one interposes friction. The treatments make the
distinction visible. It is P2 rather than P1 because US1 is a complete, useful feature with
the unmodified icon alone.

**Independent Test**: Testable by opening the configuration screen and cycling the
treatments, confirming the preview changes each time and that the created shortcut carries
whichever treatment was showing.

**Acceptance Scenarios**:

1. **Given** the configuration screen is open, **When** it renders, **Then** a horizontally
   scrollable row of icon treatments is shown above the preview, offering exactly Original,
   Invert, and Gray
2. **Given** the configuration screen has just opened, **When** the user has chosen nothing,
   **Then** Original is selected and the preview shows the app's unmodified icon
3. **Given** the treatment row is showing, **When** the user taps "Invert", **Then** the
   preview immediately shows the icon with its colours inverted
4. **Given** the treatment row is showing, **When** the user taps "Gray", **Then** the
   preview immediately shows the icon desaturated to greyscale
5. **Given** a treatment is selected, **When** the user presses "Create shortcut", **Then**
   the pinned shortcut carries exactly the icon shown in the preview
6. **Given** a treatment is selected, **When** the screen is recreated (for example by
   rotation), **Then** the same treatment is still selected and previewed

---

### User Story 3 - Back out without creating anything (Priority: P3)

A user who opens the configuration screen by mistake, or changes their mind, returns to the
list without a shortcut being created.

**Why this priority**: A screen with no exit other than committing is a trap. Small, but the
feature is not shippable without it. P3 because it is the least valuable of the three on its
own.

**Independent Test**: Open the configuration screen, press back, confirm the list returns and
no new home-screen icon exists.

**Acceptance Scenarios**:

1. **Given** the configuration screen is open, **When** the user presses the back affordance
   at the top of the screen, **Then** the installed-apps list returns and no shortcut is
   created
2. **Given** the configuration screen is open, **When** the user uses the system back
   gesture or button, **Then** the same thing happens
3. **Given** the user returns to the list, **When** the list renders, **Then** their previous
   scroll position and search query are still in place

---

### Edge Cases

- **The current launcher does not support pinning at all.** Some launchers, and some
  enterprise-managed devices, do not accept pin requests. The app says so on a dedicated screen
  shown instead of the list, rather than letting the user reach controls that cannot work.
- **The user switches to a launcher that does support pinning.** Returning to the app moves them
  on to the list without a restart.
- **The user declines the system pin dialog.** No shortcut is created; the app carries on
  normally. This is a legitimate choice, not an error.
- **The launcher accepts the request without showing a dialog.** Some OEM launchers pin
  silently. The app shows nothing either way, so the flow completes without the user waiting on
  a confirmation that never arrives — but also without any signal that it worked. See Accepted
  limitations.
- **The target app is uninstalled between opening the configuration screen and pressing
  Create.** No shortcut is created and the user is told why.
- **The target app is uninstalled after its shortcut is pinned.** Tapping the orphaned
  shortcut must not crash; the user is told the app is gone.
- **The target app's icon cannot be loaded.** The preview falls back to a neutral placeholder and
  creation is blocked with an explanation — never a crash, and never a placeholder icon pinned to the
  home screen. See Assumptions for why blocking was chosen over pinning the placeholder.
- **An app that already has a shortcut is pinned again.** The existing shortcut is updated in
  place with the newly chosen icon; no second home-screen entry appears. Because the app shows
  no confirmation of its own (FR-012), an in-place update can be entirely invisible to the user.
  See Accepted limitations.
- **The app has a very long label.** The preview must show the label the way the launcher
  will, without distorting the preview.
- **The target app updates and changes its icon.** The already-pinned shortcut keeps the icon
  captured at pin time. This is an accepted limitation, not a defect.
- **The user removes the pinned shortcut from their home screen.** Accepted — per the
  constitution, shortcut removal is a catalogued bypass path and part of the design.

## Requirements *(mandatory)*

### Functional Requirements

#### Entering the screen

- **FR-001**: Tapping an app in the installed-apps list MUST open the shortcut configuration
  screen for that app. This **replaces** the interim behaviour from feature 001, where a tap
  launched the target app directly. Reachable only where pinning is supported (FR-028).
- **FR-002**: The configuration screen MUST identify its target app by package name only.

#### When pinning is unsupported

- **FR-028**: The app MUST determine whether the current launcher accepts pin requests when it
  starts, and again whenever it returns to the foreground — the user may switch launcher while
  the app is away, and must not have to restart it.
- **FR-029**: Where pinning is unsupported, the app MUST show an explanation screen **in place
  of** the installed-apps list. Neither the list nor the configuration screen MUST be reachable,
  because every action they offer is dead on such a device.
- **FR-030**: The explanation MUST be short and plain — what is wrong and what it means for
  them, in a sentence or two, with no technical vocabulary and no error codes.
- **FR-031**: The explanation screen MUST offer actions the user can actually take: a way to
  reach the system setting where the default launcher is changed, and a way to re-check without
  restarting the app.
- **FR-032**: Where pinning becomes supported (the user switches launcher and returns), the app
  MUST proceed to the installed-apps list without needing a restart.

#### The preview

- **FR-003**: The configuration screen MUST display, centred, a preview of the shortcut as it
  will appear on the home screen, showing the target app's icon and the target app's label.
- **FR-004**: The preview MUST reflect the currently selected icon treatment at all times.

#### Icon treatments

- **FR-005**: The screen MUST present a horizontally scrollable row of icon treatments,
  positioned above the preview, containing exactly three options: Original, Invert, and Gray.
- **FR-006**: Original MUST be the selection when the screen first opens.
- **FR-007**: Selecting a treatment MUST update the preview without a perceptible delay and
  without the user taking any further action.
- **FR-008**: The selected treatment MUST survive screen recreation.

#### Creating the shortcut

- **FR-009**: The screen MUST present a "Create shortcut" action at the bottom of the screen.
- **FR-010**: Activating it MUST ask the launcher to pin a shortcut carrying exactly the icon
  and label shown in the preview.
- **FR-011**: A pinned shortcut MUST remain upgradeable: when the delay and schedule feature
  later ships, shortcuts pinned by this draft MUST gain that behaviour **without the user
  re-creating them**. A shortcut whose behaviour is fixed at pin time is not acceptable, because
  the user cannot be asked to re-pin every icon on their home screen.
- **FR-012**: Once the pin request has been issued, the configuration screen MUST close and
  return the user to the installed-apps list. The app MUST NOT present a confirmation of its
  own — any confirmation is the launcher's to give. This keeps the app from claiming success
  for a request the user may be about to decline (FR-014), at the cost noted under Accepted
  limitations.
- **FR-013**: Every pin attempt MUST be gated on the launcher actually accepting pin requests.
  The primary gate is at startup (FR-028 to FR-032), which means the configuration screen is
  normally only reachable on a device where pinning works; the check MUST still guard the pin
  itself, since support can change while the screen is open.
- **FR-014**: If the user declines the launcher's confirmation, the app MUST continue to work
  normally with no shortcut created and no error presented.
- **FR-015**: If the target app no longer resolves when "Create shortcut" is activated, no
  shortcut MUST be created and the user MUST be told.

#### Shortcut identity

- **FR-025**: A pinned shortcut MUST be identified by the package name of its target app and by
  nothing else. An app therefore has at most one shortcut at a time.
- **FR-026**: Activating "Create shortcut" for an app that already has a pinned shortcut MUST
  update that shortcut in place with the newly selected icon, rather than creating a second
  home-screen entry.
- **FR-027**: Identifying shortcuts this way MUST NOT require the app to remember which apps
  have been pinned. Re-pinning is idempotent because the identifier is derived from the target,
  not tracked.

#### The pinned shortcut's behaviour

- **FR-016**: Tapping a pinned shortcut MUST open its target app immediately — no countdown,
  no schedule check, no intermediate screen. *(Draft behaviour; the delay arrives with its own
  spec.)*
- **FR-017**: A pinned shortcut MUST keep working after SlowLock has been force-stopped and
  after the device has been rebooted.
- **FR-018**: If a pinned shortcut's target app no longer resolves when tapped, the app MUST
  NOT crash and MUST tell the user the app is unavailable.
- **FR-019**: Tapping a pinned shortcut MUST NOT leave a visible SlowLock screen behind or
  add a SlowLock entry to the recents list.

#### Leaving without creating

- **FR-020**: The screen MUST present a back affordance at the top, which returns to the
  installed-apps list without creating a shortcut.
- **FR-021**: The system back gesture or button MUST do the same.
- **FR-022**: Returning to the list MUST preserve the scroll position and search query the
  user left it in.

#### Constraints

- **FR-023**: The feature MUST NOT require any new permission, and MUST NOT present any
  permission prompt.
- **FR-024**: Icon loading and image processing MUST NOT make the screen unresponsive.

### Key Entities

- **Shortcut Draft**: What the user is composing on the configuration screen before pinning.
  Holds the target's package name, the label to display, and the chosen icon treatment. Exists
  only while the screen is open; nothing is stored if the user backs out.
- **Icon Treatment**: A named visual transformation applied to the target app's icon. Exactly
  three in this feature — Original (no change), Invert (colours inverted), Gray (desaturated).
- **Pinned Shortcut**: The home-screen entry the launcher owns once pinned. Identified by the
  target app's package name, so an app has at most one. Carries a rendered icon, a label, and
  enough information to identify the target app by package name and to route the tap back
  through SlowLock.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can go from the installed-apps list to a shortcut on their home screen in
  no more than three taps beyond selecting the app, and in under 30 seconds.
- **SC-002**: The pinned shortcut opens the correct target app on 100% of taps, on every
  launcher tested.
- **SC-003**: The icon that lands on the home screen matches the icon shown in the preview on
  100% of creations.
- **SC-004**: Switching between icon treatments updates the preview in under 100 ms, with no
  visible flicker or layout shift.
- **SC-005**: Zero permission dialogs are shown at any point in the flow.
- **SC-006**: Declining or dismissing the launcher's pin confirmation leaves the app usable
  with no crash, on 100% of attempts.
- **SC-007**: A shortcut pinned before a device reboot still opens its target app after the
  reboot, and after SlowLock has been force-stopped.
- **SC-008**: Pinning succeeds on at least two launchers from different vendors, or every
  failure is recorded with the launcher that produced it.
- **SC-009**: On a device where pinning is unsupported, the user is told why on the first screen
  they see, and can never reach a control that cannot work.

## Accepted limitations

Consequences of decisions taken deliberately for this draft. These are **not defects** and must
not be filed as bugs during manual testing:

- **Pressing "Create shortcut" can produce no observable feedback whatsoever.** On a launcher
  that pins silently, or when an existing shortcut is updated in place, the user sees the screen
  close and nothing else. They may reasonably conclude nothing happened, or go looking for a new
  icon that was never added. This is the accepted cost of leaving confirmation to the launcher
  (FR-012); a later polish pass can report the real outcome once the draft has established how
  launchers actually behave.
- **A declined pin and a successful pin are indistinguishable from inside the app.** Both end
  with the screen closing and nothing said.

## Assumptions

Reasonable defaults chosen where the description did not specify:

- **Feature 001's tap-to-launch behaviour is replaced, not preserved.** Feature 001's FR-009
  and FR-018 ("tapping a row opens the app immediately") were explicitly an interim feasibility
  proof handed off through `contracts/selection-handoff.md`. This feature is that contract's
  intended consumer. Feature 001's spec, manual test plan (T1.12, T1.16), and the launch code in
  its host activity will need updating as part of this work.
- **The unsupported-launcher screen offers two actions**: opening the system setting where the
  default launcher is chosen, and re-checking. Re-checking is largely covered by the foreground
  re-check (FR-028); an explicit control is offered because a screen that only explains, with
  nothing to press, reads as a dead end.
- **Confirmation is the launcher's job, not the app's.** The launcher already shows its own
  dialog on most devices, so a second in-app message would be redundant there, and the draft
  would rather under-promise than announce a success it cannot verify.
- **The shortcut label mirrors the target app's label exactly**, with no suffix or marker. The
  icon treatment is what distinguishes the shortcut visually.
- **One shortcut per app.** Pinning an app that already has a shortcut updates the existing
  icon instead of adding a second entry (FR-025 to FR-027). This keeps the shortcut, the target
  app, and the future per-app delay configuration in a single relationship, and matches
  Constitution V, which makes the package name the only persisted identifier for a target app.
  It also means nothing has to be remembered: the identifier is derived from the target, so
  re-pinning is naturally idempotent.
- **An icon that cannot be loaded blocks creation rather than pinning a placeholder.** The edge case
  above permits either branch; blocking was chosen because a pinned shortcut is effectively permanent,
  so a generic placeholder icon on the home screen is worse than no shortcut and defeats the point of an
  icon that mirrors the target app. Icon failures are transient, so reopening the screen retries.
- **Nothing is persisted by this feature.** The shortcut draft lives only while the screen is
  open; once pinned, the launcher owns the shortcut. Per-app delay settings, and any store to
  hold them, belong to the configuration feature.
- **Treatments apply to the fully rendered icon**, including any adaptive-icon background,
  rather than to a layer of it.
- **The preview approximates the launcher's presentation** — icon plus label at roughly
  home-screen proportions. Matching a specific launcher's icon masking, shape, or font is out of
  scope for a draft.
- **An already-pinned shortcut cannot be edited or removed from inside SlowLock.** Removal is
  done from the launcher, which the constitution already treats as an accepted bypass path.
- **The three treatments are fixed for this feature.** Adding more, or letting the user pick
  arbitrary colours, is out of scope.

## Dependencies

- **Feature 001 (`001-installed-apps-list`)** must be in place: this feature consumes its
  `onAppSelected(packageName)` seam and returns the user to its list.
- The target app's icon and label are re-resolved from the package name at the point of use, per
  the hand-off contract — no display data is carried across the seam.

## Out of Scope

Explicitly not built here, and to be rejected if proposed during implementation:

- Delay duration, countdown UI, and the screen that runs it
- Time windows, schedules, and any evaluation of whether a delay currently applies
- Storing per-app configuration of any kind
- Editing, listing, or removing shortcuts already pinned
- Icon treatments beyond Original, Invert, and Gray
- Any behaviour that depends on knowing which apps already have shortcuts
