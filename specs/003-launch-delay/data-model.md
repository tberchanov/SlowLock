# Phase 1 Data Model: Launch Delay

**Feature**: `003-launch-delay` | **Date**: 2026-08-23

The first feature in this project that persists anything. Two entities outlive a screen — the
per-app configuration (on disk) and the pinned shortcut (owned by the launcher, unchanged from
feature 002). Everything else here is transient: a navigation stage that lives in
`rememberSaveable`, and a deadline that lives for the length of one wait.

The persisted shape is **frozen** and specified in `contracts/delay-config-store.md`. This
document is its in-design form, plus the transient types around it.

---

## `DelayConfig`

What SlowLock remembers about one target app. The spec's **App Delay Configuration**.

```kotlin
data class DelayConfig(
    val delaySeconds: Int,
    val treatment: IconTreatment,
) {
    companion object {
        const val DEFAULT_SECONDS = 10
        val DEFAULT = DelayConfig(DEFAULT_SECONDS, IconTreatment.entries.first())
    }
}
```

| Field | Type | Notes |
|---|---|---|
| `delaySeconds` | `Int` | Whole seconds, strictly positive. Not clamped to the slider's range on read — see Rules |
| `treatment` | `IconTreatment` | Feature 002's enum, reused unchanged. Persisted by **name** |

**Identity**: the target app's `packageName`, and nothing else (FR-016, Constitution V). It is not
a field of this record — it is the key the record is stored under, which is what keeps the record
free of an identifier that could disagree with its own location.

**Rules**

- At most one per app. Applying replaces the whole record (FR-015); there is no partial update.
- `DEFAULT_SECONDS` is one constant with two readers: the delay screen's opening value for an app
  never configured (FR-006) and the fallback for a shortcut with no configuration (FR-032). The
  spec requires those to be the same value; sharing the constant is how that is enforced rather
  than remembered.
- `DEFAULT`'s treatment is `IconTreatment.entries.first()` — `Original` — reusing feature 002's
  rule that the ordering and the default cannot drift apart.
- **Reads sanitise, they do not validate.** A missing key, a non-positive delay, or an
  unrecognised treatment token yields the default for that field rather than an error or an
  exception. A configuration file is a thing an older or newer build wrote; refusing to read it
  helps nobody, and the worst outcome of a bad value is the delay the user would have got anyway.
- **Reads do not clamp to the slider's range.** A stored 1 or 600 is returned as it is. The range
  in `DelayRange` is a constraint on what the *screen* can produce (R11), not on what the store
  may hold — which keeps a future range change from silently rewriting a value the user chose.

**Lifetime**: disk, until the user replaces it or clears the app's data. Survives force-stop,
reboot, and update (FR-017). Included in platform backup (R13).

---

## Persisted representation

```text
SharedPreferences file: "slowlock.delay-config"

  "com.instagram.android.delaySeconds"  → Int    30
  "com.instagram.android.treatment"     → String "Gray"
```

| Element | Value | Frozen because |
|---|---|---|
| File name | `slowlock.delay-config` | A renamed file is an empty file: every configured app silently reverts to the default delay |
| Delay key | `"<packageName>.delaySeconds"` | Same |
| Treatment key | `"<packageName>.treatment"` | Same |
| Treatment token | the `IconTreatment` constant's `name` | A renamed enum constant compiles clean and reverts every configured icon to `Original` at the next apply |

Two keys rather than one encoded value: an `Editor` commit is atomic across keys, so there is no
half-written record to parse, and a third field later costs a key rather than a format version.

Neither the presence of a pinned shortcut nor the fact that one was ever requested is recorded.
Feature 002's FR-027 rule stands — identity is derived from the target, never tracked — and a
record of what had been pinned would go stale the moment the user dragged an icon off their home
screen, which the app cannot observe.

---

## `DelayConfigStore`

The only route to the persisted state. Every function suspends and does its work on
`Dispatchers.IO` (FR-036, Constitution IV).

```kotlin
class DelayConfigStore(context: Context) {
    suspend fun load(packageName: String): DelayConfig            // never null — DEFAULT if absent
    suspend fun save(packageName: String, config: DelayConfig)    // replaces both keys atomically
}
```

`load` returning a non-null `DelayConfig` rather than `DelayConfig?` is deliberate: both callers
want the default when nothing is stored, and neither has anything different to do with the
knowledge that an app is unconfigured. FR-032 is then not a branch anybody has to remember to
write — an unconfigured shortcut waits the default because `load` said so.

**Not provided, on purpose**: no `delete`, no `contains`, no listing of configured packages, no
observable flow. Each is a route to a feature the spec puts out of scope, and none has a caller.

---

## `DelayRange`

The bounds of the slider, and the pure mapping between its position and a whole number of
seconds. Pure Kotlin, no framework — which is what lets `DelayRangeTest` assert the off-by-one in
`steps` without a device.

