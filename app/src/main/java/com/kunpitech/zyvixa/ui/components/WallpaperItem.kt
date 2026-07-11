package com.kunpitech.zyvixa.ui.components

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import com.kunpitech.zyvixa.model.LiveWallpaper
import com.kunpitech.zyvixa.model.StaticWallpaper
import com.kunpitech.zyvixa.repository.WallpaperRepository
import java.io.File

@Composable
fun StaticWallpaperItem(
    wp: StaticWallpaper,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBorderColor by animateColorAsState(if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E1E30))
    val cardBgColor by animateColorAsState(if (isSelected) Color(0xFF0F1E2E) else Color(0xFF0F0F1A))

    Card(
        modifier = modifier
            .padding(vertical = 6.dp)
            .clickable { onClick() },
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
                SubcomposeAsyncImage(
                    model = wp.url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                ) {
                    val state = painter.state
                    if (state is AsyncImagePainter.State.Loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF151525)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        SubcomposeAsyncImageContent()
                    }
                }
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
                text = wp.category,
                fontSize = 9.sp,
                color = Color.Gray,
                maxLines = 1
            )
        }
    }
}

@Composable
fun LiveWallpaperItem(
    context: Context,
    wp: LiveWallpaper,
    isSelected: Boolean,
    isDownloadingActive: Boolean,
    downloadProgress: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderCol by animateColorAsState(if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E1E30))
    val bgCol by animateColorAsState(if (isSelected) Color(0xFF0F1E2E) else Color(0xFF0F0F1A))

    Card(
        modifier = modifier
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgCol),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderCol)
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
                // Background Cover Image loader
                val coverUrl = remember(wp.imageUrl, wp.id) {
                    wp.imageUrl ?: "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/live-wallpaper/${wp.id}.png"
                }

                SubcomposeAsyncImage(
                    model = "$coverUrl?t=${System.currentTimeMillis()}",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                ) {
                    val state = painter.state
                    if (state is AsyncImagePainter.State.Loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF151525)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        SubcomposeAsyncImageContent()
                    }
                }

                // Inline Looping Video Player for Selected items (only if cached)
                val isCached = remember(wp.id, isDownloadingActive) {
                    WallpaperRepository.isWallpaperCached(context, wp.id)
                }

                if (isSelected && isCached && !isDownloadingActive) {
                    val localFile = remember(wp.id) {
                        WallpaperRepository.getCachedWallpaperFile(context, wp.id)
                    }

                    var itemPlayer: MediaPlayer? by remember { mutableStateOf(null) }

                    DisposableEffect(wp.id) {
                        onDispose {
                            itemPlayer?.release()
                            itemPlayer = null
                        }
                    }

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, w: Int, h: Int) {
                                        val surface = Surface(surfaceTexture)
                                        try {
                                            itemPlayer?.release()
                                            itemPlayer = MediaPlayer().apply {
                                                setDataSource(ctx, Uri.fromFile(localFile))
                                                setSurface(surface)
                                                isLooping = true
                                                setVolume(0f, 0f)
                                                setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                                setOnPreparedListener { start() }
                                                prepareAsync()
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                        itemPlayer?.release()
                                        itemPlayer = null
                                        st.release()
                                        return true
                                    }
                                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                                }
                            }
                        }
                    )
                }

                // Downloading progress indicator
                if (isDownloadingActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { downloadProgress / 100f },
                            color = Color(0xFF00E5FF),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "$downloadProgress%",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = wp.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = wp.description,
                fontSize = 9.sp,
                color = Color.Gray,
                maxLines = 1
            )
        }
    }
}
