# Phase 0 Research: Navigation Adoption

**Date**: 2026-08-27 | **Plan**: [plan.md](./plan.md) | **Spec**: [spec.md](./spec.md)

Every version below was read from the publishing repository on 2026-08-27, not recalled (FR-023).
The command that produced each answer is given so it can be re-run.

---

## R1 — Which navigation artifact

**Decision**: `androidx.navigation:navigation-compose:2.10.0`. Both generations are **eligible**;
this is a decision on merits, not on eligibility.

**Evidence** — read the version *list*, not `<release>`, which in AndroidX metadata tracks the
newest publication and not the newest stable:

```bash
curl -s https://dl.google.com/dl/android/maven2/androidx/navigation3/group-index.xml
# navigation3-runtime, navigation3-ui:
#   … 1.0.0, 1.0.1, 1.1.0, 1.1.1 … 1.1.7, then 1.2.0-alpha01 … 1.2.0-beta01
# → two stable lines shipped; 1.1.7 is the newest stable; 1.2.0 is in beta

curl -s https://dl.google.com/dl/android/maven2/androidx/navigation/navigation-compose/maven-metadata.xml
# … <version>2.10.0</version> </versions> <lastUpdated>20260826170047</lastUpdated>

curl -s https://dl.google.com/dl/android/maven2/androidx/hilt/group-index.xml
# hilt-common, hilt-compiler, hilt-lifecycle-viewmodel, hilt-lifecycle-viewmodel-compose,
# hilt-navigation, hilt-navigation-compose, hilt-navigation-fragment, hilt-work
# → there is no hilt-navigation3 artifact at any version
```

**The spec's tiebreak does not decide this.** The Clarification admits the newer generation if it
has a stable release line. `navigation3` 1.1.7 is stable on a maintained line, so it clears that bar
and Principle I's "maintained release line" both. The rule that was supposed to settle the choice
does not settle it, and the choice has to be argued.

**Why `navigation-compose` 2.10.0 wins anyway.**

