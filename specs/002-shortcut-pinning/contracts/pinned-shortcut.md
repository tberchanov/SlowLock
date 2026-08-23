# Contract: The Pinned Shortcut

**Feature**: `002-shortcut-pinning`

**Status**: **FROZEN.** Everything else in this feature is a draft to be revisited. This is not.

---

## Why this one is different

Shortcuts pinned during this draft land on a user's home screen and stay there. The app cannot
enumerate them reliably, cannot remove them, and must never ask the user to re-create them
(FR-011). Every value below is therefore permanent from the first pin on a real device onward:
change one and the shortcuts already out there break — silently, at the moment the user taps
them, which is the most expensive kind of breakage the constitution catalogues.

The *behaviour* behind these values is expected to change. That is the point of freezing them.

---

## The frozen values

```kotlin
// ShortcutContract.kt — every constant here is permanent
object ShortcutContract {
    const val LAUNCH_ACTIVITY = "com.slowlock.shortcut.ShortcutLaunchActivity"
    const val EXTRA_TARGET_PACKAGE = "com.slowlock.shortcut.extra.TARGET_PACKAGE"
    const val ACTION = Intent.ACTION_VIEW
    fun shortcutId(targetPackage: String): String = targetPackage
}
```

| Element | Value | Frozen because |
|---|---|---|
| **Shortcut ID** | the target's package name, verbatim | It is the identity the launcher stores. A different scheme would orphan every existing shortcut and add a second icon for every app (FR-025, FR-026) |
| **Target component** | `com.slowlock.shortcut.ShortcutLaunchActivity` | The FQN is written into the persisted intent. Renaming or moving the class breaks every pinned shortcut on every device |
| **Intent action** | `android.intent.action.VIEW` | Part of the persisted intent. `ShortcutInfo.Builder` requires an action |
| **Extra key** | `com.slowlock.shortcut.extra.TARGET_PACKAGE` | The persisted payload. A renamed key means the launch activity reads nothing and every existing shortcut dead-ends |
| **Extra value** | the target's package name | Constitution V: the only identifier the platform guarantees stable |

Assembled:

```kotlin
Intent(ShortcutContract.ACTION)
    .setComponent(ComponentName(context, ShortcutLaunchActivity::class.java))
    .putExtra(ShortcutContract.EXTRA_TARGET_PACKAGE, target.packageName)
```

---

## What is explicitly NOT frozen

Free to change without touching a single pinned shortcut — this is the whole return on the
design:

| Not frozen | Notes |
|---|---|
| **What `ShortcutLaunchActivity` does** | Today: resolve and start immediately. Tomorrow: countdown, schedule check, or launch straight through when outside a delay window. This is how FR-011 is honoured — existing shortcuts gain the delay with nothing asked of the user |
| **The activity's theme and manifest attributes** | Not persisted. The transparent, `excludeFromRecents` declaration this draft uses becomes a visible countdown theme when the delay ships |

> **What actually shipped (feature 003, verified 2026-08-24).** This section's *predictions* were
> half right and are corrected here; **no frozen value below or above was touched.**
>
> - The activity now waits, then resolves and starts — but there is **no countdown** and **no
>   schedule check**. The wait screen is deliberately motionless (`003/contracts/wait-screen.md`
>   W8–W11), and schedules remain unbuilt. "A visible countdown theme" became
>   `Theme.SlowLock.Wait`, a plain DayNight theme.
> - The manifest attributes did change, all within this section's permission:
>   `theme` → `@style/Theme.SlowLock.Wait`, `launchMode="singleTop"` added, `noHistory="true"`
>   **removed**. `exported`, `excludeFromRecents` and the empty `taskAffinity` are unchanged.
> - Obligations L4 and L5 below are superseded by `003/contracts/wait-screen.md` (W19, W20); L1,
>   L2, L3, L6 and L7 all still hold verbatim.
>
> **The return on the freeze was collected in full**: every shortcut pinned under feature 002
> gained the delay with nothing re-pinned and nothing asked of the user.
| **The icon and label on any given shortcut** | Replaceable in place via `updateShortcuts` (see below) |
| **Everything about the configuration screen** | Ordinary draft UI, expected to be replaced by the delay-configuration screen |

