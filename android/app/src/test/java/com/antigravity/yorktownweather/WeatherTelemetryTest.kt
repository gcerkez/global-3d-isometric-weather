package com.antigravity.yorktownweather

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for WMO weather code categorization, headlines, emojis, AQI levels, and compass calculation.
 */
class WeatherTelemetryTest {

    @Test
    fun testWmoWeatherCodeToCategoryMapping() {
        // Clear
        assertEquals("clear", IsometricWeatherWidgetProvider.wmoToCategory(0))

        // Partly Cloudy
        assertEquals("partly_cloudy", IsometricWeatherWidgetProvider.wmoToCategory(1))
        assertEquals("partly_cloudy", IsometricWeatherWidgetProvider.wmoToCategory(2))

        // Overcast
        assertEquals("overcast", IsometricWeatherWidgetProvider.wmoToCategory(3))

        // Fog
        assertEquals("fog", IsometricWeatherWidgetProvider.wmoToCategory(45))
        assertEquals("fog", IsometricWeatherWidgetProvider.wmoToCategory(48))

        // Rain & Drizzle
        assertEquals("rain", IsometricWeatherWidgetProvider.wmoToCategory(51))
        assertEquals("rain", IsometricWeatherWidgetProvider.wmoToCategory(61))
        assertEquals("rain", IsometricWeatherWidgetProvider.wmoToCategory(65))
        assertEquals("rain", IsometricWeatherWidgetProvider.wmoToCategory(80))
        assertEquals("rain", IsometricWeatherWidgetProvider.wmoToCategory(82))

        // Snow
        assertEquals("snow", IsometricWeatherWidgetProvider.wmoToCategory(71))
        assertEquals("snow", IsometricWeatherWidgetProvider.wmoToCategory(75))
        assertEquals("snow", IsometricWeatherWidgetProvider.wmoToCategory(85))

        // Thunderstorm
        assertEquals("thunderstorm", IsometricWeatherWidgetProvider.wmoToCategory(95))
        assertEquals("thunderstorm", IsometricWeatherWidgetProvider.wmoToCategory(96))
        assertEquals("thunderstorm", IsometricWeatherWidgetProvider.wmoToCategory(99))
    }

    @Test
    fun testCategoryToHeadline() {
        assertEquals("CLEAR", IsometricWeatherWidgetProvider.categoryToHeadline("clear"))
        assertEquals("PARTLY CLOUDY", IsometricWeatherWidgetProvider.categoryToHeadline("partly_cloudy"))
        assertEquals("OVERCAST", IsometricWeatherWidgetProvider.categoryToHeadline("overcast"))
        assertEquals("COASTAL FOG", IsometricWeatherWidgetProvider.categoryToHeadline("fog"))
        assertEquals("RAIN SHOWER", IsometricWeatherWidgetProvider.categoryToHeadline("rain"))
        assertEquals("SNOWFALL", IsometricWeatherWidgetProvider.categoryToHeadline("snow"))
        assertEquals("THUNDERSTORM", IsometricWeatherWidgetProvider.categoryToHeadline("thunderstorm"))
    }

    @Test
    fun testCategoryToEmojiDayAndNight() {
        // Day
        assertEquals("☀️", IsometricWeatherWidgetProvider.categoryToEmoji("clear", isNight = false))
        assertEquals("⛅", IsometricWeatherWidgetProvider.categoryToEmoji("partly_cloudy", isNight = false))

        // Night
        assertEquals("🌙", IsometricWeatherWidgetProvider.categoryToEmoji("clear", isNight = true))
        assertEquals("☁️", IsometricWeatherWidgetProvider.categoryToEmoji("partly_cloudy", isNight = true))

        // Universal (both day & night)
        assertEquals("🌧️", IsometricWeatherWidgetProvider.categoryToEmoji("rain", isNight = false))
        assertEquals("🌧️", IsometricWeatherWidgetProvider.categoryToEmoji("rain", isNight = true))
        assertEquals("❄️", IsometricWeatherWidgetProvider.categoryToEmoji("snow", isNight = false))
        assertEquals("⛈️", IsometricWeatherWidgetProvider.categoryToEmoji("thunderstorm", isNight = true))
        assertEquals("🌫️", IsometricWeatherWidgetProvider.categoryToEmoji("fog", isNight = false))
    }

    @Test
    fun testAqiLabels() {
        assertEquals("GOOD", IsometricWeatherWidgetProvider.aqiLabel(0))
        assertEquals("GOOD", IsometricWeatherWidgetProvider.aqiLabel(50))
        assertEquals("MODERATE", IsometricWeatherWidgetProvider.aqiLabel(51))
        assertEquals("MODERATE", IsometricWeatherWidgetProvider.aqiLabel(100))
        assertEquals("SENSITIVE", IsometricWeatherWidgetProvider.aqiLabel(101))
        assertEquals("SENSITIVE", IsometricWeatherWidgetProvider.aqiLabel(150))
        assertEquals("UNHEALTHY", IsometricWeatherWidgetProvider.aqiLabel(151))
        assertEquals("UNHEALTHY", IsometricWeatherWidgetProvider.aqiLabel(200))
        assertEquals("V.UNHEALTHY", IsometricWeatherWidgetProvider.aqiLabel(201))
        assertEquals("V.UNHEALTHY", IsometricWeatherWidgetProvider.aqiLabel(300))
        assertEquals("HAZARDOUS", IsometricWeatherWidgetProvider.aqiLabel(301))
        assertEquals("HAZARDOUS", IsometricWeatherWidgetProvider.aqiLabel(500))
    }

    @Test
    fun testWindDegreesToCompass() {
        assertEquals("N", IsometricWeatherWidgetProvider.windDegreesToCompass(0))
        assertEquals("NNE", IsometricWeatherWidgetProvider.windDegreesToCompass(22))
        assertEquals("NE", IsometricWeatherWidgetProvider.windDegreesToCompass(45))
        assertEquals("ENE", IsometricWeatherWidgetProvider.windDegreesToCompass(67))
        assertEquals("E", IsometricWeatherWidgetProvider.windDegreesToCompass(90))
        assertEquals("ESE", IsometricWeatherWidgetProvider.windDegreesToCompass(112))
        assertEquals("SE", IsometricWeatherWidgetProvider.windDegreesToCompass(135))
        assertEquals("SSE", IsometricWeatherWidgetProvider.windDegreesToCompass(157))
        assertEquals("S", IsometricWeatherWidgetProvider.windDegreesToCompass(180))
        assertEquals("SSW", IsometricWeatherWidgetProvider.windDegreesToCompass(202))
        assertEquals("SW", IsometricWeatherWidgetProvider.windDegreesToCompass(225))
        assertEquals("WSW", IsometricWeatherWidgetProvider.windDegreesToCompass(247))
        assertEquals("W", IsometricWeatherWidgetProvider.windDegreesToCompass(270))
        assertEquals("WNW", IsometricWeatherWidgetProvider.windDegreesToCompass(292))
        assertEquals("NW", IsometricWeatherWidgetProvider.windDegreesToCompass(315))
        assertEquals("NNW", IsometricWeatherWidgetProvider.windDegreesToCompass(337))
        assertEquals("N", IsometricWeatherWidgetProvider.windDegreesToCompass(360))
    }
}
