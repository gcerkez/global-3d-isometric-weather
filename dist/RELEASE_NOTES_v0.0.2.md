## Global 3D Isometric Weather Widget — `v0.0.2`

Release `v0.0.2` of **Global 3D Isometric Weather Widget** (`com.antigravity.yorktownweather`), featuring a streamlined Android homescreen widget design, taller 3D diorama framing preview, interactive multi-target tap actions (alarm clock, weather forecast, and diorama settings), configurable background sync intervals, and a full automated JUnit test suite.

---

### Highlights in `v0.0.2`

* **Streamlined Homescreen HUD (Top-Only Banner)**:
  * Completely removed the cumbersome corner card overlay on Android in favor of the lightweight, modern top-only HUD banner (`widget_hud_top_bar`).
  * The 3D isometric diorama now floats unobstructed directly over the wallpaper or sky gradient with zero visual clutter on the lower half of the widget.

* **Tall Proportion 3D Diorama Preview (In-App)**:
  * Increased the height of the 3D diorama render view on Tab 0 from 215dp to 330dp, matching how the diorama displays on tall phone homescreens.
  * Hidden web-only HUD artifacts from the in-app preview so the user sees a pure, unobstructed view of their selected isometric diorama.

* **Minimalist Icon-Only Framing Controls**:
  * Cleaned up the framing action buttons (`➖`, `➕`, `◀`, `🎯`, `▶`) on the 3D Preview tab, removing redundant text labels while retaining intuitive touch targets and accessibility descriptions.

* **Interactive Multi-Target Tap Actions**:
  * **Digital Clock Tap**: Tapping the `TextClock` on the homescreen widget launches the native system Alarm / Clock app. Supports Google Deskclock, OnePlus / Oppo Deskclock, Samsung Clock, Xiaomi Alarm, and AOSP fallback.
  * **Weather Telemetry Tap**: Tapping temperature, condition, location, or humidity opens the official 7-day and hourly forecast URL in the browser (`forecast.weather.gov` for US or `weather.com` worldwide).
  * **3D Diorama Tap**: Tapping the 3D isometric island opens the app configuration activity.

* **Configurable Background Refresh & Battery Management**:
  * Users can select their preferred background weather sync interval: **15 Minutes** (live sync), **30 Minutes**, **1 Hour**, **2 Hours**, or **Manual Refresh Only** (conserves maximum battery).

* **Consolidated 5-Tab Configuration UI**:
  * Merged HUD display settings into a streamlined 5-tab interface:
    * **Tab 0 (`🖼️ 3D Preview`)**: Tall 330dp diorama canvas with icon-only framing buttons and quick zoom presets.
    * **Tab 1 (`🏛️ Diorama Scene`)**: GPS diorama auto-matcher and manual picker for all 18 world landmark dioramas.
    * **Tab 2 (`📍 Weather Location`)**: GPS auto-detection, travel follow toggle, and city search.
    * **Tab 3 (`☀️ Atmosphere`)**: Sun & sky cycle, weather simulation overrides, and particle density.
    * **Tab 4 (`⚙️ Settings`)**: Top HUD text scale (`100% / 115% / 130%`), transparent cutout mode, extended telemetry toggle, background sync interval, and units (°F/°C, MPH/km/h).

* **Comprehensive Automated Unit Tests**:
  * Added 11 automated unit tests running with Gradle and JUnit 4:
    * `DeviceLocationManagerTest.kt`: 6 tests verifying Haversine distance, Yorktown / Tabb auto-matching, Jamestown matching, global landmark bounds, and GPS coordinates for all 18 dioramas.
    * `WeatherTelemetryTest.kt`: 5 tests verifying WMO weather code mapping, headlines, day/night emojis, EPA AQI categories, and 16-point wind compass directions.

---

### Release Assets & Packaging

| Asset File | Size | Description |
| :--- | :--- | :--- |
| `global-3d-isometric-weather-v0.0.2.apk` | ~27 MB | Android Release APK signed and optimized for OnePlus 13 (OxygenOS 15) & Android 8.0+ devices |
| `global-3d-isometric-weather-v0.0.2.plasmoid` | ~37 MB | KDE Plasma 6 Desktop widget package (Bazzite / Steam Deck / Fedora Linux) |
| `global-3d-isometric-weather-web-android-v0.0.2.zip` | ~63 MB | Complete web & Android source archive with all 18 high-res diorama assets and tests |
| `SHA256SUMS.txt` | — | Cryptographic SHA-256 checksums for verifying asset integrity |

---

### Verification & Testing
* **Device**: OnePlus 13 (`PJZ110`, Android 15 / OxygenOS 15) via ADB.
* **Unit Tests**: 11/11 tests passed in `:app:testDebugUnitTest` and `:app:testReleaseUnitTest`.
* **Lint & Build**: Clean release build via Gradle (`assembleRelease`).
