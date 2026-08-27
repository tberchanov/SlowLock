package com.slowlock

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * The injection graph's root, and nothing else.
 *
 * Deliberately empty. An `Application` subclass is the most tempting place to put a static holder,
 * an eager initialiser, or a "just this one" singleton, and every one of those is the service
 * locator Principle II prohibits. Anything that looks like it belongs here belongs in a Hilt module
 * beside what it binds.
 *
 * Unlike [com.slowlock.feature.shortcut.domain.ShortcutContract.LAUNCH_ACTIVITY], this class name is not
 * frozen: the system reads it at install time from the manifest of the build that declares it.
 */
@HiltAndroidApp
class SlowLockApplication : Application()
