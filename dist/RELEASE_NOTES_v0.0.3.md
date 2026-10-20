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

#### 5. Immediate GPS Location Permission Prompt & Safe Defaults
* **Immediate Permission Request**: On app startup (`MainActivity.onCreate()`), if location permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) have not yet been granted, the app immediately presents the system runtime permission dialog rather than waiting for the user to manually toggle options.
* **Safe Checkbox Default**: "Follow Phone GPS Location" is unchecked by default if permission has not been granted. Denying the permission keeps the checkbox unchecked and cleanly defaults to the user's saved city without crashing or showing corrupted states.
* **Seamless Granting Flow**: When permission is granted, the app automatically enables follow GPS, checks the box, and resolves true device GPS location.

#### 6. 3D Volumetric Puffy Clouds in Android Render View
* **Volumetric Puffy Clouds**: Ported multi-layered 3D shaded cloud rendering (ambient underside shadow, mid-tone volume body, and sunlit highlight domes) directly to the Android widget canvas and web engine.
* **Guaranteed Visibility in Sky View**: Positioned in the visible sky zone between the top HUD banner and the upper island peaks (`areaTop + 10..46dp`), ensuring clouds are prominently visible across all zoom levels—especially at the standard 100% farthest-out view—without requiring vertical panning.
* **Synchronized Ground Shadows**: Soft elliptical shadows drift across island terrain and water directly tracking beneath clouds floating overhead.
* **Palette Responsive**: Clouds dynamically adapt color palettes for daytime, sunset/golden-hour, starry night, and stormy rain/thunderstorm conditions.

#### 7. Comprehensive Mock Device UI & Robolectric Test Suite
* Added Robolectric mock device test suite verifying:
  - Immediate location permission request on startup and default unchecked state.
  - Permission grant / denial handling and preference synchronization.
  - 3D volumetric puffy cloud rendering across cloudy, overcast, rain, thunderstorm, and snowfall conditions across zoom levels (0.85x, 1.0x, 1.25x, 1.60x).
  - Direct canvas multi-layer puffy cloud rendering without crashes or memory leaks.

#### 8. Fixed: Immediate Crash on Subsequent App Launches
* **Root Cause**: On the initial app launch, location permissions were not yet granted, so `requestPermissions()` was called asynchronously and the view hierarchy finished inflating before the user granted permission. Once permission was granted, `useCurrentLocation = true` was stored in preferences. On subsequent launches, `hasLocationPermission` was immediately `true`, triggering `detectAndApplyCurrentLocation()` at the top of `onCreate()` before `statusLabel` or `activeLocationHeader` were instantiated. This caused an immediate `UninitializedPropertyAccessException` and instant crash on every launch after the first.
* **Resolution**:
  - Moved the startup location & permission check to the very end of `onCreate()`, ensuring all view hierarchies and tab pages are fully created and mounted first.
  - Added `setStatus(...)` and `updateHeader(...)` helper guards ensuring `::statusLabel.isInitialized` and `::activeLocationHeader.isInitialized` are strictly validated before any view modifications.
  - Added `onDestroy()` lifecycle handler canceling active coroutine scopes.
  - Added comprehensive Robolectric mock device tests verifying subsequent launches with permission granted, activity lifecycle recreations (`scenario.recreate()`), all 5 tab states, and multiple saved regional diorama locations.

#### 9. Upgraded Denver & Colorado 3D Isometric Dioramas
* **Denver, CO (`denver_island*.png`)**:
  - Replaced the Seattle mashup with a dedicated 3D isometric miniature city diorama of Denver, CO (The Mile High City).
  - Features the Colorado State Capitol with its 24-karat gold-gilded dome, historic red-brick Denver Union Station with train tracks, modern downtown skyline (including the Wells Fargo Center Cash Register tower), Confluence Park with the turquoise South Platte River and footbridges, and snow-capped Rocky Mountain peaks rising in the background.
* **Colorado Springs & Pikes Peak, CO (`colorado_springs_island*.png`)**:
  - Replaced the corrupted, artifact-speckled render with a clean 3D isometric diorama.
  - Features the towering summit of Pikes Peak with the red Pikes Peak Cog Railway train ascending toward a mountain tunnel, vertical red-sandstone rock spires of Garden of the Gods, the historic Italianate Broadmoor resort hotel by an alpine lake, cascading waterfalls through a rocky gorge, and evergreen pine forests on a rich red-sandstone and granite strata slab.
