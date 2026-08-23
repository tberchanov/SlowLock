package com.slowlock.delay

import com.slowlock.shortcut.IconTreatment

/**
 * What SlowLock remembers about one target app: how long its icon makes the user wait, and how
 * that icon was treated (data-model.md §`DelayConfig`).
 *
 * **Identity is the target's package name, and it is deliberately not a field here.** The record
 * is stored *under* the package name; giving it a copy of that name would create a second place
 * for the identity to live, and a way for the two to disagree (Constitution V, FR-016).
 *
 * There is no "unconfigured" value. [DelayConfigStore.load] always answers, and the answer may be
 * [DEFAULT] — which is what stops FR-032 ("a shortcut with no configuration waits the default")
 * from being a branch every caller has to remember to write.
 */
data class DelayConfig(
    /**
     * Whole seconds. Positive, but **not** constrained to [DelayRange] — see [delayFrom].
     */
    val delaySeconds: Int,
    /** Feature 002's enum, reused unchanged. Persisted by [Enum.name], never by ordinal. */
    val treatment: IconTreatment,
) {
    companion object {
        /**
         * One constant with two readers: the delay screen's opening value for an app never
         * configured (FR-006), and the fallback for a shortcut with no configuration (FR-032).
         * The spec requires those to be the same number; sharing the constant is how that is
         * enforced rather than remembered.
         *
         * Not frozen — a changed default only affects apps that were never configured. But it
         * MUST stay a reachable slider stop, which `DelayRangeTest` asserts.
         */
        const val DEFAULT_SECONDS = 10

        /**
         * `entries.first()` rather than `Original` by name, reusing feature 002's rule that the
         * declaration order and the default selection cannot drift apart.
         */
        val DEFAULT = DelayConfig(DEFAULT_SECONDS, IconTreatment.entries.first())
    }
}

/**
 * **Frozen** (`contracts/delay-config-store.md`). Appended to the package name to form the key
 * the delay is stored under. A rename here is not a migration: every configured app silently
 * reverts to [DelayConfig.DEFAULT_SECONDS] on the next read.
 */
private const val DELAY_KEY_SUFFIX = ".delaySeconds"

/**
 * **Frozen** (`contracts/delay-config-store.md`). As [DELAY_KEY_SUFFIX], for the icon treatment.
 */
private const val TREATMENT_KEY_SUFFIX = ".treatment"

/**
 * The preference key holding [packageName]'s delay. Frozen shape: `"com.example.app.delaySeconds"`.
 *
 * Pure and framework-free on purpose — that is what lets `DelayConfigTest` assert the frozen shape
 * against a literal on the JVM, where the unit suite's `isReturnDefaultValues = true` would make a
 * `SharedPreferences`-shaped test assert nothing while appearing to pass.
 */
internal fun delayKey(packageName: String) = packageName + DELAY_KEY_SUFFIX

/** The preference key holding [packageName]'s icon treatment. Frozen: `"com.example.app.treatment"`. */
internal fun treatmentKey(packageName: String) = packageName + TREATMENT_KEY_SUFFIX

/**
 * Reads a stored delay. **Sanitises, never validates**: `null` (absent, or a wrongly-typed key the
 * store caught) and any non-positive value yield [DelayConfig.DEFAULT_SECONDS].
 *
 * Everything else is returned **unclamped**. A stored `1` or `600` comes back as it is: [DelayRange]
 * constrains what the *screen* can produce, not what the store may hold, which keeps a later range
 * change from silently rewriting a value the user chose.
 */
internal fun delayFrom(stored: Int?): Int =
    if (stored == null || stored <= 0) DelayConfig.DEFAULT_SECONDS else stored

/**
 * Reads a stored treatment token. An unrecognised or absent token yields
 * [IconTreatment.Original] rather than throwing — a configuration file is a thing an older or
 * newer build wrote, and refusing to read it helps nobody.
 *
 * The tokens are the constants' [Enum.name] values and are **frozen**: a rename compiles clean,
 * reads as unrecognised, and reverts every configured icon. `DelayConfigTest` guards it.
 */
internal fun treatmentFrom(token: String?): IconTreatment =
    IconTreatment.entries.firstOrNull { it.name == token } ?: IconTreatment.Original
