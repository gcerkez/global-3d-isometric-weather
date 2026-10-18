# Walkthrough — Global 3D Isometric Weather Widget (`v0.0.1`)

We designed, built, documented, and packaged the **Global 3D Isometric Weather Widget** (`com.antigravity.yorktownweather` `v0.0.1`) for **KDE Plasma 6 (Bazzite / Linux Desktop)**, **Web / PWA**, and **Android Homescreen & Lockscreen (OnePlus 13 / OxygenOS 15 & Android 12+)**.

---

## 1. Visual Previews (KDE Plasma 6 & Pre-Generated Regional / Landmark Dioramas)

| Sunny Daylight (Historic Yorktown 3D) | Night Rain & Expanded HUD (AQI / UV / Moon / 12h Sparkline) | Interactive Procedural 3D Mode |
| :---: | :---: | :---: |
| ![Sunny Day](docs/screenshots/yorktown_widget_sunny_day.png) | ![Night Storm](docs/screenshots/yorktown_widget_night_storm.png) | ![Procedural 3D](docs/screenshots/yorktown_widget_procedural3d.png) |

---

## 2. 18 Pre-Generated Regional, Timezone, NPS & UNESCO 3D Isometric Dioramas (Dropdown Selector)

Every pre-generated diorama features **Jamestown-style translucent turquoise cutaway water blocks** and ships with **Daylight**, **Golden Hour / Sunset**, and **Starry Night** transparent PNG variants (**54 bundled cutouts** across `package/contents/images/`, `web/images/`, and `android/app/src/main/assets/web/images/`), selectable from a clean dropdown in the Configuration Menu:

