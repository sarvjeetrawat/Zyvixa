package com.kunpitech.zyvixa.ui.screens

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunpitech.zyvixa.ZyvixaWallpaperService
import com.kunpitech.zyvixa.ui.components.*
import com.kunpitech.zyvixa.viewmodel.WallpaperViewModel
import com.kunpitech.zyvixa.repository.WallpaperRepository
import com.kunpitech.zyvixa.ads.AdMobManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DashboardScreen(
    viewModel: WallpaperViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAdDialog by remember { mutableStateOf(false) }
    var adsWatchedCount by remember { mutableStateOf(0) }
    var targetAdsCount by remember { mutableStateOf(1) }

    // State collection from ViewModel
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val animSpeed by viewModel.animSpeed.collectAsState()
    val touchEnabled by viewModel.touchEnabled.collectAsState()
    val videoUriStr by viewModel.videoUriStr.collectAsState()
    val videoLoop by viewModel.videoLoop.collectAsState()

    val activeTab by viewModel.activeTab.collectAsState()
    val activeStaticCategory by viewModel.activeStaticCategory.collectAsState()
    val activeLiveCategory by viewModel.activeLiveCategory.collectAsState()

    val activeDownloadId by viewModel.activeDownloadId.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    // Dynamic Title name calculations
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

    val applyWallpaperAction = {
        if (selectedType == "Static") {
            scope.launch {
                try {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Setting wallpaper...", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "Wallpaper set successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
            Spacer(modifier = Modifier.height(24.dp))

            // Upper Branding Header Group
            Text(
                text = "Zyvixa",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = "Premium Wallpapers Hub",
                fontSize = 12.sp,
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Premium Live Simulated Preview Box
            MockPhoneFrame {
                LiveSimulatedPreview(
                    type = selectedType,
                    videoUriStr = videoUriStr,
                    videoLoop = videoLoop,
                    modifier = Modifier.fillMaxSize()
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
                                if (activeTab != index) {
                                    viewModel.activeTab.value = index
                                    if (index == 0) {
                                        val firstStatic = viewModel.staticWallpaperCatalog.firstOrNull()
                                        if (firstStatic != null) {
                                            viewModel.selectedType.value = "Static"
                                            viewModel.videoUriStr.value = firstStatic.url
                                        }
                                    } else {
                                        val firstLive = viewModel.liveWallpaperCatalog.firstOrNull()
                                        if (firstLive != null) {
                                            viewModel.selectedType.value = "Video"
                                            if (WallpaperRepository.isWallpaperCached(context, firstLive.id)) {
                                                val file = WallpaperRepository.getCachedWallpaperFile(context, firstLive.id)
                                                viewModel.videoUriStr.value = file.absolutePath
                                            } else {
                                                viewModel.videoUriStr.value = firstLive.source
                                            }
                                        }
                                    }
                                }
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

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content Rendering
            if (activeTab == 0) {
                // STATIC WALLPAPERS TAB
                val staticCategories = listOf("All", "Abstract", "Animal", "Anime", "Car", "Cartoon", "Minimalist", "Nature", "Sport", "Spiritual")
                
                CategorySelector(
                    categories = staticCategories,
                    selectedCategory = activeStaticCategory,
                    onCategorySelected = { viewModel.activeStaticCategory.value = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter lists based on selected Category
                val filteredStatic = remember(activeStaticCategory, viewModel.staticWallpaperCatalog.size) {
                    if (activeStaticCategory == "All") {
                        viewModel.staticWallpaperCatalog
                    } else {
                        viewModel.staticWallpaperCatalog.filter { it.category == activeStaticCategory }
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

                    filteredStatic.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { wp ->
                                val isSelected = selectedType == "Static" && videoUriStr == wp.url
                                StaticWallpaperItem(
                                    wp = wp,
                                    isSelected = isSelected,
                                    onClick = {
                                        viewModel.selectedType.value = "Static"
                                        viewModel.videoUriStr.value = wp.url
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                // LIVE WALLPAPERS TAB
                val liveCategories = listOf("All", "Car", "Nature", "Abstract")

                CategorySelector(
                    categories = liveCategories,
                    selectedCategory = activeLiveCategory,
                    onCategorySelected = { viewModel.activeLiveCategory.value = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter lists based on selected Category
                val filteredLive = remember(activeLiveCategory, viewModel.liveWallpaperCatalog.size) {
                    if (activeLiveCategory == "All") {
                        viewModel.liveWallpaperCatalog
                    } else {
                        viewModel.liveWallpaperCatalog.filter { it.category == activeLiveCategory }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose Cinema Loop Design",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    filteredLive.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { wp ->
                                val isSelected = selectedType == "Video" && (
                                    videoUriStr == wp.source ||
                                    videoUriStr == wp.id ||
                                    videoUriStr.endsWith("${wp.id}.mp4") ||
                                    (wp.id == "default_video" && videoUriStr == "default_video")
                                )
                                val isDownloadingThis = activeDownloadId == wp.id

                                LiveWallpaperItem(
                                    context = context,
                                    wp = wp,
                                    isSelected = isSelected,
                                    isDownloadingActive = isDownloadingThis,
                                    downloadProgress = downloadProgress,
                                    onClick = {
                                        if (wp.isRemote) {
                                            if (WallpaperRepository.isWallpaperCached(context, wp.id)) {
                                                val file = WallpaperRepository.getCachedWallpaperFile(context, wp.id)
                                                viewModel.selectedType.value = "Video"
                                                viewModel.videoUriStr.value = file.absolutePath
                                            } else {
                                                viewModel.startDownload(context, wp)
                                            }
                                        } else {
                                            viewModel.selectedType.value = "Video"
                                            viewModel.videoUriStr.value = wp.source
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }

        // Floating Bottom Actions Card (Set as Wallpaper Button)
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
                            targetAdsCount = if (selectedType == "Static") 1 else 2
                            adsWatchedCount = 0
                            showAdDialog = true
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

        if (showAdDialog) {
            val adNumberText = if (targetAdsCount == 2) " (Ad ${adsWatchedCount + 1} of 2)" else ""
            AlertDialog(
                onDismissRequest = { showAdDialog = false },
                title = {
                    Text(
                        text = "Unlock Wallpaper$adNumberText",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    val desc = if (targetAdsCount == 2) {
                        "Please watch 2 quick video ads to unlock and set this premium live wallpaper.$adNumberText"
                    } else {
                        "Watch a quick video ad to unlock and set this premium wallpaper."
                    }
                    Text(
                        text = desc,
                        color = Color.LightGray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAdDialog = false
                            val activity = context as? Activity
                            if (activity != null) {
                                Toast.makeText(context, "Loading Ad...", Toast.LENGTH_SHORT).show()
                                AdMobManager.showRewardedAd(activity) { rewardEarned ->
                                    if (rewardEarned) {
                                        adsWatchedCount++
                                        if (adsWatchedCount >= targetAdsCount) {
                                            applyWallpaperAction()
                                        } else {
                                            // Trigger next ad sequence
                                            showAdDialog = true
                                        }
                                    } else {
                                        Toast.makeText(context, "Ad incomplete. Unlock failed.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                applyWallpaperAction()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                    ) {
                        val btnText = if (targetAdsCount == 2) "Watch Ad ${adsWatchedCount + 1}/2" else "Watch Ad"
                        Text(btnText, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAdDialog = false }
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF15102A),
                titleContentColor = Color.White,
                textContentColor = Color.LightGray
            )
        }
    }
}
