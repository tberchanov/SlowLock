# Phase 0 Research: Launch Delay

**Feature**: `003-launch-delay` | **Date**: 2026-08-23

Fourteen decisions. Three of them are the feature: where a delay is stored (R1), what makes a
wait end (R4, R5), and what keeps the screen from being interesting (R7). Two carried a cost that
had to be argued rather than assumed: persistence at all (R1) and keeping the display awake (R6) —
the second of which the maintainer settled by amending the constitution rather than by tolerating
a deviation. R12 was reversed outright: this feature ships no instrumented tests.

---

## R1 — Where per-app configuration lives

**Decision**: One `SharedPreferences` file, read and written only on `Dispatchers.IO`, behind a
single `DelayConfigStore` class with `suspend` functions. No new dependency.

**Rationale**: FR-017 requires the configuration to survive force-stop, reboot, and app update,
and FR-016 makes the package name its only key. That is a map of a dozen small entries, read
once per screen and once per shortcut tap, written once per apply. `SharedPreferences` is the
platform's built-in answer to exactly that shape: no dependency, no schema, no migration story,
atomic per-`Editor` commit, backed up by the platform, and available from a cold-started
activity without a framework being initialised first.

Its two real drawbacks do not bite here. The first access loads and parses the whole file on the
calling thread — which is why every access in this feature is a `suspend` function on
`Dispatchers.IO` (Constitution IV, FR-036), never a field read during composition. And it has no
change notification worth the name; nothing in this feature observes the store, because both
readers read once at a well-defined moment.

**Alternatives considered**:

| Option | Rejected because |
|---|---|
| **Preferences `DataStore`** | A new dependency (`androidx.datastore:datastore-preferences`) for a flow-based API over the same data, when nothing here observes a flow. Constitution II makes the default answer no, and this is not the case that overturns it |
| **Room, or any database** | A persistence engine, a code generator, and a schema-migration obligation for one integer and one enum per app |
| **A JSON or properties file written by hand** | Re-implements `SharedPreferences` badly: locking, atomic replace, and corruption recovery all become ours |
| **Carrying the delay in the pinned shortcut's intent extras** | The tempting one, and wrong. It would need no store at all — but `updateShortcuts` is the only way to rewrite a pinned shortcut's intent, it fails silently when rate-limited, it reaches nothing if the user has removed the icon, and it says nothing about apps configured before an icon was pinned. FR-018 ("in force on the very next tap, nothing asked of the user") would then depend on a call that can quietly not happen. Reading the delay at tap time makes FR-018 structural. It also keeps `contracts/pinned-shortcut.md` frozen: the persisted payload stays the package name and nothing else |
| **Holding the map in memory in the application object** | The wait runs in a cold-started activity after a reboot. There is no process to hold anything (FR-033) |

---

## R2 — The stored shape, and which parts of it are frozen

**Decision**: File `slowlock.delay-config`, two keys per configured app:

```text
"<packageName>.delaySeconds"  → Int
"<packageName>.treatment"     → String, the IconTreatment constant's name
```

The file name, the two key suffixes, and the treatment tokens (`Original`, `Invert`, `Gray`) are
**frozen** and recorded in `contracts/delay-config-store.md`. Reads sanitise: a missing or
non-positive delay reads as the default, and an unrecognised treatment token reads as
`Original`.

**Rationale**: This is the project's second body of persisted state, and it fails the same way
the first one does — silently, on a user's device, after an update they did not ask for. A
renamed enum constant is the exact hazard `ShortcutContract` was written to guard: `IconTreatment.Invert`
→ `IconTreatment.Inverted` compiles clean, and every configured app quietly reverts to `Original`.
A JVM test asserts the tokens against frozen literals, so that lands in `./gradlew test`.

Two keys rather than one encoded string because `Editor` commits are atomic across keys, so
there is no half-written record to parse, and because adding a third field later costs a key
rather than a format version.

Not clamping on read is what keeps the range a property of the screen rather than of the data:
the store accepts any positive integer, only the slider enforces 1–30 (R11), and a later change
to the range therefore cannot silently rewrite a value the user chose. **This has already paid
for itself**: the range was 5–120 when the store was written and narrowed during implementation,
and no stored value needed migrating.

