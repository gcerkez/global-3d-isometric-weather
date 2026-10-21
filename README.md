# Global 3D Isometric Weather (`v0.0.6`)

A **Global 3D Isometric Real-Time Weather Application & Widget** built for **Web & PWA (React 18 + TypeScript)**, **Android Homescreen & Lockscreen (OnePlus 13 / OxygenOS 15 & Android 8.0+)**, and **KDE Plasma 6 (Bazzite / Linux Desktop)**.

Inspired by tilt-shift isometric architectural dioramas with **translucent turquoise cutaway water blocks**, the application features **28 Regional 3D Isometric Diorama Sets** (Daylight, Golden Sunset, and Starry Night variants = **84 bundled high-res cutouts**), **Interactive Doppler Weather Radar**, **Active Severe Weather Warnings & Desktop Push Notifications**, **Multi-Provider Meteorological Engines**, and a **Full Framework Settings Suite**.

---

## 🌟 What This Repository Does (Top-Down Overview)

```
                            ┌──────────────────────────────────────────────┐
                            │      Global 3D Isometric Weather (v0.0.6)    │
                            └──────────────────────┬───────────────────────┘
                                                   │
         ┌─────────────────────────────────────────┼────────────────────────────────────────┐
         │                                         │                                        │
┌────────┴──────────────┐              ┌───────────┴───────────┐               ┌────────────┴───────────┐
│     Web / PWA App     │              │  Android App & Widget │               │   KDE Plasma 6 Applet  │
│  (React 18 + TS + UI) │              │   (Kotlin + WebView)  │               │      (QML + JS ES6)    │
└────────┬──────────────┘              └───────────┬───────────┘               └────────────┬───────────┘
         │                                         │                                        │
         ├─ Interactive Doppler Radar              ├─ 4x3 Resizable Homescreen Widget       ├─ Native Desktop Plasmoid
         ├─ Severe NWS & WMO Alerts Banner         ├─ High-Readability Top-Sky Split HUD    ├─ Stationary Weather HUD Card
         ├─ Full Framework 6-Tab Settings          ├─ Animated Rain/Snow ViewFlipper        ├─ Solar Lighting Overrides
         ├─ 7-Day & Hourly Interactive Charts      ├─ Native ForecastActivity with Graphs   ├─ 28 Handcrafted Dioramas
         └─ 5 Meteorological API Providers         └─ True Hardware GPS & AlarmManager      └─ 12-Hour Weather Sparkline
```

---

## 🚀 Key Features & Capabilities

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

### 2. Severe Weather Alerts & Desktop Push Notifications (`AlertService.ts`, `WeatherAlertsBanner.tsx`, `AlertDetailsModal.tsx`)
- **Authoritative NOAA National Weather Service Alerts**: Queries official US weather alerts directly from `api.weather.gov/alerts/active?point=lat,lon` with zero API keys required.
- **Global Meteorological Condition Analysis Fallback**: For international regions outside NOAA NWS boundaries, automatically analyzes live telemetry and WMO weather codes to detect hazardous conditions:
  - **Severe Thunderstorm Warning** (WMO codes 96, 99)
  - **Thunderstorm Watch** (WMO code 95)
  - **High Wind Warning** (sustained/gust winds $\ge$ 45 MPH) & **Wind Advisory** ($\ge$ 30 MPH)
  - **Winter Storm Warning & Blizzard Conditions** (WMO codes 75, 86)
  - **Flash Flood Advisory & Heavy Deluge** (WMO codes 65, 82)
  - **Excessive Heat Warning** (ambient temp $\ge$ 105°F)
  - **Hard Freeze Warning** (ambient temp $\le$ 10°F)
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

### 4. Multi-Provider Weather Engine (`WeatherService.ts`)
Users can switch dynamically between 5 meteorological providers:
1. **Open-Meteo**: High-resolution global numerical models (ECMWF, GFS, ICON). Free, open access, no key required.
2. **National Weather Service (NOAA)**: Official US federal government meteorological service (`api.weather.gov`). Free, no key required.
3. **MET Norway**: Authoritative European and Arctic global numerical weather models (`api.met.no`). Free, open access.
4. **OpenWeatherMap**: Popular worldwide weather API supporting custom API keys or demo simulation mode.
5. **WeatherAPI.com**: Global forecast and air quality service supporting custom tokens or simulated fallback.
- **Automatic Failover Engine**: If any custom provider fails or encounters network rate limits, the app automatically fails over to Open-Meteo without interrupting the user.

