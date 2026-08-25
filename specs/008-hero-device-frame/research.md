# Phase 0 Research: Hero Device Frame Screenshot

**Feature**: [spec.md](./spec.md) | **Date**: 2026-08-25

All unknowns are resolved. Nothing below blocks Phase 1, but **R1 carries an ask for the
maintainer** that improves the result if answered before implementation.

---

## R0 — What exactly is broken today

**Finding**: The hero "phone" in `site/index.html:256` is not an image. It is a hand-built
imitation of the app screen: a bordered `<div>` containing a drawn status bar, a drawn app chip, a
`104px` "10", a drawn slider and a drawn button. Its layout depends on a fixed height.

- `site/index.html:196` — `.phone { width: 412px; height: 820px; }` (desktop)
- `site/index.html:213` — `.phone { width: 100%; max-width: 412px; height: auto; }` (≤860px)

Inside it, `site/index.html:265` is `flex:1 … min-height:0` and `site/index.html:274` is another
`flex:1 … justify-content:center`. Those two `flex:1` boxes exist to absorb the leftover of the
820px. When the height becomes `auto`, there is no leftover: both collapse to their content, the
`28px` gap is all that separates the app chip from the number and the number from the slider, and
the screen crushes together. That is the reported "collapsed" look.

**Decision**: Do not patch the height. Remove the imitation. An image cannot collapse, because its
own intrinsic ratio sets its height at every width.

**Alternatives considered**:

- *Give `.phone` a mobile `aspect-ratio` and keep the drawn screen.* Fixes the collapse, but the
  interior is still hand-drawn: the drawn screen is already out of date (it has no preset row —
  see R2), and every future app change means re-editing markup. Rejected on FR-001 and SC-006.
- *Hide the phone below 860px.* Removes the strongest piece of evidence the app is real, on the
  devices that matter most. Rejected.

---

## R2 — The imitation has already drifted from the app

**Finding**: The supplied screenshot shows a **preset row — `5s` / `10s` / `30s`, with `10s`
selected** — sitting between the slider labels and the "Choose the icon" button. The hand-built
imitation in `site/index.html` has no such row. The site has been showing a screen the app does not
have for as long as the presets have existed.

**Decision**: Record this as the concrete evidence for FR-001/SC-006 rather than as a separate
defect. Replacing the drawing with a capture fixes it and prevents the next occurrence.

---

## R1 — Source resolution and the sharpness requirement (FR-008)

**Finding**: The supplied file is `photo_2026-08-25_19-50-41.jpg`, 576×1280, 33 KB. The name is
Telegram's export pattern and the dimensions are Telegram's compressed size — this is almost
certainly a re-encode of a native device capture (a `minSdk 33` phone captures at roughly
1080×2400). The site displays the screen at **392 CSS px** at most (the 412px frame less its two
10px borders). 576 ÷ 392 = **1.47×**, short of the 2× a modern phone or Retina laptop wants.

**Decision**: **Ask the maintainer for the original screenshot** (the PNG straight off the device,
before Telegram). Downscale it to **784 px wide** — exactly 2× the largest displayed width — and
encode WebP.

**Outcome (recorded at implementation, 2026-08-25)**: the original PNG was **not supplied** — the
ask was raised and left open rather than blocking the work. The **576px fallback shipped**:
`site/assets/hero-wait-screen.webp`, 576x1194, **15,152 bytes (14.8 KB)**, ratio 0.4824. Sharpness
is therefore 1.47x, not 2x; manual case M08 asks the maintainer to judge whether that is good
enough. Swapping in a higher-resolution encode later touches only the two size attributes on the
`<img>` (data-model V4).

**Fallback if the original is not available**: ship the supplied 576px file. Measured encodes of
the cropped frame:

| Source | Encode | Bytes |
|---|---|---|
| 576×1194 (supplied) | WebP q80 | **15.2 KB** |
| 576×1194 (supplied) | WebP q88 | 20.3 KB |
| 784×1626 (2× target) | WebP q80 | 21.0 KB |
| 784×1626 (2× target) | WebP q88 | 29.5 KB |

At 576 the picture is sharp on a 1× display and slightly soft on a 2× one — a partial FR-008 pass.
The fallback is acceptable to ship; it is not the intended end state. Either way the asset is
smaller than the 33 KB JPEG it came from, so **page weight goes down, not up** — the 60 KB per-page
budget inherited from 006-site-publishing is not at risk.

