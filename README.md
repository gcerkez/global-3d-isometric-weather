# Yorktown & Global 3D Isometric Weather Widget (`v0.0.1`)

A **3D Isometric Real-Time & Generative Weather Widget** built for **KDE Plasma 6 (Bazzite / Linux Desktop)**, **Web / PWA**, and **Android Homescreen & Lockscreen (OnePlus 13 / Android 12+)**.

Inspired by tilt-shift isometric architectural dioramas, the widget defaults to **Historic Yorktown, VA** (featuring the **George P. Coleman Memorial Bridge**, **18th-Century Tall Ship on the York River**, **Colonial Main Street / Custom House / Nelson House**, **Yorktown Victory Monument**, and **1781 Revolutionary War Siege Redoubts**) while providing a **Generative Location Engine** that adapts to **any city on Earth**.

---

## Visual Previews

| Sunny Daylight (Hybrid 3D Diorama) | Night Rain & Expanded HUD (AQI / UV / Moon / 12h Sparkline) | Interactive Procedural 3D Mode |
| :---: | :---: | :---: |
| ![Sunny Day](docs/screenshots/yorktown_widget_sunny_day.png) | ![Night Storm](docs/screenshots/yorktown_widget_night_storm.png) | ![Procedural 3D](docs/screenshots/yorktown_widget_procedural3d.png) |

---

## Key Features

### 1. Three 3D Isometric Rendering Engines
- **Hybrid 3D Isometric Diorama (`hybrid`)**: High-detail tilt-shift isometric island cutout (`1376×768` RGBA with neutral drop shadow) paired with real-time 20–30 FPS QML/HTML5 Canvas shaders for water wave sparkles, snow frost accumulation, glowing colonial brick windows, streetlamps, bridge navigation lights, and monument beacons.
- **Interactive Procedural 3D Engine (`procedural3d`)**: Depth-sorted painter's-algorithm 3D isometric engine supporting mouse drag orbit, pitch tilt, scroll-wheel zoom, and double-click camera reset.
- **AI Generative Location Diorama (`ai_generative`)**: Automatically synthesizes a custom 3D isometric city diorama for any searched city using discovered local landmarks.

### 2. Generative Location Architecture (Works for Any City Globally)
Not in Yorktown, VA? Search **any city on Earth** in the widget settings (or Web HUD) and the widget automatically generates a location-specific scene via four zero-key pipelines (`package/contents/ui/js/LocationGenerator.js`):
1. **Open-Meteo Global Geocoding API**: Search any town, city, or postal code worldwide.
2. **Wikipedia GeoSearch Landmark Discovery**: Queries `en.wikipedia.org/w/api.php?action=query&list=geosearch` around your coordinates to populate real-time interactive landmark pins and descriptions on the isometric island.
3. **OpenStreetMap Overpass 3D Building Extrusion**: Queries real building footprints (`way["building"](around:350, lat, lon)`) and height/level tags (`building:levels`) to extrude real local buildings in **Interactive Procedural 3D Mode**.
4. **AI Isometric Diorama Generator (`scripts/generate_location_diorama.py` & `ai_generative` mode)**: Synthesizes custom transparent-background Day, Sunset, and Night 3D isometric dioramas for any city using discovered Wikipedia landmarks (via Google Gemini Image API or zero-key Flux synthesis).