### 5. 28 Pre-Generated Regional, NPS & UNESCO 3D Isometric Dioramas
Each floating-island diorama features **translucent turquoise cutaway water** and comes with matching **Daylight**, **Golden Hour Sunset**, and **Starry Night** textures:

| Theme ID | Region / Landmark | Iconic Architectural & Topographic Features |
| :--- | :--- | :--- |
| `yorktown` | Historic Yorktown, VA | Colonial Waterfront, Coleman Memorial Bridge, Tall Ship, Victory Monument |
| `jamestown` | Historic Jamestown, VA | 1607 Fort Palisade, Colonial Brick Church, Susan Constant & Godspeed |
| `norfolk` | Norfolk Naval Harbor, VA | Naval Base Piers, Aircraft Carrier, Downtown Waterfront & Elizabeth River |
| `virginia_beach` | Virginia Beach, VA | Oceanfront Boardwalk, Cape Henry Lighthouse, 14th St Pier & Coastal Villas |
| `chesapeake_bay` | Chesapeake Bay, VA | Bay Bridge-Tunnel, Oyster & Crab Docks, Historic Harbor Light |
| `washington_dc` | Washington, D.C. | U.S. Capitol Dome, Washington Monument, Lincoln Memorial, Cherry Blossoms |
| `new_york` | New York City, NY | Statue of Liberty, Brooklyn Bridge, Empire State & Chrysler Skyline |
| `myrtle_beach` | Myrtle Beach, SC | SkyWheel Ferris Wheel, Oceanfront High-Rise Resorts, Boardwalk Promenade |
| `atlanta` | Atlanta, GA | Midtown Skyscraper Skyline, Georgia State Capitol, Piedmont Park |
| `orlando` | Orlando, FL | Fairytale Castle Tower, Resort Pavilions, Theme Park Roller Coaster Loops |
| `miami_caribbean` | Miami Beach & Keys, FL | Pastel Art Deco Ocean Drive Hotels, Seven Mile Bridge, Coral Reef Lagoon |
| `new_orleans` | New Orleans, LA | St. Louis Cathedral, French Quarter Balconies, Steamboat, Cypress Bayou |
| `texas` | Texas Capitol & Austin | Pink Granite Capitol Dome, Austin Skyline, River Walk Arched Bridge |
| `chicago_great_lakes` | Chicago, IL | Willis Tower, Bascule Drawbridge, Navy Pier Ferris Wheel, Cloud Gate Bean |
| `denver` | Denver, CO | Mile High Skyscraper Skyline, Historic Union Station, Rocky Mountain Foothills |
| `colorado_springs` | Colorado Springs, CO | Garden of the Gods Red Sandstone Spires, Pikes Peak Mountain Cog Depot |
| `arizona` | Arizona Sonoran Desert | Sedona Cathedral Rock Buttes, Adobe Mission, Sonoran Saguaro Cacti |
| `grand_canyon` | Grand Canyon, AZ | Terraced Sandstone Buttes, Ancestral Pueblo Dwellings, Colorado River Rapids |
| `las_vegas` | Las Vegas Strip, NV | High Roller Observation Wheel, Strip Mega-Resorts, Bellagio Fountains |
| `san_francisco` | San Francisco, CA | Golden Gate Bridge, Alcatraz Lighthouse, Painted Ladies, Cable Car |
| `seattle_pnw` | Seattle & PNW | Space Needle, Pike Place Market Clock Sign, Mt. Rainier, Evergreen Forest |
| `coastal` | New England Harbor | Striped Coastal Lighthouse, Stone Pier, Schooners & Historic Maritime Wharf |
| `london_uk` | London, UK | Big Ben Elizabeth Tower, Palace of Westminster, Tower of London, Red Bus |
| `paris_europe` | Paris, France | Notre-Dame Cathedral, Louvre Palace, Arc de Triomphe, Seine Stone Bridge |
| `rome_mediterranean` | Rome, Italy | Colosseum, Parthenon Acropolis, Roman Aqueduct, Mediterranean Lighthouse |
| `cairo_giza` | Cairo & Giza, Egypt | Ancient Sandstone Citadel, Sacred Temple Pool, Nile River, Giza Plateau |
| `tokyo_east_asia` | Tokyo, Japan | 5-Story Crimson Pagoda, Shinjuku Modern Skyline, Arched Garden Bridge |
| `alpine` | Alpine Mountain Village | Snow-Capped Mountain Peak, Timber Chalets, Glacial Lake, Stone Bridge |
| `modern` | Global Metropolis | Contemporary Glass Skyscraper District, Harbour Bridge, Waterfront Park |

