# Feature Specification: Public Site & Privacy Policy

**Feature Branch**: `006-site-publishing`

**Created**: 2026-08-25

**Status**: Draft

**Input**: User description: "`/Users/anatolii/Projects/SlowLock/site/PUBLISHING.md`" — a written plan for turning the two design exports already sitting in `site/` into a published website with a landing page and a privacy policy page, hosted free from this public repository, so the policy URL can be submitted to Google Play.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The maintainer submits a privacy policy URL to Google Play (Priority: P1)

The maintainer is filling in the Play Console listing and needs a public web address that shows SlowLock's privacy policy. They paste the address into the Play Console, and a reviewer — or any user who taps "Privacy policy" on the store listing — opens it and reads an accurate, finished statement of what the app does and does not do with their data.

**Why this priority**: The app cannot be published on Google Play without this URL. It is the only part of this feature that blocks a release, and it delivers value on its own even if no landing page ever exists.

**Independent Test**: Publish only the privacy page, open its address in a browser on a device that has never seen the project, and confirm the full policy text is readable and contains no draft or placeholder wording.

**Acceptance Scenarios**:

1. **Given** the site is published, **When** anyone opens the privacy policy address, **Then** the complete policy is displayed, including what data the app reads, what it stores, what it never does, how to delete it, and how to contact the maintainer.
2. **Given** the site is published, **When** the page is fetched by something that does not run scripts (a reviewer's tool, a search crawler, a command-line fetch), **Then** the full policy text is still present in what was fetched.
3. **Given** the privacy page is open, **When** a reader looks for signs that it is unfinished, **Then** no draft, "not legally reviewed", or placeholder notice appears anywhere on the page.
4. **Given** a reader wants to check a claim, **When** they follow the link to the source repository, **Then** the link opens the SlowLock repository.

---

### User Story 2 - A prospective user learns what SlowLock is (Priority: P2)

Someone hears about SlowLock — from the repository, a link, or the store listing — and opens the site on their phone. They read what the app does, how it is set up in three steps, and why it uses a pause instead of a block, then follow the call to action to get it.

**Why this priority**: This is the reason to have a site at all rather than a bare policy document, but the app can ship without it.

**Independent Test**: Open the landing address on a phone-sized screen and confirm the page explains the product, reads without horizontal scrolling, and every link and button leads somewhere real.

**Acceptance Scenarios**:

1. **Given** a visitor on a phone-sized screen, **When** the landing page loads, **Then** all content is readable within the screen width with no sideways scrolling and no content cut off.
2. **Given** a visitor on the landing page, **When** they follow the privacy link in the footer, **Then** the privacy policy page opens.
3. **Given** a visitor on the privacy page, **When** they follow the "back to site" link, **Then** the landing page opens.
4. **Given** a visitor reaches the site's base web address without naming a page, **Then** the landing page is displayed rather than an error.
5. **Given** a visitor follows the primary call to action, **When** they arrive, **Then** they are on Google Play — the app's own listing once it exists, and the store entry point until then, never an unrelated destination.
6. **Given** the page is shared in a chat app or posted as a link, **When** a preview is generated, **Then** the preview shows the SlowLock name and a description of the app rather than a generic or empty title.

---

### User Story 3 - The maintainer corrects the site after publishing (Priority: P3)

The app changes, a claim on the policy stops being true, or the store link becomes available. The maintainer edits the site content, and the live site shows the change shortly afterwards without any manual upload step.

**Why this priority**: Needed for the site to stay accurate over time, but only matters once the site is live.

**Independent Test**: Change one visible word on a published page, complete the maintainer's normal publish action, and confirm the live page shows the new word within a few minutes.

**Acceptance Scenarios**:

1. **Given** the site is live, **When** the maintainer publishes a content change, **Then** the live site reflects it within 5 minutes with no further steps.
2. **Given** the maintainer publishes a change to files unrelated to the site, **When** the change lands, **Then** the live site is unaffected.
3. **Given** the policy text changes, **When** it is published, **Then** the "last updated" date shown on the policy reflects that change.

---

### Edge Cases

- A visitor opens the site with scripts disabled or blocked — all content on both pages MUST still be readable.
- A visitor arrives at the site's base address with no page name — the landing page MUST be served.
- A visitor arrives at an address for a page that does not exist — a plain, non-broken response is acceptable; no requirement for a custom page.
- The app's behaviour changes so a policy claim is no longer true — the policy MUST be corrected and its date updated before the change reaches users.
- The app is not yet published to the store — the call to action points at Google Play's entry point rather than a wrong app's listing, and is swapped for the real listing address before the listing is announced.
- A very wide desktop screen — content MUST remain readable rather than stretching to the full window width.
- The published contact address is harvested by spam scrapers — accepted, provided a working contact route remains on the page.

## Requirements *(mandatory)*

### Functional Requirements

**Reachability**

- **FR-001**: The site MUST be publicly reachable at a stable web address that requires no account, payment, or invitation to visit.
- **FR-002**: The privacy policy MUST have its own permanent address, distinct from the landing page, suitable for pasting into the Google Play listing.
- **FR-003**: Visiting the site's base address without naming a page MUST display the landing page.
- **FR-004**: Page addresses MUST contain no spaces or characters that require escaping when written into a form or a message.
- **FR-005**: Navigating between the two pages MUST work from the published site in both directions, and MUST NOT depend on the site being hosted at the root of a domain.

**Content delivery**

- **FR-006**: The full text of both pages MUST be present in what a visitor's browser first receives, without requiring scripts to run in order to become visible.
- **FR-007**: Both pages MUST be readable on a phone-sized screen with no horizontal scrolling and no clipped content.
- **FR-008**: Each page MUST carry its own descriptive title naming SlowLock, and a description suitable for search results and link previews.
- **FR-009**: Every interactive element on both pages MUST either lead to a real destination or be presented as non-interactive; no element may appear clickable and do nothing.

**Privacy policy content**

- **FR-010**: The policy MUST state what device information the app reads, what it stores on the device, and that neither leaves the device.
- **FR-011**: The policy MUST state that the app makes no network requests and contains no analytics, crash-reporting, advertising, or other third-party tracking components.
- **FR-012**: The policy MUST tell the reader how to delete everything the app stored.
- **FR-013**: The policy MUST provide a working contact route for privacy questions.
- **FR-014**: The policy MUST show the date it was last changed.
- **FR-015**: The policy MUST NOT contain any draft, provisional, or "not reviewed" notice when published.
- **FR-016**: Every factual claim in the policy MUST be verifiable against the app's source code, and MUST be re-checked against that source before publishing.
- **FR-017**: The policy's claims MUST NOT contradict the Data safety declaration submitted to Google Play.

**Landing page content**

- **FR-018**: The landing page MUST explain what the app does, how a user sets it up, and why it uses a delay rather than a block.
- **FR-019**: Any statement the landing page makes about the app's behaviour (wait length, icon options, absence of tracking) MUST match the app's actual behaviour.
- **FR-020**: The landing page MUST link to the source repository.
- **FR-021**: The primary call to action MUST lead to Google Play. Until the app's own listing address is available it MAY point at the Google Play store entry point; it MUST be replaced with the app-specific listing address before the store listing is announced, and MUST NOT point at any unrelated destination in the meantime.
- **FR-022**: The repository MUST carry an MIT licence, so that the landing page's "free" and "open source" claims are accurate as written. The landing page MUST NOT claim rights the licence does not grant.

**Publishing**

- **FR-023**: Publishing an update to the site MUST require no manual upload and MUST take effect within 5 minutes of the maintainer's normal publish action.
- **FR-024**: The site's source MUST live in the repository in a directory dedicated to it, separate from the application source.
- **FR-025**: Only the site's own files MUST be served publicly; the publishing mechanism MUST NOT expose anything else from the repository.

**Independence**

- **FR-026**: Both pages MUST render without contacting any third-party service at view time, or any such dependency MUST be a deliberate, recorded decision consistent with the site's own claim that the app tracks nothing.

### Key Entities

- **Landing page**: The site's front door. Explains the product, how it is set up, why it works this way; links to the repository, to the store (or its absence), and to the privacy policy.
- **Privacy policy page**: A standalone, permanently addressed statement of the app's data behaviour, dated, with a contact route and a link back to the landing page. This is the artifact Google Play requires.
- **Site source directory**: The place in the repository where the pages and their assets are kept, distinct from application source.
- **Published site**: What a visitor actually receives at the public address, derived from the site source directory.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The privacy policy is reachable at a permanent public address and is accepted in the Google Play listing without a policy-related rejection.
- **SC-002**: A visitor with scripts disabled can read 100% of the text on both pages.
- **SC-003**: Both pages display with zero horizontal scrolling at a 390-pixel-wide viewport and at desktop width.
- **SC-004**: 100% of links and interactive elements across both pages resolve to a working destination; zero dead or placeholder links remain.
- **SC-005**: Each page shows a distinct, descriptive title; zero pages display a generic or placeholder title.
- **SC-006**: Every factual claim in the privacy policy is traceable to a specific place in the app's source code, checked at publish time.
- **SC-007**: A content correction made by the maintainer is visible on the live site within 5 minutes with no manual upload.
- **SC-008**: Each page finishes loading and is readable within 2 seconds on a typical mobile connection.
- **SC-009**: The published site costs nothing to host.

## Assumptions

- The published address will be the repository host's project-site address for this repository; no custom domain is being purchased for this feature.
- The visual design, wording, and layout of both pages are already decided — they exist in the two design exports in `site/` — and this feature preserves that design rather than redesigning it. Changes are limited to what is needed for the pages to work as published web pages.
- The pages are static informational content. There is no form, no comment box, no search, no analytics, and nothing a visitor can submit; consequently the site collects nothing and needs no cookie or consent notice.
- Fonts and images are served from the site itself rather than a third party, so that a site claiming the app has no trackers does not itself call one. (Related to FR-026.)
- The app's Google Play listing address is not yet known. The call to action ships
  pointing at the Google Play store entry point, and swapping in the app-specific
  address later is a content edit of the kind User Story 3 already covers.
- The licence chosen for the repository is MIT. Adding the licence file is part of this
  feature because a landing-page claim depends on it; the choice grants anyone reuse,
  including in closed-source work.
- The maintainer's normal publish action is committing to the repository's main branch; no separate deployment credential or account is introduced.
- The contact route on the privacy policy is the maintainer's existing email address, published in plain text, accepting the spam risk that carries.
- Multi-language support, a blog, screenshots gallery, download hosting, and any store-listing assets are out of scope.
- This feature adds no code to the Android application and requests no permission, so the constitution's app-scope boundary and permission rules are untouched by it. Its only tie to the app is FR-016 and FR-019: the site's claims must match the app's source.
- The specific hosting mechanism and directory layout are implementation choices to be settled at planning time; this specification fixes only the outcomes (free, public, stable address, automatic republish, site files only).
