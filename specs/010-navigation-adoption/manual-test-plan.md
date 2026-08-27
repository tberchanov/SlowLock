# Manual Test Plan: Navigation Adoption

**Feature**: `010-navigation-adoption` | **Date**: 2026-08-27 | **Spec**: [spec.md](./spec.md)

**This plan proves a negative, with four exceptions.** The acceptance bar is FR-001 — nothing a
user can see may differ from the pre-change build — and FR-002 names three approved differences.
A **fourth** was added by maintainer direction on 2026-08-27, after the first manual pass: the graph
now cross-fades between destinations (N2, G11). A *fifth* difference is a defect.

Everything below is judged against the **baseline** captured before any source was changed:
the install, the two configured locks, and the screenshots of every screen
([quickstart.md](./quickstart.md), "Before anything is changed"). A case with no baseline to
compare against cannot be run.

The constitution forbids instrumented suites and forbids an agent driving the connected device, so
`./gradlew test` covers only the two holder branches research R8 names. Nothing below is checked
anywhere else.

**Before filing anything as a bug**: a difference the navigation library itself introduces is
still a difference. The four listed above are the only approved ones; anything else is a finding for
the maintainer to rule on (FR-031, FR-032), recorded and not fixed.

---

## Tiers

| Tier | Device / state | When |
|---|---|---|
| **N1** | One device or emulator, API 33+, debug build | Every stage that touches UI |
| **N2** | One device or emulator with **Don't keep activities** available | Every stage that moves state between owners |
| **N3** | One physical device with a launcher that can pin | Stage 4, and again at the final gate |
| **N4** | One device or emulator, **release** build | Final gate only |
| **N5** | The **baseline install**, updated in place — never uninstalled | Final gate, once |

**N5 is single-use.** It needs a device still carrying the pre-change build's data, which stops
being true the moment the post-change build is installed. Run it on the first in-place update and
carry the result forward.

Record device, launcher, OS version and theme with every result.

**"Process death"** below means: enable *Developer options → Don't keep activities*, leave the app
via Home, then return to it. Where that is unavailable, `adb shell am kill com.slowlock` while the
app is backgrounded is equivalent.

---

## N1 — Back goes where it went before

**Tier N1.** The back stack replaces `Origin`, and these are the cases `Origin` existed to decide.

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| N1.1 | Locks screen → New lock → pick an app → on the delay screen press **system back** | The **app list**, with the flow's first step still showing | | G1, FR-013 |
| N1.2 | Locks screen → tap an existing lock's row → on the delay screen press **system back** | The **Locks screen** — not the app list | | G1, FR-013 |
| N1.3 | Repeat N1.1 using the on-screen back control in the header instead of the gesture | Identical destination, identical result | | G7, FR-010 |
| N1.4 | Repeat N1.2 using the on-screen back control | Identical destination, identical result | | G7, FR-010 |
| N1.5 | From the delay screen tap **Next**, then press back on the icon screen | The delay screen, showing the delay **chosen on the way through** — not the value on disk | | G2, FR-013 |
| N1.6 | On the icon screen choose a treatment different from the current one, press back, then **Next** again | The icon screen opens on the app's **saved** treatment. The choice abandoned by the back press is gone | | G3, FR-018 |
| N1.7 | On the **Locks screen** (or the intro screen), press system back | The app closes — the press is not consumed | | G8, FR-013 |
| N1.8 | Complete a full flow to a created lock, then press back from the Locks screen | The app closes. Back does **not** re-enter the flow that was just finished | | G9 |
| N1.9 | Walk each of the three flow steps and read the step counter | `1 / 3`, `2 / 3`, `3 / 3` — unchanged from the baseline screenshots | | G12 |

---

## N2 — Destinations cross-fade

**Tier N1. ⚠️ Re-run required.** This section was rewritten on 2026-08-27 after the maintainer
directed that transitions be enabled. The earlier pass verified the opposite — that nothing animated
— so its result does not carry over to these cases.

**This is a fourth user-visible difference from the baseline, beyond FR-002's three, and it is
deliberate.** It is not a defect under SC-004.

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| N2.1 | Move forward through every step of the flow, watching the transition | Each screen **fades** in as the previous fades out. No slide, no scale, no shared-element movement | | G11 |
| N2.2 | Press back through every step, watching the transition | The same cross-fade, in the other direction — pop is not a different animation from push | | G11 |
| N2.3 | Watch the transition into and out of the app list specifically, with the list scrolled down | The fade does not reveal a re-laid-out or re-scrolled list mid-animation | | G11, G4 |
| N2.4 | If the device offers predictive back (gesture held mid-swipe), try it | No predictive-back preview appears — that is a separate opt-in this feature still does not take | | research R11 |
| N2.5 | Enable *Developer options → Animator duration scale = 10x* and repeat N2.1 | The fade is a plain opacity cross-fade throughout, with no flash of background between screens | | G11 |

