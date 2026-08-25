# Phase 1 Data Model: Hero Device Frame Screenshot

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

This feature stores nothing and persists nothing. "Data model" here means the two content entities
the hero is made of, their fixed attributes, and the rules that must hold between them.

---

## Entity: Hero screenshot

A single image file: the app's *Wait before opening* screen, captured from the running app and
cropped to the app's own content.

| Attribute | Value | Source of truth |
|---|---|---|
| Published path | `site/assets/hero-wait-screen.webp` | [contracts/hero-image-asset.md](./contracts/hero-image-asset.md) |
| Referenced as | `assets/hero-wait-screen.webp` (relative — the site lives on a sub-path) | 006 FR-005 |
| Format | WebP, no alpha | R3 |
| Intrinsic size | 576×1194 (fallback source) or 784×1624 (preferred source) | R1 |
| Aspect ratio | 0.4824 (1 : 2.073) — **frozen**; both sizes share it | R1, R4 |
| Byte budget | <= 30 KB (measured: 15.2 KB at 576, 21.0 KB at 784) | plan Performance Goals |
| Derived from | `photo_2026-08-25_19-50-41.jpg`, cropped `(0, 58) - (576, 1252)` | R4 |
| Alt text | "SlowLock's wait screen: the Messages app selected, a 10-second wait, and a slider from 1 to 30 seconds." | R7 |

**Depicted content** — what a reader must be able to make out (FR-002, SC-002):

1. The back button and the title *Wait before opening*, with the `2 / 3` step counter.
2. The selected-app chip: the Messages icon and label. **The icon is desaturated to neutral
   grey** — see V9.
3. The `10` / `SECONDS` readout.
4. The slider, positioned at 10, with its `1s` and `30s` end labels.
5. The `5s` / `10s` / `30s` preset row, with `10s` selected. *(Absent from the imitation this
   replaces — see R2.)*
6. The `Choose the icon` primary button.

**Excluded content** (FR-010) — must not be present in the file:

- The device status bar: clock, NFC, alarm, Wi-Fi, signal meters, battery percentage.
- The gesture pill.
- Anything personal: real contact names, notifications, a recognisable wallpaper.

**Validation rules**:

- **V1** — The file's intrinsic ratio MUST equal the frozen 0.4824 within +/-0.005. A replacement
  image that changes the ratio changes the hero's height and MUST be re-checked against SC-001.
- **V2** — The `<img>` element's `width` and `height` attributes MUST equal the file's true
  intrinsic pixels. A mismatch silently reintroduces the layout shift FR-006 forbids.
- **V3** — The topmost and bottommost rows of the file MUST be the app's canvas colour (#F3F0E9),
  i.e. the crop MUST NOT clip a control. This is what makes the seam with the page-drawn chrome
  invisible.
- **V4** — Replacing the file MUST NOT require editing markup other than the two size attributes
  (SC-006).
- **V9** — The selected-app icon MUST carry no saturated colour. The capture's green Messages icon
  drew the eye away from the wait itself, which is the one thing the hero exists to show, so it is
  desaturated to neutral grey in the file. The edit is confined to a 61x61px box at (185, 275); every
  other pixel is byte-identical to the capture. A replacement screenshot MUST have the same
  treatment applied, or be captured with the app's own grey icon option already selected.

---

## Entity: Device frame

The phone-shaped surround drawn by the page. It is not part of the image (spec Assumptions) and it
does not change visually in this feature — only its interior and its sizing rules do.

| Part | Height | Provided by | Changed? |
|---|---|---|---|
| Border | 10px all round, `#17150F`, radius 44px | page CSS/inline style | no |
| Status row | 40px, `flex:none` — mono `9:30`, camera dot, three dark blocks | page markup | no |
| **Screen** | derived from the image | **`<img>`** | **YES — was a drawn imitation** |
| Gesture row | 26px, `flex:none` — the 108px pill | page markup | no |
| Shadow | `0 30px 70px rgba(0,0,0,.2)` | page CSS | no |

**Derived geometry** (R5, R6) — nothing here is hand-entered; each follows from the frame's width:

```text
screen width   = frame width - 20            (the two borders)
screen height  = screen width / 0.4824
frame height   = screen height + 40 + 26 + 20
```

| Viewport | Frame width | Screen | Frame height | Ratio |
|---|---|---|---|---|
| Desktop (>860px) | 412px (unchanged) | 392x813 | **899px** (was 820) | 2.18 |
| 390px phone | 350px | 330x684 | 770px | 2.20 |
| 320px phone | 280px | 260x539 | 625px | 2.23 |

**Validation rules**:

- **V5** — The frame MUST contain no flexible (`flex:1`) box. Its height is the sum of three fixed
  pieces and one intrinsically-sized image; a flexible box is what caused the original collapse
  (R0) and would reintroduce it.
- **V6** — The frame MUST NOT declare a `height` at any breakpoint. Height is derived, never set.
- **V7** — At every viewport width the computed ratio MUST land inside 1.9–2.3 (SC-001). The table
  above shows the band holds from 320px up; it is checked by the scripted geometry check (R8).
- **V8** — The frame's screen area MUST be painted `#F3F0EA` beneath the image, so a not-yet-decoded
  image reads as a blank phone screen rather than a hole.

---

## Relationship and the one invariant

The screenshot supplies the ratio; the frame supplies the width; the height is the product. That is
the whole model, and it is what makes the bug unrepeatable:

> **There is no viewport width at which the frame has leftover vertical space to distribute, because
> the frame never has a height of its own to distribute from.**

The imitation violated this by construction — it needed a fixed height for its two `flex:1` boxes to
have anything to expand into, and it got one only above 860px.
