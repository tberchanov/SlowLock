package com.slowlock.feature.apps.data

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import com.slowlock.feature.apps.domain.InstalledApp
import com.slowlock.feature.apps.domain.InstalledAppsRepository
import com.slowlock.core.data.packageVersionCode
import com.slowlock.core.domain.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Reads the launchable apps on the current user profile — the boundary where `LauncherApps` output
 * becomes plain [InstalledApp] values, and nothing more. Which of them the picker shows, and in what
 * order, is [com.slowlock.feature.apps.domain.LoadInstalledAppsUseCase]'s.
 *
 * `LauncherApps` needs no permission and shows no dialog (FR-015).
 */
@Singleton
class InstalledAppsSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : InstalledAppsRepository {

    private val launcherApps: LauncherApps =
        context.getSystemService(LauncherApps::class.java)

    /**
     * Enumerates off the injected dispatcher (FR-011). One entry per launcher *activity*, this app
     * included, in whatever order the platform answered — the caller narrows and collates.
     */
    override suspend fun load(): List<InstalledApp> = withContext(io) {
        val versionCodes = VersionCodeLookup(context.packageManager)
        launcherApps.getActivityList(null, Process.myUserHandle())
            .map { activity ->
                val packageName = activity.applicationInfo.packageName
                InstalledApp(
                    packageName = packageName,
                    label = activity.label.toString(),
                    versionCode = versionCodes.of(packageName),
                )
            }
    }

    /**
     * Memoizes the per-package version lookup, so a package exposing several launcher activities
     * costs one `PackageManager` round trip rather than one per activity.
     */
    private class VersionCodeLookup(private val packageManager: PackageManager) {
        private val cache = HashMap<String, Long>()

        fun of(packageName: String): Long = cache.getOrPut(packageName) {
            packageVersionCode(packageManager, packageName)
        }
    }
}
