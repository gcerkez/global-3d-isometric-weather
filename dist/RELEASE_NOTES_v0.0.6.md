# v0.0.6 — Interactive Doppler Weather Radar, Severe Weather Alerts & Full Framework Settings Suite

Release **v0.0.6** advances the **Global 3D Isometric Weather** project with interactive Doppler weather radar, active severe weather monitoring with desktop push notifications, a comprehensive 6-tab framework settings suite, and an expanded 56-test UI spec suite.

---

## 🌟 Key Highlights & Major Features

### 1. Interactive Doppler Weather Radar (`RadarView.tsx`, `RadarModal.tsx` & `RadarService.ts`)
- **100% Free, Keyless & Watermark-Free Base Map**: Replaced CartoDB tiles with **Esri World Dark Gray Base Map** tiles (`https://server.arcgisonline.com/...`) and OpenStreetMap fallbacks. Requires zero API keys, never blocks usage, has no diagnostic watermarks, and delivers fast global CDN tile rendering.
- **Double-Buffered Atomic Rendering (Zero Map Blinking)**: Re-architected `RadarView.tsx` with offscreen canvas double buffering and tile pre-caching. Eliminates the flashing/blinking artifact on frame refreshes by compositing all tiles and HUD elements offscreen and blitting them atomically in a single sync tick.
- **Expanded Regional Default Zoom**: Adjusted default radar zoom from 7 to **5** (2 steps zoomed out), providing a wide regional view of approaching storm systems and fronts across states and metro areas. Zoom controls allow zooming between levels 3 and 10.
- **Clean Tab Architecture**:
  - **Exclusively on Forecast View**: Removed the redundant radar tab from the Android configuration view (`MainActivity.kt`); the Live Radar tab is now exclusively hosted in the Forecast view (`ForecastActivity.kt`).
  - **Removed Redundant Jump Button**: Removed the `"📡 Live Radar Tab →"` button from the forecast hero card to declutter the interface, since the dedicated tab is right above it.
- **Forecast Graph Label Fix**: Fixed overlapping time axis labels in `ForecastGraphView.kt` where 9 PM and 11 PM were squashed on top of each other. Time marks now render cleanly every 3 hours across the 24-hour day.
- **Spacious, Intuitive Mobile Controls**:
  - **Dedicated Scrubber Section**: Full-width timeline slider with clear `-50m` and `+30m` bounds, live frame counter, and color-coded timestamp status badge (Cyan for `📡 Observed`, Purple for `🔮 Predicted`).
  - **Generous Touch Targets & Playback Buttons**: Dedicated `[⏮ -10m]`, `[▶ Play Loop / ⏸ Pause]`, and `[+10m ⏭]` step buttons with readable text labels, alongside speed pills (`0.5x`, `1x`, `2x`).
  - **Responsive Settings Grid**: Stacked 2-column layout separating color palette dropdown (`Universal`, `Smooth Blue/Green`, `Universal NEXRAD`, etc.) and radar opacity slider with live percentage readout (`85%`), preventing any text overlap on mobile screens.
  - **Quick Controls Help Footer**: Integrated usage tip explaining playback, scrubbing, and zoom gestures.
- **Interactive Multi-Tile Composite Canvas**:
  - Dynamic 3x3 Web Mercator (EPSG:3857) tile stitching across zoom levels 3 through 10.
  - Zoom controls (`＋`, `－`) and one-tap re-centering on current geographic coordinates (`🎯`).
  - Range rings (25 mi, 50 mi, 100 mi) with animated pulsing user location beacon and pin label.
  - Adjustable tile opacity slider (20% – 100%) and interactive dBZ reflectivity scale legend bar.
  - Resilient offline fallback simulation and dark tactical grid if network feeds are temporarily unreachable.

### 2. Modern 3D Weather App Icon
- **High-Resolution 3D Glassmorphic App Icon**: Generated a sleek, weather-themed icon featuring a floating isometric diorama cube, glowing sun, rain cloud, droplets, and cyan radar waves.
- **Full Android Mipmap Suite**: Packaged square and round launcher icons across all Android display densities (`mipmap-mdpi`, `mipmap-hdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi`, `mipmap-xxxhdpi`), high-res adaptive foreground drawable (`ic_launcher_foreground.png`), and web icons (`public/icon.png`, `web/icon.png`).
- **Manifest Integration**: Configured `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"` in `AndroidManifest.xml` for clean presentation on OnePlus / Android launchers.

### 3. Severe Weather Events & Emergency Alerts (`AlertService.ts`, `WeatherAlertsBanner.tsx`, `AlertDetailsModal.tsx`)
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