**Alternatives considered**: one key per app holding `"30|Invert"` (a format to parse, version,
and get wrong); storing the treatment's ordinal (reordering the enum silently rewrites every
saved icon — strictly worse than the name); a `Set<String>` of configured packages alongside
(bookkeeping that goes stale, and `contains` is answerable from the keys themselves).

---

## R3 — Reading the configuration without a visible gap

**Decision**: Two different answers for the two readers.

- **The delay screen**: the root loads the configuration *before* it navigates. Tapping a row
  launches a coroutine that reads the store on `Dispatchers.IO` and only then moves to the delay
  stage, so the screen's first composition already has the saved value.
- **The wait screen**: renders immediately with no configuration at all, and the read happens
  underneath it. The screen shows the same fixed text for every app and every delay (FR-025), so
  it has nothing to wait for.

**Rationale**: FR-012 says the delay screen opens with the saved delay — not with the default
that flips to the saved value a frame later, which is how a user learns not to trust the number.
Loading before navigating costs a few milliseconds of a `SharedPreferences` first load and
guarantees it. FR-022 says the wait screen appears promptly; making it wait on a disk read to
show text that does not depend on the disk read would be the opposite.

**Alternatives considered**: a loading state inside the delay screen (a spinner or a layout shift
for a read measured in milliseconds); preloading every app's configuration into memory when the
list opens (a cache to invalidate, for a read that is already fast); blocking reads on the main
thread (forbidden by Constitution IV, and the reason the store is `suspend`-only).

---

## R4 — Timing the wait

**Decision**: An `elapsedRealtime()` **deadline**, a single `kotlinx.coroutines.delay` in
`lifecycleScope`, and the deadline carried through `onSaveInstanceState`.

```text
onCreate:  deadline = savedInstanceState?.deadline ?: (elapsedRealtime() + delayMs)
           lifecycleScope.launch { delay(deadline - elapsedRealtime()); handOff() }
```

**Rationale**: One suspension point, no ticking, nothing recomputed per frame — which is what a
screen with nothing to update should cost. `lifecycleScope` cancels the coroutine when the
activity is destroyed, so every abandonment path (R5) unwinds without bookkeeping.

The deadline is anchored in `onCreate`, before the configuration read, so the disk read cannot
extend the wait; and because the anchor is later than the user's tap, the observed wait is always
at least the configured delay (FR-037).

`elapsedRealtime()` rather than `System.currentTimeMillis()` because it is monotonic and unaffected
by clock changes, and rather than `uptimeMillis()` because it keeps counting through deep sleep —
which matters only for correctness of the arithmetic, since a sleeping device has already
abandoned the wait (R5).

Saving the deadline is what makes FR-027 true: a rotation destroys and recreates the activity,
and a recreated activity that recomputed `now + delay` would silently restart the wait.

**Alternatives considered**: `Handler.postDelayed` (equivalent, but needs its own cancellation in
`onDestroy` and does not compose with the lifecycle); `CountDownTimer` (a ticking API for a screen
whose entire point is that nothing ticks); `AlarmManager` or `WorkManager` (they exist to fire
when the app is not in front of the user — which is precisely the case where the launch must not
happen, and a background activity start is forbidden by Constitution IV); a Compose
`LaunchedEffect` inside the wait composable (dies and restarts with composition, and puts the
timing where recomposition can reach it).

---

## R5 — What ends a wait

**Decision**: The wait is bound to the activity being **visible**, expressed as three concrete
mechanisms:

1. `onStop()` finishes the activity — **unless** `isChangingConfigurations` is true.
2. `android:noHistory="true"` is **removed** from the manifest entry.
3. `android:launchMode="singleTop"`, with `onNewIntent` ignoring a repeat of the same target.

Before starting the target, the hand-off re-checks that the lifecycle is still at least `STARTED`.

**Rationale**: FR-029 names one rule — the wait dies when its screen stops being visible — and
`onStop` is exactly that moment for every case the requirement lists: home, back, the app
switcher, another app taking over, the display timing out, and the device locking. Finishing
there rather than merely cancelling means there is nothing to return to, which is also FR-031.

`isChangingConfigurations` is the one exception, and it is the difference between FR-029 and
FR-027: a rotation passes through `onStop` too, and finishing there would restart the wait on
every rotation — the bug the requirement was written against.

