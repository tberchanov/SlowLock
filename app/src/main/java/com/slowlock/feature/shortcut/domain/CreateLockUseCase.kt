package com.slowlock.feature.shortcut.domain

import com.slowlock.core.domain.AppIconRepository
import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.DelayConfig
import com.slowlock.core.domain.DelayConfigRepository
import com.slowlock.core.domain.IconTreatment
import javax.inject.Inject

/**
 * Making a lock: re-resolve, write the configuration, then ask the launcher to pin.
 *
 * **The order is the contract, not an accident of line order.** The target may have been
 * uninstalled while the screen sat open, so the resolution at this moment is the one that counts.
 * The configuration is written *before* the pin request goes out, so a launcher that pins
 * asynchronously can never fire the shortcut before its delay exists on disk.
 *
 * Creating a lock is not a write of its own: a lock exists exactly when its shortcut is pinned, so
 * the pin request creates it — and only if the user accepts the launcher's dialog.
 */
class CreateLockUseCase @Inject constructor(
    private val targets: AppTargetRepository,
    private val config: DelayConfigRepository,
    private val support: PinSupportRepository,
    private val icons: AppIconRepository,
    private val pins: ShortcutPinRepository,
) {

    suspend operator fun invoke(
        packageName: String,
        delaySeconds: Int,
        treatment: IconTreatment,
    ): CreateLockResult {
        val target = targets.resolve(packageName) ?: return CreateLockResult.TargetMissing
        config.save(packageName, DelayConfig(delaySeconds, treatment))
        return CreateLockResult.Created(requestPin(target, treatment))
    }

    /**
     * The icon before the gate, in that order: a pinned shortcut is effectively permanent, so a
     * placeholder on someone's home screen is worse than no shortcut at all (C12), and there is no
     * point asking the launcher about a pin that has nothing to carry.
     */
    private suspend fun requestPin(target: AppTarget, treatment: IconTreatment): PinRequestResult {
        val icon = icons.icon(target.packageName, target.versionCode)
            ?: return PinRequestResult.IconUnavailable

        return pinWhenSupported({ support.current() }) { pins.requestPin(target, treatment, icon) }
    }
}

/**
 * The gate the constitution names: `isRequestPinShortcutSupported()` MUST guard every pin attempt,
 * not just the root's choice of screen (FR-013).
 *
 * A free function taking [support] as a lambda rather than a private `if`, so `PinGateTest` can
 * drive both answers on the JVM with no launcher and no `Context`. Support is re-read at the moment
 * of the pin, because the user can change launcher while the configuration screen is open.
 * [PinSupport.Unknown] is not an answer and does not open the gate.
 *
 * @return [PinRequestResult.Requested] if the pin was attempted, [PinRequestResult.Unsupported] if
 *   the gate held — whether the *request was issued*, never whether a shortcut was created, which
 *   the launcher owns and the app deliberately does not observe (FR-012).
 */
suspend fun pinWhenSupported(
    support: suspend () -> PinSupport,
    pin: suspend () -> Unit,
): PinRequestResult {
    if (support() != PinSupport.Supported) return PinRequestResult.Unsupported
    pin()
    return PinRequestResult.Requested
}

/**
 * What became of an attempt to make a lock.
 *
 * Neither case is an error, and neither leaves half a lock behind: a missing target writes nothing,
 * and a pin that was not requested leaves the device exactly as it was.
 */
sealed interface CreateLockResult {

    /**
     * The configuration was written and the pin path ran to its conclusion, which [pin] describes.
     *
     * The launcher owns whether an icon actually appears and never tells the app, so [pin] says
     * only how far the request got.
     */
    data class Created(val pin: PinRequestResult) : CreateLockResult

    /** The package did not resolve at the moment of the attempt. Nothing was written or pinned. */
    data object TargetMissing : CreateLockResult
}
