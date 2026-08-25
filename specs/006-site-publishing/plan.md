# Implementation Plan: Public Site & Privacy Policy

**Branch**: `main` | **Date**: 2026-08-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-site-publishing/spec.md`

> **Branch note**: this feature adds no Android source. It is a documentation-and-static-content
> change, planned and executed directly on `main` rather than on a `006-` branch.

## Summary

Two Claude Design **bundle exports** currently sit in `site/`. They are not web pages: each is a
~270 KB shell whose markup is base64-packed and rendered client-side by React 18 plus an `<x-dc>`
runtime. This feature converts them into two plain static pages — `site/index.html` and
`site/privacy.html` — and publishes them from this public repository at
`https://tberchanov.github.io/SlowLock/`, so the privacy policy has the permanent public address
Google Play requires.

The conversion is mechanical: the payload inside each bundle is already ordinary HTML with inline
styles. Unpack it, drop the runtime, write the bundled woff2 subsets out as real files, and the
pages become ~15 KB of script-free HTML that reads without JavaScript and survives a crawler fetch.

Four defects go with it, all confirmed in the current files: **every internal link is broken** (they
point at `.dc.html` names that do not exist), the privacy page renders a **"DRAFT · NOT LEGALLY
REVIEWED"** banner, both `<title>`s say **"Bundled Page"**, and there is **not one `@media` query**
in either file despite a hard-coded 412 px phone mockup — on a phone, which is the entire audience,
the layout overflows.

Publishing keeps the folder named `site/` and uses a GitHub Actions workflow, because branch deploy
offers only `/` and `/docs` in its dropdown. One repository-level change rides along: an **MIT
`LICENSE`**, without which the landing page's "open source, all of it" is not true (FR-022).

Every privacy claim on the page was checked against the app source and holds. The site is the first
artifact in this repository that is not the app; it touches no Kotlin, requests no permission, and
adds no dependency to `:app`.

## Technical Context

**Language/Version**: HTML5 + CSS. No JavaScript in the delivered pages. Python 3 (already present)
for the one-off bundle extraction; that script is a tool, not a deliverable.

**Primary Dependencies**: **None at view time.** No framework, no CDN, no webfont service, no
analytics. Fonts are extracted from the existing bundles and self-hosted (research R3, FR-026).

**Storage**: N/A — static files. No cookies, no local storage, nothing a visitor can submit.

**Testing**: No automated suite. Verification is a written manual test plan
([manual-test-plan.md](./manual-test-plan.md)) plus two scripted checks that need no framework:
`curl | grep` for script-free content (SC-002) and a headless-Chrome render at 390 px (SC-003).
`./gradlew test` and `assembleDebug` are unaffected — no file under `app/` changes.

**Target Platform**: The public web. Baseline: current Chrome, Safari, Firefox on desktop and
mobile; content must remain readable with JavaScript disabled entirely.

**Project Type**: Static two-page site published from a subdirectory of an Android repository.

**Performance Goals**: Each page under 60 KB total including fonts; readable within 2 s on a typical
mobile connection (SC-008). Down from ~270 KB and a React boot today.

**Constraints**: Served from a project-site sub-path (`/SlowLock/`), never a domain root — every
internal link must be relative (FR-005). No page filename may contain a space (FR-004). No
third-party request at view time (FR-026). Zero hosting cost (SC-009).

**Scale/Scope**: Two pages, one workflow file, one licence file, ~8 font files, ~15 KB of markup
each. No build step.

**Resolved unknowns**: all. See [research.md](./research.md) — R1 (hosting mechanism), R2 (how the
bundle is unpacked), R3 (fonts), R4 (how to make inline-styled markup responsive), R5 (sub-path
links), R6 (favicon and link preview image), R7 (licence text and the copyright line), R8 (the Play link placeholder),
R9 (what happens to `site/PUBLISHING.md`).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Evaluated against constitution v1.1.0. This feature ships no Android code, so four of the five
principles have no surface to violate; they are recorded rather than skipped.

