# Global 3D Isometric Weather Widget (`v0.0.2`)

A **Global 3D Isometric Real-Time Weather Widget** built for **KDE Plasma 6 (Bazzite / Linux Desktop)**, **Web / PWA**, and **Android Homescreen & Lockscreen (OnePlus 13 / OxygenOS 15 & Android 8.0+)**.

Inspired by tilt-shift isometric architectural dioramas with **Jamestown-style translucent turquoise cutaway water blocks**, the widget includes **18 Pre-Generated High-Resolution 3D Isometric Diorama Cutout Sets** (`Day`, `Sunset / Golden Hour`, and `Starry Night` variants = **54 bundled PNG cutouts**) covering major US regions, populated global timezones, **National Park Service (NPS) / National Register of Historic Places** landmarks, and **UNESCO World Heritage** icons.

---

## Visual Previews

| Sunny Daylight (Hybrid 3D Diorama) | Night Rain & Expanded HUD (AQI / UV / Moon / 12h Sparkline) | Interactive Procedural 3D Mode |
| :---: | :---: | :---: |
| ![Sunny Day](docs/screenshots/yorktown_widget_sunny_day.png) | ![Night Storm](docs/screenshots/yorktown_widget_night_storm.png) | ![Procedural 3D](docs/screenshots/yorktown_widget_procedural3d.png) |

---

## Key Features

### 1. 18 Pre-Generated Regional, NPS & UNESCO 3D Isometric Dioramas (Dropdown Selector)
All 18 pre-generated 3D isometric floating-island dioramas feature **Jamestown-style translucent turquoise cutaway water** and come with matching **Daylight**, **Golden Hour Sunset**, and **Starry Night** textures organized in a clean dropdown selector:

