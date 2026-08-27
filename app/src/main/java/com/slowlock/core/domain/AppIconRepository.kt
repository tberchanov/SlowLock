package com.slowlock.core.domain

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Loads app icons. Obligations:
 *
 * - Keyed by package *and* version code, so an update invalidates the cached icon implicitly: a new
 *   version produces a key that misses every tier.
 * - Icons never travel inside UI state — a bitmap in a `StateFlow` is retained for as long as the
 *   state is; each row asks for its own as it scrolls into view.
 * - Rasterisation and file I/O run off the main thread.
 * - `null` is an ordinary outcome: the row keeps its placeholder and stays usable, and the failure
 *   is deliberately not cached, so a later visit retries.
 *
 * [ImageBitmap] is Compose's type: no `android.*` type crosses this boundary (O1).
 */
interface AppIconRepository {

    /** The icon for [packageName] at [versionCode], or `null` if it cannot be produced. */
    suspend fun icon(packageName: String, versionCode: Long): ImageBitmap?

    /** Drops cached files for anything not in [keep]. Called after a completed enumeration. */
    suspend fun sweep(keep: List<String>)
}
