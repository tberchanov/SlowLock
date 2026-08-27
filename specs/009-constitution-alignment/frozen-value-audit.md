# Frozen Value Audit (T003)

**Date**: 2026-08-26 | **Task**: T003 | **Contract**: [contracts/frozen-values.md](./contracts/frozen-values.md)

Every value in F1–F4 audited against the source and test suite on unmodified `main`
(reference build confirmed green in T001: `assembleDebug` ✓, `test --rerun-tasks` ✓ — 79 tests
across 13 classes).

The question this audit answers is the constitution's, Principle VI: *is every frozen persisted
value asserted against a literal, so a rename fails the build?* Nothing else — an assertion that
compares two things that both move together is recorded here as **not** a literal assertion, because
it does not have the property the rule exists for.

## Summary

| Value | Carrier | Literal assertion | Verdict |
|---|---|---|---|
| **F1** Launch activity FQN | `ShortcutContract.LAUNCH_ACTIVITY` | Relative only | ⚠️ **Gap — see G2** |
| **F1** Manifest `android:name` | `AndroidManifest.xml` | None (not assertable on JVM) | Accepted — see note |
| **F1** R8 keep rule | `keepRules/rules.keep` | None (not assertable on JVM) | Accepted — T020 verifies |
| **F2** Preferences file | `DelayConfigStore.FILE` (private) | **None** | ❌ **Gap — T004/T005** |
| **F2** Delay key shape | `delayKey()` | ✅ `"com.example.app.delaySeconds"` | Covered |
| **F2** Treatment key shape | `treatmentKey()` | ✅ `"com.example.app.treatment"` | Covered |
| **F2** Treatment tokens | `IconTreatment` names | ✅ `listOf("Original","Invert","Gray")` | Covered — see G3 |
| **F2** Default when absent | `DelayConfig.DEFAULT_SECONDS` | Relative only | ℹ️ Not frozen — see G4 |
| **F3** Locks file | `LOCKS_FILE` | ✅ `"slowlock.locks"` | Covered |
| **F3** Locks key | `LOCKS_KEY` | ✅ `"packages"` | Covered |
| **F3** Separator | `LOCKS_SEPARATOR` | ✅ `"\n"` | Covered — **T007 is already satisfied** |
| **F4** Shortcut ID scheme | `ShortcutContract.shortcutId()` | ✅ identity over a literal package | Covered |
| **F4** Intent extra | `ShortcutContract.EXTRA_TARGET_PACKAGE` | ✅ `"com.slowlock.shortcut.extra.TARGET_PACKAGE"` | Covered |
| **F4** Intent action | `ShortcutContract.ACTION` | ✅ `"android.intent.action.VIEW"` | Covered |

**Ten of thirteen assertable values carry a literal assertion. Two gaps and one duplicate.**

---

## F1 — The pinned entry point's fully-qualified name

**Value**: `com.slowlock.shortcut.ShortcutLaunchActivity`

**Where it lives** (all three confirmed present):

| Site | File | Form |
|---|---|---|
| Constant | `shortcut/ShortcutContract.kt:…` | `const val LAUNCH_ACTIVITY = "com.slowlock.shortcut.ShortcutLaunchActivity"` |
| Manifest | `AndroidManifest.xml` | `android:name=".shortcut.ShortcutLaunchActivity"` |
| Keep rule | `keepRules/rules.keep` | `-keepnames class com.slowlock.shortcut.ShortcutLaunchActivity` |
| Pin site | `shortcut/ShortcutPinner.kt:152` | `ComponentName(context, ShortcutLaunchActivity::class.java)` — resolves at build time |

**Assertion present**: `ShortcutContractTest.the launch activity's fully-qualified name is frozen`:

```kotlin
assertEquals(ShortcutContract.LAUNCH_ACTIVITY, ShortcutLaunchActivity::class.java.name)
```

**Verdict: relative, not literal.** See **G2** below.

