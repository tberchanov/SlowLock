# Contract: Screen Inventory

**Feature**: `004-visual-redesign`

Five screens change. For each: what it must become, and — more importantly — **what must not
change**, because this is a presentation feature and every behavioural difference is a regression
until proven otherwise.

The right-hand column is the one to read during review.

---

## S1 — App list · `apps/AppListScreen.kt`

| Becomes | Must not change |
|---|---|
| `ScreenHeader("Choose an app", onBack = null)` — no back tile, no step counter (FR-010, U1) | The `AppListViewModel` and everything it owns: enumeration, sorting, the query, `onAppTapped` |
| Search field 52dp, 14dp radius, `Card` fill, `Line` border, existing placeholder (FR-011) | Search behaviour, the clear affordance, and the query surviving in `SavedStateHandle` |
| Rows 64dp, icon 44dp at `small` radius, 1dp **`Fill`** divider between rows — *not* `Line` (FR-012) | Scroll position surviving the round trip through the delay and icon screens (003 FR-011) |
| Loading, empty and no-results states restyled; wording unchanged (FR-013) | The unavailable-app snackbar and its dismissal |
| Icon placeholder uses `Fill` | `AppIconCache` and its `packageName` + `versionCode` key (Constitution V) |

**Note**: the canvas draws a back tile and `1 / 3` on this screen. Both are Phase 2 (Out of Scope).

---

## S2 — Delay · `delay/DelayConfigScreen.kt` + `delay/DelayRange.kt`

**The only screen in the feature with new behaviour.**

| Becomes | Must not change |
|---|---|
| `ScreenHeader("Wait before opening", onBack)` (U1) | `onSecondsChange` remaining the only way the value leaves this screen — it owns no state |
| App pill above the readout: `pill` shape, `Card` fill, `Line` border, icon + label (FR-015) | The value carried forward, and restored when returning from the icon step (FR-021, 003 FR-014) |
| Readout: `readout` role, ≤104sp, shrinks to fit, `SECONDS` caption in `mono` (FR-014, C11) | Back returning to the list |
| Slider keeps M3 `Slider` with custom `track`/`thumb` slots; `1s`/`30s` end labels (FR-016, research R8) | `DelayRange.MIN/MAX/STEP/STOPS/SLIDER_STEPS` and the existing `snap` mapping |
| **New**: preset row of 5s / 10s / 30s in `selectableGroup`, built from `SelectableTile` (FR-017, U4) | The unavailable-target message and when it shows |
| Primary action "Choose the icon" (FR-020) | That this screen never reads or writes the configuration store — it is handed its value (003 D9) |

**New API on `DelayRange`** — additive only:

```
val PRESETS: List<Int>          // [5, 10, 30]
fun presetFor(seconds: Int): Int?
```

Covered by `DelayRangeTest`: the literals, membership in `MIN..MAX`, `snap`-stability, and that
`presetFor` returns nothing for a non-preset (data-model §4).

**Selection is derived, never stored.** There is no "selected preset" variable. Dragging to 17
seconds highlights nothing because `presetFor(17)` is null — not because a flag was cleared.

---

## S3 — Icon & create · `shortcut/ShortcutConfigScreen.kt`

| Becomes | Must not change |
|---|---|
| `ScreenHeader("New lock", onBack)` — **title deliberately diverges from the canvas**, which still reads "New shortcut"; the terminology decision post-dates the artboard (FR-041, research R12) | Which treatment the screen opens on: `initialTreatment`, from the store via the stage (003 FR-013) |
| Preview card: `extraLarge` radius, `Card` fill, `Line` border, 96dp icon, label, delay in `mono` (FR-022) | The `create` path — resolve fresh, save, pin, then `onCreated` — and its ordering |
| `ICON` eyebrow in `mono`, capitalised in the resource (FR-023, C8) | The action staying disabled while the icon is unavailable |
| Three `SelectableTile`s in a `selectableGroup` replacing the `FilterChip` row (FR-023, U4) | `IconTreatment` and its `matrix` — the enum, its order, and its **persisted names** |
| Selected tile: `AmberWash` fill, `Amber` border (FR-024) | Both failure messages, and the snackbar on a target that vanished mid-flow |
| Primary "Add to home screen" + `mono` footnote (FR-025) | `ShortcutPinner`, `ShortcutContract`, and the pinned intent (`contracts/pinned-shortcut.md`) |

