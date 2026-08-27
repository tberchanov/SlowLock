package com.slowlock.feature.shortcut.domain

import com.slowlock.core.domain.AppTarget

/**
 * The permanent shape of a pinned shortcut. **Every constant here is frozen.**
 *
 * The normative document is `contracts/pinned-shortcut.md`; this is its in-code form. A pinned
 * shortcut outlives the app's opinion of it: SlowLock cannot enumerate the shortcuts a launcher
 * holds, cannot remove them, and must never ask the user to re-create them (FR-011). Each value
 * below is written into the launcher's persisted intent at pin time, so changing one migrates
 * nothing — it orphans every shortcut already on every home screen, and the failure surfaces at the
 * moment the user taps the icon.
 *
 * The behaviour *behind* these values is not frozen. The wait screen was added without re-pinning
 * anything, because the delay is read off disk at tap time rather than carried in the intent.
 *
 * This file is deliberately framework-free — no `android.*` imports — which is why [ACTION] is the
 * string literal rather than `Intent.ACTION_VIEW`. That keeps the frozen values assertable on the
 * JVM without a device, which `ShortcutContractTest` does.
 */
object ShortcutContract {

    /**
     * The fully-qualified name of the activity every pinned shortcut points at.
     *
     * **The most dangerous value in this codebase.** `ComponentName(context, X::class.java)`
     * resolves at build time, so renaming or moving the class compiles clean, pins *new* shortcuts
     * at the new name, and silently kills every shortcut already on a home screen.
     * `ShortcutContractTest` asserts the runtime FQN against this constant so the failure lands in
     * `./gradlew test` instead. If a rename ever becomes unavoidable the remedy is an
     * `<activity-alias>` under the old name — never a re-pin.
     */
    const val LAUNCH_ACTIVITY = "com.slowlock.shortcut.ShortcutLaunchActivity"

    /**
     * The intent extra carrying the target's package name — the persisted payload. A renamed key
     * means [ShortcutLaunchActivity] reads nothing and every existing shortcut dead-ends.
     */
    const val EXTRA_TARGET_PACKAGE = "com.slowlock.shortcut.extra.TARGET_PACKAGE"

    /**
     * The action of the persisted intent; `ShortcutInfo.Builder` requires one. The literal value of
     * `Intent.ACTION_VIEW`, written out so this file stays framework-free.
     */
    const val ACTION = "android.intent.action.VIEW"

    /**
     * The shortcut ID **is** the target's package name, verbatim (FR-025).
     *
     * Deriving the identity from the target rather than generating and storing one is what makes
     * re-pinning idempotent with no bookkeeping (FR-027): `updateShortcuts` finds an existing
     * shortcut without the app having to remember it. Any other scheme would orphan every existing
     * shortcut and add a second icon for every app (FR-026).
     */
    fun shortcutId(targetPackage: String): String = targetPackage
}

/**
 * The frozen shape of what gets pinned, as pure data — split out from `ShortcutInfo` so it can be
 * asserted on the JVM without a `Context`.
 */
data class ShortcutSpec(
    /** Equals the target's package name — see [ShortcutContract.shortcutId]. */
    val id: String,
    /** The target's label, verbatim. No suffix, no marker (spec, Assumptions). */
    val label: String,
    /**
     * Written to [ShortcutContract.EXTRA_TARGET_PACKAGE]. Equal to [id] today, stated separately
     * because the two are frozen for different reasons and could in principle diverge later.
     */
    val targetPackage: String,
)

/** Derives the pinned shortcut's shape from a resolved target. Pure, so it is unit-testable. */
fun shortcutSpec(target: AppTarget): ShortcutSpec = ShortcutSpec(
    id = ShortcutContract.shortcutId(target.packageName),
    label = target.label,
    targetPackage = target.packageName,
)
