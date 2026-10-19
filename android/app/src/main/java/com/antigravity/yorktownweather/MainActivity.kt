package com.antigravity.yorktownweather

import android.app.Activity
import android.app.AlertDialog
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
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt

/**
 * MainActivity
 * ============
 * Tabular Interactive 3D Isometric Weather App & Comprehensive Widget Configurator
 * for OnePlus 13 (OxygenOS 15) and Android 8.0+ devices.
 *
 * Tab Structure:
 *   - Tab 0: 🖼️ 3D Preview & Framing (Live 16:9 3D Scene + Camera Zoom & Pan Buttons)
 *   - Tab 1: 🏛️ Diorama Scene (18 Regional/NPS/UNESCO Models + Auto-Match Closest GPS Diorama)
 *   - Tab 2: 📍 Weather Location (GPS Auto-Detect + Travel Follow + Search + Presets)
 *   - Tab 3: 🪟 HUD Overlay (Top-Sky vs Corner Card + 100/115/130% Scaling + Transparent Mode)
 *   - Tab 4: ☀️ Atmosphere & Weather (Lighting Solar Cycle + Weather Sim Overrides + Particle Density)
 *   - Tab 5: ⚙️ Units & Preferences (Temperature & Wind Units)
 */
class MainActivity : Activity() {

    companion object {
        private const val RC_LOCATION_PERM = 1001
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var webView: WebView
    private lateinit var statusLabel: TextView
    private lateinit var activeLocationHeader: TextView
    private lateinit var dioramaThemeButton: Button
    private lateinit var autoUpdateLocCheck: CheckBox
    private lateinit var autoMatchDioramaCheck: CheckBox
    private lateinit var closestMatchStatusText: TextView
    private lateinit var framingStatsText: TextView
    private val tabButtons = mutableListOf<Button>()
    private var currentTabIndex = 0

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)

