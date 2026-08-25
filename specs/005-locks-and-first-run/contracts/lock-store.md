# Contract: Lock Store — FROZEN ON MERGE

**Feature**: `005-locks-and-first-run` | **Package**: `com.slowlock.locks`

The only genuinely new durable data in the app. This file is its authority, and it is written to
the same standard as `003/contracts/delay-config-store.md`: a rename here is not a migration, it is
every user's Locks screen going empty after an update they did not ask for.

---

## L1 — The frozen values

| | Value | Asserted by |
|---|---|---|
| File | `slowlock.locks` | `LockListTest`, against a literal |
| Key | `packages` | `LockListTest`, against a literal |
| Key | `removed` | `LockDeriveTest`, against a literal |
| Separator | `\n` | `LockListTest`, against a literal |
| Value type | `String` | |

**Neither key defines what a lock is.** A lock exists exactly when its shortcut is pinned
(FR-003a), and `LockStore` stores only what `getPinnedShortcuts()` cannot answer:

- `packages` — the last derived list, **in order**. It supplies row order (FR-006), since the
  pinned set has none, and it is the fallback when the launcher cannot be asked at all (FR-004a).
- `removed` — tombstones: packages the user removed in the app while their shortcut was still
  pinned (FR-021a). Renaming it forgets every in-app removal, and each hidden lock reappears on the
  next derivation.

The file is **separate from** `slowlock.delay-config`, which keeps its keys, its name and its value
formats exactly as feature 003 froze them (FR-008). No key is ever added to that file by this
feature.

## L2 — What a lock record holds

The package name and nothing else (FR-002, Constitution V). No label, no activity name, no
`ComponentName`, no delay, no treatment, no timestamp, no icon.

The delay and the treatment stay in `DelayConfigStore` and are read from there (FR-005). There is
exactly one copy of each value on disk, so the Locks screen and the delay screen cannot disagree.

## L3 — `LockStore` is the only reader and writer

No other class opens `slowlock.locks` — the same obligation `DelayConfigStore` carries for its own
file. Every function suspends and does its work on `Dispatchers.IO` (FR-040).

```kotlin
class LockStore(context: Context) {
    suspend fun load(): List<String>
    suspend fun add(packageName: String)
    suspend fun remove(packageName: String)
}
```

`add` and `remove` are read-modify-write, through one `Editor`, committed with `apply()` — no
caller may block on the disk.

## L4 — Reads sanitise; they never fail

Absent, empty, whitespace-only, or malformed reads as **no locks**. Blank entries are dropped,
entries are trimmed, and duplicates collapse to their first position. **Nothing on this path may
throw** (FR-007) — the wrong-type case is caught with `runCatching`, exactly as
`SharedPreferences.intOrNull` already does next door.

A recorded package that no longer resolves is **not** a read failure. It is a row, and FR-020 says
what it looks like.

## L5 — Order is insertion order

Stable across launches. `add` appends when the package is absent and **returns the list unchanged
when it is present** — editing a lock never moves its row (FR-006, FR-013, US4 scenario 5).

## L6 — When the record is written

**Completing the flow does not create a lock.** The pin request does, and only if the user accepts
(FR-003a). There is no `add`.

Exactly one call site for `clearRemoval`: `ShortcutConfigScreen`'s private `create()`, between
`store.save(...)` and `pinner.pin(...)` (research R3).

**The order is the contract**: re-resolve → save configuration → **clear any tombstone** → request
pin → `onCreated()`. The tombstone must go before the pin, because an app the user previously
removed while its icon stayed pinned is still pinned now — re-pinning changes nothing about the
shortcut, so without the clear the re-made lock would be suppressed by its own tombstone
(FR-021a). Nothing after `pin()` may condition it, because nothing after `pin()` is reported.

Exactly one call site for `remove`: the Locks screen's confirmed removal (FR-021). It writes a
**tombstone** and drops the package from the cached order; it cannot delete the lock outright,
because the lock is the pinned shortcut and Android offers no way to unpin one (FR-021a).

`derive` is the only other thing that changes what is on screen, and it is not a deletion — it is
the derivation itself, at the top of `LocksViewModel.refresh()`. Its one hard rule:
`pinnedShortcutIds` returns `null`, never an empty set, when the launcher could not be asked, and
`null` falls back to the cached list. An empty set is the claim that nothing is pinned; acting on a
failed read as though it were that claim would empty the screen (FR-004a).

Nothing else — not an uninstall, not a failed resolution, not a launcher change — may remove a lock
from the list (FR-004).

## L7 — No migration, no reconstruction

A user upgrading from a build without this feature has no `slowlock.locks` file. That reads as no
locks, they see the intro, and their already-pinned icons keep working untouched (FR-024).

**The system MUST NOT scan `slowlock.delay-config` to invent locks.** A configuration exists for
any app whose flow was walked; a lock exists only for one the user finished. Reconstructing would
also resurrect locks the user had removed.

**The launcher's pinned shortcuts, however, are exactly where locks come from** (FR-003a) — and
that changes the upgrade story for the better: a user upgrading with icons pinned by a build before
this feature sees those locks appear, because the shortcuts are still pinned and pinned is what a
lock means. Nothing is reconstructed and nothing is guessed; the icons were always the record.

## L8 — What this contract does not reach

- Anything in `003/contracts/delay-config-store.md`. Frozen there, untouched here.
- Anything in `002/contracts/pinned-shortcut.md`. `ShortcutLaunchActivity`'s fully-qualified name
  is written into every icon already on a user's home screen.
- Backup, sync, export, or any cross-device path. The app has none and this feature starts none.
