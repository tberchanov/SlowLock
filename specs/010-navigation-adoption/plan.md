# Implementation Plan: Navigation Adoption

**Branch**: `main` (no feature branch; Principle VII reserves branch creation to the maintainer) | **Date**: 2026-08-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/010-navigation-adoption/spec.md`

## Summary

Replace the hand-built navigation in `SlowLockRoot.kt` with `androidx.navigation:navigation-compose`,
and let the resulting back stack entries become the scope for every screen's state holder. Those two
changes are one piece of work: entry-scoped holders have no entries to scope to until the graph
exists, and the two workarounds constitution v3.0.0 forced on feature 009 — the icon treatment held
outside its holder, and two repositories exposed on the root holder for a screen that had none —
exist *only* because holders were activity-scoped. Adopting the graph dissolves both.

The work runs in four stages: **the graph replaces the stage machine**, then **holders move to their
entries**, then **comments, tests and the record**, then **verification**. The first two are the only
ones that can change what a user sees, and exactly three such changes are approved in advance
(FR-002).

The technical approach is constrained more by what must stay identical than by what must move. No
file changes package, no persisted value is touched, and the library's two most tempting defaults —
inter-destination transitions and deep links — are both deliberately declined, because accepting
them would be a product change wearing a refactor's clothes.

## Technical Context

**Language/Version**: Kotlin 2.3.21, JVM target 11, JDK 17 toolchain. Unchanged by this feature.

**Primary Dependencies**: Jetpack Compose (BOM 2026.08.00), Material 3, androidx.lifecycle 2.11.0,
Hilt 2.60.1 with KSP 2.3.11, kotlinx-coroutines 1.11.0 — all unchanged. **Added**:
`androidx.navigation:navigation-compose` **2.10.0**, the Kotlin serialization Gradle plugin
**2.3.21**, `org.jetbrains.kotlinx:kotlinx-serialization-core` **1.11.0**. Every version verified
against Google Maven, Maven Central and the Gradle Plugin Portal on 2026-08-27 — see research
R1-R4 for the commands and their output.

**Storage**: two `SharedPreferences` files, both frozen, both untouched by this feature. Persistence
mechanism deliberately unchanged — see D7 in the spec's Clarifications and FR-026.

**Testing**: JVM unit tests only (`./gradlew test`), JUnit4 4.13.2 with kotlinx-coroutines-test
1.11.0. Two tests are added, both for the single new branch research R8 identifies. No test asserts
navigation behaviour — that is framework behaviour, which Principle VI prohibits testing (FR-034).

**Target Platform**: Android, `minSdk 26`, `targetSdk`/`compileSdk` 37 (unchanged).

**Project Type**: Single-module Android application (`:app`).

**Performance Goals**: No numeric budget. The qualitative bar carried over from 009 still holds:
tapping a pinned icon shows the wait screen with no perceptible pause, and the wait ends at the
configured moment. This feature does not touch that path — `ShortcutLaunchActivity` gains no graph.

**Constraints**: No user-visible change except the three FR-002 approves, plus the cross-fade the
maintainer directed on 2026-08-27 (research R10's reversal, G11). Four frozen values must survive
byte-identical. No new permission, no new capability, no deep link.

**Scale/Scope**: ~60 Kotlin source files, ~7,100 lines, 14 test files. One file rewritten
(`SlowLockRoot.kt`), two new files, eight files edited, no file moved.

## Constitution Check

*GATE: evaluated before Phase 0 research; re-evaluated after Phase 1 design. Both passes recorded.
Constitution version 4.0.0, dated 2026-08-27.*

| Principle | Pre-Phase 0 | Post-Phase 1 | Notes |
|---|---|---|---|
| I. Modern Stack, Current Tooling | PASS | PASS | Three dependencies added, each first-party or `kotlinx`, each a stable release on a maintained line, each declared in `gradle/libs.versions.toml` and recorded with what breaks without it (research R1, R2). Navigation 3 was evaluated and rejected **on merits**: it is stable at `1.1.7` and therefore eligible under the spec's rule, but the zero-edit `hiltViewModel()` scoping this design rests on is published for 2.x only, and nav3 moves back-stack persistence to the application (research R1). No existing version moves; the BOM already exceeds navigation's Compose floor (R4). |
| II. Layered Architecture | PASS | PASS (1 recorded) | Every screen holder is obtained inside its destination, so the entry is its scope (FR-015, contract S1). `DelayConfigViewModel` is added so the delay screen owns what it edits, which removes the two repositories 009 had to expose on the root holder. Recorded: `RootViewModel` stays above the graph, holding pin support — deliberate, commented, and covered by the principle's own clause, but recorded because "activity-scoped holder" is the phrase the amendment made a defect. |
| III. Feature First, Layers Inside | PASS | PASS | No file changes package. Two new files land in packages that already exist: `feature/delay/ui/` for the new holder, and the root package for the route declarations — which the entry-point clause sanctions, on the same argument that already puts `SlowLockRoot` there (research R7). `ShortcutLaunchActivity` keeps its frozen fully-qualified name and stays outside the graph. Tests still mirror main package for package. |
| IV. Structured Concurrency | PASS | PASS | No new asynchrony. The new holder uses `viewModelScope` and injected repositories that are already main-safe; no dispatcher is named at a call site; no new `runCatching`, `GlobalScope`, `runBlocking` or blocking call. `collectAsStateWithLifecycle()` now follows the *entry's* lifecycle rather than the Activity's, which is stricter, not looser (research R5). |
| V. Standard Solutions, SOLID, SoC, SSoT, KISS | PASS | PASS (1 recorded) | The standard solution wins on mechanism: the bespoke `when`, the hand-written back-stack serialiser and the hand-managed retention holder are all deleted. KISS then governs how much of it is used — no wrapper around the navigation controller, no forwarding-only holder, no deep links, and the library's transitions declined because accepting them changes what a user sees (R10). Single source of truth improves: the delay lives in one holder rather than in a stage the root mutates. Recorded: the pin-support gate stays a `when` above the graph. |
| VI. Tests That Earn Their Keep | PASS | PASS | No test asserts that the library navigates, retains an entry or restores a history (FR-034). Two tests are added, both on the one branch a test can get wrong — a restored saved-state handle must win over a fresh read (R8) — and each fails against the plausible wrong implementation. Mandated coverage is untouched: nothing this feature changes goes near a frozen value, a schedule, or target resolution. Three stale verification checks are corrected or removed (FR-037). |
| VII. Version Control Is the Maintainer's | PASS | PASS | No branch created. No commit, push or tag will be performed. Work is left in the working tree and offered. Any `tasks.md` entry saying to commit is a note to the maintainer. |
| VIII. Comments That Earn Their Place | PASS | PASS | Ten comment sites are made false by this work and are corrected or deleted in the same change (FR-027, listed in the spec). Two new comments are *required*, because in both cases the code cannot say the reason: why `RootViewModel` deliberately outlives every screen, and why the pin-support gate is not a destination. |

**Gate result: PASS.** Complexity Tracking carries two entries, down from 009's five. Both are cases
where the compliant answer *reads* like the construct the amendment prohibits, and a reader is owed
the distinction. Neither is a rule left unsatisfied.

**Re-evaluated after Phase 1 design.** The design added no violation. It removed two: findings F-05
and F-06, and with them 009's `ShortcutConfigScreen` and `DelayConfigScreen` Complexity Tracking
rows, are resolved by construction rather than by a fix. 009's third superseded row — the
`rememberSaveable` navigation stage — is resolved by the stage ceasing to exist.

**Re-evaluated after implementation (T049).** What actually survived, against what was anticipated:

| Principle | Shipped | What differed from the anticipation |
|---|---|---|
| I | PASS | Nothing. The three versions resolved as researched, and `assembleRelease` needed no keep rule of its own — `kotlinx-serialization` and `navigation-common` carried their own. |
| II | PASS (1 recorded) | Nothing structural. Both new holders read their route argument out of `SavedStateHandle` **by the route property's key name** rather than through `toRoute`, which is recorded as a finding: `toRoute(SavedStateHandle)` builds a `SavedState` internally and is therefore unreachable from a JVM test, which would have put the R8 branch beyond `./gradlew test`. |
| III | PASS | Nothing. Two new files, no package created, no file moved, no feature importing another's `ui` or `data` — and, as it turned out, no feature importing the root's `Routes.kt` either. |
| IV | PASS | Nothing. `LocksViewModel.refresh()` lost its `Job` return type along with the `join()` that was its only consumer — a returned handle nothing awaits is the speculative generality Principle V names, and it became one the moment R9's re-read replaced the wait. |
| V | PASS (1 recorded) | Nothing. The `when`, the `listSaver`, the `SaveableStateHolder`, the four retention keys and both hand-managed pops are gone; no wrapper, no forwarding holder, no deep link, and all four transitions declined. |
| VI | PASS | Seven test cases across the two new holders rather than the two anticipated. Each of the two R8 branches was verified by mutation — swapping the branch turns exactly one case red — and the extra cases cover the *write* half of the same branch, without which the restore case would pass against a holder that never saved. No test asserts that the library navigates. |
| VII | PASS | Nothing. No branch, no commit. T053 is left unchecked. |
| VIII | PASS | Two comment sites more than the ten anticipated: `LocksUiState`'s KDoc linked `[com.slowlock.Stage.Home]`, a symbol this feature deletes, and `ShortcutConfigScreen`'s step-3 comment asserted its `BackHandler` was unchanged. Gate 5 caught the first; the second was corrected in the change that falsified it. |

## Project Structure

### Documentation (this feature)

```text
specs/010-navigation-adoption/
├── plan.md                      # This file
├── spec.md                      # The approved specification
├── research.md                  # Phase 0 output — 14 decisions, each with the command that verified it
├── data-model.md                # Phase 1 output — routes, holders, what is added and deleted
├── quickstart.md                # Phase 1 output — how to run the stages and the five gates
├── contracts/
│   ├── navigation-graph.md      # Destinations, edges, and obligations G1-G12
│   └── state-scope.md           # Every holder × rotate / die / pop / revisit, and rules S1-S7
├── checklists/
│   └── requirements.md          # Spec quality checklist (from /speckit-specify)
└── tasks.md                     # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

