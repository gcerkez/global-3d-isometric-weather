package com.antigravity.yorktownweather

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * ForecastActivity
 * ================
 * Premium 7-Day Weather & Precipitation Interactive Forecast View.
 *
 * Launched directly when the user taps the Homescreen Widget weather overlay
 * or via the 3D Preview app. Replaces the generic external web browser redirect
 * with a high-fidelity, native Android experience.
 *
 * Core Features:
 *  - 24-Hour Interactive Precipitation Graph with tactile finger scrubber & haptics
 *  - Metric switching between Precipitation (% & accumulation), Temperature spline, and Wind
 *  - Apple Weather-style 7-Day Forecast list with horizontal min/max temperature gradient range bars
 *    and real-time current temperature indicator dot
 *  - Interactive day selection: tapping any day filters the 24-hour graph and metrics to that day
 *  - Deep navy glassmorphism UI with smooth animations and tactile feedback
 */
class ForecastActivity : Activity() {

    data class DayForecast(
        val dateStr: String,
        val dayOfWeek: String,
        val formattedDate: String,
        val weatherCode: Int,
        val conditionLabel: String,
        val emoji: String,
        val tempMin: Double,
        val tempMax: Double,
        val precipProbMax: Int,
        val precipSum: Double,
        val sunrise: String,
        val sunset: String,
        val windSpeedMax: Double,
        val uvIndexMax: Double,
        val hourlyPoints: List<HourPoint>
    )

    data class HourPoint(
        val isoTime: String,
        val hourLabel: String,
        val hourOfDay: Int,
        val temp: Double,
        val feelsLike: Double,
        val precipProb: Int,
        val precipAmount: Double,
        val weatherCode: Int,
        val conditionText: String,
        val emoji: String,
        val windSpeed: Double,
        val humidity: Int,
        val uvIndex: Double
    )

    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    private var latitude: Double = 37.1310
    private var longitude: Double = -76.4452
    private var locationName: String = "Yorktown, VA"
    private var tempUnit: String = "F"
    private var windUnit: String = "MPH"
    private var precipUnit: String = "in"

    // UI Elements
    private lateinit var locationHeader: TextView
    private lateinit var currentTempView: TextView
    private lateinit var conditionView: TextView
    private lateinit var rangeFeelsView: TextView
    private lateinit var precipSummaryView: TextView

    // 7-Day Cards Container & State
    private lateinit var weeklyListContainer: LinearLayout
    private class DayRowHolder(
        val panel: LinearLayout,
        val tvDay: TextView,
        val chevron: TextView,
        val bodyContainer: FrameLayout,
        val graph: ForecastGraphView,
        val btnPrecip: Button,
        val btnTemp: Button,
        val btnWind: Button
    )
    private data class DayExpandedComponents(
        val view: View,
        val graph: ForecastGraphView,
        val btnPrecip: Button,
        val btnTemp: Button,
        val btnWind: Button
    )
    private val dayRowHolders = mutableListOf<DayRowHolder>()

    private var forecastDays: List<DayForecast> = emptyList()
    private var selectedDayIndex: Int = 0
    private var currentGraphMode: Int = ForecastGraphView.MODE_PRECIPITATION
    private var currentRealtimeTemp: Double = 68.0
    private var mainScrollView: ScrollView? = null
    private var radarCardView: View? = null
    private var radarWebView: WebView? = null

    private var currentForecastTabIndex: Int = 0
    private lateinit var tabContentContainer: FrameLayout
    private lateinit var tabForecastBtn: Button
    private lateinit var tabRadarBtn: Button
    private lateinit var forecastPageView: View
    private lateinit var radarPageView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read preferences or intent extras
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
        latitude = intent.getDoubleExtra("lat", prefs.getFloat("latitude", 37.1310f).toDouble())
        longitude = intent.getDoubleExtra("lon", prefs.getFloat("longitude", -76.4452f).toDouble())
        locationName = intent.getStringExtra("location_name") ?: prefs.getString("location_name", null) ?: prefs.getString("locationName", "Yorktown, VA") ?: "Yorktown, VA"
        tempUnit = intent.getStringExtra("temp_unit") ?: prefs.getString("temperatureUnit", null) ?: prefs.getString("temp_unit", "BOTH") ?: "BOTH"
        windUnit = intent.getStringExtra("wind_unit") ?: prefs.getString("windUnit", null) ?: prefs.getString("wind_unit", "MPH") ?: "MPH"
        precipUnit = if (tempUnit == "C") "mm" else "in"

        setContentView(buildRootLayout())

        // Load cached or placeholder data immediately so user sees UI instantly
        forecastDays = generateInitialForecastData()
        renderForecastData()