**The treatment tiles must not lose what the chips carried.** `FilterChip` supplied selection
semantics for assistive technology; `SelectableTile` re-supplies them via `Modifier.selectable`.
Losing them is the specific regression this row exists to prevent (FR-043).

---

## S4 — Wait · `delay/WaitScreen.kt`

The tightest constraints in the feature. Restyled, **not re-mechanised**.

| Becomes | Must not change |
|---|---|
| Background `wait_background`, painted by the composable *and* the window (FR-030, C5) | `ShortcutLaunchActivity` — its FQN, `exported`, `excludeFromRecents`, `taskAffinity`, `singleTop` |
| 40×2dp `Amber` rule at 55% opacity above the message (FR-027) | The `onStop`-unless-configuration-change rule (003 W15) |
| Message `please wait`, lower case, `mono` role, `wait_text` (FR-027, FR-028) | The `elapsedRealtime` deadline and its survival through `onSaveInstanceState` |
| `values-night` variants of all three values (FR-031, C4) | `FLAG_KEEP_SCREEN_ON`, and its release with the window |
| Resolves its own colours and type, independent of `SlowLockTheme` (FR-033) | The null-launch-intent check, before the wait and again after it |

**Three hard rules**

1. **Nothing asynchronous.** No `LaunchedEffect`, no `produceState`, no store read, no icon load,
   no label lookup. The screen must arrive complete in one frame (FR-029, research R6), and it is
   forbidden to show any of that anyway (FR-032).
2. **Nothing animates.** No fade, no pulse, no progress, no countdown — for the whole duration.
3. **Uses no component from `ui/components`.** The isolation in FR-033 is the point: a change to
   the shared components must not be able to alter this screen.

---

## S5 — Unsupported launcher · `shortcut/PinUnsupportedScreen.kt`

| Becomes | Must not change |
|---|---|
| `mono` eyebrow at 12sp / 0.14em in **`AmberDark`**, capitalised in the resource, left-aligned (FR-034, C8) | `pinSupport()` and its re-evaluation on `ON_START` |
| Message left-aligned at 22sp in `body` (FR-034) | The re-check action calling the same evaluation the lifecycle does (003 U5) |
| `PrimaryAction` "Choose home screen app", then `SecondaryAction` "Check again" (FR-035, U2, U3) | The `runCatching` around the settings intent and the fallback message |
| Restyled to the palette | That this screen takes over the whole root when support is absent |

---

## S6 — Files that must not appear in the diff

A mechanical review aid. If any of these is modified, the feature has left its scope:

| File | Why |
|---|---|
| `SlowLockRoot.kt` | No stage is added. Phase 2's work (Out of Scope). |
| `shortcut/ShortcutContract.kt` | Frozen — written into pinned intents. |
| `shortcut/ShortcutPinner.kt`, `ShortcutTarget.kt`, `PinSupport.kt` | No behavioural change. |
| `delay/DelayConfig.kt`, `DelayConfigStore.kt` | Nothing persisted changes (FR-038). |
| `delay/WaitTiming.kt` | The wait's arithmetic is untouched. |
| `apps/AppListViewModel.kt`, `InstalledAppsSource.kt`, `AppIconCache.kt` | Enumeration and caching unchanged. |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | No dependency added (FR-039). |
| `AndroidManifest.xml` | No permission, no activity attribute change. |
| `res/mipmap-*/`, `res/drawable/ic_launcher_*` | The launcher icon is deferred with no phase assigned (Out of Scope). |
| `contracts/` of features 002 and 003 | Frozen (FR-038). |

**Test files that must keep passing unmodified**: `ShortcutContractTest`, `IconTreatmentTest`,
`ShortcutTargetTest`, `PinGateTest`, `DelayConfigTest`, `WaitTimingTest`, `InstalledAppTest`,
`AppListViewModelTest`. They are the guard on FR-038 and FR-042 — a rename that reached an
identifier fails here rather than on a user's home screen.