1. **The injection path is published for 2.x and is not published for nav3.** R3's finding — that
   every existing `hiltViewModel()` call site scopes to the entry with *no edit* — rests on the
   destination supplying `LocalViewModelStoreOwner`. Under nav3 that owner comes from a decorator in
   `lifecycle-viewmodel-navigation3` (stable at `2.11.0`, the project's exact lifecycle version)
   rather than from the destination itself, and there is no first-party Hilt artifact for that path
   the way `hilt-navigation-compose` is for 2.x. The zero-edit scoping this feature's whole state
   story depends on is verified for 2.x and would have to be re-established for nav3.
2. **Back-stack persistence sits on the other side of the line.** 2.x saves and restores the back
   stack itself, which is what delivers G6. Under nav3 the back stack is a list the application
   holds. That is the library's intended design and not a hand-rolled mechanism — but this
   constitution has just prohibited "a hand-managed back stack" by name, and a design where the app
   owns the list needs that distinction argued in front of Principle V rather than assumed.
3. **Cost.** Adopting nav3 rewrites both contracts, not just the dependency line.

None of these is a disqualification. If the maintainer wants nav3 genuinely reconsidered, the two
checks that would decide it are: does `hiltViewModel()` resolve to the entry under
`rememberViewModelStoreNavEntryDecorator` with no call-site edit, and does nav3's own back-stack
persistence satisfy G6 without this project writing a serialiser. Both are answerable in an
afternoon's spike; neither is answered here, and neither is assumed here.

**Alternatives rejected**: `navigation3` 1.1.7 — eligible, rejected on the three grounds above,
recorded as a comparison; staying bespoke (prohibited outright by the constitution's Navigation
constraint).

**Consequence to watch**: this decision is a judgement with a shelf life, not a permanent fact. When
nav3 publishes a Hilt integration, ground 1 expires and the comparison should be re-run. Nothing in
this design assumes the 2.x model beyond the `NavHost`/route API itself.

---

## R2 — How destination arguments travel

**Decision**: type-safe routes. Add the Kotlin serialization Gradle plugin at **2.3.21** and
declare `org.jetbrains.kotlinx:kotlinx-serialization-core` explicitly at **1.11.0**.

**Evidence**:

```bash
curl -s https://plugins.gradle.org/m2/org/jetbrains/kotlin/plugin/serialization/\
org.jetbrains.kotlin.plugin.serialization.gradle.plugin/maven-metadata.xml | grep '2\.3\.'
# … <version>2.3.21</version>   ← exact match for the project's Kotlin

curl -s https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-core/maven-metadata.xml
# … <version>1.11.0</version>   ← newest stable

curl -s https://dl.google.com/dl/android/maven2/androidx/navigation/navigation-common/2.10.0/\
navigation-common-2.10.0.pom
# … kotlinx-serialization-core 1.7.3 (transitive)
```

The plugin version tracks Kotlin exactly, so `2.3.21` is not a choice but a consequence of the
project's language version — and moving Kotlin without moving it in step is a build break, the same
relationship the catalog already documents for KSP.

`kotlinx-serialization-core` arrives transitively at `1.7.3` through `navigation-common`. It is
**declared explicitly at 1.11.0** instead, for the reason the catalog already gives for
`kotlinx-coroutines`: a runtime the generated code depends on is pinned by this project's catalog,
not by whatever a navigation release happens to pull in. `1.11.0` is stable and API-compatible with
what `navigation-common` was compiled against.

**What breaks without them**: the routes carrying the package name, delay and treatment cannot be
declared as compiler-checked types. The fallback is string routes with declared arguments, which is
a hand-written encoding of exactly the kind Principle V now rules out, and which loses the
exhaustiveness that makes a forgotten destination a compile error today.

**Alternatives rejected**: inheriting `1.7.3` transitively (undeclared version at a point of use,
FR-022); `kotlinx-serialization-json` (not needed — routes use the core format, not JSON).

---

## R3 — Whether the injection artifact changes

**Decision**: no change. Keep `androidx.hilt:hilt-lifecycle-viewmodel-compose:1.4.0`.

Inside a `NavHost` destination the entry supplies the ambient owners — `LocalViewModelStoreOwner`,
`LocalLifecycleOwner` and `LocalSavedStateRegistryOwner` all resolve to the `NavBackStackEntry`.
`hiltViewModel()` reads `LocalViewModelStoreOwner`, so **every existing call site scopes to the
entry with no edit**. That is the whole of Principle II's scoping requirement, delivered by where
the call sits rather than by a new artifact.

`androidx.hilt:hilt-navigation-compose:1.4.0` is published and stable, and its POM confirms what
the current catalog comment says about it:

```bash
curl -s https://dl.google.com/dl/android/maven2/androidx/hilt/hilt-navigation-compose/1.4.0/\
hilt-navigation-compose-1.4.0.pom
# depends on: hilt-lifecycle-viewmodel-compose 1.4.0, navigation-compose 2.9.0
```

It is needed only to reach a **parent** entry's store — i.e. only if R9's fallback fires. The
catalog comment saying this project "does not want" the navigation runtime was accurate when it was
written and is moot from the moment this feature lands; it is corrected in the same change (FR-027).

---

## R4 — Compatibility with the current toolchain

**Decision**: nothing else moves. No version already in the catalog changes.

| Claim | Evidence |
|---|---|
| Compose BOM `2026.08.00` is still the newest | `compose-bom/maven-metadata.xml` ends at `2026.08.00` |
| The BOM's Compose version exceeds navigation's floor | BOM pins `ui`/`runtime`/`animation` at `1.12.0`; `navigation-compose:2.10.0` declares `1.10.5`. The BOM wins; no conflict. |
| Navigation's lifecycle floor is already met | `navigation-common:2.10.0` depends on `lifecycle-runtime`, `lifecycle-viewmodel` and `lifecycle-viewmodel-savedstate` at `2.11.0` — the project's exact declared version |
| New transitives need no declaration | `savedstate:1.5.0`, `collection:1.5.0`, `annotation:1.9.1` arrive resolved and are not used directly by this project's source |

**Not verified here, deliberately**: whether AGP, Hilt, KSP or the Compose BOM have newer releases.
A general currency sweep is 009's job and is out of this feature's scope; FR-023 governs *added*
versions only.

---

## R5 — What a destination's ambient owners change

**Decision**: rely on the entry-provided owners, and treat the lifecycle consequence as design, not
accident.

`NavHost` renders each destination inside the entry's own owner scope. Three consequences follow,
and the design turns on all three:

1. **`hiltViewModel()` scopes to the entry** — R3, and the whole of FR-015.
2. **`LifecycleEventEffect` inside a destination observes the *entry's* lifecycle**, not the
   Activity's. An entry becomes `STARTED`/`RESUMED` when it is opened *and* when it is popped back
   to. This is what R9 depends on, and what R6 pays for.
3. **`collectAsStateWithLifecycle()` follows the entry**, so a destination beneath another stops
   collecting. Already the desired behaviour; no change needed.

---

## R6 — The redundant enumeration on pop-back (accepted cost)

`AppListScreen` refreshes on `ON_START`. Today that is the Activity's `ON_START`, which does not
fire during an in-app round trip. After R5 it is the entry's, which **does** fire when the user
comes back from the delay step.

**Decision**: accept it. No guard is added.

The enumeration runs off the main thread, a refresh over a populated list leaves the existing rows
on screen rather than flashing a spinner (001 FR-017), and the user cannot install or uninstall
anything during an in-app round trip without also leaving the app — so the extra read can only ever
return the same answer. A guard would be machinery to avoid a redundant read that is bounded by how
often a user navigates.

Recorded because the extra binder traffic reads like a defect to anyone who finds it without this
note.

**The same shape, one screen over**: opening the delay step to *edit* an existing lock now re-reads
that app's configuration from disk, where the root previously passed the value `LocksViewModel` had
already resolved. Same answer, one extra read, and it is what makes the delay screen's load path
identical on both routes in — which is the point of D5.

---

## R7 — Where the route declarations live

**Decision**: one file, `com.slowlock.Routes.kt`, in the root package beside `SlowLockRoot.kt`.

The routes describe the graph, and the graph belongs to no capability — the same argument that
already puts `SlowLockRoot` and `MainActivity` at the root package, which Principle III's
entry-point clause sanctions explicitly.

**Alternative rejected**: one route type in each feature's `ui` package. It gives each capability
ownership of its own address, but produces four one-declaration files and buys no boundary: every
`navigate` call in this app lives in the root, so no feature ever needs another feature's route
type. If a feature ever navigates on its own, moving its route into it is a rename.

---

## R8 — Load-versus-restore: the only new branching logic

**Decision**: a holder reads from disk **only when its saved-state handle has nothing yet**.

This is the one place this feature introduces logic a test could get wrong, and it is the only
thing FR-036 asks for coverage on.

| Holder | First open | Rotation | Process death mid-edit |
|---|---|---|---|
| `DelayConfigViewModel` | handle empty → load the saved delay from disk | holder survives; no read | handle holds the **edited** delay → restore it, **do not** re-read |
| `ShortcutConfigViewModel` | handle empty → take the treatment from the route argument | holder survives; no read | handle holds the **chosen** treatment → restore it, **do not** re-take the argument |

Getting this backwards is silent: the user edits 30 seconds, the process is killed, and the screen
comes back showing the 10 that is still on disk. Neither the compiler nor the device notices.

---

## R9 — What replaces the wait before returning home

**Decision**: move `LifecycleEventEffect(ON_RESUME) { locksViewModel.refresh() }` onto the Home
destination and drop `refresh().join()` from the flow's completion.

By R5 the Home entry returns to `RESUMED` when the flow pops back to it, so the re-read fires on
exactly the transition the explicit wait existed to cover — and it keeps covering the case it was
already there for, the launcher's pin dialog closing without stopping the Activity. A shortcut
entry can no longer reach the Home entry's holder, and it should not: that reach is the cross-screen
coupling Principle II's scoping rule exists to remove.

**Accepted risk**: the pop may land one frame before the re-read completes, showing the list without
the lock just created. Verified on device (manual case), not assumed.

**Fallback if it is visible**: scope `LocksViewModel` to the graph's route entry with
`hiltViewModel(parentEntry)`, which requires R3's artifact after all. Permitted by Principle II as
"the narrowest entry covering them", wider than the behaviour needs, and therefore taken only on
device evidence and recorded as a deviation if taken (FR-042).

---

## R10 — Transitions: the default that would be a user-visible change

**Decision**: the `NavHost` is configured with no enter, exit, pop-enter or pop-exit transition.

`NavHost` animates between destinations by default. Today the stage machine swaps screens with no
animation at all, so accepting the default would put a slide-and-fade on every step of the flow —
a difference a user can see, and one FR-002 does not approve. Suppressing it is a documented
parameter of the standard component, not a bespoke replacement for it; Principle V's conflict order
leaves KISS in charge of how much of the mechanism gets used.

Adopting the library's transitions may well be an improvement. It is a product decision about how
the app feels, and it belongs to a specification that says so — not to a refactor whose acceptance
bar is that nothing looks different.

**Reversed on maintainer direction, 2026-08-27, after the manual pass.** The graph now cross-fades:
`fadeIn()` / `fadeOut()` in both directions. The reasoning above is unchanged and was not found
wrong — the decision was overridden as the product call it always was. Two consequences follow and
neither is hidden by this note:

- **FR-001 no longer holds in full.** A fourth user-visible difference now exists beyond FR-002's
  three, and it is deliberate rather than a defect under SC-004.
- **The manual pass that verified 010 predates it.** Section N2 of the manual test plan was
  rewritten from "nothing animates" to the fade, and is the one section that needs re-running.

The library's default slide was still declined: a cross-fade states less hierarchy than a slide,
and a three-step flow already carries its position in the `1 / 3` counter.

---

## R11 — Predictive back

**Decision**: nothing is enabled. The manifest does not set `android:enableOnBackInvokedCallback`,
and this feature does not add it.

Removing the per-screen back interception (FR-010) hands back to the navigation library, which is
the point. Opting the app into the platform's predictive-back animation is a separate, visible
change and is out of scope for the same reason R10's transitions are.

---

## R12 — Back on the start destination

**Decision**: verified as a manual case, not assumed.

`NavHost` leaves the start destination with nothing beneath it, so a back press there is not
consumed and falls through to the Activity, which finishes — which is what 005 FR-031 requires and
what the app does today. It is stated here because it is the one back behaviour that is delivered by
*absence* rather than by a call, and absences are what a later refactor breaks.

---

## R13 — The release build is a gate, not an assumption

**Decision**: `./gradlew assembleRelease` must succeed and the maintainer must run one complete flow
on a release build before the feature is reported complete.

The release build runs R8 with `isShrinkResources` and keep rules from `src/main/keepRules`. Both
`kotlinx-serialization` and `navigation-common` ship consumer ProGuard rules, so the generated route
serializers are expected to survive shrinking. "Expected" is the problem: a stripped serializer
fails when a user navigates, not when the project builds, and the debug builds every other gate uses
would never show it.

Any keep rule this turns out to need goes in `src/main/keepRules`, which AGP already passes through.

---

## R14 — What must not move

Restated by reference, not re-derived. [009's frozen-values contract](../009-constitution-alignment/contracts/frozen-values.md)
is unchanged and still binding: the pinned entry point's fully-qualified name, both preferences file
names and their key shapes, the shortcut identifier scheme, and the intent extra name.

Two things this feature could plausibly touch and must not:

- **`ShortcutLaunchActivity` is not a destination.** It is a separate entry point with its own task
  affinity, its own theme, a frozen fully-qualified name and exactly one screen. Routing it through
  the graph would put a frozen value at risk to gain nothing (FR-014).
- **No destination declares a deep link.** The scope boundary is fixed by the constitution, and its
  accepted limitation that deep links bypass the launcher is settled (FR-014).
