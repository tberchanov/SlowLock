# Manual Test Plan: Launch Delay

**Feature**: `003-launch-delay`

**The only verification of anything that needs a running app.** Constitution v1.1.0 forbids
instrumented suites and forbids an agent driving the connected device, so there is no
`connectedAndroidTest` behind this plan and nothing else checks the behaviours below. JVM unit
tests cover the pure core — configuration sanitising, the frozen tokens, the slider mapping, the
deadline arithmetic — and stop there.

The emphasis has also moved since features 001 and 002. Most of what matters here is a **negative**
claim: nothing moved, nothing appeared, nothing was in recents, nothing happened after the user
walked away. Absence is what a person is good at judging and a harness is not.

**Before filing anything as a bug, read the spec's "Accepted limitations".** No countdown is
correct. No confirmation after applying is correct. A wait that dies when the device locks is
correct. A shortcut pinned before this feature that suddenly pauses is correct.

---

## Tiers

| Tier | Devices | When |
|---|---|---|
| **M1–M3** | One device or emulator, API 33+ | Every change |
| **M4** | One device, screen recording available, light **and** dark mode | Every change to the wait screen |
| **M5** | One physical device | Before the feature is called complete |
| **M6** | One physical device, reboot + force-stop + app update | Before the feature is called complete |
| **M7** | A device with a shortcut pinned by the **previous** build (feature 002) | **Once, on first install of this build** — see below |
| **M8** | One non-Pixel OEM device; Xiaomi Dual Apps if available | Before release (constitution release gate) |

Record the device, launcher, OS version, and configured screen timeout with every M4–M8 result.
The screen timeout matters: M4.6 and M5.5 are about it.

**M7 is single-use and cannot be repeated.** It needs a device still carrying shortcuts pinned by
the feature 002 build, which stops being true the moment this build is installed. Run it on the
first install, record the result, and do not schedule it into a later full pass — a "complete M1–M8
run" at the end of the feature means M1–M6 and M8, with M7's earlier result carried forward.

---

## M1 — The delay configuration screen

| # | Steps | Expected | Requirement |
|---|---|---|---|
| M1.1 | Open the app, tap any row | The **delay** screen opens — not the shortcut screen, and the app does not launch | FR-001 |
| M1.2 | Look at the screen | The target's icon and label identify which app is being configured | FR-003, D2 |
| M1.3 | Look at the slider before touching it | It sits at 10 seconds, and "10 seconds" is shown beside it | FR-006, D12 |
| M1.4 | Drag the slider slowly across its range | It stops on every whole second from 1 to 30 and on nothing between — no fractional value is ever shown. The readout tracks every stop | FR-005, FR-007, D3, D6 |
| M1.5 | Drag to each end | Minimum 1 second, maximum 30 seconds; neither can be exceeded. At the minimum the readout reads **"1 second"**, singular | FR-005, FR-007 |
| M1.6 | Set 25 s, rotate the device | Still 25 s, still shown | FR-008 |
| M1.7 | Press the back affordance | The list returns | FR-010 |
| M1.8 | Reopen the same app | 10 seconds again — backing out saved nothing | FR-020 |
| M1.9 | Use the system back gesture instead | Same as the affordance | FR-010, D7 |
| M1.10 | Return to the list after scrolling far down and searching | Scroll position and search query are as they were | FR-011, N3 |
| M1.11 | Watch for permission prompts throughout | None, ever | FR-034, SC-009 |

## M2 — The trip to the shortcut screen and back

| # | Steps | Expected | Requirement |
|---|---|---|---|
| M2.1 | Set 30 s, press next | Feature 002's shortcut screen opens for the same app | FR-009 |
| M2.2 | Press back on the shortcut screen | The **delay** screen returns — not the list | FR-014 |
| M2.3 | Look at the slider | Still 30 s, the value chosen on the way through | FR-014, D5 |
| M2.4 | Change to 15 s, next, back, next again | 15 s carries forward each time; no value is ever lost or reset | FR-014 |
| M2.5 | From the shortcut screen, press back twice | Delay screen, then the list. Nothing was saved and no icon was created | FR-014, FR-020 |
| M2.6 | Set 20 s, next, pick Gray, Create shortcut | The screen closes to the **list**, silently, and an icon appears on the home screen | FR-015, FR-019 |
| M2.7 | Time the whole run from the list to the icon, counting taps after selecting the app | Under 45 seconds, no more than five taps (a slider drag counts as one) | SC-001 |

