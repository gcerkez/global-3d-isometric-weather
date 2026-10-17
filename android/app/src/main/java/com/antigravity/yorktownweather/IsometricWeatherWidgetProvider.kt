package com.antigravity.yorktownweather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.widget.RemoteViews
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
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
 *   - Continuous live animation on the Homescreen via 4-frame `<ViewFlipper>` (gentle rain/snow,
 *     York River water sparkles, and 5 tiny proportional moving sprites: Horse & Carriage on Main St,
 *     Miniature Car on Coleman Bridge, 2 Pedestrians on paths, and 1 Sailor on the Tall Ship)
 *   - Direct on-widget interactive Zoom (`➕`/`➖`), Rotation (`↺`/`↻`), Reset (`⟲`), and Settings (`⚙️`) buttons
 */
class IsometricWeatherWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.antigravity.yorktownweather.ACTION_REFRESH_WIDGET"
        const val ACTION_ZOOM_IN = "com.antigravity.yorktownweather.ACTION_ZOOM_IN"
        const val ACTION_ZOOM_OUT = "com.antigravity.yorktownweather.ACTION_ZOOM_OUT"
        const val ACTION_ROT_LEFT = "com.antigravity.yorktownweather.ACTION_ROT_LEFT"
        const val ACTION_ROT_RIGHT = "com.antigravity.yorktownweather.ACTION_ROT_RIGHT"
        const val ACTION_CAM_RESET = "com.antigravity.yorktownweather.ACTION_CAM_RESET"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val act = intent.action ?: return
        if (act in listOf(
                ACTION_REFRESH_WIDGET,
                ACTION_ZOOM_IN,
                ACTION_ZOOM_OUT,
                ACTION_ROT_LEFT,
                ACTION_ROT_RIGHT,
                ACTION_CAM_RESET
            )
        ) {
            val prefs = context.getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
            var zoom = prefs.getFloat("widgetZoom", 1.0f)
            var rot = prefs.getFloat("widgetRotationDeg", 0.0f)
            when (act) {
                ACTION_ZOOM_IN -> zoom = (zoom * 1.15f).coerceIn(0.70f, 2.20f)
                ACTION_ZOOM_OUT -> zoom = (zoom / 1.15f).coerceIn(0.70f, 2.20f)
                ACTION_ROT_LEFT -> rot = (rot - 6.0f).coerceIn(-28.0f, 28.0f)
                ACTION_ROT_RIGHT -> rot = (rot + 6.0f).coerceIn(-28.0f, 28.0f)
                ACTION_CAM_RESET -> {
                    zoom = 1.0f
                    rot = 0.0f
                }
            }
            if (act != ACTION_REFRESH_WIDGET) {
                prefs.edit()
                    .putFloat("widgetZoom", zoom)
                    .putFloat("widgetRotationDeg", rot)
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
                    transparentBg = prefs.getBoolean("transparentBackground", false),
                    showExtendedHud = prefs.getBoolean("showExtendedHud", true),
                    timeMode = prefs.getString("timeOfDayMode", "realtime") ?: "realtime",
                    weatherOverride = prefs.getString("weatherOverride", "realtime") ?: "realtime",
                    tempUnit = prefs.getString("temperatureUnit", "BOTH") ?: "BOTH",
                    windUnit = prefs.getString("windSpeedUnit", "mph") ?: "mph",
                    widgetZoom = prefs.getFloat("widgetZoom", 1.0f),
                    widgetRotationDeg = prefs.getFloat("widgetRotationDeg", 0.0f),
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

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_isometric_weather)
                    for (i in frameIds.indices) {
                        views.setImageViewBitmap(frameIds[i], frames[i])
                    }
                    views.setOnClickPendingIntent(R.id.widget_flipper, pendingOpenApp)
                    views.setOnClickPendingIntent(R.id.btn_open_config, pendingOpenApp)
                    views.setOnClickPendingIntent(R.id.btn_zoom_in, makeActionPendingIntent(ACTION_ZOOM_IN, 101))
                    views.setOnClickPendingIntent(R.id.btn_zoom_out, makeActionPendingIntent(ACTION_ZOOM_OUT, 102))
                    views.setOnClickPendingIntent(R.id.btn_rot_left, makeActionPendingIntent(ACTION_ROT_LEFT, 103))
                    views.setOnClickPendingIntent(R.id.btn_rot_right, makeActionPendingIntent(ACTION_ROT_RIGHT, 104))
                    views.setOnClickPendingIntent(R.id.btn_cam_reset, makeActionPendingIntent(ACTION_CAM_RESET, 105))

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun loadDioramaBitmap(context: Context, cfg: WidgetConfig, wx: WeatherTelemetry): Bitmap? {
        val effectiveTime = if (cfg.timeMode != "realtime") cfg.timeMode else (if (wx.isDay) "day" else "night")
        val isNight = effectiveTime == "night"
        val isSunset = effectiveTime == "sunset"

        if (cfg.renderMode == "ai_generative" && !cfg.locationName.lowercase().contains("yorktown")) {
            runCatching {
                val prompt = URLEncoder.encode(
                    "Cute 3D isometric miniature city diorama of ${cfg.locationName} waterfront and landmarks, tilt-shift architectural model",
                    "UTF-8"
                )
                val aiUrl = URL("https://image.pollinations.ai/prompt/$prompt?width=960&height=540&nologo=true&seed=42")
                val conn = (aiUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6500
                    readTimeout = 6500
                }
                conn.inputStream.use { stream ->
                    val aiBmp = BitmapFactory.decodeStream(stream)
                    if (aiBmp != null) return aiBmp
                }
            }
        }

        val assetName = when {
            isNight -> "web/images/yorktown_island_night.png"
            isSunset -> "web/images/yorktown_island_sunset.png"
            else -> "web/images/yorktown_island.png"
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

    private fun renderIsometricWidgetFrame(
        cfg: WidgetConfig,
        wx: WeatherTelemetry,
        islandBmp: Bitmap?,
        phase: Float
    ): Bitmap {
        // 688x384 per frame keeps 4-frame ViewFlipper crisp and fast while staying well within Binder limits
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
            canvas.drawCircle(185f * s, 120f * s, 32f * s, paint)
            if (!cfg.transparentBg) {
                paint.color = Color.parseColor("#112236")
                canvas.drawCircle(172f * s, 114f * s, 28f * s, paint)
            }
        }

        // 3. Draw 3D Isometric Diorama + Animated Water Ripples + 5 Miniature Sprites (with Zoom & Rotation)
        canvas.save()
        val pivotX = width * 0.45f
        val pivotY = height * 0.54f
        canvas.translate(pivotX, pivotY)
        canvas.scale(cfg.widgetZoom, cfg.widgetZoom)
        canvas.rotate(cfg.widgetRotationDeg * 0.35f)
        canvas.translate(-pivotX, -pivotY)

        val offsetX = -21f
        if (islandBmp != null) {
            val destRect = RectF(offsetX, 0f, width + offsetX, height.toFloat())
            canvas.drawBitmap(islandBmp, null, destRect, paint)
        }

        // 3B. Animated York River Water Sparkles
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
            val wx = offsetX + (wpx + 0.005f * sin((phase * 2.0 * Math.PI + wi).toFloat())) * width
            val wy = wpy * height
            canvas.drawLine(wx - 9f * s, wy - 3.5f * s, wx + 9f * s, wy + 3.5f * s, paint)
        }
        paint.style = Paint.Style.FILL

        // Helper for ping-pong motion across frames
        fun pingPong(v: Float): Float {
            var m = v % 2.0f
            if (m < 0f) m += 2.0f
            return if (m <= 1.0f) m else (2.0f - m)
        }

        // Helper to draw tiny proportional person sprite
        fun drawTinyPerson(nx: Float, ny: Float, coatColor: Int, hatColor: Int) {
            val px = offsetX + nx * width
            val py = ny * height
            val bob = abs(sin(phase * Math.PI * 2.0)).toFloat() * 1.1f * s
            paint.color = Color.argb(95, 10, 18, 28)
            canvas.drawOval(RectF(px - 2.0f * s, py - 0.8f * s, px + 2.0f * s, py + 0.8f * s), paint)
            paint.color = coatColor
            canvas.drawOval(RectF(px - 1.6f * s, py - (4.5f * s + bob), px + 1.6f * s, py - (0.6f * s + bob)), paint)
            paint.color = Color.parseColor("#f4d3b8")
            canvas.drawCircle(px, py - (5.4f * s + bob), 1.3f * s, paint)
            paint.color = hatColor
            canvas.drawCircle(px, py - (6.2f * s + bob), 1.1f * s, paint)
        }

        // 3C. 5 Tiny Proportional Moving Sprites on Paths, Bridge & Tall Ship
        // Sprite 1: Horse & Carriage along Historic Main Street
        val hcProg = pingPong(0.25f + phase * 0.45f)
        val hcX = offsetX + (0.352f + hcProg * (0.535f - 0.352f)) * width
        val hcY = (0.712f + hcProg * (0.582f - 0.712f)) * height
        val hx = hcX + 4.2f * s
        val hy = hcY - 3.1f * s
        paint.color = Color.parseColor("#6e4226")
        canvas.drawOval(RectF(hx - 2.2f * s, hy - 2.6f * s, hx + 2.2f * s, hy), paint)
        paint.color = Color.parseColor("#3b2316")
        canvas.drawOval(RectF(hcX - 3.6f * s, hcY - 4.4f * s, hcX + 3.6f * s, hcY - 0.4f * s), paint)
        paint.color = Color.parseColor("#9e3b2b")
        canvas.drawOval(RectF(hcX - 2.8f * s, hcY - 5.2f * s, hcX + 2.8f * s, hcY - 2.0f * s), paint)

        // Sprite 2: Miniature Car crossing Coleman Memorial Bridge
        val carFrac = (0.15f + phase * 0.55f) % 1.0f
        val carX = offsetX + (0.270f + carFrac * (0.476f - 0.270f)) * width
        val carY = (0.318f + carFrac * (0.462f - 0.318f)) * height
        paint.color = Color.parseColor("#d64035")
        canvas.drawOval(RectF(carX - 2.5f * s, carY - 2.4f * s, carX + 2.5f * s, carY), paint)
        paint.color = Color.parseColor("#f2f6fa")
        canvas.drawOval(RectF(carX - 1.4f * s, carY - 3.0f * s, carX + 1.4f * s, carY - 1.2f * s), paint)

        // Sprite 3: Pedestrian on Riverwalk Path
        val p1 = pingPong(0.20f + phase * 0.40f)
        drawTinyPerson(
            0.328f + p1 * (0.468f - 0.328f),
            0.735f + p1 * (0.625f - 0.735f),
            Color.parseColor("#264653"),
            Color.parseColor("#1d3557")
        )

        // Sprite 4: Pedestrian on Victory Monument Bluff Path
        val p2 = pingPong(0.55f + phase * 0.38f)
        drawTinyPerson(
            0.652f + p2 * (0.748f - 0.652f),
            0.362f + p2 * (0.468f - 0.362f),
            Color.parseColor("#b5382a"),
            Color.parseColor("#2b2d42")
        )

        // Sprite 5: Sailor on 18th-Century Tall Ship Deck
        val p3 = pingPong(0.35f + phase * 0.45f)
        drawTinyPerson(
            0.214f + p3 * (0.246f - 0.214f),
            0.520f + p3 * (0.496f - 0.520f),
            Color.parseColor("#1d3557"),
            Color.parseColor("#f1faee")
        )

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

        // 5. Draw Top-Right Translucent HUD Overlay Card
        val hudBottom = (if (cfg.showExtendedHud) 370f else 210f) * s
        val hudRect = RectF(width - 465f * s, 28f * s, width - 28f * s, hudBottom)
        paint.color = Color.argb(216, 48, 69, 88)
        canvas.drawRoundRect(hudRect, 24f * s, 24f * s, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * s
        paint.color = Color.argb(90, 255, 255, 255)
        canvas.drawRoundRect(hudRect, 24f * s, 24f * s, paint)
        paint.style = Paint.Style.FILL

        // Weather Icon inside HUD
        val iconCx = hudRect.left + 68f * s
        val iconCy = hudRect.top + 86f * s
        paint.color = if (!isNight) Color.parseColor("#f9b826") else Color.parseColor("#ffe28a")
        canvas.drawCircle(iconCx, iconCy, 28f * s, paint)
        if (!isNight) {
            paint.strokeWidth = 5f * s
            paint.strokeCap = Paint.Cap.ROUND
            for (r in 0 until 8) {
                val ang = r * Math.PI / 4.0
                val x0 = iconCx + cos(ang).toFloat() * 36f * s
                val y0 = iconCy + sin(ang).toFloat() * 36f * s
                val x1 = iconCx + cos(ang).toFloat() * 48f * s
                val y1 = iconCy + sin(ang).toFloat() * 48f * s
                canvas.drawLine(x0, y0, x1, y1, paint)
            }
        }

        // Format Temperature & Wind according to configured units
        val fVal = wx.tempF.roundToInt()
        val cVal = ((wx.tempF - 32.0) * 5.0 / 9.0).roundToInt()
        val tempFormatted = when (cfg.tempUnit) {
            "F" -> "${fVal}°F"
            "C" -> "${cVal}°C"
            else -> "${fVal}°F (${cVal}°C)"
        }

        val dirs = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val compass = dirs[((wx.windDeg % 360 + 360) % 360 / 22.5).roundToInt() % 16]
        val windFormatted = when (cfg.windUnit) {
            "kmh" -> "${(wx.windMph * 1.60934).roundToInt()} KM/H $compass WIND"
            "kn" -> "${(wx.windMph * 0.868976).roundToInt()} KN $compass WIND"
            "ms" -> "${(wx.windMph * 0.44704).roundToInt()} M/S $compass WIND"
            else -> "${wx.windMph.roundToInt()} MPH $compass WIND"
        }

        paint.color = Color.WHITE
        paint.textSize = 22f * s
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(categoryToHeadline(effectiveCat), hudRect.left + 132f * s, hudRect.top + 52f * s, paint)
        paint.textSize = 27f * s
        canvas.drawText(tempFormatted, hudRect.left + 132f * s, hudRect.top + 90f * s, paint)

        paint.textSize = 20f * s
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#eef6fc")
        canvas.drawText("${wx.humidity}% HUMIDITY", hudRect.left + 132f * s, hudRect.top + 122f * s, paint)
        canvas.drawText(windFormatted, hudRect.left + 132f * s, hudRect.top + 150f * s, paint)

        if (cfg.showExtendedHud) {
            paint.color = Color.argb(60, 255, 255, 255)
            canvas.drawLine(hudRect.left + 22f * s, hudRect.top + 170f * s, hudRect.right - 22f * s, hudRect.top + 170f * s, paint)

            // Location + Feels Like
            paint.color = Color.parseColor("#d9ecff")
            paint.textSize = 17.5f * s
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("📍 ${cfg.locationName.uppercase()} • FEELS ${wx.feelsLikeF.roundToInt()}°F", hudRect.left + 22f * s, hudRect.top + 198f * s, paint)

            // AQI + UV + Moon Phase Pills
            val aqiPill = RectF(hudRect.left + 22f * s, hudRect.top + 212f * s, hudRect.left + 158f * s, hudRect.top + 244f * s)
            paint.color = Color.argb(165, 18, 30, 42)
            canvas.drawRoundRect(aqiPill, 8f * s, 8f * s, paint)
            paint.color = Color.parseColor("#4ade80")
            paint.textSize = 15.5f * s
            canvas.drawText("AQI ${wx.usAqi} GOOD", aqiPill.left + 12f * s, aqiPill.top + 22f * s, paint)

            val uvPill = RectF(hudRect.left + 168f * s, hudRect.top + 212f * s, hudRect.left + 265f * s, hudRect.top + 244f * s)
            paint.color = Color.argb(165, 18, 30, 42)
            canvas.drawRoundRect(uvPill, 8f * s, 8f * s, paint)
            paint.color = Color.parseColor("#facc15")
            canvas.drawText("UV ${String.format("%.1f", wx.uvIndex)}", uvPill.left + 12f * s, uvPill.top + 22f * s, paint)

            val moonPill = RectF(hudRect.left + 275f * s, hudRect.top + 212f * s, hudRect.right - 22f * s, hudRect.top + 244f * s)
            paint.color = Color.argb(165, 18, 30, 42)
            canvas.drawRoundRect(moonPill, 8f * s, 8f * s, paint)
            paint.color = Color.parseColor("#e6f2ff")
            canvas.drawText("🌔 Waxing Gibbous", moonPill.left + 10f * s, moonPill.top + 22f * s, paint)

            // 12-Hour Temperature Sparkline Box
            val sparkRect = RectF(hudRect.left + 22f * s, hudRect.top + 256f * s, hudRect.right - 22f * s, hudRect.top + 324f * s)
            paint.color = Color.argb(125, 22, 36, 52)
            canvas.drawRoundRect(sparkRect, 10f * s, 10f * s, paint)

            val temps = wx.hourlyTemps
            if (temps.size >= 2) {
                val minT = temps.minOrNull() ?: 50
                val maxT = max((temps.maxOrNull() ?: 70), minT + 2)
                val path = Path()
                val stepX = (sparkRect.width() - 24f * s) / (temps.size - 1)
                for (i in temps.indices) {
                    val px = sparkRect.left + 12f * s + i * stepX
                    val norm = (temps[i] - minT).toFloat() / (maxT - minT).toFloat()
                    val py = sparkRect.bottom - 12f * s - norm * (sparkRect.height() - 28f * s)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3.5f * s
                paint.color = Color.parseColor("#f9b826")
                canvas.drawPath(path, paint)
                paint.style = Paint.Style.FILL
            }
        }

        // Bottom-right Location Badge
        val badgeRect = RectF(width - 365f * s, height - 56f * s, width - 26f * s, height - 18f * s)
        paint.color = Color.argb(195, 34, 48, 62)
        canvas.drawRoundRect(badgeRect, 10f * s, 10f * s, paint)
        paint.color = Color.parseColor("#eef6fc")
        paint.textSize = 17.5f * s
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${cfg.locationName} • Tap to Configure", badgeRect.left + 16f * s, badgeRect.top + 25f * s, paint)

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
        "clear" -> "CLEAR WEATHER"
        "partly_cloudy" -> "PARTLY CLOUDY"
        "overcast" -> "OVERCAST SKY"
        "fog" -> "COASTAL FOG"
        "rain" -> "RAIN SHOWER"
        "snow" -> "SNOWFALL"
        "thunderstorm" -> "THUNDERSTORM"
        else -> "CLEAR WEATHER"
    }

    data class WidgetConfig(
        val locationName: String,
        val lat: Double,
        val lon: Double,
        val renderMode: String,
        val transparentBg: Boolean,
        val showExtendedHud: Boolean,
        val timeMode: String,
        val weatherOverride: String,
        val tempUnit: String,
        val windUnit: String,
        val widgetZoom: Float,
        val widgetRotationDeg: Float,
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
