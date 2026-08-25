# Phase 0 Research: Locks Home & First Run

**Feature**: `005-locks-and-first-run` | **Date**: 2026-08-24

Each entry resolves one unknown the Technical Context raised. Numbering continues the project's
convention (R1… per feature, local to this file).

---

## R1 — Where the lock list is persisted

**Decision**: A **second** `SharedPreferences` file, `slowlock.locks`, holding **one** key,
`packages`, whose value is the ordered package names joined with `\n`.

**Rationale**:

- FR-008 and 003's `contracts/delay-config-store.md` freeze the configuration file's keys, name and
  value formats. Adding an enumeration key inside that file would give it a capability the
  maintainer's clarification explicitly withheld ("the configuration store keeps its frozen keys and
  gains no enumeration capability"), and would put a non-`packageName`-prefixed key into a namespace
  whose whole shape is "package name + suffix".
- A separate file is one `getSharedPreferences` call; the framework caches it per process. It adds
  no dependency, no module, no engine (Constitution II).
- **One key, not one key per lock.** `SharedPreferences` has no ordered multi-value type:
  `putStringSet` is explicitly unordered and would break FR-006, and a key-per-lock would need an
  index to order them — which is the single ordered value, spelled twice.
- **`\n` as the separator.** Android package names are `[A-Za-z0-9_]` segments separated by `.`;
  no legal package name can contain a newline, so the encoding is unambiguous and needs no escaping.

**Alternatives considered**:

| Alternative | Rejected because |
|---|---|
| Derive the list from the configuration file's keys | The maintainer's clarification rules it out, and it is wrong: a configuration exists for any app whose delay screen was opened and saved, not for any app whose lock was created. It would also resurrect locks the user removed (FR-004) and invent locks on upgrade (FR-024). |
| DataStore (Preferences or Proto) | A new dependency for one string, plus a `Flow` the root does not need. Constitution II's default answer is no. |
| Room / SQLite | An engine for a list of tens of strings. |
| A JSON blob holding delay and treatment too | Violates FR-005 — two places for the same value to live, and a way for them to disagree. |
| `putStringSet` | Unordered by contract. FR-006 requires stable order. |

## R2 — Making the frozen values testable on the JVM

**Decision**: Repeat feature 003's split exactly. `LockStore` holds **only** the
`SharedPreferences` wiring and the `Dispatchers.IO` hop; the file name, the key, the separator, and
the encode/decode/insert/remove rules are **pure functions** in `LockList.kt`.

**Rationale**: the unit suite runs with `isReturnDefaultValues = true`, so a test written against
`SharedPreferences` asserts nothing while appearing to pass. The constitution requires every frozen
persisted value to be asserted against a literal; only a pure function can carry that assertion.
This is the same reasoning, and the same shape, as `DelayConfig.kt` beside `DelayConfigStore.kt`.

## R3 — When a lock is recorded

**Decision**: inside `ShortcutConfigScreen`'s existing private `create()` suspend function,
between `store.save(...)` and `pinner.pin(...)`.

**Rationale**:

- FR-003 puts the lock's birth at flow completion and forbids it depending on the launcher dialog's
  outcome. `pin()` only *requests*; nothing after it observes an answer. Both a slot before and a
  slot after `pin()` satisfy that, so the tiebreak is ordering.
- FR-016 requires the Locks screen to show the new values by the time the user is returned to it.
  Writing inside the suspend function, before `onCreated()` navigates, makes that ordering
  structural rather than a race the Locks read has to win.
- It joins the write it belongs with: the existing comment on `create()` records that the
  configuration is written before the pin request "so that a launcher which pins asynchronously can
  never fire the shortcut before its delay exists on disk". The lock record inherits that argument
  unchanged.

**Alternative considered**: recording it in the root's `onCreated` callback. Rejected — it fires
after navigation is decided and would put the write in a race with the Locks screen's read, for no
gain: `ShortcutConfigScreen` already opens the configuration store, so no new seam is avoided.

## R4 — Intro versus Locks, without a persisted flag

**Decision**: **one** root stage, `Stage.Home`, which renders the intro or the Locks screen
according to the observed lock list. There is no `Stage.Intro`.

**Rationale**: FR-019a forbids a persisted "has been introduced" flag and makes "show the intro" a
derived predicate — `locks.isEmpty()`. Two stages would let the *saved* stage disagree with the
list: removing the last lock (US5 scenario 5) would leave `Stage.Locks` in the bundle with nothing
to show, and the code would need a correction step for a state that should never have existed. One
stage makes scenario 5 and FR-017 the same line.

