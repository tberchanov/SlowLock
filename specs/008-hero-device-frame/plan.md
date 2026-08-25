# Implementation Plan: Hero Device Frame Screenshot

**Branch**: `main` | **Date**: 2026-08-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/008-hero-device-frame/spec.md`

> **Branch note**: this feature adds no Android source. Like 006-site-publishing, it is a static
> content change planned and executed directly on `main` rather than on an `008-` branch.

## Summary

The hero "phone" in `site/index.html` is a hand-built imitation of the app's *Wait before opening*
screen, not a picture of it. Its interior is held apart by two `flex:1` boxes that only have
anything to distribute because the frame is a fixed `820px` tall. Below 860px that height becomes
`auto`, the two boxes collapse to nothing, and the screen's parts crush together — the reported
"collapsed" look (`site/index.html:196`, `:213`, `:265`, `:274`).

The fix is to stop drawing the screen and start showing it. The supplied capture is cropped to the
app's own content, encoded as a ~15 KB WebP, and placed inside the existing frame between the
page-drawn status bar and gesture pill. Both `flex:1` boxes and both fixed heights are deleted: the
frame's height becomes the sum of three fixed pieces plus an image that carries its own ratio, so it
is correct at every width with no media query and no `aspect-ratio` involved. Real `width`/`height`
attributes on the `<img>` reserve the box before the bytes land, so nothing jumps.

Replacing the drawing also corrects a drift nobody noticed: the imitation is **missing the
`5s` / `10s` / `30s` preset row** the app actually has (research R2). The site has been showing a
screen that does not exist.

Two things the maintainer should decide on rather than discover:

1. **The supplied file is a Telegram re-encode** (576×1280, 33 KB) of what was almost certainly a
   1080×2400 device capture. At the size the site uses it that is 1.47×, not the 2× a Retina screen
   wants. The original PNG would fix it; the plan ships without it if it is not forthcoming (R1).
2. **The desktop phone gets 79px taller** (412×820 → 412×899). Keeping the ratio and keeping both
   old dimensions are mutually exclusive; the plan keeps the width and the grid column, and lets the
   height follow (R6). Reversible in one line if the maintainer prefers the other trade.

## Technical Context

**Language/Version**: HTML5 + CSS, hand-maintained. No JavaScript in the delivered page. Python 3
with Pillow 10.4 (already present) for the one-off crop, and `cwebp` (already present) for the
encode — both are tools run once, not deliverables and not dependencies of the site.

**Primary Dependencies**: **None at view time.** No framework, no CDN, no image service, no
JavaScript. Unchanged from 006-site-publishing.

**Storage**: N/A — static files. One new binary asset under `site/assets/`.

**Testing**: No automated suite and no test framework added. Verification is
[manual-test-plan.md](./manual-test-plan.md) plus one scripted headless-Chrome geometry check
(research R8). `./gradlew test` and `assembleDebug` are untouched — no file under `app/` changes.

**Target Platform**: The public web, served from `https://tberchanov.github.io/SlowLock/`. Baseline:
current Chrome, Safari, Firefox on desktop and mobile; page must stay readable with JavaScript
disabled.

**Project Type**: Static site published from a subdirectory of an Android repository.

**Performance Goals**: Hero image ≤ 30 KB. **Actual: 14.8 KB** (576x1194 WebP q80), against the
33 KB JPEG it was cut from. Because it replaces nothing (the imitation was markup)
but is smaller than the 33 KB source it derives from, `index.html` **loses** roughly 2 KB of markup
and gains ~15 KB of image; the page stays comfortably inside the 60 KB budget set by
006-site-publishing (SC-008). Zero layout shift from the image (SC-004).

**Constraints**: Relative paths only — the site lives on a sub-path, never a domain root
(006 FR-005). No third-party request at view time (006 FR-026): the asset is self-hosted beside
`icon.svg` and `og.png`. No build step, no npm, no image pipeline in CI. No personal or incidental
device chrome may ship inside the image (FR-010).

**Scale/Scope**: One new asset, one `<img>` element, roughly 50 lines of markup deleted from
`site/index.html`, three CSS declarations changed. `site/privacy.html` is untouched.

**Resolved unknowns**: all — see [research.md](./research.md). R0 (root cause), R1 (source
resolution, with an ask), R2 (the drift), R3 (format), R4 (device chrome), R5 (shape-holding
mechanism), R6 (desktop footprint conflict), R7 (alt text), R8 (verification).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Evaluated against constitution **v1.2.0**. This feature ships no Android code, so four of the five
principles have no surface to violate; they are recorded rather than skipped.

| Principle | Verdict | Reasoning |
|---|---|---|
| I. Cooperative User, Not Adversary | **PASS** | The picture shows the wait being *chosen* — a slider from 1s to 30s and a chosen app. It depicts no lock, no enforcement and no penalty, which is exactly the screen the hero copy describes. |
| II. Simplicity First (YAGNI) | **PASS** | No dependency, no build step, no `<picture>` branch, no `srcset`, no second asset. The change is net-negative in markup: an imitation of a screen is deleted and one `<img>` replaces it. Pillow and `cwebp` are one-off local tools, not project dependencies. |
| III. Permission & Policy Minimalism | **PASS** | No app change, so no permission change. The feature *serves* the spirit of it: FR-010 forces the maintainer's own clock, battery level and NFC/alarm state out of the published file. |
| IV. Platform-Idiomatic Android | **N/A** | No Android code, no Compose, no system API. |
| V. Stable Identifiers | **N/A** | Nothing is persisted on a device. The asset's own stable identifier — its published path — is fixed by [contracts/hero-image-asset.md](./contracts/hero-image-asset.md). |

