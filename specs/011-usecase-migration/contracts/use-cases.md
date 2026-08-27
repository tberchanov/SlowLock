# Contract: Use Cases

**Plan**: [../plan.md](../plan.md) · **Data model**: [../data-model.md](../data-model.md)

Obligations U1–U19. Each is a behaviour that must hold after the relocation and that a JVM test can
reach. An obligation marked **(parity)** describes behaviour that exists today and must be
indistinguishable afterwards; one marked **(new seam)** is a rule that exists today but is currently
unreachable without a framework object.

---

## `LoadInstalledAppsUseCase`

- **U1** *(parity)*: the returned list excludes the package given as `@OwnPackageName`, contains at
  most one entry per package, and is ordered by label under the locale `CurrentLocale.now()` answers
  — in that order, so exclusion cannot resurrect a deduped entry and sorting sees the final set.
- **U2** *(new seam)*: `CurrentLocale.now()` is called inside `invoke`, once per invocation. A
  locale captured at construction fails this; so does one captured across two invocations.
- **U3** *(parity)*: the repository is called exactly once per invocation, and its result is not
  cached between them.

## `FilterAppsUseCase`

- **U4** *(parity)*: a blank or whitespace-only query returns the input list, same instance order,
  nothing dropped.
- **U5** *(parity)*: a non-blank query keeps entries whose label contains it, ignoring case, as a
  substring rather than a prefix — "tagram" matches "Instagram".
- **U6** *(parity)*: relative order is the input's. The filter never re-sorts, so clearing a query
  restores the original collation without a second sort.

## `LoadLocksUseCase`

- **U7** *(parity)*: when `pinnedIds()` answers `null`, the stored order is returned unchanged and
  `saveOrder` is **not** called. `null` is "could not ask"; acting on it would empty the screen.
- **U8** *(parity)*: when `pinnedIds()` answers a set, the result is `deriveLocks(stored, pinned)` —
  known packages keep position, new ones append sorted.
- **U9** *(new seam)*: `saveOrder` is called only when the derived order differs from what
  `loadOrder` returned. An unchanged ordinary visit costs a read and no write.
- **U10** *(parity)*: every package in the resulting order becomes a row, including one that no
  longer resolves — a null label, its saved delay and treatment, in its original position. Nothing
  is dropped, sorted or filtered at this step.
- **U11** *(parity)*: an empty set from the launcher is a real answer and does empty the list. Only
  `null` is not.

## `WaitDecisionUseCase`

- **U12** *(parity)*: an unresolvable target returns `Unavailable` **before** any configuration read
  or any wait is computed.
- **U13** *(new seam)*: a non-null `storedDeadlineMillis` is returned as the deadline unchanged, and
  the configuration is not consulted for it. This is the rule a restored process depends on; a
  recomputed `now + delay` here is the defect FR-027 was written against.
- **U14** *(parity)*: a null `storedDeadlineMillis` yields `deadlineFrom(anchorMillis, delaySeconds)`
  where `delaySeconds` comes from the configuration read — never a value anchored later than the
  caller's `anchorMillis`.
- **U15** *(parity)*: `remainingMillis` is never negative; a deadline already past yields `0`.

## `CreateLockUseCase`

- **U16** *(parity)*: an unresolvable package returns `TargetMissing` and **nothing is written and
  nothing is pinned**. The resolution at invocation time is the one that counts, not the one the
  screen opened with.
- **U17** *(parity)*: on a resolved package the configuration is written **before** `requestPin` is
  called. A launcher that pins asynchronously must never fire a shortcut before its delay exists on
  disk. Order, not merely both happening.
- **U18** *(new seam)*: `requestPin` is called only when support is `Supported` at the moment of the
  attempt — not `Unknown`, not a value read earlier — and only when an icon was produced. Otherwise
  the result carries `Unsupported` or `IconUnavailable` and the launcher is never asked.

## `LoadDelayConfigUseCase`

- **U19** *(new seam)*: a non-null `editedSeconds` wins over the stored delay, while the treatment
  always comes from the read. A null `editedSeconds` yields the stored delay. The read happens on
  both paths, because the treatment is needed either way.

---

## What no test may assert (FR-019)

- That a use case called the repository it was constructed with. A test whose only assertion is a
  recorded call asserts the wiring, which the compiler already checks.
- That `FilterAppsUseCase` returns a list. That `LoadLocksUseCase` returns as many rows as packages
  it was given, without checking what is in them.
- Any obligation above by restating the implementation line by line rather than driving an input to
  an output.

Each obligation must fail against a plausible wrong implementation. Gate 4 in
[../quickstart.md](../quickstart.md) verifies that by mutation rather than by claim.
