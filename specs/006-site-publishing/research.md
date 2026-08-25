# Phase 0 Research: Public Site & Privacy Policy

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-08-25

All findings below were verified against the actual files in `site/` and `app/src/main` in this
repository, not assumed.

---

## R1 — How the site gets published

**Decision**: Keep the directory named `site/`. Publish with a GitHub Actions workflow
(`actions/upload-pages-artifact` → `actions/deploy-pages`), with Pages' Source set to
**GitHub Actions**.

**Rationale**: Pages' *Deploy from a branch* source presents a fixed folder dropdown containing
exactly two entries — `/` (root) and `/docs`. There is no free-text field, so a directory named
`site/` cannot be selected there at all. The Actions source has no such restriction: the workflow
names the directory it uploads. The maintainer explicitly declined renaming to `docs/`.

The workflow carries a `paths: ['site/**', '.github/workflows/pages.yml']` filter so ordinary
Android commits do not spend Actions minutes or redeploy the site (FR-023's counterpart in User
Story 3, scenario 2). Public repositories get Pages and Actions at no cost (SC-009).

**Alternatives considered**:
- `git mv site docs` + branch deploy — strictly simpler, zero config, and would have removed the
  Principle II deviation. Rejected by the maintainer's choice of folder name.
- Publish from repository root — would serve the entire repository, including Android sources and
  Gradle files, at the public address. Violates FR-025.
- A `gh-pages` branch built by a script — the pre-Actions pattern; adds a branch and a build step
  for no gain.
- A symlink `docs -> site` — does not work; Pages does not follow it.

---

## R2 — How the bundles are unpacked

**Decision**: A one-off Python 3 script, run from the scratchpad, not committed. It reads each
bundle, parses the `<script type="__bundler/template">` JSON payload, and writes the enclosed HTML
out as a file; a second pass writes each `font/woff2` entry of `<script type="__bundler/manifest">`
to `site/fonts/`. Only the output is committed.

**Rationale**: The bundle format is fully self-describing and was confirmed by reading it: a
`manifest` (uuid → `{mime, compressed, data}` base64, gzip for scripts, raw for fonts), a
`template` (a JSON-encoded HTML string), `page_order`, and `ext_resources` (React 18.3.1 and
React-DOM, vendored from unpkg). The template's markup is already ordinary HTML with inline
`style` attributes — no JSX, no component tree, no data binding. The trailing
`<script type="text/x-dc">` block on the landing page defines `class Component extends DCLogic`
with `renderVals() { return {}; }` — an empty logic hook. Nothing is computed at runtime, so
nothing is lost by deleting the runtime.

Committing the extraction script would imply the bundles remain the source of truth. They do not:
after this feature the plain HTML is the source, edited directly, and the bundles are deleted.

**Two mechanical fixes the unpacked markup needs**, both found by reading it:
1. `sc-camel-view-box="0 0 24 24"` → `viewBox="0 0 24 24"`. The runtime lowercases camelCase SVG
   attributes on the way in and restores them on render (`CAMEL_ATTR = "sc-camel-"` in the
   runtime); without the runtime the SVGs would have no viewBox and would not scale. Two
   occurrences, both on the landing page.
2. `style-hover="background:#B87316"` (2 occurrences) → real CSS `:hover` rules. Grepping all three
   bundled scripts for `style-hover`/`styleHover` returns **zero** hits — the attribute is a canvas
   authoring feature the export does not implement, so these hover states are already dead in the
   bundles today. Converting them is a fix, not a port.

**Alternatives considered**:
- Open each bundle in a browser and copy the rendered DOM — non-reproducible, and silently bakes in
  whatever the runtime normalised.
- Retype the pages by hand from the rendered design — invites transcription drift in a privacy
  policy, which is the one document where wording accuracy is the product.
- Keep the bundles and publish them as-is — fails FR-006 (no content without scripts), and ships
  210 KB of React to display static text.

---

## R3 — Fonts

**Decision**: Extract the woff2 subsets already inside the bundles to `site/fonts/` and repoint the
existing `@font-face` rules at them. Keep the `unicode-range` splits exactly as they are.

