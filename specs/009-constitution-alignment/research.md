# Phase 0 Research: Constitution Alignment Refactor

**Date**: 2026-08-26 | **Plan**: [plan.md](./plan.md)

All version facts below were read from Google Maven and Maven Central on 2026-08-26, not recalled.

---

## R1 — Does `minSdk 26` survive the AndroidX upgrade?

**Decision**: Yes. `minSdk` stays at 26 and is not touched.

**Rationale**: AndroidX's default `minSdk` floor is 23, raised from 21 across most libraries during
2025. Compose moved to `minSdk 24`. Both are below 26, so nothing in the target set forces a bump.

**Alternatives considered**: Raising `minSdk` to reduce compatibility branches — rejected outright.
It would drop users mid-refactor, which is a user-visible change and outside this feature's scope.

**Residual risk**: low. Any library that has quietly raised its floor fails at manifest merge, which
the Stage 1 build catches immediately. If one has, it becomes a recorded finding under FR-009 rather
than a silent bump.

---

## R2 — Kotlin, KSP and Hilt must line up, and the recorded Kotlin target does not

**Decision**: Target **Kotlin 2.3.21** with the matching `compose-compiler-gradle-plugin` 2.3.21,
KSP 2.3.11 and Hilt 2.60.1. **Not** Kotlin 2.4.10.

**Rationale**: the evidence, from the published POMs:

| Artifact | Built against |
|---|---|
| `com.google.devtools.ksp:symbol-processing-api:2.3.11` | `kotlin-stdlib` 2.3.20 |
| `com.google.devtools.ksp:symbol-processing-aa:2.3.11` | `kotlin-stdlib` 2.3.20 |
| `com.google.dagger:hilt-android:2.60.1` | `kotlin-stdlib` 2.3.21 |

KSP's newest release tracks the Kotlin 2.3 line; there is no KSP release on the 2.4 line yet, and
KSP's own release notes for 2.3.7 record "bumped Kotlin target language version to 2.3". Kotlin
2.4.10 is the newest stable Kotlin, but choosing it would leave the annotation processor that Hilt
depends on without a supported release. Kotlin 2.3.21 is itself a stable release on a maintained
line, so Principle I and the "latest stable only, no prereleases" instruction are both satisfied.

**Status: confirmed by the maintainer on 2026-08-26.** The finding contradicted the Clarification
originally recorded in the spec (Kotlin 2.4.10), so it was put to the maintainer rather than
applied unilaterally. They chose 2.3.21, and the spec's Clarifications were corrected to match.

**Alternatives considered**:
- *Kotlin 2.4.10 anyway, relying on KSP2's decoupled versioning to tolerate a newer compiler* —
  possible but unverified, and the failure mode is a build that breaks late in Stage 1. Offered as
  a probe-then-fall-back option and not taken; the maintainer chose the version that is known to
  work rather than the one that might.
- *Drop Hilt to keep Kotlin 2.4.10* — rejected. The injection mechanism was the maintainer's
  decision and carries more weight than one Kotlin minor version.

---

## R3 — `hiltViewModel()` without dragging in a navigation library

**Decision**: depend on **`androidx.hilt:hilt-lifecycle-viewmodel-compose:1.4.0`**, not
`androidx.hilt:hilt-navigation-compose`.

**Rationale**: the obvious artifact for `hiltViewModel()` is `hilt-navigation-compose`, but its POM
declares a dependency on `androidx.navigation:navigation-compose:2.9.0`. This project deliberately
has no navigation library — `SlowLockRoot`'s own documentation records that decision, and the app
has a handful of root states with one transition each way. Pulling in a navigation graph as a
side effect of a DI choice would be exactly the kind of unexamined dependency Principle I forbids.
`hilt-lifecycle-viewmodel-compose` provides the same entry point and its POM pulls only
`androidx.hilt:hilt-lifecycle-viewmodel`, `lifecycle-viewmodel-compose` 2.11.0 (our target anyway)
and `hilt-android`.

**Alternatives considered**: obtaining ViewModels through a manual factory to avoid the artifact
entirely — rejected as a second wiring mechanism, which FR-027 forbids.

---

## R4 — Where the Hilt modules live

**Decision**: each `@Module` sits beside the implementations it binds, in that capability's `data`
package — `feature/apps/data/AppsDataModule.kt`, `feature/locks/data/LocksDataModule.kt`,
`feature/shortcut/data/ShortcutDataModule.kt`, `core/data/CoreDataModule.kt`. There is **no `di` package**.

**Rationale**: FR-032 forbids a directory named for a layer, a pattern, or a catch-all, and `di` is
a pattern name. Putting bindings next to what they bind also means deleting a capability deletes
its wiring with it, which is the property feature-first packaging exists to buy.

**Alternatives considered**: a top-level `di` package holding every module — rejected under FR-032
and because it recreates the layer-first scatter Principle III argues against.

---

## R5 — Hilt needs an `Application` class the project does not have

**Decision**: add `com.slowlock.SlowLockApplication` annotated `@HiltAndroidApp`, and
`android:name=".SlowLockApplication"` to the manifest's `<application>` tag. Both entry-point
activities gain `@AndroidEntryPoint`.

