package com.slowlock.shortcut

import android.content.Context
import android.content.pm.ShortcutManager

/**
 * Whether the current launcher accepts pin requests.
 *
 * Drives which screen the root shows (FR-029) and gates the pin call itself (FR-013). The
 * constitution names `isRequestPinShortcutSupported()` explicitly as a gate on *every* pin
 * attempt, which is why the check exists in both places rather than being cached once.
 */
sealed interface PinSupport {

    /**
     * Not yet checked this foreground pass. The initial value.
     *
     * **Never treat this as either answer.** It renders nothing — not the list, not an error.
     * Defaulting to [Supported] would flash the app list onto a device that cannot use it;
     * defaulting to [Unsupported] would flash an error at everyone else.
     */
    data object Unknown : PinSupport

    data object Supported : PinSupport

    data object Unsupported : PinSupport
}

/**
 * Maps a raw support check to [PinSupport].
 *
 * The check is a parameter rather than a call, so `PinGateTest` can drive both answers without
 * a launcher. This overload never returns [PinSupport.Unknown]: asking produces an answer, and
 * `Unknown` means the question has not been asked yet.
 */
fun pinSupport(isSupported: () -> Boolean): PinSupport =
    if (isSupported()) PinSupport.Supported else PinSupport.Unsupported

/**
 * The production check, against the real launcher.
 *
 * Re-evaluated on every `Lifecycle.Event.ON_START` and never cached across a background trip
 * (FR-028) — the user may have changed launcher while away.
 *
 * A missing `ShortcutManager` is read as unsupported: with no way to ask, the honest answer is
 * that pinning cannot be relied on here.
 */
fun pinSupport(context: Context): PinSupport = pinSupport {
    context.getSystemService(ShortcutManager::class.java)
        ?.isRequestPinShortcutSupported == true
}
