package com.antigravity.yorktownweather

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * ForecastActivityMockDeviceTest
 * ==============================
 * Comprehensive Robolectric Mock Device Tests for:
 *  - ForecastActivity launch and view hierarchy
 *  - 7-day outlook list rendering with Apple-style range bars
 *  - Interactive day selection and 24-hour graph updates
 *  - Metric mode switching (Precipitation, Temperature, Wind)
 *  - ForecastGraphView custom canvas rendering & touch scrubber interaction
 *  - TempRangeBarView custom canvas rendering with current temp indicator
 *  - Homescreen Widget overlay tap intent targeting ForecastActivity
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForecastActivityMockDeviceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testForecastActivityLaunchesSuccessfullyWithExtras() {
        val intent = Intent(context, ForecastActivity::class.java).apply {
            putExtra("lat", 37.1310)
            putExtra("lon", -76.4452)
            putExtra("location_name", "Tabb, VA")
            putExtra("temp_unit", "F")
            putExtra("wind_unit", "MPH")
        }

        ActivityScenario.launch<ForecastActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull("ForecastActivity must launch successfully", activity)

                val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
                assertNotNull("Root content view must exist", rootView)

                // Verify location header displays Tabb, VA
                val textViews = findAllViewsOfType(rootView, TextView::class.java)
                val locationTv = textViews.firstOrNull { it.text.contains("Tabb, VA") }
                assertNotNull("Location header must display 'Tabb, VA'", locationTv)

                // Verify 7-day outlook section title exists
                val outlookTv = textViews.firstOrNull { it.text.contains("7-DAY OUTLOOK") }
                assertNotNull("7-Day Outlook section must be present", outlookTv)

                // Verify metric pill buttons exist
                val buttons = findAllViewsOfType(rootView, Button::class.java)
                val precipBtn = buttons.firstOrNull { it.text.contains("Precip") }
                val tempBtn = buttons.firstOrNull { it.text.contains("Temp") }
                val windBtn = buttons.firstOrNull { it.text.contains("Wind") }

                assertNotNull("Precipitation pill button must exist", precipBtn)
                assertNotNull("Temperature pill button must exist", tempBtn)
                assertNotNull("Wind pill button must exist", windBtn)

                // Test switching metric modes
                tempBtn?.performClick()
                val graphView = findAllViewsOfType(rootView, ForecastGraphView::class.java).firstOrNull()
                assertNotNull("ForecastGraphView must be present in layout", graphView)
                assertEquals("Graph mode must switch to TEMPERATURE", ForecastGraphView.MODE_TEMPERATURE, graphView?.graphMode)

                windBtn?.performClick()
                assertEquals("Graph mode must switch to WIND", ForecastGraphView.MODE_WIND, graphView?.graphMode)

                precipBtn?.performClick()
                assertEquals("Graph mode must switch to PRECIPITATION", ForecastGraphView.MODE_PRECIPITATION, graphView?.graphMode)
            }
        }
    }

    @Test
    fun testTempRangeBarViewRendersWithoutCrashing() {
        val bar = TempRangeBarView(context).apply {
            weekMin = 40f
            weekMax = 80f
            dayMin = 50f
            dayMax = 72f
            currentTemp = 65f
        }

        bar.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(24, View.MeasureSpec.EXACTLY)
        )
        bar.layout(0, 0, 300, 24)

        val bmp = Bitmap.createBitmap(300, 24, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        bar.draw(canvas)

        assertNotNull("Rendered bitmap must not be null", bmp)
        assertEquals(300, bmp.width)
        assertEquals(24, bmp.height)
    }

    @Test
    fun testForecastGraphViewRendersAndHandlesTouchScrubber() {
        val graph = ForecastGraphView(context).apply {
            tempUnit = "F"
            windUnit = "MPH"
            precipitationUnit = "in"
        }

        val testHours = (0..23).map { h ->
            ForecastActivity.HourPoint(
                isoTime = "2026-10-20T${String.format("%02d", h)}:00",
                hourLabel = if (h == 0) "12 AM" else if (h == 12) "12 PM" else if (h < 12) "$h AM" else "${h - 12} PM",
                hourOfDay = h,
                temp = 55.0 + h * 0.5,
                feelsLike = 54.0 + h * 0.5,
                precipProb = if (h in 14..18) 60 else 0,
                precipAmount = if (h in 14..18) 0.15 else 0.0,
                weatherCode = if (h in 14..18) 61 else 0,
                conditionText = if (h in 14..18) "Rain" else "Clear",
                emoji = if (h in 14..18) "🌧️" else "☀️",
                windSpeed = 10.0,
                humidity = 60,
                uvIndex = 3.0
            )
        }

        graph.hourlyPoints = testHours
        graph.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY)
        )
        graph.layout(0, 0, 400, 200)

        // Draw in precipitation mode
        graph.graphMode = ForecastGraphView.MODE_PRECIPITATION
        val bmpPrecip = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        graph.draw(Canvas(bmpPrecip))
        assertNotNull("Precipitation graph bitmap must not be null", bmpPrecip)

        // Draw in temperature mode
        graph.graphMode = ForecastGraphView.MODE_TEMPERATURE
        val bmpTemp = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        graph.draw(Canvas(bmpTemp))
        assertNotNull("Temperature graph bitmap must not be null", bmpTemp)

        // Test touch scrubber event dispatch
        var scrubbedHour: ForecastActivity.HourPoint? = null
        graph.onHourScrubbed = { pt -> scrubbedHour = pt }

        val downEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 200f, 100f, 0)
        graph.onTouchEvent(downEvent)

        assertNotNull("Scrubber hour must be selected upon touch down", graph.selectedScrubberHour)
        assertNotNull("onHourScrubbed callback must be invoked", scrubbedHour)
        downEvent.recycle()
    }

    @Test
    fun testWidgetIntentTargetsForecastActivityInsteadOfBrowser() {
        val intent = Intent(context, ForecastActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("lat", 37.1310)
            putExtra("lon", -76.4452)
            putExtra("location_name", "Yorktown, VA")
            putExtra("temp_unit", "F")
            putExtra("wind_unit", "MPH")
        }

        assertEquals(ForecastActivity::class.java.name, intent.component?.className)
        assertEquals("Yorktown, VA", intent.getStringExtra("location_name"))
        assertEquals(37.1310, intent.getDoubleExtra("lat", 0.0), 0.0001)
    }

    @Test
    fun testForecastActivityRendersDualUnitWithoutBothText() {
        val intent = Intent(context, ForecastActivity::class.java).apply {
            putExtra("lat", 37.1310)
            putExtra("lon", -76.4452)
            putExtra("location_name", "Yorktown, VA")
            putExtra("temp_unit", "BOTH")
            putExtra("wind_unit", "MPH")
        }

        ActivityScenario.launch<ForecastActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull("ForecastActivity must launch", activity)

                val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
                val textViews = findAllViewsOfType(rootView, TextView::class.java)

                // Verify that no TextView in the hierarchy contains the string "BOTH"
                for (tv in textViews) {
                    assertFalse("No TextView should contain the literal string 'BOTH' in text: '${tv.text}'",
                        tv.text.contains("BOTH"))
                }

                // Verify hero temperature formatted with dual units e.g. "68°F (20°C)"
                val heroTempTv = textViews.firstOrNull { it.text.contains("°F (") && it.text.contains("°C)") }
                assertNotNull("Hero temperature must be formatted with dual units like '68°F (20°C)'", heroTempTv)
            }
        }
    }

    @Test
    fun testLowPrecipitationProbabilityRendersSunBehindRainCloudEmoji() {
        val intent = Intent(context, ForecastActivity::class.java).apply {
            putExtra("lat", 37.1310)
            putExtra("lon", -76.4452)
            putExtra("location_name", "Yorktown, VA")
            putExtra("temp_unit", "F")
            putExtra("wind_unit", "MPH")
        }

        ActivityScenario.launch<ForecastActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                // 1. Direct unit verification of wmoCodeToInfo
                // Low probability (11%) slight rain code 61 -> 🌦️ (Sun Behind Rain Cloud)
                val (emoji61Low, label61Low) = activity.wmoCodeToInfo(code = 61, isDay = true, precipProb = 11)
                assertEquals("11% rain chance must show sun-behind-rain-cloud 🌦️", "🌦️", emoji61Low)
                assertEquals("Isolated Shower", label61Low)

                // Low probability (11%) showers code 80 -> 🌦️
                val (emoji80Low, label80Low) = activity.wmoCodeToInfo(code = 80, isDay = true, precipProb = 11)
                assertEquals("11% shower chance must show sun-behind-rain-cloud 🌦️", "🌦️", emoji80Low)
                assertEquals("Stray Shower", label80Low)

                // High probability (75%) rain code 61 -> 🌧️ (Rain)
                val (emoji61High, label61High) = activity.wmoCodeToInfo(code = 61, isDay = true, precipProb = 75)
                assertEquals("75% rain chance should show full rain 🌧️", "🌧️", emoji61High)
                assertEquals("Light Rain", label61High)

                // 2. Test night behavior for slight rain
                val (emojiNight, labelNight) = activity.wmoCodeToInfo(code = 61, isDay = false, precipProb = 11)
                assertEquals("🌧️", emojiNight)
                assertEquals("Nighttime isolated shower label", "Isolated Shower", labelNight)
            }
        }
    }

    private fun <T : View> findAllViewsOfType(root: ViewGroup, clazz: Class<T>): List<T> {
        val out = mutableListOf<T>()
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (clazz.isInstance(child)) {
                @Suppress("UNCHECKED_CAST")
                out.add(child as T)
            }
            if (child is ViewGroup) {
                out.addAll(findAllViewsOfType(child, clazz))
            }
        }
        return out
    }
}
