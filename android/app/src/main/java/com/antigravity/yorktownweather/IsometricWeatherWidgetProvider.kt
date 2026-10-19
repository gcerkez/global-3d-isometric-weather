package com.antigravity.yorktownweather

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.os.SystemClock
import android.provider.AlarmClock
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
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

        fun aqiLabel(aqi: Int): String = when {
            aqi <= 50 -> "GOOD"
            aqi <= 100 -> "MODERATE"
            aqi <= 150 -> "SENSITIVE"
            aqi <= 200 -> "UNHEALTHY"
            aqi <= 300 -> "V.UNHEALTHY"
            else -> "HAZARDOUS"
        }

        fun wmoToCategory(code: Int): String = when (code) {
            0 -> "clear"
            1, 2 -> "partly_cloudy"
            3 -> "overcast"
            45, 48 -> "fog"
            in 51..67, in 80..82 -> "rain"
            in 71..77, 85, 86 -> "snow"
            in 95..99 -> "thunderstorm"
            else -> "clear"
        }

        fun categoryToHeadline(cat: String): String = when (cat) {
            "clear" -> "CLEAR"
            "partly_cloudy" -> "PARTLY CLOUDY"
            "overcast" -> "OVERCAST"
            "fog" -> "COASTAL FOG"
            "rain" -> "RAIN SHOWER"
            "snow" -> "SNOWFALL"
            "thunderstorm" -> "THUNDERSTORM"
            else -> "CLEAR"
        }

        fun categoryToEmoji(cat: String, isNight: Boolean): String = when (cat) {
            "clear" -> if (isNight) "🌙" else "☀️"
            "partly_cloudy" -> if (isNight) "☁️" else "⛅"
            "overcast" -> "☁️"
            "fog" -> "🌫️"
            "rain" -> "🌧️"
            "snow" -> "❄️"
            "thunderstorm" -> "⛈️"
            else -> if (isNight) "🌙" else "☀️"
        }

        fun windDegreesToCompass(deg: Int): String {
            val dirs = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
            return dirs[((deg % 360 + 360) % 360 / 22.5).roundToInt() % 16]
        }

        fun getAlarmClockIntent(context: Context): Intent {
            val pm = context.packageManager
            val clockPackages = listOf(
                "com.google.android.deskclock",
                "com.oneplus.deskclock",
                "com.coloros.alarmclock",
                "com.sec.android.app.clockpackage",
                "com.android.deskclock",
                "com.xiaomi.alarm"
            )
            for (pkg in clockPackages) {
                try {
                    val launch = pm.getLaunchIntentForPackage(pkg)
                    if (launch != null) {
                        launch.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        return launch
                    }
                } catch (_: Exception) {}
            }
            return Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
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
                ACTION_REFRESH_WIDGET -> {
                    Toast.makeText(context, "Refreshing weather & location...", Toast.LENGTH_SHORT).show()
                    prefs.edit().putLong("lastLocationCheckTime", 0L).apply()
                }
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

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        schedulePeriodicUpdates(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelPeriodicUpdates(context)
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
                schedulePeriodicUpdates(context)
                val prefs = context.getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)

                // Auto-detect phone location via GPS/Network if enabled
                var locName = prefs.getString("locationName", "Yorktown, VA") ?: "Yorktown, VA"
                var lat = prefs.getFloat("latitude", 37.2388f).toDouble()
                var lon = prefs.getFloat("longitude", -76.5097f).toDouble()

                if (prefs.getBoolean("useCurrentLocation", false)) {
                    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    val isLocked = km?.isDeviceLocked == true
                    // When device is locked, Android throttles/suspends background GPS hardware.
                    // Never fall back to cellular IP gateway (which points to Washington D.C.) while locked.
                    val coords = if (!isLocked) {
                        DeviceLocationManager.getBestDeviceLocation(context, allowIpFallback = false)
                    } else null

                    if (coords != null) {
                        val newLat = coords.first
                        val newLon = coords.second
                        val dLat = Math.abs(newLat - lat)
                        val dLon = Math.abs(newLon - lon)
                        val lastCheck = prefs.getLong("lastLocationCheckTime", 0L)
                        val now = System.currentTimeMillis()
                        if (dLat > 0.008 || dLon > 0.008 || locName.isEmpty() || (now - lastCheck) > 30 * 60 * 1000L) {
                            val newName = DeviceLocationManager.reverseGeocode(context, newLat, newLon)
                            locName = newName
                            lat = newLat
                            lon = newLon
                            prefs.edit()
                                .putString("locationName", locName)
                                .putFloat("latitude", lat.toFloat())
                                .putFloat("longitude", lon.toFloat())
                                .putLong("lastLocationCheckTime", now)
                                .apply()
                        } else {
                            lat = newLat
                            lon = newLon
                        }
                    }
                }

                var dioramaTheme = prefs.getString("dioramaTheme", "yorktown") ?: "yorktown"
                if (prefs.getBoolean("autoMatchDiorama", false)) {
                    val closest = DeviceLocationManager.findClosestDiorama(lat, lon)
                    dioramaTheme = closest.first.id
                }

                val cfg = WidgetConfig(
                    locationName = locName,
                    lat = lat,
                    lon = lon,
                    renderMode = prefs.getString("renderMode", "hybrid") ?: "hybrid",
                    dioramaTheme = dioramaTheme,
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

                // 7-day & hourly detailed forecast URL for the active telemetry coordinates
                val forecastUrl = if (cfg.lat >= 18.0 && cfg.lat <= 72.0 && cfg.lon >= -170.0 && cfg.lon <= -65.0) {
                    "https://forecast.weather.gov/MapClick.php?lat=${cfg.lat}&lon=${cfg.lon}"
                } else {
                    "https://weather.com/weather/today/l/${cfg.lat},${cfg.lon}"
                }
                val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(forecastUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val pendingOpenForecast = PendingIntent.getActivity(
                    context,
                    201,
                    browserIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

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

                val compass = windDegreesToCompass(telemetry.windDeg)
                val windFormatted = when (cfg.windUnit) {
                    "kmh" -> "${(telemetry.windMph * 1.60934).roundToInt()} KM/H $compass"
                    "kn" -> "${(telemetry.windMph * 0.868976).roundToInt()} KN $compass"
                    "ms" -> "${(telemetry.windMph * 0.44704).roundToInt()} M/S $compass"
                    else -> "${telemetry.windMph.roundToInt()} MPH $compass"
                }

                val themeLabel = when (cfg.dioramaTheme) {
                    "jamestown" -> "Historic Jamestown 3D"
                    "virginia_beach" -> "Virginia Beach 3D"
                    "norfolk" -> "Norfolk, VA 3D"
                    "chesapeake_bay" -> "Chesapeake Bay 3D"
                    "washington_dc" -> "Washington, D.C. 3D"
                    "new_york" -> "New York City 3D"
                    "myrtle_beach" -> "Myrtle Beach 3D"
                    "atlanta" -> "Atlanta & Piedmont 3D"
                    "orlando" -> "Orlando Theme Parks 3D"
                    "miami_caribbean" -> "Miami & Keys 3D"
                    "new_orleans" -> "New Orleans 3D"
                    "texas" -> "Texas & Austin 3D"
                    "chicago_great_lakes" -> "Chicago & Great Lakes 3D"
                    "denver" -> "Denver Mile High 3D"
                    "colorado_springs" -> "Colorado Springs 3D"
                    "arizona" -> "Arizona Sonoran 3D"
                    "grand_canyon" -> "Grand Canyon & SW 3D"
                    "las_vegas" -> "Las Vegas Strip 3D"
                    "san_francisco" -> "San Francisco 3D"
                    "seattle_pnw" -> "Seattle & PNW 3D"
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
                    val hudHeightDp = 2f + 6f + 11f + 17f * scale * 1.22f + 2f + 2f * 11.5f * scale * 1.3f + 1f +
                        (if (cfg.showExtendedHud) 1f + 11f * scale * 1.3f else 0f)

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

                    views.setViewVisibility(R.id.widget_hud_top_bar, View.VISIBLE)

                    // Bar spans the widget: 2dp root padding + 6dp margins + 10dp padding per side
                    val rowWidthDp = widgetWidthDp - 2 * (2f + 6f + 10f)
                    val tempLine = "$wxEmoji $tempFormatted"

                    views.setTextViewText(R.id.hud_bar_temp, tempLine)
                    views.setTextViewText(R.id.hud_bar_condition, headline)
                    views.setTextViewText(R.id.hud_bar_condition_loc, feelsLine)
                    views.setTextViewText(R.id.hud_bar_wind_hum, metaLine)
                    views.setTextViewText(R.id.hud_bar_badges, badgeLine)
                    views.setViewVisibility(R.id.hud_bar_badges, if (cfg.showExtendedHud) View.VISIBLE else View.GONE)

                    // Row 1 shares its width: temperature gets ~58%, the clock and headline the rest
                    views.setTextViewTextSize(R.id.hud_bar_temp, TypedValue.COMPLEX_UNIT_DIP,
                        fitTextSp(tempLine, 17f * scale, rowWidthDp * 0.58f, bold = true))
                    views.setTextViewTextSize(R.id.hud_bar_clock, TypedValue.COMPLEX_UNIT_DIP, 11.5f * scale)
                    views.setTextViewTextSize(R.id.hud_bar_condition, TypedValue.COMPLEX_UNIT_DIP,
                        fitTextSp(headline, 12f * scale, rowWidthDp * 0.40f - 55f, bold = true))
                    views.setTextViewTextSize(R.id.hud_bar_condition_loc, TypedValue.COMPLEX_UNIT_DIP,
                        fitTextSp(feelsLine, 11.5f * scale, rowWidthDp, bold = true))
                    views.setTextViewTextSize(R.id.hud_bar_wind_hum, TypedValue.COMPLEX_UNIT_DIP,
                        fitTextSp(metaLine, 11.5f * scale, rowWidthDp, bold = false))
                    views.setTextViewTextSize(R.id.hud_bar_badges, TypedValue.COMPLEX_UNIT_DIP,
                        fitTextSp(badgeLine, 11f * scale, rowWidthDp, bold = true))

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

                    // Interactive tap targets:
                    // 1. Clock tap opens system Alarm / Clock app
                    val clockIntent = getAlarmClockIntent(context)
                    val pendingClock = PendingIntent.getActivity(
                        context, 102, clockIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.hud_bar_clock, pendingClock)
                    views.setOnClickPendingIntent(R.id.hud_bar_clock_container, pendingClock)

                    // 2. HUD weather elements / rows tap opens detailed 7-day forecast
                    views.setOnClickPendingIntent(R.id.hud_bar_temp, pendingOpenForecast)
                    views.setOnClickPendingIntent(R.id.hud_bar_condition, pendingOpenForecast)
                    views.setOnClickPendingIntent(R.id.hud_bar_condition_loc, pendingOpenForecast)
                    views.setOnClickPendingIntent(R.id.hud_bar_wind_hum, pendingOpenForecast)
                    views.setOnClickPendingIntent(R.id.hud_bar_badges, pendingOpenForecast)

                    // 3. Tapping the 3D scene opens the customization & settings app
                    views.setOnClickPendingIntent(R.id.widget_flipper, pendingOpenApp)
                    views.setOnClickPendingIntent(R.id.widget_bottom_badge, pendingOpenApp)

                    // 4. Subtle bottom-right transparent reload/refresh button triggers immediate manual refresh
                    val refreshIntent = Intent(context, IsometricWeatherWidgetProvider::class.java).apply {
                        action = ACTION_REFRESH_WIDGET
                    }
                    val pendingRefresh = PendingIntent.getBroadcast(
                        context, 103, refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_widget_refresh, pendingRefresh)

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
            "yorktown", "jamestown", "virginia_beach", "norfolk", "chesapeake_bay",
            "washington_dc", "new_york", "myrtle_beach", "atlanta", "orlando",
            "miami_caribbean", "new_orleans", "texas", "chicago_great_lakes",
            "denver", "colorado_springs", "arizona", "grand_canyon", "las_vegas",
            "san_francisco", "seattle_pnw", "coastal", "london_uk", "paris_europe",
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
    internal fun renderIsometricWidgetFrame(
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

        canvas.restore()

        // 4. Atmospheric Weather FX (Lightning, Fog, Cloud Shadows, Rain & Snow) across the WHOLE rendered area
        val density = cfg.particleDensity.coerceIn(0.15f, 1.5f)
        val fullWidth = width.toFloat()
        val fullHeight = height.toFloat()

        // 3D Drifting Volumetric Puffy Clouds & Ground Shadows for cloudy/rain/storm/snow weather
        val isCloudy = (effectiveCat == "partly_cloudy" || effectiveCat == "overcast" ||
                effectiveCat == "rain" || effectiveCat == "thunderstorm" || effectiveCat == "snow")
        if (isCloudy) {
            val cloudCount = if (effectiveCat == "partly_cloudy" || effectiveCat == "snow") 3 else 4
            val isStormy = (effectiveCat == "rain" || effectiveCat == "thunderstorm" || effectiveCat == "overcast")
            val sUnit = width / 640f

            // 4A. Ground shadows cast by clouds onto the island terrain/water
            paint.style = Paint.Style.FILL
            paint.color = if (isNight) Color.argb(16, 0, 0, 0) else Color.argb(24, 15, 35, 60)
            for (ci in 0 until cloudCount) {
                val cProg = ((ci * 0.31f + phase * 0.18f) % 1.0f)
                val cloudX = (cProg * 1.30f - 0.15f) * fullWidth
                // Ground shadow tracks under the cloud when positioned across island
                if (cloudX >= ix + 0.05f * iw && cloudX <= ix + 0.95f * iw) {
                    val shadowX = cloudX - 10f * s
                    val shadowY = iy + (0.28f + (ci * 0.15f) % 0.38f) * ih
                    val cw = (65f + ci * 20f) * s
                    val ch = cw * 0.38f
                    canvas.drawOval(RectF(shadowX - cw, shadowY - ch, shadowX + cw, shadowY + ch), paint)
                }
            }

            // 4B. 3D Volumetric Puffy Clouds in the sky render area
            // Guaranteed placement in visible sky region between HUD and island peaks so they are ALWAYS visible
            for (ci in 0 until cloudCount) {
                val cProg = ((ci * 0.31f + phase * 0.18f) % 1.0f)
                val cloudX = (cProg * 1.30f - 0.15f) * fullWidth
                // Float below HUD card: areaTop + 10..46dp * k, guaranteed visible in standard/farthest-out view
                val cloudY = areaTop + (10f + (ci * 16f) % 36f) * k
                val cloudScale = (0.80f + (ci % 3) * 0.18f) * (if (isStormy) 1.15f else 1.0f)
                drawIsometricPuffyCloud(canvas, cloudX, cloudY, cloudScale, sUnit, isStormy, isSunset, isNight)
            }
        }

        // Volumetric rolling fog / mist banks across the lower island and water
        if (effectiveCat == "fog") {
            paint.style = Paint.Style.FILL
            for (fg in 0 until 4) {
                val fogProg = ((fg * 0.28f + phase * 0.35f) % 1.0f)
                val fogX = (fogProg * 1.3f - 0.15f) * fullWidth
                val fogY = iy + (0.35f + (fg * 0.14f) % 0.45f) * ih
                val fogW = (110f + fg * 25f) * s
                val fogH = (22f + fg * 6f) * s
                paint.color = Color.argb((45 + fg * 8), 225, 238, 248)
                canvas.drawOval(RectF(fogX - fogW, fogY - fogH, fogX + fogW, fogY + fogH), paint)
            }
        }

        // Thunderstorm lightning bolt & ambient sky flash (fires on frame 2 of 4)
        if (effectiveCat == "thunderstorm" && (phase * 4.0f).roundToInt() % 4 == 2) {
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(55, 235, 245, 255)
            canvas.drawRect(0f, 0f, fullWidth, fullHeight, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = 2.4f * s
            paint.color = Color.argb(245, 255, 255, 255)
            val bolt = Path().apply {
                val bx = ix + 0.36f * iw
                val by = areaTop + 10f * s
                moveTo(bx, by)
                lineTo(bx + 14f * s, by + 30f * s)
                lineTo(bx - 6f * s, by + 56f * s)
                lineTo(bx + 18f * s, by + 88f * s)
                lineTo(bx + 4f * s, by + 122f * s)
                moveTo(bx - 6f * s, by + 56f * s)
                lineTo(bx - 22f * s, by + 80f * s)
            }
            canvas.drawPath(bolt, paint)
            paint.strokeWidth = 4.5f * s
            paint.color = Color.argb(85, 185, 225, 255) // soft electric blue halo
            canvas.drawPath(bolt, paint)
            paint.style = Paint.Style.FILL
        }

        if (effectiveCat == "rain" || effectiveCat == "thunderstorm") {
            val dropCount = max(14, ((if (effectiveCat == "thunderstorm") 48 else 28) * density).roundToInt())
            paint.color = Color.argb(150, 215, 240, 255)
            paint.strokeWidth = 1.4f * s
            for (i in 0 until dropCount) {
                val prog = ((i * 0.173f) + phase * 0.32f) % 1.0f
                val seedX = ((i * 0.6180339f + 0.12f) % 1.0f)
                val rx = seedX * fullWidth - prog * 0.08f * fullWidth
                val ry = prog * (fullHeight + 20f) - 10f
                canvas.drawLine(rx, ry, rx - 4.5f * s, ry + 16f * s, paint)
            }

            // Animated water / ground splash ripples on the island's land & cutaway water surfaces
            val splashCount = max(4, (8 * density).roundToInt())
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.1f * s
            paint.color = Color.argb(130, 225, 245, 255)
            for (j in 0 until splashCount) {
                val spProg = ((j * 0.283f) + phase * 0.50f) % 1.0f
                val spX = ix + (0.22f + ((j * 0.17f) % 0.56f)) * iw
                val spY = iy + (0.32f + ((j * 0.15f) % 0.44f)) * ih
                val radX = (4.0f + spProg * 8.0f) * s
                val radY = radX * 0.48f // isometric elliptical perspective
                canvas.drawOval(RectF(spX - radX, spY - radY, spX + radX, spY + radY), paint)
            }
            paint.style = Paint.Style.FILL
        } else if (effectiveCat == "snow") {
            val flakeCount = max(12, (38 * density).roundToInt())
            paint.color = Color.argb(230, 255, 255, 255)
            for (i in 0 until flakeCount) {
                val prog = ((i * 0.137f) + phase * 0.22f) % 1.0f
                val seedX = ((i * 0.6180339f + 0.08f) % 1.0f)
                val rx = seedX * fullWidth + sin((phase * 2.0 * Math.PI + i).toFloat()) * 8f * s
                val ry = prog * (fullHeight + 20f) - 10f
                val r = (1.8f + (i % 3) * 1.0f) * s
                canvas.drawCircle(rx, ry, r, paint)
            }
        }

        return bmp
    }

    /**
     * Renders a multi-puff 3D shaded volumetric cloud with ambient underside,
     * mid-tone body, and sunlit top highlights matching the diorama clay/voxel style.
     */
    internal fun drawIsometricPuffyCloud(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        cloudScale: Float,
        s: Float,
        isStormy: Boolean,
        isSunset: Boolean,
        isNight: Boolean
    ) {
        val baseColor = when {
            isStormy -> if (isNight) Color.parseColor("#243242") else Color.parseColor("#5b7083")
            isSunset -> Color.parseColor("#9e7b96")
            isNight -> Color.parseColor("#334860")
            else -> Color.parseColor("#c2d4e6")
        }
        val midColor = when {
            isStormy -> if (isNight) Color.parseColor("#334559") else Color.parseColor("#7b90a5")
            isSunset -> Color.parseColor("#e6a58e")
            isNight -> Color.parseColor("#4d6782")
            else -> Color.parseColor("#e4eef7")
        }
        val topColor = when {
            isStormy -> if (isNight) Color.parseColor("#485e75") else Color.parseColor("#9cb0c4")
            isSunset -> Color.parseColor("#ffe0b5")
            isNight -> Color.parseColor("#6c89a8")
            else -> Color.parseColor("#ffffff")
        }

        val puffs = arrayOf(
            Triple(-38f, 8f, 22f),
            Triple(-16f, -6f, 30f),
            Triple(14f, -3f, 27f),
            Triple(38f, 9f, 20f),
            Triple(-4f, 10f, 25f),
            Triple(20f, 11f, 22f)
        )

        val cPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val totalScale = cloudScale * s

        // Layer 1: Ambient underside / base shadow
        cPaint.color = baseColor
        for (p in puffs) {
            val px = cx + p.first * totalScale
            val py = cy + (p.second + 6f) * totalScale
            val r = p.third * totalScale
            canvas.drawCircle(px, py, r, cPaint)
        }

        // Layer 2: Mid-tone volume body
        cPaint.color = midColor
        for (p in puffs) {
            val px = cx + p.first * totalScale
            val py = cy + (p.second + 2f) * totalScale
            val r = p.third * 0.96f * totalScale
            canvas.drawCircle(px, py, r, cPaint)
        }

        // Layer 3: Sunlit top highlight domes
        cPaint.color = topColor
        for (p in puffs) {
            val px = cx + (p.first - 3f) * totalScale
            val py = cy + (p.second - 2f) * totalScale
            val r = p.third * 0.86f * totalScale
            canvas.drawCircle(px, py, r, cPaint)
        }
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

    private fun schedulePeriodicUpdates(context: Context) {
        val prefs = context.getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
        val intervalMin = prefs.getInt("refreshIntervalMinutes", 15)
        if (intervalMin <= 0) return // Manual refresh only

        val intent = Intent(context, IsometricWeatherWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_WIDGET
        }
        val pi = PendingIntent.getBroadcast(
            context,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        try {
            val intervalMs = intervalMin * 60 * 1000L
            am?.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + intervalMs,
                intervalMs,
                pi
            )
        } catch (_: Exception) {}
    }

    private fun cancelPeriodicUpdates(context: Context) {
        val intent = Intent(context, IsometricWeatherWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_WIDGET
        }
        val pi = PendingIntent.getBroadcast(
            context,
            100,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            am?.cancel(pi)
        }
    }

    data class WidgetConfig(
        val locationName: String,
        val lat: Double,
        val lon: Double,
        val renderMode: String,
        val dioramaTheme: String,
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
