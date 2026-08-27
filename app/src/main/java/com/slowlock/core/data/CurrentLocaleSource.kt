package com.slowlock.core.data

import android.content.Context
import com.slowlock.core.domain.CurrentLocale
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The production locale reading, against the real configuration.
 *
 * Not `suspend` and on no dispatcher: `Resources.getConfiguration()` reads an in-memory object with
 * no binder call behind it, so a thread hop here would cost more than the read.
 */
@Singleton
class CurrentLocaleSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CurrentLocale {

    override fun now(): Locale =
        context.resources.configuration.locales.get(0) ?: Locale.getDefault()
}