## M3 — Saved configuration comes back

| # | Steps | Expected | Requirement |
|---|---|---|---|
| M3.1 | After M2.6, select the same app from the list | The delay screen opens at **20 s** | FR-012 |
| M3.2 | Press next | The shortcut screen opens with **Gray** already selected and previewed | FR-013, C15 |
| M3.3 | Change to 25 s, keep Gray, Create shortcut | Screen closes silently | FR-015 |
| M3.4 | Look at the home screen | Still exactly **one** icon for that app | FR-019 |
| M3.5 | Tap that icon | It waits **25 s**, not 20 — with no icon removed or re-added | FR-018, SC-007 |
| M3.6 | Configure a second app at 1 s | Tapping each icon waits its own app's delay. The 1 s wait is brief but the screen must still appear rather than being skipped | FR-016, SC-008 |
| M3.7 | Open the delay screen for a configured app and press back | The saved delay is unchanged on the next tap of its icon | FR-020 |
| M3.8 | Configure an app, decline the launcher's pin dialog, reopen that app in the list | The chosen delay is shown — a configuration with no icon is saved and harmless | Accepted limitations |

## M4 — The wait screen is boring

**Record the screen for the whole of a 30-second wait** — the longest the slider offers. SC-002 is checked by comparing the first
**settled** frame of that recording — once the message has rendered — with the last, not by
watching. Run the tier twice: once in light mode, once in dark (M4.9).

| # | Steps | Expected | Requirement |
|---|---|---|---|
| M4.1 | Tap a configured shortcut | The background is there within 200 ms — on the tap's own frame in practice — and the message within 500 ms. No white flash, no colour change, no app-icon splash | FR-022, W12 |
| M4.2 | Compare the recording's first **settled** frame (message rendered) with its last | Identical. Frames before it settles are the screen arriving and are excluded | FR-023, SC-002 |
| M4.3 | Look for a countdown, a timer, a bar, a ring, a spinner, a pulse | None of them exists | FR-023, W8 |
| M4.4 | Listen and feel | No sound, no vibration, no notification | FR-024, W9 |
| M4.5 | Tap the screen repeatedly, tap the text, try to drag it | Nothing happens. The wait is neither shortened nor extended | FR-026, W10 |
| M4.6 | Wait past the device's screen timeout (set it to 15 s, use the maximum 30 s delay) | The display stays on for the whole wait | W13, R6 |
| M4.7 | Check the screen against the target app | It does not name or depict the target app; it is identical for every app | W11 |
| M4.8 | Rotate the device mid-wait | The wait neither restarts nor extends — the target opens at the original moment | FR-027, W4 |
| M4.9 | Switch the device to dark mode and repeat M4.1 and M4.2 | A dark ground, not a white one, and still no flash on tap — the window background and the screen match in dark mode too | W12, spec Assumptions |

## M5 — Ending the wait

| # | Steps | Expected | Requirement |
|---|---|---|---|
| M5.1 | Set a 10 s delay, tap the icon, stay on the screen, time it | The target opens by itself, no earlier than 10 s and within a second after | FR-028, FR-037, SC-003 |
| M5.2 | Immediately open the recents list | **No SlowLock entry.** The target app is there; SlowLock is not | FR-031, SC-004 |
| M5.3 | Press back out of the target app | The home screen or the previous app — never the wait screen | FR-031 |
| M5.4 | Tap the icon, press system back mid-wait | The wait ends, the target never opens, and nothing appears later | FR-029, SC-005 |
| M5.5 | Tap the icon, press home mid-wait, wait past the delay, then wait a further minute | The target never opens. Nothing appears over whatever you moved on to | FR-029, SC-005 |
| M5.6 | Tap the icon, press the power button mid-wait, unlock after the delay would have passed | The target is **not** open, and does not open on unlock | FR-029, clarification Q2 |
| M5.7 | Tap the icon, open the app switcher mid-wait, pick another app | The target never opens | FR-029 |
| M5.8 | After any abandonment, tap the icon again | A fresh, full wait — no credit for time already served | FR-029, US4.5 |
| M5.9 | Tap the icon, and during the wait tap it again from a split-screen or notification route if reachable | No second wait screen, no restart, no extension | FR-027, W22 |

