## Global 3D Isometric Weather Widget — `v0.0.1`

Release of **Global 3D Isometric Weather Widget** (`com.antigravity.yorktownweather`), a real-time 3D isometric weather diorama widget for **KDE Plasma 6 (Bazzite / Linux Desktop)**, **Android Homescreen & Lockscreen (OnePlus 13 / OxygenOS 15 & Android 8.0+)**, and **Web / PWA**.

---

### Highlights in `v0.0.1`

* **18 Pre-Generated Regional, Timezone, NPS & UNESCO 3D Isometric Diorama Sets (54 Cutout PNGs)**:
  * Every pre-generated diorama features **Jamestown-style translucent turquoise cutaway water** and includes **Daylight**, **Golden Hour / Sunset**, and **Starry Night** transparent PNG variants selectable from a clean dropdown menu:
    1. `yorktown` — **Historic Yorktown, VA** (NPS Colonial Historical Park — upgraded to the rounded geological cliff & translucent turquoise York River base with Victory Monument, 1781 Siege Redoubts, Colonial Brick Main Street, Coleman Bridge & Tall Ship)
    2. `jamestown` — **Historic Jamestown, VA** (NPS 1607 Settlement — Triangular James Fort Palisade, 3 Ships & Turquoise River)
    3. `washington_dc` — **Washington, D.C. & Mid-Atlantic** (`America/New_York` — U.S. Capitol, Washington Monument, Lincoln Memorial & Tidal Basin)
    4. `new_york` — **New York City & Northeast** (`America/New_York` UNESCO / NPS — Statue of Liberty, Brooklyn Bridge & Empire State)
    5. `san_francisco` — **San Francisco & California Coast** (`America/Los_Angeles` NPS — Golden Gate Bridge, Alcatraz & Painted Ladies)
    6. `chicago_great_lakes` — **Chicago & Great Lakes Midwest** (`America/Chicago` — Willis Tower, Red Bascule Bridge, Navy Pier & Cloud Gate Bean)
    7. `grand_canyon` — **Grand Canyon & American Southwest** (`America/Phoenix` & `America/Denver` UNESCO / NPS — Red-Rock Buttes, Cliff Dwellings & Colorado River)
    8. `new_orleans` — **New Orleans, Gulf Coast & Mississippi** (`America/Chicago` NPS — St. Louis Cathedral, French Quarter, Steamboat & Bayou)
    9. `seattle_pnw` — **Seattle & Pacific Northwest** (`America/Los_Angeles` & `America/Anchorage` — Space Needle, Pike Place, Mt. Rainier & Ferry)
    10. `miami_caribbean` — **Miami Beach, Florida Keys & Tropics** (`America/New_York` & `Pacific/Honolulu` — Pastel Art Deco Ocean Drive, Overseas Bridge & Coral Lagoon)
    11. `coastal` — **New England & Maritime Atlantic** (`America/New_York` & `America/Halifax` — Striped Coastal Lighthouse, Schooner & Wharf)
    12. `london_uk` — **London & British Isles** (`Europe/London` UNESCO — Big Ben, Parliament, Tower of London, Westminster Abbey & Thames Bridge)
    13. `paris_europe` — **Paris & Western Europe** (`Europe/Paris` & `Europe/Berlin` UNESCO — Notre-Dame Cathedral, Louvre Palace, Arc de Triomphe & Seine Bridge)
    14. `rome_mediterranean` — **Rome, Athens & Mediterranean** (`Europe/Rome` & `Europe/Athens` UNESCO — Colosseum, Parthenon, Roman Aqueduct & Harbor Lighthouse)
    15. `cairo_giza` — **Cairo, Giza & Middle East** (`Africa/Cairo` & `Asia/Dubai` UNESCO — Ancient Sandstone Citadel, Sacred Temple Pool & Pharos of Alexandria)
    16. `tokyo_east_asia` — **Tokyo, Kyoto, Beijing & East Asia** (`Asia/Tokyo` & `Asia/Shanghai` UNESCO — 5-Story Pagoda, Imperial Palace Hall & Torii Bridge)
    17. `modern` — **Global Modern Metropolis** (`Australia/Sydney`, `Asia/Singapore`, `America/Toronto` — Contemporary Skyline & Harbour Bridge)
    18. `alpine` — **Alpine Mountain & Glacial Lake Village** (`Europe/Zurich`, Rockies & Andes — Snow-Capped Peak, Chalets & Glacial Lake)
* **Full-Area Weather Precipitation & Water Splash Ripples**:
  * Rain streaks, thunderstorm rain, and snowflakes now span the entire frame dimensions (`0..width`, `0..height`), falling naturally across the full sky and blanketing the landscape with animated cutaway water splash ripples on the diorama surfaces.
* **Auto-Match Closest 3D Diorama to Physical GPS Location**:
  * Added an intelligent GPS diorama auto-matcher: using high-precision Haversine distance, the app automatically selects the nearest regional/landmark 3D diorama for your physical coordinates (e.g. automatically matching Historic Yorktown for Yorktown, Poquoson, Newport News, and Tabb, VA).
