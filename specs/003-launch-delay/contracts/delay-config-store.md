# Contract: The Delay Configuration Store

**Feature**: `003-launch-delay`

**Status**: **FROZEN.** This is the project's second body of permanent state, after
`002-shortcut-pinning/contracts/pinned-shortcut.md`, and it fails the same way: silently, on
someone's device, after an update they did not ask for.

---

## Why this one is frozen

The values below are written to disk on a user's phone and read back by a later build. Nothing
migrates them, nothing validates them at build time, and nothing surfaces a mismatch: a renamed
file, key, or token reads as *absent*, and absent means "use the default". The user's carefully
chosen 25-second pause becomes 10 seconds, their inverted icon reverts to the original, and the
app looks like it is working.

What is **not** frozen: the default value, the slider's range and step, the storage mechanism
underneath, and everything about the two screens. Those can change freely — a changed default
only affects apps that were never configured.

---

## The frozen values

```kotlin
// DelayConfigStore.kt — every constant here is permanent
private const val FILE = "slowlock.delay-config"
private const val KEY_DELAY = ".delaySeconds"      // suffix, prefixed with the package name
private const val KEY_TREATMENT = ".treatment"     // suffix, prefixed with the package name
```

| Element | Value | Frozen because |
|---|---|---|
| **File name** | `slowlock.delay-config` | A renamed file is an empty file. Every configured app reverts to the default delay at once |
| **Delay key** | `"<packageName>.delaySeconds"` | Same, per app |
| **Treatment key** | `"<packageName>.treatment"` | Same, per app |
| **Treatment tokens** | `Original`, `Invert`, `Gray` — the `IconTreatment` constants' `name` values | A renamed enum constant compiles clean, reads as unrecognised, and reverts every configured icon to `Original` |
| **Key prefix** | the target's package name, verbatim | Constitution V: the only identifier the platform guarantees stable |

Assembled:

```kotlin
prefs.edit()
    .putInt(packageName + KEY_DELAY, config.delaySeconds)
    .putString(packageName + KEY_TREATMENT, config.treatment.name)
    .apply()
```

Both keys in one `Editor`, so a record is never half-written.

---

## The rename hazard, and its guard

`IconTreatment` is an ordinary enum in feature 002's package, with nothing about it that says
"these names are on disk". A rename or a reorder is exactly the kind of tidy-up that looks safe.

Guarded by a JVM unit test, so the failure lands in `./gradlew test` rather than on a user's phone:

```kotlin
@Test fun treatment_tokens_are_frozen() {
    assertEquals(
        listOf("Original", "Invert", "Gray"),
        IconTreatment.entries.map { it.name },
    )
}

@Test fun store_keys_are_frozen() {
    assertEquals("com.example.app.delaySeconds", delayKey("com.example.app"))
    assertEquals("com.example.app.treatment", treatmentKey("com.example.app"))
}
```

Note this freezes the **names**, not the order and not the matrices. Adding a fourth treatment is
allowed by this contract (the spec puts it out of scope for other reasons); renaming one is not.
If a rename ever becomes unavoidable, the remedy is a read-time alias from the old token to the
new constant — never a silent revert.

The ordinal is deliberately **not** persisted: reordering the enum, which feature 002 treats as a
presentation decision, would rewrite every saved icon.

---

## Reading: sanitise, never throw

| Stored state | Read as |
|---|---|
| Key absent | `DelayConfig.DEFAULT` for that field |
| Delay ≤ 0 | `DelayConfig.DEFAULT_SECONDS` |
| Delay outside the slider's range | **returned as stored** — the range constrains the screen, not the store |
| Treatment token unrecognised | `IconTreatment.Original` |
| Wrong type for a key (`ClassCastException`) | the default for that field; the read must not crash a cold-started wait |

`load` returns a non-null `DelayConfig`. There is no "unconfigured" value for a caller to handle,
which is how FR-032 stops being a branch anyone can forget: a shortcut with no configuration waits
the default because the store said so.

Not clamping to the slider's range is deliberate. It keeps a later range change from silently
rewriting values the user chose. The range belongs to the screen, not to the data.

---

## Obligations on every caller

| # | Obligation |
|---|---|
| S1 | Reach the file only through `DelayConfigStore`. No other class opens these preferences |
| S2 | Every read and write suspends and runs on `Dispatchers.IO`. Never touch the store during composition or on the main thread (Constitution IV, FR-036) |
| S3 | Key by package name alone. Never by label, `ComponentName`, or launcher activity (Constitution V) |
| S4 | Write both keys in one `Editor`, with `apply()`. Never `commit()` on a path a user is waiting on |
| S5 | Write the whole record. There is no partial update; applying replaces both fields (FR-015) |
| S6 | Never assume a record exists. `load` always answers, and the answer may be the default |
| S7 | Do not record which apps have shortcuts. Feature 002's FR-027 stands — identity is derived, never tracked |

---

## Verification

Automated (JVM, `app/src/test`):

```kotlin
@Test fun treatment_tokens_are_frozen()                  // the rename guard above
@Test fun store_keys_are_frozen()                        // file-level key shape
@Test fun absent_configuration_reads_as_default()        // FR-032
@Test fun non_positive_delay_reads_as_default()
@Test fun unknown_treatment_token_reads_as_original()
@Test fun default_seconds_is_a_reachable_slider_stop()   // DelayRange agreement
```

Manual — that a configuration survives a force-stop, a reboot, and an app update, and that the
delay a user set is the delay the icon imposes weeks later. See `manual-test-plan.md` M3 and M6.
