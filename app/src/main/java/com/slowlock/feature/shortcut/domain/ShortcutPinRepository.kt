package com.slowlock.feature.shortcut.domain

import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.IconTreatment

/**
 * Asks the launcher to pin a shortcut. Obligations:
 *
 * - `isRequestPinShortcutSupported()` gates every attempt, re-read at the moment of the pin,
 * because   the user can change launcher while the screen sits open.
 * - Icon baking runs off the main thread; the `ShortcutManager` calls stay on the caller's
 *   dispatcher, because `requestPinShortcut` puts a system dialog in front of the user.
 * - A declined system dialog is a normal outcome, not an error: it creates no lock and nothing is
 *   recorded, because identity is derived from the target and re-pinning is idempotent.
 *
 * No `android.*` type crosses this boundary (O1): the source icon is loaded by the implementation
 * rather than handed in as a `Bitmap`.
 */
interface ShortcutPinRepository {

    /**
     * Requests a pin for [target] with [treatment] baked into its icon.
     *
     * @return what happened to the *request*, never whether an icon appeared — the launcher owns
     *   that outcome and the app deliberately does not observe it.
     */
    suspend fun requestPin(target: AppTarget, treatment: IconTreatment): PinRequestResult
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
