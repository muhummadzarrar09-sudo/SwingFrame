package app.swingframe.data

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache

class BitmapFrameCache(context: Context) {
    private val maxSizeKiB: Int
    private val cache: LruCache<Int, Bitmap>

    init {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryClassMiB = activityManager?.memoryClass ?: 256
        val cacheMiB = (memoryClassMiB / 6).coerceIn(24, 96)
        maxSizeKiB = cacheMiB * 1_024
        cache = object : LruCache<Int, Bitmap>(maxSizeKiB) {
            override fun sizeOf(key: Int, value: Bitmap): Int =
                (value.allocationByteCount / 1_024).coerceAtLeast(1)
        }
    }

    operator fun get(frameIndex: Int): Bitmap? = cache.get(frameIndex)

    fun put(frameIndex: Int, bitmap: Bitmap) {
        if (bitmap.allocationByteCount / 1_024 <= maxSizeKiB) {
            cache.put(frameIndex, bitmap)
        }
    }

    fun clear() = cache.evictAll()
}
