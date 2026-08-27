# Findings: Navigation Adoption

**Feature**: `010-navigation-adoption` | **Opened**: 2026-08-27 | **Task**: T050 (collects), FR-031 / FR-032 / FR-033 (govern)

Everything noticed during this feature that the maintainer has not ruled on. Nothing here has been
acted on beyond what the ruling column says.

**The rules this file exists to enforce** (spec, Defect handling):

- **FR-031** — a defect is recorded with what it is, what it breaks, and what fixing it would
  change for a user. It is **not fixed** before the maintainer confirms *that specific fix*.
- **FR-032** — an unconfirmed defect is preserved exactly as it is. Silent correction "while in
  the file" is prohibited.
- **FR-033** — a step containing a fix is never reported as behaviour-preserving.

A general go-ahead ("proceed", "continue", "looks good") is **not** a ruling on anything below.

## Status board

| # | Raised | Severity | Subject | Status |
|---|---|---|---|---|
| G-01 | T018 / T026 | Low | Both new holders read their route argument by the property's key name, not through `toRoute` | 🟡 Open — forced by JVM testability |
| G-02 | T037 | Info | `LocksViewModel.refresh()` lost its `Job` return type, which is more than the task's wording asked for | 🟡 Open — reversible |
| G-03 | T028 | Info | `DelayConfigScreen` still takes `packageName`, which its holder could read from its own route | 🟡 Open — no action proposed |

---

## G-01 — The route argument is read by key name, not through `toRoute`

**Raised**: T018 and T026 | **Severity**: Low | **A deliberate divergence, not a defect found**

`ShortcutConfigViewModel` reads its route's treatment as `savedState["treatment"]`, keyed by the
route property's own name, rather than as `savedState.toRoute<ShortcutConfig>().treatment` — which
is the documented way a holder reads a type-safe route, and what a reader will expect to find.

**Why.** `toRoute(SavedStateHandle)` decodes through `SavedStateHandleArgStore`, which wraps each
value into a `SavedState` before handing it to the argument's `NavType`. On the JVM that is
`android.os.Bundle`, so the call is unreachable from `./gradlew test`. Using it would have put the
R8 branch — the one piece of new logic in this feature a test can get wrong — beyond every gate
this project has, since the constitution forbids instrumented suites.

**What it costs.** A coupling to the library's argument-naming contract: the argument key *is* the
property name, and renaming `ShortcutConfig.treatment` would compile clean and read as absent, then
silently fall through to `IconTreatment.entries.first()`. That is the same shape as a frozen value
and is not commented as one.

**What fixing it would change for a user**: nothing today. It is a robustness question, not a
behaviour one.

**Alternatives**: adopt Robolectric (adds a test runtime the project does not have, and the
constitution's automated-coverage clause names JVM unit tests only); pass the argument in through
`start()` as `packageName` already is, and keep only the *selection* in the handle (loses the
handle-versus-argument branch this feature was told to cover, and moves the R8 rule to the screen).

**Ruling**: _pending_

---

## G-02 — `LocksViewModel.refresh()` lost its `Job` return type

**Raised**: T037 | **Severity**: Info | **More than the task's wording asked for**

T037 asks for the KDoc sentence about the returned `Job` to be corrected. The return type went with
it: `fun refresh(): Job = viewModelScope.launch { … }` is now `fun refresh() { viewModelScope.launch
{ … } }`.

**Why.** T010 removed `locksViewModel.refresh().join()`, which was the only caller that ever used
the value. A returned handle nothing awaits is the speculative generality Principle V names as a
defect, and it became one in this feature rather than being found in it — leaving it would have
meant writing a KDoc explaining why the return value exists and nobody uses it.

**What reversing it would cost**: one line, and the KDoc sentence comes back with it. No test reads
the value; no caller does.

**Ruling**: _pending — reversible either way; nothing depends on the answer_

---

## G-03 — `DelayConfigScreen` still takes `packageName`

**Raised**: T028 | **Severity**: Info | **The gate would not catch it, and it may be correct**

The screen's only use of `packageName` is `LaunchedEffect(packageName) { viewModel.start(packageName) }`.
The holder is scoped to the `DelayConfig` entry and could read the package name out of its own
handle, which would make the parameter unnecessary.

**Why it stayed.** It is the shape `ShortcutConfigScreen` already has, and changing one without the
other would leave two screens starting their holders two different ways. Reading it from the handle
also runs into G-01: it would be a second key-name read, in a second file.

**What fixing it would change for a user**: nothing.

**Ruling**: _pending — or rule that both screens should move together, which is a change of its own_
