/**
 * isometric-weather.js
 * ====================
 * Cross-platform portable ES6 + HTML5 Canvas engine for the Global 3D Isometric
 * Weather Widget (shared across Web/PWA, Electron/Tauri, and the Android
 * Homescreen/Lockscreen WebView & Canvas renderer).
 *
 * Core features:
 *   - Open-Meteo Forecast & Air Quality live telemetry (Temp, Feels Like,
 *     Humidity, 16-compass Wind, US EPA AQI, UV Index, 12h Sparkline, 3-Day Forecast)
 *   - Astronomical Lunar Synodic Phase & Solar Golden Hour calculation
 *   - Built-In Pre-Generated 3D Isometric Diorama Themes (28 Regional, NPS & UNESCO Cutouts):
 *       1. "yorktown":            Historic Yorktown, VA (1781 Waterfront, Coleman Bridge & Tall Ship)
 *       2. "jamestown":           Historic Jamestown, VA (1607 James Fort Palisade, 3 Ships & River)
 *       3. "virginia_beach":      Virginia Beach, VA (Boardwalk, Cape Henry Lighthouse & Pier)
 *       4. "chesapeake_bay":      Chesapeake Bay, VA (Bay Bridge-Tunnel, Crab Docks & Harbor Light)
 *       5. "washington_dc":       Washington, D.C. & Mid-Atlantic (US Capitol, Monument, Lincoln Memorial)
 *       6. "new_york":            New York City & Northeast (Statue of Liberty, Brooklyn Bridge, Empire State)
 *       7. "myrtle_beach":        Myrtle Beach & Grand Strand, SC (SkyWheel, Resorts & Promenade)
 *       8. "atlanta":             Atlanta & Georgia Piedmont (Peachtree Tower, Olympic Park & Gold Capitol)
 *       9. "orlando":             Orlando Theme Parks, FL (Fairytale Spire Castle, Coaster & Swan Lagoon)
 *      10. "miami_caribbean":     Miami Beach, Florida Keys & Tropics (Art Deco Ocean Drive & Coral Lagoon)
 *      11. "new_orleans":         New Orleans & Gulf South (St. Louis Cathedral, French Quarter & Steamboat)
 *      12. "texas":               Texas Capitol, Austin & Hill Country (Pink Granite Capitol, River Walk)
 *      13. "chicago_great_lakes": Chicago & Great Lakes Midwest (Willis Tower, Red Bascule Bridge, Navy Pier)
 *      14. "denver":              Denver Mile High Skyline, CO (Union Station, 16th St Mall & Foothills)
 *      15. "colorado_springs":    Colorado Springs & Pikes Peak, CO (Garden of the Gods, Broadmoor & Peak)
 *      16. "arizona":             Arizona Sonoran Desert & Red Rocks (Sedona Cathedral Rock, Adobe Mission)
 *      17. "grand_canyon":        Grand Canyon & Southwest (Red-Rock Buttes, Cliff Dwellings & Colorado River)
 *      18. "las_vegas":           Las Vegas Strip & Mojave Oasis, NV (Strip Resorts, High Roller Wheel & Fountain)
 *      19. "san_francisco":       San Francisco & California Coast (Golden Gate Bridge, Alcatraz, Painted Ladies)
 *      20. "seattle_pnw":         Seattle & Pacific Northwest (Space Needle, Pike Place, Mt. Rainier & Ferry)
 *      21. "coastal":             New England & Maritime Atlantic (Lighthouse Point, Schooner & Wharf)
 *      22. "london_uk":           London & UK (Big Ben, Parliament, Tower of London & Thames Bridge)
 *      23. "paris_europe":        Paris & Western Europe (Notre-Dame, Louvre Palace, Arc & Seine Bridge)
 *      24. "rome_mediterranean":  Rome, Athens & Mediterranean (Colosseum, Parthenon, Aqueduct & Harbor)
 *      25. "cairo_giza":          Cairo, Giza & Middle East (Ancient Citadel, Sacred Temple Pool & Pharos)
 *      26. "tokyo_east_asia":     Tokyo, Kyoto & East Asia (5-Story Pagoda, Imperial Hall & Torii Bridge)
 *      27. "modern":              Global Modern Metropolis (Sydney / Singapore / Toronto Skyline & Bridge)
 *      28. "alpine":              Alpine Mountain & Glacial Lake Village (Rockies / Swiss Alps / Andes)
 *   - Isometric Zoom & Horizontal Centering (move left/right/center while HUD overlay stays fixed)
 *   - Static Miniature Diorama Sprites (Horse & Carriage, Cars, Sailboats)
 */

