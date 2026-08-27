package com.slowlock.feature.apps.data

import com.slowlock.feature.apps.domain.InstalledAppsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The `apps` capability's one data binding, beside what it binds rather than in a `di` package. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppsDataModule {

    /**
     * `@Singleton` because the implementation holds a cached `LauncherApps` handle. The list it
     * returns is not cached — every `load()` re-enumerates and re-collates under the current
     * locale.
     */
    @Binds
    @Singleton
    abstract fun installedAppsRepository(impl: InstalledAppsSource): InstalledAppsRepository
}