**"No backend" (Additional Constraints).** Honoured. The image is a file served from the same
origin as the page; no service resizes, hosts or transforms it at view time.

**Scope boundary (Additional Constraints).** The boundary enumerates what the *app* does. A picture
on the website is neither inside it nor a violation of it. The one real coupling is truth-telling,
and this feature strengthens it: FR-001 replaces a re-drawing that had already drifted from the app
(R2) with a capture of the app, so the site cannot silently misrepresent the product again.

**Build gate.** `./gradlew assembleDebug` and `./gradlew test` are unaffected — no file under
`app/` is touched. Both are re-run once at the end as cheap insurance that nothing under `site/`
leaked into the build (quickstart step 8).

**Testing expectations.** The mandated automated coverage (schedule logic, target resolution,
frozen persisted values) is app-side and untouched.

**No automated test may drive a device.** Respected, and worth being explicit about, because this
feature is about a screenshot. **No agent will capture, re-capture, or ask a device for anything.**
The higher-resolution source in R1 is *requested from the maintainer*, who exports it themselves.
The scripted check in R8 renders an HTML file in desktop Chrome; it touches no phone, no emulator
and no `connectedAndroidTest`.

**Version control is the maintainer's.** No task in this feature commits, pushes, or branches. The
end state is a modified working tree plus a new untracked asset, reported and left for the
maintainer — including `photo_2026-08-25_19-50-41.jpg`, which is currently staged at the repository
root and which quickstart step 7 asks the maintainer to decide about rather than deciding for them.

**Manual verification.** Satisfied by [manual-test-plan.md](./manual-test-plan.md): numbered cases,
each traced to a requirement, run by the maintainer on their own phone and laptop. The non-Pixel
OEM and Xiaomi Dual Apps release gates are app gates and are unaffected.

*Post-Phase 1 re-check: unchanged. The design added no dependency, no permission, no app file, and
no second deviation. The single tracked deviation (R6, the 79px) is a spec-internal conflict, not a
constitutional one.*

## Project Structure

### Documentation (this feature)

```text
specs/008-hero-device-frame/
├── plan.md                       # This file
├── spec.md                       # Feature specification
├── research.md                   # Phase 0: R0-R8
├── data-model.md                 # Phase 1: the asset and the frame as content entities
├── quickstart.md                 # Phase 1: end-to-end execution order
├── manual-test-plan.md           # Phase 1: numbered cases for the maintainer
├── contracts/
│   └── hero-image-asset.md       # Phase 1: the asset's frozen shape and the frame's markup contract
├── checklists/
│   └── requirements.md           # Spec quality checklist (from /speckit-specify)
└── tasks.md                      # Phase 2 output (/speckit-tasks — NOT created by /speckit-plan)
```

### Source (repository root)

```text
site/
├── index.html                    # MODIFIED — hero phone interior replaced; 3 CSS declarations changed
├── privacy.html                  # untouched
├── assets/
│   ├── icon.svg                  # untouched
│   ├── og.png                    # untouched
│   └── hero-wait-screen.webp     # NEW — the cropped, encoded app screenshot
└── fonts/                        # untouched

photo_2026-08-25_19-50-41.jpg     # source capture, currently staged at repo root;
                                  # maintainer decides whether it stays (quickstart step 7)

app/                              # untouched — no Kotlin, no Gradle, no manifest change
```

**Structure Decision**: The site keeps the flat, build-free shape 006-site-publishing established.
The new asset joins `site/assets/` beside `icon.svg` and `og.png` and is referenced by the relative
path `assets/hero-wait-screen.webp`, which resolves correctly under the `/SlowLock/` project-site
sub-path. No directory is added, no config file is added, and the GitHub Actions workflow that
publishes `site/` needs no change — it already copies the whole folder.

## Complexity Tracking

> One deviation. It is a conflict between two requirements of the spec itself, not a constitutional
> violation, and it is recorded here so the maintainer sees it at review rather than in the diff.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| The desktop hero phone grows from 412×820 to 412×899 (+79px tall), against SC-005's literal "the desktop hero at 1440px is visually unchanged apart from the picture's contents" | FR-003 requires the picture keep the screenshot's proportions. The cropped capture is 576×1194; inside the frame's 20px border, 40px status row and 26px gesture row, a 412px-wide frame is necessarily 899px tall. Keeping the ratio *and* both original dimensions is arithmetically impossible. | *Hold the height at 820px and narrow the frame to 374px* preserves the vertical rhythm but shrinks the phone and re-cuts the hero's `1fr 412px` grid column — the more conspicuous of the two changes in a two-column layout. *Crop or stretch the image to 1.99* violates FR-003 outright. The chosen trade keeps the grid column, the text column, the gap and the alignment exactly as they are, grows the phone symmetrically into existing padding, and lands at 2.18 — inside SC-001's 1.9–2.3 band and closer to a real phone than today's 1.99. Reversible in one CSS line if the maintainer disagrees; manual case M07 puts the decision in front of them. |
