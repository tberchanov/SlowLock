# Contract: State Scope

**Date**: 2026-08-27 | **Spec**: FR-015 to FR-021 | **Plan**: [../plan.md](../plan.md)

Every piece of state in the configuration app, what owns it, and what each of four events does to
it. Principle II's rule is that a screen's state lives while its entry is on the back stack and is
cleared when the entry is popped; this table is that rule made checkable.

The four events:

- **Rotate** — a configuration change. The holder survives; nothing is torn down.
- **Die** — process death while the user is on that screen, then reopening the app.
- **Pop** — leaving the screen for good, by back or by the flow completing.
- **Revisit** — opening the same screen again afterwards, possibly for a different app.

## Holders

### `RootViewModel` — Activity-scoped, deliberately

| State | Rotate | Die | Pop | Revisit |
|---|---|---|---|---|
| pin support | kept | re-read on `ON_START` | n/a — above the graph | n/a |

**Why it is not scoped to an entry, and why that is not a defect.** Pin support is not a screen's
state. It is a whole-app precondition that decides whether the graph renders at all, re-read on
every return to the foreground because a user can change launcher while the app is away. It has no
entry to belong to. Principle II's own sentence covers this case — state outliving its screen MUST
be deliberate and commented, naming the behaviour requiring it — so **the declaration carries that
comment** (FR-016). It is nonetheless recorded in the plan's Complexity Tracking, because
"activity-scoped holder" is the exact phrase the amendment made a defect and a reader is owed the
distinction.

**Never saved.** A restored answer could describe a launcher the user has since replaced (005
FR-028).

### `LocksViewModel` — `Home` entry

| State | Rotate | Die | Pop | Revisit |
|---|---|---|---|---|
| the lock list | kept | re-read on `ON_RESUME` | `Home` is the start destination and is not popped | n/a |
| `loaded` latch | kept | resets, then latches on the first read | | |
| removal explanation | kept | dropped — a dialog is not worth restoring | | |

Deliberately not saved into instance state: a saved list is a stale list, and it is one disk read
from being right.

### `AppListViewModel` — `AppList` entry

| State | Rotate | Die | Pop | Revisit |
|---|---|---|---|---|
| the installed list | kept | re-read on `ON_START` | cleared with the entry | re-read |
| search query | kept | kept — saved-state handle | **cleared** | **empty** |
| scroll position | kept | kept — the entry's saveable state | **cleared** | **top** |
| one-shot message | consumed once | not redelivered | | |

The last two rows are FR-002(a) and obligation G5 — a change from today, required by Principle II's
rule that a saved-state handle must not carry state across separate visits. Retention across the
round trip (G4) is unaffected, because the entry stays on the back stack while the flow is above it.

### `DelayConfigViewModel` — `DelayConfig` entry (new)

| State | Rotate | Die | Pop | Revisit |
|---|---|---|---|---|
| delay being edited | kept | **kept** — saved-state handle, and the disk read must not overwrite it | cleared | re-read from disk |
| saved treatment | kept | re-read | cleared | re-read |
| target | kept | re-resolved | cleared | re-resolved |
| icon | kept | re-loaded | cleared | re-loaded |
| `loaded` | kept | false, then true | cleared | false, then true |

The "die" row for the delay is the branch research R8 names and the only new logic in this feature
that a test can get wrong: **the handle wins over disk**.

Icons never enter saved state — a bitmap held for as long as the state is is exactly what 009's
icon rule forbids.

### `ShortcutConfigViewModel` — `ShortcutConfig` entry

| State | Rotate | Die | Pop | Revisit |
|---|---|---|---|---|
| **treatment selection** | kept | **kept** — saved-state handle, and the route argument must not overwrite it | **cleared** | **the new app's saved treatment** |
| target | kept | re-resolved | cleared | re-resolved |
| icon | kept | re-loaded | cleared | re-loaded |
| `creating` | kept | false | cleared | false |
| one-shot message | consumed once | not redelivered | | |

The treatment row is finding F-05's four obligations, now met by scope rather than by keeping the
value outside its holder. Every column is delivered by where the holder lives; none needs a manual
clear, a package-keyed reset, or a state holder outside the state holder.

### `WaitViewModel` — `ShortcutLaunchActivity`

Untouched. That activity hosts one screen and has no graph, so the Activity **is** the entry.
Scoping it to a navigation entry is not available and not wanted; routing that entry point through
`MainActivity`'s graph would put a frozen fully-qualified name at risk for nothing (research R14).

## Rules this contract asserts

| # | Rule | Requirement |
|---|---|---|
| **S1** | Every screen's holder is obtained inside its destination, so the entry is its scope. No holder is obtained above the `NavHost` except `RootViewModel`. | FR-015 |
| **S2** | Exactly one piece of state outlives every screen, and its declaration names the behaviour requiring it. | FR-016 |
| **S3** | A saved-state handle carries state through process death **within one visit** and never between visits. Two visits are two entries. | FR-017 |
| **S4** | A holder restoring from its saved-state handle must not be overwritten by a fresh read or by its route argument. | research R8 |
| **S5** | Each screen has exactly one state owner. After this feature no screen keeps state in composition that its holder should own. | 009 FR-036, FR-018 |
| **S6** | No holder exposes a repository on another screen's behalf. | FR-020 |
| **S7** | No holder is introduced that only forwards. `DelayConfigViewModel` qualifies because it owns the edited delay and the S4 branch; a holder that only passed values through would not. | FR-021 |
