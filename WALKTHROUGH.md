# Walkthrough — Global 3D Isometric Weather Widget (`v0.0.1`)

We designed, built, documented, and packaged the **Global 3D Isometric Weather Widget** (`com.antigravity.yorktownweather` `v0.0.1`) for **KDE Plasma 6 (Bazzite / Linux Desktop)**, **Web / PWA**, and **Android Homescreen & Lockscreen (OnePlus 13 / OxygenOS 15 & Android 12+)**.

---

## 1. Visual Previews (KDE Plasma 6 & Pre-Generated Generic Dioramas)

| Sunny Daylight (Historic Yorktown 3D) | Night Rain & Expanded HUD (AQI / UV / Moon / 12h Sparkline) | Interactive Procedural 3D Mode |
| :---: | :---: | :---: |
| ![Sunny Day](docs/screenshots/yorktown_widget_sunny_day.png) | ![Night Storm](docs/screenshots/yorktown_widget_night_storm.png) | ![Procedural 3D](docs/screenshots/yorktown_widget_procedural3d.png) |

---

## 2. Built-In Pre-Generated Generic Dioramas & In-Menu Custom City Cutout Generator

- **4 Built-In Pre-Generated 3D Isometric Diorama Sets (`1376×768` RGBA with Day, Sunset & Night variants)**:
  1. **`yorktown`** (`yorktown_island*.png`): Historic Yorktown, VA (Coleman Memorial Bridge, 18th-Century Tall Ship, Colonial Main Street, Victory Monument, and 1781 Siege Redoubts).
  2. **`coastal`** (`coastal_island*.png`): Pre-Generated Generic Coastal Harbor & Lighthouse Town (Non-City-Specific).
  3. **`modern`** (`modern_island*.png`): Pre-Generated Generic Modern Downtown Metropolis & Suspension Bridge (Non-City-Specific).
  4. **`alpine`** (`alpine_island*.png`): Pre-Generated Generic Alpine Mountain & Lake Village (Non-City-Specific).
- **Integrated Config Menu Custom High-Res City Diorama Cutout Generator**:
  - **KDE Plasma 6** (`package/contents/ui/config/ConfigAppearance.qml` & `ConfigLocation.qml`): Includes a **`✨ Generate High-Res Cutout`** button right inside the widget settings that discovers local Wikipedia landmarks and synthesizes a high-resolution 3D isometric cutout for any city.
  - **Android** (`android/app/src/main/java/com/antigravity/yorktownweather/MainActivity.kt`): Includes an on-device **`🎨 Generate & Apply High-Res 3D Cutout Now`** card that queries Wikipedia GeoSearch, downloads a 3D isometric floating-island render, runs edge flood-fill background removal in Kotlin, and saves transparent `custom_city_day.png`, `custom_city_sunset.png`, and `custom_city_night.png` variants directly to device storage (`filesDir`).

---

## 3. Isometric Zoom & Horizontal Centering (Rotation Removed, Overlay Stationary)

- **Horizontal Centering / Offset (`◀` `🎯` `▶`) + Zoom (`➖` `➕`)**:
  - Rotation has been removed across all platforms (`YorktownHybridScene.qml`, `YorktownProcedural3D.qml`, `web/isometric-weather.js`, and `IsometricWeatherWidgetProvider.kt`) so the isometric scene always remains at its optimal architectural angle.
  - Users can move the entire 3D isometric diorama left, center (`0 px`), or right (`-220 px` to `+220 px`) using the bottom-left pill (`➖` `➕` `◀` `🎯` `▶`), horizontal mouse/touch drag, or Config Menu sliders while the **Weather HUD Overlay stays completely stationary** in its configured position.
- **Static Scenic Miniature Sprites**:
  - Animated walking people have been removed; static scenic miniature sprites (stationed Colonial Horse & Carriage on Historic Main Street, miniature cars on bridges, and sailboats) remain proportional to the 3D render.

---

## 4. Extrapolated Android Homescreen HUD & Scroll-Safe Config Menu

- **Extrapolated High-Readability Android HUD Without Covering the 3D Render** (`android/app/src/main/res/layout/widget_isometric_weather.xml` & `IsometricWeatherWidgetProvider.kt`):
  - Renders the weather overlay using crisp native Android `RemoteViews` `TextView`s (`17sp+` bold temperature, condition, location, humidity, wind, AQI, UV, and Moon phase) instead of downsampled bitmap text.
  - **Top-Sky Split Banner (`top-sky`, Default)**: Docks the wide, legible HUD across the upper sky while shifting the 3D isometric island downward (`islandOffsetY = +28f`) so **0% of the 3D rendering is obscured**.
  - **Configurable HUD Text Scale**: Choose `100%`, `115% (Recommended)`, or `130% (Extra-Large)` in the Android Config Menu.
- **Spacious, Scroll-Safe Android Config Menu** (`MainActivity.kt`):
  - Uses a single full-screen `ScrollView` with `FOCUS_BEFORE_DESCENDANTS` so `EditText` boxes never steal focus while scrolling.
  - Replaces all `Spinner` dropdowns with scroll-safe `Button` rows that open a native `AlertDialog` single-choice picker only on deliberate tap.

---

## 5. Developer Cache-Clearing Reference

- **KDE Plasma 6**:
  ```bash
  rm -rf ~/.cache/plasmashell/qmlcache/*
  ./install.sh
  systemctl --user restart plasma-plasmashell.service
  ```
- **Android (OnePlus 13 / ADB)**:
  ```bash
  ./scripts/install_android_usb.sh
  ```
