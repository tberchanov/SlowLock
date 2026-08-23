# Implementation Plan: Launch Delay

**Branch**: `003-launch-delay` | **Date**: 2026-08-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-launch-delay/spec.md`

## Summary

A delay configuration screen slots in between the app list and feature 002's shortcut screen: a
slider in whole seconds, a numeric readout, and a "next" that carries the value forward. Applying
saves the delay and the icon treatment against the target's package name and pins the shortcut as
before. Tapping the pinned icon now shows a motionless "Please wait" screen for that app's delay,
then opens the target and leaves nothing in recents.

Three decisions carry the feature. **The delay is read at tap time, not carried in the shortcut**
(research R1) — that is what makes a changed delay take effect on the icon already on the home
screen (FR-018) and what keeps `contracts/pinned-shortcut.md` frozen, so every shortcut pinned by
feature 002 gains the wait with nothing asked of the user. **A wait is bound to the screen being
visible** (R5): `onStop` finishes the activity unless it is a configuration change, which is one
rule covering back, home, the app switcher, and the display timing out, while leaving rotation
alone. **The screen is boring by construction** (R7): a flat colour resource painted by both the
window and the composable, no `MaterialTheme` in scope, no animation API in the file, and the
activity's `windowBackground` set to that same colour so the tap lands on the final background
with nothing to flash — in a light and a dark variant, because a screen designed not to be
noticed must not be a white field at night.

Technical shape: one `SharedPreferences` file behind a `suspend`-only `DelayConfigStore`, with its
file name, key suffixes, and treatment tokens frozen the way the shortcut contract is; an
`elapsedRealtime` deadline carried through `onSaveInstanceState` so a rotation neither restarts nor
extends a wait; `SlowLockRoot`'s `when` grown from two branches to three with the chosen delay
hoisted into the stage so back from the shortcut screen restores it; and feature 002's
`ShortcutConfigScreen` widened by two parameters with its single exit split into `onBack` and
`onCreated`. One cost is argued rather than assumed and appears in Complexity Tracking: persisting
anything at all. `FLAG_KEEP_SCREEN_ON` was the second until Constitution v1.1.0 made a
window-scoped display lock explicitly allowed; it is now compliant, not a deviation.

## Technical Context

**Language/Version**: Kotlin 2.2.10, Java/JVM target 11

**Primary Dependencies**: **No new dependencies.** Existing set only — Jetpack Compose (BOM
2026.02.01), Material 3, `core-ktx`, `activity-compose`, `lifecycle-runtime-ktx`,
`lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`. The `androidTest` coordinates already
declared in `app/build.gradle.kts` stay unused — Constitution v1.1.0 forbids instrumented suites
outright (research R12). Platform APIs added by this feature:
`SharedPreferences`, `SystemClock.elapsedRealtime`, `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON`,
Material 3 `Slider`.

**Storage**: One `SharedPreferences` file, `slowlock.delay-config`, holding two keys per configured
app (`<pkg>.delaySeconds`, `<pkg>.treatment`). The project's first persisted state. Accessed only
through `DelayConfigStore`, whose every function is `suspend` and runs on `Dispatchers.IO`. File
name, key suffixes, and treatment tokens are frozen — `contracts/delay-config-store.md`.

**Testing**: **JVM unit tests only, plus a manual pass run by the maintainer.** Constitution
v1.1.0 forbids instrumented suites and forbids an agent driving the connected device, so
everything that can only be seen on a running app — the wait's timing, the hand-off, abandonment,
what a launcher does with a pin request — is a numbered case in `manual-test-plan.md` and is
verified by a person. Automated coverage is the pure core: configuration sanitising, the frozen
persistence tokens, the slider mapping, and the wait arithmetic (R12). Gates: `./gradlew
assembleDebug` and `./gradlew test`.

**Target Platform**: Android, `minSdk 33`, `targetSdk`/`compileSdk 37`

**Project Type**: Mobile app — single `:app` Gradle module, `com.slowlock`

**Performance Goals**: Wait background on screen within 200 ms of the tap and its message within
500 ms, with the starting window already the final background (FR-022); target opens within one
second of the deadline (SC-003); list → configured icon in under 45 seconds and ≤5 taps (SC-001);
zero SlowLock battery attribution outside an active wait (SC-010)

**Constraints**: Zero permission prompts (FR-034, SC-009); no service, notification, polling loop,
or power-management lock beyond keeping the display awake for the visible length of a wait
(FR-035, Constitution IV as amended); no disk I/O on the main thread (Constitution IV, FR-036); no
background activity start under any circumstance (FR-029); nothing visibly changes on the wait
screen for its whole duration (FR-023, SC-002); shortcuts already pinned by feature 002 must gain
the wait without being re-created; `contracts/pinned-shortcut.md` stays byte-for-byte true

**Scale/Scope**: One new screen, one screen widened, one activity rewritten, one persisted file.
Six new source files, four changed, three new unit-test files, one manual test plan of eight
tiers.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Constitution v1.1.0** (amended 2026-08-23, part-way through this feature's planning — see
Recorded rulings). Evaluated pre-research and re-evaluated post-design; both results shown, and
the two rows the amendment touched are re-marked against the new text.

| Principle | Binding rule as it applies here | Pre-Phase 0 | Post-Phase 1 |
|---|---|---|---|
| **I. Cooperative User, Not Adversary** | The wait is escapable by back and by home, and abandoning it is a first-class outcome rather than an error (FR-029, US4) — the constitution's requirement is met without needing a skip control. No bypass path is closed: the original icon, recents, deep links, and launcher search all still open the target instantly, and removing the shortcut still removes the friction (spec, Accepted limitations). Nothing here polices the user; the delay they wait through is the one they chose. | ✅ PASS | ✅ PASS |
| **II. Simplicity First (YAGNI)** | **Zero new dependencies** — `DataStore` rejected (R1), `navigation-compose` rejected again (R9); the instrumented suite that would have needed `espresso-intents` is not built at all (R12). No new module, no DI, no ViewModel for either new surface. Schedules and time windows are not built, though the constitution's scope boundary allows them. **One deviation**: persistence is introduced, which the principle names as a justified deviation rather than a default — Complexity Tracking row 1. | ⚠️ PASS with 1 justified deviation | ⚠️ PASS with 1 justified deviation |
| **III. Permission & Policy Minimalism** | **Zero** new `<uses-permission>` elements and zero prompts (FR-034). `FLAG_KEEP_SCREEN_ON` is a window flag and needs no permission — a `PowerManager` wake lock would have needed `WAKE_LOCK`, which is why it was rejected (R6). No `QUERY_ALL_PACKAGES`, no `AccessibilityService`, no `PACKAGE_USAGE_STATS`. The activity stays `exported="false"`. | ✅ PASS | ✅ PASS |
| **IV. Platform-Idiomatic Android** | Kotlin + Compose + Material 3, no XML layouts. `getLaunchIntentForPackage()`'s null handled at both call sites — before the wait and again after it (FR-030). Every store read and write on `Dispatchers.IO` (R1, FR-036). The launch originates in the user's tap and is re-checked against `STARTED` before it fires, so a background activity start is impossible by construction (R5). No service, no receiver, no polling, no alarm; the process ends with the activity and is not killed by hand (R8). `FLAG_KEEP_SCREEN_ON` sits inside the allowance v1.1.0 added: a window flag, on a screen the user is looking at, on a feature that does not work without it, released with the window. No `PowerManager` wake lock (R6). | ⚠️ PASS with 1 deviation (pre-amendment) | ✅ PASS |
| **V. Stable Identifiers** | `packageName` is the configuration's only key, the shortcut's ID, and the intent's only payload (FR-016). No `ComponentName` of a target is persisted or matched. Labels stay display-only. **One new hazard, guarded**: the persisted treatment token is an enum constant's name, so a rename would silently revert every configured icon to `Original` — frozen in `contracts/delay-config-store.md` and asserted by a JVM test, exactly as `ShortcutContract` guards the activity's FQN (R2). | ✅ PASS | ✅ PASS |

**Technology Standards check**: fixed stack honoured (Kotlin/Compose/M3, single module, Java 11,
minSdk 33, targetSdk 37, `com.slowlock`). No dependency added, so `gradle/libs.versions.toml` is
untouched and `app/build.gradle.kts` changes only if the instrumented suite needs a source set
declaration (it does not — `src/androidTest` is conventional). No backend, no network, no
analytics, no third-party SDK.

**Scope boundary check**: "per-app schedule and delay configuration" and "a delay screen that
launches the target" are the two remaining v1 items. This feature builds the delay half of the
first and the whole of the second. Schedules are deliberately not built — less than the boundary
permits, which YAGNI encourages and no principle forbids. Since v1.1.0 the boundary also states
that how the delay screen presents is the feature's decision, which is where FR-023's static
screen now sits without argument.

### Recorded rulings

All three of this plan's original rulings were **superseded by Constitution v1.1.0**, amended on
2026-08-23 at the maintainer's instruction after `/speckit-analyze` found that two of them were
reinterpreting binding text rather than following it. What follows is the record of what changed
and why, kept because the next feature will otherwise re-derive it.

**On keeping the display awake.** v1.0.0 said "no wake locks in v1" without qualification, and
this plan took `FLAG_KEEP_SCREEN_ON` anyway, justifying it in Complexity Tracking on the grounds
that the rule's intent was background drain. `/speckit-analyze` called that dilution of a MUST,
correctly. v1.1.0 now allows a window-scoped display lock explicitly and on stated conditions,
all of which this feature meets. **Result: compliant, no longer a deviation.** Complexity
Tracking row 2 is removed. Without the flag, a delay longer than the device's screen timeout
could never complete (R6) — the reason the allowance was worth asking for.

**On the word "countdown".** v1.0.0 described v1's delay screen as a *countdown* `DelayActivity`,
which FR-023 contradicts outright. This plan ruled the word non-normative. v1.1.0 replaces it with
"delay screen" and states that presentation is the feature's decision. **Result: no ruling
needed; the text now says what this plan argued it meant.**

**On feature 002's instrumented-test waiver.** 002 waived instrumented coverage of the hand-off
"for this feature only" and said it returned in full here. It does not: v1.1.0 removes the
instrumented requirement entirely and forbids instrumented suites, so the clause 002 waived no
longer exists. **Result: moot. Nothing is inherited and nothing is owed.** The coverage that
`WaitHandoffTest` would have carried is now manual cases M5.1, M5.4–M5.9 and M6.6–M6.7, run by
the maintainer (R12).

**Gate result**: PASS. Pre-amendment the gate passed with two justified deviations; post-amendment
one of them is compliant outright and only the persistence row remains. Nothing is silent, and the
one remaining deviation is reversible before implementation begins.

## Project Structure

### Documentation (this feature)

```text
specs/003-launch-delay/
├── plan.md                        # This file
├── research.md                    # Phase 0 — 14 decisions
├── data-model.md                  # Phase 1
├── quickstart.md                  # Phase 1
├── manual-test-plan.md            # Phase 1 — primary verification artifact
├── contracts/
│   ├── delay-config-store.md      # FROZEN persisted shape
│   ├── wait-screen.md             # Obligations of the wait activity
│   └── delay-config-screen.md     # The new screen, and the widened 002 seam
├── checklists/
│   └── requirements.md            # From /speckit-specify — 16/16
└── tasks.md                       # /speckit-tasks output — not created here
```

### Source Code (repository root)

```text
app/src/main/java/com/slowlock/
├── MainActivity.kt                     # unchanged
├── SlowLockRoot.kt                     # CHANGED — three stages, delay hoisted (R9)
├── apps/                               # unchanged (list, icon cache, view model)
├── delay/                              # NEW package
│   ├── DelayConfig.kt                  # the record, the default, sanitising, frozen tokens
│   ├── DelayConfigStore.kt             # SharedPreferences, suspend-only, Dispatchers.IO
│   ├── DelayConfigScreen.kt            # slider + readout + next + back
│   ├── DelayRange.kt                   # min/max/step and the slider value mapping (pure)
│   ├── WaitTiming.kt                   # deadline and remaining time (pure)
│   └── WaitScreen.kt                   # the static composable
├── shortcut/
│   ├── ShortcutLaunchActivity.kt       # CHANGED — hosts the wait, then hands off
│   ├── ShortcutConfigScreen.kt         # CHANGED — +delaySeconds, +initialTreatment, onBack/onCreated
│   ├── ShortcutContract.kt             # unchanged and frozen
│   ├── ShortcutPinner.kt               # unchanged
│   ├── ShortcutTarget.kt               # unchanged
│   ├── PinSupport.kt                   # unchanged
│   └── PinUnsupportedScreen.kt         # unchanged
└── ui/theme/                           # unchanged

