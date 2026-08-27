package com.slowlock.core.data

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Process
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.slowlock.feature.apps.domain.iconCacheKey
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.core.domain.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Two-tier icon cache: an in-process [LruCache] over WebP files in `cacheDir` (research.md R5).
 *
 * Keys carry the version code (Constitution V), which makes invalidation implicit — an app
 * update produces a key that misses both tiers, so a stale icon is never served. Orphaned
 * files are removed by [sweep].
 *
 * Rasterization and file I/O run on the injected [IoDispatcher] (FR-011, obligation D2).
 */
@Singleton
class AppIconCache @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : AppIconRepository {

    private val launcherApps: LauncherApps =
        context.getSystemService(LauncherApps::class.java)

    private val memory = LruCache<String, ImageBitmap>(MEMORY_ENTRIES)

    private val iconDir = File(context.cacheDir, DIRECTORY)

    /**
     * The icon for a package at a given version, on two tiers and one key.
     *
     * `null` if it cannot be produced — the row then keeps its neutral placeholder and stays
     * selectable (FR-016). A failure is deliberately not cached, so a later visit retries.
     */
    override suspend fun icon(packageName: String, versionCode: Long): ImageBitmap? {
        val key = iconCacheKey(packageName, versionCode)
        memory.get(key)?.let { return it }

        return withContext(io) {
            val file = fileFor(packageName, versionCode)
            val bitmap =
                readFromDisk(file) ?: rasterize(packageName)?.also { writeToDisk(file, it) }
            bitmap?.asImageBitmap()?.also { memory.put(key, it) }
        }
    }

    /**
     * Deletes cached files for versions that are no longer installed (FR-012, SC-005). Called after
     * a successful load rather than at construction, so it sweeps against the freshly enumerated
     * set.
     *
     * [keep] holds [iconCacheKey] values, which keeps `android.*` out of the domain signature (O1):
     * the caller names what it wants by the same key it would ask for an icon by.
     */
    override suspend fun sweep(keep: List<String>) = withContext(io) {
        val current = keep.mapTo(HashSet(), ::fileNameFor)
        iconDir.listFiles()?.forEach { file ->
            if (file.name !in current) file.delete()
        }
        Unit
    }

    private fun readFromDisk(file: File): Bitmap? =
        if (file.exists()) BitmapFactory.decodeFile(file.path) else null

    /**
     * Best-effort: a full disk, a swept directory or a revoked cache dir all fail silently, because
     * the file tier is an optimisation. The icon is already rasterized and the next visit simply
     * rasterizes it again — nothing the user sees depends on the write landing.
     */
    private fun writeToDisk(file: File, bitmap: Bitmap) {
        runCatching {
            iconDir.mkdirs()
            file.outputStream().use { bitmap.compress(webpFormat(), 100, it) }
        }
    }

    /**
     * `WEBP_LOSSLESS` is API 30. Below it the only WEBP constant is the deprecated `WEBP`, which at
     * quality 100 encodes losslessly anyway — so the branch is about which constant exists, not
     * about what gets written.
     */
    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSLESS
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

    private fun rasterize(packageName: String): Bitmap? = runCatching {
        launcherApps.getActivityList(packageName, Process.myUserHandle())
            .firstOrNull()
            ?.getIcon(context.resources.displayMetrics.densityDpi)
            ?.toBitmap()
    }.getOrNull()

    private fun fileFor(packageName: String, versionCode: Long) =
        File(iconDir, fileNameFor(iconCacheKey(packageName, versionCode)))

    /**
     * The on-disk name for a cache key. The two formats are one substitution apart and a package
     * name can contain neither separator, so the mapping is total and reversible — which leaves one
     * decision about what a cached icon is called rather than one per tier.
     */
    private fun fileNameFor(cacheKey: String) =
        cacheKey.replace(KEY_SEPARATOR, FILE_SEPARATOR) + FILE_EXTENSION

    private companion object {
        /** Comfortably more than one screenful of rows, far less than a full app list. */
        const val MEMORY_ENTRIES = 40
        const val DIRECTORY = "app-icons"
        const val KEY_SEPARATOR = ':'
        const val FILE_SEPARATOR = '-'
        const val FILE_EXTENSION = ".webp"
    }
}