`noHistory` has to go for the same reason. It would deliver FR-029 for free, but it is the
system deciding when to finish us, with no exception for a configuration change — so a rotation
would restart the wait, and there would be nothing in the class to read or reason about. Explicit
`onStop` logic is one line, is visible where a reviewer looks for it, and is what the six
abandonment cases in the manual plan exercise (M5.4–M5.9). The attribute is not part
of the frozen contract — `contracts/pinned-shortcut.md` lists the activity's manifest attributes
as explicitly not frozen.

`singleTop` covers FR-027's second clause: a second tap of the same shortcut is delivered to
`onNewIntent` instead of stacking a second instance, and is ignored, so it neither restarts nor
extends the running wait. A new intent naming a *different* package restarts the wait for the new
target, which is the spec's "user taps a different app's shortcut" edge case. Both are close to
unreachable in practice — the wait screen is full-screen, so reaching another icon means going
home first, which abandons it — and both are one `if` each.

The `STARTED` re-check before `startActivity` closes the race where the coroutine is already
resuming as `onStop` runs: the delay expires, the continuation is queued, and the user presses
home before it runs. Without the check, the target would launch from a stopping activity, which
is both wrong and the kind of background start Constitution IV forbids.

**Alternatives considered**: cancelling on `ON_PAUSE` (too eager — the notification shade, a
permission dialog, or a partly-obscuring window pauses without hiding the screen, and the user
has not left); keeping the activity alive on `onStop` and resuming the remainder on return (the
"pocket the phone" bypass the clarification explicitly closed); `android:configChanges` to swallow
rotation instead of saving the deadline (fights the platform to avoid saving one long).

---

## R6 — Keeping the display awake during the wait

**Decision**: `FLAG_KEEP_SCREEN_ON` on the wait window, held only while that window is visible.

**Constitutional standing**: this was taken as a deviation from v1.0.0's unqualified "no wake
locks in v1", justified in the plan's Complexity Tracking. `/speckit-analyze` judged that
reinterpretation rather than compliance, and the maintainer amended the constitution instead:
**v1.1.0 permits a window-scoped display lock** on a screen the user is actively looking at, for a
feature that does not work without it, released with the window. All three conditions hold here,
so this is now compliant and the Complexity Tracking row is gone. `PowerManager` wake locks stay
forbidden, and the argument below is what the allowance was granted for.

**Rationale**: R5 makes the display timing out abandon the wait. Without this flag, any delay
longer than the device's screen timeout is unreachable: the user taps the icon, stares at a
deliberately motionless screen, the display sleeps at 15 seconds, and the wait is gone. On a
30-second delay — the longest the slider offers — the target app can never open at all on that
phone, no matter how patiently the user waits, and the screen by design gives them no hint that
touching it would help. That is not friction; it is a broken feature. Narrowing the maximum from
120 s to 30 s shortens the exposure but does not remove it: plenty of devices ship with a 15- or
30-second timeout, which is under or level with the longest delay.

The flag is the narrowest fix available: it is a window flag, not a `PowerManager` wake lock; it
is scoped to a window the user is actively looking at; it is released by the system when that
window goes away, including on every abandonment path; and it cannot outlive the wait. The
constitution's stated intent for that rule is battery drain while the app is not in front of the
user ("Battery cost at rest MUST be zero"), and this changes nothing at rest.

**Alternatives considered**:

| Option | Rejected because |
|---|---|
| **No flag; cap the maximum delay below the shortest plausible screen timeout** | The shortest timeout Android offers is 15 seconds. A product whose longest delay is 15 seconds is a different product |
| **No flag; tell the user to tap the screen to keep it awake** | Touch does keep the display alive, and FR-026 already makes tapping inert, so the mechanism works — but instructing the user to keep touching the phone is the opposite of "put it down and think", and it is more text on a screen that should have almost none |
| **A `PowerManager` `SCREEN_BRIGHT_WAKE_LOCK`** | Deprecated, needs the `WAKE_LOCK` permission (Constitution III), and can outlive the activity if released wrongly. Strictly worse in every dimension |
| **Dim the screen instead of keeping it fully lit** | Extra state, extra API surface, and a screen that changes brightness mid-wait is a screen that changes (FR-023) |

