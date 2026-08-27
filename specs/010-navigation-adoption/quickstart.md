# Quickstart: Navigation Adoption

**Date**: 2026-08-27 | **Plan**: [plan.md](./plan.md)

How to run this feature and know it worked. Read [plan.md](./plan.md) for the shape,
[research.md](./research.md) for why each decision went the way it did, and
[contracts/](./contracts/) for the obligations each gate checks.

## Before anything is changed

**Nothing blocks Stage 1.** All seven decisions were approved before the specification was written,
and Phase 0 verified every version against the publishing repository (research R1-R4).

**Capture the baseline.** The maintainer, on a device:

1. Build and install current `main`.
2. Create at least two locks with different delays and different icon treatments. Pin both.
3. Screenshot every screen: intro, locks list, app list, delay config, shortcut config, wait.
4. Note how the app list looks when you leave it and come back — G5 is the one place this feature
   deliberately changes that, and the baseline is what makes the difference reviewable.
5. Keep that install. It is the in-place-update fixture the final gate needs (SC-002) — do not
   uninstall it at any point during the feature.

## The four stages

Every step ends with `./gradlew assembleDebug` and `./gradlew test` both passing (FR-038). Nothing
is committed by the agent — each stage ends with changes in the working tree and an offer
(Principle VII, FR-044).

### Stage 1 — The graph replaces the stage machine

Dependencies into the catalog, the four route types, the `NavHost`, and every deletion in
[data-model.md](./data-model.md) §5 except the two that belong to Stage 2. Screen signatures do not
change yet; their callbacks call `navigate` and `popBackStack` instead of assigning a stage.

```bash
./gradlew assembleDebug
./gradlew test
```

**Gate 1 — the prohibited constructs are gone.** All four must return nothing:

```bash
SRC=app/src/main/java/com/slowlock
grep -rn 'sealed interface Stage\|StageSaver\|enum class Origin' $SRC
grep -rn 'rememberSaveableStateHolder\|SaveableStateProvider\|removeState(' $SRC
grep -rn 'BackHandler' $SRC
grep -rn 'listSaver' $SRC
```

**Gate 2 — the graph has no transitions** (G11, research R10). The `NavHost` call must set all four
transition parameters to none; a bare `NavHost(...)` is a user-visible change FR-002 does not
approve.

### Stage 2 — Holders move to their entries

`DelayConfigViewModel` is added; the treatment moves into `ShortcutConfigViewModel`; `RootViewModel`
loses `targets`, `icons` and `configFor()`; the two screens lose the parameters that carried them.

**Gate 3 — no holder above the graph but the root's.** The only `hiltViewModel()` outside a
destination is `RootViewModel`:

```bash
grep -rn 'hiltViewModel()' app/src/main/java/com/slowlock
```

**Gate 4 — no screen keeps state its holder should own** (S5). The only surviving `rememberSaveable`
in a feature screen should be a list state or an equivalent UI-only value; a treatment, a delay or
any domain value there is a defect:

```bash
grep -rn 'rememberSaveable' app/src/main/java/com/slowlock/feature
```

### Stage 3 — Comments, tests and the record

FR-027's comment list, the two new holder tests (research R8), the corrected verification checks
(FR-037), the F-05 and F-06 rulings, and `CLAUDE.md` repointed.

**Gate 5 — no comment describes a removed mechanism.** Each of these must return nothing:

```bash
SRC=app/src/main/java/com/slowlock
grep -rni 'stage' $SRC
grep -rni 'scoped to the activity\|Activity.s store\|SaveableStateHolder' $SRC
grep -n 'does not want\|does not have' gradle/libs.versions.toml   # the refusal wording, not the coordinate
```

`grep -rni 'stage'` will also match the `1 / 3` step wording and anything legitimately about a flow
step — read the matches, do not just count them.

### Stage 4 — Verification

```bash
./gradlew assembleDebug
./gradlew test
./gradlew assembleRelease     # research R13 — this gate exists because R8 can strip a route serializer
```

**Gate 6 — no user-facing string moved** (FR-005, SC-003). Must return nothing:

```bash
git diff --stat -- app/src/main/res/values*/strings.xml
```

A reworded string is a user-visible difference FR-002 does not approve, and it is the one such
difference no device case reliably catches — a reviewer reads what the screen says, not what it
said last week.

Then the manual plan, by the maintainer, on a device. See `manual-test-plan.md` (produced by
`/speckit-tasks`). At minimum it must cover FR-041's list:

| What | Obligation |
|---|---|
| Back from the delay step, entered from the app list | G1 |
| Back from the delay step, entered by editing a lock | G1 |
| Back from the icon step returns the delay chosen on the way through | G2 |
| Back from the icon step discards the treatment chosen there | G3 |
| App list scroll and query survive the round trip | G4 |
| App list opens fresh after being left entirely | G5, **FR-002(a)** |
| Process death on each of the four destinations | G6 |
| System back and the on-screen control do the same thing, on every screen | G7 |
| Back on the root screen leaves the app | G8 |
| Completing the flow returns home with the flow popped — back cannot re-enter it | G9 |
| No animation between steps | G11 |
| A newly pinned lock is on the list when the flow returns | **FR-002(b)** |
| The delay screen opens on the saved value, never a default first | **FR-002(c)** |
| Treatment kept across rotation and across process death | state-scope §`ShortcutConfigViewModel` |
| Pin support lost and regained returns the user to where they were | FR-012 |
| One complete flow on a **release** build | research R13 |
| In-place update over the baseline install: every lock and every pinned icon still works | SC-002 |

Plus the manual test plans of features 001-005 and 007 in full (SC-001). Exactly three cases may
differ from their recorded expected result, and each must be one of FR-002's three.

## If a fallback fires

Two are pre-approved and both are taken **only on device evidence** (FR-042):

- **The lock list is visibly one frame stale** after the flow returns → scope `LocksViewModel` to
  the graph's route entry, which pulls in `androidx.hilt:hilt-navigation-compose:1.4.0` (research
  R3, R9). Record it as a deviation.
- **The delay screen visibly flashes** rather than withholding → keep the configuration read on the
  holder of the screen that initiated it and pass the loaded value as a `DelayConfig` route
  argument (research R8, D5). Record it as a deviation.

Anything else a user can see is a finding, not a fallback. Record it, do not fix it, and wait for a
ruling (FR-031, FR-032).

## What must never change

Restated so it is in front of whoever runs these gates: the pinned entry point's fully-qualified
name, both preferences file names and their key shapes, the shortcut identifier scheme, and the
intent extra name. `ShortcutContractTest`'s literal assertion is what turns an accidental move into
a red build instead of a dead icon on a user's home screen. If it goes red, stop.
