# Manual Test Plan: Pinned Shortcut Creation

**Feature**: `002-shortcut-pinning`

The primary verification artifact, as in feature 001 — and more so here. The open questions are
about *other apps' behaviour on real devices*: does this launcher honour the pin, does the icon
that lands match the preview, does the shortcut still work after a reboot. No test suite can
install a second launcher, answer a system dialog, or reboot a phone.

**Before filing anything as a bug, read the spec's "Accepted limitations".** No feedback after
"Create shortcut" is correct. A silent in-place update is correct. Not being able to tell a
decline from a success is correct.

---

## Tiers

| Tier | Devices | When |
|---|---|---|
| **M1–M3** | One device or emulator, API 33+ | Every change |
| **M4** | At least two launchers from different vendors (SC-008) | Before the feature is called complete |
| **M5** | One physical device, reboot and force-stop | Before the feature is called complete |
| **M6** | A device where pinning is unsupported, or a stubbed `PinSupport` | Before the feature is called complete |

Record the launcher name and version with every M4 and M5 result. SC-008 is satisfied either by
success on two vendors' launchers **or** by every failure being recorded against the launcher
that produced it — a recorded failure is a result, not a blocked test.

---

## M1 — The configuration screen

| # | Steps | Expected | Requirement |
|---|---|---|---|
| M1.1 | Open the app, tap any row | ~~The configuration screen opens~~; the app does **not** launch. **Superseded by 003 M1.1** — a row tap now opens the **delay** screen, and this screen is reached with "next". The app still does not launch | FR-001, superseded |
| M1.2 | Look at the preview | The target's icon and label, centred, at roughly home-screen proportions | FR-003, C2 |
| M1.3 | Look at the treatment row | Above the preview, horizontally scrollable, exactly Original / Invert / Gray in that order | FR-005, C3 |
| M1.4 | Note the initial selection | Original, and the preview is unmodified — **only for an app with no saved configuration**. **Narrowed by 003 M3.2**: a previously configured app opens on its saved treatment | FR-006, narrowed |
| M1.5 | Tap Invert | Colours invert instantly. **Transparent areas stay transparent** — not a solid black square | FR-007, R7 |
| M1.6 | Tap Gray | Desaturated instantly | FR-007 |
| M1.7 | Switch treatments repeatedly and quickly | No flicker, no layout shift, no lag | SC-004, C5 |
| M1.8 | Rotate the device with Gray selected | Gray is still selected and still previewed | FR-008, C7 |
| M1.9 | Open an app with a very long label | The label truncates the way a launcher would; the preview does not distort or resize | C14, edge case |
| M1.10 | Watch for permission prompts throughout | None, ever | FR-023, SC-005 |

---

## M2 — Creating and tapping a shortcut

| # | Steps | Expected | Requirement |
|---|---|---|---|
| M2.1 | Pick an app, keep Original, press "Create shortcut" | The screen closes back to the list. The launcher may show a confirmation or may pin silently — **both correct**. The app itself shows nothing | FR-010, FR-012 |
| M2.2 | Find the new icon on the home screen | One icon, the target's label, the previewed icon | FR-010, SC-003 |
| M2.3 | Tap it | ~~The target app opens immediately — no intermediate screen, no delay.~~ **Superseded by 003 M5.1**: the wait screen appears for the app's configured delay, then the target opens. "No countdown" is the one clause that survives, and 003 M4 now tests it as a design obligation | FR-016, superseded |
| M2.4 | Watch closely while tapping | ~~No SlowLock screen flashes, not even for a frame.~~ **Superseded by 003 M4**: a SlowLock screen is now the point — it is shown deliberately, for the whole delay. What survives is that nothing flashes *on the way in or out* of it, which 003 M4.1 and M5.3 cover | FR-019, superseded |
| M2.5 | Open the recents switcher | The target is there; **SlowLock is not** | FR-019 |
| M2.6 | Repeat M2.1–M2.3 with Invert, on a different app | The pinned icon is inverted and matches what the preview showed | SC-003 |
| M2.7 | Time the whole flow from the list | ≤3 taps past selecting the app, under 30 seconds | SC-001 |

---

## M3 — Backing out, declining, and things going missing

| # | Steps | Expected | Requirement |
|---|---|---|---|
| M3.1 | Open the config screen, press the back affordance | Back to the list, no shortcut created | FR-020 |
| M3.2 | Open it again, use the system back gesture | Same | FR-021 |
| M3.3 | Scroll far down the list, search for something, tap a row, come back | **Scroll position and search query both preserved** | FR-022 |
| M3.4 | Press "Create shortcut", then decline the launcher's dialog | No shortcut created; the app carries on normally; no error shown; no crash | FR-014, SC-006 |
| M3.5 | Repeat M3.4 five times | Usable every time, no crash | SC-006 |
| M3.6 | Open the config screen for an app, uninstall that app from another window, then press "Create shortcut" | No shortcut created, and the user is told. The screen stays open | FR-015, C11 |
| M3.7 | Pin a shortcut, then uninstall the target, then tap the shortcut | No crash; the user is told the app is unavailable | FR-018 |
| M3.8 | Open the config screen for an app whose icon fails to load | A neutral placeholder is previewed; **"Create shortcut" is disabled** with a short explanation; no crash. Backing out and reopening retries the load | C12, edge case |