* **Triple Lighting Variants**: Clean transparent-background PNG cutouts with soft contact drop shadows and built all 3 lighting variants (**Daylight**, **Golden Hour Sunset**, and **Starry Night with glowing windows**) across Android assets, Web, and KDE Plasma 6.

#### 10. Vertical Camera View Movement (Pan Up / Pan Down)
* **Android Config App**: Added `▲` (**Pan Up**) and `▼` (**Pan Down**) buttons in Tab 0 framing controls alongside `◀` (Left), `▶` (Right), and `🎯` (Center Island / Reset). Live readout displays both horizontal and vertical offsets: `Zoom: 100% • Offset: X +0 px, Y +0 px`.
* **Android Widget Provider**: Added `ACTION_PAN_UP` and `ACTION_PAN_DOWN` intents, persisting `widgetOffsetY` in preferences. The diorama bitmap shifts vertically along `widgetOffsetY` while the weather HUD overlay remains stationary.
* **Web & Android WebView**: Implemented `cameraPanY` with `adjustCamera("pan_up")` and `adjustCamera("pan_down")`. Two-axis mouse and touch pointer dragging lets users pan the island freely in any direction.
* **KDE Plasma 6 Widget**: Added `dioramaOffsetY` in `main.xml`, a Vertical Position slider and `▲`/`▼` buttons in `ConfigAppearance.qml`, and interactive two-axis dragging in `YorktownHybridScene.qml` and `YorktownProcedural3D.qml`.

#### 11. Flexible Widget Grid Resizing (4x2 to 4x4 and 5x2 to 5x4)
* **Launcher Grid Metadata**: Updated `isometric_weather_widget_info.xml` with `minResizeHeight="95dp"`, `minHeight="110dp"`, `maxResizeWidth="800dp"`, and `maxResizeHeight="700dp"`. This enables home screen launchers across all DPI ratios to resize the widget down to **2 rows** (`4x2` and `5x2`), while expanding up to 4+ rows (`4x4` and `5x4`) and 5+ columns on high-DPI screens, foldables, and tablets.
* **Adaptive Compact Height Layout**: In `IsometricWeatherWidgetProvider.kt`, widgets smaller than 165dp height dynamically collapse secondary telemetry lines (wind/humidity and AQI/UV badges) into a compact top bar so the 3D diorama island always has full vertical space to render cleanly.

#### 12. Reset Button Depress Animation & Tactile Haptic Feedback
* **Android App UI**: Enhanced `makeStyledButton` with a `StateListDrawable` (dark navy to highlighted `#2a4e70` navy fill with a glowing `#ffd166` gold border) and an active touch depress animation (`scale(0.88)` on press with `HapticFeedbackConstants.KEYBOARD_TAP`, smoothly rebounding to `1.0` on release).
* **Android Widget RemoteViews**: Created `widget_btn_selector.xml` with pressed-state highlighting and stroke animation for `btn_cam_reset` (`🎯`) and `btn_widget_refresh` (`⟳`).
* **Web UI**: Added CSS `:active` animation (`transform: scale(0.86)` with background color highlight) to `#camCenter` and all control buttons.

---

### 📦 Distributable Packages

| Package | File | Description |
|---|---|---|
| **Android APK** | [`global-3d-isometric-weather-v0.0.3.apk`](global-3d-isometric-weather-v0.0.3.apk) | Native Android Homescreen & Lockscreen Widget (`home_screen\|keyguard`) |
| **KDE Plasma 6 Widget** | [`global-3d-isometric-weather-v0.0.3.plasmoid`](global-3d-isometric-weather-v0.0.3.plasmoid) | Desktop plasmoid for KDE Plasma 6.0+ |
| **Web / PWA Standalone** | [`global-3d-isometric-weather-web-android-v0.0.3.zip`](global-3d-isometric-weather-web-android-v0.0.3.zip) | Portable standalone HTML5/ES6 web engine |