        // Check GPS / location permissions immediately on launch
        if (!DeviceLocationManager.hasLocationPermission(this)) {
            // Uncheck follow GPS by default until user grants permission
            if (prefs.getBoolean("useCurrentLocation", false)) {
                prefs.edit().putBoolean("useCurrentLocation", false).apply()
            }
            requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                RC_LOCATION_PERM
            )
        } else if (prefs.getBoolean("useCurrentLocation", false)) {
            detectAndApplyCurrentLocation()
        }

        // Root container (vertical)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0a1520"))
            setPadding(0, dp(12), 0, 0)
        }

        // --- TOP HEADER ---
        val headerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(8))
        }

        activeLocationHeader = TextView(this).apply {
            val curName = prefs.getString("locationName", "Yorktown, VA") ?: "Yorktown, VA"
            val curDiorama = prefs.getString("dioramaTheme", "yorktown") ?: "yorktown"
            text = "⚙️ Global 3D Isometric Weather\nActive: $curName • Diorama: $curDiorama"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        headerContainer.addView(activeLocationHeader)

        statusLabel = TextView(this).apply {
            text = "Select any tab below to configure diorama visuals, live weather, or widget display."
            setTextColor(Color.parseColor("#7ec3ed"))
            textSize = 11.5f
            setPadding(0, dp(2), 0, 0)
        }
        headerContainer.addView(statusLabel)
        rootLayout.addView(headerContainer)

        // --- TAB BAR (Horizontal Scrollable Strip) ---
        val tabTitles = listOf(
            "🖼️ 3D Preview",
            "🏛️ Diorama Scene",
            "📍 Weather Location",
            "☀️ Atmosphere",
            "⚙️ Settings"
        )

        val tabScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(dp(14), dp(4), dp(14), dp(10))
        }
        val tabRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val tabContentContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }

        // --- BUILD EACH TAB'S VIEW ---
        val tabPages = listOf(
            buildTab0PreviewAndFraming(),
            buildTab1DioramaScene(),
            buildTab2Location(),
            buildTab3Atmosphere(),
            buildTab4Settings()
        )

        fun selectTab(idx: Int) {
            currentTabIndex = idx
            tabContentContainer.removeAllViews()
            tabContentContainer.addView(tabPages[idx])

            for (i in tabButtons.indices) {
                val btn = tabButtons[i]
                val isSelected = (i == idx)
                btn.setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#8da8c4"))
                btn.typeface = Typeface.create(Typeface.DEFAULT, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                btn.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(16).toFloat()
                    setColor(if (isSelected) Color.parseColor("#1f6feb") else Color.parseColor("#132232"))
                    setStroke(dp(1), if (isSelected) Color.parseColor("#58a6ff") else Color.parseColor("#263d54"))
                }
            }
            prefs.edit().putInt("active_tab", idx).apply()
        }

        for (i in tabTitles.indices) {
            val title = tabTitles[i]
            val tabBtn = Button(this).apply {
                text = title
                textSize = 12f
                isAllCaps = false
                setPadding(dp(14), dp(6), dp(14), dp(6))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(36)
                ).apply {
                    setMargins(0, 0, dp(8), 0)
                }
                layoutParams = lp
                setOnClickListener { selectTab(i) }
            }
            tabButtons.add(tabBtn)
            tabRow.addView(tabBtn)
        }
        tabScroll.addView(tabRow)
        rootLayout.addView(tabScroll)

        rootLayout.addView(tabContentContainer)

        // --- BOTTOM ACTION BAR ---
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0c1824"))
            setPadding(dp(16), dp(10), dp(16), dp(14))
            gravity = Gravity.CENTER
        }

        val applyBtn = Button(this).apply {
            text = "✅ Apply & Update Widget"
            textSize = 13.5f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#238636"))
                setStroke(dp(1), Color.parseColor("#3fb950"))
            }
            setOnClickListener {
                hideKeyboard()
                applyAndRefreshWidgets("Widget updated! Check your Homescreen.")
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    setResult(RESULT_OK, resultValue)
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, "Widget updated! Check your Homescreen.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        bottomBar.addView(applyBtn)
        rootLayout.addView(bottomBar)

        setContentView(rootLayout)

        val savedTab = prefs.getInt("active_tab", 0).coerceIn(0, tabTitles.size - 1)
        selectTab(savedTab)
    }

    /**
     * TAB 0: 🖼️ 3D Preview & Framing
     * Live 16:9 3D diorama canvas + camera zoom and horizontal pan controls.
     */
    private fun buildTab0PreviewAndFraming(): View {
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
        }

        // Live 16:9 Interactive 3D Diorama Card
        val previewFrame = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#0d1b2a"))
                setStroke(dp(1), Color.parseColor("#3b628a"))
            }
            clipChildren = true
        }

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
        previewFrame.addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(330)
            )
        )
        layout.addView(
            previewFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(330)
            ).apply {
                setMargins(0, 0, 0, dp(12))
            }
        )

        layout.addView(makeSectionHeader("Camera Zoom & Horizontal Framing"))

        val camHint = TextView(this).apply {
            text = "Scale and shift the 3D diorama left or right to frame your town. The homescreen weather overlay stays stationary in its configured position."
            setTextColor(Color.parseColor("#b8d4ec"))
            textSize = 12f
            setPadding(0, 0, 0, dp(8))
        }
        layout.addView(camHint)

        fun updateFramingStats() {
            val z = prefs.getFloat("widgetZoom", 1.0f)
            val o = prefs.getFloat("widgetOffsetX", 0.0f)
            val sign = if (o > 0) "+" else ""
            framingStatsText.text = "Zoom: ${(z * 100).toInt()}% • Horizontal Offset: $sign${o.toInt()} px"
        }

        // Framing Action Buttons (Icon-Only)
        val camControlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(8))
        }

        val camButtons = listOf(
            Triple("➖", "zoom_out", "Zoom Out"),
            Triple("➕", "zoom_in", "Zoom In"),
            Triple("◀", "pan_left", "Pan Left"),
            Triple("🎯", "center", "Center Island"),
            Triple("▶", "pan_right", "Pan Right")
        )
        for ((icon, act, desc) in camButtons) {
            val btn = makeStyledButton(icon) {
                var zoom = prefs.getFloat("widgetZoom", 1.0f)
                var offsetX = prefs.getFloat("widgetOffsetX", 0.0f)
                when (act) {
                    "zoom_in" -> zoom = (zoom * 1.15f).coerceIn(0.65f, 2.40f)
                    "zoom_out" -> zoom = (zoom / 1.15f).coerceIn(0.65f, 2.40f)
                    "pan_left" -> offsetX = (offsetX - 28.0f).coerceIn(-180.0f, 180.0f)
                    "pan_right" -> offsetX = (offsetX + 28.0f).coerceIn(-180.0f, 180.0f)
                    "center" -> {
                        zoom = 1.0f
                        offsetX = 0.0f
                    }
                }
                prefs.edit()
                    .putFloat("widgetZoom", zoom)
                    .putFloat("widgetOffsetX", offsetX)
                    .apply()
                val jsAct = if (act == "center") "reset" else act
                webView.evaluateJavascript("if (window.isoEngine) window.isoEngine.adjustCamera('$jsAct');", null)
                updateFramingStats()
                applyAndRefreshWidgets("Camera framing adjusted")
            }.apply {
                contentDescription = desc
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                lp.setMargins(0, 0, dp(6), 0)
                layoutParams = lp
                setPadding(dp(2), dp(8), dp(2), dp(8))
                textSize = 17f
            }
            camControlRow.addView(btn)
        }
        layout.addView(camControlRow)

        framingStatsText = TextView(this).apply {
            setTextColor(Color.parseColor("#ffd166"))
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, dp(2), 0, dp(12))
        }
        updateFramingStats()
        layout.addView(framingStatsText)

        // Quick zoom preset row
        val presetRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val zoomPresets = listOf(
            "Far (85%)" to 0.85f,
            "Standard (100%)" to 1.00f,
            "Close-Up (125%)" to 1.25f,
            "Hero Detail (160%)" to 1.60f
        )
        for ((pName, pZoom) in zoomPresets) {
            val chip = makeStyledButton(pName) {
                prefs.edit().putFloat("widgetZoom", pZoom).apply()
                webView.evaluateJavascript("if (window.isoEngine) { window.isoEngine.state.cameraZoom = $pZoom; window.isoEngine.refreshAll(); }", null)
                updateFramingStats()
                applyAndRefreshWidgets("Zoom set to ${(pZoom * 100).toInt()}%")
            }.apply {
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                    setMargins(0, 0, dp(4), 0)
                }
                layoutParams = lp
                setPadding(dp(4), dp(7), dp(4), dp(7))
                textSize = 11f
            }
            presetRow.addView(chip)
        }
        layout.addView(presetRow)

        scroll.addView(layout)
        return scroll
    }

    /**
     * TAB 1: 🏛️ 3D Diorama Scene
     * Auto-Match Closest GPS Diorama + 18 Regional, NPS & UNESCO Pre-Generated Diorama Models.
     */
    private fun buildTab1DioramaScene(): View {
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
        }

        layout.addView(makeSectionHeader("1. Auto-Match Closest 3D Diorama to My Location"))

        // GPS Auto-Match Card
        val gpsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#142538"))
                setStroke(dp(1), Color.parseColor("#2a557c"))
            }
            setPadding(dp(14), dp(12), dp(14), dp(14))
        }

        fun updateClosestDioramaDisplay() {
            val lat = prefs.getFloat("latitude", 37.2388f).toDouble()
            val lon = prefs.getFloat("longitude", -76.5097f).toDouble()
            val closest = DeviceLocationManager.findClosestDiorama(lat, lon)
            val dName = closest.first.name
            val miles = String.format(Locale.US, "%.1f", closest.second)
            closestMatchStatusText.text = "📍 Nearest landmark diorama:\n$dName (~$miles miles from weather location)"
        }

        autoMatchDioramaCheck = CheckBox(this).apply {
            text = "Auto-Use Closest Matching 3D Diorama for My Location"
            setTextColor(Color.WHITE)
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isChecked = prefs.getBoolean("autoMatchDiorama", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("autoMatchDiorama", checked).apply()
                if (checked) {
                    val lat = prefs.getFloat("latitude", 37.2388f).toDouble()
                    val lon = prefs.getFloat("longitude", -76.5097f).toDouble()
                    val closest = DeviceLocationManager.findClosestDiorama(lat, lon)
                    prefs.edit().putString("dioramaTheme", closest.first.id).apply()
                    dioramaThemeButton.text = "▼  ${closest.first.name} (Auto-Matched)"
                    applyAndRefreshWidgets("Auto-matched closest 3D diorama: ${closest.first.name}")
                } else {
                    applyAndRefreshWidgets("Auto-match disabled (using manual diorama)")
                }
                updateClosestDioramaDisplay()
            }
        }
        gpsCard.addView(autoMatchDioramaCheck)

        closestMatchStatusText = TextView(this).apply {
            setTextColor(Color.parseColor("#7ec3ed"))
            textSize = 12f
            setPadding(dp(4), dp(6), 0, 0)
        }
        updateClosestDioramaDisplay()
        gpsCard.addView(closestMatchStatusText)
        layout.addView(gpsCard)

        layout.addView(makeSectionHeader("2. Manual 3D Isometric Diorama Model"))

        val themeEntries = DeviceLocationManager.REGIONAL_DIORAMAS.map {
            "${it.name} — ${it.description}" to it.id
        }

        val themeRow = makePickerRow(
            label = "Choose Any 3D Diorama (Completely Independent of Weather Coordinates):",
            dialogTitle = "Select 3D Isometric Diorama",
            entries = themeEntries,
            currentVal = prefs.getString("dioramaTheme", "yorktown") ?: "yorktown"
        ) { newVal ->
            prefs.edit().putString("dioramaTheme", newVal).apply()
            if (prefs.getBoolean("autoMatchDiorama", false)) {
                prefs.edit().putBoolean("autoMatchDiorama", false).apply()
                autoMatchDioramaCheck.isChecked = false
            }
            applyAndRefreshWidgets("3D Diorama set to $newVal")
            updateClosestDioramaDisplay()
        }
        dioramaThemeButton = themeRow.second
        layout.addView(themeRow.first)

        scroll.addView(layout)
        return scroll
    }

    /**
     * TAB 2: 📍 Weather Location
     * GPS Auto-Detect, Travel Follow Toggle, City Search, and Presets.
     */
    private fun buildTab2Location(): View {
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
        }

        layout.addView(makeSectionHeader("Active Weather Location"))

        val locHint = TextView(this).apply {
            text = "Your weather coordinates are completely independent of your selected 3D diorama. Follow your live phone GPS as you travel or lock to any town/city."
            setTextColor(Color.parseColor("#b8d4ec"))
            textSize = 12f
            setPadding(0, 0, 0, dp(8))
        }
        layout.addView(locHint)

        val autoLocBtn = makeStyledButton("📍 Auto-Detect My Current Location (GPS)") {
            detectAndApplyCurrentLocation()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(8))
            }
            setPadding(dp(14), dp(11), dp(14), dp(11))
            textSize = 13f
        }
        layout.addView(autoLocBtn)

        val hasPerm = DeviceLocationManager.hasLocationPermission(this)
        val shouldFollowGps = hasPerm && prefs.getBoolean("useCurrentLocation", false)
        if (!hasPerm && prefs.getBoolean("useCurrentLocation", false)) {
            prefs.edit().putBoolean("useCurrentLocation", false).apply()
        }

        autoUpdateLocCheck = CheckBox(this).apply {
            text = "Follow Phone GPS Location (Automatically update weather as you travel)"
            setTextColor(Color.parseColor("#e6f1fa"))
            textSize = 13f
            isChecked = shouldFollowGps
            setPadding(dp(4), dp(2), dp(4), dp(10))
            setOnCheckedChangeListener { buttonView, checked ->
                if (checked && !DeviceLocationManager.hasLocationPermission(this@MainActivity)) {
                    buttonView.isChecked = false
                    requestPermissions(
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                        RC_LOCATION_PERM
                    )
                    return@setOnCheckedChangeListener
                }
                prefs.edit().putBoolean("useCurrentLocation", checked).apply()
                if (checked) {
                    detectAndApplyCurrentLocation()
                } else {
                    applyAndRefreshWidgets("Auto-update disabled (locked to current city)")
                }
            }
        }
        layout.addView(autoUpdateLocCheck)

        // Search Row
        val searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(12))
        }
        val cityInput = EditText(this).apply {
            hint = "Search city weather (e.g. Tabb, Yorktown, San Francisco)..."
            setHintTextColor(Color.parseColor("#7c96b0"))
            setTextColor(Color.WHITE)
            textSize = 13.5f
            setSingleLine(true)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#16283a"))
                setStroke(dp(1), Color.parseColor("#3d6080"))
            }
        }
        searchRow.addView(
            cityInput,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                setMargins(0, 0, dp(8), 0)
            }
        )
        val searchBtn = makeStyledButton("Search") {
            hideKeyboard()
            val q = cityInput.text.toString().trim()
            if (q.length >= 2) geocodeAndApplyCity(q)
        }
        searchRow.addView(searchBtn)
        layout.addView(searchRow)

        layout.addView(makeSectionHeader("Quick Location Presets"))

        val presetScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(2), 0, dp(12))
        }
        val presetRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val presets = listOf(
            PresetCity("Yorktown, VA", 37.2388f, -76.5097f),
            PresetCity("Tabb, VA", 37.1310f, -76.4452f),
            PresetCity("Jamestown, VA", 37.2085f, -76.7741f),
            PresetCity("Williamsburg, VA", 37.2707f, -76.7075f),
            PresetCity("Washington, D.C.", 38.8951f, -77.0364f),
            PresetCity("New York, NY", 40.7128f, -74.0060f),
            PresetCity("San Francisco, CA", 37.7749f, -122.4194f),
            PresetCity("Chicago, IL", 41.8781f, -87.6298f),
            PresetCity("London, UK", 51.5074f, -0.1278f),
            PresetCity("Paris, FR", 48.8566f, 2.3522f),
            PresetCity("Rome, IT", 41.9028f, 12.4964f),
            PresetCity("Cairo, EG", 30.0444f, 31.2357f),
            PresetCity("Tokyo, JP", 35.6762f, 139.6503f)
        )
        for (p in presets) {
            val chip = makeStyledButton(p.name) {
                saveLocation(p.name, p.lat, p.lon)
            }.apply {
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, dp(8), 0)
                }
                layoutParams = lp
                setPadding(dp(12), dp(8), dp(12), dp(8))
            }
            presetRow.addView(chip)
        }
        presetScroll.addView(presetRow)
        layout.addView(presetScroll)

        scroll.addView(layout)
        return scroll
    }

    /**
     * TAB 3: ☀️ Atmosphere & Weather
     * Solar cycle, condition overrides, and weather particle intensity.
     */
    private fun buildTab3Atmosphere(): View {
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
        }

        layout.addView(makeSectionHeader("Atmosphere, Lighting & Weather Simulation"))

        layout.addView(
            makePickerRow(
                label = "Time of Day / Sky Lighting:",
                dialogTitle = "Select Time of Day Lighting",
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
            }.first
        )

        layout.addView(
            makePickerRow(
                label = "Weather Condition Simulation:",
                dialogTitle = "Select Weather Condition",
                entries = listOf(
                    "Live Real-Time Open-Meteo Weather" to "realtime",
                    "Preview: Clear Weather (Sunny / Starry)" to "clear",
                    "Preview: Partly Cloudy (3D Volumetric Clouds)" to "partly_cloudy",
                    "Preview: Rain Shower (Rain Across Canvas & Ripples)" to "rain",
                    "Preview: Thunderstorm (Heavy Rain & Lightning)" to "thunderstorm",
                    "Preview: Snowfall (Snowflakes Across Canvas)" to "snow",
                    "Preview: Coastal Fog" to "fog"
                ),
                currentVal = prefs.getString("weatherOverride", "realtime") ?: "realtime"
            ) { newVal ->
                prefs.edit().putString("weatherOverride", newVal).apply()
                applyAndRefreshWidgets("Weather mode set to $newVal")
            }.first
        )

        layout.addView(
            makePickerRow(
                label = "Weather Effect Intensity & Speed:",
                dialogTitle = "Select Weather FX Intensity",
                entries = listOf(
                    "Subtle / Gentle (25% — Calm, Light Drops)" to "0.25",
                    "Balanced / Natural (50% — Recommended)" to "0.50",
                    "Standard (100%)" to "1.00"
                ),
                currentVal = String.format("%.2f", prefs.getFloat("particleDensity", 0.50f))
            ) { newVal ->
                val f = newVal.toFloatOrNull() ?: 0.50f
                prefs.edit().putFloat("particleDensity", f).apply()
                applyAndRefreshWidgets("Weather effect intensity set to ${(f * 100).toInt()}%")
            }.first
        )

        scroll.addView(layout)
        return scroll
    }

    /**
     * TAB 4: ⚙️ Settings, Display & Units
     * HUD text scale, transparent cutout mode, extended telemetry, refresh intervals, and units.
     */
    private fun buildTab4Settings(): View {
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
        }

        // --- HUD DISPLAY & READABILITY ---
        layout.addView(makeSectionHeader("Widget Top HUD & Readability"))

        layout.addView(
            makePickerRow(
                label = "Top Overlay Text Size Scale:",
                dialogTitle = "Select Overlay Text Size",
                entries = listOf(
                    "Standard Size (100%)" to "1.00",
                    "Extrapolated Large (115% — Recommended for Phone Homescreen)" to "1.15",
                    "Extrapolated Extra-Large (130% — Maximum Readability)" to "1.30"
                ),
                currentVal = String.format("%.2f", prefs.getFloat("hudScale", 1.15f))
            ) { newVal ->
                val f = newVal.toFloatOrNull() ?: 1.15f
                prefs.edit().putFloat("hudScale", f).apply()
                applyAndRefreshWidgets("Overlay text scale set to ${(f * 100).toInt()}%")
            }.first
        )

        val transparentCheck = CheckBox(this).apply {
            text = "Transparent Cutout Mode (Float 3D island directly over phone wallpaper)"
            setTextColor(Color.parseColor("#e6f1fa"))
            textSize = 13.5f
            isChecked = prefs.getBoolean("transparentBackground", false)
            setPadding(dp(4), dp(8), dp(4), dp(8))
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("transparentBackground", checked).apply()
                applyAndRefreshWidgets(if (checked) "Transparent Cutout Mode enabled" else "Sky Backdrop Card enabled")
            }
        }
        layout.addView(transparentCheck)

        val expandedHudCheck = CheckBox(this).apply {
            text = "Show Extended Telemetry (AQI, UV Index, Moon Phase & Feels Like)"
            setTextColor(Color.parseColor("#e6f1fa"))
            textSize = 13.5f
            isChecked = prefs.getBoolean("showExtendedHud", true)
            setPadding(dp(4), dp(4), dp(4), dp(10))
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("showExtendedHud", checked).apply()
                applyAndRefreshWidgets("Widget HUD telemetry updated")
            }
        }
        layout.addView(expandedHudCheck)

        // --- BACKGROUND SYNC & BATTERY ---
        layout.addView(makeSectionHeader("Background Sync & Battery"))

        layout.addView(
            makePickerRow(
                label = "Background Refresh Interval:",
                dialogTitle = "Select Refresh Interval",
                entries = listOf(
                    "15 Minutes (Default — Live sync)" to "15",
                    "30 Minutes (Balanced)" to "30",
                    "1 Hour (Battery friendly)" to "60",
                    "2 Hours (Low battery usage)" to "120",
                    "Manual Refresh Only (Battery Saver)" to "0"
                ),
                currentVal = prefs.getInt("refreshIntervalMinutes", 15).toString()
            ) { newVal ->
                val min = newVal.toIntOrNull() ?: 15
                prefs.edit().putInt("refreshIntervalMinutes", min).apply()
                applyAndRefreshWidgets(if (min > 0) "Refresh interval set to $min mins" else "Manual refresh only enabled")
            }.first
        )

        // --- TEMPERATURE & WIND UNITS ---
        layout.addView(makeSectionHeader("Temperature & Wind Units"))

        layout.addView(
            makePickerRow(
                label = "Temperature Display Format:",
                dialogTitle = "Select Temperature Unit",
                entries = listOf(
                    "Dual: °F (°C) — Reference Style (e.g. 68°F (20°C))" to "BOTH",
                    "Fahrenheit only (°F)" to "F",
                    "Celsius only (°C)" to "C"
                ),
                currentVal = prefs.getString("temperatureUnit", "BOTH") ?: "BOTH"
            ) { newVal ->
                prefs.edit().putString("temperatureUnit", newVal).apply()
                applyAndRefreshWidgets("Temperature unit set to $newVal")
            }.first
        )

        layout.addView(
            makePickerRow(
                label = "Wind Speed Unit:",
                dialogTitle = "Select Wind Speed Unit",
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
            }.first
        )

        scroll.addView(layout)
        return scroll
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        currentFocus?.let { view ->
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
    }

    private fun makeSectionHeader(title: String): TextView {
        val density = resources.displayMetrics.density
        return TextView(this).apply {
            text = title.uppercase()
            setTextColor(Color.parseColor("#ffd166"))
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, (14 * density).roundToInt(), 0, (6 * density).roundToInt())
        }
    }

    private fun makeStyledButton(label: String, onClick: () -> Unit): Button {
        val density = resources.displayMetrics.density
        return Button(this).apply {
            text = label
            textSize = 12.5f
            setTextColor(Color.WHITE)
            isAllCaps = false
            setPadding(
                (14 * density).roundToInt(),
                (10 * density).roundToInt(),
                (14 * density).roundToInt(),
                (10 * density).roundToInt()
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * density
                setColor(Color.parseColor("#1b3044"))
                setStroke((1 * density).roundToInt(), Color.parseColor("#446f96"))
            }
            setOnClickListener { onClick() }
        }
    }

    private fun makePickerRow(
        label: String,
        dialogTitle: String,
        entries: List<Pair<String, String>>,
        currentVal: String,
        onSelected: (String) -> Unit
    ): Pair<View, Button> {
        val density = resources.displayMetrics.density
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (6 * density).roundToInt(), 0, (10 * density).roundToInt())
        }
        val lbl = TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#b8d4ec"))
            textSize = 12.5f
            setPadding(0, 0, 0, (4 * density).roundToInt())
        }
        row.addView(lbl)

        var selectedValue = currentVal
        val currentLabel = entries.firstOrNull { it.second == selectedValue }?.first ?: entries.first().first

        val pickerBtn = Button(this).apply {
            text = "▼  $currentLabel"
            textSize = 13f
            setTextColor(Color.WHITE)
            isAllCaps = false
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(
                (14 * density).roundToInt(),
                (11 * density).roundToInt(),
                (14 * density).roundToInt(),
                (11 * density).roundToInt()
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 9f * density
                setColor(Color.parseColor("#182c40"))
                setStroke((1 * density).roundToInt(), Color.parseColor("#467299"))
            }
            setOnClickListener {
                hideKeyboard()
                val labels = entries.map { it.first }.toTypedArray()
                val checkedIdx = entries.indexOfFirst { it.second == selectedValue }.coerceAtLeast(0)
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(dialogTitle)
                    .setSingleChoiceItems(labels, checkedIdx) { dialog, which ->
                        val chosen = entries[which]
                        selectedValue = chosen.second
                        this.text = "▼  ${chosen.first}"
                        onSelected(chosen.second)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        row.addView(pickerBtn)
        return row to pickerBtn
    }

    private fun geocodeAndApplyCity(query: String) {
        statusLabel.text = "Searching Open-Meteo directory for '$query'..."
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 6000
                        readTimeout = 6000
                    }
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val r = results.getJSONObject(0)
                        val name = r.getString("name")
                        val admin1 = r.optString("admin1", "")
                        val country = r.optString("country_code", "")
                        val disp = when {
                            admin1.isNotEmpty() -> "$name, $admin1"
                            country.isNotEmpty() -> "$name, $country"
                            else -> name
                        }
                        Triple(disp, r.getDouble("latitude").toFloat(), r.getDouble("longitude").toFloat())
                    } else null
                }.getOrNull()
            }
            if (result != null) {
                val (disp, lat, lon) = result
                saveLocation(disp, lat, lon)
            } else {
                statusLabel.text = "No matching location found for '$query'."
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_LOCATION_PERM) {
            val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
            if (grantResults.isNotEmpty() && grantResults.any { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                statusLabel.text = "GPS permission granted. Resolving exact device location..."
                prefs.edit().putBoolean("useCurrentLocation", true).apply()
                if (::autoUpdateLocCheck.isInitialized) {
                    autoUpdateLocCheck.isChecked = true
                }
                detectAndApplyCurrentLocation()
            } else {
                prefs.edit().putBoolean("useCurrentLocation", false).apply()
                if (::autoUpdateLocCheck.isInitialized) {
                    autoUpdateLocCheck.isChecked = false
                }
                statusLabel.text = "⚠️ Location permission was denied. Defaulting to saved city."
            }
        }
    }

    private fun detectAndApplyCurrentLocation() {
        if (!DeviceLocationManager.hasLocationPermission(this)) {
            val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("useCurrentLocation", false).apply()
            if (::autoUpdateLocCheck.isInitialized) {
                autoUpdateLocCheck.isChecked = false
            }
            requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                RC_LOCATION_PERM
            )
            return
        }

        statusLabel.text = "Acquiring true GPS/device location..."
        scope.launch {
            val coords = DeviceLocationManager.getBestDeviceLocation(this@MainActivity)
            if (coords != null) {
                val (lat, lon) = coords
                statusLabel.text = "Reverse geocoding ($lat, $lon)..."
                val disp = DeviceLocationManager.reverseGeocode(this@MainActivity, lat, lon)
                val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("useCurrentLocation", true)
                    .putLong("lastLocationCheckTime", System.currentTimeMillis())
                    .apply()
                if (::autoUpdateLocCheck.isInitialized) {
                    autoUpdateLocCheck.isChecked = true
                }
                saveLocation(disp, lat.toFloat(), lon.toFloat(), keepAutoUpdate = true)

                // If auto-match diorama is active, update diorama too
                if (prefs.getBoolean("autoMatchDiorama", false)) {
                    val closest = DeviceLocationManager.findClosestDiorama(lat, lon)
                    prefs.edit().putString("dioramaTheme", closest.first.id).apply()
                    if (::dioramaThemeButton.isInitialized) {
                        dioramaThemeButton.text = "▼  ${closest.first.name} (Auto-Matched)"
                    }
                }
                statusLabel.text = "📍 Auto-detected GPS location: $disp"
            } else {
                statusLabel.text = "Could not resolve GPS location. Please ensure Location is enabled in Settings."
            }
        }
    }

    private fun saveLocation(name: String, lat: Float, lon: Float, keepAutoUpdate: Boolean = false) {
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("locationName", name)
            .putFloat("latitude", lat)
            .putFloat("longitude", lon)
            .putBoolean("useCurrentLocation", keepAutoUpdate)
            .apply()
        if (!keepAutoUpdate && ::autoUpdateLocCheck.isInitialized) {
            autoUpdateLocCheck.isChecked = false
        }

        // If autoMatchDiorama is enabled, also update diorama theme
        var dTheme = prefs.getString("dioramaTheme", "yorktown") ?: "yorktown"
        if (prefs.getBoolean("autoMatchDiorama", false)) {
            val closest = DeviceLocationManager.findClosestDiorama(lat.toDouble(), lon.toDouble())
            dTheme = closest.first.id
            prefs.edit().putString("dioramaTheme", dTheme).apply()
            if (::dioramaThemeButton.isInitialized) {
                dioramaThemeButton.text = "▼  ${closest.first.name} (Auto-Matched)"
            }
        }

        activeLocationHeader.text = "⚙️ Global 3D Isometric Weather\nActive: $name • Diorama: $dTheme"
        applyAndRefreshWidgets("Weather location set to $name")
    }

    private fun syncSettingsToWebView() {
        if (!::webView.isInitialized) return
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
        val name = (prefs.getString("locationName", "Yorktown, VA") ?: "Yorktown, VA").replace("'", "\\'")
        val lat = prefs.getFloat("latitude", 37.2388f)
        val lon = prefs.getFloat("longitude", -76.5097f)
        val rMode = prefs.getString("renderMode", "hybrid") ?: "hybrid"
        val dTheme = prefs.getString("dioramaTheme", "yorktown") ?: "yorktown"
        val tMode = prefs.getString("timeOfDayMode", "realtime") ?: "realtime"
        val wMode = prefs.getString("weatherOverride", "realtime") ?: "realtime"
        val pDensity = prefs.getFloat("particleDensity", 0.50f)
        val zoom = prefs.getFloat("widgetZoom", 1.0f)
        val offsetX = prefs.getFloat("widgetOffsetX", 0.0f)

        val js = """
            const hud = document.getElementById('hudCard');
            if (hud) hud.style.display = 'none';
            const cam = document.querySelector('.camera-pill');
            if (cam) cam.style.display = 'none';
            if (window.isoEngine) {
                window.isoEngine.state.locationName = '$name';
                window.isoEngine.state.latitude = $lat;
                window.isoEngine.state.longitude = $lon;
                window.isoEngine.state.renderMode = '$rMode';
                window.isoEngine.setDioramaTheme('$dTheme');
                window.isoEngine.state.timeMode = '$tMode';
                window.isoEngine.state.weatherOverride = '$wMode';
                window.isoEngine.state.particleIntensity = $pDensity;
                window.isoEngine.state.cameraZoom = $zoom;
                window.isoEngine.state.cameraPanX = $offsetX;
                window.isoEngine.refreshAll();
            }
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun applyAndRefreshWidgets(msg: String) {
        statusLabel.text = "✓ $msg"
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
        val curName = prefs.getString("locationName", "Yorktown, VA") ?: "Yorktown, VA"
        val curDiorama = prefs.getString("dioramaTheme", "yorktown") ?: "yorktown"
        activeLocationHeader.text = "⚙️ Global 3D Isometric Weather\nActive: $curName • Diorama: $curDiorama"
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

    private data class PresetCity(
        val name: String,
        val lat: Float,
        val lon: Float
    )
}
