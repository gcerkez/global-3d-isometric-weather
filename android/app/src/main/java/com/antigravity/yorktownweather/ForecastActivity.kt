package com.antigravity.yorktownweather

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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

    private lateinit var graphTitleView: TextView
    private lateinit var forecastGraph: ForecastGraphView
    private lateinit var scrubberReadoutView: TextView

    private lateinit var btnModePrecip: Button
    private lateinit var btnModeTemp: Button
    private lateinit var btnModeWind: Button

    // Metric Grid Views
    private lateinit var metricPrecipVal: TextView
    private lateinit var metricPrecipSub: TextView
    private lateinit var metricSunVal: TextView
    private lateinit var metricSunSub: TextView
    private lateinit var metricWindVal: TextView
    private lateinit var metricWindSub: TextView
    private lateinit var metricUvVal: TextView
    private lateinit var metricUvSub: TextView

    // 7-Day Cards Container
    private lateinit var weeklyListContainer: LinearLayout
    private val dayRowViews = mutableListOf<View>()

    private var forecastDays: List<DayForecast> = emptyList()
    private var selectedDayIndex: Int = 0
    private var currentRealtimeTemp: Double = 68.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read preferences or intent extras
        val prefs = getSharedPreferences("yorktown_weather_prefs", Context.MODE_PRIVATE)
        latitude = intent.getDoubleExtra("lat", prefs.getFloat("latitude", 37.1310f).toDouble())
        longitude = intent.getDoubleExtra("lon", prefs.getFloat("longitude", -76.4452f).toDouble())
        locationName = intent.getStringExtra("location_name") ?: prefs.getString("location_name", "Yorktown, VA") ?: "Yorktown, VA"
        tempUnit = intent.getStringExtra("temp_unit") ?: prefs.getString("temp_unit", "F") ?: "F"
        windUnit = intent.getStringExtra("wind_unit") ?: prefs.getString("wind_unit", "MPH") ?: "MPH"
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
    }

    private fun buildRootLayout(): View {
        val rootScroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#070d18")) // Deep midnight navy
            isFillViewport = true
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(16)
            setPadding(pad, dp(12), pad, dp(32))
        }

        // 1. Top Navigation Bar (Back Button + Location + Refresh Button)
        content.addView(buildTopBar())

        // 2. Hero Weather Card
        content.addView(buildHeroWeatherCard())

        // 3. Interactive 24-Hour Graph Card
        content.addView(buildGraphCard())

        // 4. Selected Day Key Metrics Grid
        content.addView(buildMetricsGridCard())

        // 5. 7-Day Outlook Card
        content.addView(buildSevenDayCard())

        rootScroll.addView(content)
        return rootScroll
    }

    private fun buildTopBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(12))
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
            text = "7-Day Outlook & Precipitation Radar"
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
            background = makeCardBackground(Color.parseColor("#0e1726"), Color.parseColor("#1e293b"), dp(18))
            val pad = dp(16)
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
            text = "68°$tempUnit"
            textSize = 42f
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
            text = "High 72° • Low 48° • Feels 67°"
            textSize = 12f
            setTextColor(Color.parseColor("#cbd5e1"))
        }
        condBox.addView(rangeFeelsView)
        rowTop.addView(condBox)
        card.addView(rowTop)

        // Summary chip
        precipSummaryView = TextView(this).apply {
            text = "💧 0% chance of precipitation today"
            textSize = 12f
            setTextColor(Color.parseColor("#38bdf8"))
            background = makeCardBackground(Color.parseColor("#162842"), Color.parseColor("#1e3a5f"), dp(8))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
        }
        card.addView(precipSummaryView)

        return card
    }

    private fun buildGraphCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeCardBackground(Color.parseColor("#0e1726"), Color.parseColor("#1e293b"), dp(18))
            val pad = dp(14)
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        graphTitleView = TextView(this).apply {
            text = "Hourly Breakdown • TODAY"
            textSize = 13.5f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        headerRow.addView(graphTitleView)

        // Tab Pill Buttons for Metric Mode
        val pillBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        btnModePrecip = makeMetricPillButton("🌧️ Precip", active = true) {
            switchGraphMode(ForecastGraphView.MODE_PRECIPITATION)
        }
        pillBar.addView(btnModePrecip)

        btnModeTemp = makeMetricPillButton("🌡️ Temp", active = false) {
            switchGraphMode(ForecastGraphView.MODE_TEMPERATURE)
        }
        pillBar.addView(btnModeTemp)

        btnModeWind = makeMetricPillButton("💨 Wind", active = false) {
            switchGraphMode(ForecastGraphView.MODE_WIND)
        }
        pillBar.addView(btnModeWind)

        headerRow.addView(pillBar)
        card.addView(headerRow)

        // Custom 24-Hour Graph View
        forecastGraph = ForecastGraphView(this).apply {
            tempUnit = this@ForecastActivity.tempUnit
            windUnit = this@ForecastActivity.windUnit
            precipitationUnit = this@ForecastActivity.precipUnit
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(210)).apply {
                topMargin = dp(8)
            }
            onHourScrubbed = { pt ->
                pt?.let {
                    scrubberReadoutView.text = "${it.hourLabel} • ${it.emoji} ${it.conditionText} • 💧 ${it.precipProb}% • ${it.temp.roundToInt()}°$tempUnit"
                }
            }
        }
        card.addView(forecastGraph)

        scrubberReadoutView = TextView(this).apply {
            text = "👆 Drag horizontally across the graph to inspect hourly precipitation & conditions."
            textSize = 10.5f
            setTextColor(Color.parseColor("#94a3b8"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6)
            }
        }
        card.addView(scrubberReadoutView)

        return card
    }

    private fun buildMetricsGridCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeCardBackground(Color.parseColor("#0e1726"), Color.parseColor("#1e293b"), dp(18))
            val pad = dp(14)
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
        }

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val (tilePrecip, vPrecipVal, vPrecipSub) = makeMetricTile("🌧️ PRECIPITATION", "0.00 in", "Peak chance: 0%")
        metricPrecipVal = vPrecipVal
        metricPrecipSub = vPrecipSub
        row1.addView(tilePrecip)

        val (tileSun, vSunVal, vSunSub) = makeMetricTile("🌅 SUN CYCLE", "7:14 AM", "Sunset: 6:21 PM")
        metricSunVal = vSunVal
        metricSunSub = vSunSub
        row1.addView(tileSun)
        card.addView(row1)

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            }
        }

        val (tileWind, vWindVal, vWindSub) = makeMetricTile("💨 WIND & GUSTS", "8 MPH", "Direction: NNE")
        metricWindVal = vWindVal
        metricWindSub = vWindSub
        row2.addView(tileWind)

        val (tileUv, vUvVal, vUvSub) = makeMetricTile("☀️ UV & HUMIDITY", "3.2 Moderate", "Avg Humidity: 60%")
        metricUvVal = vUvVal
        metricUvSub = vUvSub
        row2.addView(tileUv)
        card.addView(row2)

        return card
    }

    private fun buildSevenDayCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeCardBackground(Color.parseColor("#0e1726"), Color.parseColor("#1e293b"), dp(18))
            val pad = dp(14)
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val title = TextView(this).apply {
            text = "📅 7-DAY OUTLOOK"
            textSize = 13.5f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        card.addView(title)

        val hint = TextView(this).apply {
            text = "Tap any day to view its full 24-hour precipitation curve and hourly conditions."
            textSize = 11f
            setTextColor(Color.parseColor("#94a3b8"))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
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
        forecastGraph.graphMode = mode
        updatePillState(btnModePrecip, mode == ForecastGraphView.MODE_PRECIPITATION)
        updatePillState(btnModeTemp, mode == ForecastGraphView.MODE_TEMPERATURE)
        updatePillState(btnModeWind, mode == ForecastGraphView.MODE_WIND)
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

        // 1. Update Hero Card
        currentTempView.text = "${currentRealtimeTemp.roundToInt()}°$tempUnit"
        conditionView.text = "${selDay.emoji} ${selDay.conditionLabel}"
        rangeFeelsView.text = "High ${selDay.tempMax.roundToInt()}° • Low ${selDay.tempMin.roundToInt()}° • Wind ${selDay.windSpeedMax.roundToInt()} $windUnit"

        val rainChip = if (selDay.precipSum > 0.005) {
            val amt = if (precipUnit == "mm") "${String.format("%.1f", selDay.precipSum)}mm" else "${String.format("%.2f", selDay.precipSum)} in"
            "🌧️ $amt precipitation expected (${selDay.precipProbMax}% max probability)"
        } else if (selDay.precipProbMax > 0) {
            "💧 ${selDay.precipProbMax}% chance of rain"
        } else {
            "☀️ Dry conditions • 0% chance of rain"
        }
        precipSummaryView.text = rainChip

        // 2. Update Graph Card
        graphTitleView.text = "Hourly Breakdown • ${selDay.dayOfWeek}"
        forecastGraph.hourlyPoints = selDay.hourlyPoints

        // 3. Update Metric Grid
        val precipAmtStr = if (precipUnit == "mm") "${String.format("%.1f", selDay.precipSum)} mm" else "${String.format("%.2f", selDay.precipSum)} in"
        metricPrecipVal.text = precipAmtStr
        metricPrecipSub.text = "Peak chance: ${selDay.precipProbMax}%"

        metricSunVal.text = selDay.sunrise
        metricSunSub.text = "Sunset: ${selDay.sunset}"

        metricWindVal.text = "${selDay.windSpeedMax.roundToInt()} $windUnit"
        metricWindSub.text = "Max sustained gusts"

        val uvLabel = when {
            selDay.uvIndexMax < 3.0 -> "Low"
            selDay.uvIndexMax < 6.0 -> "Moderate"
            selDay.uvIndexMax < 8.0 -> "High"
            else -> "Very High"
        }
        metricUvVal.text = "${String.format("%.1f", selDay.uvIndexMax)} $uvLabel"
        val avgHum = (selDay.hourlyPoints.map { it.humidity }.average()).roundToInt()
        metricUvSub.text = "Avg Humidity: $avgHum%"

        // 4. Populate 7-Day Rows with Apple-style Range Bars
        weeklyListContainer.removeAllViews()
        dayRowViews.clear()

        val weekMinTemp = forecastDays.minOf { it.tempMin }.toFloat()
        val weekMaxTemp = forecastDays.maxOf { it.tempMax }.toFloat()

        for (i in forecastDays.indices) {
            val day = forecastDays[i]
            val isSelected = (i == selectedDayIndex)
            val isToday = (i == 0)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val bg = if (isSelected) {
                    makeCardBackground(Color.parseColor("#1b2f4f"), Color.parseColor("#ffd166"), dp(12), strokeWidth = dp(1.5f))
                } else {
                    makeCardBackground(Color.parseColor("#101c2e"), Color.parseColor("#1a2b42"), dp(12))
                }
                background = bg
                val padH = dp(12)
                val padV = dp(10)
                setPadding(padH, padV, padH, padV)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(6)
                }
                applyPressAnimation(this)
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    selectedDayIndex = i
                    renderForecastData()
                }
            }

            // Day Title
            val tvDay = TextView(this).apply {
                text = if (isToday) "TODAY" else day.dayOfWeek
                textSize = 12.5f
                setTextColor(if (isSelected) Color.parseColor("#ffd166") else Color.WHITE)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(dp(58), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            row.addView(tvDay)

            // Weather Icon & Prob
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

            if (day.precipProbMax >= 20) {
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
            row.addView(iconBox)

            // Min Temp
            val tvMin = TextView(this).apply {
                text = "${day.tempMin.roundToInt()}°"
                textSize = 12f
                setTextColor(Color.parseColor("#94a3b8"))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(dp(30), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            row.addView(tvMin)

            // Apple Weather style Temp Range Bar
            val rangeBar = TempRangeBarView(this).apply {
                weekMin = weekMinTemp
                weekMax = weekMaxTemp
                dayMin = day.tempMin.toFloat()
                dayMax = day.tempMax.toFloat()
                currentTemp = if (isToday) currentRealtimeTemp.toFloat() else null
                layoutParams = LinearLayout.LayoutParams(0, dp(12), 1.0f).apply {
                    marginStart = dp(8)
                    marginEnd = dp(8)
                }
            }
            row.addView(rangeBar)

            // Max Temp
            val tvMax = TextView(this).apply {
                text = "${day.tempMax.roundToInt()}°"
                textSize = 12.5f
                setTextColor(Color.WHITE)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                gravity = Gravity.START
                layoutParams = LinearLayout.LayoutParams(dp(30), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            row.addView(tvMax)

            dayRowViews.add(row)
            weeklyListContainer.addView(row)
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

                    val (hEmoji, hLabelText) = wmoCodeToInfo(hCode, hourOfDay in 6..19)

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

            val (dEmoji, dLabelText) = wmoCodeToInfo(code, isDay = true)

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

                hours.add(
                    HourPoint(
                        isoTime = "2026-10-2${i}T${String.format("%02d", h)}:00",
                        hourLabel = label,
                        hourOfDay = h,
                        temp = t,
                        feelsLike = t - 1.5,
                        precipProb = hourProb,
                        precipAmount = hourAmt,
                        weatherCode = if (pProb > 0) 61 else 0,
                        conditionText = if (pProb > 0) "Rain Showers" else "Clear Sky",
                        emoji = if (pProb > 0) "🌧️" else (if (h in 6..19) "☀️" else "🌙"),
                        windSpeed = 8.0 + (h % 5),
                        humidity = 55 + (h % 20),
                        uvIndex = if (h in 10..16) 4.5 else 0.0
                    )
                )
            }

            out.add(
                DayForecast(
                    dateStr = "2026-10-2$i",
                    dayOfWeek = days[i],
                    formattedDate = "Oct ${20 + i}",
                    weatherCode = if (pProb > 0) 61 else 0,
                    conditionLabel = if (pProb > 0) "Scattered Showers" else "Sunny",
                    emoji = if (pProb > 0) "🌧️" else "☀️",
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

    private fun wmoCodeToInfo(code: Int, isDay: Boolean): Pair<String, String> {
        return when (code) {
            0 -> if (isDay) "☀️" to "Clear Sky" else "🌙" to "Clear Sky"
            1 -> if (isDay) "🌤️" to "Mainly Clear" else "🌤️" to "Mainly Clear"
            2 -> "⛅" to "Partly Cloudy"
            3 -> "☁️" to "Overcast"
            45, 48 -> "🌫️" to "Foggy"
            51, 53, 55 -> "🌦️" to "Drizzle"
            61, 63 -> "🌧️" to "Rain"
            65 -> "🌧️" to "Heavy Rain"
            71, 73, 75, 77 -> "🌨️" to "Snow"
            80, 81, 82 -> "🌧️" to "Rain Showers"
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
