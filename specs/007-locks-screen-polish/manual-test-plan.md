# Manual Test Plan: Legible system bar and a redesigned Locks screen

**Feature**: `specs/007-locks-screen-polish` | **Run by**: the maintainer, on a real device

Required by the constitution's Manual Verification rule. Every case is numbered and names the
requirement it covers. No agent may run these; an agent states which are outstanding and waits.

**Setup**: at least three locks, on different apps, with different delays (one single-digit, one
two-digit) and different icon treatments. One further lock whose target app has since been
uninstalled, for M7.

---

## M1 — The bars, dark mode · FR-001, FR-002, SC-001

1. Set the device to dark mode.
2. Open SlowLock.
3. **Pass**: the clock, signal and battery at the top are dark and readable against the bone
   background. Nothing at the top is white or washed out.

## M2 — The bars, light mode · FR-002, SC-002

1. Switch the device to light mode with the app open, then reopen it.
2. **Pass**: the top of the screen looks identical to M1. Toggling the setting changes nothing
   anywhere in the app.

## M3 — The bars, every screen · FR-003, FR-005

1. In dark mode, walk: Locks → New lock → app list → delay → icon → back to Locks. Force the
   unsupported-launcher screen if the device can (or note it as not reachable).
2. **Pass**: dark indicators on every one of them, and the app still draws behind the bars — no new
   white or grey strip appears at the top or bottom.

## M4 — The wait screen is untouched · FR-004

1. Still in dark mode, tap a pinned SlowLock shortcut.
2. **Pass**: the wait screen appears in its dark variant and behaves exactly as before — no flash,
   nothing moves, no change from this feature.
3. Repeat in light mode: the light variant, unchanged.

## M5 — The heading · FR-006, FR-007, FR-008, FR-009, FR-011, SC-003

1. Open the app on the Locks screen.
2. **Pass**: a large "Locks" title; directly beneath it an uppercase, letter-spaced mono caption
   stating the number of locks and nothing else — no mention of the home screen; nothing else above
   the list, no back control and no step counter.
3. Hold the `New · Locks` artboard beside it. **Pass**: sizes, weights, spacing and colour match.

## M6 — The rows · FR-013, FR-014, FR-015, FR-016, SC-003, SC-004

1. **Pass**: each row is a card showing, in order, the app icon, the app name, the icon treatment
   beneath the name, and the delay in an amber badge at the trailing edge.
2. **Pass**: the badge reads compactly ("10s"), in the mono face, dark amber on the pale amber fill.
3. **Pass**: the second line names the treatment only — the delay no longer appears there.
4. Ask someone how long one of the locks waits. **Pass**: they answer from the badge, at a glance.

## M7 — The uninstalled row · FR-020

1. Find the row whose app is gone.
2. **Pass**: unchanged from before this feature — its message, its visible "How to remove" control,
   no badge, and it does not respond to a tap.

## M8 — A long app name · FR-017

1. Install or pick an app with a very long name and make a lock for it.
2. **Pass**: the name ellipsises; the treatment line and the badge are both fully visible and the
   badge is not squeezed.

## M9 — Largest font scale · FR-021, SC-005

1. Set the system font size to its maximum.
2. **Pass**: title, caption, names, treatment lines and badges all grow. Rows get taller. Nothing is
   clipped or overlapping. The only truncation anywhere is a long app name.

## M10 — Screen reader · FR-012, FR-018, FR-019, SC-006

1. Turn TalkBack on and swipe through the Locks screen.
2. **Pass**: the title reads normally; the caption is read as words ("3 locks"), not spelled out.
3. **Pass**: each lock is one stop reading the app name, the treatment, and the delay **in words**
   ("10 second wait") — never "ten s".
4. **Pass**: the removal explanation is offered as an action on every row, including the
   uninstalled one.
5. **Pass**: tap still opens the lock for editing; long press still opens the removal explanation.

## M11 — Behaviour is unchanged · FR-019

1. Tap a row → the lock opens for editing on its saved values.
2. Long press a row → the removal explanation appears, with one "OK" button, and dismisses on
   outside tap and on back.
3. Tap "New lock" → the flow starts.
4. **Pass**: all three behave exactly as they did before this feature.

## M12 — Nothing else moved · FR-025, SC-003, contract L12

1. Compare the app list, delay, icon, first-run and unsupported-launcher screens against how they
   looked before the change.
2. **Pass**: identical apart from their system bars.

## M13 — RTL · edge case

1. Switch the device to an RTL locale (or enable "Force RTL layout").
2. **Pass**: the icon sits on the right, the badge on the left; nothing overlaps.

## M14 — API 26 navigation bar · contract S7 · ACCEPTED LIMITATION

Only reachable on an API 26 device or emulator image.

1. Run the app on API 26 in dark mode.
2. **Expected**: the *status* bar indicators are dark and correct. The *navigation* bar keeps light
   icons, because the platform gained dark navigation-bar icons in API 27.
3. Record the result as **tested** or **untested and accepted**. This is a known, accepted
   limitation and does not block the feature.

## M15 — OEM release gate · constitution

Unchanged standing requirement, not introduced here: before any release, shortcut pinning is
verified on at least one non-Pixel OEM device, and Xiaomi Dual Apps behaviour is recorded as tested
or explicitly untested.

---

## Results

| Case | Result | Notes |
|---|---|---|
| M1 | | |
| M2 | | |
| M3 | | |
| M4 | | |
| M5 | | |
| M6 | | |
| M7 | | |
| M8 | | |
| M9 | | |
| M10 | | |
| M11 | | |
| M12 | | |
| M13 | | |
| M14 | | |
| M15 | | |
