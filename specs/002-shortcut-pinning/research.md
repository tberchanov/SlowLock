# Phase 0 Research: Pinned Shortcut Creation

**Feature**: `002-shortcut-pinning` | **Date**: 2026-08-23

Decisions taken before design. Every `NEEDS CLARIFICATION` from the Technical Context is
resolved here. Where a decision rests on platform behaviour that is easy to get wrong, the
primary source is cited — this is a feasibility draft, and a wrong assumption about the
shortcut APIs is exactly the failure mode the feature exists to rule out.

---

## R1 — Which pinning API

**Decision**: The platform `ShortcutManager` (`android.content.pm`), obtained via
`getSystemService(ShortcutManager::class.java)`. No compat wrapper.

**Rationale**: `requestPinShortcut()` landed in API 26; `minSdk` is 33, so every device this
app runs on has it. `ShortcutManagerCompat` (already on the classpath inside
`androidx.core`) exists to paper over API 25 and below — on API 33+ it delegates straight
through, adding an indirection that buys nothing.

**Alternatives considered**: `ShortcutManagerCompat` — free (no new dependency) but no
behaviour gained above `minSdk 33`; rejected under Constitution II. `ShortcutManagerCompat`
is worth revisiting only if `minSdk` ever drops below 26, which the constitution's fixed
stack does not contemplate.

---

## R2 — Detecting whether the launcher accepts pin requests (FR-028…FR-032)

**Decision**: `ShortcutManager.isRequestPinShortcutSupported()`, called on every
`Lifecycle.Event.ON_START` at the root of the composition. The result drives which screen the
root shows: unsupported → explanation screen, supported → app list.

**Rationale**: `ON_START` fires on first launch and again on every return to the foreground,
which is exactly the pair of moments FR-028 names, and it costs one cheap binder call — no
receiver, no polling, nothing running while the app is away (Constitution IV: zero battery at
rest). Because the check re-runs on return, a user who switches launcher and comes back lands
on the list with no restart (FR-032). The call touches neither disk nor network, so it is safe
on the main thread.

**Alternatives considered**: **Check once in `onCreate`** — cheaper still, but a user who
switches launcher must force-stop the app to recover, which FR-028 forbids. **Observe
`ACTION_PACKAGE_CHANGED` / role changes** — a receiver and a permission surface to detect
something one binder call answers on resume; rejected under Constitution II. **Query the
current home-role holder and match against a list of known-good launchers** — a maintained
blocklist of OEM launchers, wrong the moment a new one ships.

---

## R3 — Shortcut identity, and re-pinning the same app (FR-025…FR-027)

**Decision**: The shortcut ID **is** the target's package name, verbatim. Creating a shortcut
issues two calls, in this order:

1. `updateShortcuts(listOf(info))` — refreshes an already-pinned shortcut in place.
2. `requestPinShortcut(info, null)` — pins it if it is not pinned yet.

Neither call is conditional, and the app records nothing about what it has pinned before.

**Rationale**: This is Constitution V applied to the shortcut itself, and it is what makes
FR-027 (no bookkeeping) achievable — both calls are no-ops in the case they do not apply, so
"has this app been pinned before?" never has to be asked:

- `updateShortcuts` is documented as "Update all existing shortcuts with the same IDs. Target
  shortcuts may be pinned and/or dynamic, but they must not be immutable." A shortcut this app
  created is mutable, so a pinned one takes the new icon. An ID that was never pinned matches
  nothing and the call does nothing.
- `requestPinShortcut` on an ID the launcher already has pinned does **not** add a second
  icon and does **not** show the confirmation dialog. AOSP's `ShortcutRequestPinProcessor`
  short-circuits: *"When the shortcut is already pinned by this launcher, the request will
  always succeed, so just send the result at this point."*

So the first pin shows the launcher's dialog and adds one icon; every later pin of the same app
silently updates that icon. One app, at most one shortcut, no state kept — FR-025 through
FR-027 fall out of the identifier choice rather than being enforced by code.

`updateShortcuts` returns `false` when rate-limited, and the rate limit resets whenever the app
comes to the foreground. Every call here originates in a foreground tap, so the limit is not
reachable in practice; the return value is logged, not acted on.

**This is the second half of the accepted limitation in the spec**: an in-place update produces
no dialog and no new icon, so on that path the user sees the screen close and nothing else.
Recorded there deliberately, not to be filed as a bug.

