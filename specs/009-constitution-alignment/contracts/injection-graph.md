# Contract: Injection Graph

**Date**: 2026-08-26 | **Spec**: FR-016, FR-024, FR-027, FR-028, FR-039 | **Plan**: [../plan.md](../plan.md)

One mechanism, declared once. Hilt 2.60.1 with KSP. No service locator, no static holder, no
second wiring path anywhere in the codebase.

## Roots

| Component | Annotation | Notes |
|---|---|---|
| `SlowLockApplication` | `@HiltAndroidApp` | New file; holds nothing else. Manifest gains `android:name=".SlowLockApplication"`. |
| `MainActivity` | `@AndroidEntryPoint` | |
| `ShortcutLaunchActivity` | `@AndroidEntryPoint` | Stays at its frozen FQN (F1). |

Composables obtain state holders through `hiltViewModel()` from
`androidx.hilt:hilt-lifecycle-viewmodel-compose` — **not** `hilt-navigation-compose`, whose POM
drags in `navigation-compose` (research R3).

## Modules — each beside what it binds

There is no `di` package (FR-032, research R4).

| Module | Binds |
|---|---|
| `core/data/CoreDataModule.kt` | `DelayConfigRepository`, `AppTargetRepository`, `AppIconRepository`; the `@IoDispatcher` and `@DefaultDispatcher` qualifiers |
| `feature/apps/data/AppsDataModule.kt` | `InstalledAppsRepository` |
| `feature/locks/data/LocksDataModule.kt` | `LockOrderRepository`, `PinnedShortcutsRepository` |
| `feature/shortcut/data/ShortcutDataModule.kt` | `PinSupportRepository`, `ShortcutPinRepository` |

All bindings are `@Singleton` where the implementation holds a cached handle (a
`SharedPreferences` instance, a `LauncherApps` handle, the icon cache directory) and unscoped
otherwise. `@ApplicationContext` is the only `Context` injected; no activity context reaches a
repository.

## Dispatchers

```
@Qualifier annotation class IoDispatcher
@Qualifier annotation class DefaultDispatcher
```

Provided as plain `CoroutineDispatcher` values. Obligations:

- **D1** — No production file names `Dispatchers.IO` or `Dispatchers.Default` at a call site. The
  only place those constants appear is the module that provides them.
- **D2** — Every repository takes its dispatcher through the constructor and applies it itself, so
  callers stay main-safe (O2 in [repository-interfaces.md](./repository-interfaces.md)).
- **D3** — Tests substitute a test dispatcher through the same constructor parameter. No
  `Dispatchers.setMain` gymnastics is required for anything except a ViewModel's `viewModelScope`.
- **D4** — No `DispatcherProvider` interface. A qualifier already supplies the seam; an interface
  with one implementation on the same side of it is what FR-044 forbids.

## State holders

| Holder | Injected |
|---|---|
| `RootViewModel` | `DelayConfigRepository`, `PinSupportRepository` |
| `AppListViewModel` | `InstalledAppsRepository`, `AppTargetRepository`, `AppIconRepository`, `SavedStateHandle` |
| `LocksViewModel` | `LockOrderRepository`, `PinnedShortcutsRepository`, `DelayConfigRepository`, `AppTargetRepository`, `AppIconRepository` |
| `ShortcutConfigViewModel` | `ShortcutPinRepository`, `DelayConfigRepository`, `AppTargetRepository`, `AppIconRepository` |
| `WaitViewModel` | `DelayConfigRepository`, `AppTargetRepository`, `SavedStateHandle` |

All annotated `@HiltViewModel`, all extending plain `ViewModel`. Obligations:

- **V1** — `AndroidViewModel` is gone. No state holder takes an `Application`.
- **V2** — `@JvmOverloads` and the reflection-found constructors go with it (FR-028). They existed
  only to let the default `SavedStateViewModelFactory` find a constructor, which is precisely the
  problem injection solves.
- **V3** — No state holder resolves a string resource. Where a message must reach the user, the
  event carries a resource identifier and the composable resolves it (research R7), so the text is
  identical and the domain stays framework-free.
- **V4** — `DelayConfigScreen` gets **no** state holder. It owns no state, reads nothing and
  decides nothing; adding one would be the speculative structure Principle V calls a defect.

## Verification

- `./gradlew assembleDebug` proves the graph resolves. A missing binding is a compile error, which
  is the property this mechanism was chosen for.
- Grep gates for the Stage 2 review: zero matches for `Dispatchers.IO` / `Dispatchers.Default`
  outside `CoreDataModule`; zero matches for `AndroidViewModel`; zero `remember { SomeStore(...) }`
  in any composable.
- The configuration cache stays on. A second configured build in Stage 1 confirms Hilt's Gradle
  plugin and KSP2 both honour it; if either does not, that is a recorded finding, not a silent
  `configuration-cache=false`.
