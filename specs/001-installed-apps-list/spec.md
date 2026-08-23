# Feature Specification: Installed Applications List

**Feature Branch**: `001-installed-apps-list`

**Created**: 2026-08-15

**Status**: Draft

**Input**: User description: "A screen with installed applications list."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse launchable apps (Priority: P1)

A user opens SlowLock for the first time and wants to see which of their apps they could
slow down. The app presents a single scrollable list of every app on the device that can
be opened from the home screen, each row showing the app's own icon and its name as the
user knows it. The user recognises their apps at a glance and can scroll the whole list
smoothly.

**Why this priority**: Without a trustworthy list of apps, no other part of the product
can start. This story alone is a demonstrable, shippable slice: the user can confirm
SlowLock sees their apps correctly.

**Independent Test**: Install on a device with a mix of apps, open SlowLock, and confirm
every app that has a home-screen icon appears exactly once with the correct icon and
label, and that scrolling the full list stays smooth.

**Acceptance Scenarios**:

1. **Given** a device with launchable apps installed, **When** the user opens SlowLock,
   **Then** a list of those apps is shown, each row with the app's icon and its
   user-visible name.
2. **Given** the app list is being read from the system, **When** loading takes longer
   than a moment, **Then** the screen shows a loading indication rather than a blank or
   frozen screen.
3. **Given** the list has loaded, **When** the user scrolls from top to bottom,
   **Then** scrolling remains smooth and no row is blank or shows a wrong icon.
4. **Given** the list is shown, **When** the user looks for SlowLock itself,
   **Then** SlowLock does not appear in the list.
5. **Given** the list is shown, **When** the user compares the ordering,
   **Then** apps are ordered alphabetically by their displayed name, case-insensitively,
   in the device's current language.

---

### User Story 2 - Find a specific app quickly (Priority: P2)

A user with many installed apps wants to reach one particular app (for example
Instagram) without scrolling through the whole list. They type part of the name and the
list narrows to matching apps as they type.

**Why this priority**: On a typical device the list runs to well over a hundred entries,
and the product's value depends on the user reaching their problem app in seconds. It is
still a separate slice — the list is usable without it.

**Independent Test**: With the list loaded, type a partial app name and confirm only
matching apps remain, then clear the query and confirm the full list returns.

**Acceptance Scenarios**:

1. **Given** the full list is shown, **When** the user types text into the search field,
   **Then** only apps whose displayed name contains that text (ignoring case) remain
   visible.
2. **Given** a search query is active, **When** the user clears the query,
   **Then** the full list is restored in its original order.
3. **Given** a search query matches no app, **When** the results are shown,
   **Then** the screen states that no apps match, instead of showing an empty area.

---

### User Story 3 - Open the selected app (Priority: P3)

Having found the app they want, the user taps its row and SlowLock opens that app
immediately. **No delay is applied.** Configuring time windows and delay duration, and the
countdown that precedes a launch, are a separate feature with their own spec.

Launching the target directly is a deliberate feasibility proof: it exercises the single
mechanism the entire product depends on — that SlowLock can take a package name it stored
earlier, resolve it, and start the right app — before any configuration, scheduling, or
shortcut-pinning machinery is built on top of it. If this does not work, nothing later in
the roadmap works either.

**Why this priority**: It is the smallest change that converts the picker from a list into
a demonstration that the product idea is buildable. It is last because it depends on US1,
and because the delay logic it stands in for is a separate feature.

**Independent Test**: Tap an app row and confirm the app that opens is the one tapped; then
return to SlowLock and confirm the list is exactly where you left it.

**Acceptance Scenarios**:

1. **Given** the list is shown, **When** the user taps an app row, **Then** that
   application opens in the foreground, with no delay and no countdown.
2. **Given** an app was tapped, **When** SlowLock resolves it for launching, **Then** it is
   resolved from a value that survives the app being updated or renamed.
3. **Given** the user taps a row and returns to the list, **When** the list reappears,
   **Then** the scroll position and any active search query are preserved.
4. **Given** a tapped application cannot be launched, **When** the launch is attempted,
   **Then** the screen reports the application is unavailable and no crash occurs.

---

### Edge Cases

- **No launchable apps found.** The screen shows an explanatory empty state rather than a
  blank screen. (Realistically only occurs on stripped-down or emulator images.)
- **An app is installed while the list is open.** The list does not need to update live;
  it refreshes the next time the screen is opened.
- **An app is uninstalled while the list is open.** A stale row may remain until refresh;
  tapping it must not crash, and the screen must report that the app is no longer
  available.
- **An app has no icon or an icon that fails to load.** A neutral placeholder icon is
  shown; the row remains selectable.
- **Two apps share the same displayed name.** Both rows appear; they are distinguished by
  their underlying identity, not their name, so selecting either targets the right app.
- **Very long app names.** The name is truncated to a single line rather than reflowing
  or pushing the row layout out of shape.
- **The device language changes.** Names and ordering follow the new language the next
  time the screen is opened.
- **An app is updated and its icon changes.** The list shows the new icon rather than a
  stale cached one.
- **The user rotates the device or the screen is recreated.** The list, scroll position,
  and search query survive without a full reload flash.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The screen MUST list every application on the device that the user can open
  from the home screen, and MUST exclude applications that have no launchable entry point.
- **FR-002**: Each list row MUST display the application's own icon and its user-visible
  name in the device's current language.
- **FR-003**: SlowLock itself MUST NOT appear in the list.
- **FR-004**: Each application MUST appear exactly once, even when it exposes more than
  one launchable entry point.
- **FR-005**: The list MUST be ordered alphabetically by displayed name, compared
  case-insensitively using the device's current language rules.
