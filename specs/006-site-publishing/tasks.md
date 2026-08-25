---
description: "Task list for Public Site & Privacy Policy"
---

# Tasks: Public Site & Privacy Policy

**Input**: Design documents from `/specs/006-site-publishing/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: No automated test tasks. The constitution allows JVM unit tests only, and this feature
adds no JVM code; verification is [manual-test-plan.md](./manual-test-plan.md) plus two scripted
checks (`curl | grep`, headless Chrome) that need no framework.

**Organization**: Tasks are grouped by user story. US1 (the privacy policy URL) is the MVP and
ships on its own.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1, US2, US3 — maps to the user stories in spec.md

## Path Conventions

Site source lives in `site/` at the repository root (plan.md → Structure Decision). The one-off
extraction script is a tool, not a deliverable: it runs from the scratchpad and is never committed
(research R2).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Get the real content out of the two bundle exports without touching the originals yet.

- [X] T001 Write the one-off extraction script in the scratchpad (NOT committed): parse `<script type="__bundler/template">` as JSON in each of `site/SlowLock Site.html` and `site/SlowLock Privacy Policy.html`, and write the enclosed HTML to `site/index.html` and `site/privacy.html` respectively (research R2)
- [X] T002 Extend the scratchpad script to decode the `<script type="__bundler/manifest">` entries whose `mime` is `font/woff2` and write each to `site/fonts/`, naming them by family, weight and subset (e.g. `site/fonts/instrument-sans-400-latin.woff2`) (research R3)
- [X] T003 Run the script and confirm the output: `site/index.html`, `site/privacy.html`, and 8 files under `site/fonts/` exist, and both HTML files open in a browser showing the design (styling will be broken until T007)
- [X] T004 [P] Create `site/.nojekyll` (empty file) so GitHub Pages skips Jekyll processing

**Checkpoint**: Real HTML is on disk. The bundles are still present and untouched.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Strip the runtime and rebuild the document shell. Every task here applies to BOTH
pages, and no user story work can start until they are done.

**⚠️ CRITICAL**: US1 and US2 both edit these same two files. Complete this phase first.

- [X] T005 Remove the design-canvas runtime from `site/index.html` and `site/privacy.html`: delete the `<script src="…uuid…">` tag, the `<x-dc>` and `<helmet>` wrapper elements (keeping their children), and the trailing `<script type="text/x-dc">` block (research R2)
- [X] T006 Remove the two `<link rel="preconnect">` tags pointing at `fonts.googleapis.com` and `fonts.gstatic.com` from both pages — the fonts are self-hosted and these would open third-party connections for nothing (FR-026, research R3)
- [X] T007 Repoint every `@font-face` `src: url("…uuid…")` in both pages at the matching `fonts/<name>.woff2` file from T002, leaving each rule's `unicode-range`, `font-weight` and `font-display` exactly as authored (research R3)
- [X] T008 [P] Replace `sc-camel-view-box="0 0 24 24"` with `viewBox="0 0 24 24"` in `site/index.html` (2 occurrences — the Play Store glyph and the back arrow); without this the SVGs have no viewBox and will not scale (research R2)
- [X] T009 [P] Delete the `data-screen-label="Site · Hero screen"` attribute from `site/index.html` (contracts/page-content.md)
- [X] T010 Add a `<style>` block to the `<head>` of each page and give the containers listed in the research R4 table a class name each; move only those containers' layout declarations into the block, leaving all other inline styles untouched (FR-007, research R4)
- [X] T011 Add a `@media (max-width: 860px)` section to each `<style>` block implementing the mobile column of the research R4 table: hero and "Why" and stat grids to one column, "How it works" to one column, phone mockup to `width:100%; max-width:412px; height:auto`, section padding 48px→20px and 96px→56px, hero heading to `clamp()`, header nav wrapping (FR-007, SC-003)
- [X] T012 Add a max-width guard to each `<style>` block so content does not stretch across an ultrawide window (spec Edge Cases, manual case M7)
- [X] T013 [P] Create `site/assets/icon.svg` from the lock mark already drawn inline in both page headers — `#17150F` rounded square, `#C9821F` ring, notch — as a standalone square SVG (research R6)
- [X] T014 Add to the `<head>` of both pages: `<meta charset="utf-8">`, `<meta name="viewport" content="width=device-width, initial-scale=1">`, and `<link rel="icon" href="assets/icon.svg">` (FR-008, contracts/page-content.md)

