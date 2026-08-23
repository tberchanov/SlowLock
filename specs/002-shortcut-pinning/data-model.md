# Phase 1 Data Model: Pinned Shortcut Creation

**Feature**: `002-shortcut-pinning` | **Date**: 2026-08-23

Nothing here is persisted by SlowLock. The spec's Assumptions are explicit: the draft lives
only while the screen is open, and once pinned the **launcher** owns the shortcut. There is no
database, no `DataStore`, no preferences file — and adding one would be a constitutional
deviation, not a design choice. The only thing that outlives the screen is the shortcut itself,
held by the system's `ShortcutManager`.

---

## `IconTreatment`

The named visual transformations, and the single source of truth for what each one does.

```kotlin
enum class IconTreatment {
    Original,
    Invert,
    Gray;

    val matrix: FloatArray?   // 4x5 colour matrix, null for Original
}
```

| Field | Type | Notes |
|---|---|---|
| `matrix` | `FloatArray?` | 4×5, row-major, **literal constants — no framework call**. `null` for `Original`: no filter is applied at all rather than an identity one |

**Values** (FR-005; derivation and the alpha-row trap in research.md R7):

| Case | Matrix |
|---|---|
| `Original` | `null` |
| `Invert` | `-1 0 0 0 255 / 0 -1 0 0 255 / 0 0 -1 0 255 / 0 0 0 1 0` |
| `Gray` | `0.213 0.715 0.072 0 0` on each of the three colour rows, `0 0 0 1 0` for alpha — the literal coefficients `ColorMatrix.setSaturation(0f)` produces |

**Rules**

- Exactly these three. Adding a fourth is out of scope for this feature (spec, Out of Scope).
- Declaration order is display order in the treatment row (FR-005).
- `Original` is the initial selection (FR-006) — it is `entries.first()`, so the ordering rule
  and the default rule cannot drift apart.
- The alpha row is identity in every case. Inverting it turns transparent icon corners into
  opaque black and yields a solid square.
- One matrix feeds both the preview (`ColorFilter.colorMatrix`) and the pinned bitmap
  (`ColorMatrixColorFilter`). That shared origin is what makes SC-003 structural: the preview
  and the pinned icon cannot disagree, because there is only one definition to disagree with.

**Lifetime**: compile-time constant.

---

## `ShortcutTarget`

The resolved facts about the app being configured. Produced at the boundary — everything past
this point is plain data, resolvable and testable without a device.

```kotlin
data class ShortcutTarget(
    val packageName: String,
    val label: String,
    val versionCode: Long,
)
```

| Field | Type | Notes |
|---|---|---|
| `packageName` | `String` | The identity. The only value that arrived across the seam from feature 001 |
| `label` | `String` | Localized, display only. Becomes the shortcut's label verbatim, no suffix |
| `versionCode` | `Long` | Icon-cache staleness marker (Constitution V), not identity |

**Rules**

- Built by re-resolving `packageName` against `PackageManager` — obligation C3 of feature 001's
  `contracts/selection-handoff.md`: display data is never carried across the seam.
- Resolution returns `null` when the package no longer resolves. That is the FR-015 path (the
  app was uninstalled while the screen was open), and the null-`getLaunchIntentForPackage()`
  case the constitution requires a unit test for.
- `label` is never a key, never matched on, never compared (Constitution V).
- No icon field. Icons are loaded separately through 001's `AppIconCache` and never travel
  inside state — the same rule `InstalledApp` follows, for the same reason.

**Lifetime**: resolved when the configuration screen opens; re-resolved when "Create shortcut"
is pressed, because the app can be uninstalled in between (FR-015).

---

## `ShortcutDraft`

What the user is composing. The spec's key entity; in code it is two values in composition, not
a stored record.

```kotlin
// held as composition state, not a persisted type
target:    ShortcutTarget
treatment: IconTreatment   // rememberSaveable
```

**Rules**

