package com.kunpitech.zyvixa.ui.screens

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import com.kunpitech.zyvixa.ZyvixaWallpaperService
import com.kunpitech.zyvixa.ui.components.*
import com.kunpitech.zyvixa.model.LiveWallpaper
import com.kunpitech.zyvixa.model.StaticWallpaper
import com.kunpitech.zyvixa.viewmodel.WallpaperViewModel
import com.kunpitech.zyvixa.repository.WallpaperRepository
import com.kunpitech.zyvixa.ads.AdMobManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class AppScreen {
    Preview,
    Browse
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: WallpaperViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // UI Screen Navigation State
    var activeAppScreen by remember { mutableStateOf(AppScreen.Preview) }
    
    // Ad and popup states
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

    // Favorites mapping state
    val favorites = remember { mutableStateMapOf<String, Boolean>() }
    
    // Search query state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

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

    // Dynamic Clock & Date for Full-Screen Preview Screen
    var timeString by remember { mutableStateOf("12:45") }
    var dateString by remember { mutableStateOf("Saturday, July 11") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDateTime.now()
            timeString = now.format(DateTimeFormatter.ofPattern("HH:mm"))
            dateString = now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
            kotlinx.coroutines.delay(10000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07070F))
    ) {
        // SCREEN SWITCHING BLOCK
        Crossfade(targetState = activeAppScreen, modifier = Modifier.fillMaxSize(), label = "screen_fade") { screen ->
            when (screen) {
                AppScreen.Preview -> {
                    // PREVIEW IS THE BACKGROUND SCREEN
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background wallpaper preview layer
                        if (selectedType == "Static") {
                            SubcomposeAsyncImage(
                                model = videoUriStr,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            ) {
                                val state = painter.state
                                if (state is AsyncImagePainter.State.Loading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF07070F)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color(0xFFE2B266).copy(alpha = 0.5f)
                                        )
                                    }
                                } else {
                                    SubcomposeAsyncImageContent()
                                }
                            }
                        } else {
                            LiveSimulatedPreview(
                                type = selectedType,
                                videoUriStr = videoUriStr,
                                videoLoop = videoLoop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Gradient protection overlay (top/bottom shadows)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.5f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )

                        // Top Header clock overlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            // Serif Brand Logo
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Zyvixa",
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(start = 2.dp, top = 10.dp)
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2B266))
                                )
                            }

                            // Big Serif Clock and Date
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = timeString,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 62.sp,
                                    fontWeight = FontWeight.Light,
                                    color = Color.White,
                                    lineHeight = 62.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dateString,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Bottom sheet controls band
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 68.dp) // Avoid overlap with bottom nav bar
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            // Underlined Tabs switcher (Static / Live)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
                            ) {
                                listOf("Static", "Live").forEachIndexed { index, tabTitle ->
                                    val isSelected = (index == 0 && selectedType == "Static") || (index == 1 && selectedType == "Video")
                                    val textCol by animateColorAsState(if (isSelected) Color.White else Color.Gray)
                                    Column(
                                        modifier = Modifier.clickable {
                                            if (index == 0) {
                                                viewModel.activeTab.value = 0
                                                val firstStatic = viewModel.staticWallpaperCatalog.firstOrNull()
                                                if (firstStatic != null) {
                                                    viewModel.selectedType.value = "Static"
                                                    viewModel.videoUriStr.value = firstStatic.url
                                                }
                                            } else {
                                                viewModel.activeTab.value = 1
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
                                        },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = tabTitle,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textCol
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(16.dp)
                                                    .height(2.dp)
                                                    .background(Color(0xFFE2B266))
                                            )
                                        }
                                    }
                                }
                            }

                            // Anchored selected bar details
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xF911111E))
                                    .border(1.dp, Color(0xFF1E1E30), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "SELECTED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE2B266),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = activeName,
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                                
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
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE2B266),
                                        disabledContainerColor = Color(0xFF1E1E30)
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text(
                                        text = if (isDownloadingActive) "Downloading $downloadProgress%" else "Set as wallpaper",
                                        color = if (isDownloadingActive) Color.Gray else Color.Black,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                AppScreen.Browse -> {
                    // GALLERY BROWSE SCREEN
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(bottom = 60.dp)
                    ) {
                        // Title header and search action row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSearchExpanded) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search wallpapers...", color = Color.Gray, fontSize = 14.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFE2B266),
                                        unfocusedBorderColor = Color(0xFF1E1E30),
                                        focusedContainerColor = Color(0xFF11111E),
                                        unfocusedContainerColor = Color(0xFF11111E),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { 
                                            searchQuery = ""
                                            isSearchExpanded = false 
                                        }) {
                                            Text("✕", color = Color.Gray, fontSize = 12.sp)
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                                )
                            } else {
                                val currentCatTitle = if (activeTab == 0) activeStaticCategory else activeLiveCategory
                                Text(
                                    text = currentCatTitle,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF11111E))
                                        .clickable { isSearchExpanded = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Category tabs selection row (underlined)
                        val categoriesList = remember(activeTab) {
                            if (activeTab == 0) {
                                listOf("All", "Abstract", "Animal", "Anime", "Car", "Cartoon", "Minimalist", "Nature", "Sport", "Spiritual")
                            } else {
                                listOf("All", "Car", "Nature", "Abstract")
                            }
                        }
                        
                        val selectedCategory = if (activeTab == 0) activeStaticCategory else activeLiveCategory

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            categoriesList.forEach { cat ->
                                val isSelected = cat == selectedCategory
                                val textCol by animateColorAsState(if (isSelected) Color.White else Color.Gray)
                                Column(
                                    modifier = Modifier.clickable {
                                        if (activeTab == 0) {
                                            viewModel.activeStaticCategory.value = cat
                                        } else {
                                            viewModel.activeLiveCategory.value = cat
                                        }
                                    },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textCol
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(12.dp)
                                                .height(2.dp)
                                                .background(Color(0xFFE2B266))
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // True Masonry staggered grid list
                        val filteredList = remember(activeTab, selectedCategory, searchQuery, viewModel.staticWallpaperCatalog.size, viewModel.liveWallpaperCatalog.size) {
                            if (activeTab == 0) {
                                val base = if (selectedCategory == "All") viewModel.staticWallpaperCatalog else viewModel.staticWallpaperCatalog.filter { it.category == selectedCategory }
                                if (searchQuery.isEmpty()) base else base.filter { it.name.contains(searchQuery, ignoreCase = true) }
                            } else {
                                val base = if (selectedCategory == "All") viewModel.liveWallpaperCatalog else viewModel.liveWallpaperCatalog.filter { it.category == selectedCategory }
                                if (searchQuery.isEmpty()) base else base.filter { it.title.contains(searchQuery, ignoreCase = true) }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (filteredList.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No wallpapers found.", color = Color.Gray)
                                }
                            } else {
                                LazyVerticalStaggeredGrid(
                                    columns = StaggeredGridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 18.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalItemSpacing = 12.dp
                                ) {
                                    items(filteredList) { wp ->
                                        // Staggered heights aspect logic
                                        val idx = remember(wp) { wp.hashCode() }
                                        val cardAspectRatio = remember(idx) {
                                            if (idx % 3 == 0) 9f / 16f else if (idx % 3 == 1) 3f / 4f else 1f / 1f
                                        }

                                        val wpId = remember(wp) { if (wp is StaticWallpaper) wp.id else (wp as LiveWallpaper).id }
                                        val wpName = remember(wp) { if (wp is StaticWallpaper) wp.name else (wp as LiveWallpaper).title }
                                        val wpCat = remember(wp) { if (wp is StaticWallpaper) wp.category else (wp as LiveWallpaper).category }
                                        val wpUrl = remember(wp) { if (wp is StaticWallpaper) wp.url else (wp as LiveWallpaper).imageUrl ?: "" }
                                        
                                        val isCurrentSelected = remember(selectedType, videoUriStr, wp) {
                                            if (wp is StaticWallpaper) {
                                                selectedType == "Static" && videoUriStr == wp.url
                                            } else {
                                                val liveWp = wp as LiveWallpaper
                                                selectedType == "Video" && (
                                                    videoUriStr == liveWp.source ||
                                                    videoUriStr == liveWp.id ||
                                                    videoUriStr.endsWith("${liveWp.id}.mp4")
                                                )
                                            }
                                        }

                                        val cardBorderColor by animateColorAsState(if (isCurrentSelected) Color(0xFFE2B266) else Color.Transparent)

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                                .clickable {
                                                    if (wp is StaticWallpaper) {
                                                        viewModel.selectedType.value = "Static"
                                                        viewModel.videoUriStr.value = wp.url
                                                    } else {
                                                        val liveWp = wp as LiveWallpaper
                                                        if (WallpaperRepository.isWallpaperCached(context, liveWp.id)) {
                                                            val file = WallpaperRepository.getCachedWallpaperFile(context, liveWp.id)
                                                            viewModel.selectedType.value = "Video"
                                                            viewModel.videoUriStr.value = file.absolutePath
                                                        } else {
                                                            viewModel.startDownload(context, liveWp)
                                                        }
                                                    }
                                                },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF11111E))
                                        ) {
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                // Thumbnail cover loader
                                                SubcomposeAsyncImage(
                                                    model = wpUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(cardAspectRatio),
                                                    contentScale = ContentScale.Crop
                                                ) {
                                                    val state = painter.state
                                                    if (state is AsyncImagePainter.State.Loading) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color(0xFF11111E)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            CircularProgressIndicator(
                                                                color = Color(0xFFE2B266).copy(alpha = 0.3f),
                                                                strokeWidth = 2.dp,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    } else {
                                                        SubcomposeAsyncImageContent()
                                                    }
                                                }

                                                // Top right heart icon
                                                val isFavorite = favorites[wpId] == true
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(8.dp)
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.4f))
                                                        .clickable { favorites[wpId] = !isFavorite },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                        contentDescription = "Favorite",
                                                        tint = if (isFavorite) Color.Red else Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }

                                                // Gradient detail protection inside card bottom
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .align(Alignment.BottomCenter)
                                                        .aspectRatio(cardAspectRatio)
                                                        .background(
                                                            Brush.verticalGradient(
                                                                colors = listOf(
                                                                    Color.Transparent,
                                                                    Color.Black.copy(alpha = 0.7f)
                                                                ),
                                                                startY = 150f
                                                            )
                                                        )
                                                )

                                                // Card bottom labels overlay
                                                Column(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(12.dp)
                                                ) {
                                                    Text(
                                                        text = wpName,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = wpCat,
                                                        fontSize = 9.sp,
                                                        color = Color.LightGray,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Anchored bottom selected band details
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color(0xF907070F))
                                    .border(1.dp, Color(0xFF11111E))
                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "SELECTED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        letterSpacing = 1.sp
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
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE2B266),
                                        disabledContainerColor = Color(0xFF1E1E30)
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text(
                                        text = if (isDownloadingActive) "Downloading..." else "Set",
                                        color = if (isDownloadingActive) Color.Gray else Color.Black,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SLEEK BOTTOM NAVIGATION BAR
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(60.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF90B0B14)),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            border = BorderStroke(1.dp, Color(0xFF1E1E30).copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Preview (Home)
                Column(
                    modifier = Modifier
                        .clickable { activeAppScreen = AppScreen.Preview }
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val previewIconColor by animateColorAsState(if (activeAppScreen == AppScreen.Preview) Color(0xFFE2B266) else Color.Gray)
                    Text(
                        text = "PREVIEW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = previewIconColor,
                        letterSpacing = 1.sp
                    )
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color(0xFF1E1E30))
                )

                // Tab 2: Browse (Gallery)
                Column(
                    modifier = Modifier
                        .clickable { activeAppScreen = AppScreen.Browse }
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val browseIconColor by animateColorAsState(if (activeAppScreen == AppScreen.Browse) Color(0xFFE2B266) else Color.Gray)
                    Text(
                        text = "BROWSE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = browseIconColor,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // GOOGLE ADMOB REWARDED AD DIALOG POPUP
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2B266))
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
