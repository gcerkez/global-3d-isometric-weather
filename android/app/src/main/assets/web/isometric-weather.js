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
 *   - Built-In Pre-Generated 3D Isometric Diorama Themes (18 Regional, NPS & UNESCO Cutouts):
 *       1. "yorktown":            Historic Yorktown, VA (1781 Waterfront, Coleman Bridge & Tall Ship)
 *       2. "jamestown":           Historic Jamestown, VA (1607 James Fort Palisade, 3 Ships & Turquoise River)
 *       3. "washington_dc":       Washington, D.C. & Mid-Atlantic (US Capitol, Monument, Lincoln Memorial)
 *       4. "new_york":            New York City & Northeast (Statue of Liberty, Brooklyn Bridge, Empire State)
 *       5. "san_francisco":       San Francisco & California Coast (Golden Gate Bridge, Alcatraz, Painted Ladies)
 *       6. "chicago_great_lakes": Chicago & Great Lakes Midwest (Willis Tower, Red Bascule Bridge, Navy Pier)
 *       7. "grand_canyon":        Grand Canyon & Southwest (Red-Rock Buttes, Cliff Dwellings & Colorado River)
 *       8. "new_orleans":         New Orleans & Gulf South (St. Louis Cathedral, French Quarter & Steamboat)
 *       9. "seattle_pnw":         Seattle & Pacific Northwest (Space Needle, Pike Place, Mt. Rainier & Ferry)
 *      10. "miami_caribbean":     Miami Beach, Florida Keys & Tropics (Art Deco Ocean Drive & Coral Lagoon)
 *      11. "coastal":             New England & Maritime Atlantic (Lighthouse Point, Schooner & Wharf)
 *      12. "london_uk":           London & UK (Big Ben, Parliament, Tower of London & Thames Bridge)
 *      13. "paris_europe":        Paris & Western Europe (Notre-Dame, Louvre Palace, Arc & Seine Bridge)
 *      14. "rome_mediterranean":  Rome, Athens & Mediterranean (Colosseum, Parthenon, Aqueduct & Harbor)
 *      15. "cairo_giza":          Cairo, Giza & Middle East (Ancient Citadel, Sacred Temple Pool & Pharos)
 *      16. "tokyo_east_asia":     Tokyo, Kyoto & East Asia (5-Story Pagoda, Imperial Hall & Torii Bridge)
 *      17. "modern":              Global Modern Metropolis (Sydney / Singapore / Toronto Skyline & Bridge)
 *      18. "alpine":              Alpine Mountain & Glacial Lake Village (Rockies / Swiss Alps / Andes)
 *   - Isometric Zoom & Horizontal Centering (move left/right/center while HUD overlay stays fixed)
 *   - Static Miniature Diorama Sprites (Horse & Carriage, Cars, Sailboats)
 */