**Alternatives considered**: **`packageName` + treatment name as the ID** — would let one app
carry an Original *and* a Gray shortcut, but breaks the one-app-one-shortcut relation the delay
configuration will need, and contradicts FR-025. **A generated UUID plus a stored map from
package to ID** — needs a persistence engine this feature is explicitly not allowed to add, and
the map goes stale the moment the user deletes a shortcut from their launcher, which the app
cannot observe. **`requestPinShortcut` alone, no `updateShortcuts`** — simpler by one line, but
the AOSP short-circuit above returns success *without* applying the new `ShortcutInfo`, so
re-pinning with a different treatment would leave the old icon in place and quietly fail FR-026.

**Sources**: [ShortcutManager.updateShortcuts](https://learn.microsoft.com/en-us/dotnet/api/android.content.pm.shortcutmanager.updateshortcuts?view=net-android-35.0)
· [AOSP `ShortcutRequestPinProcessor`](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/services/core/java/com/android/server/pm/ShortcutRequestPinProcessor.java)

---

## R4 — What the shortcut's intent points at (FR-011, FR-016)

**Decision**: The shortcut intent targets a SlowLock activity — never the target app's own
launch intent. The activity is `com.slowlock.shortcut.ShortcutLaunchActivity`, addressed by
explicit `ComponentName`, action `ACTION_VIEW`, carrying the target package name in the extra
`com.slowlock.shortcut.extra.TARGET_PACKAGE`.

**Rationale**: FR-011 is the one requirement in this draft that cannot be walked back, because
shortcuts pinned now stay on the user's home screen forever. A shortcut whose intent is the
target app's launch intent has its behaviour frozen at pin time: when the delay feature ships,
every such shortcut would still launch instantly, and the user would have to re-pin every icon.
Routing the tap through SlowLock means the delay arrives by changing this activity's body —
existing shortcuts inherit it with nothing asked of the user.

`ShortcutInfo.Builder` requires the intent to carry an action, hence `ACTION_VIEW`.

**The class's fully-qualified name is part of the persisted shortcut.** Renaming or moving
`ShortcutLaunchActivity` silently breaks every shortcut already on a home screen — the same
silent-breakage failure Constitution V catalogues for `ComponentName`s, arriving from the other
direction. Two mitigations, both cheap:

- The frozen names live in `ShortcutContract.kt` as constants, and a JVM unit test asserts the
  runtime FQN still equals the frozen string. A rename fails `./gradlew test` instead of failing
  on a user's home screen.
- If a rename ever becomes unavoidable, the escape hatch is an `<activity-alias>` under the old
  name — not a re-pin.

**Alternatives considered**: **The target app's launch intent directly** — one less activity and
no manifest change, but forfeits FR-011 outright. Rejected; this is the decision the feature
exists to get right. **A custom action with an `<intent-filter>` instead of an explicit
component**, so the class could be renamed freely — the action string just replaces the class
name as the frozen thing, and an `<intent-filter>` normally means `exported="true"`, widening
the launch surface to every app on the device for no gain (Constitution III). Rejected. **A
`PendingIntent`** — `ShortcutInfo` takes an `Intent`, not a `PendingIntent`.

---

## R5 — Whether the launch activity must be exported

**Decision**: `android:exported="false"`.

**Rationale**: The launcher does not `startActivity` the shortcut's intent itself — it calls
`LauncherApps.startShortcut()`, and the system starts the intent under the *publisher's*
identity: `mActivityTaskManagerInternal.startActivitiesAsPackage(publisherPackage, …)`, with the
AOSP source commenting *"Note the target activity doesn't have to be exported."* SlowLock is
starting its own activity, so no export is needed, and none is granted — the smallest surface
that works (Constitution III).

**Alternatives considered**: `exported="true"` — would also work and is what several tutorials
show, but hands every app on the device a way to invoke the launch path. Rejected. Because this
carries a "if I am wrong the shortcut is dead" risk, the manual test plan checks tapping a
pinned shortcut as its first case (M2.1), and flipping to `exported="true"` is the one-line
fallback if a launcher is ever found that does not go through `startShortcut`.

**Source**: [AOSP `LauncherAppsService.startShortcut`](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/services/core/java/com/android/server/pm/LauncherAppsService.java)

---

## R6 — Not leaving a SlowLock screen or a recents entry behind (FR-019)

**Decision**: `ShortcutLaunchActivity` is declared `android:excludeFromRecents="true"`,
`android:noHistory="true"`, `android:taskAffinity=""`, with a fully transparent theme, and calls
`finish()` immediately after starting the target. The target is started with
`FLAG_ACTIVITY_NEW_TASK`.

**Rationale**: Each attribute closes one way the user could see SlowLock where they expect only
their app: the empty `taskAffinity` keeps the activity out of SlowLock's main task (so the list
screen is not resurrected under it), `excludeFromRecents` keeps it out of the recents switcher,
`noHistory` stops it being restored if the user returns from the target, and the transparent
theme means nothing is ever drawn even for a frame. `FLAG_ACTIVITY_NEW_TASK` gives the target
its own task, matching the flag the interim launch path in feature 001 already uses.

