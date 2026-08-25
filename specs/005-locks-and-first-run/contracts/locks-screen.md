# Contract: The Two New Screens

**Feature**: `005-locks-and-first-run` | **Package**: `com.slowlock.locks`

Both screens are built from `004/contracts/design-tokens.md` and `004/contracts/ui-components.md`.
Nothing here redefines a token; every metric below is either already in C9 or is **derived** and
marked so — see research R8 for why that distinction matters, and SC-007 for the check that
settles it.

---

## K1 — `IntroScreen` (canvas "New · First run")

```kotlin
@Composable fun IntroScreen(onStart: () -> Unit, modifier: Modifier = Modifier)
```

**Obligations**

- No `ScreenHeader`, no back tile, no step counter. It is a root (FR-031).
- States what the app does in plain language, and states the limits: **nothing is blocked and
  nothing is counted** (FR-018). That second half is a product commitment, not copy — the spec's
  permanent Out of Scope forbids statistics of any kind.
- Exactly one `PrimaryAction` (U2), "Set up a lock", calling `onStart` (FR-019).
- Every string is a string resource (FR-038). Any stylistic capitalisation is stored capitalised;
  no case transformation runs (FR-036, C8).
- Light palette regardless of the system setting (FR-037), inherited from `SlowLockTheme`.
- Stateless. It neither reads the lock list nor knows why it is showing.

**Derived metrics** (not in C9): body copy in the `Body` role at `Ink60`; any eyebrow in the
`Eyebrow` role at `AmberDark` (C2 — never `Amber` for a glyph).

## K2 — `LocksScreen` (canvas "New · Locks")

```kotlin
@Composable fun LocksScreen(
    state: LocksUiState,
    iconCache: AppIconCache,
    onNewLock: () -> Unit,
    onEdit: (packageName: String) -> Unit,
    onExplainRemoval: (packageName: String) -> Unit,
    onDismissExplanation: () -> Unit,
    modifier: Modifier = Modifier,
)
```

**Obligations**

- Title, a count, and one row per lock (FR-010).
- **The count states the number and nothing more.** The canvas subtitle "3 ON YOUR HOME SCREEN" is
  **not shipped**. Reconciliation (FR-004a) makes the list converge on the pinned icons, but it does
  not make the claim safe at any given moment: a launcher that does not unpin keeps reporting a
  shortcut that is gone, and a lock inside the not-yet-observed window has no icon yet. Asserting
  it anyway is exactly the claim Constitution I forbids (FR-011, spec Clarifications). Use a plural
  resource (`3 locks`), never a hand-built string.
- Exactly one `PrimaryAction` (U2), "+ New lock", calling `onNewLock` (FR-014).
- No search, no filter, no sort, no reorder, no drag handle, no per-lock toggle, no "pin again".
  All are permanently out of scope.
- Stateless: it holds nothing, loads nothing, and persists nothing. Every mutation leaves through a
  callback (U5).

**Row** — a card in a list, so `MaterialTheme.shapes.large` (18dp), the slot C9 reserved for this
screen by name. `Card` fill, `Line` hairline border.

| Element | Token / role |
|---|---|
| App icon | 44dp at `shapes.extraSmall`, from `iconCache.icon(packageName, versionCode)`; `Fill` placeholder while absent |
| Label | `RowLabel`, `Ink` — the app's **current** label, resolved fresh, never stored (FR-012, SC-006) |
| Delay | `Footnote` (mono), `Ink40` — every number the user reads is mono (C6) |
| Treatment | `Footnote` (mono), `Ink40`, from the existing `shortcut_treatment_*` resources |
| Row height | ≥64dp; grows with the font scale rather than clipping (SC-008) |

Labels truncate with an ellipsis; the delay and the treatment stay legible (spec edge case).

**The row must not block on its icon** (FR-015). The list renders as soon as the state is `Ready`;
icons arrive as they load, the same way `AppListRow` already does it.

## K3 — The unavailable row (FR-020)

A lock whose package does not resolve (`label == null`):

- **Is shown.** Never hidden — the user's home screen may still carry its icon.
- **Is not tappable into the edit flow**, and carries no `combinedClickable`.
- Names what is wrong, in a string resource, using the package name — the only thing left to
  identify it by.
- Renders its icon slot as the `Fill` placeholder.
- Carries a **visible** control opening the removal explanation, because it has no tap target for
  the long press to attach to (research R6).
- Must not crash the screen or the resolution pass.

## K4 — Removal is explained, never offered (FR-021, FR-022, SC-012)

**There is no remove action, and adding one is a defect.** A lock is its pinned shortcut (FR-003a),
Android offers no way to unpin one, and a button that only hid the row would leave the icon on the
home screen still waiting and still opening the app. What the screen offers is an explanation.

- Available rows: `Modifier.combinedClickable(onClick = onEdit, onLongClick = onExplainRemoval)`.
- **Every** row additionally carries a custom accessibility action, "How to remove this lock", so
  the explanation is reachable without a long press (FR-041, research R6).
- `state.explainingRemoval != null` shows one dialog. Container `Card`, title `Ink`, body `Ink60` —
  stated, not left to Material's derived defaults, which land outside the eleven.
- **One button**, "OK", wired to `onDismissExplanation`. `AlertDialog`'s `confirmButton` slot is
  mandatory, so "OK" goes there and `dismissButton` is left off rather than filled with a synonym.
- **The wording is the deliverable.** It MUST name the app, MUST say that removing the icon from
  the home screen is what removes the lock, and MUST say the user does that themselves. It MUST NOT
  imply SlowLock can remove the icon, MUST NOT imply anything happens on dismissal, and MUST NOT
  suggest the app is uninstalled (FR-022, Constitution I).

## K5 — `ScreenHeader` gains `step`

```kotlin
ScreenHeader(title: String, onBack: (() -> Unit)?, step: Int? = null, modifier: Modifier = Modifier)
```

- `step == null` renders no counter — the behaviour every existing caller keeps by default.
- Otherwise renders `"%1$d / 3"` from a string resource, `Footnote` role, `Ink40`, at the trailing
  edge of the header row. The `3` is a literal in the resource, never a computed stage count
  (research R7).
- The counter is decorative to assistive technology or is announced as part of the title; it is
  never a control.
- **The app list now passes `onBack`** (FR-028) — U1's "`onBack == null` renders no tile and no
  leading space" rule stays, and stops having a caller.

**New callers**: app list `step = 1`, delay `step = 2`, icon `step = 3` — on both the create and
the edit path (FR-029).

## K6 — What these screens must not do

- No colour or `sp` literal. Colour comes from `MaterialTheme.colorScheme`, type from
  `SlowLockType` (C1, C6). `SlowLockPaletteTest` enforces the colour half across the source tree.
- No twelfth colour token. A twelfth is a build failure, not a decision (FR-033, SC-009).
- No new text pairing that is not in `SlowLockPaletteTest` (C3, SC-010).
- No `uppercase()` on user-visible text (C8, FR-036).
- No dark palette (FR-037) — Phase 3.
- No new component in `ui/components` unless a **second** screen needs it (U5). The lock row has
  one caller and stays in `LocksScreen.kt`.
- No change to `WaitScreen` in any respect (FR-027).
- No bare root `Column`. Both screens are rooted in a `Scaffold` and apply its `contentPadding`
  before any padding of their own — the window-inset rule is `contracts/root-navigation.md` N11,
  and it binds every screen in the app, not only these two.