        // Fetch fresh 7-day data from Open-Meteo
        fetchSevenDayForecast()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
        radarWebView?.destroy()
        radarWebView = null
    }

    fun formatTemperature(tempVal: Double, unitMode: String = tempUnit): String {
        val fVal = if (unitMode == "C") ((tempVal * 9.0 / 5.0) + 32.0).roundToInt() else tempVal.roundToInt()
        val cVal = if (unitMode == "C") tempVal.roundToInt() else ((tempVal - 32.0) * 5.0 / 9.0).roundToInt()
        return when (unitMode) {
            "F" -> "${fVal}°F"
            "C" -> "${cVal}°C"
            "BOTH", "Dual" -> "${fVal}°F (${cVal}°C)"
            else -> "${fVal}°F"
        }
    }

    fun formatTempShort(tempVal: Double, unitMode: String = tempUnit): String {
        val fVal = if (unitMode == "C") ((tempVal * 9.0 / 5.0) + 32.0).roundToInt() else tempVal.roundToInt()
        val cVal = if (unitMode == "C") tempVal.roundToInt() else ((tempVal - 32.0) * 5.0 / 9.0).roundToInt()
        return when (unitMode) {
            "C" -> "${cVal}°"
            else -> "${fVal}°"
        }
    }

    private fun buildRootLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#070d18")) // Deep midnight navy
        }

        // 1. Top Navigation Bar (Back Button + Location + Refresh Button)
        root.addView(buildTopBar())

        // 2. Tab Bar: [ 📅 7-Day Forecast ] [ 📡 Live Radar ]
        root.addView(buildForecastTabBar())

        // 3. Tab Content Container
        tabContentContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }

        forecastPageView = buildForecastPage()
        radarPageView = buildLiveRadarCard()

        tabContentContainer.addView(forecastPageView)
        tabContentContainer.addView(radarPageView)

        root.addView(tabContentContainer)

        // Select initial tab (0: 7-Day Forecast)
        selectForecastTab(0)

        return root
    }

    private fun buildForecastTabBar(): View {
        val tabRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), 0, dp(16), dp(10))
        }

        tabForecastBtn = Button(this).apply {
            text = "📅 7-Day Forecast"
            textSize = 12f
            isAllCaps = false
            setPadding(dp(14), dp(6), dp(14), dp(6))
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1.0f).apply {
                marginEnd = dp(8)
            }
            applyPressAnimation(this)
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                selectForecastTab(0)
            }
        }
        tabRow.addView(tabForecastBtn)

        tabRadarBtn = Button(this).apply {
            text = "📡 Live Doppler Radar"
            textSize = 12f
            isAllCaps = false
            setPadding(dp(14), dp(6), dp(14), dp(6))
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1.0f)
            applyPressAnimation(this)
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                selectForecastTab(1)
            }
        }
        tabRow.addView(tabRadarBtn)

        return tabRow
    }

    private fun selectForecastTab(idx: Int) {
        currentForecastTabIndex = idx
        if (::forecastPageView.isInitialized && ::radarPageView.isInitialized) {
            forecastPageView.visibility = if (idx == 0) View.VISIBLE else View.GONE
            radarPageView.visibility = if (idx == 1) View.VISIBLE else View.GONE
        }
        if (::tabForecastBtn.isInitialized && ::tabRadarBtn.isInitialized) {
            val isForecast = (idx == 0)
            tabForecastBtn.setTextColor(if (isForecast) Color.WHITE else Color.parseColor("#8da8c4"))
            tabForecastBtn.typeface = Typeface.create(Typeface.DEFAULT, if (isForecast) Typeface.BOLD else Typeface.NORMAL)
            tabForecastBtn.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(if (isForecast) Color.parseColor("#1f6feb") else Color.parseColor("#132232"))
                setStroke(dp(1), if (isForecast) Color.parseColor("#58a6ff") else Color.parseColor("#263d54"))
            }

            val isRadar = (idx == 1)
            tabRadarBtn.setTextColor(if (isRadar) Color.WHITE else Color.parseColor("#8da8c4"))
            tabRadarBtn.typeface = Typeface.create(Typeface.DEFAULT, if (isRadar) Typeface.BOLD else Typeface.NORMAL)
            tabRadarBtn.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(if (isRadar) Color.parseColor("#1f6feb") else Color.parseColor("#132232"))
                setStroke(dp(1), if (isRadar) Color.parseColor("#58a6ff") else Color.parseColor("#263d54"))
            }
        }
    }

    private fun buildForecastPage(): View {
        val rootScroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#070d18")) // Deep midnight navy
            isFillViewport = true
        }
        mainScrollView = rootScroll

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(16)
            setPadding(pad, dp(4), pad, dp(32))
        }

        // 1. Compact Hero Weather Status Card
        content.addView(buildHeroWeatherCard())

        // 2. 7-Day Outlook & Expansion Panels (Immediately below hero card without any radar in the middle!)
        content.addView(buildSevenDayExpansionCard())

        rootScroll.addView(content)
        return rootScroll
    }

    private fun buildTopBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(8))
        }

        // Back button with depress animation
        val btnBack = Button(this).apply {
            text = "←"
            textSize = 20f
            setTextColor(Color.WHITE)
            background = makeCardBackground(Color.parseColor("#14213d"), Color.parseColor("#1f3256"), dp(12))
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            applyPressAnimation(this)
            setOnClickListener { finish() }
            contentDescription = "Back"
        }
        bar.addView(btnBack)

        val titleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                marginStart = dp(12)
                marginEnd = dp(12)
            }
        }

        locationHeader = TextView(this).apply {
            text = "📍 $locationName"
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            maxLines = 1
        }
        titleContainer.addView(locationHeader)

        val subHeader = TextView(this).apply {
            text = "7-Day Outlook & Hourly Forecast"
            textSize = 11.5f
            setTextColor(Color.parseColor("#94a3b8"))
        }
        titleContainer.addView(subHeader)
        bar.addView(titleContainer)

        // Refresh button with depress animation
        val btnRefresh = Button(this).apply {
            text = "⟳"
            textSize = 19f
            setTextColor(Color.parseColor("#ffd166"))
            background = makeCardBackground(Color.parseColor("#14213d"), Color.parseColor("#1f3256"), dp(12))
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            applyPressAnimation(this)
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                fetchSevenDayForecast()
            }
            contentDescription = "Refresh Forecast"
        }
        bar.addView(btnRefresh)

        return bar
    }

    private fun buildHeroWeatherCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeCardBackground(Color.parseColor("#0e1726"), Color.parseColor("#1e293b"), dp(16))
            val pad = dp(14)
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
        }

        val rowTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        currentTempView = TextView(this).apply {
            text = formatTemperature(currentRealtimeTemp)
            textSize = if (tempUnit == "BOTH" || tempUnit == "Dual") 28f else 38f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        rowTop.addView(currentTempView)

        val condBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(14)
            }
        }

        conditionView = TextView(this).apply {
            text = "☀️ Clear Sky"
            textSize = 16f
            setTextColor(Color.parseColor("#ffd166"))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        condBox.addView(conditionView)

        rangeFeelsView = TextView(this).apply {
            text = "High 72° • Low 48° • Wind 8 MPH"
            textSize = 11.5f
            setTextColor(Color.parseColor("#cbd5e1"))
        }
        condBox.addView(rangeFeelsView)
        rowTop.addView(condBox)
        card.addView(rowTop)

        // Summary chip & Radar jump button
        val chipRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            }
        }

        precipSummaryView = TextView(this).apply {
            text = "☀️ Dry conditions • 0% chance of rain"
            textSize = 11.5f
            setTextColor(Color.parseColor("#38bdf8"))
            background = makeCardBackground(Color.parseColor("#142236"), Color.parseColor("#1e3a5f"), dp(8))
            setPadding(dp(10), dp(5), dp(10), dp(5))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                marginEnd = dp(8)
            }
        }
        chipRow.addView(precipSummaryView)

        val btnJumpRadar = Button(this).apply {
            text = "📡 Live Radar Tab →"
            textSize = 11f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            background = makeCardBackground(Color.parseColor("#0284c7"), Color.parseColor("#38bdf8"), dp(8))
            setPadding(dp(10), dp(4), dp(10), dp(4))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34))
            applyPressAnimation(this)
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                selectForecastTab(1)
            }
        }
        chipRow.addView(btnJumpRadar)
        card.addView(chipRow)

        return card
    }

    private fun buildLiveRadarCard(): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#070d18"))
            val pad = dp(16)
            setPadding(pad, dp(4), pad, dp(16))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Header Row
        val headerCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = makeCardBackground(Color.parseColor("#0a1422"), Color.parseColor("#1c304a"), dp(14))
            val padH = dp(14)
            setPadding(padH, dp(10), padH, dp(10))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
        }

        val textGroup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        val title = TextView(this).apply {
            text = "📡 LIVE DOPPLER RADAR COMPOSITE"
            textSize = 13.5f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        textGroup.addView(title)

        val sub = TextView(this).apply {
            text = "Real-time precipitation radar & nowcast loop for $locationName"
            textSize = 11f
            setTextColor(Color.parseColor("#94a3b8"))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(2)
            }
        }
        textGroup.addView(sub)
        headerCard.addView(textGroup)

        val btnReloadRadar = Button(this).apply {
            text = "⟳"
            textSize = 16f
            setTextColor(Color.parseColor("#38bdf8"))
            background = makeCardBackground(Color.parseColor("#13273e"), Color.parseColor("#25456b"), dp(10))
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
            applyPressAnimation(this)
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                radarWebView?.reload()
            }
            contentDescription = "Reload Radar"
        }
        headerCard.addView(btnReloadRadar)
        layout.addView(headerCard)

        // Radar WebView Container with rounded border - takes FULL remaining vertical space!
        val frame = FrameLayout(this).apply {
            background = makeCardBackground(Color.parseColor("#070d18"), Color.parseColor("#1e334d"), dp(12))
            clipChildren = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }

        val wv = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            @Suppress("DEPRECATION")
            settings.allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            settings.allowUniversalAccessFromFileURLs = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            setBackgroundColor(Color.parseColor("#070d18"))

            val encodedName = android.net.Uri.encode(locationName)
            loadUrl("file:///android_asset/web/index.html#radar?lat=$latitude&lon=$longitude&name=$encodedName")
        }
        radarWebView = wv
        frame.addView(wv, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        layout.addView(frame)

        radarCardView = layout
        return layout
    }

    private fun buildSevenDayExpansionCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeCardBackground(Color.parseColor("#0a121e"), Color.parseColor("#1a273b"), dp(18))
            val pad = dp(14)
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val title = TextView(this).apply {
            text = "📅 7-DAY OUTLOOK & HOURLY BREAKDOWN"
            textSize = 13.5f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        card.addView(title)

        val hint = TextView(this).apply {
            text = "Tap any day to expand its interactive 24-hour graph & condition details."
            textSize = 11f
            setTextColor(Color.parseColor("#94a3b8"))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(2)
                bottomMargin = dp(10)
            }
        }
        card.addView(hint)

        weeklyListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        card.addView(weeklyListContainer)

        return card
    }

    private fun buildExpandedDayComponent(day: DayForecast): DayExpandedComponents {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padH = dp(10)
            setPadding(padH, dp(4), padH, dp(12))
        }

        // Material Divider
        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#1e293b"))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
                topMargin = dp(2)
                bottomMargin = dp(8)
            }
        }
        container.addView(divider)

        // Header controls (Title + Segmented Button Toggle Group)
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvGraphTitle = TextView(this).apply {
            text = "Hourly Breakdown • ${day.dayOfWeek}"
            textSize = 12.5f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        headerRow.addView(tvGraphTitle)

        // Segmented Button Toggle Group (Angular Material Button Toggle / PrimeUI SelectButton)
        val pillBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val bPrecip = makeMetricPillButton("🌧️ Precip", active = (currentGraphMode == ForecastGraphView.MODE_PRECIPITATION)) {
            switchGraphMode(ForecastGraphView.MODE_PRECIPITATION)
        }
        pillBar.addView(bPrecip)

        val bTemp = makeMetricPillButton("🌡️ Temp", active = (currentGraphMode == ForecastGraphView.MODE_TEMPERATURE)) {
            switchGraphMode(ForecastGraphView.MODE_TEMPERATURE)
        }
        pillBar.addView(bTemp)

        val bWind = makeMetricPillButton("💨 Wind", active = (currentGraphMode == ForecastGraphView.MODE_WIND)) {
            switchGraphMode(ForecastGraphView.MODE_WIND)
        }
        pillBar.addView(bWind)

        headerRow.addView(pillBar)
        container.addView(headerRow)

        // Scrubber Readout View
        val tvScrubber = TextView(this).apply {
            text = "👆 Drag horizontally across the graph to inspect hourly precipitation & conditions."
            textSize = 10f
            setTextColor(Color.parseColor("#94a3b8"))
            gravity = Gravity.CENTER
            background = makeCardBackground(Color.parseColor("#142236"), Color.parseColor("#1e3a5f"), dp(6))
            val pad = dp(6)
            setPadding(pad, dp(4), pad, dp(4))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6)
            }
        }

        // Custom 24-Hour Graph Canvas
        val graphView = ForecastGraphView(this).apply {
            tempUnit = this@ForecastActivity.tempUnit
            windUnit = this@ForecastActivity.windUnit
            precipitationUnit = this@ForecastActivity.precipUnit
            hourlyPoints = day.hourlyPoints
            graphMode = currentGraphMode
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(190)).apply {
                topMargin = dp(8)
            }
            onHourScrubbed = { pt ->
                pt?.let {
                    tvScrubber.text = "${it.hourLabel} • ${it.emoji} ${it.conditionText} • 💧 ${it.precipProb}% • ${formatTemperature(it.temp)}"
                }
            }
        }
        container.addView(graphView)
        container.addView(tvScrubber)

        // Day's 4 Key Metric Tiles (Material Sub-Cards)
        val precipAmtStr = if (precipUnit == "mm") "${String.format("%.1f", day.precipSum)} mm" else "${String.format("%.2f", day.precipSum)} in"
        val uvLabel = when {
            day.uvIndexMax < 3.0 -> "Low"
            day.uvIndexMax < 6.0 -> "Moderate"
            day.uvIndexMax < 8.0 -> "High"
            else -> "Very High"
        }
        val avgHum = if (day.hourlyPoints.isNotEmpty()) (day.hourlyPoints.map { it.humidity }.average()).roundToInt() else 50

        val metricsBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            }
        }

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val (tilePrecip, _, _) = makeMetricTile("🌧️ PRECIPITATION", precipAmtStr, "Peak chance: ${day.precipProbMax}%")
        row1.addView(tilePrecip)

        val (tileSun, _, _) = makeMetricTile("🌅 SUN CYCLE", day.sunrise, "Sunset: ${day.sunset}")
        row1.addView(tileSun)
        metricsBox.addView(row1)

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6)
            }
        }
        val (tileWind, _, _) = makeMetricTile("💨 WIND & GUSTS", "${day.windSpeedMax.roundToInt()} $windUnit", "Max sustained gusts")
        row2.addView(tileWind)

        val (tileUv, _, _) = makeMetricTile("☀️ UV & HUMIDITY", "${String.format("%.1f", day.uvIndexMax)} $uvLabel", "Avg Humidity: $avgHum%")
        row2.addView(tileUv)
        metricsBox.addView(row2)

        container.addView(metricsBox)

        return DayExpandedComponents(container, graphView, bPrecip, bTemp, bWind)
    }

    private fun makeMetricPillButton(label: String, active: Boolean, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val padH = dp(8)
            val padV = dp(4)
            setPadding(padH, padV, padH, padV)
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            isAllCaps = false
            updatePillState(this, active)
            applyPressAnimation(this)
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(28)).apply {
                marginStart = dp(4)
            }
        }
    }

    private fun updatePillState(btn: Button, active: Boolean) {
        if (active) {
            btn.setTextColor(Color.parseColor("#070d18"))
            btn.background = makeCardBackground(Color.parseColor("#ffd166"), Color.parseColor("#ffe082"), dp(14))
        } else {
            btn.setTextColor(Color.parseColor("#94a3b8"))
            btn.background = makeCardBackground(Color.parseColor("#14213d"), Color.parseColor("#1f3256"), dp(14))
        }
    }

    private fun switchGraphMode(mode: Int) {
        currentGraphMode = mode
        for (holder in dayRowHolders) {
            holder.graph.graphMode = mode
            updatePillState(holder.btnPrecip, mode == ForecastGraphView.MODE_PRECIPITATION)
            updatePillState(holder.btnTemp, mode == ForecastGraphView.MODE_TEMPERATURE)
            updatePillState(holder.btnWind, mode == ForecastGraphView.MODE_WIND)
            holder.graph.invalidate()
        }
    }

    private fun makeMetricTile(title: String, initialVal: String, initialSub: String): Triple<View, TextView, TextView> {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeCardBackground(Color.parseColor("#14213d"), Color.parseColor("#1f3256"), dp(12))
            val pad = dp(10)
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        }

        val tvTitle = TextView(this).apply {
            text = title
            textSize = 10f
            setTextColor(Color.parseColor("#94a3b8"))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        tile.addView(tvTitle)

        val tvVal = TextView(this).apply {
            text = initialVal
            textSize = 15f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(3)
            }
        }
        tile.addView(tvVal)

        val tvSub = TextView(this).apply {
            text = initialSub
            textSize = 10.5f
            setTextColor(Color.parseColor("#38bdf8"))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(2)
            }
        }
        tile.addView(tvSub)

        return Triple(tile, tvVal, tvSub)
    }

    private fun renderForecastData() {
        if (forecastDays.isEmpty()) return
        val selDay = forecastDays[selectedDayIndex.coerceIn(0, forecastDays.size - 1)]
        updateHeroCard(selDay)
        populateWeeklyList()
    }

    private fun updateHeroCard(selDay: DayForecast) {
        currentTempView.text = formatTemperature(currentRealtimeTemp)
        currentTempView.textSize = if (tempUnit == "BOTH" || tempUnit == "Dual") 30f else 40f
        conditionView.text = "${selDay.emoji} ${selDay.conditionLabel}"
        val highStr = formatTempShort(selDay.tempMax)
        val lowStr = formatTempShort(selDay.tempMin)
        rangeFeelsView.text = "High $highStr • Low $lowStr • Wind ${selDay.windSpeedMax.roundToInt()} $windUnit"

        val chipIcon = if (selDay.precipProbMax <= 25) "🌦️" else "🌧️"
        val rainChip = if (selDay.precipSum > 0.005) {
            val amt = if (precipUnit == "mm") "${String.format("%.1f", selDay.precipSum)}mm" else "${String.format("%.2f", selDay.precipSum)} in"
            "$chipIcon $amt precipitation expected (${selDay.precipProbMax}% max probability)"
        } else if (selDay.precipProbMax > 0) {
            val smallChip = if (selDay.precipProbMax <= 25) "🌦️" else "💧"
            "$smallChip ${selDay.precipProbMax}% chance of rain"
        } else {
            "☀️ Dry conditions • 0% chance of rain"
        }
        precipSummaryView.text = rainChip
    }

    private fun selectDayRow(newIndex: Int) {
        if (newIndex !in forecastDays.indices) return

        val transition = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(ChangeBounds())
            addTransition(Fade())
            duration = 240
            interpolator = DecelerateInterpolator(1.5f)
        }
        TransitionManager.beginDelayedTransition(weeklyListContainer, transition)

        if (newIndex == selectedDayIndex) {
            val h = dayRowHolders[selectedDayIndex]
            h.panel.background = makeCardBackground(Color.parseColor("#0e1726"), Color.parseColor("#1e293b"), dp(12))
            h.tvDay.setTextColor(Color.WHITE)
            h.chevron.animate().rotation(0f).setDuration(200).start()
            h.chevron.setTextColor(Color.parseColor("#64748b"))
            h.bodyContainer.visibility = View.GONE
            selectedDayIndex = -1
            return
        }

        val oldIndex = selectedDayIndex
        selectedDayIndex = newIndex

        // 1. Collapse previous active panel smoothly
        if (oldIndex in dayRowHolders.indices) {
            val oldH = dayRowHolders[oldIndex]
            oldH.panel.background = makeCardBackground(Color.parseColor("#0e1726"), Color.parseColor("#1e293b"), dp(12))
            oldH.tvDay.setTextColor(Color.WHITE)
            oldH.chevron.animate().rotation(0f).setDuration(200).start()
            oldH.chevron.setTextColor(Color.parseColor("#64748b"))
            oldH.bodyContainer.visibility = View.GONE
        }

        // 2. Expand new active panel smoothly
        if (newIndex in dayRowHolders.indices) {
            val newH = dayRowHolders[newIndex]
            newH.panel.background = makeCardBackground(Color.parseColor("#122034"), Color.parseColor("#38bdf8"), dp(14), strokeWidth = dp(1.5f))
            newH.tvDay.setTextColor(Color.parseColor("#ffd166"))
            newH.chevron.animate().rotation(180f).setDuration(200).start()
            newH.chevron.setTextColor(Color.parseColor("#38bdf8"))

            newH.graph.graphMode = currentGraphMode
            updatePillState(newH.btnPrecip, currentGraphMode == ForecastGraphView.MODE_PRECIPITATION)
            updatePillState(newH.btnTemp, currentGraphMode == ForecastGraphView.MODE_TEMPERATURE)
            updatePillState(newH.btnWind, currentGraphMode == ForecastGraphView.MODE_WIND)
            newH.graph.invalidate()

            newH.bodyContainer.visibility = View.VISIBLE
        }

        // 3. Update top Hero Card
        updateHeroCard(forecastDays[newIndex])
    }

    private fun populateWeeklyList() {
        weeklyListContainer.removeAllViews()
        dayRowHolders.clear()

        val weekMinTemp = forecastDays.minOf { it.tempMin }.toFloat()
        val weekMaxTemp = forecastDays.maxOf { it.tempMax }.toFloat()

        for (i in forecastDays.indices) {
            val day = forecastDays[i]
            val isExpanded = (i == selectedDayIndex)
            val isToday = (i == 0)

            val panel = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val bg = if (isExpanded) {
                    makeCardBackground(Color.parseColor("#122034"), Color.parseColor("#38bdf8"), dp(14), strokeWidth = dp(1.5f))
                } else {
                    makeCardBackground(Color.parseColor("#0e1726"), Color.parseColor("#1e293b"), dp(12))
                }
                background = bg
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(8)
                }
            }

            // Header (mat-expansion-panel-header)
            val headerRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val padH = dp(12)
                val padV = dp(11)
                setPadding(padH, padV, padH, padV)
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    selectDayRow(i)
                }
            }

            // Day Title
            val tvDay = TextView(this).apply {
                text = if (isToday) "TODAY" else day.dayOfWeek
                textSize = 12.5f
                setTextColor(if (isExpanded) Color.parseColor("#ffd166") else Color.WHITE)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            headerRow.addView(tvDay)

            // Weather Icon & Prob (mat-badge / p-badge)
            val iconBox = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(dp(62), ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            val tvIcon = TextView(this).apply {
                text = day.emoji
                textSize = 16f
            }
            iconBox.addView(tvIcon)

            if (day.precipProbMax >= 10 && (day.emoji == "🌦️" || day.emoji == "🌧️" || day.emoji == "🌨️" || day.emoji == "⛈️" || day.precipSum > 0.005 || day.precipProbMax >= 20)) {
                val tvProb = TextView(this).apply {
                    text = "${day.precipProbMax}%"
                    textSize = 10f
                    setTextColor(Color.parseColor("#38bdf8"))
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        marginStart = dp(3)
                    }
                }
                iconBox.addView(tvProb)
            }
            headerRow.addView(iconBox)

            // Min Temp
            val tvMin = TextView(this).apply {
                text = formatTempShort(day.tempMin)
                textSize = 12f
                setTextColor(Color.parseColor("#94a3b8"))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            headerRow.addView(tvMin)

            // Apple Weather style Temp Range Bar
            val rangeBar = TempRangeBarView(this).apply {
                weekMin = weekMinTemp
                weekMax = weekMaxTemp
                dayMin = day.tempMin.toFloat()
                dayMax = day.tempMax.toFloat()
                currentTemp = if (isToday) currentRealtimeTemp.toFloat() else null
                layoutParams = LinearLayout.LayoutParams(0, dp(14), 1.0f).apply {
                    marginStart = dp(8)
                    marginEnd = dp(8)
                }
            }
            headerRow.addView(rangeBar)

            // Max Temp
            val tvMax = TextView(this).apply {
                text = formatTempShort(day.tempMax)
                textSize = 12.5f
                setTextColor(Color.WHITE)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                gravity = Gravity.START
                layoutParams = LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            headerRow.addView(tvMax)

            // Material Expansion Chevron Indicator
            val chevron = TextView(this).apply {
                text = "▼"
                textSize = 10f
                rotation = if (isExpanded) 180f else 0f
                setTextColor(if (isExpanded) Color.parseColor("#38bdf8") else Color.parseColor("#64748b"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(18), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginStart = dp(4)
                }
            }
            headerRow.addView(chevron)

            panel.addView(headerRow)

            // Expansion Body Container
            val bodyContainer = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                visibility = if (isExpanded) View.VISIBLE else View.GONE
            }

            val expandedComp = buildExpandedDayComponent(day)
            bodyContainer.addView(expandedComp.view)

            panel.addView(bodyContainer)

            dayRowHolders.add(
                DayRowHolder(
                    panel = panel,
                    tvDay = tvDay,
                    chevron = chevron,
                    bodyContainer = bodyContainer,
                    graph = expandedComp.graph,
                    btnPrecip = expandedComp.btnPrecip,
                    btnTemp = expandedComp.btnTemp,
                    btnWind = expandedComp.btnWind
                )
            )
            weeklyListContainer.addView(panel)
        }
    }

    private fun fetchSevenDayForecast() {
        activityScope.launch(Dispatchers.IO) {
            val tempParam = if (tempUnit == "C") "celsius" else "fahrenheit"
            val windParam = if (windUnit == "kmh") "kmh" else if (windUnit == "ms") "ms" else if (windUnit == "kn") "kn" else "mph"
            val precipParam = if (tempUnit == "C") "mm" else "inch"

            val urlStr = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,wind_direction_10m" +
                "&hourly=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,wind_speed_10m,uv_index" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,sunrise,sunset,wind_speed_10m_max,uv_index_max" +
                "&temperature_unit=$tempParam&wind_speed_unit=$windParam&precipitation_unit=$precipParam&timezone=auto&forecast_days=7"

            try {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)

                val parsedDays = parseOpenMeteoJson(json)
                val curTemp = json.optJSONObject("current")?.optDouble("temperature_2m", currentRealtimeTemp) ?: currentRealtimeTemp

                withContext(Dispatchers.Main) {
                    if (parsedDays.isNotEmpty()) {
                        forecastDays = parsedDays
                        currentRealtimeTemp = curTemp
                        renderForecastData()
                    }
                }
            } catch (e: Exception) {
                // Network error: continue displaying cached/placeholder data gracefully
            }
        }
    }

    private fun parseOpenMeteoJson(json: JSONObject): List<DayForecast> {
        val daily = json.optJSONObject("daily") ?: return emptyList()
        val hourly = json.optJSONObject("hourly") ?: return emptyList()

        val dailyTimes = daily.optJSONArray("time") ?: return emptyList()
        val dailyCodes = daily.optJSONArray("weather_code")
        val dailyMaxT = daily.optJSONArray("temperature_2m_max")
        val dailyMinT = daily.optJSONArray("temperature_2m_min")
        val dailyPrecipSum = daily.optJSONArray("precipitation_sum")
        val dailyPrecipProb = daily.optJSONArray("precipitation_probability_max")
        val dailySunrise = daily.optJSONArray("sunrise")
        val dailySunset = daily.optJSONArray("sunset")
        val dailyWindMax = daily.optJSONArray("wind_speed_10m_max")
        val dailyUvMax = daily.optJSONArray("uv_index_max")

        val hourlyTimes = hourly.optJSONArray("time")
        val hourlyTemps = hourly.optJSONArray("temperature_2m")
        val hourlyFeels = hourly.optJSONArray("apparent_temperature")
        val hourlyPrecipProb = hourly.optJSONArray("precipitation_probability")
        val hourlyPrecip = hourly.optJSONArray("precipitation")
        val hourlyCodes = hourly.optJSONArray("weather_code")
        val hourlyWind = hourly.optJSONArray("wind_speed_10m")
        val hourlyHum = hourly.optJSONArray("relative_humidity_2m")
        val hourlyUv = hourly.optJSONArray("uv_index")

        val out = mutableListOf<DayForecast>()
        val dayNameFormat = SimpleDateFormat("EEE", Locale.US)
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val shortDateFormat = SimpleDateFormat("MMM d", Locale.US)
        val timeLabelFormat = SimpleDateFormat("h a", Locale.US)
        val isoTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)

        val totalDays = dailyTimes.length().coerceAtMost(7)

        for (d in 0 until totalDays) {
            val dateStr = dailyTimes.optString(d, "")
            val parsedDate = runCatching { isoFormat.parse(dateStr) }.getOrNull() ?: Date()
            val dayOfWeek = if (d == 0) "TODAY" else dayNameFormat.format(parsedDate).uppercase()
            val formattedDate = shortDateFormat.format(parsedDate)

            val code = dailyCodes?.optInt(d, 0) ?: 0
            val maxT = dailyMaxT?.optDouble(d, 70.0) ?: 70.0
            val minT = dailyMinT?.optDouble(d, 50.0) ?: 50.0
            val pSum = dailyPrecipSum?.optDouble(d, 0.0) ?: 0.0
            val pProb = dailyPrecipProb?.optInt(d, 0) ?: 0

            val rawSunrise = dailySunrise?.optString(d, "") ?: ""
            val rawSunset = dailySunset?.optString(d, "") ?: ""
            val sunriseStr = formatIsoTime(rawSunrise, timeLabelFormat) ?: "7:00 AM"
            val sunsetStr = formatIsoTime(rawSunset, timeLabelFormat) ?: "6:30 PM"

            val maxWind = dailyWindMax?.optDouble(d, 10.0) ?: 10.0
            val maxUv = dailyUvMax?.optDouble(d, 4.0) ?: 4.0

            // Collect 24 hours corresponding to this day
            val hours = mutableListOf<HourPoint>()
            val startHourIdx = d * 24
            val endHourIdx = startHourIdx + 24

            if (hourlyTimes != null && startHourIdx < hourlyTimes.length()) {
                val bound = endHourIdx.coerceAtMost(hourlyTimes.length())
                for (h in startHourIdx until bound) {
                    val rawTime = hourlyTimes.optString(h, "")
                    val hDate = runCatching { isoTimeFormat.parse(rawTime) }.getOrNull() ?: Date()
                    val hLabel = timeLabelFormat.format(hDate)
                    val cal = Calendar.getInstance().apply { time = hDate }
                    val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)

                    val t = hourlyTemps?.optDouble(h, maxT) ?: maxT
                    val f = hourlyFeels?.optDouble(h, t) ?: t
                    val pop = hourlyPrecipProb?.optInt(h, pProb) ?: pProb
                    val pAmt = hourlyPrecip?.optDouble(h, 0.0) ?: 0.0
                    val hCode = hourlyCodes?.optInt(h, code) ?: code
                    val wSpeed = hourlyWind?.optDouble(h, maxWind) ?: maxWind
                    val hum = hourlyHum?.optInt(h, 50) ?: 50
                    val uv = hourlyUv?.optDouble(h, 0.0) ?: 0.0

                    val (hEmoji, hLabelText) = wmoCodeToInfo(hCode, hourOfDay in 6..19, pop)

                    hours.add(
                        HourPoint(
                            isoTime = rawTime,
                            hourLabel = hLabel,
                            hourOfDay = hourOfDay,
                            temp = t,
                            feelsLike = f,
                            precipProb = pop,
                            precipAmount = pAmt,
                            weatherCode = hCode,
                            conditionText = hLabelText,
                            emoji = hEmoji,
                            windSpeed = wSpeed,
                            humidity = hum,
                            uvIndex = uv
                        )
                    )
                }
            }

            val (dEmoji, dLabelText) = wmoCodeToInfo(code, isDay = true, precipProb = pProb)

            out.add(
                DayForecast(
                    dateStr = dateStr,
                    dayOfWeek = dayOfWeek,
                    formattedDate = formattedDate,
                    weatherCode = code,
                    conditionLabel = dLabelText,
                    emoji = dEmoji,
                    tempMin = minT,
                    tempMax = maxT,
                    precipProbMax = pProb,
                    precipSum = pSum,
                    sunrise = sunriseStr,
                    sunset = sunsetStr,
                    windSpeedMax = maxWind,
                    uvIndexMax = maxUv,
                    hourlyPoints = hours
                )
            )
        }

        return out
    }

    private fun formatIsoTime(iso: String, outFormat: SimpleDateFormat): String? {
        if (iso.isEmpty()) return null
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            val d = parser.parse(iso) ?: return null
            outFormat.format(d)
        }.getOrNull()
    }

    private fun generateInitialForecastData(): List<DayForecast> {
        val days = listOf("TODAY", "WED", "THU", "FRI", "SAT", "SUN", "MON")
        val out = mutableListOf<DayForecast>()
        val baseMax = if (tempUnit == "C") 21.0 else 70.0
        val baseMin = if (tempUnit == "C") 10.0 else 50.0

        for (i in days.indices) {
            val maxT = baseMax + (i % 3) * 2.0 - (i % 2) * 1.5
            val minT = baseMin + (i % 2) * 2.0
            val pProb = if (i == 1 || i == 4) 45 else 0
            val pAmt = if (pProb > 0) (if (precipUnit == "mm") 6.5 else 0.25) else 0.0

            val hours = mutableListOf<HourPoint>()
            for (h in 0..23) {
                val label = when (h) {
                    0 -> "12 AM"
                    12 -> "12 PM"
                    in 1..11 -> "$h AM"
                    else -> "${h - 12} PM"
                }
                val t = minT + (maxT - minT) * (0.5 + 0.5 * kotlin.math.sin((h - 8) * Math.PI / 12.0))
                val hourProb = if (pProb > 0 && h in 13..19) pProb else 0
                val hourAmt = if (hourProb > 0) (pAmt / 6.0) else 0.0
                val hCode = if (hourProb > 0) 61 else 0
                val (hEmoji, hLabelText) = wmoCodeToInfo(hCode, h in 6..19, hourProb)

                hours.add(
                    HourPoint(
                        isoTime = "2026-10-2${i}T${String.format("%02d", h)}:00",
                        hourLabel = label,
                        hourOfDay = h,
                        temp = t,
                        feelsLike = t - 1.5,
                        precipProb = hourProb,
                        precipAmount = hourAmt,
                        weatherCode = hCode,
                        conditionText = hLabelText,
                        emoji = hEmoji,
                        windSpeed = 8.0 + (h % 5),
                        humidity = 55 + (h % 20),
                        uvIndex = if (h in 10..16) 4.5 else 0.0
                    )
                )
            }

            val dayCode = if (pProb > 0) 61 else 0
            val (dEmoji, dLabelText) = wmoCodeToInfo(dayCode, isDay = true, precipProb = pProb)

            out.add(
                DayForecast(
                    dateStr = "2026-10-2$i",
                    dayOfWeek = days[i],
                    formattedDate = "Oct ${20 + i}",
                    weatherCode = dayCode,
                    conditionLabel = dLabelText,
                    emoji = dEmoji,
                    tempMin = minT,
                    tempMax = maxT,
                    precipProbMax = pProb,
                    precipSum = pAmt,
                    sunrise = "7:14 AM",
                    sunset = "6:21 PM",
                    windSpeedMax = 12.0,
                    uvIndexMax = 4.8,
                    hourlyPoints = hours
                )
            )
        }
        return out
    }

    fun wmoCodeToInfo(code: Int, isDay: Boolean, precipProb: Int = 0): Pair<String, String> {
        return when (code) {
            0 -> if (isDay) "☀️" to "Clear Sky" else "🌙" to "Clear Sky"
            1 -> if (isDay) "🌤️" to "Mainly Clear" else "🌤️" to "Mainly Clear"
            2 -> "⛅" to "Partly Cloudy"
            3 -> "☁️" to "Overcast"
            45, 48 -> "🌫️" to "Foggy"
            51, 53, 55 -> { // Drizzle
                if (isDay) {
                    if (precipProb <= 20) "🌦️" to "Stray Drizzle"
                    else "🌦️" to "Drizzle"
                } else {
                    "🌧️" to "Drizzle"
                }
            }
            56, 57 -> "🌨️" to "Freezing Drizzle"
            61 -> { // Slight Rain
                if (isDay) {
                    when {
                        precipProb <= 20 -> "🌦️" to "Isolated Shower"
                        precipProb <= 40 -> "🌦️" to "Passing Showers"
                        precipProb < 60 -> "🌦️" to "Light Rain"
                        else -> "🌧️" to "Light Rain"
                    }
                } else {
                    if (precipProb <= 20) "🌧️" to "Isolated Shower" else "🌧️" to "Light Rain"
                }
            }
            63 -> { // Moderate Rain
                if (isDay && precipProb <= 20) "🌦️" to "Passing Shower"
                else "🌧️" to "Rain"
            }
            65 -> "🌧️" to "Heavy Rain"
            66, 67 -> "🌨️" to "Freezing Rain"
            71, 73, 75, 77 -> "🌨️" to "Snow"
            80 -> { // Rain Showers: Slight
                if (isDay) {
                    when {
                        precipProb <= 20 -> "🌦️" to "Stray Shower"
                        precipProb <= 50 -> "🌦️" to "Scattered Showers"
                        else -> "🌧️" to "Rain Showers"
                    }
                } else {
                    if (precipProb <= 20) "🌧️" to "Stray Shower" else "🌧️" to "Rain Showers"
                }
            }
            81 -> { // Rain Showers: Moderate
                if (isDay && precipProb <= 20) "🌦️" to "Passing Shower"
                else "🌧️" to "Rain Showers"
            }
            82 -> "🌧️" to "Violent Rain Showers"
            85, 86 -> "🌨️" to "Snow Showers"
            95, 96, 99 -> "⛈️" to "Thunderstorm"
            else -> if (isDay) "☀️" to "Sunny" else "🌙" to "Clear"
        }
    }

    private fun makeCardBackground(fillColor: Int, strokeColor: Int, cornerRadiusDp: Int, strokeWidth: Int = dp(1)): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            setStroke(strokeWidth, strokeColor)
            cornerRadius = cornerRadiusDp.toFloat()
        }
    }

    private fun applyPressAnimation(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                }
            }
            false
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()
    private fun dp(v: Float): Int = (v * resources.displayMetrics.density).roundToInt()
}