The theme is presentation, not contract — it is *not* persisted in the shortcut, so the delay
feature is free to give this activity a visible countdown theme later without touching anything
a pinned shortcut carries (R4).

**Alternatives considered**: `@android:style/Theme.NoDisplay` — the traditional choice, but on
modern API levels an activity with it must finish before `onResume` or the system throws; the
translucent theme has no such trap. **A `BroadcastReceiver` or a service as the shortcut target**
— `ShortcutInfo` intents start activities, and Constitution IV forbids background activity
starts anyway.

---

## R7 — Applying the three icon treatments (FR-005…FR-007, SC-003, SC-004)

**Decision**: One `IconTreatment` enum owns a single 4×5 colour matrix per treatment, as a plain
`FloatArray`. That one array is used two ways:

- **Preview** — converted to a Compose `ColorFilter.colorMatrix(...)` and handed to `Image`. No
  pixels are copied; switching treatment is a recomposition of one node.
- **Pinned icon** — converted to an `android.graphics.ColorMatrix`, applied through a
  `ColorMatrixColorFilter` while drawing the source bitmap into a new bitmap, off the main
  thread, once, at create time.

`Original` carries no matrix at all (`null` filter), so the unmodified path draws nothing extra.

| Treatment | Matrix |
|---|---|
| Original | none — source pixels unchanged |
| Invert | `-1 0 0 0 255 / 0 -1 0 0 255 / 0 0 -1 0 255 / 0 0 0 1 0` — RGB inverted, **alpha row untouched** so transparent corners stay transparent |
| Gray | `0.213 0.715 0.072 0 0` on each colour row, alpha row identity — the luminance-weighted desaturation `setSaturation(0f)` produces, written out as literals |

**Rationale**: SC-004 gives treatment switching a 100 ms budget with no flicker. A `ColorFilter`
on the existing `ImageBitmap` costs no allocation and no I/O, so the preview updates within a
frame; baking a bitmap per tap would allocate on every selection and risk exactly the flicker
the criterion forbids. Deriving both the preview filter and the pinned bitmap from the *same*
matrix constant is what makes SC-003 ("the icon that lands matches the preview") structural
rather than something to be verified by eye — and because the matrices are pure data, they are
unit-testable on the JVM with no device.

The matrices are **literal constants, never computed by calling `android.graphics.ColorMatrix`**. Unit tests run with `isReturnDefaultValues = true`, under which every framework method returns a default — a Gray matrix derived from `setSaturation(0f)` would come back empty in the one place it is checked, and the test would assert nothing while appearing to pass. Keeping `IconTreatment` free of `android.*` imports keeps it honestly testable; the conversions to Compose `ColorFilter` and `ColorMatrixColorFilter` are one-liners at the two call sites.

Inverting the alpha row is the classic mistake: it turns a transparent background opaque black
and makes every inverted icon a solid square.

**Alternatives considered**: **Per-pixel loops** — slower, more code, and reimplements what the
platform's colour matrix already does correctly. **`RenderEffect` / `AGSL` shaders** — API 33
has them, but they are a heavier route to a colour matrix and do not produce the `Bitmap` the
pin path needs. **Bake on every selection and show the baked bitmap as the preview** — would
guarantee preview/pin parity even more literally, but at the cost of the 100 ms budget; parity
is already structural through the shared matrix.

---

## R8 — Producing the bitmap the launcher gets (SC-003)

