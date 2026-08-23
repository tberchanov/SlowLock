package com.slowlock.delay

/**
 * The arithmetic of one wait (data-model.md §`WaitDeadline`).
 *
 * **"Now" is a parameter, not a `SystemClock` call inside these functions.** That single seam is
 * what makes the restore path — a deadline anchored before a rotation, evaluated after it —
 * checkable on the JVM. With no instrumented suite in this project (Constitution: "No automated
 * test may drive a device"), it is the only way this arithmetic is verified automatically at all.
 *
 * The caller's clock is `SystemClock.elapsedRealtime()`: it counts through deep sleep and is
 * immune to the wall clock changing under the wait (research.md R4).
 */
private const val MILLIS_PER_SECOND = 1_000L

/**
 * The moment the wait ends, on the same elapsed-realtime clock as [nowElapsedMillis].
 *
 * Anchored in `onCreate` **before** the configuration read, so a slow disk cannot extend the wait
 * the user experiences.
 */
fun deadlineFrom(nowElapsedMillis: Long, delaySeconds: Int): Long =
    nowElapsedMillis + delaySeconds * MILLIS_PER_SECOND

/**
 * How much of the wait is left. **Never negative.**
 *
 * A deadline already in the past — the restored-deadline case, where the activity comes back after
 * the wait would have ended — yields `0`, so the value can be handed straight to `delay()`. A
 * negative here would reach `delay(-4)`, which returns immediately in a way that only looks
 * correct until it does not.
 */
fun remainingMillis(deadlineElapsedMillis: Long, nowElapsedMillis: Long): Long =
    (deadlineElapsedMillis - nowElapsedMillis).coerceAtLeast(0L)
