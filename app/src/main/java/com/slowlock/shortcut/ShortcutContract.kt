package com.slowlock.shortcut

/**
 * The permanent shape of a pinned shortcut. **Every constant here is frozen.**
 *
 * The normative document is `contracts/pinned-shortcut.md`; this is its in-code form. Unlike
 * the rest of this feature — a draft, expected to be replaced by the delay-configuration screen,
 * though in the event feature 003 wrapped it rather than replacing it — nothing below may change
 * once a shortcut has been pinned on a real device.
 *
 * The reason is that a pinned shortcut outlives the app's opinion of it. SlowLock cannot
 * enumerate the shortcuts a launcher holds, cannot remove them, and must never ask the user to
 * re-create them (FR-011). Each value below is written into the launcher's persisted intent at
 * pin time, so changing one does not migrate anything — it orphans every shortcut already on
 * every home screen, and the failure surfaces at the moment the user taps the icon.
 *
 * What is *not* frozen is the behaviour behind these values, and **feature 003 collected on
 * that**: [ShortcutLaunchActivity] no longer starts the target immediately — it shows a wait
 * screen for the app's configured delay first. Every shortcut pinned by feature 002 picked the
 * wait up with nothing re-pinned and nothing asked of the user, because the delay is read off
 * disk at tap time rather than carried in the intent. Freezing the shape is what bought that.
 *
 * Two corrections to what this paragraph used to predict, since a wrong prediction left standing
 * in a frozen file is how the file stops being trusted: there is **no countdown** — the wait
 * screen is deliberately motionless, and that is a design obligation rather than an omission
 * (`specs/003-launch-delay/contracts/wait-screen.md` W8–W11) — and there is **no schedule check**,
 * which remains unbuilt.
 *
 * None of that touched a single value below, which is the point.
 *
 * This file is deliberately framework-free — no `android.*` imports — which is why [ACTION] is
 * the string literal rather than `Intent.ACTION_VIEW`. It keeps the frozen values assertable on
 * the JVM without a device, and `ShortcutContractTest` does exactly that.
 */
object ShortcutContract {

    /**
     * The fully-qualified name of the activity every pinned shortcut points at.
     *
     * **The most dangerous value in this codebase.** `ComponentName(context, X::class.java)`
     * resolves at build time, so renaming or moving the class compiles clean and pins *new*
     * shortcuts at the new name, while every shortcut already on a home screen keeps pointing
     * at the old one and dies silently. `ShortcutContractTest` asserts the runtime FQN against
     * this constant so the failure lands in `./gradlew test` instead.
     *
     * If a rename ever becomes genuinely unavoidable, the remedy is an `<activity-alias>` under
     * the old name — never a re-pin.
     */
    const val LAUNCH_ACTIVITY = "com.slowlock.shortcut.ShortcutLaunchActivity"

    /**
     * The intent extra carrying the target's package name — the persisted payload.
     *
     * A renamed key means [ShortcutLaunchActivity] reads nothing and every existing shortcut
     * dead-ends.
     */
    const val EXTRA_TARGET_PACKAGE = "com.slowlock.shortcut.extra.TARGET_PACKAGE"

    /**
     * The action of the persisted intent. `ShortcutInfo.Builder` requires one.
     *
     * The literal value of `Intent.ACTION_VIEW`, written out so this file stays framework-free.
     */
    const val ACTION = "android.intent.action.VIEW"

    /**
     * The shortcut ID **is** the target's package name, verbatim (FR-025).
     *
     * This is what makes re-pinning idempotent with no bookkeeping (FR-027): the identity is
     * derived from the target rather than generated and stored, so `updateShortcuts` finds an
     * existing shortcut without the app having to remember it was created. Any other scheme
     * would orphan every existing shortcut and add a second icon for every app (FR-026).
     */
    fun shortcutId(targetPackage: String): String = targetPackage
}

/**
 * The frozen shape of what gets pinned, as pure data.
 *
 * Split out from `ShortcutInfo` deliberately so it can be asserted on the JVM without a
 * `Context` (data-model.md §`ShortcutSpec`).
 */
data class ShortcutSpec(
    /** Equals the target's package name — see [ShortcutContract.shortcutId]. */
    val id: String,
    /** The target's label, verbatim. No suffix, no marker (spec, Assumptions). */
    val label: String,
    /**
     * Written to [ShortcutContract.EXTRA_TARGET_PACKAGE]. Equal to [id] today, and stated
     * separately because the two are frozen for different reasons and could in principle
     * diverge in a later feature.
     */
    val targetPackage: String,
)

/**
 * Derives the pinned shortcut's shape from a resolved target. Pure, so the frozen values are
 * unit-testable.
 */
fun shortcutSpec(target: ShortcutTarget): ShortcutSpec = ShortcutSpec(
    id = ShortcutContract.shortcutId(target.packageName),
    label = target.label,
    targetPackage = target.packageName,
)
