package com.slowlock.feature.shortcut.data

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.util.Log
import androidx.compose.ui.graphics.asAndroidBitmap
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.IconTreatment
import com.slowlock.core.domain.IoDispatcher
import com.slowlock.shortcut.ShortcutLaunchActivity
import com.slowlock.feature.shortcut.domain.PinRequestResult
import com.slowlock.feature.shortcut.domain.PinSupport
import com.slowlock.feature.shortcut.domain.PinSupportRepository
import com.slowlock.feature.shortcut.domain.ShortcutContract
import com.slowlock.feature.shortcut.domain.ShortcutPinRepository
import com.slowlock.feature.shortcut.domain.ShortcutSpec
import com.slowlock.feature.shortcut.domain.shortcutSpec
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

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
 * Bakes the chosen treatment into a bitmap and asks the launcher to pin a shortcut carrying it.
 *
 * Nothing is recorded about what has been pinned (FR-027): identity is derived from the target, so
 * re-pinning is idempotent with no bookkeeping — and any such record would go stale the moment the
 * user deleted a shortcut from their launcher, which the app cannot observe.
 */
@Singleton
class ShortcutPinner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    /** Injected so tests can drive it and production reads the *current* launcher (FR-013). */
    private val support: PinSupportRepository,
    /**
     * Loaded here rather than handed in, which keeps `android.graphics.Bitmap` out of
     * [ShortcutPinRepository]'s signature (O1). The screen already asked for the same package and
     * version to draw its preview, so this hits the memory tier.
     */
    private val icons: AppIconRepository,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : ShortcutPinRepository {

    /**
     * Requests a pin for [target] carrying [treatment] applied to its icon.
     *
     * The bitmap work runs on the injected dispatcher (FR-024, D2); the two `ShortcutManager` calls
     * stay on the caller's — they are cheap binder calls from a foreground tap, and
     * `requestPinShortcut` puts a system dialog in front of the user.
     *
     * No `IntentSender` is passed (FR-012): the app does not observe the outcome, and a success
     * callback would only tempt the confirmation the spec has promised not to show.
     */
    override suspend fun requestPin(
        target: AppTarget,
        treatment: IconTreatment,
    ): PinRequestResult {
        // C12: non-null pixels or nothing. A pinned shortcut is effectively permanent, so a
        // placeholder on someone's home screen is worse than no shortcut at all.
        val source = icons.icon(target.packageName, target.versionCode)
            ?: return PinRequestResult.IconUnavailable

        return pinWhenSupported({ support.current() }) {
            val treated = withContext(io) { bake(source.asAndroidBitmap(), treatment) }
            request(shortcutSpec(target), treated)
        }
    }

    /**
     * Draws [source] through the treatment's colour filter into a *new* bitmap, never a mutation:
     * [source] is the cached icon and the list screen is still drawing it (research R8).
     *
     * `launcherLargeIconSize` caps the result at roughly 192x192, comfortably inside the ~1 MB
     * binder limit a full-resolution icon could threaten. A non-positive answer falls back to the
     * source's own size rather than a zero-sized bitmap that would throw.
     *
     * A `null` matrix means [IconTreatment.Original]: no filter at all, rather than an identity one
     * that would still cost a pass over every pixel.
     */
    private fun bake(source: Bitmap, treatment: IconTreatment): Bitmap {
        val size = context.getSystemService(ActivityManager::class.java)
            ?.launcherLargeIconSize
            ?.takeIf { it > 0 }
            ?: source.width

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            treatment.matrix?.let { colorFilter = ColorMatrixColorFilter(ColorMatrix(it)) }
        }

        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { output ->
            Canvas(output).drawBitmap(
                source,
                Rect(0, 0, source.width, source.height),
                Rect(0, 0, size, size),
                paint,
            )
        }
    }

    /**
     * The two calls from `contracts/pinned-shortcut.md`, in this order, neither conditional. Each
     * is a no-op where it does not apply, which removes the need to ask "has this app been pinned
     * before?": [ShortcutManager.updateShortcuts] refreshes an already-pinned shortcut and matches
     * nothing otherwise; [ShortcutManager.requestPinShortcut] pins a new one, and on an ID the
     * launcher already holds it succeeds immediately with no dialog and no second icon.
     *
     * `requestPinShortcut` alone would not do: AOSP short-circuits the already-pinned case
     * *without* applying the new `ShortcutInfo`, so a re-pin with a different treatment would
     * silently keep the old icon and fail FR-026 (research R3).
     *
     * `updateShortcuts` returning `false` is logged, not acted on: it means rate-limited — a limit
     * that resets on every foreground entry, and every call here originates in a foreground tap —
     * or simply "nothing to update yet", the ordinary first-pin case.
     */
    private fun request(spec: ShortcutSpec, icon: Bitmap) {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: run {
            Log.w(TAG, "No ShortcutManager; cannot pin ${spec.id}")
            return
        }

        val intent = Intent(ShortcutContract.ACTION)
            .setComponent(ComponentName(context, ShortcutLaunchActivity::class.java))
            .putExtra(ShortcutContract.EXTRA_TARGET_PACKAGE, spec.targetPackage)

        val info = ShortcutInfo.Builder(context, spec.id)
            .setShortLabel(spec.label)
            .setLongLabel(spec.label)
            .setIcon(Icon.createWithBitmap(icon))
            .setIntent(intent)
            .build()

        if (!manager.updateShortcuts(listOf(info))) {
            Log.d(TAG, "updateShortcuts did not apply for ${spec.id} (first pin, or rate limited)")
        }
        manager.requestPinShortcut(info, null)
    }

    private companion object {
        const val TAG = "SlowLock"
    }
}
