# Feature Specification: Hero Device Frame Screenshot

**Feature Branch**: `008-hero-device-frame`

**Created**: 2026-08-25

**Status**: Draft

**Input**: User description: "In the site on mobile the application screen from hero section looks collapsed. Use the application screenshot in the device frame. @photo_2026-08-25_19-50-41.jpg"

## Context

The landing page hero pairs the headline with a picture of the app. Today that picture is a
hand-drawn imitation of the "Wait before opening" screen, assembled from page elements. At the
desktop size it is given a fixed height and looks right. On a narrow screen it is allowed to
size itself, its centred middle section has nothing to fill, and the parts of the screen crush
together — the app icon, the big number and the slider stack up with no breathing room, so the
picture reads as broken rather than as a phone.

A real screenshot of that same screen already exists and is supplied with this feature. Showing
the actual screenshot inside a phone frame removes the whole class of problem: a picture keeps
its proportions at every width, and it shows visitors what the app genuinely looks like rather
than a re-drawing that can drift away from the app over time.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A visitor on a phone sees the app screen intact (Priority: P1)

Someone opens the landing page on their phone. Below the headline and the Play button they see a
picture of the app: a phone-shaped frame containing the "Wait before opening" screen, with the
selected app, the countdown number, the slider and the button all sitting exactly where they sit
in the app itself. Nothing overlaps, nothing is squashed, and the frame is tall in the way a
phone is tall.

**Why this priority**: This is the reported defect. Mobile visitors are the majority of traffic
for an Android app's landing page, and the hero image is the first evidence they get that the app
is real and finished. A visibly broken picture undercuts that in the first seconds.

**Independent Test**: Open the page at typical phone widths (from roughly 320px up to the point
where the layout switches to two columns) and confirm the hero picture keeps a phone's
proportions and shows every element of the app screen legibly.

**Acceptance Scenarios**:

1. **Given** the landing page opened at a 390px-wide viewport, **When** the visitor looks at the
   hero, **Then** the app picture appears as an upright phone whose height is roughly twice its
   width, and every element of the app screen is visible and separated as in the app.
2. **Given** the landing page opened at a 320px-wide viewport, **When** the visitor looks at the
   hero, **Then** the picture still fits inside the page with its side margins intact and does
   not cause the page to scroll sideways.
3. **Given** the visitor rotates the phone to landscape, **When** the hero is shown, **Then** the
   picture keeps its proportions and stays within the visible width.

---

### User Story 2 - The picture shows the real app (Priority: P2)

A visitor on any device sees, in the hero, a screenshot taken from the running app rather than an
approximation of it — the same typeface, spacing, colours and controls they will meet after
installing.

**Why this priority**: It is the reason the fix takes this shape rather than a layout patch. It
also removes a maintenance trap: the imitation has to be re-edited by hand every time the app
screen changes, and silently becomes a lie when nobody does.

**Independent Test**: Compare the hero picture side by side with the supplied screenshot and with
the app's own screen; they show the same content.

**Acceptance Scenarios**:

1. **Given** the published page on a desktop browser, **When** the hero is compared with the app's
   "Wait before opening" screen, **Then** the picture shows that screen as the app actually
   renders it, including the app chip, the "10 SECONDS" readout, the slider, the preset choices
   and the primary button.
2. **Given** the hero picture, **When** it is examined closely, **Then** it is a single captured
   image of the app screen rather than a re-drawing of it.

---

### User Story 3 - The desktop hero is unharmed (Priority: P3)

A visitor on a laptop sees the hero exactly as composed: text on the left, phone on the right, the
phone the same size and weight in the layout as before.

**Why this priority**: The desktop hero is not broken. The change must not trade one regression
for another, but it delivers no new value on its own.

**Independent Test**: Compare the page at 1440px before and after the change; the hero's
proportions, alignment and spacing are unchanged.

**Acceptance Scenarios**:

1. **Given** a 1440px-wide viewport, **When** the hero is shown, **Then** the phone occupies the
   same column and the same visual footprint as before, aligned with the text block as before.
2. **Given** a viewport wider than 1600px, **When** the hero is shown, **Then** the phone stops
   widening along with the rest of the page content.

---

### Edge Cases

- **The image has not loaded yet or fails to load**: the frame must hold its space rather than
  collapsing and reflowing the hero, and the picture must carry a text description so a visitor
  who never sees it still learns what it showed.