Only the deltas. Every file not listed is unchanged, and **no file moves**.

```text
app/src/main/java/com/slowlock/
├── Routes.kt                       # NEW — Home, AppList, DelayConfig, ShortcutConfig
├── SlowLockRoot.kt                 # REWRITTEN — NavHost + the pin-support gate; ~358 lines → ~120
├── RootViewModel.kt                # loses targets, icons, configFor(); gains the FR-016 comment
├── MainActivity.kt                 # unchanged
├── shortcut/
│   └── ShortcutLaunchActivity.kt   # unchanged — outside the graph, frozen FQN
├── feature/
│   ├── apps/ui/AppListScreen.kt    # loses its BackHandler
│   ├── delay/ui/
│   │   ├── DelayConfigScreen.kt    # loses targets/icons params and its BackHandler; observes a holder
│   │   └── DelayConfigViewModel.kt # NEW — the delay being edited, the target, the icon, the R8 branch
│   ├── locks/ui/LocksViewModel.kt  # refresh() KDoc only
│   └── shortcut/ui/
│       ├── ShortcutConfigScreen.kt # loses initialTreatment and its rememberSaveable and BackHandler
│       └── ShortcutConfigViewModel.kt # gains the treatment selection
└── ui/                             # unchanged

app/src/test/java/com/slowlock/
├── feature/delay/ui/DelayConfigViewModelTest.kt        # NEW
└── feature/shortcut/ui/ShortcutConfigViewModelTest.kt  # NEW

gradle/libs.versions.toml            # + navigation, serialization plugin, serialization-core;
                                     #   the anti-navigation comment corrected
app/build.gradle.kts                 # + the serialization plugin and two dependencies
CLAUDE.md                            # repointed at this plan
specs/009-constitution-alignment/findings.md   # F-05 and F-06 ruled and closed
```

