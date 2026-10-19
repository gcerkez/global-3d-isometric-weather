# v0.0.3 — Global 3D Isometric Weather Widget (Android APK / KDE Plasma 6 / Web)

Release **v0.0.3** brings major stability fixes for Android background location tracking, a direct on-widget refresh button, and 6 newly integrated high-fidelity 3D isometric zoned dioramas.

---

### 🌟 Key Highlights & Bug Fixes

#### 1. Fixed: Locked-Screen Location Resetting to Washington, D.C.
* **Root Cause**: When the phone was locked or in Doze mode, Android suspended background GPS polling. Previously, a 6-second timeout caused `DeviceLocationManager` to fall back to carrier IP geolocation (`get.geojs.io`). On mobile cellular networks (T-Mobile/AT&T/Verizon in Virginia), regional carrier gateways originate from Northern Virginia / Washington D.C., causing the widget to silently overwrite the phone's true location (Yorktown, VA / Norfolk, VA) with Washington, D.C.
* **Resolution**:
  - Background widget syncs now strictly disable cellular IP geolocation (`allowIpFallback = false`).
  - Added `KeyguardManager.isDeviceLocked` detection: when locked, the widget reuses the best cached physical location without timing out or draining battery.
  - On location timeout, the widget preserves the user's saved location coordinates and never corrupts saved preferences.

#### 2. Added: Bottom-Right Transparent Reload/Refresh Button
* Added a semi-transparent glass button (`⟳`) in the bottom-right corner of the rendered view on the Android widget.
* Styled with translucent dark glass (`#881C2C3C`) with subtle border so it never covers or obstructs the 3D diorama.
* Tapping the button shows a feedback toast (*"Refreshing weather & location..."*), resets the location timeout, immediately polls fresh physical GPS, and updates all widget frames with live weather telemetry.

#### 3. Updated 3D Diorama for Norfolk & Chesapeake Bay (Zero Buildings Over Water)
* Added a dedicated waterfront diorama for **Norfolk, VA** & **Chesapeake Bay**:
  - Translucent turquoise Elizabeth River harbor channel strictly confined to the lower-left with sailing tugboats.
  - Concrete wharf bulkhead with historic naval battleship (**USS Wisconsin**) moored cleanly alongside.
  - Nauticus-style maritime museum pavilion and downtown office towers strictly anchored on solid elevated dry land.
  - Clean stratified bedrock cross-section foundation.
  - Zero buildings hovering or floating over water.

#### 4. New High-Fidelity 3D Isometric Regional Dioramas
* Integrated 5 additional zoned dioramas with Day, Sunset (golden hour glow), and Night (deep indigo with warm glowing windows) variants:
  1. **Virginia Beach / Coastal** (ocean block, causeway bridge anchored shore-to-shore, beachfront hotels, lighthouse)
  2. **Colorado Springs / Alpine** (stratified rock strata cutaway, alpine chalets, gorge stream & waterfall)
  3. **Las Vegas** (desert resort plaza, central fountain lagoon with shooting jets, luxury towers & palm trees)
  4. **Orlando** (fairytale castle spire, central turquoise fountain lagoon, roller coaster with concrete footings)
  5. **Atlanta** (Piedmont Park pond, elevated MARTA-style transit rail line on concrete pillars, glass skyscrapers)

#### 5. Atmospheric Weather FX Across All Platforms
* Procedural branching lightning bolts and screen illumination flashes during thunderstorms (WMO 95, 96, 99).
* Volumetric rolling fog banks across water channels for fog conditions (WMO 45, 48).
* Drifting cloud shadows across diorama terrain during overcast / partly cloudy days.
* Full-canvas dynamic precipitation (rain/snow) coverage with perspective splash ripples on water surfaces.

---

### 📦 Distributable Packages

| Package | File | Description |
|---|---|---|
| **Android APK** | [`global-3d-isometric-weather-v0.0.3.apk`](global-3d-isometric-weather-v0.0.3.apk) | Native Android Homescreen & Lockscreen Widget (`home_screen\|keyguard`) |
| **KDE Plasma 6 Widget** | [`global-3d-isometric-weather-v0.0.3.plasmoid`](global-3d-isometric-weather-v0.0.3.plasmoid) | Desktop plasmoid for KDE Plasma 6.0+ |
| **Web / PWA Standalone** | [`global-3d-isometric-weather-web-android-v0.0.3.zip`](global-3d-isometric-weather-web-android-v0.0.3.zip) | Portable standalone HTML5/ES6 web engine |
