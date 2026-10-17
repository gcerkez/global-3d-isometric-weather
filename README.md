# Global 3D Isometric Weather Widget (`v0.0.1`)

A **Global 3D Isometric Real-Time & Generative Weather Widget** built for **KDE Plasma 6 (Bazzite / Linux Desktop)**, **Web / PWA**, and **Android Homescreen & Lockscreen (OnePlus 13 / Android 12+)**.

Inspired by tilt-shift isometric architectural dioramas, the widget provides a **Generative Location Engine** that adapts to **any city on Earth** (and ships out-of-the-box with a high-detail diorama of **Historic Yorktown, VA** featuring the **George P. Coleman Memorial Bridge**, **18th-Century Tall Ship on the York River**, **Colonial Main Street / Custom House / Nelson House**, **Yorktown Victory Monument**, and **1781 Revolutionary War Siege Redoubts**).

---

## Visual Previews

| Sunny Daylight (Hybrid 3D Diorama) | Night Rain & Expanded HUD (AQI / UV / Moon / 12h Sparkline) | Interactive Procedural 3D Mode |
| :---: | :---: | :---: |
| ![Sunny Day](docs/screenshots/yorktown_widget_sunny_day.png) | ![Night Storm](docs/screenshots/yorktown_widget_night_storm.png) | ![Procedural 3D](docs/screenshots/yorktown_widget_procedural3d.png) |

---

## Key Features

### 1. Three 3D Isometric Rendering Engines
- **Hybrid 3D Isometric Diorama (`hybrid`)**: High-detail tilt-shift isometric island cutout (`1376×768` RGBA with neutral drop shadow) paired with real-time 20–30 FPS QML/HTML5 Canvas shaders for water wave sparkles, snow frost accumulation, glowing windows, streetlamps, bridge navigation lights, and monument beacons.
- **Interactive Procedural 3D Engine (`procedural3d`)**: Depth-sorted painter's-algorithm 3D isometric engine supporting mouse drag orbit, pitch tilt, scroll-wheel zoom, and double-click camera reset.
- **AI Generative Location Diorama (`ai_generative`)**: Automatically synthesizes a custom 3D isometric city diorama for any searched city using discovered local landmarks.

### 2. Generative Location Architecture (Works for Any City Globally)
Search **any city on Earth** in the widget settings (or Web HUD) and the widget automatically generates a location-specific scene via four zero-key pipelines (`package/contents/ui/js/LocationGenerator.js`):
1. **Open-Meteo Global Geocoding API**: Search any town, city, or postal code worldwide.
2. **Wikipedia GeoSearch Landmark Discovery**: Queries `en.wikipedia.org/w/api.php?action=query&list=geosearch` around your coordinates to populate real-time interactive landmark pins and descriptions on the isometric island.
3. **OpenStreetMap Overpass 3D Building Extrusion**: Queries real building footprints (`way["building"](around:350, lat, lon)`) and height/level tags (`building:levels`) to extrude real local buildings in **Interactive Procedural 3D Mode**.
4. **AI Isometric Diorama Generator (`scripts/generate_location_diorama.py` & `ai_generative` mode)**: Synthesizes custom transparent-background Day, Sunset, and Night 3D isometric dioramas for any city using discovered Wikipedia landmarks (via Google Gemini Image API or zero-key Flux synthesis).

### 3. Living Miniature Diorama Sprites & Interactive Isometric Camera (Desktop & Android)
- **Proportional Miniature Living Sprites (5 Active Sprites)**:
  - **Horse & Carriage on Historic Main Street** (or a miniature town car in modern cities) trotting between the Custom House, Nelson House, and Swan Tavern.
  - **Miniature Car on the George P. Coleman Memorial Bridge** crossing the double-swing steel truss bridge with glowing headlights at dusk/night.
  - **2 Miniature Pedestrians** strolling along the Yorktown Riverwalk shoreline path and the Victory Monument / Siege Redoubts bluff path.
  - **Tall Ship Crew / Sailor Sprite** moving along the wooden deck of the 18th-century schooner on the York River.
- **Universal Interactive Zoom & Rotation**:
  - **KDE Desktop & Web**: Scroll wheel or pinch to zoom (`0.70x` to `2.25x`), drag to rotate/tilt the isometric diorama in **both** Hybrid and Procedural 3D modes, double-click to reset, or use the bottom-left camera toolbar (`➖` `➕` `↺` `↻` `⟲`).
  - **Android Homescreen Widget & App**: Tap `➖` `➕` `↺` `↻` `⟲` directly on the bottom-left corner of the Homescreen widget (or pinch/drag in the config app) to zoom and rotate your placed widget.

