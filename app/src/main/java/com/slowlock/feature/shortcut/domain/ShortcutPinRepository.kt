package com.slowlock.feature.shortcut.domain

import androidx.compose.ui.graphics.ImageBitmap
import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.IconTreatment

/**
 * Asks the launcher to pin a shortcut. Obligations:
 *
 * - Icon baking runs off the main thread; the `ShortcutManager` calls stay on the caller's
 *   dispatcher, because `requestPinShortcut` puts a system dialog in front of the user.
 * - A declined system dialog is a normal outcome, not an error: it creates no lock and nothing is
 *   recorded, because identity is derived from the target and re-pinning is idempotent.
 *
 * The support gate is **not** here. `isRequestPinShortcutSupported()` must guard every attempt, but
 * whether to attempt is a decision, and it lives in [CreateLockUseCase] with the icon load it is
 * paired with.
 *
 * No `android.*` type crosses this boundary (O1): the icon arrives as Compose's [ImageBitmap], the
 * same type [com.slowlock.core.domain.AppIconRepository] already answers with, and the conversion
 * to `android.graphics.Bitmap` happens inside the implementation.
 */
interface ShortcutPinRepository {

    /**
     * Requests a pin for [target] with [treatment] baked into [icon].
     *
     * Returns nothing: the launcher owns whether an icon appears and never tells the app, and the
     * two refusals this used to report — no support, no icon — are decided before it is called.
     */
    suspend fun requestPin(target: AppTarget, treatment: IconTreatment, icon: ImageBitmap)
}

/**
 * What became of a pin request. Deliberately not a `Boolean`, which would fold two very different
 * refusals — "this launcher will not take pins" and "there was no icon to bake" — into one `false`.
 *
 * None of these is an error: a pin that was not requested leaves the device exactly as it was.
 */
sealed interface PinRequestResult {

    /**
     * The request reached the launcher. Not that an icon appeared: the user may still decline the
     * system dialog, and the app is never told either way.
     */
    data object Requested : PinRequestResult

    /** Support was not confirmed at the moment of the attempt. The gate held. */
    data object Unsupported : PinRequestResult

    /**
     * No icon could be produced, so there was nothing to bake. A pinned shortcut is effectively
     * permanent, so a placeholder on someone's home screen is worse than no shortcut at all.
     */
    data object IconUnavailable : PinRequestResult
}
