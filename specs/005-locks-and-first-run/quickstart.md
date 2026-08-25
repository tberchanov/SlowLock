# Quickstart: Locks Home & First Run

**Feature**: `005-locks-and-first-run`

What to read, in what order, and the handful of rules that are easy to break by accident.

---

## Read first

1. [`contracts/lock-store.md`](./contracts/lock-store.md) — the one new durable thing in the app.
   Three frozen literals. A rename empties every user's Locks screen.
2. [`contracts/root-navigation.md`](./contracts/root-navigation.md) — the transition table and the
   list of files this feature may touch.
3. [`contracts/locks-screen.md`](./contracts/locks-screen.md) — the two new screens.
4. [`data-model.md`](./data-model.md) — the exact signatures.
5. [`research.md`](./research.md) — why each of those is what it is. R8 in particular: the canvas
   was not readable from the planning session, so the new screens' metrics are derived and SC-007
   is the check that settles them.

Background, unchanged and still binding: `004/contracts/design-tokens.md`,
`004/contracts/ui-components.md`, `003/contracts/delay-config-store.md`,
`002/contracts/pinned-shortcut.md`.

## Build

```bash
./gradlew assembleDebug
./gradlew test
```

Both must pass before the feature is complete. There is **no** `connectedAndroidTest` in this
project and adding one is forbidden by the constitution.

## The five things easiest to get wrong

1. **Copying the delay into the lock record.** FR-005 forbids it. The record holds a package name.
   The delay and the treatment are read from `DelayConfigStore`, so there is one copy of each on
   disk and no way for the screens to disagree.
2. **Deriving the lock list from the configuration file's keys.** L7 forbids it. A configuration
   exists for any app whose flow was walked; a lock exists only for one the user finished — and
   deriving would resurrect locks the user had removed and invent them on upgrade.
3. **Adding a `Stage.Intro`.** N2 forbids it. The intro is what `Stage.Home` renders when the list
   is empty. Two stages let the saved stage disagree with the list, and turn "the last lock was
   removed" into a correction step instead of a consequence.
4. **Shipping the canvas subtitle verbatim.** "3 ON YOUR HOME SCREEN" claims something Android does
   not let the app verify. FR-011 and Constitution I. The count states the number.
5. **Making removal long-press-only.** TalkBack does not surface `onLongClick`. FR-041 needs the
   custom accessibility action, and unavailable rows need the visible control (K3, K4).

## Where the writes are

Exactly two call sites, and nowhere else:

- `LockStore.add` — inside `ShortcutConfigScreen.create()`, between `store.save(…)` and
  `pinner.pin(…)`. The order is the contract (L6).
- `LockStore.remove` — the Locks screen's confirmed removal (FR-021).

Nothing on the delay or icon screen writes. An abandoned edit therefore leaves everything as it was
with no rollback path (N7).

## Testing

JVM only. Two new files:

**`LockListTest`** — the three frozen constants against literals; encode/decode round trip;
`null`/empty/blank/malformed → empty; blanks dropped and entries trimmed; duplicates collapse to
first position; `withLock` appends when absent and is a no-op when present; `withoutLock` removes
and is a no-op when absent; order stable across an edit.

**`LocksViewModelTest`** — with the platform lookups injected as lambdas, mirroring
`AppListViewModel`: an empty list yields the intro condition; a resolvable package yields an
available row with the stored delay and treatment; **a package whose launch intent or label
resolves to null yields an unavailable row and does not throw** (FR-020, and the constitution's
null-`getLaunchIntentForPackage()` obligation); a package with nothing stored yields
`DelayConfig.DEFAULT`'s values.

**The ten existing test files must pass unmodified** (FR-042; the spec says eight — research R9
records the undercount). They are the guard that a rename reached an identifier rather than a
user's home screen.

## Manual verification

Required before the feature is complete: a written, numbered, requirement-traceable
`manual-test-plan.md`, produced in the tasks phase. **The maintainer runs it.** An agent must not
drive the connected device to pre-verify a case — it states which cases need running and waits.

The cases that can only be seen on a device: what a launcher does with the pin request, that a
removed lock's home-screen icon still waits and still opens the app (SC-012), the two new screens
against their artboards (SC-007), and the largest font scale on the smallest supported screen
(SC-008).
