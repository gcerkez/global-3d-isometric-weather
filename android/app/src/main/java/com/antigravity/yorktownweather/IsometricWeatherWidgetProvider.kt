package com.antigravity.yorktownweather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.widget.RemoteViews
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * IsometricWeatherWidgetProvider
 * ==============================
 * Native Android Homescreen & Lockscreen (`home_screen|keyguard`) AppWidgetProvider
 * designed for OnePlus 13 (OxygenOS 15) and Android 8.0+ devices.
 *
 * Composites the 3D Isometric Diorama (`yorktown_island.png` / `_sunset.png` / `_night.png`),
 * dynamic sky gradient, weather particles (rain, snow, clouds, stars, lunar phase), and the
 * San Francisco reference-style HUD card onto a hardware-backed Bitmap for RemoteViews.
 */
class IsometricWeatherWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.antigravity.yorktownweather.ACTION_REFRESH_WIDGET") {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, IsometricWeatherWidgetProvider::class.java))
            onUpdate(context, mgr, ids)
        }
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
                val locationName = prefs.getString("locationName", "Yorktown, VA") ?: "Yorktown, VA"
                val lat = prefs.getFloat("latitude", 37.2388f).toDouble()
                val lon = prefs.getFloat("longitude", -76.5097f).toDouble()

                val telemetry = fetchOpenMeteoTelemetry(lat, lon)
                val renderedBitmap = renderIsometricWidgetBitmap(context, locationName, telemetry)

                val launchIntent = Intent(context, MainActivity::class.java)
                val pendingClick = PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_isometric_weather)
                    views.setImageViewBitmap(R.id.widget_canvas_image, renderedBitmap)
                    views.setOnClickPendingIntent(R.id.widget_canvas_image, pendingClick)
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Queries Open-Meteo Forecast & Air Quality APIs for current weather, UV, and US EPA AQI.
     */
    private fun fetchOpenMeteoTelemetry(lat: Double, lon: Double): WeatherTelemetry {
        val urlStr = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,wind_direction_10m" +
            "&temperature_unit=fahrenheit&wind_speed_unit=mph&timezone=auto"
        var tempF = 68.0
        var feelsF = 67.0
        var hum = 64
        var wind = 8.0
        var windDeg = 25
        var code = 0
        var isDay = true

        runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(body).getJSONObject("current")
            tempF = current.getDouble("temperature_2m")
            feelsF = current.getDouble("apparent_temperature")
            hum = current.getDouble("relative_humidity_2m").roundToInt()
            wind = current.getDouble("wind_speed_10m")
            windDeg = current.optInt("wind_direction_10m", 25)
            code = current.getInt("weather_code")
            isDay = current.getInt("is_day") == 1
        }

        var aqi = 24
        runCatching {
            val aqUrl = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&current=us_aqi"
            val conn = (URL(aqUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val cur = JSONObject(body).optJSONObject("current")
            if (cur != null && !cur.isNull("us_aqi")) {
                aqi = cur.getDouble("us_aqi").roundToInt()
            }
        }

        return WeatherTelemetry(tempF, feelsF, hum, wind, windDeg, code, isDay, aqi)
    }

    /**
     * Composites the sky gradient, 3D isometric diorama cutout, weather particles,
     * and top-right HUD card onto a crisp 1376x768 ARGB_8888 Bitmap for RemoteViews.
     */
    private fun renderIsometricWidgetBitmap(
        context: Context,
        locationName: String,
        wx: WeatherTelemetry
    ): Bitmap {
        val width = 1376
        val height = 768
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 1. Rounded Sky Card Gradient
        val isStorm = wx.weatherCode >= 51
        val topColor = when {
            !wx.isDay -> Color.parseColor("#0d1b2a")
            isStorm -> Color.parseColor("#4a6278")
            else -> Color.parseColor("#7fc4f2")
        }
        val botColor = when {
            !wx.isDay -> Color.parseColor("#1b314c")
            isStorm -> Color.parseColor("#7b92a8")
            else -> Color.parseColor("#a8dbf8")
        }
        paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), topColor, botColor, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 38f, 38f, paint)
        paint.shader = null

        // Subtle border around sky card
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = if (!wx.isDay) Color.parseColor("#3b526b") else Color.parseColor("#bce0f7")
        canvas.drawRoundRect(RectF(1.5f, 1.5f, width - 1.5f, height - 1.5f), 38f, 38f, paint)
        paint.style = Paint.Style.FILL

        // 2. Load & Draw 3D Isometric Cutout Asset
        val assetName = if (!wx.isDay) "web/images/yorktown_island_night.png" else "web/images/yorktown_island.png"
        runCatching {
            context.assets.open(assetName).use { stream ->
                val islandBmp = BitmapFactory.decodeStream(stream)
                if (islandBmp != null) {
                    val destRect = RectF(-42f, 0f, width - 42f, height.toFloat())
                    canvas.drawBitmap(islandBmp, null, destRect, paint)
                }
            }
        }

        // 3. Draw Weather Particles (Rain / Snow) if precipitating
        if (wx.weatherCode in 51..67 || wx.weatherCode in 80..99) {
            paint.color = Color.argb(145, 200, 232, 255)
            paint.strokeWidth = 2.6f
            for (i in 0 until 55) {
                val rx = ((i * 73) % width).toFloat()
                val ry = ((i * 47) % height).toFloat()
                canvas.drawLine(rx, ry, rx - 10f, ry + 32f, paint)
            }
        } else if (wx.weatherCode in 71..77 || wx.weatherCode in 85..86) {
            paint.color = Color.argb(215, 255, 255, 255)
            for (i in 0 until 45) {
                val sx = ((i * 79) % width).toFloat()
                val sy = ((i * 53) % height).toFloat()
                canvas.drawCircle(sx, sy, 4.5f, paint)
            }
        }

        // 4. Draw Top-Right Translucent HUD Overlay Card (matching San Francisco reference)
        val hudRect = RectF(width - 455f, 30f, width - 30f, 275f)
        paint.color = Color.argb(212, 52, 74, 94)
        canvas.drawRoundRect(hudRect, 24f, 24f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.argb(85, 255, 255, 255)
        canvas.drawRoundRect(hudRect, 24f, 24f, paint)
        paint.style = Paint.Style.FILL

        // Weather Icon inside HUD (Sun or Moon + Cloud)
        val iconCx = hudRect.left + 68f
        val iconCy = hudRect.top + 88f
        paint.color = if (wx.isDay) Color.parseColor("#f9b826") else Color.parseColor("#ffe28a")
        canvas.drawCircle(iconCx, iconCy, 28f, paint)
        if (wx.isDay) {
            paint.strokeWidth = 5f
            paint.strokeCap = Paint.Cap.ROUND
            for (r in 0 until 8) {
                val ang = r * Math.PI / 4.0
                val x0 = iconCx + cos(ang).toFloat() * 36f
                val y0 = iconCy + sin(ang).toFloat() * 36f
                val x1 = iconCx + cos(ang).toFloat() * 48f
                val y1 = iconCy + sin(ang).toFloat() * 48f
                canvas.drawLine(x0, y0, x1, y1, paint)
            }
        }

        // HUD Text Lines
        paint.color = Color.WHITE
        paint.textSize = 23f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val condLabel = wmoToLabel(wx.weatherCode)
        val tempC = ((wx.tempF - 32.0) * 5.0 / 9.0).roundToInt()
        val dirs = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val compass = dirs[((wx.windDeg % 360 + 360) % 360 / 22.5).roundToInt() % 16]

        canvas.drawText(condLabel, hudRect.left + 132f, hudRect.top + 54f, paint)
        paint.textSize = 28f
        canvas.drawText("${wx.tempF.roundToInt()}°F (${tempC}°C)", hudRect.left + 132f, hudRect.top + 92f, paint)

        paint.textSize = 21f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#eef6fc")
        canvas.drawText("${wx.humidity}% HUMIDITY", hudRect.left + 132f, hudRect.top + 124f, paint)
        canvas.drawText("${wx.windMph.roundToInt()} MPH $compass WIND", hudRect.left + 132f, hudRect.top + 154f, paint)

        // Divider
        paint.color = Color.argb(60, 255, 255, 255)
        canvas.drawLine(hudRect.left + 22f, hudRect.top + 174f, hudRect.right - 22f, hudRect.top + 174f, paint)

        // Location & AQI Footer inside HUD
        paint.color = Color.parseColor("#ffd166")
        paint.textSize = 18.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("📍 ${locationName.uppercase()} • FEELS ${wx.feelsLikeF.roundToInt()}°F • AQI ${wx.usAqi}", hudRect.left + 22f, hudRect.top + 206f, paint)

        // Bottom-right Location Badge
        val badgeRect = RectF(width - 340f, height - 56f, width - 26f, height - 18f)
        paint.color = Color.argb(190, 34, 48, 62)
        canvas.drawRoundRect(badgeRect, 10f, 10f, paint)
        paint.color = Color.parseColor("#eef6fc")
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("$locationName • 3D Isometric", badgeRect.left + 18f, badgeRect.top + 25f, paint)

        return bmp
    }

    private fun wmoToLabel(code: Int): String = when (code) {
        0 -> "CLEAR WEATHER"
        1, 2 -> "PARTLY CLOUDY"
        3 -> "OVERCAST SKY"
        45, 48 -> "COASTAL FOG"
        in 51..67, in 80..82 -> "RAIN SHOWER"
        in 71..77, 85, 86 -> "SNOWFALL"
        in 95..99 -> "THUNDERSTORM"
        else -> "CLEAR WEATHER"
    }

    data class WeatherTelemetry(
        val tempF: Double,
        val feelsLikeF: Double,
        val humidity: Int,
        val windMph: Double,
        val windDeg: Int,
        val weatherCode: Int,
        val isDay: Boolean,
        val usAqi: Int
    )
}