---

## M4 — Re-pinning, and the launcher matrix *(two vendors, SC-008)*

Run the whole section on each launcher. Record launcher name and version.

| # | Steps | Expected | Requirement |
|---|---|---|---|
| M4.1 | Pin app X as Original | One icon appears | FR-010 |
| M4.2 | Pin app X again as Gray | **Still exactly one icon**, now grey. Likely no dialog and no visible feedback at all — correct | FR-026, accepted limitation |
| M4.3 | Count home-screen icons for X | Exactly one, always | FR-025 |
| M4.4 | `adb shell dumpsys shortcut \| grep -A 20 com.slowlock` | One shortcut whose ID is X's package name | FR-025, FR-027 |
| M4.5 | Compare the pinned icon against the preview | Matching treatment. Note if the launcher adds its own background plate — **record it, do not fix it** | SC-003, R8 |
| M4.6 | Note whether this launcher shows a pin dialog at all | Recorded either way | R3 |

**Record for each launcher**: vendor, version, pin honoured (y/n), dialog shown (y/n), icon
faithful (y/n/plated), and anything surprising. This table is the feasibility answer the whole
feature exists to produce.

---

## M5 — Surviving without the app *(physical device)*

| # | Steps | Expected | Requirement |
|---|---|---|---|
| M5.1 | Pin a shortcut, then `adb shell am force-stop com.slowlock`, then tap it | The target opens | FR-017, SC-007 |
| M5.2 | `adb reboot`, wait for boot, tap the shortcut without opening SlowLock first | The target opens | FR-017, SC-007 |
| M5.3 | Clear SlowLock from recents, tap the shortcut | The target opens | FR-017 |
| M5.4 | After M5.2, check the icon still shows the treatment chosen at pin time | Unchanged | FR-010 |

> **Amended by feature 003 (M6.1, M6.3).** In M5.1 to M5.3 the target no longer opens
> *immediately*: **the wait screen appears first, for the app's configured delay, and the target
> opens after it.** The delay is read off disk on the launch path, so a force-stopped or
> just-rebooted process reads it exactly as a warm one does — which makes these three cases
> **stronger** evidence than they were, not weaker. `specs/003-launch-delay/manual-test-plan.md`
> M6.1 and M6.3 supersede them and are the versions to run. M5.4 is unaffected.

A failure here means the launch path depends on SlowLock's process state, which the design
forbids (`contracts/pinned-shortcut.md`, L6). Feature 003 did not weaken that: it added a disk
read to the same path, and `DelayConfigStore` answers with the default rather than failing when
nothing is stored, so a wait still runs with no in-process state whatsoever.

---

## M6 — Where pinning is unsupported

Needs a launcher that refuses pin requests, or a temporary stub returning `Unsupported` (which
must not be committed).

| # | Steps | Expected | Requirement |
|---|---|---|---|
| M6.1 | Open the app | The explanation screen, **instead of** the app list, on the first screen shown | FR-029, SC-009 |
| M6.2 | Read it | A sentence or two, plain language, no error codes, no API names | FR-030, U2 |
| M6.3 | Try to reach the list or the config screen | Impossible by any route | FR-029, SC-009 |
| M6.4 | Press the launcher-settings control | The system's default-launcher setting opens; if it cannot, the app says so and stays usable | FR-031, U3, U4 |
| M6.5 | Press re-check while still unsupported | Stays on the explanation screen, no crash, no flicker | FR-031, U5 |
| M6.6 | Switch to a launcher that supports pinning, return to the app **without restarting it** | The app list appears | FR-028, FR-032 |
| M6.7 | Switch back to the unsupported launcher and return | The explanation screen returns | FR-028 |

---

## Regressions in feature 001

Feature 001's tap behaviour is replaced by this feature; its T1.12 and T1.16 no longer describe
reality and are re-written as part of this work.

| # | Steps | Expected | Requirement |
|---|---|---|---|
| M7.1 | Re-run 001's T1.1–T1.11 | Unchanged — enumeration, ordering, search, empty and no-results states all behave as before | 001 |
| M7.2 | 001 T1.12 (was: tap launches the app) | **Superseded** — a tap now opens the configuration screen | FR-001 |
| M7.3 | 001 T1.16 (was: state preserved after tap-and-return) | **Re-written** — state preserved after returning from the configuration screen | FR-022 |
| M7.4 | Confirm `AppListScreen.kt` was not modified | Untouched. The seam did its job | 001 selection-handoff |

---

## Sign-off

The feature is not complete until:

- [X] `./gradlew assembleDebug` passes
- [X] `./gradlew test` passes
- [X] M1–M3 pass on at least one device
- [ ] M4 run on two vendors' launchers, with the results table filled in — successes or recorded failures (SC-008)
- [X] M5 run on a physical device, including a real reboot (SC-007)
- [X] M6 run against an unsupported launcher or a stub (SC-009)
- [X] M7 confirms feature 001 still behaves, with T1.12/T1.16 updated
- [ ] Behaviour under Xiaomi Dual Apps recorded as tested or explicitly untested (Constitution, Manual verification)