**Decision**: Reuse feature 001's `AppIconCache` as the source of pixels
(`ImageBitmap.asAndroidBitmap()`), draw it through the treatment's colour filter into a **new**
`Bitmap` sized to `ActivityManager.getLauncherLargeIconSize()`, and wrap it with
`Icon.createWithBitmap`. All of it on `Dispatchers.IO` (FR-024).

**Rationale**: The cache is already keyed `packageName + versionCode` per Constitution V, already
rasterizes off the main thread, and is already what the preview draws — so the pinned icon and
the preview provably share a source. Drawing into a new bitmap rather than mutating the cached
one keeps the cache entry clean for the list screen. `getLauncherLargeIconSize()` is the size the
current launcher actually wants, and caps the bitmap at roughly 192×192 (~147 KB) — comfortably
inside the ~1 MB binder transaction limit that a full-resolution icon could threaten.

`createWithBitmap` rather than `createWithAdaptiveBitmap`: the cached bitmap is what an
`AdaptiveIconDrawable` already drew *through its own mask*, so handing it to
`createWithAdaptiveBitmap` would invite the launcher to mask an already-masked icon and crop the
edges. The cost is that some launchers frame a legacy icon in their own background plate — the
treatment stays visible, and matching a specific launcher's masking is out of scope for a draft
per the spec's Assumptions. The manual test plan records what each launcher does (M4.x), which is
the kind of finding this draft exists to gather.

**Alternatives considered**: **Re-rasterize from `LauncherApps` at pin time** — bypasses the
cache, duplicates code that already exists, and opens a window for preview and pinned icon to
disagree. **`createWithAdaptiveBitmap` over a full-bleed re-render of the adaptive layers** —
more faithful on launchers that mask, but needs separate handling of adaptive and legacy
drawables and a safe-zone-aware render; premature for a draft (Constitution II), and worth
revisiting once M4.x says how launchers actually behave.

---

## R9 — Navigating list → configuration → back, without a navigation library (FR-020…FR-022)

**Decision**: A `when` over root state in a `SlowLockRoot` composable, with the selected package
held in `rememberSaveable`, `BackHandler` for the system gesture, and a
`rememberSaveableStateHolder()` wrapping each screen. No `navigation-compose`.

**Rationale**: The app has three root states (unsupported / list / configuration) and one
transition each way. `navigation-compose` is a new dependency whose graph, routes, and argument
encoding all exist to solve problems this app does not have yet — Constitution II's default
answer is no, and nothing here breaks without it.

The one thing that does need care is FR-022. The app list and the search query already survive,
for reasons feature 001 built in: the list lives in `AppListViewModel`, scoped to the Activity's
`ViewModelStore`, which outlives the composable leaving composition; the query is mirrored into
`SavedStateHandle`. **Scroll position is the exception** — `rememberLazyListState` saves through
`rememberSaveable`, and a `rememberSaveable` inside a composable that leaves composition is
discarded unless something retains it. `rememberSaveableStateHolder()` is precisely that
something (it is the mechanism `NavHost` itself uses), it lives in
`androidx.compose.runtime.saveable` which is already on the classpath transitively, and it is
about four lines. Without it, returning from the configuration screen would drop the user at the
top of the list, failing FR-022 in a way that is easy to miss in a quick manual pass.

**Alternatives considered**: **`navigation-compose`** — as above; revisit when the app has a real
graph. **Hoist `rememberLazyListState` into the root and pass it into `AppListScreen`** — works
and adds nothing, but changes 001's screen contract to leak a `foundation` type to its caller for
a problem the state holder solves without touching the signature. **A second Activity for the
configuration screen** — the platform back stack would handle FR-020/FR-021 for free, but
`AppListViewModel` is Activity-scoped, so the list would be re-enumerated on every return: the
reload flash FR-017 forbade in feature 001, reintroduced here.

---

## R10 — Where the configuration screen's state lives (FR-008)

**Decision**: No ViewModel. `rememberSaveable` holds the selected treatment; a `produceState`
keyed on the package name resolves label, version code, and icon off the main thread; the pin
action runs in the composable's coroutine scope.

**Rationale**: The screen holds one enum and one async load. `rememberSaveable` survives rotation
directly (a Kotlin enum is `Serializable`, which the default saver handles), which is all FR-008
asks for. The 001 `AppListViewModel` earned its place by holding ~150 enumerated entries that
must not be re-fetched on rotation; re-resolving one label and one icon — the latter from a warm
disk cache — costs nothing worth a ViewModel, and adding one would be the speculative
architecture Constitution II names.

