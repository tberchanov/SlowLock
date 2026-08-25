# Manual Test Plan: Hero Device Frame Screenshot

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

Run by **the maintainer**, on their own devices. No agent drives a phone (constitution, Development
Workflow & Quality Gates).

Test the file locally first (`file://…/site/index.html`), then repeat M01 and M02 against the
published page once it is live.

| | |
|---|---|
| Date run | ______ |
| Local file / published URL | ______ |
| Phone used | ______ |
| Result | ______ |

---

## Mobile — the reported defect

### M01 — The app screen is intact on a phone *(FR-003, FR-004, SC-001, SC-002 · Story 1)*

Open the page on your phone in portrait.

**Expect**: the hero shows an upright phone frame. Inside it the *Wait before opening* screen is
whole — title and `2 / 3`, the Messages chip, the big `10` over `SECONDS`, the slider with `1s` and
`30s`, the `5s`/`10s`/`30s` presets, and the `Choose the icon` button. Nothing overlaps, nothing is
squashed, and the elements are spaced as they are in the app itself.

**Fail if**: the chip, the number and the slider are bunched together — that is the original bug.

- [ ] Pass  [ ] Fail — notes: ______

### M02 — Legible without zooming *(SC-002)*

Same view, at your normal reading distance, without pinching.

**Expect**: you can read `Wait before opening`, `Messages`, `10`, `SECONDS`, `1s`, `30s`, the three
presets and the button label.

- [ ] Pass  [ ] Fail — notes: ______

### M03 — No sideways scroll on a narrow phone *(FR-004, SC-001)*

Swipe left and right anywhere on the hero. If you have a small phone (or use the browser's device
emulation at 320px), check there too.

**Expect**: the page does not move horizontally. The frame keeps a visible margin on both sides.

- [ ] Pass  [ ] Fail — notes: ______

### M04 — Landscape *(spec Edge Cases)*

Rotate to landscape.

**Expect**: the frame stays within the width and keeps its proportions. It may be taller than the
screen and require scrolling — that is fine.

- [ ] Pass  [ ] Fail — notes: ______

---

## Loading behaviour

### M05 — Nothing jumps *(FR-006, SC-004)*

Hard-reload the page on your phone with the cache cleared, watching the hero as it loads.

**Expect**: the frame is at its final size and position from the first paint. The image fills a box
that was already there. The Play button and the copy below do not move.

**Fail if**: the phone frame appears short and then snaps taller, pushing the page down.

- [ ] Pass  [ ] Fail — notes: ______

### M06 — Fast enough *(SC-003)*

Same reload, off Wi-Fi, on mobile data.

**Expect**: the screenshot is fully visible within about two seconds.

- [ ] Pass  [ ] Fail — notes: ______

---

## Desktop

### M07 — The desktop hero, and the height decision *(FR-005, SC-005 · Story 3 · plan Complexity Tracking)*

Open the page on a laptop at roughly 1440px wide. Compare against the current published page in a
second window.

**Expect**: the text column, the headline, the Play button, the gap and the alignment are exactly
where they were. The phone occupies the same column and the same width (412px).

**Expected difference — this is the tracked deviation, please confirm you accept it.** Measured at
1440px, before and after:

| element | before | after |
|---|---|---|
| `.hero` | x=72, w=1296, h=964.0 | x=72, w=1296, **h=1042.6** |
| `.hero-title` | x=120, w=560, h=138.7 | identical |
| `.phone` | x=908, w=412, h=820.0 | x=908, w=412, **h=898.6** |
| `.btn-play` | x=120, w=256.3, h=60 | identical |
| `#how` | x=0, w=1440, h=474.6 | identical |

Every horizontal dimension is unchanged. The phone is **+78.6px taller**, and because the hero is
vertically centred the text column therefore **sits about 39px lower** than it did — measured as a
39px shift in a pixel diff of the left half. Nothing below the hero changes except by that offset.

Keeping the screenshot's true proportions and both of the old dimensions is arithmetically
impossible; the plan chose to keep the width and the grid column.

- [ ] Accept the taller phone
- [ ] Prefer the alternative — hold 820px and let the frame narrow to 374px *(one-line change)*
- Notes: ______

### M08 — Sharpness on a high-density screen *(FR-008)*

