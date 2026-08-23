package com.slowlock.delay

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * **Frozen** (`contracts/delay-config-store.md`). A renamed file is an empty file: every
 * configured app reverts to the default delay at once, silently, on someone's device, after an
 * update they did not ask for.
 */
private const val FILE = "slowlock.delay-config"

/**
 * The only route to SlowLock's persisted per-app configuration
 * (`contracts/delay-config-store.md`, obligation S1 — no other class opens these preferences).
 *
 * **Every function suspends and does its work on [Dispatchers.IO]** (Constitution IV, FR-036).
 * One of the two callers is [com.slowlock.shortcut.ShortcutLaunchActivity], on the path a user is
 * already waiting on, so neither a read nor a write may touch the main thread.
 *
 * This is the one file in the `delay` package with `android.*` imports. Everything the store
 * decides — the key shapes, what a missing or nonsensical value reads as — lives in
 * [DelayConfig.kt][delayFrom] as pure functions instead, because the JVM suite runs with
 * `isReturnDefaultValues = true`: a test written against `SharedPreferences` here would assert
 * nothing while appearing to pass. This class is deliberately left with only the wiring.
 */
class DelayConfigStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /**
     * The configuration for [packageName] — **never null**. [DelayConfig.DEFAULT] when nothing is
     * stored, which is what makes FR-032 structural rather than a branch a caller could forget.
     *
     * Reads sanitise rather than validate: absent, non-positive, wrongly typed, or an unrecognised
     * treatment token each yield the default for that field. Nothing here throws, because this
     * runs on a cold-started wait and a crash costs the user the app they were trying to open.
     */
    suspend fun load(packageName: String): DelayConfig = withContext(Dispatchers.IO) {
        DelayConfig(
            delaySeconds = delayFrom(prefs.intOrNull(delayKey(packageName))),
            treatment = treatmentFrom(prefs.stringOrNull(treatmentKey(packageName))),
        )
    }

    /**
     * Replaces [packageName]'s whole record. There is no partial update (FR-015, obligation S5).
     *
     * Both keys go through one [SharedPreferences.Editor] so a record is never half-written, and
     * `apply()` rather than `commit()` because no caller may block on the disk (obligation S4).
     */
    suspend fun save(packageName: String, config: DelayConfig) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putInt(delayKey(packageName), config.delaySeconds)
            .putString(treatmentKey(packageName), config.treatment.name)
            .apply()
    }
}

/**
 * `null` when the key is absent **or holds the wrong type**.
 *
 * The `runCatching` is not defensive habit: a key written as a `String` by some earlier or later
 * build throws `ClassCastException` from [SharedPreferences.getInt], and the honest answer to
 * "what delay did the user choose?" in that case is the default, not a crash on the launch path.
 */
private fun SharedPreferences.intOrNull(key: String): Int? =
    runCatching { if (contains(key)) getInt(key, 0) else null }.getOrNull()

/** As [intOrNull], for the treatment token. */
private fun SharedPreferences.stringOrNull(key: String): String? =
    runCatching { getString(key, null) }.getOrNull()