---

## R7 — Making the screen boring, and keeping it that way

**Decision**: A flat colour resource painted by both the window and the composable, a single
centred line of text, no `MaterialTheme`, no dynamic colour, no animation API anywhere in the
file, and the activity's `windowBackground` set to the *same* colour resource so the system's
starting window is indistinguishable from the composed screen.

**Rationale**: "Static" is a property that decays. Every default in the stack pushes the other
way — Material's dynamic colour would tint the screen from the wallpaper, a themed surface would
animate its colour on a theme change, and any progress affordance a future contributor adds would
look like an improvement. Painting a literal colour resource with no theme in scope makes the
screen's appearance a two-line fact rather than a chain of defaults, and gives the contract in
`contracts/wait-screen.md` something to assert.

Matching `windowBackground` to that colour removes the one visible change the user would otherwise
see: without it, the system paints the starting window in the app theme's background and the
screen changes colour a frame later — a flash, on the screen that must not move. With it, the tap
lands on the final background immediately (FR-022) and the only subsequent change is the text
appearing, which is the screen arriving rather than the screen changing.

`android:windowDisablePreview` is removed for the same reason: the wait activity now *wants* a
starting window, where the invisible launcher activity of feature 002 wanted none.

Deliberately absent: the target app's name and icon (spec, Assumptions — naming the app the user
is craving re-triggers the craving, and an identical screen every time is less interesting than
one that varies), any progress or time display (FR-023), sound, vibration, and notifications
(FR-024), and any clickable element (FR-026).

**Alternatives considered**: reusing `SlowLockTheme` (dynamic colour makes the screen vary per
device and per wallpaper, and cannot match a static `windowBackground`); a system-bar colour
scheme (`targetSdk 37` enforces edge-to-edge and ignores the attributes — a full-bleed flat
background is both simpler and more uniform); an XML layout instead of Compose (Constitution IV
forbids XML layouts for new screens, and the saving would be nil).

---

## R8 — Leaving nothing behind

**Decision**: Keep feature 002's `android:excludeFromRecents="true"` and `android:taskAffinity=""`,
start the target with `FLAG_ACTIVITY_NEW_TASK`, and call `finish()` immediately after. Do not kill
the process.

**Rationale**: FR-031 is feature 002's FR-019 applied to a screen that is now visible, and the
manifest attributes that satisfied it there are unchanged by making the activity draw. The empty
task affinity puts the wait in its own task, `excludeFromRecents` keeps that task out of the
switcher, `NEW_TASK` gives the target its own task so backing out of it does not unwind into
SlowLock, and `finish()` ends ours.

The user's phrasing — "the SlowLock application process is finished" — is satisfied by there being
no SlowLock task in recents and no SlowLock UI in front of the user. Actually killing the process
(`Process.killProcess`, `exitProcess`) is rejected: it is the platform anti-pattern, it would abort
the `SharedPreferences` write queue mid-flight, and the empty process the system keeps around costs
nothing and is reclaimed on demand. Constitution IV — work with the platform, never around it.

Unaffected on purpose: `MainActivity` keeps its normal affinity and its own recents entry. That is
SlowLock's own app, which the user opened deliberately; hiding it would be a different (and worse)
feature.

---

## R9 — Three stages instead of two

**Decision**: `SlowLockRoot` keeps its `when` over root state and grows a stage:

```kotlin
sealed interface Stage {
    data object List : Stage
    data class Delay(val packageName: String, val seconds: Int, val treatment: IconTreatment) : Stage
    data class Shortcut(val packageName: String, val seconds: Int, val treatment: IconTreatment) : Stage
}
```

The chosen delay and the treatment are **hoisted into the stage**, not owned by the screens.
`SaveableStateHolder` keys stay one per branch, with the list's entry retained and the other two
dropped on the way out.

**Rationale**: FR-014 is the whole reason for hoisting. Back from the shortcut screen must return
to the delay screen showing *the delay the user chose on the way through*, not the saved one and
not the default — so the value has to live above both screens. Once it is hoisted, the delay
screen has no state of its own worth saving and the round trip is a state transition rather than a
restoration problem.

