package com.slowlock.shortcut

/**
 * The named visual transformations offered for a shortcut icon, and the single source of truth
 * for what each one does (FR-005).
 *
 * One matrix feeds both the live preview (a Compose `ColorFilter`) and the bitmap baked into the
 * pinned shortcut (an `android.graphics.ColorMatrixColorFilter`). That shared origin is what
 * makes SC-003 — "the icon that lands matches the preview" — structural rather than something to
 * verify by eye: there is only one definition for the two to disagree with.
 *
 * Declaration order is display order in the treatment row, and [Original] is the initial
 * selection (FR-006) as `entries.first()`, so the ordering rule and the default rule cannot
 * drift apart.
 *
 * **The matrices are literal constants, not framework calls.** Deriving [Gray] from
 * `android.graphics.ColorMatrix().setSaturation(0f)` would look equivalent and be untestable:
 * the JVM suite runs with `isReturnDefaultValues = true`, under which that call returns an empty
 * matrix, so `IconTreatmentTest` would assert nothing while appearing to pass — and the error
 * would only surface as a wrong icon that is already permanent on someone's home screen.
 */
enum class IconTreatment(
    /**
     * A 4×5 row-major colour matrix, or `null` for [Original].
     *
     * `null` means *no filter is applied at all*, rather than an identity one — a filter that
     * does nothing still costs a pass over every pixel.
     *
     * Treat the array as read-only. It is a shared compile-time constant, not a defensive copy;
     * both consumers (`ColorMatrix`, `ColorMatrixColorFilter`) copy the values in rather than
     * retaining or mutating this instance.
     */
    val matrix: FloatArray?,
) {
    /** The target app's icon, untouched. */
    Original(null),

    /**
     * Photographic negative: `R' = 255 - R` on each colour channel.
     *
     * **The alpha row stays identity.** Inverting it too turns an icon's transparent corners
     * opaque black, which makes every inverted icon a solid square — the single most likely way
     * to get this wrong (research.md R7, manual case M1.5).
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