## M6 — Durability

| # | Steps | Expected | Requirement |
|---|---|---|---|
| M6.1 | Force-stop SlowLock, tap a configured shortcut | The wait runs and the target opens, exactly as before | FR-033, W24 |
| M6.2 | Force-stop SlowLock, open it, select a configured app | The saved delay and treatment are shown | FR-017, SC-006 |
| M6.3 | Reboot the device, repeat M6.1 and M6.2 | Both unchanged | FR-017, FR-033 |
| M6.4 | Install a newer build over the top, repeat M6.2 | The saved delay and treatment survive the update | FR-017 |
| M6.5 | Clear the app's data, tap an existing shortcut | It waits the **default** 10 s and opens the target. The icon still works | FR-032, edge case |
| M6.6 | Uninstall the target app, then tap its shortcut | "That app is no longer available", **with no wait first**, and no crash | FR-030, W5 |
| M6.7 | Tap a shortcut, and uninstall the target during the wait | At the end of the wait: the same message, no crash | FR-030, W17, W18 |
| M6.8 | Use the device normally for a day with several apps configured, then open Settings → Battery and look for SlowLock | No SlowLock entry, or one accounted for entirely by the waits' own screen time. No background usage, no wakelock line | FR-035, SC-010 |

## M7 — Shortcuts from the previous build

The migration case. Needs a shortcut pinned by the **feature 002 build**, before this feature was
installed.

| # | Steps | Expected | Requirement |
|---|---|---|---|
| M7.1 | Install this build over a build with pinned shortcuts. Tap an old shortcut | It now waits the default 10 s, then opens the target | FR-032, SC-011 |
| M7.2 | Check the home screen | The icon is unchanged — same picture, same label, same position. Nothing was re-pinned | 002 FR-011 |
| M7.3 | Configure that app to 30 s and apply | The same icon now waits 30 s; still exactly one icon | FR-018, FR-019 |

---

## M8 — Release gate: other vendors' devices

The constitution's release gate, and the only tier that can answer SC-004. Not needed on every
change; needed before this ships.

| # | Steps | Expected | Requirement |
|---|---|---|---|
| M8.1 | On a non-Pixel OEM device, run M2.6, then M5.1 and M5.2 | A shortcut is pinned, waits its delay, opens the target, and leaves no recents entry | Constitution §Manual verification, SC-004 |
| M8.2 | On that device, run M5.5 (home mid-wait) and M4.6 (screen timeout) | Same behaviour as the primary device. Aggressive OEM battery management must not launch the target after the user left, nor kill the wait early | FR-029, W13 |
| M8.3 | If a Xiaomi device with Dual Apps is available, configure a cloned app and tap its shortcut. If not available, record it as **untested** | Recorded either way — tested with the result, or explicitly untested. An unrecorded gap is the failure here | Constitution §Manual verification |
| M8.4 | Note the launcher name and version for every M8 result | Recorded alongside the outcome | SC-004 |

## What a failure here means

| Symptom | Most likely cause |
|---|---|
| The wait restarts when you rotate | `onStop` finishing without the `isChangingConfigurations` exception, or the deadline not saved (W4, W15) |
| Back from the shortcut screen shows the saved delay, not the chosen one | The delay screen is owning the value instead of the root (D5, R9) |
| A flash of a different colour when the icon is tapped | `windowBackground` and the composable are not painting the same colour resource (W12) |
| Old shortcuts stopped working entirely | Something changed in `002-shortcut-pinning/contracts/pinned-shortcut.md`. Stop and check the frozen values |
| Every configured icon reverted to Original | An `IconTreatment` constant was renamed (`contracts/delay-config-store.md`) |
| The target opens after the user pressed home | The `STARTED` re-check before `startActivity` is missing (W16) |
