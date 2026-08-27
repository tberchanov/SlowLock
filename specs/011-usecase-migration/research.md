# Phase 0 Research: Use Cases Hold the Logic

**Date**: 2026-08-27 · **Plan**: [plan.md](./plan.md) · **Spec**: [spec.md](./spec.md)

Twelve decisions. Each names what was chosen, why, and what was rejected. The two that carried real
trade-offs are R1 (the pin boundary the spec deferred) and R5 (the search filter against single
source of truth); R9 is the one the spec did not anticipate.

---

## R1 — The `ShortcutPinner` icon boundary

**Question**: FR-009 forbids a repository reading another repository. `ShortcutPinner` reads two —
`PinSupportRepository` for the gate and `AppIconRepository` for the icon it bakes. Moving those out
means the icon has to reach the repository from outside, and the interface was deliberately shaped
to keep platform types out.

**Decision**: the icon is handed in. `ShortcutPinRepository.requestPin(target, treatment, icon:
ImageBitmap)` — and it returns `Unit`, because the two refusals it used to report are now decided
before it is called.

**Rationale**: the premise the spec deferred on turns out not to hold. `AppIconRepository.icon()`
already returns `androidx.compose.ui.graphics.ImageBitmap` across a `core/domain` interface, and its
KDoc states the obligation explicitly: *"[ImageBitmap] is Compose's type: no `android.*` type
crosses this boundary (O1)."* So `ImageBitmap` is already a domain-visible type in this project, and
handing one to `ShortcutPinRepository` crosses nothing that is not already crossed. The type O1
actually forbids is `android.graphics.Bitmap`, and the `asAndroidBitmap()` conversion stays inside
the implementation where it is today. There is no trade-off to make.

**Alternatives rejected**:

- *Leave the icon read in the repository.* Fails FR-009 outright; it is one of the two reads the
  requirement names.
- *Pass the icon as encoded bytes.* Buys no boundary that `ImageBitmap` does not already have, and
  costs an encode on the way in and a decode on the way out for a bitmap that is about to be drawn.
- *Move the bake into the use case too.* Puts `android.graphics.Canvas`, `ColorMatrix` and `Paint`
  into `domain`, which Principle II forbids in as many words. The bake is how a shortcut icon is
  written; it stays with the write.

**Consequence**: `PinRequestResult.Unsupported` and `PinRequestResult.IconUnavailable` are produced
by the use case rather than the repository. The sealed type survives unchanged — see R7 for why
nothing acts on it, then or now.

---

## R2 — Where a use case lives, and what it is called

