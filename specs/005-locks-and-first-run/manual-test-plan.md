# Manual Test Plan: Locks Home & First Run

**Feature**: `005-locks-and-first-run` | **Task**: T045 | **Runs on**: a physical device or emulator

**The maintainer runs this.** No agent may drive the connected device to pre-verify a case
(Constitution, *Manual verification*). Nothing in this feature has instrumented coverage, and
nothing in it is going to: the JVM suite covers what is decidable off-device
(`LockListTest`, `LocksViewModelTest`), and everything below is what a JVM test cannot see.

## How to use this

Each case is numbered, states its setup, its steps, and the one thing that makes it pass, and names
the requirement it exists for. A case fails if *any* of its expectations misses — record which.

**Device cases** are marked 📱 and cannot be checked any other way. Cases marked 🔁 must be run
twice: once on the create path (entered from the app list) and once on the edit path (entered from
a lock row), because FR-029 requires both to behave identically.

**Before starting**: install the debug build, and for the fresh-install cases clear the app's data
(`Settings → Apps → SlowLock → Storage → Clear storage`, or `adb shell pm clear com.slowlock`).
Clearing storage removes the lock record but **not** the pinned home-screen icons — that asymmetry
is the subject of cases 6.4 and 7.1 and is not a bug.

---

## 1. First run (US1)

### 1.1 📱 The intro appears on a fresh install
**Setup**: app data cleared, no locks.
**Steps**: open SlowLock.
**Pass**: the intro screen appears — not the app list. It has no back tile and no step counter.
**Requirement**: FR-017, FR-019a, US1 scenario 1

### 1.2 The intro says what the app does, and what it does not
**Setup**: as 1.1.
**Steps**: read the body copy.
**Pass**: it states that SlowLock puts a wait in front of an app, **and** states that nothing is
blocked and nothing is counted. Both halves are present.
**Requirement**: FR-018, contract K1

### 1.3 The intro's one action opens the app list
**Steps**: tap "Set up a lock".
**Pass**: the app list opens, at step `1 / 3`. There is exactly one button on the intro.
**Requirement**: FR-019, US1 scenario 2

### 1.4 📱 The intro survives rotation
**Steps**: on the intro, rotate the device.
**Pass**: still the intro. No flash of the app list, no blank screen that stays blank.
**Requirement**: FR-008, US1 scenario 3

### 1.5 📱 The intro survives process death
**Steps**: on the intro, background the app; `adb shell am kill com.slowlock`; reopen from Recents.
**Pass**: still the intro.
**Requirement**: FR-008, US1 scenario 3

### 1.6 📱 System back on the intro exits the app
**Steps**: on the intro, press system back (or use the back gesture).
**Pass**: SlowLock exits to wherever you came from. It does not navigate anywhere inside the app.
**Requirement**: FR-031, US1 scenario 4

### 1.7 The whole 004 flow still works from the intro
**Steps**: from the intro, tap "Set up a lock", pick an app, set a delay, choose a treatment, tap
"Add to home screen", accept the launcher's dialog.
**Pass**: the flow behaves exactly as it did before this feature — same screens, same order, same
wording, plus the step counters. The icon lands on the home screen and waits when tapped.
**Requirement**: FR-042, US1 independent test

---

## 2. The Locks screen (US2)

### 2.1 📱 A returning user opens on their locks
**Setup**: at least one lock, created through the flow.
**Steps**: leave SlowLock entirely (Recents → swipe away), reopen it.
**Pass**: the Locks screen appears with no tap and no search. Not the intro, not the app list.
**Requirement**: FR-009, SC-002, US2 scenario 1

### 2.2 A row shows the app, the delay and the treatment
**Setup**: a lock at a distinctive delay (say 25s) with a distinctive treatment (say Inverted).
**Steps**: read the row.
**Pass**: the row shows that app's icon, its **current** label, `25 seconds` and `Inverted`. The
icon is the real one, not the grey placeholder, within a moment of the screen appearing.
**Requirement**: FR-010, FR-012, FR-015, US2 scenario 2