1. **`yorktown`** (`yorktown_island*.png`): **Historic Yorktown, VA** (NPS Colonial Historical Park — 1781 Colonial Waterfront, George P. Coleman Memorial Bridge, 18th-Century Tall Ship, Victory Monument & Siege Redoubts).
2. **`jamestown`** (`jamestown_island*.png`): **Historic Jamestown, VA** (NPS 1607 Settlement — Triangular James Fort Wooden Palisade, Colonial Brick Church, Thatch Houses, *Susan Constant*, *Godspeed* & *Discovery* on Turquoise James River).
3. **`washington_dc`** (`washington_dc_island*.png`): **Washington, D.C. & Mid-Atlantic** (`America/New_York` — U.S. Capitol Dome, Washington Monument Obelisk, Lincoln Memorial, Jefferson Memorial, Cherry Blossoms & Tidal Basin).
4. **`new_york`** (`new_york_island*.png`): **New York City & Northeast** (`America/New_York` UNESCO / NPS — Statue of Liberty, Brooklyn Bridge, Empire State & Chrysler Skyline, Central Park & Harbor Ferry).
5. **`san_francisco`** (`san_francisco_island*.png`): **San Francisco & California Coast** (`America/Los_Angeles` NPS — Golden Gate Bridge, Alcatraz Island Lighthouse, Painted Ladies Victorians, Coit Tower & Cable Car).
6. **`chicago_great_lakes`** (`chicago_great_lakes_island*.png`): **Chicago & Great Lakes Midwest** (`America/Chicago` — Willis Tower, Red Bascule Drawbridge, Navy Pier Ferris Wheel, Cloud Gate Bean & Lake Michigan).
7. **`grand_canyon`** (`grand_canyon_island*.png`): **Grand Canyon & American Southwest** (`America/Phoenix` & `America/Denver` UNESCO / NPS — Red-Rock Sandstone Buttes, Pueblo Cliff Dwellings, Arch Bridge & Turquoise Colorado River).
8. **`new_orleans`** (`new_orleans_island*.png`): **New Orleans, Gulf Coast & Mississippi** (`America/Chicago` NPS — St. Louis Cathedral, French Quarter Cast-Iron Balconies, Streetcar, Paddlewheel Steamboat & Bayou).
9. **`seattle_pnw`** (`seattle_pnw_island*.png`): **Seattle & Pacific Northwest** (`America/Los_Angeles` & `America/Anchorage` — Space Needle, Pike Place Market, Snow-Capped Mt. Rainier & Puget Sound Ferry).
10. **`miami_caribbean`** (`miami_caribbean_island*.png`): **Miami Beach, Florida Keys & Tropics** (`America/New_York` & `Pacific/Honolulu` — Pastel Art Deco Ocean Drive, Seven Mile Overseas Bridge, Lighthouse & Coral Reef Lagoon).
11. **`coastal`** (`coastal_island*.png`): **New England & Maritime Atlantic** (`America/New_York` & `America/Halifax` — Striped Coastal Lighthouse, Stone Pier, Schooners & Historic Wharf).
12. **`london_uk`** (`london_uk_island*.png`): **London & British Isles** (`Europe/London` UNESCO — Big Ben Elizabeth Tower, Parliament, Tower of London Keep, Westminster Abbey, Red Phone Box, Double-Decker Bus & Thames Bridge).
13. **`paris_europe`** (`paris_europe_island*.png`): **Paris & Western Europe** (`Europe/Paris` & `Europe/Berlin` UNESCO — Notre-Dame Cathedral, Louvre / Versailles Palace, Arc de Triomphe, Fountain Gardens & Seine Bridge).
14. **`rome_mediterranean`** (`rome_mediterranean_island*.png`): **Rome, Athens & Mediterranean** (`Europe/Rome` & `Europe/Athens` UNESCO — Roman Colosseum, Parthenon, Roman Aqueduct, Pantheon Basilica & Harbor Lighthouse).
15. **`cairo_giza`** (`cairo_giza_island*.png`): **Cairo, Giza & Middle East** (`Africa/Cairo` & `Asia/Dubai` UNESCO — Ancient Sandstone Citadel, Sacred Torch-Lit Temple Pool, Chariot Colonnade Temple & Pharos of Alexandria).
16. **`tokyo_east_asia`** (`tokyo_east_asia_island*.png`): **Tokyo, Kyoto, Beijing & East Asia** (`Asia/Tokyo` & `Asia/Shanghai` UNESCO — 5-Story Pagoda, Imperial Palace Hall, Traditional Multi-Tiered Temple & Garden Bridge).
17. **`modern`** (`modern_island*.png`): **Global Modern Metropolis** (`Australia/Sydney`, `Asia/Singapore`, `America/Toronto` — Glass Skyscraper District, Harbour Suspension Bridge & Ferry).
18. **`alpine`** (`alpine_island*.png`): **Alpine Mountain & Glacial Lake Village** (`Europe/Zurich`, Rockies & Andes — Snow-Capped Peak, Timber Chalets, Stone Arch Bridge & Turquoise Glacial Lake).

---

## 3. Decoupled Weather Telemetry Location & 3D Diorama Visual

- Weather coordinates (`latitude`, `longitude`, `locationName`) are completely independent of the selected 3D Isometric Diorama (`dioramaTheme`).
- Searching or selecting any town/city (such as **Tabb, VA**, **Williamsburg, VA**, or any global city via Open-Meteo geocoding) updates live weather telemetry without overwriting the user's chosen 3D isometric render.
- The unreliable online "Generate a Render" option has been removed across all platforms in favor of the 18 curated, zero-latency pre-generated 3D isometric cutout sets.

---

## 4. Isometric Zoom, Horizontal Centering, Hidden Camera Controls & Island-Centered Weather

- **Horizontal Centering / Offset (`◀` `🎯` `▶`) + Zoom (`➖` `➕`)**:
  - Rotation is disabled so the isometric island remains at its optimal architectural angle.
  - Users can shift the 3D isometric diorama left, center (`0 px`), or right (`-220 px` to `+220 px`) while the **Weather HUD Overlay stays stationary**.
