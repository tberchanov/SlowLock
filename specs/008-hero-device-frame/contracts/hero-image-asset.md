# Contract: Hero image asset & frame markup

**Feature**: [../spec.md](../spec.md) | **Plan**: [../plan.md](../plan.md)

The site's external interface is the set of URLs it publishes and the markup a crawler or a
screen reader receives. This feature adds one URL and rewrites one block of markup. Both are frozen
here so a later change that breaks them is a contract change, not an accident.

---

## 1. Published URL

| | |
|---|---|
| Repository path | `site/assets/hero-wait-screen.webp` |
| Published URL | `https://tberchanov.github.io/SlowLock/assets/hero-wait-screen.webp` |
| Referenced from the page as | `assets/hero-wait-screen.webp` |
| Content type | `image/webp` |

**C1** — The reference MUST be relative. The site is a GitHub **project** site served from the
`/SlowLock/` sub-path; a root-relative `/assets/...` resolves to `tberchanov.github.io/assets/...`
and 404s. This is the same rule that broke every link in 006-site-publishing.

**C2** — The file MUST be served from this origin. No CDN, no image host, no third-party transform.
A page that says the app makes no network calls must not itself call out (006 FR-026).

**C3** — The filename MUST contain no space and no uppercase letter, matching the two assets already
in that folder (006 FR-004).

**C4** — The path is stable. Renaming it after publication breaks any external link or cached copy,
and MUST be treated as a change to this contract.

---

## 2. Asset shape

| Property | Requirement |
|---|---|
| Format | WebP, lossy, no alpha channel |
| Intrinsic width x height | `576 x 1194` (fallback source) **or** `784 x 1624` (preferred source) |
| Aspect ratio | `0.4824` (+/- 0.005) — frozen |
| File size | <= 30 KB |
| Colour at row 0 and last row | the app canvas, `#F3F0E9` (+/- 2 per channel) |

**C5** — Whichever source is used, the ratio is the frozen value. Width and height may change
together; the ratio may not (data-model V1).

**C6** — The file MUST NOT contain the device status bar, the gesture pill, or any personal detail
(FR-010, data-model "Excluded content"). This is verifiable by eye: the top row of the image is the
app's canvas, and the first thing below it is the back button.

**C6a** — The selected-app icon MUST be desaturated to neutral grey (data-model V9). This is the
only permitted departure from a literal capture, it is confined to a 61x61px box at (185, 275), and
it depicts a state the app can genuinely produce — the grey icon option the landing page's own step
03 describes.

**C7** — EXIF and any other metadata MUST be stripped. `cwebp` does this by default; do not pass
`-metadata all`.

---

## 3. Markup contract

The hero phone MUST render as this shape. Attribute order and inline-style formatting follow the
surrounding file's conventions; the elements, the attributes named below, and the absence of the
ones called out are what is fixed.

```html
<div class="phone" style="…border:10px solid #17150F;border-radius:44px;overflow:hidden;
                          display:flex;flex-direction:column;…">

  <!-- status row — unchanged, flex:none, 40px -->
  <div style="height:40px;…;flex:none;…"> … </div>

  <!-- the screen -->
  <img src="assets/hero-wait-screen.webp"
       width="576" height="1194"
       alt="SlowLock's wait screen: the Messages app selected, a 10-second wait, and a slider from 1 to 30 seconds."
       decoding="async"
       fetchpriority="high"
       style="display:block;width:100%;height:auto;background:#F3F0EA">

  <!-- gesture row — unchanged, flex:none, 26px -->
  <div style="height:26px;…;flex:none;…"> … </div>
</div>
```

**C8** — `width` and `height` attributes are **mandatory** and MUST equal the file's true intrinsic
pixels. They are what reserves the box before the bytes arrive (FR-006, SC-004). Without them the
hero jumps on every cold load.

**C9** — `alt` is **mandatory** and non-empty, with the text in §2 of [../research.md](../research.md)
(R7). The image carries the page's central argument; it is not decoration (FR-007).

**C10** — `loading="lazy"` MUST NOT be set. The image is above the fold; lazy-loading it delays the
one thing the hero exists to show (SC-003).

**C11** — `display:block` is **mandatory**. Without it the inline baseline leaves a few stray pixels
between the image and the gesture row, and the frame's interior no longer meets its edges.

**C12** — The `<img>` MUST be a direct flex child of `.phone` with **no** `flex` shorthand on it and
**no** wrapper that declares `flex:1`. This is the whole fix (data-model V5).

**C13** — `.phone` MUST NOT declare `height` at any breakpoint, and MUST NOT declare
`aspect-ratio` (data-model V6). Its height is the sum of its children.

**C14** — Only two `.phone` rules survive:
`.phone { width: 412px; }` above 860px and
`.phone { width: 100%; max-width: 412px; justify-self: center; }` at or below it.

---

## 4. What this contract does NOT cover

- The hero's copy, the Play button, and the `FOR ANDROID` eyebrow — untouched.
- `site/privacy.html`, `site/assets/og.png`, `site/assets/icon.svg` — untouched. The link-preview
  image is a separate asset with its own purpose and is not replaced by this one.
- The publishing workflow — it copies `site/` wholesale and needs no entry for a new file.
- Anything under `app/`.

---

## 5. Breaking-change checklist

A future change breaks this contract if it does any of:

- moves or renames `site/assets/hero-wait-screen.webp`;
- ships an image whose ratio is not 0.4824, without re-running the SC-001 geometry check;
- lets the `width`/`height` attributes drift from the file's real pixels;
- reintroduces a `flex:1` child or a fixed `height` inside `.phone`;
- adds a third-party request to render the hero.
