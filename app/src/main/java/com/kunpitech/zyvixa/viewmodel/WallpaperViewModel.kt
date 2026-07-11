package com.kunpitech.zyvixa.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.zyvixa.model.LiveWallpaper
import com.kunpitech.zyvixa.model.StaticWallpaper
import com.kunpitech.zyvixa.repository.WallpaperRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WallpaperViewModel(context: Context) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("zyvixa_settings", Context.MODE_PRIVATE)

    // UI state values backed by preferences where appropriate
    val selectedType = MutableStateFlow(prefs.getString("wp_type", "Static") ?: "Static")
    val selectedTheme = MutableStateFlow(prefs.getString("wp_theme", "Ocean Breeze") ?: "Ocean Breeze")
    val animSpeed = MutableStateFlow(prefs.getFloat("wp_speed", 1.0f))
    val touchEnabled = MutableStateFlow(prefs.getBoolean("wp_touch", true))
    val videoUriStr = MutableStateFlow(prefs.getString("wp_video_uri", "default_video") ?: "default_video")
    val videoLoop = MutableStateFlow(prefs.getBoolean("wp_video_loop", true))

    // Active tab and active categories
    val activeTab = MutableStateFlow(if (selectedType.value == "Video") 1 else 0)
    val activeStaticCategory = MutableStateFlow("All")
    val activeLiveCategory = MutableStateFlow("All")

    // Downloading states
    val activeDownloadId = MutableStateFlow<String?>(null)
    val downloadProgress = MutableStateFlow(0)

    // Wallpaper Catalogs
    val liveWallpaperCatalog = mutableStateListOf<LiveWallpaper>()
    val staticWallpaperCatalog = mutableStateListOf<StaticWallpaper>()

    init {
        // Save preference updates on change
        viewModelScope.launch {
            launch {
                selectedType.collect { prefs.edit().putString("wp_type", it).apply() }
            }
            launch {
                selectedTheme.collect { prefs.edit().putString("wp_theme", it).apply() }
            }
            launch {
                animSpeed.collect { prefs.edit().putFloat("wp_speed", it).apply() }
            }
            launch {
                touchEnabled.collect { prefs.edit().putBoolean("wp_touch", it).apply() }
            }
            launch {
                videoUriStr.collect { prefs.edit().putString("wp_video_uri", it).apply() }
            }
            launch {
                videoLoop.collect { prefs.edit().putBoolean("wp_video_loop", it).apply() }
            }
            launch {
                activeTab.collect {
                    selectedType.value = if (it == 0) "Static" else "Video"
                }
            }
        }

        // Initialize local starter lists
        loadStarters()

        // Fetch remote catalogs
        fetchCatalogs()
    }

    private fun loadStarters() {
        // Live Wallpaper starters
        val liveStarters = listOf(
            LiveWallpaper("default_video", "BMW Red Eye", "Red JDM headlight glow", "default_video", false, listOf(androidx.compose.ui.graphics.Color(0xFFE91E63), androidx.compose.ui.graphics.Color(0xFF3F51B5)), "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/live-wallpaper/default_video.png", "Car"),
            LiveWallpaper("live_wallpaper_one", "M3 Midnight", "Night city drift highway", "live_wallpaper_one", false, listOf(androidx.compose.ui.graphics.Color(0xFF9C27B0), androidx.compose.ui.graphics.Color(0xFF2196F3)), "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/live-wallpaper/live_wallpaper_one.png", "Car"),
            LiveWallpaper("wallpaper_3", "BMW Glow Eye", "Dark aesthetic red headlights", "wallpaper_3", false, listOf(androidx.compose.ui.graphics.Color(0xFF673AB7), androidx.compose.ui.graphics.Color(0xFF009688)), "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/live-wallpaper/wallpaper_3.png", "Car"),
            LiveWallpaper("wallpaper_4", "Tokyo Drift", "Neon street sliding loop", "wallpaper_4", false, listOf(androidx.compose.ui.graphics.Color(0xFFE040FB), androidx.compose.ui.graphics.Color(0xFF00E5FF)), "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/live-wallpaper/wallpaper_4.png", "Car")
        )
        liveWallpaperCatalog.addAll(liveStarters)

        // Static Wallpaper starters (1..30)
        val pngIndices = setOf(1, 2, 3, 4, 9, 11, 13, 14, 15, 20, 25)
        val staticStarters = (1..30).map { idx ->
            val ext = if (pngIndices.contains(idx)) "png" else "jpg"
            val cat = when {
                idx == 1 -> "Minimalist"
                idx in 2..4 -> "Nature"
                else -> "Spiritual"
            }
            StaticWallpaper(
                id = "wallpaper_$idx",
                name = "Wallpaper $idx",
                url = "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/wallpaper/wallpaper_$idx.$ext",
                category = cat
            )
        }
        staticWallpaperCatalog.addAll(staticStarters)
    }

    fun fetchCatalogs() {
        viewModelScope.launch {
            // Fetch live catalog
            val liveUrl = "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/live-wallpaper/catalog.json?t=${System.currentTimeMillis()}"
            val remoteLive = WallpaperRepository.fetchRemoteCatalog(liveUrl)
            if (!remoteLive.isNullOrEmpty()) {
                liveWallpaperCatalog.clear()
                liveWallpaperCatalog.addAll(remoteLive)
            }

            // Fetch static catalog
            val staticUrl = "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/wallpaper/catalog.json?t=${System.currentTimeMillis()}"
            val remoteStatic = WallpaperRepository.fetchStaticCatalog(staticUrl)
            if (!remoteStatic.isNullOrEmpty()) {
                staticWallpaperCatalog.clear()
                staticWallpaperCatalog.addAll(remoteStatic)
            }
        }
    }

    fun startDownload(context: Context, wp: LiveWallpaper) {
        viewModelScope.launch {
            activeDownloadId.value = wp.id
            downloadProgress.value = 0
            val file = WallpaperRepository.downloadVideo(context, wp.id, wp.source) { progress ->
                downloadProgress.value = progress
            }
            if (file != null) {
                videoUriStr.value = file.absolutePath
            }
            activeDownloadId.value = null
        }
    }
}
