package com.slowlock.shortcut

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The gate the constitution names: `isRequestPinShortcutSupported()` MUST guard **every** pin
 * attempt, not just the root's choice of screen (FR-013).
 *
 * It is a free function taking [support] as a lambda rather than a private `if` inside
 * [ShortcutPinner] so that `PinGateTest` can drive both answers on the JVM, with no launcher and
 * no `Context`. Support is re-read here, at the moment of the pin, because the user can change
 * launcher while the configuration screen is open — a value read when the screen opened would be
 * an assumption, not a check.
 *
 * [PinSupport.Unknown] is not an answer and does not open the gate.
 *
 * @return `true` if the pin was attempted. Note this reports whether the *request was issued*,
 *   never whether a shortcut was created — the launcher owns that outcome and the app
 *   deliberately does not observe it (FR-012).
 */
suspend fun pinWhenSupported(
    support: () -> PinSupport,
    pin: suspend () -> Unit,
): Boolean {
    if (support() != PinSupport.Supported) return false
    pin()
    return true
}

/**
 * Bakes the chosen treatment into a bitmap and asks the launcher to pin a shortcut carrying it.
 *
 * [sourceIcon] is **non-null by signature**, which is how obligation C12 is enforced by the
 * compiler rather than by discipline: a pinned shortcut is effectively permanent, so a neutral
 * placeholder on someone's home screen is worse than no shortcut, and this class must be
 * unreachable without real pixels.
 *
 * Nothing is recorded about what has been pinned (FR-027). Identity is derived from the target
 * (`ShortcutContract.shortcutId`), so re-pinning is idempotent with no bookkeeping — and any
 * such record would go stale the moment the user deleted a shortcut from their launcher, which
 * the app cannot observe.
 */
class ShortcutPinner(
    private val context: Context,
    /**
     * The support check, injected so it can be driven in tests and so the production path reads
     * the *current* launcher rather than a cached answer (FR-013, FR-028).
     */
    private val support: () -> PinSupport = { pinSupport(context) },
) {

    /**
     * Requests a pin for [target] carrying [treatment] applied to [sourceIcon].
     *
     * The bitmap work runs on [Dispatchers.IO] (FR-024). The two `ShortcutManager` calls stay
     * on the caller's dispatcher — they are cheap binder calls made from a foreground tap, and
     * `requestPinShortcut` puts a system dialog in front of the user, which belongs to the
     * interaction that asked for it.
     *
     * **No `IntentSender` is passed** (FR-012). The app does not observe the outcome; a success
     * callback would only tempt the confirmation the spec has promised not to show.
     *
     * @return `true` if the pin was requested, `false` if support was not confirmed at this
     *   moment. A `true` does **not** mean an icon appeared — see [pinWhenSupported].
     */
    suspend fun pin(
        target: ShortcutTarget,
        treatment: IconTreatment,
        sourceIcon: Bitmap,
    ): Boolean = pinWhenSupported(support) {
        val treated = withContext(Dispatchers.IO) { bake(sourceIcon, treatment) }
        request(shortcutSpec(target), treated)
    }

    /**
     * Draws [source] through the treatment's colour filter into a **new** bitmap sized to what
     * the current launcher asks for.
     *
     * A new bitmap, never a mutation: [source] is feature 001's cached icon and the list screen
     * is still drawing it (research.md R8).
     *
     * `getLauncherLargeIconSize()` is the size this launcher actually wants, and caps the result
     * at roughly 192x192 — comfortably inside the ~1 MB binder limit a full-resolution icon
     * could threaten. A non-positive answer (no `ActivityManager`, or a stub) falls back to the
     * source's own size rather than producing a zero-sized bitmap that would throw.
     *
     * A `null` matrix means [IconTreatment.Original]: no filter is attached at all, rather than
     * an identity one that would still cost a pass over every pixel.
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
     * The two calls from `contracts/pinned-shortcut.md`, in this order, **neither conditional**.
     *
     * Each is a no-op in the case it does not apply, which is precisely what removes the need to
     * ask "has this app been pinned before?":
     *
     * - [ShortcutManager.updateShortcuts] refreshes an already-pinned shortcut in place, and
     *   matches nothing when the ID has never been pinned.
     * - [ShortcutManager.requestPinShortcut] pins a new one, and on an ID the launcher already
     *   holds it succeeds immediately with no dialog and no second icon.
     *
     * `requestPinShortcut` alone would not do: AOSP short-circuits the already-pinned case
     * *without* applying the new `ShortcutInfo`, so a re-pin with a different treatment would
     * silently keep the old icon and fail FR-026 (research.md R3).
     *
     * `updateShortcuts` returns `false` when rate-limited. The limit resets on every foreground
     * entry and every call here originates in a foreground tap, so the value is logged and not
     * acted on — and a `false` also just means "nothing to update yet", which is the ordinary
     * first-pin case.
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
