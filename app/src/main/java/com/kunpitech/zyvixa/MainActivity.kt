package com.kunpitech.zyvixa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kunpitech.zyvixa.ui.screens.DashboardScreen
import com.kunpitech.zyvixa.ui.screens.SplashScreen
import com.kunpitech.zyvixa.ui.theme.ZyvixaTheme
import com.kunpitech.zyvixa.viewmodel.WallpaperViewModel

import com.kunpitech.zyvixa.ads.AdMobManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdMobManager.initialize(this)
        setContent {
            ZyvixaTheme {
                val context = this
                val viewModel = remember { WallpaperViewModel(context) }
                var showSplash by remember { mutableStateOf(true) }

                Crossfade(targetState = showSplash, label = "splash_fade") { isSplash ->
                    if (isSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color(0xFF07070F)
                        ) { innerPadding ->
                            DashboardScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}