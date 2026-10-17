package com.antigravity.yorktownweather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * IsometricWeatherWidgetProvider
 * ==============================
 * Native Android Homescreen & Lockscreen (`home_screen|keyguard`) AppWidgetProvider
 * designed for OnePlus 13 (OxygenOS 15) and Android 8.0+ devices.
 *
 * Features:
 *   - 4x3 default grid size, fully resizable horizontally & vertically (`onAppWidgetOptionsChanged`)
 *   - Extrapolated, high-DPI native `RemoteViews` HUD Overlay (Top-Sky Split Banner or Corner Card)
 *     that renders large, crisp typography in the open sky above the 3D island without covering the render
 *   - Built-in Pre-Generated Generic Dioramas (`coastal`, `modern`, `alpine`), Historic `yorktown`,
 *     and on-device `custom_city` high-res cutout support
 *   - Continuous live animation on the Homescreen via 4-frame `<ViewFlipper>` (gentle rain/snow
 *     and water sparkles) with static miniature diorama sprites (animated walking people removed)
 *   - Direct on-widget interactive Zoom (`➖`/`➕`), Horizontal Centering (`◀`/`🎯`/`▶`), and Settings (`⚙️`) buttons
 *     (rotation intentionally removed so the isometric island stays at its optimal architectural angle)
 */
class IsometricWeatherWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.antigravity.yorktownweather.ACTION_REFRESH_WIDGET"
        const val ACTION_ZOOM_IN = "com.antigravity.yorktownweather.ACTION_ZOOM_IN"
        const val ACTION_ZOOM_OUT = "com.antigravity.yorktownweather.ACTION_ZOOM_OUT"
        const val ACTION_PAN_LEFT = "com.antigravity.yorktownweather.ACTION_PAN_LEFT"
        const val ACTION_PAN_RIGHT = "com.antigravity.yorktownweather.ACTION_PAN_RIGHT"
        const val ACTION_CAM_RESET = "com.antigravity.yorktownweather.ACTION_CAM_RESET"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val act = intent.action ?: return
        if (act in listOf(
                ACTION_REFRESH_WIDGET,
                ACTION_ZOOM_IN,
                ACTION_ZOOM_OUT,
                ACTION_PAN_LEFT,
                ACTION_PAN_RIGHT,
                ACTION_CAM_RESET
            )
        ) {
            val prefs = context.getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
            var zoom = prefs.getFloat("widgetZoom", 1.0f)
            var offsetX = prefs.getFloat("widgetOffsetX", 0.0f)
            when (act) {
                ACTION_ZOOM_IN -> zoom = (zoom * 1.15f).coerceIn(0.65f, 2.40f)
                ACTION_ZOOM_OUT -> zoom = (zoom / 1.15f).coerceIn(0.65f, 2.40f)
                ACTION_PAN_LEFT -> offsetX = (offsetX - 28.0f).coerceIn(-180.0f, 180.0f)
                ACTION_PAN_RIGHT -> offsetX = (offsetX + 28.0f).coerceIn(-180.0f, 180.0f)
                ACTION_CAM_RESET -> {
                    zoom = 1.0f
                    offsetX = 0.0f
                }
            }
            if (act != ACTION_REFRESH_WIDGET) {
                prefs.edit()
                    .putFloat("widgetZoom", zoom)
                    .putFloat("widgetOffsetX", offsetX)
                    .apply()
            }
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, IsometricWeatherWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                onUpdate(context, mgr, ids)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                val prefs = context.getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
                val cfg = WidgetConfig(
                    locationName = prefs.getString("locationName", "Yorktown, VA") ?: "Yorktown, VA",
                    lat = prefs.getFloat("latitude", 37.2388f).toDouble(),
                    lon = prefs.getFloat("longitude", -76.5097f).toDouble(),
                    renderMode = prefs.getString("renderMode", "hybrid") ?: "hybrid",
                    dioramaTheme = prefs.getString("dioramaTheme", "yorktown") ?: "yorktown",
                    hudPosition = prefs.getString("hudPosition", "top-sky") ?: "top-sky",
                    hudScale = prefs.getFloat("hudScale", 1.15f),
                    transparentBg = prefs.getBoolean("transparentBackground", false),
                    showExtendedHud = prefs.getBoolean("showExtendedHud", true),
                    timeMode = prefs.getString("timeOfDayMode", "realtime") ?: "realtime",
                    weatherOverride = prefs.getString("weatherOverride", "realtime") ?: "realtime",
                    tempUnit = prefs.getString("temperatureUnit", "BOTH") ?: "BOTH",
                    windUnit = prefs.getString("windSpeedUnit", "mph") ?: "mph",
                    widgetZoom = prefs.getFloat("widgetZoom", 1.0f),
                    widgetOffsetX = prefs.getFloat("widgetOffsetX", 0.0f),
                    particleDensity = prefs.getFloat("particleDensity", 0.50f)
                )

                val telemetry = fetchOpenMeteoTelemetry(cfg.lat, cfg.lon)
                val baseIsland = loadDioramaBitmap(context, cfg, telemetry)

                val frameIds = intArrayOf(
                    R.id.widget_frame_0,
                    R.id.widget_frame_1,
                    R.id.widget_frame_2,
                    R.id.widget_frame_3
                )
                val frames = (0 until 4).map { idx ->
                    renderIsometricWidgetFrame(cfg, telemetry, baseIsland, idx / 4.0f)
                }

                val launchIntent = Intent(context, MainActivity::class.java)
                val pendingOpenApp = PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                fun makeActionPendingIntent(action: String, reqCode: Int): PendingIntent {
                    val i = Intent(context, IsometricWeatherWidgetProvider::class.java).apply {
                        this.action = action
                    }
                    return PendingIntent.getBroadcast(
                        context,
                        reqCode,
                        i,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                val effectiveCat = if (cfg.weatherOverride != "realtime") cfg.weatherOverride else wmoToCategory(telemetry.weatherCode)
                val effectiveTime = if (cfg.timeMode != "realtime") cfg.timeMode else (if (telemetry.isDay) "day" else "night")
                val wxEmoji = categoryToEmoji(effectiveCat, effectiveTime == "night")
                val headline = categoryToHeadline(effectiveCat)

                val fVal = telemetry.tempF.roundToInt()
                val cVal = ((telemetry.tempF - 32.0) * 5.0 / 9.0).roundToInt()
                val tempFormatted = when (cfg.tempUnit) {
                    "F" -> "${fVal}°F"
                    "C" -> "${cVal}°C"
                    else -> "${fVal}°F (${cVal}°C)"
                }
                val feelsFormatted = when (cfg.tempUnit) {
                    "C" -> "${((telemetry.feelsLikeF - 32.0) * 5.0 / 9.0).roundToInt()}°C"
                    else -> "${telemetry.feelsLikeF.roundToInt()}°F"
                }

                val dirs = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
                val compass = dirs[((telemetry.windDeg % 360 + 360) % 360 / 22.5).roundToInt() % 16]
                val windFormatted = when (cfg.windUnit) {
                    "kmh" -> "${(telemetry.windMph * 1.60934).roundToInt()} KM/H $compass"
                    "kn" -> "${(telemetry.windMph * 0.868976).roundToInt()} KN $compass"
                    "ms" -> "${(telemetry.windMph * 0.44704).roundToInt()} M/S $compass"
                    else -> "${telemetry.windMph.roundToInt()} MPH $compass"
                }

                val themeLabel = when (cfg.dioramaTheme) {
                    "coastal" -> "Coastal Harbor 3D"
                    "modern" -> "Modern City 3D"
                    "alpine" -> "Alpine Village 3D"
                    "custom_city" -> "Custom City 3D Cutout"
                    else -> "Historic Yorktown 3D"
                }

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_isometric_weather)
                    for (i in frameIds.indices) {
                        views.setImageViewBitmap(frameIds[i], frames[i])
                    }

                    // Populate & scale Extrapolated Native RemoteViews HUD
                    val scale = cfg.hudScale.coerceIn(0.90f, 1.45f)
                    if (cfg.hudPosition == "top-sky") {
                        views.setViewVisibility(R.id.widget_hud_top_bar, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_hud_corner, View.GONE)

                        views.setTextViewText(R.id.hud_bar_temp, "$wxEmoji $tempFormatted")
                        views.setTextViewText(R.id.hud_bar_condition_loc, "$headline • 📍 ${cfg.locationName.uppercase()}")
                        views.setTextViewText(R.id.hud_bar_wind_hum, "💧 ${telemetry.humidity}% • 💨 $windFormatted")
                        views.setTextViewText(
                            R.id.hud_bar_badges,
                            if (cfg.showExtendedHud) "AQI ${telemetry.usAqi} GOOD • UV ${String.format("%.1f", telemetry.uvIndex)} • 🌔 • FEELS $feelsFormatted"
                            else "FEELS LIKE $feelsFormatted"
                        )

                        views.setTextViewTextSize(R.id.hud_bar_temp, TypedValue.COMPLEX_UNIT_SP, 16.5f * scale)
                        views.setTextViewTextSize(R.id.hud_bar_condition_loc, TypedValue.COMPLEX_UNIT_SP, 11.2f * scale)
                        views.setTextViewTextSize(R.id.hud_bar_wind_hum, TypedValue.COMPLEX_UNIT_SP, 11.2f * scale)
                        views.setTextViewTextSize(R.id.hud_bar_badges, TypedValue.COMPLEX_UNIT_SP, 10.5f * scale)
                    } else {
                        views.setViewVisibility(R.id.widget_hud_top_bar, View.GONE)
                        views.setViewVisibility(R.id.widget_hud_corner, View.VISIBLE)

                        views.setTextViewText(R.id.hud_corner_condition, "$wxEmoji $headline")
                        views.setTextViewText(R.id.hud_corner_temp, tempFormatted)
                        views.setTextViewText(R.id.hud_corner_meta, "💧 ${telemetry.humidity}% • 💨 $windFormatted")
                        views.setTextViewText(R.id.hud_corner_loc, "📍 ${cfg.locationName.uppercase()} • FEELS $feelsFormatted")
                        views.setTextViewText(
                            R.id.hud_corner_badges,
                            "AQI ${telemetry.usAqi} GOOD • UV ${String.format("%.1f", telemetry.uvIndex)} • 🌔"
                        )
                        views.setViewVisibility(R.id.hud_corner_badges, if (cfg.showExtendedHud) View.VISIBLE else View.GONE)

                        views.setTextViewTextSize(R.id.hud_corner_condition, TypedValue.COMPLEX_UNIT_SP, 11.2f * scale)
                        views.setTextViewTextSize(R.id.hud_corner_temp, TypedValue.COMPLEX_UNIT_SP, 17.0f * scale)
                        views.setTextViewTextSize(R.id.hud_corner_meta, TypedValue.COMPLEX_UNIT_SP, 11.2f * scale)
                        views.setTextViewTextSize(R.id.hud_corner_loc, TypedValue.COMPLEX_UNIT_SP, 10.8f * scale)
                        views.setTextViewTextSize(R.id.hud_corner_badges, TypedValue.COMPLEX_UNIT_SP, 10.2f * scale)
                    }

                    views.setTextViewText(R.id.widget_bottom_badge, "${cfg.locationName} • $themeLabel")

                    views.setOnClickPendingIntent(R.id.widget_flipper, pendingOpenApp)
                    views.setOnClickPendingIntent(R.id.widget_hud_top_bar, pendingOpenApp)
                    views.setOnClickPendingIntent(R.id.widget_hud_corner, pendingOpenApp)
                    views.setOnClickPendingIntent(R.id.btn_open_config, pendingOpenApp)
                    views.setOnClickPendingIntent(R.id.btn_zoom_in, makeActionPendingIntent(ACTION_ZOOM_IN, 101))
                    views.setOnClickPendingIntent(R.id.btn_zoom_out, makeActionPendingIntent(ACTION_ZOOM_OUT, 102))
                    views.setOnClickPendingIntent(R.id.btn_pan_left, makeActionPendingIntent(ACTION_PAN_LEFT, 103))
                    views.setOnClickPendingIntent(R.id.btn_pan_right, makeActionPendingIntent(ACTION_PAN_RIGHT, 104))
                    views.setOnClickPendingIntent(R.id.btn_cam_reset, makeActionPendingIntent(ACTION_CAM_RESET, 105))

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Loads the appropriate 3D isometric cutout bitmap:
     *   - Built-in Pre-Generated Generic themes (`coastal`, `modern`, `alpine`)
     *   - Historic `yorktown`
     *   - On-device generated `custom_city` cutout stored in `context.filesDir`
     */
    private fun loadDioramaBitmap(context: Context, cfg: WidgetConfig, wx: WeatherTelemetry): Bitmap? {
        val effectiveTime = if (cfg.timeMode != "realtime") cfg.timeMode else (if (wx.isDay) "day" else "night")
        val isNight = effectiveTime == "night"
        val isSunset = effectiveTime == "sunset"

        // 1. Check for on-device Custom Generated High-Res City Cutout first
        if (cfg.dioramaTheme == "custom_city" || cfg.renderMode == "ai_generative") {
            val suffix = when {
                isNight -> "night"
                isSunset -> "sunset"
                else -> "day"
            }
            val customFile = File(context.filesDir, "custom_city_$suffix.png")
            if (customFile.exists() && customFile.length() > 1024) {
                runCatching {
                    val bmp = BitmapFactory.decodeFile(customFile.absolutePath)
                    if (bmp != null) return bmp
                }
            }
        }

        // 2. Load from built-in Pre-Generated Diorama Cutout sets in assets
        val prefix = when (cfg.dioramaTheme) {
            "coastal" -> "coastal_island"
            "modern" -> "modern_island"
            "alpine" -> "alpine_island"
            else -> "yorktown_island"
        }
        val assetName = when {
            isNight -> "web/images/${prefix}_night.png"
            isSunset -> "web/images/${prefix}_sunset.png"
            else -> "web/images/$prefix.png"
        }
        return runCatching {
            context.assets.open(assetName).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    private fun fetchOpenMeteoTelemetry(lat: Double, lon: Double): WeatherTelemetry {
        val urlStr = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,wind_direction_10m" +
            "&hourly=temperature_2m,precipitation_probability,uv_index" +
            "&daily=temperature_2m_max,temperature_2m_min" +
            "&temperature_unit=fahrenheit&wind_speed_unit=mph&timezone=auto&forecast_days=3"
        var tempF = 68.0
        var feelsF = 67.0
        var hum = 64
        var wind = 8.0
        var windDeg = 25
        var code = 0
        var isDay = true
        var uvIndex = 3.2
        val hourlyTemps = mutableListOf(68, 69, 71, 72, 71, 69, 67, 65, 63, 62, 61, 60)

        runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val current = json.getJSONObject("current")
            tempF = current.getDouble("temperature_2m")
            feelsF = current.getDouble("apparent_temperature")
            hum = current.getDouble("relative_humidity_2m").roundToInt()
            wind = current.getDouble("wind_speed_10m")
            windDeg = current.optInt("wind_direction_10m", 25)
            code = current.getInt("weather_code")
            isDay = current.getInt("is_day") == 1

            val hourly = json.optJSONObject("hourly")
            if (hourly != null) {
                val tArr = hourly.optJSONArray("temperature_2m")
                val uvArr = hourly.optJSONArray("uv_index")
                if (tArr != null && tArr.length() >= 12) {
                    hourlyTemps.clear()
                    for (i in 0 until 12) {
                        hourlyTemps.add(tArr.getDouble(i).roundToInt())
                    }
                }
                if (uvArr != null && uvArr.length() > 12) {
                    uvIndex = uvArr.optDouble(12, 3.2)
                }
            }
        }

        var aqi = 24
        runCatching {
            val aqUrl = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&current=us_aqi"
            val conn = (URL(aqUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val cur = JSONObject(body).optJSONObject("current")
            if (cur != null && !cur.isNull("us_aqi")) {
                aqi = cur.getDouble("us_aqi").roundToInt()
            }
        }

        return WeatherTelemetry(tempF, feelsF, hum, wind, windDeg, code, isDay, aqi, uvIndex, hourlyTemps)
    }

    /**
     * Renders a single frame of the 3D isometric scene.
     * The HUD overlay is rendered natively via RemoteViews in `widget_isometric_weather.xml`,
     * while the 3D island is positioned slightly lower (`islandOffsetY`) and shifted horizontally
     * by `cfg.widgetOffsetX` so the HUD never obscures the 3D rendering and stays fixed when panning.
     */
    private fun renderIsometricWidgetFrame(
        cfg: WidgetConfig,
        wx: WeatherTelemetry,
        islandBmp: Bitmap?,
        phase: Float
    ): Bitmap {
        val width = 688
        val height = 384
        val s = width / 1376f
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val effectiveTime = if (cfg.timeMode != "realtime") cfg.timeMode else (if (wx.isDay) "day" else "night")
        val isNight = effectiveTime == "night"
        val isSunset = effectiveTime == "sunset"
        val effectiveCat = if (cfg.weatherOverride != "realtime") cfg.weatherOverride else wmoToCategory(wx.weatherCode)

        // 1. Sky Card Gradient (unless Transparent Wallpaper Cutout Mode is checked)
        if (!cfg.transparentBg) {
            val topColor = when {
                isNight -> Color.parseColor("#0d1b2a")
                isSunset -> Color.parseColor("#3a506b")
                effectiveCat == "rain" || effectiveCat == "thunderstorm" -> Color.parseColor("#4a6278")
                else -> Color.parseColor("#7fc4f2")
            }
            val botColor = when {
                isNight -> Color.parseColor("#1b314c")
                isSunset -> Color.parseColor("#f29a6b")
                effectiveCat == "rain" || effectiveCat == "thunderstorm" -> Color.parseColor("#7b92a8")
                else -> Color.parseColor("#a8dbf8")
            }
            paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), topColor, botColor, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 22f, 22f, paint)
            paint.shader = null

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = if (isNight) Color.parseColor("#3b526b") else Color.parseColor("#bce0f7")
            canvas.drawRoundRect(RectF(1f, 1f, width - 1f, height - 1f), 22f, 22f, paint)
            paint.style = Paint.Style.FILL
        }

        // 2. Moon Crescent Disc in Night Sky
        if (isNight) {
            paint.color = Color.parseColor("#fff2b2")
            canvas.drawCircle(165f * s, 175f * s, 28f * s, paint)
            if (!cfg.transparentBg) {
                paint.color = Color.parseColor("#112236")
                canvas.drawCircle(153f * s, 169f * s, 24f * s, paint)
            }
        }

        // 3. Draw 3D Isometric Diorama + Animated Water Sparkles + Static Miniature Sprites
        // Shift island slightly down when using Top-Sky HUD so 0% of the 3D town is covered by the enlarged HUD!
        val islandOffsetY = if (cfg.hudPosition == "top-sky") 28f else 8f
        val islandOffsetX = cfg.widgetOffsetX

        canvas.save()
        val pivotX = width * 0.5f + islandOffsetX
        val pivotY = height * 0.56f + islandOffsetY
        canvas.translate(pivotX, pivotY)
        canvas.scale(cfg.widgetZoom, cfg.widgetZoom)
        canvas.translate(-pivotX, -pivotY)

        if (islandBmp != null) {
            val destRect = RectF(
                islandOffsetX,
                islandOffsetY,
                width + islandOffsetX,
                height + islandOffsetY
            )
            canvas.drawBitmap(islandBmp, null, destRect, paint)
        }

        // 3B. Animated Water Sparkles
        val waterPts = arrayOf(
            0.14f to 0.47f, 0.18f to 0.52f, 0.24f to 0.56f,
            0.28f to 0.46f, 0.33f to 0.42f, 0.38f to 0.34f, 0.44f to 0.28f
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.6f * s
        for (wi in waterPts.indices) {
            val (wpx, wpy) = waterPts[wi]
            val alpha = (110 + (65 * sin((phase * 2.0 * Math.PI) + wi * 1.3)).toInt()).coerceIn(40, 210)
            paint.color = if (isSunset) Color.argb(alpha, 255, 210, 145) else Color.argb(alpha, 230, 248, 255)
            val wxPt = islandOffsetX + (wpx + 0.005f * sin((phase * 2.0 * Math.PI + wi).toFloat())) * width
            val wyPt = islandOffsetY + wpy * height
            canvas.drawLine(wxPt - 9f * s, wyPt - 3.5f * s, wxPt + 9f * s, wyPt + 3.5f * s, paint)
        }
        paint.style = Paint.Style.FILL

        // 3C. Static Scenic Miniature Sprites (Animated walking people removed)
        if (cfg.dioramaTheme == "yorktown") {
            // Static Sprite 1: Colonial Horse & Carriage stationed on Historic Main Street
            val hcX = islandOffsetX + 0.442f * width
            val hcY = islandOffsetY + 0.648f * height
            val hx = hcX + 4.2f * s
            val hy = hcY - 3.1f * s
            paint.color = Color.parseColor("#6e4226")
            canvas.drawOval(RectF(hx - 2.2f * s, hy - 2.6f * s, hx + 2.2f * s, hy), paint)
            paint.color = Color.parseColor("#3b2316")
            canvas.drawOval(RectF(hcX - 3.6f * s, hcY - 4.4f * s, hcX + 3.6f * s, hcY - 0.4f * s), paint)
            paint.color = Color.parseColor("#9e3b2b")
            canvas.drawOval(RectF(hcX - 2.8f * s, hcY - 5.2f * s, hcX + 2.8f * s, hcY - 2.0f * s), paint)

            // Static Sprite 2 & 3: Miniature Cars on Coleman Memorial Bridge
            val carX = islandOffsetX + 0.365f * width
            val carY = islandOffsetY + 0.384f * height
            paint.color = Color.parseColor("#d64035")
            canvas.drawOval(RectF(carX - 2.5f * s, carY - 2.4f * s, carX + 2.5f * s, carY), paint)
            paint.color = Color.parseColor("#f2f6fa")
            canvas.drawOval(RectF(carX - 1.4f * s, carY - 3.0f * s, carX + 1.4f * s, carY - 1.2f * s), paint)

            val car2X = islandOffsetX + 0.422f * width
            val car2Y = islandOffsetY + 0.425f * height
            paint.color = Color.parseColor("#2b6cb0")
            canvas.drawOval(RectF(car2X - 2.4f * s, car2Y - 2.2f * s, car2X + 2.4f * s, car2Y), paint)
        }

        canvas.restore()

        // 4. Gentle, Natural-Speed Animated Weather Particles (Rain / Snow)
        val density = cfg.particleDensity.coerceIn(0.15f, 1.5f)
        if (effectiveCat == "rain" || effectiveCat == "thunderstorm") {
            val dropCount = max(4, ((if (effectiveCat == "thunderstorm") 26 else 16) * density).roundToInt())
            paint.color = Color.argb(125, 205, 234, 255)
            paint.strokeWidth = 1.4f * s
            for (i in 0 until dropCount) {
                val prog = ((i * 0.173f) + phase * 0.25f) % 1.0f
                val rx = (((i * 0.618f) % 1.08f) - prog * 0.12f) * width
                val ry = prog * height
                canvas.drawLine(rx, ry, rx - 4.5f * s, ry + 15f * s, paint)
            }
        } else if (effectiveCat == "snow") {
            val flakeCount = max(5, (22 * density).roundToInt())
            paint.color = Color.argb(210, 255, 255, 255)
            for (i in 0 until flakeCount) {
                val prog = ((i * 0.137f) + phase * 0.20f) % 1.0f
                val sx = (((i * 0.618f) % 1.0f) + 0.015f * sin((phase * 2 * Math.PI + i).toFloat())) * width
                val sy = prog * height
                canvas.drawCircle(sx, sy, 2.6f * s, paint)
            }
        }

        return bmp
    }

    private fun wmoToCategory(code: Int): String = when (code) {
        0 -> "clear"
        1, 2 -> "partly_cloudy"
        3 -> "overcast"
        45, 48 -> "fog"
        in 51..67, in 80..82 -> "rain"
        in 71..77, 85, 86 -> "snow"
        in 95..99 -> "thunderstorm"
        else -> "clear"
    }

    private fun categoryToHeadline(cat: String): String = when (cat) {
        "clear" -> "CLEAR"
        "partly_cloudy" -> "PARTLY CLOUDY"
        "overcast" -> "OVERCAST"
        "fog" -> "COASTAL FOG"
        "rain" -> "RAIN SHOWER"
        "snow" -> "SNOWFALL"
        "thunderstorm" -> "THUNDERSTORM"
        else -> "CLEAR"
    }

    private fun categoryToEmoji(cat: String, isNight: Boolean): String = when (cat) {
        "clear" -> if (isNight) "🌙" else "☀️"
        "partly_cloudy" -> if (isNight) "☁️" else "⛅"
        "overcast" -> "☁️"
        "fog" -> "🌫️"
        "rain" -> "🌧️"
        "snow" -> "❄️"
        "thunderstorm" -> "⛈️"
        else -> if (isNight) "🌙" else "☀️"
    }

    data class WidgetConfig(
        val locationName: String,
        val lat: Double,
        val lon: Double,
        val renderMode: String,
        val dioramaTheme: String,
        val hudPosition: String,
        val hudScale: Float,
        val transparentBg: Boolean,
        val showExtendedHud: Boolean,
        val timeMode: String,
        val weatherOverride: String,
        val tempUnit: String,
        val windUnit: String,
        val widgetZoom: Float,
        val widgetOffsetX: Float,
        val particleDensity: Float
    )

    data class WeatherTelemetry(
        val tempF: Double,
        val feelsLikeF: Double,
        val humidity: Int,
        val windMph: Double,
        val windDeg: Int,
        val weatherCode: Int,
        val isDay: Boolean,
        val usAqi: Int,
        val uvIndex: Double,
        val hourlyTemps: List<Int>
    )
}
