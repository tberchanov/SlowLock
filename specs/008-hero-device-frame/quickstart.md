# Quickstart: Hero Device Frame Screenshot

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

End-to-end execution order. Steps 1–6 are the work; 7–8 are hand-off. Nothing here commits, pushes,
or touches a device.

---

## Step 0 — The one ask, up front (R1)

Ask the maintainer for **the original screenshot straight off the phone** — the PNG in the device's
screenshot folder, not the Telegram copy. The supplied `photo_2026-08-25_19-50-41.jpg` is 576×1280,
which is 1.47× at the size the site uses it, short of the 2× a Retina screen wants (FR-008).

**Do not block on it.** If the original is not to hand, proceed with the supplied file — the result
is sharp on a 1× display and slightly soft on a 2× one, and the asset can be swapped later without
touching markup beyond two size attributes (data-model V4).

The agent does not capture this itself. The constitution reserves the device for the maintainer.

---

## Step 1 — Crop the source (R4)

Crop away the device status bar and the gesture pill so the file holds the app's screen and nothing
personal (FR-010). Boundaries measured against the supplied file: the status-bar glyphs end at row
47, the back button starts at row ~76, the CTA bottom is row 1224, the gesture pill starts at row
1264.

```bash
python3 - <<'PY'
from PIL import Image
src = Image.open('photo_2026-08-25_19-50-41.jpg').convert('RGB')
src.crop((0, 58, 576, 1252)).save('/tmp/hero-crop.png')   # -> 576x1194
PY
```

If the higher-resolution original arrived, scale the crop box by the same factor (its status bar and
gesture pill sit at the same proportional rows) and keep the resulting ratio at 0.4824.

**Check before moving on**: the top row and bottom row of the crop are the app canvas `#F3F0E9`, and
no control is clipped (data-model V3, contract C6).

---

## Step 2 — Encode (R3)

```bash
cwebp -q 80 -quiet /tmp/hero-crop.png -o site/assets/hero-wait-screen.webp
```

Expected ~15 KB from the 576px source, ~21 KB from a 784px one. Both are inside the 30 KB budget and
both are smaller than the 33 KB JPEG they came from, so the page gets lighter.

Do not pass `-metadata all` — metadata is stripped by default and should stay stripped (contract C7).

---

## Step 3 — Replace the frame's interior (`site/index.html`)

Delete the hand-built screen — everything between the status row and the gesture row, currently
`site/index.html:265`–`:298`, roughly 50 lines including both `flex:1` boxes, the drawn app chip, the
`104px` "10", the drawn slider and the drawn button.

In its place put the single `<img>` from [contracts/hero-image-asset.md](./contracts/hero-image-asset.md) §3.
Keep the status row and the gesture row exactly as they are.

**Do not skip**: `width`, `height`, `alt`, `display:block` and the absence of `loading="lazy"` are
each load-bearing — contract C8 through C12.

---

## Step 4 — Fix the sizing rules (R5, R6)

Two CSS edits, both deletions of a height:

- `site/index.html:196` — `.phone { width: 412px; height: 820px; }` becomes `.phone { width: 412px; }`
- `site/index.html:213` — `.phone { width: 100%; max-width: 412px; height: auto; justify-self: center; }`
  becomes `.phone { width: 100%; max-width: 412px; justify-self: center; }`

Add no `aspect-ratio` and no replacement height anywhere (contract C13). The frame's height is now
the sum of 20px border + 40px status + the image + 26px gesture, at every width.

The desktop grid column stays `1fr 412px`. The phone becomes 899px tall instead of 820px — expected,
and the tracked deviation in the plan's Complexity Tracking.

---

## Step 5 — Scripted geometry check (R8, SC-001)

Renders the page in headless Chrome at three widths and reports the frame's ratio and whether the
page scrolls sideways. This is a browser on the laptop, not a device.

```bash
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
for W in 320 390 1440; do
  "$CHROME" --headless --disable-gpu --virtual-time-budget=3000 \
    --window-size=$W,900 --screenshot=/tmp/hero-$W.png \
    "file://$PWD/site/index.html" 2>/dev/null
done
```

Then open the three PNGs and confirm: the phone reads as a phone at all three, nothing is clipped at
320, and the desktop hero is unchanged apart from the phone's height.

**Pass condition**: ratio between 1.9 and 2.3 at every width; `scrollWidth <= innerWidth` at 320.

---

## Step 6 — Read the page as a visitor would

- Load `site/index.html` with images blocked. The frame keeps its full height and shows a blank
  cream screen; the hero does not reflow, and the alt text stands in (FR-006, FR-007).
- Load it with JavaScript disabled. Nothing changes — there is none.
- Zoom to 200%. Nothing overflows.

---

## Step 7 — Decide what happens to the source file

`photo_2026-08-25_19-50-41.jpg` is currently **staged at the repository root**. It is a working
input, not a site asset, and the root is not where it belongs. Three options — the maintainer's
call, not the agent's:

| Option | Effect |
|---|---|
| Unstage and delete it | Cleanest. The published `.webp` is the artifact; the source is reproducible from the device. |
| Move it into `specs/008-hero-device-frame/` | Keeps the provenance of the crop with the feature that made it. |
| Leave it at the root | Not recommended — an unexplained photo in the repository root. |

---

## Step 8 — Build gate and hand-off

```bash
./gradlew assembleDebug
./gradlew test
```

No file under `app/` changed, so both should pass untouched; run them once as insurance.

Then **stop**. Report what changed and leave the working tree uncommitted:

- modified: `site/index.html`
- new, untracked: `site/assets/hero-wait-screen.webp`
- staged, undecided: `photo_2026-08-25_19-50-41.jpg`

The constitution reserves the commit and the push for the maintainer. Offer them; do not perform
them.

---

## Order of operations at a glance

```text
0. ask for the original PNG (non-blocking)
1. crop  ─────────────────┐
2. encode to WebP         ├─ produces site/assets/hero-wait-screen.webp
3. swap the markup  ──────┤
4. drop the two heights   ├─ produces the modified site/index.html
5. scripted geometry check
6. read it as a visitor
7. maintainer decides about the source photo
8. gradle gate, then hand off uncommitted
```
