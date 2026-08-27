package com.slowlock.core.domain

/**
 * The named visual transformations offered for a shortcut icon, and the single source of truth for
 * what each one does (FR-005).
 *
 * One matrix feeds both the live preview and the bitmap baked into the pinned shortcut, which is
 * what makes SC-003 — "the icon that lands matches the preview" — structural: there is only one
 * definition for the two to disagree with.
 *
 * Declaration order is display order, and [Original] is the initial selection as `entries.first()`
 * (FR-006), so the ordering rule and the default rule cannot drift apart.
 *
 * **The constant names below are a frozen persisted value** (F2). `DelayConfigStore` writes
 * `Enum.name` and `treatmentFrom` reads it back by matching on it, so a rename is not a rename — it
 * is an unrecognised token, which reads as [Original] and reverts every icon the user configured.
 * `DelayConfigTest` asserts the three strings so that lands in `./gradlew test`. Adding a fourth
 * treatment is allowed; renaming one is not.
 *
 * The matrices are literal constants, not framework calls: deriving [Gray] from
 * `ColorMatrix().setSaturation(0f)` would look equivalent and be untestable on the JVM, and the
 * error would surface only as a wrong icon already permanent on someone's home screen.
 */
enum class IconTreatment(
    /**
     * A 4×5 row-major colour matrix, or `null` for [Original] — meaning *no filter at all*, since
     * an identity filter still costs a pass over every pixel.
     *
     * Treat the array as read-only: it is a shared compile-time constant, not a defensive copy.
     */
    val matrix: FloatArray?,
) {
    /** The target app's icon, untouched. */
    Original(null),

    /**
     * Photographic negative: `R' = 255 - R` on each colour channel. The alpha row stays identity —
     * inverting it too turns transparent corners opaque black, making every icon a solid square.
     */
    Invert(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),

    /**
     * Fully desaturated, using the luminance coefficients `ColorMatrix.setSaturation(0f)`
     * produces: each colour channel becomes the same weighted sum of the three inputs.
     */
    Gray(
        floatArrayOf(
            0.213f, 0.715f, 0.072f, 0f, 0f,
            0.213f, 0.715f, 0.072f, 0f, 0f,
            0.213f, 0.715f, 0.072f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
}
