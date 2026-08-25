# Manual Test Plan: Public Site & Privacy Policy

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

Required by the constitution ("Every feature MUST ship with a written manual test plan whose cases
are numbered and traceable to requirements"). Cases M1–M9 run against a local server before
pushing; M10–M14 run against the live site.

**Local server**: `python3 -m http.server 8000 --directory site`

---

## Pre-publish (local)

### M1 — Bare address serves the landing page
**Requirement**: FR-003, SC-005
**Steps**: Open `http://localhost:8000/`.
**Pass**: The landing page renders. Tab title reads "SlowLock — A pause between you and the app",
not "Bundled Page". Favicon is the lock mark.

### M2 — Navigation works in both directions
**Requirement**: FR-005, SC-004
**Steps**: From the landing page, click "Privacy policy" in the footer. From the privacy page,
click both "← Back to site" (header) and "Back to site" (footer).
**Pass**: All three links resolve. No 404, no `.dc.html` in any address bar.

### M3 — No draft banner survives
**Requirement**: FR-015
**Steps**: Read the privacy page top to bottom; then
`grep -ri 'draft\|not legally reviewed\|placeholder' site/`.
**Pass**: Nothing on the page and no grep match. **This case blocks publishing on failure.**

### M4 — Content is readable with JavaScript disabled
**Requirement**: FR-006, SC-002
**Steps**: DevTools → Settings → Debugger → *Disable JavaScript*. Hard-reload both pages.
**Pass**: Both pages render completely — every section, every heading, the full policy text.
Nothing is blank, nothing says "Unpacking…".

### M5 — Content survives a scriptless fetch
**Requirement**: FR-006, SC-002, SC-006
**Steps**:
```bash
curl -s http://localhost:8000/privacy.html | grep -c "collects nothing"
curl -s http://localhost:8000/ | grep -c "A pause between you and the app"
```
**Pass**: Both print `1` or more.

### M6 — Phone layout
**Requirement**: FR-007, SC-003
**Steps**: DevTools device toolbar at 390 × 844. Scroll both pages fully.
**Pass**: No horizontal scrollbar, no content clipped at either edge, the phone mockup fits inside
the viewport, and the three-step and two-column sections have collapsed to one column.

### M7 — Desktop layout unchanged
**Requirement**: spec Edge Cases
**Steps**: View both pages at 1440 px and at 2560 px.
**Pass**: The design matches the original bundles at 1440 px. At 2560 px the content stays within
its max-width rather than stretching across the window.

### M8 — No dead interactive elements, no third-party requests
**Requirement**: FR-009, FR-026, SC-004
**Steps**: Hover both call-to-action buttons. Open DevTools → Network, reload both pages, sort by
domain.
**Pass**: Both buttons show a visible hover state. Every request is to `localhost`; zero requests to
`fonts.googleapis.com`, `fonts.gstatic.com`, `unpkg.com`, or any other host.

### M9 — No authoring leftovers
**Requirement**: contracts/page-content.md
**Steps**:
```bash
grep -rn 'sc-camel\|style-hover\|data-screen-label\|x-dc\|helmet\|__bundler' site/
grep -c '<script' site/index.html site/privacy.html
```
**Pass**: First command: no matches. Second: `0` for both files.

---

## Post-publish (live)

### M10 — The site is live at the contracted addresses
**Requirement**: FR-001, FR-002, SC-001
**Steps**:
```bash
curl -s -o /dev/null -w '%{http_code}\n' https://tberchanov.github.io/SlowLock/
curl -s -o /dev/null -w '%{http_code}\n' https://tberchanov.github.io/SlowLock/privacy.html
```
**Pass**: Both print `200`.

### M11 — The live policy is readable without scripts
**Requirement**: FR-006, SC-002
**Steps**: `curl -s https://tberchanov.github.io/SlowLock/privacy.html | grep -c "collects nothing"`
**Pass**: Prints `1` or more. This is what a Play reviewer's tooling sees.

### M12 — On a real phone
**Requirement**: FR-007, SC-003, SC-008
**Steps**: Open both live addresses on the maintainer's Android device over mobile data. Navigate
between them.
**Pass**: No horizontal scrolling, text legible without zooming, each page readable within about
2 seconds, both links work.

### M13 — An app-only commit does not redeploy the site
**Requirement**: FR-023 / User Story 3 scenario 2
**Steps**: Push a commit that touches only files under `app/`. Watch the Actions tab.
**Pass**: No "Deploy site to Pages" run is triggered.

### M14 — A content edit reaches the live site
**Requirement**: FR-023, SC-007, User Story 3
**Steps**: Change one visible word in `site/index.html`, commit, push to `main`. Start a timer.
Reload the live landing page.
**Pass**: The change is visible within 5 minutes, with no manual upload step. Revert afterwards if
the change was only a probe.

---

## Traceability

| Requirement | Cases |
|---|---|
| FR-001, FR-002 | M10 |
| FR-003 | M1 |
| FR-005 | M2 |
| FR-006 | M4, M5, M11 |
| FR-007 | M6, M12 |
| FR-008 | M1 |
| FR-009 | M8 |
| FR-015 | M3 |
| FR-023 | M13, M14 |
| FR-026 | M8 |
| SC-002 | M4, M5, M11 |
| SC-003 | M6, M12 |
| SC-004 | M2, M8 |
| SC-007 | M14 |

**Not covered here** — verified by inspection during implementation rather than by a case:
FR-010 to FR-014 and FR-016 to FR-022 (content accuracy against source; see the claim table in
[data-model.md](./data-model.md)), FR-004, FR-024, FR-025.

---

## Results

Run on 2026-08-25 against `python3 -m http.server --directory site` (M1–M9) using headless Chrome
141 for the rendered checks. M10–M14 are post-publish and are recorded once the site is live.

| Case | Result | Evidence |
|---|---|---|
| M1 | **PASS** | `/` serves `index.html`, `200`; `<title>SlowLock — A pause between you and the app</title>`; `<link rel="icon" href="assets/icon.svg">` serves `200`. |
| M2 | **PASS** | All internal hrefs are `index.html` / `privacy.html`; no `.dc.html` remains anywhere (`grep` → no match). Three back-links on the policy page (logo, header, footer) all repointed. |
| M3 | **PASS** | Draft callout removed; `grep -ri 'draft\|not legally reviewed\|placeholder' site/` → no match. |
| M4 | **PASS** | Trivially satisfied: `grep -c '<script' site/index.html site/privacy.html` → `0` for both, so there is no script to disable. |
| M5 | **PASS** | `curl … privacy.html \| grep -c "collects nothing"` → `3`; `curl … / \| grep -c "A pause between you and the app"` → `3`. |
| M6 | **PASS** | `documentElement.scrollWidth <= clientWidth` at 320, 360, 390, 412, 480, 600, 768, 860, 861 and 1024 px on both pages. Rendered at 500 px: hero, "How it works", "Why" and the policy stat pair are all one column; the phone mockup fits. |
| M7 | **PASS** | 1440 px matches the bundle design. At 2560 px `.page { max-width: 1600px }` holds the content centred rather than stretching. |
| M8 | **PASS** | `.btn-play:hover` and `.btn-repo:hover` are real rules (the dead `style-hover` attributes are gone). No `http(s)` resource reference of any kind in either page — fonts, favicon and preview image are all local. |
| M9 | **PASS** | `grep -rn 'sc-camel\|style-hover\|data-screen-label\|x-dc\|helmet\|__bundler' site/` → no match; `grep -c '<script'` → `0`, `0`. |
| M10 | pending | Post-publish. |
| M11 | pending | Post-publish. |
| M12 | pending | Post-publish, maintainer's device. |
| M13 | pending | Post-publish. |
| M14 | pending | Post-publish. |

**Build gate**: `git status --short app/ gradle/` → empty; `./gradlew test assembleDebug` →
`BUILD SUCCESSFUL`.