**Decision**: a flat file in its feature's existing `domain` package. `class <Verb><Noun>UseCase
@Inject constructor(...)` with `suspend operator fun invoke(...)`. No `usecase` subpackage.

**Rationale**: Constitution II names `domain` as "models, use cases, repository interfaces" — the
layer is already the address. Principle III fixes the shape at
`com.slowlock.feature.<feature>.{ui, domain, data}` and earns a subpackage only when a feature spans
more than one layer; a fourth level is not sanctioned by it.

**Alternatives rejected**: `domain/usecase/` (a level Principle III does not provide for, and it
would group by pattern inside a package already named for its layer); top-level functions (the form
question was settled before the spec — Constitution v5.0.0 states the class form).

---

## R3 — Hilt wiring

**Decision**: none. No module, no `@Binds`, no `@Provides`, and no `@Singleton`.

**Rationale**: a concrete class annotated `@Inject constructor` is constructed by Hilt directly.
Every `@Binds` in `CoreDataModule`, `AppsDataModule`, `LocksDataModule` and `ShortcutDataModule`
exists to map an interface to its implementation; a use case has no interface, so there is nothing
to map. The two reasons the existing bindings give for `@Singleton` — a cached `LauncherApps`
handle, and one writer for a frozen file — apply to none of them: a use case holds no state.

**Verified**: R9's `CurrentLocale` is the one new interface and does need a `@Binds`, in
`CoreDataModule` beside the others.

---

## R4 — `LockOrderRepository` after the merge leaves

**Decision**: `deriveOrder(pinned: Set<String>)` is replaced by `saveOrder(order: List<String>)`.
`loadOrder()` is unchanged. `LoadLocksUseCase` reads the pinned set, reads the stored order, applies
`deriveLocks`, and calls `saveOrder` only when the result differs from what it read.

**Rationale**: the store stays the only writer of `slowlock.locks` (obligation L3, and US1's second
acceptance scenario), while the reconciliation and the write-only-if-changed rule become the use
case's. Both are rules, and both are currently unreachable by a test that does not construct a
`SharedPreferences`.

**Cost accepted**: two dispatcher hops where there was one. Both calls are main-safe by contract
(O2); the read is a `getString` and the write an `apply()`, so nothing blocks and no ordering
guarantee is lost — nothing else writes that key.

**Alternative rejected**: keep `deriveOrder` and let the use case call it. The merge would still be
in the store, which is exactly what FR-002 forbids.

---

## R5 — The search filter against single source of truth

**Question**: FR-008 takes the query filter out of `AppListUiState`. But Principle V says derived
state MUST be computed, never stored in parallel and synced by hand — so the filtered list cannot
simply become a fourth stored field that two call sites remember to update.

**Decision**: `AppListViewModel` holds its raw inputs in a private
`MutableStateFlow<AppListInputs>` and exposes the screen's state as a derived flow:

```text
uiState = _inputs
    .map { AppListUiState(it.isLoading, it.apps, it.query, filterApps(it.apps, it.query)) }
    .stateIn(viewModelScope, SharingStarted.Eagerly, AppListUiState())