**Structure Decision**: unchanged from 009 — a single `:app` module with feature-first packages and
layer subpackages inside each. This feature moves no file and creates no package. The two new files
land where their owners already live: the delay holder inside the delay capability's `ui` layer, and
the route declarations in the root package alongside `SlowLockRoot`, which belongs to no capability
and which Principle III's entry-point clause already places there. Splitting the routes across the
four features was considered and rejected in research R7: it produces four one-declaration files and
buys no boundary, because every navigation call in this app lives in the root.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| **`RootViewModel` is scoped to the Activity**, not to a navigation entry — the exact shape Principle II's amendment turned into a defect | It holds one thing: whether this launcher can pin shortcuts. That is a whole-app precondition deciding whether the graph renders at all, re-read on every return to the foreground because the user can change launcher while the app is away. It belongs to no screen, so there is no entry that could scope it. Principle II's own clause covers exactly this — state outliving its screen MUST be deliberate and commented, naming the behaviour requiring it — and the declaration carries that comment (FR-016). | Making the gate a destination and giving it an entry-scoped holder was rejected: it would push and pop a destination in response to a lifecycle signal, which is more mechanism than the behaviour needs and which KISS governs under Principle V's conflict order. Folding pin support into the `Home` destination's holder was rejected too — the gate must render *instead of* the graph, including instead of `Home`, so a holder inside the graph is too late to decide it. Recorded rather than omitted because "activity-scoped holder" is the phrase the amendment made a defect, and a reader who finds this one is owed the distinction. |
| **The pin-support gate is a `when`** sitting above the `NavHost`, in the same file that just had a prohibited `when` deleted from it | Three outcomes, one of which is "render nothing yet" and one of which is "render this instead of the whole graph". None is a screen the user navigates to, and none belongs in a back stack: the `Unsupported` state must leave the stack untouched so the user's place returns when support does — which is behaviour 005 specified and this feature preserves exactly (contract, "The pin-support gate"). | Expressing it as a destination pushed and popped on the lifecycle signal was rejected on the reason above, and because it would put a lifecycle-driven `navigate` call in the one place a spurious extra entry is hardest to notice. Splitting it into a separate composable above `SlowLockRoot` was rejected as moving the reader's confusion rather than answering it. The answer is the comment FR-012 requires: this `when` is over a precondition, not over a screen. |