| Principle | Verdict | Reasoning |
|---|---|---|
| I. Cooperative User, Not Adversary | **PASS** | The landing page sells the pause, not enforcement, and the "Why" section states plainly that nothing is forbidden. No copy claims the app blocks or polices anything. |
| II. Simplicity First (YAGNI) | **PASS (one deviation, tracked)** | No framework, no static-site generator, no build step, no npm. The one deviation is a CI workflow where a folder rename would need none — see Complexity Tracking. |
| III. Permission & Policy Minimalism | **PASS** | No app change, so no permission change. The feature *serves* this principle: FR-016 forces every policy claim to be re-checked against source before publishing, and FR-017 forces the page and the Play Data safety form to agree. |
| IV. Platform-Idiomatic Android | **N/A** | No Android code, no UI, no system API. |
| V. Stable Identifiers | **N/A** | Nothing is persisted on a device. The site's own stable identifier — its published URL — is fixed by FR-002 and the contract in `contracts/published-urls.md`. |

**"No backend" (Additional Constraints).** Honoured, and extended: the site has no server, no form,
no account, and — by the FR-026 decision to self-host fonts — makes no third-party request at view
time. A page that claims the app calls nothing must not itself call Google Fonts.

**Scope boundary (Additional Constraints).** The boundary enumerates what the *app* does; a website
is neither inside it nor a violation of it. Nothing here adds app behaviour. The only coupling is
truth-telling: FR-016 and FR-019 require the site's claims to match `app/src/main`.

**Build gate.** `./gradlew assembleDebug` and `./gradlew test` are unaffected — no file under
`app/` is touched. The gate is re-run once at the end regardless, as cheap insurance that the
`LICENSE` addition and workflow file changed nothing (quickstart step 8).

**Testing expectations.** The mandated automated coverage (schedule logic, target resolution, frozen
persisted values) is app-side and untouched. No instrumented suite is added — the prohibition is
respected trivially, since nothing here runs on a device.

**Manual verification.** Satisfied by [manual-test-plan.md](./manual-test-plan.md): 14 numbered
cases, each traced to a requirement. The non-Pixel OEM and Xiaomi Dual Apps release gates are app
gates and are unaffected by this feature.

*Post-Phase 1 re-check: unchanged. The design added no dependency, no permission, no app file, and
no second deviation.*

## Project Structure

### Documentation (this feature)

```text
specs/006-site-publishing/
├── plan.md                    # This file
├── spec.md                    # Feature specification
├── research.md                # Phase 0: R1-R9
├── data-model.md              # Phase 1: the site's content entities
├── quickstart.md              # Phase 1: end-to-end execution order
├── manual-test-plan.md        # Phase 1: 14 numbered verification cases
├── contracts/
│   ├── published-urls.md      # The addresses this feature promises to keep stable
│   └── page-content.md        # What each page must contain, and what it must never contain
└── checklists/
    └── requirements.md        # Spec quality checklist (18/18)
```

### Source (repository root)

```text
site/                          # The site source. Published as-is; no build step.
├── index.html                 # Landing page          (was "SlowLock Site.html")
├── privacy.html               # Privacy policy        (was "SlowLock Privacy Policy.html")
├── .nojekyll                  # Skip Jekyll processing
├── fonts/                     # woff2 subsets extracted from the bundles (research R3)
│   ├── instrument-sans-*.woff2
│   └── jetbrains-mono-*.woff2
└── assets/
    ├── icon.svg               # Favicon: the lock mark already drawn in the page header
    └── og.png                 # 1200x630 link-preview image (research R6)

.github/workflows/
└── pages.yml                  # Uploads site/ to GitHub Pages on push to main

LICENSE                        # MIT (FR-022); copyright line per research R7
```

**Structure Decision**: The site stays at `site/`, published by a GitHub Actions workflow rather
than by branch deploy, because Pages' branch-deploy source offers only `/` and `/docs` — there is no
free-text folder field, so `/docs` would have forced a rename the maintainer declined. Everything
under `site/` is published and nothing else is (FR-025); the Android sources are untouched by the
deployment and are separate from the site source (FR-024). Both original bundle files are deleted
once their converted output is verified — keeping them would leave two files that look like the site
but are not it.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| A CI workflow (`.github/workflows/pages.yml`) where zero configuration would do | GitHub Pages' branch-deploy source is a fixed dropdown with exactly two entries, `/` and `/docs`. Publishing a directory named anything else — here, `site/` — is only possible through the Actions source, which requires a workflow file. | `git mv site docs` plus branch deploy needs no workflow, no Actions minutes, and no `permissions:` block. It was rejected by the maintainer, who asked to keep the folder named `site/`. The cost is one 20-line file with no build step and a `paths:` filter so it runs only when the site changes; it is also the path that already works if the site ever grows a build step. Recorded as a deviation from Principle II rather than argued away. |