```

**Rationale**: `visibleApps` is rebuilt with every state it appears in, from the two fields that own
it, so it cannot drift from them — which is what single source of truth asks for, and what a stored
field could not promise. Deriving exposed UI state from internal state is the standard holder shape,
which Principle V's conflict order says wins on choice of mechanism.

**`SharingStarted.Eagerly`, not `WhileSubscribed`**: the upstream is a hot `MutableStateFlow` and the
mapping is an in-memory filter over roughly 150 entries, so there is no upstream work to stop and
restart and no subscription cost to amortise. `WhileSubscribed`'s timeout would be a knob with no
question behind it, which is the speculative generality KISS names.

**Alternatives rejected**:

- *Store `visibleApps` as a field, recomputed inside every `update`.* The parallel copy Principle V
  prohibits: a later third call site can set `apps` without it, and nothing fails.
- *Call the use case from the composable.* Principle II keeps rules out of composables, and it would
  re-filter on every recomposition rather than on every input change.
- *Keep the getter and give the state class a reference to the use case.* The filtering would still
  be in the state class, so FR-008 fails, and a UI state object would hold an injected collaborator.

**Consequence**: one new type, `AppListInputs`. `AppListUiState` gains `visibleApps` as a
constructor parameter and keeps `isEmpty`, `hasNoResults` and `isPopulated` derived from it — those
are presentation shape and stay (FR-012).

---

## R6 — `WaitViewModel`: what moves and what cannot

**Decision**: `WaitDecisionUseCase(targets, config, clock)` with

```text
suspend operator fun invoke(target: String, anchorMillis: Long, storedDeadlineMillis: Long?): WaitDecision
```

returning `WaitDecision.Unavailable` or `WaitDecision.Wait(deadlineMillis, remainingMillis)`.

Moving: resolve-before-waiting (W5), establishing the deadline exactly once with a stored one
winning over a freshly computed one (W4, FR-027), and computing what remains.

Staying in the holder: every `SavedStateHandle` read and write, the same-target and different-target
branches of `start()` (W22, W23), the `waitJob` cancellation, the `delay()` itself, and sending the
events.

**Rationale**: FR-005 names three things and only three. `SavedStateHandle` is a presentation type
that cannot enter `domain`, and process-death survival is the screen's concern under FR-012; passing
the stored deadline in as a plain `Long?` is what makes the rule that decides on it assertable
without one. The `delay()` is a suspension bound to the screen's lifetime, not a rule — it stays
where the scope that cancels it is.

**Alternative rejected**: give the use case the handle. It would put an `androidx.lifecycle` type in
`domain`, and it would make the branch that matters testable only by constructing one.

---

## R7 — `ShortcutConfigViewModel.create()`

**Decision**: one `CreateLockUseCase(targets, config, support, icons, pins)` returning
`CreateLockResult.Created(pin: PinRequestResult)` or `CreateLockResult.TargetMissing`. It holds the
re-resolve, the write, the gate, the icon load and the pin request — the whole ordered chain FR-006
names, including `pinWhenSupported`, which moves out of `ShortcutPinner.kt` with it.

**Not two use cases**: a separate `PinShortcutUseCase` would have exactly one caller, this one.
Principle V inlines an abstraction with one implementation and no seam justifying it, and the seam a
test needs is `CreateLockUseCase`'s own constructor.

**The pin outcome is computed and discarded, exactly as today.** `ShortcutConfigViewModel` already
ignores `requestPin`'s result — the launcher owns whether an icon appears and never tells the app
(FR-012 of feature 005). FR-014 forbids changing that, so `Created` carries the result and the
holder drops it. This needs a comment, because the code cannot say why a value is deliberately
unread.

**Five collaborators is the recorded cost.** See plan.md, Complexity Tracking.

---

## R8 — `DelayConfigViewModel.start()`

**Decision**: `LoadDelayConfigUseCase(config)` with `suspend operator fun invoke(packageName: String,
editedSeconds: Int?): DelayConfig`, holding the rule that an edited value wins over the saved one.
The two other reads in `start()` — resolving the target, loading the icon — stay in the holder.

**Rationale**: FR-007 names the edited-wins rule and nothing else. Each remaining read is a single
repository call feeding state with no rule between; wrapping either would be the forwarding-only use
case FR-011 and Principle V prohibit.

**Not forwarding-only itself**: it holds the branch, which is the one this feature's predecessor
identified as the only thing in the holder a test could get wrong.

---

## R9 — Where the own-package name and the current locale come from

**Question**: not anticipated by the spec. FR-001 moves `excludeSelf` and `sortedByLabel` out of
`InstalledAppsSource` — but `excludeSelf` needs `context.packageName` and `sortedByLabel` needs the
locale, read at load time so a language change re-collates on the next load. A use case in `domain`
cannot reach either.

**Decision**: two different seams, because they are two different kinds of value.

- **Own package name** — a Hilt qualifier, `@OwnPackageName`, on a `String` provided once in
  `CoreDataModule` from `context.packageName`. The use case receives a `String`.
- **Current locale** — `fun interface CurrentLocale { fun now(): Locale }` in `core/domain`,
  implemented in `core/data` over `context.resources.configuration.locales`.

**Rationale**: the split follows the project's own precedent, stated in `Dispatchers.kt` (D4) and
`WaitTiming.kt`. A value that is fixed for the life of the process gets a qualifier, because a
qualifier already supplies the substitution seam and an interface over it would be the
`DispatcherProvider` mistake D4 names. A reading that must be taken *at the moment of use* gets a
one-function interface, which is exactly `ElapsedClock`'s shape and rationale. `java.util.Locale` is
`java.*`, so nothing platform-specific crosses.

**Alternatives rejected**:

- *One `DeviceContextRepository` holding both.* Bundles two unrelated facts behind a name that
  describes no source, and the package name is not a source read at all.
- *`BuildConfig.APPLICATION_ID` in the use case.* A generated symbol in `domain`, and not equal to
  what the code does today — `context.packageName` reflects an applicationId suffix, the constant
  does not.
- *Keep both readings in the source and pass the raw list out.* That is `load()` still collating,
  which FR-001 forbids.

---

## R10 — What must not move

Recorded so the migration does not overreach. Each is read-write or presentation shape under
FR-010, FR-011, FR-012 and FR-013:

| Site | Why it stays |
|---|---|
| `AppIconCache` | Memory tier, file tier, rasterize, sweep — how it reads and writes its one source, not a rule a requirement states. |
| `AppTargetSource` | Decodes one platform source through `resolveTarget`. |
| `PinSupportSource` | Decodes one platform source through `pinSupport`. |
| `DelayConfigStore` | Codec both ways over one file. |
| `PinnedShortcutsSource` | One read; `null` on failure is the source's own answer. |
| `PinnedShortcutsSource`'s `null`-vs-empty rule | Stated at the interface, applied by the use case — the source only reports. |
| `AppListViewModel.onAppTapped` | One repository call, then a presentation reaction (drop the row, raise a message). Wrapping it is FR-011's prohibition. |
| `ShortcutConfigViewModel.start()`, `DelayConfigViewModel`'s target and icon reads | Single logic-free calls feeding state. |
| `RootViewModel.refreshSupport()` | One call, no rule. |
| `showsIntro`, `showsLocks`, `isEmpty`, `hasNoResults`, `isPopulated`, `canCreate`, `withLocks`'s latch | Presentation shape (FR-012). |
| `assembleLocks`, `deriveLocks`, `locksFrom`, `sortedByLabel`, `resolveTarget`, `deadlineFrom`, `pinSupport` | Already pure and already in `domain`. A use case calls them; none is rewritten. |

---

## R11 — Test migration

**Decision**: existing test files move only where their subject moves. Six new test files, one per
new use case.

| Existing | Fate |
|---|---|
| `LockDeriveTest`, `LockListTest`, `AppTargetTest`, `DelayConfigTest`, `WaitTimingTest`, `ShortcutContractTest`, `IconTreatmentTest`, `DelayRangeTest`, `SlowLockPaletteTest` | Unchanged. Their subjects are pure functions that do not move. |
| `InstalledAppTest` | **Split.** Its `excludeSelf`/`dedupeByPackage`/`sortedByLabel` cases stay. Its four `visibleApps` cases (lines 87-126) move to `FilterAppsUseCaseTest` — they exercise a `ui` class from a `domain` test file today, which the move also corrects. |
| `PinGateTest` | Moves `feature/shortcut/data/` → `feature/shortcut/domain/`; its `pinWhenSupported` cases move to `CreateLockUseCaseTest`, its `pinSupport` cases stay. |
| `LocksViewModelTest`, `AppListViewModelTest`, `WaitViewModelTest`, `ShortcutConfigViewModelTest`, `DelayConfigViewModelTest` | Split. Cases asserting a *rule* move to the use case's test; cases asserting the holder's own state mapping stay. |

**New**: `LoadInstalledAppsUseCaseTest`, `FilterAppsUseCaseTest`, `LoadLocksUseCaseTest`,
`WaitDecisionUseCaseTest`, `CreateLockUseCaseTest`, `LoadDelayConfigUseCaseTest`.

**FR-019 in practice**: no test may assert that a use case called the repository it was given. Every
case must fail against a plausible wrong implementation — the mutation check in quickstart.md's
Gate 4 is how that is verified rather than asserted.

**FR-020 preserved**: schedule and time-window evaluation is `WaitTimingTest` plus
`WaitDecisionUseCaseTest`; target resolution including the null launch-intent path is `AppTargetTest`;
every frozen persisted value is `LockListTest`, `DelayConfigTest` and `ShortcutContractTest`. None of
those subjects moves.

**`PinGateTest`'s move fixes a standing defect**: `pinWhenSupported` is a pure decision function
living in a `data` file, which Principle III's one-file-one-layer rule already prohibited.

---

## R12 — Dependencies

**Decision**: none added, none moved. No change to `gradle/libs.versions.toml`.

**Verified**: a use case needs `javax.inject.Inject` (present, via Hilt 2.60.1) and
`kotlinx.coroutines` (present, 1.11.0). `stateIn` and `SharingStarted` are
`kotlinx-coroutines-core`. `java.util.Locale` is the JDK. Nothing in this feature reaches for an
artifact the project does not already declare.
