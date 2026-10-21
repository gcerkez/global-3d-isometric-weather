# v0.0.5 — Global 3D Isometric Weather (React + TypeScript Component Architecture)

Release **v0.0.5** is a major architectural refactor transitioning the entire application from vanilla monolithic HTML/DOM manipulation to a modern, modular **React 18 + TypeScript** component architecture. It eliminates page-refresh flickering when expanding forecast rows, adds multi-provider weather API support with seamless fallbacks, and establishes a robust **Vitest + React Testing Library** UI spec test suite.

---

## 🌟 Key Highlights & Improvements

### 1. Modern React 18 & TypeScript Component Architecture
- **No More Monolithic HTML / DOM-Wiping**: Replaced raw `innerHTML = ""` DOM detachment patterns with reactive, declarative React components.
- **Component Breakdown**:
  - `DayForecastRow.tsx`: Smooth expansion panel with animated CSS transitions, temperature range bars, precipitation badges, and current temperature indicator.
  - `HourlyBreakdownChart.tsx`: High-DPI canvas graph displaying 24-hour forecasts with interactive touch/mouse scrubbing, mode toggles (`Precip`, `Temp`, `Wind`), and 4 key metric cards (Precipitation, Sun Cycle, Wind & Gusts, UV & Humidity).
  - `WeatherProviderSelector.tsx`: Dropdown selector with live badges and custom API key configuration.
  - `ForecastModal.tsx`: Comprehensive 7-day outlook container with backdrop blur, hero metrics card, and keyboard accessibility (Escape to close, Enter/Space for day selection).
  - `WeatherHudCard.tsx`: Glassmorphic telemetry HUD displaying live clock, dual units (°F / °C), AQI, UV, diorama themes, and geocoding city search.
  - `WeatherCanvas.tsx`: 3D isometric diorama canvas host, camera zoom/pan pills, and transparent floating refresh button.
  - `App.tsx`: Central coordinator managing telemetry, diorama matching, provider selection, and Android Kotlin WebView interop via `window.isoEngine`.

### 2. Elimination of Day Row Expansion Flicker Bug (Web & Native Android)
- **Web App**: With React state (`selectedDayIdx`), selecting a day toggles the active accordion panel without unmounting or re-rendering unaffected day rows.
- **Native Android (`ForecastActivity.kt`)**: Previously, tapping a day row caused a double-collapse animation glitch:
  - Eliminated conflicting `LayoutTransition` on `weeklyListContainer` that was firing separate child removal animations.
  - Pre-assembled self-contained `DayRowHolder` instances for all 7 days with their own `ForecastGraphView` and metric tiles, eliminating all dynamic view removal, detachment, and re-parenting.
  - Implemented unified `TransitionSet(ORDERING_TOGETHER)` with `ChangeBounds()` and `Fade()`, synchronizing the collapse of the previously opened row and the expansion of the new row into a single 240ms motion with zero double-collapse.
  - Removed touch-down scale squishing on the header row to preserve crisp layout geometry.

### 3. Authentic Homescreen Widget Overlay in 3D Preview (Tab 0)
- In the Android app, Tab 0 ("3D Preview") now displays the exact authentic homescreen widget overlay matching `widget_isometric_weather.xml`:
  - Top glassmorphic HUD pill: Current temperature, live digital clock (`TextClock`), condition label in `#FFD166`, uppercase location name with feels-like temperature, humidity and compass-directed wind speed, and live AQI + UV badges. Tapping the HUD immediately opens `ForecastActivity`.
  - Bottom-right diorama badge pill and transparent refresh button: Displays the active 3D diorama theme and provides an instant one-tap refresh with haptic feedback.
  - Hides desktop-oriented web HUD inputs (search bar, provider select, diorama dropdown, camera controls) in the embedded WebView so the native widget overlay sits cleanly on top of the 3D diorama canvas and clouds.

### 4. Multi-Provider Weather API Integration
Users can now choose from 5 meteorological data providers:
1. **Open-Meteo**: High-resolution global numerical models (ECMWF, GFS, ICON). Free, open access, no key required.
2. **National Weather Service (NOAA)**: Official US government forecasts via `api.weather.gov` gridpoints. Free, no key required.
3. **MET Norway**: Authoritative European/Arctic numerical weather prediction models via `api.met.no`. Free, open access.
4. **OpenWeatherMap**: Worldwide commercial weather API supporting custom API key storage or cached simulation mode.
5. **WeatherAPI.com**: Global forecasts and air quality metrics supporting custom tokens or simulated fallback.
- **Automatic Fallback Engine**: If any custom provider fails or encounters network rate limits, the app automatically fails over to Open-Meteo without interrupting the user.

### 5. Comprehensive UI Spec Test Suite (Vitest + React Testing Library)
- Includes 31 comprehensive spec tests across 6 test suites:
  - `DayForecastRow.spec.tsx`: Verifies metrics display, "TODAY" dot, click expansion without DOM wipes, and keyboard navigation.
  - `ForecastModal.spec.tsx`: Verifies modal opening/closing, Escape key handling, and day accordion switching.
  - `WeatherProviderSelector.spec.tsx`: Verifies provider dropdown list, selection changes, and API key persistence in localStorage.
  - `HourlyBreakdownChart.spec.tsx`: Verifies mode toggling, metrics calculation, and canvas rendering.
  - `WeatherHudCard.spec.tsx`: Verifies telemetry rendering, search submissions, diorama theme selection, and unit toggling.
  - `WeatherService.spec.ts`: Verifies WMO code conversions, API key handling, provider switching, and network fallback behavior.

### 6. Floating Manual Refresh Button & Android Interop
- Added the transparent red refresh button (`↻`) at the bottom-right of the render view for manual weather and diorama updates.
- Preserved full backward compatibility with Android Kotlin (`MainActivity.kt`) and KDE Plasma 6 through the `window.isoEngine` bridge.

---

## 📦 Release Artifacts

| Platform | File | Description |
| :--- | :--- | :--- |
| **Android APK** | `global-3d-isometric-weather-v0.0.5.apk` | Native Android Homescreen & Lockscreen Widget + 7-Day Forecast Activity |
| **KDE Plasma 6 Widget** | `global-3d-isometric-weather-v0.0.5.plasmoid` | Desktop plasmoid for KDE Plasma 6 with 7-day forecast strip |
| **Web / PWA Standalone** | `global-3d-isometric-weather-web-android-v0.0.5.zip` | Standalone React 18 + TypeScript web bundle with 3D isometric diorama engine |

### SHA256 Checksums
```text
435a104de3d4ee5c060965b56c63284a377b74eb979ec4d721f131e309d0d540  global-3d-isometric-weather-v0.0.5.apk
1ba936c71b3e6d6a2c1f9dab69e06200fd58aed97414d154744aa269d7914602  global-3d-isometric-weather-v0.0.5.plasmoid
dc82bf53e0e4eb4924c59217ea67c0dc03b951c113dfa96d5b0886a4f115ff4f  global-3d-isometric-weather-web-android-v0.0.5.zip
```
