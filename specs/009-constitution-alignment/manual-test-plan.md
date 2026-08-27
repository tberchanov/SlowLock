# Manual Test Plan: Constitution Alignment Refactor

**Feature**: `009-constitution-alignment` | **Date**: 2026-08-26 | **Spec**: [spec.md](./spec.md)

**This plan proves a negative.** Every other feature's plan asks "does the new thing work?"; this
one asks "is anything at all different?" The refactor's whole acceptance bar is FR-001 — no screen,
layout, wording, interaction, timing or ordering may differ from the pre-refactor build — and the
only instrument that can judge that is a person holding the baseline screenshots beside the
device.

Constitution v2.1.0 forbids instrumented suites and forbids an agent driving the connected device.
`./gradlew test` covers the pure core — the frozen values against literals, the sanitising reads,
the deadline arithmetic — and stops there. Nothing below is checked anywhere else.

**The comparison is against the baseline, not against memory.** The baseline is the install,
the screenshots and the two configured locks captured in T008 per
[quickstart.md](./quickstart.md). A case with no baseline to compare against cannot be run.

**Before filing anything as a bug**, check [contracts/frozen-values.md](./contracts/frozen-values.md)
and the spec's FR-001a. A rendering difference that comes from an upgraded library's own changed
default is a **finding for the maintainer to rule on**, not automatically a defect. A difference
originating in this project's own code is never covered by that exception.

---

## Tiers

| Tier | Devices / state | When |
|---|---|---|
| **M1** | The **baseline install**, updated in place — never uninstalled | **Once**, at the final gate. Single-use: see below |
| **M2** | One device or emulator, API 33+, light **and** dark mode | Every stage that touches UI |
| **M3** | One device or emulator, API 33+ | Every stage |
| **M4** | One physical device, at least one shortcut already pinned | Every stage; again at the final gate |
| **M5** | One device or emulator, API 33+ | Every stage that touches a string or a resource |
| **M6** | One physical device, reboot + force-stop available | Final gate |

**M1 is single-use and cannot be repeated.** It needs a device still carrying the *pre-refactor*
build's data. That stops being true the moment the post-refactor build is installed. Run it on the
first in-place update, record the result, and carry it forward — a later "full pass" means M2–M6.

Record device, launcher, OS version and theme with every result.

**Device-required cases are marked ✅ in the Device column.** Those cases cannot be observed
anywhere but on a running app on real hardware — a launcher's pin dialog, a hand-off between
processes, and elapsed wall-clock timing have no JVM equivalent. Cases without the mark can be run
on an emulator.

---

## M1 — In-place update loses nothing

**Tier M1. Run once, on the first update over the baseline install. Do not uninstall first.**

This is the case the four frozen values exist for. Every failure here is silent on the device and
invisible in the diff.

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| M1.1 | Install the post-refactor build **over** the baseline install (`adb install -r`, or a Play internal-track update). Do not uninstall, do not clear data | The update succeeds and the app opens to the Locks screen, not the intro | ✅ | FR-003, FR-005 |
| M1.2 | Compare the Locks list against the baseline screenshot | The same locks, the same number of rows, in the **same order** | | FR-003, SC-002 |
| M1.3 | Open each lock's delay screen | Each shows the delay configured in the baseline — not 10 seconds, unless 10 was what was set | | FR-003, F2 |
| M1.4 | Check each lock's icon treatment on the shortcut screen | The treatment configured in the baseline is still the selected one | | FR-003, F2 |
| M1.5 | Tap each pinned icon on the home screen | Each opens the wait screen and hands off to **its own** target app | ✅ | FR-004, SC-002 |
| M1.6 | Look at the home-screen icons themselves | Unchanged — same label, same treatment, same position. No icon has gone dead, and no second icon has appeared for any app | ✅ | FR-004, FR-005 |
| M1.7 | Count what the update asked of the user | Nothing. No re-pin prompt, no re-configuration, no permission dialog, no migration screen | ✅ | FR-005 |

> A failure in M1.2 or M1.3 means a preferences file name or key changed — check F2 and F3.
> A failure in M1.5 means the launch activity's fully-qualified name changed — check F1.
> A failure in M1.5 where a *second* icon appears means the shortcut ID scheme changed — check F4.

---

## M2 — Every screen renders as it did

