package com.slowlock.feature.delay.domain

/**
 * The bounds of the delay slider, and the pure mapping between a position on it and a whole number
 * of seconds.
 *
 * Pure Kotlin with no Compose import, which lets `DelayRangeTest` assert the off-by-one in
 * [SLIDER_STEPS] without a device — the kind of error that yields a slider looking entirely correct
 * while landing on the wrong values.
 *
 * These numbers are provisional and not frozen: nothing outside this object depends on them, and
 * the store deliberately does not clamp to them.
 */
object DelayRange {

    /** The shortest wait the screen can produce. */
    const val MIN_SECONDS = 1

    /** The longest wait the screen can produce. */
    const val MAX_SECONDS = 30

    /**
     * Every reachable value is a multiple of this (FR-005). At `1`, [snap] is the identity inside
     * the range and the stops exist only to keep the slider's `Float` off the state; the constant
     * stays so a coarser step is a one-line change here and nowhere else.
     */
    const val STEP_SECONDS = 1

    /** How many values the slider can land on, endpoints included. Derived, never written down. */
    val STOPS: Int = (MAX_SECONDS - MIN_SECONDS) / STEP_SECONDS + 1

    /**
     * What Material's `Slider` wants for `steps`: the stops *between* the endpoints, two fewer than
     * [STOPS]. Derived rather than written, because `28` typed by hand next to a range of 30 stops
     * is the exact off-by-one that survives review. Asserted in `DelayRangeTest`.
     */
    val SLIDER_STEPS: Int = STOPS - 2

    /**
     * Clamps [seconds] into `[MIN_SECONDS, MAX_SECONDS]`, then rounds to the nearest
     * [STEP_SECONDS]. The screen applies this to the slider's `Float` before it becomes state, so
     * the displayed number, the stored number and the handle's position are always the same value
     * (FR-007).
     */
    fun snap(seconds: Int): Int {
        val offset = seconds.coerceIn(MIN_SECONDS, MAX_SECONDS) - MIN_SECONDS
        val rounded = (offset + STEP_SECONDS / 2) / STEP_SECONDS * STEP_SECONDS
        return MIN_SECONDS + rounded
    }

    /**
     * One-tap shortcuts to the delays people actually pick (FR-017).
     *
     * Additive, deliberately: every value here is inside `MIN_SECONDS..MAX_SECONDS` and is a stop
     * [snap] leaves alone, both asserted in `DelayRangeTest`. A preset the slider could not also
     * reach would be a second, competing answer to "what is a legal delay".
     */
    val PRESETS: List<Int> = listOf(5, 10, 30)

    /**
     * The preset matching [seconds], or null when none does — the whole of the preset row's
     * selection logic. Nothing stores which preset is selected: the screen asks at composition
     * time, which makes "dragged to 17 seconds, so nothing is highlighted" correct by construction
     * (FR-018).
     */
    fun presetFor(seconds: Int): Int? = PRESETS.firstOrNull { it == seconds }
}
