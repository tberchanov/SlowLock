# Findings: Constitution Alignment Refactor

**Feature**: `009-constitution-alignment` | **Opened**: 2026-08-26 | **Task**: T081 (collects), FR-009 / FR-010 / FR-011 (govern)

Everything noticed during the refactor that the maintainer has not ruled on. Nothing here has been
acted on.

**The rules this file exists to enforce** (spec, Defect handling):

- **FR-009** — a defect is recorded with what it is, what it breaks, and what fixing it would
  change for a user. It is **not fixed** before the maintainer confirms *that specific fix*.
- **FR-010** — a confirmed fix is carried out **separately** from the structural change around it.
- **FR-011** — an unconfirmed defect is preserved **exactly as it is**. Silent correction "while in
  the file" is prohibited.
- **FR-012** — a refactor step is never reported as behaviour-preserving if it also contains a fix.

A general go-ahead ("proceed", "continue", "looks good") is **not** a ruling on anything below.
Each item needs its own answer.

## Status board

| # | Raised | Severity | Subject | Status |
|---|---|---|---|---|
| F-01 | T003 | **High** | F1's frozen FQN is asserted relatively, not against a literal | ✅ **Closed** — approved and added before Phase 5 |
| F-02 | T018 | Low | Kotlin 2.3.21 flags an unreachable cast in `SlowLockPaletteTest` | 🟡 Open — no action proposed |
| F-03 | T003 | Info | `contracts/frozen-values.md` lists `DEFAULT_SECONDS` as frozen; source says otherwise | 🟡 Open — doc correction available |
| F-04 | T003 | Info | T006 duplicates an assertion already in `DelayConfigTest` | 🟡 **Open — T074 ruled: keep both.** Ruling reversible |
| F-05 | T046 | **Medium** | The treatment selection stayed in `rememberSaveable` instead of moving to the state holder | ✅ **Closed** — resolved by construction in 010 |
| F-06 | T050 | Medium | `RootViewModel` gained two repositories beyond `injection-graph.md`'s list | ✅ **Closed** — resolved by construction in 010 |
| F-07 | T047 | Medium | `ElapsedClock` added; not in `data-model.md` | 🟡 Open — required by T076 |
| F-08 | T047/T052 | Medium | The wait decides the hand-off; the window performs it | 🟡 Open — divergence from T047's wording |
| F-09 | T043 | **Medium** | The app-list tap now requires a *label*, not just a launch intent | 🟡 **Open — a real, narrow behaviour divergence (FR-001)** |
| F-10 | T038 | Low | `PinRequestResult.IconUnavailable` is unreachable from today's UI | 🟡 Open |
| F-11 | T054 | Info | Gate 3's regex matches `MutableInteractionSource` | 🟡 Open — the gate, not the code |
| F-12 | T065 | Info | Gate A's `^import android` also matches `androidx` | 🟡 Open — the gate, not the code |
| F-13 | T065 | **Medium** | An FR-025 violation from Phase 4 was fixed *in* Phase 5, which FR-035 reserves for moves | 🟡 **Open — a recorded FR-035 exception** |
| F-14 | T068/T070 | **Medium** | A one-shot message no longer survives a rotation, because a channel is consumed where a sentinel was not | 🟡 **Open — an intended but user-visible consequence of FR-038** |
| F-15 | T074 | Low | Two tests were deleted; deletion is not obviously covered by the "record, do not fix" rule | 🟡 Open — reversible |
| F-16 | T077 | Low | `isReturnDefaultValues` is gone, and seven KDocs that cited it were reworded | 🟡 Open — wording only |
| F-17 | T069/T070 | Info | The snackbar text now resolves through `LocalContext.current.resources`, not `stringResource` | 🟡 Open |

---

## F-01 — F1's fully-qualified name is asserted relatively, not against a literal

**Raised**: T003 (frozen-value audit) | **Severity**: High | **Blocks**: Phase 5 (the move)