```kotlin
object DelayRange {
    const val MIN_SECONDS = 1
    const val MAX_SECONDS = 30
    const val STEP_SECONDS = 1

    val STOPS: Int          // 30  — (MAX - MIN) / STEP + 1
    val SLIDER_STEPS: Int   // 28  — STOPS - 2, the stops *between* the endpoints

    fun snap(seconds: Int): Int   // clamp to [MIN, MAX], then round to the nearest STEP
}
```

**Rules**

- Every reachable value is a whole number of seconds and a multiple of `STEP_SECONDS` (FR-005).
  At the current step of `1` those are the same statement: every whole second in the range is
  reachable, and `snap` is the identity inside it. The step survives as a constant because the
  range is provisional — a coarser one is a one-line change and nothing outside this object
  assumes either value.
- `SLIDER_STEPS` is `STOPS - 2` because Material's `Slider` counts the stops *between* the
  endpoints. Getting this wrong yields a slider that looks right and lands on the wrong values;
  it is derived rather than written down, and asserted.
- `snap` is what the screen applies to the slider's `Float` before it becomes state, so the
  displayed number, the stored number, and the slider's position are the same value at all times
  (FR-007).
- `DelayConfig.DEFAULT_SECONDS` (10) must be a reachable stop — it sits inside `1..30`. Asserted in `DelayRangeTest` — a
  default the slider cannot land on would make the readout disagree with the handle the moment
  the user touched it.
- These three numbers are the spec's provisional values (spec, Assumptions). They are cheap to
  change: nothing outside this object depends on them, and the store deliberately does not clamp
  to them.

---

## `Stage` — the navigation state

Transient. Held in `SlowLockRoot`'s `rememberSaveable`, so it survives rotation and process death
but nothing more.

```kotlin
sealed interface Stage {
    data object List : Stage
    data class Delay(val packageName: String, val seconds: Int, val treatment: IconTreatment) : Stage
    data class Shortcut(val packageName: String, val seconds: Int, val treatment: IconTreatment) : Stage
}
```

| Transition | Trigger | What carries |
|---|---|---|
| `List` → `Delay` | a row tap, **after** the store read completes (R3) | the saved configuration, or `DelayConfig.DEFAULT` |
| `Delay` → `List` | back affordance or system back (FR-010) | nothing; nothing is saved |
| `Delay` → `Shortcut` | "next" (FR-009) | the delay currently on the slider |
| `Shortcut` → `Delay` | back affordance or system back (FR-014) | the same delay, **not** re-read from the store |
| `Shortcut` → `List` | "Create shortcut", after the save and the pin request (FR-015) | nothing |

`seconds` and `treatment` live on the stage rather than inside the screens because FR-014 requires
the value the user chose on the way through to come back with them — a screen that owned it would
have lost it, and one that re-read the store would show the saved value instead of the chosen one
(R9).

`treatment` rides along from `Delay` so that the shortcut screen's opening selection is the saved
one (FR-013) without a second store read.

**Persistence**: `rememberSaveable` with a `listSaver`; `IconTreatment` is `Serializable`, and the
other two fields are primitives.

---

## `WaitDeadline`

One occurrence of the pause. The spec's **Wait**. Never stored on disk; carried across a
configuration change only.

```kotlin
// WaitTiming.kt — pure, so the arithmetic is testable without a device
fun deadlineFrom(nowElapsedMillis: Long, delaySeconds: Int): Long
fun remainingMillis(deadlineElapsedMillis: Long, nowElapsedMillis: Long): Long  // never negative
```

| Field | Type | Notes |
|---|---|---|
| deadline | `Long` | `SystemClock.elapsedRealtime()` at `onCreate` plus the delay, in milliseconds (R4) |
| target package | `String` | Read from the shortcut's intent extra, never from anywhere else (002's obligation L1) |

**Rules**

- Anchored in `onCreate`, **before** the configuration read, so a slow disk cannot extend the wait.
  The anchor is later than the user's tap, so the observed wait is never shorter than the
  configured delay (FR-037).
- Written to `onSaveInstanceState` and preferred over a fresh anchor on restore, so a rotation
  neither restarts nor extends the wait (FR-027).
- `elapsedRealtime`, not wall-clock: monotonic, immune to the user or the network changing the
  time (R4).
- `remainingMillis` never returns a negative value — a restored deadline that has already passed
  means hand off now, not `delay(-4)`.
- The deadline dies with the activity. There is no scheduled work, no alarm, and nothing that can
  fire after the wait is abandoned (FR-029, FR-035).

---

## What is not modelled

- **A schedule, time window, or weekday rule.** Out of scope (spec).
- **A list of configured apps.** The store answers per package; nothing needs the set, and the
  installed-apps list deliberately does not mark configured apps (spec, Assumptions).
- **Whether a shortcut exists for an app.** Unobservable and unnecessary — feature 002's FR-027.
- **Any record of a wait: when it happened, whether it was abandoned, how long the user lasted.**
  Statistics are out of scope, and collecting them would need storage the constitution's non-goals
  rule out.