**Alternatives considered**:

- *Re-capture the screen from a running app.* Best fidelity, but the constitution forbids an agent
  driving the device, and it costs the maintainer a device session for something a file they
  already have can supply. Rejected in favour of the ask above.
- *Two sources with `srcset` (1× and 2×).* Buys a few KB on 1× displays for a second asset to keep
  in step. At 15–30 KB the saving does not pay for the complexity. Rejected (Principle II).
- *SVG re-drawing of the screen.* That is the imitation again, in another format. Rejected.

---

## R3 — Format

**Decision**: **WebP, single file, no `<picture>` fallback.** WebP is supported by every browser in
the site's stated baseline (current Chrome, Safari, Firefox, desktop and mobile) and has been since
Safari 14 (2020). It is 55% smaller than the supplied JPEG at visually equal quality.

**Alternatives considered**: JPEG (simplest, ~2× the bytes); AVIF (smaller still, but slower to
decode on old phones and a narrower floor for marginal gain); `<picture>` with a JPEG fallback
(a second asset and a markup branch to serve browsers the site does not target). All rejected.

---

## R4 — The device chrome in the screenshot (FR-010)

**Finding**: The supplied capture includes the phone's own status bar (`19:50`, NFC, alarm, Wi-Fi,
two signal meters, `59%`) and the gesture pill. Measured against the image, the status-bar glyphs
occupy rows **26–47** and the gesture pill rows **1264–1269**; the app's own content runs from the
back button at row ~76 to the bottom of the CTA at row 1224. The screen's background is
**#F3F0E9**, within one unit of the site's `#F3F0EA`.

**Decision**: **Crop the file to `(0, 58) – (576, 1252)`** — 576×1194 — removing both bars, and keep
the frame's existing page-drawn status bar (the mono `9:30` and the three dark blocks) and gesture
pill. Because the two background colours are indistinguishable, the crop is seamless.

This satisfies FR-010 by *removing* the incidental readings from the file rather than covering them,
keeps the frame page-drawn as the spec assumes, and leaves the site free to restyle the chrome
without touching the image.

**Alternatives considered**:

- *Ship the capture whole, with its own status bar, and delete the page-drawn one.* Puts a real
  battery percentage and a stranger's clock in the hero, and pins the site's chrome styling inside a
  binary. Violates FR-010. Rejected.
- *Crop with CSS (`object-fit` / negative offsets) instead of cropping the file.* The personal
  detail is still shipped to every visitor, merely hidden. Rejected.

---

## R5 — Making the frame hold its shape (FR-003, FR-004, FR-006)

**Decision**: Delete both `flex:1` boxes and both fixed heights. The frame becomes three stacked
pieces with no flexible space at all:

```text
.phone  ── 10px border, fixed width, height NOT set
├── status row      40px, flex:none   (page-drawn, unchanged)
├── <img>           width:100%; height:auto; display:block
└── gesture row     26px, flex:none   (page-drawn, unchanged)
```

The frame's height is then whatever those three add up to, at every viewport width, with no media
query involved in the ratio. Give the `<img>` its true `width="576" height="1194"` attributes and the
browser reserves the right box **before the bytes arrive** — that alone satisfies FR-006, with no
`aspect-ratio` declaration and no placeholder.

`loading` is left at its default (**not** `lazy` — the image is above the fold), with
`fetchpriority="high"` and `decoding="async"` so it competes with the fonts rather than after them.
Paint the image's own box `#F3F0EA` so a not-yet-decoded image reads as a blank phone screen.

**Alternatives considered**: `aspect-ratio` on `.phone` (needs a magic number recomputed by hand
whenever the crop changes — the `<img>` already knows its ratio); a padding-top ratio hack (same
objection, worse); a CSS `background-image` (no intrinsic size, so CLS returns and there is nowhere
to put alt text — fails FR-006 and FR-007).

---

## R6 — Desktop footprint, and a conflict the maintainer should see

**Finding**: FR-003 (keep the ratio) and FR-005/SC-005 (identical desktop footprint) **cannot both
hold exactly.** Today's frame is 412×820 — a ratio of 1.99. The cropped screenshot, once the 40px
status row, the 26px gesture row and the 20px of border are added, gives a frame of 412×**899** at
the same width (ratio 2.18), or 374×820 at the same height.

