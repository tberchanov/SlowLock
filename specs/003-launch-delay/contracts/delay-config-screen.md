# Contract: The Delay Configuration Screen, and the Widened Shortcut Screen

**Feature**: `003-launch-delay`

Two seams. The new screen between the app list and feature 002's shortcut screen, and the two
changes that screen forces on its neighbour.

Neither is frozen. Both are ordinary UI, expected to be revisited — unlike
`delay-config-store.md`, nothing here reaches disk or a launcher.

---

## The new screen

```kotlin
@Composable
fun DelayConfigScreen(
    packageName: String,
    seconds: Int,                       // pre-resolved by the caller — never read from the store here
    onSecondsChange: (Int) -> Unit,     // hoisted: the caller owns the value (R9)
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
)
```

### Obligations

| # | Obligation |
|---|---|
| D1 | Take the target by package name and nothing else. Label and icon are **re-resolved here**, never carried across the seam — feature 001's `contracts/selection-handoff.md` C3, and the app can be uninstalled while this screen is open (FR-002, FR-003) |
| D2 | Show which app is being configured: its icon and its label, resolved off the main thread |
| D3 | Render a Material 3 `Slider` bounded by `DelayRange`, with `DelayRange.SLIDER_STEPS` discrete stops, so every reachable value is a whole multiple of `STEP_SECONDS` (FR-005) |
| D4 | Show the current value as a number of seconds beside the slider, updating as it moves. Use the plurals resource, never string concatenation (FR-007) |
| D5 | Never own the chosen value. It arrives as `seconds` and leaves through `onSecondsChange`; the screen holds no `rememberSaveable` copy of it. This is what makes FR-014 hold — the value survives the trip to the shortcut screen and back because it never lived here (R9) |
| D6 | Apply `DelayRange.snap` to the slider's `Float` before reporting it, so the readout, the stored value, and the handle position can never disagree |
| D7 | Present a back affordance at the top, and honour the system back gesture through `BackHandler`. Both call `onBack`, and neither saves anything (FR-010, FR-020) |
| D8 | Present a "next" action that calls `onNext`. Nothing is written to the store here — applying happens on the shortcut screen (FR-015) |
| D9 | Never read or write `DelayConfigStore`. The caller has already read it (R3) |
| D10 | Never launch the target app. This screen configures; nothing in SlowLock's UI opens a target (spec, Assumptions) |
| D11 | No ViewModel. One async resolution and one hoisted `Int` — feature 002's R10 rule, unchanged |

### What the caller owes it

| # | Obligation |
|---|---|
| D12 | `seconds` is already the app's saved delay, or `DelayConfig.DEFAULT_SECONDS` when it has none. The screen must never open on a value it then has to correct (FR-006, FR-012) |
| D13 | The store read happens **before** the transition to this screen, so its first composition is already correct (R3) |
| D14 | This screen is reachable only where pinning is supported. Feature 002's root gate (its FR-028 to FR-032) still takes over the whole app when it is not (FR-004) |

---

## The widened shortcut screen

Feature 002's `contracts/shortcut-config-screen.md` describes the current seam. This feature
changes it in two ways.

### Before

```kotlin
@Composable
fun ShortcutConfigScreen(
    packageName: String,
    onDone: () -> Unit,     // every exit: created, backed out, system back
    modifier: Modifier = Modifier,
)
```

### After

```kotlin
@Composable
fun ShortcutConfigScreen(
    packageName: String,
    delaySeconds: Int,                    // NEW — saved with the treatment on apply
    initialTreatment: IconTreatment,      // NEW — the saved treatment, or Original
    onBack: () -> Unit,                   // NEW — replaces onDone for the two cancel paths
    onCreated: () -> Unit,                // NEW — replaces onDone for the apply path
    modifier: Modifier = Modifier,
)
```

### Why the single exit had to split

Feature 002's contract says the caller "cannot tell which and must not need to", and its reason
was sound: an outcome callback invites the confirmation message FR-012 forbids.

FR-014 makes the caller need to know — back returns to the delay screen, creating returns to the
list. **The original reason still holds and is unaffected**: neither callback says anything to the
user, and the screen still cannot distinguish a honoured pin from a declined one. What splits is
navigation, not feedback.

### Obligations added

| # | Obligation |
|---|---|
| C15 | Open with `initialTreatment` selected and previewed, rather than `IconTreatment.entries.first()`. Original remains the opening selection only because the caller passes it for unconfigured apps (FR-013) |
| C16 | On apply, write `DelayConfig(delaySeconds, treatment)` through `DelayConfigStore` **before** requesting the pin. The pin puts a system dialog in front of the user; the store write must not queue behind it, and a crash between the two must leave the configuration saved rather than an icon whose delay was never written (R10, FR-015) |
| C17 | Call `onCreated` only after the save and the pin request have both been issued; call `onBack` for the affordance and the system gesture (FR-014, FR-020) |
| C18 | Keep every existing obligation from feature 002's contract. The re-resolve before pinning, the support gate at the moment of the pin, the silence on success, and the frozen `shortcutSpec` derivation are all unchanged |

### Obligations explicitly unchanged

- The pinned shortcut's shape. Nothing in this feature touches
  `002-shortcut-pinning/contracts/pinned-shortcut.md`'s frozen values — verified deliberately,
  because the delay is read at tap time rather than carried in the intent (R1).
- Re-pinning stays idempotent, identity stays derived, and nothing is recorded about which apps
  have shortcuts (002 FR-025 to FR-027).
- The screen still shows no confirmation of any kind (002 FR-012).

---

## The root's obligations

`SlowLockRoot` owns the flow. Its obligations are the ones the two screens deliberately do not
have.

| # | Obligation |
|---|---|
| N1 | Read the store once, on selection, on `Dispatchers.IO`, and transition only when it answers (R3, D13) |
| N2 | Hold the chosen delay and treatment in the stage, so the trip to the shortcut screen and back preserves them (FR-014, R9) |
| N3 | Retain the list's `SaveableStateHolder` entry across the round trip and drop the other two on exit, so scroll position and search query survive two screens rather than one (FR-011) |
| N4 | Keep pin support gating everything. An unsupported launcher takes over the root, including the new screen (FR-004) |
| N5 | Say nothing to the user on any transition. Navigation is the only difference between the outcomes (002 FR-012) |
