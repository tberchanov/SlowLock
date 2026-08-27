package com.slowlock.core.domain

import javax.inject.Qualifier

/**
 * This app's own package name.
 *
 * A qualifier rather than an interface, on the same reasoning as [IoDispatcher] (D4): the value is
 * fixed for the life of the process, so a qualifier already supplies the substitution seam and an
 * interface over it would be the second abstraction D4 names. A reading that can change between
 * calls gets an interface instead — see [CurrentLocale].
 *
 * Not `BuildConfig.APPLICATION_ID`: that constant is the configured applicationId, while the value
 * a running app must exclude itself by is `Context.packageName`, which carries any suffix the build
 * applied.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OwnPackageName
