# v0.0.6 — Interactive Doppler Weather Radar, Severe Weather Alerts & Full Framework Settings Suite

Release **v0.0.6** advances the **Global 3D Isometric Weather** project with interactive Doppler weather radar, active severe weather monitoring with desktop push notifications, a comprehensive 6-tab framework settings suite, and an expanded 56-test UI spec suite.

---

## 🌟 Key Highlights & Major Features

### 1. Interactive Doppler Weather Radar (`RadarModal.tsx` & `RadarService.ts`)
- **Keyless Global Doppler Radar**: Integrated with the open RainViewer API (`api.rainviewer.com`) and CartoDB Dark Matter base maps, enabling live precipitation mapping across North America, Europe, Asia, and worldwide.
- **Animated Playback Loop**:
  - Smooth looping across past observed radar frames and forward nowcast projections.
  - Play/Pause toggle (also triggered via keyboard `Space`), frame step backward/forward buttons, and timeline scrubbing slider.
  - Multi-speed playback controls (`0.5x`, `1x`, `2x`).
- **Interactive Multi-Tile Composite Canvas**:
  - Dynamic 3x3 Web Mercator (EPSG:3857) tile stitching at zoom levels 4 through 10.
  - Zoom controls (`＋`, `－`) and one-tap re-centering on current geographic coordinates (`🎯`).
  - Range rings (25 mi, 50 mi, 100 mi) with animated pulsing user location beacon and pin label.
  - Color palette selection: Original Universal, Universal Blue, The Weather Channel, and NEXRAD Level-III.
  - Adjustable tile opacity slider (20% – 100%) and interactive dBZ reflectivity scale legend bar.
  - Resilient offline fallback simulation if RainViewer network feeds are temporarily unreachable.

### 2. Severe Weather Events & Emergency Alerts (`AlertService.ts`, `WeatherAlertsBanner.tsx`, `AlertDetailsModal.tsx`)
- **Authoritative NOAA National Weather Service Alerts**: Queries official US weather alerts directly from `api.weather.gov/alerts/active?point=lat,lon` with zero API keys required.
- **Global Meteorological Condition Analysis Fallback**: For international regions outside NOAA NWS boundaries, automatically analyzes live telemetry and WMO weather codes to detect hazardous conditions:
  - Severe Thunderstorm Warning (WMO codes 96, 99)
  - Thunderstorm Watch (WMO code 95)
  - High Wind Warning (sustained/gust winds $\ge$ 45 MPH) & Wind Advisory ($\ge$ 30 MPH)
  - Winter Storm Warning & Blizzard Conditions (WMO codes 75, 86)
  - Flash Flood Advisory & Heavy Deluge (WMO codes 65, 82)
  - Excessive Heat Warning (ambient temp $\ge$ 105°F)
  - Hard Freeze Warning (ambient temp $\le$ 10°F)
- **Glassmorphic Warning Banner**: Displays an urgent pulsating alert badge color-coded by severity (`Extreme`, `Severe`, `Moderate`, `Minor`).
- **Alert Details Modal**: Features onset/expiration timing, urgency badges, safety instructions (e.g. interior shelter guidelines), and complete meteorological statements.
- **Desktop Push Notifications**: Dispatches native desktop notifications for urgent hazards with built-in deduplication.

### 3. Full Framework Settings & Configuration Suite (`SettingsModal.tsx`)
Eliminated raw HTML settings pages in favor of an integrated, 6-tab React configuration suite persisted in `localStorage`:
- **📍 Location & Presets**: Active coordinates display, 1-tap device GPS autolocate, geocoded city/ZIP search, and 18 favorite preset city chips (Yorktown, Norfolk, Washington D.C., Denver, London, Tokyo, Paris, etc.).
- **🛰️ Meteorological Providers & Custom API Tokens**: Seamless switching between Open-Meteo, NOAA NWS, MET Norway, OpenWeatherMap, and WeatherAPI.com with custom API token management.
- **📏 Units & Display Preferences**: Temperature (°F, °C, Dual °F+°C), Wind Speed (MPH, KM/H, Knots, M/S), Precipitation (Inches, Millimeters), and Clock Format (12-hour, 24-hour).
- **🏛️ 3D Diorama & Simulation**: Selection across all 28 handcrafted regional dioramas and solar lighting overrides (Realtime Solar, Daylight Noon, Golden Sunset, Night Sky).
- **📡 Doppler Radar Preferences**: Default radar color palette and tile opacity settings.
- **🚨 Severe Alerts & Notifications**: Severe weather monitoring toggle, desktop push notification permission manager, and auditory chime toggles.

### 4. Expanded UI Spec Test Suite (Vitest + React Testing Library)
- **11 Test Suites / 56 Tests (100% Passing)**:
  - `AlertService.spec.ts`: Validates NOAA NWS GeoJSON parsing, WMO code fallback detection, and notification deduplication.
  - `WeatherAlerts.spec.tsx`: Validates `WeatherAlertsBanner` severity sorting, counter badge, and `AlertDetailsModal` emergency statements.
  - `RadarModal.spec.tsx`: Validates canvas rendering, playback pause/play toggle, timeline scrubbing, zoom buttons, and palette selectors.
  - `SettingsModal.spec.tsx`: Validates tab navigation, city searches, GPS autolocate, provider selection, and API key persistence.
  - `RadarService.spec.ts`: Validates Web Mercator tile projection, RainViewer URL formatting, and cache management.
  - Existing suites for `WeatherService`, `ForecastModal`, `HourlyBreakdownChart`, `DayForecastRow`, `WeatherHudCard`, and `WeatherProviderSelector`.

### 5. Multi-Platform Build & Packaging
- **Android App & Widget (`v0.0.6`)**: Updated `versionCode 6` and `versionName 0.0.6` with clean release APK compilation (`gradle assembleRelease`).
- **KDE Plasma 6 Plasmoid (`v0.0.6`)**: Packaged plasmoid with updated metadata version.
- **Web PWA Bundle (`v0.0.6`)**: Synchronized production Vite build across `web/` and Android assets.

---

## 📦 Release Artifacts

| Platform | File | Description |
| :--- | :--- | :--- |
| **Android APK** | `global-3d-isometric-weather-v0.0.6.apk` | Native Android application with homescreen widget, radar view, alerts, and 3D diorama |
| **KDE Plasma 6** | `global-3d-isometric-weather-v0.0.6.plasmoid` | Desktop applet for KDE Plasma 6 with real-time weather and 3D scenes |
| **Web Distribution** | `global-3d-isometric-weather-web-android-v0.0.6.zip` | Standalone Web application assets and PWA bundle |
| **Checksums** | `SHA256SUMS.txt` | Cryptographic SHA-256 verification hashes for all release artifacts |