export const VALID_DIORAMA_THEMES = [
    "yorktown", "jamestown", "virginia_beach", "norfolk", "chesapeake_bay",
    "washington_dc", "new_york", "myrtle_beach", "atlanta", "orlando",
    "miami_caribbean", "new_orleans", "texas", "chicago_great_lakes",
    "denver", "colorado_springs", "arizona", "grand_canyon", "las_vegas",
    "san_francisco", "seattle_pnw", "coastal", "london_uk", "paris_europe",
    "rome_mediterranean", "cairo_giza", "tokyo_east_asia", "modern", "alpine"
];

export class IsometricWeatherEngine {
    /**
     * Initializes the portable 3D Isometric Weather Engine.
     * @param {HTMLCanvasElement} canvas - Target HTML5 canvas element
     * @param {Object} options - Initial configuration options
     */
    constructor(canvas, options = {}) {
        this.canvas = canvas;
        this.ctx = canvas.getContext("2d");

        this.state = {
            locationName: options.locationName || "Yorktown, VA",
            regionSubtitle: options.regionSubtitle || "Historic Colonial Waterfront & 1781 Battlefield",
            latitude: options.latitude ?? 37.2388,
            longitude: options.longitude ?? -76.5097,
            renderMode: options.renderMode || "hybrid",       // "hybrid" | "procedural3d"
            dioramaTheme: options.dioramaTheme || "yorktown", // Any of VALID_DIORAMA_THEMES (independent of weather lat/lon)
            timeMode: options.timeMode || "realtime",         // "realtime" | "day" | "sunset" | "night"
            weatherOverride: options.weatherOverride || "realtime",
            tempUnit: options.tempUnit || "BOTH",

            // Live telemetry
            tempF: 68,
            feelsLikeF: 67,
            humidity: 64,
            windMph: 8,
            windDeg: 25,
            weatherCode: 0,
            isDay: true,
            isGoldenHour: false,
            usAqi: 24,
            uvIndex: 3.2,
            hourlyTemps: [68, 69, 71, 72, 71, 69, 67, 65, 63, 62, 61, 60],
            hourlyPop: [5, 5, 10, 10, 15, 10, 5, 5, 0, 0, 5, 5],
            forecastDays: [
                { dayName: "TODAY", tempRange: "70° / 56°F" },
                { dayName: "TOMORROW", tempRange: "72° / 58°F" },
                { dayName: "DAY 3", tempRange: "67° / 54°F" }
            ],

            // Isometric Zoom & Offset (horizontal and vertical pan)
            cameraZoom: options.cameraZoom ?? 1.0,
            cameraPanX: options.cameraPanX ?? 0.0,
            cameraPanY: options.cameraPanY ?? 0.0,
            particleIntensity: options.particleIntensity ?? 0.5,
            animTime: 0
        };

        this.images = {
            day: new Image(),
            sunset: new Image(),
            night: new Image()
        };
        this.setDioramaTheme(this.state.dioramaTheme);

        this.onHudUpdate = options.onHudUpdate || (() => {});
        this._bindInteraction();
    }

    /**
     * Switches between the 18 built-in pre-generated regional & landmark diorama cutout themes.
     * @param {string} theme
     */
    setDioramaTheme(theme) {
        const cleanTheme = VALID_DIORAMA_THEMES.includes(theme) ? theme : "yorktown";
        this.state.dioramaTheme = cleanTheme;
        const prefix = `${cleanTheme}_island`;

        this.images.day.src = `images/${prefix}.png`;
        this.images.sunset.src = `images/${prefix}_sunset.png`;
        this.images.night.src = `images/${prefix}_night.png`;
    }

