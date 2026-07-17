package app.swingframe.data

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.LruCache

/** Memory-bounded preview cache that also yields immediately to Android memory pressure. */
@Suppress("DEPRECATION")
class BitmapFrameCache(context: Context) : ComponentCallbacks2, AutoCloseable {
    private val appContext = context.applicationContext
    private val maxSizeKiB: Int
    private val cache: LruCache<Int, Bitmap>

    init {
        val activityManager = appContext.getSystemService(ActivityManager::class.java)
        val memoryClassMiB = activityManager?.memoryClass ?: 256
        val cacheMiB = (memoryClassMiB / 6).coerceIn(24, 96)
        maxSizeKiB = cacheMiB * 1_024
        cache = object : LruCache<Int, Bitmap>(maxSizeKiB) {
            override fun sizeOf(key: Int, value: Bitmap): Int =
                (value.allocationByteCount / 1_024).coerceAtLeast(1)
        }
        appContext.registerComponentCallbacks(this)
    }

    operator fun get(frameIndex: Int): Bitmap? = cache.get(frameIndex)

    fun put(frameIndex: Int, bitmap: Bitmap) {
        if (!bitmap.isRecycled && bitmap.allocationByteCount / 1_024 <= maxSizeKiB) {
            cache.put(frameIndex, bitmap)
        }
    }

    fun clear() = cache.evictAll()

    override fun onTrimMemory(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> clear()
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> clear()
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> cache.trimToSize(maxSizeKiB / 4)
        }
    }

    override fun onLowMemory() = clear()

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    override fun close() {
        clear()
        appContext.unregisterComponentCallbacks(this)
    }
}