**Checkpoint**: Both pages are script-free, self-hosted, responsive and correctly headed. Verify
now with `grep -c '<script' site/index.html site/privacy.html` → **0** for both (manual case M9).

---

## Phase 3: User Story 1 - Privacy policy URL for Google Play (Priority: P1) 🎯 MVP

**Goal**: A permanent public address showing an accurate, finished privacy policy that a Play
reviewer — or a reviewer's non-JavaScript tooling — can read.

**Independent test**: Publish and open `https://tberchanov.github.io/SlowLock/privacy.html` on a
device that has never seen the project; the full policy is readable and contains no draft wording.
This story is a valid ship on its own — the landing page can follow later.

- [X] T015 [US1] **Remove the "DRAFT · NOT LEGALLY REVIEWED" block** from `site/privacy.html` — the bordered callout containing that label and the "drafted from a description of the app's behaviour, not by a lawyer" sentence. This is the single most important edit in the feature (FR-015, manual case M3)
- [X] T016 [US1] Fix both back-links in `site/privacy.html`: the header link and the footer link currently point at `SlowLock Site.dc.html`, a name that has never existed here — change both to `href="index.html"` (FR-005, research R5)
- [X] T017 [US1] Set `<title>SlowLock — Privacy Policy</title>` and add `<meta name="description">` describing the policy in one sentence, replacing the current `<title>Bundled Page</title>` (FR-008)
- [X] T018 [US1] [P] Add `og:title`, `og:description`, `og:url` (`https://tberchanov.github.io/SlowLock/privacy.html`) and `og:type` meta tags to `site/privacy.html` (FR-008)
- [X] T019 [US1] Re-verify every claim in `site/privacy.html` against the claim→source table in [data-model.md](./data-model.md): no `INTERNET` permission, no analytics dependency, `<queries>` scoped to MAIN/LAUNCHER, `SharedPreferences` storage, `allowBackup="false"`. Correct the page, not the table, if anything has drifted (FR-016)
- [X] T020 [US1] Confirm or update the "Last updated" date in `site/privacy.html` to the date the policy text was last actually changed (FR-014)
- [X] T021 [US1] Verify the contact address `tberchanov@gmail.com` and the repository link in the "Verifying this yourself" section both resolve (FR-013, FR-016)
- [X] T022 [US1] Run manual cases M3, M4, M5 and M9 from [manual-test-plan.md](./manual-test-plan.md) against `python3 -m http.server 8000 --directory site`: no draft wording anywhere, full text renders with JavaScript disabled, `curl … | grep -c "collects nothing"` ≥ 1, no authoring leftovers (FR-006, FR-015, SC-002)
- [X] T023 [US1] Run manual case M6 against `site/privacy.html` at 390 px: no horizontal scrolling, stat pair collapsed to one column (FR-007, SC-003)

**Checkpoint**: The policy page is correct and finished locally. It goes live in Phase 5 (T036).

---

## Phase 4: User Story 2 - Landing page (Priority: P2)

**Goal**: A visitor on a phone learns what SlowLock is, how it is set up, and why it uses a pause
rather than a block — and every link and button leads somewhere real.

**Independent test**: Open the landing address on a phone-sized screen; the page explains the
product, reads without horizontal scrolling, and no element is dead.

