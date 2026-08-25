# Feature Specification: Legible system bar and a redesigned Locks screen

**Feature Branch**: `007-locks-screen-polish`

**Created**: 2026-08-25

**Status**: Draft

**Input**: User description: "Use the claude_design MCP to import the `Mobile app redesign proposal` project (`SlowLock Redesign.dc.html`). Implement: App UI improvements. The system action bar should be black (now it contains white text that looks bad on the light application background). On the Locks screen the title and list items should be redesigned according to the design canvas."

**Design source**: artboard `1a` — *Proposed direction · restyle + reflow*, screen `New · Locks` — in the Claude Design project `Mobile app redesign proposal` (`4fe7e35d-2bb5-4814-b99e-4ce3107bdbb0`), file `SlowLock Redesign.dc.html`. Where this spec and the artboard disagree, the artboard is the authority on appearance and this spec is the authority on wording and honesty.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The clock and battery stay readable (Priority: P1)

Someone whose phone is set to dark mode opens SlowLock. The app is light — bone paper with near-black type — and today the row of system indicators at the very top (clock, signal, battery) is drawn in white, which leaves it all but invisible against the app's own background. They should be able to read the time and their battery level while the app is open, on any device, whatever their system theme is set to.

**Why this priority**: It is a legibility defect visible on the first frame of every screen, on a large share of devices, and it is independent of everything else in this feature.

**Independent Test**: Set the device to dark mode, open the app, and confirm the system indicators at the top of the screen are dark and readable against the app background. Repeat in light mode; the result must be identical.

**Acceptance Scenarios**:

1. **Given** the device is set to dark mode, **When** the user opens SlowLock, **Then** the system indicators across the top of the screen are drawn dark against the app's light background and are readable.
2. **Given** the device is set to light mode, **When** the user opens SlowLock, **Then** the system indicators look exactly as they do in scenario 1.
3. **Given** the user is anywhere in the configuration flow (Locks, first run, app list, delay, icon, home-screen-unsupported), **When** they look at the top of the screen, **Then** the indicators are dark and readable on that screen too.
4. **Given** the user taps a pinned shortcut and the wait screen appears, **When** the device is in dark mode, **Then** the wait screen keeps its own light/dark behaviour and is unaffected by this change.

---

### User Story 2 - The Locks screen reads like the design (Priority: P2)

A returning user opens SlowLock and lands on their locks. The heading should be the confident, large title the design shows, with the count set beneath it as a quiet mono caption, rather than the smaller header the flow screens use.

**Why this priority**: The title is the first thing on the app's home screen and it is the visible difference between "a screen" and "the design". It is separable from the rows and can ship on its own.

**Independent Test**: Open the app with at least one lock and compare the heading block against the `New · Locks` artboard: a large title, a mono caption below it stating the count, and nothing else above the list.

**Acceptance Scenarios**:

1. **Given** the user has three locks, **When** the Locks screen opens, **Then** a large title reading "Locks" appears at the top, with a mono, uppercase, letter-spaced caption beneath it stating how many locks there are.
2. **Given** the user has exactly one lock, **When** the Locks screen opens, **Then** the caption reads in the singular.
3. **Given** the user has no locks, **When** the app opens, **Then** the first-run screen shows instead and the Locks screen does not render at all — the heading block has no zero state to design.
4. **Given** a screen reader is active, **When** the heading block is reached, **Then** the title and the count are announced as ordinary text, and the count is spoken in words rather than as an all-caps abbreviation.

---

### User Story 3 - Each lock reads as a card with its delay called out (Priority: P2)

Scanning the list, the user should be able to find a lock by its app icon and name, and read its delay without reading a sentence. The design puts the delay in an amber badge at the trailing edge of each row and leaves the icon treatment as the row's quiet second line.

**Why this priority**: The delay is the product's central value, and the design states it as a number the eye lands on. This is the substance of "redesign the list items", but it depends on nothing in User Story 2.

**Independent Test**: With three locks at different delays and different icon treatments, compare a row against the artboard: icon, name, treatment beneath the name, delay badge at the trailing edge.

**Acceptance Scenarios**:

1. **Given** a lock for an installed app, **When** its row draws, **Then** the row shows the app's icon, the app's name, the icon treatment beneath the name, and the delay in a badge at the trailing edge.
2. **Given** a lock with a 10-second delay, **When** its row draws, **Then** the badge states the delay compactly (for example "10s") in the numeric typeface.
3. **Given** an app with a very long name, **When** its row draws, **Then** the name truncates and the badge and treatment stay fully visible.
4. **Given** a screen reader is active, **When** a row is reached, **Then** it is announced as one stop that includes the app name, the full delay in words, and the treatment.
5. **Given** the user taps a row, **When** the tap lands, **Then** it opens that lock for editing exactly as before.
6. **Given** the user long-presses a row, **When** the press completes, **Then** the removal explanation appears exactly as before.
7. **Given** a lock whose app has been uninstalled, **When** its row draws, **Then** it keeps its existing appearance and its visible "How to remove" control, and shows no delay badge.

---

### Edge Cases

- **The largest system font scale**: the title, the caption, the row name, the treatment line, and the badge must all grow with it. Rows grow taller rather than clipping, and the badge grows rather than truncating its number.
- **Zero locks**: unreachable on this screen. Feature 005 routes an empty list to the first-run screen, so the smallest count the caption can ever state is one.
- **A lock whose app is gone**: no delay badge, no redesigned two-line body — the existing unavailable row is deliberately left as it is, because its message is a sentence and not a name plus a detail.
- **An icon that has not loaded yet**: the row draws immediately with the placeholder at the new icon size, as it does today.
- **Right-to-left layouts**: "trailing edge" means trailing, so the badge sits on the left in RTL and the icon on the right.
- **Very long delays**: the badge must fit a two-digit number and its unit without clipping.

## Requirements *(mandatory)*

### Functional Requirements

#### The system bar

- **FR-001**: The system status indicators drawn over the app's own screens MUST render in a dark treatment against the app's light background, so that they meet the same legibility bar as the app's own text.
- **FR-002**: FR-001 MUST hold regardless of the device's light/dark system setting — the appearance MUST NOT change when the user switches the device to dark mode.
- **FR-003**: FR-001 MUST apply to every screen the app's own theme covers: the Locks screen, the first-run screen, the app list, the delay screen, the icon screen, and the home-screen-unsupported screen.
- **FR-004**: The wait screen a pinned shortcut shows MUST be excluded from FR-001 and MUST keep its existing behaviour of following the device's light/dark setting.
- **FR-005**: The app MUST continue to draw edge to edge; this feature changes the appearance of the system indicators, not the app's use of the space behind them.

#### The Locks screen heading

- **FR-006**: The Locks screen MUST present its heading as a large title, visually distinct from and larger than the title style the flow screens use.
- **FR-007**: The heading text MUST read "Locks", matching the design source.
- **FR-008**: A caption MUST sit directly beneath the title, set in the numeric typeface, uppercase, with the letter-spacing the design specifies, in the app's caption ink colour.
- **FR-009**: The caption MUST state the number of locks and nothing more. It MUST NOT claim the locks are on the home screen, because the app cannot know that (Constitution I; carried forward from feature 005 FR-011).
- **FR-010**: The caption MUST be correctly pluralised. It has no zero state: a user with no locks sees the first-run screen instead, and the Locks screen is only ever rendered with at least one lock.
- **FR-011**: The heading block MUST NOT introduce a back control, a step counter, or any other control — the Locks screen is the app's root.
- **FR-012**: The caption MUST be announced by a screen reader in ordinary spoken words, not as an all-caps string that a screen reader may spell out.

#### The lock rows

- **FR-013**: Each row for an available lock MUST show, in order along the row: the app's icon, a body block containing the app's name above the icon treatment, and a delay badge at the trailing edge.
- **FR-014**: The row's second line MUST state the icon treatment only. The delay MUST move out of that line and into the badge.
- **FR-015**: The delay badge MUST state the delay in compact form (a number and a unit, for example "10s"), set in the numeric typeface at medium weight, on the accent-wash fill with the dark-accent ink, with its own corner radius as the design specifies.
- **FR-016**: The row MUST use the icon size, corner radius, internal padding, spacing, card fill, hairline border and card radius the design source specifies for this screen.
- **FR-017**: The app name MUST truncate before the treatment line or the badge does; the badge MUST never be compressed or clipped by a long name.
- **FR-018**: A row MUST be announced to a screen reader as a single stop that carries the app name, the delay stated in full words, and the treatment — the compact badge form MUST NOT be the only thing spoken.
- **FR-019**: Tapping an available row MUST continue to open that lock for editing; long-pressing MUST continue to open the removal explanation; the custom accessibility action for the removal explanation MUST remain on every row.
- **FR-020**: A row whose app is no longer installed MUST keep its current message, its current visible removal control, and its current absence of a tap target, and MUST NOT show a delay badge.
- **FR-021**: Rows MUST grow in height rather than clip at large system font scales.