### 2.3 The count states the number and nothing else
**Setup**: exactly one lock, then exactly two.
**Steps**: read the line under the title in both states.
**Pass**: it reads `1 lock`, then `2 locks`. It does **not** say "on your home screen" or anything
equivalent — reconciliation makes the list converge on the icons, but 7.6 and 7.7 are both states
in which such a claim would be wrong.
**Requirement**: FR-011, Constitution I

### 2.4 "+ New lock" opens the app list
**Steps**: tap "+ New lock".
**Pass**: the app list opens at `1 / 3`. There is exactly one primary button on the Locks screen.
**Requirement**: FR-014, US2 scenario 3

### 2.5 A new lock appears **immediately**, with no hide/show
**Steps**: from Locks, run the flow for an app with **no** existing lock; finish it and tap **Add**
in the launcher's dialog.
**Pass**: as the dialog closes you are on Locks with the new lock **already** there, with the
values you just chose. You must **not** have to background and reopen the app to see it.
**Note**: the row correctly does **not** appear while the dialog is still open — until you tap Add,
nothing is pinned and so no lock exists (FR-003a).
**Requirement**: FR-016, FR-003a, N8, US2 scenario 5

### 2.5a 📱 The **first** lock replaces the intro
**Setup**: no locks at all — the intro is showing.
**Steps**: tap "Set up a lock", walk the flow, finish it, tap **Add** in the launcher's dialog.
**Pass**: as the dialog closes you are on the **Locks screen** with your one lock on it.
**Note**: the intro is what is behind the dialog while it is open, and that is correct — no lock
exists yet. What must not happen is the intro *staying* after you tap Add.
**Requirement**: FR-016, FR-017, FR-003a, N8

### 2.5b 📱 **Cancelling the pin dialog creates no lock**
**Setup**: note how many locks you have.
**Steps**: run the flow to the end, tap "Add to home screen", then tap **Cancel** in the launcher's
dialog.
**Pass**: **no new lock appears** — the list is exactly as it was, and stays that way after
backgrounding and reopening the app. No icon was created and no row was either.
**Requirement**: FR-003a, FR-011a

### 2.6 An existing lock updates in place — one row, not two
**Steps**: run the flow again for an app that **already** has a lock, choosing a different delay
and a different treatment; finish it.
**Pass**: there is still exactly **one** row for that app, in its **original position** in the
list, showing the **new** delay and treatment.
**Requirement**: FR-013, FR-016, US2 scenario 4, US4 scenario 5

### 2.7 📱 Locks survive a force-stop and a reboot
**Steps**: with two locks, force-stop SlowLock (`Settings → Apps → SlowLock → Force stop`), reopen
it. Then restart the device and open it again.
**Pass**: both locks are still listed, in the same order, with the same values, both times.
**Requirement**: FR-004, SC-005

### 2.8 📱 Rotation and process death on the Locks screen
**Steps**: on Locks, rotate. Then background, `adb shell am kill com.slowlock`, reopen from
Recents.
**Pass**: the Locks screen both times, populated. It does not fall back to the intro or the app
list, and it does not sit blank.
**Requirement**: FR-008, FR-016, N9

### 2.9 📱 Order is creation order and does not shuffle
**Setup**: create three locks, noting the order you made them in.
**Steps**: leave and reopen the app three times.
**Pass**: the rows are in the order you created them, identically, every time.
**Requirement**: FR-006, SC-004

---

## 3. Locks whose app is gone (US2 / FR-020)

### 3.1 📱 An uninstalled app's lock is shown, not hidden
**Setup**: create a lock for an app you are willing to uninstall (a sideloaded test APK is ideal).
**Steps**: uninstall that app. Reopen SlowLock.
**Pass**: the row is **still there**. It names the missing app by its **package name**, says it is
no longer installed, and says the lock stays until you remove it. The icon slot shows the grey
placeholder.
**Requirement**: FR-020, contract K3, US2 scenario 7

### 3.2 📱 The unavailable row is not tappable and does not crash
**Steps**: tap the unavailable row. Then scroll the list up and down past it several times.
**Pass**: the tap does nothing at all — no navigation, no flicker, no press ripple. The screen does
not crash, and the other rows behave normally.
**Requirement**: FR-020, contract K3

