package com.slowlock.feature.locks.data

import com.slowlock.feature.locks.domain.LockOrderRepository
import com.slowlock.feature.locks.domain.PinnedShortcutsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The `locks` capability's data bindings, beside what they bind rather than in a `di` package. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocksDataModule {

    /** `@Singleton`: the implementation caches a `SharedPreferences` handle for `slowlock.locks`. */
    @Binds
    @Singleton
    abstract fun lockOrderRepository(impl: LockOrderStore): LockOrderRepository

    /**
     * `@Singleton` for its cached system-service lookup. **Nothing about the answer is cached** —
     * the pinned set is re-read on every ask, because the user can pin or remove an icon at any
     * moment and a remembered answer would be a screen that disagrees with the home screen.
     */
    @Binds
    @Singleton
    abstract fun pinnedShortcutsRepository(impl: PinnedShortcutsSource): PinnedShortcutsRepository
}