#### Design-system integrity

- **FR-022**: This feature MUST NOT introduce a twelfth colour. Every colour it uses MUST come from the existing eleven-token palette.
- **FR-023**: Every new text-on-surface pairing this feature puts on screen MUST be declared in the palette's pairing list and MUST meet the project's existing contrast floor.
- **FR-024**: Any new type role or corner radius this feature needs MUST be added to the app's central type and shape definitions, not declared inside a screen.
- **FR-025**: No screen outside the Locks screen may change appearance as a result of this feature, apart from the system-indicator change of FR-001.

### Key Entities

No new entities. The feature changes how the existing **Lock** (app name, package, delay in seconds, icon treatment, availability) is presented; nothing about what is stored, derived, or persisted changes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On a device set to dark mode, the clock and battery indicator at the top of every app screen are readable against the app background — verified by eye and by the same contrast floor the app applies to its own text.
- **SC-002**: Switching the device between light and dark mode produces no visible change to any of the app's own screens.
- **SC-003**: The Locks screen's heading block and a lock row placed beside the `New · Locks` artboard match it in size, weight, spacing, colour and radius, judged by a reviewer holding the two side by side.
- **SC-004**: A user asked "how long does the Messages lock wait?" can answer from the list without reading a full sentence — the delay is legible as a number at a glance.
- **SC-005**: At the largest system font scale, no text on the Locks screen is clipped, truncated other than the app name, or overlapping.
- **SC-006**: A screen reader pass over the Locks screen reads the title, the count in words, and one stop per lock carrying name, full delay and treatment; every lock still exposes the removal explanation as an action.
- **SC-007**: The palette still contains exactly eleven colours after this feature, and the automated palette and pairing checks pass.
- **SC-008**: Every requirement above is covered by a numbered case in the feature's manual test plan, and all cases pass on the maintainer's device.

## Assumptions

- **"The system action bar should be black" means its contents, not its background.** The complaint is white indicators on a light ground, and the design artboards draw the status-bar strip as part of the screen's bone background with near-black glyphs. This feature therefore darkens the indicators and leaves the background as the app's own. If the intent was instead a solid black bar across the top, that is a different change and this spec does not cover it.
- **The root cause is the system-bar appearance following the device theme.** The app is light-only by design (feature 004 FR-008); the indicators currently follow the device's dark-mode setting, which is why they turn white. Fixing that is what FR-002 asks for.
- **The title text changes from "Your locks" to "Locks".** The design canvas says "Locks" and the request says to redesign the title according to the canvas.
- **The caption keeps feature 005's honest wording.** The canvas reads "3 ON YOUR HOME SCREEN"; the second half is not shipped and stays not shipped, for the reason recorded in feature 005. Only the *styling* of that line — mono, uppercase, letter-spaced — is adopted here.
- **The unavailable row is out of the redesign.** The canvas has no artboard for it, and inventing one would be a guess. It keeps what feature 005 gave it.
- **The first-run screen is out of scope.** It is what an empty list shows, it is not being redesigned here, and this feature touches it only through the system-bar change of FR-001.
- **New type roles and one new corner radius are expected.** The large title, the letter-spaced caption, the row name at medium weight and the badge label are not in the current role set, and the badge's radius is not one of the five existing radii. They are additions to the central definitions, not exceptions to them (FR-024).
- **Dark mode for the app itself remains out of scope**, as it has been since feature 004. FR-002 is about not letting the device setting leak in, not about building a dark theme.
- **No behaviour, navigation, storage or permission changes.** This feature is presentation only.
- **The design source is read-only here.** The Claude Design project is a reference; this feature does not write back to it.