export const VALID_DIORAMA_THEMES = [
    "yorktown", "jamestown", "washington_dc", "new_york", "san_francisco",
    "chicago_great_lakes", "grand_canyon", "new_orleans", "seattle_pnw",
    "miami_caribbean", "coastal", "london_uk", "paris_europe",
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

            // Isometric Zoom & Horizontal Offset (rotation intentionally disabled)
            cameraZoom: options.cameraZoom ?? 1.0,
            cameraPanX: options.cameraPanX ?? 0.0,
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
     * Adjusts isometric zoom or horizontal centering/offset programmatically (rotation removed).
     * @param {"zoom_in"|"zoom_out"|"pan_left"|"pan_right"|"center"|"reset"} action
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
        } else if (action === "center") {
            this.state.cameraPanX = 0;
        } else if (action === "reset") {
            this.state.cameraZoom = 1.0;
            this.state.cameraPanX = 0;
        }
    }

    /**
     * Binds horizontal drag (to center/move the isometric left or right), pinch-to-zoom, wheel zoom, and double-click reset.
     */
    _bindInteraction() {
        let dragging = false, lastX = 0;
        let lastPinchDist = 0;

        this.canvas.addEventListener("pointerdown", e => {
            dragging = true;
            lastX = e.clientX;
        });
        window.addEventListener("pointerup", () => { dragging = false; });
        window.addEventListener("pointermove", e => {
            if (!dragging) return;
            const dx = e.clientX - lastX;
            this.state.cameraPanX = Math.max(-220, Math.min(220, this.state.cameraPanX + dx * 0.65));
            lastX = e.clientX;
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
        const dy = (h - dh) * 0.5;

        ctx.save();
        const cx = dx + dw * 0.5;
        const cy = dy + dh * 0.5;
        ctx.translate(cx, cy);
        ctx.scale(this.state.cameraZoom, this.state.cameraZoom);
        ctx.translate(-cx, -cy);

        if (img && img.complete && img.naturalWidth > 0) {
            ctx.drawImage(img, dx, dy, dw, dh);
        }

        // 2B. Water wave sparkles & static scenic miniature sprites (animated walking people removed)
        const s = dw / 1376.0;
        const waterPts = [
            { x: 0.14, y: 0.47 }, { x: 0.18, y: 0.52 }, { x: 0.24, y: 0.56 },
            { x: 0.28, y: 0.46 }, { x: 0.33, y: 0.42 }, { x: 0.38, y: 0.34 },
            { x: 0.44, y: 0.28 }, { x: 0.49, y: 0.32 }
        ];
        for (let wi = 0; wi < waterPts.length; wi++) {
            const wp = waterPts[wi];
            const waveAlpha = 0.20 + 0.18 * Math.sin(t * 2.2 + wi * 1.3);
            const wx = dx + (wp.x + Math.sin(t * 0.9 + wi) * 0.006) * dw;
            const wy = dy + wp.y * dh;
            ctx.strokeStyle = tState === "sunset"
                ? `rgba(255, 210, 145, ${waveAlpha.toFixed(3)})`
                : `rgba(225, 245, 255, ${waveAlpha.toFixed(3)})`;
            ctx.lineWidth = 1.6 * s;
            ctx.beginPath();
            ctx.moveTo(wx - 10 * s, wy - 4 * s);
            ctx.lineTo(wx + 10 * s, wy + 4 * s);
            ctx.stroke();
        }

        if (this.state.dioramaTheme === "yorktown") {
            // Static Sprite 1: Colonial Horse & Carriage stationed on Historic Main Street
            const hcX = dx + 0.442 * dw;
            const hcY = dy + 0.648 * dh;
            const hx = hcX + 4.1 * s;
            const hy = hcY - 3.0 * s;

            ctx.strokeStyle = "rgba(70, 48, 32, 0.75)";
            ctx.lineWidth = 0.9 * s;
            ctx.beginPath();
            ctx.moveTo(hcX, hcY - 2.0 * s);
            ctx.lineTo(hx, hy - 1.6 * s);
            ctx.stroke();

            ctx.fillStyle = "#6e4226";
            ctx.beginPath();
            ctx.ellipse(hx, hy - 1.5 * s, 2.1 * s, 1.3 * s, 0, 0, Math.PI * 2);
            ctx.fill();

            ctx.fillStyle = "#3b2316";
            ctx.beginPath();
            ctx.ellipse(hcX, hcY - 2.5 * s, 3.5 * s, 2.1 * s, 0, 0, Math.PI * 2);
            ctx.fill();
            ctx.fillStyle = "#9e3b2b";
            ctx.beginPath();
            ctx.ellipse(hcX, hcY - 3.8 * s, 2.8 * s, 1.5 * s, 0, 0, Math.PI * 2);
            ctx.fill();

            // Static Sprite 2 & 3: Miniature Cars on the George P. Coleman Memorial Bridge
            const carX = dx + 0.365 * dw;
            const carY = dy + 0.384 * dh;
            ctx.fillStyle = "#d64035";
            ctx.beginPath();
            ctx.ellipse(carX, carY - 1.2 * s, 2.4 * s, 1.3 * s, 0, 0, Math.PI * 2);
            ctx.fill();
            ctx.fillStyle = "#f2f6fa";
            ctx.beginPath();
            ctx.ellipse(carX, carY - 2.1 * s, 1.4 * s, 0.8 * s, 0, 0, Math.PI * 2);
            ctx.fill();

            const car2X = dx + 0.422 * dw;
            const car2Y = dy + 0.425 * dh;
            ctx.fillStyle = "#2b6cb0";
            ctx.beginPath();
            ctx.ellipse(car2X, car2Y - 1.2 * s, 2.3 * s, 1.2 * s, 0, 0, Math.PI * 2);
            ctx.fill();
        }

        ctx.restore();

        // 3. Gentle, Natural-Speed Weather Particles (Rain / Snow)
        const pScale = Math.max(0.15, this.state.particleIntensity ?? 1.0);
        if (cat === "rain" || cat === "thunderstorm") {
            const dropCount = Math.max(4, Math.round((cat === "thunderstorm" ? 32 : 20) * pScale));
            ctx.strokeStyle = "rgba(205, 232, 255, 0.46)";
            ctx.lineWidth = 1.2;
            ctx.beginPath();
            for (let i = 0; i < dropCount; i++) {
                const prog = ((t * 0.24 + i * 0.173) % 1.0);
                const rx = (((i * 0.618) % 1.1) - prog * 0.14) * w;
                const ry = prog * h;
                ctx.moveTo(rx, ry);
                ctx.lineTo(rx - 4, ry + 14);
            }
            ctx.stroke();
        } else if (cat === "snow") {
            const flakeCount = Math.max(5, Math.round(26 * pScale));
            ctx.fillStyle = "rgba(255, 255, 255, 0.82)";
            for (let i = 0; i < flakeCount; i++) {
                const prog = ((t * 0.10 + i * 0.137) % 1.0);
                const sx = (((i * 0.618) % 1.0) + Math.sin(t * 0.8 + i) * 0.02) * w;
                const sy = prog * h;
                ctx.beginPath();
                ctx.arc(sx, sy, 2.2, 0, Math.PI * 2);
                ctx.fill();
            }
        }
    }
}
