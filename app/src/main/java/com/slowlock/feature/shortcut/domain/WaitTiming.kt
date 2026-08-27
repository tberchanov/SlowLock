package com.slowlock.feature.shortcut.domain

/**
 * The arithmetic of one wait.
 *
 * "Now" is a parameter, not a `SystemClock` call inside these functions. That seam is what makes
 * the restore path — a deadline anchored before a rotation, evaluated after it — checkable on the
 * JVM, and with no instrumented suite in this project it is the only automatic check there is.
 */
private const val MILLIS_PER_SECOND = 1_000L

/**
 * Reads the elapsed-realtime clock — the seam the constitution requires over it. Without it, a
 * `SystemClock.elapsedRealtime()` inside the state holder would put the wait's own timing back out
 * of reach of the JVM suite.
 *
 * Not a `DispatcherProvider`-shaped mistake (D4): a dispatcher already has a qualifier supplying
 * its seam, the clock has none, so this is the first abstraction over it rather than a second.
 *
 * The reading counts through deep sleep and is immune to the wall clock changing (research R4).
 */
fun interface ElapsedClock {

    /** The current elapsed-realtime reading, in milliseconds. */
    fun nowMillis(): Long
}

/**
 * The moment the wait ends, on the same elapsed-realtime clock as [nowElapsedMillis]. Anchored
 * *before* the configuration read, so a slow disk cannot extend the wait the user experiences.
 */
fun deadlineFrom(nowElapsedMillis: Long, delaySeconds: Int): Long =
    nowElapsedMillis + delaySeconds * MILLIS_PER_SECOND

/**
 * How much of the wait is left, never negative: a deadline already in the past yields `0`, so the
 * value can go straight to `delay()` rather than reaching it as a negative.
 */
fun remainingMillis(deadlineElapsedMillis: Long, nowElapsedMillis: Long): Long =
    (deadlineElapsedMillis - nowElapsedMillis).coerceAtLeast(0L)