**Decision**: **Keep the width at 412px and let the height grow to ~899px.** The hero grid column
stays `1fr 412px`, the text column, the gap and the alignment are untouched, and the horizontal
composition — which is what "footprint" means in a two-column hero — is genuinely unchanged. The
phone grows 79px taller, symmetrically, because the hero is `align-items:center`; the hero section
goes from ~964px to ~1043px tall on desktop. 2.18 is inside SC-001's 1.9–2.3 band and is closer to
a real phone's proportions than today's 1.99.

**This is a documented, deliberate deviation from SC-005's literal "visually unchanged".** It is
recorded here, in the plan's Complexity Tracking, and as manual case M07 so the maintainer sees it
and can overrule it.

**Alternative considered**: *Hold 820px and narrow the column to 374px.* Preserves vertical rhythm
exactly but leaves the phone visibly smaller and re-cuts the grid column, which is the more
noticeable change of the two. Available as a one-line reversal if the maintainer prefers it.

---

## R9 — The app icon competes with the wait (added at implementation)

**Finding**: In the shipped hero, the capture's green Messages icon was the most saturated thing on
the page after the brand orange. It pulled the eye to the chosen app — the opposite of the hero's
argument, which is about the pause, not the app.

**Decision**: Desaturate the icon to neutral grey, in the file, inside a 61x61px box at (185, 275).
Saturation is removed continuously rather than by threshold, so antialiased edges blend and the
warm cream chip behind the icon keeps its tint. Verified: zero green-dominant pixels remain
anywhere in the image, and every pixel outside that box is byte-identical to the capture.

**Why this is not a truthfulness problem**: the app really does offer a grey icon — the landing
page's own step 03 says the shortcut lands "with the app's own icon, or a gray or inverted one". The
picture shows a state the app can produce.

**Alternatives considered**: *CSS filter over the image region* — impossible without slicing the
image or overlaying an element positioned by hand, which reintroduces exactly the brittle
page-drawn geometry this feature deleted. *Re-capture with the grey icon selected* — better, and
available whenever the maintainer supplies a new screenshot; not worth a device session on its own.

## R7 — Alt text (FR-007)

**Decision**:

> `alt="SlowLock's wait screen: the Messages app selected, a 10-second wait, and a slider from 1 to 30 seconds."`

Names what the picture demonstrates rather than describing furniture. Short enough to be read
aloud without tedium; specific enough that a visitor who never sees the image learns the same thing
a sighted visitor does.

**Alternatives considered**: `alt="Screenshot of the SlowLock app"` (says nothing the surrounding
copy has not already said); an empty `alt` (correct only for decoration — this image *is* the
argument).

---

## R8 — How this gets verified

**Decision**: Manual test plan + two scripted checks, matching how 006-site-publishing verified the
same file. No test framework is added.

- **Scripted**: headless Chrome (present at `/Applications/Google Chrome.app`) renders
  `site/index.html` at 320, 390 and 1440px and reports `document.documentElement.scrollWidth`
  against `window.innerWidth` (SC-001's no-sideways-scroll half) and the frame's measured
  width/height ratio (SC-001's proportion half). This is a page in a browser, not a device — the
  constitution's instrumented-test prohibition is about driving the connected Android device and
  does not reach it.
- **Manual**: the maintainer opens the published page on their own phone. Cases in
  [manual-test-plan.md](./manual-test-plan.md).

---

## Resolved unknowns summary

| ID | Question | Resolution |
|---|---|---|
| R0 | Why does it collapse? | Two `flex:1` boxes sized by a fixed height that becomes `auto` below 860px |
| R2 | Is the drawing accurate? | No — it is missing the 5s/10s/30s preset row |
| R1 | Is the supplied file sharp enough? | 1.47×, not 2×. Ask for the original; 15 KB WebP fallback ships |
| R3 | What format? | WebP, one file, no `<picture>` |
| R4 | What about the clock and battery? | Crop the file to (0,58)–(576,1252); keep the page-drawn chrome |
| R5 | How is the shape held? | Delete the `flex:1` boxes; `<img>` with real `width`/`height` attributes |
| R6 | Same desktop footprint? | Width yes (412px), height grows 820→899. Deviation documented |
| R7 | Alt text? | Names the demonstration, not the furniture |
| R8 | Verification? | Headless-Chrome ratio check + maintainer's phone |
| R9 | The green app icon competes with the wait | Desaturated to neutral grey in the file, inside a 61x61 box at (185, 275) |
