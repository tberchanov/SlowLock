package com.slowlock.core.domain

/**
 * The only route to SlowLock's persisted per-app configuration (F2). No other type opens
 * `slowlock.delay-config`, so there is one copy of the delay and the treatment on disk and the two
 * screens cannot disagree (Constitution V).
 *
 * Obligations:
 *
 * - Main-safe (O2): both functions move themselves to the injected dispatcher, so a caller never
 *   wraps a call in `withContext`.
 * - Nothing throws on the launch path (O3): [load] runs on a cold-started wait, where an exception
 *   costs the user the app they were trying to open.
 * - [load] sanitises rather than validates — every unreadable value reads as that field's default.
 * - [save] replaces the whole record through one editor, so it is never half-written.
 */
interface DelayConfigRepository {

    /**
     * The configuration for [packageName] — never null, [DelayConfig.DEFAULT] when nothing is
     * stored, which makes "a shortcut with no configuration waits the default" structural.
     */
    suspend fun load(packageName: String): DelayConfig

    /** Replaces [packageName]'s whole record. */
    suspend fun save(packageName: String, config: DelayConfig)
}