**Rationale**: Both bundles carry the complete set — Instrument Sans (400/500/600) and JetBrains
Mono (400/500), latin and latin-ext, already subsetted, 2–42 KB each — together with the
`@font-face` CSS that references them. Nothing needs converting, subsetting or downloading. The
same families ship inside the app itself (`app/src/main/res/font/*.ttf`, 5 files), so the site and
the app stay visually identical by construction.

The two `<link rel="preconnect">` tags pointing at `fonts.googleapis.com` and `fonts.gstatic.com`
are removed: they are leftovers from the design environment, and with the fonts self-hosted they
would open connections to a third party for nothing.

**Alternatives considered**:
- Link Google Fonts — two fewer files, but a page asserting "no analytics, no third-party SDKs, the
  app calls nothing" would itself call Google on every view. Rejected on FR-026 and on the
  credibility of the claim it undermines.
- Convert the app's `.ttf` files to woff2 — needs a converter that is not installed
  (`woff2_compress` is absent), and would ship full unsubsetted faces, several times larger than the
  subsets already in hand.
- System fonts only — free, but discards the design the spec's Assumptions say to preserve.

---

## R4 — Making inline-styled markup responsive

**Decision**: Add one `<style>` block per page in `<head>`. Give the handful of containers that must
change a class name, and put the layout rules — desktop *and* mobile — in that block, leaving the
rest of the inline styles untouched.

**Rationale**: There are **zero** `@media` queries in either bundle, and a media query cannot target
an inline `style` attribute, so responsiveness cannot be retrofitted without a stylesheet. Keeping
the change surgical matters: the design is fixed by the spec's Assumptions, and a wholesale
rewrite into classes would be a redesign in disguise.

The containers that need it, all confirmed in the markup:

| Element | Desktop as authored | Under `max-width: 860px` |
|---|---|---|
| Hero section | `grid-template-columns: 1fr 412px` | single column, mockup below text |
| Phone mockup | `width: 412px; height: 820px` | `width: 100%; max-width: 412px; height: auto` |
| "How it works" | `repeat(3, 1fr)` | single column |
| "Why" panel | `1fr 1fr` | single column |
| Header nav | fixed row, `gap: 28px` | wrap, smaller gap |
| Section padding | `48px` / `96px` | `20px` / `56px` |
| Hero heading | `font-size: 68px` | `clamp()` down to ~40px |
| Privacy stat pair | `1fr 1fr` | single column |

The same block holds the two `:hover` rules from R2 and a `max-width` guard so text does not stretch
across an ultrawide window (an Edge Case in the spec).

**Alternatives considered**:
- Convert every inline style to classes — a clean stylesheet, and a total rewrite of markup that is
  already correct. Rejected on scope.
- A CSS framework — a dependency, a build step, and a redesign, for two pages.
- Leave it desktop-only — fails FR-007 and SC-003 for the entire mobile audience.

---

## R5 — Links under a project sub-path

**Decision**: Every internal link is relative and bare: `href="privacy.html"` and
`href="index.html"`. No leading slash, anywhere. Anchors (`#how`, `#why`) are unaffected.

**Rationale**: The site is served from `https://tberchanov.github.io/SlowLock/`, not from a domain
root, so `/privacy.html` would resolve to `tberchanov.github.io/privacy.html` — off the site
entirely. This also keeps `file://` previews working during development, and keeps the pages
correct if a custom domain is ever attached.

Both current links are broken and must be replaced regardless: the landing footer points at
`SlowLock Privacy.dc.html` and the privacy page at `SlowLock Site.dc.html` — neither name has ever
existed in this directory (the files are `SlowLock Site.html` and `SlowLock Privacy Policy.html`).
The `.dc.html` suffix is the design-canvas naming from the authoring environment.

`site/index.html` is what makes the bare address work (FR-003); `.nojekyll` keeps Pages from running
the files through Jekyll.

**Alternatives considered**:
- Absolute paths with the `/SlowLock/` prefix — works, breaks on a custom domain and on local
  preview, and hard-codes the repository name into every link.
- A `<base href>` tag — one more thing to get wrong for two links.

