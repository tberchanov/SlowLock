# Contract: Navigation Graph

**Date**: 2026-08-27 | **Spec**: FR-007 to FR-014 | **Plan**: [../plan.md](../plan.md)

One graph, four destinations, hosted by `MainActivity`. Every obligation below is either preserved
from an earlier feature or introduced by this one, and each is traceable to the requirement it
serves.

## Destinations

| Route | Renders | Start |
|---|---|---|
| `Home` | `IntroScreen` when the lock list is empty, `LocksScreen` when it is not — derived, never stored (FR-011) | ✅ |
| `AppList` | `AppListScreen` | |
| `DelayConfig(packageName)` | `DelayConfigScreen` | |
| `ShortcutConfig(packageName, delaySeconds, treatment)` | `ShortcutConfigScreen` | |

## Edges

| From | Action | To | Notes |
|---|---|---|---|
| `Home` (intro) | Start | `AppList` | |
| `Home` (locks) | New lock | `AppList` | |
| `Home` (locks) | Tap an available row | `DelayConfig(packageName)` | Unavailable rows carry no click target |
| `AppList` | Select an app that still resolves | `DelayConfig(packageName)` | A package that no longer resolves raises a message and drops the row; no navigation |
| `DelayConfig` | Next | `ShortcutConfig(packageName, delaySeconds, treatment)` | |
| `ShortcutConfig` | Shortcut created | `Home`, popping the flow | Everything above `Home` is popped |
| any | Back | the entry beneath | |

## Obligations

Numbered `G`, so a later reader can cite them the way earlier features' obligations are cited.

| # | Obligation | Source |
|---|---|---|
| **G1** | Back from `DelayConfig` reaches `AppList` when the flow was entered from the app list, and `Home` when an existing lock was tapped. Delivered by the back stack, not by a stored origin. | 003 FR-010, 005 FR-032, FR-013 |
| **G2** | Back from `ShortcutConfig` reaches `DelayConfig` showing the delay the user chose on the way through — not the value on disk. Delivered by the `DelayConfig` entry surviving beneath it. | 003 FR-014, FR-013 |
| **G3** | Back from `ShortcutConfig` discards the treatment chosen there. Delivered by that entry being popped. | 005 N3, FR-018 |
| **G4** | `AppList`'s scroll position and search query survive the round trip through `DelayConfig` and `ShortcutConfig`. Delivered by the `AppList` entry staying on the back stack. | 001 FR-017, 002 FR-022, 003 FR-011, FR-013 |
| **G5** | `AppList` opens fresh — no query, scrolled to the top — when it is entered after having been popped. **This is a change from today**, required by Principle II and approved as FR-002(a). No earlier requirement is violated: all four cited in G4 scope retention to the round trip. | FR-002(a), FR-015 |
| **G6** | Rotation and process death return the user to the destination they were on, with its arguments and its holder's saved state. Delivered by the library's own saved back stack plus each holder's saved-state handle. | 003 FR-008, 005 FR-032, FR-013 |
| **G7** | The system back gesture does exactly what the on-screen back control does, on every screen with one. Delivered by both resolving to the same pop; per-screen interception is removed. | 005 FR-030, FR-010 |
| **G8** | Back on `Home` leaves the app. Delivered by absence: nothing sits beneath the start destination, so the press is not consumed and the Activity finishes. | 005 FR-031, FR-013, research R12 |
| **G9** | Completing the flow returns to `Home` with everything above it popped, so back from there cannot re-enter the flow that was just finished. | 005 N3 |
| **G10** | No destination declares a deep link, and `ShortcutLaunchActivity` is not a destination. | FR-014, research R14 |
| **G11** | The graph cross-fades between destinations: `fadeIn()` entering and popping-in, `fadeOut()` exiting and popping-out. **A deliberate, maintainer-directed change from the pre-010 build**, which animated nothing — see research R10's reversal note. The library's own default slide is not used. | Maintainer direction, 2026-08-27 |
| **G12** | Each of the three flow steps still shows `1 / 3`, `2 / 3`, `3 / 3`. The step number is a screen's own constant, not derived from the back stack depth. | 005 FR-029 |

## The pin-support gate

`PinSupport` is read by `RootViewModel` above the graph and is **not** a destination (FR-012).

| Value | Rendered |
|---|---|
| `Unknown` | nothing — rendering an answer before there is one is the flash the state exists to prevent |
| `Unsupported` | `PinUnsupportedScreen`, taking over the whole app; the back stack is left untouched, so the user's place returns when support does |
| `Supported` | the `NavHost` |

It stays a gate because it is a whole-app precondition re-evaluated on every return to the
foreground, not a screen the user navigates to — pushing and popping a destination in response to a
lifecycle signal is more mechanism than the behaviour needs. **The gate MUST carry a comment saying
so**, because the next reader will otherwise mistake the surviving `when` for the construct this
feature removed.

## Lifecycle hooks, and which owner each observes

| Hook | Where it sits after this feature | Whose lifecycle | Why |
|---|---|---|---|
| `refreshSupport()` on `ON_START` | above the graph, in `SlowLockRoot` | the Activity's | a whole-app precondition; first launch and every return to the foreground (005 FR-028) |
| `LocksViewModel.refresh()` on `ON_RESUME` | the `Home` destination | the `Home` entry's | fires on the pop back from the flow, which is what replaces the explicit wait (research R9) |
| `AppListViewModel.refresh()` on `ON_START` | the `AppList` destination | the `AppList` entry's | also fires on a pop back into the list — a redundant read, accepted (research R6) |