**Tier M2. Run in light and dark mode. Compare each against the matching baseline screenshot.**

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| M2.1 | Fresh install on a device with no locks; open the app | The intro screen, pixel-for-pixel as the baseline | | FR-001 |
| M2.2 | Open the Locks screen with at least two locks | Same layout, same row shape, same ordering, same header | | FR-001 |
| M2.3 | Open the app list | Same layout, same ordering (label, current locale), same search affordance, same icons | | FR-001 |
| M2.4 | Open the delay configuration screen | Same layout, same slider position, same presets, same readout wording | | FR-001, FR-002 |
| M2.5 | Open the shortcut configuration screen | Same layout, same three treatments in the same order, same previews | | FR-001 |
| M2.6 | Tap a pinned icon and hold on the wait screen | Same layout, same background colour, still deliberately motionless — no countdown, no progress indicator | ✅ | FR-001 |
| M2.7 | Rotate on each screen above | Each survives rotation exactly as the baseline did | | FR-001 |
| M2.8 | Repeat M2.1–M2.6 in the other theme | Same as the baseline for that theme | | FR-001 |
| M2.9 | Note any difference at all from a baseline screenshot | Record it. **Do not fix it and do not accept it** — it goes to the maintainer for a ruling under FR-001a | | FR-001a, SC-005 |

---

## M3 — Every interaction behaves as it did

**Tier M3.**

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| M3.1 | Run one complete create-a-lock pass: list → app → delay → shortcut → Create | The same screens in the same order, the same number of taps, and the pin dialog appears | ✅ | FR-001 |
| M3.2 | Cancel the launcher's pin dialog | The app degrades gracefully, no lock is created, and nothing is left half-written | ✅ | FR-001 |
| M3.3 | From the shortcut screen, press back | The delay screen returns with the value chosen on the way through — not the list, not a reset value | | FR-001 |
| M3.4 | Scroll far down the app list, search, open an app, come back | Scroll position and query are as they were | | FR-001, FR-023a |
| M3.5 | Enter the delay screen from the **Locks** screen, then press back | Returns to the Locks screen. Enter it from the **app list** and press back: returns to the app list | | FR-001, FR-023a |
| M3.6 | Re-pin an app that already has a lock | One icon, not two; no duplicate row on the Locks screen | ✅ | FR-001, F4 |
| M3.7 | Drag a pinned icon off the home screen, reopen the app | The lock disappears from the Locks screen | ✅ | FR-001 |
| M3.8 | Watch for permission prompts throughout M3 | None, ever | ✅ | FR-001, FR-006 |
| M3.9 | Open the app after a device reboot **without unlocking first**, if the launcher allows it | Same behaviour as the baseline: the lock list is not emptied | ✅ | FR-001 |

### M3b — The root's scroll, query and back-origin (R9)

**Tier M3.** The root arbiter was split: the pre-navigation configuration read and the pin-support
check moved into `RootViewModel`, while the navigation `stage` deliberately stayed in
`rememberSaveable` (FR-023a). Three specified behaviours ride on exactly that split, and each is
invisible until someone walks the flow:

- **scroll and query survive the round trip**, because the root's `SaveableStateHolder` retains the
  list's entry while dropping the other two;
- **back returns to whichever screen the flow was entered from**, because the origin is part of the
  stage;
- **the delay and treatment chosen on the way through are carried in the stage**, not re-read from
  disk and not held by the screens that show them.

A change that moved the stage into a `SavedStateHandle` would break all three while still building
and still looking correct on a quick pass. These cases are the check.

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| M3b.1 | Scroll the app list far down, type a query, open an app, press back | The query is still there, the scroll position is where it was, and the list is not back at the top | | FR-001, FR-023a, R9 |
| M3b.2 | Repeat M3b.1 but go two screens deep — app → delay → shortcut — then back all the way | Scroll and query still intact after the **two-screen** round trip, which is the case the holder was added for | | FR-001, FR-023a, R9 |
| M3b.3 | Scroll and query the app list, then **rotate** | Both survive the rotation, on the list itself and on the way back from the flow | | FR-001, FR-023a, R9 |
| M3b.4 | Scroll and query the app list, background the app, and let the system kill it (developer options → "Don't keep activities", or `adb shell am kill`) | The query survives process death. Compare the scroll behaviour against the baseline and record it either way | ✅ | FR-001, FR-023a, R9 |
| M3b.5 | Enter the delay screen from a **lock row** on the Locks screen, then press back | Returns to the **Locks** screen | | FR-001, FR-023a, R9 |
| M3b.6 | Enter the delay screen from the **app list**, then press back | Returns to the **app list**, with its scroll and query | | FR-001, FR-023a, R9 |
| M3b.7 | Repeat M3b.5 and M3b.6 using the **system back gesture** rather than the on-screen control | Identical to the on-screen control in both cases — same destination, same state | | FR-001, FR-030, R9 |
| M3b.8 | Enter the flow from a lock row, **rotate** on the delay screen, then press back | Still returns to the Locks screen. Rotation does not lose where the flow was entered from | | FR-001, FR-023a, R9 |
| M3b.9 | Move the slider to a non-default delay, continue to the shortcut screen, then press back | The delay screen shows **the value chosen on the way through**, not the value on disk and not the default | | FR-001, FR-014, R9 |
| M3b.10 | Repeat M3b.9 with a rotation on the shortcut screen before pressing back | Same — the carried value survives the rotation | | FR-001, FR-014, R9 |
| M3b.11 | Choose a treatment on the shortcut screen, press back to the delay screen, then go forward again | Matches the baseline. **Note**: the treatment is dropped on leaving the flow by design (root N3); a treatment that no longer persists across a full exit is the *specified* behaviour, not a regression | | FR-001, R9 |
| M3b.12 | Open a lock that already has a configuration | The delay screen opens on the **saved** values, not the defaults — the root's pre-navigation read still happens before it navigates | | FR-001, FR-012, R9 |
| M3b.13 | Switch to a launcher that does not support pinning while the app is open, then return to it | The pin-unsupported screen appears on the next `ON_START`, as the baseline. Switch back and the flow returns | ✅ | FR-001, FR-028, R9 |

