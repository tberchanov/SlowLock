# Contract: Shortcut Configuration Screen

**Feature**: `002-shortcut-pinning`

Draft UI, expected to be replaced by the delay-configuration screen it is a stand-in for. What
it must not do is compromise `contracts/pinned-shortcut.md`, which is frozen.

---

## Signature

```kotlin
@Composable
fun ShortcutConfigScreen(
    packageName: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
)
```

One `String` in — the seam feature 001 hands across (`selection-handoff.md`). Label, icon, and
version code are re-resolved here, never carried across (obligation C3).

`onDone` is invoked for **every** exit — created, backed out, system back. The caller cannot
tell which, and must not need to: FR-012 says the app shows no confirmation, so "created" and
"cancelled" are the same event as far as navigation is concerned. A callback that reported the
outcome would invite exactly the message the spec forbids.

---

## Observable behaviour

| # | Behaviour | Requirement |
|---|---|---|
| C1 | Opens showing a back affordance at the top, the treatment row, the preview centred, and "Create shortcut" at the bottom | FR-003, FR-005, FR-009, FR-020 |
| C2 | The preview shows the target's icon and the target's label, at roughly home-screen proportions | FR-003 |
| C3 | The treatment row is horizontally scrollable and contains exactly Original, Invert, Gray, in that order | FR-005 |
| C4 | `Original` is selected on open | FR-006 |
| C5 | Tapping a treatment updates the preview within one frame, with no further action and no layout shift | FR-007, SC-004 |
| C6 | The preview always reflects the current selection | FR-004 |
| C7 | The selected treatment survives rotation and process recreation | FR-008 |
| C8 | "Create shortcut" pins a shortcut carrying exactly the icon and label in the preview | FR-010, SC-003 |
| C9 | After the pin request is issued the screen closes; no confirmation, snackbar, toast, or dialog is shown | FR-012 |
| C10 | The pin is not attempted unless support is confirmed at that moment | FR-013, Constitution IV |
| C11 | If the target no longer resolves on create, no shortcut is created and the user is told; the screen stays open | FR-015 |
| C12 | If the icon cannot be loaded, a neutral placeholder is previewed and "Create shortcut" is **disabled** with a short explanation. The screen never crashes and stays usable; backing out and returning retries the load, since icon failures are not cached | Edge case: icon fails to load |
| C13 | The back affordance and the system back gesture both exit without creating anything | FR-020, FR-021 |
| C14 | A long label is shown the way a launcher would — truncated, not distorting the preview | Edge case: very long label |
| C15 | No permission prompt is shown at any point | FR-023, SC-005 |
| C16 | Icon loading and bitmap treatment never block the main thread | FR-024 |

---

## What the screen must not do

| Must not | Why |
|---|---|
| Show any success or failure message after "Create shortcut" | FR-012 — confirmation is the launcher's. The app cannot tell a decline from a success and must not claim either |
| Pass an `IntentSender` to `requestPinShortcut` | The outcome the app has promised not to report |
| Track, store, or check whether this app already has a shortcut | FR-027. Re-pinning is idempotent because the ID is derived |
| Persist anything | Spec, Assumptions: nothing is persisted by this feature |
| Bake a bitmap on each treatment tap | SC-004's budget; the preview uses a `ColorFilter` (research.md R7) |
| Alter the label — no suffix, no marker | Spec, Assumptions. The treatment is what distinguishes the shortcut |
| Offer editing or removal of an existing shortcut | Spec, Out of Scope. Removal is the launcher's, and an accepted bypass path (Constitution I) |

---

## Contract: Root state

`SlowLockRoot` owns which of three screens is showing. Support is re-checked on every
`ON_START`, never cached across a background trip (FR-028).

| State | Shows | Requirement |
|---|---|---|
| `Unknown` | Nothing — not the list, not an error | Avoids flashing the wrong screen before the answer is known |
| `Unsupported` | The explanation screen, **in place of** the list. Neither the list nor the configuration screen is reachable | FR-029 |
| `Supported`, no selection | `AppListScreen` | FR-001 |
| `Supported`, selection | `ShortcutConfigScreen` | FR-001 |

Transitions: a row tap sets the selection (FR-001, replacing 001's interim launch); `onDone`
clears it (FR-012, FR-020, FR-021); support flipping to `Unsupported` at any point takes over
the root, configuration screen included (FR-013); flipping to `Supported` moves the user on
with no restart (FR-032).

Returning to the list restores the scroll position and the search query (FR-022). The list and
query survive in `AppListViewModel`; the scroll offset needs
`rememberSaveableStateHolder()` — see research.md R9 for why it is not free.

---

## Contract: Pinning-unsupported screen

| # | Behaviour | Requirement |
|---|---|---|
| U1 | Explains in a sentence or two what is wrong and what it means for the user | FR-030 |
| U2 | No technical vocabulary, no error codes, no API names | FR-030 |
| U3 | Offers a control that opens the system setting where the default launcher is chosen | FR-031 |
| U4 | If that setting cannot be opened, says so; the screen stays usable | research.md R11 |
| U5 | Offers a control that re-checks without a restart | FR-031 |
| U6 | Shown before the app list is ever reachable on such a device | FR-029, SC-009 |

---

## Changes to feature 001

The spec's Assumptions call these out; they are part of this feature's work.

| Artifact | Change |
|---|---|
| `MainActivity.kt` | `launchApp()` and its interim-proof documentation are removed. The root becomes `SlowLockRoot` |
| `contracts/selection-handoff.md` (001) | Marked consumed. The "Interim implementation: launch the target directly" section is superseded by this feature; the callback shape itself is unchanged — which was the point |
| `spec.md` (001), FR-009 / FR-018 | Annotated as superseded: a tap now opens the configuration screen |
| `manual-test-plan.md` (001), T1.12 / T1.16 | Re-written against the new tap behaviour |
| `AppListScreen.kt` | **Unchanged.** Swapping the launch for navigation without touching this file is the seam working as designed |