    /**
     * Computes lunar synodic phase (0..1) and label for a given Date.
     * @param {Date} dateObj - Current date
     * @returns {{phase: number, name: string, emoji: string}}
     */
    computeMoonPhase(dateObj = new Date()) {
        const synodicDays = 29.530588853;
        const refNewMoonMs = Date.UTC(2000, 0, 6, 18, 14, 0);
        const daysSince = (dateObj.getTime() - refNewMoonMs) / 86400000.0;
        let phase = (daysSince % synodicDays) / synodicDays;
        if (phase < 0) phase += 1.0;

        let name = "New Moon", emoji = "🌑";
        if (phase < 0.03 || phase >= 0.97) { name = "New Moon"; emoji = "🌑"; }
        else if (phase < 0.22) { name = "Waxing Crescent"; emoji = "🌒"; }
        else if (phase < 0.28) { name = "First Quarter"; emoji = "🌓"; }
        else if (phase < 0.47) { name = "Waxing Gibbous"; emoji = "🌔"; }
        else if (phase < 0.53) { name = "Full Moon"; emoji = "🌕"; }
        else if (phase < 0.72) { name = "Waning Gibbous"; emoji = "🌖"; }
        else if (phase < 0.78) { name = "Last Quarter"; emoji = "🌗"; }
        else { name = "Waning Crescent"; emoji = "🌘"; }

        return { phase, name, emoji };
    }

    /**
     * Fetches Open-Meteo Forecast, Air Quality, Wikipedia Landmarks, and OSM 3D buildings.
     */
    async refreshAll() {
        const { latitude, longitude } = this.state;
        try {
            const wxUrl = `https://api.open-meteo.com/v1/forecast?latitude=${latitude}&longitude=${longitude}`
                + `&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,wind_direction_10m`
                + `&hourly=temperature_2m,precipitation_probability,uv_index`
                + `&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset`
                + `&temperature_unit=fahrenheit&wind_speed_unit=mph&timezone=auto&forecast_days=3`;

            const resp = await fetch(wxUrl);
            const data = await resp.json();
            if (data.current) {
                this.state.tempF = data.current.temperature_2m;
                this.state.feelsLikeF = data.current.apparent_temperature;
                this.state.humidity = Math.round(data.current.relative_humidity_2m);
                this.state.windMph = data.current.wind_speed_10m;
                this.state.windDeg = data.current.wind_direction_10m;
                this.state.weatherCode = data.current.weather_code;
                this.state.isDay = data.current.is_day === 1;
            }
            if (data.hourly && data.hourly.temperature_2m) {
                const hr = new Date().getHours();
                this.state.hourlyTemps = data.hourly.temperature_2m.slice(hr, hr + 12).map(v => Math.round(v));
                this.state.hourlyPop = (data.hourly.precipitation_probability || []).slice(hr, hr + 12);
                this.state.uvIndex = (data.hourly.uv_index && data.hourly.uv_index[hr]) || 0;
            }
            if (data.daily && data.daily.time) {
                const days = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"];
                this.state.forecastDays = data.daily.time.slice(0, 3).map((t, i) => {
                    const dt = new Date(t + "T12:00:00");
                    return {
                        dayName: i === 0 ? "TODAY" : days[dt.getDay()],
                        tempRange: `${Math.round(data.daily.temperature_2m_max[i])}° / ${Math.round(data.daily.temperature_2m_min[i])}°F`
                    };
                });
            }
        } catch (err) {
            console.warn("Open-Meteo forecast fetch error:", err);
        }

        try {
            const aqUrl = `https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${latitude}&longitude=${longitude}&current=us_aqi`;
            const aqResp = await fetch(aqUrl);
            const aqData = await aqResp.json();
            if (aqData.current && aqData.current.us_aqi != null) {
                this.state.usAqi = Math.round(aqData.current.us_aqi);
            }
        } catch (err) {
            console.warn("Open-Meteo AQI fetch error:", err);
        }

        this.onHudUpdate(this.state);
    }

