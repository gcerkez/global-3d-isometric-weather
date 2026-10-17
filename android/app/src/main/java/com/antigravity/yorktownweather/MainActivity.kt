package com.antigravity.yorktownweather

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * MainActivity
 * ============
 * Full Interactive 3D Isometric Weather App & Comprehensive Widget Configurator
 * for OnePlus 13 (OxygenOS 15) and Android 8.0+ devices.
 *
 * Provides complete parity with the KDE Plasma 6 widget settings:
 *   - Location Search (Open-Meteo Geocoding) + Quick Regional Presets
 *   - Render Mode (Hybrid 3D Diorama, Interactive Procedural 3D, AI Generative City)
 *   - Transparent Wallpaper Cutout Mode (float 3D island directly over OnePlus wallpaper)
 *   - Lighting / Time of Day Override (Real-Time Solar Cycle, Day, Sunset, Night)
 *   - Weather Condition Override (Live Open-Meteo, Clear, Cloudy, Rain, Storm, Snow, Fog)
 *   - Temperature Unit (Dual °F (°C), °F only, °C only) & Wind Unit (MPH, KM/H, KN, M/S)
 *   - Expanded HUD Telemetry Toggle (AQI / UV / Moon Phase / 12h Sparkline on Widget)
 */
class MainActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var webView: WebView
    private lateinit var statusLabel: TextView
    private lateinit var activeLocationHeader: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0d1b2a"))
        }

        // 1. Top Live Interactive 3D Isometric Preview (42% screen height)
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            @Suppress("DEPRECATION")
            settings.allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            settings.allowUniversalAccessFromFileURLs = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    syncSettingsToWebView()
                }
            }
            setBackgroundColor(Color.parseColor("#0d1b2a"))
            loadUrl("file:///android_asset/web/index.html")
        }
        rootLayout.addView(
            webView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0.44f
            )
        )

        // 2. Bottom Scrollable Configuration Panel (56% screen height)
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#132232"))
        }
        val configContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 32, 40, 64)
        }

        // Header Title
        activeLocationHeader = TextView(this).apply {
            val curName = prefs.getString("locationName", "Yorktown, VA") ?: "Yorktown, VA"
            text = "⚙️ Global 3D Isometric Weather Settings\nActive Location: $curName"
            setTextColor(Color.WHITE)
            textSize = 16.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        }
        configContainer.addView(activeLocationHeader)

        statusLabel = TextView(this).apply {
            text = "Changes automatically update your Homescreen & Lockscreen widgets."
            setTextColor(Color.parseColor("#7fc4f2"))
            textSize = 12.5f
            setPadding(0, 0, 0, 20)
        }
        configContainer.addView(statusLabel)

        // --- SECTION 1: Location & Geocoding Search ---
        configContainer.addView(makeSectionHeader("1. Location & Global City Search"))

        val searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 12)
        }
        val cityInput = EditText(this).apply {
            hint = "Search any city (e.g. Yorktown, San Francisco, Kyoto)..."
            setHintTextColor(Color.parseColor("#7c96b0"))
            setTextColor(Color.WHITE)
            textSize = 14f
            setSingleLine(true)
        }
        searchRow.addView(
            cityInput,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        )
        val searchBtn = makeStyledButton("Search & Apply") {
            val q = cityInput.text.toString().trim()
            if (q.length >= 2) geocodeAndApplyCity(q)
        }
        searchRow.addView(searchBtn)
        configContainer.addView(searchRow)

        // Quick Presets
        val presetScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, 4, 0, 18)
        }
        val presetRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val presets = listOf(
            Triple("Yorktown, VA", 37.2388f, -76.5097f),
            Triple("Williamsburg, VA", 37.2707f, -76.7075f),
            Triple("Norfolk, VA", 36.8508f, -76.2859f),
            Triple("San Francisco, CA", 37.7749f, -122.4194f),
            Triple("Tokyo, Japan", 35.6762f, 139.6503f),
            Triple("London, UK", 51.5074f, -0.1278f)
        )
        for ((name, lat, lon) in presets) {
            val chip = makeStyledButton(name) {
                saveLocation(name, lat, lon)
            }.apply {
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 16, 0)
                layoutParams = lp
            }
            presetRow.addView(chip)
        }
        presetScroll.addView(presetRow)
        configContainer.addView(presetScroll)

        // --- SECTION 2: 3D Rendering Engine & Wallpaper Backdrop ---
        configContainer.addView(makeSectionHeader("2. 3D Isometric Rendering & Backdrop"))

        configContainer.addView(
            makeSpinnerRow(
                label = "Render Mode:",
                entries = listOf(
                    "Hybrid: High-Detail 3D Diorama + Weather FX" to "hybrid",
                    "AI Generative: Custom 3D City Art for Selected City" to "ai_generative"
                ),
                currentVal = prefs.getString("renderMode", "hybrid") ?: "hybrid"
            ) { newVal ->
                prefs.edit().putString("renderMode", newVal).apply()
                applyAndRefreshWidgets("Render Mode updated")
            }
        )

        val transparentCheck = CheckBox(this).apply {
            text = "Transparent Cutout Mode (Float 3D island directly over OnePlus wallpaper)"
            setTextColor(Color.parseColor("#e6f1fa"))
            textSize = 13.5f
            isChecked = prefs.getBoolean("transparentBackground", false)
            setPadding(8, 14, 8, 14)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("transparentBackground", checked).apply()
                applyAndRefreshWidgets(if (checked) "Transparent Cutout Mode enabled" else "Sky Backdrop Card enabled")
            }
        }
        configContainer.addView(transparentCheck)

        val expandedHudCheck = CheckBox(this).apply {
            text = "Show Extended Telemetry on Widget (AQI, UV, Moon Phase & 12h Sparkline)"
            setTextColor(Color.parseColor("#e6f1fa"))
            textSize = 13.5f
            isChecked = prefs.getBoolean("showExtendedHud", true)
            setPadding(8, 8, 8, 18)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("showExtendedHud", checked).apply()
                applyAndRefreshWidgets("Widget HUD telemetry updated")
            }
        }
        configContainer.addView(expandedHudCheck)

        // --- SECTION 3: Atmosphere, Time of Day & Weather Simulation ---
        configContainer.addView(makeSectionHeader("3. Time of Day & Weather Simulation"))

        configContainer.addView(
            makeSpinnerRow(
                label = "Time of Day / Sky:",
                entries = listOf(
                    "Real-Time Solar Cycle (Live Day / Sunset / Night)" to "realtime",
                    "Always Sunny Daylight (Reference Blue Sky)" to "day",
                    "Golden Hour / Sunset" to "sunset",
                    "Starry Night (Glowing Windows & Moon Phase)" to "night"
                ),
                currentVal = prefs.getString("timeOfDayMode", "realtime") ?: "realtime"
            ) { newVal ->
                prefs.edit().putString("timeOfDayMode", newVal).apply()
                applyAndRefreshWidgets("Time of day set to $newVal")
            }
        )

        configContainer.addView(
            makeSpinnerRow(
                label = "Weather Condition:",
                entries = listOf(
                    "Live Real-Time Open-Meteo Weather" to "realtime",
                    "Preview: Clear Weather (Sunny / Starry)" to "clear",
                    "Preview: Partly Cloudy (3D Volumetric Clouds)" to "partly_cloudy",
                    "Preview: Rain Shower (Rain Streaks & Ripples)" to "rain",
                    "Preview: Thunderstorm (Heavy Rain & Lightning)" to "thunderstorm",
                    "Preview: Snowfall (Snowflakes & Frost)" to "snow",
                    "Preview: Coastal Fog" to "fog"
                ),
                currentVal = prefs.getString("weatherOverride", "realtime") ?: "realtime"
            ) { newVal ->
                prefs.edit().putString("weatherOverride", newVal).apply()
                applyAndRefreshWidgets("Weather mode set to $newVal")
            }
        )

        // --- SECTION 4: Units ---
        configContainer.addView(makeSectionHeader("4. Temperature & Wind Units"))

        configContainer.addView(
            makeSpinnerRow(
                label = "Temperature Display:",
                entries = listOf(
                    "Dual: °F (°C) — Reference Style (e.g. 68°F (20°C))" to "BOTH",
                    "Fahrenheit only (°F)" to "F",
                    "Celsius only (°C)" to "C"
                ),
                currentVal = prefs.getString("temperatureUnit", "BOTH") ?: "BOTH"
            ) { newVal ->
                prefs.edit().putString("temperatureUnit", newVal).apply()
                applyAndRefreshWidgets("Temperature unit set to $newVal")
            }
        )

        configContainer.addView(
            makeSpinnerRow(
                label = "Wind Speed Unit:",
                entries = listOf(
                    "Miles per hour (MPH)" to "mph",
                    "Kilometers per hour (KM/H)" to "kmh",
                    "Knots (KN)" to "kn",
                    "Meters per second (M/S)" to "ms"
                ),
                currentVal = prefs.getString("windSpeedUnit", "mph") ?: "mph"
            ) { newVal ->
                prefs.edit().putString("windSpeedUnit", newVal).apply()
                applyAndRefreshWidgets("Wind unit set to $newVal")
            }
        )

        // Apply & Close / Refresh Button
        val applyBtn = makeStyledButton("✅ Save & Update Homescreen / Lockscreen Widget") {
            applyAndRefreshWidgets("All widget settings applied!")
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, resultValue)
                finish()
            } else {
                Toast.makeText(this, "Widget updated! Check your Homescreen.", Toast.LENGTH_SHORT).show()
            }
        }.apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 28, 0, 16)
            layoutParams = lp
        }
        configContainer.addView(applyBtn)

        scrollView.addView(configContainer)
        rootLayout.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0.56f
            )
        )

        setContentView(rootLayout)
    }

    private fun makeSectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title.uppercase()
            setTextColor(Color.parseColor("#ffd166"))
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, 22, 0, 8)
        }
    }

    private fun makeStyledButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 12f
            setTextColor(Color.WHITE)
            isAllCaps = false
            setPadding(28, 16, 28, 16)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f
                setColor(Color.parseColor("#25415c"))
                setStroke(2, Color.parseColor("#5b8db8"))
            }
            setOnClickListener { onClick() }
        }
    }

    private fun makeSpinnerRow(
        label: String,
        entries: List<Pair<String, String>>,
        currentVal: String,
        onSelected: (String) -> Unit
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 12)
        }
        val lbl = TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#b8d4ec"))
            textSize = 13f
            setPadding(0, 0, 0, 4)
        }
        row.addView(lbl)

        val spinner = Spinner(this).apply {
            val labels = entries.map { it.first }
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, labels)
            this.adapter = adapter
            val initIdx = entries.indexOfFirst { it.second == currentVal }.coerceAtLeast(0)
            setSelection(initIdx, false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    (view as? TextView)?.setTextColor(Color.WHITE)
                    onSelected(entries[position].second)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        row.addView(spinner)
        return row
    }

    private fun geocodeAndApplyCity(query: String) {
        statusLabel.text = "Searching Open-Meteo directory for '$query'..."
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                    }
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val results = JSONObject(body).optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val r = results.getJSONObject(0)
                        val name = r.getString("name")
                        val admin = r.optString("admin1", "")
                        val display = if (admin.isNotEmpty()) "$name, $admin" else name
                        Triple(display, r.getDouble("latitude").toFloat(), r.getDouble("longitude").toFloat())
                    } else null
                }.getOrNull()
            }
            if (result != null) {
                saveLocation(result.first, result.second, result.third)
            } else {
                statusLabel.text = "No matching city found for '$query'."
            }
        }
    }

    private fun saveLocation(name: String, lat: Float, lon: Float) {
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
        val isYorktown = name.lowercase().contains("yorktown")
        prefs.edit()
            .putString("locationName", name)
            .putFloat("latitude", lat)
            .putFloat("longitude", lon)
            .putString("renderMode", if (isYorktown) "hybrid" else "ai_generative")
            .apply()

        activeLocationHeader.text = "⚙️ Global 3D Isometric Weather Settings\nActive Location: $name"
        applyAndRefreshWidgets("Updated location to $name [$lat, $lon]")
    }

    private fun syncSettingsToWebView() {
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
        val name = (prefs.getString("locationName", "Yorktown, VA") ?: "Yorktown, VA").replace("'", "\\'")
        val lat = prefs.getFloat("latitude", 37.2388f)
        val lon = prefs.getFloat("longitude", -76.5097f)
        val rMode = prefs.getString("renderMode", "hybrid") ?: "hybrid"
        val tMode = prefs.getString("timeOfDayMode", "realtime") ?: "realtime"
        val wMode = prefs.getString("weatherOverride", "realtime") ?: "realtime"

        val js = """
            if (window.isoEngine) {
                window.isoEngine.state.locationName = '$name';
                window.isoEngine.state.latitude = $lat;
                window.isoEngine.state.longitude = $lon;
                window.isoEngine.state.renderMode = '$rMode';
                window.isoEngine.state.timeMode = '$tMode';
                window.isoEngine.state.weatherOverride = '$wMode';
                window.isoEngine.refreshAll();
            }
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun applyAndRefreshWidgets(msg: String) {
        statusLabel.text = "✓ $msg"
        syncSettingsToWebView()

        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, IsometricWeatherWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            val updateIntent = Intent(this, IsometricWeatherWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            sendBroadcast(updateIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
