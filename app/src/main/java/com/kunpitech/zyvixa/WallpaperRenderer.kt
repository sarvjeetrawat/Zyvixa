package com.kunpitech.zyvixa

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.MotionEvent
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

interface WallpaperRenderer {
    fun setup(width: Int, height: Int)
    fun draw(canvas: Canvas)
    fun update(speedMultiplier: Float, colorTheme: String, touchEnabled: Boolean)
    fun onTouchEvent(event: MotionEvent)
    fun onOffsetsChanged(xOffset: Float, yOffset: Float)
}

// -------------------------------------------------------------
// PLEXUS / PARTICLE RENDERER
// -------------------------------------------------------------
class PlexusRenderer : WallpaperRenderer {
    private var width = 0
    private var height = 0
    private var particles = mutableListOf<Particle>()
    private var speed = 1.0f
    private var theme = "Ocean Breeze"
    private var isTouchEnabled = true

    private var touchX = -1f
    private var touchY = -1f
    private var isTouching = false

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var radius: Float,
        var color: Int
    )

    override fun setup(width: Int, height: Int) {
        this.width = width
        this.height = height
        particles.clear()
        val random = Random(System.currentTimeMillis())
        val count = 65
        for (i in 0 until count) {
            particles.add(
                Particle(
                    x = random.nextFloat() * width,
                    y = random.nextFloat() * height,
                    vx = (random.nextFloat() - 0.5f) * 3f,
                    vy = (random.nextFloat() - 0.5f) * 3f,
                    radius = random.nextFloat() * 4f + 3f,
                    color = Color.WHITE
                )
            )
        }
    }

    override fun update(speedMultiplier: Float, colorTheme: String, touchEnabled: Boolean) {
        speed = speedMultiplier
        theme = colorTheme
        isTouchEnabled = touchEnabled
    }

    override fun draw(canvas: Canvas) {
        // Clear screen with a rich dark blue/black background
        canvas.drawColor(Color.parseColor("#080812"))

        // Determine colors based on theme
        val (particleColorHex, lineColorHex) = when (theme) {
            "Cosmic Neon" -> Pair("#E040FB", "#A020F0") // Pink / Violet
            "Ocean Breeze" -> Pair("#00E5FF", "#00B0FF") // Cyan / Blue
            "Forest Fire" -> Pair("#FF6D00", "#D50000") // Orange / Red
            else -> Pair("#00E5FF", "#00B0FF") // Default to Ocean Breeze
        }
        val pColor = Color.parseColor(particleColorHex)
        val lColor = Color.parseColor(lineColorHex)

        val size = particles.size
        // 1. Update particles physics
        for (i in 0 until size) {
            val p = particles[i]
            
            // Move particles
            p.x += p.vx * speed
            p.y += p.vy * speed

            // Pull towards touch if enabled and touching
            if (isTouchEnabled && isTouching && touchX >= 0 && touchY >= 0) {
                val dx = touchX - p.x
                val dy = touchY - p.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < 350f) {
                    // Attraction force
                    val force = (350f - dist) / 350f * 0.15f
                    p.x += dx * force
                    p.y += dy * force
                }
            }

            // Screen boundary check
            if (p.x < 0) { p.x = 0f; p.vx = -p.vx }
            else if (p.x > width) { p.x = width.toFloat(); p.vx = -p.vx }

            if (p.y < 0) { p.y = 0f; p.vy = -p.vy }
            else if (p.y > height) { p.y = height.toFloat(); p.vy = -p.vy }
        }

        // 2. Draw lines between close particles
        paint.strokeWidth = 1.5f
        for (i in 0 until size) {
            val p1 = particles[i]
            for (j in i + 1 until size) {
                val p2 = particles[j]
                val dx = p1.x - p2.x
                val dy = p1.y - p2.y
                val dist = sqrt(dx * dx + dy * dy)
                val maxDist = 180f

                if (dist < maxDist) {
                    val alpha = (((maxDist - dist) / maxDist) * 160).toInt()
                    paint.color = Color.argb(alpha, Color.red(lColor), Color.green(lColor), Color.blue(lColor))
                    paint.style = Paint.Style.STROKE
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
                }
            }
        }

        // 3. Draw particles
        paint.style = Paint.Style.FILL
        for (p in particles) {
            // Draw outer glow
            paint.color = Color.argb(40, Color.red(pColor), Color.green(pColor), Color.blue(pColor))
            canvas.drawCircle(p.x, p.y, p.radius * 2f, paint)

            // Draw core
            paint.color = pColor
            canvas.drawCircle(p.x, p.y, p.radius, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y
                isTouching = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouching = false
            }
        }
    }

    override fun onOffsetsChanged(xOffset: Float, yOffset: Float) {
        // Shift velocity offset slightly on swipe
        val shift = xOffset * 10f
        for (p in particles) {
            p.x += (Random.nextFloat() - 0.5f) * shift
        }
    }
}

