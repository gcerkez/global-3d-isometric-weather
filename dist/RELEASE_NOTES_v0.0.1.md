## Global 3D Isometric Weather Widget — `v0.0.1`

Initial release of **Global 3D Isometric Weather Widget** (`com.antigravity.yorktownweather`), a real-time 3D isometric generative weather diorama widget for **KDE Plasma 6 (Bazzite / Linux Desktop)**, **Web / PWA**, and **Android Homescreen & Lockscreen (OnePlus 13 / Android 12+)**.

---

### Highlights in `v0.0.1`

* **Generative Location Engine (Works for Any City on Earth)**:
  * **Open-Meteo Geocoding Search**: Search and select any city or town worldwide.
  * **Wikipedia GeoSearch Landmark Discovery**: Automatically discovers real local landmarks around your coordinates and populates interactive hover pins (`LocationGenerator.js`).
  * **OpenStreetMap Overpass 3D Building Extrusion**: Queries real building footprints and heights around the selected city in **Interactive Procedural 3D Mode**.
  * **AI Generative Diorama Mode (`ai_generative` & `scripts/generate_location_diorama.py`)**: Synthesizes custom 3D isometric city dioramas for any city on demand.
* **Built-In High-Detail Historic Yorktown, VA Diorama**:
  * Features the **George P. Coleman Memorial Bridge** (double-swing steel truss bridge), **18th-Century Three-Masted Tall Ship** on the York River, **Colonial Main Street** (Custom House, Nelson House, Swan Tavern), **Yorktown Victory Monument**, and **1781 Revolutionary War Siege Redoubts & Artillery**.
  * Transparent-background island cutouts (`day`, `sunset`, `night`) with soft drop shadows for both **Sky Card Mode** and **Transparent Desktop Cutout Mode**.
* **Atmospheric, Astronomical & Environmental Telemetry**:
  * **US EPA Air Quality Index (AQI)** & **WHO UV Index** color-coded badges (`AstronomyAndAqi.js`).
  * **Astronomical Lunar Synodic Phase** badge + dynamically lit crescent/full moon disc in the night sky (`WeatherEffectsOverlay.qml`).
  * **12-Hour Temperature & Precipitation Probability Sparkline** inside the expandable HUD drawer (`WeatherHudCard.qml`).
  * **3D Volumetric Cloud Ground Shadows**, **Post-Rain Rainbow Arc**, and **High-Wind Gust Streaks**.
* **Cross-Platform Portability**:
  * Native **KDE Plasma 6 `.plasmoid`** package (`global-3d-isometric-weather-v0.0.1.plasmoid`).
  * Portable **HTML5 Canvas / ES6 / PWA** & **Android Homescreen/Lockscreen (`home_screen|keyguard`) AppWidget** bundle (`global-3d-isometric-weather-web-android-v0.0.1.zip`).

---

### Release Assets & Packaging

| Asset | Target Platform | Installation Command |
|---|---|---|
| `global-3d-isometric-weather-v0.0.1.plasmoid` | **KDE Plasma 6 Desktop** (Bazzite, Fedora, Arch, SteamOS, Kubuntu) | `kpackagetool6 --type Plasma/Applet --install global-3d-isometric-weather-v0.0.1.plasmoid` |
| `global-3d-isometric-weather-web-android-v0.0.1.zip` | **Web / PWA & Android** (OnePlus 13 Homescreen & Lockscreen) | Extract and serve `web/` or build `android/` in Android Studio |
| `SHA256SUMS.txt` | Checksum Verification | `sha256sum -c SHA256SUMS.txt` |