---

## M4 — Timing is unchanged (FR-001b)

**Tier M4. This is the case the injection mechanism is most likely to cost.** Hilt adds a
generated component and a factory hop to the cold start of `ShortcutLaunchActivity`, which is on
the path a user is already waiting on.

**No numeric threshold and no measurement tooling.** The bar is what the maintainer perceives,
judged against the baseline on the same device.

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| M4.1 | Force-stop SlowLock. Tap a pinned icon (a genuine cold start) | The wait screen appears with **no perceptible pause** — no blank frame, no flash of a different colour, no visible delay between the tap and the screen | ✅ | FR-001b, SC-015 |
| M4.2 | Repeat M4.1 five times, alternating with the baseline build if a second device is available | Consistent. A pause that appears on some taps and not others still counts as a pause | ✅ | FR-001b |
| M4.3 | Set a lock to 30 s, tap its icon, and time the wait against a stopwatch | It ends at approximately the configured moment, and no later than the baseline did | ✅ | FR-001b |
| M4.4 | Set a lock to the 1 s minimum, tap its icon | The hand-off happens promptly and the wait screen is not skipped | ✅ | FR-001b |
| M4.5 | Tap a pinned icon and rotate the device mid-wait | The wait **does not restart**. It ends at the same moment it would have | ✅ | FR-001b, R10 |
| M4.6 | Tap a pinned icon, press home mid-wait, do not return | The wait ends silently. The target app does **not** launch into the foreground later | ✅ | FR-001b, R10 |
| M4.7 | Note any pause a user would notice | Record it as a finding. It is **not** an acceptable cost of the injection mechanism | ✅ | FR-001b |

### M4b — The wait's rotation and abandonment behaviour (R10)

**Tier M4. The highest-risk change in the feature.** The wait moved out of
`ShortcutLaunchActivity` and into `WaitViewModel`, and with it the mechanism that survives a
rotation: an instance-state bundle became a `SavedStateHandle`, and the coroutine moved from
`lifecycleScope` — which a rotation cancels — into `viewModelScope`, which it does not. The
behaviour is meant to be identical and arrive more directly. **These cases are what decides
whether it is.**

`WaitViewModelTest` covers the deadline arithmetic on virtual time — withheld until the deadline,
delivered once, resumed rather than restarted, re-anchored on a new target. It cannot cover a real
activity being torn down, a real process being killed, or a real launcher. Everything below is
checked here or nowhere.

