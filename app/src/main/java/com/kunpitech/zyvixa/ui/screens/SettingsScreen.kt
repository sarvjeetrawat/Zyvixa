package com.kunpitech.zyvixa.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunpitech.zyvixa.repository.WallpaperRepository
import com.kunpitech.zyvixa.viewmodel.WallpaperViewModel

@Composable
fun SettingsScreen(
    viewModel: WallpaperViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val videoLoop by viewModel.videoLoop.collectAsState()
    val animSpeed by viewModel.animSpeed.collectAsState()
    val touchEnabled by viewModel.touchEnabled.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07070F))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 60.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF11111E))
                    .border(1.dp, Color(0xFF1E1E30), CircleShape)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Settings",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Wallpaper Settings
            item { SectionLabel("Wallpaper Settings") }
            item {
                SettingsCard {
                    ToggleRow(
                        title = "Video Loop",
                        subtitle = "Repeat live wallpaper continuously",
                        checked = videoLoop,
                        onCheckedChange = { viewModel.videoLoop.value = it }
                    )
                    SettingsDivider()
                    ToggleRow(
                        title = "Touch Interaction",
                        subtitle = "Enable interactive effects on touch",
                        checked = touchEnabled,
                        onCheckedChange = { viewModel.touchEnabled.value = it }
                    )
                    SettingsDivider()
                    // Animation Speed Slider
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Animation Speed", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                Text("Controls live wallpaper animation rate", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                            }
                            Text(
                                text = String.format("%.1fx", animSpeed),
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE2B266)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = animSpeed,
                            onValueChange = { viewModel.animSpeed.value = it },
                            valueRange = 0.5f..3.0f,
                            steps = 9,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFE2B266),
                                activeTrackColor = Color(0xFFE2B266),
                                inactiveTrackColor = Color(0xFF1E1E30),
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent
                            )
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("0.5x", fontSize = 11.sp, color = Color.Gray)
                            Text("3.0x", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Cache & Storage
            item { SectionLabel("Cache & Storage") }
            item {
                SettingsCard {
                    ActionRow(
                        title = "Clear Wallpaper Cache",
                        subtitle = "Delete all downloaded live wallpapers",
                        isDestructive = true,
                        onClick = {
                            val freed = WallpaperRepository.clearCache(context)
                            val mb = freed / (1024 * 1024)
                            Toast.makeText(context,
                                if (mb > 0) "Cleared ${mb}MB of cached videos" else "Cache is already empty",
                                Toast.LENGTH_SHORT).show()
                        }
                    )
                    SettingsDivider()
                    ActionRow(
                        title = "Refresh Catalog",
                        subtitle = "Fetch latest wallpapers from server",
                        onClick = {
                            viewModel.fetchCatalogs()
                            Toast.makeText(context, "Refreshing catalog…", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // About
            item { SectionLabel("About") }
            item {
                SettingsCard {
                    InfoRow(title = "App Version", value = "1.0.0")
                    SettingsDivider()
                    InfoRow(title = "Developer", value = "Kunpi Tech")
                    SettingsDivider()
                    ActionRow(
                        title = "Rate this App",
                        subtitle = "Support us with a 5-star review",
                        showChevron = true,
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.kunpitech.zyvixa")))
                            } catch (e: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.kunpitech.zyvixa")))
                            }
                        }
                    )
                    SettingsDivider()
                    ActionRow(
                        title = "Share App",
                        subtitle = "Invite friends to try Zyvixa",
                        showChevron = true,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Zyvixa — Premium Wallpapers")
                                putExtra(Intent.EXTRA_TEXT,
                                    "Check out Zyvixa - Premium Wallpapers Hub!\nhttps://play.google.com/store/apps/details?id=com.kunpitech.zyvixa")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Zyvixa"))
                        }
                    )
                    SettingsDivider()
                    ActionRow(
                        title = "Privacy Policy",
                        subtitle = "Read our privacy policy",
                        showChevron = true,
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/document/d/1kX_5o5xIHnCvwo7K59MjhaGvz6rcaGu1w_qnE3aGe9Y/edit?usp=sharing")))
                        }
                    )
                }
            }

            // Footer
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Zyvixa", fontFamily = FontFamily.Serif, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.3f))
                        Box(modifier = Modifier.padding(start = 2.dp, top = 6.dp).size(4.dp).clip(CircleShape).background(Color(0xFFE2B266).copy(alpha = 0.3f)))
                    }
                    Text("Premium Wallpapers Hub", fontSize = 11.sp, color = Color.Gray.copy(alpha = 0.4f), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

// ── Reusable Components ──────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFFE2B266),
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF11111E))
            .border(1.dp, Color(0xFF1E1E30), RoundedCornerShape(16.dp)),
        content = content
    )
}

@Composable
private fun SettingsDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1E1E30)))
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF07070F),
                checkedTrackColor = Color(0xFFE2B266),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF1E1E30),
                uncheckedBorderColor = Color(0xFF1E1E30)
            )
        )
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
        Text(value, fontSize = 13.sp, color = Color.Gray)
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String? = null,
    isDestructive: Boolean = false,
    showChevron: Boolean = false,
    onClick: () -> Unit
) {
    val titleColor by animateColorAsState(
        if (isDestructive) Color(0xFFFF6B6B) else Color.White,
        animationSpec = tween(200), label = "title_color"
    )
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = titleColor)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
        }
        if (showChevron) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}