// -------------------------------------------------------------
// MATRIX DIGITAL RAIN RENDERER
// -------------------------------------------------------------
class MatrixRenderer : WallpaperRenderer {
    private var width = 0
    private var height = 0
    private var columns = 0
    private var fontPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val charSize = 42f
    private lateinit var drops: FloatArray
    private lateinit var columnSpeeds: FloatArray
    private lateinit var streamChars: Array<CharArray>

    private var speed = 1.0f
    private var theme = "Ocean Breeze"
    private var isTouchEnabled = true

    private var rippleX = -1f
    private var rippleY = -1f
    private var rippleRadius = 0f
    private var rippleActive = false

    private val matrixChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZアイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン".toCharArray()

    override fun setup(width: Int, height: Int) {
        this.width = width
        this.height = height
        columns = (width / charSize).toInt() + 1
        drops = FloatArray(columns)
        columnSpeeds = FloatArray(columns)
        streamChars = Array(columns) { CharArray(20) }

        val random = Random(System.currentTimeMillis())
        for (i in 0 until columns) {
            drops[i] = random.nextFloat() * -height
            columnSpeeds[i] = random.nextFloat() * 12f + 10f
            
            for (j in 0 until 20) {
                streamChars[i][j] = matrixChars[random.nextInt(matrixChars.size)]
            }
        }
    }

    override fun update(speedMultiplier: Float, colorTheme: String, touchEnabled: Boolean) {
        speed = speedMultiplier
        theme = colorTheme
        isTouchEnabled = touchEnabled
    }

    override fun draw(canvas: Canvas) {
        // Render rich black back-buffer
        canvas.drawColor(Color.parseColor("#020202"))

        // Ripple expansion
        if (rippleActive) {
            rippleRadius += 25f * speed
            if (rippleRadius > width && rippleRadius > height) {
                rippleActive = false
            }
        }

        // Determine colors based on theme
        val (headColorHex, bodyColorHex) = when (theme) {
            "Cosmic Neon" -> Pair("#FFFFFF", "#E040FB") // Purple rain
            "Ocean Breeze" -> Pair("#FFFFFF", "#00E5FF") // Blue rain
            "Forest Fire" -> Pair("#FFFFFF", "#FF6D00") // Red/Orange rain
            else -> Pair("#FFFFFF", "#00FF00") // Classic Matrix Green
        }
        val headColor = Color.parseColor(headColorHex)
        val bodyColor = Color.parseColor(bodyColorHex)

        fontPaint.textSize = charSize

        val random = Random(System.currentTimeMillis())
        for (i in 0 until columns) {
            val x = i * charSize
            val y = drops[i]

            // Draw character streams trailing upwards
            val trailLength = 15
            for (j in 0 until trailLength) {
                val charY = y - j * charSize
                if (charY < 0 || charY > height + charSize) continue

                // Check touch ripple impact
                var drawColor = bodyColor
                var displayChar = streamChars[i][j % 20]

                if (isTouchEnabled && rippleActive) {
                    val dx = x - rippleX
                    val dy = charY - rippleY
                    val dist = sqrt(dx * dx + dy * dy)
                    if (Math.abs(dist - rippleRadius) < 80f) {
                        // Glitch effect on the ripple wave
                        drawColor = Color.WHITE
                        displayChar = '*'
                    }
                }

                // Smooth transparency trail
                val alphaPercent = (1f - (j.toFloat() / trailLength.toFloat()))
                val alpha = (alphaPercent * 255).toInt().coerceIn(0, 255)

                if (j == 0) {
                    fontPaint.color = headColor
                    fontPaint.setShadowLayer(10f, 0f, 0f, headColor)
                } else {
                    fontPaint.color = Color.argb(alpha, Color.red(drawColor), Color.green(drawColor), Color.blue(drawColor))
                    fontPaint.setShadowLayer(4f, 0f, 0f, drawColor)
                }

                canvas.drawText(displayChar.toString(), x, charY, fontPaint)
            }

            // Remove shadow layer for other drawing operations
            fontPaint.clearShadowLayer()

            // Update drop positions
            drops[i] += columnSpeeds[i] * speed
            if (drops[i] > height + charSize * trailLength) {
                drops[i] = -charSize
                columnSpeeds[i] = random.nextFloat() * 12f + 10f
            }

            // Randomly flip characters in streams for active look
            if (random.nextFloat() < 0.05f) {
                streamChars[i][random.nextInt(20)] = matrixChars[random.nextInt(matrixChars.size)]
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN) {
            rippleX = event.x
            rippleY = event.y
            rippleRadius = 0f
            rippleActive = true
        }
    }

    override fun onOffsetsChanged(xOffset: Float, yOffset: Float) {
        // Swipe shifts drops slightly
        val shift = xOffset * charSize * 3f
        for (i in 0 until columns) {
            drops[i] += (Random.nextFloat() - 0.5f) * shift
        }
    }
}

// -------------------------------------------------------------
// AURA FLOW / GRADIENT RENDERER
// -------------------------------------------------------------
class AuraFlowRenderer : WallpaperRenderer {
    private var width = 0
    private var height = 0
    private var time = 0.0f
    