**Timing note**: use a 30-second lock throughout M4b, so there is room to act mid-wait and read a
stopwatch against the result.

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| M4b.1 | Tap a pinned icon, rotate at ~5 s, rotate back at ~10 s, and time the total | The wait ends 30 s after the **tap**, not 30 s after either rotation. The screen does not blink, restart, or show a different frame | ✅ | FR-001b, R10 |
| M4b.2 | Tap a pinned icon and rotate **repeatedly** — six or more times across the wait | Still ends 30 s after the tap. Repeated rotation neither extends the wait nor triggers a second hand-off | ✅ | FR-001b, R10 |
| M4b.3 | Tap a pinned icon, rotate at ~29 s so the rotation straddles the deadline | Exactly one hand-off, and the target app opens once. **Two launches here is the defect this case exists for** | ✅ | FR-001b, FR-038, R10 |
| M4b.4 | Tap a pinned icon; mid-wait force-stop SlowLock from system settings, then reopen the launcher | Nothing launches, then or later. The wait is gone with the process | ✅ | FR-001b, R10 |
| M4b.5 | Tap a pinned icon, press **back** mid-wait | The wait is abandoned and the target does not launch — matching the baseline exactly | ✅ | FR-001b, R10 |
| M4b.6 | Tap a pinned icon, press **home** mid-wait, then return via recents before the deadline | Matches the baseline. Whatever the baseline did — resume the same wait, or abandon it — is what this build must do | ✅ | FR-001b, R10 |
| M4b.7 | Tap a pinned icon, press home mid-wait, and open a different app until well past the deadline | The target app **never** comes to the foreground. Abandonment is silent | ✅ | FR-001b, R10 |
| M4b.8 | Tap the **same** pinned icon a second time mid-wait | Nothing visibly happens: no restart, no extension, no second wait, and the deadline is unchanged | ✅ | FR-001b, R10 |
| M4b.9 | Tap a **different** lock's pinned icon mid-wait | The wait re-anchors to the new target and runs that lock's full delay. The first target does not launch | ✅ | FR-001b, R10 |
| M4b.10 | Set a lock longer than the device's screen timeout (e.g. 60 s if it sleeps at 30 s) and tap it | The display stays on for the whole wait and the hand-off happens. `FLAG_KEEP_SCREEN_ON` is still doing its job | ✅ | FR-001b, R10 |
| M4b.11 | Tap a pinned icon whose app was uninstalled since the lock was made | The unavailable message appears **immediately** — the user does not sit through the delay first | ✅ | FR-001b, R10 |
| M4b.12 | Tap a pinned icon, then lock the screen mid-wait and unlock after the deadline | Matches the baseline. Record the behaviour either way | ✅ | FR-001b, R10 |

---

## M5 — Not one string differs (FR-002)

**Tier M5.**

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| M5.1 | Read every visible string on every screen against the baseline screenshots | Identical, character for character. No added word, no removed word, no changed capitalisation | | FR-002, SC-003 |
| M5.2 | Trigger the "app unavailable" message from the app list | The same wording as the baseline, shown the same way | | FR-002 |
| M5.3 | Trigger every snackbar on the shortcut configuration screen | Same wording, same duration, same placement | | FR-002 |
| M5.4 | Read the delay readout at 1 second and at 2 seconds | "1 second" singular, "2 seconds" plural — exactly as the baseline | | FR-002 |
| M5.5 | Rotate while a one-shot message is showing | Matches the baseline's behaviour. **Note**: US5 changes the mechanism behind one-shot messages; a message that no longer re-appears after rotation is the *intended* change and is recorded, not filed as a regression | | FR-002, FR-038 |
| M5.6 | Diff `app/src/main/res/values/strings.xml` against the baseline commit | Zero changes | | FR-002, SC-003 |

---

## M6 — Nothing was added, removed, or newly asked for

**Tier M6. Final gate.**

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| M6.1 | Walk every screen looking for anything that was not in the baseline | Nothing new. No setting, no button, no badge, no toggle | | FR-006 |
| M6.2 | Walk every screen looking for anything missing | Nothing gone | | FR-006 |
| M6.3 | Check the app's permission list in system settings | Identical to the baseline. No `QUERY_ALL_PACKAGES`, no accessibility, no usage-stats | ✅ | FR-006 |
| M6.4 | Leave the app closed for an hour, then check battery usage | Zero background cost, as the baseline. No service, no wake lock | ✅ | FR-006 |
| M6.5 | Check recents after a wait | No SlowLock entry, as the baseline | ✅ | FR-006 |

---

## Traceability

| Requirement | Cases |
|---|---|
| FR-001 — no user-visible behaviour differs | M2.1–M2.8, M3.1–M3.9, M3b.1–M3b.13 |
| FR-001a — approved library-default rendering differences only | M2.9 |
| FR-001b — timing bar for the pinned-icon tap | M4.1–M4.7, M4b.1–M4b.12 |
| FR-002 — not one user-facing string differs | M5.1–M5.6 |
| FR-003 — every persisted value survives byte-identical | M1.1–M1.4 |
| FR-004 — every already-pinned shortcut keeps working | M1.5, M1.6 |
| FR-005 — nothing is re-created, re-pinned or re-configured | M1.1, M1.6, M1.7 |
| FR-006 — no capability added, removed, or scope changed | M6.1–M6.5 |
| FR-023a — the navigation stage stays in `rememberSaveable` | M3.4, M3.5, M3b.1–M3b.8, M3b.12 |
| FR-038 — one-shot events cannot re-fire | M4b.3, M5.5 |
| R9 — the root arbiter split | M3b.1–M3b.13 |
| R10 — the wait moved into a state holder | M4.5, M4.6, M4b.1–M4b.12 |

## Result log

| Date | Build / stage | Device | Launcher | OS | Theme | Cases run | Result | Findings raised |
|---|---|---|---|---|---|---|---|---|
| | | | | | | | | |