**What it is.** `ShortcutContractTest` guards the most dangerous value in the codebase with:

```kotlin
assertEquals(ShortcutContract.LAUNCH_ACTIVITY, ShortcutLaunchActivity::class.java.name)
```

Both sides of that assertion move together.

| Change | Caught today? |
|---|---|
| Class moved, constant left alone | ✅ the assertion fails |
| Constant changed, class left alone | ✅ the assertion fails |
| Class moved **and** constant updated to match | ❌ **the suite stays green** |

**What it breaks.** The third row is what an IDE "Move class" refactor produces, and Phase 5 moves
nearly every file in the tree. The result is a green build that pins *new* shortcuts at the new
name and leaves every shortcut already on every home screen pointing at a component that no longer
exists. The user finds out by tapping an icon that does nothing, and SlowLock can neither enumerate
those shortcuts nor repair them. Contrast `EXTRA_TARGET_PACKAGE` one file away, which *is* asserted
against a raw literal.

**What fixing it would change for a user.** Nothing. It is one added assertion in a test file. No
source file changes, no behaviour changes, no persisted value changes.

**Proposed fix**, alongside the existing assertion, which stays — the two catch different failures:

```kotlin
assertEquals("com.slowlock.shortcut.ShortcutLaunchActivity", ShortcutContract.LAUNCH_ACTIVITY)
```