app/src/main/
├── AndroidManifest.xml                 # CHANGED — wait theme, singleTop, noHistory removed
├── res/values/
│   ├── colors.xml                      # CHANGED — the flat wait colours (light)
│   ├── strings.xml                     # CHANGED — delay screen, wait screen, plurals
│   └── themes.xml                      # CHANGED — Theme.SlowLock.Wait replaces .Invisible
└── res/values-night/
    └── colors.xml                      # NEW — the dark variant of the two wait colours

app/src/test/java/com/slowlock/
├── delay/DelayConfigTest.kt            # NEW — defaults, sanitising, frozen tokens
├── delay/DelayRangeTest.kt             # NEW — slider mapping, step count, clamping
├── delay/WaitTimingTest.kt             # NEW — deadline arithmetic, restore, expiry
└── shortcut/*.kt                       # unchanged

# No app/src/androidTest. Constitution v1.1.0 forbids instrumented suites; the behaviour they
# would have covered is manual-test-plan.md M4-M6, run by the maintainer.
```

**Structure Decision**: Single `:app` module, unchanged. The new `com.slowlock.delay` package sits
beside `apps` and `shortcut` and holds everything the delay owns — the record, its store, its two
screens, and the two pure helpers the tests drive. `ShortcutLaunchActivity` stays in `shortcut`
because its fully-qualified name is frozen (R14); it imports from `delay` rather than moving.

## Downstream edits to features 001 and 002

This feature amends its predecessors rather than only extending them. The plan owns these edits
and `/speckit-tasks` must schedule them:

| Document or code | Change |
|---|---|
| `specs/002-shortcut-pinning/spec.md` FR-001 | A list tap now opens the **delay** screen, not the shortcut screen |
| `specs/002-shortcut-pinning/spec.md` FR-006 | Original is the opening treatment only for apps with no saved configuration (this feature's FR-013) |
| `specs/002-shortcut-pinning/spec.md` FR-016 | A pinned shortcut no longer opens the target immediately — it waits first |
| `specs/002-shortcut-pinning/manual-test-plan.md` M1.1, M1.4, M2.3, M5.x | Assert the old behaviour; re-point at this feature's cases |
| `specs/002-shortcut-pinning/contracts/shortcut-config-screen.md` | The screen's seam gains two parameters and splits its exit (R10) |
| `contracts/pinned-shortcut.md` (002) | **No change.** Verified explicitly: this feature alters only what is listed there as *not* frozen — the activity's body, its theme, and its manifest attributes |

## Complexity Tracking

**One** deviation, down from two. The `FLAG_KEEP_SCREEN_ON` row was removed when Constitution
v1.1.0 made a window-scoped display lock explicitly permitted — the reasoning that justified it
survives as research R6 and in Recorded rulings above, but it is no longer a deviation to track.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| **Persistence introduced** (Constitution II names a persistence engine as a justified deviation, not a default) — one `SharedPreferences` file | FR-017 requires the delay and treatment to survive force-stop, reboot, and update, and FR-012/FR-013 require them to be shown again when the user returns. The feature does not exist without durable per-app state | **No persistence, carrying the delay in the shortcut's intent extras**: rewriting a pinned shortcut's intent needs `updateShortcuts`, which fails silently when rate-limited, reaches nothing once the user has removed the icon, and cannot represent an app configured before pinning — so FR-018 would depend on a call that can quietly not happen (R1). **DataStore or Room**: a real new dependency or a real engine, for a map of integers. `SharedPreferences` is the platform's built-in and adds no coordinate to `libs.versions.toml` |