- [X] T024 [US2] Fix the privacy link in the `site/index.html` footer: it points at `SlowLock Privacy.dc.html`, which does not exist — change it to `href="privacy.html"` (FR-005, research R5)
- [X] T025 [US2] Set `<title>SlowLock — A pause between you and the app</title>` and add a one-sentence `<meta name="description">`, replacing `<title>Bundled Page</title>` (FR-008)
- [X] T026 [US2] [P] Add `og:title`, `og:description`, `og:url` (`https://tberchanov.github.io/SlowLock/`), `og:type` and `og:image` meta tags to `site/index.html` (FR-008)
- [X] T027 [US2] Convert the two dead `style-hover="…"` attributes in `site/index.html` into real `:hover` rules in the page's `<style>` block — the Play button (`background:#B87316`) and the repository button (`background:#17150F; color:#F3F0EA`). They render nothing today; zero references exist in any bundled script (FR-009, research R2)
- [X] T028 [US2] Add `<!-- TODO: replace with the app's Play listing URL -->` immediately above the call-to-action anchor in `site/index.html`, leaving its `https://play.google.com/store` href in place until the listing address is known (FR-021, research R8)
- [X] T029 [US2] Verify the repository link `https://github.com/tberchanov/SlowLock` resolves, and re-check each landing-page claim against the table in [contracts/page-content.md](./contracts/page-content.md): 1–30 seconds, three icon treatments, no accounts, no analytics, no network calls (FR-019, FR-020)
- [X] T030 [US2] Create `LICENSE` at the repository root with the standard unmodified MIT text, year 2026, copyright holder per research R7 (defaults to `tberchanov` unless the maintainer supplies a name) — without this the page's "Open source, all of it" is not true (FR-022)
- [X] T031 [US2] [P] Add a licence line (`[MIT](LICENSE)`) to `README.md` if one exists at the repository root (FR-022, research R7)
- [X] T032 [US2] Render `site/assets/og.png` at 1200×630 with headless Chrome from an SVG built from the lock mark and wordmark, per the command in [quickstart.md](./quickstart.md) §4. If rasterizing fails, remove the `og:image` tag added in T026 rather than leaving a broken reference (research R6)
- [X] T033 [US2] Run manual cases M1, M2, M6, M7 and M8 from [manual-test-plan.md](./manual-test-plan.md): bare address serves the landing page with the right title and favicon, navigation works both directions, no horizontal scroll at 390 px, desktop unchanged at 1440 px and contained at 2560 px, both buttons show hover, and the Network panel shows zero third-party requests (FR-003, FR-005, FR-007, FR-009, FR-026)

**Checkpoint**: Both pages are complete and verified locally. Nothing is published yet.

---

## Phase 5: User Story 3 - Publishing and updating (Priority: P3)

**Goal**: The site is live, and a content correction reaches it within 5 minutes with no manual
upload.

**Independent test**: Change one visible word, push, and see it live within 5 minutes.

- [X] T034 [US3] Create `.github/workflows/pages.yml` exactly as given in [quickstart.md](./quickstart.md) §9: `on.push.branches: [main]` with `paths: ['site/**', '.github/workflows/pages.yml']` plus `workflow_dispatch`; `permissions` for `contents: read`, `pages: write`, `id-token: write`; a `pages` concurrency group; and checkout → configure-pages → upload-pages-artifact (`path: site`) → deploy-pages. No build step (research R1, contracts/published-urls.md)
- [X] T035 [US3] Commit the site, the workflow and the licence to `main` and push
- [X] T036 [US3] Set **Settings → Pages → Source: GitHub Actions** in the GitHub repository, then watch the "Deploy site to Pages" run complete in the Actions tab (research R1)
- [X] T037 [US3] Run manual cases M10 and M11: both live addresses return `200`, and `curl -s https://tberchanov.github.io/SlowLock/privacy.html | grep -c "collects nothing"` returns ≥ 1 — this is what a Play reviewer's tooling sees (FR-001, FR-002, FR-006, SC-001)
- [ ] T038 [US3] Run manual case M12: open both live addresses on a real Android device over mobile data, navigate between them, confirm no horizontal scrolling and each page readable within about 2 seconds (FR-007, SC-003, SC-008)
- [X] T039 [US3] Run manual case M14: change one visible word in `site/index.html`, push, and confirm the live page updates within 5 minutes; revert the probe afterwards (FR-023, SC-007)
- [X] T040 [US3] Run manual case M13: push a commit touching only files under `app/` and confirm no "Deploy site to Pages" run is triggered — this is what the `paths:` filter buys (FR-023, User Story 3 scenario 2)

