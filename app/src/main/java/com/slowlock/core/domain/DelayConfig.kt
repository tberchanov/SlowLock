package com.slowlock.core.domain

/**
 * What SlowLock remembers about one target app: how long its icon makes the user wait, and how that
 * icon was treated.
 *
 * Identity is the target's package name and is deliberately not a field: the record is stored
 * *under* that name, and a copy of it here would be a second place for the identity to live and a
 * way for the two to disagree (Constitution V, FR-016).
 *
 * There is no "unconfigured" value — [DelayConfigStore.load] always answers, possibly [DEFAULT] —
 * which stops FR-032 from being a branch every caller has to remember to write.
 */
data class DelayConfig(
    /** Whole seconds. Positive, but *not* constrained to [DelayRange] — see [delayFrom]. */
    val delaySeconds: Int,
    /** Persisted by [Enum.name], never by ordinal. */
    val treatment: IconTreatment,
) {
    companion object {
        /**
         * One constant with two readers: the delay screen's opening value for an app never
         * configured (FR-006) and the fallback for a shortcut with no configuration (FR-032). The
         * spec requires them to be the same number.
         *
         * Not frozen, but it MUST stay a reachable slider stop, which `DelayRangeTest` asserts.
         */
        const val DEFAULT_SECONDS = 10

        /** `entries.first()` rather than `Original` by name, so declaration order cannot drift. */
        val DEFAULT = DelayConfig(DEFAULT_SECONDS, IconTreatment.entries.first())
    }
}

/**
 * **FROZEN** (`contracts/delay-config-store.md`, F2). The `SharedPreferences` file every per-app
 * delay and treatment is stored in. A renamed file is an empty file: every configured app reverts
 * to [DelayConfig.DEFAULT] at once, silently, after an update the user did not ask for.
 *
 * It lives here rather than in [DelayConfigStore] because a value the JVM suite cannot reach is a
 * value nothing guards; `DelayConfigTest` asserts it against a literal.
 */
internal const val DELAY_CONFIG_FILE = "slowlock.delay-config"

/**
 * **Frozen.** Appended to the package name to form the key the delay is stored under. A rename is
 * not a migration: every configured app silently reverts to the default on the next read.
 */
private const val DELAY_KEY_SUFFIX = ".delaySeconds"

/** **Frozen.** As [DELAY_KEY_SUFFIX], for the icon treatment. */
private const val TREATMENT_KEY_SUFFIX = ".treatment"

/**
 * The preference key holding [packageName]'s delay. Frozen shape: `"com.example.app.delaySeconds"`.
 * Framework-free so `DelayConfigTest` can assert it against a literal on the JVM.
 */
internal fun delayKey(packageName: String) = packageName + DELAY_KEY_SUFFIX

/** The key holding [packageName]'s icon treatment. Frozen: `"com.example.app.treatment"`. */
internal fun treatmentKey(packageName: String) = packageName + TREATMENT_KEY_SUFFIX

/**
 * Reads a stored delay. Sanitises, never validates: `null` and any non-positive value yield
 * [DelayConfig.DEFAULT_SECONDS], and everything else comes back unclamped. [DelayRange] constrains
 * what the *screen* can produce, not what the store may hold, so a later range change cannot
 * silently rewrite a value the user chose.
 */
internal fun delayFrom(stored: Int?): Int =
    if (stored == null || stored <= 0) DelayConfig.DEFAULT_SECONDS else stored

/**
 * Reads a stored treatment token. An unrecognised or absent token yields [IconTreatment.Original]
 * rather than throwing — the file may have been written by an older or newer build.
 *
 * The tokens are the constants' [Enum.name] values and are **frozen**: a rename compiles clean,
 * reads as unrecognised, and reverts every configured icon. `DelayConfigTest` guards it.
 */
internal fun treatmentFrom(token: String?): IconTreatment =
    IconTreatment.entries.firstOrNull { it.name == token } ?: IconTreatment.Original