---

## N3 — What a screen holds, and how long

**Tiers N1 and N2.** Rotation and process death on every destination.

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| N3.1 | On the **Locks** (or intro) screen, rotate the device | The same screen, the same list, the same scroll position | | G6 |
| N3.2 | On the **app list**, type a query and scroll, then rotate | Query and scroll position both kept | | G6 |
| N3.3 | On the **delay** screen, drag to a non-default value, then rotate | The dragged value is kept | | G6 |
| N3.4 | On the **icon** screen, choose a non-default treatment, then rotate | The chosen treatment is kept | | G6, state-scope §`ShortcutConfigViewModel` |
| N3.5 | Repeat N3.1 with **process death** instead of rotation | The Locks screen, list re-read | | G6 |
| N3.6 | Repeat N3.2 with **process death** | Back on the app list, with the **query still in the field** | | G6, S3 |
| N3.7 | Repeat N3.3 with **process death** | Back on the delay screen showing the value the user **dragged to** — not the value on disk, and not the default | | G6, S4, research R8 |
| N3.8 | Repeat N3.4 with **process death** | Back on the icon screen with the treatment the user **chose** — not the app's saved treatment, and not the route argument | | G6, S4, research R8 |
| N3.9 | On the app list, type a query and scroll down. Select an app, go to the icon screen, then press back twice to return to the list | Query and scroll position are **both still there** — the round trip retains them | | G4 |
| N3.10 | From the state N3.9 ends in, press back once more to leave the app list entirely, then open the app list again | The list opens **fresh**: no query, scrolled to the top. **This differs from the baseline and is approved** | | **G5, FR-002(a)** |
| N3.11 | Configure app A to the icon step and choose a non-default treatment. Back all the way out. Now configure app **B** | B's icon step opens on **B's** saved treatment. A's abandoned choice does not appear | | G3, S3, F-05 |

> N3.10 is the one deliberate change in this section. If any other row differs from the baseline,
> record it as a finding.

---

## N4 — The delay screen owns what it edits

**Tier N1.** FR-002(c) allows a withheld value; it does not allow a wrong one.

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| N4.1 | Tap an existing lock configured to a **non-default** delay (e.g. 30s) and watch the readout as the screen appears | It shows 30 — **never 10 first**. A blank instant before the number is approved; a default that corrects itself is a defect | | **FR-002(c)**, FR-019 |
| N4.2 | Repeat N4.1 entering from the **app list**, on an app already configured to a non-default delay | Identical: the saved value, never a default first | | **FR-002(c)**, FR-019 |
| N4.3 | Enter the delay screen for an app **never configured** | It opens on 10 seconds, as in the baseline | | FR-001 |
| N4.4 | On the delay screen, read the app pill above the readout | The target's icon and label, as in the baseline screenshot | | FR-001 |
| N4.5 | Drag the slider, tap each preset, and confirm the preset highlight | Identical to the baseline: any value that is not 5, 10 or 30 leaves all three unhighlighted | | FR-001 |

> If N4.1 or N4.2 shows a visible **flash** of a default, that is the pre-approved D5 fallback
> (research R8) — record it as a deviation and take the fallback (FR-042). A wrong value that never
> corrects itself is a defect, not a fallback.

---

## N5 — Creating a lock still creates a lock

**Tiers N1 and N3.** Requires a launcher that can pin.

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| N5.1 | Complete a flow for an app with no lock: accept the launcher's pin dialog | The icon lands on the home screen, and the app returns to the **Locks screen** with the flow popped | ✅ | G9 |
| N5.2 | Immediately after N5.1, look at the Locks list | The new lock is on it. **A single frame of the list without it is approved**; a list that stays wrong is a defect | ✅ | **FR-002(b)**, research R9 |
| N5.3 | Complete a flow for an app that **already** has a lock, changing its delay | The re-pin succeeds silently (no dialog, on most launchers) and the Locks list shows the **new** delay on return | ✅ | **FR-002(b)**, N8 |
| N5.4 | Decline the launcher's pin dialog in N5.1 | No lock appears; the app is on the Locks screen, unchanged | ✅ | FR-001 |
| N5.5 | Tap the newly pinned icon on the home screen | The wait screen appears and hands off to the target after the configured delay | ✅ | FR-004 |

> N5.2 is the only place FR-002(b) can be observed. Watch the first frame after the return.

---

## N6 — Pin support is still a gate, not a screen

