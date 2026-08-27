package com.slowlock.core.data

import android.content.Context
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.CurrentLocale
import com.slowlock.core.domain.DefaultDispatcher
import com.slowlock.core.domain.DelayConfigRepository
import com.slowlock.core.domain.IoDispatcher
import com.slowlock.core.domain.OwnPackageName
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * The repositories no single capability owns, plus the two dispatchers and the two platform
 * readings a use case cannot take for itself.
 *
 * There is deliberately no `di` package (FR-032): a module lives beside what it binds.
 *
 * `core/data` holds shared repository *implementations* — a documented deviation in plan.md's
 * Complexity Tracking. Putting any of them in one capability's `data` would make the others import
 * it, which FR-030 forbids; duplicating the store would put two writers on one frozen file.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreDataModule {

    @Binds
    @Singleton
    abstract fun delayConfigRepository(impl: DelayConfigStore): DelayConfigRepository

    @Binds
    @Singleton
    abstract fun appTargetRepository(impl: AppTargetSource): AppTargetRepository

    @Binds
    @Singleton
    abstract fun appIconRepository(impl: AppIconCache): AppIconRepository

    @Binds
    @Singleton
    abstract fun currentLocale(impl: CurrentLocaleSource): CurrentLocale

    companion object {

        /**
         * The only place in this project that names `Dispatchers.IO` (D1). Every repository takes
         * its dispatcher through the constructor and applies it itself, so callers stay main-safe
         * (O2) and a test substitutes through that same parameter (D3).
         */
        @Provides
        @IoDispatcher
        fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

        /** As [ioDispatcher], for CPU-bound work. The only place naming `Dispatchers.Default`. */
        @Provides
        @DefaultDispatcher
        fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

        /**
         * The one place reading `Context.packageName`, so a use case can exclude this app from a
         * list of apps without reaching for a `Context` of its own.
         */
        @Provides
        @OwnPackageName
        fun ownPackageName(@ApplicationContext context: Context): String = context.packageName
    }
}
