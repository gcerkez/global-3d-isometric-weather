package com.antigravity.yorktownweather

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.ArrayDeque
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * MainActivity
 * ============
 * Spacious, Scroll-Safe Interactive 3D Isometric Weather App & Comprehensive Widget Configurator
 * for OnePlus 13 (OxygenOS 15) and Android 8.0+ devices.
 *
 * Key UX Improvements:
 *   - Full-screen vertical `ScrollView` (no cramped split screen) with `FOCUS_BEFORE_DESCENDANTS`
 *     so scrolling never triggers text boxes or pops up the keyboard accidentally.
 *   - Scroll-safe modal `AlertDialog` Option Pickers instead of hair-trigger `Spinner` dropdowns,
 *     so scrolling through the config menu is completely smooth and effortless.
 *   - Built-in Pre-Generated Generic (Non-City-Specific) 3D Isometric Dioramas:
 *       * `yorktown`: Historic Yorktown, VA (1781 Colonial Waterfront & Coleman Bridge)
 *       * `coastal`:  Pre-Generated Generic: Coastal Harbor & Lighthouse Town
 *       * `modern`:   Pre-Generated Generic: Modern Downtown Metropolis & Bridge
 *       * `alpine`:   Pre-Generated Generic: Alpine Mountain & Lake Village
 *       * `custom_city`: Custom Generated High-Res City Diorama Cutout
 *   - Built-in On-Device Custom High-Res Diorama Cutout Generator ("Section D" right inside the Config Menu):
 *       * Discovers city landmarks via Wikipedia GeoSearch, downloads a high-res 3D isometric island,
 *         runs edge flood-fill background removal on-device, and synthesizes Day, Sunset & Night variants.
 *   - Isometric Zoom (`➖`/`➕`) and Horizontal Centering (`◀ Left` / `🎯 Center` / `▶ Right`) while
 *     keeping the HUD overlay stationary in its configured position.
 *   - Extrapolated HUD Overlay Position (`Top-Sky Split Banner` or `Corner Card`) & Text Size Scale.
 */
class MainActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var webView: WebView
    private lateinit var statusLabel: TextView
    private lateinit var activeLocationHeader: TextView
    private lateinit var dioramaThemeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
        val density = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).roundToInt()

        // Single Full-Screen ScrollView so the menu is never squashed
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0f1e2e"))
            isFillViewport = true
            isSmoothScrollingEnabled = true
        }

        // FOCUS_BEFORE_DESCENDANTS prevents EditText from stealing focus during scroll gestures
        val configContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
            isFocusable = true
            isFocusableInTouchMode = true
            setPadding(dp(18), dp(16), dp(18), dp(42))
        }

        // Top Title & Status Banner
        activeLocationHeader = TextView(this).apply {
            val curName = prefs.getString("locationName", "Yorktown, VA") ?: "Yorktown, VA"
            text = "⚙️ Global 3D Isometric Weather\nActive Location: $curName"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, 0, 0, dp(4))
        }
        configContainer.addView(activeLocationHeader)

        statusLabel = TextView(this).apply {
            text = "Scroll freely — settings open only when tapped. Your 3D Diorama render is independent of your weather coordinates."
            setTextColor(Color.parseColor("#8ecae6"))
            textSize = 12.5f
            setPadding(0, 0, 0, dp(12))
        }
        configContainer.addView(statusLabel)

        // Live 16:9 Interactive 3D Isometric Preview Card at Top of ScrollView
        val previewFrame = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#0d1b2a"))
                setStroke(dp(1), Color.parseColor("#4a7c59"))
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
                dp(205)
            )
        )
        configContainer.addView(
            previewFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(205)
            ).apply {
                setMargins(0, 0, 0, dp(12))
            }
        )

        // --- SECTION 1: Isometric Zoom & Horizontal Centering (Move Left / Center / Right) ---
        configContainer.addView(makeSectionHeader("1. Isometric Zoom & Horizontal Centering"))

        val camHint = TextView(this).apply {
            text = "Move the 3D isometric island left, center, or right as a whole while leaving the weather HUD overlay stationary."
            setTextColor(Color.parseColor("#b8d4ec"))
            textSize = 12f
            setPadding(0, 0, 0, dp(8))
        }
        configContainer.addView(camHint)

        val camControlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(2), 0, dp(10))
        }
        val camButtons = listOf(
            "➖ Zoom" to "zoom_out",
            "➕ Zoom" to "zoom_in",
            "◀ Left" to "pan_left",
            "🎯 Center" to "center",
            "▶ Right" to "pan_right"
        )
        for ((lbl, act) in camButtons) {
            val btn = makeStyledButton(lbl) {
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
                val sign = if (offsetX > 0) "+" else ""
                applyAndRefreshWidgets("Isometric Zoom: ${(zoom * 100).toInt()}%, Horizontal Offset: $sign${offsetX.toInt()} px")
            }.apply {
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                lp.setMargins(0, 0, dp(6), 0)
                layoutParams = lp
                setPadding(dp(6), dp(10), dp(6), dp(10))
                textSize = 11.5f
            }
            camControlRow.addView(btn)
        }
        configContainer.addView(camControlRow)

        // --- SECTION 2: Pre-Generated 3D Isometric Diorama Dropdown (Independent of Weather Lat/Lon) ---
        configContainer.addView(makeSectionHeader("2. Pre-Generated 3D Isometric Diorama Scene"))

        val themeEntries = listOf(
            "🏛️ Historic Yorktown, VA — 1781 Waterfront, Coleman Bridge & Tall Ship (NPS)" to "yorktown",
            "⛵ Historic Jamestown, VA — 1607 James Fort Palisade, 3 Ships & Turquoise River (NPS)" to "jamestown",
            "🏛️ Washington, D.C. & Mid-Atlantic — US Capitol, Monument, Lincoln Memorial & Basin" to "washington_dc",
            "🗽 New York City & Northeast — Statue of Liberty, Brooklyn Bridge & Empire State" to "new_york",
            "🌉 San Francisco & California Coast — Golden Gate Bridge, Alcatraz & Painted Ladies" to "san_francisco",
            "🏙️ Chicago & Great Lakes Midwest — Willis Tower, Red Bascule Bridge & Navy Pier" to "chicago_great_lakes",
            "🏜️ Grand Canyon & Southwest — Red-Rock Buttes, Cliff Dwellings & Colorado River" to "grand_canyon",
            "🎷 New Orleans & Gulf South — St. Louis Cathedral, French Quarter & Steamboat" to "new_orleans",
            "🌲 Seattle & Pacific Northwest — Space Needle, Pike Place, Mt. Rainier & Ferry" to "seattle_pnw",
            "🌴 Miami Beach, Florida Keys & Tropics — Art Deco Ocean Drive & Coral Lagoon" to "miami_caribbean",
            "⚓ New England & Maritime Atlantic — Lighthouse Point, Schooner & Historic Wharf" to "coastal",
            "🕰️ London & UK (Europe/London) — Big Ben, Parliament, Tower of London & Thames" to "london_uk",
            "🏰 Paris & Western Europe (Europe/Paris) — Notre-Dame, Louvre, Arc & Seine Bridge" to "paris_europe",
            "🏛️ Rome, Athens & Mediterranean — Colosseum, Parthenon, Aqueduct & Harbor" to "rome_mediterranean",
            "🏺 Cairo, Giza & Middle East — Ancient Citadel, Sacred Temple Pool & Pharos" to "cairo_giza",
            "⛩️ Tokyo, Kyoto & East Asia (Asia/Tokyo) — 5-Story Pagoda, Imperial Hall & Torii" to "tokyo_east_asia",
            "🌆 Global Modern Metropolis (Sydney / Singapore / Toronto) — Skyline & Bridge" to "modern",
            "🏔️ Alpine Mountain & Glacial Lake Village (Rockies / Swiss Alps / Andes)" to "alpine"
        )
        val themeRow = makePickerRow(
            label = "Active 3D Isometric Diorama (Independent of Weather Location):",
            dialogTitle = "Select Pre-Generated 3D Isometric Diorama",
            entries = themeEntries,
            currentVal = prefs.getString("dioramaTheme", "yorktown") ?: "yorktown"
        ) { newVal ->
            prefs.edit().putString("dioramaTheme", newVal).apply()
            applyAndRefreshWidgets("3D Diorama theme set to $newVal")
        }
        dioramaThemeButton = themeRow.second
        configContainer.addView(themeRow.first)

        // --- SECTION 3: Weather Telemetry Location & Global City Search (Independent of 3D Diorama) ---
        configContainer.addView(makeSectionHeader("3. Active Weather Location (Independent of 3D Render)"))

        val locHint = TextView(this).apply {
            text = "Use your phone's auto-detected current location or search any city/town for live Open-Meteo weather while keeping whichever 3D isometric diorama you like best."
            setTextColor(Color.parseColor("#b8d4ec"))
            textSize = 12f
            setPadding(0, 0, 0, dp(6))
        }
        configContainer.addView(locHint)

        val autoLocBtn = makeStyledButton("📍 Auto-Detect My Current Location") {
            detectAndApplyCurrentLocation()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(2), 0, dp(8))
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            textSize = 13f
        }
        configContainer.addView(autoLocBtn)

        val searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(10))
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
        configContainer.addView(searchRow)

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
                )
                lp.setMargins(0, 0, dp(8), 0)
                layoutParams = lp
                setPadding(dp(12), dp(8), dp(12), dp(8))
            }
            presetRow.addView(chip)
        }
        presetScroll.addView(presetRow)
        configContainer.addView(presetScroll)

        // --- SECTION 4: Extrapolated Widget HUD Overlay Layout & Size ---
        configContainer.addView(makeSectionHeader("4. Widget HUD Overlay Layout & Readability"))

        configContainer.addView(
            makePickerRow(
                label = "Widget Overlay Position (Stays Fixed While Isometric Moves):",
                dialogTitle = "Select Widget Overlay Layout",
                entries = listOf(
                    "Top-Sky Split Banner (Large Text in Upper Sky, 0% Island Overlap — Recommended)" to "top-sky",
                    "Top-Right Corner Card (Reference Style)" to "top-right"
                ),
                currentVal = prefs.getString("hudPosition", "top-sky") ?: "top-sky"
            ) { newVal ->
                prefs.edit().putString("hudPosition", newVal).apply()
                applyAndRefreshWidgets("Widget HUD layout set to $newVal")
            }.first
        )

        configContainer.addView(
            makePickerRow(
                label = "Widget Overlay Text Size (Extrapolated Readability):",
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
            text = "Transparent Cutout Mode (Float 3D island directly over OnePlus wallpaper)"
            setTextColor(Color.parseColor("#e6f1fa"))
            textSize = 13.5f
            isChecked = prefs.getBoolean("transparentBackground", false)
            setPadding(dp(4), dp(8), dp(4), dp(8))
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("transparentBackground", checked).apply()
                applyAndRefreshWidgets(if (checked) "Transparent Cutout Mode enabled" else "Sky Backdrop Card enabled")
            }
        }
        configContainer.addView(transparentCheck)

        val expandedHudCheck = CheckBox(this).apply {
            text = "Show Extended Telemetry on Widget (AQI, UV, Moon Phase & Feels Like)"
            setTextColor(Color.parseColor("#e6f1fa"))
            textSize = 13.5f
            isChecked = prefs.getBoolean("showExtendedHud", true)
            setPadding(dp(4), dp(4), dp(4), dp(10))
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("showExtendedHud", checked).apply()
                applyAndRefreshWidgets("Widget HUD telemetry updated")
            }
        }
        configContainer.addView(expandedHudCheck)

        // --- SECTION 5: Atmosphere, Time of Day & Weather Simulation ---
        configContainer.addView(makeSectionHeader("5. Time of Day & Weather Simulation"))

        configContainer.addView(
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

        configContainer.addView(
            makePickerRow(
                label = "Weather Condition:",
                dialogTitle = "Select Weather Condition",
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
            }.first
        )

        configContainer.addView(
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

        // --- SECTION 6: Temperature & Wind Units ---
        configContainer.addView(makeSectionHeader("6. Temperature & Wind Units"))

        configContainer.addView(
            makePickerRow(
                label = "Temperature Display:",
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

        configContainer.addView(
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

        // Save & Update Button
        val applyBtn = makeStyledButton("✅ Save & Update Homescreen / Lockscreen Widget") {
            hideKeyboard()
            applyAndRefreshWidgets("All widget settings saved & applied!")
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
            lp.setMargins(0, dp(22), 0, dp(16))
            layoutParams = lp
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#1f6f54"))
                setStroke(dp(1), Color.parseColor("#6ee7b7"))
            }
        }
        configContainer.addView(applyBtn)

        scrollView.addView(configContainer)
        setContentView(scrollView)
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
            setPadding(0, (16 * density).roundToInt(), 0, (6 * density).roundToInt())
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
                setColor(Color.parseColor("#213a52"))
                setStroke((1 * density).roundToInt(), Color.parseColor("#5b8db8"))
            }
            setOnClickListener { onClick() }
        }
    }

    /**
     * Scroll-safe replacement for Android Spinner: renders a clean labeled Button that opens
     * a native AlertDialog single-choice modal ONLY on deliberate tap, never during vertical scrolling.
     */
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
                val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("useCurrentLocation", false).apply()
                saveLocation(disp, lat, lon)
            } else {
                statusLabel.text = "No matching location found for '$query'."
            }
        }
    }

    private fun detectAndApplyCurrentLocation() {
        statusLabel.text = "Auto-detecting current phone location via network/IP..."
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val conn = (URL("https://get.geojs.io/v1/ip/geo.json").openConnection() as HttpURLConnection).apply {
                        connectTimeout = 5000
                        readTimeout = 5000
                    }
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    val city = json.optString("city", "").trim()
                    val region = json.optString("region", "").trim()
                    val lat = json.optDouble("latitude", Double.NaN)
                    val lon = json.optDouble("longitude", Double.NaN)
                    if (!lat.isNaN() && !lon.isNaN() && city.isNotEmpty()) {
                        val name = if (region.isNotEmpty()) "$city, $region" else city
                        Triple(name, lat.toFloat(), lon.toFloat())
                    } else null
                }.getOrNull()
            }
            if (result != null) {
                val (disp, lat, lon) = result
                val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("useCurrentLocation", true).apply()
                saveLocation(disp, lat, lon)
                statusLabel.text = "📍 Auto-detected current phone location: $disp"
            } else {
                statusLabel.text = "Could not auto-detect phone location. Please search manually."
            }
        }
    }

    /**
     * Saves the active weather telemetry coordinates (`name`, `lat`, `lon`) independently of
     * the user's selected 3D Isometric Diorama (`dioramaTheme`).
     */
    private fun saveLocation(name: String, lat: Float, lon: Float) {
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("locationName", name)
            .putFloat("latitude", lat)
            .putFloat("longitude", lon)
            .apply()
        activeLocationHeader.text = "⚙️ Global 3D Isometric Weather\nActive Location: $name"
        applyAndRefreshWidgets("Weather location set to $name (3D Diorama render unchanged)")
    }

    private fun syncSettingsToWebView() {
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
        statusLabel.text = "✓ $msg — Homescreen & Lockscreen widgets updated."
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