| Theme ID | Region / Timezone / Heritage Designation | Iconic Landmarks & Architectural Features |
| :--- | :--- | :--- |
| `yorktown` | **Historic Yorktown, VA** (`America/New_York` • NPS Colonial Historical Park) | 1781 Colonial Waterfront, George P. Coleman Memorial Bridge, 18th-Century Tall Ship, Victory Monument & Siege Redoubts |
| `jamestown` | **Historic Jamestown, VA** (`America/New_York` • NPS / 1607 Settlement) | 1607 Triangular James Fort Wooden Palisade, Colonial Brick Church, Thatch Houses, *Susan Constant*, *Godspeed* & *Discovery* on Turquoise James River |
| `washington_dc` | **Washington, D.C. & Mid-Atlantic** (`America/New_York` • NPS National Mall) | U.S. Capitol Dome, Washington Monument Obelisk, Lincoln Memorial, Jefferson Memorial, Cherry Blossoms & Tidal Basin |
| `new_york` | **New York City & Northeast** (`America/New_York` • UNESCO / NPS) | Statue of Liberty, Brooklyn Bridge Gothic Towers, Empire State & Chrysler Skyline, Central Park & Staten Island Ferry |
| `san_francisco` | **San Francisco & California Coast** (`America/Los_Angeles` • NPS Golden Gate) | International-Orange Golden Gate Bridge, Alcatraz Island Lighthouse, Painted Ladies Victorians, Coit Tower & Cable Car |
| `chicago_great_lakes` | **Chicago & Great Lakes Midwest** (`America/Chicago`) | Willis Tower, Red Double-Leaf Bascule Drawbridge, Navy Pier Ferris Wheel, Cloud Gate Bean & Lake Michigan |
| `grand_canyon` | **Grand Canyon & American Southwest** (`America/Phoenix` & `America/Denver` • UNESCO / NPS) | Terraced Red-Rock Sandstone Buttes, Ancestral Pueblo Cliff Dwellings, Desert Arch Bridge & Turquoise Colorado River Rapids |
| `new_orleans` | **New Orleans, Gulf Coast & Mississippi** (`America/Chicago` • NPS French Quarter) | St. Louis Cathedral Triple Spires, Cast-Iron French Quarter Balconies, Streetcar, Paddlewheel Steamboat & Cypress Bayou |
| `seattle_pnw` | **Seattle & Pacific Northwest** (`America/Los_Angeles` & `America/Anchorage` • NPS) | Space Needle, Pike Place Market Clock Sign, Snow-Capped Mt. Rainier, Evergreen Forest & Puget Sound Ferry |
| `miami_caribbean` | **Miami Beach, Florida Keys & Tropics** (`America/New_York` & `Pacific/Honolulu`) | Pastel Art Deco Ocean Drive Hotels, Seven Mile Overseas Bridge, Cape Florida Lighthouse & Turquoise Coral Reef Lagoon |
| `coastal` | **New England & Maritime Atlantic** (`America/New_York` & `America/Halifax`) | Striped Coastal Lighthouse on Rocky Headland, Stone Pier, Schooners & Historic Maritime Wharf |
| `london_uk` | **London & British Isles** (`Europe/London` • UNESCO World Heritage) | Big Ben Elizabeth Tower, Palace of Westminster, Tower of London Keep, Gothic Abbey, Red Phone Box, Double-Decker Bus & Thames Bridge |
| `paris_europe` | **Paris & Western Europe** (`Europe/Paris` & `Europe/Berlin` • UNESCO) | Notre-Dame Cathedral, Louvre / Versailles Palace, Arc de Triomphe, Tuileries Fountain Gardens & Seine Stone Bridge |
| `rome_mediterranean` | **Rome, Athens & Mediterranean** (`Europe/Rome` & `Europe/Athens` • UNESCO) | Roman Colosseum, Parthenon Acropolis Temple, Roman Aqueduct, Pantheon Basilica, Triumphal Arch & Mediterranean Lighthouse |
| `cairo_giza` | **Cairo, Giza & Middle East** (`Africa/Cairo` & `Asia/Dubai` • UNESCO) | Ancient Sandstone Citadel, Sacred Torch-Lit Temple Pool, Chariot Colonnade Temple, Pharos of Alexandria & Nile River |
| `tokyo_east_asia` | **Tokyo, Kyoto, Beijing & East Asia** (`Asia/Tokyo` & `Asia/Shanghai` • UNESCO) | 5-Story Crimson Pagoda, Imperial Palace Hall, Traditional Multi-Tiered Temple, Arched Garden Bridge & Harbor |
| `modern` | **Global Modern Metropolis** (`Australia/Sydney`, `Asia/Singapore`, `America/Toronto`) | Contemporary Glass Skyscraper District, Harbour Suspension Bridge, Waterfront Park & Commuter Ferry |
| `alpine` | **Alpine Mountain & Glacial Lake Village** (`Europe/Zurich`, Rockies & Andes) | Snow-Capped Mountain Peak, Timber Chalets, Evergreen Forest, Stone Arch Bridge & Turquoise Glacial Lake |

### 2. Decoupled Weather Location & 3D Diorama Visual
- Your **live weather coordinates (`lat, lon`) are completely independent of your selected 3D Isometric Diorama**.
- For example, if you live in **Tabb, Virginia (`37.1310, -76.4452`)**, you can configure **Tabb, VA** (or any global city via Open-Meteo geocoding search) for live temperature, wind, AQI, UV, and forecast telemetry while displaying the **Historic Jamestown**, **Historic Yorktown**, **Grand Canyon**, or **Tokyo** 3D isometric render!

### 3. Isometric Zoom & Horizontal Centering (Clean View & Overlay Stays Fixed)
- **Zoom & Horizontal Offset (`➖` `➕` `◀` `🎯` `▶`)**:
  - **Rotation is intentionally disabled** so the isometric island always stays at its crisp architectural projection angle.
  - **Move Isometric Left / Center / Right (`◀` `🎯` `▶` or Horizontal Drag / Slider)**: Shift the entire 3D isometric diorama left or right as a whole (`-220 px` to `+220 px`) to frame or center it wherever you like **while the Weather HUD Overlay stays stationary in its configured position**.
  - **Clean Presentation (Hidden Camera Buttons & Settings Gear)**: On-screen camera movement pills and settings gears are cleanly hidden from view on the widget canvas. Tapping the 3D diorama canvas opens the configuration settings.
  - **Zoom In / Out (`➖` `➕` / Scroll Wheel / Pinch)**: Smoothly scale the isometric diorama from `65%` to `240%`.

