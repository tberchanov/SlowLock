package com.slowlock.core.domain

import java.util.Locale

/**
 * Reads the locale in force *now*.
 *
 * [ElapsedClock]'s shape and rationale, for a different reading: a locale captured at construction
 * would collate the app list under whatever language was set when the object was built, so a
 * language change would not re-collate until the process restarted. Taking it at call time is the
 * behaviour; the interface is what puts that behaviour in reach of the JVM suite.
 *
 * [Locale] is `java.*`, so nothing platform-specific crosses this boundary.
 */
fun interface CurrentLocale {

    /** The first locale of the current configuration, falling back to the JVM default. */
    fun now(): Locale
}