    private var speed = 1.0f
    private var theme = "Ocean Breeze"
    private var isTouchEnabled = true

    private var targetTouchX = -1f
    private var targetTouchY = -1f
    private var currentTouchX = -1f
    private var currentTouchY = -1f

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    override fun setup(width: Int, height: Int) {
        this.width = width
        this.height = height
        currentTouchX = width / 2f
        currentTouchY = height / 2f
        targetTouchX = width / 2f
        targetTouchY = height / 2f
    }

    override fun update(speedMultiplier: Float, colorTheme: String, touchEnabled: Boolean) {
        speed = speedMultiplier
        theme = colorTheme
        isTouchEnabled = touchEnabled
    }

    override fun draw(canvas: Canvas) {
        time += 0.012f * speed

        // Intercept/interpolate touch points smoothly
        if (isTouchEnabled && targetTouchX >= 0) {
            currentTouchX += (targetTouchX - currentTouchX) * 0.1f
            currentTouchY += (targetTouchY - currentTouchY) * 0.1f
        } else {
            // Recenter if not touching
            val rx = width / 2f
            val ry = height / 2f
            currentTouchX += (rx - currentTouchX) * 0.05f
            currentTouchY += (ry - currentTouchY) * 0.05f
        }

        // Palette definitions
        val colors = when (theme) {
            "Cosmic Neon" -> intArrayOf(
                Color.parseColor("#4A148C"), // Deep Purple
                Color.parseColor("#880E4F"), // Deep Pink/Magenta
                Color.parseColor("#00E5FF"), // Neon Cyan
                Color.parseColor("#0A0015")  // Dark space base
            )
            "Ocean Breeze" -> intArrayOf(
                Color.parseColor("#0D47A1"), // Deep Blue
                Color.parseColor("#00838F"), // Cyan-Teal
                Color.parseColor("#80DEEA"), // Soft Sky Blue
                Color.parseColor("#020815")  // Near black teal
            )
            "Forest Fire" -> intArrayOf(
                Color.parseColor("#E65100"), // Flame Orange
                Color.parseColor("#780016"), // Dark Crimson
                Color.parseColor("#FFD54F"), // Golden Yellow
                Color.parseColor("#120202")  // Coal Black
            )
            else -> intArrayOf(
                Color.parseColor("#0D47A1"),
                Color.parseColor("#00838F"),
                Color.parseColor("#80DEEA"),
                Color.parseColor("#020815")
            )
        }

        // Draw background base color
        canvas.drawColor(colors[3])

        // First fluid blob (driven by time & touch)
        val blob1X = width / 2f + cos(time) * (width / 3f) + (currentTouchX - width / 2f) * 0.4f
        val blob1Y = height / 2f + sin(time) * (height / 4f) + (currentTouchY - height / 2f) * 0.4f
        val radius1 = width * 0.95f
        val shader1 = RadialGradient(
            blob1X, blob1Y, radius1,
            intArrayOf(colors[0], Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader1
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Second fluid blob (driven opposite to blob1)
        val blob2X = width / 2f + sin(time * 0.8f) * (width / 4f) - (currentTouchX - width / 2f) * 0.3f
        val blob2Y = height / 2f + cos(time * 1.1f) * (height / 3f) - (currentTouchY - height / 2f) * 0.3f
        val radius2 = width * 0.85f
        val shader2 = RadialGradient(
            blob2X, blob2Y, radius2,
            intArrayOf(colors[1], Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader2
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Third highlight fluid blob
        val blob3X = width / 2f + cos(time * 1.5f) * (width / 5f)
        val blob3Y = height / 2f + sin(time * 0.9f) * (height / 5f)
        val radius3 = width * 0.6f
        val shader3 = RadialGradient(
            blob3X, blob3Y, radius3,
            intArrayOf(colors[2], Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader3
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Clear shader
        paint.shader = null
    }

    override fun onTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                targetTouchX = event.x
                targetTouchY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                targetTouchX = -1f
                targetTouchY = -1f
            }
        }
    }

    override fun onOffsetsChanged(xOffset: Float, yOffset: Float) {
        // Shift colors slightly on page scroll
        time += xOffset * 0.15f
    }
}
