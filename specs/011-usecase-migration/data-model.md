# Phase 1 Data Model: Use Cases Hold the Logic

**Date**: 2026-08-27 · **Plan**: [plan.md](./plan.md) · **Research**: [research.md](./research.md)

No persisted shape changes. The four frozen values are untouched (FR-015). What follows is the
in-memory structure: six use cases added, four interfaces changed, one new seam pair, one new UI
input type, and what is deleted.

---

## Use cases added

All six are `class … @Inject constructor(...)` in their feature's `domain` package, with
`operator fun invoke`. None is `@Singleton`, none needs a Hilt module (R3).

### `feature/apps/domain/LoadInstalledAppsUseCase`

```text
LoadInstalledAppsUseCase(
    apps: InstalledAppsRepository,
    @OwnPackageName ownPackage: String,
    locale: CurrentLocale,
)
suspend operator fun invoke(): List<InstalledApp>
```

Enumerate, exclude self, dedupe by package, sort by label under the locale read now — in that order
(FR-001). Calls the existing pure `excludeSelf`, `dedupeByPackage`, `sortedByLabel`; rewrites none of
them. The locale is read inside `invoke`, never cached, which is what makes a language change
re-collate on the next load.

### `feature/apps/domain/FilterAppsUseCase`

```text
FilterAppsUseCase()
operator fun invoke(apps: List<InstalledApp>, query: String): List<InstalledApp>
```

Substring, case-insensitive, original order preserved; a blank query returns the input unchanged
(FR-008). **Not `suspend`** — it takes no repository and touches no source, and it is invoked
synchronously inside the state derivation of R5. That asymmetry with the other five needs a comment.

### `feature/locks/domain/LoadLocksUseCase`

```text
LoadLocksUseCase(
    lockOrder: LockOrderRepository,
    pinnedShortcuts: PinnedShortcutsRepository,
    config: DelayConfigRepository,
    targets: AppTargetRepository,
)
suspend operator fun invoke(): List<Lock>
```

Read the pinned set; `null` means the launcher could not be asked, so the stored order stands and
nothing is pruned (FR-003). Otherwise reconcile with `deriveLocks` and write back through
`saveOrder` only when the order changed (FR-002). Then assemble rows with `assembleLocks` (FR-004).

### `feature/shortcut/domain/WaitDecisionUseCase`

```text
WaitDecisionUseCase(
    targets: AppTargetRepository,
    config: DelayConfigRepository,
    clock: ElapsedClock,
)
suspend operator fun invoke(
    target: String,
    anchorMillis: Long,
    storedDeadlineMillis: Long?,
): WaitDecision
```

Resolve first — never make someone wait for an app that was already gone (W5). Then the deadline:
`storedDeadlineMillis` wins if present, otherwise `deadlineFrom(anchorMillis, delaySeconds)`. Then
`remainingMillis` against the clock (FR-005).

### `feature/shortcut/domain/CreateLockUseCase`

```text
CreateLockUseCase(
    targets: AppTargetRepository,
    config: DelayConfigRepository,
    support: PinSupportRepository,
    icons: AppIconRepository,
    pins: ShortcutPinRepository,
)
suspend operator fun invoke(
    packageName: String,
    delaySeconds: Int,
    treatment: IconTreatment,
): CreateLockResult
```

Re-resolve, write the configuration, then request the pin — in that order, and the order is the
contract (FR-006). Holds the support gate (`pinWhenSupported`, moved here from `ShortcutPinner.kt`)
and the icon load, both of which used to sit inside the repository (FR-009).

### `feature/delay/domain/LoadDelayConfigUseCase`

```text
LoadDelayConfigUseCase(config: DelayConfigRepository)
suspend operator fun invoke(packageName: String, editedSeconds: Int?): DelayConfig
```

`editedSeconds` wins over the saved delay when present; the treatment always comes from the read
(FR-007).

---

## Types added

| Type | Where | What it is |
|---|---|---|
| `WaitDecision` | `feature/shortcut/domain` | `Unavailable` \| `Wait(deadlineMillis: Long, remainingMillis: Long)`. |
| `CreateLockResult` | `feature/shortcut/domain` | `Created(pin: PinRequestResult)` \| `TargetMissing`. |
| `CurrentLocale` | `core/domain` | `fun interface { fun now(): Locale }`. `ElapsedClock`'s shape, for the same reason (R9). |
| `CurrentLocaleSource` | `core/data` | Reads `context.resources.configuration.locales.get(0)`, falling back to `Locale.getDefault()` — the exact body deleted from `InstalledAppsSource`. |
| `@OwnPackageName` | `core/domain` | Hilt qualifier on a `String`, beside `@IoDispatcher` in `Dispatchers.kt`. |
| `AppListInputs` | `feature/apps/ui` | `(isLoading: Boolean, apps: List<InstalledApp>, query: String)` — what the holder stores, from which `AppListUiState` is derived (R5). |