### 4. Precipitation & Weather Events Centered Over 3D Island
- **Strictly Bounded Island Weather**: Rain streaks, thunderstorm rain, and snowflakes are mathematically bounded directly over the 3D diorama island (`islandLeft..islandRight` and `islandTop..islandBottom`).
- **Ground & Water Splash Ripples**: Animated splash ripples render on the diorama ground and cutaway water surfaces instead of drifting off to the bottom-right corner.

### 5. Interactive 7-Day & Hourly Forecast & True GPS Auto-Detection
- **Tap Overlay for Full Forecast**: Tapping the Weather HUD banner or corner card immediately opens the detailed 7-day and hourly weather forecast in your web browser (NWS `forecast.weather.gov` for US locations or `weather.com` worldwide).
- **True Hardware GPS & Network Location Acquisition**: Resolves your phone's true physical coordinates using Android's native GPS and network location providers (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) rather than cellular carrier IP proxies (which often route mobile data through distant regional ISP gateways in Washington, D.C.).
- **Periodic Background Location Updates**: When "Follow Phone GPS Location" is enabled, the widget periodically re-checks your position every 15 minutes via `AlarmManager` and `onUpdate`, keeping your weather and city name continuously updated as you travel.
- **Desktop & Web Auto-Detection**: Supports instant one-click geolocation auto-detection on Desktop KDE Plasma 6 and Web.

### 6. Integrated Sky Digital Clock & High-Readability Android HUD
- **Live Digital Clock**: An elegant, high-contrast digital clock is embedded directly into the Weather HUD banner and corner card across all platforms (`TextClock` in Android `RemoteViews`, 1-second live `Timer` in KDE Plasma 6 `WeatherHudCard.qml`, and `setInterval` in Web HTML5). Positioned in the upper sky to ensure zero encroachment on the 3D diorama render.
- **Extrapolated High-Readability Android HUD**:
  - Native high-DPI `RemoteViews` typography (**Top-Sky Split Banner** or **Corner Card**) with auto-fitting text scaling (`100%`, `115% Recommended`, `130% Extra-Large`) preventing text cutoff or squashing on any phone screen.
  - Uses an auto-animating 4-frame `<ViewFlipper>` so gentle rain/snow and water ripples animate live on your Homescreen.

### 7. Spacious, Scroll-Safe Android Configuration Menu
- The Android Configuration App (`MainActivity.kt`) uses a **single full-screen vertical `ScrollView`** with `FOCUS_BEFORE_DESCENDANTS` and replaces hair-trigger `Spinner` dropdowns with **scroll-safe modal `AlertDialog` setting cards** that only open on a deliberate tap—eliminating accidental dropdown pops or keyboard focus thefts while scrolling.

### 8. Static Scenic Miniature Sprites & Calm Weather Physics
- **Static Scenic Miniature Sprites**: Stationed colonial Horse & Carriage on Historic Main Street, miniature cars on the Coleman Bridge, and harbor ships/sailboats (animated walking people removed for a clean, distraction-free architectural model aesthetic).
- **Calm, Natural-Speed Weather Particles**: Gentle rain and snow physics (`25%`, `50%`, `100%` intensity) paired with US EPA AQI, WHO UV Index, Lunar Synodic Phase, 12-Hour Temperature/Precipitation Sparkline, 3D Volumetric Cloud Ground Shadows, and Post-Rain Rainbow arcs.

> 📖 **Architecture & Feature Walkthrough**: See [WALKTHROUGH.md](WALKTHROUGH.md) for a detailed architectural tour and file-by-file breakdown.

---

## Multi-Platform Architecture

