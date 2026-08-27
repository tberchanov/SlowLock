package com.slowlock.feature.shortcut.data

import android.os.SystemClock
import com.slowlock.feature.shortcut.domain.ElapsedClock
import com.slowlock.feature.shortcut.domain.PinSupportRepository
import com.slowlock.feature.shortcut.domain.ShortcutPinRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The `shortcut` capability's data bindings. Beside what they bind, not in a `di` package (FR-032).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ShortcutDataModule {

    /**
     * `@Singleton` for the cached system-service lookup only. **The support answer is never
     * cached** (FR-028): every call asks the current launcher, because the user may have changed
     * it while the app was away, and this is the gate the constitution names on every pin attempt.
     */
    @Binds
    @Singleton
    abstract fun pinSupportRepository(impl: PinSupportSource): PinSupportRepository

    @Binds
    @Singleton
    abstract fun shortcutPinRepository(impl: ShortcutPinner): ShortcutPinRepository

    companion object {

        /**
         * **The only place `SystemClock` is named.**
         *
         * `elapsedRealtime()` counts through deep sleep and is immune to the wall clock changing
         * under a wait, which is why it and not `currentTimeMillis()` anchors the deadline
         * (research R4). Behind [ElapsedClock] so `WaitViewModel`'s timing is drivable on the JVM.
         */
        @Provides
        @Singleton
        fun elapsedClock(): ElapsedClock = ElapsedClock { SystemClock.elapsedRealtime() }
    }
}
