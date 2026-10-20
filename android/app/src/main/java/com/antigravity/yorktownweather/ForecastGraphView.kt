package com.antigravity.yorktownweather

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * ForecastGraphView
 * =================
 * Premium interactive 24-hour weather graph supporting:
 *  - Mode 0: Precipitation Probability & Hourly Accumulation (Bar chart + gradient fill + % badges)
 *  - Mode 1: Temperature & Apparent "Feels Like" Curve (Smooth cubic Bezier spline + area fill)
 *  - Mode 2: Wind Speed & Direction (Speed curve / bars + wind arrow markers)
 *
 * Interactive Touch Scrubber:
 *  - Scrubbing finger across the graph reveals a glowing cursor line and floating glassmorphism
 *    tooltip card showing exact time, condition emoji/text, precipitation chance, rain volume,
 *    and temperature.
 *  - Haptic feedback fires cleanly on hour boundaries as the finger glides across the timeline.
 */
class ForecastGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val MODE_PRECIPITATION = 0
        const val MODE_TEMPERATURE = 1
        const val MODE_WIND = 2
    }

    var graphMode: Int = MODE_PRECIPITATION
        set(value) {
            field = value
            invalidate()
        }

    var tempUnit: String = "F"
        set(value) {
            field = value
            invalidate()
        }

    var windUnit: String = "MPH"
        set(value) {
            field = value
            invalidate()
        }

    var precipitationUnit: String = "in"
        set(value) {
            field = value
            invalidate()
        }

    var hourlyPoints: List<ForecastActivity.HourPoint> = emptyList()
        set(value) {
            field = value
            selectedScrubberHour = null
            invalidate()
        }

    /** Index of the hour currently scrubbed (0..23). Null when not actively scrubbing. */
    var selectedScrubberHour: Int? = null
        private set

    var onHourScrubbed: ((ForecastActivity.HourPoint?) -> Unit)? = null

    // Paints
    private val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1f2e46")
        strokeWidth = 1f * resources.displayMetrics.density
        style = Paint.Style.STROKE
    }

    private val gridTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748b")
        textSize = 9.5f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.RIGHT
    }

    private val timeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94a3b8")
        textSize = 10f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val precipBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val precipValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38bdf8")
        textSize = 10f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val precipAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#ffd166")
        textSize = 9f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val tempLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#ffd166")
        strokeWidth = 3f * resources.displayMetrics.density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val tempFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val scrubberLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38bdf8")
        strokeWidth = 1.5f * resources.displayMetrics.density
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 6f), 0f)
    }

    private val scrubberDotHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5538bdf8")
        style = Paint.Style.FILL
    }

    private val scrubberDotCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // Floating Tooltip Card Paints
    private val tooltipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E60f172a")
        style = Paint.Style.FILL
    }

    private val tooltipBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38bdf8")
        strokeWidth = 1.2f * resources.displayMetrics.density
        style = Paint.Style.STROKE
    }

    private val tooltipTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 12f * resources.displayMetrics.scaledDensity
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    private val tooltipBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#93c5fd")
        textSize = 11f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.LEFT
    }

    private val tooltipSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#cbd5e1")
        textSize = 10f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.LEFT
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (hourlyPoints.isEmpty()) return super.onTouchEvent(event)

        val density = resources.displayMetrics.density
        val padL = 34f * density
        val padR = 12f * density
        val chartW = width - padL - padR
        val stepX = chartW / hourlyPoints.size.coerceAtLeast(1)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val touchX = event.x.coerceIn(padL, width - padR)
                val rawIdx = ((touchX - padL) / stepX).toInt()
                val hourIdx = rawIdx.coerceIn(0, hourlyPoints.size - 1)

                if (selectedScrubberHour != hourIdx) {
                    selectedScrubberHour = hourIdx
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onHourScrubbed?.invoke(hourlyPoints[hourIdx])
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                // Retain selection so user can continue reading the selected tooltip
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val density = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val padL = 34f * density
        val padR = 12f * density
        val padT = 24f * density
        val padB = 26f * density

        val chartW = w - padL - padR
        val chartH = h - padT - padB
        if (chartW <= 0f || chartH <= 0f) return

        // 1. Draw horizontal guide lines & labels
        when (graphMode) {
            MODE_PRECIPITATION -> drawPrecipitationGrid(canvas, padL, padT, chartW, chartH)
            MODE_TEMPERATURE -> drawTemperatureGrid(canvas, padL, padT, chartW, chartH)
            MODE_WIND -> drawWindGrid(canvas, padL, padT, chartW, chartH)
        }

        if (hourlyPoints.isEmpty()) return

        val count = hourlyPoints.size
        val stepX = chartW / count

        // 2. Draw graph data series
        when (graphMode) {
            MODE_PRECIPITATION -> drawPrecipitationBars(canvas, padL, padT, chartH, stepX, count)
            MODE_TEMPERATURE -> drawTemperatureSpline(canvas, padL, padT, chartH, stepX, count)
            MODE_WIND -> drawWindBars(canvas, padL, padT, chartH, stepX, count)
        }

        // 3. Draw time labels along bottom axis
        val baselineY = padT + chartH + 16f * density
        for (i in 0 until count) {
            // Display label every 3 hours (e.g. 12 AM, 3 AM, 6 AM, 9 AM, 12 PM, 3 PM, 6 PM, 9 PM)
            val pt = hourlyPoints[i]
            if (i % 3 == 0 || i == count - 1) {
                val cx = padL + i * stepX + stepX / 2f
                val label = pt.hourLabel
                canvas.drawText(label, cx, baselineY, timeTextPaint)
            }
        }

        // 4. Draw Interactive Scrubber & Floating Tooltip Card
        selectedScrubberHour?.let { hourIdx ->
            if (hourIdx in 0 until count) {
                drawScrubberAndTooltip(canvas, hourIdx, padL, padT, chartH, stepX)
            }
        }
    }

    private fun drawPrecipitationGrid(canvas: Canvas, padL: Float, padT: Float, chartW: Float, chartH: Float) {
        val levels = listOf(100 to "100%", 75 to "75%", 50 to "50%", 25 to "25%", 0 to "0%")
        for ((pct, label) in levels) {
            val y = padT + chartH * (1f - pct / 100f)
            canvas.drawLine(padL, y, padL + chartW, y, gridLinePaint)
            canvas.drawText(label, padL - 4f * resources.displayMetrics.density, y + 3.5f * resources.displayMetrics.density, gridTextPaint)
        }
    }

    private fun drawTemperatureGrid(canvas: Canvas, padL: Float, padT: Float, chartW: Float, chartH: Float) {
        if (hourlyPoints.isEmpty()) return
        val minT = hourlyPoints.minOf { it.temp }
        val maxT = hourlyPoints.maxOf { it.temp }
        val range = (maxT - minT).coerceAtLeast(6.0)
        val midT = (minT + maxT) / 2.0

        val levels = listOf(maxT, midT, minT)
        for (t in levels) {
            val fraction = ((t - minT) / range).toFloat().coerceIn(0f, 1f)
            val y = padT + chartH * (1f - fraction)
            canvas.drawLine(padL, y, padL + chartW, y, gridLinePaint)
            canvas.drawText("${t.roundToInt()}°", padL - 4f * resources.displayMetrics.density, y + 3.5f * resources.displayMetrics.density, gridTextPaint)
        }
    }

    private fun drawWindGrid(canvas: Canvas, padL: Float, padT: Float, chartW: Float, chartH: Float) {
        val maxW = (hourlyPoints.maxOfOrNull { it.windSpeed } ?: 20.0).coerceAtLeast(15.0)
        val levels = listOf(maxW, maxW / 2.0, 0.0)
        for (wVal in levels) {
            val fraction = (wVal / maxW).toFloat().coerceIn(0f, 1f)
            val y = padT + chartH * (1f - fraction)
            canvas.drawLine(padL, y, padL + chartW, y, gridLinePaint)
            canvas.drawText("${wVal.roundToInt()}", padL - 4f * resources.displayMetrics.density, y + 3.5f * resources.displayMetrics.density, gridTextPaint)
        }
    }

    private fun drawPrecipitationBars(canvas: Canvas, padL: Float, padT: Float, chartH: Float, stepX: Float, count: Int) {
        val density = resources.displayMetrics.density
        val barWidth = (stepX * 0.65f).coerceIn(4f * density, 18f * density)
        val radius = barWidth / 2f

        for (i in 0 until count) {
            val pt = hourlyPoints[i]
            val prob = pt.precipProb.coerceIn(0, 100)
            val barH = chartH * (prob / 100f)
            val cx = padL + i * stepX + stepX / 2f
            val left = cx - barWidth / 2f
            val right = cx + barWidth / 2f
            val bottom = padT + chartH
            val top = bottom - barH

            if (barH > 1f) {
                precipBarPaint.shader = LinearGradient(
                    cx, top, cx, bottom,
                    Color.parseColor("#38bdf8"),
                    Color.parseColor("#1e3a8a"),
                    Shader.TileMode.CLAMP
                )
                val barRect = RectF(left, top, right, bottom)
                canvas.drawRoundRect(barRect, radius, radius, precipBarPaint)
            }

            // Draw probability percentage on top of bar if >= 20%
            if (prob >= 20) {
                canvas.drawText("$prob%", cx, top - 3f * density, precipValPaint)
            }

            // Draw rain volume if > 0.00
            if (pt.precipAmount >= 0.01) {
                val amtStr = if (precipitationUnit == "mm") {
                    String.format("%.1f", pt.precipAmount)
                } else {
                    String.format("%.2f\"", pt.precipAmount)
                }
                val labelY = if (prob >= 20) top - 14f * density else top - 3f * density
                canvas.drawText(amtStr, cx, labelY, precipAmountPaint)
            }
        }
    }

    private fun drawTemperatureSpline(canvas: Canvas, padL: Float, padT: Float, chartH: Float, stepX: Float, count: Int) {
        val minT = hourlyPoints.minOf { it.temp }
        val maxT = hourlyPoints.maxOf { it.temp }
        val range = (maxT - minT).coerceAtLeast(6.0)

        val points = mutableListOf<PointF>()
        for (i in 0 until count) {
            val pt = hourlyPoints[i]
            val cx = padL + i * stepX + stepX / 2f
            val fraction = ((pt.temp - minT) / range).toFloat().coerceIn(0f, 1f)
            val cy = padT + chartH * (1f - fraction)
            points.add(PointF(cx, cy))
        }

        // Build smooth cubic Bezier path
        val path = Path()
        val fillPath = Path()
        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(points[0].x, padT + chartH)
            fillPath.lineTo(points[0].x, points[0].y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val controlX1 = p0.x + (p1.x - p0.x) / 2f
                val controlY1 = p0.y
                val controlX2 = p0.x + (p1.x - p0.x) / 2f
                val controlY2 = p1.y
                path.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
            }

            fillPath.lineTo(points.last().x, padT + chartH)
            fillPath.close()

            // Area fill
            tempFillPaint.shader = LinearGradient(
                0f, padT, 0f, padT + chartH,
                Color.parseColor("#44ffd166"),
                Color.parseColor("#05ffd166"),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(fillPath, tempFillPaint)

            // Stroke line
            canvas.drawPath(path, tempLinePaint)
        }
    }

    private fun drawWindBars(canvas: Canvas, padL: Float, padT: Float, chartH: Float, stepX: Float, count: Int) {
        val density = resources.displayMetrics.density
        val maxW = (hourlyPoints.maxOfOrNull { it.windSpeed } ?: 20.0).coerceAtLeast(15.0)
        val barWidth = (stepX * 0.60f).coerceIn(4f * density, 16f * density)
        val radius = barWidth / 2f

        for (i in 0 until count) {
            val pt = hourlyPoints[i]
            val barH = chartH * (pt.windSpeed / maxW).toFloat().coerceIn(0f, 1f)
            val cx = padL + i * stepX + stepX / 2f
            val left = cx - barWidth / 2f
            val right = cx + barWidth / 2f
            val bottom = padT + chartH
            val top = bottom - barH

            precipBarPaint.shader = LinearGradient(
                cx, top, cx, bottom,
                Color.parseColor("#34d399"),
                Color.parseColor("#065f46"),
                Shader.TileMode.CLAMP
            )
            val barRect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(barRect, radius, radius, precipBarPaint)

            if (pt.windSpeed >= 5.0) {
                canvas.drawText("${pt.windSpeed.roundToInt()}", cx, top - 3f * density, precipValPaint)
            }
        }
    }

    private fun drawScrubberAndTooltip(
        canvas: Canvas,
        hourIdx: Int,
        padL: Float,
        padT: Float,
        chartH: Float,
        stepX: Float
    ) {
        val density = resources.displayMetrics.density
        val pt = hourlyPoints[hourIdx]
        val cx = padL + hourIdx * stepX + stepX / 2f

        // 1. Vertical glowing guide line
        canvas.drawLine(cx, padT, cx, padT + chartH, scrubberLinePaint)

        // 2. Cursor dot at current metric value
        val dotY = when (graphMode) {
            MODE_PRECIPITATION -> padT + chartH * (1f - (pt.precipProb.coerceIn(0, 100) / 100f))
            MODE_TEMPERATURE -> {
                val minT = hourlyPoints.minOf { it.temp }
                val maxT = hourlyPoints.maxOf { it.temp }
                val range = (maxT - minT).coerceAtLeast(6.0)
                padT + chartH * (1f - ((pt.temp - minT) / range).toFloat().coerceIn(0f, 1f))
            }
            MODE_WIND -> {
                val maxW = (hourlyPoints.maxOfOrNull { it.windSpeed } ?: 20.0).coerceAtLeast(15.0)
                padT + chartH * (1f - (pt.windSpeed / maxW).toFloat().coerceIn(0f, 1f))
            }
            else -> padT + chartH / 2f
        }

        canvas.drawCircle(cx, dotY, 7f * density, scrubberDotHalo)
        canvas.drawCircle(cx, dotY, 4f * density, scrubberDotCenter)

        // 3. Floating Tooltip Card
        val cardW = 160f * density
        val cardH = 58f * density
        var cardL = cx - cardW / 2f
        if (cardL < padL) cardL = padL
        if (cardL + cardW > width - 8f * density) cardL = width - 8f * density - cardW
        val cardT = padT + 4f * density
        val cardR = cardL + cardW
        val cardB = cardT + cardH

        val cardRect = RectF(cardL, cardT, cardR, cardB)
        canvas.drawRoundRect(cardRect, 10f * density, 10f * density, tooltipBgPaint)
        canvas.drawRoundRect(cardRect, 10f * density, 10f * density, tooltipBorderPaint)

        val textPadL = cardL + 10f * density
        val line1 = "${pt.hourLabel} • ${pt.emoji} ${pt.conditionText}"
        val line2 = when (graphMode) {
            MODE_PRECIPITATION -> "💧 ${pt.precipProb}% chance" + (if (pt.precipAmount > 0.005) " • ${String.format("%.2f", pt.precipAmount)}$precipitationUnit" else "")
            MODE_TEMPERATURE -> "🌡️ ${formatTemp(pt.temp)} (Feels ${formatTempShort(pt.feelsLike)})"
            MODE_WIND -> "💨 ${pt.windSpeed.roundToInt()} $windUnit • Hum ${pt.humidity}%"
            else -> ""
        }
        val line3 = "Temp: ${formatTemp(pt.temp)} • Wind: ${pt.windSpeed.roundToInt()} $windUnit"

        canvas.drawText(line1, textPadL, cardT + 17f * density, tooltipTitlePaint)
        canvas.drawText(line2, textPadL, cardT + 33f * density, tooltipBodyPaint)
        canvas.drawText(line3, textPadL, cardT + 48f * density, tooltipSubPaint)
    }

    private fun formatTemp(tempVal: Double): String {
        val fVal = if (tempUnit == "C") ((tempVal * 9.0 / 5.0) + 32.0).roundToInt() else tempVal.roundToInt()
        val cVal = if (tempUnit == "C") tempVal.roundToInt() else ((tempVal - 32.0) * 5.0 / 9.0).roundToInt()
        return when (tempUnit) {
            "F" -> "${fVal}°F"
            "C" -> "${cVal}°C"
            "BOTH", "Dual" -> "${fVal}°F (${cVal}°C)"
            else -> "${fVal}°F"
        }
    }

    private fun formatTempShort(tempVal: Double): String {
        val fVal = if (tempUnit == "C") ((tempVal * 9.0 / 5.0) + 32.0).roundToInt() else tempVal.roundToInt()
        val cVal = if (tempUnit == "C") tempVal.roundToInt() else ((tempVal - 32.0) * 5.0 / 9.0).roundToInt()
        return when (tempUnit) {
            "C" -> "${cVal}°"
            else -> "${fVal}°"
        }
    }
}