**Checkpoint**: The site is live at its contracted addresses and the update loop is proven.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T041 [P] Delete the two bundle exports: `site/SlowLock Site.html` and `site/SlowLock Privacy Policy.html`. Keeping them would leave two files that look like the site but are not it (plan.md → Structure Decision)
- [X] T042 [P] Delete `site/PUBLISHING.md` — its content now lives in this feature's spec and plan, and anything left in `site/` is served publicly, which would expose an internal to-do list with open decisions at `https://tberchanov.github.io/SlowLock/PUBLISHING.md` (research R9)
- [X] T043 Confirm the Android app is untouched: `git status --short app/ gradle/` produces no output, and `./gradlew test assembleDebug` passes (constitution build gate, FR-024)
- [X] T044 Verify FR-025 on the live site: only files from `site/` are served — `https://tberchanov.github.io/SlowLock/app/build.gradle.kts` and `.../specs/` return 404
- [ ] T045 Submit the policy URL in Play Console → *Policy* → *App content* → *Privacy policy*: `https://tberchanov.github.io/SlowLock/privacy.html` (SC-001)
- [ ] T046 Fill the Play Console **Data safety** form as no data collected and no data shared, and confirm it does not contradict any statement on the published policy page (FR-017)
- [X] T047 Record the manual-test-plan results (M1–M14 pass/fail) in [manual-test-plan.md](./manual-test-plan.md) or the feature's completion note (constitution: manual verification)

---

## Dependencies & Execution Order

### Phase dependencies

```text
Phase 1 (Setup, T001-T004)
   └─▶ Phase 2 (Foundational, T005-T014)   ← BLOCKS everything below
          ├─▶ Phase 3 US1 (T015-T023)  ─┐
          └─▶ Phase 4 US2 (T024-T033)  ─┤  US1 and US2 touch different files
                                        └─▶ Phase 5 US3 (T034-T040)
                                                └─▶ Phase 6 Polish (T041-T047)
```

### Story dependencies

- **US1 (P1)** — depends only on Phase 2. Independently shippable: publish `privacy.html` and the
  Play requirement is met even with no landing page.
- **US2 (P2)** — depends only on Phase 2. Touches `site/index.html`, `LICENSE`, `README.md`,
  `site/assets/og.png`; no file overlap with US1, so the two phases can be worked in either order
  or concurrently.
- **US3 (P3)** — depends on at least one page being finished. In practice both are, so it runs
  after Phase 4.

### Within-phase ordering

- T007 depends on T002 (fonts must exist before rules point at them).
- T011 and T012 depend on T010 (the `<style>` block and class names must exist first).
- T027 depends on T010 (its `:hover` rules go in that block).
- T032 depends on T013 (the OG image is built from the same mark).
- T041 must not run before T003 and T022/T033 confirm the converted pages are good — deleting the
  bundles is the point of no return.
- T039 and T040 require T036 (Pages must be serving before an update can be observed).

### Parallel opportunities

- **Phase 1**: T004 is independent of T001–T003.
- **Phase 2**: T008, T009 and T013 are `[P]` — different edits, no shared dependency.
- **Phase 3 / Phase 4**: the two story phases are file-disjoint and can run concurrently. Within
  them, T018 and T026 are `[P]` (different files), and T030/T031 touch `LICENSE`/`README.md`, not
  the pages.
- **Phase 6**: T041 and T042 are `[P]`.

---

## Implementation Strategy

**MVP = Phase 1 + Phase 2 + Phase 3 (US1), then publish.** That is T001–T023 plus T034–T037: the
privacy policy live at its permanent address, which is the only part of this feature that blocks a
Play release. Everything after it improves the site rather than unblocking the app.

**Increment 2**: Phase 4 (US2) — the landing page and the licence.

**Increment 3**: Phases 5 and 6 — prove the update loop, delete the bundles and the working note,
submit to Play.

**One input still open**: the MIT copyright holder line in T030. It defaults to `tberchanov` and
blocks nothing; supply a name and it is a one-line edit.