### 4. Atmospheric, Astronomical & Environmental Telemetry
- **Calm, Natural-Speed Weather Particles**: Tuned rain and snow physics so precipitation falls at a gentle, realistic cadence (even at 25% subtle intensity, only 4–6 calm drops fall across the scene).
- **US EPA Air Quality Index (AQI) & WHO UV Index Pills**: Color-coded live badges in the expandable HUD drawer (`AstronomyAndAqi.js`).
- **Astronomical Lunar Synodic Phase**: Real-time moon phase calculation (`New Moon` through `Waning Crescent`) rendered both as a HUD badge and as a dynamically lit crescent/full moon disc in the night sky (`WeatherEffectsOverlay.qml`).
- **12-Hour Temperature & Precipitation Probability Sparkline**: Smooth Bezier temperature curve + hourly rain probability bars inside the HUD card (`WeatherHudCard.qml`).
- **3D Volumetric Cloud Ground Shadows, Post-Rain Rainbow & High-Wind Gusts**: Multi-puff isometric clouds cast soft ground shadows across the island, high winds (>12 mph) trigger angled air-current streaks, and clearing rain reveals a translucent rainbow arc.

> 📖 **Architecture & Feature Walkthrough**: See [WALKTHROUGH.md](WALKTHROUGH.md) for a detailed architectural tour and file-by-file breakdown.

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
│           ├── YorktownHybridScene.qml     # High-detail diorama + sprites + zoom/rotate + water/light shader
│           ├── YorktownProcedural3D.qml    # Rotatable/zoomable 3D isometric engine + sprites + OSM buildings
│           ├── WeatherEffectsOverlay.qml   # 3D clouds, ground shadows, moon phase, rainbow, gentle rain/snow
│           ├── WeatherHudCard.qml          # Top-right HUD card + AQI/UV/Moon + 12h sparkline + 3-day forecast
│           └── js/
│               ├── AstronomyAndAqi.js      # Lunar synodic phase, solar golden hour, US EPA AQI & UV math
│               └── LocationGenerator.js    # Wikipedia GeoSearch, OSM Overpass 3D, & AI diorama generator
├── web/                                    # Portable HTML5 Canvas / PWA Widget (Desktop, Browser, WebView)
│   ├── index.html                          # Standalone responsive widget, HUD & camera controls
│   ├── isometric-weather.js                # Portable ES6 IsometricWeatherEngine module
│   └── manifest.json                       # PWA manifest
├── android/                                # Android Homescreen & Lockscreen AppWidget (OnePlus 13 / Android 12+)
│   └── app/src/main/
│       ├── java/com/antigravity/yorktownweather/
│       │   ├── IsometricWeatherWidgetProvider.kt          # 4-frame live ViewFlipper widget & camera controls
│       │   └── MainActivity.kt                            # Live 3D preview & full widget configuration app
│       ├── res/layout/widget_isometric_weather.xml        # Auto-animating ViewFlipper + on-widget camera bar
│       └── res/xml/isometric_weather_widget_info.xml      # 4x3 default grid, horizontal|vertical resizable
├── scripts/
│   ├── generate_location_diorama.py        # CLI tool to generate transparent Day/Sunset/Night dioramas for any city
│   ├── install_android_usb.sh              # One-step ADB USB installer & widget broadcaster
│   └── publish_github_release.sh           # Automated GitHub Release publisher
├── WALKTHROUGH.md                          # Detailed architectural walkthrough & implementation reference
├── install.sh                              # One-step KDE Plasma 6 installer & .plasmoid packager
└── preview.py                              # Desktop preview launcher & offscreen screenshot verifier
```

---

## Build & Installation Instructions

### Option A: KDE Plasma 6 Desktop (Bazzite, Fedora Atomic, Arch, SteamOS, Ubuntu)

No root privileges or `rpm-ostree` layering required—installs cleanly into user-space (`~/.local/share/plasma/plasmoids/`).

#### 1. Install from Release `.plasmoid` Archive
Download `global-3d-isometric-weather-v0.0.1.plasmoid` from the [GitHub Releases](../../releases) page and run:
```bash
kpackagetool6 --type Plasma/Applet --install global-3d-isometric-weather-v0.0.1.plasmoid
# Or to upgrade an existing installation:
kpackagetool6 --type Plasma/Applet --upgrade global-3d-isometric-weather-v0.0.1.plasmoid
```

#### 2. Build & Install from Source
```bash
git clone git@github.com:gcerkez/global-3d-isometric-weather.git
cd global-3d-isometric-weather
./install.sh
```

#### 3. Add to Your KDE Desktop or Panel
1. **Right-click** on your KDE desktop wallpaper and select **Enter Edit Mode** (or press `Meta+D`, then hold left-click).
2. Click **Add or Manage Widgets...** in the top Edit Mode toolbar.
3. Search for **`Global 3D Isometric Weather`** and drag it onto your desktop.
4. **Right-click the widget → Configure Global 3D Isometric Weather...** to customize:
   - **Location & Units**: Search any city worldwide or pick presets (`Yorktown, VA`, `Williamsburg, VA`, `San Francisco, CA`, etc.), choose `°F (°C)`, `°F`, or `°C`, and wind units (`MPH`, `KM/H`, `KN`, `M/S`).
   - **3D Rendering & Weather**: Switch between **Hybrid Isometric Art**, **Interactive Procedural 3D**, and **AI Generative Diorama**, toggle **Transparent Desktop Cutout Mode**, adjust **Weather Particle Density** (`25%`–`150%`), or preview any time of day and weather state.

#### 4. Test in a Standalone Window
```bash
plasmawindowed com.antigravity.yorktownweather
# Or use the Python preview utility:
./preview.py
```

---

### Option B: Generate Custom High-Res Diorama Cutouts for Any City (CLI)

Want crisp, transparent-background multi-time-of-day cutouts for another city (e.g., `"Kyoto, Japan"`, `"Charleston, SC"`, or `"San Francisco, CA"`)? Run:

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

### Option D: Android Homescreen & Lockscreen Widget (OnePlus 13 / OxygenOS 15 & Android 8.0+)

The Android app & widget are packaged as a signed `.apk` (`global-3d-isometric-weather-v0.0.1.apk`) containing both:
- **4×3 Resizable Homescreen & Lockscreen AppWidget (`IsometricWeatherWidgetProvider.kt`)**:
  - Defaults to a **4×3 grid size** and supports full horizontal & vertical resizing (`resizeMode="horizontal|vertical"`, `onAppWidgetOptionsChanged`).
  - Uses an auto-animating 4-frame `<ViewFlipper>` (`widget_isometric_weather.xml`) so falling rain/snow, York River water ripples, and the 5 miniature sprites (Horse & Carriage, Bridge Car, Pedestrians, Sailor) **animate live right on your Homescreen**.
  - Includes an on-widget bottom-left toolbar (`➖` `➕` `↺` `↻` `⟲` `⚙️`) to zoom and rotate the isometric render directly from the Homescreen.
- **Interactive 3D Isometric Weather App & Configurator (`MainActivity.kt`)**: Full interactive 3D Isometric WebView + city search bar, presets, camera controls, weather intensity selector, and unit settings.

#### 1. Install Directly on Your Phone from GitHub Releases
Download `global-3d-isometric-weather-v0.0.1.apk` from the [GitHub Releases](../../releases) page on your phone and tap to install.

#### 2. Install Over USB Cable from Your Computer (`adb`)
1. On your OnePlus 13, enable **Developer options** (*Settings → About device → Version → Tap Build number 7 times*) and turn on **USB debugging** (*Settings → Additional settings → Developer options → USB debugging*).
2. Plug your phone into your computer via USB and tap **Allow** on the *Allow USB debugging?* prompt.
3. Run the included one-step USB installer:
   ```bash
   ./scripts/install_android_usb.sh
   ```
4. Long-press your OnePlus 13 Homescreen → **Widgets** → **Global 3D Isometric Weather** and drag the **4×3** widget onto your Homescreen or Lockscreen. Long-press the placed widget anytime to drag its resize handles!

#### 3. Build `.apk` from Source
```bash
gradle -p android assembleRelease
# Output APK: android/app/build/outputs/apk/release/app-release.apk
```

---

## Developer Troubleshooting: Clearing QML & Android Widget Caches

When iterating on QML shaders, JavaScript modules, or Android `AppWidgetProviderInfo` XML layouts, desktop shells and mobile launchers aggressively cache compiled assets in memory. Use the steps below to avoid stale UI or frozen widget layouts during development:

### 1. KDE Plasma 6 (`plasmashell` QML Cache)
KDE Plasma 6 keeps existing QML/JS files mapped in the running `plasmashell` process and stores precompiled `.qmlc` / `.jsc` bytecode in `~/.cache/plasmashell/qmlcache/`. Running `kpackagetool6 --upgrade` updates files on disk but **will not reload QML in an already-placed desktop widget** until `plasmashell` is restarted:
```bash
# Clear compiled QML bytecode cache and restart Plasma Shell cleanly:
rm -rf ~/.cache/plasmashell/qmlcache/*
./install.sh
systemctl --user restart plasma-plasmashell.service
```

### 2. Android Launcher & AppWidget Cache (OnePlus OxygenOS 15 / Android 12+)
Android's `AppWidgetService` and the OxygenOS launcher cache `isometric_weather_widget_info.xml` (such as minimum grid cell sizes and resize modes) when a widget is first bound. If you change `targetCellWidth`, `targetCellHeight`, or `<ViewFlipper>` IDs in XML:
```bash
# Reinstall APK and broadcast an immediate AppWidget update:
adb install -r dist/global-3d-isometric-weather-v0.0.1.apk
adb shell am broadcast -a com.antigravity.yorktownweather.ACTION_REFRESH_WIDGET \
  -n com.antigravity.yorktownweather/.IsometricWeatherWidgetProvider

# If the launcher still caches old grid resize bounds, remove the widget from the Homescreen
# and re-add it from the Widgets drawer (or clear app data via):
adb shell pm clear com.antigravity.yorktownweather
```

---

## License

Released under the **GPL-3.0-or-later** license.