**Consequence**: while the lock list has not yet been read, `Stage.Home` renders **nothing** — the
same rule `PinSupport.Unknown` already follows in the root, and for the same reason: rendering an
answer that is not one flashes the intro at a user who has ten locks.

## R5 — Where the lock list is read, and how the rows resolve

**Decision**: a new `LocksViewModel` (`AndroidViewModel`, activity-scoped via `viewModel()`),
following `AppListViewModel`'s shape — a `StateFlow<LocksUiState>`, refreshed on `ON_START`, with
platform lookups injected as lambdas so the JVM suite can exercise them.

One IO pass loads: the lock list, then each package's `DelayConfig`, then each package's
`ShortcutTarget` (label + version code, `null` = unavailable). **Icons are not loaded in that
pass** — each row loads its own through the existing `AppIconCache`, exactly as `AppListRow` does.

**Rationale**:

- `ON_START` is already the root's refresh hook for pin support, and it is what makes the list
  correct after a return from the flow, after an uninstall, and after a language change (FR-016,
  SC-006).
- A ViewModel survives rotation, so the resolved rows are not re-read on every configuration change.
- `resolveShortcutTarget(context, packageName)` already exists and already applies the same
  lowest-labelled-activity rule the app list uses, so a lock row and its app-list row cannot show
  different text. Reusing it is why FR-020's "cannot be resolved" needs no new logic: `null` is
  the answer and always has been.
- Splitting the icon out is FR-015: label resolution is a bounded handful of binder calls, icon
  rasterization is not, and the screen must not block on it.

**Alternative considered**: resolving each row's label lazily inside the row composable too.
Rejected — availability (FR-020) decides whether the row is *tappable*, so it cannot arrive after
the row is interactive.

## R6 — Removal without a long press

**Decision**: `Modifier.combinedClickable(onClick = edit, onLongClick = confirmRemove)` on
available rows, plus a **custom accessibility action** ("Remove lock") on every row via
`Modifier.semantics { customActions = … }`. Unavailable rows are **not** clickable and carry a
visible remove control instead.

**Rationale**: FR-041 requires the removal affordance to be reachable without a long press. TalkBack
does not surface `onLongClick` as an action a screen-reader user can reach by exploration; a custom
action appears in the local actions menu, which is the platform's own answer to this. The visible
control on unavailable rows is not redundancy — those rows have no tap target at all, so the custom
action would be their only route, and a row the user is told is broken should show its one exit.

**Alternative considered**: a trailing overflow button on every row. Rejected — the canvas draws
none, and it would put a second control inside a 64dp row for a P3 action.

## R7 — The step counter

**Decision**: `ScreenHeader` gains `step: Int?` (null = no counter), rendering `"%1$d / 3"` in the
`Footnote` role (JetBrains Mono 12sp) in `Ink40`, pushed to the trailing edge.

**Rationale**: U1 named this parameter and deferred it to this feature by name, on the grounds that
the counter is only honest once step 1 has a predecessor — which is what `Stage.Home` now gives it.
Mono because it is a number the user reads (C6). `Ink40` because it is a caption; it measures
4.74:1 on `Bone` and is already in `SlowLockPaletteTest`, so no pairing is added.

**The `3` is a literal in the string resource**, not a computed count of stages. There are three
steps because the design says three, and a formatter that derived it would break the moment a
non-step root state was added.

## R8 — The canvas was not machine-readable in this session

**Recorded honestly, because it bounds what this plan can claim.** `SlowLock Redesign.dc.html` is
not in the repository and no Claude Design MCP tool was available to this session, so the two new
screens' metrics are **derived from the frozen tokens in `004/contracts/design-tokens.md`**, not
measured off the artboards.

**Consequence**: every metric this plan fixes for the two new screens is inside C9's existing table
or is stated in `contracts/locks-screen.md` as a derived value. `SC-007` — "visually
indistinguishable from their artboards, judged side by side" — is a **maintainer manual check**, and
it is the gate that catches any derived value that turns out to be wrong. Any correction it produces
is a change to `contracts/locks-screen.md`, never to `design-tokens.md`.

## R9 — FR-042 undercounts the test files

The spec says "the eight existing unit test files". There are **ten**: the eight
`004/contracts/screen-inventory.md` §S6 lists, plus `DelayRangeTest` (003) and
`SlowLockPaletteTest` (004). The requirement's intent is "all of them, unmodified", and this plan
holds all ten to it. No spec edit is needed; this note is the record.