Still no navigation library (feature 002's R9 stands, and the reasoning gets stronger with a
linear three-stage flow): the graph is a line, the arguments are two primitives and an enum, and
`rememberSaveable` over a `Parcelize`-free sealed hierarchy is a few lines. `Stage` needs a saver,
which is one `listSaver` — cheaper than the dependency.

**Alternatives considered**: a root `ViewModel` holding the flow state (a class to hold three
fields that `rememberSaveable` already survives process death for); leaving the delay inside the
delay screen and re-reading the store on back (shows the saved value, not the chosen one —
directly contradicts FR-014); `navigation-compose` (rejected in 002 for reasons unchanged here).

---

## R10 — Widening the shortcut configuration screen

**Decision**: `ShortcutConfigScreen` gains `delaySeconds: Int` and `initialTreatment: IconTreatment`,
and its single `onDone` splits into `onBack()` and `onCreated()`. The screen's create path writes
the configuration through `DelayConfigStore` **before** requesting the pin.

**Rationale**: Three requirements land on this screen and none of them fit its current shape.
FR-013 needs the saved treatment as the opening selection, so the internal
`rememberSaveable { entries.first() }` becomes `rememberSaveable { initialTreatment }`. FR-014
needs back and create to lead to different places, so the caller now has to know which happened.
FR-015 needs both values written on apply, and the screen is where the treatment is known.

Feature 002's note that "the caller cannot tell which and must not need to" is narrowed rather than
contradicted. Its reason was that an outcome callback invites the confirmation message FR-012
forbids — and it still does. What the root does with the two callbacks is navigate: back goes to
the delay screen, created goes to the list. Neither says anything to the user, and neither can:
the screen still cannot tell a honoured pin from a declined one.

Saving before pinning, rather than after, because the pin puts a system dialog in front of the
user and the store write must not be waiting behind it. It also fixes the ordering under a crash:
a saved configuration with no icon is harmless (the user re-pins), whereas an icon whose delay was
never written would fall back to the default and silently ignore what the user chose.

**Alternatives considered**: the root performing the save from `onCreated(treatment)` (spreads one
commit across two files and makes the ordering against the pin implicit); a new screen replacing
002's (throws away a working screen to add two parameters); keeping `onDone` and having the root
guess by comparing state (unknowable, and exactly the ambiguity the split removes).

---

## R11 — The delay control

**Decision**: A Material 3 `Slider` with `valueRange = 1f..30f` and `steps = 28` (30 stops, one
second apart), a whole-second `Int` in state, and a plurals-formatted readout beside it. Default
10 seconds, shared as one constant with the unconfigured-shortcut fallback (FR-032).

**Revised during implementation** from `5f..120f` / `steps = 22` (24 stops, five apart). The
numbers were provisional from the start and nothing outside `DelayRange` depended on them, which
is what made the change a one-line edit plus this paragraph. Everything below still holds; only
what the stops are *for* has shifted.

**Rationale**: The clarification fixed the control, not the numbers. At a five-second step the
stops existed to keep every reachable value a *round* number; at a one-second step they exist to
keep it a *whole* one, which is the part FR-005 actually requires — the slider hands back a
`Float`, and without discrete stops and `snap` the readout would show 17 seconds for a handle
resting at 16.999. `steps` counts the stops *between* the endpoints, hence 28 for 30 positions —
the off-by-one that a unit test over the mapping is there to catch, and the one number that had
to move with the range.

The narrower range also changes what the readout has to survive: **one second is now selectable**,
so the `<plurals>` resource is load-bearing rather than defensive. It was written as a plural on
the argument that the minimum was provisional; the minimum then moved.

The readout is a requirement in its own right (FR-007): a slider without one lets the user choose
a value they cannot name. It uses a `<plurals>` resource so "1 second" is not reachable as
"1 seconds" — even though the current minimum is 5, because the minimum is provisional and a
plural bug found later is found by a translator, not a test.

The default lives in `DelayConfig.DEFAULT_SECONDS` and is read by both the screen (FR-006) and the
wait path (FR-032), so the two cannot drift — the requirement that they be the same value is
structural rather than remembered.

**Alternatives considered**: preset chips (the clarification chose otherwise); a continuous slider
with rounding at the edges (rounding at read time means the displayed value and the stored value
can disagree by a hair); a numeric text field (a keyboard, validation, and error states, for a
value with 24 sensible settings).

