package com.kunpitech.zyvixa.repository

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kunpitech.zyvixa.model.LiveWallpaper
import com.kunpitech.zyvixa.model.StaticWallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object WallpaperRepository {

    suspend fun fetchRemoteCatalog(urlStr: String): List<LiveWallpaper>? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) ZyvixaApp/1.1")
                setRequestProperty("Accept", "application/json, text/plain, */*")
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 15000
                useCaches = false
            }
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val input = connection.inputStream
            val json = input.bufferedReader().use { it.readText() }
            
            data class RawLiveWallpaper(
                val id: String,
                val title: String,
                val description: String,
                val videoUrl: String?,
                val imageUrl: String?,
                val isRemote: Boolean,
                val gradientColors: List<String>?,
                val category: String?
            )
            
            val type = object : TypeToken<List<RawLiveWallpaper>>() {}.type
            val rawList: List<RawLiveWallpaper> = Gson().fromJson(json, type)
            
            rawList.map { raw ->
                val colorList = raw.gradientColors?.map { hex ->
                    try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (e: Exception) {
                        Color.Gray
                    }
                } ?: listOf(Color(0xFFE91E63), Color(0xFF3F51B5))
                
                // Categorize dynamic live wallpapers based on index/id mapping
                val cat = raw.category ?: when {
                    raw.id in listOf("default_video", "live_wallpaper_one", "wallpaper_3", "wallpaper_4", "video_2", "video_3", "video_4", "video_9", "video_10", "video_21") -> "Car"
                    raw.id in listOf("video_5", "video_7", "video_8", "video_12", "video_13", "video_19", "video_20", "video_24") -> "Nature"
                    raw.id in listOf("video_33", "video_34", "video_35", "video_36", "video_37", "video_38", "video_39") -> "Animal"
                    else -> "Abstract"
                }

                LiveWallpaper(
                    id = raw.id,
                    title = raw.title,
                    description = raw.description,
                    source = raw.videoUrl ?: raw.imageUrl ?: "default_video",
                    isRemote = raw.isRemote,
                    gradientColors = colorList,
                    imageUrl = raw.imageUrl,
                    category = cat
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchStaticCatalog(urlStr: String): List<StaticWallpaper>? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) ZyvixaApp/1.1")
                setRequestProperty("Accept", "application/json, text/plain, */*")
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 15000
                useCaches = false
            }
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val input = connection.inputStream
            val json = input.bufferedReader().use { it.readText() }
            
            data class RawStaticWallpaper(
                val id: String,
                val name: String,
                val url: String,
                val category: String?
            )
            
            val type = object : TypeToken<List<RawStaticWallpaper>>() {}.type
            val rawList: List<RawStaticWallpaper> = Gson().fromJson(json, type)
            
            rawList.map { raw ->
                // Distribute static wallpapers into clean aesthetic categories
                val idx = raw.id.substringAfterLast("_").toIntOrNull() ?: 1
                val cat = raw.category ?: when {
                    idx == 1 -> "Minimalist"
                    idx in 2..4 -> "Nature"
                    idx in 5..30 -> "Spiritual"
                    idx in 31..45 -> "Animal"
                    idx in 46..60 -> "Anime"
                    idx in 76..85 -> "Nature"
                    idx in 86..102 -> "Cartoon"
                    idx in 103..132 -> "Sport"
                    idx in 133..148 -> "Abstract"
                    idx in 149..149 -> "Car"
                    idx in 150..155 -> "Minimalist"
                    idx in 156..160 -> "Animal"
                    idx in 161..180 -> "Nature"
                    idx in 181..190 -> "Car"
                    else -> "Abstract"
                }

                StaticWallpaper(
                    id = raw.id,
                    name = raw.name,
                    url = raw.url,
                    category = cat
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadVideo(
        context: Context,
        id: String,
        urlStr: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "wallpapers")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val destFile = File(cacheDir, "$id.mp4")

            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(destFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    onProgress((total * 100 / fileLength).toInt())
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            return@withContext destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isWallpaperCached(context: Context, id: String): Boolean {
        val file = File(File(context.cacheDir, "wallpapers"), "$id.mp4")
        return file.exists() && file.length() > 0
    }

    fun getCachedWallpaperFile(context: Context, id: String): File {
        return File(File(context.cacheDir, "wallpapers"), "$id.mp4")
    }

    fun clearCache(context: Context): Long {
        val cacheDir = File(context.cacheDir, "wallpapers")
        var freed = 0L
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { file ->
                freed += file.length()
                file.delete()
            }
        }
        return freed
    }
}
