package com.slowlock.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the launchable apps on the current user profile.
 *
 * This is the boundary: `LauncherApps` output is mapped to plain [InstalledApp] values here
 * and nowhere else, which is what leaves dedup, sorting, and filtering as pure functions
 * testable without a device (research.md R10).
 *
 * `LauncherApps` needs no permission and shows no dialog (FR-015).
 */
class InstalledAppsSource(private val context: Context) {

    private val launcherApps: LauncherApps =
        context.getSystemService(LauncherApps::class.java)

    /**
     * Enumerates, excludes SlowLock, deduplicates, and sorts — in that order
     * (`data-model.md` construction rules). Entirely off the main thread (FR-011).
     *
     * The locale is read at load time rather than cached, so a language change picked up by
     * the next [load] re-collates the list (FR-005).
     */
    suspend fun load(): List<InstalledApp> = withContext(Dispatchers.IO) {
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
     * Memoizes the per-package version lookup, so a package exposing several launcher
     * activities costs one `PackageManager` round trip rather than one per activity.
     */
    private class VersionCodeLookup(private val packageManager: PackageManager) {
        private val cache = HashMap<String, Long>()

        fun of(packageName: String): Long = cache.getOrPut(packageName) {
            runCatching {
                packageManager
                    .getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                    .longVersionCode
            }.getOrDefault(UNKNOWN_VERSION)
        }
    }

    private companion object {
        /** A package that vanished mid-enumeration still gets a usable, stable cache key. */
        const val UNKNOWN_VERSION = 0L
    }
}