- **FR-006**: The screen MUST show a loading state while the list is being assembled, an
  empty state when no applications are found, and a distinct no-results state when a
  search query matches nothing.
- **FR-007**: Users MUST be able to filter the list by typing text, matching
  case-insensitively against any part of the displayed name.
- **FR-008**: Users MUST be able to clear the search query and return to the full list.
- **FR-009**: Users MUST be able to tap an application's row to open that application in
  the foreground.
  **Superseded by `002-shortcut-pinning`**: a row tap now opens the shortcut configuration
  screen (002 FR-001) rather than launching the target. Launching did not disappear — it moved
  to `ShortcutLaunchActivity`, reached from the pinned home-screen icon instead of from the
  list. The requirement is recorded as it shipped; only its consumer changed.
- **FR-010**: A tapped application MUST be resolved and launched using an identifier that
  remains valid across updates of that application and across changes to its displayed
  name.
- **FR-018**: Tapping a row MUST open the target application immediately. No delay,
  countdown, schedule evaluation, or shortcut pinning is part of this feature.
  **Superseded by `002-shortcut-pinning`**: a tap no longer opens the target at all, so the
  immediacy clause no longer describes anything. Shortcut pinning is precisely what a tap now
  leads to — which was out of scope for 001 and is the whole of 002. The exclusion of delay,
  countdown, and schedule evaluation **still holds**: none of them exists yet, and 002 adds
  none of them.
- **FR-011**: The screen MUST remain responsive to scrolling and typing at all times;
  reading the application list, decoding icons, and any file access MUST NOT block the
  interface.
- **FR-012**: Application icons MUST be cached so that reopening the screen does not
  re-read every icon from scratch, and the cache MUST be invalidated when an application
  is updated.
- **FR-013**: The list MUST be re-read each time the screen is opened, so applications
  installed or removed since the previous visit are reflected.
- **FR-014**: Tapping an application that has since been uninstalled, or whose launch
  otherwise fails, MUST NOT crash; the screen MUST inform the user that the application is
  no longer available and leave them on the list.
- **FR-015**: The feature MUST NOT request any user-facing permission and MUST NOT
  require any system permission dialog to display the list.
- **FR-016**: Rows MUST show a neutral placeholder icon when an application's icon cannot
  be loaded, and MUST remain selectable.
- **FR-017**: The scroll position and the active search query MUST be preserved when the
  screen is recreated (for example on rotation) or returned to from the selection
  hand-off.

### Key Entities

- **Installed Application**: A user-openable application present on the device. Attributes:
  a stable identity used for all matching and persistence, a display name (localized,
  presentation only), an icon (presentation only), and a version marker used to detect
  when a cached icon is stale.
- **App List State**: What the screen is currently showing — loading, populated, empty, or
  no-results — together with the active search query and scroll position.
- **Cached Icon**: A stored icon image associated with one application and its version
  marker, reused across screen visits and discarded when the version marker changes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On a device with 150 launchable applications, the list is visible and
  scrollable within 1 second of opening the screen.
- **SC-002**: Every application that has a home-screen icon appears in the list — verified
  against the device launcher's own app drawer, with 100% agreement apart from SlowLock
  itself.
- **SC-003**: Scrolling the full list produces no visible stutter and no blank or
  mismatched rows on a mid-range device.
- **SC-004**: A user who knows the name of the app they want reaches it in under 5
  seconds using search.
- **SC-005**: Reopening the screen a second time displays the list at least twice as fast
  as the first open, through icon caching.
- **SC-006**: Displaying the list triggers zero permission prompts.
- **SC-007**: 95% of first-time users correctly identify and select their intended app on
  the first attempt, without selecting the wrong entry.

## Assumptions

- **Delay logic is out of scope.** Configuring time windows and delay duration, evaluating
  schedules, the countdown screen, and pinning the home-screen shortcut are all separate
  features with their own specs. In this feature a tap opens the target app immediately.
- **Launching the target here is a feasibility proof, not the final flow.** In the finished
  product the picker hands the chosen app to a configuration screen, and launching happens
  later from a pinned shortcut via the countdown. Launching directly from the list now
  validates the resolve-and-start mechanism early, at the cost of one call site that will
  be replaced when the configuration feature lands. The seam is kept explicit
  (`onAppSelected(packageName)`) so that replacement is a one-line change.
- **All launchable apps are listed, including preinstalled system apps** (Settings, Phone,
  Camera). `highlevel_spec.md` §2 specifies "all launchable installed applications", and
  filtering system apps out would hide legitimate targets such as a preinstalled browser.
- **Search is included in v1.** A list of 150+ entries is unusable by scrolling alone; app
  pickers conventionally include name search. It is scoped as an independent P2 slice so it
  can be dropped without invalidating P1.
- **Current user profile only.** Work-profile and dual-app (Xiaomi Dual Apps, Samsung
  Secure Folder) entries are out of scope for this feature; `highlevel_spec.md` §5.9 already
  records dual-app behaviour as untested.
- **No live updates while the screen is open.** Refresh on each visit is sufficient;
  observing package install/remove events in real time is unnecessary complexity for v1.
- **No multi-select.** One application is chosen per pass, matching the one-dialog-per-pin
  constraint recorded in `highlevel_spec.md` §5.8.
- **No "already configured" indicator.** No configuration store exists yet; marking already
  slowed apps belongs with the feature that creates that data.
- **Recently used or suggested apps are not surfaced.** Determining recent usage would
  require usage-statistics access, which the constitution forbids in v1.
- **The device is running the minimum supported platform version or newer**, so the app
  list can be read without a restricted permission.
