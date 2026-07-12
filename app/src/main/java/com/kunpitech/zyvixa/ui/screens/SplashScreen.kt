package com.kunpitech.zyvixa.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

enum class SplashPhase {
    ColdStart,
    Handoff,
    Exit
}

@Composable
fun SplashScreen(
    firstWallpaperUrl: String,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPhase by remember { mutableStateOf(SplashPhase.ColdStart) }

    // Phase control sequence
    LaunchedEffect(Unit) {
        // Cold start phase (Z logo, Zyvixa branding and dots indicator)
        delay(1800)
        // Transition to Handoff phase (dissolves first wallpaper behind the text)
        currentPhase = SplashPhase.Handoff
        // Handoff duration for wallpaper image load
        delay(1600)
        // Transition to exit phase (everything fades out to reveal the dashboard)
        currentPhase = SplashPhase.Exit
        // Final fadeout delay before callback
        delay(500)
        onFinished()
    }

    // Resolve wallpaper preview image URL
    val resolvedImageUrl = remember(firstWallpaperUrl) {
        if (firstWallpaperUrl.startsWith("http") && 
            (firstWallpaperUrl.contains(".png") || firstWallpaperUrl.contains(".jpg") || firstWallpaperUrl.contains(".jpeg"))
        ) {
            firstWallpaperUrl
        } else {
            // Live wallpaper: Map to its static preview thumbnail URL
            val name = firstWallpaperUrl.substringAfterLast("/").replace(".mp4", "")
            when {
                name == "live_wallpaper_one" -> "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/live-wallpaper/live_wallpaper_one.png"
                name.contains("default_video") -> "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/live-wallpaper/default_video.png"
                name.startsWith("wallpaper_") -> "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/live-wallpaper/$name.png"
                else -> "https://raw.githubusercontent.com/sarvjeetrawat/Zyvixa/main/Assets/live-wallpaper/default_video.png"
            }
        }
    }

    // Animated float parameters
    val wallpaperAlpha by animateFloatAsState(
        targetValue = if (currentPhase != SplashPhase.ColdStart) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "wallpaper_fade"
    )

    val logoMarkAlpha by animateFloatAsState(
        targetValue = if (currentPhase == SplashPhase.ColdStart) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "logo_fade"
    )

    val contentsAlpha by animateFloatAsState(
        targetValue = if (currentPhase != SplashPhase.Exit) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "contents_fade"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07070F))
    ) {
        // Base amber corner glow + deep dark radial gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x28E2B266), Color.Transparent),
                        center = Offset(800f, 100f),
                        radius = 1200f
                    )
                )
        )

        // Wallpaper photo overlay fades in behind components in Phase 2 (Handoff)
        AsyncImage(
            model = resolvedImageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(wallpaperAlpha),
            contentScale = ContentScale.Crop
        )

        // Dark dim overlay above the wallpaper for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        // Core Brand Components Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(contentsAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App-Icon Bordered "Z" Logo Box (Fades out during handoff)
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .alpha(logoMarkAlpha)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF11111E).copy(alpha = 0.8f))
                    .border(BorderStroke(1.2.dp, Color(0xFFE2B266).copy(alpha = 0.8f)), RoundedCornerShape(26.dp))
                    .padding(4.dp)
                    .border(BorderStroke(1.dp, Color(0xFFE2B266).copy(alpha = 0.15f)), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Z",
                    fontFamily = FontFamily.Serif,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE2B266)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Serif Wordmark: "Zyvixa."
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Zyvixa",
                    fontFamily = FontFamily.Serif,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp, top = 14.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2B266))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Crossfading Subtitle Text
            Crossfade(
                targetState = currentPhase,
                animationSpec = tween(durationMillis = 800),
                label = "subtitle_crossfade"
            ) { phase ->
                val subtitle = if (phase == SplashPhase.ColdStart) {
                    "Premium wallpapers hub"
                } else {
                    "CURATING TODAY'S PICKS"
                }
                Text(
                    text = subtitle,
                    fontSize = if (phase == SplashPhase.ColdStart) 14.sp else 11.sp,
                    fontWeight = if (phase == SplashPhase.ColdStart) FontWeight.Normal else FontWeight.Bold,
                    color = if (phase == SplashPhase.ColdStart) Color.Gray else Color(0xFFE2B266),
                    letterSpacing = if (phase == SplashPhase.ColdStart) 0.5.sp else 2.sp
                )
            }
        }

        // Bottom Cold-start dots indicator (fades out during handoff)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 60.dp)
                .alpha(logoMarkAlpha),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFE2B266)))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.5f)))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.5f)))
            }
        }
    }
}
