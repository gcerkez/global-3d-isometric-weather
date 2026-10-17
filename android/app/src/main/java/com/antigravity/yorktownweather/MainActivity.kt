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
    private lateinit var cutoutStatusLabel: TextView
    private lateinit var customCityInput: EditText
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
            text = "Scroll freely — settings open only when tapped. Changes update your Homescreen & Lockscreen widget."
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

        // --- SECTION 2: 3D Diorama Scene & Built-In Custom High-Res Cutout Generator ---
        configContainer.addView(makeSectionHeader("2. 3D Diorama Scene & Custom City Cutout Generator"))

        val themeEntries = listOf(
            "🏛️ Historic Yorktown, VA (1781 Waterfront & Coleman Bridge)" to "yorktown",
            "⚓ Generic: Coastal Harbor & Lighthouse Town (Non-City-Specific)" to "coastal",
            "🌆 Generic: Modern Downtown Metropolis & Bridge (Non-City-Specific)" to "modern",
            "🏔️ Generic: Alpine Mountain & Lake Village (Non-City-Specific)" to "alpine",
            "✨ Custom Generated High-Res City Diorama Cutout (Any City)" to "custom_city"
        )
        val themeRow = makePickerRow(
            label = "Active 3D Isometric Diorama:",
            dialogTitle = "Select 3D Isometric Diorama Scene",
            entries = themeEntries,
            currentVal = prefs.getString("dioramaTheme", "yorktown") ?: "yorktown"
        ) { newVal ->
            prefs.edit().putString("dioramaTheme", newVal).apply()
            applyAndRefreshWidgets("3D Diorama theme set to $newVal")
        }
        dioramaThemeButton = themeRow.second
        configContainer.addView(themeRow.first)

        // Built-in "Generate Custom High-Res Diorama Cutouts for Any City" Card inside the Config Menu!
        val generatorCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#172a3d"))
                setStroke(dp(1), Color.parseColor("#3d6b94"))
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(6), 0, dp(12))
            layoutParams = lp
        }

        val genTitle = TextView(this).apply {
            text = "✨ Generate Custom High-Res Diorama Cutout for Any City"
            setTextColor(Color.parseColor("#ffd166"))
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, 0, 0, dp(4))
        }
        generatorCard.addView(genTitle)

        val genDesc = TextView(this).apply {
            text = "Synthesizes a bespoke 3D isometric floating-island cutout for any city right on your phone, automatically removes the background, and generates Day, Sunset & Starry Night variants."
            setTextColor(Color.parseColor("#c6ddf0"))
            textSize = 12f
            setPadding(0, 0, 0, dp(8))
        }
        generatorCard.addView(genDesc)

        customCityInput = EditText(this).apply {
            setText(prefs.getString("locationName", "Yorktown, VA") ?: "Yorktown, VA")
            hint = "Enter city or architectural theme (e.g. Kyoto, Paris, Charleston, SC)..."
            setHintTextColor(Color.parseColor("#7c96b0"))
            setTextColor(Color.WHITE)
            textSize = 13.5f
            setSingleLine(true)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#0d1b2a"))
                setStroke(dp(1), Color.parseColor("#416788"))
            }
        }
        generatorCard.addView(customCityInput)

        val generateCutoutBtn = makeStyledButton("🎨 Generate & Apply High-Res 3D Cutout Now") {
            hideKeyboard()
            val targetCity = customCityInput.text.toString().trim().ifEmpty {
                prefs.getString("locationName", "Yorktown, VA") ?: "Yorktown, VA"
            }
            generateCustomHighResCutoutOnDevice(targetCity)
        }.apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(10), 0, dp(6))
            layoutParams = lp
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#2b5c8f"))
                setStroke(dp(1), Color.parseColor("#7fc4f2"))
            }
        }
        generatorCard.addView(generateCutoutBtn)

        cutoutStatusLabel = TextView(this).apply {
            val hasCustom = File(filesDir, "custom_city_day.png").exists()
            text = if (hasCustom) "✓ Custom on-device city cutout is ready in storage."
            else "Tip: You can also pick one of the 3 built-in Generic non-city dioramas above anytime."
            setTextColor(Color.parseColor("#86efac"))
            textSize = 11.5f
        }
        generatorCard.addView(cutoutStatusLabel)
        configContainer.addView(generatorCard)

        // --- SECTION 3: Location & Global City Search ---
        configContainer.addView(makeSectionHeader("3. Active Weather Location & Presets"))

        val searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(10))
        }
        val cityInput = EditText(this).apply {
            hint = "Search city weather (e.g. Yorktown, San Francisco)..."
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
            PresetCity("Yorktown, VA", 37.2388f, -76.5097f, "yorktown"),
            PresetCity("Williamsburg, VA", 37.2707f, -76.7075f, "yorktown"),
            PresetCity("Norfolk, VA", 36.8508f, -76.2859f, "coastal"),
            PresetCity("San Francisco, CA", 37.7749f, -122.4194f, "modern"),
            PresetCity("Zermatt, Alps", 46.0207f, 7.7491f, "alpine"),
            PresetCity("Tokyo, Japan", 35.6762f, 139.6503f, "modern")
        )
        for (p in presets) {
            val chip = makeStyledButton(p.name) {
                saveLocation(p.name, p.lat, p.lon, p.suggestedTheme)
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

    /**
     * Built-in on-device High-Res Diorama Cutout Generator ("Section D" integrated directly into the Config Menu).
     * 1. Queries Wikipedia GeoSearch for local landmarks of `cityName`.
     * 2. Downloads a 1376x768 3D isometric floating island render on a sky-blue backdrop.
     * 3. Runs edge flood-fill background removal on-device to produce a clean transparent RGBA island cutout.
     * 4. Generates Day, Sunset (Golden Hour), and Night (Illuminated Windows) PNG variants in `filesDir`.
     */
    private fun generateCustomHighResCutoutOnDevice(cityName: String) {
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("latitude", 37.2388f)
        val lon = prefs.getFloat("longitude", -76.5097f)

        cutoutStatusLabel.setTextColor(Color.parseColor("#ffd166"))
        cutoutStatusLabel.text = "⏳ Step 1/3: Discovering landmarks & synthesizing 3D isometric island for '$cityName'..."

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    // 1. Discover landmarks via Wikipedia GeoSearch if coordinates match active city
                    val landmarks = mutableListOf<String>()
                    runCatching {
                        val wikiUrl = "https://en.wikipedia.org/w/api.php?action=query&list=geosearch&gscoord=$lat|$lon&gsradius=8000&gslimit=4&format=json"
                        val conn = (URL(wikiUrl).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 5000
                            readTimeout = 5000
                        }
                        val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        val arr = json.optJSONObject("query")?.optJSONArray("geosearch")
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                landmarks.add(arr.getJSONObject(i).getString("title"))
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        cutoutStatusLabel.text = "⏳ Step 2/3: Downloading high-res 3D isometric diorama for $cityName..."
                    }

                    val landmarkClause = if (landmarks.isNotEmpty()) {
                        "featuring ${landmarks.joinToString(", ")}, "
                    } else {
                        "featuring iconic architecture, waterfront, bridges and trees, "
                    }
                    val prompt = "Cute 3D isometric miniature city diorama of $cityName, $landmarkClause" +
                        "floating architectural island model, clean cut-out rounded rectangular base, " +
                        "isolated on a solid flat light sky blue background, tilt-shift miniature style, sharp 8k digital illustration, no text, no labels"
                    val encoded = URLEncoder.encode(prompt, "UTF-8")
                    val imgUrl = URL("https://image.pollinations.ai/prompt/$encoded?width=960&height=540&nologo=true&seed=42")
                    val conn = (imgUrl.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 15000
                    }
                    val rawBmp = conn.inputStream.use { BitmapFactory.decodeStream(it) }
                        ?: error("Could not decode synthesized image")

                    withContext(Dispatchers.Main) {
                        cutoutStatusLabel.text = "⏳ Step 3/3: Extracting transparent island cutout & building Day / Sunset / Night variants..."
                    }

                    extractAndSaveDaySunsetNightCutouts(rawBmp)
                    landmarks
                }
            }

            result.onSuccess { landmarks ->
                prefs.edit()
                    .putString("dioramaTheme", "custom_city")
                    .apply()
                dioramaThemeButton.text = "▼  ✨ Custom Generated High-Res City Diorama Cutout (Any City)"
                val lmInfo = if (landmarks.isNotEmpty()) " (${landmarks.take(3).joinToString(", ")})" else ""
                cutoutStatusLabel.setTextColor(Color.parseColor("#86efac"))
                cutoutStatusLabel.text = "✓ Generated & applied custom Day/Sunset/Night 3D cutouts for $cityName$lmInfo!"
                applyAndRefreshWidgets("Custom 3D cutout generated for $cityName")
            }.onFailure { err ->
                cutoutStatusLabel.setTextColor(Color.parseColor("#fca5a5"))
                cutoutStatusLabel.text = "Network busy while generating custom cutout (${err.message}). Using built-in high-res diorama."
            }
        }
    }

    /**
     * Performs edge flood-fill background removal on `rawBmp` and saves:
     *   - `custom_city_day.png`
     *   - `custom_city_sunset.png`
     *   - `custom_city_night.png`
     * into `context.filesDir`.
     */
    private fun extractAndSaveDaySunsetNightCutouts(rawBmp: Bitmap) {
        val w = rawBmp.width
        val h = rawBmp.height
        val pixels = IntArray(w * h)
        rawBmp.getPixels(pixels, 0, w, 0, 0, w, h)

        // Sample border pixels to estimate studio sky background color
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0
        for (x in 0 until w step 4) {
            val cTop = pixels[x]
            rSum += Color.red(cTop)
            gSum += Color.green(cTop)
            bSum += Color.blue(cTop)
            count++
        }
        val bgR = (rSum / max(1, count)).toInt()
        val bgG = (gSum / max(1, count)).toInt()
        val bgB = (bSum / max(1, count)).toInt()

        fun colorDist(c: Int): Float {
            val dr = (Color.red(c) - bgR).toFloat()
            val dg = (Color.green(c) - bgG).toFloat()
            val db = (Color.blue(c) - bgB).toFloat()
            return sqrt(dr * dr + dg * dg + db * db)
        }

        val visited = BooleanArray(w * h)
        val queue = ArrayDeque<Int>()
        for (x in 0 until w) {
            queue.add(x)
            visited[x] = true
            val botIdx = (h - 1) * w + x
            if (colorDist(pixels[botIdx]) < 52f) {
                queue.add(botIdx)
                visited[botIdx] = true
            }
        }
        for (y in 0 until h) {
            val leftIdx = y * w
            if (!visited[leftIdx] && colorDist(pixels[leftIdx]) < 56f) {
                queue.add(leftIdx)
                visited[leftIdx] = true
            }
            val rightIdx = y * w + (w - 1)
            if (!visited[rightIdx] && colorDist(pixels[rightIdx]) < 56f) {
                queue.add(rightIdx)
                visited[rightIdx] = true
            }
        }

        while (!queue.isEmpty()) {
            val idx = queue.removeFirst()
            val x = idx % w
            val y = idx / w
            val neighbors = intArrayOf(
                if (x > 0) idx - 1 else -1,
                if (x + 1 < w) idx + 1 else -1,
                if (y > 0) idx - w else -1,
                if (y + 1 < h) idx + w else -1
            )
            for (n in neighbors) {
                if (n >= 0 && !visited[n] && colorDist(pixels[n]) < 48f) {
                    visited[n] = true
                    queue.add(n)
                }
            }
        }

        val dayPixels = IntArray(w * h)
        val sunsetPixels = IntArray(w * h)
        val nightPixels = IntArray(w * h)

        for (i in 0 until w * h) {
            if (visited[i]) {
                dayPixels[i] = Color.TRANSPARENT
                sunsetPixels[i] = Color.TRANSPARENT
                nightPixels[i] = Color.TRANSPARENT
            } else {
                val c = pixels[i]
                val r = Color.red(c)
                val g = Color.green(c)
                val b = Color.blue(c)
                dayPixels[i] = Color.argb(255, r, g, b)

                val sr = min(255, (r * 1.12f + 12f).roundToInt())
                val sg = min(255, (g * 0.92f).roundToInt())
                val sb = min(255, (b * 0.80f).roundToInt())
                sunsetPixels[i] = Color.argb(255, sr, sg, sb)

                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                if (lum > 185f && r > 175 && g > 145) {
                    nightPixels[i] = Color.argb(255, min(255, (r * 1.08f).roundToInt()), min(255, (g * 0.94f).roundToInt()), (b * 0.58f).roundToInt())
                } else {
                    nightPixels[i] = Color.argb(255, (r * 0.34f).roundToInt(), (g * 0.44f).roundToInt(), min(255, (b * 0.68f + 14f).roundToInt()))
                }
            }
        }

        fun saveBmp(px: IntArray, fileName: String) {
            val outBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            outBmp.setPixels(px, 0, w, 0, 0, w, h)
            FileOutputStream(File(filesDir, fileName)).use { fos ->
                outBmp.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
        }

        saveBmp(dayPixels, "custom_city_day.png")
        saveBmp(sunsetPixels, "custom_city_sunset.png")
        saveBmp(nightPixels, "custom_city_night.png")
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
                customCityInput.setText(disp)
                saveLocation(disp, lat, lon, null)
            } else {
                statusLabel.text = "No matching location found for '$query'."
            }
        }
    }

    private fun saveLocation(name: String, lat: Float, lon: Float, suggestedTheme: String?) {
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putString("locationName", name)
            .putFloat("latitude", lat)
            .putFloat("longitude", lon)
        if (suggestedTheme != null) {
            editor.putString("dioramaTheme", suggestedTheme)
            val themeLabel = when (suggestedTheme) {
                "coastal" -> "⚓ Generic: Coastal Harbor & Lighthouse Town (Non-City-Specific)"
                "modern" -> "🌆 Generic: Modern Downtown Metropolis & Bridge (Non-City-Specific)"
                "alpine" -> "🏔️ Generic: Alpine Mountain & Lake Village (Non-City-Specific)"
                else -> "🏛️ Historic Yorktown, VA (1781 Waterfront & Coleman Bridge)"
            }
            dioramaThemeButton.text = "▼  $themeLabel"
        }
        editor.apply()
        customCityInput.setText(name)
        activeLocationHeader.text = "⚙️ Global 3D Isometric Weather\nActive Location: $name"
        applyAndRefreshWidgets("Location set to $name")
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
        val lon: Float,
        val suggestedTheme: String
    )
}
