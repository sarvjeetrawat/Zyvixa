package com.kunpitech.zyvixa

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.widget.VideoView
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.media.MediaPlayer
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import coil.compose.AsyncImage
import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kunpitech.zyvixa.ui.theme.ZyvixaTheme
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZyvixaTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF07070F)
                ) { innerPadding ->
                    WallpaperDashboard(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

data class LiveWallpaper(
    val id: String,
    val title: String,
    val description: String,
    val source: String,
    val isRemote: Boolean,
    val gradientColors: List<Color>,
    val imageUrl: String? = null
)

data class StaticWallpaper(
    val id: String,
    val name: String,
    val url: String
)

suspend fun downloadVideo(
    context: Context,
    id: String,
    urlStr: String,
    onProgress: (Int) -> Unit
): File? = withContext(Dispatchers.IO) {
    try {
        val destDir = File(context.cacheDir, "wallpapers")
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        val destFile = File(destDir, "$id.mp4")
        if (destFile.exists() && destFile.length() > 0) {
            return@withContext destFile
        }

        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
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

suspend fun fetchRemoteCatalog(urlStr: String): List<LiveWallpaper>? = withContext(Dispatchers.IO) {
    try {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
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
            val gradientColors: List<String>?
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
            
            LiveWallpaper(
                id = raw.id,
                title = raw.title,
                description = raw.description,
                source = raw.videoUrl ?: raw.imageUrl ?: "default_video",
                isRemote = raw.isRemote,
                gradientColors = colorList,
                imageUrl = raw.imageUrl
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
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            return@withContext null
        }

        val input = connection.inputStream
        val json = input.bufferedReader().use { it.readText() }
        
        data class RawStaticWallpaper(
            val id: String,
            val name: String,
            val url: String
        )
        
        val type = object : TypeToken<List<RawStaticWallpaper>>() {}.type
        val rawList: List<RawStaticWallpaper> = Gson().fromJson(json, type)
        
        rawList.map { raw ->
            StaticWallpaper(
                id = raw.id,
                name = raw.name,
                url = raw.url
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun rememberVideoThumbnail(context: Context, resId: Int): Bitmap? {
    return remember(resId) {
        if (resId == 0) return@remember null
        val retriever = MediaMetadataRetriever()
        try {
            val afd = context.resources.openRawResourceFd(resId)
            retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }
}

@Composable
fun MockPhoneFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .width(180.dp)
            .height(320.dp)
            .background(Color(0xFF07070C), RoundedCornerShape(32.dp))
            .border(3.dp, Color(0xFF1E1E30), RoundedCornerShape(32.dp))
            .padding(6.dp)
            .clip(RoundedCornerShape(26.dp))
    ) {
        content()

        // Speaker Notch
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp)
                .size(width = 45.dp, height = 4.dp)
                .background(Color(0xFF1E1E30), RoundedCornerShape(2.dp))
        )

        // Camera Punch Hole
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(10.dp)
                .background(Color.Black, CircleShape)
                .border(1.dp, Color(0x3DFFFFFF), CircleShape)
        )

        // Status Bar Overlays
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "09:41",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📶",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 8.sp
                )
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 7.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.85f)
                            .background(Color.Green)
                    )
                }
            }
        }
    }
}

