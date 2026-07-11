package com.kunpitech.zyvixa.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.kunpitech.zyvixa.R
import java.io.File

@Composable
fun LiveSimulatedPreview(
    type: String,
    videoUriStr: String?,
    videoLoop: Boolean,
    modifier: Modifier = Modifier
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
                modifier = modifier.fillMaxSize(),
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
            modifier = modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F1A))
        )
    }
}
