# Manual Test Plan: Visual Redesign (Phase 1)

**Feature**: `004-visual-redesign` | **Run by**: the maintainer, on a device

The constitution forbids instrumented suites and forbids an agent driving the connected device.
Automated coverage in this feature is two JVM test files — the palette arithmetic and the preset
logic. **Everything below is verified by a person.**

Eight tiers, 41 cases, each traceable to a requirement. Record every case pass or fail; a case not
run is not a pass.

**Devices needed**: one phone. Tiers 1 and 8 need a second device with a different wallpaper and
system accent. Tier 5 needs a dark room. Tier 7 wants the smallest screen available.

---

## M1 — Identity and typography · FR-001–FR-005, SC-002

| # | Case | Expected | Req |
|---|---|---|---|
| M1.1 | Open the app on a device with a bright, saturated wallpaper | No wallpaper-derived colour anywhere. Ground is warm bone, primary action is amber | FR-001 |
| M1.2 | Open the same screens on a second device with a different system accent | Pixel-for-pixel the same colours as M1.1 | FR-001, SC-002 |
| M1.3 | Airplane mode, force-stop, cold launch | Text renders in Instrument Sans / JetBrains Mono immediately. No system-font frame, no reflow, no swap | FR-003, research R2 |
| M1.4 | Read every screen; check each number and unit caption | Every numeral and unit label is monospaced. No number in the proportional face | FR-004 |
| M1.5 | **Install an app with a CJK, Arabic, Hebrew or Devanagari label; open the list, then its delay and icon screens** | The label renders legibly in *some* face. **No missing-glyph boxes (tofu)** | FR-005 |
| M1.6 | If M1.5 shows tofu | Apply research R3's contingency: app labels only fall back to the platform default family. Re-run M1.5 | FR-005 |
| M1.7 | Set the system to dark mode; open list, delay, icon, unsupported screens | All four stay light and stay legible | FR-008 |
| M1.8 | Cold-launch the app and watch the very first frame | The starting window is bone. No white flash before the app draws | research R5, C5 |

---

## M2 — App list · FR-010–FR-013

| # | Case | Expected | Req |
|---|---|---|---|
| M2.1 | Open the list | Title "Choose an app". **No back tile, no `1 / 3` counter** | FR-010 |
| M2.2 | Inspect the search field | 52dp, rounded, card fill, hairline border, existing placeholder | FR-011 |
| M2.3 | Inspect rows | 64dp tall, 44dp rounded icons, hairline divider between rows | FR-012 |
| M2.4 | Type a query matching nothing | No-results state in the new palette; wording unchanged | FR-013 |
| M2.5 | Clear the query | Full list returns; clear affordance works as before | S1 |
| M2.6 | Scroll well down, tap an app, go to the icon step, back out twice | **Scroll position is where you left it** | S1, 003 FR-011 |
| M2.7 | Rotate the device mid-list | Scroll and query survive | S1 |
| M2.8 | Tap an app that was uninstalled while the list was open | Existing snackbar, unchanged wording | S1 |

---

## M3 — Delay screen and presets · FR-014–FR-021

| # | Case | Expected | Req |
|---|---|---|---|
| M3.1 | Open the delay screen | Large mono numeral is the biggest thing on screen, `SECONDS` beneath it, app pill above | FR-014, FR-015 |
| M3.2 | Inspect the slider | Amber active track, `Fill` inactive track, 26dp ring thumb, `1s` and `30s` end labels | FR-016 |
| M3.3 | Tap the `5s` preset | Readout reads 5, slider moves to match, `5s` is highlighted | FR-017, FR-018 |
| M3.4 | Tap `10s`, then `30s` | Each takes **exactly one tap** and highlights correctly | FR-017, SC-003 |
| M3.5 | Drag the slider to 17 | Readout 17, **no preset highlighted** | FR-018 |
| M3.6 | Drag to exactly 10 | `10s` becomes highlighted without being tapped | FR-018 |
| M3.7 | Drag to each end | Stops at 1 and 30; no value outside the range is reachable | FR-019 |
| M3.8 | Read the primary action | "Choose the icon" | FR-020 |
| M3.9 | Set 17, continue, come back | **17 is still shown**, not the saved value and not a preset | FR-021 |
| M3.10 | Rotate on the delay screen | Same value, same screen | FR-021 |
| M3.11 | Press back | Returns to the list | FR-021 |
| M3.12 | Open the delay screen for a previously configured app | Opens on its saved delay | FR-021 |

---

## M4 — Icon step · FR-022–FR-026

| # | Case | Expected | Req |
|---|---|---|---|
| M4.1 | Open the icon step at 10 seconds | Bordered preview card: 96dp icon, label, "10 second wait" in mono | FR-022 |
| M4.2 | Inspect the treatment row | Three equal tiles, each a swatch above a name, under an `ICON` eyebrow | FR-023 |
| M4.3 | Tap "Inverted" | That tile fills amber-wash with an amber border; the preview icon inverts | FR-024 |
| M4.4 | Tap "Gray", then "Original" | Selection follows; exactly one tile selected at all times | FR-024 |
| M4.5 | Read the primary action and the line below it | "Add to home screen"; footnote says the launcher will ask to confirm | FR-025 |
| M4.6 | Read the header | **"New lock"** — the word "shortcut" appears nowhere on screen | FR-041 |
| M4.7 | Open for an app configured as Gray last time | Opens with Gray selected | FR-026 |
| M4.8 | Uninstall the target from another device path, then tap the action | Existing unavailable message; **no lock created** | FR-026 |
| M4.9 | Complete the flow | Launcher's confirm dialog appears; icon lands on the home screen | FR-026 |
| M4.10 | Back out from the icon step | Returns to the delay screen with the same seconds | FR-021 |