```text
├── package/                                # Native KDE Plasma 6 Plasmoid (QML + ES6 JS)
│   ├── metadata.json                       # Plasma 6 KPackage manifest (com.antigravity.yorktownweather)
│   └── contents/
│       ├── config/                         # KConfig XT schema (dioramaTheme, dioramaZoom, dioramaOffsetX, etc.)
│       ├── images/                         # 18 Pre-generated Day/Sunset/Night 3D cutouts (54 PNGs)
│       └── ui/
│           ├── main.qml                    # Root PlasmoidItem & Open-Meteo / AQI orchestrator
│           ├── YorktownHybridScene.qml     # 18 Pre-generated dioramas + static sprites + zoom/horizontal-pan + water/light shader
│           ├── YorktownProcedural3D.qml    # Zoomable/centerable 3D isometric engine + static sprites + OSM buildings
│           ├── WeatherEffectsOverlay.qml   # 3D clouds, ground shadows, moon phase, rainbow, gentle rain/snow
│           ├── WeatherHudCard.qml          # Stationary HUD card + AQI/UV/Moon + 12h sparkline + 3-day forecast
│           ├── config/
│           │   ├── ConfigAppearance.qml    # 18-theme Diorama dropdown, render mode, zoom & horizontal offset
│           │   └── ConfigLocation.qml      # Decoupled weather city search, presets (Tabb, Yorktown, Jamestown, etc.) & units
│           └── js/
│               ├── AstronomyAndAqi.js      # Lunar synodic phase, solar golden hour, US EPA AQI & UV math
│               └── LocationGenerator.js    # Wikipedia GeoSearch & OSM Overpass 3D helper
├── web/                                    # Portable HTML5 Canvas / PWA Widget (Desktop, Browser, WebView)
│   ├── index.html                          # Standalone responsive widget, HUD, 18-theme dropdown & zoom/center controls
│   ├── isometric-weather.js                # Portable ES6 IsometricWeatherEngine module
│   └── images/                             # 18 Pre-generated Day/Sunset/Night 3D cutouts (54 PNGs)
├── android/                                # Android Homescreen & Lockscreen AppWidget (OnePlus 13 / Android 12+)
│   └── app/src/main/
│       ├── java/com/antigravity/yorktownweather/
│       │   ├── DeviceLocationManager.kt                   # Hardware GPS/Network provider + Haversine nearest-diorama auto-matcher
│       │   ├── IsometricWeatherWidgetProvider.kt          # Full-canvas weather rendering + extrapolated HUD + auto-matching
│       │   └── MainActivity.kt                            # 6-Tab configuration UI + live 3D preview & camera framing tab
│       ├── res/layout/widget_isometric_weather.xml        # ViewFlipper + Top-Sky/Corner HUD
│       └── res/xml/isometric_weather_widget_info.xml      # 4x3 default grid, horizontal|vertical resizable
├── scripts/
│   ├── build_all_pregenerated_dioramas.py  # Offline 3D diorama cutout & Day/Sunset/Night generator script
│   ├── upgrade_yorktown_diorama.py         # Composites Yorktown landmarks onto the rounded-cliff turquoise-water island base (run after the script above)
│   ├── install_android_usb.sh              # One-command USB ADB installer for Android phones
│   └── publish_github_release.sh           # Packages & publishes v0.0.1 release assets to GitHub
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

#### 3. Select Any Pre-Generated 3D Diorama & Independent Weather Location
1. **Right-click the widget → Configure Global 3D Isometric Weather...**
2. In **3D Rendering & Weather**:
   - **3D Isometric Diorama**: Open the dropdown to pick from all **18 Pre-Generated Regional, NPS & UNESCO Dioramas** (`Historic Yorktown`, `Historic Jamestown`, `Washington, D.C.`, `New York City`, `San Francisco`, `Chicago & Great Lakes`, `Grand Canyon`, `New Orleans`, `Seattle & PNW`, `Miami & Keys`, `New England Harbor`, `London`, `Paris`, `Rome`, `Cairo`, `Tokyo`, `Global Modern Metropolis`, or `Alpine Village`).
   - **Isometric Zoom & Horizontal Position**: Use the sliders or buttons (`◀ Left`, `🎯 Center (0 px)`, `▶ Right`) to position the 3D island while keeping the HUD overlay stationary.
3. In **Location & Units**:
   - Search any city/town (e.g., **Tabb, VA**) or click a preset. Your weather telemetry updates to that location while your chosen 3D isometric diorama remains untouched.

---

### Option B: Android Homescreen & Lockscreen Widget (OnePlus 13 / OxygenOS 15 & Android 8.0+)

The Android app & widget are packaged as a signed `.apk` (`global-3d-isometric-weather-v0.0.2.apk`):
- **4×3 Resizable Homescreen & Lockscreen AppWidget (`IsometricWeatherWidgetProvider.kt`)**:
  - **Clean Top-Sky Split HUD Banner**: Lightweight native text in the upper sky with 0% overlap on the floating 3D diorama (corner card removed for optimal diorama visibility).
  - **Interactive Multi-Target Actions**:
    - Tap the **TextClock** to launch your native Alarm / Clock app (Google Clock, OnePlus Deskclock, Samsung Clock, Xiaomi Alarm).
    - Tap the **Weather Conditions / Temp** to open the 7-day detailed weather forecast in your browser.
    - Tap the **3D Diorama Island** to open the settings configurator.
  - **Live Weather Animation**: Uses an auto-animating 4-frame `<ViewFlipper>` with smooth rain streaks, snowfall, and water splash ripples directly over the diorama.
  - **Configurable Background Refresh**: Set periodic updates to 15m, 30m, 1h, 2h, or manual refresh to optimize battery consumption.
- **Modern 5-Tab Configuration App (`MainActivity.kt`)**:
  - **Tab 0 (`🖼️ 3D Preview`)**: Taller 330dp diorama canvas with minimalist icon-only buttons (`➖`, `➕`, `◀`, `🎯`, `▶`) and quick zoom presets.
  - **Tab 1 (`🏛️ Diorama Scene`)**: GPS auto-matcher and dropdown picker for all 18 world landmark dioramas.
  - **Tab 2 (`📍 Weather Location`)**: GPS auto-detection, travel follow toggle, and geocoding city search.
  - **Tab 3 (`☀️ Atmosphere`)**: Sun cycle, weather simulation overrides, and particle density.
  - **Tab 4 (`⚙️ Settings`)**: Top HUD scale, transparent cutout mode, extended telemetry, and refresh intervals.

#### 1. Install via Direct `.apk` Download or USB Cable (`adb`)
Download `global-3d-isometric-weather-v0.0.2.apk` from [GitHub Releases](../../releases) directly onto your phone and tap to install, or when connected via USB run:
```bash
./scripts/install_android_usb.sh
```

#### 2. Build `.apk` & Run Tests from Source
```bash
gradle -p android test assembleRelease
# Output APK: android/app/build/outputs/apk/release/app-release.apk
```

---

### Option C: Portable Web / PWA Widget

```bash
python3 -m http.server 8080 --directory web
# Open http://localhost:8080 in any desktop or mobile browser
```

---

## Developer Troubleshooting: Clearing QML & Android Widget Caches

### 1. KDE Plasma 6 (`plasmashell` QML Bytecode Cache)
To ensure modified QML files take effect and are not loaded from cached bytecode:
```bash
rm -rf ~/.cache/plasmashell/qmlcache/* ~/.cache/plasmawindowed/qmlcache/*
./install.sh
systemctl --user restart plasma-plasmashell.service
```

### 2. Android Launcher & AppWidget Cache (OnePlus OxygenOS 15 / Android 12+)
To force the Android launcher to re-inflate `RemoteViews` layouts and clear cached widget views:
```bash
adb install -r dist/global-3d-isometric-weather-v0.0.2.apk
adb shell am broadcast -a com.antigravity.yorktownweather.ACTION_REFRESH_WIDGET \
  -n com.antigravity.yorktownweather/.IsometricWeatherWidgetProvider
```
Or run the automated installer script:
```bash
./scripts/install_android_usb.sh
```

---

## License

Released under the **GPL-3.0-or-later** license.
