# Data Model: Constitution Alignment Refactor

**Date**: 2026-08-26 | **Plan**: [plan.md](./plan.md)

This feature adds no persisted data and changes no stored shape. What follows is the structural
model instead: every current file, where it lands, which layer it belongs to, and what constrains
it. It is the checklist the Stage 3 move is executed and reviewed against.

## Entities

- **Capability** — one directory under `com.slowlock.feature`, named for a user-facing area.
  Four exist. A capability owns `ui`, `domain` and `data` subpackages; it may import `core`,
  `ui.components` and `ui.theme`, and nothing else outside itself.
- **Layer** — `ui`, `domain` or `data`. Dependencies point inward: `ui → domain ← data`. A
  `domain` file has no `android.*` import. A file belongs to exactly one layer.
- **Shared home** — `core` for cross-feature types and shared implementations; `ui.components` and
  `ui.theme` for the design system.
- **Entry point** — `MainActivity`, `SlowLockRoot`, `SlowLockApplication`, `ShortcutLaunchActivity`.
  Sits at the root package or directly in the capability it serves, never in a layer subpackage.
- **Frozen value** — see [contracts/frozen-values.md](./contracts/frozen-values.md). Constrains
  where its carrier may move.

## Migration map

Legend: **F** = carries or guards a frozen value. **S** = gains a repository seam. **N** = new file.

### Root package — `com.slowlock`

| Current | Target | Layer | Notes |
|---|---|---|---|
| `MainActivity.kt` | unchanged | entry point | Gains `@AndroidEntryPoint`. System-bar writer stays the app's only one. |
| `SlowLockRoot.kt` | unchanged | entry point | Keeps `stage` in `rememberSaveable` (FR-023a). Loses the store read and the platform call. |
| — | `SlowLockApplication.kt` **N** | entry point | `@HiltAndroidApp`; manifest gains `android:name`. |
| — | `RootViewModel.kt` **N** | presentation | Owns pin support and the pre-navigation config read. |

### `core`

| Current | Target | Layer | Notes |
|---|---|---|---|
| `delay/DelayConfig.kt` | `core/domain/DelayConfig.kt` | domain | Model plus the pure `delayFrom`/`treatmentFrom` sanitisers. Shared by `delay` and `shortcut`. |
| `shortcut/IconTreatment.kt` | `core/domain/IconTreatment.kt` | domain | Moves whole — **no split needed**: it has no `android.*` import, because its colour matrices are deliberately literal constants so the JVM suite can assert them. Used by three capabilities. Enum constant **names are frozen** — they are the persisted treatment token. **F** |
| `shortcut/ShortcutTarget.kt` (data class part) | `core/domain/AppTarget.kt` | domain | Renamed (R11). Pure data; no icon field, as today. |
| — | `core/domain/DelayConfigRepository.kt` **N S** | domain | |
| — | `core/domain/AppTargetRepository.kt` **N S** | domain | |
| — | `core/domain/AppIconRepository.kt` **N S** | domain | |
| — | `core/domain/Dispatchers.kt` **N** | domain | `@IoDispatcher`, `@DefaultDispatcher` qualifiers only. |
| `delay/DelayConfigStore.kt` | `core/data/DelayConfigStore.kt` | data | **F** — file name `slowlock.delay-config`, both key shapes, default-on-missing, `runCatching` type guards. |
| `shortcut/ShortcutTarget.kt` (resolution part) | `core/data/AppTargetSource.kt` | data | Split per FR-033: the pure type and the `PackageManager` lookup were one file. |
| `apps/AppIconCache.kt` | `core/data/AppIconCache.kt` | data | Keyed by package + version code, unchanged. |
| `compat/PackageCompat.kt` | `core/data/PackageCompat.kt` | data | `compat` disappears — not a capability name (FR-032). |
| — | `core/data/CoreDataModule.kt` **N** | data | Bindings for the three core repositories plus the dispatcher qualifiers. |

### `apps` — the installed-app list

| Current | Target | Layer | Notes |
|---|---|---|---|
| `apps/AppListScreen.kt` | `feature/apps/ui/AppListScreen.kt` | ui | Resolves the unavailable message from a resource id carried on the event (R7). |
| `apps/AppListViewModel.kt` | `feature/apps/ui/AppListViewModel.kt` | ui | `AndroidViewModel` → `ViewModel`; `@HiltViewModel`; lambda seams → repositories; `@JvmOverloads` gone. |
| `apps/AppListUiState.kt` | `feature/apps/ui/AppListUiState.kt` | ui | Loses `unavailableAppMessage`; derived getters unchanged. |
| `apps/InstalledApp.kt` | `feature/apps/domain/InstalledApp.kt` | domain | Model plus the pure dedupe/sort/exclude-self helpers. |
| — | `feature/apps/domain/InstalledAppsRepository.kt` **N S** | domain | |
| `apps/InstalledAppsSource.kt` | `feature/apps/data/InstalledAppsSource.kt` | data | Locale still read at load time, not cached. |
| — | `feature/apps/data/AppsDataModule.kt` **N** | data | |

### `delay` — per-app delay configuration

| Current | Target | Layer | Notes |
|---|---|---|---|
| `delay/DelayConfigScreen.kt` | `feature/delay/ui/DelayConfigScreen.kt` | ui | **No state holder, deliberately** — it owns no state, reads nothing, decides nothing (FR-023, Principle V). Icon arrives through `AppIconRepository`. |
| `delay/DelayRange.kt` | `feature/delay/domain/DelayRange.kt` | domain | Pure. |
| `delay/DelayConfig.kt` | → `core/domain` | — | Shared; see above. |
| `delay/DelayConfigStore.kt` | → `core/data` | — | Shared; see above. |
| `delay/WaitScreen.kt`, `delay/WaitTiming.kt` | → `shortcut` | — | The wait belongs to the tap-to-launch path (FR-029a). |

