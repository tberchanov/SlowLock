# Quickstart: Launch Delay

**Feature**: `003-launch-delay`

---

## Prerequisites

- JDK 11+ (project targets Java 11)
- Android SDK with API 37 installed; `local.properties` already points at the SDK
- A device or emulator on API 33+ (`minSdk 33`)
- Features 001 and 002 in place — this feature slots a screen between them and rewrites what a
  pinned shortcut does

**A physical device matters more here than in 002.** Three of this feature's behaviours are
invisible on a stock emulator: what happens when the display times out mid-wait (M4.6), whether
the app is absent from recents after the hand-off (M5.2), and whether a configuration survives a
real reboot (M6.3).

---

## Build and test

```bash
./gradlew assembleDebug   # constitution build gate
./gradlew test            # JVM: config sanitising, frozen tokens, slider mapping, wait maths
./gradlew installDebug    # push to a connected device for the manual pass
```

Both gates MUST pass before this feature is reported complete.

**There is no instrumented suite, and there must not be one.** Constitution v1.1.0 forbids
`src/androidTest`, `connectedAndroidTest`, Espresso and UI Automator outright, and forbids an
agent driving the connected device to pre-verify a manual case. Everything that can only be seen
on a running app — the wait's timing, the hand-off, the six ways of abandoning a wait, what a
launcher does with a pin request — is a numbered case in `manual-test-plan.md`, run by the
maintainer. Feature 002's waiver of the old instrumented requirement is moot: the requirement no
longer exists (plan, Recorded rulings).

---

## Dependencies

**None added.** `gradle/libs.versions.toml` is untouched, as it was in feature 002. The
`androidTest` coordinates already sitting in `app/build.gradle.kts` stay unused — leave them
alone rather than tidying them away; removing them is a separate decision from this feature.

Platform APIs new to this feature: `SharedPreferences`, `SystemClock.elapsedRealtime`,
`FLAG_KEEP_SCREEN_ON`, Material 3 `Slider`.

---

## Manifest and theme changes

One activity entry changes; no permissions, before or after.

```xml
<activity
    android:name=".shortcut.ShortcutLaunchActivity"
    android:exported="false"
    android:excludeFromRecents="true"
    android:launchMode="singleTop"
    android:taskAffinity=""
    android:theme="@style/Theme.SlowLock.Wait" />
```

```xml
<!-- themes.xml — replaces Theme.SlowLock.Invisible -->
<style name="Theme.SlowLock.Wait" parent="android:Theme.DeviceDefault.DayNight">
    <item name="android:windowActionBar">false</item>
    <item name="android:windowNoTitle">true</item>
    <item name="android:windowBackground">@color/wait_background</item>
</style>
```

> **Corrected at T040 (2026-08-24).** This block previously named
> `android:Theme.Material.DayNight.NoActionBar` as the parent. **That theme does not exist** —
> the platform ships no Material DayNight variant, and resource linking fails on it.
> `android:Theme.DeviceDefault.DayNight` is the only switching platform theme family, and since
> it has no `NoActionBar` variant the two window flags do that job. AppCompat's DayNight was not
> an option: it is a new dependency, which this feature forbids. `@color/wait_background`
> resolves through `values-night` by configuration regardless of the parent, so the night
> behaviour described below is unaffected.

`@color/wait_background` and `@color/wait_text` each need a `values-night` variant. A `DayNight`
parent plus night colours is what keeps the wait screen from being a white field at 2 a.m. — and
because the window and the composable resolve the *same* resource, they stay matched in both
modes.

Three deletions matter as much as the additions, and each has a reason in
`contracts/wait-screen.md`: `android:noHistory` (it would restart the wait on every rotation),
`android:windowDisablePreview` (the wait screen wants a starting window), and
`Theme.SlowLock.Invisible` itself (the activity is now meant to be seen).

`windowBackground` and the composable must paint the **same colour resource**. That is what makes
the tap land on the final background with nothing to flash.

---

## Where things live

```text
com.slowlock.delay/
├── DelayConfig.kt        # the record, DEFAULT_SECONDS, sanitising
├── DelayConfigStore.kt   # SharedPreferences, suspend-only, Dispatchers.IO — FROZEN keys
├── DelayRange.kt         # 1..30 by 1; the slider mapping
├── WaitTiming.kt         # deadline and remaining — pure
├── DelayConfigScreen.kt  # slider, readout, next, back
└── WaitScreen.kt         # one line of text on a flat colour

com.slowlock.shortcut/
├── ShortcutLaunchActivity.kt   # now hosts the wait — name still frozen
└── ShortcutConfigScreen.kt     # +delaySeconds, +initialTreatment, onBack/onCreated
```

---

## The three things most likely to be got wrong

1. **Finishing on `onStop` without the `isChangingConfigurations` exception.** Rotating the device
   would then restart the wait, which FR-027 forbids and which no unit test will catch. The
   deadline must also be written to `onSaveInstanceState` and preferred on restore.
2. **Letting the delay screen own the chosen value.** Back from the shortcut screen must show what
   the user chose on the way through, not the saved value and not the default (FR-014). The value
   is hoisted into `SlowLockRoot`'s stage for exactly this reason.
3. **Renaming an `IconTreatment` constant.** The names are on disk now
   (`contracts/delay-config-store.md`). A rename compiles clean and silently reverts every
   configured icon to Original. A JVM test guards it — do not "fix" that test by updating the
   expected strings.

---

## Verifying by hand

`manual-test-plan.md` is not a supplement here — it is the **only** verification of every
behaviour that needs a running app. Eight tiers, M1 to M8, and the maintainer runs them.

Record M4 on video. "Nothing changed" is hard to assert by watching and easy to assert by
comparing the first **settled** frame — once the message has rendered — with the last (SC-002).

Before filing anything as a bug, read the spec's **Accepted limitations**. No countdown is
correct. No confirmation after applying is correct. A wait that dies when the screen locks is
correct. An old shortcut that suddenly pauses for ten seconds is correct.