### 3. REA (`Reverse Engineer Anything`) Weather & Atmospheric Features
Using **[`rea` (Reverse Engineer Anything)](https://github.com/morluto/rea)** inside a Distrobox container, we analyzed 8 open-source isometric weather and city repositories (`skytile`, `citycubes`, `mykonos-island-voxels`, `weatherWorld`, `tiny-patiala`, `advanced-weather-widget`, `minimalist-animated-weather`, and `isometric-city`) and implemented:
- **US EPA Air Quality Index (AQI) & WHO UV Index Pills**: Color-coded live badges in the expandable HUD drawer (`AstronomyAndAqi.js`).
- **Astronomical Lunar Synodic Phase**: Real-time moon phase calculation (`New Moon` through `Waning Crescent`) rendered both as a HUD badge and as a dynamically lit crescent/full moon disc in the night sky (`WeatherEffectsOverlay.qml`).
- **12-Hour Temperature & Precipitation Probability Sparkline**: Smooth Bezier temperature curve + hourly rain probability bars inside the HUD card (`WeatherHudCard.qml`).
- **3D Volumetric Cloud Ground Shadows, Post-Rain Rainbow & High-Wind Gusts**: Multi-puff isometric clouds cast soft ground shadows across the island, high winds (>12 mph) trigger angled air-current streaks, and clearing rain reveals a translucent rainbow arc.

See [`docs/REA_ANALYSIS.md`](docs/REA_ANALYSIS.md) for the complete REA reverse-engineering report and cryptographic Evidence IDs (`ev_...`).

---

## Multi-Platform Architecture

To support **KDE Plasma 6 Desktop (Bazzite / Linux)**, **Web/PWA**, and **Android Homescreen/Lockscreen (OnePlus 13)** from a shared design and JavaScript core:

```text
├── package/                                # Native KDE Plasma 6 Plasmoid (QML + ES6 JS)
│   ├── metadata.json                       # Plasma 6 KPackage manifest (com.antigravity.yorktownweather)
│   └── contents/
│       ├── config/                         # KConfig XT schema & settings pages
│       ├── images/                         # Transparent Day / Sunset / Night isometric cutouts
│       └── ui/
│           ├── main.qml                    # Root PlasmoidItem & Open-Meteo / AQI / Generative orchestrator
│           ├── YorktownHybridScene.qml     # High-detail diorama + water/window/landmark shader overlay
│           ├── YorktownProcedural3D.qml    # Rotatable/zoomable 3D isometric engine + OSM buildings
│           ├── WeatherEffectsOverlay.qml   # 3D clouds, ground shadows, moon phase, rainbow, rain/snow/fog
│           ├── WeatherHudCard.qml          # Top-right HUD card + AQI/UV/Moon + 12h sparkline + 3-day forecast
│           └── js/
│               ├── AstronomyAndAqi.js      # Lunar synodic phase, solar golden hour, US EPA AQI & UV math
│               └── LocationGenerator.js    # Wikipedia GeoSearch, OSM Overpass 3D, & AI diorama generator
├── web/                                    # Portable HTML5 Canvas / PWA Widget (Desktop, Browser, WebView)
│   ├── index.html                          # Standalone responsive widget & HUD
│   ├── isometric-weather.js                # Portable ES6 IsometricWeatherEngine module
│   └── manifest.json                       # PWA manifest
├── android/                                # Android Homescreen & Lockscreen AppWidget (OnePlus 13 / Android 12+)
│   └── app/src/main/
│       ├── java/com/antigravity/yorktownweather/IsometricWeatherWidgetProvider.kt
│       └── res/xml/isometric_weather_widget_info.xml  # widgetCategory="home_screen|keyguard"
├── scripts/
│   └── generate_location_diorama.py        # CLI tool to generate transparent Day/Sunset/Night dioramas for any city
├── docs/
│   └── REA_ANALYSIS.md                     # Reverse Engineer Anything (REA) analysis & evidence report
├── install.sh                              # One-step KDE Plasma 6 installer & .plasmoid packager
└── preview.py                              # Desktop preview launcher & offscreen screenshot verifier
```

---

## Build & Installation Instructions

### Option A: KDE Plasma 6 Desktop (Bazzite, Fedora Atomic, Arch, SteamOS, Ubuntu)

No root privileges or `rpm-ostree` layering required—installs cleanly into user-space (`~/.local/share/plasma/plasmoids/`).

#### 1. Install from Release `.plasmoid` Archive
Download `yorktown-3d-isometric-weather-v0.0.1.plasmoid` from the [GitHub Releases](../../releases) page and run:
```bash
kpackagetool6 --type Plasma/Applet --install yorktown-3d-isometric-weather-v0.0.1.plasmoid
# Or to upgrade an existing installation:
kpackagetool6 --type Plasma/Applet --upgrade yorktown-3d-isometric-weather-v0.0.1.plasmoid
```

#### 2. Build & Install from Source
```bash
git clone git@github.com:gcerkez/yorktown-3d-isometric-weather.git
cd yorktown-3d-isometric-weather
./install.sh
```

#### 3. Add to Your KDE Desktop or Panel
1. **Right-click** on your KDE desktop wallpaper and select **Enter Edit Mode** (or press `Meta+D`, then hold left-click).
2. Click **Add or Manage Widgets...** in the top Edit Mode toolbar.
3. Search for **`Yorktown 3D Isometric Weather`** and drag it onto your desktop.
4. **Right-click the widget → Configure Yorktown 3D Isometric Weather...** to customize:
   - **Location & Units**: Search any city worldwide or pick presets (`Yorktown, VA`, `Williamsburg, VA`, `San Francisco, CA`, etc.), choose `°F (°C)`, `°F`, or `°C`, and wind units (`MPH`, `KM/H`, `KN`, `M/S`).
   - **3D Rendering & Weather**: Switch between **Hybrid Isometric Art**, **Interactive Procedural 3D**, and **AI Generative Diorama**, toggle **Transparent Desktop Cutout Mode**, or preview any time of day and weather state.

#### 4. Test in a Standalone Window
```bash
plasmawindowed com.antigravity.yorktownweather
# Or use the Python preview utility:
./preview.py
```

---

### Option B: Generate Custom High-Res Diorama Cutouts for Any City (CLI)

Want the same crisp, transparent-background multi-time-of-day cutout quality as Yorktown for another city (e.g., `"Kyoto, Japan"`, `"Charleston, SC"`, or `"San Francisco, CA"`)? Run:

```bash
# Uses GEMINI_API_KEY if set, or zero-key Flux synthesis automatically:
./scripts/generate_location_diorama.py --city "Charleston, SC" --install-as-default
```
This discovers the city's top Wikipedia landmarks, synthesizes a 3D isometric diorama, removes the background with a soft neutral drop shadow, and generates matching `_day.png`, `_sunset.png`, and `_night.png` textures.

---

### Option C: Portable Web / PWA Widget

Run the cross-platform HTML5 Canvas / ES6 version in any browser, Electron/Tauri shell, or mobile PWA:

```bash
python3 -m http.server 8080 --directory web
# Open http://localhost:8080 in any desktop or mobile browser
```

---

### Option D: Android Homescreen & Lockscreen Widget (OnePlus 13 / OxygenOS 15 & Android 12+)

The `android/` directory provides a native Kotlin `AppWidgetProvider` (`IsometricWeatherWidgetProvider.kt`) configured with `android:widgetCategory="home_screen|keyguard"` so it works on both the **OnePlus 13 Homescreen** and **Lockscreen / Ambient Display**:

1. **Native APK Build**:
   - Open the `android/` module in **Android Studio Ladybug (2024.2+)** with Android SDK 34/35.
   - Copy `package/contents/images/yorktown_island*.png` into `android/app/src/main/assets/`.
   - Build & install onto your OnePlus 13 via `adb install app-debug.apk`.
   - Long-press the OnePlus 13 Homescreen → **Widgets** → **Yorktown 3D Isometric Weather** and drag to your Homescreen or Lockscreen.
2. **Instant PWA / KWGT Mode on OnePlus 13**:
   - Host or serve the `web/` bundle (or GitHub Pages) and add it to the OnePlus 13 Homescreen via Chrome (**Add to Home screen**), or load `web/index.html` inside a **KWGT (Kustom Widget Maker)** WebView/Bitmap layer.

---

## License

Released under the **GPL-3.0-or-later** license.