**Standing.** FR-013a already sanctions this class of change ("adding an assertion over a frozen
value … is NOT a structural change"), and no visibility change is needed — the constant is already
public. It is nonetheless **not in `tasks.md`**, so it stays unmade pending a specific confirmation.

**Note on the contract.** `contracts/frozen-values.md` describes the guard as "asserts the runtime
FQN against the constant", so the current form is what was specified rather than a drift from the
plan. This finding says the *specified* form is weaker than Principle VI requires.

**Ruling**: ✅ **Approved and applied, 2026-08-26.** The literal assertion now sits alongside the
existing one in `ShortcutContractTest`. Verified by mutation: setting the constant to
`"com.slowlock.feature.shortcut.ui.ShortcutLaunchActivity"` — the exact coordinated rename the old guard
missed — turns `./gradlew test` red at the new assertion, and the suite is green again with it
restored. Phase 5 then ran with the guard in place.

---

## F-02 — Kotlin 2.3.21 flags an unreachable cast in `SlowLockPaletteTest`

**Raised**: T018 | **Severity**: Low | **Blocks**: nothing

**What it is.** The upgraded compiler emits a new warning:

```
w: SlowLockPaletteTest.kt:69:72 This cast can never succeed.
```

at:

```kotlin
?: fail("screen_ground is missing from values/colors.xml") as Nothing
```

`org.junit.Assert.fail` is declared to return `void`, so `Unit as Nothing` can never succeed —
which Kotlin 2.3 now says out loud where 2.2.10 did not.

**What it breaks.** Nothing, at runtime. `fail()` always throws, so the cast is unreachable; the
`as Nothing` exists only to give the elvis branch a bottom type. The test behaves identically
before and after the upgrade, and it passes.

**Why it is recorded rather than fixed.** It is a pre-existing code pattern that the upgrade merely
surfaced — not an API break, so outside T018's remit ("resolve any API break … without changing
behaviour"), and not in any task. Under FR-011 it stays exactly as it is.

**What fixing it would change for a user.** Nothing. `error(...)` or `throw AssertionError(...)`
in place of `fail(...) as Nothing` would silence it with identical behaviour.

**Ruling**: _pending — no action proposed; the build is green with it_

---

## F-03 — The frozen-values contract lists `DEFAULT_SECONDS` as frozen; the source says it is not

**Raised**: T003 | **Severity**: Info | **Blocks**: nothing

`contracts/frozen-values.md` F2's table lists "Default when absent | `DelayConfig.DEFAULT`,
`DEFAULT_SECONDS = 10`" among the frozen forms. `DelayConfig.kt`'s own KDoc says the opposite:
"**Not frozen** — a changed default only affects apps that were never configured."

**The source is right.** A changed default cannot corrupt or orphan anything already on disk; it
only changes what an unconfigured app opens at. Its real obligation — that it stay a reachable
slider stop — is asserted by `DelayRangeTest`.

Recorded so a later reader comparing the audit against the contract table does not read the absence
of a literal assertion as a gap. A one-word correction to the contract table is available.

**Ruling**: _pending — documentation only_

---

## F-04 — T006 duplicates an assertion already in `DelayConfigTest`

**Raised**: T003 | **Severity**: Info | **Blocks**: nothing | **Owner**: T074

`DelayConfigTest.treatment tokens are frozen` and the new
`IconTreatmentTest.the constant names are frozen` (added by T006 as written) assert the same three
literals. After Phase 5 both files land in `core/domain`, at which point they are adjacent
duplicates.

**Deliberately not resolved now.** Pruning a test in Phase 2 would remove a guard while the code
under it is still moving. **T074** — Phase 7's test audit, whose job this is — decides which one
keeps its place.

**T074's ruling: both stay.** FR-048 names three things a test must be removed for — asserting only
what a constructor was given, restating the implementation, covering framework behaviour — and a
duplicated literal assertion is none of them. Both fire on a real defect, which T078 confirmed: an
IDE-style `Gray` → `Grey` rename that compiles clean turned `DelayConfigTest.treatment tokens are
frozen`, `DelayConfigTest.a known treatment token reads back as itself` and
`IconTreatmentTest.the constant names are frozen` red together.

The argument for pruning is single-source-of-truth; the argument against is that these are the
frozen-value assertions this entire feature's safety rests on, and deleting one during a refactor
whose contract is "these values must survive byte-identical" is the wrong trade at the wrong
moment. Belt and braces cost two lines.

**Ruling**: _pending — accept "both stay", or name which one to drop. Reversible either way; nothing
depends on the answer_

---

## F-05 — The treatment selection stayed in `rememberSaveable`

**Raised**: T046 | **Severity**: Medium | **A deliberate divergence from the task's wording**

T046 says `ShortcutConfigViewModel` owns "the treatment selection, the pin request and the
configuration write". It owns the second and third. The first stayed in `rememberSaveable` inside
`ShortcutConfigScreen`.

**Why.** The treatment's lifetime is *specified behaviour*, not an accident of where it lives:

| Event | Required | `rememberSaveable` in the root's holder | A `hiltViewModel()` holder |
|---|---|---|---|
| Rotation | choice kept | ✅ | ✅ |
| Process death on the screen | choice kept | ✅ | ❌ holder is new and empty |
| Back out, re-enter the same app | choice **discarded** | ✅ (root drops `CONFIG_KEY`) | ❌ Activity-scoped, choice persists |
| Configure a *different* app next | choice **discarded** | ✅ | ❌ carries forward |

Rows 3 and 4 are root obligation N3 — "an abandoned treatment does not survive the round trip and
reappear, for a different app". A holder obtained by `hiltViewModel()` is scoped to the Activity's
`ViewModelStore`, so it outlives the exit that is supposed to discard it. Keying a reset on the
package fixes rows 3 and 4 but breaks row 2, because the reset cannot tell process death from
re-entry.

**Precedent**: FR-023a makes exactly this argument for the root's `stage` — presentation state
whose existing mechanism already delivers a specified restore behaviour stays where it is.

**What reversing it would cost**: the four rows above have to be re-satisfied by some other
mechanism. I could not find one that satisfies all four.

**Ruling**: **closed — resolved by construction, not by a fix.** Feature 010 adopts the navigation
library, and a holder obtained inside a destination is scoped to that back stack entry rather than
to the `Activity`. The fourth column of the table above is no longer the choice it describes: an
entry-scoped `hiltViewModel()` satisfies all four rows, because rows 3 and 4 are delivered by the
entry being popped and rows 1 and 2 by the holder and its saved-state handle. The selection now
lives in `ShortcutConfigViewModel`, which is where T046 asked for it. Nothing was fixed here —
the constraint that made the divergence necessary was removed.

---

## F-06 — `RootViewModel` gained two repositories beyond the injection graph

**Raised**: T050 | **Severity**: Medium

`contracts/injection-graph.md` lists `RootViewModel` as injected with `DelayConfigRepository` and
`PinSupportRepository`. It now also takes `AppTargetRepository` and `AppIconRepository`, which it
exposes publicly and the root hands to `DelayConfigScreen`.

**Why.** Obligation V4 says `DelayConfigScreen` gets **no** state holder. But it resolves a label
and loads an icon, so it needs both repositories, and FR-024 forbids it constructing them at the
point of use. With no holder of its own, the root is the only place they can come from.

This is the same pattern `LocksViewModel.icons` already uses for rows that load their own icons, so
it is not a new idea in this codebase — but it is two more public members on the root's holder than
the contract anticipated.

**Alternatives rejected**: field-injecting them into `MainActivity` and threading them through
`SlowLockRoot` (more UI plumbing, same coupling); a Hilt `EntryPoint` in the composable (a service
locator, prohibited outright).

**Ruling**: **closed — resolved by construction, not by a fix.** Feature 010 gives the delay screen
`DelayConfigViewModel`, scoped to its own back stack entry, which reverses V4 as the alternative
above anticipated. That holder owns the delay being edited and the R8 load-versus-restore branch,
so it is not the forwarding-only holder V4 was written to prevent. `RootViewModel` now takes
`PinSupportRepository` alone, which is fewer collaborators than `injection-graph.md` listed —
`DelayConfigRepository` went with `configFor()`. No repository is exposed on another screen's
behalf anywhere in the app (010 contract S6).

---

## F-07 — `ElapsedClock` was added, and is not in `data-model.md`

**Raised**: T047 | **Severity**: Medium

A `fun interface ElapsedClock { fun nowMillis(): Long }` now sits beside `WaitTiming`'s pure
functions, provided in `ShortcutDataModule` as `SystemClock.elapsedRealtime()`.

**Why.** `WaitViewModel` has to call `remainingMillis(deadline, now)` *after* an asynchronous
configuration read, so "the caller supplies the time" — `repository-interfaces.md`'s rule — makes
the state holder the caller. A `SystemClock.elapsedRealtime()` inside it would put the wait's own
timing back out of reach of the JVM suite, and **T076 explicitly requires
`WaitViewModelTest` to drive the deadline behaviour**. The constitution also names the clock among
the sources that must be reached through a seam (Principle II, Repository).

**Why it is not the `DispatcherProvider` mistake D4 warns about**: a dispatcher already has a
qualifier supplying its seam, so an interface over it would be a *second*. The clock has none, so
this is the first.

**Cost**: one `fun interface` and one `@Provides`. No new file — it lives in `WaitTiming.kt`, which
`data-model.md` already moves to `feature/shortcut/domain/`.

**Ruling**: _pending_

---

## F-08 — The wait decides the hand-off; the window performs it

**Raised**: T047, T052 | **Severity**: Medium

T047 says `WaitViewModel` owns "resolve → read → wait → hand off". It owns the first three and
*decides* the fourth, emitting `WaitEvent.HandOff`. `ShortcutLaunchActivity` performs it.

**Why three things cannot move into the holder**:

1. Re-resolution at hand-off produces a platform `Intent`, which obligation O1 forbids crossing out
   of the domain.
2. The `STARTED` race guard reads *this window's* lifecycle — T052 requires it "carried across
   verbatim", and there is no other window to read.
3. Only an `Activity` can start another one.

The activity's package-manager lookup runs on an injected `@IoDispatcher`, so it stays off the main
thread exactly as it did and names no dispatcher (D1).

**Consequence**: `ShortcutLaunchActivity` is 256 lines rather than the ~80 a pure window would be,
because those three plus `onStop`, `onNewIntent` and the toast are all genuinely window business.

**Ruling**: _pending_

---

## F-09 — The app-list tap now requires a label, not just a launch intent

**Raised**: T043 | **Severity**: Medium | **This is a real behaviour divergence, however narrow**

`AppListViewModel.onAppTapped` used to call `resolveLaunchIntent(packageName) != null`. It now calls
`AppTargetRepository.resolve(packageName) != null`, and `resolve` returns `null` for **two**
reasons: no launch intent, *or* no label from `LauncherApps`.

**The divergence**: a package with a launch intent but no resolvable label.

| | Before | After |
|---|---|---|
| Tap | hands off | raises "no longer available", drops the row |
| Then | delay screen opens, shows "no longer available" | user stays on the list |

The user is told the same thing either way; **where** they are told it differs by one screen.

**How narrow.** `getLaunchIntentForPackage` returning non-null means the package has a LAUNCHER
activity, so `getActivityList` is non-empty and the label resolves. Reaching this needs a
`LauncherApps`/`PackageManager` disagreement — a locked work profile, or a `runCatching` that
caught a throw. Rare, not impossible.

**Why it was not avoided.** `AppTargetRepository` has exactly one method by contract, and `resolve`
is what "is this still available?" means in the approved design. Preserving the old behaviour
exactly would need a second method with one caller.

**What fixing it would change for a user**: nothing in the common case; in the rare case, the
message moves back one screen.

**Ruling**: _pending — accept, or add `isLaunchable()` to the repository_

---

## F-10 — `PinRequestResult.IconUnavailable` is unreachable from today's UI

**Raised**: T038 | **Severity**: Low

`ShortcutPinRepository.requestPin` loads the icon itself — obligation O1 forbids a `Bitmap` in the
signature — so it now has an outcome the old `pin(target, treatment, sourceIcon: Bitmap)` could not
have: no icon could be produced.

The screen's create action is disabled until an icon has loaded, so this branch cannot currently be
reached. It is not speculative generality in the strict sense — the repository genuinely produces
the outcome, and refusing to pin a blank is obligation C12 — but nothing reads it today, exactly as
nothing read the `Boolean` it replaced.

**Flagged for T074** alongside F-04, since that is where the suite's honesty is judged.

**Ruling**: _pending — no action proposed_

---

## F-11 — Gate 3's regex matches `MutableInteractionSource`

**Raised**: T054 | **Severity**: Info | **The gate, not the code**

`quickstart.md`'s third Stage 2 gate is:

```
grep -rn "remember.*\(Store\|Cache\|Pinner\|Source\)(" app/src/main/java --include=*.kt
```

Its only surviving match is `ScreenHeader.kt:131`, `interactionSource = remember { MutableInteractionSource() }` —
a Compose interaction primitive, untouched by this feature and not a data source.

All three gates pass on substance: every other match is prose inside KDoc explaining what was
removed. Recorded so the next person running the gates does not read this as a failure. Tightening
the pattern to `remember[^\n]*\b(DelayConfigStore|AppIconCache|LockStore|ShortcutPinner|InstalledAppsSource)\(`
would remove the false positive.

**Ruling**: _pending — documentation only_

---

## F-12 — Gate A's `^import android` also matches `androidx`

**Raised**: T065 | **Severity**: Info | **The gate, not the code**

`quickstart.md`'s FR-025 gate is `grep -rn "^import android" $SRC/*/domain/`. Its one match is:

```
core/domain/AppIconRepository.kt:3:import androidx.compose.ui.graphics.ImageBitmap
```

`androidx` begins with `android`, so the pattern catches Jetpack alongside the framework.
`ImageBitmap` is not an `android.*` type, and `repository-interfaces.md` **declares this exact
signature** — `suspend fun icon(packageName: String, versionCode: Long): ImageBitmap?` — so the
import is what the approved design asks for.

Adding the dot — `^import android\.` — makes the gate say what it means. Under that pattern the
gate passes with no matches at all.

**Ruling**: _pending — documentation only_

---

## F-13 — An FR-025 violation from Phase 4 was fixed inside Phase 5

**Raised**: T065 | **Severity**: Medium | **A recorded exception to FR-035**

**What happened.** Phase 5's own gate caught a violation Phase 4 had introduced:
`core/domain/AppTarget.kt` declared

```kotlin
resolveLaunchIntent: (String) -> Intent?
```

`android.content.Intent` in a `domain` file is what FR-025 prohibits outright. The seam had been
carried across from the pre-refactor `ShortcutTarget.kt`, where the file was not in a `domain`
package and the rule did not yet bite.

**What was changed**, in Phase 5:

```kotlin
isLaunchable: (String) -> Boolean          // was: resolveLaunchIntent: (String) -> Intent?
if (!isLaunchable(packageName)) return null // was: if (resolveLaunchIntent(packageName) == null)
```

with the one production caller now passing `{ packageManager.getLaunchIntentForPackage(it) != null }`.

**Why this is behaviour-preserving.** `resolveTarget` only ever compared the result to `null`. The
comparison moved from callee to caller; nothing else reads the intent. `AppTargetTest` drives both
branches and passes unchanged in substance (`{ null }` → `{ false }`, `{ Intent() }` → `{ true }`),
and it no longer imports a framework type at all — which makes the constitution's mandated
null-resolution coverage strictly purer than it was.

**Why it is nonetheless recorded.** **FR-035 says Phase 5 contains no logic change** — every task
is a move or a rename the compiler proves complete. A signature change is neither. The two
alternatives were both worse: leave the phase's own gate failing, or defer a known FR-025 breach
past the phase whose job is to establish the layering. It is recorded here rather than reported as
a clean move, which is what FR-012 asks for.

**Ruling**: _pending — accept the exception, or split it into its own step_

---

## F-14 — A one-shot message no longer survives a rotation

**Raised**: T068 / T070 | **Severity**: Medium | **An intended consequence of FR-038 that a user
can nonetheless see**

**What changed.** `AppListUiState.unavailableAppMessage` and `ShortcutConfigUiState.message` were
nullable `@StringRes Int` fields on a `StateFlow`, cleared by the screen calling
`onUnavailableMessageShown()` / `onMessageShown()` after showing the snackbar. Both are now a
`Channel<Int>(Channel.BUFFERED)` exposed as `receiveAsFlow()`, collected in a `LaunchedEffect`.
The clear-the-flag calls are gone.

**Why**: FR-038 and Constitution IV are explicit — "a one-shot event MUST use a consume-once
mechanism, never an observable state field". The old field was exactly the sentinel the rule names,
and the manual clear was the bug surface it warns about. `WaitViewModel` already used this shape.

**The behaviour difference.** Rotate the device while the snackbar is showing:

| | Baseline | Now |
|---|---|---|
| Snackbar visible, device rotated | The field is still set, so the message **re-appears** on the recreated screen | The value was consumed; the message does **not** re-appear |

Nothing else differs. The message still appears once per occurrence, the dead row is still dropped
in the same update, and the text is the same resource.

**Why it is recorded rather than reported as behaviour-preserving.** FR-001 says no user-visible
behaviour may differ, and this one does, in a narrow case. It is the *point* of FR-038 — a
re-appearing one-shot is the defect the rule exists to prevent — so the two requirements
genuinely pull against each other here and the maintainer owns the call. `manual-test-plan.md`
M5.5 already flags it as an expected, not-a-regression difference; this is the finding that entry
points at.

**If it must be preserved**, the mechanism would have to be a `SavedStateHandle`-backed sentinel
with an explicit clear — i.e. the thing FR-038 prohibits — so preserving it means an FR-038
exception, not a different implementation.

**Ruling**: _pending — accept the rotation difference under FR-038, or grant FR-038 an exception here_

---

## F-15 — Two tests were deleted by T074

**Raised**: T074 | **Severity**: Low | **Reversible**

T074 says to remove tests that "assert only what a constructor was given, restate the
implementation, or cover framework behaviour". Two were removed:

| Test | Why |
|---|---|
| `AppTargetTest.the resolved target keeps the package name it was asked about` | Asserted `target?.packageName == INSTALLED` — the value it had just passed in — and was already covered by `a resolvable package produces a populated target`, which asserts the whole `AppTarget` by equality two tests above it |
| `LockDeriveTest.declining leaves the locks that already existed untouched` | Byte-identical call and assertion to `there is no way for the app to hide a pinned lock` in the same class: `deriveLocks(cached = listOf("com.a"), pinned = setOf("com.a"))`. Its reading was folded into that test's comment rather than dropped |

**Why this is recorded.** The refactor's standing rule is *record, do not fix* (FR-011), and
deleting a test is not obviously on either side of that line — it is neither a silent correction
nor plainly within FR-048's three named categories for the second one. Both deletions are
one `git checkout` away from being undone, and no coverage moved: T075 and T078 both re-confirmed
the mandated areas after the removals.

**Ruling**: _pending — accept both removals, or restore either_

---

## F-16 — `isReturnDefaultValues` is gone, and seven KDocs were reworded

**Raised**: T077 | **Severity**: Low | **Wording, not behaviour**

T077 asked for `testOptions.unitTests.isReturnDefaultValues = true` to be removed "if nothing in
the suite still needs it". Nothing did: all 88 tests pass without it. Its comment claimed
"AppListViewModelTest needs to construct an Application", which stopped being true when that
ViewModel's `Application` was replaced by injected repositories in Phase 4.

**The consequential edit.** Seven KDocs across `main` and `test` cited the flag *as the reason*
their logic is pure — the argument being "a `SharedPreferences`-shaped test would assert nothing
while appearing to pass". With the flag gone the conclusion holds but the mechanism does not: such
a test now throws on the first unmocked framework call rather than passing vacuously. All seven
were reworded to cite the absent framework instead: `LockListTest`, `AppTargetTest`,
`IconTreatmentTest`, `LockOrderStore`, `IconTreatment`, `DelayConfigStore`, `DelayConfig`.

**Why it is recorded.** These are comment-only edits in files whose behaviour this feature promises
not to change, and they were made without being asked for by name. Removing the flag is arguably a
small hardening — a future test that touches `SharedPreferences` now fails loudly instead of
silently — which is a change in kind, not just in configuration.

**Ruling**: _pending — accept, or restore the flag and revert the seven comments_

---

## F-17 — The snackbar text now resolves through `resources`, not `stringResource`

**Raised**: T069 / T070 | **Severity**: Info

Both screens previously read the resource id off the state during composition and resolved it with
`stringResource(messageRes)`. The id now arrives inside a coroutine, where no composable can be
called, so it is resolved with `LocalContext.current.resources.getString(messageRes)` and the
collector's `LaunchedEffect` is keyed on `viewModel` and `resources`.

Same resource, same string, same locale — `LocalContext` carries the current configuration. The
one difference worth naming: a configuration change swaps the `resources` instance and therefore
restarts the collector, which is the correct behaviour (the next message resolves in the new
locale) but is a restart that did not previously happen.

**Ruling**: _pending — informational; no action proposed_

---

## Closed

- **F-01** — F1's frozen FQN now carries a literal assertion. Approved 2026-08-26, applied,
  mutation-verified, and in place before the Phase 5 move began.
