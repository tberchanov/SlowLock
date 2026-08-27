# Contract: Frozen Values

**Date**: 2026-08-26 | **Spec**: FR-003, FR-004, FR-007, FR-029b | **Plan**: [../plan.md](../plan.md)

Every value here was written into a launcher's persisted intent or onto a user's disk by a build
already in the field. Changing one does not migrate anything — it orphans or resets what the user
already has, and the failure surfaces when they tap an icon. Each has a unit test asserting it
against a literal, which is the mechanism that turns a rename into a red build instead of a support
report.

Obligation on this feature: **every value below is byte-identical before and after.** The class or
file carrying it may be renamed or moved only where the value itself provably travels unchanged.

## F1 — The pinned entry point's fully-qualified name

```
com.slowlock.shortcut.ShortcutLaunchActivity
```

**Where it lives**: `ShortcutContract.LAUNCH_ACTIVITY`, the manifest's `android:name`, and
`app/src/main/keepRules/rules.keep` as `-keepnames`.

**Why frozen**: `ComponentName(context, X::class.java)` resolves at build time. Renaming or moving
the class compiles clean, pins *new* shortcuts at the new name, and leaves every shortcut already
on a home screen pointing at a name that no longer exists.

**Obligation on the rearrangement**: `ShortcutLaunchActivity.kt` stays directly in
`com.slowlock.shortcut` — **not** in `feature/shortcut/`, and not in a layer subpackage under it.
This is not a deviation: constitution Principle III states that a frozen fully-qualified name
outranks the package shape, and names this class as the case in point.

**Guard**: `ShortcutContractTest` asserts the runtime FQN against the constant. The R8 keep rule is
re-verified after the move by inspecting a release mapping, because a wrong keep rule fails only in
a shrunk build.

## F2 — The delay configuration store

| Value | Frozen form |
|---|---|
| Preferences file | `slowlock.delay-config` |
| Delay key | `<packageName>` + `.delaySeconds` |
| Treatment key | `<packageName>` + `.treatment` |
| Treatment token | the `IconTreatment` enum constant **name**, verbatim |
| Default when absent | `DelayConfig.DEFAULT`, `DEFAULT_SECONDS = 10` |

**Obligation**: the enum constant names `Original`, `Invert`, `Gray` are the persisted tokens.
Moving `IconTreatment` to `core/domain` is safe; renaming a constant is not.

**Also frozen in behaviour**: a missing, non-positive, wrongly-typed or unrecognised value reads as
the default for that field. Nothing throws — this runs on a cold-started wait, and a crash costs
the user the app they were opening.

## F3 — The locks order store

| Value | Frozen form |
|---|---|
| Preferences file | `slowlock.locks` |
| Key | `packages` |
| Separator | `\n` |

**Obligation**: `LockStore` may be renamed to `LockOrderStore`, and `LockList.kt` may move to
`feature/locks/domain/`, because neither name is persisted. The three constants above and the encode/decode
round trip are.

## F4 — The shortcut identity and payload

| Value | Frozen form |
|---|---|
| Shortcut ID | the target's package name, verbatim |
| Intent extra | `com.slowlock.shortcut.extra.TARGET_PACKAGE` |
| Intent action | `android.intent.action.VIEW` |

**Why the ID scheme matters**: identity is derived from the target rather than generated and
stored, which is what makes re-pinning idempotent with no bookkeeping. Any other scheme orphans
every existing shortcut and adds a second icon per app.

**Note**: the extra's name contains `com.slowlock.shortcut`, which reads like a package path but is
an opaque string. It does not track where any class lives and must not be "corrected" to follow the
rearrangement.

## Verification

1. `./gradlew test` — `ShortcutContractTest`, the delay-config tests and the lock-list tests each
   assert their values against literals. A rename fails the build.
2. After Stage 3, diff the frozen constants against this document by hand. The compiler cannot
   catch a value that was changed consistently everywhere.
3. At the final gate, an in-place update over the baseline install (R14) must show every lock with
   its delay and treatment intact and every pinned icon still launching — SC-002.
