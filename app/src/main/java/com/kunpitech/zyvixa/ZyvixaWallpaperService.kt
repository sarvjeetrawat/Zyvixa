package com.kunpitech.zyvixa

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceHolder
import java.io.File

class ZyvixaWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return LiveWallpaperEngine()
    }

    private inner class LiveWallpaperEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private var renderer: WallpaperRenderer = PlexusRenderer()
        private var isVisible = false
        private var hasSurface = false
        private var width = 0
        private var height = 0
        private var currentType = "Plexus"
        private var currentVideoUri: String? = null

        private var mediaPlayer: MediaPlayer? = null
        private var isPrepared = false
        private var currentVideoLoop = true
        private lateinit var prefs: SharedPreferences

        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (isVisible && hasSurface && currentType != "Video") {
                    drawFrame()
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            prefs = getSharedPreferences("zyvixa_settings", Context.MODE_PRIVATE)
            prefs.registerOnSharedPreferenceChangeListener(this)
            
            // Initial load
            loadSettings()
            
            // Enable touch events
            setTouchEventsEnabled(true)
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            isVisible = false
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            releaseMediaPlayer()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.isVisible = visible
            if (currentType == "Video") {
                if (visible) {
                    if (isPrepared) {
                        mediaPlayer?.start()
                    }
                } else {
                    if (isPrepared) {
                        mediaPlayer?.pause()
                    }
                }
            } else {
                if (visible) {
                    Choreographer.getInstance().postFrameCallback(frameCallback)
                } else {
                    Choreographer.getInstance().removeFrameCallback(frameCallback)
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            hasSurface = true
            if (currentType == "Video") {
                initMediaPlayer()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            width = w
            height = h
            if (currentType != "Video") {
                renderer.setup(w, h)
                drawFrame()
            } else {
                mediaPlayer?.setSurface(holder.surface)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            hasSurface = false
            isVisible = false
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            releaseMediaPlayer()
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            if (currentType != "Video") {
                renderer.onTouchEvent(event)
            }
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            if (currentType != "Video") {
                renderer.onOffsetsChanged(xOffset, yOffset)
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key != null) {
                loadSettings()
            }
        }

        private fun initMediaPlayer() {
            releaseMediaPlayer()
            var uriString = prefs.getString("wp_video_uri", null)
            if (uriString.isNullOrEmpty()) {
                uriString = "default_video"
            }
            
            // Fix legacy ID shifts on recompiles
            if (uriString.startsWith("android.resource://") && !uriString.contains("/raw/")) {
                uriString = "default_video"
            }
            currentVideoUri = uriString
            val videoLoop = prefs.getBoolean("wp_video_loop", true)

            if (!hasSurface) {
                return
            }

            try {
                mediaPlayer = MediaPlayer().apply {
                    if (uriString.startsWith("content://") || uriString.startsWith("file://") || uriString.startsWith("/")) {
                        val uri = if (uriString.startsWith("/")) Uri.fromFile(File(uriString)) else Uri.parse(uriString)
                        setDataSource(applicationContext, uri)
                    } else {
                        val uri = Uri.parse(uriString)
                        val lastSegment = uri.lastPathSegment ?: uriString
                        val resId = if (lastSegment.toIntOrNull() != null) {
                            lastSegment.toInt()
                        } else {
                            resources.getIdentifier(lastSegment, "raw", packageName)
                        }

                        if (resId != 0) {
                            val afd = resources.openRawResourceFd(resId)
                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            afd.close()
                        } else {
                            val afd = resources.openRawResourceFd(R.raw.default_video)
                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            afd.close()
                        }
                    }
                    
                    setSurface(surfaceHolder.surface)
                    isLooping = videoLoop
                    setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    
                    setOnPreparedListener { mp ->
                        isPrepared = true
                        if (isVisible) {
                            mp.start()
                        }
                    }
                    
                    setOnErrorListener { _, _, _ ->
                        releaseMediaPlayer()
                        true
                    }
                    
                    prepareAsync()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    // Fail-safe robust fallback to default video resource
                    mediaPlayer = MediaPlayer().apply {
                        val afd = resources.openRawResourceFd(R.raw.default_video)
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                        setSurface(surfaceHolder.surface)
                        isLooping = videoLoop
                        setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                        setOnPreparedListener { mp ->
                            isPrepared = true
                            if (isVisible) {
                                mp.start()
                            }
                        }
                        setOnErrorListener { _, _, _ ->
                            releaseMediaPlayer()
                            true
                        }
                        prepareAsync()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    releaseMediaPlayer()
                }
            }
        }

        private fun releaseMediaPlayer() {
            isPrepared = false
            mediaPlayer?.let {
                try {
                    it.stop()
                } catch (_: Exception) {}
                try {
                    it.release()
                } catch (_: Exception) {}
            }
            mediaPlayer = null
        }

        private fun loadSettings() {
            val type = prefs.getString("wp_type", "Plexus") ?: "Plexus"
            val speed = prefs.getFloat("wp_speed", 1.0f)
            val theme = prefs.getString("wp_theme", "Ocean Breeze") ?: "Ocean Breeze"
            val touchEnabled = prefs.getBoolean("wp_touch", true)
            var videoUri = prefs.getString("wp_video_uri", null)
            if (videoUri.isNullOrEmpty()) {
                videoUri = "default_video"
            }
            val videoLoop = prefs.getBoolean("wp_video_loop", true)

            val typeChanged = type != currentType
            val videoUriChanged = videoUri != currentVideoUri
            val videoLoopChanged = videoLoop != currentVideoLoop
            
            currentType = type
            currentVideoUri = videoUri
            currentVideoLoop = videoLoop

            if (type == "Video") {
                // Remove frame callback for custom canvas rendering
                Choreographer.getInstance().removeFrameCallback(frameCallback)
                
                if (typeChanged || videoUriChanged) {
                    initMediaPlayer()
                } else if (videoLoopChanged) {
                    mediaPlayer?.isLooping = videoLoop
                } else {
                    // Resume playback if visible
                    if (isVisible && isPrepared) {
                        mediaPlayer?.start()
                    }
                }
            } else {
                // If switching away from video, release it
                if (typeChanged) {
                    releaseMediaPlayer()
                }

                val currentRendererType = when (renderer) {
                    is PlexusRenderer -> "Plexus"
                    is MatrixRenderer -> "Matrix"
                    is AuraFlowRenderer -> "Aura Flow"
                    else -> "Plexus"
                }

                if (type != currentRendererType) {
                    renderer = when (type) {
                        "Matrix" -> MatrixRenderer()
                        "Aura Flow" -> AuraFlowRenderer()
                        else -> PlexusRenderer()
                    }
                    if (width > 0 && height > 0) {
                        renderer.setup(width, height)
                    }
                }

                renderer.update(speed, theme, touchEnabled)

                if (isVisible && hasSurface) {
                    Choreographer.getInstance().postFrameCallback(frameCallback)
                }
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null
            try {
                canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }

                if (canvas != null) {
                    renderer.draw(canvas)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