---

## Interfaces changed

### `LockOrderRepository` (`feature/locks/domain`)

| Before | After |
|---|---|
| `suspend fun loadOrder(): List<String>` | unchanged |
| `suspend fun deriveOrder(pinned: Set<String>): List<String>` | **removed** |
| — | `suspend fun saveOrder(order: List<String>)` **added** |

The store stays the only writer of `slowlock.locks`; the decision about *what* to write moves out
(R4). The `deriveOrder` KDoc's warning — never pass an empty set standing in for "could not ask" —
moves to `LoadLocksUseCase`, which is now the only place that can make that mistake.

### `ShortcutPinRepository` (`feature/shortcut/domain`)

| Before | After |
|---|---|
| `suspend fun requestPin(target: AppTarget, treatment: IconTreatment): PinRequestResult` | `suspend fun requestPin(target: AppTarget, treatment: IconTreatment, icon: ImageBitmap)` |

The icon is handed in and the return becomes `Unit` — the two refusals it used to report are decided
before it is called (R1). `PinRequestResult` itself is unchanged and is now produced by
`CreateLockUseCase`.

### `InstalledAppsRepository` (`feature/apps/domain`)

Signature unchanged: `suspend fun load(): List<InstalledApp>`. **Its contract inverts.** It now
returns the raw enumeration — every launcher activity, SlowLock included, in whatever order the
platform gave. The obligations list in its KDoc says the opposite today and is rewritten (FR-021).

### `CoreDataModule` (`core/data`)

Gains `@Binds` for `CurrentLocale` and an `@Provides @OwnPackageName fun ownPackageName(): String`.
`Dispatchers.IO` and `Dispatchers.Default` stay named exactly once, as D1 requires.

---

## What is deleted

| Deleted | From |
|---|---|
| `.excludeSelf(...).dedupeByPackage().sortedByLabel(...)` chain and `currentLocale()` | `InstalledAppsSource` |
| `VersionCodeLookup` | **stays** — it memoizes one source's own reads, not a rule |
| `deriveOrder` and its `deriveLocks` call and conditional write | `LockOrderStore` |
| The four-source body of `refresh()` | `LocksViewModel` |
| `visibleApps` getter | `AppListUiState` |
| `pinWhenSupported` free function | `ShortcutPinner.kt` (moves to `CreateLockUseCase`) |
| `icons` and `support` constructor parameters, the `pinWhenSupported` call, the `IconUnavailable` branch | `ShortcutPinner` |
| The deadline and resolve body of `run()` | `WaitViewModel` |
| The re-resolve, save and pin body of `create()` | `ShortcutConfigViewModel` |
| The `edited ?: saved.delaySeconds` branch | `DelayConfigViewModel` |

Nothing is deleted from `domain`. Every pure function this feature relies on already exists.

---

## Holders after the change

| Holder | Injects after | Keeps |
|---|---|---|
| `AppListViewModel` | `LoadInstalledAppsUseCase`, `FilterAppsUseCase`, `AppTargetRepository`, `AppIconRepository`, `SavedStateHandle` | `onAppTapped`'s resolve-and-react (FR-011), the query mirror, the message channel, `icons` exposed for lazy rows |
| `LocksViewModel` | `LoadLocksUseCase`, `AppIconRepository` | The explanation dialog state, `icons` exposed |
| `WaitViewModel` | `WaitDecisionUseCase`, `SavedStateHandle` | Every handle read and write, the same-target and different-target branches, `waitJob`, the `delay()`, the event channel |
| `ShortcutConfigViewModel` | `CreateLockUseCase`, `AppTargetRepository`, `AppIconRepository`, `SavedStateHandle` | `start()`'s two reads, the treatment selection and its restore rule, the message channel |
| `DelayConfigViewModel` | `LoadDelayConfigUseCase`, `AppTargetRepository`, `AppIconRepository`, `SavedStateHandle` | `start()`'s target and icon reads, the edit mirror |
| `RootViewModel` | `PinSupportRepository` | Everything. One call, no rule (R10). |

`AppListViewModel`, `ShortcutConfigViewModel` and `DelayConfigViewModel` keep a repository each
because the calls they make carry no rule. That is FR-011 working, not FR-009 being missed.