---

## R12 — Verifying the wait without an instrumented suite

**Decision**: No `app/src/androidTest` at all. The wait's timing, the hand-off, abandonment, and
the unresolvable-target path are **numbered manual cases**, run by the maintainer against
`manual-test-plan.md`. JVM tests cover only what is pure.

**Rationale**: This reverses the decision originally recorded here, and the reversal came from the
maintainer during `/speckit-analyze`: **no automated test may drive a connected device, and an
agent must never drive one to pre-verify a manual case.** Constitution v1.1.0 makes that binding
for the project, which also removes the instrumented requirement feature 002 had waived — the
clause 002 promised would "return in full" here no longer exists, so nothing is owed.

The original plan was an `ActivityScenario` suite handing off to a stub launcher activity in the
test APK, chosen because it avoided `espresso-intents`. It was sound and it is not being built.
What it would have asserted, and where each assertion now lives:

| Would have been | Now |
|---|---|
| `target_is_not_started_before_the_deadline` | M5.1 — timed by hand against a 10 s delay |
| `target_is_started_after_the_deadline` | M5.1, and M3.5 for a changed delay |
| `leaving_before_the_deadline_abandons_the_wait` | M5.4–M5.9 — six departures, one per route, which is more than the one case a scenario could drive |
| `unresolvable_target_finishes_without_waiting` | M6.6, and M6.7 for the target vanishing mid-wait |

The trade is real and worth naming: the timing assertions become a stopwatch rather than a
millisecond comparison, and nothing catches a regression automatically. Against that, the manual
cases test the actual product on an actual launcher — which is what these behaviours depend on —
and the six abandonment routes (back, home, switcher, power button, another app, re-tap) are
things a scenario cannot faithfully reproduce anyway.

**What still carries automated weight**: the pure core. The deadline arithmetic including the
restored-deadline case (`WaitTimingTest`), the configuration sanitising and the **frozen token
rename guard** (`DelayConfigTest`), and the slider mapping (`DelayRangeTest`). Those are where a
silent, permanent error is possible, and none of them needs a device.

**Alternatives considered**: keeping the suite and letting the maintainer skip it (a gate nobody
runs is worse than no gate); Robolectric (rejected by feature 002 already, and it would assert a
simulated lifecycle rather than a real one); a headless timing test over an extracted controller
(possible, but the extraction exists only to be tested, and the arithmetic it would cover is
already `WaitTiming`).

## R13 — Backup and restore of the configuration

**Decision**: Leave `android:allowBackup="true"` and the empty backup rules as they are; the
preferences file is backed up and restored with the app.

**Rationale**: Restoring a user's delays onto a new device is the behaviour they would expect, and
the data is trivial and non-sensitive — package names they configured and a number each. A restore
onto a device with no pinned shortcuts is harmless: the configuration sits dormant until the user
pins, which is the same state a declined pin already produces (spec, Accepted limitations).

Worth stating because the opposite failure is silent: excluding the file would make a device
transfer look like the data loss the spec's edge case describes, where every icon quietly reverts
to the default delay.

---

## R14 — One activity, and the constitution's word for it

**Decision**: The wait screen lives in `ShortcutLaunchActivity`. No `DelayActivity` is created.

**Rationale**: The class name is frozen by `contracts/pinned-shortcut.md` — it is written into the
persisted intent of every shortcut already on a home screen — and that contract's whole purpose was
to let this feature change what the activity *does* without touching what shortcuts point at.
Adding a second activity for the wait would mean either re-pinning every shortcut (forbidden by
002's FR-011) or a launch activity whose only job is to start another one.

The constitution names a "countdown `DelayActivity`" in its scope boundary and its testing
expectations. This feature builds that clause's subject under its frozen name and, per FR-023,
without a countdown. The plan's Constitution Check records that reading explicitly rather than
letting the mismatch pass unremarked.

**Alternatives considered**: a separate `DelayActivity` with an `<activity-alias>` under the frozen
name (an alias is the documented remedy for a *forced* rename, not a reason to seek one); renaming
the class to match the constitution's wording (the single most dangerous edit in the codebase, and
`ShortcutContractTest` exists to stop it).
