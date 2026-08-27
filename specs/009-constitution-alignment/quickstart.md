# Quickstart: Constitution Alignment Refactor

**Date**: 2026-08-26 | **Plan**: [plan.md](./plan.md)

How to run this feature and know it worked. Read [plan.md](./plan.md) for the shape and
[research.md](./research.md) for why each decision went the way it did.

## Before anything is changed

**Nothing blocks Stage 1.** The Kotlin version question raised by research R2 is settled:
**Kotlin 2.3.21**, confirmed by the maintainer on 2026-08-26, because the newest KSP (2.3.11) is
built against Kotlin 2.3.20 and Hilt 2.60.1 against 2.3.21 — there is no KSP release on the Kotlin
2.4 line.

**Capture the baseline** (FR-008, research R14). The maintainer, on a device:

1. Build and install current `main`.
2. Create at least two locks with different delays and different icon treatments. Pin both.
3. Screenshot every screen: intro, locks list, app list, delay config, shortcut config, wait.
4. Keep that install. It is the in-place-update fixture the final gate needs (SC-002) — do not
   uninstall it at any point during the feature.

## The four stages

Each stage is a set of steps. Every step ends with `./gradlew assembleDebug` and `./gradlew test`
both passing (FR-051). Nothing is committed by the agent — each stage ends with changes in the
working tree and an offer (Principle VII, FR-055).

### Stage 1 — Toolchain

Versions to their targets, Hilt and KSP plumbing, `SlowLockApplication`, dead instrumented-test
config removed. **No structural change of any kind** — FR-053b depends on this stage being
bisectable on its own.

```bash
./gradlew assembleDebug
./gradlew test
./gradlew assembleDebug     # second run: confirms the configuration cache still serves
```

Then the **smoke pass** (FR-018), by the maintainer on a device:

- the app launches;
- the intro and the locks list render;
- one complete pass through the create-a-lock flow, including the pin dialog;
- one tap of an already-pinned icon through the wait to the hand-off, judged against FR-001b —
  no perceptible pause before the wait screen, and the wait ends when it should.

Any rendering difference here is recorded and ruled on under FR-001a: a changed library default may
be accepted, a regression is corrected.

### Stage 2 — Seams

Repository interfaces and implementations, constructor injection, composables and
`ShortcutLaunchActivity` emptied of data and platform access. **Files stay where they are** — this
stage changes contents, the next one changes paths, and keeping them apart is what makes both
diffs readable.

Review gates for this stage:

```bash
# no dispatcher named at a call site
grep -rn "Dispatchers\.\(IO\|Default\)" app/src/main/java --include=*.kt | grep -v CoreDataModule

# no Application-typed state holders left
grep -rn "AndroidViewModel\|@JvmOverloads" app/src/main/java --include=*.kt

# no data source constructed in a composable
grep -rn "remember.*\(Store\|Cache\|Pinner\|Source\)(" app/src/main/java --include=*.kt
```

All three must return nothing.

### Stage 3 — Move

The package rearrangement from [data-model.md](./data-model.md), tests moved with their subjects.
**No logic change** (FR-035). `ShortcutLaunchActivity.kt` does not move (F1).

```bash
./gradlew test
grep -rn "com.slowlock.compat" app/src        # must return nothing
grep -c "keepnames class com.slowlock.shortcut.ShortcutLaunchActivity" \
  app/src/main/keepRules/rules.keep           # must be 1
```

The layering gates. All three must return nothing:

```bash
SRC=app/src/main/java/com/slowlock

# FR-025 — no platform import inside a domain package
grep -rn "^import android" $SRC/core/domain/ $SRC/feature/*/domain/

# FR-026 — a domain file may not import a ui or data package
grep -rnE "^import com\.slowlock\.(core|feature\.[a-z]+)\.(ui|data)\." \
  $SRC/core/domain/ $SRC/feature/*/domain/

# FR-030, SC-008 — no capability may import another capability's ui or data.
# `core`, `ui.components` and `ui.theme` are permitted and deliberately not matched.
for c in apps delay locks shortcut; do
  others=$(printf '%s\n' apps delay locks shortcut | grep -v "^$c$" | paste -sd'|' -)
  grep -rnE "^import com\.slowlock\.feature\.($others)\.(ui|data)\." "$SRC/feature/$c/"
done

# Principle III — no capability left outside the feature namespace. The lone exception is
# ShortcutLaunchActivity.kt, whose frozen FQN outranks the shape.
ls $SRC | grep -vxE 'core|feature|shortcut|ui|[A-Z].*\.kt'
ls $SRC/shortcut | grep -vx 'ShortcutLaunchActivity.kt'
```

The root package is deliberately out of scope for the third gate: `SlowLockRoot`, `RootViewModel`
and `MainActivity` are entry points that arbitrate between capabilities, so importing a
capability's `ui` is what they are for. The loop scans only the four capability directories.

Then check every value in [contracts/frozen-values.md](./contracts/frozen-values.md) by hand. The
compiler cannot catch a constant that was changed consistently everywhere.

### Stage 4 — Settle

One-shot events off the `StateFlow` sentinel, one state owner per screen, the test suite pruned
against FR-048, `isReturnDefaultValues` removed if nothing needs it.

## The final gate

By the maintainer, on a device (FR-053a):

1. **In-place update** over the baseline install from before Stage 1 — not a fresh install. Every
   lock still listed with its delay and treatment; every pinned icon still launches its target.
2. This feature's own manual test plan, in full.
3. The six app-relevant legacy plans in full: features 001, 002, 003, 004, 005 and 007. The plans
   for 006 and 008 cover the marketing site and are out of scope.
4. Compare against the Stage 0 screenshots.

## What to watch

| Risk | Where | Why it is easy to miss |
|---|---|---|
| The wait moving to a `WaitViewModel` | research R10 | Rotation must not restart the wait, and abandonment must still cancel. The mechanism changes from an instance-state bundle to `SavedStateHandle`; the behaviour must not. |
| `SaveableStateHolder` keys | research R9 | Scroll position and query must survive the round trip to the Locks screen; the two configuration screens' state must not. Silent when wrong. |
| `null` vs empty set | contracts/repository-interfaces.md | Conflating them empties the user's whole lock list on a bad read. |
| Treatment enum constant names | contracts/frozen-values.md, F2 | They are the persisted token. Moving the enum is safe; renaming a constant resets every configured app. |
| The R8 keep rule | research R13 | A wrong keep rule fails only in a shrunk release build, on a user's device. |
