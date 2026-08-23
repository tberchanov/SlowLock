# Quickstart: Pinned Shortcut Creation

**Feature**: `002-shortcut-pinning`

---

## Prerequisites

- JDK 11+ (project targets Java 11)
- Android SDK with API 37 installed; `local.properties` already points at the SDK
- A device or emulator on API 33+ (`minSdk 33`)
- Feature 001 in place — this feature consumes its `onAppSelected(packageName)` seam and its
  `AppIconCache`

**A physical device matters here.** The emulator's default launcher supports pinning, so it can
show the happy path, but SC-008 asks for two vendors' launchers and SC-007 asks for a reboot.
Neither is answerable on one emulator.

---

## Build and test

```bash
./gradlew assembleDebug     # constitution build gate
./gradlew test              # JVM suite: treatment matrices, frozen contract, null-resolve, pin gate
./gradlew installDebug      # push to a connected device
```

Both `assembleDebug` and `test` MUST pass before this feature is reported complete
(Constitution, Build gate). There is no `connectedAndroidTest` suite — verification is manual,
see `manual-test-plan.md`.

---

## Dependencies

**None added.** `gradle/libs.versions.toml` and `app/build.gradle.kts` are untouched by this
feature — the first one where that is true. Everything used is either a platform API
(`ShortcutManager`, `Icon`, `ColorMatrixColorFilter`) or already on the classpath
(`rememberSaveableStateHolder` comes in transitively with Compose UI).

---

## Manifest change

One activity, no permissions:

```xml
<activity
    android:name=".shortcut.ShortcutLaunchActivity"
    android:exported="false"
    android:excludeFromRecents="true"
    android:noHistory="true"
    android:taskAffinity=""
    android:theme="@style/Theme.SlowLock.Invisible" />
```

- `exported="false"` is correct and deliberate: the launcher calls `LauncherApps.startShortcut()`
  and the system starts the intent under SlowLock's own identity, so no export is needed
  (research.md R5).
- The other four attributes together are FR-019 — no visible SlowLock screen, no recents entry
  (research.md R6).
- **The `android:name` is frozen.** It is written into every pinned shortcut's intent. See
  `contracts/pinned-shortcut.md`; `ShortcutContractTest` fails the build if it changes.

No `<uses-permission>` is added. No `<queries>` entry is added — the home-settings intent is
guarded with `runCatching`, not `resolveActivity` (research.md R11).

---

## Theme addition

`res/values/themes.xml`:

```xml
<style name="Theme.SlowLock.Invisible" parent="android:Theme.Translucent.NoTitleBar">
    <item name="android:windowDisablePreview">true</item>
</style>
```

Translucent rather than `Theme.NoDisplay`: on modern API levels a `NoDisplay` activity must
finish before `onResume` or the system throws. Not part of the frozen contract — the delay
feature replaces it with a visible countdown theme.

---

## Strings added

`res/values/strings.xml` — no existing string is changed:

| Key | Purpose |
|---|---|
| `shortcut_config_title` | Screen title |
| `shortcut_config_back` | Back affordance content description |
| `shortcut_config_create` | "Create shortcut" |
| `shortcut_treatment_original` / `_invert` / `_gray` | Treatment labels (FR-005) |
| `shortcut_target_unavailable` | FR-015 — target gone at create time |
| `shortcut_launch_unavailable` | FR-018 — target gone when the shortcut is tapped |
| `pin_unsupported_message` | FR-030 — plain, no error codes, no API names |
| `pin_unsupported_open_settings` / `_recheck` | FR-031 |
| `pin_unsupported_settings_failed` | research.md R11 — home settings could not be opened |

---

## Trying it by hand

```bash
./gradlew installDebug
```

1. Open SlowLock. On a normal device you land on the app list; on a launcher that refuses pin
   requests you land on the explanation screen instead (FR-029).
2. Tap any app — the configuration screen opens rather than the app launching. **This is the
   behaviour change to feature 001.**
3. Tap Invert, then Gray. The preview changes instantly; nothing else moves.
4. Press "Create shortcut". The screen closes. Your launcher may show a confirmation, or may pin
   silently, or — if this app already had a shortcut — may do nothing visible at all. **All three
   are correct** (spec, Accepted limitations).
5. Find the icon on your home screen and tap it. The target app opens immediately, with no
   SlowLock screen in between and no SlowLock entry in recents.

To watch the pinning decision the app makes:

```bash
adb logcat -s SlowLock
```

To see what the system thinks is pinned:

```bash
adb shell dumpsys shortcut | grep -A 20 com.slowlock
```

To exercise the unsupported path without changing launcher, `dumpsys` will not help — install a
launcher that refuses pin requests, or temporarily stub `PinSupport` to return `Unsupported` and
re-run. The stub must not be committed.

---

## Verifying the two behaviours most likely to be wrong

**Re-pin updates in place (FR-026).** Pin an app as Original, then pin the same app as Gray. You
should end up with **one** icon, now grey. Two icons means `updateShortcuts` is not being called
or the ID is not the package name. No visible feedback on the second pin is expected and correct.

**Shortcuts survive without the app running (FR-017, SC-007).**

```bash
adb shell am force-stop com.slowlock
# tap the shortcut — the target must still open
adb reboot
# tap the shortcut again after boot
```

A failure here means something in the launch path depends on SlowLock's process state, which the
design forbids (`contracts/pinned-shortcut.md`, L6).

---

## Where to look first when something is wrong

| Symptom | Look at |
|---|---|
| Nothing happens on "Create shortcut" | Expected on a silent launcher or an in-place update. Confirm with `dumpsys shortcut` before treating it as a bug |
| A second icon appears for the same app | `updateShortcuts` call missing, or the shortcut ID is not exactly the package name (`ShortcutContract.shortcutId`) |
| Tapping a shortcut does nothing | `ShortcutLaunchActivity` renamed or moved — `ShortcutContractTest` should have caught it. Existing shortcuts on the device are already broken and must be re-pinned |
| Pinned icon differs from the preview | The bake path is not using `IconTreatment.matrix`, or the launcher applied its own background plate to a legacy icon (research.md R8 — record it in M4.x rather than fixing it) |
| Inverted icons are solid black squares | The alpha row of the invert matrix was inverted along with RGB (research.md R7) |
| A SlowLock screen flashes before the target | A manifest attribute is missing from the launch activity, or `finish()` runs late |
| Returning from the config screen loses scroll position | `rememberSaveableStateHolder` missing from the root (research.md R9) |
