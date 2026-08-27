package com.slowlock.core.data

import android.content.pm.PackageManager
import android.os.Build

/**
 * The version code of [packageName], across every API level the app supports. Two platform seams
 * are folded into one call, because both call sites need the same answer and must not drift apart:
 *
 * - `PackageManager.PackageInfoFlags` is API 33; below that the `Int` flags overload is the only
 *   one, and it is deprecated on 33+.
 * - `longVersionCode` is API 28; below that `versionCode` is an `Int` and is widened here. Safe in
 *   the direction that matters — the value is only ever part of a cache key, so it needs to be
 *   stable and distinct, not numerically identical to what a newer platform reports.
 *
 * Never throws: a package that vanishes mid-enumeration still gets a stable key.
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
