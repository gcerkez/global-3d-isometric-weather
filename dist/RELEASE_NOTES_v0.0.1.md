## Global 3D Isometric Weather Widget — `v0.0.1`

Release of **Global 3D Isometric Weather Widget** (`com.antigravity.yorktownweather`), a real-time 3D isometric generative weather diorama widget for **KDE Plasma 6 (Bazzite / Linux Desktop)**, **Android Homescreen & Lockscreen (OnePlus 13 / OxygenOS 15 & Android 8.0+)**, and **Web / PWA**.

---

### Highlights in `v0.0.1`

* **Living Miniature Diorama Sprites (5 Proportional Active Sprites)**:
  * **Horse & Carriage on Historic Main Street** (or miniature town car in modern cities) trotting between the Custom House, Nelson House, and Swan Tavern.
  * **Miniature Car on the George P. Coleman Memorial Bridge** crossing the double-swing steel truss bridge over the York River with glowing headlights at night.
  * **2 Miniature Pedestrians** strolling along the Yorktown Riverwalk shoreline path and the Victory Monument / Siege Redoubts bluff path.
  * **18th-Century Tall Ship Sailor Sprite** moving along the wooden deck of the three-masted schooner on the York River.
* **Interactive Isometric Zoom & Rotation (Desktop & Android)**:
  * **KDE Plasma 6 & Web**: Scroll wheel or pinch to zoom (`0.70x`–`2.25x`), drag to rotate/tilt in **both** Hybrid and Procedural 3D modes, double-click to reset, or use the bottom-left camera toolbar (`➖` `➕` `↺` `↻` `⟲`).
  * **Android Homescreen Widget & App**: Tap `➖` `➕` `↺` `↻` `⟲` `⚙️` directly on the bottom-left corner of the Homescreen widget (or in the config app) to zoom and rotate your placed widget.
* **Android 4×3 Resizable Widget with Live Multi-Frame Homescreen Animation**:
  * Defaults to a **4×3 grid size** with full horizontal & vertical resizability (`resizeMode="horizontal|vertical"`, `onAppWidgetOptionsChanged`).
  * Uses an auto-animating 4-frame `<ViewFlipper>` (`widget_isometric_weather.xml`) so falling rain/snow, York River water sparkles, and the 5 miniature sprites continuously animate live on your Homescreen.
* **Generative Location Engine (Works for Any City on Earth)**:
  * **Open-Meteo Geocoding Search**, **Wikipedia GeoSearch Landmark Discovery**, **OpenStreetMap Overpass 3D Building Extrusion**, and **AI Generative Diorama Mode** (`ai_generative` & `scripts/generate_location_diorama.py`).
* **Atmospheric, Astronomical & Environmental Telemetry**:
  * **Calm, Natural-Speed Weather Particles** (gentle rain/snow cadence with `25%`, `50%`, and `100%` intensity controls).
  * **US EPA Air Quality Index (AQI)**, **WHO UV Index**, **Astronomical Lunar Synodic Phase**, **12-Hour Temperature & Precipitation Sparkline**, **3D Volumetric Cloud Ground Shadows**, **Post-Rain Rainbow Arc**, and **High-Wind Gust Streaks**.

---

### Release Assets & Packaging

| Asset | Target Platform | Installation Method |
|---|---|---|
| `global-3d-isometric-weather-v0.0.1.apk` | **Android Homescreen & Lockscreen** (OnePlus 13 / Android 8.0+) | Download `.apk` directly on your phone and tap to install, or run `./scripts/install_android_usb.sh` over USB |
| `global-3d-isometric-weather-v0.0.1.plasmoid` | **KDE Plasma 6 Desktop** (Bazzite, Fedora, Arch, SteamOS, Kubuntu) | `kpackagetool6 --type Plasma/Applet --install global-3d-isometric-weather-v0.0.1.plasmoid` |
| `global-3d-isometric-weather-web-android-v0.0.1.zip` | **Web / PWA & Android Source Bundle** | Extract and serve `web/` or open `android/` in Android Studio |
| `SHA256SUMS.txt` | Checksum Verification | `sha256sum -c SHA256SUMS.txt` |
