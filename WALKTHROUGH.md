# Walkthrough — Global 3D Isometric Weather Widget (`v0.0.1`)

We designed, built, documented, and packaged the **Global 3D Isometric Weather Widget** (`com.antigravity.yorktownweather` `v0.0.1`) for **KDE Plasma 6 (Bazzite / Linux Desktop)**, **Web / PWA**, and **Android Homescreen & Lockscreen (OnePlus 13 / OxygenOS 15 & Android 12+)**.

---

## 1. Visual Previews (KDE Plasma 6 Renders)

| Sunny Daylight (Hybrid 3D Diorama) | Night Rain & Expanded HUD (AQI / UV / Moon / 12h Sparkline) | Interactive Procedural 3D Mode |
| :---: | :---: | :---: |
| ![Sunny Day](docs/screenshots/yorktown_widget_sunny_day.png) | ![Night Storm](docs/screenshots/yorktown_widget_night_storm.png) | ![Procedural 3D](docs/screenshots/yorktown_widget_procedural3d.png) |

---

## 2. Living Miniature Diorama Sprites & Universal Interactive Camera

- **5 Proportional Moving Sprites** (`package/contents/ui/YorktownHybridScene.qml`, `package/contents/ui/YorktownProcedural3D.qml`, `web/isometric-weather.js`, and `IsometricWeatherWidgetProvider.kt`):
  1. **Horse & Carriage on Historic Main Street** (or miniature town car in modern cities): Trots smoothly along the cobblestone street between the Custom House, Nelson House, and Swan Tavern with a warm carriage lantern at dusk/night.
  2. **Miniature Car on the George P. Coleman Memorial Bridge**: Travels across the double-swing steel truss bridge over the York River with glowing headlights at night.
  3. **Riverwalk Shoreline Pedestrian**: A tiny proportional pedestrian sprite strolling along the York River waterfront path.
  4. **Victory Monument Bluff Pedestrian**: A tiny proportional pedestrian sprite strolling along the bluff path between the Victory Monument and the 1781 Siege Redoubts.
  5. **18th-Century Tall Ship Sailor**: A tiny crew sprite moving along the wooden deck of the three-masted schooner on the York River.
- **Universal Interactive Zoom & Rotation**:
  - **KDE Desktop (`YorktownHybridScene.qml` & `YorktownProcedural3D.qml`)**: Supports mouse drag to rotate/tilt (`cameraYawDeg`, `cameraPitchDeg`), right-drag to pan, scroll wheel to zoom (`0.70x`–`2.25x`), double-click to reset, plus a compact bottom-left interactive toolbar (`➖` `➕` `↺` `↻` `⟲`).
  - **Web & Android Config App (`web/isometric-weather.js`, `web/index.html`, `MainActivity.kt`)**: Supports 2-finger pinch-to-zoom, 1-finger drag rotation, mouse wheel zoom, and on-screen camera buttons.
  - **Android Homescreen Widget (`widget_isometric_weather.xml`, `IsometricWeatherWidgetProvider.kt`)**: Includes direct on-widget buttons (`➖` `➕` `↺` `↻` `⟲` `⚙️`) in the bottom-left corner that broadcast camera updates to zoom and rotate the placed Homescreen widget immediately.

---

## 3. Android 4×3 Resizable Widget & Live Multi-Frame Homescreen Animation

- **4×3 Default Grid & Full Resizability** (`android/app/src/main/res/xml/isometric_weather_widget_info.xml`):
  - Configured with `targetCellWidth="4"`, `targetCellHeight="3"`, `minWidth="250dp"`, `minHeight="180dp"`, `minResizeWidth="180dp"`, `minResizeHeight="140dp"`, `maxResizeWidth="1000dp"`, `maxResizeHeight="900dp"`, and `resizeMode="horizontal|vertical"`.
  - Implements `onAppWidgetOptionsChanged` in `IsometricWeatherWidgetProvider.kt` so dragging the resize handles on OxygenOS 15 immediately re-renders the widget.
- **Live Homescreen Animation via `<ViewFlipper>`** (`android/app/src/main/res/layout/widget_isometric_weather.xml`):
  - Uses an auto-starting 4-frame `<ViewFlipper android:autoStart="true" android:flipInterval="520">` so rain streaks, snowflakes, York River water ripples, and the 5 miniature sprites continuously animate right on the Android Homescreen without freezing.

---

## 4. Generative Location Engine (Any City Worldwide)

To ensure users in any location get a tailored 3D isometric experience:
- `package/contents/ui/js/LocationGenerator.js`:
  - `fetchWikipediaLandmarks`: Queries the Wikipedia GeoSearch API around any selected coordinates to populate interactive landmark tooltips on the island.
  - `fetchOsmBuildings`: Queries the OpenStreetMap Overpass API (`way["building"]`) and projects real local building footprints and heights into the `YorktownProcedural3D.qml` 3D engine.
  - `inferRegionalArchetype`: Adapts bridge style (`coleman_truss`, `suspension_golden`, `stone_arch`), monument style, and building palettes to the region.
  - `buildZeroKeyAiDioramaUrl` & `scripts/generate_location_diorama.py`: Generates custom 3D isometric city dioramas for any city on Earth.

---

## 5. Atmospheric, Astronomical & Environmental Telemetry

- **Calm, Natural-Speed Weather Particles** (`package/contents/ui/WeatherEffectsOverlay.qml`, `web/isometric-weather.js`, `IsometricWeatherWidgetProvider.kt`):
  - Slowed down rain and snow fall speeds by ~4× and reduced particle density so even 25% intensity produces a gentle, atmospheric 4–6 drop shower rather than a fast downpour.
- **US EPA AQI, WHO UV Index & Lunar Synodic Phase** (`package/contents/ui/js/AstronomyAndAqi.js`):
  - Real-time lunar phase calculation (`New Moon` through `Waning Crescent`), solar Golden Hour window evaluation, and color-coded AQI & UV pills.
- **12-Hour Temperature & Precipitation Probability Sparkline** (`package/contents/ui/WeatherHudCard.qml`):
  - Smooth temperature curve + hourly precipitation probability bars inside the expandable HUD drawer.
- **3D Volumetric Cloud Ground Shadows, Post-Rain Rainbow & High-Wind Gusts** (`package/contents/ui/WeatherEffectsOverlay.qml`).

---

## 6. Developer Cache-Clearing Reference

- **KDE Plasma 6**:
  ```bash
  rm -rf ~/.cache/plasmashell/qmlcache/*
  ./install.sh
  systemctl --user restart plasma-plasmashell.service
  ```
- **Android (OnePlus 13 / ADB)**:
  ```bash
  ./scripts/install_android_usb.sh
  # If launcher caches old widget grid bounds, remove & re-add widget or run:
  adb shell pm clear com.antigravity.yorktownweather
  ```
