# Contract: Selection Hand-off

**Feature**: `001-installed-apps-list`
**Status**: **CONSUMED** by `002-shortcut-pinning`.

The seam where this feature ends and the (then not yet built) configuration feature begins.
Pinning this down now is what lets US3 ship before its consumer exists.

---

## Outcome

The consumer arrived as `002-shortcut-pinning`, and **the contract held without a single
change to its shape**:

```kotlin
onAppSelected(packageName: String)
```

One `String`, still the package name, still nothing else. `AppListScreen.kt` was **not
modified** by 002 — the interim launch was swapped for navigation to the shortcut
configuration screen entirely on the consumer's side, in `SlowLockRoot`. That was the entire
point of writing this contract before the consumer existed, and it is the concrete evidence
that the seam was drawn in the right place.

The consumer obligations were all discharged in 002:

| # | Where |
|---|---|
| C1 | `ShortcutContract.shortcutId(targetPackage) = targetPackage` — identity is the package name, nothing else is persisted (002 FR-025, FR-027) |
| C2 | `ShortcutConfigScreen` re-resolves on open **and again** at the moment of the pin, because the app can be uninstalled while the screen is open (002 FR-015) |
| C3 | Label, icon, and version code are re-resolved in `resolveShortcutTarget`, never carried across the seam |

One thing the contract did not anticipate: 002 is not the delay-configuration feature this
was written to hand off to. The configuration screen is a **draft stand-in** for it. The seam
was drawn in terms of what crosses it rather than which feature was on the far side, which is
why the far side changing identity cost nothing.

---

## The contract

```kotlin
onAppSelected(packageName: String)
```

**One `String`. The package name. Nothing else.**

This is not minimalism for its own sake — it is Constitution V made concrete:

- `packageName` is the only identifier the platform guarantees stable across app updates
  and label changes (FR-010).
- `label` is localized and mutable; passing it would invite the consumer to key on it.
- `ComponentName` / launcher activity name is renamed across updates and fails silently;
  Constitution V forbids persisting or matching on it.
- `versionCode` is an icon-cache detail, meaningless to the consumer, and stale the moment
  the app updates.

If the consumer needs a label or an icon, it re-resolves them from the package name at the
point of use. Display data is never carried across the seam.

---

## Producer obligations (this feature)

| # | Obligation |
|---|---|
| P1 | Invoke `onAppSelected` exactly once per tap, with the package of the row the user actually tapped |
| P2 | Never invoke it with a package that does not currently resolve via `getLaunchIntentForPackage()` (FR-014) |
| P3 | Never invoke it with SlowLock's own package (FR-003) |
| P4 | Never apply a delay, countdown, or schedule check before the call — the consumer decides what happens next (FR-018) |
| P5 | Preserve scroll position and query so the user returns to where they were (FR-017) |

---

## Consumer obligations (future feature)

| # | Obligation |
|---|---|
| C1 | Treat the package name as the persisted key; never persist label or `ComponentName` |
| C2 | Re-check `getLaunchIntentForPackage()` at use time — the app may be uninstalled between selection and use |
| C3 | Re-resolve label and icon for display rather than expecting them to be handed over |

---

## Interim implementation: launch the target directly

> **SUPERSEDED by `002-shortcut-pinning`.** This section is kept as the record of what
> shipped in 001 and why. The interim launch is **gone**: `MainActivity.launchApp()` was
> deleted, the root became `SlowLockRoot`, and a tap now opens the shortcut configuration
> screen (002 FR-001). The feasibility proof it existed to produce — resolve a stored package
> name, start the right app — **succeeded**, and that mechanism now lives in
> `ShortcutLaunchActivity`, launched from the pinned home-screen icon. `FLAG_ACTIVITY_NEW_TASK`
> and the caught `ActivityNotFoundException` both carried over to it, as the note below
> predicted they would.

`MainActivity` supplies the callback. Until the configuration screen exists, it **starts
the target app**:

```kotlin
AppListScreen(
    onAppSelected = { packageName ->
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        // null already filtered by the ViewModel (P2), but the app can be
        // uninstalled between resolve and start — catch and report.
        runCatching { intent?.let(::startActivity) }
    },
)
```

This is a **feasibility proof**, not the final flow. It exercises the one mechanism the
whole product rests on — resolve a stored package name, start the right app — before any
configuration, scheduling, or shortcut machinery is built on it. No delay, countdown, or
schedule evaluation belongs here (FR-018); those arrive with their own spec.

Three things this implementation must get right:

| Requirement | Why |
|---|---|
| `FLAG_ACTIVITY_NEW_TASK` | The target gets its own task, so returning to SlowLock returns to the list rather than unwinding the target's back stack into it — and it is the same flag the future `DelayActivity` will need |
| The launch originates from the user's tap, in the foreground | Constitution IV forbids background activity starts. A tap handler is a user-initiated foreground context, so this is compliant — but the call site must stay in the tap path and never move to a service or receiver |
| `ActivityNotFoundException` is caught | The ViewModel's null check (P2) closes the common case, but the app can be uninstalled in the window between resolving and starting. A non-null intent is not a guarantee (FR-014) |

Swapping this for navigation to the configuration screen must not require touching
`AppListScreen` — that is the point of the callback shape.

---

## Verification

Automated (unit, `app/src/test`) — the null-resolution path only:

```kotlin
@Test fun a_package_that_does_not_resolve_is_not_handed_off() { … }  // P2, FR-014
```

Manual — that the correct app actually opens, and that the round trip preserves list
state. See `manual-test-plan.md` T1.12, T1.13, T2.8. Whether the right app appears on
screen is precisely the kind of thing a device test would assert badly and a human
verifies instantly.

**Post-002**: T1.12 and T1.16 were re-written against the new tap behaviour — a tap opens the
configuration screen, and the round trip through it preserves list state. That the correct app
actually opens is now verified from the pinned shortcut, in 002's `manual-test-plan.md` M2 and
M5, which additionally cover what this feature never could: that it still works after a
force-stop and a reboot.