On a Retina laptop or a modern phone, look closely at the `10`, the `SECONDS` caption and the preset
labels.

**Expect**: crisp edges.

**Known limitation — this applies: the fallback source shipped.** The original device PNG was not
supplied, so the published asset is the 576px-wide Telegram copy against a 392px display width —
**1.47×, not 2×**. Slight softness is expected. Sending the original screenshot straight off the
phone fixes it, and swapping it in changes only two attributes in the markup (research R1,
quickstart step 0).

- [ ] Sharp  [ ] Acceptably soft  [ ] Too soft — ask for the original PNG
- Notes: ______

### M09 — Ultrawide *(SC-001, 006 ultrawide guard)*

Widen the window past 1600px.

**Expect**: the phone stops growing along with the rest of the page content.

- [ ] Pass  [ ] Fail — notes: ______

---

## Content and privacy

### M10 — Nothing personal shipped *(FR-010)*

Look at the top and bottom edges of the screen inside the frame.

**Expect**: the site's own drawn status bar (mono `9:30`, the camera dot, three dark blocks) and its
own gesture pill. **Not** `19:50`, not `59%`, no NFC or alarm icon, no Wi-Fi or signal meters.

**Expect**: no real contact name, notification, or anything else identifying a person.

- [ ] Pass  [ ] Fail — notes: ______

### M11 — The seam is invisible *(R4)*

Look at where the drawn status bar meets the image, and where the image meets the gesture row.

**Expect**: no visible line, band or colour step. The app canvas and the page background are within
one unit of each other.

- [ ] Pass  [ ] Fail — notes: ______

### M12 — The picture matches the app *(FR-001, SC-002 · Story 2)*

Open SlowLock on your phone, go to *Wait before opening*, and hold it beside the site.

**Expect**: the same screen. In particular the `5s`/`10s`/`30s` preset row is present in both — the
imitation this replaces did not have it (research R2).

- [ ] Pass  [ ] Fail — notes: ______

---

## Degraded conditions

### M13 — Images blocked *(FR-006, FR-007, spec Edge Cases)*

In your browser's settings, block images for the site, then reload.

**Expect**: the frame keeps its full height — the hero does not collapse or reflow — and the browser
shows the alt text: *"SlowLock's wait screen: the Messages app selected, a 10-second wait, and a
slider from 1 to 30 seconds."*

- [ ] Pass  [ ] Fail — notes: ______

### M14 — Screen reader *(FR-007)*

Turn on TalkBack (Android) or VoiceOver (macOS) and swipe through the hero.

**Expect**: the image is announced with the alt text above — a description of what the app is doing,
not a filename and not "image".

- [ ] Pass  [ ] Fail — notes: ______

### M15 — No JavaScript *(006 FR-025)*

Disable JavaScript and reload.

**Expect**: no change at all. The page has none.

- [ ] Pass  [ ] Fail — notes: ______

---

## Regression

### M16 — Nothing else moved *(spec Assumptions — scope)*

Scroll the whole page, then open `privacy.html`.

**Expect**: *How it works*, *Why it exists*, the open-source section, the header, the footer and the
privacy page are all exactly as before. No second screenshot appeared anywhere.

- [ ] Pass  [ ] Fail — notes: ______

### M17 — Published paths resolve *(contract C1)*

On the live site, right-click the hero image and open it in its own tab.

**Expect**: it loads at `https://tberchanov.github.io/SlowLock/assets/hero-wait-screen.webp`.

**Fail if**: 404 — that means a root-relative path slipped in.

- [ ] Pass  [ ] Fail — notes: ______

---

## Traceability

| Case | Requirements |
|---|---|
| M01 | FR-003, FR-004, SC-001, SC-002 |
| M02 | SC-002 |
| M03 | FR-004, SC-001 |
| M04 | Edge Cases |
| M05 | FR-006, SC-004 |
| M06 | FR-008, SC-003 |
| M07 | FR-005, SC-005, plan Complexity Tracking |
| M08 | FR-008 |
| M09 | SC-001 |
| M10 | FR-010 |
| M11 | FR-002 |
| M12 | FR-001, SC-002, SC-006 |
| M13 | FR-006, FR-007 |
| M14 | FR-007 |
| M15 | 006 FR-025 (regression) |
| M16 | spec Assumptions (scope) |
| M17 | FR-009, contract C1 |
