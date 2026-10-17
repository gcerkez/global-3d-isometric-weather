# REA (`Reverse Engineer Anything`) Analysis Report

This document records the reverse-engineering analysis conducted with **[`rea` (Reverse Engineer Anything)](https://github.com/morluto/rea)** (`rea-agents v6.5.0`) inside the **`orca-build` Distrobox container** on an immutable Bazzite Linux host.

---

## 1. Execution Environment & Methodology

Because the host OS is **Bazzite (immutable Fedora Atomic KDE)**, Node.js `v22.19.0` and `@morluto/rea-agents` (`v6.5.0`) were provisioned in user-space (`/home/gcerkez/.local/node22/bin/rea`) and executed inside the `orca-build` Distrobox container using `rea`'s 5-stage analysis workflow:

1. **Triage & Binary/Source Identification** (`rea analyze-javascript-application --json <repo_dir>`)
2. **Static Analysis & Surface Extraction** (ES6/TypeScript modules, Three.js / Canvas shaders, Overpass & Weather API endpoints)
3. **Behavioral / Dynamic Flow Analysis** (Coordinate projection math, particle physics, HUD sparkline rendering)
4. **Synthesis** (Separating **Seen** vs **Inferred** vs **Unknown** facts with cryptographic REA Evidence IDs)
5. **Feature Porting** into `com.antigravity.yorktownweather` (KDE Plasma 6 QML + Portable Web/Android ES6 Canvas Engine)

---

## 2. Analyzed GitHub Isometric Weather & City Repositories

| # | Repository | REA Evidence ID (`bundleId`) | Core Architecture Analyzed | Features Ported to `yorktown-3d-isometric-weather` |
|---|---|---|---|---|
| 1 | [`RamAnanth/skytile`](https://github.com/RamAnanth/skytile) | `ev_8aa42db2b2e13feac3fbc224d085f691f81db67fcad0e7fc5d5322d6da0a993f` | Isometric 3D Weather Tile (`Three.js` + React) | **Post-rain rainbow arc** over the island (`WeatherEffectsOverlay.qml`), **high-wind streak lines** (>12 mph), **astronomical moon phase disc**, and **cloud ground shadows**. |
| 2 | [`diwaspoudell/citycubes`](https://github.com/diwaspoudell/citycubes) | `ev_7b77041625e5efa5fcd20b824e884e3bf420fc6c4b4e0669a9f7a285371509a0` | Generative 3D Isometric City (`Overpass API` + `Open-Meteo`) | **Generative OpenStreetMap Overpass 3D building footprint query** (`LocationGenerator.fetchOsmBuildings`) & **Open-Meteo Air Quality (`us_aqi`) + UV Index** integration. |
| 3 | [`boona13/mykonos-island-voxels`](https://github.com/boona13/mykonos-island-voxels) | `ev_5bf00df2a804dc6ce36fab9ad8c927dd1876bfa7ad2278968cf41a98c6087282` | Isometric Coastal Island Diorama (`Three.js` + `SunCalc`) | **Solar elevation & Golden Hour window** (`AstronomyAndAqi.evaluateSolarCycle`), coastal wave crest sparkles, and bluff monument beacon lighting. |
| 4 | [`TheWispy/weatherWorld`](https://github.com/TheWispy/weatherWorld) | `ev_0201e07b93bbf0faec9931d72bb1fe2fadde1eff8d7c5901a5a7292329961bee` | Procedural 3D Weather World | Multi-tier terrain elevation slabs, dynamic roof snow accumulation, and storm lightning flash decay curves. |
| 5 | [`aeiouvcode/tiny-patiala`](https://github.com/aeiouvcode/tiny-patiala) | `ev_e4ab638c9093064593a5e9d4cd4823d4397177812c6f6927e39e327351c965b7` | Location-Themed Isometric Heritage City | **Regional architectural archetype system** (`LocationGenerator.inferRegionalArchetype`) & interactive historical landmark hover pins. |
| 6 | [`pnedyalkov91/advanced-weather-widget`](https://github.com/pnedyalkov91/advanced-weather-widget) | `ev_0b811fd40fcac6f3139892849db1fe27b60353ae5370afb74b5cb99ab1cebbf9` | Desktop Weather HUD Widget | **12-Hour Temperature & Precipitation Probability Sparkline Chart** (`WeatherHudCard.qml`) inside the expandable HUD drawer. |
| 7 | [`Samy879/minimalist-animated-weather`](https://github.com/Samy879/minimalist-animated-weather) | `ev_94739ca0ea684467e034f554fbab5a41cc708bef3a702dd7df681cce0afa0a03` | Minimalist Animated Weather | Smooth CSS/QML sky gradient transitions and layered foreground/background cloud parallax. |
| 8 | `DavidHDev/isometric-city` | `ev_a19f04c81e9203f7b120e88912c4091b771a5590d2e3310f8a991c7d82310f12` | HTML5 2D Canvas Isometric Painter's Algorithm | Back-to-front isometric depth key sorting (`depth = tier * 100 + ry + z0 * 0.15`) for zero-artifact building/terrain occlusion. |

---

## 3. REA Evidence Breakdown (Seen vs. Inferred vs. Unknown)

### A. Seen (Directly Verified in REA Static & AST Bundles)
- **Open-Meteo Multi-Endpoint Telemetry (`citycubes`)**: Combining `/v1/forecast` (`temperature_2m`, `apparent_temperature`, `hourly.temperature_2m`, `hourly.precipitation_probability`, `hourly.uv_index`) with `/v1/air-quality` (`current.us_aqi`, `current.pm2_5`) requires **zero API keys** and works out-of-the-box on Linux desktops and Android devices.
- **OpenStreetMap Overpass Building Extrusion (`citycubes`)**: Querying `way["building"](around:350, lat, lon); out body; >; out skel qt;` returns real building polygon bounding boxes and `building:levels` tags that map directly into isometric 3D boxes.
- **Atmospheric Micro-Interactions (`skytile`)**:
  - Cloud shadows projected onto the ground plane (`y + 82px` with `rgba(18, 30, 45, 0.14)`) dramatically increase perceived 3D depth on flat 2D/QML canvases.
  - Post-rain rainbow arcs and lunar synodic phase rendering (`29.530588853` day synodic cycle) provide immediate visual context without cluttering the text HUD.

### B. Inferred (Synthesized for KDE Plasma 6 & Cross-Platform Portability)
- **Generative Location Hybrid Architecture**: While `citycubes` renders untextured 3D boxes from OpenStreetMap and `tiny-patiala` hardcodes one city's landmarks, combining **Wikipedia GeoSearch API** (`list=geosearch`) + **OpenStreetMap Overpass 3D footprints** + **AI Isometric Diorama synthesis** (`scripts/generate_location_diorama.py` & Pollinations/Gemini endpoints) makes **any location on Earth generative** across all 3 render modes (`hybrid`, `procedural3d`, and `ai_generative`).

### C. Unknown / Platform Constraints Addressed
- **Android Lockscreen & Homescreen RemoteViews Constraint**: Android `AppWidgetProvider` (`RemoteViews`) does not allow direct `<canvas>` or QML execution inside the launcher host process. We solved this in `android/app/src/main/java/com/antigravity/yorktownweather/IsometricWeatherWidgetProvider.kt` by rendering the isometric diorama, sky gradient, and HUD card onto a hardware-backed offscreen `Bitmap` and updating `RemoteViews.setImageViewBitmap` on both `home_screen` and `keyguard` (lockscreen).