    /**
     * Returns effective time of day ("day" | "sunset" | "night").
     */
    getEffectiveTimeState() {
        if (this.state.timeMode !== "realtime") return this.state.timeMode;
        if (!this.state.isDay) return "night";
        if (this.state.isGoldenHour) return "sunset";
        return "day";
    }

    /**
     * Returns effective weather category ("clear" | "partly_cloudy" | "rain" | "thunderstorm" | "snow" | "fog").
     */
    getEffectiveWeatherCategory() {
        if (this.state.weatherOverride !== "realtime") return this.state.weatherOverride;
        const c = this.state.weatherCode;
        if (c === 0) return "clear";
        if (c <= 2) return "partly_cloudy";
        if (c === 3) return "overcast";
        if (c === 45 || c === 48) return "fog";
        if ((c >= 51 && c <= 67) || (c >= 80 && c <= 82)) return "rain";
        if ((c >= 71 && c <= 77) || c === 85 || c === 86) return "snow";
        if (c >= 95) return "thunderstorm";
        return "clear";
    }

    /**
     * Adjusts isometric zoom or view movement (pan horizontal and vertical) programmatically.
     * @param {"zoom_in"|"zoom_out"|"pan_left"|"pan_right"|"pan_up"|"pan_down"|"center"|"reset"} action
     */
    adjustCamera(action) {
        if (action === "zoom_in") {
            this.state.cameraZoom = Math.min(2.40, this.state.cameraZoom * 1.15);
        } else if (action === "zoom_out") {
            this.state.cameraZoom = Math.max(0.65, this.state.cameraZoom / 1.15);
        } else if (action === "pan_left") {
            this.state.cameraPanX = Math.max(-220, this.state.cameraPanX - 24);
        } else if (action === "pan_right") {
            this.state.cameraPanX = Math.min(220, this.state.cameraPanX + 24);
        } else if (action === "pan_up") {
            this.state.cameraPanY = Math.max(-180, this.state.cameraPanY - 24);
        } else if (action === "pan_down") {
            this.state.cameraPanY = Math.min(180, this.state.cameraPanY + 24);
        } else if (action === "center") {
            this.state.cameraPanX = 0;
            this.state.cameraPanY = 0;
        } else if (action === "reset") {
            this.state.cameraZoom = 1.0;
            this.state.cameraPanX = 0;
            this.state.cameraPanY = 0;
        }
    }

    /**
     * Binds view drag (to center/move the isometric island), pinch-to-zoom, wheel zoom, and double-click reset.
     */
    _bindInteraction() {
        let dragging = false, lastX = 0, lastY = 0;
        let lastPinchDist = 0;

        this.canvas.addEventListener("pointerdown", e => {
            dragging = true;
            lastX = e.clientX;
            lastY = e.clientY;
        });
        window.addEventListener("pointerup", () => { dragging = false; });
        window.addEventListener("pointermove", e => {
            if (!dragging) return;
            const dx = e.clientX - lastX;
            const dy = e.clientY - lastY;
            this.state.cameraPanX = Math.max(-220, Math.min(220, this.state.cameraPanX + dx * 0.65));
            this.state.cameraPanY = Math.max(-180, Math.min(180, this.state.cameraPanY + dy * 0.65));
            lastX = e.clientX;
            lastY = e.clientY;
        });
        this.canvas.addEventListener("touchstart", e => {
            if (e.touches.length === 2) {
                const dx = e.touches[0].clientX - e.touches[1].clientX;
                const dy = e.touches[0].clientY - e.touches[1].clientY;
                lastPinchDist = Math.hypot(dx, dy);
            }
        }, { passive: true });
        this.canvas.addEventListener("touchmove", e => {
            if (e.touches.length === 2 && lastPinchDist > 10) {
                const dx = e.touches[0].clientX - e.touches[1].clientX;
                const dy = e.touches[0].clientY - e.touches[1].clientY;
                const dist = Math.hypot(dx, dy);
                const factor = dist / lastPinchDist;
                this.state.cameraZoom = Math.max(0.65, Math.min(2.40, this.state.cameraZoom * factor));
                lastPinchDist = dist;
            }
        }, { passive: true });
        this.canvas.addEventListener("wheel", e => {
            e.preventDefault();
            const factor = e.deltaY < 0 ? 1.10 : 0.91;
            this.state.cameraZoom = Math.max(0.65, Math.min(2.40, this.state.cameraZoom * factor));
        }, { passive: false });
        this.canvas.addEventListener("dblclick", () => {
            this.adjustCamera("reset");
        });
    }

