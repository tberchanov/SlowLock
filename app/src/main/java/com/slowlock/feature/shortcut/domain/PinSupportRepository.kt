package com.slowlock.feature.shortcut.domain

/**
 * Whether the current launcher accepts pin requests
 * (contracts/repository-interfaces.md, `shortcut/domain`).
 *
 * Obligations:
 *
 * - **Asking always produces an answer.** [PinSupport.Unknown] means the question has not been
 *   asked and is the caller's initial value, never a return from here.
 * - A missing `ShortcutManager` reads as [PinSupport.Unsupported]: with no way to ask, the honest
 *   answer is that pinning cannot be relied on.
 * - **Never cached across a background trip.** The user may have changed launcher while away, so a
 *   value read once at startup would be an assumption rather than a check.
 */
interface PinSupportRepository {

    /** The launcher's current answer. Never [PinSupport.Unknown]. */
    suspend fun current(): PinSupport
}
