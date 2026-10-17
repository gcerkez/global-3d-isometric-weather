## Global 3D Isometric Weather Widget — `v0.0.1`

Release of **Global 3D Isometric Weather Widget** (`com.antigravity.yorktownweather`), a real-time 3D isometric generative weather diorama widget for **KDE Plasma 6 (Bazzite / Linux Desktop)**, **Android Homescreen & Lockscreen (OnePlus 13 / OxygenOS 15 & Android 8.0+)**, and **Web / PWA**.

---

### Highlights in `v0.0.1`

* **Built-In Pre-Generated Generic Dioramas + In-Menu Custom High-Res City Cutout Generator**:
  * Includes **4 Pre-Generated High-Resolution 3D Isometric Diorama Sets** (`1376×768` RGBA with Day, Sunset & Night variants):
    1. `yorktown` — Historic Yorktown, VA (1781 Colonial Waterfront & Coleman Bridge)
    2. `coastal` — Pre-Generated Generic: Coastal Harbor & Lighthouse Town (Non-City-Specific)
    3. `modern` — Pre-Generated Generic: Modern Downtown Metropolis & Suspension Bridge (Non-City-Specific)
    4. `alpine` — Pre-Generated Generic: Alpine Mountain & Lake Village (Non-City-Specific)
  * **Built-In Config Menu "Generate Custom High-Res Diorama Cutout for Any City"**: Directly inside the KDE and Android Configuration Menus, enter any city name and click **Generate High-Res Cutout** to discover local Wikipedia landmarks, synthesize a 3D isometric island, remove the background, and generate Day/Sunset/Night cutouts on-device.
* **Isometric Zoom & Horizontal Centering (Rotation Removed, HUD Stays Stationary)**:
  * Move the entire 3D isometric diorama left (`◀`), center (`🎯`), or right (`▶`) and zoom (`➖` / `➕`) on both Desktop and Android while leaving the Weather HUD Overlay fixed in its configured position.
* **Static Scenic Miniature Sprites**:
  * Features static miniature sprites (stationed Colonial Horse & Carriage on Historic Main Street, miniature cars on the Coleman Bridge, and harbor sailboats) with animated walking people removed.
* **Extrapolated, High-Readability Android Homescreen HUD & Scroll-Safe Config Menu**:
  * Native high-DPI `RemoteViews` **Top-Sky Split Banner** and **Corner HUD Card** with `100% / 115% / 130%` text scaling that sits in the open upper sky without covering the 3D isometric town.
  * Full-screen scroll-safe Android configuration menu (`MainActivity.kt`) using modal `AlertDialog` pickers instead of hair-trigger `Spinner`s so scrolling never triggers accidental taps.
* **Atmospheric, Astronomical & Environmental Telemetry**:
  * Calm, natural-speed rain/snow physics (`25%`, `50%`, `100%`), US EPA AQI, WHO UV Index, Lunar Synodic Phase, 12-Hour Temperature & Precipitation Sparkline, 3D Volumetric Cloud Ground Shadows, and Post-Rain Rainbow arcs.

---

### Release Assets & Packaging

| Asset | Target Platform | Installation Method |
|---|---|---|
| `global-3d-isometric-weather-v0.0.1.apk` | **Android Homescreen & Lockscreen** (OnePlus 13 / Android 8.0+) | Download `.apk` directly on your phone and tap to install, or run `./scripts/install_android_usb.sh` over USB |
| `global-3d-isometric-weather-v0.0.1.plasmoid` | **KDE Plasma 6 Desktop** (Bazzite, Fedora, Arch, SteamOS, Kubuntu) | `kpackagetool6 --type Plasma/Applet --install global-3d-isometric-weather-v0.0.1.plasmoid` |
| `global-3d-isometric-weather-web-android-v0.0.1.zip` | **Web / PWA & Android Source Bundle** | Extract and serve `web/` or open `android/` in Android Studio |
| `SHA256SUMS.txt` | Checksum Verification | `sha256sum -c SHA256SUMS.txt` |
