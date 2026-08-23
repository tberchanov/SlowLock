package com.slowlock.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Process
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Two-tier icon cache: an in-process [LruCache] over WebP files in `cacheDir` (research.md R5).
 *
 * Keys carry the version code (Constitution V), which makes invalidation implicit — an app
 * update produces a key that misses both tiers, so a stale icon is never served. Orphaned
 * files are removed by [sweep].
 *
 * Rasterization and file I/O run on [Dispatchers.IO] (FR-011).
 */
class AppIconCache(private val context: Context) {

    private val launcherApps: LauncherApps =
        context.getSystemService(LauncherApps::class.java)

    private val memory = LruCache<String, ImageBitmap>(MEMORY_ENTRIES)

    private val iconDir = File(context.cacheDir, DIRECTORY)

    /**
     * The icon for [app], or `null` if it cannot be produced — the row then keeps its
     * neutral placeholder and stays selectable (FR-016).
     *
     * A failure is deliberately not cached, so a later visit retries.
     */
    suspend fun icon(app: InstalledApp): ImageBitmap? = icon(app.packageName, app.versionCode)

    /**
     * The icon for a package at a given version, on the same two tiers and the same keys.
     *
     * The cache never needed a whole [InstalledApp] — only these two values (Constitution V) —
     * and feature 002's `ShortcutTarget` is not one. Widening the entry point is cheaper than
     * making the shortcut package construct a throwaway [InstalledApp], which would couple it to
     * feature 001's list model for nothing.
     *
     * Cache keys, on-disk file naming, and sweep behaviour are unchanged.
     */
    suspend fun icon(packageName: String, versionCode: Long): ImageBitmap? {
        val key = iconCacheKey(packageName, versionCode)
        memory.get(key)?.let { return it }

        return withContext(Dispatchers.IO) {
            val file = fileFor(packageName, versionCode)
            val bitmap =
                readFromDisk(file) ?: rasterize(packageName)?.also { writeToDisk(file, it) }
            bitmap?.asImageBitmap()?.also { memory.put(key, it) }
        }
    }

    /**
     * Deletes cached files for versions that are no longer installed (FR-012, SC-005).
     *
     * Called after a successful load rather than at construction, so it sweeps against the
     * freshly enumerated set instead of a guess.
     */
    suspend fun sweep(installed: List<InstalledApp>) = withContext(Dispatchers.IO) {
        val current = installed.mapTo(HashSet()) { fileNameFor(it.packageName, it.versionCode) }
        iconDir.listFiles()?.forEach { file ->
            if (file.name !in current) file.delete()
        }
        Unit
    }

    private fun readFromDisk(file: File): Bitmap? =
        if (file.exists()) BitmapFactory.decodeFile(file.path) else null

    private fun writeToDisk(file: File, bitmap: Bitmap) {
        runCatching {
            iconDir.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, it) }
        }
    }

    private fun rasterize(packageName: String): Bitmap? = runCatching {
        launcherApps.getActivityList(packageName, Process.myUserHandle())
            .firstOrNull()
            ?.getIcon(context.resources.displayMetrics.densityDpi)
            ?.toBitmap()
    }.getOrNull()

    private fun fileFor(packageName: String, versionCode: Long) =
        File(iconDir, fileNameFor(packageName, versionCode))

    private fun fileNameFor(packageName: String, versionCode: Long) =
        "$packageName-$versionCode.webp"

    private companion object {
        /** Comfortably more than one screenful of rows, far less than a full app list. */
        const val MEMORY_ENTRIES = 40
        const val DIRECTORY = "app-icons"
    }
}