`delay` ends up holding one screen and one pure range object. That is the correct size for it.

### `locks` — the lock list

| Current | Target | Layer | Notes |
|---|---|---|---|
| `locks/LocksScreen.kt` | `feature/locks/ui/LocksScreen.kt` | ui | |
| `locks/IntroScreen.kt` | `feature/locks/ui/IntroScreen.kt` | ui | |
| `locks/LocksViewModel.kt` | `feature/locks/ui/LocksViewModel.kt` | ui | `@HiltViewModel`; the four injected lambdas become three repositories. |
| `locks/LocksUiState.kt` | `feature/locks/ui/LocksUiState.kt` | ui | The `loaded` latch is behaviour — preserved exactly. |
| `locks/Lock.kt` | `feature/locks/domain/Lock.kt` | domain | **Split (FR-033)**: the `Lock` model and `assembleLocks` stay here; the `withContext` hop moves behind the repository. |
| `locks/LockList.kt` | `feature/locks/domain/LockList.kt` | domain | Already pure; `deriveLocks`, `locksFrom`, `encodeLocks`. **F** — the encoding is the stored format. |
| — | `feature/locks/domain/LockOrderRepository.kt` **N S** | domain | |
| — | `feature/locks/domain/PinnedShortcutsRepository.kt` **N S** | domain | |
| `locks/LockStore.kt` | `feature/locks/data/LockOrderStore.kt` | data | **F** — file name and key. Renamed class (R8); the persisted names are untouched. |
| `locks/PinnedShortcuts.kt` | `feature/locks/data/PinnedShortcutsSource.kt` | data | **F**-adjacent: `null` means "could not ask" and must never prune. Direct-boot `IllegalStateException` guard preserved. |
| — | `feature/locks/data/LocksDataModule.kt` **N** | data | |

### `shortcut` — pin creation and the whole tap-to-launch path

| Current | Target | Layer | Notes |
|---|---|---|---|
| `shortcut/ShortcutLaunchActivity.kt` | **unchanged path** | entry point | **F — MUST NOT MOVE.** Its FQN is in every pinned intent and in the R8 keep rule. Keeps window lifecycle only; delegates to `WaitViewModel`. |
| `delay/WaitScreen.kt` | `feature/shortcut/ui/WaitScreen.kt` | ui | Still deliberately motionless: nothing asynchronous, no store read, no icon load. |
| — | `feature/shortcut/ui/WaitViewModel.kt` **N** | ui | Anchor + deadline in `SavedStateHandle`. Highest-risk item (R10). |
| `shortcut/ShortcutConfigScreen.kt` | `feature/shortcut/ui/ShortcutConfigScreen.kt` | ui | Loses the three `remember`-constructed collaborators. |
| — | `feature/shortcut/ui/ShortcutConfigViewModel.kt` **N** | ui | Owns the treatment selection, the pin call and the config write. |
| `shortcut/PinUnsupportedScreen.kt` | `feature/shortcut/ui/PinUnsupportedScreen.kt` | ui | |
| `shortcut/ShortcutContract.kt` | `feature/shortcut/domain/ShortcutContract.kt` | domain | **F** — every constant. Already framework-free; stays that way. `ShortcutSpec` and `shortcutSpec()` travel with it. |
| `shortcut/PinSupport.kt` (sealed type + pure overload) | `feature/shortcut/domain/PinSupport.kt` | domain | **Split (FR-033)** from the `Context` overload. |
| `delay/WaitTiming.kt` | `feature/shortcut/domain/WaitTiming.kt` | domain | `deadlineFrom`, `remainingMillis`. Pure; constitution-mandated coverage. |
| — | `feature/shortcut/domain/ShortcutPinRepository.kt` **N S** | domain | |
| — | `feature/shortcut/domain/PinSupportRepository.kt` **N S** | domain | |
| `shortcut/ShortcutPinner.kt` | `feature/shortcut/data/ShortcutPinner.kt` | data | `isRequestPinShortcutSupported()` still gates every pin attempt. |
| `shortcut/PinSupport.kt` (`Context` overload) | `feature/shortcut/data/PinSupportSource.kt` | data | |
| `shortcut/ShortcutPinner.kt` (`bake`) | stays inside `feature/shortcut/data/ShortcutPinner.kt` | data | The icon baking is already a private function of the pinner and already lands in `data`. Nothing to extract — FR-033 is satisfied as-is. |
| — | `feature/shortcut/data/ShortcutDataModule.kt` **N** | data | |

### `ui` — the design system

`ui/components/*` and `ui/theme/*` are already where the constitution puts them. Unchanged, except
that `Type.kt`'s `SlowLockType` object and the palette stay exactly as they are — `SlowLockPaletteTest`
asserts the palette against literals and must keep passing.

### Tests

Every file under `app/src/test/java/com/slowlock/` moves to mirror its subject's new package
(FR-034). No test changes package *content* during the move step; assertions are revised only in
Stage 4, against FR-048.

## Validation rules carried across unchanged

These are behaviour, not structure, and the refactor must not touch them:

1. A missing, malformed or wrongly-typed stored value reads as the default. Nothing throws on the
   launch path.
2. `pinnedShortcutIds` returning `null` means "could not ask" and falls back to the stored order.
   An empty set means the launcher genuinely holds none.
3. `LocksUiState.loaded` latches — no refresh can blank a populated screen.
4. A package with no launch intent never becomes a shortcut and never survives a tap.
5. The wait re-resolves the target at hand-off, and abandons silently if the screen is no longer
   `STARTED`.
6. `onStop` finishes the wait activity unless the configuration is changing.