**Rationale**: Hilt's generated component is rooted at the `Application`. The project currently has
none, so one is created holding nothing but the annotation.

**User-visible impact**: none. The `<application>` attributes that matter to users — `allowBackup`,
`dataExtractionRules`, icon, label, theme — are untouched. Process start does gain component
creation; that cost is judged against FR-001b's timing bar at both verification gates, and it lands
on the pinned-icon tap path, which is why that bar exists.

---

## R6 — How dispatchers get injected

**Decision**: qualifier annotations in `core/domain` (`@IoDispatcher`, `@DefaultDispatcher`) with
`CoroutineDispatcher` bindings provided by `CoreDataModule`. Every `withContext(Dispatchers.IO)`
currently written inline becomes `withContext(ioDispatcher)` against a constructor-injected value.

**Rationale**: Principle IV requires dispatchers be injected so tests can substitute a test
dispatcher, and FR-039 restates it. Qualifiers are the standard shape and keep the injected type a
plain `CoroutineDispatcher` rather than a bespoke provider interface with one implementation, which
FR-044 would call out.

**Alternatives considered**: a `DispatcherProvider` interface — rejected under FR-044/Principle V:
one implementation, no seam beyond what a qualifier already gives.

**Consequence for tests**: `kotlinx-coroutines-test` 1.11.0 is added (test scope only) so
`StandardTestDispatcher`/`UnconfinedTestDispatcher` can be handed in. Recorded per FR-017: without
it, tests exercising injected dispatchers would hand-roll a dispatcher and re-implement what the
library provides.

---

## R7 — One-shot events off the `StateFlow` sentinel