### 4. Full Framework Settings & Configuration Suite (`SettingsModal.tsx`)
Eliminated raw HTML settings pages in favor of an integrated, 6-tab React configuration suite persisted in `localStorage`:
- **📍 Location & Presets**: Active coordinates display, 1-tap device GPS autolocate, geocoded city/ZIP search, and 18 favorite preset city chips (Yorktown, Norfolk, Washington D.C., Denver, London, Tokyo, Paris, etc.).
- **🛰️ Meteorological Providers & Custom API Tokens**: Seamless switching between Open-Meteo, NOAA NWS, MET Norway, OpenWeatherMap, and WeatherAPI.com with custom API token management.
- **📏 Units & Display Preferences**: Temperature (°F, °C, Dual °F+°C), Wind Speed (MPH, KM/H, Knots, M/S), Precipitation (Inches, Millimeters), and Clock Format (12-hour, 24-hour).
- **🏛️ 3D Diorama & Simulation**: Selection across all 28 handcrafted regional dioramas and solar lighting overrides (Realtime Solar, Daylight Noon, Golden Sunset, Night Sky).
- **📡 Doppler Radar Preferences**: Default radar color palette and tile opacity settings.
- **🚨 Severe Alerts & Notifications**: Severe weather monitoring toggle, desktop push notification permission manager, and auditory chime toggles.

- **Standalone 3D Viewport Isolation (`#preview`) & Real-Time Framing Synchronization**:
  - Eliminated duplicate overlay overlap in the Android 3D preview configurator tab. Previously, the desktop HUD overlay rendered in the embedded WebView beneath Android's native widget HUD overlay.
  - Introduced dedicated `#preview` route and `hideControls` mode in `App.tsx` and `WeatherCanvas.tsx`: in preview mode, all desktop HUD cards, alert banners, and camera controls are omitted so only the clean 3D isometric diorama renders behind the authentic native Android widget overlay.
  - **Live Real-Time Framing Controls**: The preview in Tab 0 now acts as the immediate real-time source of truth when pressing camera movement buttons (`[－]`, `[＋]`, `[◀]`, `[▲]`, `[🎯]`, `[▼]`, `[▶]`) and quick zoom preset chips:
    - Added `setCameraFraming(zoom, panX, panY)` in `IsometricEngine.ts` to directly set exact coordinate framing.
    - Added vertical panning (`cameraPanY`, `shiftY = (cameraPanY / 384.0) * h`) with full projection parity matching `IsometricWeatherWidgetProvider.kt`.
    - Implemented `pan_up` and `pan_down` actions in `IsometricEngine.adjustCamera()`.
    - Passed initial framing parameters (`&zoom=...&panX=...&panY=...`) in the preview route URL so the preview immediately reflects saved preferences on startup with zero lag or desynchronization.

### 5. Expanded UI Spec Test Suite (Vitest + React Testing Library + Robolectric)
- **12 Test Suites / 62 Tests (100% Passing)**:
  - `WeatherCanvas.spec.tsx`: Validates `WeatherCanvas` all 7 camera controls (zoom in/out, pan left/right/up/down, reset), real-time framing adjustment, control omission in `hideControls` mode, and verifies that `#preview` loads the standalone 3D diorama canvas with accurate query param framing and zero desktop HUD overlays.
  - `AlertService.spec.ts`: Validates NOAA NWS GeoJSON parsing, WMO code fallback detection, and notification deduplication.
  - `WeatherAlerts.spec.tsx`: Validates `WeatherAlertsBanner` severity sorting, counter badge, and `AlertDetailsModal` emergency statements.
  - `RadarModal.spec.tsx`: Validates canvas rendering, playback pause/play toggle, timeline scrubbing, zoom buttons, and palette selectors.
  - `ForecastModal.spec.tsx`: Validates 7-Day outlook and Live Doppler Radar tab switching, accordion expansions, and hero radar shortcut button.
  - `SettingsModal.spec.tsx`: Validates tab navigation, city searches, GPS autolocate, provider selection, and API key persistence.
  - `RadarService.spec.ts`: Validates Web Mercator tile projection, RainViewer URL formatting, and cache management.
  - `ForecastActivityMockDeviceTest.kt`: Validates native Android 7-day forecast rendering, range bars, graph metric switches, and Live Doppler Radar card embedding.
  - `MainActivityVisualMockDeviceTest.kt`: Validates all 6 Android configuration tabs including Live Radar tab and camera controls.
  - Existing suites for `WeatherService`, `DayForecastRow`, `HourlyBreakdownChart`, `WeatherHudCard`, and `WeatherProviderSelector`.

### 6. Multi-Platform Build & Packaging
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