Testability is not sacrificed, because the parts worth testing are not stateful: the treatment
matrices, the frozen shortcut contract, and target resolution are pure functions taking injected
lambdas, unit-testable on the JVM exactly as `AppListViewModel`'s `resolveLaunchIntent` seam is.

**Alternatives considered**: **A `ShortcutConfigViewModel`** — symmetric with 001 and habitual,
but needs a factory to receive the package name plus a `viewModel(key = …)` to avoid one app's
state leaking into the next, which is more machinery than the state it protects. **Hoist the
treatment into `SlowLockRoot`** — survives rotation too, but puts one screen's private state in
the root's signature.

---

## R11 — Reaching the launcher setting from the unsupported screen (FR-031)

**Decision**: `startActivity(Intent(Settings.ACTION_HOME_SETTINGS))` inside `runCatching`. If it
throws `ActivityNotFoundException`, tell the user the setting could not be opened; the re-check
control is unaffected.

**Rationale**: `ACTION_HOME_SETTINGS` is the documented way to the default-launcher screen, but
some OEM builds and managed devices do not expose it — on a device already odd enough to refuse
pin requests, that is a live possibility. Catching is both simpler and more reliable than probing
with `resolveActivity()`, which would return null under package visibility unless a `<queries>`
entry were added — a manifest change bought for nothing (Constitution III).

The explicit re-check button (FR-031) is largely redundant with the `ON_START` re-check from R2,
and is present because a screen that only explains, with nothing to press, reads as a dead end —
the spec's Assumptions say as much.

**Alternatives considered**: `ACTION_SETTINGS` as a fallback when home settings is missing —
dumps the user at the root of Settings with no hint what to do; the message is more honest.
**`RoleManager.createRequestRoleIntent(ROLE_HOME)`** — offers to make *SlowLock* the launcher,
which is emphatically not what is wanted.

---

## R12 — Verification strategy

**Decision**: Manual-first, matching feature 001. `manual-test-plan.md` is the primary artifact;
the JVM suite covers what is pure and what the constitution mandates. No instrumented suite.

**Rationale**: The load-bearing questions here — does this launcher honour the pin, does the icon
that lands match the preview, does tapping the shortcut open the right app after a reboot — are
about *other apps' behaviour on real devices*. An instrumented test cannot install a second
launcher, cannot answer the system pin dialog, and cannot reboot; SC-008 asks for two vendors'
launchers, which is a device matrix, not a test suite. A human answers all of them in minutes.

What the JVM suite must cover:

| Test | Why |
|---|---|
| `IconTreatmentTest` — matrix values; Original has no filter; Invert leaves the alpha row identity; Gray is saturation 0 | The pure core of SC-003/SC-004; the alpha-row mistake in R7 is silent and permanent once pinned |
| `ShortcutContractTest` — `ShortcutLaunchActivity`'s runtime FQN equals the frozen constant; extra key frozen; shortcut ID equals the package name | Turns the R4 rename hazard from a home-screen failure into a build failure |
| `ShortcutTargetTest` — resolution returns null when `getLaunchIntentForPackage()` returns null, and the pin is not attempted | Constitution: "Target resolution and the null `getLaunchIntentForPackage()` path — unit tests" (FR-015) |
| `PinGateTest` — the pin path is not entered when support is reported false | FR-013, Constitution IV ("`isRequestPinShortcutSupported()` MUST gate every pin attempt") |

The constitution's instrumented-test obligation names `DelayActivity`'s countdown and hand-off.
No countdown exists in this draft; `ShortcutLaunchActivity` is its ancestor, and its hand-off is
covered by `ShortcutTargetTest` on the pure side and M2.3/M5.x manually. That gap is carried as an
**explicit, dated waiver in plan.md**, not as a clause that quietly does not apply — see
"Testing-expectations check". **When the delay feature adds the countdown, the obligation attaches
in full, and that feature does not inherit the waiver.**

**Alternatives considered**: **An instrumented test asserting `requestPinShortcut` was called** —
would assert that the app made a call, which is not in doubt; the doubt is what the launcher does
with it. **Robolectric** — a new dependency to fake the very platform whose real behaviour is the
question.
