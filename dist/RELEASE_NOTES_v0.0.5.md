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

### 2. Elimination of Day Row Expansion Flicker Bug
- In previous versions, clicking any day row called `updateForecastModalUI()`, which executed `listContainer.innerHTML = ""` to destroy and rebuild all DOM nodes, resulting in the app momentarily going blank and refilling.
- With React state (`selectedDayIdx`), selecting a day simply toggles the active accordion panel without unmounting or re-rendering unaffected day rows.

### 3. Multi-Provider Weather API Integration
Users can now choose from 5 meteorological data providers:
1. **Open-Meteo**: High-resolution global numerical models (ECMWF, GFS, ICON). Free, open access, no key required.
2. **National Weather Service (NOAA)**: Official US government forecasts via `api.weather.gov` gridpoints. Free, no key required.
3. **MET Norway**: Authoritative European/Arctic numerical weather prediction models via `api.met.no`. Free, open access.
4. **OpenWeatherMap**: Worldwide commercial weather API supporting custom API key storage or cached simulation mode.
5. **WeatherAPI.com**: Global forecasts and air quality metrics supporting custom tokens or simulated fallback.
- **Automatic Fallback Engine**: If any custom provider fails or encounters network rate limits, the app automatically fails over to Open-Meteo without interrupting the user.

### 4. Comprehensive UI Spec Test Suite (Vitest + React Testing Library)
- Includes 31 comprehensive spec tests across 6 test suites:
  - `DayForecastRow.spec.tsx`: Verifies metrics display, "TODAY" dot, click expansion without DOM wipes, and keyboard navigation.
  - `ForecastModal.spec.tsx`: Verifies modal opening/closing, Escape key handling, and day accordion switching.
  - `WeatherProviderSelector.spec.tsx`: Verifies provider dropdown list, selection changes, and API key persistence in localStorage.
  - `HourlyBreakdownChart.spec.tsx`: Verifies mode toggling, metrics calculation, and canvas rendering.
  - `WeatherHudCard.spec.tsx`: Verifies telemetry rendering, search submissions, diorama theme selection, and unit toggling.
  - `WeatherService.spec.ts`: Verifies WMO code conversions, API key handling, provider switching, and network fallback behavior.

### 5. Floating Manual Refresh Button & Android Interop
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
4ad2c99ac4d435e547d39205772bbc99adeefaeae1c2705331d3bd0c70cdeb2c  global-3d-isometric-weather-v0.0.5.apk
1ba936c71b3e6d6a2c1f9dab69e06200fd58aed97414d154744aa269d7914602  global-3d-isometric-weather-v0.0.5.plasmoid
da57ebb548282b6302aac817876e6ba08c1a08e2df1d444fc8b34269c32e8369  global-3d-isometric-weather-web-android-v0.0.5.zip
```
