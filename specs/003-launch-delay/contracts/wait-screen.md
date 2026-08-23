# Contract: The Wait Screen

**Feature**: `003-launch-delay`

**Supersedes**: the launch obligations L1–L7 in
`specs/002-shortcut-pinning/contracts/pinned-shortcut.md`. Those obligations were written for an
activity that launched immediately and drew nothing. L1, L2, L3, L6 and L7 carry over unchanged
and are restated below as W1, W7, W8, W12 and W13; L4 and L5 are replaced, because the activity now
draws and now waits.

**Does not change**: the frozen half of that contract. The shortcut ID, the target component's
fully-qualified name, the intent action, and the extra key are untouched by this feature. That is
what lets shortcuts pinned by feature 002 gain the wait with nothing asked of the user.

---

## What the screen is

One activity — `com.slowlock.shortcut.ShortcutLaunchActivity`, the frozen name (research R14) —
showing one static message for the configured delay, then starting the target app.

Its whole design is subtractive. Everything a screen normally does to hold attention is a defect
here, so the obligations below are mostly prohibitions, and the manual plan checks for absence.

---

## Obligations

### Reading the target

| # | Obligation |
|---|---|
| W1 | Read the target package from `ShortcutContract.EXTRA_TARGET_PACKAGE`. Never from the intent's data, action, or component (002 L1) |
| W2 | A missing extra is treated exactly like an uninstalled target: tell the user, finish, no wait |

### Before the wait

| # | Obligation |
|---|---|
| W3 | Anchor the deadline in `onCreate`, before any disk read, from `SystemClock.elapsedRealtime()` (R4) |
| W4 | Restore the deadline from `savedInstanceState` when present, in preference to a fresh anchor. A rotation must neither restart nor extend the wait (FR-027) |
| W5 | Resolve the target **before** waiting. If it does not resolve, tell the user and finish immediately — never make someone wait for an app that is already gone (spec, edge cases) |
| W6 | Read the delay through `DelayConfigStore` on `Dispatchers.IO`, underneath the already-visible screen. No configuration read may gate the first frame (FR-022, R3) |
| W7 | Use the default delay when nothing is stored. This is not a branch in this class — `load` answers with the default (FR-032, `delay-config-store.md` S6) |

### During the wait

| # | Obligation |
|---|---|
| W8 | Show one fixed message. No countdown, no remaining or elapsed time, no progress, no spinner, no animation, no transition, no pulse (FR-023, FR-025) |
| W9 | No sound, no vibration, no haptics, no notification (FR-024) |
| W10 | No clickable, focusable, or draggable element anywhere on the screen. A tap does nothing (FR-026) |
| W11 | Do not name or depict the target app. The same screen for every app (spec, Assumptions) |
| W12 | Do not read `MaterialTheme`, dynamic colour, or any wallpaper-derived value. Paint the flat colour resource that the activity's `windowBackground` also uses, so the starting window and the composed screen are indistinguishable (R7). Provide a `values-night` variant of both colours: the screen must follow the device's light/dark setting, or it becomes a full-brightness white field at night on the one screen designed not to be noticed |
| W13 | Hold `FLAG_KEEP_SCREEN_ON` for the window's lifetime, and nothing longer. No `PowerManager` wake lock, ever. Permitted by Constitution IV as amended in v1.1.0, on exactly these terms (R6) |
| W14 | Register no back handler. The system's back must finish the activity, which abandons the wait (FR-029) |

### Ending the wait

| # | Obligation |
|---|---|
| W15 | On `onStop`, finish — **unless** `isChangingConfigurations` is true. This is the whole of FR-029: back, home, the app switcher, another app taking over, the display timing out, and the device locking all arrive here (R5) |
| W16 | Before starting the target, re-check that the lifecycle is at least `STARTED`. The deadline can expire in the same instant the user leaves, and a launch from a stopping activity is the background start Constitution IV forbids |
| W17 | Re-resolve the target at hand-off, not at anchor time. It can be uninstalled during the wait (FR-030, 002 L2) |
| W18 | A null resolution at hand-off tells the user the app is unavailable and finishes. Never crash (FR-030, 002 L3) |
| W19 | Start the target with `FLAG_ACTIVITY_NEW_TASK`, then `finish()` immediately (002 L4, FR-031) |
| W20 | Leave no visible SlowLock screen and no recents entry, in either outcome. Backing out of the target must not return to the wait screen (FR-031) |
| W21 | Never kill the process by hand. The activity finishing is the end of the feature's work (R8) |

### Repeat and concurrent taps

| # | Obligation |
|---|---|
| W22 | `launchMode="singleTop"`. A second tap of the same shortcut arrives at `onNewIntent` and is **ignored** — no second wait, no restart, no extension (FR-027) |
| W23 | A new intent naming a *different* target restarts the wait for that target (spec, edge cases) |

### Independence

| # | Obligation |
|---|---|
| W24 | Work with SlowLock force-stopped and after a reboot. Read nothing from memory that a cold start would not have (FR-033, 002 L6) |
| W25 | Never persist or match a `ComponentName` of the target. The package name is the only identifier (Constitution V, 002 L7) |

---

## Manifest shape

```xml
<activity
    android:name=".shortcut.ShortcutLaunchActivity"
    android:exported="false"
    android:excludeFromRecents="true"
    android:launchMode="singleTop"
    android:taskAffinity=""
    android:theme="@style/Theme.SlowLock.Wait" />
```

Changed from feature 002, and each change is deliberate:

| Attribute | Was | Now | Why |
|---|---|---|---|
| `android:theme` | `Theme.SlowLock.Invisible` | `Theme.SlowLock.Wait` | The activity is now meant to be seen. Opaque, no action bar, `windowBackground` set to the wait colour so the starting window matches the composed screen (R7) |
| `android:noHistory` | `true` | **removed** | It would finish the activity on every `onStop`, including a rotation, restarting the wait. The explicit `onStop` rule (W15) has the exception it needs and can be driven by a test (R5) |
| `android:launchMode` | *(standard)* | `singleTop` | W22 |
| `android:windowDisablePreview` (theme) | `true` | **removed** | The wait screen wants a starting window; the invisible activity did not (R7) |

Unchanged and load-bearing: `exported="false"` (the system starts the intent as the publisher),
`excludeFromRecents` and the empty `taskAffinity` (FR-031).

---

## Verification

**Manual, by the maintainer.** Constitution v1.1.0 forbids instrumented suites and forbids an
agent driving the connected device, so every obligation above that can only be observed on a
running app is a numbered case:

| Obligations | Cases |
|---|---|
| W3, W17–W19 — the wait runs its length and hands off | M5.1 |
| W15, W16 — every departure abandons it | M5.4–M5.9, six routes, one each |
| W4 — a rotation neither restarts nor extends | M4.8 |
| W5, W18 — an unresolvable target, before and during | M6.6, M6.7 |
| W8–W12 — nothing moves, nothing sounds, nothing is tappable, night and day | M4.1–M4.7, M4.9 |
| W13 — the display stays awake for a long wait | M4.6 |
| W20, W21 — nothing left in recents, nothing behind the target | M5.2, M5.3 |
| W22, W24 — repeat taps, force-stop, reboot | M5.9, M6.1, M6.3 |

JVM (`app/src/test`), the parts that need no device: the deadline arithmetic and the restore path
(`WaitTimingTest`, W3, W4), and the configuration read the wait depends on (`DelayConfigTest`, W7).

Absence is the assertion in most of the manual cases, so M4 is worth doing on video: SC-002
compares the **first settled frame** — once the message has rendered — with the last.
