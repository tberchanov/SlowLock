package com.slowlock.core.data

import android.content.Context
import android.content.SharedPreferences
import com.slowlock.core.domain.DELAY_CONFIG_FILE
import com.slowlock.core.domain.DelayConfig
import com.slowlock.core.domain.DelayConfigRepository
import com.slowlock.core.domain.IoDispatcher
import com.slowlock.core.domain.delayFrom
import com.slowlock.core.domain.delayKey
import com.slowlock.core.domain.treatmentFrom
import com.slowlock.core.domain.treatmentKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The only route to SlowLock's persisted per-app configuration — no other class opens these
 * preferences (obligation S1).
 *
 * Every function suspends onto the injected [IoDispatcher] (Constitution IV, FR-036, D2), so
 * callers stay main-safe without having to know it (O2). The dispatcher arrives through the
 * constructor, which lets a test drive both functions on a test dispatcher with no
 * `Dispatchers.setMain` (D3).
 *
 * Left with only the wiring: everything the store decides — the file name, the key shapes, what a
 * missing or nonsensical value reads as — lives in `DelayConfig.kt` as pure values the JVM suite
 * can reach without a framework.
 */
@Singleton
class DelayConfigStore @Inject constructor(
    @ApplicationContext context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : DelayConfigRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(DELAY_CONFIG_FILE, Context.MODE_PRIVATE)

    /**
     * The configuration for [packageName] — never null, [DelayConfig.DEFAULT] when nothing is
     * stored, which makes FR-032 structural rather than a branch a caller could forget.
     *
     * Reads sanitise rather than validate, and nothing throws: this runs on a cold-started wait,
     * and a crash costs the user the app they were trying to open.
     */
    override suspend fun load(packageName: String): DelayConfig = withContext(io) {
        DelayConfig(
            delaySeconds = delayFrom(prefs.intOrNull(delayKey(packageName))),
            treatment = treatmentFrom(prefs.stringOrNull(treatmentKey(packageName))),
        )
    }

    /**
     * Replaces [packageName]'s whole record; there is no partial update (FR-015, S5). Both keys go
     * through one [SharedPreferences.Editor] so a record is never half-written, and `apply()`
     * rather than `commit()` because no caller may block on the disk (S4).
     */
    override suspend fun save(packageName: String, config: DelayConfig) = withContext(io) {
        prefs.edit()
            .putInt(delayKey(packageName), config.delaySeconds)
            .putString(treatmentKey(packageName), config.treatment.name)
            .apply()
    }
}

/**
 * `null` when the key is absent or holds the wrong type. The `runCatching` is not defensive habit:
 * a key written as a `String` by some other build throws `ClassCastException` from
 * [SharedPreferences.getInt], and the honest answer then is the default, not a crash on launch.
 */
private fun SharedPreferences.intOrNull(key: String): Int? =
    runCatching { if (contains(key)) getInt(key, 0) else null }.getOrNull()

/** As [intOrNull], for the treatment token. */
private fun SharedPreferences.stringOrNull(key: String): String? =
    runCatching { getString(key, null) }.getOrNull()