- **Distraction-Free Visuals (Hidden Camera Buttons & Settings Gear)**:
  - On-screen zoom and camera movement buttons (`➖` `➕` `◀` `🎯` `▶`) and the settings gear (`⚙️`) are cleanly hidden from the widget canvas across Android, KDE Plasma 6 Desktop, and Web.
  - Tapping the 3D diorama canvas directly opens the configuration settings dialog.
- **Precipitation & Weather Events Centered Over 3D Island**:
  - Rain streaks, thunderstorm rain, and snowflakes are now mathematically calculated to fall strictly over the bounds of the 3D diorama island (`islandLeft..islandRight` and `islandTop..islandBottom`).
  - Animated ground and water splash ripples render directly on the island surfaces rather than floating off to the bottom-right.
- **Interactive 7-Day & Hourly Weather Forecast Link**:
  - Tapping the Weather HUD Overlay (banner or corner card) opens the official 7-day and hourly weather forecast in the browser (`forecast.weather.gov` for US locations or `weather.com` worldwide).
- **True Hardware GPS & Network Location Acquisition (`DeviceLocationManager.kt`)**:
  - Added Android runtime permissions `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` to acquire the phone's true physical coordinates via GPS and network providers rather than relying solely on cellular carrier IP proxies (which route mobile traffic through distant ISP data centers like Washington, D.C.).
  - Reverse-geocodes exact coordinates into local city names (e.g. `Yorktown, VA`, `Poquoson, VA`, `Tabb, VA`) using Android's native `Geocoder` with a robust fallback to BigDataCloud reverse geocoding.
  - **Periodic Background Location Updates**: When "Follow Phone GPS Location" is enabled, the widget periodically re-checks the device's location every 15 minutes via `AlarmManager` and `onUpdate`, automatically refreshing the weather and local city name as the user travels.
- **Integrated Sky Digital Clock**:
  - Live digital clock embedded in the HUD banner and corner card across all platforms (`TextClock` in Android `RemoteViews`, 1-second live `Timer` in KDE Plasma 6 `WeatherHudCard.qml`, and `setInterval` in Web HTML5), designed to sit elegantly in the upper sky without obstructing the 3D island.
- **Extrapolated High-Readability Android HUD (`widget_isometric_weather.xml` & `IsometricWeatherWidgetProvider.kt`)**:
  - Uses native high-DPI `RemoteViews` typography (`Top-Sky Split Banner` or `Corner Card`) with `100% / 115% / 130%` text scaling and auto-fitting text sizes so words and icons never clip or run off screen.
- **Spacious, Scroll-Safe Android Config Menu (`MainActivity.kt`)**:
  - Single full-screen `ScrollView` with `FOCUS_BEFORE_DESCENDANTS` and scroll-safe `AlertDialog` modal pickers.

---

## 5. Developer Cache-Clearing Reference

When developing or modifying QML components or Android widgets, local caches can sometimes serve stale bytecode or layout definitions:

- **KDE Plasma 6 (Desktop)**:
  Wipe QML bytecode caches and reload the Plasma shell service:
  ```bash
  rm -rf ~/.cache/plasmashell/qmlcache/* ~/.cache/plasmawindowed/qmlcache/*
  ./install.sh
  systemctl --user restart plasma-plasmashell.service
  ```
  Or preview standalone without restarting the shell:
  ```bash
  plasmawindowed com.antigravity.yorktownweather
  ```

- **Android (OnePlus 13 / ADB)**:
  Reinstall the APK with `-r` (replace existing) and broadcast the widget refresh intent to force `RemoteViews` re-inflation:
  ```bash
  adb install -r android/app/build/outputs/apk/release/app-release.apk
  adb shell am broadcast -a com.antigravity.yorktownweather.ACTION_REFRESH_WIDGET \
    -n com.antigravity.yorktownweather/.IsometricWeatherWidgetProvider
  ```
  Or run the automated helper script:
  ```bash
  ./scripts/install_android_usb.sh
  ```