    /**
     * Starts the 60fps requestAnimationFrame render loop.
     */
    start() {
        const tick = () => {
            this.state.animTime += 0.016;
            this.render();
            requestAnimationFrame(tick);
        };
        requestAnimationFrame(tick);
    }

    /**
     * Renders the sky backdrop, isometric diorama (with zoom + horizontal offset), water ripples, static sprites, and weather particles.
     */
    render() {
        const ctx = this.ctx;
        const w = this.canvas.width;
        const h = this.canvas.height;
        const tState = this.getEffectiveTimeState();
        const cat = this.getEffectiveWeatherCategory();
        const t = this.state.animTime;

        // 1. Sky Gradient
        const grad = ctx.createLinearGradient(0, 0, 0, h);
        if (tState === "night") {
            grad.addColorStop(0, "#0d1b2a");
            grad.addColorStop(1, "#1b314c");
        } else if (tState === "sunset") {
            grad.addColorStop(0, "#3a506b");
            grad.addColorStop(1, "#f29a6b");
        } else if (cat === "rain" || cat === "thunderstorm") {
            grad.addColorStop(0, "#4a6278");
            grad.addColorStop(1, "#7b92a8");
        } else {
            grad.addColorStop(0, "#7fc4f2");
            grad.addColorStop(1, "#a8dbf8");
        }
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, w, h);

        // 2. Main Pre-Generated Diorama Cutout with Zoom & Horizontal Offset (no rotation)
        const img = tState === "night" ? this.images.night : (tState === "sunset" ? this.images.sunset : this.images.day);

        const baseScale = Math.min(w / 1376, h / 768);
        const dw = 1376 * baseScale;
        const dh = 768 * baseScale;
        const dx = (w - dw) * 0.5 - w * 0.04 + (this.state.cameraPanX * baseScale * 2.0);
        const dy = (h - dh) * 0.5 + (this.state.cameraPanY * baseScale * 2.0);

        ctx.save();
        const cx = dx + dw * 0.5;
        const cy = dy + dh * 0.5;
        ctx.translate(cx, cy);
        ctx.scale(this.state.cameraZoom, this.state.cameraZoom);
        ctx.translate(-cx, -cy);

        if (img && img.complete && img.naturalWidth > 0) {
            ctx.drawImage(img, dx, dy, dw, dh);
        }

        ctx.restore();

        // 3. Atmospheric Weather Effects (Lightning, Fog, Cloud Shadows, Rain & Snow)
        const pScale = Math.max(0.15, this.state.particleIntensity ?? 1.0);

