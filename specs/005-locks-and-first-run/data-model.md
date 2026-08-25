# Phase 1 Data Model: Locks Home & First Run

**Feature**: `005-locks-and-first-run`

Three kinds of thing: one new durable record, one new transient UI state, and one grown root state.
Nothing existing changes shape.

---

## 1. `LockList` — the durable record (new)

**File**: `app/src/main/java/com/slowlock/locks/LockList.kt` — pure Kotlin, no `android.*` imports.

```kotlin
/** FROZEN. */ internal const val LOCKS_FILE = "slowlock.locks"
/** FROZEN. */ internal const val LOCKS_KEY = "packages"
/** FROZEN. */ internal const val LOCKS_SEPARATOR = "\n"

internal fun locksFrom(stored: String?): List<String>
internal fun encodeLocks(packages: List<String>): String
internal fun withLock(packages: List<String>, packageName: String): List<String>
internal fun withoutLock(packages: List<String>, packageName: String): List<String>
```

| Rule | Requirement |
|---|---|
| `locksFrom(null)`, `locksFrom("")`, `locksFrom("   ")` → `emptyList()` | FR-007 |
| Blank entries are dropped; surrounding whitespace is trimmed; duplicates collapse keeping the **first** position | FR-007, FR-013 |
| Nothing on the read path throws | FR-007 |
| `withLock` appends when absent and **returns the list unchanged when present** — never moves it | FR-006, FR-013 |
| `withoutLock` removes every occurrence; removing an absent package is a no-op | FR-021 |
| Order is insertion order, and only `withLock`/`withoutLock` may change it | FR-006 |
| No delay, no treatment, no label, no activity, no `ComponentName` is stored here | FR-002, FR-005, Constitution V |

**Frozen** means the three constants above are asserted against literals in `LockListTest`. A
rename compiles clean and empties every user's Locks screen.

## 2. `LockStore` — the wiring (new)

**File**: `app/src/main/java/com/slowlock/locks/LockStore.kt`

```kotlin
class LockStore(context: Context) {
    suspend fun load(): List<String>
    suspend fun add(packageName: String)
    suspend fun remove(packageName: String)
}
```

- Every function suspends on `Dispatchers.IO` (FR-040, Constitution IV).
- `add`/`remove` are read-modify-write through `locksFrom` → `withLock`/`withoutLock` →
  `encodeLocks`, one `Editor`, `apply()`.
- It is the **only** class that opens `slowlock.locks`, mirroring obligation S1 of
  `contracts/delay-config-store.md`.
- It holds no logic of its own: everything decidable is in §1, where the JVM suite can reach it.

## 3. `Lock` — a row, assembled at read time (new, transient)

```kotlin
data class Lock(
    val packageName: String,     // identity, from LockStore
    val label: String?,          // null = unavailable (FR-020)
    val versionCode: Long,       // AppIconCache key only, never identity
    val delaySeconds: Int,       // from DelayConfigStore
    val treatment: IconTreatment // from DelayConfigStore
)

val Lock.isAvailable: Boolean get() = label != null
```

**Not persisted.** Its identity comes from `LockStore`; its values come from `DelayConfigStore`;
its display comes from `resolveShortcutTarget`. FR-005 is why the delay and treatment are read
rather than stored: there is exactly one copy of each on disk.

**No icon field** — the same rule `InstalledApp` and `ShortcutTarget` already follow. Icons travel
through `AppIconCache`, never inside state.

## 4. `LocksUiState` — the screen's state (new, transient)

```kotlin
data class LocksUiState(
    /**
     * False until the first read completes, and **never true again**. A refresh over an
     * already-populated list leaves its rows on screen (FR-015, FR-016) — the root renders
     * nothing only before there has ever been an answer, which is the rule
     * `PinSupport.Unknown` follows and for the same reason (research R4).
     */
    val loaded: Boolean = false,
    val locks: List<Lock> = emptyList(),
    val explainingRemoval: String? = null, // package whose removal explanation is showing
)
```

| Derived | Meaning |
|---|---|
| `!loaded` | Nothing is rendered at the root — **first read only** |
| `loaded && locks.isEmpty()` | **Intro screen** (FR-017, FR-019a) |
| `loaded && locks.isNotEmpty()` | **Locks screen**, `locks.size` rows (FR-009, FR-010) |

`explainingRemoval` lives in the state rather than in the row so the dialog survives recomposition
and so at most one can be open. It is not a pending action: SlowLock cannot remove a lock (FR-021),
so the dialog it opens explains how rather than offering to.

## 5. `Stage` — the root state (grown)

```kotlin
sealed interface Stage {
    data object Home : Stage
    data object List : Stage
    data class Delay(packageName, seconds, treatment, origin: Origin) : Stage
    data class Shortcut(packageName, seconds, treatment, origin: Origin) : Stage
}

enum class Origin { List, Home }
```

- `Home` is the **initial** stage, replacing `List` (FR-009, FR-017).
- `Origin` is the "flow entry point" entity from the spec: it is the only thing that differs
  between creating and editing, and it decides exactly one thing — where a back from the delay
  step goes (FR-023, US4 scenario 3). It carries no other meaning; the step counters read `2 / 3`
  and `3 / 3` on both paths (FR-029).
- `Shortcut` carries `Origin` only to hand it back to `Delay` on a back press.

**`StageSaver` grows to match**, keeping its existing rules: the discriminant is written
explicitly, the treatment is stored by `Enum.name`, an unrecognised token sanitises rather than
throwing, and an unrecognised discriminant restores as **`Home`** (it was `List`). The `Origin` is
saved by `name` with the same sanitising rule.

## 6. What does not change

| | |
|---|---|
| `DelayConfig`, `delayKey`, `treatmentKey`, `delayFrom`, `treatmentFrom` | FR-008 |
| `DelayConfigStore`'s file, keys, formats, and its `load`/`save` signatures | FR-008 |
| `IconTreatment`, its order, and its persisted names | FR-026 |
| `ShortcutContract`, `ShortcutPinner`, `ShortcutLaunchActivity`'s FQN | FR-026 |
| `WaitScreen`, `WaitTiming`, `Theme.SlowLock.Wait` | FR-027 |
| `InstalledApp`, `InstalledAppsSource`, `AppIconCache`, `AppListViewModel` | Out of scope |
| The eleven colour tokens | FR-033, SC-009 |