**Tier N1.** The gate survives this feature as a `when` above the graph.

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| N6.1 | Switch the device to a launcher that cannot pin shortcuts, then bring SlowLock to the foreground | The unsupported screen takes over the whole app, exactly as in the baseline | ✅ | FR-012 |
| N6.2 | From the state N6.1 leaves, switch **back** to a pinning launcher and return to SlowLock | The user is returned **to where they were** — the back stack was not disturbed | ✅ | FR-012, contract "The pin-support gate" |
| N6.3 | Repeat N6.2 having been **mid-flow** (on the icon step) when support was lost | The icon step returns, with its choices | ✅ | FR-012 |
| N6.4 | Cold-launch the app and watch the first frame | No flash of the wrong screen before the launcher is asked | ✅ | FR-001 |
| N6.5 | On the unsupported screen, tap the recheck control with support still unavailable | The same screen; no navigation happens | ✅ | FR-001 |

---

## N7 — The app list still behaves

**Tier N1.** FR-002(a) changes when the list resets, not what it does.

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| N7.1 | Open the app list and compare against the baseline screenshot | Same apps, same order, same rows, same search field | | FR-001 |
| N7.2 | Type a query that matches nothing | The no-results message, unchanged | | FR-001 |
| N7.3 | Leave the app list to the delay step and come straight back | The list is still populated — no spinner flash on the return | | research R6 |
| N7.4 | Uninstall an app while SlowLock is backgrounded, then return to the app list | The uninstalled app is gone from the list | | FR-001 |

> N7.3 is where research R6's accepted redundant read would show itself. Rows staying up is the
> pass; a spinner is a finding.

---

## N8 — The release build

**Tier N4. Run on a release build, not a debug one.**

This tier exists because R8 can strip a generated route serializer, and the failure appears when a
user navigates rather than when the project builds.

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| N8.1 | Install a **release** build and run one complete create-a-lock flow end to end | Every step navigates; no crash, no blank screen | ✅ | research R13 |
| N8.2 | Tap a pinned icon through to the hand-off on that release build | The wait screen appears and the target launches | ✅ | research R13 |
| N8.3 | On the release build, rotate on the delay step and on the icon step | Both restore, with their arguments intact | ✅ | research R13, G6 |

> A route that resolves in debug and fails here is exactly R13's failure mode. If it fires, the
> keep rule goes in `app/src/main/keepRules/rules.keep`.

---

## N9 — In-place update loses nothing

**Tier N5. Run once, on the first update over the baseline install. Do not uninstall first.**

| # | Steps | Expected | Device | Requirement |
|---|---|---|---|---|
| N9.1 | Install the post-change build **over** the baseline install (`adb install -r`). Do not uninstall, do not clear data | The update succeeds and the app opens to the Locks screen | ✅ | SC-002 |
| N9.2 | Compare the Locks list against the baseline screenshot | The same locks, the same count, the same order | | SC-002, FR-003 |
| N9.3 | Open each lock's delay screen | Each shows the delay configured in the baseline | | SC-002, FR-003 |
| N9.4 | Check each lock's icon treatment on the icon step | Each shows the treatment configured in the baseline | | SC-002, FR-003 |
| N9.5 | Tap each pinned icon on the home screen | Each opens the wait screen and hands off to **its own** target | ✅ | SC-002, FR-004 |
| N9.6 | Count what the update asked of the user | Nothing: no re-pin, no re-configuration, no migration screen | ✅ | SC-002, FR-005 |

> A failure in N9.2 or N9.3 means a preferences file name or key changed. A failure in N9.5 means
> the launch activity's fully-qualified name changed. Both are frozen values — stop.

---

## N10 — Everything earlier features specified

**Tiers N1–N3.** Run the manual test plans of features **001–005 and 007** in full (SC-001).

**Exactly four cases may differ from their recorded expected result**, and each must be one of:

| Difference | Where it appears here | Approved as |
|---|---|---|
| The app list opens fresh after being left entirely | N3.10 | FR-002(a) |
| The lock list may be one frame behind on return | N5.2 | FR-002(b) |
| The delay screen may withhold its value while loading | N4.1, N4.2 | FR-002(c) |
| Destinations cross-fade instead of swapping instantly | N2 | Maintainer direction, 2026-08-27 |

The fourth row will show up in **every** earlier plan's navigation case, since those plans were
written against a build that animated nothing. Count it once, not once per case.

**Any fifth difference is a finding** (SC-004). Record it in `findings.md`, do not fix it, and
wait for a ruling.

---

## Coverage against FR-041

FR-041's minimum list, and where each is covered:

| FR-041 requires | Cases |
|---|---|
| Both routes out of the delay step | N1.1, N1.2 |
| Process death on each destination | N3.5, N3.6, N3.7, N3.8 |
| Round-trip retention, and the fresh re-entry of FR-002(a) | N3.9, N3.10 |
| The treatment discard and the treatment restore | N1.6, N3.11 (discard); N3.4, N3.8 (restore) |
| FR-002(b) | N5.2, N5.3 |
| FR-002(c) | N4.1, N4.2 |
| Back on the root screen leaves the app | N1.7 |
| Pin support lost and regained | N6.1, N6.2, N6.3 |