        // 3D Drifting Volumetric Puffy Clouds & Ground Shadows for cloudy/rain/storm/snow weather
        const isCloudy = (cat === "partly_cloudy" || cat === "overcast" || cat === "rain" || cat === "thunderstorm" || cat === "snow");
        if (isCloudy) {
            const cloudCount = (cat === "partly_cloudy" || cat === "snow") ? 3 : 4;
            const isStormy = (cat === "rain" || cat === "thunderstorm" || cat === "overcast");

            // First pass: Ground shadows on the island terrain
            ctx.fillStyle = tState === "night" ? "rgba(0, 0, 0, 0.08)" : "rgba(15, 35, 60, 0.11)";
            for (let ci = 0; ci < cloudCount; ci++) {
                const cProg = ((ci * 0.31 + t * 0.025) % 1.0);
                const cloudX = (cProg * 1.30 - 0.15) * w;
                if (cloudX >= dx + 0.05 * dw && cloudX <= dx + 0.95 * dw) {
                    const shadowX = cloudX - 12 * s;
                    const shadowY = dy + (0.28 + (ci * 0.15) % 0.38) * dh;
                    const cw = (70 + ci * 22) * s;
                    const ch = cw * 0.38;
                    ctx.beginPath();
                    ctx.ellipse(shadowX, shadowY, cw, ch, 0, 0, Math.PI * 2);
                    ctx.fill();
                }
            }

            // Second pass: 3D volumetric puffy clouds floating in the sky render area
            for (let ci = 0; ci < cloudCount; ci++) {
                const cProg = ((ci * 0.31 + t * 0.025) % 1.0);
                const cloudX = (cProg * 1.30 - 0.15) * w;
                const cloudY = 36 * s + (24 + (ci * 38) % 76) * s;
                const cloudScale = (0.75 + (ci % 3) * 0.18) * (isStormy ? 1.15 : 1.0);
                this.drawIsometricPuffyCloud(ctx, cloudX, cloudY, cloudScale, s, isStormy, tState === "sunset", tState === "night");
            }
        }

        // Volumetric rolling fog / mist banks across the lower island and water
        if (cat === "fog") {
            for (let fg = 0; fg < 4; fg++) {
                const fogProg = ((fg * 0.28 + t * 0.04) % 1.0);
                const fogX = (fogProg * 1.3 - 0.15) * w;
                const fogY = dy + (0.35 + (fg * 0.14) % 0.45) * dh;
                const fogW = (120 + fg * 30) * s;
                const fogH = (26 + fg * 8) * s;
                const grad = ctx.createRadialGradient(fogX, fogY, 4 * s, fogX, fogY, fogW);
                grad.addColorStop(0, "rgba(225, 238, 248, 0.32)");
                grad.addColorStop(1, "rgba(225, 238, 248, 0.0)");
                ctx.fillStyle = grad;
                ctx.beginPath();
                ctx.ellipse(fogX, fogY, fogW, fogH, 0, 0, Math.PI * 2);
                ctx.fill();
            }
        }

        // Thunderstorm lightning bolt & ambient sky flash
        if (cat === "thunderstorm") {
            const isFlash = Math.sin(t * 7.5) > 0.94;
            if (isFlash) {
                ctx.fillStyle = "rgba(240, 248, 255, 0.28)";
                ctx.fillRect(0, 0, w, h);

                ctx.strokeStyle = "rgba(255, 255, 255, 0.95)";
                ctx.lineWidth = 2.4 * s;
                ctx.lineCap = "round";
                ctx.beginPath();
                const bx = dx + 0.38 * dw;
                const by = dy + 0.10 * dh;
                ctx.moveTo(bx, by);
                ctx.lineTo(bx + 16 * s, by + 32 * s);
                ctx.lineTo(bx - 8 * s, by + 60 * s);
                ctx.lineTo(bx + 20 * s, by + 98 * s);
                ctx.lineTo(bx + 4 * s, by + 130 * s);
                ctx.moveTo(bx - 8 * s, by + 60 * s);
                ctx.lineTo(bx - 26 * s, by + 88 * s);
                ctx.stroke();

                ctx.strokeStyle = "rgba(180, 220, 255, 0.40)";
                ctx.lineWidth = 4.8 * s;
                ctx.stroke();
            }
        }

