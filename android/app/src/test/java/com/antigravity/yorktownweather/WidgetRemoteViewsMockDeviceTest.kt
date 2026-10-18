package com.antigravity.yorktownweather

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.AlarmClock
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * WidgetRemoteViewsMockDeviceTest
 * ===============================
 * Android SDK Mock Device Tests for the Homescreen AppWidget layout, views,
 * click intents, and procedural diorama rendering.
 *
 * Runs headless on the simulated Android SDK device environment:
 *  - Verifies corner card is completely removed from widget XML layout
 *  - Verifies top-only HUD banner layout hierarchy and elements
 *  - Verifies alarm clock tap intent creation and fallback resolution
 *  - Verifies island weather dynamic bitmap canvas rendering
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetRemoteViewsMockDeviceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testMockDeviceWidgetLayoutInflatesWithoutCornerCard() {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.widget_isometric_weather, null)

        assertNotNull("Widget layout must inflate successfully on mock device", layout)

        // Verify top HUD banner exists
        val topBar = layout.findViewById<LinearLayout>(R.id.widget_hud_top_bar)
        assertNotNull("widget_hud_top_bar must be present in widget layout", topBar)

        // Verify clock and temperature text views exist
        val clock = layout.findViewById<TextClock>(R.id.hud_bar_clock)
        assertNotNull("hud_bar_clock must be present", clock)

        val clockContainer = layout.findViewById<FrameLayout>(R.id.hud_bar_clock_container)
        assertNotNull("hud_bar_clock_container must be present for click target", clockContainer)

        val temp = layout.findViewById<TextView>(R.id.hud_bar_temp)
        assertNotNull("hud_bar_temp must be present", temp)

        // Verify corner card is completely absent from resources
        val cornerId = context.resources.getIdentifier("widget_hud_corner", "id", context.packageName)
        assertEquals("widget_hud_corner must not exist in layout IDs", 0, cornerId)
    }

    @Test
    fun testMockDeviceAlarmClockIntentCreation() {
        val intent = IsometricWeatherWidgetProvider.getAlarmClockIntent(context)
        assertNotNull("getAlarmClockIntent must produce a non-null intent", intent)
        assertTrue("Intent must specify FLAG_ACTIVITY_NEW_TASK", (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
        assertTrue(
            "Intent should target alarm clock action or specific deskclock package",
            intent.action == AlarmClock.ACTION_SHOW_ALARMS || intent.component?.packageName?.contains("clock") == true
        )
    }

    @Test
    fun testMockDeviceDioramaCutoutRendering() {
        // Create an in-memory sample diorama base bitmap
        val baseBmp = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        assertNotNull("Base bitmap should be created on mock device", baseBmp)
        assertEquals(400, baseBmp.width)
        assertEquals(300, baseBmp.height)

        val provider = IsometricWeatherWidgetProvider()
        val cfg = IsometricWeatherWidgetProvider.WidgetConfig(
            locationName = "Yorktown, VA",
            lat = 37.2388,
            lon = -76.5097,
            renderMode = "hybrid",
            dioramaTheme = "yorktown",
            hudScale = 1.0f,
            transparentBg = false,
            showExtendedHud = true,
            timeMode = "day",
            weatherOverride = "rain",
            tempUnit = "F",
            windUnit = "MPH",
            widgetZoom = 1.0f,
            widgetOffsetX = 0.0f,
            particleDensity = 0.50f
        )
        val wx = IsometricWeatherWidgetProvider.WeatherTelemetry(
            tempF = 65.0,
            feelsLikeF = 64.0,
            humidity = 70,
            windMph = 10.0,
            windDeg = 180,
            weatherCode = 61,
            isDay = true,
            usAqi = 30,
            uvIndex = 4.0,
            hourlyTemps = listOf(60, 62, 65, 66)
        )

        val rendered = provider.renderIsometricWidgetFrame(cfg, wx, baseBmp, 0f, 320f, 250f, 80f)
        assertNotNull("Rendered bitmap must not be null", rendered)
        assertTrue("Rendered bitmap width should be positive", rendered.width > 0)
        assertTrue("Rendered bitmap height should be positive", rendered.height > 0)
    }
}