### 6. Android Homescreen AppWidget & Native App (`v0.0.6`)
- **4×3 Resizable Homescreen AppWidget (`IsometricWeatherWidgetProvider.kt`)**:
  - **Authentic Top-Sky Split HUD**: Clean typography in the upper sky with 0% overlap on the 3D diorama.
  - **Interactive Multi-Target Taps**:
    - Tap the **TextClock** to launch the device's native Clock / Alarm app.
    - Tap the **Weather Conditions / Temp** to open the native 7-day `ForecastActivity`.
    - Tap the **3D Diorama Island** to open the full configuration app.
  - **Smooth Weather Animations**: Uses a 4-frame `<ViewFlipper>` for rain streaks, snowfall, and water splash ripples directly on the homescreen.
- **Native `ForecastActivity.kt`**:
  - Fixed animation glitch: Single smooth 240ms transition (`TransitionSet` with `ChangeBounds` and `Fade`) when expanding day rows with zero double-collapse.
  - Interactive hourly forecast charts with precipitation, temperature, and wind graphs.
- **Hardware GPS & Battery Optimization**:
  - Resolves true physical coordinates via native Android GPS and network location providers.
  - Configurable update intervals (15m, 30m, 1h, 2h, or manual refresh) via `AlarmManager`.
- **Floating Manual Refresh Button**: Transparent red refresh button (`↻`) at the bottom-right of the render view for instant weather and diorama updates.

### 7. KDE Plasma 6 Desktop Plasmoid
- Native user-space plasmoid for KDE Plasma 6 (`~/.local/share/plasma/plasmoids/`).
- Real-time weather, AQI, UV index, lunar synodic phases, and 12-hour temperature/precipitation sparklines.
- Solar cycle lighting simulation with smooth transitions between day, golden hour sunset, and night.

---

## 📂 Repository Structure

