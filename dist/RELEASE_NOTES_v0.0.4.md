# v0.0.4 — Global 3D Isometric Weather Widget (Android APK / KDE Plasma 6 / Web)

Release **v0.0.4** introduces a premium native **7-Day Interactive Forecast & Precipitation Radar** experience across Android, Web, and KDE Plasma 6. Tapping the homescreen widget overlay now opens a dedicated, high-fidelity forecast screen with interactive precipitation scrubbing rather than redirecting out to an external web browser.

---

### 🌟 Key Highlights & New Features

#### 1. Native 7-Day Forecast & Precipitation Radar (`ForecastActivity`)
* **Overlay Tap Integration**: Tapping the homescreen widget's weather overlay (`hud_bar_temp`, `hud_bar_condition`, `hud_bar_wind_hum`, etc.) now immediately launches `ForecastActivity` natively instead of opening an external web browser.
* **Direct App Access**: Added a dedicated **"📅 View 7-Day Forecast & Precipitation Radar"** action button in Tab 0 of `MainActivity`.
* **Instant Responsive Launch**: Cached telemetry populates immediately upon launch while fresh 7-day data synchronizes asynchronously in the background.

#### 2. Interactive 24-Hour Graph with Tactile Touch Scrubber (`ForecastGraphView`)
* **Custom Canvas Rendering**: High-performance custom Android `View` rendering hourly data points across the day.
* **Metric Modes**:
  - **🌧️ Precipitation Mode**: Hourly probability bars with a radiant electric-cyan to midnight-blue gradient (`#38bdf8` to `#1e3a8a`), probability percentage callouts (e.g. `60%`), and rain/snow accumulation badges (e.g. `0.15"` or `3.5mm`).
  - **🌡️ Temperature Mode**: Smooth cubic Bezier spline connecting 24 hourly temperature points with a translucent golden glow gradient area fill underneath (`#ffd166`).
  - **💨 Wind Mode**: Hourly wind speed bars with gradient emerald coloring and speed indicators.
* **Tactile Touch Scrubber**:
  - Gliding a finger across the 24-hour timeline draws a vertical glowing dashed guide line and dynamic value dot.
  - Fires physical haptic feedback (`HapticFeedbackConstants.KEYBOARD_TAP`) on every hour boundary.
  - Displays a floating glassmorphism tooltip card with exact time, condition emoji + description, precipitation probability & accumulation, and temperature.

#### 3. Apple Weather-Style 7-Day Outlook & Temperature Range Bars (`TempRangeBarView`)
* **Dynamic Range Bars**: Custom `View` rendering an Apple Weather-style horizontal temperature range gradient pill bar for each day:
  - Background track representing the full week's minimum to maximum temperature span.
  - Proportional colored pill segment spanning the day's minimum to maximum temperature, with temperature-responsive gradient hues (cool ice blue, teal, warm gold, amber, and coral).
  - Glowing white center dot indicating the real-time current temperature on **"Today"**.
* **Interactive Day Selector**: Tapping ANY of the 7 day rows highlights that day with a golden glow border and immediately updates the 24-hour graph and metrics grid to display that day's full hourly timeline.

#### 4. Selected Day Telemetry & Metric Tiles
* **Precipitation**: Total expected accumulation and peak probability hour.
* **Sun Cycle**: Astronomical sunrise and sunset times.
* **Wind & Gusts**: Peak sustained winds and direction.
* **UV & Humidity**: Peak UV Index with WHO safety category (Low, Moderate, High, Very High) and average relative humidity.

#### 5. Web & Standalone PWA Parity
* **Glassmorphic Forecast Modal**: Added `#forecastModal` with frosted blur backdrop (`backdrop-filter: blur(14px)`), accessible by clicking the HUD card or the **"📅 7-Day Forecast"** badge.
* **Interactive HTML5 Canvas Graph**: 24-hour chart supporting pointer hover and touch drag scrubbing with floating tooltip card.
* **Synchronized Assets**: Kept `web/` and `android/app/src/main/assets/web/` in exact synchronization.

#### 6. KDE Plasma 6 Desktop Plasmoid Parity
* **7-Day Weather Fetch**: Updated `main.qml` to query 7 full days of forecast data and hourly precipitation from Open-Meteo.
* **Flickable 7-Day Strip**: Upgraded `WeatherHudCard.qml` with a scrollable 7-day forecast strip featuring condition emojis, temperature ranges, and rain probability badges (`💧 40%`).
* **Direct Plasmoid Expand**: Clicking the **"📅 7-Day"** control directly expands the telemetry drawer inside the desktop widget without launching a browser.

#### 7. Automated Mock Device & Unit Test Suite
* Added `ForecastActivityMockDeviceTest.kt` verifying:
  - `ForecastActivity` launch, intent extra parsing, and view hierarchy.
  - Metric mode switching between Precipitation, Temperature, and Wind.
  - `TempRangeBarView` drawing and current temperature indicator placement.
  - `ForecastGraphView` 24-hour canvas rendering and touch scrubber motion event handling.
  - Dual unit temperature formatting eliminating literal "BOTH" text.
  - Intelligent precipitation icon scaling: low probability days (≤ 20%, e.g. 11%) now display sun-behind-rain-cloud (`🌦️`) and labels like "Isolated Shower" rather than full rain clouds (`🌧️`).
  - Widget pending intent targeting `ForecastActivity` rather than browser.
* All 55 unit tests passing.

---

### 📦 Distributable Packages

| Package | File | Description |
| :--- | :--- | :--- |
| **Android APK** | `global-3d-isometric-weather-v0.0.4.apk` | Native Android Homescreen & Lockscreen Widget + 7-Day Forecast Activity |
| **KDE Plasma 6 Widget** | `global-3d-isometric-weather-v0.0.4.plasmoid` | Desktop plasmoid for KDE Plasma 6 with 7-day forecast strip |
| **Web / PWA Standalone** | `global-3d-isometric-weather-web-android-v0.0.4.zip` | Standalone HTML5 Canvas engine with 7-day forecast modal |

### 🔐 Checksums (SHA-256)
```text
9eea4de164c43afbb73aa8226ac872ea2ad69d3903b94071760a9f24e8c0580f  global-3d-isometric-weather-v0.0.4.apk
8cbad6823cf94b7d838d869cb0d887d7e876f70c27a666591647f99f7d1195d6  global-3d-isometric-weather-v0.0.4.plasmoid
2ea010a841a138621122c466f477e092bce7abff9f8ef91984b2ce18ed81fef8  global-3d-isometric-weather-web-android-v0.0.4.zip
```