---

## The rename hazard, and its guard

`ComponentName(context, ShortcutLaunchActivity::class.java)` resolves at build time, so a
refactor that renames or moves the class compiles clean and pins *new* shortcuts at the new
name — while every shortcut already on a home screen keeps pointing at the old one and dies.

Guarded by a JVM unit test, so the failure lands in `./gradlew test` rather than on a user's
home screen:

```kotlin
@Test fun launch_activity_fqn_is_frozen() {
    assertEquals(ShortcutContract.LAUNCH_ACTIVITY, ShortcutLaunchActivity::class.java.name)
}
```

If a rename ever becomes genuinely unavoidable, the remedy is an `<activity-alias>` under the
old name — never a re-pin.

---

## Creating and updating: two calls, neither conditional

```kotlin
val info = ShortcutInfo.Builder(context, spec.id)
    .setShortLabel(spec.label)
    .setLongLabel(spec.label)
    .setIcon(Icon.createWithBitmap(treatedBitmap))
    .setIntent(intent)
    .build()

shortcutManager.updateShortcuts(listOf(info))   // refreshes an already-pinned shortcut
shortcutManager.requestPinShortcut(info, null)  // pins it if it is not pinned yet
```

Both calls run every time, and the app records nothing about what it has pinned before (FR-027).
Each is a no-op in the case it does not apply:

| Situation | `updateShortcuts` | `requestPinShortcut` | User sees |
|---|---|---|---|
| First pin of this app | matches nothing, no-op | launcher shows its dialog, one icon added | The launcher's dialog (or nothing, on a launcher that pins silently) |
| App already has a shortcut | updates icon and label in place | already pinned → succeeds immediately, **no dialog, no second icon** | Nothing at all |

The second row is the spec's accepted limitation, in mechanism: an update is invisible from
inside the app. `requestPinShortcut` alone would not do — AOSP short-circuits the
already-pinned case *without* applying the new `ShortcutInfo`, so the new icon would be
silently dropped (research.md R3).

`updateShortcuts` returns `false` when rate-limited; the limit resets on every foreground
entry, every call here originates in a foreground tap, so the value is logged and not acted on.

**No `IntentSender` is passed.** The app does not observe the outcome — FR-012 leaves
confirmation to the launcher, and a success callback would only tempt a message the app has
promised not to show.

---

## Obligations on the launch activity

| # | Obligation |
|---|---|
| L1 | Read the target package from `EXTRA_TARGET_PACKAGE`. Never from the intent's data, action, or anything else |
| L2 | Re-resolve the package at tap time via `getLaunchIntentForPackage()`. The app may have been uninstalled since the pin (FR-018) |
| L3 | Null resolution ⇒ tell the user the app is unavailable and finish. Never crash (FR-018) |
| L4 | Start the target with `FLAG_ACTIVITY_NEW_TASK`, then `finish()` immediately (FR-016) |
| L5 | Leave no visible SlowLock screen and no recents entry (FR-019) |
| L6 | Work with SlowLock force-stopped and after a reboot — no reliance on any process, service, or in-memory state (FR-017) |
| L7 | Never persist or match on a `ComponentName` of the *target* app; the package name is the only identifier (Constitution V) |

L6 is what makes the whole design viable: the shortcut carries everything the launch needs, so
there is nothing to restore and nothing to keep running (Constitution IV — zero battery at rest).

---

## Verification

Automated (JVM, `app/src/test`):

```kotlin
@Test fun launch_activity_fqn_is_frozen()          // the rename guard above
@Test fun extra_key_is_frozen()
@Test fun shortcut_id_is_the_target_package()      // FR-025
@Test fun unresolvable_target_is_not_launched()    // L2, L3, FR-018
```

Manual — that a launcher honours the pin, that the icon matches the preview, and that the
shortcut survives a reboot and a force-stop. See `manual-test-plan.md` M2.x and M5.x. SC-008
requires two vendors' launchers, which is a device matrix rather than a test suite.
