## Global 3D Isometric Weather Widget — `v0.0.1`

Release of **Global 3D Isometric Weather Widget** (`com.antigravity.yorktownweather`), a real-time 3D isometric weather diorama widget for **KDE Plasma 6 (Bazzite / Linux Desktop)**, **Android Homescreen & Lockscreen (OnePlus 13 / OxygenOS 15 & Android 8.0+)**, and **Web / PWA**.

---

### Highlights in `v0.0.1`

* **18 Pre-Generated Regional, Timezone, NPS & UNESCO 3D Isometric Diorama Sets (54 Cutout PNGs)**:
  * Every pre-generated diorama features **Jamestown-style translucent turquoise cutaway water** and includes **Daylight**, **Golden Hour / Sunset**, and **Starry Night** transparent PNG variants selectable from a clean dropdown menu:
    1. `yorktown` — **Historic Yorktown, VA** (NPS Colonial Historical Park — upgraded to the rounded geological cliff & translucent turquoise York River base with Victory Monument, 1781 Siege Redoubts, Colonial Brick Main Street, Coleman Bridge & Tall Ship)
    2. `jamestown` — **Historic Jamestown, VA** (NPS 1607 Settlement — Triangular James Fort Palisade, 3 Ships & Turquoise River)
    3. `washington_dc` — **Washington, D.C. & Mid-Atlantic** (`America/New_York` — U.S. Capitol, Washington Monument, Lincoln Memorial & Tidal Basin)
    4. `new_york` — **New York City & Northeast** (`America/New_York` UNESCO / NPS — Statue of Liberty, Brooklyn Bridge & Empire State)
    5. `san_francisco` — **San Francisco & California Coast** (`America/Los_Angeles` NPS — Golden Gate Bridge, Alcatraz & Painted Ladies)
    6. `chicago_great_lakes` — **Chicago & Great Lakes Midwest** (`America/Chicago` — Willis Tower, Red Bascule Bridge, Navy Pier & Cloud Gate Bean)
    7. `grand_canyon` — **Grand Canyon & American Southwest** (`America/Phoenix` & `America/Denver` UNESCO / NPS — Red-Rock Buttes, Cliff Dwellings & Colorado River)
    8. `new_orleans` — **New Orleans, Gulf Coast & Mississippi** (`America/Chicago` NPS — St. Louis Cathedral, French Quarter, Steamboat & Bayou)
    9. `seattle_pnw` — **Seattle & Pacific Northwest** (`America/Los_Angeles` & `America/Anchorage` — Space Needle, Pike Place, Mt. Rainier & Ferry)
    10. `miami_caribbean` — **Miami Beach, Florida Keys & Tropics** (`America/New_York` & `Pacific/Honolulu` — Pastel Art Deco Ocean Drive, Overseas Bridge & Coral Lagoon)
    11. `coastal` — **New England & Maritime Atlantic** (`America/New_York` & `America/Halifax` — Striped Coastal Lighthouse, Schooner & Wharf)
    12. `london_uk` — **London & British Isles** (`Europe/London` UNESCO — Big Ben, Parliament, Tower of London, Westminster Abbey & Thames Bridge)
    13. `paris_europe` — **Paris & Western Europe** (`Europe/Paris` & `Europe/Berlin` UNESCO — Notre-Dame Cathedral, Louvre Palace, Arc de Triomphe & Seine Bridge)
    14. `rome_mediterranean` — **Rome, Athens & Mediterranean** (`Europe/Rome` & `Europe/Athens` UNESCO — Colosseum, Parthenon, Roman Aqueduct & Harbor Lighthouse)
    15. `cairo_giza` — **Cairo, Giza & Middle East** (`Africa/Cairo` & `Asia/Dubai` UNESCO — Ancient Sandstone Citadel, Sacred Temple Pool & Pharos of Alexandria)
    16. `tokyo_east_asia` — **Tokyo, Kyoto, Beijing & East Asia** (`Asia/Tokyo` & `Asia/Shanghai` UNESCO — 5-Story Pagoda, Imperial Palace Hall & Torii Bridge)
    17. `modern` — **Global Modern Metropolis** (`Australia/Sydney`, `Asia/Singapore`, `America/Toronto` — Contemporary Skyline & Harbour Bridge)
    18. `alpine` — **Alpine Mountain & Glacial Lake Village** (`Europe/Zurich`, Rockies & Andes — Snow-Capped Peak, Chalets & Glacial Lake)
* **Decoupled Weather Telemetry Location & 3D Diorama Visual**:
  * Your weather coordinates (`lat, lon`) are completely independent of the active 3D isometric diorama—for example, you can monitor live weather in **Tabb, VA** while displaying the **Jamestown**, **Yorktown**, **Grand Canyon**, or **Tokyo** 3D render.
* **Isometric Zoom & Horizontal Centering (Rotation Removed, HUD Stays Stationary)**:
  * Move the entire 3D isometric diorama left (`◀`), center (`🎯`), or right (`▶`) and zoom (`➖` / `➕`) on both Desktop and Android while leaving the Weather HUD Overlay fixed in its configured position.
* **Extrapolated, High-Readability Android Homescreen HUD & Scroll-Safe Config Menu**:
  * Native high-DPI `RemoteViews` **Top-Sky Split Banner** and **Corner HUD Card** with `100% / 115% / 130%` text scaling that sits in the open upper sky without covering the 3D isometric town.
  * Full-screen scroll-safe Android configuration menu (`MainActivity.kt`) using modal `AlertDialog` pickers instead of hair-trigger `Spinner`s so scrolling never triggers accidental taps.

---

### Release Assets & Packaging

| Asset | Target Platform | Installation Method |
|---|---|---|
| `global-3d-isometric-weather-v0.0.1.apk` | **Android Homescreen & Lockscreen** (OnePlus 13 / Android 8.0+) | Download `.apk` directly on your phone and tap to install, or run `./scripts/install_android_usb.sh` over USB |
| `global-3d-isometric-weather-v0.0.1.plasmoid` | **KDE Plasma 6 Desktop** (Bazzite, Fedora, Arch, SteamOS, Kubuntu) | `kpackagetool6 --type Plasma/Applet --install global-3d-isometric-weather-v0.0.1.plasmoid` |
| `global-3d-isometric-weather-web-android-v0.0.1.zip` | **Web / PWA & Android Source Bundle** | Extract and serve `web/` or open `android/` in Android Studio |
| `SHA256SUMS.txt` | Checksum Verification | `sha256sum -c SHA256SUMS.txt` |
