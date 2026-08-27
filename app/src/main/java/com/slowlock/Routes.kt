package com.slowlock

import com.slowlock.core.domain.IconTreatment
import kotlinx.serialization.Serializable

/**
 * The graph's four addresses. They live at the root package because the graph belongs to no
 * capability, and every `navigate` call in this app is in [SlowLockRoot] (research R7).
 *
 * There is no route carrying where the flow was entered from: the back stack answers that, so back
 * from [DelayConfig] reaches whichever entry is beneath it.
 */

/** Renders the intro when there are no locks and the Locks screen when there are (FR-011). */
@Serializable
data object Home

@Serializable
data object AppList

/**
 * The package name alone: the destination's own holder reads that app's saved configuration, so
 * the delay and the treatment have one loader and one shape on both routes in.
 */
@Serializable
data class DelayConfig(val packageName: String)

/** [treatment] crosses by [Enum.name], the same token `DelayConfigStore` persists. */
@Serializable
data class ShortcutConfig(
    val packageName: String,
    val delaySeconds: Int,
    val treatment: IconTreatment,
)
