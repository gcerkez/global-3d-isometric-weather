package com.antigravity.yorktownweather

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

/**
 * TempRangeBarView
 * ================
 * High-precision, Apple Weather-style horizontal temperature range pill bar.
 *
 * Renders:
 *  1. A subtle dark translucent track representing the week's overall [weekMin, weekMax] range.
 *  2. A smooth rounded gradient pill for the day's [dayMin, dayMax] temperature range,
 *     dynamically color-coded (cool blue/teal to warm amber/coral).
 *  3. An optional glowing white indicator dot for the current real-time temperature (on "Today").
 */
class TempRangeBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var weekMin: Float = 30f
        set(value) {
            field = value
            invalidate()
        }

    var weekMax: Float = 85f
        set(value) {
            field = value
            invalidate()
        }

    var dayMin: Float = 45f
        set(value) {
            field = value
            invalidate()
        }

    var dayMax: Float = 72f
        set(value) {
            field = value
            invalidate()
        }

    /** Real-time current temperature. If non-null, renders a bright white indicator dot on the bar. */
    var currentTemp: Float? = null
        set(value) {
            field = value
            invalidate()
        }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1f2e46")
        style = Paint.Style.FILL
    }

    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#44FFFFFF")
        style = Paint.Style.FILL
    }

    private val dotInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val trackRect = RectF()
    private val pillRect = RectF()

    init {
        // Default height ~6dp
        minimumHeight = (6 * resources.displayMetrics.density).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val barHeight = (5.5f * resources.displayMetrics.density).coerceAtMost(h * 0.75f)
        val cornerRadius = barHeight / 2f
        val top = (h - barHeight) / 2f
        val bottom = top + barHeight

        // 1. Draw background track for week range
        trackRect.set(0f, top, w, bottom)
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint)

        val totalRange = max(1f, weekMax - weekMin)

        // 2. Compute day min/max positions along track
        val startFraction = ((dayMin - weekMin) / totalRange).coerceIn(0f, 1f)
        val endFraction = ((dayMax - weekMin) / totalRange).coerceIn(0f, 1f)

        var left = startFraction * w
        var right = endFraction * w

        // Ensure minimum visible pill width
        if (right - left < barHeight) {
            right = (left + barHeight).coerceAtMost(w)
            left = (right - barHeight).coerceAtLeast(0f)
        }

        pillRect.set(left, top, right, bottom)

        // Dynamic temperature gradient colors
        val startColor = tempToColor(dayMin)
        val endColor = tempToColor(dayMax)

        pillPaint.shader = LinearGradient(
            left, top, right, bottom,
            startColor, endColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, pillPaint)

        // 3. Render current temperature indicator dot (for Today)
        currentTemp?.let { cur ->
            val curFraction = ((cur - weekMin) / totalRange).coerceIn(0f, 1f)
            val dotX = (curFraction * w).coerceIn(left + cornerRadius, right - cornerRadius)
            val dotY = h / 2f

            // Halo & solid center dot
            val haloRadius = barHeight * 0.95f
            val centerRadius = barHeight * 0.55f
            canvas.drawCircle(dotX, dotY, haloRadius, dotHaloPaint)
            canvas.drawCircle(dotX, dotY, centerRadius, dotInnerPaint)
        }
    }

    private fun tempToColor(temp: Float): Int {
        // Temperature gradient mapping in Fahrenheit
        return when {
            temp < 32f -> Color.parseColor("#60a5fa") // Ice blue
            temp < 50f -> Color.parseColor("#38bdf8") // Sky cyan
            temp < 65f -> Color.parseColor("#2dd4bf") // Teal / emerald
            temp < 78f -> Color.parseColor("#ffd166") // Warm gold
            temp < 88f -> Color.parseColor("#fb923c") // Amber orange
            else -> Color.parseColor("#f87171")       // Coral red
        }
    }
}