- **Very narrow viewports (320px and below)**: the picture scales down with the page instead of
  forcing horizontal scrolling.
- **Very short viewports (landscape phones)**: the picture is allowed to be taller than the
  screen; the visitor scrolls, as with any other hero content.
- **Slow or metered connections**: the picture must not be so heavy that the hero is blank for
  seconds on a mobile connection.
- **High-density screens**: the picture must not look soft or blocky on a phone or a Retina
  laptop.
- **A visitor who has printed the page or uses a text-only view**: the description stands in for
  the picture.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The hero MUST present the app's "Wait before opening" screen as a screenshot image
  taken from the running app, replacing the current imitation built from page elements.
- **FR-002**: The screenshot MUST be presented inside a phone frame consistent with the rest of the
  page's visual language — an upright rounded body with a dark bezel and the existing drop shadow —
  so it reads as a device and not as a bare image dropped into the layout.
- **FR-003**: The framed picture MUST keep the screenshot's proportions at every viewport width,
  never stretching, squashing or cropping the app screen.
- **FR-004**: At viewport widths where the hero is a single column, the framed picture MUST fit
  within the page's side margins and MUST NOT cause horizontal scrolling, down to a 320px viewport.
- **FR-005**: At desktop widths the framed picture MUST occupy the same position and visual
  footprint in the hero as the current one, leaving the rest of the hero untouched.
- **FR-006**: The framed picture MUST reserve its final space before the image data arrives, so
  the hero does not visibly jump when the image loads.
- **FR-007**: The picture MUST carry a short text description of what it shows, for visitors using
  a screen reader or who never receive the image.
- **FR-008**: The screenshot MUST remain sharp on high-density displays at the sizes the page uses
  it, and MUST be light enough not to delay the hero on a typical mobile connection.
- **FR-009**: The screenshot asset MUST live with the site's other assets and be published by the
  existing site deployment, requiring no new hosting or third-party service.
- **FR-010**: The screenshot MUST NOT display anything personal or incidental to the demonstration
  — a phone owner's notifications, real contact names, or a battery and clock reading that draws
  attention to itself. Where the supplied image carries such incidental detail, it is either
  removed from the image or covered by the frame.

### Key Entities

- **Hero screenshot**: a single image of the app's "Wait before opening" screen, upright, roughly
  9:20 in proportion, stored alongside the site's other assets.
- **Device frame**: the phone-shaped surround the screenshot sits in — bezel, corner radius and
  shadow — supplied by the page rather than by the image.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: At every viewport width from 320px to 1600px, the hero picture keeps a phone's
  proportions (its height between 1.9 and 2.3 times its width) and the page never scrolls
  sideways.
- **SC-002**: A visitor on a phone can identify every element of the app screen — the chosen app,
  the wait length, the slider, the presets and the button — without zooming.
- **SC-003**: The hero picture is fully visible within 2 seconds of the page opening on a typical
  mobile connection.
- **SC-004**: The hero occupies its final position from first paint; the picture loading moves no
  other element on the page.
- **SC-005**: The desktop hero at 1440px is visually unchanged apart from the picture's contents.
- **SC-006**: Updating the pictured app screen requires replacing one image file and nothing
  else.

## Assumptions

- The supplied screenshot (`photo_2026-08-25_19-50-41.jpg`, 576×1280) is the intended source
  image; it may be re-exported at higher resolution or in a more efficient format if that is what
  meets FR-008.
- The screenshot replaces the imitation at all viewport widths, not only on mobile. Keeping a
  hand-built mock for desktop and a screenshot for mobile would mean maintaining two pictures of
  the same screen and letting them drift apart.
- The device frame stays a page-drawn surround rather than being baked into the image, so the
  frame can follow the site's palette and the screenshot can be swapped on its own.
- The supplied screenshot's own status bar and gesture bar are treated as incidental system
  chrome (FR-010) rather than as part of the app screen worth showing.
- Only the hero picture is in scope. The rest of the page — the "How it works", "Why" and
  open-source sections, the header and the footer — is untouched, and no second screenshot is
  added elsewhere.
- The site remains a static hand-maintained page deployed the way it is today; no build step,
  image pipeline or dependency is introduced.
- Verification is by the maintainer looking at the page at the stated widths; the project runs no
  automated browser tests.
