package com.slowlock.core.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.IoDispatcher
import com.slowlock.core.domain.resolveTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Resolves a package's display facts against the platform.
 *
 * **The decision is not here.** Which packages resolve to `null` — no launch intent, no label —
 * is [resolveTarget]'s, which is a pure function over three lambdas and is where the
 * constitution's mandated null-`getLaunchIntentForPackage()` coverage lives. This class supplies
 * those lambdas and owns the thread hop, and holds nothing else worth asserting.
 *
 * Both configuration screens and both lists re-resolve through here, so the label rule below —
 * `LauncherApps`, lowest-labelled activity wins — has exactly one definition. That is the same
 * rule `dedupeByPackage` applies when building the app list, which is what makes the text on a
 * configuration screen match the row the user tapped rather than an `ApplicationInfo` label that
 * can differ.
 */
@Singleton
class AppTargetSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : AppTargetRepository {

    private val launcherApps: LauncherApps =
        context.getSystemService(LauncherApps::class.java)

    override suspend fun resolve(packageName: String): AppTarget? = withContext(io) {
        val packageManager = context.packageManager
        resolveTarget(
            packageName = packageName,
            isLaunchable = { packageManager.getLaunchIntentForPackage(it) != null },
            loadLabel = {
                runCatching {
                    launcherApps.getActivityList(it, Process.myUserHandle())
                        .minOfOrNull { activity -> activity.label.toString() }
                }.getOrNull()
            },
            loadVersionCode = { packageVersionCode(packageManager, it) },
        )
    }
}
