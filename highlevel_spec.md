# Android App Spec: Launch Delay ("Slowdown") — Handoff Context

> This document is context for a fresh LLM session. It describes an Android application
> to be built, the chosen technical approach, deliberate trade-offs already made, and
> known limitations. Read it fully before proposing changes — several "obvious"
> improvements have already been considered and rejected for stated reasons.

---

## 1. Problem

Users open certain apps (Instagram, YouTube, messengers) reflexively — an unconscious
reach, not a deliberate decision. The goal is to insert friction between the impulse and
the app, giving the user a moment to notice what they're doing and back out.

**Target behaviour:** interrupt the automatic reach, not to hard-block usage.
**Non-goal:** enforcement. The user is not an adversary. This is a self-nudge for a
cooperative user, not a parental control or an addiction lock.

---

## 2. User Flow

1. User opens the Slowdown app.
2. User sees a list of all launchable installed applications (icon + label).
3. User selects an app to slow down.
4. For that app, user configures:
    - active time windows (e.g. 09:00–18:00, weekdays) during which the delay applies
    - delay duration in seconds
5. On confirmation, a new icon for that app is pinned to the home screen launcher.
   The icon and label visually match the target app.
6. Tapping the pinned icon opens a full-screen delay screen (countdown for the configured
   duration), after which the real app is launched. Outside the configured time windows,
   the launch is immediate.

---

## 3. Technical Approach

### 3.1 Enumerating installed apps

Query launchable activities via `PackageManager.queryIntentActivities()` with
`ACTION_MAIN` + `CATEGORY_LAUNCHER`.

Preferred alternative: `LauncherApps.getActivityList(null, userHandle)` — same data but
profile-aware, so it also sees work-profile apps.

**Package visibility (Android 11+):** declare the intent shape in the manifest. This is
sufficient; do **not** request `QUERY_ALL_PACKAGES`.

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

`QUERY_ALL_PACKAGES` is on Google Play's restricted permission list and requires written
justification at review. It is not needed here and must be avoided.

### 3.2 Creating the slowed icon

`ShortcutManager.requestPinShortcut()` pins a shortcut to the home screen.

- The shortcut's intent targets the app's own `DelayActivity`, carrying the target package
  name as an extra.
- Icon: `Icon.createWithBitmap(<target app icon rasterized to bitmap>)`.
- Label: the target app's label from `PackageManager`.
- Guard with `isRequestPinShortcutSupported()`.

### 3.3 The delay

`DelayActivity` is launched by a user tap, so it is a legitimate foreground start — this
sidesteps the Android 10+ background activity start restriction entirely.

The activity reads the target package from its intent extras, checks the configured
schedule, shows a countdown if within an active window, then launches the target via
`PackageManager.getLaunchIntentForPackage(pkg)` (which can return `null` — handle it).

### 3.4 Data model notes

- **Persist package names, never launcher activity names.** Launcher activity names change
  across app updates (aliases get renamed; Instagram has done this). Matching on a stored
  `ComponentName` will silently break.
- **Labels are localized and unstable.** Display them; match on package name.
- Cache icons to disk keyed by `packageName + versionCode`.

---

## 4. Why This Approach Was Chosen

The main alternative is an `AccessibilityService` listening for
`TYPE_WINDOW_STATE_CHANGED`, or a foreground service polling `UsageStatsManager`. Both
detect *any* foreground change and therefore cover far more launch paths. Both were
rejected for v1 because:

| Concern | Shortcut approach | Accessibility / polling approach |
|---|---|---|
| Dangerous permissions | none | `AccessibilityService` or `PACKAGE_USAGE_STATS` |
| Play Store policy risk | none | High — accessibility-for-non-accessibility is a common rejection reason |
| Battery | zero | Background service, ongoing drain |
| OEM battery killers (Xiaomi, Huawei, Oppo) | unaffected | Service gets killed silently; app appears to work but doesn't |
| Onboarding friction | one system dialog per pinned app | Multi-step permission + OEM whitelisting flow |
| Time to ship | days | weeks |

The shortcut approach is a fast, cheap, low-risk v1.

---

## 5. Known Limitations (Accepted, Not Oversights)

These are real and were weighed deliberately. Do not re-raise them as blockers without new
information.

1. **The original icon still exists.** The app drawer entry cannot be hidden on stock
   Android. The user can always tap the real icon and bypass the delay entirely.
2. **Recents is uncovered.** After the first launch, the app sits in the recents stack.
   Subsequent opens via the app switcher involve no icon and no delay. This is likely a
   high-volume unconscious-open path.
3. **Deep links are uncovered.** A Reel link tapped in a messenger, a notification tap, a
   share-sheet target, or an in-app browser handoff all bypass the launcher entirely.
4. **Launcher search is uncovered** (Pixel search bar, Samsung Finder).
5. **The delay is not enforceable.** The user can press home or back during the countdown.
6. **Removal is trivial** — long-press and drag the shortcut.
7. **Launcher badging.** Some launchers (including Pixel Launcher) draw a small source-app
   badge on pinned shortcuts, so the icon will not look perfectly native.
8. **No batch pinning.** Each pinned shortcut requires its own system confirmation dialog.
9. **Dual-app clones** (Xiaomi Dual Apps, Samsung Secure Folder) run the same package name
   under a different user ID. Behaviour untested.
10. **Uninstalling the app** leaves dead shortcuts on the home screen.

**Consequence:** this design works for a user who *wants* the friction. It does not stop a
user who is actively routing around it, and it does not cover the majority of non-launcher
entry points.

---

## 6. Open Questions

1. **Success metric.** What signal indicates the delay actually changed behaviour?
   "Shortcut retained for N days" is measurable without permissions. Actual usage reduction
   is not measurable without `PACKAGE_USAGE_STATS`.
2. **False positives.** The delay fires on deliberate opens too (replying to a work DM).
   At what frequency does this cause uninstall? Is there any signal that distinguishes a
   reflexive open from a purposeful one?
3. **v2 path.** A hybrid — keep the shortcut for the launcher path, add `UsageStatsManager`
   polling to catch recents and deep links. This is a much softer permission than
   `AccessibilityService` and avoids the accessibility policy review, at the cost of slower
   detection (a brief flash of the target app before the delay screen appears). Is the
   flash acceptable given the product thesis?
4. Does asking v1 users — who granted nothing — for a background permission in v2 create a
   trust problem, given they may self-select as permission-averse?

---

## 7. Prior Art

Comparable shipped apps: one sec (iOS-first, research-backed), ScreenZen, Minimalist Phone.
Technical feasibility is established; differentiation is the open strategic question.

---

## 8. Suggested Next Steps

1. Build the app picker (`LauncherApps` + off-thread icon loading + disk cache).
2. Build shortcut pinning with icon/label mirroring.
3. Build `DelayActivity` with schedule check and countdown.
4. Test on a Xiaomi device with Dual Apps enabled.
5. Instrument shortcut retention.