### Resolved during planning

**Navigation 3 is eligible and was rejected on merits.** The spec's Clarifications left the
artifact open on purpose and set an eligibility rule — latest stable line, checked on the day.
Phase 0's first pass misread AndroidX's `<release>` field as the newest stable and concluded nav3
had no stable line at all. It has two: `1.0.x` and `1.1.x`, newest stable `1.1.7`. The eligibility
rule therefore admits both generations and decides nothing, and R1 now carries the actual
comparison — the Hilt injection path, back-stack ownership, and the cost of rewriting both
contracts. The outcome is unchanged; the reason is different, and the previous reason was false.

**Consequence to watch**: ground 1 above expires the day a first-party Hilt integration for nav3
publishes. Re-run R1's comparison then rather than treating this row as settled.

**Neither pre-approved fallback fired (T048, FR-042).** Both are taken only on device evidence, and
the device checkpoints (T017, T025, T032, T044-T047) were run by the maintainer on 2026-08-27 with
no difference beyond the three FR-002 approves. Neither trigger was observed:

| Fallback | Status | What would trigger it |
|---|---|---|
| Graph-scoped `LocksViewModel` via `hiltViewModel(parentEntry)`, pulling in `hilt-navigation-compose` (R9) | **Not taken** | The lock list visibly one frame stale after the flow returns — manual case N5.2 |
| The loaded delay carried as a `DelayConfig` route argument (R8/D5) | **Not taken** | The delay screen visibly flashing a default rather than withholding — manual cases N4.1, N4.2 |

Both rows stand on the manual pass, not on an assumption. If either behaviour appears in a later
device session, the fallback is taken then and recorded here with the case that produced it.

**Of the two library defaults declined at planning time, one was reinstated and one still stands.**
Deep links remain declined (R14). Transitions were declined (R10) and then **enabled on maintainer
direction after the manual pass** — as a cross-fade rather than the library's default slide. That
makes a fourth user-visible difference beyond FR-002's three, deliberate rather than a defect under
SC-004, and it puts manual case N2 back in the queue. R10 carries the full reversal note.
