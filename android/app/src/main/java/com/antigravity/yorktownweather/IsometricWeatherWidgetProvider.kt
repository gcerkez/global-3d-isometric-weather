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
                    "jamestown" -> "Historic Jamestown 3D"
                    "washington_dc" -> "Washington, D.C. 3D"
                    "new_york" -> "New York City 3D"
                    "san_francisco" -> "San Francisco 3D"
                    "chicago_great_lakes" -> "Chicago & Great Lakes 3D"
                    "grand_canyon" -> "Grand Canyon & SW 3D"
                    "new_orleans" -> "New Orleans 3D"
                    "seattle_pnw" -> "Seattle & PNW 3D"
                    "miami_caribbean" -> "Miami & Keys 3D"
                    "coastal" -> "Maritime Harbor 3D"
                    "london_uk" -> "London & UK 3D"
                    "paris_europe" -> "Paris & Europe 3D"
                    "rome_mediterranean" -> "Rome & Mediterranean 3D"
                    "cairo_giza" -> "Cairo & Giza 3D"
                    "tokyo_east_asia" -> "Tokyo & East Asia 3D"
                    "modern" -> "Modern Metropolis 3D"
                    "alpine" -> "Alpine Village 3D"
                    else -> "Historic Yorktown 3D"
                }

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_isometric_weather)

                    // Real on-screen widget size (dp). Portrait launchers lay out at MIN_WIDTH x MAX_HEIGHT.
                    val widgetWidthDp = widgetWidthDp(appWidgetManager, widgetId)
                    val widgetHeightDp = widgetHeightDp(appWidgetManager, widgetId)

                    // Populate & auto-fit the native RemoteViews HUD
                    val scale = cfg.hudScale.coerceIn(0.90f, 1.45f)

                    // Upper-bound HUD height (dp) for the stacked top bar so the island is drawn below it:
                    // root padding + margin + vertical padding + each row's line height (~1.3x text size)
                    val hudHeightDp = if (cfg.hudPosition == "top-sky") {
                        2f + 6f + 11f + 17f * scale * 1.22f + 2f + 2f * 11.5f * scale * 1.3f + 1f +
                            (if (cfg.showExtendedHud) 1f + 11f * scale * 1.3f else 0f)
                    } else 0f

                    for (i in frameIds.indices) {
                        val frame = renderIsometricWidgetFrame(
                            cfg, telemetry, baseIsland, i / 4.0f,
                            widgetWidthDp, widgetHeightDp, hudHeightDp
                        )
                        views.setImageViewBitmap(frameIds[i], frame)
                    }
                    val feelsLine = "📍 ${cfg.locationName.uppercase()} • FEELS $feelsFormatted"
                    val metaLine = "💧 ${telemetry.humidity}% • 💨 $windFormatted"
                    val badgeLine = "AQI ${telemetry.usAqi} ${aqiLabel(telemetry.usAqi)} • UV ${String.format("%.1f", telemetry.uvIndex)} • 🌔"

                    if (cfg.hudPosition == "top-sky") {
                        views.setViewVisibility(R.id.widget_hud_top_bar, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_hud_corner, View.GONE)

                        // Bar spans the widget: 2dp root padding + 6dp margins + 10dp padding per side
                        val rowWidthDp = widgetWidthDp - 2 * (2f + 6f + 10f)
                        val tempLine = "$wxEmoji $tempFormatted"

                        views.setTextViewText(R.id.hud_bar_temp, tempLine)
                        views.setTextViewText(R.id.hud_bar_condition, headline)
                        views.setTextViewText(R.id.hud_bar_condition_loc, feelsLine)
                        views.setTextViewText(R.id.hud_bar_wind_hum, metaLine)
                        views.setTextViewText(R.id.hud_bar_badges, badgeLine)
                        views.setViewVisibility(R.id.hud_bar_badges, if (cfg.showExtendedHud) View.VISIBLE else View.GONE)

                        // Row 1 shares its width: temperature gets ~62%, the headline the rest
                        views.setTextViewTextSize(R.id.hud_bar_temp, TypedValue.COMPLEX_UNIT_DIP,
                            fitTextSp(tempLine, 17f * scale, rowWidthDp * 0.62f, bold = true))
                        views.setTextViewTextSize(R.id.hud_bar_condition, TypedValue.COMPLEX_UNIT_DIP,
                            fitTextSp(headline, 12f * scale, rowWidthDp * 0.36f - 8f, bold = true))
                        views.setTextViewTextSize(R.id.hud_bar_condition_loc, TypedValue.COMPLEX_UNIT_DIP,
                            fitTextSp(feelsLine, 11.5f * scale, rowWidthDp, bold = true))
                        views.setTextViewTextSize(R.id.hud_bar_wind_hum, TypedValue.COMPLEX_UNIT_DIP,
                            fitTextSp(metaLine, 11.5f * scale, rowWidthDp, bold = false))
                        views.setTextViewTextSize(R.id.hud_bar_badges, TypedValue.COMPLEX_UNIT_DIP,
                            fitTextSp(badgeLine, 11f * scale, rowWidthDp, bold = true))
                    } else {
                        views.setViewVisibility(R.id.widget_hud_top_bar, View.GONE)
                        views.setViewVisibility(R.id.widget_hud_corner, View.VISIBLE)

                        // Corner card may use up to ~65% of the widget width (12dp padding each side)
                        val cardWidthDp = widgetWidthDp * 0.65f - 24f
                        val condLine = "$wxEmoji $headline"

                        views.setTextViewText(R.id.hud_corner_condition, condLine)
                        views.setTextViewText(R.id.hud_corner_temp, tempFormatted)
                        views.setTextViewText(R.id.hud_corner_meta, metaLine)
                        views.setTextViewText(R.id.hud_corner_loc, feelsLine)
                        views.setTextViewText(R.id.hud_corner_badges, badgeLine)
                        views.setViewVisibility(R.id.hud_corner_badges, if (cfg.showExtendedHud) View.VISIBLE else View.GONE)

                        views.setTextViewTextSize(R.id.hud_corner_condition, TypedValue.COMPLEX_UNIT_DIP,
                            fitTextSp(condLine, 11.2f * scale, cardWidthDp, bold = true))
                        views.setTextViewTextSize(R.id.hud_corner_temp, TypedValue.COMPLEX_UNIT_DIP,
                            fitTextSp(tempFormatted, 17f * scale, cardWidthDp, bold = true))
                        views.setTextViewTextSize(R.id.hud_corner_meta, TypedValue.COMPLEX_UNIT_DIP,
                            fitTextSp(metaLine, 11.2f * scale, cardWidthDp, bold = false))
                        views.setTextViewTextSize(R.id.hud_corner_loc, TypedValue.COMPLEX_UNIT_DIP,
                            fitTextSp(feelsLine, 10.8f * scale, cardWidthDp, bold = true))
                        views.setTextViewTextSize(R.id.hud_corner_badges, TypedValue.COMPLEX_UNIT_DIP,
                            fitTextSp(badgeLine, 10.2f * scale, cardWidthDp, bold = true))
                    }

                    // Bottom-right theme badge: only shown when there's room beside the ~190dp
                    // control bar, and fitted to the remaining space so the two never overlap.
                    val badgeRoomDp = widgetWidthDp - 4f - 10f - 190f - 10f - 16f - 8f
                    if (badgeRoomDp >= 70f) {
                        views.setViewVisibility(R.id.widget_bottom_badge, View.VISIBLE)
                        views.setTextViewText(R.id.widget_bottom_badge, themeLabel)
                        views.setTextViewTextSize(R.id.widget_bottom_badge, TypedValue.COMPLEX_UNIT_DIP,
                            fitTextSp(themeLabel, 10.5f, badgeRoomDp, bold = false, minSp = 8f))
                    } else {
                        views.setViewVisibility(R.id.widget_bottom_badge, View.GONE)
                    }

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
     * Loads the selected pre-generated 3D isometric cutout bitmap (`day`, `sunset`, or `night`)
     * from the 18 regional, US state, timezone, NPS, and UNESCO landmark dioramas bundled in assets.
     */
    private fun loadDioramaBitmap(context: Context, cfg: WidgetConfig, wx: WeatherTelemetry): Bitmap? {
        val effectiveTime = if (cfg.timeMode != "realtime") cfg.timeMode else (if (wx.isDay) "day" else "night")
        val isNight = effectiveTime == "night"
        val isSunset = effectiveTime == "sunset"

        val validThemes = setOf(
            "yorktown", "jamestown", "washington_dc", "new_york", "san_francisco",
            "chicago_great_lakes", "grand_canyon", "new_orleans", "seattle_pnw",
            "miami_caribbean", "coastal", "london_uk", "paris_europe",
            "rome_mediterranean", "cairo_giza", "tokyo_east_asia", "modern", "alpine"
        )
        val cleanTheme = if (cfg.dioramaTheme in validThemes) cfg.dioramaTheme else "yorktown"
        val prefix = "${cleanTheme}_island"
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
     * Renders a single frame of the 3D isometric scene at the widget's real aspect ratio.
     *
     * The HUD overlay is drawn natively via RemoteViews in `widget_isometric_weather.xml`.
     * Here the island is fitted (aspect-preserving) into the region *below* the HUD
     * ([hudHeightDp]) and above the bottom control bar, then zoomed by `cfg.widgetZoom` and
     * shifted by `cfg.widgetOffsetX`, so the HUD never covers the render and stays fixed when panning.
     *
     * @param widthDp  widget width in dp
     * @param heightDp widget height in dp
     * @param hudHeightDp estimated height of the top HUD (0 for the corner card layout)
     */
    private fun renderIsometricWidgetFrame(
        cfg: WidgetConfig,
        wx: WeatherTelemetry,
        islandBmp: Bitmap?,
        phase: Float,
        widthDp: Float,
        heightDp: Float,
        hudHeightDp: Float
    ): Bitmap {
        val width = 640
        val k = width / widthDp                       // px per dp in this bitmap
        val height = (heightDp * k).roundToInt().coerceIn(240, 1100)
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

        // 3. Fit the 3D island into the free area between the HUD and the bottom control bar.
        // The island art is 1376x768; we preserve that aspect ratio and center it in the area.
        val areaTop = (hudHeightDp + 4f) * k
        val areaBottom = height - 30f * k              // leave most of the ~34dp control bar clear
        val areaH = (areaBottom - areaTop).coerceAtLeast(height * 0.45f)
        val islandAspect = 1376f / 768f
        var iw = width.toFloat()
        var ih = iw / islandAspect
        if (ih > areaH) {
            ih = areaH
            iw = ih * islandAspect
        }
        // widgetOffsetX is stored in legacy 688px-frame units (±180), so scale to this frame width
        val ix = (width - iw) / 2f + cfg.widgetOffsetX / 688f * width
        val iy = areaTop + (areaH - ih) / 2f
        val sI = iw / 1376f                            // sprite scale relative to island art

        // Moon crescent in the night sky, just above-left of the island
        if (isNight) {
            val mx = ix + 0.12f * iw
            val my = (iy + 0.12f * ih).coerceAtLeast(areaTop + 30f * sI)
            paint.color = Color.parseColor("#fff2b2")
            canvas.drawCircle(mx, my, 26f * sI, paint)
            if (!cfg.transparentBg) {
                paint.color = Color.parseColor("#112236")
                canvas.drawCircle(mx - 11f * sI, my - 6f * sI, 22f * sI, paint)
            }
        }

        canvas.save()
        val pivotX = ix + iw * 0.5f
        val pivotY = iy + ih * 0.56f
        canvas.translate(pivotX, pivotY)
        canvas.scale(cfg.widgetZoom, cfg.widgetZoom)
        canvas.translate(-pivotX, -pivotY)

        if (islandBmp != null) {
            canvas.drawBitmap(islandBmp, null, RectF(ix, iy, ix + iw, iy + ih), paint)
        }

        // 3B. Animated water sparkles on the front turquoise water block (normalized island coords)
        val waterPts = arrayOf(
            0.50f to 0.72f, 0.55f to 0.63f, 0.62f to 0.70f, 0.66f to 0.77f,
            0.70f to 0.65f, 0.78f to 0.58f, 0.84f to 0.52f
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.6f * sI
        for (wi in waterPts.indices) {
            val (wpx, wpy) = waterPts[wi]
            val alpha = (110 + (65 * sin((phase * 2.0 * Math.PI) + wi * 1.3)).toInt()).coerceIn(40, 210)
            paint.color = if (isSunset) Color.argb(alpha, 255, 210, 145) else Color.argb(alpha, 230, 248, 255)
            val wxPt = ix + (wpx + 0.005f * sin((phase * 2.0 * Math.PI + wi).toFloat())) * iw
            val wyPt = iy + wpy * ih
            canvas.drawLine(wxPt - 9f * sI, wyPt - 3.5f * sI, wxPt + 9f * sI, wyPt + 3.5f * sI, paint)
        }
        paint.style = Paint.Style.FILL

        // 3C. Static scenic miniature sprites (animated walking people removed)
        if (cfg.dioramaTheme == "yorktown") {
            // Static Sprite 1: Colonial horse & carriage on the brick Main Street
            val hcX = ix + 0.40f * iw
            val hcY = iy + 0.49f * ih
            val hx = hcX + 4.2f * sI
            val hy = hcY - 3.1f * sI
            paint.color = Color.parseColor("#6e4226")
            canvas.drawOval(RectF(hx - 2.2f * sI, hy - 2.6f * sI, hx + 2.2f * sI, hy), paint)
            paint.color = Color.parseColor("#3b2316")
            canvas.drawOval(RectF(hcX - 3.6f * sI, hcY - 4.4f * sI, hcX + 3.6f * sI, hcY - 0.4f * sI), paint)
            paint.color = Color.parseColor("#9e3b2b")
            canvas.drawOval(RectF(hcX - 2.8f * sI, hcY - 5.2f * sI, hcX + 2.8f * sI, hcY - 2.0f * sI), paint)

            // Static Sprites 2 & 3: miniature cars on the Coleman Memorial Bridge deck
            val carX = ix + 0.33f * iw
            val carY = iy + 0.60f * ih
            paint.color = Color.parseColor("#d64035")
            canvas.drawOval(RectF(carX - 2.5f * sI, carY - 2.4f * sI, carX + 2.5f * sI, carY), paint)
            paint.color = Color.parseColor("#f2f6fa")
            canvas.drawOval(RectF(carX - 1.4f * sI, carY - 3.0f * sI, carX + 1.4f * sI, carY - 1.2f * sI), paint)

            val car2X = ix + 0.39f * iw
            val car2Y = iy + 0.64f * ih
            paint.color = Color.parseColor("#2b6cb0")
            canvas.drawOval(RectF(car2X - 2.4f * sI, car2Y - 2.2f * sI, car2X + 2.4f * sI, car2Y), paint)
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

    /**
     * Returns the current widget width in dp. Launchers report a size range: in portrait the
     * widget is laid out at OPTION_APPWIDGET_MIN_WIDTH, so that's the safe value to fit against.
     * Falls back to a 4-column phone width (~300dp) before the launcher has sent options.
     */
    private fun widgetWidthDp(mgr: AppWidgetManager, widgetId: Int): Float {
        val opts = mgr.getAppWidgetOptions(widgetId)
        val minW = opts?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
        val maxW = opts?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0) ?: 0
        val w = when {
            minW > 0 -> minW
            maxW > 0 -> maxW
            else -> 300
        }
        return w.toFloat().coerceAtLeast(160f)
    }

    /**
     * Returns the current widget height in dp. In portrait the widget is laid out at
     * OPTION_APPWIDGET_MAX_HEIGHT. Falls back to a 3-row phone height (~250dp).
     */
    private fun widgetHeightDp(mgr: AppWidgetManager, widgetId: Int): Float {
        val opts = mgr.getAppWidgetOptions(widgetId)
        val maxH = opts?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) ?: 0
        val minH = opts?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
        val h = when {
            maxH > 0 -> maxH
            minH > 0 -> minH
            else -> 250
        }
        return h.toFloat().coerceAtLeast(120f)
    }

    /**
     * Returns the largest text size (in dp, applied with COMPLEX_UNIT_DIP) up to [desired]
     * at which [text] fits on one line within [availDp].
     *
     * Sizes are applied in dp rather than sp so the system font-scale setting can't push
     * text past the edge after we've measured it. Emoji glyphs are measured with the default
     * typeface, and a 6% safety margin covers launcher/OEM font differences (e.g. OnePlus Sans).
     */
    private fun fitTextSp(
        text: String,
        desired: Float,
        availDp: Float,
        bold: Boolean,
        minSp: Float = 8.5f
    ): Float {
        if (availDp <= 0f) return minSp
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = desired
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        val measured = p.measureText(text) * 1.06f
        if (measured <= availDp) return desired
        return (desired * availDp / measured).coerceAtLeast(minSp)
    }

    /** US EPA AQI category label. */
    private fun aqiLabel(aqi: Int): String = when {
        aqi <= 50 -> "GOOD"
        aqi <= 100 -> "MODERATE"
        aqi <= 150 -> "SENSITIVE"
        aqi <= 200 -> "UNHEALTHY"
        aqi <= 300 -> "V.UNHEALTHY"
        else -> "HAZARDOUS"
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