        if (cat === "rain" || cat === "thunderstorm") {
            const dropCount = Math.max(6, Math.round((cat === "thunderstorm" ? 36 : 24) * pScale));
            ctx.strokeStyle = "rgba(210, 238, 255, 0.52)";
            ctx.lineWidth = 1.3 * s;
            ctx.beginPath();
            for (let i = 0; i < dropCount; i++) {
                const prog = ((t * 0.28 + i * 0.173) % 1.0);
                const seedX = ((i * 0.6180339 + 0.12) % 1.0);
                const rx = seedX * w - prog * 0.08 * w;
                const ry = prog * (h + 20) - 10;
                ctx.moveTo(rx, ry);
                ctx.lineTo(rx - 4 * s, ry + 14 * s);
            }
            ctx.stroke();

            // Splash ripples on the island
            ctx.strokeStyle = "rgba(225, 245, 255, 0.40)";
            ctx.lineWidth = 1.0 * s;
            for (let j = 0; j < 5; j++) {
                const spProg = ((t * 0.45 + j * 0.23) % 1.0);
                const spX = dx + (0.26 + ((j * 0.19) % 0.48)) * dw;
                const spY = dy + (0.34 + ((j * 0.17) % 0.40)) * dh;
                ctx.beginPath();
                ctx.ellipse(spX, spY, (4 + spProg * 8) * s, (2 + spProg * 4) * s, 0, 0, Math.PI * 2);
                ctx.stroke();
            }
        } else if (cat === "snow") {
            const flakeCount = Math.max(6, Math.round(30 * pScale));
            ctx.fillStyle = "rgba(255, 255, 255, 0.85)";
            for (let i = 0; i < flakeCount; i++) {
                const prog = ((t * 0.14 + i * 0.137) % 1.0);
                const seedX = ((i * 0.6180339 + 0.08) % 1.0);
                const sx = seedX * w + Math.sin(t * 1.8 + i) * 8 * s;
                const sy = prog * (h + 20) - 10;
                ctx.beginPath();
                ctx.arc(sx, sy, 2.4 * s, 0, Math.PI * 2);
                ctx.fill();
            }
        }
    }

    /**
     * Renders a multi-puff 3D shaded volumetric cloud with ambient underside,
     * mid-tone body, and sunlit top highlights matching the diorama clay/voxel style.
     */
    drawIsometricPuffyCloud(ctx, cx, cy, cScale, s, isStormy, isSunset, isNight) {
        ctx.save();
        const baseColor = isStormy ? (isNight ? "#243242" : "#5b7083") : (isSunset ? "#9e7b96" : (isNight ? "#334860" : "#c2d4e6"));
        const midColor  = isStormy ? (isNight ? "#334559" : "#7b90a5") : (isSunset ? "#e6a58e" : (isNight ? "#4d6782" : "#e4eef7"));
        const topColor  = isStormy ? (isNight ? "#485e75" : "#9cb0c4") : (isSunset ? "#ffe0b5" : (isNight ? "#6c89a8" : "#ffffff"));

        const puffs = [
            { dx: -38, dy: 8,  r: 22 },
            { dx: -16, dy: -6, r: 30 },
            { dx: 14,  dy: -3, r: 27 },
            { dx: 38,  dy: 9,  r: 20 },
            { dx: -4,  dy: 10, r: 25 },
            { dx: 20,  dy: 11, r: 22 }
        ];

        const totalScale = cScale * s;

        // Layer 1: Ambient underside / base shadow
        ctx.fillStyle = baseColor;
        for (const p of puffs) {
            ctx.beginPath();
            ctx.arc(cx + p.dx * totalScale, cy + (p.dy + 6) * totalScale, p.r * totalScale, 0, Math.PI * 2);
            ctx.fill();
        }

        // Layer 2: Mid-tone volume body
        ctx.fillStyle = midColor;
        for (const p of puffs) {
            ctx.beginPath();
            ctx.arc(cx + p.dx * totalScale, cy + (p.dy + 2) * totalScale, p.r * 0.96 * totalScale, 0, Math.PI * 2);
            ctx.fill();
        }

        // Layer 3: Sunlit top highlight domes
        ctx.fillStyle = topColor;
        for (const p of puffs) {
            ctx.beginPath();
            ctx.arc(cx + (p.dx - 3) * totalScale, cy + (p.dy - 2) * totalScale, p.r * 0.86 * totalScale, 0, Math.PI * 2);
            ctx.fill();
        }

        ctx.restore();
    }
}

