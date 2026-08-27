package com.slowlock.feature.apps.data

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import com.slowlock.feature.apps.domain.InstalledApp
import com.slowlock.feature.apps.domain.InstalledAppsRepository
import com.slowlock.feature.apps.domain.dedupeByPackage
import com.slowlock.feature.apps.domain.excludeSelf
import com.slowlock.feature.apps.domain.sortedByLabel
import com.slowlock.core.data.packageVersionCode
import com.slowlock.core.domain.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Reads the launchable apps on the current user profile — the boundary where `LauncherApps` output
 * becomes plain [InstalledApp] values, which leaves dedup, sorting and filtering as pure functions
 * testable without a device. `LauncherApps` needs no permission and shows no dialog (FR-015).
 */
@Singleton
class InstalledAppsSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : InstalledAppsRepository {

    private val launcherApps: LauncherApps =
        context.getSystemService(LauncherApps::class.java)

    /**
     * Enumerates, excludes SlowLock, deduplicates and sorts, in that order, off the injected
     * dispatcher (FR-011). The locale is read at load time rather than cached, so a language change
     * re-collates the list on the next [load] (FR-005).
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
            .excludeSelf(context.packageName)
            .dedupeByPackage()
            .sortedByLabel(currentLocale())
    }

    private fun currentLocale(): Locale =
        context.resources.configuration.locales.get(0) ?: Locale.getDefault()

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