- `treatment` survives screen recreation (FR-008) via `rememberSaveable`; a Kotlin enum is
  `Serializable`, which the default saver handles.
- Nothing is written anywhere if the user backs out (FR-020, FR-021). There is no store to
  write to.
- The draft is per-target. Opening a different app starts a fresh draft at `Original` — the
  screen's state is keyed by package name, so one app's treatment cannot leak into another's.

**Lifetime**: exists only while the configuration screen is composed.

---

## `ShortcutSpec`

The frozen shape of what gets pinned, as pure data — deliberately split out from `ShortcutInfo`
so it can be asserted on the JVM without a `Context`. The normative contract is
`contracts/pinned-shortcut.md`; this is its in-code representation.

```kotlin
data class ShortcutSpec(
    val id: String,              // == target packageName
    val label: String,
    val targetPackage: String,   // carried in the intent extra
)
```

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | **Is** the target's package name (FR-025). One app, at most one shortcut |
| `label` | `String` | The target's label, verbatim, no marker (spec, Assumptions) |
| `targetPackage` | `String` | Written to `com.slowlock.shortcut.extra.TARGET_PACKAGE`; equals `id`, and is stated separately because the two are frozen for different reasons and could in principle diverge in a later feature |

**Rules**

- `id` is derived from the target, never generated and never stored — which is what makes
  re-pinning idempotent with no bookkeeping (FR-027).
- The intent is always `ACTION_VIEW` at `ShortcutLaunchActivity` — never the target app's own
  launch intent. Fixing behaviour at pin time would break FR-011 permanently.
- Produced by a pure function `shortcutSpec(target: ShortcutTarget): ShortcutSpec`, so the
  frozen values are unit-testable.

**Lifetime**: constructed per pin; the resulting `ShortcutInfo` is owned by `ShortcutManager`
from then on.

---

## `PinSupport`

Whether the current launcher accepts pin requests. Drives which screen the root shows.

```kotlin
sealed interface PinSupport {
    data object Unknown : PinSupport      // not yet checked this foreground pass
    data object Supported : PinSupport
    data object Unsupported : PinSupport
}
```

**Rules**

- Re-evaluated on every `Lifecycle.Event.ON_START` (FR-028), never cached across a background
  trip — the user may have changed launcher while away.
- `Unknown` renders nothing rather than guessing. Defaulting to `Supported` would flash the app
  list onto a device that cannot use it; defaulting to `Unsupported` would flash an error at
  everyone else.
- `Unsupported` makes both the list and the configuration screen unreachable (FR-029).
- Also checked immediately before the pin call (FR-013), because support can change while the
  configuration screen is open.

**Lifetime**: composition state, re-derived every foreground pass. Never persisted.

---

## Boundary mapping

Where platform types become plain data. Everything below the line is unit-testable.

```
LauncherApps / PackageManager
        │  resolve(packageName)                    ← IO dispatcher
        ▼
  ShortcutTarget?                                  ← null ⇒ FR-015
        │  + IconTreatment (user choice)
        ▼
  ShortcutDraft (composition state)
        │  shortcutSpec(target)                    ← pure
        ▼
  ShortcutSpec ──┬─► ShortcutInfo.Builder ─► updateShortcuts() ─► requestPinShortcut()
                 │
  cached icon ───┴─► treatment matrix ─► Bitmap    ← IO dispatcher
```

---

## What is deliberately absent

| Not modelled | Why |
|---|---|
| A record of which apps have shortcuts | FR-027 forbids needing one; identity is derived from the target, so re-pinning is idempotent without it. Any such record also goes stale silently when the user deletes a shortcut from their launcher — which the app cannot observe |
| Per-app delay or schedule | The configuration feature's, with its own spec (spec, Out of Scope) |
| Pin outcome / success state | FR-012: the screen closes and says nothing; confirmation is the launcher's. Modelling an outcome would invite showing it |
| A treatment applied to icon layers | Treatments apply to the fully rendered icon, adaptive background included (spec, Assumptions) |