---

## R6 — Favicon and link-preview image

**Decision**: `site/assets/icon.svg` — the lock mark already drawn inline in both page headers
(a `#17150F` rounded square, a `#C9821F` ring, a notch), lifted into a standalone SVG. Referenced
as `<link rel="icon" href="assets/icon.svg">`. For `og:image`, render a 1200×630 PNG from a small
SVG built from the same mark plus the wordmark, using headless Chrome
(`--headless --screenshot --window-size=1200,630`), which is installed on this machine.

**Rationale**: The app's own launcher icons are Android adaptive vector XML
(`res/mipmap-anydpi/ic_launcher.xml` → `drawable/ic_launcher_foreground.xml`), a format no browser
reads, and no rasteriser (`rsvg-convert`) is installed. The site's inline mark is the same design
and is already SVG, so it needs no conversion for the favicon. Only the preview image must be
raster — most link scrapers do not render SVG — and Chrome is the rasteriser already present.

**Fallback**: if the PNG cannot be produced, ship the `og:title`/`og:description`/`og:url` tags
without `og:image`. Previews degrade to a text card; FR-008 is still met, and no dead reference is
left behind (a broken `og:image` is worse than none).

**Alternatives considered**:
- `.ico` favicon — needs a converter, and every target browser has read SVG favicons for years.
- Screenshot the rendered landing page for `og:image` — 1200×630 of a desktop hero crops badly and
  goes stale every time the page changes.
- No favicon — leaves a blank browser tab and reads as unfinished.

---

## R7 — The licence, and the copyright line

**Decision**: MIT, chosen by the maintainer. Standard unmodified MIT text in `LICENSE` at the
repository root, year **2026**.

**Copyright holder — one input needed from the maintainer.** The only identity this repository
records is the git handle `tberchanov`. A licence is a legal document and the holder line should be
a legal name, which nothing in the repository states. **Default if unanswered: `Copyright (c) 2026
tberchanov`** — a handle is legally weaker than a full name but is unambiguous, verifiable against
the repository, and better than a guessed name. This does not block any other task; the line is one
edit.

**Rationale**: MIT is the shortest permissive licence, imposes no obligation on the app's own
distribution through Google Play, and makes the landing page's "Open source, all of it" and "Free"
accurate as written (FR-022). Without any licence the repository is *source-available*: readers get
the right to view, and no right to reuse — so the claim on the page would be false.

Also add `[MIT](LICENSE)` to the repository README if one exists, so the licence is discoverable
where people look first.

**Alternatives considered**: Apache-2.0 (patent grant, longer), GPL-3.0 (copyleft, strongest match
for the ethos, needs care for Play distribution), no licence + reworded page. All offered; MIT
chosen.

---

## R8 — The Google Play link

**Decision**: The call-to-action keeps `https://play.google.com/store` for now, exactly as authored,
and a single-line comment marks it: `<!-- TODO: replace with the app's Play listing URL -->`.

**Rationale**: The maintainer confirmed the app-specific listing address will be substituted later
and that the store entry point is acceptable in the meantime (FR-021). The destination is at least
truthful about where the app will live, resolves to a real page, and is not an unrelated
destination. The comment is what makes the placeholder findable at swap time; a `grep TODO site/`
is the check.

**Alternatives considered**: a non-interactive "Coming to Google Play" label, or a link to GitHub
Releases — both offered, both declined.

---

## R9 — What happens to `site/PUBLISHING.md`

**Decision**: Delete it in the final task of this feature, once the site is live and verified.

**Rationale**: It was the working note that this spec and plan were generated *from*. Keeping it
leaves two documents describing the same work, in the directory that is published — and it would be
served at `https://tberchanov.github.io/SlowLock/PUBLISHING.md`, exposing an internal to-do list
with open decisions on the public site. The Spec Kit artifacts under `specs/006-site-publishing/`
are the record.

**Alternatives considered**:
- Move it to `specs/006-site-publishing/` — same duplication, one directory over.
- Keep it and add it to a Pages ignore — Pages has no ignore mechanism short of Jekyll config;
  simpler to delete a file whose content now lives in the spec.
