package com.example.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.data.CloneCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val matchedPresetKey: String?
)

class InstalledAppsBridge(private val context: Context) {

    // Bounded in-memory bitmap cache (~5 MB max) so scrolling 200+ apps never drops frames or OOMs.
    private val bitmapCache = object : LruCache<String, ImageBitmap>(96) {}
    private val missingPackages: MutableSet<String> = Collections.synchronizedSet(HashSet())

    suspend fun loadInstalledLaunchableApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    mainIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            val selfPackage = context.packageName
            val seen = HashSet<String>(activities.size)

            activities.mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == selfPackage || !seen.add(pkg)) return@mapNotNull null
                val label = try {
                    resolveInfo.loadLabel(pm)?.toString()?.trim().orEmpty()
                } catch (_: Exception) {
                    ""
                }.ifEmpty { pkg.substringAfterLast('.') }
                val preset = CloneCatalog.findByPackageName(pkg)
                InstalledAppInfo(
                    packageName = pkg,
                    label = label,
                    matchedPresetKey = preset?.key
                )
            }.sortedWith(
                compareByDescending<InstalledAppInfo> { it.matchedPresetKey != null }
                    .thenBy { it.label.lowercase() }
            )
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Instant non-blocking O(1) memory read for Compose UI — never performs Binder IPC on main thread.
     */
    fun getCachedIconBitmap(packageName: String): ImageBitmap? {
        if (packageName.isBlank()) return null
        return bitmapCache.get(packageName)
    }

    /**
     * Loads and rasterizes the app icon on Dispatchers.IO and caches it in LruCache.
     */
    suspend fun loadIconBitmap(packageName: String): ImageBitmap? {
        if (packageName.isBlank()) return null
        bitmapCache.get(packageName)?.let { return it }
        if (missingPackages.contains(packageName)) return null

        return withContext(Dispatchers.IO) {
            bitmapCache.get(packageName)?.let { return@withContext it }
            try {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val bitmap = drawable.toBitmap(width = 128, height = 128).asImageBitmap()
                bitmapCache.put(packageName, bitmap)
                bitmap
            } catch (_: OutOfMemoryError) {
                bitmapCache.evictAll()
                null
            } catch (_: Exception) {
                missingPackages.add(packageName)
                null
            }
        }
    }

    /**
     * Pre-warms icons for cloned spaces in the background so the Vault icon grid renders instantaneously.
     */
    suspend fun prewarmIcons(packageNames: Collection<String>) = withContext(Dispatchers.IO) {
        for (pkg in packageNames) {
            if (pkg.isBlank() || bitmapCache.get(pkg) != null || missingPackages.contains(pkg)) continue
            loadIconBitmap(pkg)
        }
    }

    /**
     * Safe synchronous Drawable fetch used only for one-off Home Screen shortcut bitmap rendering.
     */
    fun getAppIcon(packageName: String): Drawable? {
        if (packageName.isBlank() || missingPackages.contains(packageName)) return null
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            missingPackages.add(packageName)
            null
        }
    }

    fun clearMissingCache() {
        missingPackages.clear()
    }

    fun launchPersonalApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
