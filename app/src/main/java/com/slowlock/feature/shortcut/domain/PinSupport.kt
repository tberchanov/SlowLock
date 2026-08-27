package com.slowlock.feature.shortcut.domain

/**
 * Whether the current launcher accepts pin requests. Drives which screen the root shows (FR-029)
 * and gates the pin call itself (FR-013) — `isRequestPinShortcutSupported()` guards *every*
 * attempt, which is why the check exists in both places rather than being cached once.
 */
sealed interface PinSupport {

    /**
     * Not yet checked this foreground pass, and never to be treated as either answer: it renders
     * nothing. [Supported] would flash the app list onto a device that cannot use it, [Unsupported]
     * would flash an error at everyone else.
     */
    data object Unknown : PinSupport

    data object Supported : PinSupport

    data object Unsupported : PinSupport
}

/**
 * Maps a raw support check to [PinSupport]. The check is a parameter rather than a call, so
 * `PinGateTest` can drive both answers without a launcher. Never returns [PinSupport.Unknown]:
 * asking produces an answer, and `Unknown` means the question has not been asked.
 */
fun pinSupport(isSupported: () -> Boolean): PinSupport =
    if (isSupported()) PinSupport.Supported else PinSupport.Unsupported