@Composable
fun WallpaperDashboard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zyvixa_settings", Context.MODE_PRIVATE) }

    // Read settings states
    var selectedType by remember {
        val saved = prefs.getString("wp_type", "Static") ?: "Static"
        val migrated = if (saved == "Plexus" || saved == "Matrix" || saved == "Aura Flow" || saved == "Static") "Static" else "Video"
        mutableStateOf(migrated)
    }
    var selectedTheme by remember { mutableStateOf(prefs.getString("wp_theme", "Ocean Breeze") ?: "Ocean Breeze") }
    var animSpeed by remember { mutableStateOf(prefs.getFloat("wp_speed", 1.0f)) }
    var touchEnabled by remember { mutableStateOf(prefs.getBoolean("wp_touch", true)) }
    var videoUriStr by remember { mutableStateOf(prefs.getString("wp_video_uri", "default_video") ?: "default_video") }
    var videoLoop by remember { mutableStateOf(prefs.getBoolean("wp_video_loop", true)) }

    // Tab state: 0 for Wallpaper (Static), 1 for Live Wallpaper (Videos)
    var activeTab by remember { mutableStateOf(if (selectedType == "Video") 1 else 0) }

    var activeDownloadId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // Save states to SharedPreferences
    LaunchedEffect(selectedType, selectedTheme, animSpeed, touchEnabled, videoUriStr, videoLoop) {
        prefs.edit().apply {
            putString("wp_type", selectedType)
            putString("wp_theme", selectedTheme)
            putFloat("wp_speed", animSpeed)
            putBoolean("wp_touch", touchEnabled)
            putString("wp_video_uri", videoUriStr)
            putBoolean("wp_video_loop", videoLoop)
            apply()
        }
    }

    // Synchronize tab and selectedType
    LaunchedEffect(activeTab) {
        if (activeTab == 0) {
            selectedType = "Static"
        } else {
            selectedType = "Video"
        }
    }

    val activeName = remember(selectedType, videoUriStr) {
        if (selectedType == "Video") {
            val name = videoUriStr.substringAfterLast("/").replace(".mp4", "")
            when (name) {
                "default_video" -> "BMW Red Eye"
                "live_wallpaper_one" -> "M3 Midnight"
                "wallpaper_3" -> "BMW Glow Eye"
                "wallpaper_4" -> "Tokyo Drift"
                "wallpaper_5" -> "Yellow Forest"
                "wallpaper_6" -> "Cyber Cruise"
                "wallpaper_7" -> "Neon Highway"
                "wallpaper_8" -> "Forest Stream"
                "wallpaper_9" -> "Urban Night"
                "wallpaper_10" -> "Cosmic Leaves"
                else -> name.replace("_", " ").replaceFirstChar { it.uppercase() }
            }
        } else if (selectedType == "Static") {
            val name = videoUriStr.substringAfterLast("/").substringBeforeLast(".")
            name.replace("wallpaper_", "Wallpaper ").replaceFirstChar { it.uppercase() }
        } else {
            selectedType
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Main Scrollable Dashboard Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Branding Header
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "ZYVIXA",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Premium Live Wallpaper Studio",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Mock Mobile Device Preview Frame
            MockPhoneFrame {
                LiveSimulatedPreview(
                    type = selectedType,
                    theme = selectedTheme,
                    speed = animSpeed,
                    touchEnabled = touchEnabled,
                    videoUriStr = videoUriStr,
                    videoLoop = videoLoop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Glassmorphic Tab Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                 listOf("Wallpaper", "Live Wallpaper").forEachIndexed { index, tabTitle ->
                    val isTabSelected = activeTab == index
                    val tabBg by animateColorAsState(if (isTabSelected) Color(0xFF1E1E30) else Color.Transparent)
                    val tabTextCol by animateColorAsState(if (isTabSelected) Color(0xFF00E5FF) else Color.Gray)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(tabBg)
                            .clickable { 
                                activeTab = index
                                selectedType = if (index == 0) "Static" else "Video"
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = tabTextCol
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tab Content Rendering
            if (activeTab == 0) {
                val localStaticStarter = remember {
                    val pngIndices = setOf(1, 2, 3, 4, 9, 11, 13, 14, 15, 20, 25)
                    val webpIndices = setOf<Int>()
                    (1..30).map { idx ->
                        val ext = when {
                            pngIndices.contains(idx) -> "png"
                            webpIndices.contains(idx) -> "webp"
                            else -> "jpg"
                        }
                        StaticWallpaper(
                            id = "wallpaper_$idx",
                            name = "Wallpaper $idx",
                            url = "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/wallpaper/wallpaper_$idx.$ext"
                        )
                    }
                }

                val staticWallpapersList = remember { mutableStateListOf<StaticWallpaper>().apply { addAll(localStaticStarter) } }

                LaunchedEffect(Unit) {
                    val remoteList = fetchStaticCatalog("https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/wallpaper/catalog.json?t=${System.currentTimeMillis()}")
                    if (!remoteList.isNullOrEmpty()) {
                        staticWallpapersList.clear()
                        staticWallpapersList.addAll(remoteList)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose Static Wallpaper",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    staticWallpapersList.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { wp ->
                                val isSelected = selectedType == "Static" && videoUriStr == wp.url
                                val cardBorderColor by animateColorAsState(if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E1E30))
                                val cardBgColor by animateColorAsState(if (isSelected) Color(0xFF0F1E2E) else Color(0xFF0F0F1A))

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 6.dp)
                                        .clickable {
                                            selectedType = "Static"
                                            videoUriStr = wp.url
                                        },
                                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, cardBorderColor)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(9f / 16f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF151525)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = wp.url,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = wp.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Static background",
                                            fontSize = 9.sp,
                                            color = Color.Gray,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                val localStarterList = remember {
                    listOf(
                        LiveWallpaper("default_video", "BMW Red Eye", "Red JDM headlight glow", "default_video", false, listOf(Color(0xFFE91E63), Color(0xFF3F51B5)), "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/live-wallpaper/default_video.png"),
                        LiveWallpaper("live_wallpaper_one", "M3 Midnight", "Night city drift highway", "live_wallpaper_one", false, listOf(Color(0xFF9C27B0), Color(0xFF2196F3)), "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/live-wallpaper/live_wallpaper_one.png"),
                        LiveWallpaper("wallpaper_3", "BMW Glow Eye", "Dark aesthetic red headlights", "wallpaper_3", false, listOf(Color(0xFF673AB7), Color(0xFF009688)), "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/live-wallpaper/wallpaper_3.png"),
                        LiveWallpaper("wallpaper_4", "Tokyo Drift", "Neon street sliding loop", "wallpaper_4", false, listOf(Color(0xFFE040FB), Color(0xFF00E5FF)), "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/live-wallpaper/wallpaper_4.png")
                    )
                }

                val wallpaperCatalog = remember { mutableStateListOf<LiveWallpaper>().apply { addAll(localStarterList) } }

                LaunchedEffect(Unit) {
                    val remoteList = fetchRemoteCatalog("https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/live-wallpaper/catalog.json?t=${System.currentTimeMillis()}")
                    if (!remoteList.isNullOrEmpty()) {
                        wallpaperCatalog.clear()
                        wallpaperCatalog.addAll(localStarterList)
                        val remoteOnly = remoteList.filter { rem -> localStarterList.none { loc -> loc.id == rem.id } }
                        wallpaperCatalog.addAll(remoteOnly)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose Cinema Loop",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    wallpaperCatalog.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { wp ->
                                val isCached = !wp.isRemote || isWallpaperCached(context, wp.id)
                                val isVideoSelected = selectedType == "Video" && (
                                    (wp.isRemote && videoUriStr == getCachedWallpaperFile(context, wp.id).absolutePath) ||
                                    (!wp.isRemote && videoUriStr == wp.id)
                                )

                                val isThisDownloading = activeDownloadId == wp.id

                                val cardBorderColor by animateColorAsState(if (isVideoSelected) Color(0xFF00E5FF) else if (isThisDownloading) Color(0xFFE040FB) else Color(0xFF1E1E30))
                                val cardBgColor by animateColorAsState(if (isVideoSelected) Color(0xFF0F1E2E) else Color(0xFF0F0F1A))

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 6.dp)
                                        .clickable(enabled = !isThisDownloading) {
                                            if (isCached) {
                                                selectedType = "Video"
                                                videoUriStr = if (wp.isRemote) {
                                                    getCachedWallpaperFile(context, wp.id).absolutePath
                                                } else {
                                                    wp.id
                                                }
                                            } else {
                                                if (activeDownloadId == null) {
                                                    activeDownloadId = wp.id
                                                    downloadProgress = 0
                                                    scope.launch {
                                                        val downloadedFile = downloadVideo(context, wp.id, wp.source) { progress ->
                                                            downloadProgress = progress
                                                        }
                                                        activeDownloadId = null
                                                        if (downloadedFile != null && downloadedFile.exists()) {
                                                            selectedType = "Video"
                                                            videoUriStr = downloadedFile.absolutePath
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, cardBorderColor)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(9f / 16f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Brush.verticalGradient(wp.gradientColors)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // 1. Render Video playing if selected (and cached/local)
                                            if (isVideoSelected && isCached) {
                                                val videoPath = if (wp.isRemote) {
                                                    getCachedWallpaperFile(context, wp.id).absolutePath
                                                } else {
                                                    wp.id
                                                }
                                                
                                                val miniUri = remember(videoPath) {
                                                    if (videoPath.startsWith("/")) {
                                                        Uri.fromFile(File(videoPath))
                                                    } else {
                                                        val resId = context.resources.getIdentifier(videoPath, "raw", context.packageName)
                                                        if (resId != 0) {
                                                            Uri.parse("android.resource://${context.packageName}/$resId")
                                                        } else {
                                                            val fallbackId = context.resources.getIdentifier("default_video", "raw", context.packageName)
                                                            Uri.parse("android.resource://${context.packageName}/$fallbackId")
                                                        }
                                                    }
                                                }
                                                
                                                key(miniUri) {
                                                    var miniPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
                                                    DisposableEffect(Unit) {
                                                        onDispose {
                                                            miniPlayer?.release()
                                                            miniPlayer = null
                                                        }
                                                    }
                                                    AndroidView(
                                                        modifier = Modifier.fillMaxSize(),
                                                        factory = { ctx ->
                                                            TextureView(ctx).apply {
                                                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                                                    var activeSurface: Surface? = null
                                                                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                                                        val surface = Surface(surfaceTexture)
                                                                        activeSurface = surface
                                                                        try {
                                                                            miniPlayer?.release()
                                                                            miniPlayer = MediaPlayer().apply {
                                                                                setDataSource(ctx, miniUri)
                                                                                setSurface(surface)
                                                                                isLooping = true
                                                                                setVolume(0f, 0f)
                                                                                setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                                                                setOnPreparedListener {
                                                                                    start()
                                                                                }
                                                                                prepareAsync()
                                                                            }
                                                                        } catch (e: Exception) {
                                                                            e.printStackTrace()
                                                                        }
                                                                    }
                                                                    override fun onSurfaceTextureSizeChanged(s: SurfaceTexture, w: Int, h: Int) {}
                                                                    override fun onSurfaceTextureDestroyed(s: SurfaceTexture): Boolean {
                                                                        miniPlayer?.release()
                                                                        miniPlayer = null
                                                                        activeSurface?.release()
                                                                        activeSurface = null
                                                                        return true
                                                                    }
                                                                    override fun onSurfaceTextureUpdated(s: SurfaceTexture) {}
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            } else {
                                                // 2. Render Remote Thumbnail
                                                if (wp.isRemote && !wp.imageUrl.isNullOrEmpty()) {
                                                    AsyncImage(
                                                        model = wp.imageUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                        onError = { err ->
                                                            android.util.Log.e("CoilError", "Failed to load image from: ${wp.imageUrl}", err.result.throwable)
                                                        }
                                                    )
                                                }
                                                
                                                // 3. Render Local Thumbnail
                                                if (!wp.isRemote) {
                                                    val localResId = remember(wp.id) {
                                                        context.resources.getIdentifier(wp.id, "raw", context.packageName)
                                                    }
                                                    val localThumbnail = rememberVideoThumbnail(context, localResId)
                                                    if (localThumbnail != null) {
                                                        Image(
                                                            bitmap = localThumbnail.asImageBitmap(),
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                        )
                                                    }
                                                }
                                            }

                                            // 4. Translucent Dark Mask
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.2f))
                                            )

                                            // 5. Action Icon or Downloader Progress
                                            if (isThisDownloading) {
                                                CircularProgressIndicator(
                                                    progress = { downloadProgress / 100f },
                                                    color = Color(0xFFE040FB),
                                                    strokeWidth = 3.dp,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = wp.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isVideoSelected) Color.White else Color.Gray,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (isThisDownloading) "Downloading $downloadProgress%" else wp.description,
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Playback Settings",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F1A)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E1E30))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Loop Video Playback", fontSize = 12.sp, color = Color.Gray)
                                    Text(text = "Continuously repeat the video wallpaper", fontSize = 9.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = videoLoop,
                                    onCheckedChange = { videoLoop = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00E5FF),
                                        checkedTrackColor = Color(0xFF0F2E3E),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color(0xFF1E1E30)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Bottom spacing to prevent floating action bar overlap
            Spacer(modifier = Modifier.height(100.dp))
        }

        // Floating Bottom Actions Card (Set as Live Wallpaper Button)
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF907070F)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF1E1E30))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Selected Theme",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activeName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                val isDownloadingActive = activeDownloadId != null
                Button(
                    onClick = {
                        if (!isDownloadingActive) {
                            if (selectedType == "Static") {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.Main) {
                                            android.widget.Toast.makeText(context, "Setting wallpaper...", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        
                                        val imageLoader = coil.ImageLoader(context)
                                        val request = coil.request.ImageRequest.Builder(context)
                                            .data(videoUriStr)
                                            .allowHardware(false)
                                            .build()
                                        val result = (imageLoader.execute(request) as? coil.request.SuccessResult)?.drawable
                                        val bitmap = (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                        
                                        if (bitmap != null) {
                                            val wm = WallpaperManager.getInstance(context)
                                            wm.setBitmap(bitmap)
                                            withContext(Dispatchers.Main) {
                                                android.widget.Toast.makeText(context, "Wallpaper set successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                android.widget.Toast.makeText(context, "Failed to load image", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        withContext(Dispatchers.Main) {
                                            android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                    putExtra(
                                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                        ComponentName(context, ZyvixaWallpaperService::class.java)
                                    )
                                }
                                context.startActivity(intent)
                            }
                        }
                    },
                    enabled = !isDownloadingActive,
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color(0xFF1E1E30)),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(
                                if (isDownloadingActive) {
                                    Brush.horizontalGradient(listOf(Color(0xFF301030), Color(0xFF151525)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF9C27B0)))
                                }
                            )
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isDownloadingActive) "Downloading $downloadProgress%" else "Set as Wallpaper",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDownloadingActive) Color.Gray else Color.White
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// COMPOSE MINI SIMULATED PREVIEW (WITH VIDEOVIEW SUPPORT)
// -------------------------------------------------------------
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LiveSimulatedPreview(
    type: String,
    theme: String,
    speed: Float,
    touchEnabled: Boolean,
    videoUriStr: String?,
    videoLoop: Boolean
) {
    if (type == "Video") {
        val context = LocalContext.current
        val parsedUri = remember(videoUriStr) {
            val uriStr = videoUriStr
            if (uriStr.isNullOrEmpty() || uriStr == "default_video") {
                Uri.parse("android.resource://${context.packageName}/${R.raw.default_video}")
            } else if (uriStr.startsWith("content://") || uriStr.startsWith("file://") || uriStr.startsWith("android.resource://")) {
                Uri.parse(uriStr)
            } else if (uriStr.startsWith("/")) {
                Uri.fromFile(File(uriStr))
            } else {
                val resId = context.resources.getIdentifier(uriStr, "raw", context.packageName)
                if (resId != 0) {
                    Uri.parse("android.resource://${context.packageName}/$resId")
                } else {
                    Uri.parse("android.resource://${context.packageName}/${R.raw.default_video}")
                }
            }
        }

        key(parsedUri) {
            var localPlayer: MediaPlayer? = null
            DisposableEffect(Unit) {
                onDispose {
                    localPlayer?.release()
                }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                val surface = Surface(surfaceTexture)
                                try {
                                    localPlayer?.release()
                                    localPlayer = MediaPlayer().apply {
                                        setDataSource(ctx, parsedUri)
                                        setSurface(surface)
                                        isLooping = videoLoop
                                        setVolume(0f, 0f)
                                        setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                        setOnPreparedListener {
                                            start()
                                        }
                                        prepareAsync()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                localPlayer?.release()
                                localPlayer = null
                                surface.release()
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                    }
                }
            )
        }
    } else if (type == "Static" || type == "Image") {
        AsyncImage(
            model = videoUriStr,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F1A))
        )
    }
}

class LocalParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float
)