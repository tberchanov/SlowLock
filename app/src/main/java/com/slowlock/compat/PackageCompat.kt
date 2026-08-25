package com.slowlock.compat

import android.content.pm.PackageManager
import android.os.Build

/**
 * The version code of [packageName], across every API level the app supports.
 *
 * Two platform seams are folded into one call here, because both call sites — feature 001's
 * icon cache key ([com.slowlock.apps.InstalledAppsSource]) and the shortcut target's
 * ([com.slowlock.shortcut.resolveShortcutTarget]) — need exactly the same answer and must not
 * be allowed to drift apart:
 *
 * - `PackageManager.PackageInfoFlags` is API 33. Below that, the `Int` flags overload is the
 *   only one there is; it is deprecated on 33+, which is why the modern call is not simply
 *   used unconditionally.
 * - `longVersionCode` is API 28. Below that, `versionCode` is an `Int` and is widened here.
 *   The widening is safe in the direction that matters: the value is only ever used as part of
 *   a cache key, so it needs to be stable and distinct, not numerically identical to what a
 *   newer platform would report.
 *
 * **Never throws.** A package that vanishes mid-enumeration is the ordinary case, not an error,
 * and it still gets a usable, stable key from [UNKNOWN_VERSION].
 */
fun packageVersionCode(packageManager: PackageManager, packageName: String): Long =
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager
                .getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                .longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
        }
    }.getOrDefault(UNKNOWN_VERSION)

/** A package that vanished mid-lookup still gets a usable, stable cache key. */
const val UNKNOWN_VERSION = 0L
