package com.slowlock.feature.delay.domain

import com.slowlock.core.domain.DelayConfig
import com.slowlock.core.domain.DelayConfigRepository
import javax.inject.Inject

/**
 * The configuration the delay screen should open on.
 *
 * **An edit in hand wins over the value on disk.** A caller only has one after the user changed it,
 * so letting the read supply the delay would silently replace an edit made before the process died
 * with the stale saved value — a failure nothing on screen distinguishes from correct behaviour.
 *
 * The read happens either way, because the treatment has no edited counterpart: re-reading it gives
 * the identical answer, and the screen needs it to carry forward to the icon step.
 */
class LoadDelayConfigUseCase @Inject constructor(
    private val config: DelayConfigRepository,
) {

    /**
     * @param editedSeconds a delay the user has already changed, or `null` if they have not.
     */
    suspend operator fun invoke(packageName: String, editedSeconds: Int?): DelayConfig {
        val saved = config.load(packageName)
        return saved.copy(delaySeconds = editedSeconds ?: saved.delaySeconds)
    }
}