### 3.3 📱 The unavailable row carries a visible "How to remove" control
**Steps**: look at the unavailable row.
**Pass**: a visible "How to remove" control is present on it, and tapping it opens the explanation
(case 6.1's dialog). This row has no long press, which is why the control is drawn.
**Requirement**: FR-041, contract K3, research R6

### 3.4 📱 A reinstalled app comes back on its own
**Steps**: reinstall the app from 3.1. Reopen SlowLock.
**Pass**: the row is a normal, tappable lock again, with its icon, its label and the delay and
treatment it had. Nothing needed migrating.
**Requirement**: FR-005, SC-006

### 3.5 📱 A renamed app, or a language change, shows the current label
**Steps**: change the device language to one the locked app is translated into (or update an app
that has been renamed). Reopen SlowLock.
**Pass**: the row shows the app's **current** label, not the one it had when the lock was made.
**Requirement**: FR-012, SC-006, Constitution V

---

## 4. The wizard (US3)

### 4.1 The app list has a back control that returns to the root
**Steps**: from Locks, tap "+ New lock", then the back tile on the app list.
**Pass**: the Locks screen. Not the intro, and not an exit.
**Requirement**: FR-028, US3 scenario 1

### 4.2 The step counters read 1 / 3, 2 / 3, 3 / 3 🔁
**Steps**: walk all three steps, on the **create** path (from "+ New lock") and again on the
**edit** path (from tapping a lock row — which starts at step 2).
**Pass**: the app list reads `1 / 3`, the delay screen `2 / 3`, the icon screen `3 / 3`. The
numbers are the same on both paths.
**Requirement**: FR-029, US3 scenarios 2–4

### 4.3 📱 System back matches the on-screen control on every step 🔁
**Steps**: on each of the three steps, in turn, note where the on-screen back tile goes, then
repeat using the system back gesture instead.
**Pass**: identical destination and identical state each time. In particular, back from the icon
screen returns to the delay screen showing **the delay you chose on the way through**, not the one
on disk.
**Requirement**: FR-030, 003 FR-014, US3 scenario 5

### 4.4 📱 The app list keeps its scroll position and query across the round trip
**Steps**: from Locks, open the app list, type a query and scroll well down it. Press back to
Locks. Tap "+ New lock" again.
**Pass**: the same query and the same scroll position. You do not have to scroll or type again.
**Requirement**: FR-011 (003), N3, N4

### 4.5 📱 Rotation and process death mid-flow 🔁
**Steps**: on the delay screen with a non-default value set, rotate. Then background,
`adb shell am kill com.slowlock`, reopen from Recents. Repeat on the icon screen.
**Pass**: you return to the step you were on, with the value you had chosen — and a back press
from there still goes where the path you entered by says it should (case 5 below).
**Requirement**: FR-008, N9

---

## 5. Editing a lock (US4)

### 5.1 Tapping a lock opens the delay step on its saved values
**Setup**: a lock at 25s / Inverted.
**Steps**: from Locks, tap that row.
**Pass**: the delay screen opens showing **25 seconds** and reading `2 / 3`. There is no frame in
which it shows the default and then corrects itself.
**Requirement**: FR-023, N6, US4 scenario 1

### 5.2 Continuing carries the saved treatment
**Steps**: from 5.1, tap through to the icon step.
**Pass**: **Inverted** is the selected treatment on arrival, not Original.
**Requirement**: FR-013 (003), US4 scenario 2

### 5.3 📱 Back from an edit returns to Locks, not the app list
**Steps**: from 5.1, press back (both the tile and the system gesture).
**Pass**: the **Locks** screen. The app list is never shown — you never went through it.
**Requirement**: FR-023, US4 scenario 3

### 5.4 📱 Back from a *creation* still returns to the app list
**Steps**: from Locks, tap "+ New lock", pick an app, then press back on the delay screen.
**Pass**: the **app list**, still scrolled and still filtered as you left it. This is the same
control as 5.3 behaving differently because the flow was entered differently — check both in one
sitting.
**Requirement**: FR-023, N3

### 5.5 An abandoned edit writes nothing
**Setup**: a lock at 25s / Inverted.
**Steps**: tap it, change the delay to 60s, change the treatment, then press back out of the flow
entirely without tapping "Add to home screen". Return to Locks.
**Pass**: the row still reads **25 seconds / Inverted**. Nothing was written. Leave the app and
reopen it to confirm.
**Requirement**: FR-023a, N7, US4 scenario 4

### 5.6 A finished edit updates the row in place
**Steps**: as 5.5 but finish the flow, accepting the launcher's dialog.
**Pass**: one row for that app, in its original position, reading the new values. See also 2.6.
**Requirement**: FR-016, US4 scenario 5

### 5.7 📱 Editing takes at most three taps and one drag from launch
**Steps**: from a cold launch, count the interactions needed to change an existing lock's delay:
tap the row, drag the slider, tap "Choose the icon", tap "Add to home screen".
**Pass**: no more than three taps and one drag before the launcher's own dialog.
**Requirement**: SC-003

---

## 6. Removing a lock (US5)

**SlowLock cannot remove a lock.** A lock is its pinned shortcut, Android offers no way to unpin
one, and the only real removal is the user taking the icon off their home screen. This section
checks that the app says so clearly and never pretends otherwise.

### 6.1 📱 A long press opens the explanation
**Setup**: two locks.
**Steps**: long-press one row.
**Pass**: one dialog appears, explaining how to remove a lock. Long-pressing the other row while it
is open replaces it rather than stacking a second one.
**Requirement**: FR-021, US5 scenario 1

### 6.2 **The explanation's wording** — the deliverable
**Steps**: read the dialog word for word.
**Pass**: all five hold.
- It **names the app**.
- It says that **removing the icon from the home screen** is what removes the lock.
- It says the **user** does that themselves.
- It does **not** imply SlowLock can remove the icon, or that anything happens when the dialog
  closes.
- It does **not** suggest the app is uninstalled.

**This case is the deliverable of US5, not a formality.** If the wording drifts on any of the five,
the case fails.
**Requirement**: FR-022, SC-012, contract K4, Constitution I

### 6.3 The dialog has exactly one button, and it only closes the dialog
**Steps**: read the buttons. Tap **OK**. Then reopen it and dismiss by tapping outside, and again
with system back.
**Pass**: there is exactly **one** button, reading "OK" — no "Remove", no "Delete", nothing
destructive. All three dismissals close the dialog and **change nothing**: the lock is still there
with the same values, and its icon is still on the home screen.
**Requirement**: FR-021, contract K4, US5 scenario 3

### 6.4 📱 Following the instructions actually works
**Setup**: a lock whose icon is on the home screen.
**Steps**: read the dialog, close it, then do what it says — remove that icon from the home screen.
Reopen SlowLock.
**Pass**: the lock is **gone** from the list. The dialog told the truth.
**Note**: if it does not, check 7.7 before filing — some launchers never unpin, and the app cannot
work around that.
**Requirement**: FR-003a, FR-022, SC-012

### 6.5 📱 The app itself was not uninstalled
**Steps**: after 6.4, open your app drawer and find the app that lock was for.
**Pass**: it is still installed and still works. Removing a lock removes a shortcut, nothing more —
which is the dialog's last sentence.
**Requirement**: FR-022

### 6.6 Removing the last lock returns to the intro
**Steps**: remove home-screen icons until no locks remain. Reopen SlowLock.
**Pass**: the intro screen appears — the same one a fresh install gets.
**Requirement**: FR-017, US5 scenario 5, US2 scenario 6

### 6.7 📱 The dialog is not expected to survive process death
**Steps**: open the explanation, then `adb shell am kill com.slowlock` and reopen from Recents.
**Pass**: the dialog is **gone** and the lock is **still there**. Intended behaviour, recorded here
so it is not filed as a bug: `explainingRemoval` is deliberately transient, and nothing was pending
anyway.
**Requirement**: data-model.md §4

---

## 7. Upgrades and the rest of the app

### 7.1 📱 A user upgrading from a build without this feature sees the intro
**Setup**: install the **previous** build (the `004` branch), create two locks so their icons are
on the home screen, then install this build over the top **without clearing data**.
**Steps**: open SlowLock.
**Pass**: the **Locks screen**, listing both locks with the delays and treatments you set on the
old build. They were never lost: the shortcuts are still pinned, and pinned is what a lock means
(FR-003a). Then tap the old home-screen icons — they **still wait and still open their apps**.
**Note**: this reverses what the original spec required (the intro). A user with **no** pinned
icons still sees the intro.
**Requirement**: FR-024, FR-003a

### 7.2 📱 Dismissing the dialog, rather than cancelling it, also creates no lock
**Steps**: run the flow to the end, tap "Add to home screen", then dismiss the launcher's dialog by
tapping outside it or pressing back — not with the Cancel button.
**Pass**: no lock appears, same as 2.5b. Nothing was pinned, so nothing was derived.
**Requirement**: FR-003a, FR-011a

### 7.3 📱 **Dragging the icon off the home screen removes the lock**
**Setup**: a lock whose icon is on the home screen, and which you have opened SlowLock at least
once since creating (that first visit is what records the observation FR-004a's guard needs).
**Steps**: drag that lock's icon off the home screen / remove it. Reopen SlowLock.
**Pass**: the lock is **gone** from the list. Any other lock is untouched.
**If it does not**: check 7.6 before filing it — some launchers never unpin, and that is a
launcher behaviour this app cannot work around.
**Requirement**: FR-004a, spec edge case

### 7.6 📱 A lock created and its icon removed before reopening is gone
**Setup**: create a lock, accept the launcher's dialog, then **immediately** remove its icon from
the home screen without opening SlowLock in between.
**Steps**: open SlowLock.
**Pass**: the lock is **not** listed. There is no observation window to wait out — the shortcut is
not pinned, so there is no lock.
**Requirement**: FR-003a

### 7.7 📱 A launcher that does not unpin simply keeps the lock
**Setup**: a launcher known not to unpin on icon removal (several OEM launchers do not).
**Steps**: remove a lock's icon, reopen SlowLock.
**Pass**: the lock is still listed, with its values intact, and removing it by hand still works.
**This is a pass, not a failure** — the shortcut still reports as pinned, and "pinned" is the
definition. It is why FR-011 still forbids the count claiming the icons are there.
**Requirement**: FR-003a, FR-011

### 7.8 📱 A walked-but-abandoned flow leaves no lock
**Setup**: walk the flow for an app as far as the icon step, then back out **without** tapping
"Add to home screen". The configuration store now holds a delay for that app.
**Steps**: open SlowLock.
**Pass**: **no lock** for that app. A configuration is not a lock — only a pinned shortcut is, and
nothing was pinned (FR-024a).
**Requirement**: FR-024a, L7

### 7.4 📱 The unsupported-launcher screen takes over ahead of both new screens
**Setup**: locks exist. Switch to a launcher that does not support pinning (or use a device/emulator
whose default launcher does not).
**Steps**: open SlowLock.
**Pass**: the unsupported-launcher screen renders **in place of everything** — not the Locks
screen, not the intro, and not either of them behind it. Switch back to a supporting launcher,
return to the app, and Locks appears again without a restart.
**Requirement**: FR-025, N1, 002 FR-029, 003 FR-004

### 7.5 📱 Zero change to any persisted delay or treatment value
**Steps**: on the **previous** build, create a lock at 45s / Grayscale. Install this build over it
without clearing data, and edit that lock.
**Pass**: the delay screen opens on **45 seconds** and the icon step on **Grayscale**. The stored
values were read, not migrated and not defaulted.
**Requirement**: FR-008, SC-014

---

## 8. Visual and accessibility conformance

### 8.1 📱 Both new screens match their artboards
**Setup**: a device or emulator at **412 × 892 dp**, default font scale, default display size.
**Steps**: compare the intro and the Locks screen against the "New · First run" and "New · Locks"
artboards.
**Pass**: visually indistinguishable — spacing, type sizes, the 18dp row cards, the 44dp icons, the
hairline borders.
**Note**: the two screens' metrics are **derived** from 004's frozen tokens rather than measured
off the artboards (research R8). **This case is what settles them.** A correction goes into
`contracts/locks-screen.md`, never into `004/contracts/design-tokens.md`.
**Requirement**: SC-007, contract K1, K2

### 8.2 📱 Largest font scale on the smallest supported screen
**Setup**: the smallest supported screen, `Settings → Display → Font size` at maximum, and display
size at maximum too.
**Steps**: view the intro and the Locks screen, including a row for an app with a very long label,
and open the removal explanation.
**Pass**: everything remains usable. Rows **grow** rather than clip; a long label truncates with an
ellipsis while **the delay and the treatment stay legible**; the primary action is reachable; no
text is cut off mid-glyph.
**Requirement**: SC-008, spec edge case

### 8.2a 📱 **No screen draws under the system bars**
**Setup**: a device with a visible status bar and a gesture bar (or a notch/cutout, which is
sharper still).
**Steps**: visit **all five** screens in turn — the intro, the Locks screen, the app list, the
delay step, the icon step. Look at the top of each and then the bottom.
**Pass**: on every one, the header — back tile, title and step counter — sits **fully below** the
status bar and clock, and the bottom action clears the gesture bar. Nothing is overlapped, dimmed
by the bar's scrim, or clipped.
**Note**: the delay step is the one that failed this. It was the only screen built on a bare
`Column` instead of a `Scaffold`, so it alone had nothing applying the system-bar inset — steps 1
and 3 looked correct on either side of it. Contract N11 now binds all five.
**Requirement**: N11, SC-007, SC-008

### 8.3 📱 Both new screens render light regardless of the system setting
**Steps**: turn on system dark mode. Open the intro and the Locks screen.
**Pass**: both render in the light palette, unchanged. Dark mode is Phase 3 and `SlowLockTheme`
takes no `darkTheme` parameter, so this is a structural certainty — check it anyway.
**Requirement**: FR-037

### 8.4 📱 TalkBack reads every row completely
**Setup**: TalkBack on.
**Steps**: swipe through the Locks screen.
**Pass**: each row announces the app's **label**, its **delay** and its **treatment**. The count
line and the title are announced. Nothing is announced as an unlabelled button.
**Requirement**: SC-011

### 8.5 📱 TalkBack hears each step counter
**Steps**: with TalkBack on, walk all three steps of the flow.
**Pass**: the counter is announced **with the title** — "Choose an app, step 1 of 3" or equivalent
— on each step. It is never announced as a control, and never left silent as decoration.
**Requirement**: FR-029, contract K5, T026

### 8.6 📱 **The removal explanation is reachable without a long press**
**Steps**: with TalkBack on, focus a lock row and open TalkBack's **actions** menu (swipe up-then-
right, or the local context menu on older versions). Do the same on an **unavailable** row.
**Pass**: a "How to remove this lock" action is offered on **both**, and choosing it opens the
explanation. At no point is a long press required.
**Requirement**: FR-041, SC-011, research R6

---

## Coverage

| Area | Cases |
|---|---|
| US1 — first run | 1.1–1.7 |
| US2 — the Locks screen | 2.1–2.9 (incl. 2.5a, 2.5b), 3.1–3.5 |
| US3 — the wizard | 4.1–4.5 |
| US4 — editing | 5.1–5.7 |
| US5 — removal | 6.1–6.7 |
| Upgrades, pinning, launcher support | 7.1–7.8 |
| Visual and accessibility conformance | 8.1–8.6 (incl. 8.2a) |

**49 cases.** The ones that cannot be inferred from any other and would be quietest if broken:
**2.5b** (cancelling creates no lock), **6.2** (the explanation's wording), **6.4** (following the
instructions actually works), **7.1** (the upgrade path), **7.3** (removing an icon removes the
lock), and **8.1** (the derived metrics against the artboards).

**2.5b is the case this design exists for**: cancelling the launcher's dialog must create no lock.
**2.5 and 2.5a** catch the refresh half — the list updates when the dialog closes, without the user
backgrounding the app.

**6.2 and 6.4 are a pair**: 6.2 checks the app tells the truth about how removal works, and 6.4
checks that what it says is actually true. A change to either the copy or `deriveLocks` needs both
re-run.