* **Modern Tabular Configuration View (Android)**:
  * Redesigned the configuration page into a clean, responsive 6-tab interface with zero scrolling conflicts:
    * **Tab 0 (`🖼️ 3D Preview & Framing`)**: Live 16:9 interactive 3D preview with camera zoom (`➖` / `➕`) and horizontal framing (`◀` / `🎯` / `▶`) controls, live readout, and 4 quick zoom presets (Far 85%, Standard 100%, Close-Up 125%, Hero Detail 160%).
    * **Tab 1 (`🏛️ Diorama Scene`)**: GPS diorama auto-match toggle and live landmark distance indicator, plus manual picker for all 18 world landmark dioramas.
    * **Tab 2 (`📍 Weather Location`)**: GPS Auto-detect button, travel follow toggle, live reverse-geocoding city search bar, and quick preset chips.
    * **Tab 3 (`🪟 HUD Overlay`)**: Top-sky banner vs corner card position, text scale (`100% / 115% / 130%`), transparent cutout mode, and telemetry toggles.
    * **Tab 4 (`☀️ Atmosphere & Weather`)**: Solar cycle / sky lighting, live weather simulation overrides (Clear, Clouds, Rain, Thunderstorm, Snow, Fog), and particle intensity.
    * **Tab 5 (`⚙️ Units & Preferences`)**: Temperature (°F/°C) and wind speed (MPH/km/h) formatting.
* **Decoupled Weather Telemetry Location & 3D Diorama Visual**:
  * Your weather coordinates (`lat, lon`) are completely independent of the active 3D isometric diorama—for example, you can monitor live weather in **Tabb, VA** while displaying the **Jamestown**, **Yorktown**, **Grand Canyon**, or **Tokyo** 3D render.
* **Distraction-Free Visuals (Hidden Camera Buttons & Settings Gear)**:
  * On-screen zoom and camera movement buttons (`➖` `➕` `◀` `🎯` `▶`) and the settings gear (`⚙️`) are cleanly hidden from the widget canvas across Android, KDE Plasma 6 Desktop, and Web.
  * Tapping the 3D diorama canvas opens the configuration settings dialog.
* **Interactive 7-Day & Hourly Weather Forecast on HUD Tap**:
  * Tapping the Weather HUD Overlay (banner or corner card) opens the official 7-day and hourly weather forecast in the browser (`forecast.weather.gov` for US locations or `weather.com` worldwide).
* **True Hardware GPS & Network Location Acquisition (`ACCESS_FINE_LOCATION`)**:
  * Resolves true physical coordinates via GPS and network providers rather than relying on cellular ISP IP proxies (which often route mobile data through Ashburn / Washington, D.C.).
  * Reverse-geocodes exact coordinates into local city names (e.g. `Yorktown, VA`, `Poquoson, VA`, `Tabb, VA`) using Android's native `Geocoder` and BigDataCloud.
  * **Periodic Background Updates**: When "Follow Phone GPS Location" is enabled, the widget checks for location changes every 15 minutes via `AlarmManager` and `onUpdate`, continuously syncing weather as the user travels.
* **Integrated Sky Digital Clock**:
  * Live digital clock embedded in the HUD banner and corner card across all platforms (`TextClock` in Android `RemoteViews`, 1-second live `Timer` in KDE Plasma 6 `WeatherHudCard.qml`, and `setInterval` in Web HTML5), designed to sit elegantly in the upper sky without obstructing the 3D island.
* **Extrapolated, High-Readability Android Homescreen HUD**:
  * Native high-DPI `RemoteViews` **Top-Sky Split Banner** and **Corner HUD Card** with `100% / 115% / 130%` text scaling that sits in the open upper sky without covering the 3D isometric town.
  * The HUD uses stacked full-width rows that auto-fit to the real widget width, so text and icons never run off the edge on narrow (4-column) phone widgets.

---

### Release Assets & Packaging

| Asset | Target Platform | Installation Method |
|---|---|---|
| `global-3d-isometric-weather-v0.0.1.apk` | **Android Homescreen & Lockscreen** (OnePlus 13 / Android 8.0+) | Download `.apk` directly on your phone and tap to install, or run `./scripts/install_android_usb.sh` over USB |
| `global-3d-isometric-weather-v0.0.1.plasmoid` | **KDE Plasma 6 Desktop** (Bazzite, Fedora, Arch, SteamOS, Kubuntu) | `kpackagetool6 --type Plasma/Applet --install global-3d-isometric-weather-v0.0.1.plasmoid` |
| `global-3d-isometric-weather-web-android-v0.0.1.zip` | **Web / PWA & Android Source Bundle** | Extract and serve `web/` or open `android/` in Android Studio |
| `SHA256SUMS.txt` | Checksum Verification | `sha256sum -c SHA256SUMS.txt` |