**Manifest and keep rule**: neither is reachable from a JVM unit test, so neither can carry a
literal assertion and that is not recorded as a gap. The keep rule is covered by **T020**, which
verifies it against a release build's mapping output. The manifest's `android:name` is covered
transitively: it is a relative path (`.shortcut.ShortcutLaunchActivity`) resolved against the
`com.slowlock` namespace, and a class move that left it stale would fail the build.

---

## F2 — The delay configuration store

**Preferences file `slowlock.delay-config`** — the known gap Phase 2 exists to close.

```kotlin
// app/src/main/java/com/slowlock/delay/DelayConfigStore.kt:13
private const val FILE = "slowlock.delay-config"
```

`private` and inside the one file in the package with `android.*` imports, so it is reachable from
no test at all. Compare F3's `LOCKS_FILE`, which is `internal` and lives in the pure `LockList.kt`
precisely so `LockListTest` can assert it. **This is the asymmetry T004/T005 correct.** ❌

**Key shapes** — covered. `DelayConfigTest.store keys are frozen` asserts both against raw
literals rather than rebuilding them from the same suffix constants, which is what makes the
assertion independent of the implementation:

```kotlin
assertEquals("com.example.app.delaySeconds", delayKey("com.example.app"))
assertEquals("com.example.app.treatment", treatmentKey("com.example.app"))
```

**Treatment tokens** — covered, in `DelayConfigTest.treatment tokens are frozen`. See **G3** for
where T006 lands relative to it.

**Sanitising behaviour** — covered by five further tests in `DelayConfigTest` (absent,
non-positive, out-of-range unclamped, unknown token, known token round trip). Not a value, but it
is the "reads as the default, nothing throws" obligation F2 also freezes.

---

## F3 — The locks order store

All three values covered by literal assertions in `LockListTest.store file, key and separator are
frozen`:

```kotlin
assertEquals("slowlock.locks", LOCKS_FILE)
assertEquals("packages", LOCKS_KEY)
assertEquals("\n", LOCKS_SEPARATOR)
```

The encode/decode round trip is separately covered (`encode joins with the frozen separator`,
`encode and decode round trip preserving order`).

> **T007 is already satisfied by the existing suite** and needs no new assertion. T007's own
> wording — "if not already covered" — anticipates exactly this.

---

## F4 — The shortcut identity and payload

All three covered by literal assertions in `ShortcutContractTest`. Confirmed reaching the real pin
path: `ShortcutPinner.request()` builds the persisted intent from `ShortcutContract.ACTION` and
`ShortcutContract.EXTRA_TARGET_PACKAGE`, and `ShortcutInfo.Builder(context, spec.id)` takes the ID
from `shortcutSpec()`, which is itself asserted end to end by `the spec carries the package name as
both id and payload`.

`PinnedShortcuts.pinnedShortcutIds()` reads the same IDs back as package names, which is what makes
`deriveLocks` a set operation — the ID scheme's freeze is load-bearing on the Locks screen as well
as on the home screen.

---

## Findings

### G1 — F2's preferences file name carries no assertion ❌

**Status**: known at planning time; **T004 and T005 close it.** Recorded here for completeness, not
as new information.

**What breaks without it**: renaming the file compiles clean, reads as absent, and reverts every
configured app to a 10-second default with the original treatment — silently, on a real device,
after an update the user did not ask for. Nothing in the build catches it today.

### G2 — F1's fully-qualified name is asserted relatively, not against a literal ⚠️ **NEW**

**Not covered by any task in `tasks.md`. Raised for the maintainer's ruling.**

`ShortcutContractTest` asserts `ShortcutContract.LAUNCH_ACTIVITY == ShortcutLaunchActivity::class.java.name`.
Both sides move together. Concretely:

| Change | Caught today? |
|---|---|
| Class moved, constant left alone | ✅ Yes — the assertion fails |
| Constant changed, class left alone | ✅ Yes |
| Class moved **and** constant updated to match | ❌ **No — the suite stays green** |

