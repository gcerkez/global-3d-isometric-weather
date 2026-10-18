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

* **Weather Particle Rendering Fix (No False Rain in Clear Weather)**:
  * Resolved an issue where oscillating water reflection lines were drawn continuously on the water area, creating the visual impression of light rain streaks falling left-to-right near the bottom in clear/sunny weather.
  * Weather particles (rain, thunderstorm streaks, snow flurries) are now strictly guarded by active precipitation telemetry (`rain`, `thunderstorm`, `snow`), leaving clear weather crisp and undisturbed.

* **10 New Regional & Middle America 3D Isometric Dioramas (Total 28 Diorama Models)**:
  * Integrated architectural landmark tiles into 10 new regional diorama island scenes (Day, Sunset, and Night sets):
    * **Virginia Beach, VA (`virginia_beach`)**: Oceanfront boardwalk, Cape Henry lighthouse, and ocean fishing pier.
    * **Chesapeake Bay, VA (`chesapeake_bay`)**: Chesapeake Bay Bridge-Tunnel, working crab/oyster docks, and harbor light.
    * **Myrtle Beach, SC (`myrtle_beach`)**: Grand Strand, SkyWheel Ferris wheel, and oceanfront resorts.
    * **Orlando Theme Parks, FL (`orlando`)**: Fairytale castle spires, roller coaster, carousel, and Lake Eola swan lagoon.
    * **Atlanta, GA (`atlanta`)**: Peachtree Center skyscraper, Midtown corporate tower, Georgia Capitol dome, and Olympic Park.
    * **Denver, CO (`denver`)**: Mile High skyline, historic Union Station, and Rocky Mountain foothills.
    * **Colorado Springs, CO (`colorado_springs`)**: Garden of the Gods sandstone spires, Pikes Peak mountain depot, and Broadmoor alpine manor.
    * **Texas Capitol & Hill Country (`texas`)**: Pink granite State Capitol dome, San Antonio River Walk bridge, and oil derrick.
    * **Arizona & Sonoran Desert (`arizona`)**: Sedona Cathedral Rock, adobe mission, and desert saguaro oasis pool.
    * **Las Vegas Strip & Mojave Oasis, NV (`las_vegas`)**: Strip mega-resorts, High Roller wheel, Caesars Colosseum, and Bellagio fountains.

* **Consolidated 5-Tab Configuration UI**:
  * Merged HUD display settings into a streamlined 5-tab interface:
    * **Tab 0 (`🖼️ 3D Preview`)**: Tall 330dp diorama canvas with icon-only framing buttons and quick zoom presets.
    * **Tab 1 (`🏛️ Diorama Scene`)**: GPS diorama auto-matcher and manual picker for all 28 world landmark dioramas.
    * **Tab 2 (`📍 Weather Location`)**: GPS auto-detection, travel follow toggle, and city search.
    * **Tab 3 (`☀️ Atmosphere`)**: Sun & sky cycle, weather simulation overrides, and particle density.
    * **Tab 4 (`⚙️ Settings`)**: Top HUD text scale (`100% / 115% / 130%`), transparent cutout mode, extended telemetry toggle, background sync interval, and units (°F/°C, MPH/km/h).

* **Comprehensive Automated Mock Device UI & Unit Tests (19 Tests)**:
  * Integrated **Robolectric** and **AndroidX Test** to run full Android SDK mock device UI simulation tests on the JVM without requiring a physical phone connected:
    * `MainActivityVisualMockDeviceTest.kt`: 5 mock device UI tests verifying 5-tab navigation, camera framing controls (zoom in/out, pan left/right, reset), location presets (Yorktown, Tabb, Jamestown), background sync interval selection, and preference persistence.
    * `WidgetRemoteViewsMockDeviceTest.kt`: 3 mock device tests verifying widget layout inflation, total absence of legacy corner cards, alarm clock intent creation, and dynamic procedural weather canvas rendering.
    * `DeviceLocationManagerTest.kt`: Tests verifying Haversine distance, Yorktown / Tabb auto-matching, Jamestown matching, global landmark bounds, and GPS coordinates for all 28 dioramas.
    * `WeatherTelemetryTest.kt`: Tests verifying WMO weather code mapping, headlines, day/night emojis, EPA AQI categories, and 16-point wind compass directions.
  * Added `scripts/run_android_emulator.sh` to easily launch an Android Virtual Device (AVD) emulator, install the app, and run visual usage tests directly or via IntelliJ IDEA / Android Studio.

---

### Release Assets & Packaging

| Asset File | Size | Description |
| :--- | :--- | :--- |
| `global-3d-isometric-weather-v0.0.2.apk` | ~76 MB | Android Release APK signed and optimized for Android 8.0+ / Android 15 (OxygenOS 15) with all 28 high-res 3D dioramas |
| `global-3d-isometric-weather-v0.0.2.plasmoid` | ~75 MB | KDE Plasma 6 Desktop widget package (Bazzite / Steam Deck / Fedora Linux) |
| `global-3d-isometric-weather-web-android-v0.0.2.zip` | ~147 MB | Complete web & Android source archive with all 28 high-res diorama assets, tests, and mock device configs |
| `SHA256SUMS.txt` | — | Cryptographic SHA-256 checksums for verifying asset integrity |

---

### Verification & Testing
* **Physical Device**: Verified on OnePlus 13 (`PJZ110`, Android 15 / OxygenOS 15) via ADB.
* **Android SDK Mock Device UI**: 19/19 tests passed in `:app:testDebugUnitTest` and `:app:testReleaseUnitTest`.
* **Lint & Build**: Clean release build via Gradle (`assembleRelease`).