**Decision**: `AppListUiState.unavailableAppMessage` (and the shortcut screen's snackbar messages)
move to a `Channel` consumed as a flow. The `onUnavailableMessageShown()` clear-the-flag call
disappears.

**Rationale**: Principle IV is explicit — "one-shot events use a channel or an equivalent
consume-once mechanism, never a `StateFlow` with a sentinel". The current field is exactly that
sentinel, and the manual clear is the bug surface it warns about.

**Behaviour to preserve exactly**: the message is shown once per occurrence, and the dead row is
still dropped from the list in the same update. The list mutation stays in state; only the message
moves to the event channel.

**Consequence**: the message text currently resolves through `Application.getString()` inside the
ViewModel. With no `Application` in the ViewModel, the event carries a string resource identifier
and the composable resolves it. Same resource, same text — FR-002 holds.

---

## R8 — Repository interfaces over two frozen preference files

**Decision**: three interfaces in `core/domain` (`DelayConfigRepository`, `AppTargetRepository`,
`AppIconRepository`) and two in `feature/locks/domain` (`LockOrderRepository`,
`PinnedShortcutsRepository`), plus two in `feature/shortcut/domain` (`ShortcutPinRepository`,
`PinSupportRepository`). All suspend, all main-safe, all returning domain values.

**Rationale**: FR-020. Each of these already has a real seam behind it — a `SharedPreferences`
file, `LauncherApps`, `ShortcutManager`, the icon cache directory — so none is an
interface-with-one-implementation of the kind FR-044 forbids; the platform is the second
implementation in every test.

**What must not change**: the file names, key shapes, default-on-missing behaviour, the
`runCatching` type-mismatch guards, and the `null`-means-"could not ask" contract of the pinned
shortcut read. `null` is not an empty set; conflating them empties the user's lock list. These are
carried into the implementations verbatim and asserted by the frozen-value tests.

**Naming note**: `LockStore` becomes `LockOrderStore` behind `LockOrderRepository`, because what it
stores is the row *order*, not the locks. The class name is not frozen; the file name and key
inside it are.

---

## R9 — Splitting the root arbiter

**Decision**: a `RootViewModel` takes the `DelayConfigStore` read that precedes navigation and the
`pinSupport(context)` platform check. The navigation `stage` stays in `rememberSaveable` in
`SlowLockRoot`.

**Rationale**: FR-023a settles this. Which screen is showing is presentation state; reading
persisted configuration and calling a platform service are not. `rememberSaveable` is also what
currently delivers the process-death restore that features 003 and 005 specified, and a
`SavedStateHandle` rewrite would put that behaviour at risk for no principle gained.

**What must be preserved exactly**: the `SaveableStateHolder` keys and which of them are dropped on
exit (the list's scroll position and query survive the round trip; the two configuration screens'
state does not), and the `Origin` rule that back returns to whichever screen the flow was entered
from. These are the two behaviours most likely to break silently.

---

## R10 — The wait path, and the riskiest change in the feature

**Decision**: `ShortcutLaunchActivity` keeps only window-lifecycle duties — the `onStop`
finish-unless-changing-configurations rule, `onNewIntent` de-duplication, and the unavailable
toast. Resolution, the configuration read, the wait and the hand-off move to a `WaitViewModel`
holding the anchor and deadline in a `SavedStateHandle`.

**Rationale**: Principle II and FR-022 forbid business rules in an activity, and this activity
currently holds the whole of the delay behaviour.

**Risk**: this is the highest-risk change in the feature. The current code anchors on
`SystemClock.elapsedRealtime()`, saves anchor and deadline into the instance-state bundle, and
depends on `lifecycleScope` cancellation for every abandonment path. Moving to `viewModelScope`
changes what survives a configuration change — a ViewModel outlives rotation where the bundle was
being restored into a new activity. The requirement it protects (a rotation must not restart the
wait, FR-027 of feature 003) is preserved either way, but by a different mechanism.

**Mitigation**: this change gets its own stage step, its own manual cases in the feature's test
plan, and the existing `WaitTiming` unit tests are extended rather than replaced. The race guard
that abandons a hand-off when the screen is no longer `STARTED` is carried across verbatim: it
prevents a background activity start, which Principle IV forbids outright.

**Alternative considered**: leaving the wait logic in the activity and recording a deviation —
rejected. It is the second-clearest Principle II violation in the codebase, and leaving it would
mean the refactor stopped at the file that most needed it.

---

## R11 — `ShortcutTarget` becomes `AppTarget`

**Decision**: rename on the move into `core/domain`.

**Rationale**: the type is "the resolved facts about an installed app" — package name, current
label, version code. It is already used by `locks` to build rows, and once it sits in the shared
home a name that says "shortcut" describes one of its three consumers. Nothing about the name is
frozen: it is never persisted and never written into an intent.

**Cost**: a rename inside a move step, which is churn FR-035 asks to keep separate from logic
change. It is a pure rename, done in the same step as the move, and the compiler proves it complete.

---

## R12 — What happens to the injected-lambda test seams

**Decision**: the `suspend (String) -> DelayConfig`, `(String) -> Intent?`, `suspend () -> Set<String>?`
and similar lambda parameters on `AppListViewModel`, `LocksViewModel` and `resolveTarget` are
replaced by the repository interfaces from R8. FR-050 requires the coverage they carry to survive
the swap.

**Rationale**: those lambdas exist only because there was no injection mechanism. With Hilt the
seam is the interface, and the fake is a test implementation of it. The constitution's mandated
coverage — target resolution including the null `getLaunchIntentForPackage()` path, and every
frozen persisted value asserted against a literal — is exercised through the fakes instead.

**What gets simpler**: `AppListViewModelTest` currently needs to construct an `Application`, which
is why `testOptions.unitTests.isReturnDefaultValues = true` exists. With plain `ViewModel`s taking
repositories, that dependency goes away. The flag itself stays until the suite is re-run without
it, then is removed if nothing needs it.

---

## R13 — R8 keep rules, and the configuration cache

**Decision**: `-keepnames class com.slowlock.shortcut.ShortcutLaunchActivity` in
`app/src/main/keepRules/rules.keep` stays exactly as it is and is re-verified after the move. Hilt
and KSP ship their own consumer rules; none are added by hand.

**Rationale**: the keep rule guards the same frozen name `ShortcutContractTest` guards in source.
Since R14's tree leaves that class where it is, the rule remains correct — but it is checked, not
assumed, because a wrong keep rule fails only in a release build, on a user's device.

**Configuration cache**: `org.gradle.configuration-cache=true` is on. The Hilt Gradle plugin and
KSP2 both support it in current releases, but this is verified by running a second configured build
in Stage 1 rather than assumed. If either breaks it, that is a recorded finding, not a silent
`configuration-cache=false`.

---

## R14 — Capturing the behaviour baseline

**Decision**: before any change, build and install the current `main` as a release-signed or debug
build, create at least two locks with different delays and different icon treatments, pin both,
and record: the values on disk, the pinned icons' behaviour, and screenshots of every screen. FR-008
requires this to exist before Stage 1.

**Rationale**: "nothing changed" is only checkable against something. The device state also becomes
the in-place-update fixture SC-002 needs — the same install is upgraded over at the end, which is
the only way to prove the frozen values survived.

**Who runs it**: the maintainer. No agent drives the device (constitution, Development Workflow).

---

## R15 — Stage ordering and what a reviewable step is

**Decision**: four stages, each a set of steps that individually build and pass `./gradlew test`.

1. **Toolchain** — versions, Hilt/KSP plumbing, `SlowLockApplication`, dead instrumented-test config
   removed. No structural change of any kind (FR-053b depends on this). Ends at the smoke-pass gate.
2. **Seams** — repository interfaces, implementations, constructor injection, composables and the
   activity emptied of data and platform access. Files stay where they are.
3. **Move** — the package rearrangement, tests moved with their subjects. No logic change (FR-035).
4. **Settle** — one-shot events, single state owners, test-suite pruning against FR-048.

**Rationale**: separating stage 2 from stage 3 is what makes both reviewable. A diff that moves a
file *and* changes it reads as a rewrite; the constitution's own reasoning about legibility applies
to the refactor's diff as much as to its output.

**Version control**: every stage ends with changes in the working tree and an offer to commit.
Nothing is committed, pushed, branched or tagged by the agent (Principle VII, FR-055).
