# Contract: Page Content

**Feature**: [../spec.md](../spec.md)

What each published page must contain, must not contain, and must be true about. Written as
checkable statements — each is a line in [../manual-test-plan.md](../manual-test-plan.md).

## Both pages

**MUST**

- Render their full text with JavaScript disabled (FR-006). No `<script>` tag ships at all.
- Carry a distinct `<title>` naming SlowLock, a `<meta name="description">`, and
  `<link rel="icon">` (FR-008). Neither page may say "Bundled Page".
- Include `<meta charset="utf-8">` and `<meta name="viewport" content="width=device-width,
  initial-scale=1">`.
- Display with no horizontal scrolling at a 390 px viewport and at desktop width (FR-007).
- Use relative internal links (FR-005).
- Load no resource from a third-party host (FR-026) — fonts, images and styles all come from
  `site/`.

**MUST NOT**

- Contain an element that appears interactive but has no destination or effect (FR-009).
- Contain a `preconnect`/`dns-prefetch` to a font or analytics host.
- Contain any leftover authoring attribute: `sc-camel-*`, `style-hover`, `data-screen-label`,
  `<x-dc>`, `<helmet>`.

## Landing page (`index.html`)

**MUST**

- Explain what the app does, the three-step setup, and why a pause rather than a block (FR-018).
- Link to `https://github.com/tberchanov/SlowLock` (FR-020).
- Point its primary call to action at Google Play (FR-021), carrying
  `<!-- TODO: replace with the app's Play listing URL -->` until the listing address is known.
- Link to `privacy.html` from the footer (FR-005).
- State only behaviour the app actually has (FR-019).

**Claims currently made, and what keeps each true** — re-check before publishing (FR-019):

| Claim | Kept true by |
|---|---|
| "Free · no accounts · no analytics" | No auth code; no analytics dependency; `LICENSE` present |
| "one to thirty seconds" | `DelayRange.MIN_SECONDS` / `MAX_SECONDS` |
| "the app's own icon, or a gray or inverted one" | `IconTreatment` enum (3 values) |
| "makes no network calls" | No `INTERNET` permission |
| "Open source, all of it" | `LICENSE` (MIT) exists at repository root |

## Privacy policy page (`privacy.html`)

**MUST**

- State what is read, what is stored, and that neither leaves the device (FR-010).
- State no network requests and no analytics/crash/ads/third-party components (FR-011).
- Tell the reader how to delete everything (FR-012).
- Give a working contact route (FR-013).
- Show a last-updated date, revised whenever the text changes (FR-014).
- Link back to `index.html` (FR-005) and to the repository (FR-016).

**MUST NOT**

- Contain the words "DRAFT", "not legally reviewed", or any other provisional notice (FR-015).
  *This banner exists in the current file and its removal is the single most important edit in the
  feature.*
- Assert anything not traceable to `app/src/main` (FR-016).
- Contradict the Play Console Data safety declaration (FR-017).

## Change rule

Any edit to a claim on either page requires the same edit's justification in the source, or the
claim comes off the page. When the policy's text changes, its date changes with it — those two are
never separated.
