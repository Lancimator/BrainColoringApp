package com.example.braincoloringapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper

class BrainView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val LAST_EXIT_TIME_KEY = "last_exit_time"
    private val FILL_COUNT_KEY = "fill_count"
    private var drawLeft = 0f
    private var drawTop = 0f
    private var drawScale = 1f
    private var selectedColor = Color.RED
    private lateinit var bitmap: Bitmap
    private lateinit var mutableBitmap: Bitmap
    private val prefs = context.getSharedPreferences("BrainPrefs", Context.MODE_PRIVATE)
    private var availableFills = 1
    private var nextFillTime = 10
    private var fillCooldownMillis = 10_000L
    private var rewiredCount = 0
    private var rewiredListener: ((Int) -> Unit)? = null
    private val REWIRED_COUNT_KEY = "rewired_count"


    fun setRewiredListener(listener: (Int) -> Unit) {
        this.rewiredListener = listener
    }

    private var fillListener: ((available: Int, nextInSec: Int) -> Unit)? = null

    fun setFillListener(listener: (Int, Int) -> Unit) {
        this.fillListener = listener
    }
    private var celebrationListener: (() -> Unit)? = null

    fun setCelebrationListener(listener: () -> Unit) {
        celebrationListener = listener
    }

    private val timerHandler = Handler(Looper.getMainLooper())
    private val fillRunnable = object : Runnable {
        override fun run() {
            nextFillTime -= 1
            if (nextFillTime <= 0) {
                availableFills++
                nextFillTime = 10
            }
            fillListener?.invoke(availableFills, nextFillTime)

            timerHandler.postDelayed(this, 1000)
        }
    }

    init {
        loadBitmap()
        loadSavedFills()
        startFillTimer()
    }

    fun saveFillsOnExit() {
        prefs.edit()
            .putInt(FILL_COUNT_KEY, availableFills)
            .putLong(LAST_EXIT_TIME_KEY, System.currentTimeMillis())
            .putInt(REWIRED_COUNT_KEY, rewiredCount) // ✅ save rewired count
            .apply()
    }

    fun getRewiredCount(): Int {
        return rewiredCount
    }


    fun setSelectedColor(color: Int) {
        selectedColor = color
    }

    private fun startFillTimer() {
        timerHandler.post(fillRunnable)
    }

    private fun loadSavedFills() {
        val lastTime = prefs.getLong(LAST_EXIT_TIME_KEY, -1L)
        availableFills = prefs.getInt(FILL_COUNT_KEY, 1)


        if (lastTime != -1L) {
            val now = System.currentTimeMillis()
            val elapsedSeconds = ((now - lastTime) / 1000).toInt()
            val newFills = elapsedSeconds / 10
            val leftover = elapsedSeconds % 10

            availableFills += newFills
            nextFillTime = if (leftover == 0) 10 else 10 - leftover
        }
        rewiredCount = prefs.getInt(REWIRED_COUNT_KEY, 0)
        rewiredListener?.invoke(rewiredCount)
        fillListener?.invoke(availableFills, nextFillTime)
    }


    private fun loadBitmap(applySavedColors: Boolean = true) {
        bitmap = BitmapFactory.decodeResource(resources, R.drawable.brain)
        mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (applySavedColors) {
            loadColors()
        }
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width
        val viewHeight = height

        val bitmapWidth = mutableBitmap.width
        val bitmapHeight = mutableBitmap.height

        drawScale = minOf(
            viewWidth.toFloat() / bitmapWidth,
            viewHeight.toFloat() / bitmapHeight
        )

        val scaledWidth = bitmapWidth * drawScale
        val scaledHeight = bitmapHeight * drawScale

        drawLeft = (viewWidth - scaledWidth) / 2
        drawTop = (viewHeight - scaledHeight) / 2

        val destRect = RectF(drawLeft, drawTop, drawLeft + scaledWidth, drawTop + scaledHeight)
        canvas.drawBitmap(mutableBitmap, null, destRect, null)
    }



    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            // Map screen coordinates to bitmap coordinates
            val touchX = ((event.x - drawLeft) / drawScale).toInt()
            val touchY = ((event.y - drawTop) / drawScale).toInt()

            // Make sure we're within bitmap bounds
            if (touchX in 0 until mutableBitmap.width && touchY in 0 until mutableBitmap.height) {
                val targetColor = mutableBitmap.getPixel(touchX, touchY)
                val r = Color.red(targetColor)
                val g = Color.green(targetColor)
                val b = Color.blue(targetColor)

                val isNotLine = targetColor != Color.BLACK
                val isNotExcludedColor = !(r == 254 && g == 255 && b == 255)

                if (isNotLine && isNotExcludedColor && availableFills > 0) {
                    availableFills--
                    rewiredCount++
                    floodFill(mutableBitmap, touchX, touchY, targetColor, selectedColor)
                    saveColor(touchX, touchY, selectedColor)
                    invalidate()
                    fillListener?.invoke(availableFills, nextFillTime)
                    rewiredListener?.invoke(rewiredCount)

                    if (rewiredCount == 5) {
                        celebrationListener?.invoke()
                    }
                }




            }
        }
        return true
    }


    fun resetImage() {
        // Clear saved color data and fill info
        prefs.edit()
            .clear()
            .apply()
        prefs.edit().remove(REWIRED_COUNT_KEY).apply()


        // Reset logic
        availableFills = 1
        nextFillTime = 10
        rewiredCount = 0

        // Reload a fresh, clean copy of the image
        bitmap = BitmapFactory.decodeResource(resources, R.drawable.brain)
        mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        // Redraw and update UI
        invalidate()
        fillListener?.invoke(availableFills, nextFillTime)
        rewiredListener?.invoke(rewiredCount)
    }



    private fun saveColor(x: Int, y: Int, color: Int) {
        prefs.edit().putInt("${x}_${y}", color).apply()
    }

    private fun loadColors() {
        for ((key, value) in prefs.all) {
            val coords = key.split("_")
            if (coords.size == 2) {
                val x = coords[0].toIntOrNull()
                val y = coords[1].toIntOrNull()
                if (x != null && y != null) {
                    floodFill(mutableBitmap, x, y, mutableBitmap.getPixel(x, y), value as Int)
                }
            }
        }
    }

    private fun floodFill(bmp: Bitmap, x: Int, y: Int, targetColor: Int, replacementColor: Int) {
        if (targetColor == replacementColor) return

        val stack = ArrayDeque<Point>()
        stack.add(Point(x, y))

        while (stack.isNotEmpty()) {
            val point = stack.removeLast()
            val cx = point.x
            val cy = point.y

            if (cx !in 0 until bmp.width || cy !in 0 until bmp.height) continue
            if (bmp.getPixel(cx, cy) != targetColor) continue

            bmp.setPixel(cx, cy, replacementColor)

            stack.add(Point(cx + 1, cy))
            stack.add(Point(cx - 1, cy))
            stack.add(Point(cx, cy + 1))
            stack.add(Point(cx, cy - 1))
        }
    }
}