The third row is the one this refactor makes likely. Phase 5 moves nearly every file in the tree,
an IDE "Move class" refactor updates every reference including the constant, and the result is a
green build that orphans every shortcut already on every home screen. F1's own contract calls this
"the most dangerous value in this codebase"; it is the only frozen value whose guard does not have
the property the rule was written for. Contrast F4's extra key one file away, which *is* asserted
against a raw literal.

**Why the contract reads as it does**: `contracts/frozen-values.md` describes this guard as
"asserts the runtime FQN against the constant", so the current form is what was specified rather
than an oversight. This finding says the specified form is weaker than the constitution's rule
requires, not that the code diverged from the plan.

**Proposed remedy** (one line, no source change, no behaviour change):

```kotlin
assertEquals("com.slowlock.shortcut.ShortcutLaunchActivity", ShortcutContract.LAUNCH_ACTIVITY)
```

alongside the existing assertion, which stays — the two catch different failures and both are
wanted.

**Standing**: FR-013a already sanctions this class of change ("adding an assertion over a frozen
value … is NOT a structural change"), and it needs no visibility change because the constant is
already `public`. It is nonetheless **not in `tasks.md`**, so it is left unmade pending the
maintainer's confirmation (FR-009, FR-011). T005's mitigation is currently doing more work than
T003's most dangerous value's.

### G3 — T006 overlaps an existing assertion ℹ️

`DelayConfigTest.treatment tokens are frozen` already asserts the constant names against the
literals `"Original"`, `"Invert"`, `"Gray"`. T006 asks for the same assertion in
`IconTreatmentTest`, where the enum is declared — which is where someone about to rename a constant
is actually looking, and where the assertion survives if the store is ever re-shaped.

`IconTreatmentTest` today asserts the *entries* (identity and order), not their *names*, so T006 is
not literally redundant in that file.

**Handling**: T006 is executed as written. After Phase 5 both test files land in the same package
(`core/domain`), at which point the two assertions are genuinely adjacent duplicates.
**Flagged for T074** — Phase 7's test audit — to decide which one keeps its place. It is not
resolved now, because pruning tests in Phase 2 would remove a guard while the code under it is
still moving.

### G4 — `DEFAULT_SECONDS` is listed as frozen but documented as not ℹ️

`contracts/frozen-values.md` F2 lists "Default when absent | `DelayConfig.DEFAULT`,
`DEFAULT_SECONDS = 10`" in its frozen-form table. `DelayConfig.kt`'s own KDoc says the opposite:
"**Not frozen** — a changed default only affects apps that were never configured."

The source is right and the contract table is loose. A changed default cannot corrupt or orphan
anything already on disk; it only changes what an unconfigured app opens at. Its real obligation —
that it stay a reachable slider stop — is asserted by `DelayRangeTest.the default delay is a
reachable slider stop`, and `DelayConfigTest` asserts the sanitising reads relative to it.

**No literal assertion is needed and none is proposed.** Recorded so a later reader comparing the
audit against the contract table does not read the absence as a gap. A one-word correction to the
contract table is available if the maintainer wants it.

### G5 — The wait's instance-state keys are not frozen ℹ️

`ShortcutLaunchActivity` holds two bundle keys:

```kotlin
const val KEY_ANCHOR = "com.slowlock.wait.ANCHOR_ELAPSED_MILLIS"
const val KEY_DEADLINE = "com.slowlock.wait.DEADLINE_ELAPSED_MILLIS"
```

They look like the frozen values — a package-shaped string, a `const val`, an obligation to keep a
wait alive across a rotation — and they are **not** frozen. Instance state lives for one activity
instance and never reaches disk or a launcher, so **T047 may rename them freely** when it moves the
anchor and deadline into a `SavedStateHandle`.

Recorded because the resemblance is close enough to cost someone an hour, and because T047 is the
feature's highest-risk task and should not spend any of its risk budget here.

---

## What this audit did not change

No source file and no test file was modified by T003. The gaps above are recorded, not closed:
G1 is T004/T005's work, G2 awaits a maintainer ruling, G3 is deferred to T074, and G4 and G5 are
notes.
