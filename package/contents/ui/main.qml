/**
 * main.qml
 * --------
 * Root KDE Plasma 6 PlasmoidItem for the 3D Isometric Weather Widget
 * (com.antigravity.yorktownweather).
 *
 * Orchestrates:
 *   - Open-Meteo Forecast API polling (current telemetry, 12h hourly sparkline,
 *     UV index, 3-day forecast, sunrise/sunset golden hour detection)
 *   - Open-Meteo Air Quality API polling (US EPA AQI + PM2.5)
 *   - Astronomical lunar synodic phase calculation (AstronomyAndAqi.js)
 *   - Generative Location Discovery (LocationGenerator.js):
 *       * Wikipedia GeoSearch API for real-time local landmarks
 *       * OpenStreetMap Overpass API for 3D building footprints
 *       * Regional 3D architectural archetype inference
 *       * On-demand AI Isometric Diorama synthesis URL generation
 */
import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as QQC2
import org.kde.plasma.plasmoid
import org.kde.plasma.core as PlasmaCore
import org.kde.kirigami as Kirigami
import "js/AstronomyAndAqi.js" as Astro
import "js/LocationGenerator.js" as LocGen

PlasmoidItem {
    id: root

    width: 688
    height: 384
    Layout.minimumWidth: 320
    Layout.minimumHeight: 200
    Layout.preferredWidth: 688
    Layout.preferredHeight: 384

    Plasmoid.backgroundHints: PlasmaCore.Types.NoBackground
    preferredRepresentation: fullRepresentation

    // Configuration bindings with sensible Yorktown, VA defaults
    readonly property string cfgLocationName: Plasmoid.configuration.locationName || "Yorktown, VA"
    readonly property string cfgRegionSubtitle: Plasmoid.configuration.regionSubtitle || "Historic Colonial Waterfront & 1781 Battlefield"
    readonly property double cfgLatitude: Plasmoid.configuration.latitude !== undefined ? Plasmoid.configuration.latitude : 37.2388
    readonly property double cfgLongitude: Plasmoid.configuration.longitude !== undefined ? Plasmoid.configuration.longitude : -76.5097
    readonly property string cfgTempUnit: Plasmoid.configuration.temperatureUnit || "BOTH"
    readonly property string cfgWindUnit: Plasmoid.configuration.windSpeedUnit || "mph"
    readonly property int cfgRefreshMins: Plasmoid.configuration.refreshIntervalMinutes || 15
    readonly property string cfgRenderMode: Plasmoid.configuration.renderMode || "hybrid"
    readonly property string cfgDioramaTheme: Plasmoid.configuration.dioramaTheme || "yorktown"
    readonly property double cfgDioramaZoom: Plasmoid.configuration.dioramaZoom !== undefined ? Plasmoid.configuration.dioramaZoom : 1.0
    readonly property int cfgDioramaOffsetX: Plasmoid.configuration.dioramaOffsetX !== undefined ? Plasmoid.configuration.dioramaOffsetX : 0
    readonly property int cfgDioramaOffsetY: Plasmoid.configuration.dioramaOffsetY !== undefined ? Plasmoid.configuration.dioramaOffsetY : 0
    readonly property string cfgTimeMode: Plasmoid.configuration.timeOfDayMode || "realtime"
    readonly property string cfgWeatherOverride: Plasmoid.configuration.weatherOverride || "realtime"
    readonly property string cfgHudPosition: Plasmoid.configuration.hudPosition || "top-right"
    readonly property double cfgHudOpacity: Plasmoid.configuration.hudOpacity !== undefined ? Plasmoid.configuration.hudOpacity : 0.82
    readonly property bool cfgTransparentBg: Plasmoid.configuration.transparentBackground || false
    readonly property bool cfgShowLandmarks: Plasmoid.configuration.showLandmarkLabels !== undefined ? Plasmoid.configuration.showLandmarkLabels : true
    readonly property bool cfgShowBadge: Plasmoid.configuration.showLocationBadge !== undefined ? Plasmoid.configuration.showLocationBadge : true
    readonly property double cfgParticleDensity: Plasmoid.configuration.particleDensity !== undefined ? Plasmoid.configuration.particleDensity : 1.0

    // Live telemetry state
    property double liveTempF: 68.0
    property double liveFeelsLikeF: 67.0
    property int liveHumidity: 68
    property double liveWindMph: 8.0
    property int liveWindDeg: 25
    property int liveWeatherCode: 0
    property int previousWeatherCode: 0
    property bool liveIsDay: true
    property bool liveIsGoldenHour: false
    property int liveUsAqi: 24
    property double liveUvIndex: 3.2
    property var liveHourlyTemps: [68, 69, 71, 72, 71, 69, 67, 65, 63, 62, 61, 60]
    property var liveHourlyPrecipProb: [5, 5, 10, 10, 15, 10, 5, 5, 0, 0, 5, 5]
    property string lastFetchTime: "Just now"
    property var liveForecastDays: [
        { dayName: "TODAY", tempRange: "70° / 56°" },
        { dayName: "TOMORROW", tempRange: "72° / 58°" },
        { dayName: "DAY 3", tempRange: "67° / 54°" }
    ]

    // Generative Location state (populated automatically for any global city)
    property var dynamicLandmarks: []
    property var dynamicOsmBuildings: []
    property var currentArchetype: LocGen.inferRegionalArchetype(cfgLocationName, cfgLatitude, cfgLongitude, "")
    property string aiDioramaUrl: ""

    // Astronomical Moon Phase state
    property var moonData: Astro.computeMoonPhase(new Date())

    // Computed AQI & UV metadata
    readonly property var aqiInfo: Astro.classifyUsAqi(liveUsAqi)
    readonly property var uvInfo: Astro.classifyUvIndex(liveUvIndex)

    // True if location is near Historic Yorktown, VA (within ~0.08 deg)
    readonly property bool isYorktownDefault: (Math.abs(cfgLatitude - 37.2388) < 0.08 && Math.abs(cfgLongitude - (-76.5097)) < 0.08)

    // Computed effective weather & time states
    readonly property string effectiveWeatherCategory: {
        if (cfgWeatherOverride !== "realtime") return cfgWeatherOverride
        return wmoToCategory(liveWeatherCode)
    }

    readonly property bool showPostRainRainbow: {
        var curCat = effectiveWeatherCategory
        var prevCat = wmoToCategory(previousWeatherCode)
        return (effectiveTimeState !== "night") && (curCat === "clear" || curCat === "partly_cloudy") && (prevCat === "rain" || prevCat === "thunderstorm")
    }

    readonly property string effectiveConditionLabel: {
        if (cfgWeatherOverride !== "realtime") {
            if (cfgWeatherOverride === "clear") return "CLEAR WEATHER"
            if (cfgWeatherOverride === "partly_cloudy") return "PARTLY CLOUDY"
            if (cfgWeatherOverride === "overcast") return "OVERCAST SKY"
            if (cfgWeatherOverride === "rain") return "RAIN SHOWER"
            if (cfgWeatherOverride === "thunderstorm") return "THUNDERSTORM"
            if (cfgWeatherOverride === "snow") return "SNOWFALL"
            if (cfgWeatherOverride === "fog") return "COASTAL FOG"
        }
        return wmoToDescription(liveWeatherCode)
    }

    readonly property string effectiveTimeState: {
        if (cfgTimeMode !== "realtime") return cfgTimeMode
        if (!liveIsDay) return "night"
        if (liveIsGoldenHour) return "sunset"
        return "day"
    }

    readonly property string formattedTemperature: {
        var f = Math.round(liveTempF)
        var c = Math.round((liveTempF - 32.0) * 5.0 / 9.0)
        if (cfgTempUnit === "F") return f + "°F"
        if (cfgTempUnit === "C") return c + "°C"
        return f + "°F (" + c + "°C)"
    }

    readonly property string formattedFeelsLike: {
        var f = Math.round(liveFeelsLikeF)
        var c = Math.round((liveFeelsLikeF - 32.0) * 5.0 / 9.0)
        if (cfgTempUnit === "C") return c + "°C"
        return f + "°F"
    }

    readonly property string formattedWind: {
        var spd = liveWindMph
        var unitLabel = "MPH"
        if (cfgWindUnit === "kmh") {
            spd = liveWindMph * 1.60934
            unitLabel = "KM/H"
        } else if (cfgWindUnit === "kn") {
            spd = liveWindMph * 0.868976
            unitLabel = "KN"
        } else if (cfgWindUnit === "ms") {
            spd = liveWindMph * 0.44704
            unitLabel = "M/S"
        }
        var dir = degToCompass16(liveWindDeg)
        return Math.round(spd) + " " + unitLabel + " " + dir + " WIND"
    }

    /**
     * Converts wind direction degrees (0..360) to 16-point compass cardinal label.
     */
    function degToCompass16(deg) {
        var dirs = ["N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"]
        var idx = Math.round(((deg % 360) + 360) % 360 / 22.5) % 16
        return dirs[idx]
    }

    /**
     * Maps WMO weather interpretation code to internal visual weather category.
     */
    function wmoToCategory(code) {
        if (code === 0) return "clear"
        if (code === 1 || code === 2) return "partly_cloudy"
        if (code === 3) return "overcast"
        if (code === 45 || code === 48) return "fog"
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "rain"
        if ((code >= 71 && code <= 77) || code === 85 || code === 86) return "snow"
        if (code >= 95) return "thunderstorm"
        return "clear"
    }

    /**
     * Maps WMO weather code to an uppercase HUD headline string matching the reference card.
     */
    function wmoToDescription(code) {
        if (code === 0) return "CLEAR WEATHER"
        if (code === 1) return "MAINLY CLEAR"
        if (code === 2) return "PARTLY CLOUDY"
        if (code === 3) return "OVERCAST SKY"
        if (code === 45 || code === 48) return "COASTAL FOG"
        if (code >= 51 && code <= 55) return "LIGHT DRIZZLE"
        if (code === 56 || code === 57) return "FREEZING DRIZZLE"
        if (code === 61) return "LIGHT RAIN"
        if (code === 63) return "MODERATE RAIN"
        if (code === 65) return "HEAVY RAIN"
        if (code === 66 || code === 67) return "FREEZING RAIN"
        if (code === 71) return "LIGHT SNOW"
        if (code === 73) return "SNOWFALL"
        if (code === 75 || code === 77) return "HEAVY SNOW"
        if (code >= 80 && code <= 82) return "RAIN SHOWERS"
        if (code === 85 || code === 86) return "SNOW SHOWERS"
        if (code === 95) return "THUNDERSTORM"
        if (code >= 96) return "HEAVY STORM"
        return "CLEAR WEATHER"
    }

    /**
     * Fetches current weather, 12-hour sparkline telemetry, UV index, and 3-day forecast
     * from the Open-Meteo Forecast API.
     */
    function fetchWeatherData() {
        root.moonData = Astro.computeMoonPhase(new Date())

        var url = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=" + encodeURIComponent(cfgLatitude)
            + "&longitude=" + encodeURIComponent(cfgLongitude)
            + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,wind_direction_10m"
            + "&hourly=temperature_2m,precipitation_probability,uv_index"
            + "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max"
            + "&temperature_unit=fahrenheit&wind_speed_unit=mph&timezone=auto&forecast_days=3"

        var xhr = new XMLHttpRequest()
        xhr.open("GET", url)
        xhr.onreadystatechange = function() {
            if (xhr.readyState === XMLHttpRequest.DONE && xhr.status === 200) {
                try {
                    var data = JSON.parse(xhr.responseText)
                    if (data.current) {
                        root.previousWeatherCode = root.liveWeatherCode
                        root.liveTempF = data.current.temperature_2m
                        root.liveFeelsLikeF = data.current.apparent_temperature
                        root.liveHumidity = Math.round(data.current.relative_humidity_2m)
                        root.liveWindMph = data.current.wind_speed_10m
                        root.liveWindDeg = data.current.wind_direction_10m
                        root.liveWeatherCode = data.current.weather_code
                        root.liveIsDay = (data.current.is_day === 1)
                    }
                    if (data.hourly && data.hourly.temperature_2m) {
                        var nowHour = (new Date()).getHours()
                        var temps12 = []
                        var pop12 = []
                        for (var h = 0; h < 12; h++) {
                            var idx = Math.min(data.hourly.temperature_2m.length - 1, nowHour + h)
                            var tf = data.hourly.temperature_2m[idx]
                            temps12.push(root.cfgTempUnit === "C" ? Math.round((tf - 32) * 5 / 9) : Math.round(tf))
                            pop12.push(data.hourly.precipitation_probability ? (data.hourly.precipitation_probability[idx] || 0) : 0)
                        }
                        root.liveHourlyTemps = temps12
                        root.liveHourlyPrecipProb = pop12
                        if (data.hourly.uv_index && data.hourly.uv_index.length > nowHour) {
                            root.liveUvIndex = data.hourly.uv_index[nowHour] || 0.0
                        }
                    }
                    if (data.daily && data.daily.time && data.daily.time.length >= 3) {
                        var days = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"]
                        var parsed = []
                        for (var i = 0; i < 3; i++) {
                            var dt = new Date(data.daily.time[i] + "T12:00:00")
                            var dName = (i === 0) ? "TODAY" : days[dt.getDay()]
                            var maxF = Math.round(data.daily.temperature_2m_max[i])
                            var minF = Math.round(data.daily.temperature_2m_min[i])
                            if (root.cfgTempUnit === "C") {
                                var maxC = Math.round((maxF - 32) * 5 / 9)
                                var minC = Math.round((minF - 32) * 5 / 9)
                                parsed.push({ dayName: dName, tempRange: maxC + "° / " + minC + "°C" })
                            } else {
                                parsed.push({ dayName: dName, tempRange: maxF + "° / " + minF + "°F" })
                            }
                        }
                        root.liveForecastDays = parsed

                        // Check solar cycle / Golden Hour via AstronomyAndAqi.js
                        if (data.daily.sunrise && data.daily.sunset) {
                            var solar = Astro.evaluateSolarCycle(Date.now(), data.daily.sunrise[0], data.daily.sunset[0])
                            root.liveIsGoldenHour = solar.isGoldenHour
                        }
                    }
                    var now = new Date()
                    root.lastFetchTime = Qt.formatTime(now, "hh:mm AP")
                } catch (e) {
                    console.warn("YorktownWeather: Error parsing Open-Meteo JSON:", e)
                }
            }
        }
        xhr.send()

        fetchAirQualityData()
    }

    /**
     * Fetches real-time US EPA Air Quality Index (AQI) from Open-Meteo Air Quality API.
     */
    function fetchAirQualityData() {
        var aqUrl = "https://air-quality-api.open-meteo.com/v1/air-quality"
            + "?latitude=" + encodeURIComponent(cfgLatitude)
            + "&longitude=" + encodeURIComponent(cfgLongitude)
            + "&current=us_aqi,pm2_5"
        var xhr = new XMLHttpRequest()
        xhr.open("GET", aqUrl)
        xhr.onreadystatechange = function() {
            if (xhr.readyState === XMLHttpRequest.DONE && xhr.status === 200) {
                try {
                    var res = JSON.parse(xhr.responseText)
                    if (res.current && res.current.us_aqi !== undefined && res.current.us_aqi !== null) {
                        root.liveUsAqi = Math.round(res.current.us_aqi)
                    }
                } catch (e) {
                    console.warn("YorktownWeather: Error parsing Air Quality JSON:", e)
                }
            }
        }
        xhr.send()
    }

    /**
     * Updates generative location data (Wikipedia landmarks, OpenStreetMap 3D buildings,
     * regional architectural archetype, and AI diorama synthesis URL) when location changes.
     */
    function refreshGenerativeLocation() {
        root.currentArchetype = LocGen.inferRegionalArchetype(cfgLocationName, cfgLatitude, cfgLongitude, "")
        if (root.isYorktownDefault) {
            root.dynamicLandmarks = []
            root.dynamicOsmBuildings = []
            root.aiDioramaUrl = LocGen.buildGenerativeDioramaUrl(
                cfgLocationName,
                ["George P. Coleman Memorial Bridge", "Yorktown Victory Monument", "1781 Siege Redoubts"],
                effectiveTimeState,
                effectiveWeatherCategory
            )
            return
        }

        LocGen.fetchWikipediaLandmarks(cfgLatitude, cfgLongitude, function(landmarks) {
            root.dynamicLandmarks = landmarks
            var titles = []
            for (var i = 0; i < landmarks.length; i++) titles.push(landmarks[i].title)
            root.aiDioramaUrl = LocGen.buildGenerativeDioramaUrl(
                cfgLocationName,
                titles,
                effectiveTimeState,
                effectiveWeatherCategory
            )
        })

        LocGen.fetchOsmBuildings(cfgLatitude, cfgLongitude, function(buildings) {
            root.dynamicOsmBuildings = buildings
        })
    }

    onCfgLatitudeChanged: {
        fetchWeatherData()
        refreshGenerativeLocation()
    }
    onCfgLongitudeChanged: {
        fetchWeatherData()
        refreshGenerativeLocation()
    }
    onCfgLocationNameChanged: refreshGenerativeLocation()

    Component.onCompleted: {
        fetchWeatherData()
        refreshGenerativeLocation()
    }

    Timer {
        interval: Math.max(5, root.cfgRefreshMins) * 60 * 1000
        running: true
        repeat: true
        onTriggered: root.fetchWeatherData()
    }

    fullRepresentation: Item {
        id: fullRep
        anchors.fill: parent

        readonly property real uiScale: Math.max(0.65, Math.min(1.45, Math.min(width / 688.0, height / 384.0)))

        // 1. Dynamic Sky Backdrop Card (matches the reference San Francisco sky card, or transparent in Desktop Cutout mode)
        Rectangle {
            id: skyCard
            anchors.fill: parent
            radius: 14
            visible: !root.cfgTransparentBg
            border.color: root.effectiveTimeState === "night" ? "#3b526b" : "#bce0f7"
            border.width: 1.5
            clip: true

            property color skyTopColor: {
                if (root.effectiveTimeState === "night") return "#0d1b2a"
                if (root.effectiveTimeState === "sunset") return "#3a506b"
                if (root.effectiveWeatherCategory === "thunderstorm") return "#3a4e60"
                if (root.effectiveWeatherCategory === "rain" || root.effectiveWeatherCategory === "overcast") return "#5c768d"
                if (root.effectiveWeatherCategory === "fog") return "#8da8bf"
                return "#7fc4f2" // Crisp sunny day top blue matching reference image
            }

            property color skyBottomColor: {
                if (root.effectiveTimeState === "night") return "#1b314c"
                if (root.effectiveTimeState === "sunset") return "#f29a6b"
                if (root.effectiveWeatherCategory === "thunderstorm") return "#586f82"
                if (root.effectiveWeatherCategory === "rain" || root.effectiveWeatherCategory === "overcast") return "#88a0b5"
                if (root.effectiveWeatherCategory === "fog") return "#bfd2e3"
                return "#a8dbf8" // Reference image horizon light blue (#a8dbf8)
            }

            Behavior on skyTopColor { ColorAnimation { duration: 600 } }
            Behavior on skyBottomColor { ColorAnimation { duration: 600 } }

            gradient: Gradient {
                GradientStop { position: 0.0; color: skyCard.skyTopColor }
                GradientStop { position: 1.0; color: skyCard.skyBottomColor }
            }

            // Starfield when in Night mode
            Canvas {
                id: starCanvas
                anchors.fill: parent
                visible: root.effectiveTimeState === "night"
                opacity: visible ? 0.85 : 0.0
                Behavior on opacity { NumberAnimation { duration: 500 } }
                onPaint: {
                    var ctx = getContext("2d")
                    ctx.clearRect(0, 0, width, height)
                    ctx.fillStyle = "#ffffff"
                    for (var i = 0; i < 48; i++) {
                        var sx = ((i * 0.6180339) % 1.0) * width
                        var sy = ((i * 0.381966) % 0.65) * height
                        var sr = (i % 3 === 0) ? 1.6 : 1.0
                        ctx.globalAlpha = 0.35 + (i % 5) * 0.14
                        ctx.beginPath()
                        ctx.arc(sx, sy, sr, 0, Math.PI * 2)
                        ctx.fill()
                    }
                }
            }
        }

        // 2. Back-Layer 3D Volumetric Clouds, Moon Phase Disc & Post-Rain Rainbow Arc
        WeatherEffectsOverlay {
            anchors.fill: parent
            weatherCategory: root.effectiveWeatherCategory
            timeState: root.effectiveTimeState
            windSpeedMph: root.liveWindMph
            particleDensity: root.cfgParticleDensity
            moonPhase: root.moonData.phase
            showRainbow: root.showPostRainRainbow
            renderBackLayerOnly: true
            renderFrontLayerOnly: false
        }

        // 3A. Pre-Generated 3D Isometric Cutout Mode: High-Detail 3D Isometric Diorama + Animated Water, Frost & Night Illumination
        YorktownHybridScene {
            anchors.fill: parent
            visible: root.cfgRenderMode !== "procedural3d"
            weatherCategory: root.effectiveWeatherCategory
            timeState: root.effectiveTimeState
            showLandmarkLabels: root.cfgShowLandmarks
            dynamicLandmarks: root.dynamicLandmarks
            dioramaTheme: root.cfgDioramaTheme
            cameraZoom: root.cfgDioramaZoom
            cameraPanX: root.cfgDioramaOffsetX
            cameraPanY: root.cfgDioramaOffsetY
            onZoomEdited: function(newZoom) {
                Plasmoid.configuration.dioramaZoom = Math.round(newZoom * 100) / 100
            }
            onPanXEdited: function(newPanX) {
                Plasmoid.configuration.dioramaOffsetX = Math.round(newPanX)
            }
            onPanYEdited: function(newPanY) {
                Plasmoid.configuration.dioramaOffsetY = Math.round(newPanY)
            }
        }

        // 3B. Procedural 3D Mode: Interactive Zoomable & Horizontally Centerable 3D Isometric Model (Yorktown or Generative OSM City)
        YorktownProcedural3D {
            anchors.fill: parent
            visible: root.cfgRenderMode === "procedural3d"
            weatherCategory: root.effectiveWeatherCategory
            timeState: root.effectiveTimeState
            locationName: root.cfgLocationName
            windSpeedMph: root.liveWindMph
            regionalArchetype: root.currentArchetype
            osmBuildings: root.dynamicOsmBuildings
            cameraZoom: root.cfgDioramaZoom
            cameraPanX: root.cfgDioramaOffsetX
            cameraPanY: root.cfgDioramaOffsetY
            onZoomEdited: function(newZoom) {
                Plasmoid.configuration.dioramaZoom = Math.round(newZoom * 100) / 100
            }
            onPanXEdited: function(newPanX) {
                Plasmoid.configuration.dioramaOffsetX = Math.round(newPanX)
            }
            onPanYEdited: function(newPanY) {
                Plasmoid.configuration.dioramaOffsetY = Math.round(newPanY)
            }
        }

        // 4. Front-Layer 3D Volumetric Clouds, Ground Cloud Shadows, Rain Streaks, Water Ripples, Snowflakes, Wind Streaks & Fog
        WeatherEffectsOverlay {
            anchors.fill: parent
            weatherCategory: root.effectiveWeatherCategory
            timeState: root.effectiveTimeState
            windSpeedMph: root.liveWindMph
            particleDensity: root.cfgParticleDensity
            moonPhase: root.moonData.phase
            showRainbow: false
            renderBackLayerOnly: false
            renderFrontLayerOnly: true
        }

        // 5. Reference-Style Weather HUD Overlay Card (Default: Top-Right matching San Francisco reference)
        WeatherHudCard {
            id: weatherHud
            scaleFactor: fullRep.uiScale
            conditionText: root.effectiveConditionLabel
            weatherCategory: root.effectiveWeatherCategory
            isNight: root.effectiveTimeState === "night"
            temperatureText: root.formattedTemperature
            feelsLikeText: root.formattedFeelsLike
            humidity: root.liveHumidity
            windText: root.formattedWind
            locationName: root.cfgLocationName
            lastUpdated: root.lastFetchTime
            cardOpacity: root.cfgHudOpacity
            forecastDays: root.liveForecastDays
            forecastUrl: {
                if (root.cfgLatitude >= 18.0 && root.cfgLatitude <= 72.0 && root.cfgLongitude >= -170.0 && root.cfgLongitude <= -65.0) {
                    return "https://forecast.weather.gov/MapClick.php?lat=" + root.cfgLatitude + "&lon=" + root.cfgLongitude
                }
                return "https://weather.com/weather/today/l/" + root.cfgLatitude + "," + root.cfgLongitude
            }

            aqiValue: root.liveUsAqi
            aqiLabel: root.aqiInfo.label
            aqiColor: root.aqiInfo.color
            uvIndex: root.liveUvIndex
            uvLabel: root.uvInfo.label
            uvColor: root.uvInfo.color
            moonPhaseName: root.moonData.name
            moonPhaseIcon: root.moonData.emoji
            hourlyTemps: root.liveHourlyTemps
            hourlyPrecipProb: root.liveHourlyPrecipProb

            currentRenderMode: root.cfgRenderMode
            currentTimeMode: root.cfgTimeMode
            currentWeatherOverride: root.cfgWeatherOverride

            anchors.top: (root.cfgHudPosition === "top-right" || root.cfgHudPosition === "top-left") ? parent.top : undefined
            anchors.bottom: (root.cfgHudPosition === "bottom-right" || root.cfgHudPosition === "bottom-left") ? parent.bottom : undefined
            anchors.right: (root.cfgHudPosition === "top-right" || root.cfgHudPosition === "bottom-right") ? parent.right : undefined
            anchors.left: (root.cfgHudPosition === "top-left" || root.cfgHudPosition === "bottom-left") ? parent.left : undefined
            anchors.margins: Math.round(16 * fullRep.uiScale)

            onToggleRenderModeRequested: {
                Plasmoid.configuration.renderMode = (root.cfgRenderMode === "hybrid") ? "procedural3d" : "hybrid"
            }
            onCycleTimeModeRequested: {
                var modes = ["realtime", "day", "sunset", "night"]
                var idx = (modes.indexOf(root.cfgTimeMode) + 1) % modes.length
                Plasmoid.configuration.timeOfDayMode = modes[idx]
            }
            onCycleWeatherOverrideRequested: {
                var wxModes = ["realtime", "clear", "partly_cloudy", "rain", "thunderstorm", "snow", "fog"]
                var idx = (wxModes.indexOf(root.cfgWeatherOverride) + 1) % wxModes.length
                Plasmoid.configuration.weatherOverride = wxModes[idx]
            }
            onRefreshRequested: {
                root.fetchWeatherData()
                root.refreshGenerativeLocation()
            }
        }

        // 6. Subtle Bottom-Right Location & Historic Theme Badge (matching the placement of the bottom-right pill in the reference)
        Rectangle {
            visible: root.cfgShowBadge && root.cfgHudPosition !== "bottom-right"
            anchors.right: parent.right
            anchors.bottom: parent.bottom
            anchors.rightMargin: Math.round(14 * fullRep.uiScale)
            anchors.bottomMargin: Math.round(12 * fullRep.uiScale)
            height: Math.round(24 * fullRep.uiScale)
            width: badgeText.implicitWidth + Math.round(20 * fullRep.uiScale)
            radius: Math.round(5 * fullRep.uiScale)
            color: badgeMouse.containsMouse ? "#d8253545" : "#b822303e"
            border.color: Qt.rgba(1, 1, 1, 0.22)
            border.width: 1

            Text {
                id: badgeText
                anchors.centerIn: parent
                text: root.cfgLocationName + " • " + (root.cfgRenderMode === "hybrid" ? "3D Isometric" : (root.cfgRenderMode === "ai_generative" ? "AI Generative 3D" : "Interactive 3D"))
                color: "#eef6fc"
                font.pixelSize: Math.max(9, Math.round(11 * fullRep.uiScale))
                font.weight: Font.Medium
                font.letterSpacing: 0.4
            }

            MouseArea {
                id: badgeMouse
                anchors.fill: parent
                hoverEnabled: true
                cursorShape: Qt.PointingHandCursor
                onClicked: {
                    Plasmoid.configuration.renderMode = (root.cfgRenderMode === "hybrid") ? "procedural3d" : "hybrid"
                }
            }
        }
    }
}