---

## M5 — The wait · FR-027–FR-033, SC-005, SC-006

The tightest tier. Run every case; this is the screen users meet most.

| # | Case | Expected | Req |
|---|---|---|---|
| M5.1 | Light mode, daylight. Tap a pinned icon and watch the first moment | Bone ground is there on the tap's own frame. **No white flash, no colour change** | FR-030, SC-005 |
| M5.2 | **Dark mode, dark room.** Tap a pinned icon | Dark warm background. **No white field at any point** | FR-031 |
| M5.3 | Set a 30s lock. Tap it and watch the whole wait | Screen arrives complete — rule and message together, never one before the other. Then **nothing changes for 30 seconds** | FR-029, SC-006 |
| M5.4 | Photograph the first second and the last second of M5.3 | The two frames are indistinguishable | SC-006 |
| M5.5 | Read the wait screen | `please wait`, lower case, mono, above it a short amber rule | FR-027, FR-028 |
| M5.6 | Read it again, carefully | Does **not** name the app, state a duration, or suggest loading | FR-032 |
| M5.7 | Let the wait complete | Target app opens; SlowLock leaves nothing in recents | 003, S4 |
| M5.8 | Press back mid-wait | Wait abandons; target does not open | 003, S4 |
| M5.9 | Press home mid-wait | Same | 003, S4 |
| M5.10 | Rotate mid-wait | Wait **neither restarts nor extends** | 003, S4 |
| M5.11 | Start a 30s wait, don't touch the screen | Display stays awake for the whole wait | 003, S4 |
| M5.12 | Tap the same pinned icon twice quickly | One wait, not two | 003, S4 |
| M5.13 | **Tap an icon pinned before this feature was built** | Works, and shows the redesigned wait screen. Not re-created | FR-040 |

---

## M6 — Unsupported launcher · FR-034–FR-036

| # | Case | Expected | Req |
|---|---|---|---|
| M6.1 | Set a launcher that refuses pinned shortcuts; open the app | Mono uppercase eyebrow, left-aligned message, amber primary + outlined secondary | FR-034, FR-035 |
| M6.2 | Tap "Choose home screen app" | Home settings open | FR-036 |
| M6.3 | Switch back to a supporting launcher, return to the app | Flow proceeds; no restart needed | FR-036 |
| M6.4 | Tap "Check again" while still unsupported | Re-checks; screen stays | FR-036 |

---

## M7 — Scaling, size and layout · FR-014a, SC-010

| # | Case | Expected | Req |
|---|---|---|---|
| M7.1 | **Largest system font scale, smallest available device.** Open every screen | Every interactive control fully visible **without scrolling**. On the delay screen the numeral has shrunk to make room | FR-014a, SC-010 |
| M7.2 | Same, delay screen specifically | The numeral is still the largest element on the screen | FR-014a |
| M7.3 | Smallest system font scale | Nothing is comically small or misaligned | FR-014a |
| M7.4 | Set the device to an RTL locale; open every screen | Header, preset row, treatment tiles and preview card mirror correctly | Edge Cases |
| M7.5 | Install an app with a very long label; view it in the list, the pill and the preview card | Truncates with ellipsis in all three. No overflow, no clipping | Edge Cases |

---

## M8 — Accessibility · FR-043–FR-045, SC-008, SC-011

| # | Case | Expected | Req |
|---|---|---|---|
| M8.1 | Enable a screen reader. Walk the delay screen | Every control announces a meaningful label. `5s` announces as the action it performs, not as two characters | FR-044 |
| M8.2 | With the reader on, tap a preset | The **selection change is announced** — not signalled by colour alone | FR-043 |
| M8.3 | With the reader on, walk the treatment tiles and change selection | Selection state announced, as the chips used to do | FR-043 |
| M8.4 | Walk all five screens with the reader | Every interactive control has a label; nothing is unreachable or unlabelled | SC-011 |
| M8.5 | Tap the edges of a preset chip and a treatment tile | **Known limitation**: these are 44dp, below the 48dp floor, and may be harder to hit. Record the observation — this is accepted, not a bug | FR-045 |
| M8.6 | Read captions and eyebrows in bright daylight | Legible. The thinnest pairing is muted ink on the ground at 4.74:1 | SC-008 |
| M8.7 | Confirm no accent-coloured word appears on the ground | Accent text is the darker amber; the bright amber is only fills, borders and the wait rule | FR-009, C2 |

---

## Release gate (constitution, unchanged)

Beyond this plan, before any release:

- Lock creation verified on **at least one non-Pixel OEM device**.
- Behaviour under **Xiaomi Dual Apps** recorded as tested or explicitly untested.