```text
├── src/                                    # Modern React 18 + TypeScript Source
│   ├── components/
│   │   ├── RadarModal.tsx                  # Interactive Doppler radar modal (RainViewer API + Canvas)
│   │   ├── WeatherAlertsBanner.tsx         # Pulsing glassmorphic severe weather warning banner
│   │   ├── AlertDetailsModal.tsx           # Full emergency statement, severity badges & safety tips
│   │   ├── SettingsModal.tsx               # 6-Tab framework settings & configuration suite
│   │   ├── WeatherHudCard.tsx              # Telemetry HUD card, digital clock, quick actions
│   │   ├── WeatherCanvas.tsx               # 3D diorama canvas host & camera controls
│   │   ├── ForecastModal.tsx               # 7-day outlook container with day rows & hourly chart
│   │   ├── DayForecastRow.tsx              # Animated day forecast accordion row
│   │   ├── HourlyBreakdownChart.tsx        # High-DPI 24h interactive forecast canvas graph
│   │   └── WeatherProviderSelector.tsx     # Meteorological provider dropdown with key config
│   ├── services/
│   │   ├── alerts/AlertService.ts          # NOAA NWS API + WMO telemetry hazard analyzer
│   │   ├── radar/RadarService.ts           # RainViewer API, Web Mercator tiling & tile cache
│   │   └── weather/WeatherService.ts       # Multi-provider coordinator & Open-Meteo fallback
│   ├── engine/IsometricEngine.ts           # 3D diorama rendering, weather physics & cloud shadows
│   ├── types/weather.ts                    # TypeScript types (Alerts, Radar, Settings, Telemetry)
│   └── tests/                              # Vitest + React Testing Library Spec Suite (11 suites / 56 tests)
│       ├── AlertService.spec.ts            # NWS GeoJSON & WMO condition analysis tests
│       ├── WeatherAlerts.spec.tsx          # Warning banner & alert modal spec tests
│       ├── RadarService.spec.ts            # Web Mercator math & tile URL generation tests
│       ├── RadarModal.spec.tsx             # Interactive radar controls & scrubber tests
│       ├── SettingsModal.spec.tsx          # 6-Tab settings switching & persistence tests
│       ├── WeatherService.spec.ts          # Multi-provider & Open-Meteo fallback tests
│       ├── ForecastModal.spec.tsx          # 7-Day outlook modal tests
│       ├── DayForecastRow.spec.tsx         # Accordion row expansion tests
│       ├── HourlyBreakdownChart.spec.tsx   # Canvas chart rendering tests
│       ├── WeatherHudCard.spec.tsx         # Telemetry HUD tests
│       └── WeatherProviderSelector.spec.tsx# Provider selector tests
├── package/                                # Native KDE Plasma 6 Plasmoid (QML + ES6 JS)
│   ├── metadata.json                       # Plasma 6 KPackage manifest (com.antigravity.yorktownweather)
│   └── contents/
│       ├── config/                         # KConfig XT schemas
│       ├── images/                         # 28 Pre-generated Day/Sunset/Night 3D cutouts (84 PNGs)
│       └── ui/                             # QML UI components & scenes
├── web/                                    # Built Web Distribution & PWA Assets
│   ├── index.html                          # Production React bundle entry
│   ├── assets/                             # Compiled CSS & JS bundles
│   └── images/                             # 84 Bundled diorama PNGs
├── android/                                # Native Android Application & AppWidget
│   └── app/src/main/
│       ├── java/com/antigravity/yorktownweather/
│       │   ├── DeviceLocationManager.kt    # Native GPS/Network provider & diorama auto-matcher
│       │   ├── IsometricWeatherWidgetProvider.kt # Homescreen AppWidget provider (RemoteViews)
│       │   ├── ForecastActivity.kt         # Native 7-day forecast with smooth 240ms transition
│       │   └── MainActivity.kt             # Native Android configuration activity & WebView preview
│       ├── res/                            # Native layouts, drawables, and widget XML configurations
│       └── assets/web/                     # Embedded React web application assets
├── dist/                                   # Release Packages & Cryptographic Checksums
│   ├── global-3d-isometric-weather-v0.0.6.apk
│   ├── global-3d-isometric-weather-v0.0.6.plasmoid
│   ├── global-3d-isometric-weather-web-android-v0.0.6.zip
│   ├── RELEASE_NOTES_v0.0.6.md
│   └── SHA256SUMS.txt
├── scripts/
│   ├── publish_github_release.sh           # SSH release pusher & GitHub Release publisher
│   ├── install_android_usb.sh              # One-command USB ADB installer for Android phones
│   └── run_android_emulator.sh             # AVD emulator test launcher
├── vite.config.ts                          # Vite build & Vitest test runner configuration
└── tsconfig.json                           # Strict TypeScript configuration
```

---

## 🛠️ Build, Test & Installation Instructions

### 1. Web Application & Spec Test Suite

#### Run All Vitest UI Spec Tests (11 Suites / 56 Tests):
```bash
npm test -- --run
```

#### Build Production Web Bundle:
```bash
npm run build
```
Build output is generated into `dist-web/` and synchronized to `web/` and `android/app/src/main/assets/web/`.

---

### 2. Android App & Homescreen Widget (OnePlus 13 & Android 8.0+)

#### Install via Direct APK:
Download `global-3d-isometric-weather-v0.0.6.apk` from [GitHub Releases](../../releases) and install directly on your device.

#### Install via USB ADB:
```bash
./scripts/install_android_usb.sh
```

#### Run Unit Tests & Build Release APK from Source:
```bash
export JAVA_HOME=/home/gcerkez/.local/jdk17
export ANDROID_HOME=/home/gcerkez/.local/android-sdk
gradle -p android testDebugUnitTest
gradle -p android assembleRelease
# Output APK: android/app/build/outputs/apk/release/app-release.apk
```

---

### 3. KDE Plasma 6 Desktop Plasmoid (Linux Desktop / Bazzite / SteamOS)

#### Install from Release `.plasmoid` Archive:
```bash
kpackagetool6 --type Plasma/Applet --install dist/global-3d-isometric-weather-v0.0.6.plasmoid
# Or to upgrade an existing installation:
kpackagetool6 --type Plasma/Applet --upgrade dist/global-3d-isometric-weather-v0.0.6.plasmoid
```

#### Build & Install from Source:
```bash
./install.sh
```

---

## 📄 License

Released under the **GPL-3.0-or-later** license.
